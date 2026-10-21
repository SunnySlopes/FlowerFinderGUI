#include "core.h"

#include <float.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifdef _OPENMP
#include <omp.h>
#endif

static int *prog_cur;
static int *prog_tot;
static int prog_preset;

void cts_bind_progress(int *current, int *total)
{
    prog_cur = current;
    prog_tot = total;
    prog_preset = 0;
}

static void progress_set_total(int n)
{
    if (prog_tot && n > 0)
        __atomic_store_n(prog_tot, n, __ATOMIC_RELAXED);
}

static void progress_expect(int n)
{
    if (!prog_preset)
        progress_set_total(n);
}

static void progress_tick(void)
{
    if (prog_cur)
        __atomic_fetch_add(prog_cur, 1, __ATOMIC_RELAXED);
}

/* Swamp needs high E, flower needs low H. Pale also needs low E + high H. */
static int eh_span_ok(double emin, double emax, double hmin, double hmax,
                     const ScanCfg *cfg, double me, double mh)
{
    if (!(emax > cfg->e_hi - me && hmin < cfg->h_lo + mh))
        return 0;
    if (!cfg->no_pale &&
        !(emin < cfg->e_lo + me && hmax > cfg->h_hi - mh))
        return 0;
    return 1;
}

/* ---------------- 1D / 2D sliding min-max (monotonic deque) ---------------- */

static void slide1d(const float *src, int n, int win, int *dq,
                    float *out_min, float *out_max)
{
    int i, head, tail;
    if (n < win || win < 1) return;
    head = tail = 0;
    for (i = 0; i < n; i++) {
        while (tail > head && src[dq[tail-1]] >= src[i]) tail--;
        dq[tail++] = i;
        if (dq[head] <= i - win) head++;
        if (i >= win - 1) out_min[i - win + 1] = src[dq[head]];
    }
    head = tail = 0;
    for (i = 0; i < n; i++) {
        while (tail > head && src[dq[tail-1]] <= src[i]) tail--;
        dq[tail++] = i;
        if (dq[head] <= i - win) head++;
        if (i >= win - 1) out_max[i - win + 1] = src[dq[head]];
    }
}

/* out_min/out_max are (w-winx+1) * (h-winz+1), row-major. */
static void slide2d_xy(const float *f, int w, int h, int winx, int winz,
                       float *out_min, float *out_max)
{
    int ow = w - winx + 1, oh = h - winz + 1;
    int x, z, dqn = w > h ? w : h;
    int *dq;
    float *tm, *tx, *cm, *cx, *om, *ox;
    if (ow <= 0 || oh <= 0) return;
    if (dqn < winx) dqn = winx;
    if (dqn < winz) dqn = winz;
    dq = (int *)malloc((size_t)dqn * sizeof(int));
    tm = (float *)malloc((size_t)h * ow * sizeof(float));
    tx = (float *)malloc((size_t)h * ow * sizeof(float));
    cm = (float *)malloc((size_t)h * sizeof(float));
    cx = (float *)malloc((size_t)h * sizeof(float));
    om = (float *)malloc((size_t)oh * sizeof(float));
    ox = (float *)malloc((size_t)oh * sizeof(float));
    if (!dq || !tm || !tx || !cm || !cx || !om || !ox) {
        free(dq); free(tm); free(tx); free(cm); free(cx); free(om); free(ox);
        return;
    }
    for (z = 0; z < h; z++)
        slide1d(f + (size_t)z * w, w, winx, dq,
                tm + (size_t)z * ow, tx + (size_t)z * ow);
    for (x = 0; x < ow; x++) {
        for (z = 0; z < h; z++) {
            cm[z] = tm[(size_t)z * ow + x];
            cx[z] = tx[(size_t)z * ow + x];
        }
        slide1d(cm, h, winz, dq, om, ox);
        for (z = 0; z < oh; z++)
            out_min[(size_t)z * ow + x] = om[z];
        slide1d(cx, h, winz, dq, om, ox);
        for (z = 0; z < oh; z++)
            out_max[(size_t)z * ow + x] = ox[z];
    }
    free(dq); free(tm); free(tx); free(cm); free(cx); free(om); free(ox);
}

/* out_min/out_max are (w-win+1) * (h-win+1), row-major. */
static void slide2d(const float *f, int w, int h, int win,
                    float *out_min, float *out_max)
{
    slide2d_xy(f, w, h, win, win, out_min, out_max);
}

/* ---------------- hit list ---------------- */

static int push_hit(Hit **hits, int *nhits, int *cap, Hit h, int max_hits)
{
    int i;
    for (i = 0; i < *nhits; i++) {
        if ((*hits)[i].qx == h.qx && (*hits)[i].qz == h.qz) return 1;
    }
    if (*nhits >= max_hits) return 1;
    if (*nhits >= *cap) {
        int ncap = *cap ? *cap * 2 : 64;
        Hit *p = (Hit *)realloc(*hits, (size_t)ncap * sizeof(Hit));
        if (!p) return 0;
        *hits = p;
        *cap = ncap;
    }
    (*hits)[(*nhits)++] = h;
    return 1;
}

static double hit_score(const Hit *h, const ScanCfg *cfg)
{
    double s = h->w.e_max - cfg->e_hi;
    double v = cfg->h_lo - h->w.h_min;
    if (v < s) s = v;
    if (!cfg->no_pale) {
        v = cfg->e_lo - h->w.e_min; if (v < s) s = v;
        v = h->w.h_max - cfg->h_hi; if (v < s) s = v;
    }
    return s;
}

/* Merge hits that are within cfg->cluster blocks (Chebyshev) of a rep;
 * keep the one with the largest margin score.  O(n * reps), reps is small. */
static void cluster_hits(Hit **hits, int *nhits, const ScanCfg *cfg)
{
    int n = *nhits, m = 0, i, j;
    Hit *out;
    if (n <= 1 || cfg->cluster <= 0) return;
    out = (Hit *)malloc((size_t)n * sizeof(Hit));
    if (!out) return;
    for (i = 0; i < n; i++) {
        const Hit *h = &(*hits)[i];
        int found = -1;
        for (j = 0; j < m; j++) {
            if (abs(h->qx - out[j].qx) * 4 <= cfg->cluster &&
                abs(h->qz - out[j].qz) * 4 <= cfg->cluster) {
                found = j;
                break;
            }
        }
        if (found < 0) {
            out[m++] = *h;
        } else if (hit_score(h, cfg) > hit_score(&out[found], cfg)) {
            out[found] = *h;
        }
    }
    free(*hits);
    *hits = out;
    *nhits = m;
}

/* ---------------- multigrid screen ---------------- */

/* Coarse necessary condition: if the 16x16 (every 5 quart) subset cannot
 * reach the four thresholds even with a generous margin, the full window
 * cannot pass.  Lossy by design (fast mode). */
static int quick_possible_rect(const Climate *c, int qx, int qz,
                               int winx, int winz, const ScanCfg *cfg)
{
    const double me = cfg->qm_e, mh = cfg->qm_h;
    double emin = 1e30, emax = -1e30, hmin = 1e30, hmax = -1e30;
    int mx = winx - 1, mz = winz - 1;
    int midx = winx / 2, midz = winz / 2;
    int maxd = mx > mz ? mx : mz, t;
    int xs[4] = { 0, mx, 0, mx };
    int zs[4] = { 0, 0, mz, mz };
    int i;
    if (winx <= 0 || winz <= 0) return 0;
#define QSAMPLE(x, z) do { \
        double e_, h_; \
        climate_sample(c, (double)(qx + (x)), (double)(qz + (z)), 0, &e_, &h_); \
        if (e_ < emin) emin = e_; if (e_ > emax) emax = e_; \
        if (h_ < hmin) hmin = h_; if (h_ > hmax) hmax = h_; \
    } while (0)
    for (i = 0; i < 4; i++) QSAMPLE(xs[i], zs[i]);
    QSAMPLE(midx, 0); QSAMPLE(midx, mz); QSAMPLE(0, midz); QSAMPLE(mx, midz);
    for (t = 0; t <= maxd; t += 8) {
        int x = maxd ? (t * mx) / maxd : 0;
        int z = maxd ? (t * mz) / maxd : 0;
        QSAMPLE(x, z);
        QSAMPLE(x, mz - z);
    }
    QSAMPLE(mx, mz);
#undef QSAMPLE
    return eh_span_ok(emin, emax, hmin, hmax, cfg, me, mh);
}

static int quick_possible(const Climate *c, int qx, int qz, int win,
                          const ScanCfg *cfg)
{
    return quick_possible_rect(c, qx, qz, win, win, cfg);
}

/* Full candidate path: near-hit dedupe -> quick pattern -> optional perimeter
 * pre-check -> full no-shift window -> optional shift re-check.  A row is added
 * only if the chosen verifier passes.  Returns 0 only on hit-list failure. */
static int try_candidate(const Climate *c, const ScanCfg *cfg,
                         int winx, int winz,
                         int qx1, int qz1, ScanStats *st,
                         Hit **hits, int *nhits, int *cap,
                         int tx, int tz)
{
    WindowEval w, wp;
    int jj, near = 0;
    if (tx > qx1 - winx || tz > qz1 - winz) return 1;
    if (cfg->cluster > 0) {
        for (jj = 0; jj < *nhits; jj++) {
            if (abs(tx - (*hits)[jj].qx) * 4 <= cfg->cluster &&
                abs(tz - (*hits)[jj].qz) * 4 <= cfg->cluster) {
                near = 1;
                break;
            }
        }
    }
    if (near) return 1;
    if (cfg->quick && !quick_possible_rect(c, tx, tz, winx, winz, cfg)) {
        st->quick_rejects++;
        return 1;
    }
    if (cfg->perimeter || !cfg->full_verify) {
        window_eval_perimeter_rect(c, tx, tz, winx, winz,
                                   cfg->perimeter_step, 0, &wp);
        st->perimeter_checks++;
        if (!eh_span_ok(wp.e_min, wp.e_max, wp.h_min, wp.h_max, cfg,
                        cfg->pm_e, cfg->pm_h)) {
            st->perimeter_rejects++;
            return 1;
        }
    }
    if (!cfg->full_verify) {
        Hit hit;
        if (cfg->shift) {
            WindowEval ws;
            window_eval_perimeter_rect(c, tx, tz, winx, winz,
                                       cfg->perimeter_step, 1, &ws);
            st->perimeter_checks++;
            if (!eh_span_ok(ws.e_min, ws.e_max, ws.h_min, ws.h_max, cfg, 0, 0))
                return 1;
            wp = ws;
        }
        wp.pass = 1;
        hit.qx = tx; hit.qz = tz; hit.w = wp;
        return push_hit(hits, nhits, cap, hit, cfg->max_hits);
    }
    window_eval_rect(c, tx, tz, winx, winz, 0, &w);
    st->exact_checks++;
    if (!eh_span_ok(w.e_min, w.e_max, w.h_min, w.h_max, cfg, 0, 0))
        return 1;
    if (cfg->shift) {
        WindowEval ws;
        window_eval_rect(c, tx, tz, winx, winz, 1, &ws);
        st->exact_checks++;
        if (!eh_span_ok(ws.e_min, ws.e_max, ws.h_min, ws.h_max, cfg, 0, 0))
            return 1;
        w = ws;
    }
    w.pass = 1;
    {
        Hit hit;
        hit.qx = tx; hit.qz = tz; hit.w = w;
        return push_hit(hits, nhits, cap, hit, cfg->max_hits);
    }
}

static int screen_area_square(const Climate *c, int64_t bx0, int64_t bz0, int64_t bx1, int64_t bz1,
                const ScanCfg *cfg, int64_t step_l0, double m_e, double m_h,
                double bg, Hit **hits, int *nhits, ScanStats *st)
{
    int qx0, qx1, qz0, qz1, win, s0, l1, w1;
    int gnx, gny, i, j, nx, nz, onx, onz, ix, iz, ns;
    float *el = NULL, *hh = NULL, *l0minE = NULL, *l0maxE = NULL;
    float *l0minH = NULL, *l0maxH = NULL;
    float *ef = NULL, *hf = NULL;
    double t0, t1;
    int cap = 0;
    memset(st, 0, sizeof(*st));
    *hits = NULL; *nhits = 0;
    if (bx1 <= bx0 || bz1 <= bz0) return 0;
    qx0 = (int)floor_div4(bx0); qz0 = (int)floor_div4(bz0);
    qx1 = (int)floor_div4(bx1 - 1); qz1 = (int)floor_div4(bz1 - 1);
    win = cfg->window / 4;
    s0 = (int)(step_l0 / 4);
    if (s0 < 8) s0 = 8;
    l1 = cfg->l1_step;           /* quart; 8 = 32 blocks, 16 = 64 blocks */
    if (l1 < 4) l1 = 4;
    w1 = win / l1;
    ns = win / s0 + 2;           /* samples covering [bx, bx+win+s0] */
    if (win % l1 || w1 < 2 || ns < 2) return 0;
    {
        int span = (qx1 - qx0) + win + s0;
        gnx = span / s0 + 3;
        gny = ((qz1 - qz0) + win + s0) / s0 + 3;
    }
    t0 = now_sec();
    el = (float *)malloc((size_t)gnx * gny * sizeof(float));
    hh = (float *)malloc((size_t)gnx * gny * sizeof(float));
    if (!el || !hh) { free(el); free(hh); return 0; }
    progress_expect(gny);
#pragma omp parallel for schedule(static) if(!omp_in_parallel())
    for (j = 0; j < gny; j++) {
        int ii;
        for (ii = 0; ii < gnx; ii++) {
            double qx = (double)(qx0 + ii * s0);
            double qz = (double)(qz0 + j * s0);
            double e, h;
            climate_low_h(c, qx, qz, &e, &h);
            el[(size_t)j * gnx + ii] = (float)e;
            hh[(size_t)j * gnx + ii] = (float)h;
        }
        progress_tick();
    }
    t1 = now_sec();
    st->t_fill = t1 - t0;
    st->points += (int64_t)gnx * gny;

    for (j = 0; j + ns - 1 < gny; j++) {
        int bz = qz0 + j * s0;
        if (bz > qz1 - win) continue;
        for (i = 0; i + ns - 1 < gnx; i++) {
            int bx = qx0 + i * s0;
            double emin = 1e30, emax = -1e30, hmin = 1e30, hmax = -1e30;
            int a, b;
            if (bx > qx1 - win) continue;
            for (b = 0; b < ns; b++) {
                for (a = 0; a < ns; a++) {
                    double ev = el[(size_t)(j + b) * gnx + (i + a)];
                    double hv = hh[(size_t)(j + b) * gnx + (i + a)];
                    if (ev < emin) emin = ev;
                    if (ev > emax) emax = ev;
                    if (hv < hmin) hmin = hv;
                    if (hv > hmax) hmax = hv;
                }
            }
            if (!eh_span_ok(emin, emax, hmin, hmax, cfg, bg + m_e, m_h))
                continue;
            st->cand_l0++;
            {
                int best_ax = 0, best_az = 0, have_best = 0;
                double best_score = -1e30;
            /* L1a: 64-block full-field prefilter over the union region. */
                int l1a = 16, w1a = win / l1a;
                int nxa = (win + s0) / l1a + 1;
                int onxa = nxa - w1a + 1;
                float *fa = NULL, *ha = NULL, *mna = NULL, *mxa = NULL, *mnh = NULL, *mxh = NULL;
                int pass_ok = 0;
                if (w1a >= 1 && onxa >= 1) {
                    fa = (float *)malloc((size_t)nxa * nxa * sizeof(float));
                    ha = (float *)malloc((size_t)nxa * nxa * sizeof(float));
                    mna = (float *)malloc((size_t)onxa * onxa * sizeof(float));
                    mxa = (float *)malloc((size_t)onxa * onxa * sizeof(float));
                    mnh = (float *)malloc((size_t)onxa * onxa * sizeof(float));
                    mxh = (float *)malloc((size_t)onxa * onxa * sizeof(float));
                    if (fa && ha && mna && mxa && mnh && mxh) {
                        pass_ok = 1;
                        for (iz = 0; iz < nxa; iz++) {
                            for (ix = 0; ix < nxa; ix++) {
                                double e, h;
                                climate_sample(c, (double)(bx + ix * l1a),
                                               (double)(bz + iz * l1a), 0, &e, &h);
                                fa[(size_t)iz * nxa + ix] = (float)e;
                                ha[(size_t)iz * nxa + ix] = (float)h;
                            }
                        }
                        st->points += (int64_t)nxa * nxa;
                        slide2d(fa, nxa, nxa, w1a, mna, mxa);
                        slide2d(ha, nxa, nxa, w1a, mnh, mxh);
                        for (iz = 0; iz < onxa; iz++) {
                            for (ix = 0; ix < onxa; ix++) {
                                size_t k = (size_t)iz * onxa + ix;
                                int wx = bx + ix * l1a, wz = bz + iz * l1a;
                                double sc, v;
                                if (wx > qx1 - win || wz > qz1 - win) continue;
                                if (!eh_span_ok(mna[k], mxa[k], mnh[k], mxh[k],
                                                cfg, 0.08, 0.08))
                                    continue;
                                sc = mxa[k] - (cfg->e_hi - 0.08);
                                v = (cfg->h_lo + 0.08) - mnh[k]; if (v < sc) sc = v;
                                if (!cfg->no_pale) {
                                    v = (cfg->e_lo + 0.08) - mna[k]; if (v < sc) sc = v;
                                    v = mxh[k] - (cfg->h_hi - 0.08); if (v < sc) sc = v;
                                }
                                if (sc > best_score) {
                                    best_score = sc;
                                    best_ax = wx; best_az = wz;
                                    have_best = 1;
                                }
                            }
                        }
                    }
                }
                free(fa); free(ha); free(mna); free(mxa); free(mnh); free(mxh);
                if (!pass_ok || !have_best) continue;
                st->cand_l1a++;
                if (cfg->l1_boundary) {
                    WindowEval wb;
                    int pass_b = 0, ox, oz;
                    window_eval_perimeter_step(c, best_ax, best_az, win, 8, 0, &wb);
                    st->points += 4 * (win / 8 + 1);
                    if (eh_span_ok(wb.e_min, wb.e_max, wb.h_min, wb.h_max, cfg, 0.04, 0.04))
                        pass_b = 1;
                    if (pass_b) {
                        for (oz = 0; oz < 2; oz++) {
                            for (ox = 0; ox < 2; ox++) {
                                st->cand_l1++;
                                if (!try_candidate(c, cfg, win, win, qx1, qz1, st,
                                                   hits, nhits, &cap,
                                                   best_ax + ox * 4, best_az + oz * 4))
                                    goto done;
                            }
                        }
                    }
                    continue;
                }
            }
            /* L1b: full field on a 32-block grid over the union region. */
            nx = (win + s0) / l1 + 1;
            nz = nx;
            {
                int any_e = 0;
                ef = (float *)malloc((size_t)nx * nz * sizeof(float));
                if (!ef) continue;
                for (iz = 0; iz < nz; iz++) {
                    for (ix = 0; ix < nx; ix++) {
                        ef[(size_t)iz * nx + ix] =
                            (float)climate_e(c, (double)(bx + ix * l1),
                                             (double)(bz + iz * l1), 0);
                    }
                }
                st->points += (int64_t)nx * nz;
                onx = nx - w1 + 1; onz = nz - w1 + 1;
                l0minE = (float *)malloc((size_t)onx * onz * sizeof(float));
                l0maxE = (float *)malloc((size_t)onx * onz * sizeof(float));
                if (!l0minE || !l0maxE) {
                    free(ef); free(l0minE); free(l0maxE);
                    ef = NULL; l0minE = l0maxE = NULL;
                    continue;
                }
                slide2d(ef, nx, nz, w1, l0minE, l0maxE);
                for (iz = 0; iz < onz && !any_e; iz++) {
                    for (ix = 0; ix < onx; ix++) {
                        size_t k = (size_t)iz * onx + ix;
                        int wx = bx + ix * l1, wz = bz + iz * l1;
                        if (wx > qx1 - win || wz > qz1 - win) continue;
                        if ((cfg->no_pale || l0minE[k] < cfg->e_lo + 0.04) &&
                            l0maxE[k] > cfg->e_hi - 0.04) {
                            any_e = 1;
                            break;
                        }
                    }
                }
                if (cfg->e_first && !any_e) {
                    free(ef); free(l0minE); free(l0maxE);
                    ef = NULL; l0minE = l0maxE = NULL;
                    continue;
                }
                hf = (float *)malloc((size_t)nx * nz * sizeof(float));
                if (!hf) {
                    free(ef); free(l0minE); free(l0maxE);
                    ef = NULL; l0minE = l0maxE = NULL;
                    continue;
                }
                for (iz = 0; iz < nz; iz++) {
                    for (ix = 0; ix < nx; ix++) {
                        hf[(size_t)iz * nx + ix] =
                            (float)climate_h(c, (double)(bx + ix * l1),
                                             (double)(bz + iz * l1));
                    }
                }
                st->points += (int64_t)nx * nz;
                l0minH = (float *)malloc((size_t)onx * onz * sizeof(float));
                l0maxH = (float *)malloc((size_t)onx * onz * sizeof(float));
                if (!l0minH || !l0maxH) {
                    free(ef); free(hf); free(l0minE); free(l0maxE);
                    free(l0minH); free(l0maxH);
                    ef = hf = NULL; l0minE = l0maxE = l0minH = l0maxH = NULL;
                    continue;
                }
                slide2d(hf, nx, nz, w1, l0minH, l0maxH);
            }
            for (iz = 0; iz < onz; iz++) {
                for (ix = 0; ix < onx; ix++) {
                    size_t k = (size_t)iz * onx + ix;
                    int wx = bx + ix * l1;
                    int wz = bz + iz * l1;
                    int ox, oz, noff;
                    if (wx > qx1 - win || wz > qz1 - win) continue;
                    if (!eh_span_ok(l0minE[k], l0maxE[k], l0minH[k], l0maxH[k],
                                    cfg, 0.04, 0.04))
                        continue;
                    st->cand_l1++;
                    noff = cfg->offsets;
                    if (noff < 1) noff = 1;
                    if (noff > 2) noff = 2;
                    for (oz = 0; oz < noff; oz++) {
                        for (ox = 0; ox < noff; ox++) {
                            int tx = wx + ox * 4, tz = wz + oz * 4;
                            if (!try_candidate(c, cfg, win, win, qx1, qz1, st,
                                               hits, nhits, &cap, tx, tz))
                                goto done;
                        }
                    }
                }
            }
            free(ef); free(hf);
            free(l0minE); free(l0maxE); free(l0minH); free(l0maxH);
            ef = hf = NULL;
            l0minE = l0maxE = l0minH = l0maxH = NULL;
        }
    }
done:
    free(el); free(hh);
    free(ef); free(hf);
    free(l0minE); free(l0maxE); free(l0minH); free(l0maxH);
    if (cfg->cluster > 0) cluster_hits(hits, nhits, cfg);
    st->t_total = now_sec() - t0;
    st->hits = *nhits;
    return 1;
}
/* Rectangular mode: direct 320/640-style window sweep.  The square path keeps
 * its multigrid L0/L1 cascade; a 640x320 window is wide enough that the direct
 * perimeter prefilter is simpler and avoids assuming a square union. */
static int screen_area_rect(const Climate *c, int64_t bx0, int64_t bz0,
                            int64_t bx1, int64_t bz1, const ScanCfg *cfg,
                            int64_t step_l0, double m_e, double m_h, double bg,
                            Hit **hits, int *nhits, ScanStats *st)
{
    int qx0, qx1, qz0, qz1, winx, winz, sx, sz, bx, bz, cap = 0;
    double t0 = now_sec();
    (void)bg;
    memset(st, 0, sizeof(*st));
    *hits = NULL; *nhits = 0;
    if (bx1 <= bx0 || bz1 <= bz0) return 0;
    qx0 = (int)floor_div4(bx0); qz0 = (int)floor_div4(bz0);
    qx1 = (int)floor_div4(bx1 - 1); qz1 = (int)floor_div4(bz1 - 1);
    winx = cfg->window / 4;
    winz = cfg->window_z / 4;
    if (winx <= 0 || winz <= 0 || qx1 - qx0 + 1 < winx || qz1 - qz0 + 1 < winz)
        return 0;
    sx = (int)(step_l0 / 4);
    if (sx < 4) sx = 4;
    for (bz = qz0; bz + winz - 1 <= qz1; bz += sx) {
        for (bx = qx0; bx + winx - 1 <= qx1; bx += sx) {
            WindowEval wp;
            st->windows++;
            /* coarse E/H boundary prefilter at 16-block spacing */
            window_eval_perimeter_rect(c, bx, bz, winx, winz, 4, 0, &wp);
            st->points += (int64_t)2 * (winx / 4 + winz / 4 + 2);
            if (!eh_span_ok(wp.e_min, wp.e_max, wp.h_min, wp.h_max, cfg, m_e, m_h))
                continue;
            st->cand_l1++;
            if (!try_candidate(c, cfg, winx, winz, qx1, qz1, st, hits, nhits,
                               &cap, bx, bz))
                return 0;
        }
    }
    if (cfg->cluster > 0) cluster_hits(hits, nhits, cfg);
    st->hits = *nhits;
    st->t_total = now_sec() - t0;
    return 1;
}

static int eval_rect_orientation(const Climate *c, const ScanCfg *cfg,
                                  float *ef, float *hf, int gw, int gh,
                                  int qx0, int qz0, int cg,
                                  int winx, int winz, int stepq,
                                  int qx1, int qz1, double m_e, double m_h,
                                  Hit **hits, int *nhits, int *cap,
                                  ScanStats *st)
{
    int cw = winx / cg, ch = winz / cg;
    int ow = gw - cw + 1, oh = gh - ch + 1;
    float *mne = NULL, *mxe = NULL, *mnh = NULL, *mxh = NULL;
    int ix, iz, bx, bz;
    if (cw <= 0 || ch <= 0 || ow <= 0 || oh <= 0) return 1;
    mne = (float *)malloc((size_t)ow * oh * sizeof(float));
    mxe = (float *)malloc((size_t)ow * oh * sizeof(float));
    mnh = (float *)malloc((size_t)ow * oh * sizeof(float));
    mxh = (float *)malloc((size_t)ow * oh * sizeof(float));
    if (!mne || !mxe || !mnh || !mxh) {
        free(mne); free(mxe); free(mnh); free(mxh);
        return 0;
    }
    slide2d_xy(ef, gw, gh, cw, ch, mne, mxe);
    slide2d_xy(hf, gw, gh, cw, ch, mnh, mxh);
    for (bz = qz0; bz + winz - 1 <= qz1; bz += stepq) {
        iz = (bz - qz0) / cg;
        if (iz >= oh) break;
        for (bx = qx0; bx + winx - 1 <= qx1; bx += stepq) {
            size_t k;
            ix = (bx - qx0) / cg;
            if (ix >= ow) break;
            k = (size_t)iz * ow + ix;
            st->windows++;
            if (!eh_span_ok(mne[k], mxe[k], mnh[k], mxh[k], cfg,
                            m_e + 0.02, m_h + 0.02))
                continue;
            st->cand_l1++;
            if (!try_candidate(c, cfg, winx, winz, qx1, qz1, st, hits, nhits,
                               cap, bx, bz)) {
                free(mne); free(mxe); free(mnh); free(mxh);
                return 0;
            }
        }
    }
    free(mne); free(mxe); free(mnh); free(mxh);
    return 1;
}

int screen_area_rect_both(const Climate *c, int64_t bx0, int64_t bz0,
                          int64_t bx1, int64_t bz1, const ScanCfg *cfg,
                          int64_t step_l0, double m_e, double m_h,
                          Hit **hits_x, int *nhits_x,
                          Hit **hits_z, int *nhits_z, ScanStats *st)
{
    int qx0, qx1, qz0, qz1, winx, winz, gw, gh, ix, iz, cg = 16;
    float *ef = NULL, *hf = NULL;
    int capx = 0, capz = 0, stepq;
    double t0 = now_sec();
    memset(st, 0, sizeof(*st));
    *hits_x = NULL; *nhits_x = 0;
    *hits_z = NULL; *nhits_z = 0;
    if (bx1 <= bx0 || bz1 <= bz0) return 0;
    qx0 = (int)floor_div4(bx0); qz0 = (int)floor_div4(bz0);
    qx1 = (int)floor_div4(bx1 - 1); qz1 = (int)floor_div4(bz1 - 1);
    winx = cfg->window / 4;
    winz = cfg->window_z / 4;
    if (winx <= 0 || winz <= 0 || winx == winz) return 0;
    if (winx % 16 || winz % 16) cg = 8;
    if (winx % cg || winz % cg) cg = 4;
    if (winx % cg || winz % cg) cg = 2;
    if (winx % cg || winz % cg) return 0;
    gw = (qx1 - qx0) / cg + 3;
    gh = (qz1 - qz0) / cg + 3;
    ef = (float *)malloc((size_t)gw * gh * sizeof(float));
    hf = (float *)malloc((size_t)gw * gh * sizeof(float));
    if (!ef || !hf) { free(ef); free(hf); return 0; }
    progress_expect(gh);
#pragma omp parallel for schedule(static) if(!omp_in_parallel())
    for (iz = 0; iz < gh; iz++) {
        int xx;
        for (xx = 0; xx < gw; xx++) {
            double e, h;
            climate_sample(c, (double)(qx0 + xx * cg),
                           (double)(qz0 + iz * cg), 0, &e, &h);
            ef[(size_t)iz * gw + xx] = (float)e;
            hf[(size_t)iz * gw + xx] = (float)h;
        }
        progress_tick();
    }
    st->points += (int64_t)gw * gh;
    stepq = (int)(step_l0 / 4 / cg) * cg;
    if (stepq < cg) stepq = cg;
    if (!eval_rect_orientation(c, cfg, ef, hf, gw, gh, qx0, qz0, cg,
                               winx, winz, stepq, qx1, qz1, m_e, m_h,
                               hits_x, nhits_x, &capx, st) ||
        !eval_rect_orientation(c, cfg, ef, hf, gw, gh, qx0, qz0, cg,
                               winz, winx, stepq, qx1, qz1, m_e, m_h,
                               hits_z, nhits_z, &capz, st)) {
        free(ef); free(hf);
        return 0;
    }
    free(ef); free(hf);
    if (cfg->cluster > 0) {
        cluster_hits(hits_x, nhits_x, cfg);
        cluster_hits(hits_z, nhits_z, cfg);
    }
    st->hits = *nhits_x + *nhits_z;
    st->t_total = now_sec() - t0;
    return 1;
}

static int64_t align_tile_start(int64_t block, int64_t origin, int64_t step_blocks)
{
    int64_t q = floor_div4(block), o = floor_div4(origin);
    int64_t s = step_blocks / 4;
    if (s < 1) s = 1;
    if (q < o) q = o;
    return (o + ((q - o) / s) * s) * 4;
}

typedef struct {
    Hit *hx; int nx;
    Hit *hz; int nz;
    ScanStats st;
} BothTile;

static int rect_rows(int64_t bx0, int64_t bz0, int64_t bx1, int64_t bz1, const ScanCfg *cfg)
{
    int qx0, qx1, qz0, qz1, winx, winz, cg = 16, gh;
    if (bx1 <= bx0 || bz1 <= bz0) return 0;
    qx0 = (int)floor_div4(bx0); qz0 = (int)floor_div4(bz0);
    qx1 = (int)floor_div4(bx1 - 1); qz1 = (int)floor_div4(bz1 - 1);
    winx = cfg->window / 4;
    winz = cfg->window_z / 4;
    if (winx <= 0 || winz <= 0 || winx == winz) return 0;
    if (winx % 16 || winz % 16) cg = 8;
    if (winx % cg || winz % cg) cg = 4;
    if (winx % cg || winz % cg) cg = 2;
    if (winx % cg || winz % cg) return 0;
    gh = (qz1 - qz0) / cg + 3;
    return gh > 0 ? gh : 0;
}

static int square_rows(int64_t bx0, int64_t bz0, int64_t bx1, int64_t bz1,
                       const ScanCfg *cfg, int64_t step_l0)
{
    int qx0, qx1, qz0, qz1, win, s0, l1, w1, ns, gny, span;
    if (bx1 <= bx0 || bz1 <= bz0) return 0;
    qx0 = (int)floor_div4(bx0); qz0 = (int)floor_div4(bz0);
    qx1 = (int)floor_div4(bx1 - 1); qz1 = (int)floor_div4(bz1 - 1);
    win = cfg->window / 4;
    s0 = (int)(step_l0 / 4);
    if (s0 < 8) s0 = 8;
    l1 = cfg->l1_step;
    if (l1 < 4) l1 = 4;
    w1 = win / l1;
    ns = win / s0 + 2;
    if (win % l1 || w1 < 2 || ns < 2) return 0;
    span = (qz1 - qz0) + win + s0;
    gny = span / s0 + 3;
    return gny > 0 ? gny : 0;
}

static void preset_tile_rows(int ntx, int ntz, int64_t bx0, int64_t bz0, int64_t bx1, int64_t bz1,
                             int64_t tw, int64_t th, int64_t overlap, int64_t step_l0,
                             const ScanCfg *cfg, int square)
{
    int t, sum = 0;
    for (t = 0; t < ntx * ntz; t++) {
        int64_t xa, xb, za, zb;
        int xx = t % ntx, zz = t / ntx;
        xa = bx0 + (int64_t)xx * tw;
        za = bz0 + (int64_t)zz * th;
        xb = xa + tw; if (xb > bx1) xb = bx1;
        zb = za + th; if (zb > bz1) zb = bz1;
        if (xa >= xb || za >= zb) continue;
        xa -= overlap; if (xa < bx0) xa = bx0;
        za -= overlap; if (za < bz0) za = bz0;
        xb += overlap; if (xb > bx1) xb = bx1;
        zb += overlap; if (zb > bz1) zb = bz1;
        xa = align_tile_start(xa, bx0, step_l0);
        za = align_tile_start(za, bz0, step_l0);
        sum += square ? square_rows(xa, za, xb, zb, cfg, step_l0)
                      : rect_rows(xa, za, xb, zb, cfg);
    }
    if (sum > 0) {
        progress_set_total(sum);
        prog_preset = 1;
    }
}

int screen_area_rect_both_parallel(const Climate *c, int64_t bx0, int64_t bz0,
                                   int64_t bx1, int64_t bz1, const ScanCfg *cfg,
                                   int64_t step_l0, double m_e, double m_h,
                                   int threads,
                                   Hit **hits_x, int *nhits_x,
                                   Hit **hits_z, int *nhits_z, ScanStats *st)
{
    int nt = threads > 0 ? threads : 1;
    int ntx, ntz, tx, tz, t, i;
    int64_t tw, th, overlap;
    BothTile *tr;
    size_t totalx = 0, totalz = 0;
    Hit *ax = NULL, *az = NULL;
    double t0 = now_sec();
#ifdef _OPENMP
    if (threads <= 0) nt = omp_get_max_threads();
#endif
    if (nt < 1) nt = 1;
    if (nt > 32) nt = 32;
    if (bx1 - bx0 < 2 * (int64_t)(cfg->window > cfg->window_z ? cfg->window : cfg->window_z) ||
        bz1 - bz0 < 2 * (int64_t)(cfg->window > cfg->window_z ? cfg->window : cfg->window_z))
        nt = 1;
    if (nt == 1)
        return screen_area_rect_both(c, bx0, bz0, bx1, bz1, cfg, step_l0,
                                     m_e, m_h, hits_x, nhits_x,
                                     hits_z, nhits_z, st);
    overlap = (cfg->window > cfg->window_z ? cfg->window : cfg->window_z) + step_l0;
    ntx = (int)ceil(sqrt((double)nt));
    ntz = (nt + ntx - 1) / ntx;
    tw = (bx1 - bx0 + ntx - 1) / ntx;
    th = (bz1 - bz0 + ntz - 1) / ntz;
    tr = (BothTile *)calloc((size_t)ntx * ntz, sizeof(*tr));
    if (!tr)
        return screen_area_rect_both(c, bx0, bz0, bx1, bz1, cfg, step_l0,
                                     m_e, m_h, hits_x, nhits_x,
                                     hits_z, nhits_z, st);
    preset_tile_rows(ntx, ntz, bx0, bz0, bx1, bz1, tw, th, overlap, step_l0, cfg, 0);
#pragma omp parallel for schedule(dynamic,1)
    for (t = 0; t < ntx * ntz; t++) {
        int64_t xa, xb, za, zb;
        int xx = t % ntx, zz = t / ntx;
        xa = bx0 + (int64_t)xx * tw;
        za = bz0 + (int64_t)zz * th;
        xb = xa + tw; if (xb > bx1) xb = bx1;
        zb = za + th; if (zb > bz1) zb = bz1;
        if (xa >= xb || za >= zb) continue;
        xa -= overlap; if (xa < bx0) xa = bx0;
        za -= overlap; if (za < bz0) za = bz0;
        xb += overlap; if (xb > bx1) xb = bx1;
        zb += overlap; if (zb > bz1) zb = bz1;
        xa = align_tile_start(xa, bx0, step_l0);
        za = align_tile_start(za, bz0, step_l0);
        screen_area_rect_both(c, xa, za, xb, zb, cfg, step_l0, m_e, m_h,
                              &tr[t].hx, &tr[t].nx,
                              &tr[t].hz, &tr[t].nz, &tr[t].st);
    }
    prog_preset = 0;
    /* Mark phase-1 work complete before the merge so the UI is not stuck at <100%. */
    if (prog_cur && prog_tot) {
        int tot = __atomic_load_n(prog_tot, __ATOMIC_RELAXED);
        if (tot > 0)
            __atomic_store_n(prog_cur, tot, __ATOMIC_RELAXED);
    }
    for (t = 0; t < ntx * ntz; t++) {
        if (cfg->cluster > 0) {
            cluster_hits(&tr[t].hx, &tr[t].nx, cfg);
            cluster_hits(&tr[t].hz, &tr[t].nz, cfg);
        }
        totalx += tr[t].nx;
        totalz += tr[t].nz;
    }
    ax = (Hit *)malloc((totalx ? totalx : 1) * sizeof(Hit));
    az = (Hit *)malloc((totalz ? totalz : 1) * sizeof(Hit));
    if (!ax || !az) {
        free(ax); free(az);
        for (t = 0; t < ntx * ntz; t++) { free(tr[t].hx); free(tr[t].hz); }
        free(tr);
        return screen_area_rect_both(c, bx0, bz0, bx1, bz1, cfg, step_l0,
                                     m_e, m_h, hits_x, nhits_x,
                                     hits_z, nhits_z, st);
    }
    totalx = totalz = 0;
    memset(st, 0, sizeof(*st));
    for (t = 0; t < ntx * ntz; t++) {
        if (tr[t].nx) {
            memcpy(ax + totalx, tr[t].hx, (size_t)tr[t].nx * sizeof(Hit));
            totalx += tr[t].nx;
        }
        if (tr[t].nz) {
            memcpy(az + totalz, tr[t].hz, (size_t)tr[t].nz * sizeof(Hit));
            totalz += tr[t].nz;
        }
        st->points += tr[t].st.points;
        st->windows += tr[t].st.windows;
        st->cand_l0 += tr[t].st.cand_l0;
        st->cand_l1a += tr[t].st.cand_l1a;
        st->cand_l1 += tr[t].st.cand_l1;
        st->exact_checks += tr[t].st.exact_checks;
        st->quick_rejects += tr[t].st.quick_rejects;
        st->perimeter_checks += tr[t].st.perimeter_checks;
        st->perimeter_rejects += tr[t].st.perimeter_rejects;
        st->t_fill += tr[t].st.t_fill;
        st->t_slide += tr[t].st.t_slide;
        st->t_l1 += tr[t].st.t_l1;
        st->t_exact += tr[t].st.t_exact;
        st->t_total += tr[t].st.t_total;
        free(tr[t].hx); free(tr[t].hz);
    }
    free(tr);
    *hits_x = ax; *nhits_x = (int)totalx;
    *hits_z = az; *nhits_z = (int)totalz;
    if (cfg->cluster > 0) {
        cluster_hits(hits_x, nhits_x, cfg);
        cluster_hits(hits_z, nhits_z, cfg);
    }
    st->hits = *nhits_x + *nhits_z;
    st->t_total = now_sec() - t0;
    return 1;
}

int screen_area(const Climate *c, int64_t bx0, int64_t bz0,
                int64_t bx1, int64_t bz1, const ScanCfg *cfg,
                int64_t step_l0, double m_e, double m_h, double bg,
                Hit **hits, int *nhits, ScanStats *st)
{
    if (cfg->window == cfg->window_z)
        return screen_area_square(c, bx0, bz0, bx1, bz1, cfg, step_l0,
                                  m_e, m_h, bg, hits, nhits, st);
    return screen_area_rect(c, bx0, bz0, bx1, bz1, cfg, step_l0,
                            m_e, m_h, bg, hits, nhits, st);
}

typedef struct {
    Hit *hits;
    int nhits;
    ScanStats st;
} TileOut;

int screen_area_parallel(const Climate *c, int64_t bx0, int64_t bz0,
                         int64_t bx1, int64_t bz1, const ScanCfg *cfg,
                         int64_t step_l0, double m_e, double m_h, double bg,
                         int threads, Hit **hits, int *nhits, ScanStats *st)
{
    int nt = threads > 0 ? threads : 1;
    int ntx, ntz, tx, tz, t;
    int64_t tw, th, overlap;
    TileOut *tr;
    size_t total = 0;
    Hit *all;
    int i;
#ifdef _OPENMP
    if (threads <= 0) nt = omp_get_max_threads();
#endif
    if (nt < 1) nt = 1;
    if (nt > 32) nt = 32;
    {
        int64_t maxwin = cfg->window > cfg->window_z ? cfg->window : cfg->window_z;
        if (bx1 - bx0 < maxwin || bz1 - bz0 < maxwin)
            nt = 1;
    }
    if (nt == 1)
        return screen_area(c, bx0, bz0, bx1, bz1, cfg, step_l0, m_e, m_h,
                           bg, hits, nhits, st);

    ntx = (int)ceil(sqrt((double)nt));
    ntz = (nt + ntx - 1) / ntx;
    tw = (bx1 - bx0 + ntx - 1) / ntx;
    th = (bz1 - bz0 + ntz - 1) / ntz;
    overlap = (cfg->window > cfg->window_z ? cfg->window : cfg->window_z) + step_l0;
    tr = (TileOut *)calloc((size_t)ntx * ntz, sizeof(*tr));
    if (!tr)
        return screen_area(c, bx0, bz0, bx1, bz1, cfg, step_l0, m_e, m_h,
                           bg, hits, nhits, st);
    preset_tile_rows(ntx, ntz, bx0, bz0, bx1, bz1, tw, th, overlap, step_l0, cfg, 1);

#pragma omp parallel for schedule(dynamic,1)
    for (t = 0; t < ntx * ntz; t++) {
        int64_t xa, xb, za, zb;
        int xx = t % ntx, zz = t / ntx;
        xa = bx0 + (int64_t)xx * tw;
        za = bz0 + (int64_t)zz * th;
        xb = xa + tw; if (xb > bx1) xb = bx1;
        zb = za + th; if (zb > bz1) zb = bz1;
        if (xa >= xb || za >= zb) continue;
        xa -= overlap; if (xa < bx0) xa = bx0;
        za -= overlap; if (za < bz0) za = bz0;
        xb += overlap; if (xb > bx1) xb = bx1;
        zb += overlap; if (zb > bz1) zb = bz1;
        xa = align_tile_start(xa, bx0, step_l0);
        za = align_tile_start(za, bz0, step_l0);
        screen_area(c, xa, za, xb, zb, cfg, step_l0, m_e, m_h, bg,
                    &tr[t].hits, &tr[t].nhits, &tr[t].st);
    }
    prog_preset = 0;
    if (prog_cur && prog_tot) {
        int tot = __atomic_load_n(prog_tot, __ATOMIC_RELAXED);
        if (tot > 0)
            __atomic_store_n(prog_cur, tot, __ATOMIC_RELAXED);
    }

    for (t = 0; t < ntx * ntz; t++) {
        if (cfg->cluster > 0)
            cluster_hits(&tr[t].hits, &tr[t].nhits, cfg);
        total += tr[t].nhits;
    }
    all = (Hit *)malloc((total ? total : 1) * sizeof(Hit));
    if (!all) {
        for (t = 0; t < ntx * ntz; t++) free(tr[t].hits);
        free(tr);
        return screen_area(c, bx0, bz0, bx1, bz1, cfg, step_l0, m_e, m_h,
                           bg, hits, nhits, st);
    }
    total = 0;
    memset(st, 0, sizeof(*st));
    for (t = 0; t < ntx * ntz; t++) {
        if (tr[t].nhits > 0) {
            memcpy(all + total, tr[t].hits, (size_t)tr[t].nhits * sizeof(Hit));
            total += tr[t].nhits;
        }
        st->points += tr[t].st.points;
        st->windows += tr[t].st.windows;
        st->cand_l0 += tr[t].st.cand_l0;
        st->cand_l1a += tr[t].st.cand_l1a;
        st->cand_l1 += tr[t].st.cand_l1;
        st->exact_checks += tr[t].st.exact_checks;
        st->quick_rejects += tr[t].st.quick_rejects;
        st->perimeter_checks += tr[t].st.perimeter_checks;
        st->perimeter_rejects += tr[t].st.perimeter_rejects;
        st->t_fill += tr[t].st.t_fill;
        st->t_slide += tr[t].st.t_slide;
        st->t_l1 += tr[t].st.t_l1;
        st->t_exact += tr[t].st.t_exact;
        st->t_total += tr[t].st.t_total;
        free(tr[t].hits);
    }
    free(tr);
    *hits = all;
    *nhits = (int)total;
    if (cfg->cluster > 0) cluster_hits(hits, nhits, cfg);
    st->hits = *nhits;
    return 1;
}


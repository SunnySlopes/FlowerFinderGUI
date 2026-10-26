#include "FlowerFarmSearch.h"

#include "core.h"
#include "cubiomes/biomes.h"
#include "cubiomes/noise.h"
#include "cubiomes/util.h"

#include <algorithm>
#include <chrono>
#include <climits>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <string>
#include <thread>
#include <vector>

namespace {

/* CTS biomes defaults from cts 1.0.0 cmd_biomes. */
constexpr int64_t kStep = 256;
constexpr double kMarginE = 0.08;
constexpr double kMarginH = 0.12;
constexpr double kBg = 0.09;
constexpr int kBiomeY = 256;
constexpr int kNetherY = 64;
constexpr int kPaleDist = 64;
constexpr int kFlowerAreaMin = 2000;

int stopRequested(void *user)
{
    auto *c = static_cast<FlowerSearchControl *>(user);
    return (c && c->stopReq && c->stopReq->load()) ? 1 : 0;
}

/* Blocks while paused; returns nonzero when the caller must abort. */
int waitForPause(void *user)
{
    auto *c = static_cast<FlowerSearchControl *>(user);
    if (!c)
        return 0;
    while (c->pauseReq && c->pauseReq->load())
    {
        if (c->stopReq && c->stopReq->load())
            return 1;
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }
    return (c->stopReq && c->stopReq->load()) ? 1 : 0;
}

bool pauseOk(FlowerSearchControl &c)
{
    return waitForPause(&c) == 0;
}

/* Binds the C engine to the C++ control object for the lifetime of a search. */
struct CtsBinding {
    explicit CtsBinding(FlowerSearchControl &c) : control(c)
    {
        cts_bind_control(&stopRequested, &waitForPause, &control);
    }
    ~CtsBinding()
    {
        cts_bind_control(nullptr, nullptr, nullptr);
        cts_bind_progress(nullptr, nullptr);
    }
    FlowerSearchControl &control;
};

void storeInt(std::atomic_int *p, int value)
{
    if (p)
        p->store(value);
}

int loadInt(std::atomic_int *p)
{
    return p ? p->load() : 0;
}

void setPhase(FlowerSearchControl &c, int phase, int total)
{
    storeInt(c.phase, phase);
    storeInt(c.total, total);
    storeInt(c.current, 0);
    if (c.phaseTotal && c.phaseTotal != c.total)
        storeInt(c.phaseTotal, total);
    if (c.phaseCurrent && c.phaseCurrent != c.current)
        storeInt(c.phaseCurrent, 0);
}

void setPhaseCurrent(FlowerSearchControl &c, int current)
{
    storeInt(c.current, current);
    if (c.phaseCurrent && c.phaseCurrent != c.current)
        storeInt(c.phaseCurrent, current);
}

void setOverall(FlowerSearchControl &c, int current, int total)
{
    storeInt(c.overallCurrent, current);
    storeInt(c.overallTotal, total);
}

int alignDown(int value, int multiple, int minimum)
{
    if (value < minimum)
        return 0;
    value -= value % multiple;
    return value >= minimum ? value : 0;
}

ScanCfg makeCfg(int windowX, int windowZ, int fullVerify, int noPale, int cluster,
                int threads)
{
    ScanCfg cfg{};
    cfg.e_lo = 0.05;
    cfg.e_hi = 0.55;
    cfg.h_lo = -0.35;
    cfg.h_hi = 0.30;
    cfg.window = windowX;
    cfg.window_z = windowZ;
    cfg.shift = 1;
    cfg.max_hits = 50000;
    /* Climate-stage cluster keeps phase-1 hit lists small (overlap is extreme without it).
     * Verified rows are clustered again with cherry / origin preference. */
    cfg.cluster = cluster;
    cfg.offsets = 2;
    cfg.quick = 1;
    cfg.qm_e = 0.02;
    cfg.qm_h = 0.0;
    cfg.l1_step = 8;
    cfg.l1_boundary = 0;
    cfg.e_first = 1;
    cfg.perimeter = 0;
    cfg.perimeter_step = 2;
    cfg.pm_e = 0.02;
    cfg.pm_h = 0.02;
    cfg.full_verify = fullVerify ? 1 : 0;
    cfg.no_pale = noPale ? 1 : 0;
    cfg.threads = threads > 0 ? threads : 1;
    const unsigned hw = std::thread::hardware_concurrency();
    cfg.max_threads = hw > 0 ? (int) hw : 1;
    return cfg;
}

int64_t floorDiv8(int64_t v)
{
    return v >= 0 ? v / 8 : -((-v + 7) / 8);
}

/* Internal row: public fields plus search-only ranking keys. */
struct OutRow : FlowerRow {
    int score = 0;
    int hasPinkPetals = 0;
    int order = 0;
    int qx = 0;
    int qz = 0;
};

/* GUI: 1.18~1.21.3 → MC_1_21_3; 1.21.4+ → newest. */
struct VersionRules {
    bool requirePale = true;
    bool allowBirchMeadow = true;
    bool allowCherry = true;
    bool reportPaleArea = true;
    bool pinkPetalsVersionNote = false;
    /* Chebyshev dedupe window (blocks). Legacy ranges need a wider merge. */
    int clusterBlocks = 80;
};

VersionRules rulesForMc(int mc)
{
    VersionRules r;
    if (mc < MC_1_21_4)
    {
        r.requirePale = false;
        r.allowBirchMeadow = false;
        r.reportPaleArea = false;
        r.allowCherry = true;
        r.pinkPetalsVersionNote = true;
        r.clusterBlocks = 160;
    }
    return r;
}

/** Aggregate bonus labels: birch/meadow → wildflowers, cherry → pink_petals. */
std::string largestWoodBonus(int birch, int oldBirch, int meadow, int cherry,
                             const VersionRules &rules)
{
    std::string bonus;
    if (rules.allowBirchMeadow && (birch > 0 || oldBirch > 0 || meadow > 0))
        bonus = "wildflowers";
    if (rules.allowCherry && cherry > 0)
    {
        if (!bonus.empty())
            bonus += '+';
        bonus += rules.pinkPetalsVersionNote ? "pink_petals(1.19.4+)" : "pink_petals";
    }
    return bonus;
}

/** Best nether biome under the overworld window (rank: basalt → … → warped). */
std::string netherBiomesInWindow(const Generator &nether, int wx, int wz,
                                 int winxBlocks, int winzBlocks, int mc)
{
    static const int kRank[] = {
        basalt_deltas, nether_wastes, crimson_forest, soul_sand_valley, warped_forest
    };
    bool seen[256] = {};
    const int nMinX = (int) floorDiv8(wx);
    const int nMaxX = (int) floorDiv8((int64_t) wx + winxBlocks - 1);
    const int nMinZ = (int) floorDiv8(wz);
    const int nMaxZ = (int) floorDiv8((int64_t) wz + winzBlocks - 1);
    const int spanX = nMaxX - nMinX + 1;
    const int spanZ = nMaxZ - nMinZ + 1;
    /* Dense enough for ranking; stride grows for large windows. */
    const int step = (spanX > 64 || spanZ > 64) ? 2 : 1;
    for (int nz = nMinZ; nz <= nMaxZ; nz += step)
    {
        for (int nx = nMinX; nx <= nMaxX; nx += step)
        {
            const int id = biome_at_cached(&nether, nx, kNetherY, nz);
            if (id >= 0 && id < 256)
                seen[id] = true;
        }
    }
    /* Corners in case stride skipped them. */
    const int corners[4][2] = {
        {nMinX, nMinZ}, {nMaxX, nMinZ}, {nMinX, nMaxZ}, {nMaxX, nMaxZ}
    };
    for (const auto &c : corners)
    {
        const int id = biome_at_cached(&nether, c[0], kNetherY, c[1]);
        if (id >= 0 && id < 256)
            seen[id] = true;
    }
    for (int id : kRank)
    {
        if (!seen[id])
            continue;
        const char *name = biome2str(mc, id);
        if (name)
            return name;
    }
    return "unknown";
}

int64_t originDist2(const OutRow &r)
{
    return (int64_t) r.cx * r.cx + (int64_t) r.cz * r.cz;
}

/** Prefer cherry grove; else closer to origin. Chebyshev cluster in blocks. */
bool clusterVerifiedRows(std::vector<OutRow> &rows, int clusterBlocks,
                         FlowerSearchControl &control)
{
    if (rows.size() <= 1 || clusterBlocks <= 0)
        return true;
    std::vector<OutRow> kept;
    kept.reserve(rows.size());
    for (size_t n = 0; n < rows.size(); n++)
    {
        if ((n & 63) == 0 && !pauseOk(control))
            return false;
        const OutRow &h = rows[n];
        int found = -1;
        for (size_t j = 0; j < kept.size(); j++)
        {
            if (std::abs(h.qx - kept[j].qx) * 4 <= clusterBlocks &&
                std::abs(h.qz - kept[j].qz) * 4 <= clusterBlocks)
            {
                found = (int) j;
                break;
            }
        }
        if (found < 0)
        {
            kept.push_back(h);
            continue;
        }
        const OutRow &cur = kept[(size_t) found];
        bool replace = false;
        if (h.hasPinkPetals != cur.hasPinkPetals)
            replace = h.hasPinkPetals > cur.hasPinkPetals;
        else if (originDist2(h) < originDist2(cur))
            replace = true;
        if (replace)
            kept[(size_t) found] = h;
    }
    rows.swap(kept);
    return true;
}

std::string formatRow(const FlowerRow &h, const VersionRules &rules)
{
    std::string line = "/tp " + std::to_string(h.cx) + " 64 " + std::to_string(h.cz) + " side=";
    if (h.sideX == h.sideZ)
        line += std::to_string(h.sideX);
    else
        line += std::to_string(h.sideX) + "x" + std::to_string(h.sideZ);
    line += " flowerArea=" + std::to_string(h.flowerArea);
    if (rules.reportPaleArea)
        line += " paleArea=" + std::to_string(h.paleArea);
    line += " swampArea=" + std::to_string(h.swampArea);
    line += " nether=" + h.nether;
    if (!h.bonus.empty())
        line += " bonus=" + h.bonus;
    return line;
}

struct FlowerNoise {
    DoublePerlinNoise dpn{};
    PerlinNoise oct[4]{};
};

void initFlowerNoise(FlowerNoise &fn)
{
    Xoroshiro xr;
    xSetSeed(&xr, 2345);
    const double amp[1] = {1.0};
    xDoublePerlinInit(&fn.dpn, &xr, fn.oct, amp, 0, 1, -1);
}

int flowerId(const FlowerNoise &fn, int x, int y, int z)
{
    const double scale = 0.020833334;
    const double value = sampleDoublePerlin(&fn.dpn, x * scale, y * scale, z * scale);
    double t = (1.0 + value) / 2.0;
    if (t < 0.0)
        t = 0.0;
    else if (t > 0.9999)
        t = 0.9999;
    return (int) (t * 11.0);
}

/** Both gradient ends (dandelion = 0, lily of the valley = 10) within 32 blocks vertically. */
bool flowerExtremes(const FlowerNoise &fn, const std::vector<int> &xs, const std::vector<int> &zs, int &outY)
{
    if (xs.empty())
        return false;
    std::vector<int> dandelion;
    std::vector<int> lily;
    for (int y = 80; y <= 319; y += 2)
    {
        bool hasDandelion = false;
        bool hasLily = false;
        for (size_t i = 0; i < xs.size(); i++)
        {
            const int id = flowerId(fn, xs[i], y, zs[i]);
            if (id == 0)
                hasDandelion = true;
            else if (id == 10)
                hasLily = true;
            if (hasDandelion && hasLily)
                break;
        }
        if (hasDandelion)
            dandelion.push_back(y);
        if (hasLily)
            lily.push_back(y);
    }
    int best = 1 << 30;
    int bestY = 0;
    for (int dy : dandelion)
    {
        for (int ly : lily)
        {
            const int span = std::abs(dy - ly);
            if (span <= 32 && span < best)
            {
                best = span;
                bestY = (int) std::lround((dy + ly) / 2.0);
            }
        }
    }
    if (best > 32)
        return false;
    outY = bestY;
    return true;
}

bool considerHit(const Climate &climate, const Generator &overworld, const Generator &nether,
                 const FlowerNoise &flowers, int mc, const VersionRules &rules, const Hit &hit,
                 int winxBlocks, int winzBlocks, char orient, int order,
                 FlowerSearchControl &control, OutRow &row, bool &accepted)
{
    accepted = false;
    if (!pauseOk(control))
        return false;
    const int winx = winxBlocks / 4;
    const int winz = winzBlocks / 4;
    if (!biome_precheck(&climate, hit.qx, hit.qz, winx, winz, 1, rules.requirePale ? 1 : 0))
        return true;
    if (rules.requirePale)
    {
        if (std::abs(hit.w.e_min_qx - hit.w.h_max_qx) * 4 > kPaleDist ||
            std::abs(hit.w.e_min_qz - hit.w.h_max_qz) * 4 > kPaleDist)
            return true;
        if (!biome_pale_actual_possible(&climate, &overworld, hit.qx, hit.qz, winx, winz, kBiomeY))
            return true;
    }

    BiomeResult br{};
    biome_check_window_fast(&climate, &overworld, hit.qx, hit.qz, winx, winz, kBiomeY,
                            rules.requirePale ? 1 : 0, &br);
    if (rules.requirePale)
    {
        if (!(br.pale && br.swamp))
            return true;
    }
    else if (!br.swamp)
    {
        return true;
    }
    if (!pauseOk(control))
        return false;

    const int wx = hit.qx * 4;
    const int wz = hit.qz * 4;
    std::vector<int> flowerX;
    std::vector<int> flowerZ;
    int paleCells = 0, swampCells = 0;
    int birchCells = 0, oldCells = 0, meadowCells = 0, cherryCells = 0;
    flowerX.reserve(256);
    flowerZ.reserve(256);
    for (int qz = 0; qz < winz; qz++)
    {
        for (int qx = 0; qx < winx; qx++)
        {
            const int bx = wx + qx * 4;
            const int bz = wz + qz * 4;
            const int id = biome_at_cached(&overworld, bx, kBiomeY, bz);
            if (id == flower_forest)
            {
                flowerX.push_back(bx + 2);
                flowerZ.push_back(bz + 2);
            }
            else if (id == pale_garden)
                paleCells++;
            else if (id == swamp)
                swampCells++;
            else if (id == birch_forest)
                birchCells++;
            else if (id == old_growth_birch_forest)
                oldCells++;
            else if (id == meadow)
                meadowCells++;
            else if (id == cherry_grove)
                cherryCells++;
        }
    }
    const int flowerArea = (int) flowerX.size() * 16;
    if (flowerArea <= kFlowerAreaMin)
        return true;
    const int cap = 512;
    const int stride = std::max(1, (flowerArea / 16 + cap - 1) / cap);
    std::vector<int> sampleX;
    std::vector<int> sampleZ;
    sampleX.reserve((size_t) std::min((int) flowerX.size(), cap));
    sampleZ.reserve(sampleX.capacity());
    for (size_t i = 0; i < flowerX.size(); i += (size_t) stride)
    {
        sampleX.push_back(flowerX[i]);
        sampleZ.push_back(flowerZ[i]);
    }
    int flowerY = 0;
    if (!pauseOk(control))
        return false;
    if (!flowerExtremes(flowers, sampleX, sampleZ, flowerY))
        return true;

    const int cx = wx + winxBlocks / 2;
    const int cz = wz + winzBlocks / 2;
    if (!pauseOk(control))
        return false;
    const std::string netherList =
        netherBiomesInWindow(nether, wx, wz, winxBlocks, winzBlocks, mc);
    const bool basalt = netherList.find("basalt_deltas") != std::string::npos;
    const bool woodBonus = rules.allowBirchMeadow && (birchCells || oldCells || meadowCells);
    const bool cherryBonus = rules.allowCherry && cherryCells > 0;
    row.score = (basalt ? 1 : 0) + (woodBonus ? 1 : 0) + (cherryBonus ? 1 : 0);
    row.hasPinkPetals = cherryBonus ? 1 : 0;
    row.order = order;
    row.qx = hit.qx;
    row.qz = hit.qz;
    row.sideX = winxBlocks;
    row.sideZ = winzBlocks;
    row.cx = cx;
    row.cz = cz;
    row.paleX = br.pale_qx * 4;
    row.paleZ = br.pale_qz * 4;
    row.swampX = br.swamp_qx * 4;
    row.swampZ = br.swamp_qz * 4;
    row.flowerX = flowerX.front() - 2;
    row.flowerZ = flowerZ.front() - 2;
    row.flowerY = flowerY;
    row.flowerArea = flowerArea;
    row.paleArea = paleCells * 16;
    row.swampArea = swampCells * 16;
    row.orient = orient;
    row.bonus = largestWoodBonus(birchCells, oldCells, meadowCells, cherryCells, rules);
    row.nether = netherList;
    accepted = true;
    return true;
}

} // namespace

std::vector<FlowerRow> runFlowerFarmSearchRows(
    uint64_t seed,
    int mc,
    int minX,
    int minZ,
    int maxX,
    int maxZ,
    int longSide,
    int shortSide,
    bool squareOnly,
    int frameStep,
    int threads,
    FlowerSearchControl &control)
{
    std::vector<FlowerRow> result;
    if (mc == MC_UNDEF)
        mc = MC_NEWEST;
    if (maxX < minX || maxZ < minZ)
        return result;

    /* "threads <= 0" or anything above the machine limit means "use all cores". */
    const unsigned hwThreads = std::thread::hardware_concurrency();
    const int maxThreads = hwThreads > 0 ? (int) hwThreads : 1;
    if (threads <= 0 || threads > maxThreads)
        threads = maxThreads;

    const VersionRules rules = rulesForMc(mc);

    const bool square = squareOnly || shortSide == longSide;
    const int windowX = alignDown(longSide, square ? 32 : 8, square ? 64 : 8);
    const int windowZ = square ? windowX : alignDown(shortSide, 8, 8);
    if (windowX <= 0 || windowZ <= 0)
        return result;

    CtsBinding binding(control);
    setPhase(control, 0, 1);
    setOverall(control, 0, 0);
    if (!pauseOk(control))
        return result;

    Climate climate{};
    climate_init(&climate, seed, 0);
    if (mc != MC_NEWEST)
    {
        initBiomeNoise(&climate.bn, mc);
        setBiomeSeed(&climate.bn, seed, 0);
    }

    ScanCfg cfg = makeCfg(windowX, windowZ, frameStep <= 4, rules.requirePale ? 0 : 1,
                          rules.clusterBlocks, threads);
    const int64_t x0 = minX;
    const int64_t z0 = minZ;
    const int64_t x1 = (int64_t) maxX + 1;
    const int64_t z1 = (int64_t) maxZ + 1;

    setPhase(control, 1, 1);
    setOverall(control, 0, 0);
    cts_bind_progress(reinterpret_cast<int *>(control.current), reinterpret_cast<int *>(control.total));

    Hit *hits = nullptr;
    Hit *hitsZ = nullptr;
    int nHits = 0;
    int nHitsZ = 0;
    ScanStats stats{};
    const bool dual = windowX != windowZ;
    int screened = 0;
    if (dual)
    {
        screened = screen_area_rect_both_parallel(&climate, x0, z0, x1, z1, &cfg, kStep,
            kMarginE, kMarginH, threads, &hits, &nHits, &hitsZ, &nHitsZ, &stats);
    }
    else
    {
        screened = screen_area_parallel(&climate, x0, z0, x1, z1, &cfg, kStep,
            kMarginE, kMarginH, kBg, threads, &hits, &nHits, &stats);
    }
    cts_bind_progress(nullptr, nullptr);
    if (!screened || !pauseOk(control))
    {
        std::free(hits);
        std::free(hitsZ);
        return result;
    }

    /* Phase 2 total: one unit per candidate plus one finalize unit
     * (cluster / sort / copy), so the bar never sits at 100% while the
     * tail still runs. */
    const int phase1Total = std::max(1, loadInt(control.total));
    const int candidateCount = nHits + nHitsZ;
    const int phase2Total = candidateCount < INT_MAX ? candidateCount + 1 : INT_MAX;
    const int overallTotal = phase1Total > INT_MAX - phase2Total
        ? INT_MAX : phase1Total + phase2Total;
    setPhase(control, 2, phase2Total);
    setOverall(control, phase1Total, overallTotal);

    Generator overworld{};
    Generator nether{};
    setupGenerator(&overworld, mc, 0);
    applySeed(&overworld, DIM_OVERWORLD, seed);
    setupGenerator(&nether, mc, 0);
    applySeed(&nether, DIM_NETHER, seed);

    FlowerNoise flowers;
    initFlowerNoise(flowers);

    std::vector<OutRow> rows;
    rows.reserve((size_t) std::min(candidateCount, 4096));
    int done = 0;
    bool keep = true;
    auto take = [&](const Hit &hit, int winx, int winz, char orient, int order) {
        if (!keep)
            return;
        OutRow row;
        bool accepted = false;
        keep = considerHit(climate, overworld, nether, flowers, mc, rules, hit, winx, winz, orient, order,
                           control, row, accepted);
        if (keep && accepted)
            rows.push_back(std::move(row));
        done++;
        setPhaseCurrent(control, done);
        setOverall(control, phase1Total + done, overallTotal);
    };

    if (dual)
    {
        const char orientX = windowX >= windowZ ? 'X' : 'Z';
        const char orientZ = orientX == 'X' ? 'Z' : 'X';
        for (int i = 0; i < nHits; i++)
            take(hits[i], windowX, windowZ, orientX, i);
        for (int i = 0; i < nHitsZ && keep; i++)
            take(hitsZ[i], windowZ, windowX, orientZ, nHits + i);
    }
    else
    {
        for (int i = 0; i < nHits; i++)
            take(hits[i], windowX, windowZ, 0, i);
    }
    std::free(hits);
    std::free(hitsZ);
    if (!keep)
        return result;

    /* Candidates are done; keep one phase-2 unit for the finalize step. */
    setPhaseCurrent(control, candidateCount);
    setOverall(control, phase1Total + candidateCount, overallTotal);
    if (!clusterVerifiedRows(rows, rules.clusterBlocks, control))
        return result;
    if (!pauseOk(control))
        return result;

    /* Same rule as GUI Sort: cherry first, then nearer to origin. */
    std::sort(rows.begin(), rows.end(), [](const OutRow &a, const OutRow &b) {
        if (a.hasPinkPetals != b.hasPinkPetals)
            return a.hasPinkPetals > b.hasPinkPetals;
        const int64_t da = originDist2(a), db = originDist2(b);
        if (da != db)
            return da < db;
        if (a.qx != b.qx)
            return a.qx < b.qx;
        if (a.qz != b.qz)
            return a.qz < b.qz;
        return a.order < b.order;
    });
    if (!pauseOk(control))
        return result;

    result.reserve(rows.size());
    for (const OutRow &row : rows)
        result.push_back(static_cast<const FlowerRow &>(row));

    setPhaseCurrent(control, phase2Total);
    setOverall(control, overallTotal, overallTotal);
    return result;
}

std::vector<std::string> runFlowerFarmSearch(
    uint64_t seed,
    int mc,
    int minX,
    int minZ,
    int maxX,
    int maxZ,
    int longSide,
    int shortSide,
    bool squareOnly,
    int frameStep,
    int threads,
    FlowerSearchControl &control)
{
    const int effectiveMc = mc == MC_UNDEF ? MC_NEWEST : mc;
    const VersionRules rules = rulesForMc(effectiveMc);
    const std::vector<FlowerRow> rows = runFlowerFarmSearchRows(
        seed, mc, minX, minZ, maxX, maxZ, longSide, shortSide, squareOnly,
        frameStep, threads, control);
    std::vector<std::string> lines;
    lines.reserve(rows.size());
    for (const FlowerRow &row : rows)
        lines.push_back(formatRow(row, rules));
    return lines;
}

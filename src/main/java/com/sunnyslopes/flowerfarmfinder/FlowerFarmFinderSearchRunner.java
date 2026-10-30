package com.sunnyslopes.flowerfarmfinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Flower-farm search via JNI ({@link FlowerFarmFinderBridge}).
 * Progress from {@link FlowerFarmFinderBridge#getSearchProgress()}.
 */
public class FlowerFarmFinderSearchRunner {

    /* ---- Default search-range square side (blocks), by tab × version ---- */
    /** Single-seed, 1.21.4+. */
    public static final int SEARCH_RANGE_SIDE_LATEST = 131_072;
    /** Single-seed, 1.18~1.21.3. */
    public static final int SEARCH_RANGE_SIDE_LEGACY = 32_768;
    /** Seed-list, 1.21.4+. */
    public static final int LIST_RANGE_SIDE_LATEST = 32_768;
    /** Seed-list, 1.18~1.21.3. */
    public static final int LIST_RANGE_SIDE_LEGACY = 8_192;

    /** Max search area (blocks). */
    public static final long MAX_SEARCH_AREA_BLOCKS = 60_000_001L * 60_000_001L;

    /** UI: 1.18~1.21.3 (cubiomes {@code 1.21.3}; no pale; pink_petals(1.19.4+) only). */
    public static final String MC_RANGE_LEGACY = "1.18~1.21.3";
    /** UI: 1.21.4+ (cubiomes newest; pale required; full bonus). */
    public static final String MC_RANGE_1_21_4 = "1.21.4+";
    /** Default UI version range. */
    public static final String DEFAULT_MC_VERSION = MC_RANGE_1_21_4;
    /** Supported UI version ranges. */
    public static final String[] SUPPORTED_MC_VERSIONS = {MC_RANGE_LEGACY, MC_RANGE_1_21_4};

    /** Default farm window written when switching to {@link #MC_RANGE_LEGACY}. */
    public static final int FLOWER_SIDE_LEGACY = 320;

    public static final int FLOWER_LONG_MIN = 256;
    public static final int FLOWER_LONG_MAX = 1024;
    public static final int FLOWER_LONG_DEFAULT = 640;
    public static final int FLOWER_SHORT_DEFAULT = 320;
    public static final int FLOWER_VIEW_DEFAULT = 10;
    /** Passed through to native CTS. 4 enables full-window verification; 16 keeps the boundary screen. */
    public static final int FLOWER_FRAME_STEP_FAST = 16;
    public static final int FLOWER_FRAME_STEP_PRECISE = 4;

    /** Optional result filters passed to native CTS. */
    public static final int FILTER_ORIENTATION_ANY = 0;
    public static final int FILTER_ORIENTATION_HORIZONTAL = 1;
    public static final int FILTER_ORIENTATION_VERTICAL = 2;

    /* ---- Native search phases, in the order the search runs them ---- */
    /** Preparing climate / generators. */
    public static final int PHASE_PREPARING = 0;
    /** Climate screen (CTS coarse filter over the search area). */
    public static final int PHASE_CLIMATE = 1;
    /** Post-screen merge/cluster of candidate windows; no meaningful numeric progress. */
    public static final int PHASE_MERGE = 2;
    /** Biome verification of the candidate windows. */
    public static final int PHASE_VERIFY = 3;

    /** Default flower-forest edge margin (blocks); the filter stays off until checked. */
    public static final int FLOWER_CENTER_MARGIN_DEFAULT = 0;

    private static final long PROGRESS_POLL_MS = 100L;

    /** Map UI range label to cubiomes version string passed to native. */
    public static String toCubiomesMcVersion(String uiSelection) {
        if (MC_RANGE_LEGACY.equals(uiSelection)) {
            return "1.21.3";
        }
        return "26.3";
    }

    /** {@code true} when the selection keeps current 1.21.4+ search behaviour. */
    public static boolean isLatestMcRange(String uiSelection) {
        return uiSelection == null || uiSelection.isBlank() || MC_RANGE_1_21_4.equals(uiSelection.trim());
    }

    /** Normalizes the orientation filter to {@link #FILTER_ORIENTATION_ANY}/HORIZONTAL/VERTICAL. */
    public static int clampOrientation(int orientation) {
        return (orientation == FILTER_ORIENTATION_HORIZONTAL || orientation == FILTER_ORIENTATION_VERTICAL)
                ? orientation : FILTER_ORIENTATION_ANY;
    }

    /**
     * @param phase {@link #PHASE_PREPARING} / {@link #PHASE_CLIMATE} / {@link #PHASE_MERGE} / {@link #PHASE_VERIFY};
     *     phase 2 (merge/cluster) has no meaningful numeric progress, only "running".
     * @param done when true, search has finished (success, stop, or error); UI should re-enable controls.
     * @param refiningResults native phase 3 (biome verification) is running.
     * @param nativePauseSettled when {@code true}, native pause has taken effect (progress stopped moving for a grace period).
     * @param nativeStopRequested native stop flag is set (search may still be winding down until {@code done}).
     */
    public record ProgressInfo(int phase, long processed, long total, double percentage, long elapsedMs, long remainingMs,
                               int currentSeedIndex, int totalSeeds, boolean done, boolean refiningResults,
                               boolean nativePauseSettled, boolean nativeStopRequested) {
        public static ProgressInfo of(int phase, long processed, long total, long elapsedMs, long remainingMs) {
            return of(phase, processed, total, elapsedMs, remainingMs, false, false, false);
        }

        /** @param verifyingResults native phase 3 (biome verification). */
        public static ProgressInfo of(int phase, long processed, long total, long elapsedMs, long remainingMs,
                                      boolean verifyingResults) {
            return of(phase, processed, total, elapsedMs, remainingMs, verifyingResults, false, false);
        }

        public static ProgressInfo of(int phase, long processed, long total, long elapsedMs, long remainingMs,
                                      boolean verifyingResults, boolean nativePauseSettled, boolean nativeStopRequested) {
            double pct = total > 0 ? (processed * 100.0 / total) : 0;
            return new ProgressInfo(phase, processed, total, pct, elapsedMs, remainingMs, 0, 0, false, verifyingResults,
                nativePauseSettled, nativeStopRequested);
        }

        /** Final callback after native search returns. */
        public static ProgressInfo terminal(long elapsedMs, long processed, long total) {
            long t = Math.max(1, total);
            long p = Math.min(processed, t);
            double pct = t > 0 ? (p * 100.0 / t) : 100.0;
            return new ProgressInfo(0, p, t, pct, elapsedMs, 0, 0, 0, true, false, false, false);
        }
    }

    private static final Object NATIVE_LOCK = new Object();
    private static volatile FlowerFarmFinderSearchRunner activeRunner;

    /** True while any runner still holds the native search lock (including wind-down after stop). */
    public static boolean isAnySearchActive() {
        synchronized (NATIVE_LOCK) {
            return activeRunner != null;
        }
    }

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private volatile long runStartTimeMs = 0;
    private volatile long totalPausedMs = 0;
    private volatile long pauseStartMs = 0;

    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }

    public void pause() {
        if (!isPaused) {
            isPaused = true;
            pauseStartMs = System.currentTimeMillis();
            try {
                FlowerFarmFinderBridge.pause();
            } catch (Throwable ignored) {
            }
        }
    }

    public void resume() {
        if (isPaused) {
            if (pauseStartMs > 0) {
                totalPausedMs += System.currentTimeMillis() - pauseStartMs;
                pauseStartMs = 0;
            }
            isPaused = false;
            try {
                FlowerFarmFinderBridge.resume();
            } catch (Throwable ignored) {
            }
        }
    }

    /** Requests native stop; {@link #isRunning()} stays true until search returns on the search thread. */
    public void stop() {
        try {
            FlowerFarmFinderBridge.stop();
        } catch (Throwable ignored) {
        }
    }

    private long getElapsedMs() {
        long base = System.currentTimeMillis() - runStartTimeMs;
        long paused = totalPausedMs;
        if (pauseStartMs > 0) {
            paused += System.currentTimeMillis() - pauseStartMs;
        }
        return Math.max(0, base - paused);
    }

    /** Start flower-farm search. {@code shortSide} equals {@code longSide} when the short side is off. */
    public boolean startFlowerSearch(long seed, int minX, int maxX, int minZ, int maxZ,
                                     int longSide, int shortSide, boolean squareOnly, int threadCount,
                                     boolean fastMode, boolean bothWildflowers, boolean basaltDeltas,
                                     int orientation, int flowerCenterMargin, String mcVersion,
                                     Consumer<ProgressInfo> progressCallback, Consumer<String> resultCallback) {
        if (!tryAcquireActiveRunner()) {
            return false;
        }
        Thread t = new Thread(() -> runFlowerSearch(seed, minX, maxX, minZ, maxZ, longSide, shortSide, squareOnly,
                threadCount, fastMode, bothWildflowers, basaltDeltas, orientation, flowerCenterMargin, mcVersion,
                progressCallback, resultCallback),
            "flower-farm-search");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public boolean runFlowerSearchBlocking(long seed, int minX, int maxX, int minZ, int maxZ,
                                           int longSide, int shortSide, boolean squareOnly, int threadCount,
                                           boolean fastMode, boolean bothWildflowers, boolean basaltDeltas,
                                           int orientation, int flowerCenterMargin, String mcVersion,
                                           Consumer<ProgressInfo> progressCallback, Consumer<String> resultCallback) {
        if (!tryAcquireActiveRunner()) {
            return false;
        }
        runFlowerSearch(seed, minX, maxX, minZ, maxZ, longSide, shortSide, squareOnly, threadCount, fastMode,
            bothWildflowers, basaltDeltas, orientation, flowerCenterMargin, mcVersion, progressCallback, resultCallback);
        return true;
    }

    private boolean tryAcquireActiveRunner() {
        synchronized (NATIVE_LOCK) {
            if (activeRunner != null) {
                return false;
            }
            activeRunner = this;
            return true;
        }
    }

    private void releaseActiveRunner() {
        synchronized (NATIVE_LOCK) {
            if (activeRunner == this) {
                activeRunner = null;
            }
        }
    }

    private void runFlowerSearch(long seed, int minX, int maxX, int minZ, int maxZ,
                                 int longSide, int shortSide, boolean squareOnly, int threadCount,
                                 boolean fastMode, boolean bothWildflowers, boolean basaltDeltas,
                                 int orientation, int flowerCenterMargin, String mcVersion,
                                 Consumer<ProgressInfo> progressCallback, Consumer<String> resultCallback) {
        isRunning = true;
        isPaused = false;
        totalPausedMs = 0;
        pauseStartMs = 0;
        runStartTimeMs = System.currentTimeMillis();
        int cores = Runtime.getRuntime().availableProcessors();
        int threads = (threadCount <= 0) ? cores : Math.min(threadCount, cores);
        if (threads < 1) {
            threads = 1;
        }
        long searchWidth = (long) (maxX - minX) + 1;
        long searchHeight = (long) (maxZ - minZ) + 1;
        long searchArea = searchWidth * searchHeight;
        if (searchArea <= 0 || searchArea > MAX_SEARCH_AREA_BLOCKS) {
            isRunning = false;
            releaseActiveRunner();
            if (resultCallback != null && searchArea > MAX_SEARCH_AREA_BLOCKS) {
                resultCallback.accept("[Error] Search area too large.");
            }
            if (progressCallback != null) {
                progressCallback.accept(ProgressInfo.terminal(getElapsedMs(), 0, 1));
            }
            return;
        }

        AtomicLong lastProcessed = new AtomicLong(0);
        AtomicLong lastTotal = new AtomicLong(0);
        AtomicInteger etaPhase = new AtomicInteger(Integer.MIN_VALUE);
        AtomicLong etaPhaseStartElapsedMs = new AtomicLong(0L);
        AtomicLong lastProgressWallMs = new AtomicLong(System.currentTimeMillis());
        AtomicLong lastCurSeen = new AtomicLong(Long.MIN_VALUE);
        AtomicInteger lastPhaseSeen = new AtomicInteger(Integer.MIN_VALUE);
        ScheduledExecutorService progressScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "flower-farm-progress");
            t.setDaemon(true);
            return t;
        });
        progressScheduler.scheduleAtFixedRate(() -> {
            if (progressCallback == null || !isRunning || activeRunner != FlowerFarmFinderSearchRunner.this) {
                return;
            }
            try {
                int[] arr = FlowerFarmFinderBridge.getSearchProgress();
                if (arr == null || arr.length < 4) {
                    return;
                }
                boolean tryStop = arr.length > 5 && arr[5] != 0;
                boolean tryPause = arr.length > 4 && arr[4] != 0;
                int cur = arr[0];
                int tot = arr[1];
                int nPhase = arr[2];
                // Phase 2 (merge/cluster) publishes its own unit counts; use them
                // so the bar starts at 0 for that phase.  While the counters are
                // not published yet, keep the previous frame instead of showing
                // numbers that belong to another phase.
                if (nPhase == PHASE_MERGE) {
                    if (arr.length >= 12 && arr[11] > 0) {
                        cur = arr[10];
                        tot = arr[11];
                    } else {
                        return;
                    }
                }
                long elapsed = getElapsedMs();
                if (tot <= 0) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (nPhase != lastPhaseSeen.get() || cur != lastCurSeen.get()) {
                    lastPhaseSeen.set(nPhase);
                    lastCurSeen.set(cur);
                    lastProgressWallMs.set(now);
                }
                // Native pause takes effect at its next checkpoint. Treat the
                // search as paused once progress has not moved for a short grace
                // period, instead of claiming it immediately.
                boolean pauseSettled = tryPause && (now - lastProgressWallMs.get() >= 300L);
                // ETA is measured from the moment this phase was first observed,
                // so phase 1 and phase 2 do not inherit an unrelated start time.
                if (nPhase != etaPhase.get()) {
                    etaPhase.set(nPhase);
                    etaPhaseStartElapsedMs.set(elapsed);
                }
                long phaseElapsed = Math.max(0L, elapsed - etaPhaseStartElapsedMs.get());
                lastProcessed.set(cur);
                lastTotal.set(tot);
                long remaining = 0L;
                if (!isPaused && tot > cur && cur > 0 && phaseElapsed > 0) {
                    double eta = phaseElapsed * ((double) (tot - cur) / (double) cur);
                    if (eta > 0 && eta < (double) Long.MAX_VALUE) {
                        remaining = (long) eta;
                    }
                }
                progressCallback.accept(ProgressInfo.of(nPhase, cur, tot, elapsed, remaining,
                    nPhase == PHASE_VERIFY, pauseSettled, tryStop));
            } catch (Throwable ignored) {
            }
        }, PROGRESS_POLL_MS, PROGRESS_POLL_MS, TimeUnit.MILLISECONDS);

        try {
            String uiMc = (mcVersion == null || mcVersion.isBlank()) ? DEFAULT_MC_VERSION : mcVersion.trim();
            String mc = toCubiomesMcVersion(uiMc);
            int step = fastMode ? FLOWER_FRAME_STEP_FAST : FLOWER_FRAME_STEP_PRECISE;
            int sideB = squareOnly ? longSide : shortSide;
            String[] raw;
            synchronized (NATIVE_LOCK) {
                raw = FlowerFarmFinderBridge.flowerSearch(seed, minX, minZ, maxX, maxZ,
                    longSide, sideB, squareOnly ? 1 : 0, step, threads,
                    bothWildflowers ? 1 : 0, basaltDeltas ? 1 : 0,
                    clampOrientation(orientation), Math.max(0, flowerCenterMargin), mc);
            }
            if (raw != null && resultCallback != null && raw.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String line : raw) {
                    sb.append(line).append('\n');
                }
                resultCallback.accept(sb.toString());
            }
        } catch (UnsatisfiedLinkError e) {
            if (resultCallback != null) {
                resultCallback.accept("[Error] Native library not loaded. Build with: jni/build-jni.bat. " + e.getMessage());
            }
        } finally {
            progressScheduler.shutdownNow();
            try {
                progressScheduler.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            isRunning = false;
            releaseActiveRunner();
            if (pauseStartMs > 0) {
                totalPausedMs += System.currentTimeMillis() - pauseStartMs;
                pauseStartMs = 0;
            }
            isPaused = false;
            if (progressCallback != null) {
                long p = lastProcessed.get();
                long t = lastTotal.get();
                if (t <= 0) {
                    t = Math.max(p, 1);
                }
                progressCallback.accept(ProgressInfo.terminal(getElapsedMs(), p, t));
            }
        }
    }
}

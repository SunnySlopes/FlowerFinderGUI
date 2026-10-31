package com.sunnyslopes.flowerfarmfinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * JNI bridge to native FlowerFarmFinder library (CTS flower-farm search).
 */
public final class FlowerFarmFinderBridge {

    private static boolean loaded = false;

    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        if (loaded) return;
        try {
            System.loadLibrary("libFlowerFarmFinderLibJ");
            loaded = true;
            return;
        } catch (UnsatisfiedLinkError e) {
            UnsatisfiedLinkError firstError = e;
            // Try loading from native library packaged inside the jar.
            if (tryLoadFromBundledResources()) {
                loaded = true;
                return;
            }

            // Fallback: try build output directory
            Path base = Paths.get(System.getProperty("user.dir", "."));
            List<String> libNames = getNativeLibNamesForCurrentOs();
            Path[] candidates = {
                base.resolve("native"),
                base.resolve("jni").resolve("native").resolve("jni").resolve("build"),
                base.resolve("build").resolve("libs").resolve("native"),
            };
            for (Path dir : candidates) {
                for (String libName : libNames) {
                    File f = dir.resolve(libName).toFile();
                    if (f.exists()) {
                        System.load(f.getAbsolutePath());
                        loaded = true;
                        return;
                    }
                }
            }
            throw new UnsatisfiedLinkError("FlowerFarmFinder native library not found. Build with: jni/build-jni.bat. " + firstError.getMessage());
        }
    }

    private static boolean tryLoadFromBundledResources() {
        String osFolder = getOsResourceFolder();
        if (osFolder == null) return false;

        for (String libName : getNativeLibNamesForCurrentOs()) {
            String resourcePath = "/native/" + osFolder + "/" + libName;
            if (tryLoadSingleBundledResource(resourcePath, libName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryLoadSingleBundledResource(String resourcePath, String libName) {
        try (InputStream is = FlowerFarmFinderBridge.class.getResourceAsStream(resourcePath)) {
            if (is == null) return false;
            Path tempDir = Files.createTempDirectory("flowerfarmfinder-native-");
            Path tempLib = tempDir.resolve(libName);
            Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
            tempLib.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            System.load(tempLib.toAbsolutePath().toString());
            return true;
        } catch (IOException | UnsatisfiedLinkError ex) {
            return false;
        }
    }

    private static String getOsResourceFolder() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        if (os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("linux")) return "linux";
        return null;
    }

    private static List<String> getNativeLibNamesForCurrentOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> names = new ArrayList<>();
        if (os.contains("win")) {
            names.add("libFlowerFarmFinderLibJ.dll");
            names.add("FlowerFarmFinderLibJ.dll");
        } else if (os.contains("mac")) {
            names.add("libFlowerFarmFinderLibJ.dylib");
        } else {
            names.add("libFlowerFarmFinderLibJ.so");
        }
        return names;
    }

    /**
     * CTS climate-transition search. {@code shortSide} is ignored when {@code squareOnly != 0}.
     * {@code frameStep <= 4} enables full-window verification; otherwise the CTS boundary screen is used.
     * <ul>
     *   <li>{@code bothWildflowers}: keep only windows with wildflowers (birch/meadow) and pink petals (cherry).</li>
     *   <li>{@code basaltDeltas}: keep only windows whose nether counterpart contains basalt deltas.</li>
     *   <li>{@code orientation}: 0 = any, 1 = horizontal (X span >= Z span), 2 = vertical (Z span >= X span).</li>
     *   <li>{@code flowerCenterMargin}: keep only windows whose two flower-gradient ends
     *       (dandelion / lily of the valley) both lie inside the window centre, at least this
     *       many blocks from every edge; 0 disables the filter.</li>
     * </ul>
     * @return one result line per hit, or null if stopped
     */
    public static native String[] flowerSearch(long seed, int minX, int minZ, int maxX, int maxZ,
        int longSide, int shortSide, int squareOnly, int frameStep, int numThreads,
        int bothWildflowers, int basaltDeltas, int orientation, int flowerCenterMargin, String mcVersion);

    /**
     * Progress query. Returns
     * {@code [current, total, phase, status, tryPause, tryStop,
     * phaseCurrent, phaseTotal, overallCurrent, overallTotal]}, or {@code null} if idle.
     * {@code current}/{@code total} describe the current phase.
     * {@code phase}: 0 = preparation, 1 = climate screen, 2 = post-screen merge/cluster tail,
     * 3 = verification. Phase 2 has no meaningful numeric progress.
     * {@code status}: phase 3: 1 = verifying, 2 = verification done; other phases: 1.
     */
    public static native int[] getSearchProgress();

    /** Try pause task. */
    public static native boolean pause();

    /** Try resume task. */
    public static native boolean resume();

    /** Try to stop the task. */
    public static native boolean stop();



    private FlowerFarmFinderBridge() {}
}

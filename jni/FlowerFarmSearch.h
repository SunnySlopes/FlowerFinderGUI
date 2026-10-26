#pragma once

#include <atomic>
#include <cstdint>
#include <string>
#include <vector>

/**
 * Search control shared between the JNI bridge and the native search.
 *
 * current/total always describe the current phase:
 *   0 = preparation, 1 = climate screen, 2 = verification.
 * phaseCurrent/phaseTotal are explicit aliases for hosts that prefer them.
 * overallCurrent/overallTotal cover the whole search; overallTotal is filled
 * once the phase-2 candidate count is known.
 *
 * Pointers are optional except current/total/phase while a search runs.
 */
struct FlowerSearchControl {
    std::atomic_int *current = nullptr;
    std::atomic_int *total = nullptr;
    std::atomic_int *phase = nullptr;
    std::atomic_bool *pauseReq = nullptr;
    std::atomic_bool *stopReq = nullptr;
    std::atomic_int *phaseCurrent = nullptr;
    std::atomic_int *phaseTotal = nullptr;
    std::atomic_int *overallCurrent = nullptr;
    std::atomic_int *overallTotal = nullptr;
};

/**
 * One verified flower-farm window, independent of the legacy /tp text format.
 * cx/cz are the window centre in block coordinates; sideX/sideZ are the
 * requested window extents along X/Z (long side last for Z-oriented hits).
 */
struct FlowerRow {
    int cx = 0, cz = 0;
    int sideX = 0, sideZ = 0;
    int flowerArea = 0, paleArea = 0, swampArea = 0;
    int flowerX = 0, flowerY = 0, flowerZ = 0;
    int paleX = 0, paleZ = 0, swampX = 0, swampZ = 0;
    char orient = 0;             // 'X' long edge on X, 'Z' long edge on Z, 0 square
    std::string nether;          // cubiomes English biome key
    std::string bonus;           // wildflowers / pink_petals / wildflowers+pink_petals
};

/**
 * CTS 1.0.0 climate-transition search (pale garden + swamp + flower forest).
 * Window sizes come from the existing long/short side. frameStep <= 4 enables
 * CTS full-window verification; larger steps keep the CTS boundary screen.
 * Rows are returned in the same order as the legacy /tp lines.
 */
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
    FlowerSearchControl &control);

/**
 * Legacy text API. Each result line is a /tp command. Kept for the Java GUI
 * and existing callers; internally it calls runFlowerFarmSearchRows().
 */
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
    FlowerSearchControl &control);

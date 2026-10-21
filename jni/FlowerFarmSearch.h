#pragma once

#include <atomic>
#include <string>
#include <vector>

struct FlowerSearchControl {
    std::atomic_int *current;
    std::atomic_int *total;
    std::atomic_int *phase;
    std::atomic_bool *pauseReq;
    std::atomic_bool *stopReq;
};

/**
 * CTS 1.0.0 climate-transition search (pale garden + swamp + flower forest).
 * Window sizes come from the existing long/short side. frameStep <= 4 enables
 * CTS full-window verification; larger steps keep the CTS boundary screen.
 * Each result line is a /tp command.
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

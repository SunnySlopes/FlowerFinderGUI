#include "com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge.h"

#include <atomic>
#include <exception>
#include <mutex>
#include <new>
#include <thread>
#include <vector>

#include "../../FlowerFarmSearch.h"
#include "cubiomes/biomes.h"
#include "cubiomes/util.h"

struct Progress {
    std::atomic_int current{0};
    std::atomic_int total{0};
    std::atomic_int phase{0};          // 0 = preparation, 1 = screen, 2 = verify
    std::atomic_int overallCurrent{0};
    std::atomic_int overallTotal{0};
    std::atomic_bool try_pause{false};
    std::atomic_bool try_stop{false};
};

static std::mutex searchMutex;
static Progress progress{};

static void resetProgressState()
{
    progress.current.store(0);
    progress.total.store(0);
    progress.phase.store(0);
    progress.overallCurrent.store(0);
    progress.overallTotal.store(0);
    progress.try_pause.store(false);
    progress.try_stop.store(false);
}

JNIEXPORT jobjectArray JNICALL Java_com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge_flowerSearch
  (JNIEnv *env, jclass, jlong seed, jint minX, jint minZ, jint maxX, jint maxZ,
   jint longSide, jint shortSide, jint squareOnly, jint frameStep, jint numThreads, jstring mcVersion)
{
    std::lock_guard<std::mutex> guard(searchMutex);
    int mc = MC_NEWEST;
    if (mcVersion != nullptr)
    {
        const char *mcStr = env->GetStringUTFChars(mcVersion, nullptr);
        if (mcStr != nullptr)
        {
            int parsed = str2mc(mcStr);
            if (parsed != MC_UNDEF)
                mc = parsed;
            env->ReleaseStringUTFChars(mcVersion, mcStr);
        }
    }
    resetProgressState();
    FlowerSearchControl control{
        &progress.current, &progress.total, &progress.phase,
        &progress.try_pause, &progress.try_stop,
        &progress.current, &progress.total,
        &progress.overallCurrent, &progress.overallTotal
    };
    unsigned hw = std::thread::hardware_concurrency();
    int maxThreads = hw > 0 ? (int) hw : 1;
    int threads = numThreads > 0 ? (int) numThreads : maxThreads;
    if (threads < 1)
        threads = 1;
    if (threads > maxThreads)
        threads = maxThreads;
    std::vector<std::string> lines;
    try {
        lines = runFlowerFarmSearch((uint64_t) seed, mc, minX, minZ, maxX, maxZ,
            longSide, shortSide, squareOnly != 0, frameStep, threads, control);
    } catch (const std::bad_alloc &) {
        resetProgressState();
        return nullptr;
    } catch (const std::exception &) {
        resetProgressState();
        return nullptr;
    }
    if (progress.try_stop.load())
    {
        resetProgressState();
        return nullptr;
    }
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray((jsize) lines.size(), stringClass, nullptr);
    if (!arr)
    {
        resetProgressState();
        return nullptr;
    }
    for (jsize i = 0; i < (jsize) lines.size(); i++)
    {
        jstring s = env->NewStringUTF(lines[(size_t) i].c_str());
        env->SetObjectArrayElement(arr, i, s);
        env->DeleteLocalRef(s);
    }
    resetProgressState();
    return arr;
}

JNIEXPORT jintArray JNICALL Java_com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge_getSearchProgress
  (JNIEnv *env, jclass)
{
    int tot = progress.total.load();
    if (tot <= 0)
        return nullptr;

    int phase = progress.phase.load();
    int cur = progress.current.load();
    int status = 1;
    if (phase == 2)
        status = cur >= tot ? 2 : 1;
    int overallCur = progress.overallCurrent.load();
    int overallTot = progress.overallTotal.load();
    if (overallTot <= 0) {
        overallCur = cur;
        overallTot = tot;
    }

    jint data[10] = {
        cur, tot, phase, status,
        progress.try_pause.load() ? 1 : 0,
        progress.try_stop.load() ? 1 : 0,
        progress.current.load(), progress.total.load(),
        overallCur, overallTot
    };
    jintArray arr = env->NewIntArray(10);
    if (!arr)
        return nullptr;
    env->SetIntArrayRegion(arr, 0, 10, data);
    return arr;
}

JNIEXPORT jboolean JNICALL Java_com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge_pause(JNIEnv *, jclass)
{
    progress.try_pause.store(true);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge_resume(JNIEnv *, jclass)
{
    progress.try_pause.store(false);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sunnyslopes_flowerfarmfinder_FlowerFarmFinderBridge_stop(JNIEnv *, jclass)
{
    progress.try_stop.store(true);
    return JNI_TRUE;
}

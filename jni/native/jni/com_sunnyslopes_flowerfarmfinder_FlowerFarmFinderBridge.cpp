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
    std::atomic_int chunkInRunning{0};
    std::atomic_int phase1{0};
    std::atomic_bool try_pause{false};
    std::atomic_bool try_stop{false};
};

static std::mutex searchMutex;
static Progress progress{};

static void resetProgressState()
{
    progress.chunkInRunning.store(0);
    progress.current.store(0);
    progress.total.store(0);
    progress.phase1.store(0);
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
        &progress.current, &progress.total, &progress.phase1,
        &progress.try_pause, &progress.try_stop
    };
    int threads = numThreads > 0 ? (int) numThreads : (int) std::thread::hardware_concurrency();
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
    if (progress.total.load() <= 0)
        return nullptr;

    int status = 0;
    int phase = progress.phase1.load();
    int cur = progress.current.load();
    int tot = progress.total.load();
    if (phase == 0 || phase == 1)
        status = progress.chunkInRunning.load() == 0 ? 1 : 0;
    else if (phase == 2)
        status = cur >= tot ? 2 : 1;

    jint data[6] = {
        cur, tot, phase, status,
        progress.try_pause.load() ? 1 : 0, progress.try_stop.load() ? 1 : 0
    };
    jintArray arr = env->NewIntArray(6);
    if (!arr)
        return nullptr;
    env->SetIntArrayRegion(arr, 0, 6, data);
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

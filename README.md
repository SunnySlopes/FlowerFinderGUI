# Flower Farm Finder / 全花农场查找工具

A GUI tool for finding climate-transition windows that fit a full flower farm in Minecraft Java Edition 1.18+.

用于在 Minecraft Java 版 1.18+ 中查找适合「全花农场」的气候过渡窗口（花林 + 沼泽，1.21.4+ 另需苍白花园）的 GUI 工具。

基于 CTS（Climate Transition Search）气候噪声筛选，再做群系与花卉噪声校验。

## How to Run / 如何运行

**Java 17 or higher is required.**

**此程序需要 Java 17 或更高版本。**

```bat
java -jar build\libs\FlowerFarmFinder-1.0.0.jar
```

Or right-click the `.jar`, choose **Open with**, and select **Java 17+**.

也可右键 `.jar`，选择**打开方式**，使用 **Java 17 及以上版本**启动。

## How to Use / 如何使用

Two tabs: **Single Seed Search** and **Search from Seed List**.

两个标签页：**单种子搜索**与**多种子列表搜索**。

Both tabs share **Fast Mode** (on by default) and **Game Version**:

两个标签页均有**快速模式**（默认勾选）和**游戏版本**：

| UI label / 界面选项 | cubiomes | Behaviour / 行为 |
|---|---|---|
| `1.21.4+` (default / 默认) | `26.3` | Pale garden required; full bonus / 需苍白花园；完整加分群系 |
| `1.18~1.21.3` | `1.21.3` | Pale optional; no birch/meadow wildflowers bonus; cherry noted as `pink_petals(1.19.4+)` / 苍白可选；无白桦/草甸野花加分；樱花标注 `pink_petals(1.19.4+)` |

### Single Seed Search / 单种子搜索

- Enter seed, thread count, demand long/short side, simulation distance (模距), Fast Mode, version, and range (`MinX/MaxX/MinZ/MaxZ`).
- Default window: long **640**, short **320** (short side can be unchecked → square). Default search square side: **131072** (`1.21.4+`), or **32768** on `1.18~1.21.3`.
- Simulation distance **8～32** writes long side as `view × 32` and clears the short-side option.
- Click **Start Search**. You can **Pause**, **Resume**, or **Stop**.
- Results are `/tp` lines; you can **Sort** (cherry / `pink_petals` first, then nearer to origin) and **Export**.

- 输入种子、线程数、要求区域长/短边、模距、快速模式、版本与搜索范围。
- 默认窗口：长边 **640**、短边 **320**（取消勾选短边则为正方形）。默认搜索正方形边长：**131072**（`1.21.4+`），`1.18~1.21.3` 为 **32768**。
- 模距 **8～32** 会把长边写成 `模距 × 32`，并取消短边选项。
- 点击**开始搜索**，可**暂停** / **继续** / **停止**。
- 结果为 `/tp` 行，可**排序**（有樱花在前，同组按距原点距离）与**导出**。

### Search from Seed List / 多种子列表搜索

- **Select File**: one seed per line.
- Same window / Fast Mode / version parameters; default search square side **32768** (`1.21.4+`), or **8192** on `1.18~1.21.3`.
- After completion: export full results or seed list only; **Sort** puts seeds with any cherry hit first, then by that seed’s nearest result to origin.

- **选择文件**：每行一个种子。
- 窗口 / 快速模式 / 版本参数同上；默认搜索正方形边长：**32768**（`1.21.4+`），`1.18~1.21.3` 为 **8192**。
- 完成后可导出完整结果或仅种子列表；**排序**将出现过樱花的种子整体靠前，再按该种子距原点最近的结果排序。

## Fast Mode / Precise Mode / 快速模式与精确模式

CTS `frameStep` controls verification depth:

CTS 的 `frameStep` 控制校验深度：

| Mode / 模式 | frameStep | Behaviour / 行为 |
|---|---|---|
| **Fast Mode** (default) / **快速模式**（默认） | 16 | Boundary screen only — faster, may miss some hits / 仅边界粗筛，更快，可能漏检 |
| **Precise Mode** (unchecked) / **精确模式**（取消勾选） | 4 | Full-window verification — slower, fewer misses / 整窗精确校验，更慢、漏检更少 |

Long side must be **256～1024**. Short side (when enabled) must be ≤ long side.

长边须为 **256～1024**；启用短边时须 ≤ 长边。

## Search Semantics / 搜索语义

Native search (`flowerSearch`) runs in two phases:

原生搜索（`flowerSearch`）分两阶段：

1. **Phase 1** — CTS climate screen (erosion + humidity boxes) over the search area, step 256.
2. **Phase 2** — For each hit: overworld biome check + flower-noise extremes + nether biome under the window.

1. **阶段 1** — 在搜索区内做 CTS 气候粗筛（侵蚀 + 湿度盒），步进 256。
2. **阶段 2** — 对候选点做主世界群系校验、花卉噪声极值、以及窗口下方地狱群系。

Acceptance highlights:

通过条件要点：

- Window must contain **flower forest** + **swamp**; on `1.21.4+` also **pale garden**.
- `flowerArea` must be **> 2000** (4×4 cells × 16).
- Flower noise must have both gradient ends (dandelion / lily of the valley) within **32** vertical blocks.
- Score prefers pink petals, then basalt deltas / wood bonus / cherry, then closer to world origin.

- 窗口须含**花林** + **沼泽**；`1.21.4+` 还须含**苍白花园**。
- `flowerArea` 须 **> 2000**（4×4 格 × 16）。
- 花卉噪声须在竖直 **32** 格内同时出现两端（蒲公英 / 铃兰）。
- 排序优先樱花粉瓣，其次玄武岩三角洲 / 木材加分 / 樱花，再按距出生点更近。

Result line example / 结果行示例：

```text
/tp x 64 z side=640x320 flowerArea=N paleArea=M swampArea=K nether=... [bonus=wildflowers+pink_petals]
```

- `side=` — demand window size (square or `WxH`)
- `flowerArea=` / `paleArea=` / `swampArea=` — patch sizes in the window（`paleArea` only on 1.21.4+）
- `nether=` — nether biomes under the overworld window
- `bonus=` — optional `wildflowers` and/or `pink_petals`

## Features / 功能特点

- Multi-threaded CTS climate + biome search
- Pause / resume / stop with live progress and ETA
- Batch search from seed list files
- Sort and export results

- 多线程 CTS 气候 + 群系搜索
- 暂停 / 继续 / 停止，实时进度与剩余时间
- 种子列表批量搜索
- 结果排序与导出

## Build / 构建

On Windows, `buildMainJar` can compile the JNI DLL via `jni/build-jni.bat` (needs JDK with `include/jni.h`, and MSYS2 UCRT64 `gcc`/`g++` on `PATH`).

Windows 下 `buildMainJar` 可通过 `jni/build-jni.bat` 编译 JNI DLL（需要带 `include/jni.h` 的 JDK，以及 PATH 中的 MSYS2 UCRT64 `gcc`/`g++`）。

```bat
gradle buildMainJar
```

Or build the native library alone / 也可单独编译 native：

```bat
jni\build-jni.bat
```

DLL is copied to `native/windows/libFlowerFarmFinderLibJ.dll` and packaged into the JAR by `prepareNativeResources`.

DLL 会复制到 `native/windows/libFlowerFarmFinderLibJ.dll`，并由 `prepareNativeResources` 打入 JAR。

Init cubiomes if needed / 如需初始化 cubiomes 子模块：

```bat
git submodule update --init jni/cubiomes
```

## Core Libraries / 核心依赖

- [cubiomes](https://github.com/Cubitect/cubiomes) — via `jni/cubiomes` (biome / climate noise)
- CTS sources under `jni/cts/` — climate-transition window search
- Authors / 作者：SunnySlopes & M33 Galaxy

## Notes / 注意事项

- Match thread count to your CPU for stability.
- Native libraries (`dll` / `so` / `dylib`) are packaged in the JAR and extracted to a temp directory at runtime.
- To refresh the Windows native lib: rebuild with `jni/build-jni.bat` (overwrites `native/windows/libFlowerFarmFinderLibJ.dll`).

- 建议按 CPU 能力设置线程数以获得更稳定体验。
- 原生库已打包进 jar，运行时解压到临时目录加载。
- 更新 Windows native 库：运行 `jni/build-jni.bat`（覆盖 `native/windows/libFlowerFarmFinderLibJ.dll`）。

---

**Find better full flower farm sites, faster.**
**更快地找到更优质的全花农场点位。**

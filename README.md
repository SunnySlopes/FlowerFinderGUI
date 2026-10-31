# Flower Farm Finder / 全花农场查找工具

A bilingual Swing GUI for finding climate-transition windows that fit a full flower farm in Minecraft Java Edition 1.18+.

用于在 Minecraft Java 版 1.18+ 中查找适合「全花农场」的气候过渡窗口（花林 + 沼泽，1.21.4+ 另需苍白花园）的双语 Swing GUI 工具。

The search runs in two stages: a CTS (Climate Transition Search) climate-noise screen, then biome and flower-noise verification of the surviving candidates.

搜索分两步：先用 CTS（Climate Transition Search）气候噪声筛选，再对候选窗口做群系与花卉噪声校验。

## Module Contents / 模块内容

This repository is the GUI module of Flower Farm Finder. It contains only the flower-farm search pipeline — river / karst-cave finder code is not part of this module.

本仓库是 Flower Farm Finder 的 GUI 模块，只包含全花农场搜索流程——不包含河流 / 溶洞查找相关代码。

| Path / 路径 | Contents / 内容 |
|---|---|
| `src/main/java/com/sunnyslopes/flowerfarmfinder/FlowerFarmFinderFrame.java` | Swing UI, tabs, filters, results / 界面、标签页、筛选与结果 |
| `src/main/java/com/sunnyslopes/flowerfarmfinder/FlowerFarmFinderSearchRunner.java` | Search lifecycle, progress polling, ETA / 搜索生命周期、进度与剩余时间 |
| `src/main/java/com/sunnyslopes/flowerfarmfinder/FlowerFarmFinderBridge.java` | JNI bridge and native-library loading / JNI 桥接与原生库加载 |
| `src/main/resources/` | i18n (`messages_*.properties`), font, icon, logging / 国际化、字体、图标与日志 |
| `jni/FlowerFarmSearch.cpp` / `.h` | Native flower-farm search (CTS + verification) / 原生全花搜索（CTS + 校验） |
| `jni/native/jni/` | JNI entry points / JNI 入口 |
| `jni/cts/` | CTS 1.0.0 climate-transition sources / CTS 1.0.0 气候过渡源码 |
| `jni/cubiomes/` | cubiomes submodule (biome / noise) / cubiomes 子模块（群系 / 噪声） |
| `jni/build-jni.bat` | Windows native build / Windows 原生库构建 |
| `native/windows/` | Prebuilt DLL packaged into the JAR / 打包进 JAR 的预编译 DLL |

## Requirements and Run / 运行要求

**Java 17 or higher is required.**

**此程序需要 Java 17 或更高版本。**

```bat
java -jar build\libs\FlowerFarmFinder-1.0.3.jar
```

Or right-click the `.jar`, choose **Open with**, and select **Java 17+**.

也可右键 `.jar`，选择**打开方式**，使用 **Java 17 及以上版本**启动。

The UI is available in English and Chinese; switch languages from the **Language** selector.

界面支持英文和中文，可用 **Language / 语言** 下拉框切换。

## How to Use / 如何使用

Two tabs: **Single Seed Search** and **Search from Seed List**.

两个标签页：**单种子搜索**与**多种子列表搜索**。

Both tabs share **Fast Mode** (on by default), **Game Version**, window size, simulation distance and the optional filters.

两个标签页共用**快速模式**（默认勾选）、**游戏版本**、窗口尺寸、模距以及可选筛选条件。

The settings column scrolls vertically when the window is short, while **Start / Pause / Stop** and the progress area stay pinned below it.

窗口较矮时，左侧设置栏可上下滚动，**开始 / 暂停 / 停止**与进度区域固定在下方，始终可见。

### Single Seed Search / 单种子搜索

- Enter a seed, thread count, window long/short side, simulation distance, Fast Mode, game version and the search range (`MinX` / `MaxX` / `MinZ` / `MaxZ`).
- Click **Start Search**; use **Pause**, **Resume** or **Stop** at any time.
- Results appear as `/tp` lines. **Sort** re-orders them (pink petals first, then nearest to origin) and **Export** writes them to a text file.

- 输入种子、线程数、窗口长/短边、模距、快速模式、游戏版本与搜索范围（`MinX` / `MaxX` / `MinZ` / `MaxZ`）。
- 点击**开始搜索**，可随时**暂停** / **继续** / **停止**。
- 结果为 `/tp` 行，可**排序**（有樱花粉瓣的在前，同组按距原点距离）与**导出**。

### Search from Seed List / 多种子列表搜索

- **Select File** loads one seed per line.
- The same window, Fast Mode, version and filter parameters apply, with separate default search ranges.
- After completion, export the full results or only the seed list; **Sort** puts seeds with any cherry hit first, then orders by each seed's nearest result to origin.

- **选择文件**载入每行一个种子的列表。
- 窗口、快速模式、版本与筛选参数与单种子页相同，默认搜索范围单独设置。
- 完成后可导出完整结果或仅种子列表；**排序**会把出现过樱花的种子整体靠前，再按该种子距原点最近的结果排序。

## Search Parameters / 搜索参数

| Parameter / 参数 | Default / 默认 | Behaviour / 行为 |
|---|---|---|
| Thread count / 线程数 | CPU cores / CPU 核心数 | Values above the CPU core count are clamped to it. Used by the climate screen and candidate verification. / 超过 CPU 核心数时自动下调；用于气候粗筛与候选校验。 |
| Window long side / 窗口长边 | `640` | Must be **256～1024** blocks. / 须为 **256～1024** 格。 |
| Window short side / 窗口短边 | `320` | Must be ≤ long side. Unchecked → square window. / 须 ≤ 长边；取消勾选则为正方形窗口。 |
| Simulation distance / 模距 | `10` | **8～32**. Writes the long side as `32 × distance + 16` (capped at 1024) and clears the short-side option. / **8～32**，把长边写成 `32 × 模距 + 16`（上限 1024），并取消短边选项。 |
| Square area side / 正方形区域边长 | `131072` / `32768` / `8192` | When checked, sets `MinX/MaxX/MinZ/MaxZ` to a square centred on the origin. Defaults: single-seed `131072` (`1.21.4+`) or `32768` (`1.18~1.21.3`); seed-list `32768` or `8192`. / 勾选后把搜索范围设成以原点为中心的正方形。默认值：单种子 `131072`（`1.21.4+`）或 `32768`（`1.18~1.21.3`）；列表 `32768` 或 `8192`。 |
| Fast Mode / 快速模式 | On / 勾选 | Boundary screen instead of full-window verification; see below. / 仅边界粗筛，不做整窗校验，详见下文。 |
| Game Version / 游戏版本 | `1.21.4+` | See the version table below. / 见下方版本表。 |

### Game Versions / 游戏版本

| UI label / 界面选项 | cubiomes | Behaviour / 行为 |
|---|---|---|
| `1.21.4+` (default / 默认) | `26.3` | Pale garden required; full bonus set / 需苍白花园；完整加分群系 |
| `1.18~1.21.3` | `1.21.3` | Pale optional; no birch/meadow wildflowers bonus; cherry shown as `pink_petals(1.19.4+)` / 苍白可选；无白桦/草甸野花加分；樱花标注 `pink_petals(1.19.4+)` |

### Search Range / 搜索范围

- `MinX` / `MaxX` / `MinZ` / `MaxZ` are inclusive block coordinates; the range must stay inside the world border.
- **Reset to Default** restores the tab's default range; **Reset to World Border** expands it to the world border.
- The maximum search area is limited; an oversized range is rejected before the native search starts.

- `MinX` / `MaxX` / `MinZ` / `MaxZ` 为闭区间方块坐标，范围须位于世界边界内。
- **重置为默认值**恢复该标签页的默认范围；**重置为世界边界**扩展到世界边界。
- 搜索面积有上限，过大的范围会在原生搜索开始前被拒绝。

## Optional Filters / 筛选条件

Both tabs have four optional filters (off by default). They are applied after the hard requirements, before ranking.

两个标签页都有四个可选筛选条件（默认关闭），在硬性条件之后、排序之前生效。

| Filter / 选项 | Behaviour / 行为 |
|---|---|
| **Both wildflowers** / **两种野花** | Keep only windows whose bonus contains both `wildflowers` (birch/meadow) and `pink_petals` (cherry). Disabled on `1.18~1.21.3`, which has no wildflower bonus. / 只保留同时含 `wildflowers`（白桦/草甸）与 `pink_petals`（樱花）的窗口；`1.18~1.21.3` 无野花加分，该项禁用。 |
| **Nether contains basalt deltas** / **地狱含玄武岩三角洲** | Keep only windows whose nether counterpart contains basalt deltas. / 只保留对应下界窗口含玄武岩三角洲的窗口。 |
| **Require horizontal/vertical** / **要求横向/竖向** | Horizontal keeps `sideX ≥ sideZ`; vertical keeps `sideZ ≥ sideX`. Square windows satisfy both. / 横向只保留 X 跨度 ≥ Z 跨度；竖向只保留 Z 跨度 ≥ X 跨度；正方形窗口两者都满足。 |
| **Flower forest edge distance** / **繁花森林离边距离** | Off by default, distance field `0`. When checked and filled, both flower-gradient ends (dandelion / lily of the valley) must lie inside the window centre — at least that many blocks from every edge of the demand window. / 默认不勾选，距离填 `0`。勾选并填写后，繁花的 2 个花梯度极端（蒲公英/铃兰）都须落在中心区内，即距要求区域四边均不小于该距离。 |

## Fast Mode / Precise Mode / 快速模式与精确模式

CTS `frameStep` controls verification depth:

CTS 的 `frameStep` 控制校验深度：

| Mode / 模式 | frameStep | Behaviour / 行为 |
|---|---|---|
| **Fast Mode** (default) / **快速模式**（默认） | 16 | Boundary screen only — faster, may miss some hits. Rectangular windows sample the border every 16 blocks; square windows keep 8. / 仅边界粗筛，更快，可能漏检；矩形窗口周界按 16 格采样，正方形窗口仍为 8 格。 |
| **Precise Mode** (unchecked) / **精确模式**（取消勾选） | 4 | Full-window verification — slower, fewer misses; border sampled every 8 blocks. / 整窗精确校验，更慢、漏检更少；周界按 8 格采样。 |

## Search Phases / 搜索阶段

Native search (`flowerSearch`) runs in these phases, in this order:

原生搜索（`flowerSearch`）按以下顺序执行：

1. **Phase 0 / Preparation** — climate and generator setup.
2. **Phase 1 / Climate screen** — builds the coarse climate field and sweeps the search area with the CTS erosion/humidity screen. The field work is counted here, so the bar advances from the first row.
3. **Phase 2 / Merge candidates** — clusters the screened candidates. Progress shows candidate hits, not scan units.
4. **Phase 3 / Verification** — for every candidate: overworld biome check, flower-noise extremes and the nether biome under the window. Candidates are verified **in parallel** with the same thread count. The final unit covers sort and overlap de-duplication.

1. **阶段 0 / 准备** — 初始化气候与群系生成器。
2. **阶段 1 / 气候粗筛** — 构建粗气候场并用 CTS 侵蚀/湿度条件遍历搜索区；粗场工作计入本阶段，因此进度条从第一行起就推进。
3. **阶段 2 / 整理候选** — 合并筛选出的候选窗口；进度按候选计数显示，而不是扫描单位。
4. **阶段 3 / 校验** — 对每个候选点做主世界群系校验、花卉噪声极值、以及窗口下方的地狱群系；候选按相同线程数**并行**校验，最后一个单位用于排序与重叠去重。

Every phase starts its bar at 0; phase 3 includes one extra finalize unit so the bar does not sit at 100% while sorting still runs.

每个阶段的进度都从 0 开始；阶段 3 会多算 1 个收尾单位，避免排序尚未结束时进度条已停在 100%。

Fast mode trades a little completeness for speed. Phase 3 is often the largest cost (it re-samples a whole window per candidate), so the thread count matters most there; results are merged and sorted with a total order, so the output does not depend on the thread count.

快速模式用少量完整性换速度。阶段 3 通常是耗时大头（每个候选都要重采一整个窗口），线程数主要影响该阶段；结果先合并再按全序排序，因此输出与线程数无关。

## Acceptance and Ranking / 通过条件与排序

- Window must contain **flower forest** + **swamp**; on `1.21.4+` also **pale garden**.
- `flowerArea` must be **> 2000** (4×4 cells × 16).
- Flower noise must have both gradient ends (dandelion / lily of the valley) within **32** vertical blocks.
- With **Flower forest edge distance** set, both gradient ends must also sit in the window centre (≥ distance from every edge).
- Verified windows are **de-duplicated by window overlap**: after ranking, a window that intersects an already-kept window is dropped, so every returned result occupies a disjoint area.
- Results are ranked **pink petals (cherry) first, then nearer to world origin**.

- 窗口须含**花林** + **沼泽**；`1.21.4+` 还须含**苍白花园**。
- `flowerArea` 须 **> 2000**（4×4 格 × 16）。
- 花卉噪声须在竖直 **32** 格内同时出现两端（蒲公英 / 铃兰）。
- 勾选**繁花森林离边距离**时，两端还须落在窗口中心区内（距四边均 ≥ 该距离）。
- 校验通过的窗口按**窗口是否重叠**去重：排序后，与已保留窗口相交的窗口会被丢弃，因此返回的窗口互不重叠。
- 结果按**樱花粉瓣优先、再按距世界原点更近**排序。

## Result Format / 结果格式

```text
/tp x 64 z side=640x320 flowerArea=N paleArea=M swampArea=K nether=... [bonus=wildflowers+pink_petals]
```

- `x` / `z` — window centre (block coordinates); the suggested teleport height is `64`.
- `side=` — window size (square as `N`, rectangle as `WxH`).
- `flowerArea=` / `paleArea=` / `swampArea=` — sampled patch sizes in the window (`paleArea` only on `1.21.4+`).
- `nether=` — highest-ranked nether biome found under the window (basalt deltas → nether wastes → crimson forest → soul sand valley → warped forest).
- `bonus=` — optional `wildflowers` and/or `pink_petals`.

- `x` / `z` — 窗口中心（方块坐标），建议传送高度为 `64`。
- `side=` — 窗口尺寸（正方形为 `N`，矩形为 `WxH`）。
- `flowerArea=` / `paleArea=` / `swampArea=` — 窗口内采样到的面积（`paleArea` 仅 `1.21.4+`）。
- `nether=` — 窗口下方排名最高的地狱群系（玄武岩三角洲 → 下界荒地 → 绯红森林 → 灵魂沙峡谷 → 诡异森林）。
- `bonus=` — 可选 `wildflowers` 和/或 `pink_petals`。

## Features / 功能特点

- Single-seed and seed-list search with live progress and per-phase ETA
- Multi-threaded climate screen **and** candidate verification
- Pause / resume / stop with deferred stop while the native search winds down
- Fast (boundary screen) and precise (full-window) modes
- Four optional result filters
- Sort and export results; export the seed list only
- English / Chinese UI

## Build / 构建

**Java JAR**

```bat
.\gradlew.bat buildMainJar
```

Or, with a local Gradle installation:

```bat
gradle buildMainJar
```

The fat JAR is written to `build\libs\FlowerFarmFinder-1.0.3.jar`.

**Windows native library**

`buildMainJar` can compile the JNI DLL via `jni/build-jni.bat`. This needs a JDK with `include/jni.h` and MSYS2 UCRT64 `gcc` / `g++` on `PATH` (the script also prepends `C:\msys64\ucrt64\bin`).

Windows 下 `buildMainJar` 可通过 `jni/build-jni.bat` 编译 JNI DLL，需要带 `include/jni.h` 的 JDK，以及 PATH 中的 MSYS2 UCRT64 `gcc` / `g++`（脚本也会把 `C:\msys64\ucrt64\bin` 加到 PATH 前面）。

Build the native library alone / 也可单独编译 native：

```bat
jni\build-jni.bat
```

The DLL is copied to `native/windows/libFlowerFarmFinderLibJ.dll` and packaged into the JAR by `prepareNativeResources`.

DLL 会复制到 `native/windows/libFlowerFarmFinderLibJ.dll`，并由 `prepareNativeResources` 打入 JAR。

Initialize the cubiomes submodule if needed / 如需初始化 cubiomes 子模块：

```bat
git submodule update --init jni/cubiomes
```

## Core Libraries / 核心依赖

- [cubiomes](https://github.com/Cubitect/cubiomes) — via `jni/cubiomes` (biome / climate noise)
- CTS 1.0.0 sources under `jni/cts/` — climate-transition window search
- Authors / 作者：SunnySlopes & M33 Galaxy

## Notes / 注意事项

- Match the thread count to your CPU for stability; verification is parallel, so extra threads still help after the climate screen.
- On load the native library sets `GOMP_SPINCOUNT=0` / `OMP_WAIT_POLICY=passive` unless you already set them. This stops idle OpenMP workers from spinning into the serial tail (sort / de-dup) and the UI.
- The output is deterministic: candidates are merged and sorted with a total order, so thread count does not change the result set.
- Fast mode may return slightly fewer windows than precise mode. Use precise mode when completeness matters more than speed.
- Native libraries (`dll` / `so` / `dylib`) are packaged in the JAR and extracted to a temp directory at runtime.
- To refresh the Windows native library: rebuild with `jni/build-jni.bat` (overwrites `native/windows/libFlowerFarmFinderLibJ.dll`).

- 建议按 CPU 能力设置线程数；候选校验已并行化，因此气候粗筛结束后提高线程数仍然有效。
- 原生库加载时会设置 `GOMP_SPINCOUNT=0` / `OMP_WAIT_POLICY=passive`（若你已设置则保留你的值），避免 OpenMP 空闲线程在串行收尾（排序 / 去重）和界面线程上自旋。
- 输出是确定的：候选先合并再按全序排序，因此线程数不会改变结果集。
- 快速模式可能比精确模式少返回少量窗口。更看重完整性时请使用精确模式。
- 原生库（`dll` / `so` / `dylib`）已打包进 JAR，运行时解压到临时目录加载。
- 更新 Windows 原生库：运行 `jni\build-jni.bat`（覆盖 `native/windows/libFlowerFarmFinderLibJ.dll`）。

---

**Find better full flower farm sites, faster.**
**更快地找到更优质的全花农场点位。**

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

## Optional Filters / 筛选条件

Both tabs have four optional filters (off by default):

两个标签页都有四个可选筛选条件（默认关闭）：

| Filter / 选项 | Behaviour / 行为 |
|---|---|
| **Both wildflowers** / **两种野花** | Keep only windows whose bonus contains both `wildflowers` (birch/meadow) and `pink_petals` (cherry). Disabled on `1.18~1.21.3`, which has no wildflower bonus. / 只保留同时含 `wildflowers`（白桦/草甸）与 `pink_petals`（樱花）的窗口；`1.18~1.21.3` 无野花加分，该项禁用。 |
| **Nether contains basalt deltas** / **地狱含玄武岩三角洲** | Keep only windows whose nether counterpart contains basalt deltas. / 只保留对应下界窗口含玄武岩三角洲的窗口。 |
| **Require horizontal/vertical** / **要求横向/竖向** | Horizontal keeps `sideX ≥ sideZ`; vertical keeps `sideZ ≥ sideX`. Square windows satisfy both. / 横向只保留 X 跨度 ≥ Z 跨度；竖向只保留 Z 跨度 ≥ X 跨度；正方形窗口两者都满足。 |
| **Flower forest edge distance** / **繁花森林离边距离** | Off by default, distance field `0`. When checked and filled, both flower-gradient ends (dandelion / lily of the valley) must lie inside the window centre — at least that many blocks from every edge of the demand window. / 默认不勾选，距离填 `0`。勾选并填写后，繁花的 2 个花梯度极端（蒲公英/铃兰）都须落在中心区内，即距要求区域四边均不小于该距离。 |

Verified windows are **de-duplicated by window overlap**: after ranking, a window that intersects an already-kept window is dropped, so every returned result occupies a disjoint area.

校验通过的窗口按**窗口是否重叠**去重：排序后，与已保留窗口相交的窗口会被丢弃，因此返回的窗口互不重叠。

## Search Semantics / 搜索语义

Native search (`flowerSearch`) runs in these phases, in this order:

原生搜索（`flowerSearch`）按以下阶段执行：

1. **Phase 1** — CTS climate screen: builds the coarse E/H field and sweeps the search area (erosion + humidity boxes). The field rows are part of this phase, so the bar starts at 0 and rises from the first row.
2. **Phase 2** — Merge/cluster of the screened candidates, reported as "merged candidates processed / candidates to merge" (hit counts, not scan units). The bar starts at 0 when the merge begins.
3. **Phase 3** — For each candidate: overworld biome check + flower-noise extremes + nether biome under the window. The bar starts at 0 for this phase.
   Candidates are verified **in parallel** with the same thread count as phase 1, so this phase scales with cores.

1. **阶段 1** — CTS 气候粗筛：构建粗 E/H 气候场并遍历搜索区（侵蚀 + 湿度盒）。粗场行数计入本阶段，因此进度条从 0 开始、从第一行起就推进。
2. **阶段 2** — 合并/聚类粗筛得到的候选窗口，进度按“已整理候选数 / 待整理候选数”显示（候选计数，不是扫描单位）；开始时从 0 计。
3. **阶段 3** — 对每个候选点做主世界群系校验、花卉噪声极值、以及窗口下方地狱群系；本阶段同样从 0 开始。
   候选点使用与阶段 1 相同的线程数**并行**校验，因此该阶段随核心数加速。

Fast mode trades a little completeness for speed: the coarse field uses 128-block cells (precise mode: 64-block) and the window border is sampled every 16 blocks (precise mode: 8). Fast mode can therefore return slightly fewer windows than precise mode.

快速模式用少量完整性换速度：粗气候场使用 128 格（精确模式为 64 格），窗口周界按 16 格采样（精确模式为 8 格），因此快速模式可能比精确模式少返回少量窗口。

Phase 3 usually dominates the runtime (it re-samples a whole window per candidate), so both the thread slider and the CPU matter most there. Results are merged and then sorted with a total order, so the output is identical regardless of thread count.

阶段 3 通常是耗时大头（每个候选都要重采一整个窗口），因此线程数与 CPU 性能主要影响这一阶段。结果先合并再按全序排序，所以输出与线程数无关。

Acceptance highlights:

通过条件要点：

- Window must contain **flower forest** + **swamp**; on `1.21.4+` also **pale garden**.
- `flowerArea` must be **> 2000** (4×4 cells × 16).
- Flower noise must have both gradient ends (dandelion / lily of the valley) within **32** vertical blocks.
- With **Flower forest edge distance** set, both gradient ends must also sit in the window centre (≥ distance from every edge).
- Score prefers pink petals, then basalt deltas / wood bonus / cherry, then closer to world origin.

- 窗口须含**花林** + **沼泽**；`1.21.4+` 还须含**苍白花园**。
- `flowerArea` 须 **> 2000**（4×4 格 × 16）。
- 花卉噪声须在竖直 **32** 格内同时出现两端（蒲公英 / 铃兰）。
- 勾选**繁花森林离边距离**时，两端还须落在窗口中心区内（距四边均 ≥ 该距离）。
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

- Multi-threaded climate screen **and** candidate verification
- Pause / resume / stop with live progress and ETA
- Batch search from seed list files
- Sort and export results

- 多线程气候粗筛与候选校验
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
- Phase 3 (verification) is parallel, so raising the thread count still helps after the climate screen finishes.
- On load the native library sets `GOMP_SPINCOUNT=0` / `OMP_WAIT_POLICY=passive` unless you already set them. This stops idle OpenMP workers from spinning into the serial tail (sort / de-dup) and the UI; it costs nothing because the search uses only a few long parallel regions.
- The coarse climate field is counted inside phase 1 (field rows + window sweep), so every phase starts its progress at 0 and no phase ends on a tile-dependent tail.
- Fast mode may return slightly fewer windows than precise mode (coarser field and border sampling). Use precise mode when completeness matters more than speed.
- Native libraries (`dll` / `so` / `dylib`) are packaged in the JAR and extracted to a temp directory at runtime.
- To refresh the Windows native lib: rebuild with `jni/build-jni.bat` (overwrites `native/windows/libFlowerFarmFinderLibJ.dll`).

- 建议按 CPU 能力设置线程数以获得更稳定体验。
- 阶段 3（候选校验）已并行化，因此气候粗筛结束后提高线程数仍然有效。
- 原生库加载时会设置 `GOMP_SPINCOUNT=0` / `OMP_WAIT_POLICY=passive`（若你已设置则保留你的值）。这样 OpenMP 空闲线程不会在串行收尾（排序 / 去重）和界面线程上自旋抢 CPU；本搜索只进入少数几个长并行区，关闭自旋几乎没有代价。
- 粗气候场的工作计入阶段 1（粗场行数 + 窗口扫描），因此每个阶段的进度都从 0 开始，也不会再出现由最慢瓦片决定的阶段尾。
- 快速模式可能比精确模式少返回少量窗口（粗场与周界采样更粗）。更看重完整性时请使用精确模式。
- 原生库已打包进 jar，运行时解压到临时目录加载。
- 更新 Windows native 库：运行 `jni/build-jni.bat`（覆盖 `native/windows/libFlowerFarmFinderLibJ.dll`）。

---

**Find better full flower farm sites, faster.**
**更快地找到更优质的全花农场点位。**

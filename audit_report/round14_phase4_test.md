# 第十四轮 Phase 4 测试与质量回归报告

执行人：测测（android-test）  
执行时间：2026-11-24（JDK 17 / Gradle wrapper 9.3.0 / `--offline`）  
任务来源：主理人第 3 项「检查各个游戏、功能是否正常」之测试证据

### 测试时刻与源码状态（主理人要求注明）

- **最后一次全量重跑：2026-11-24 10:25**，基于当时工作区最新源码，含 `testDebugUnitTest` + `assembleDebug` + 五个校验脚本。
- 关键文件时间戳：`DateNightFragment.java` 10:09:48（悠悠已修编译错误）；`AgeGateDialogFragment.java` 10:16:55、`TonightNoteDialogFragment.java` 10:17:14（P0 弹窗按钮修复）；`values/strings.xml` 10:22:37（张力增强中）；`assets/content/**` 由源源持续改写。
- 因此本报告数字**不是**编译错误修复前的快照，已确认在修复后源码上重跑。
- 但 `ui/**` 与 `strings.xml` 仍处于专家活跃改写窗口；按主理人要求，**待所有专家定稿后还需再跑一次最终回归**，本轮数字为阶段证据。

---

## 0. 结论摘要

| 项目 | 结果 | 说明 |
|---|---|---|
| 全量单元测试 | **29 类 / 161 用例 / 0 失败 / 0 错误 / 0 跳过** | 基线 26 类 131 例 → 新增 3 类 30 例；修复编译错误后复跑确认 |
| 当前工作区可编译性 | ✅ 可编译 | 中途发现的 `DateNightFragment.java:138` 编译错误已由悠悠修复并复验 |
| 校验脚本 | 4 绿 / 1 红 | `verify_content` 已由源源修复至 EXIT=0；`check_compliance` 仍 EXIT=1（阻断 1290 / 复核 1325） |
| Debug 构建 | ✅ BUILD SUCCESSFUL | `./gradlew :app:assembleDebug --offline` |
| Release 构建 | ✅ BUILD SUCCESSFUL | 跳过 lint 三连（离线预期，`PITFALL-023`），EXIT=0 |

**重要口径**：本报告只证明「纯逻辑层单测通过」与「脚本实测数字」，**不声称单测证明了 36 个模式或任何 UI 功能在真机上正常**。UI 端到端由稳稳（android-runtime）用模拟器验证。

---

## 1. 单元测试

### 1.1 精确数字

```
命令：./gradlew :app:testDebugUnitTest --offline
结果：BUILD SUCCESSFUL（悠悠修复编译错误后复跑确认）
测试类数 = 29（基线 26 + 新增 3）
用例总数 = 161（基线 131 + 新增 30）
失败数 = 0
错误数 = 0
跳过数 = 0
```

证据：`app/build/test-results/testDebugUnitTest/TEST-*.xml` 汇总统计。

### 1.2 新增测试类与用例

**A · `core/session/DateNightSessionEngineTest`（13 例）**

| 用例 | 覆盖点 |
|---|---|
| `warmUpDrawsFourThenAdvancesToDeepen` | **markDone 阶段推进 bug 回归**：WARM_UP 抽满 4 张后必须变为 DEEPEN |
| `allFourStagesReachableInOrder` | 13 张严格按 WARM_UP×4 → DEEPEN×4 → PEAK×3 → CLOSING×2 |
| `totalCardsEqualToThirteen` | 会话恰好 13 张结束，结束后 `drawNext()` 返回 null |
| `peakStageHasNoCountdown` | PEAK `durationSec=0`、`isTimed()=false` |
| `stageProgressMatchesEnumCardCount` | 进度/总数与枚举一致；`SESSION_KEY="DATE_NIGHT"` 独立于模式目录 |
| `hardNoTagIsNeverDrawn` | HARD_NO tag 全局阻断，池内多数卡含该 tag 也绝不放宽 |
| `tierRespectsStageMaxAndPlayerMax` | tier ≤ min(stage.maxTier, playerMaxTier)，回退不抬高上限 |
| `preferredTagsAreHonored` | 偏好池充足时不抽无偏好 tag 的卡 |
| `recentShieldPreventsImmediateRepeat` | 会话内 13 张 ID 全不重复 |
| `poolEmptyFallsBackWithoutNull` | 主池空时先降 tier 再降 tag 回退到 ≥4 张，返回非 null |
| `fullyEmptyPoolReturnsNullWithoutException` | 全局无卡返回 null，由 UI 降级，不抛异常 |
| `midSessionExitDoesNotCorruptState` | 中途 `finishSession()` 后不再产卡、计数不被篡改 |
| `finishSessionReportsCountsForBothPartners` | A/B 完成数一致，跳过数正确 |

回归点说明：修复前 `markDone()` 只判断 `stageDrawCount >= stage.cardCount` 却不调用 `advanceStage()`，导致 WARM_UP 抽满后阶段永不推进。`warmUpDrawsFourThenAdvancesToDeepen` 在修复版上通过，可直接锁死该行为。

**B · `core/profile/AgeGateManagerTest`（7 例）**

首启未确认且成人向内容不可见、confirm 跨实例持久化、decline 标志持久化、软门禁下 heatRating≤2 模式可达且模式总数仍为 36、重复 confirm/decline 幂等、SP 文件名 `velvet_age_gate` 与两个布尔键契约、读/写异常时 fail-closed。

**C · `core/journal/TonightNoteManagerTest`（10 例）**

空态读取、写入往返、**同日覆盖写不追加（键数不变）**、不同日期独立且倒序、1~5 心情值保留、越界值处理、日期与 `TogetherDateFormat.toIso` 一致、非法日期不写半截键、文本 trim + 140 字截断、LiveData 保存后更新。

### 1.3 测试基建（无新增依赖）

工程 `testImplementation` 只有 JUnit 4，无 Robolectric / Mockito。为驱动依赖 `Context` 的 Manager，新增纯 JVM 测试支持类（均在 `app/src/test/**`）：

- `FakeSharedPreferences`：实现 SP 接口，`commit()` 原子写回；可注入读/写异常验证 fail-closed。
- `FakeContext`：继承 `ContextWrapper`（复用其已实现的全部抽象方法），仅覆写 `getApplicationContext()` / `getSharedPreferences()`。
- `JvmTaskExecutorRule`：临时把 `ArchTaskExecutor` delegate 换成同步、`isMainThread()=true` 的假实现，解决 `LiveData.setValue()` 触发 `Looper.getMainLooper()` 桩异常；测试结束即恢复，不污染其它测试类。

全程遵守 JUnit 4 断言参数顺序 `assertTrue(msg, cond)` / `assertEquals(msg, expected, actual)`（`PITFALL-029 / ADR-013`）。

### 1.4 规格偏差（需裁决，未擅自改业务代码）

`TonightNoteManager.clampMood()`（第 113~116 行）实际语义为「1~5 原样保留，越界值返回 -1（未标）」，与 `MoodCheckinManager.clampMood()`（第 95~97 行 `Math.max(1, Math.min(5, score))`，真正 clamp 到 1~5）**不同**。主理人任务卡写「复用 `MoodCheckinManager.clampMood`」且「心情标记 1~5 clamp」。

- 支持当前实现：`TonightNote.java:22` 与 `saveNote` javadoc 均明确 `-1 表示未标`，产品上「只写一句话不标心情」合理。
- 支持规格字面：任务卡明确要求 clamp，且两处同名方法语义漂移易踩坑。

我按「当前实现 + 文档」写断言并全部通过，**不把它伪装成 bug，也不迁就通过**；请架架与主理人裁决是否需统一为 clamp 到 1~5。若裁决为 clamp，需改 `TonightNoteManager.java:113-116`，我会同步改 `moodScoreOutOfRangeBecomesUnset` 断言。

---

## 2. 内容校验脚本（5 项，实测 EXIT 与命中数）

| 脚本 | EXIT | 命中 / 摘要 |
|---|---|---|
| `check_strings_mix.py` | **0** | `OK: no script contamination in string resources` |
| `sl_lint.py` | **0** | `OK: no script contamination in sealed-letter strings` |
| `dp_lint.py` | **0** | `OK: no Latin contamination in CJK strings` |
| `verify_content.py` | **0** | ✅ 全部通过（复跑；首轮曾 49 问题，源源修复后归零） |
| `check_compliance.py` | **1** | **阻断级 1290 / 复核级 1325 / 串词简繁回归 0**；扫描 8 语言、strings.xml 5472 条、内容 JSON 104 文件 / 139209 个字符串值（10:25 最新复跑） |

`verify_content.py` 首轮失败（**现已消除**）：
- 首轮 49 问题：`ID_MISSING` 7 项（`fate_cubes.json` 在 zh / zh_tw / ja / ko / es / fr / pt 七语种各缺 3 个 ID）+ `PLACEHOLDER_MISMATCH` 42 项（多语言模板相对 en 多出 `punishment_twist` 占位符，如 `gen_hot_03`、`gen_explore_08/12/13`、`gen_flirty_09`、`ma_5` 等）。
- 源源（android-data）完成 `assets/content/**` 改写后复跑 **EXIT=0 / ✅ 全部通过**，`AC-G05` 的 ID_MISSING=0、PLACEHOLDER_MISMATCH=0 已达成。本测试**未修改其任何文件**（`PITFALL-011 / PITFALL-014 / ADR-012`）。

`check_compliance.py` 仍 EXIT=1：命中以 RED-C（暧昧 / naughty / steamy）、RED-P（Synced / synchronize）为主；`check_strings_mix` / `sl_lint` / `dp_lint` 三脚本同时为 0，说明并非串词回归。命中数由首轮阻断 1467 / 复核 1339 降为 10:25 复跑阻断 1290 / 复核 1325，表明词表正在收敛中（本轮新落盘脚本，见 `round14_tension_spec.md` §词表归属 v1.1 裁决 / 安安 SEC-1）。判断为**词表未收敛**而非新增内容质量回归；仅记录命中数，不修改脚本。该脚本非历史四脚本基线项，不阻断本轮单测与构建结论。

---

## 3. 构建验证

| 命令 | EXIT | 结果 |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --offline` | 0 | ✅ BUILD SUCCESSFUL，29 类 / 161 例全通过 |
| `./gradlew :app:assembleDebug --offline` | 0 | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:assembleRelease --offline -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease` | 0 | ✅ BUILD SUCCESSFUL（4m16s，R8 正常执行） |

过程中的阻塞与闭环（质量证据）：

```
首轮 assembleDebug 失败：
app/src/main/java/com/velvet/couplesgame/ui/session/DateNightFragment.java:138
错误: 从 lambda 表达式引用的本地变量必须是最终变量或实际上的最终变量
    segments[i].requestLayout();
             ^
```

根因：`applyStageSpacing()` 中 `for (int i = 1; ...)` 的 `i` 在循环更新表达式中变化，不是 effectively final，却被 `anim.addUpdateListener(a -> {...})` 捕获。建议修法已 SendMessage 给悠悠：循环内声明 `final TextView segment = segments[i];`，lambda 改用 `segment.requestLayout()`。

该文件在 10:05:25 被并发修改引入错误、10:09:48 修复；我随即复跑 `testDebugUnitTest` + `assembleDebug`，均 BUILD SUCCESSFUL。**本测试全程未改动任何 `app/src/main/**`**。此案例与 `PITFALL-024`（资源/源码改动致编译阻断）同类，已沉淀给 android-knowledge。

Release 离线跳过 lint 属预期（`PITFALL-023`），不得把 lint 缓存缺失误判为构建失败；本次 Release 跳过三连后 EXIT=0。

---

## 4. 覆盖边界声明（需求第 3 项证据范围）

**单测已证明**：
- 约会夜引擎的纯逻辑：阶段推进（含历史 bug 回归）、13 张总量、抽卡约束（tier / HARD_NO / 偏好 tag / shield）、池空回退与 null 约定、会话结束状态。
- 年龄门禁的本地存储语义：首启 fail-closed、confirm/decline 持久化与幂等、软门禁不改变模式目录（仍 36 项、温和模式非空）、读写异常不误放行。
- 今晚一句话的本地存储语义：SP 分键读写、同日覆盖写、日期与心情边界、文本裁剪、LiveData 更新。

**单测未证明、交稳稳（android-runtime）模拟器端到端**：
- 36 个模式逐一可达 / 可返回 / 折叠展开（单测只验证目录仍 36 且 decline 后温和模式非空，不验证 UI 可达性）。
- `AgeGateDialogFragment` 首启**弹窗时机**是否早于任何成人向内容呈现（需 Activity 生命周期，单测无法覆盖）。
- `DateNightFragment` / `TonightNote` 系列 Fragment 的渲染、点击、导航、横竖屏与 8 语言布局。
- 真机稳定性、性能、FLAG_SECURE、截图验证（`PITFALL-019 / 020 / 021`）。
- 文案调性 AC-701~703（须机器词表 + 人工抽检，非单测范畴）。

---

## 5. 规范与风险引用

| 编号 | 与本轮关系 |
|---|---|
| AC-101~107 | Phase 1 基准/阶段门/落地门禁；AC-106「不引入新权限/网络/SDK」由安全域静态扫描负责，单测不替代 |
| AC-701~703 | 8 语言私密调性与四档热度梯度，单测不覆盖，交 data/ui/security |
| AC-G05 | 8 语言 key 集合一致 + 13 类 × 8 语言 ID 一致（ID_MISSING=0、PLACEHOLDER_MISMATCH=0）；当前 `verify_content` EXIT=1，未达成 |
| [API-008] | 约会夜抽卡与 null 降级约定，已由 13 例覆盖 |
| [API-009] | 年龄门禁本地布尔存储与 fail-closed，已由 7 例覆盖 |
| [API-010] | 今晚一句话 SP 分键范式，已由 10 例覆盖 |
| PITFALL-029 / ADR-013 | JUnit 4 断言参数顺序、assets 相对路径 |
| PITFALL-011 / 014 / ADR-012 | ID 对齐与 `CONTENT_FILES`，对应当前 ID_MISSING |
| PITFALL-007 | 占位符不一致历史坑，对应当前 PLACEHOLDER_MISMATCH |
| PITFALL-022 / ADR-006 | Gson 字段 R8 改名；C 项采用 SP 分键范式规避，本轮未引入 Gson 落盘模型 |
| PITFALL-023 | Release 离线跳过 lint，非构建失败 |
| ADR-010 / PITFALL-024 | 改动后必跑四脚本 + 全量单测 + 构建；资源/源码改动可致编译阻断 |

---

## 6. 闭环状态与剩余动作

已闭环：
1. ✅ 悠悠修复 `DateNightFragment.java:138` 编译错误，我复跑单测与 Debug 构建全绿。
2. ✅ 源源完成 `assets/content/**` 改写，`verify_content.py` 复跑 EXIT=0。
3. ✅ 全量单测 161/161、`assembleDebug` EXIT=0、`assembleRelease`（跳过 lint 三连）EXIT=0。

剩余（非本测试职权）：
4. 安安 / 筑筑继续收敛 `check_compliance.py` 词表（10:25 仍 EXIT=1，阻断 1290 / 复核 1325）。收敛后我可复跑更新数字。
5. 架架 / 主理人裁决心情标记 clamp 语义偏差（见 §1.4）。
6. 稳稳（android-runtime）完成 UI 端到端、两个 DialogFragment P0 弹窗复验与 36 模式可达性验证（见 §4 覆盖边界）。
7. **所有专家定稿后按主理人指令再跑一次最终回归**，以最终源码刷新本报告数字与时刻。

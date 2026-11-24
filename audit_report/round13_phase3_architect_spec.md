# VelvetCouples 第 13 轮 · Phase 3 架构实现规格（A / B / C 三项补充内容）

> **子标题**：第 1 项补充内容的实现规格 —— 引导式约会夜串场 / 18+ 年龄确认 / 今晚一句话会话日记
> **作者**：架架（android-architect）
> **下发**：铮铮（android-studio-expert-team-lead），Phase 3 续接任务
> **日期**：2026-11-23
> **基准来源**：本报告的判定基准 = `audit_report/round13_phase1_architect.md`（**v1.1**，含 §2.2 的 47 条 AC、§4 的 P1~P7 / N1~N7 / T1~T4 / TONE-CHK 编号、§6 模块边界、§7 API-001~API-007）
> **红线来源**：`audit_report/round13_phase1_security.md`（安安）—— 本规格与其 §2.1 `RED-A`、§2.2 `RED-B`、§2.3 `RED-C`、§2.4 `RED-D`（含 2.4.3 存量清单）、§2.5 `RED-E`、§3.2 / §3.4 `RED-P-1` **逐条对齐，不得冲突**
> **知识来源**：`audit_report/round13_phase1_knowledge.md`（识识）—— §3.3 第八轮销账表、历史 ADR-001/002/014
> **硬约束**：**本规格只写规格，不写代码**。架架不修改任何代码文件；`res/layout/**` 与 `res/values*/strings.xml` 归【悠悠 android-ui】，`assets/content/**` 与 `scripts/gen_*.py` 归【源源 android-data】。

---

## 0. 主理人裁决摘要与本规格的承接方式

| 裁决 | 内容 | 本规格的承接 |
|---|---|---|
| **Q1** | 分级口径统一 **18+**；两处文档同步由 android-release 在 P5 落实 | 影响 B 项年龄确认文案语义（§2.4）与 A 项 `heat_scope_fmt` 语境；本规格**不涉及文档修改** |
| **Q2** | Play 政策适用性**法务判定，工程不等**；取**最严路径**：存量动作化文案一律降调 | §4 存量降调约束清单；架架给出**改写约束**（不改写文案本身） |
| **Q4** | 存量降调范围**全部纳入本轮**，共 5 组 | §4.1 五组逐条约束 + §4.2 额外发现（`paywall_benefits` 数字过时） |
| **Q7** | 内容有实质改动 → **须重填 IARC 问卷**，由 android-release 在 P5 判定并执行；**改写专家不得自行判断** | 写入 §4.3 与 AC-706；本规格明确标注「改写专家不得自行判断是否触发重填」 |
| **Q6** | Keystore 明文降级路径**不由架架出方案**，由 android-security 在 Phase 4 出技术方案、android-data 落地 | **本规格不涉及此项**，仅在 §4.4 标注为「他人负责，架架不越权」 |
| **落地范围** | A / B / C **三项全部落地**；A 采用「主页会话入口」形态，**不新增 `GameModeCatalog` 项**（保持 36） | §1.1 / §1.6 / §4.5 明确约束 |
| **第 7 项张力** | **刺激感只能由情绪密度、节奏与留白实现，绝不由动作清单实现**；T4 与 T3 的差别是情绪密度 | §1.2 阶段设计 + §3 全部 strings 语义描述 + §5.3 文风范式基准 |
| **第 5 组降调边界** | `mode_ldr_sync` 等**只改显示文案，不改 id / 枚举 / TagDisplayMapper 键** | §4.1 第 5 组显式标注「键与 id 不动」 |

---

## 1. A 项实现规格 —— 引导式「约会夜」串场

### 1.1 定位与形态（落实「主页会话入口，不新增 GameModeCatalog 项」）

- **产品形态**：用户在**主页**（`fragment_home.xml`）上，通过一张新卡片进入「今晚的约会夜」，由 App 编排**从暖场到收尾**的一整段双人流程，而不是再增加第 37 个并列的游戏模式。
- **`GameModeCatalog` 不变**：保持 **36 项**（VIP 12 / 免费 24，实测值）。因此 **不触发** Phase10/Phase12 的硬编码数量断言（历史 ADR-014：`36 · 24 · 12`）。本项**不新增枚举值**。
- **统计键独立于 `GameModeCatalog`**：会话在 `session_stats` 中的键为 **`"DATE_NIGHT"`**（新增字符串常量，非枚举）。这与历史 `SEALED_LETTER` / `LOVE_GRAPH` 的做法一致（它们也各自是 `GameModeCatalog` 项，但 `DATE_NIGHT` **不是**）。
- **`TagDisplayMapper` 必须新增映射**（否则踩历史第八轮「fallback 显示情侣棋局」坑）：
  ```
  if ("DATE_NIGHT".equals(gameModeKey)) return R.string.mode_date_night;
  ```
  → 需要 **1 个新 string key `mode_date_night`**（8 语言）。**注意**：`TagDisplayMapper.resolveGameModeRes()` 只是查表，**新增一条 `if` 不改变模式总数**，不影响任何数量断言。

### 1.2 Phase 机状态定义（4 阶段）

> **核心设计原则（安安 RED-D / 主理人裁决）**：阶段推进靠**情绪密度**与**节奏**，不靠动作升级。T1→T4 的可感知差异是「留白更多、心跳更近、语言更轻、更快到下一个悬念」，**不是**「动作更具体」。

| 阶段 | 枚举 | 情绪目标 | 允许的 `heatRating` 档 | 抽卡数 | 语言节奏 | 阶段提示语 P 特征要求 |
|---|---|---|---|---|---|---|
| **WARM_UP 暖场** | `WARM_UP` | 轻松、破冰、安全 | tier ≤ 2 | 4 | 短句、留白多、疑问句为主 | P1 / P2 / P3 / P6 |
| **DEEPEN 深入** | `DEEPEN` | 靠近、试探、话题变私密 | tier ≤ 3 | 4 | 半句收尾、感官词增加 | P1 / P2 / P3 / P4 / P6 |
| **PEAK 高点** | `PEAK` | 心跳感最强、悬念最强 | tier ≤ 4 | 3 | 省略号密集、节奏碎、留白最大 | P1 / P2 / P3 / P4 / P5 / P6 / P7（**P7 必选**） |
| **CLOSING 收尾** | `CLOSING` | 回落、温柔、留余韵 | tier ≤ 2 | 2 | 长句、舒缓、低刺激 | P1 / P2 / P3 / P6 / P7 |

**阶段参数表（建议初值，可由 Phase 3 调参，不改结构）**：

```java
// 纯 Java，无 Android 依赖
public enum Stage {
    WARM_UP(2, 4, 0),   // (maxTier, cardCount, durationSecPerCard)
    DEEPEN (3, 4, 0),
    PEAK    (4, 3, 0),   // PEAK 不内置计时，改为「共同决定何时继续」
    CLOSING (2, 2, 0);
}
```

- **总卡数 13 张**（4+4+3+2），单次会话时长可控。
- **PEAK 阶段刻意不设倒计时**：安安 `RED-B3`「明确可随时停止」与主理人「情绪密度」裁决下，PEAK 由双方自行决定何时继续，避免「计时器把节奏变成任务」的压迫感（对应 N3 命令式）。

### 1.3 每阶段抽卡策略（复用 `SocialGamesEngine` 的防重复机制）

**复用点与新增点的边界**：

| 能力 | 复用来源 | 说明 |
|---|---|---|
| `ConsentFilter.isAllowed(tags)` | ✅ 直接复用 | 任一方 HARD_NO 的标签**全局阻断**，与 `SocialGamesEngine.loadPool()` 逻辑一致 |
| `EntitlementManager.getSelectedMaxTier()` | ✅ 直接复用 | 与 `SocialGamesEngine.loadPool()` 的 `playerMaxTier` 同源，保证 VIP 档位一致 |
| `ContentRepository.loadMergedPoolAsync` | ✅ 直接复用 | 官方卡池（8 语言 decks 874 卡），**不新增题库 JSON**（历史 ADR-002） |
| `MAX_RECENT_SHIELD=12` / `MIN_AVAILABLE=4` | ✅ 语义复用 | 本引擎自带同值常量与**同款算法**（见 §1.4） |
| 阶段 → tier 上限 | 🆕 新增 | `SocialGamesEngine` 的 `tierCap` 是 per-mode 静态值；串场需要 per-stage 动态值 |
| 阶段 → 偏好 tag | 🆕 新增 | per-stage 偏好，语义见下表 |
| 跨阶段不重复 | 🆕 新增 | `SocialGamesEngine` 的 `recentIds` 是**单 mode 内**的去重；串场跨阶段需**会话级**去重 |

**per-stage 偏好 tag 建议**（沿用既有 tag 词表，**不新增标签**）：

| 阶段 | 偏好 tag | 理由 |
|---|---|---|
| WARM_UP | `deep_talk`, `playful`, `eye_contact` | 破冰、轻松、可开口 |
| DEEPEN | `deep_talk`, `physical_light`, `roleplay` | 话题变私密，但仍属「轻」 |
| PEAK | `physical_light`, `bite_lick`, `roleplay` | 依安安 `RED-D` T3/T4 档位口径；**注意**：这些 tag 的卡内文本需先过 §4 降调，**不因本项而放宽** |
| CLOSING | `deep_talk`, `eye_contact` | 温柔回落 |

> **重要约束**：`bite_lick` / `restraint_light` / `blindfold` 等标签的**存量风险文本**由 §4 降调与 Phase 4 全量扫描处理（安安 §2.4.3 最后一行）。**本项不得新增同类标签或文本**，也不得因为 PEAK 阶段而放宽 `ConsentFilter` 的 HARD_NO 阻断。

**抽卡主流程（每次请求一张卡）**：

```
1. cap = min(stage.maxTier, entitlementManager.getSelectedMaxTier())
2. candidates = pool 中 tier ≤ cap 且 ConsentFilter.isAllowed(tags) 为 true 的卡
3. candidates = candidates 中至少命中 1 个 stage.preferredTags 的卡
4. 若 candidates 为空 → 【降档回退】，见 §1.6 错误与边界
5. 交叉过滤：剔除会话级 recentIds 中最近 shieldSize 张（shieldSize = min(12, max(0, poolSize-4)))
6. 若过滤后为空 → 用被 shield 的 fallback 池（与 SocialGamesEngine 同样语义）
7. 在 unseen（本会话未见）子集中随机；unseen 为空则用全池
8. 记录到会话级 recentIds；记录到 seenThisSession
```

### 1.4 防重复常量（与 `SocialGamesEngine` 对齐，注释需指明来源）

```java
private static final int MAX_RECENT_SHIELD = 12;  // 与 SocialGamesEngine 同值（第六轮 ADR-002）
private static final int MIN_AVAILABLE = 4;       // 与 SocialGamesEngine 同值：始终保留 4 张可用卡
```

> **本项不修改 `SocialGamesEngine`**（避免影响既有 12 个 social 模式与 `Phase16SocialGamesTest` 8 条断言）。新引擎自带常量并注明同源。

### 1.5 复用点清单（明确「不重写什么」）

| 组件 | 复用方式 | 禁止事项 |
|---|---|---|
| `SocialGamesEngine` | **只读参考其算法**，不修改其源码 | 不改 `ModeConfig` / 不加 mode id |
| `DareTimerHelper.start(int durationSeconds, Listener)` | ✅ 直接复用 | — |
| `HapticHelper.countdownFinished(View)` / `tick(View)` / `confirm(View)` | ✅ 直接复用（第三轮已扩展） | — |
| `PassPhoneDialog` | ✅ 直接复用（递手机换人） | — |
| `PhysicsAnimator.flipCard3D` | ✅ 直接复用（若需翻卡动效） | — |
| `TaskPromptDialogHelper` | ✅ 直接复用（展示卡面） | — |
| `ContentRepository.loadMergedPoolAsync` | ✅ 直接复用 | — |
| `recordSessionStat(String, int, int, int)` | ✅ 直接复用 | — |
| `ConsentFilter` | ✅ 直接复用 | — |

### 1.6 错误与边界（明确可验收）

| 场景 | 处理 | 可验收口径 |
|---|---|---|
| **池空**（该阶段无任何合规卡） | **降档回退**：依次尝试 tier-1 → tier-2 → 最低可用档；每档至少保留 **≥4 张**可用卡；若全局仍空 → 展示「今晚先聊聊天」类的**降级提示**并允许跳过该阶段，**不得崩溃、不得空指针** | 构造「ConsentFilter 全 HARD_NO」的极端场景单测：不崩、不返回 null、会话正常结束并记账 |
| **ConsentFilter 拒绝后换阶段档位** | 抽卡时若候选被 `isAllowed` 过滤至空，**先降 tier 再降偏好 tag**，最后才放弃该阶段；**绝不**通过放宽 HARD_NO 来"凑"卡 | 单测：注入 HARD_NO 标签，断言该标签卡永不出现在 `drawNext()` 结果中 |
| **用户中途退出** | 记账口径：**已完成张数**计入 A/B 完成数（沿用 `SocialGameFragment` 的 `completedSinceLastWrite` + `onDestroyView()` 兜底 flush 范式，历史第八轮）；**未完成的阶段不记完成**，但**已抽过的卡不因退出而重置**（会话级 recentIds 落盘于本次会话内存即可，不需跨会话持久化） | 单测：抽 5 张、完成 3 张后退出 → `recordSessionStat("DATE_NIGHT", 3, 3, 0)`；完成 0 张退出 → 记 `(0,0,0)` 且**不写脏数据** |
| **VIP 未解锁** | `getSelectedMaxTier()` 决定上限；免费用户 PEAK 阶段**自动降为 DEEPEN 的 tag 偏好与 tier≤3**，且**不显示任何"解锁"诱导字样**（安安 `RED-A7`：禁止"付费换取性行为"式表述） | 单测：playerMaxTier=2 时，PEAK 阶段实际抽卡 tier ≤ 3 |
| **重复启动同一会话** | 每次进入生成新会话实例；会话内 `seenThisSession` 保证不重复 | 单测：同会话连抽 13 张，断言无重复 id |

### 1.7 主页入口 UI 形态描述（**不写布局 XML**）

> 归属【悠悠 android-ui】独占 `res/layout/**`。以下仅为**形态规格**。

- **位置**：主页现有卡片序列（`card_daily_reveal` → `card_mood_checkin` → `card_milestones` → `card_date_plan` → `card_letter` → `card_love_graph`）之后，**追加一张新卡片**。
- **卡片三件套范式**（沿用既有约定）：
  - `card_date_night`（卡片容器）
  - `tv_date_night`（主标题）
  - `tv_date_night_sub`（副标题，显示当前阶段 / 上次完成情况）
  - `btn_date_night_open`（主按钮，文案随状态变化：**未开始 → 开始今晚 / 进行中 → 继续 / 已完成 → 今晚已完成**）
- **视觉与交互**：
  - 复用 `bg_card_velvet`（既有 velvet 卡片背景），**不新增颜色资源**（12 套主题由 `AppTheme` 枚举驱动，避免主题回归）。
  - 卡片进入后 → 新 Fragment `DateNightFragment`（`ui/session/` 包）。
- **导航**：新增 1 个 `nav_date_night` 节点 + 1 个 `action_home_to_date_night` action（**新增导航节点不是新增模式**，不影响 36 计数与数量断言）。
- **会话内 UI 形态**（`fragment_date_night.xml`，归属悠悠）：
  - 顶部：阶段指示（4 段进度条 / 阶段名）
  - 中部：卡面区（复用 `TaskPromptDialogHelper` 的卡面展示样式；`tv_card_text` 风格对齐既有 `fragment_home.xml` 的 `tv_card_text`）
  - 底部：主行动按钮 + **「安全暂停」条**（复用既有 Pause / Safe Word 栏样式，**不得移除**）
- **⚠️ 与既有 UI 的一处冲突需悠悠注意**：首页 `card_preview_container` 已有 `tv_heat_explanation` 展示 `heat_scope_fmt`。该 key 的**「all 20 games / 20 款游戏」数字已过时**（实际 36 模式），且含 sync 歧义（§4.1 第 6 组）。**降调由悠悠在本轮一并处理**，本项不依赖该 key。

### 1.8 会话结束与统计

| 时点 | 动作 |
|---|---|
| 阶段全部完成 | `recordSessionStat("DATE_NIGHT", completedA, completedB, skipsUsed)`，其中 `completedA = completedB = 本会话完成张数`（沿用 `DailyReveal` / `SocialGameFragment` 的 A=B 语义） |
| 用户中途退出 | 在 `onDestroyView()` **兜底 flush**（历史第八轮 `SocialGameFragment` 同款修法），避免"退出即丢记录" |
| 统计键注册 | `TagDisplayMapper.resolveGameModeRes()` 新增 `"DATE_NIGHT"` → `R.string.mode_date_night`（**必须**，否则记忆档案 fallback 显示「情侣棋局」） |
| 模式计数 | `GameModeCatalog` **不动**，仍 36；`Phase10`/`Phase12` 断言**不动** |

---

## 2. B 项实现规格 —— 18+ / 成人向年龄确认

### 2.1 触发时机（裁决 Q1：口径统一 18+）

- **必须在任何成人向内容呈现之前**：`MainActivity` 启动 → 若 `ageGateManager.isConfirmed() == false` → **先**展示年龄确认弹窗 → **确认后才**进入主页（进而才可能触及 `card_preview_container` 的成人向卡面、paywall、VIP 模式）。
- **一次性**：确认结果持久化，之后不再拦截。
- **不复用 `OnboardingDialogFragment`** 的伴侣资料流程（那是「他/她/TA」+ Safe Word），年龄确认是**独立的前置门**，逻辑上必须早于它 —— 否则用户在填写昵称时已进入成人向应用语境。

### 2.2 存储

| 项 | 规格 |
|---|---|
| 介质 | **SharedPreferences**（与 `MoodCheckinManager` / `CoupleProfileManager` 同范式），独立 pref 文件名 |
| 内容 | 单个 boolean：`confirmed = true` |
| 加密 | **不需要**（非敏感数据；且 `allowBackup=false`，与 `RED-P` 数据面零外传一致） |
| 新增类 | `core/profile/AgeGateManager.java`（纯逻辑 + SP 读写，无 Android UI 依赖，可单测） |
| 写入时机 | 用户点击「确认」时立即写入（先写盘再关窗，避免进程被杀导致重复弹窗） |

### 2.3 不可绕过性 —— **架架建议**

> **建议：允许「拒绝」但拒绝后不提供成人向内容（软门禁），而非硬阻断整个 App（硬门禁）。**

| 方案 | 描述 | 评价 |
|---|---|---|
| ✅ **软门禁（建议采纳）** | 用户点「不符合」→ 记录 `declined = true` → **不进入成人向区域**（不可见 paywall、不可见成人向卡面、不可进入任何 `heatRating ≥ 3` 的模式），但**保留** `heatRating ≤ 2` 的温和模式与基础功能 | ① 满足 Play Families 政策「成人向定位明确」的要求 —— 不面向儿童目录；② 不把用户彻底挡在门外（Play 政策不要求阻断，**阻断反而可能被判为「虚假功能承诺」或影响审核**）；③ 保留转化路径，且未满 18 岁者不会接触成人向内容 |
| ❌ 硬门禁 | 点「不符合」→ 整个 App 不可用 | **不建议**：① 审核侧通常不要求硬阻断；② 可能被视为功能不实（商店页宣称 18+ 内容却完全不可用）；③ 无利于任何留存 |
| ❌ 不做年龄确认 | 直接沿用现状 | **不建议**：安安已指出 `PLAY_CONSOLE_CONTENT_RATING.md` §2.4 要求首启年龄确认，属**已知待办项**，不做则该待办无法关闭 |

- **裁决请求**：软门禁 vs 硬门禁属产品策略，**交 team-lead / 用户确认**（§7 待确认 #1）。架架给出建议与理由，不自行拍板。
- **若采纳软门禁**，需在**同一轮**内让 `heatRating ≥ 3` 的 16 个模式（3/4/5 火焰共 21 个：heat3=10, heat4=6, heat5=5）在 UI 层不可见 —— 属悠悠的 `GameModeAdapter` 过滤，**不删 `GameModeCatalog` 项**（保持 36）。

### 2.4 8 语言文案语义意图（≥2 条：**确认语 + 拒绝后的处理**）

> **不给最终文案**（悠悠撰写）。以下为「语义意图 + 必须命中的 P 特征 + 归属层级」。**本组文案不属于私密调性场景**，但仍须遵守 H-8（不得功能说明书式）与安安 `RED-B1`（成人向定位明确）。

| # | key | 语义意图 | 必须命中 | 禁止 | 梯度 |
|---|---|---|---|---|---|
| 1 | `agegate_title` | 标题：声明这是一个**面向成年人**的情侣互动应用 | 明确含「成年人 / 18 岁以上」之意 | 不得用「诱惑」「刺激」等调性词；不得暗示露骨 | 不适用 |
| 2 | `agegate_body` | 正文：一句话说明这是**仅限成年人**的情侣双人私密互动，使用者须自行确保年满 18 岁；不含任何露骨或挑逗措辞 | 「成年人」明确；简洁 | **禁止**任何身体 / 动作 / 性暗示词（安安 `RED-A2`/`A7`）；禁止功能罗列式（N4） | 不适用 |
| 3 | `agegate_confirm` | 确认按钮：「我已年满 18 岁，继续」 | 直接、肯定 | — | 不适用 |
| 4 | `agegate_decline` | 拒绝按钮：「我未满 18 岁」 | 直接、否定 | — | 不适用 |
| 5 | `agegate_declined_notice` | 拒绝后的提示：告知将仅保留基础功能，不展示成人向内容 | 说明事实 | 不得说教、不得指责 | 不适用 |
| 6 | `agegate_decline_note` | 补充说明：数据仍只留在这台设备上（**否定式**表述，允许且鼓励，见安安 §3.3 允许词表） | 否定式「不离开本机 / 不上传 / 无需账号」 | 禁止肯定式数据流措辞（安安 §3.2） | 不适用 |

---

## 3. C 项实现规格 —— 「今晚一句话」会话回顾日记

### 3.1 定位

- **历史闭环**：识识 §3.3 确认的未落地项 `U-03`「关系日记 / 时间轴」，本项是其**轻量化落地**（只做「每晚一句话」，不做完整时间轴 / 照片 / 导出）。
- **UGC 约束位（主理人已裁决）**：**仅本机、不上传、不预审、不分享、不导出**。
  - **不上传** → 符合安安 `RED-A6`、§3.1 数据面零外传；
  - **不预审** → 用户自填内容不做词表扫描（与 `CustomTaskEntity` 既有范式一致；该范式已在 IARC 问卷申报为「有限 UGC，无发布/分享」）；
  - **不分享 / 不导出** → 避免触发安安 §3.4 末条「新增分享/导出能力 → 必须重填问卷」。
- **与已有功能的差异**：`session_stats` 是**数值**、`MoodCheckinManager` 是**心情 + 感激**、`记忆档案` 是**统计列表**。本项是**双方各留一句自由文本**（情绪存档），定位不重叠。

### 3.2 数据结构

```java
// core/journal/TonightNote.java —— 纯 Java，无 Android 依赖
public class TonightNote {
    public String dateIso;        // yyyy-MM-dd（ISO_LOCAL_DATE），由 TogetherDateFormat.toIso() 产出
    public String noteA;          // 伴侣 A 的一句话（可为空串）
    public String noteB;          // 伴侣 B 的一句话（可为空串）
    public int moodScore;         // 心情标记，1~5，语义沿用 MoodCheckinManager 的 clampMood（1=最低，5=最高）
    public long createdAtMillis;  // 创建时间戳（本地排序用）
}
```

| 字段 | 约束 | 理由 |
|---|---|---|
| `dateIso` | **主键的一部分**，格式 `yyyy-MM-dd` | 与 `MoodCheckinManager` 的 `day_` 前缀键范式一致；`TogetherDateFormat.toIso()` 保证与既有日期口径一致（**注意其 UTC 锚定转换只用于 MaterialDatePicker**，本项按本地日 `LocalDate.now()` 生成 ISO，与 `MoodCheckinManager` 现有做法相同） |
| `noteA` / `noteB` | 允许空串（一方未写） | 双人可只由一方写；**空串不得视为「未写过」以外的语义** |
| `moodScore` | 1~5，默认 -1 表示未标 | 复用 `MoodCheckinManager.clampMood` 的语义与边界 |
| `createdAtMillis` | 本地时间戳 | 排序用；**不做任何网络时间同步**（安安 `RED-P`） |

> **不含字段**（明确排除，避免后续被"顺手加上"）：无 `id` 之外的可分享标识、无云端 id、无"公开"标记 —— 因为本项**不支持分享/导出**，加这些字段只会制造未来越界风险。

### 3.3 存储与写入时机

| 项 | 规格 |
|---|---|
| 存储介质 | **SharedPreferences**，独立 pref 文件名（与 `MoodCheckinManager` 的 `velvet_mood_checkin` 平行） |
| 键结构 | `day_<yyyy-MM-dd>` → JSON（复用 `SealedLetterManager` 的 Gson 落盘范式）；**或** 分键 `day_<date>_noteA` / `_noteB` / `_mood`（复用 `MoodCheckinManager` 分键范式） |
| **推荐** | **分键范式**（`MoodCheckinManager` 同款）—— 避免新增 Gson 模型带来的 **R8 混淆字段名**风险（历史第十一轮踩坑：Gson 字段名被 R8 改名导致升级后读不出数据） |
| R8 规则 | 若最终选 JSON 落盘，**必须**在 `app/proguard-rules.pro` 加 `-keep class com.velvet.couplesgame.core.journal.TonightNote { *; }`，并加反射校验测试（历史 `SealedLetterStoreContractTest` 同款） |
| 写入时机 | **① 会话结束回看页**（A 项 CLOSING 阶段结束后，或任一游戏结束后的「今晚一句话」入口）→ **② 独立打卡入口**（主页 `card_mood_checkin` 旁或设置页内） |
| 写入时机（防抖） | 同一天重复写入**覆盖当日记录**（每人每天一句话），不追加历史 |
| LiveData | `observeNotes(): LiveData<List<TonightNote>>`，按 `dateIso` 倒序 |

### 3.4 复用点

| 组件 | 复用方式 |
|---|---|
| `TogetherDateFormat.toIso(LocalDate)` | ✅ 生成 `dateIso`（与「在一起天数」口径统一） |
| `TogetherDateFormat.display(String, Locale)` | ✅ 本地化日期展示 |
| `MoodCheckinManager` 的 SP + LiveData + `clampMood` 范式 | ✅ 目录结构与心情分值语义 |
| `AppContainer` 注册 | ✅ 新增 `journalManager` 字段 |

### 3.5 呈现层级（规格描述，不写布局）

```
第 1 层：主页卡片（复用 card_* 三件套范式）
        ├─ 主标题：今晚的一句话
        ├─ 副标题：今日已写 / 今日还未写 / 最近一次是 X 月 X 日
        └─ 按钮：写一句 / 回看
第 2 层：今日编辑（弹窗或独立 Fragment）
        ├─ 两个输入区：A 一句 / B 一句（PassPhoneDialog 递手机，或同一屏上下两个）
        ├─ 心情标记：5 档单选（语义沿用 MoodCheckinManager）
        └─ 保存
第 3 层：历史列表（按日倒序）
        ├─ 每日一行：日期 + 心情点 + A/B 两句摘要
        └─ 点击展开全文
```

- **隐私姿态**：列表默认**折叠**（只显示摘要），需点击才展开 —— 与 `FLAG_SECURE` 防截屏叙事一致。
- **无"删除全部"以外的批量导出/分享**（安安 §3.4）。

### 3.6 8 语言新增 strings 的 key 清单与语义意图（约 8~10 条）

> **不给最终文案**（悠悠撰写）。「P 特征」列对本组文案的适用性做区分：**私密调性文案**适用 P1~P7；**功能性文案**只强制 P2（第二人称）与 H-8（不得说明书式）；**标题类**适用 P1/P3。

| # | key | 语义意图 | 语义层级 | 必须命中 | 禁止 | 归属 |
|---|---|---|---|---|---|---|
| 1 | `tonight_note_title` | 标题：今晚留一句话（**留白式**，不写「日记」「记录」） | 私密调性 | P1 / P3 | 不得用「日记 / 记录 / 心情报告」等冷冰冰功能词（N4/N6） | 悠悠 |
| 2 | `tonight_note_subtitle` | 副标题：只留在这台设备上、只有你们看得到（**否定式**数据流表述） | 功能性 | P2 / 否定式 | **禁止**肯定式「同步 / 上传 / 分享」（安安 §3.2） | 悠悠 |
| 3 | `tonight_note_prompt` | 输入引导语：今晚最想说的一句（**邀约式**，非命令式） | 私密调性 | P1 / P2 / P3 / P5 | 不得命令式（N3） | 悠悠 |
| 4 | `tonight_note_prompt_b` | B 方输入引导语的差异化变体（**不得与 A 方逐字相同**，避免机翻感） | 私密调性 | P1 / P2 / P3 | 同上 | 悠悠 |
| 5 | `tonight_note_mood_label` | 心情标记的组标题：此刻的心情（**第二人称**） | 功能性 | P2 | 不得罗列 5 档的直白说明（N4） | 悠悠 |
| 6 | `tonight_note_save` | 保存按钮 | 功能性 | — | — | 悠悠 |
| 7 | `tonight_note_empty_today` | 今日空态：今晚还没留下什么（**留白式**，不用「暂无数据」） | 私密调性 | P1 / P2 | 禁止「暂无 / 无数据 / null」（N4/N6） | 悠悠 |
| 8 | `tonight_note_history_title` | 历史列表标题 | 私密调性 | P1 / P3 | 不得用「历史记录 / 归档」（N6） | 悠悠 |
| 9 | `tonight_note_saved_toast` | 保存成功反馈：极简、温和 | 私密调性 | P2 | 不得「已成功保存到服务器 / 已同步」（安安 §3.2） | 悠悠 |
| 10 | `tonight_note_close_hint` | 关闭/收起提示 | 功能性 | — | — | 悠悠 |

> **共 10 条**（含 1 条 `mode_date_night` 归属 A 项，则 A + B + C 新增合计见 §6）。

---

## 4. 存量降调约束清单（承接主理人 Q2 / Q4；**架架只给约束，不写文案**）

> **归属【悠悠】**（`values*/strings.xml` 独占）。**架架不修改任何 strings.xml。**
> **口径**：安安 `RED-A2`（动作分解）、`RED-A3`（胁迫 / 惩罚）、`RED-A7`（意图满足性欲）、`RED-C`（唯一允许的六种手法）、`RED-D`（2.4.1 强度上限）、`RED-P-1`（sync 歧义）。
> **原则（主理人裁决 Q2「最严路径」）**：一律**降调**，不做"加码"；降调方向 = **感官 / 悬念 / 情绪密度 / 双人 / 可停**。

### 4.1 主理人指定的 5 组

| 组 | key（`values/strings.xml` 行号） | 现状（en / zh-CN） | 必须删除 | 降调方向 | 归属 |
|---|---|---|---|---|---|
| **1** | `tier_desc_3`（:247）、`tier_desc_4`（:248） | "Slow burn — soft licking, nibbling, neck/collarbone dares & lap tease." / "暧昧升温 —— 轻舔、轻咬耳垂锁骨、跨坐互动与深吻。" | `licking / nibbling / 轻舔 / 轻咬 / 跨坐 / 深吻 / lap tease` | 改为**感官 + 悬念 + 距离感**描述；**不得写动作动词**（安安 `RED-C` 2.3.2 C-ZH-2 的「越界」对照即取自本工程存量 `values-zh-rCN/strings.xml:246`） | 悠悠 |
| **1** | `tier_desc_4`（:248） | "After Midnight — bold physical punishments, clothing dares, playful spanks & pinning hands." / "午夜高能 —— 褪衣惩罚、按手控制、俏皮拍打与高阶触感挑逗。" | `punishments / spanks / pinning hands / 惩罚 / 按手 / 拍打 / 褪衣` | 改为「**更近的距离、更慢的节奏、说停就停**」类表述（安安 2.4.3 建议方向）；**必须保留可停语义**（`RED-B3`） | 悠悠 |
| **2** | `paywall_benefits`（:198） | "• Unlock 9 Extreme VIP Modes (Kama Positions, 5-Stage Foreplay, Body Map, Desire Ladder, Prop Lab, Connect 4, Auction & more)…" / 8 语言含「体位ルーレット・5段階フォアプレイ」 | `Kama Positions / 5-Stage Foreplay / 体位 / 前戯 / foreplay / 体位ルーレット` | 改为**中性游戏化命名**（安安建议「默契轮盘 / 十层心动阶梯」类）；**这是商店可见文案**（`RED-D4`：商店成熟度不得高于应用内实际内容） | 悠悠 |
| **3** | `mode_ladder_desc`（:399） | "10-step climb: one partner performs nonstop physical foreplay while the other answers sizzling truths." | `foreplay / nonstop physical / performs` | 改为「**十步递进的默契挑战**」类（安安 2.4.3 建议方向）；**不得暗示单向施动-受动结构**（`RED-B2`） | 悠悠 |
| **4** | `mode_auction_desc`（:323） | "Bid 100 Desire Chips for dominance contracts, blindfold control & midnight privileges." | `dominance / control` | 保留竞拍玩法，**去掉支配语义**，补「双方自愿、随时可停」语境（安安 2.4.3 + `RED-B3`） | 悠悠 |
| **5** | `mode_ldr_sync`（:467）、`mode_ldr_sync_desc`（:468） | "Long-Distance Sync" / "Sync a tiny moment together from anywhere - a timer counts you both in." / zh-CN「异地同步」「无论相隔多远，同步完成一个小仪式」 | `Sync / 同步 / 异地 / from anywhere` | 改为**本地化表述**（安安 3.4 举例：「同一时刻 / A Shared Countdown / 함께 카운트다운」）；**且不得新增 sync 类动词描述数据流**（`RED-P-1`） | 悠悠 |

> **第 5 组的硬边界（主理人明确）**：**只改显示文案**，`mode_ldr_sync` 的 **mode id（`"ldr_sync"`）、`GameModeCatalog.LDR_SYNC` 枚举、`TagDisplayMapper` 的 `"LDR_SYNC"` 键** 一律**不动** → **不触发**任何数量断言或映射回归。

### 4.2 本轮额外发现（架架新增，**需 team-lead 确认是否并入本轮**）

| # | 位置 | 现状 | 问题 | 建议 |
|---|---|---|---|---|
| **6** | `heat_scope_fmt`（`values/strings.xml:244` 及 8 语言） | en："…(Synced across all 20 games)"；zh-CN："（自动同步到全部 20 款游戏）"；zh-TW：「（自動同步到全部 20 款遊戲）」；ja：「全20ゲームに反映」；ko：「20개 전 게임에 적용」；es：「Aplicado a los 20 juegos」；fr：「Appliqué aux 20 jeux」；pt-BR：「Válido para os 20 jogos」 | **双重问题**：① **数字过时** —— 实测 **36 模式**（VIP **12** / 免费 **24**），文案仍写「20」；② **sync 歧义** —— en 写 `Synced across`，zh 写「自动同步到」（安安 `RED-P-1` 明列此 key 为歧义词专条）。**该 key 位于首页 `tv_heat_explanation`，是用户可见的常驻文案** | ① 数字改为 **36**（或改为不含具体数字的表述，如「全部游戏统一使用」，安安 `RED-P-1` 给出的「`applies to all 20 games` / 全部 20 款游戏统一使用 / 全ゲーム共通 / 모든 게임에 동일하게 적용」句式，但**数字必须同步改为 36**）；② 去掉 `Synced` / 「自动同步」等数据流动词 |
| **7** | `paywall_benefits`（:198）同 key | "Unlock **9** Extreme VIP Modes" | **数字过时** —— 实测 VIP 模式 **12** 个（positions/foreplay/body_map/ladder/auction/props/roleplay/blackjack/secret_mission/scenario_choice/hot_seat = 11 + `NAUGHTY_CONNECT4` 计入 → 12） | 与第 2 组一并修正为 **12**；否则商店页宣称与实际不符（`RED-D4`） |
| **8** | 同 key | "Unlimited Velvet Spark surprise prompts across **all 20 games**" | 数字过时（36） | 改为 36 或不含数字 |

> **架架的判断**：第 6~8 组**属于事实准确性缺陷（我方 AC-302）**，且第 6 组同时命中安安 `RED-P-1`；它们与主理人已批准的 5 组处于**同一批 strings.xml**，分开处理会产生**同一文件两轮写入冲突**（R-08 风险）。**建议并入本轮一并处理**，但**数字修正（20→36、9→12）超出纯「降调」范畴，属事实修复**，须 team-lead 确认（§7 #2）。

### 4.3 IARC 问卷触发（裁决 Q7）

| 事实 | 判定 | 责任 |
|---|---|---|
| 本轮内容有**实质改动**（5+3 组 strings 降调 + A/B/C 新增成人向内容） | → **触发 IARC 问卷重填** | **android-release** 在 P5 判定并执行（`RED-D3`） |
| 改写专家（悠悠/源源）是否需自行判断"影响不大" | **明令禁止** | `RED-D3`：**不得**由改写专家自行判断是否影响问卷任一项答案 |
| 架架是否判定 | **不判定**（工程不等法务，裁决 Q2） | 架架只确保规格把该触发条件写明 |

### 4.4 明确不属于本规格的事项

| 项 | 归属 | 说明 |
|---|---|---|
| **Q6 Keystore 明文降级路径** | android-security（Phase 4 出技术方案）+ android-data（落地） | 主理人明确「不由你出方案」→ **本规格完全不涉及**，架架不越权 |
| 商店文案 / ASO / 截图 | android-release（P5） | `RED-D4` |
| IARC 问卷填写 | android-release（P5） | `RED-D3` |

---

## 5. 8 语言新增 strings 的 key 命名规范

### 5.1 前缀约定

| 功能 | 前缀 | 示例 |
|---|---|---|
| A 项 引导式约会夜 | **`datenight_`** | `datenight_stage_warmup`、`datenight_enter`、`datenight_closing_prompt` |
| A 项 统计标签 | **`mode_date_night`**（沿用 `mode_` 既有模式命名范式，供 `TagDisplayMapper` 使用） | `mode_date_night` |
| B 项 18+ 年龄确认 | **`agegate_`** | `agegate_title`、`agegate_confirm` |
| C 项 今晚一句话 | **`tonight_note_`** | `tonight_note_title`、`tonight_note_prompt` |

> **统一要求**：新 key 一律 **小写 + 下划线**（与既有 652 条 key 风格一致），**前缀不得复用既有前缀**（避免与 `mode_` / `settings_` / `mood_` / `date_planner_` / `letter_` 等既有命名空间冲突）。
> **不新增 `mode_` 枚举**：A 项只新增 **1 个** `mode_date_night`（纯显示标签），**不新增 `GameModeCatalog` 项**。

### 5.2 8 语言 key 集合一致性（硬要求）

| 要求 | 说明 |
|---|---|
| **必须**：8 个 `values*` 目录的 key 集合**完全一致** | 与既有 652 条同等标准（AC-301 / AC-G05） |
| **禁止**：任何语言缺失某 key | 缺失会导致 `aapt` 在该语言下回落 `values/`（英文），造成「部分用户看英文」的隐性缺陷（AC-302） |
| **禁止**：任何语言把 key 定义为**空串** | 空串会**渲染为空白**而非回落 —— 比缺失更糟（用户看到空白按钮）。见 §5.3 |

### 5.3 关于「哪些 key 允许在某些语言下为空串」的明确回答

> **本规格的结论：本轮 A / B / C 的全部新 key，8 语言一律必须有非空译文。**

理由（基于对现有 CI 的实测）：

| 事实 | 证据 | 影响 |
|---|---|---|
| `verify_content.py` 的第 7 类校验**只做全局 union** | 实测第 135~149 行：把所有 `values*/strings.xml` 的 name 收进**一个** `defined_strings` 集合，再检查布局/菜单/导航里引用的 `@string/X` 是否在其中 | **它不校验 per-language key 集合对齐**（`grep -c "key_set\|align\|per_lang"` = **0**） |
| 因此 | 某语言缺 key / 空串 → **CI 不会报错** | 若仅靠现有 CI，A/B/C 的 8 语言不对齐会**静默通过**，直到真机在非英文 locale 下暴露空白或英文回落 |
| 空串的行为 | aapt 允许 `<string name="x"></string>`，运行时 `getString()` 返回 `""` → **界面显示空白**，不回落 `values/` | 空串是**不可见缺陷**，比缺失更难发现 |
| 缺失的行为 | aapt 资源链接期若某语言缺 key 但 `values/` 有 → 该语言回落英文；**若全 8 语言都缺** → 构建失败（历史第一轮 `placeholder_desc` 即此类阻断） | 缺失会「部分回落英文」，也是隐性缺陷 |

**因此本轮给出两条配套要求**：

1. **内容侧（悠悠）**：新 key 8 语言一律非空；若某语言确实难以自然表达，**采用「等价改写」而非空串**（例如某语言难以自然表达「留白」概念时，改用该语言的自然留白句式，而非留空）。
2. **CI 侧（建议 android-build / android-test 顺手补）**：在 `verify_content.py` 增加第 9 类检查 —— **per-language key 集合对齐**（取 `values/strings.xml` 的 name 集合为基准，其余 7 语言逐一比对，缺失或空串均报 `STRING_KEY_MISSING` / `STRING_KEY_EMPTY`）。
   - **这不是本轮 A/B/C 的阻塞项**（可与本轮并行），但**是 AC-G05「8 语言 key 集合一致」目前唯一缺少的机器校验**。
   - 建议归口：android-build（脚本属 `scripts/`，与 `verify_content.py` 同目录）；架架在 [API-005] 契约中已预留该要求。

> **补充**：若未来确有「仅某语言适用」的内容（如某地区的文化专属提示），正确做法是**新增独立的 locale 目录**（如 `values-b+zh+Hant`）或使用 `tools:locale` 标注，而不是在 8 语言主目录留空串。

### 5.4 新增 key 总量

| 功能 | 新增 key 数 | 前缀 |
|---|---|---|
| A 引导式约会夜 | **16**（含 `mode_date_night` 1 条） | `datenight_` × 15 + `mode_date_night` × 1 |
| B 18+ 年龄确认 | **6** | `agegate_` |
| C 今晚一句话 | **10** | `tonight_note_` |
| **合计** | **32 条**（× 8 语言 = 256 条译文） | — |

> **A 项 16 条的语义清单**（语义意图 + P 特征 + 梯度；**不给最终文案**）：

| # | key | 语义意图 | 必命中 P 特征 | 梯度 |
|---|---|---|---|---|
| 1 | `mode_date_night` | 会话在记忆档案中的显示标签（**中性功能名**，非调性文案） | — （功能标签） | 不适用 |
| 2 | `datenight_title` | 主标题：今晚的约会夜 | P1 / P3 | T1 |
| 3 | `datenight_subtitle` | 副标题：由 App 陪你走完这一晚（**不写功能罗列**） | P1 / P2 / P3 / P6 | T1 |
| 4 | `datenight_enter` | 进入按钮 | P3 | T1 |
| 5 | `datenight_stage_warmup` | 阶段名：暖场（**克制、轻松**） | P1 / P3 | T1 |
| 6 | `datenight_stage_deepen` | 阶段名：靠近一点 | P1 / P3 | T2 |
| 7 | `datenight_stage_peak` | 阶段名：心跳（**不写失控、不写动作**） | P1 / P3 / P5 | T3 |
| 8 | `datenight_stage_closing` | 阶段名：慢慢收尾 | P1 / P3 | T1 |
| 9 | `datenight_warmup_prompt` | 暖场阶段引导语：今晚从哪儿开始 | P1 / P2 / P3 / P6 | T1 |
| 10 | `datenight_deepen_prompt` | 深入阶段引导语：再靠近一点 | P1 / P2 / P3 / P4 / P6 | T2 |
| 11 | `datenight_peak_prompt` | 高点阶段引导语：**留白 + 心跳 + 可停**（**P7 必选**） | P1 / P2 / P3 / P4 / P5 / P6 / **P7** | T3（上限口径封顶 `Suggestive/Mild`） |
| 12 | `datenight_closing_prompt` | 收尾阶段引导语：温柔收尾、留余韵 | P1 / P2 / P3 / P6 / P7 | T1 |
| 13 | `datenight_progress_fmt` | 进度提示（含 `%1$d/%2$d` 占位符） | P2 | 功能性 |
| 14 | `datenight_skip` | 跳过本张 | P2 | 功能性 |
| 15 | `datenight_pause_safe` | 安全暂停条文案（**呼应既有 Safe Word / Pause**，不得移除） | P2 / P7 语义 | 功能性 |
| 16 | `datenight_finish` | 会话完成提示：温柔收束、**必含可停/余韵语义** | P1 / P2 / P6 / P7 | T1 |

> **禁用说明**：以上 16 条中，**任何一条都不得**包含身体部位、器物、动作分解、命令式语态（安安 `RED-A2` / `RED-C` 铁律 / 主理人裁决）。`datenight_peak_prompt` 与 `datenight_finish` **必须**含 P7 边界语义（`RED-B3`）。

---

## 6. §6.2 落点更新（落到 §6.1 现有分层图上）

```
app  (单 Activity + Navigation Component + ViewBinding)

├── ui/                          表现层
│   ├── home/                    HomeFragment  ← 新增「今晚的约会夜」卡片（三件套）
│   ├── session/                 🆕 DateNightFragment          （A 项）
│   ├── onboarding/              OnboardingDialogFragment       （B 项挂在更前置的位置）
│   ├── consent/ deck/ settings/ （不变）
│   ├── common/                  PassPhoneDialog / HapticHelper / DareTimerHelper
│   │                            / TaskPromptDialogHelper / PhysicsAnimator（全部复用）
│   └── social/ dateplanner/ letter/ lovegraph/ …（不变）

├── core/                        领域层（纯 Java 优先，无 Android 依赖，可单测）
│   ├── engine/                  既有 12 类引擎（不动）
│   ├── session/                 🆕 DateNightSessionEngine      （A 项，Phase 机 + 抽卡）
│   │                            🆕 DateNightStage（枚举 WARM_UP/DEEPEN/PEAK/CLOSING）
│   ├── profile/                 CoupleProfileManager / MoodCheckinManager
│   │                            / TogetherDateFormat
│   │                            🆕 AgeGateManager             （B 项，SP 布尔 + isConfirmed）
│   ├── journal/                 🆕 TonightNote                 （C 项，纯 Java 数据模型）
│   │                            🆕 TonightNoteManager          （C 项，SP + LiveData）
│   ├── i18n/                    LocaleManager / AssetContentLoader
│   │                            TagDisplayMapper  ← 新增 1 条 "DATE_NIGHT" 映射
│   └── …（其余不变）

├── data/                        持久化层
│   ├── local/dao/ entity/       （不变，本轮无 Room 变更）
│   └── repository/              ContentRepository（复用 loadMergedPoolAsync / recordSessionStat）
│                                ConsentRepository（复用，边界规则不动）

└── assets/content/{8 lang}/     🆕 本轮【不新增任何 JSON 文件】（ADR-002）
```

| 项 | 范式 | 与既有的一致性 |
|---|---|---|
| `DateNightSessionEngine` | **纯 Java、无 Android 依赖、构造注入 `Random`**（便于确定性单测） | 与 `SocialGamesEngine` / `DatePlannerEngine` / `LoveGraphEngine` 同款 |
| `DateNightStage` | 枚举内含 `(maxTier, cardCount, preferredTags)` | 与 `SocialGamesEngine.ModeConfig` 同款 |
| `AgeGateManager` | `Context` 构造 + SP 读写 + `isConfirmed()` | 与 `MoodCheckinManager` 同款 |
| `TonightNoteManager` | `Context` 构造 + SP 分键 + `LiveData` | 与 `MoodCheckinManager` 同款（**分键而非 Gson**，规避 R8 字段名坑） |
| `AppContainer` | 新增 3 个 `public final` 字段并 `new` 注入 | 与既有 12 个 manager 一致 |

> **不新增依赖**：不引入任何新 Gradle 依赖、不引入 Room migration、不新增权限。

---

## 7. §7 接口契约补充（[API-008] ~ [API-010]）

> 格式与 §7 既有 API-001~API-007 完全一致（模块 / 方法 / 契约 / 错误 / 线程）。

```
[API-008]
模块：ui/session/DateNightFragment → core/session/DateNightSessionEngine
方法：startSession(sessionMood: int) / drawNext(): TaskCard / currentStage(): DateNightStage
      / stageProgress(): int / markDone() / finishSession(): SessionResult
契约：纯 Java、无 Android 依赖、可单测（构造注入 Random 以支持确定性断言）；
      抽卡必须依次满足：tier ≤ min(stage.maxTier, playerMaxTier)
      AND ConsentFilter.isAllowed(tags) AND 命中 stage.preferredTags
      AND 不在会话级 recentIds 的 shield 窗口内
      （shieldSize = min(12, max(0, poolSize-4))，与 SocialGamesEngine 同值同算法）；
      PEAK 阶段不设倒计时（由双方决定节奏，呼应 RED-B3「可随时停止」）；
      绝不通过放宽 HARD_NO 来补足卡池
错误：池空 → 逐级降档（tier-1 → 最低可用档），每档至少保留 ≥4 张可用卡；
      全局仍空 → 返回 null 并由 Fragment 展示降级提示，**不得抛异常、不得返回 null TaskCard 被直接解引用**；
      ConsentFilter 全 HARD_NO 时会话应正常结束并以 (0,0,0) 记账
线程：纯计算，主线程调用；不触碰 IO
```

```
[API-009]
模块：ui/onboarding（首启门） → core/profile/AgeGateManager
方法：isConfirmed(): boolean / confirm(): void / decline(): void / isDeclined(): boolean
契约：首启时若 !isConfirmed()，**必须先于任何成人向内容呈现**完成年龄确认；
      结果存 SharedPreferences 独立 pref 文件的单个 boolean，写盘先于关窗；
      裁决 Q1：口径统一 18+，文案语义见 §2.4（6 条 key，8 语言非空）；
      架架建议软门禁（decline → 仅保留 heatRating ≤ 2 的温和模式，不硬阻断整个 App），
      软门禁 / 硬门禁属产品策略，交 team-lead 确认
错误：SP 读写失败 → 视为「未确认」（fail-closed），**不得默认放行**
线程：主线程读写（SP 轻量）；无网络、无 IO
```

```
[API-010]
模块：ui/journal（编辑/回看） → core/journal/TonightNoteManager
方法：saveNote(dateIso: String, noteA: String, noteB: String, moodScore: Int): void
      / observeNotes(): LiveData<List<TonightNote>> / getNote(dateIso: String): TonightNote
      / getTodayIsoDate(): String
契约：dateIso 由 TogetherDateFormat.toIso() 产出（yyyy-MM-dd），与「在一起天数」口径统一；
      同一天重复保存**覆盖**当日记录（每人每天一句），不追加；
      noteA / noteB 允许空串（一方未写）；moodScore ∈ [1,5]（-1 表示未标）；
      **存储仅限本机 SharedPreferences（分键范式，非 Gson），不上传 / 不预审 / 不分享 / 不导出**，
      任何新增 share / export / upload 能力将同时违反 RED-A6 与 RED-P 并触发 IARC 重填；
      列表按 dateIso 倒序，默认折叠摘要
错误：日期非法或非 ISO 格式 → 拒绝写入并返回 false（不得写入半截键）；
      SP 写入失败 → 保留既有值，不产生半写状态
线程：SP 写入走 AppContainer.ioExecutor；LiveData 回主线程 observe
```

---

## 8. 规格与红线/基准的对齐检查表（**供悠悠与源源自查**）

| 检查项 | 依据 | A 项 | B 项 | C 项 | 降调 5 组 |
|---|---|---|---|---|---|
| 未新增权限（manifest 仍仅 `VIBRATE`） | 安安 §4.2 / 我方 [API-004] / AC-G01 | ✅ 纯 UI + 本地 | ✅ 纯 SP | ✅ 纯 SP | ✅ 仅文案 |
| 未引入网络 / 账号 / 地图 SDK / 位置 / 第三方 SDK | 安安 §3.1 / AC-G01 | ✅ | ✅ | ✅ | ✅ |
| 未新增 `GameModeCatalog` 项（保持 36） | 主理人硬约束 / 历史 ADR-014 | ✅ | ✅ | ✅ | ✅（第 5 组仅改显示文案） |
| 未新增题库 JSON | 历史 ADR-002 | ✅ | ✅ | ✅ | ✅ |
| 未新增内容资产翻译风险 | 我方 ADR-R13-008 | ✅ 仅 16 条 strings | ✅ 仅 6 条 | ✅ 仅 10 条 | ✅ 仅改写既有 key |
| 8 语言 key 集合一致 | AC-G05 / [API-005] | ✅ 16×8 | ✅ 6×8 | ✅ 10×8 | ✅ 既有 key 不新增/不删 |
| 8 语言无空串 | §5.3 | ✅ | ✅ | ✅ | ✅ |
| 调性：T1~T4 单调递增、T4 不靠动作升级 | 安安 `RED-D` / 主理人裁决 | ✅ §5.4 A 项 16 条 | 不适用（功能性） | ✅ §3.6 | ✅ 全部降调 |
| 调性：P2 + P3 必选、正面 ≥4 | 我方 §4.6 TONE-CHK | ✅ | 功能性文案按 §2.4 | ✅ | ✅ |
| 调性：T3/T4 必须含 P7 边界共识 | 安安 `RED-B3` | ✅ `datenight_peak_prompt` / `datenight_finish` | 不适用 | ✅ `tonight_note_prompt` 类 | ✅ |
| 无 sync / 上传 / 云端 / 分享 / 定位 数据流措辞 | 安安 `RED-P-1` / §3.2 | ✅ 新 key 全避 | ✅ `agegate_decline_note` 用否定式 | ✅ `tonight_note_subtitle` / `tonight_note_saved_toast` 用否定式 | ✅ 第 5 组 + `heat_scope_fmt` |
| 无动作分解 / 器官 / 器物 | 安安 `RED-A2` | ✅ | ✅ | ✅ | ✅ |
| 无胁迫 / 惩罚 / 失去意识 | 安安 `RED-A3` | ✅ | ✅ | ✅ | ✅ 第 1、4 组重点 |
| 无「意图满足性欲」式表述（含 paywall） | 安安 `RED-A7` | ✅ | ✅ | ✅ | ✅ 第 2、3 组重点 |
| 双人共同视角 | 安安 `RED-B5` / 我方 P6 | ✅ | 不适用 | ✅ | ✅ |
| 商店成熟度不高于应用内实际内容 | 安安 `RED-D4` | 不适用 | 不适用 | 不适用 | ✅ 第 2 组 |
| IARC 重填判定交 android-release | 安安 `RED-D3` / 裁决 Q7 | ✅ 不自行判定 | ✅ | ✅ | ✅ |
| **不涉及 Keystore 明文降级路径** | 裁决 Q6 | ✅ | ✅ | ✅ | ✅ |

---

## 9. 待确认项（交回 team-lead）

| # | 待确认 | 我的建议 | 决策方 |
|---|---|---|---|
| 1 | **B 项门禁强度**：软门禁（decline → 保留 ≤2 火焰模式）还是硬门禁（decline → 全 App 不可用） | **软门禁**（理由见 §2.3） | 用户 + team-lead |
| 2 | §4.2 的第 6~8 组（`heat_scope_fmt` 与 `paywall_benefits` 的**数字过时 20→36 / 9→12** + `Synced/自动同步` 歧义）是否**并入本轮** | **建议并入**（同一文件同批处理，避免 R-08 写入冲突；且属 AC-302 事实修复） | team-lead |
| 3 | A 项 PEAK 阶段**是否设倒计时** | **不设**（避免计时器把节奏变任务，与 N3 压迫感冲突） | team-lead（产品体验） |
| 4 | C 项是否允许**删除/编辑**当日记录 | **允许当日覆盖写**，不提供批量导出/分享 | team-lead + 用户 |
| 5 | `verify_content.py` 增加第 9 类「per-language key 对齐」校验 | **建议本轮并行补**（唯一缺机器校验的 AC-G05 项） | android-build / android-test |
| 6 | C 项数据是否需要 Room 表（而非 SharedPreferences） | **不建议**：需 migration，且共享偏好分键范式已足够 | team-lead |

---

## 10. 交付给下游专家的速查

| 专家 | 你需要什么（在本文件的位置） | 交付形式 |
|---|---|---|
| **悠悠 android-ui** | A 项 UI 形态 §1.7；A 项 16 条 key 语义 §5.4；B 项 6 条 key 语义 §2.4；C 项 10 条 key 语义 §3.6；呈现层级 §3.5；**存量降调 5 组约束 §4.1**（+ §4.2 待确认）；8 语言 key 规范与空串禁令 §5.1~§5.3；对齐自查表 §8 | `res/layout/**` + `res/values*/strings.xml`；**最终文案由你撰写**，须过 §8 自查表与安安红线 |
| **源源 android-data** | A 项**不新增 JSON**（§1.3 / §6）；本轮内容层主要工作是**存量题目文本的降调**（安安 §2.4.3 末行：`bite_lick` / `restraint_light` / `blindfold` / `suck softly and kiss on lower belly` 等）| `assets/content/**` + `scripts/gen_*.py`；**须跑 `verify_content.py` / `dp_lint.py` / `sl_lint.py` / `check_strings_mix.py` 四脚本 EXIT=0** |
| **安安 android-security** | §4.2 待确认第 6~8 组；§8 对齐自查表；**Q6 项本规格未涉及**（按裁决交由你出技术方案） | 确认规格与 `RED-A/B/C/D/E`、`RED-P-1` 无冲突 |
| **铮铮 team-lead** | §0 裁决承接表；§9 六项待确认；§4.3 IARC 触发判定交 P5 | 拍板 §9 |
| **android-build / android-test** | §5.3 建议补 per-language key 对齐校验；AC-G05 机器化 | `scripts/verify_content.py` 增加第 9 类检查 |

---

> **规格结束**。本文件只定义结构、契约与判定口径，**不含任何最终文案正文，不含任何代码**。
> 架架承诺：本轮与 Phase 3 全程**未修改任何代码、布局、strings.xml 或内容 JSON**；唯一写入为本规格文件（及 Phase 1 报告 v1.1 增量修订）。
> 关联文件：`round13_phase1_architect.md`（基准 v1.1）、`round13_phase1_security.md`（红线）、`round13_phase1_knowledge.md`（历史销账与陷阱库）。

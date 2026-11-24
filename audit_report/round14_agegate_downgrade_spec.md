# VelvetCouples 第 14 轮 · 18+ 软门禁下沉数据层架构方案（P0 合规缺陷）

> **作者**：架架（android-architect）
> **下发**：铮铮（android-studio-expert-team-lead），P0 任务卡
> **触发**：安安（android-security）Phase 4 扫描发现 P0 合规缺陷，主理人核实采纳
> **日期**：2026-11-23
> **基准文件**：`round14_tension_spec.md`（v1.2）、`round13_phase1_architect.md`（v1.1）、`round13_phase3_architect_spec.md`（A/B/C 规格）、`round13_phase1_security.md`（RED-A/B/C/D/E/P/T）
> **硬约束**：只写方案不改代码；不推翻任一 RED 红线；下沉后仍满足 TDS 门禁（T3/T4 ≥15/20）；保持纯本地、零权限（仅 VIBRATE）、零网络；模式数恒 36；全程简体中文。
> **职责边界**：本文件给出唯一卡口、接口契约、可测断言与风险边界；落地由 android-data（源源）执行、android-test（测测）写单测、android-security（安安）终裁。

---

## 0. 一句话结论（给主理人）

**把 18+ 判定从「HomeFragment 的一处 UI 过滤」下沉为「`getSelectedMaxTier()` 返回值的一个不可绕过的上限钳制」**：在所有抽卡路径读取 tier 上限的唯一数值源里，叠加 `AgeGateManager.isAdultContentAllowed()`，decline 时把 effective maxTier 恒钳到 **≤2**。这样无需改动 40+ 个 Fragment、无需改动 TaskEngine / SocialGamesEngine / DateNightSessionEngine 内部语义，所有抽卡（含 A 项约会夜）自动只能抽到 heatRating ≤2 的卡，且 ConsentFilter 的 HARD_NO 逻辑完全不变。

---

## 1. 根因分析

### 1.1 `isAdultContentAllowed` 全工程消费点（grep 实证）

| 位置 | 文件:行 | 用途 | 是否生产消费点 |
|---|---|---|---|
| 判定定义 | `core/profile/AgeGateManager.java:67` | `return isConfirmed() && !isDeclined()` | 定义（非消费） |
| **唯一生产消费点** | `ui/home/HomeFragment.java:293-300` | `renderGameModesList()` 中 `if (!adultContentAllowed)` → 过滤掉 `heatRating >= 3` 的模式卡，使其不出现在主页列表 | ✅ **是** |
| 触发 | `MainActivity.java:73` / `AgeGateDialogFragment.java` | 首启弹窗、`confirm(true/false)` 写入 | 写入侧（非消费） |
| 注册 | `AppContainer.java:31,52` | 构造 AgeGateManager | 装配（非消费） |
| 测试 | `test/.../AgeGateManagerTest.java` | 验证 fail-closed、持久化、36 模式不变 | 测试（非消费） |

**结论**：`isAdultContentAllowed()` 在**生产代码中只有 1 个消费点**，且位于 UI 渲染层（`renderGameModesList`）。它控制的是「主页模式列表显示哪些卡」，而非「内容池能抽出哪些卡」。这正是缺陷根因——**门禁挂在展示层，而非数据层**。

### 1.2 为什么只在 HomeFragment 消费会失效（失效机理）

`renderGameModesList()` 的行为是：`decline` → 过滤模式列表 → 用户从主页看不到 heatRating≥3 的模式卡 → 点卡片时 `navigate(selected.navActionId)` 进入对应 Fragment。这条路径上门禁"看起来"生效，但它防不住以下任一情况：

1. **非模式列表入口直达**：App 内存在多条**不经过 `renderGameModesList()`** 的抽卡/内容入口，它们只读取 `getSelectedMaxTier()` 或直接从内容池抽卡，从不调用 `isAdultContentAllowed()`。即使主页隐藏了高火焰模式卡，这些入口仍能抽到高 tier 内容。
2. **状态时序问题**：年龄确认是首启一次性写入（`MainActivity.java:73`）。若用户先以"成年"通过 → 进入某高 tier 模式 Fragment 并停留 → 通过任何方式（测试注入、SP 编辑、二次弹窗流程）转为 decline，已打开的 Fragment 不会重新执行门禁，抽卡循环继续用旧的 maxTier。
3. **导航图的静态性**：`nav_graph.xml` 共 70 个 `<fragment>/<action>`，**无 `<deepLink>`**（已 grep 确认）。因此"外部深链直达"在本项目**不成立**，不能作为绕过路径申报；但**内部跨 Fragment action**（如 paywall 解锁后的 `ACTION_MODE_PREFIX` 回跳、每日揭晓入口、任务提示弹窗 `TaskPromptDialogHelper`）确实存在，且这些路径都不读 ageGate。

> **给安安的准确表述**：本项目**没有** nav_graph 深链、没有通知、没有 Widget、没有第三方跳转入口（任务卡原描述的这几类可绕过路径在本工程**实际不存在**，不列入）。**实际存在的绕过路径全部是"App 内非主页模式列表的抽卡入口"**，见 §1.3。

### 1.3 实际存在的可绕过路径清单（全部经过 maxTier 但不读 ageGate）

以下入口均能抽卡/取内容，但**没有一条调用 `isAdultContentAllowed()`**；它们统一只读 `getSelectedMaxTier()`（grep 逐条核实）：

| 入口 | 文件:行 | 抽卡方式 | 受 maxTier 约束 | 受 ageGate 约束 |
|---|---|---|---|---|
| 主页快速抽卡卡 | `HomeFragment.java:652-660` | `drawNext`/`generate(maxTier)` | ✅ | ❌ |
| 每日双人揭晓 | `DailyRevealFragment.java:68` | 内容池加载 | ✅（间接） | ❌ |
| 任务提示弹窗换卡 | `TaskPromptDialogHelper.java:132-133` | `drawNext(cap)` | ✅ | ❌ |
| 幸运轮盘 | `LuckyWheelFragment.java:186-203` | `drawNext`/`generate` | ✅ | ❌ |
| 心跳保持 | `HeartbeatHoldFragment.java:205-207` | `drawNext(maxTier)` | ✅ | ❌ |
| 12 个 social 游戏 | `SocialGameFragment.java:71-89` | `SocialGamesEngine.loadPool(maxTier)` | ✅ | ❌ |
| A 项约会夜串场 | `DateNightFragment.java:80-85` | `DateNightSessionEngine.loadPool(maxTier)` | ✅ | ❌ |
| 其余 20+ 模式 Fragment | 见 §1.4 grep 全表 | `drawNext(maxTier)` | ✅ | ❌ |

**这张表就是下沉方案的立足点**：既然"所有抽卡路径都读 maxTier、但都不是都读 ageGate"，那么**把 ageGate 语义注入 maxTier 本身**，就能一次性覆盖全表——这就是唯一卡口的选择依据。

### 1.4 关键事实：`getSelectedMaxTier()` 是全工程 tier 上限的唯一数值源

grep `getSelectedMaxTier()` 得到 **40+ 处生产消费**，覆盖：
- 所有 36 个游戏模式 Fragment 的抽卡路径；
- 主页快速抽卡（`HomeFragment:652`）、每日揭晓、任务提示弹窗；
- A 项约会夜（`DateNightFragment:80`）、12 个 social 游戏（`SocialGameFragment:71`）；
- 程序化生成 `ProceduralTaskGenerator.generate(maxTier, ...)`。

**没有发现任何硬编码 `maxTier=4/5` 的抽卡调用**：可疑点均已核实为 `Math.min(..., getSelectedMaxTier())` 钳制：
- `LuckyWheelFragment:187` `effectiveTier = Math.min(landed.tier, maxAllowedTier)` ✅
- `CoupleBoardFragment:169` `cappedTier = Math.min(tier, maxAllowed)` ✅
- `DrawGuessFragment:234` `cappedPenaltyTier = Math.min(rawPenaltyTier, getSelectedMaxTier())` ✅
- `CoupleMinesFragment:100`、`TaskPromptDialogHelper:132`、`VelvetBlackjackFragment:183` 同理 ✅

**下游过滤机制**：`TaskEngine.isEligible()`（`TaskEngine.java:181`）用 `if (card.tier > maxTier) return false` 硬过滤，程序化生成 `proceduralGenerator.generate(maxTier, ...)` 同样以 maxTier 为上限。因此 **maxTier 就是最终的、物理的 tier 闸门**——只要把它的值钳到 ≤2，任何抽卡算法都不可能产出 >2 火焰的卡。

> **注（边界）**：`bundle.positions` / `bundle.foreplayStages` 这类 ExtremeModes 固定内容池是 `addAll` 全量加载（`PositionsRouletteFragment:106`、`ForeplayEscalatorFragment:84`），不走 `drawNext`、无 tier 字段过滤。但它们属于**模式级门禁**范畴（用户需先能进入该 VIP 模式），与"抽卡 tier 门禁"是两层。本 P0 的核心断言是"任意模式抽不到 >2 火焰卡"，故 §2 方案采用**双层卡口**（内容 tier + 模式入口），详见 §2.3。

---

## 2. 下沉方案

### 2.1 唯一卡口的选择与论证

**候选卡口评估**（任务卡要求我判断 `SocialGamesEngine.drawNext` vs `ContentRepository` 哪个是唯一卡口；我的结论是**两者都不是**，正确卡口是第三处）：

| 候选 | 覆盖范围 | 是否唯一/可绕过 | 结论 |
|---|---|---|---|
| `SocialGamesEngine.drawNext`（实际为 `loadPool`） | 仅 12 个 social 模式 | ❌ 覆盖 12/36，其他 24 模式与主页抽卡全绕过 | **否决** |
| `ContentRepository.loadMergedPoolAsync` | 合并卡池的加载 | ❌ 只是"加载"，无 tier 语义；且大量路径直连 `assetLoader.loadXxx`（extreme/intimate/party/fate/scratch/roleplay/quiz）不经过它；在加载层过滤会污染所有调用方（含 VIP 用户） | **否决** |
| `TaskEngine.drawNext` | 大部分抽卡 | ❌ social / dateNight 用自己的引擎，不走它；且 signature 无 ageGate 参数，改动面大 | **否决（作为唯一卡口）** |
| ✅ **`EntitlementManager.getSelectedMaxTier()`** | **全工程 tier 上限的唯一数值源，40+ 处消费，是所有抽卡的共同上游** | ✅ **不可绕过**：任何抽卡都必须先取得 maxTier；下游 `TaskEngine.isEligible` 以 `card.tier > maxTier` 物理过滤；不存在硬编码 tier 旁路 | **采纳（主卡口）** |

**为什么 `getSelectedMaxTier()` 不可绕过**（三重论证）：

1. **汇聚性**：无论从哪个 Fragment、哪个按钮进入，抽卡前必须调用它取 tier 上限（§1.4 全表）。它是所有抽卡路径的"最大公约数"。
2. **物理性**：它返回的 int 直接成为 `isEligible()` 的比较阈值和 `generate()` 的生成上限——不是"建议值"而是"硬过滤值"。改这个值 = 改所有抽卡的物理边界。
3. **无旁路**：grep 确认无硬编码 tier 绕过（§1.4 四个可疑点全部是 `Math.min(..., getSelectedMaxTier())`）。 AgeGate 一旦钳住这个返回值，就没有第二条路能拿到 >2 的 maxTier。

### 2.2 主卡口实现（内容 tier 门禁）

**唯一改动点**：`EntitlementManager.getSelectedMaxTier()` 的返回值，在既有 `FeatureGate.resolveEffectiveMaxTier` 之后，再叠加一次 ageGate 钳制。

```java
// 目标语义（伪码，供源源落地，非最终实现）
public int getSelectedMaxTier() {
    int resolved = ...; // 现有逻辑：defaultTier + FeatureGate.resolveEffectiveMaxTier(...)
    // ★ P0 新增：18+ 软门禁下沉——decline / 未确认 一律钳到 FREE_MAX_TIER(2)
    if (ageGateManager != null && !ageGateManager.isAdultContentAllowed()) {
        return Math.min(resolved, FeatureGate.FREE_MAX_TIER); // FREE_MAX_TIER == 2
    }
    return resolved;
}
```

**落地要点**：
- `EntitlementManager` 需持有 `AgeGateManager` 引用（构造注入，见 API-011）。`AppContainer` 构造顺序保证 `ageGateManager` 先于 `entitlementManager` 创建（`AppContainer.java:52` vs `:73`），注入无循环依赖。
- **不修改** `AgeGateManager` 的 SP 单布尔存储（PREF_NAME / KEY_CONFIRMED / KEY_DECLINED 原样）。
- **不修改** `FeatureGate` 的常量（`FREE_MAX_TIER=2` / `PREMIUM_MAX_TIER=4`）与纯函数语义，避免破坏 `Phase4ReleaseTest` 的既有断言（`resolveEffectiveMaxTier(4,false)==2` 等）。
- **fail-closed**：`isAdultContentAllowed()` 本身在读取异常时返回 false（未确认/de declined），故异常态自动落入钳制分支 → 异常时也只会抽 ≤2 卡，符合安全预期。

**自动收益**（无需逐处改动）：
- 主页快速抽卡、每日揭晓、轮盘、心跳、social 12 游戏、A 项约会夜、其余 20+ 模式 —— 全部因为读同一个 `getSelectedMaxTier()` 而自动受限。
- `TaskEngine` / `SocialGamesEngine.loadPool` / `DateNightSessionEngine.loadPool` / `ProceduralTaskGenerator.generate` 的内部逻辑**一行不用改**。

### 2.3 补充卡口（模式入口门禁）——覆盖固定内容池与 VIP 模式

主卡口解决"抽卡 tier"，但**模式级门禁**（能否进入一个 VIP/高 heatRating 模式）需要补一处，否则用户 decline 后仍可从已解锁状态进入 VIP 模式并看到其固定内容池（如 positions/foreplay 的 `addAll` 内容）。

**补充卡口**：在 `FeatureGate.canAccessGameMode(mode, isPremiumUnlocked)` 叠加 ageGate 维度——decline 时 `heatRating >= 3` 的模式一律不可访问。

```java
// 目标语义（伪码）
public boolean canAccessGameMode(GameModeCatalog mode, boolean isPremiumUnlocked,
                                 boolean adultContentAllowed) {
    if (mode == null) return true;
    if (!adultContentAllowed && mode.heatRating >= 3) return false; // ★ P0 新增
    return !mode.vipRequired || isPremiumUnlocked;
}
```

- 现有 `canAccessGameMode` 只在 `HomeFragment:336` 被调用（模式卡点击时）。下沉后，`EntitlementManager.canAccessGameMode(mode)` 内部注入 ageGate，使所有经由此方法的访问判定统一受限。
- **注意**：`canAccessGameMode` 目前消费面窄（仅主页卡片点击），因此它是**补充**防线而非主卡口；主卡口仍是 §2.2 的 maxTier 钳制。两者叠加 = 内容层 + 入口层双保险。
- **36 模式不变**：此卡口只影响"是否允许进入"，不增删 `GameModeCatalog` 枚举项，不影响任何数量断言。

### 2.4 与 ConsentFilter 的叠加关系（不是替代）

- `ConsentFilter` 的 `HARD_NO` 是**双人边界否决**（任一方硬否决的卡全局屏蔽），与年龄门禁是**两个独立正交维度**。
- 下沉方案**不触碰** ConsentFilter：它继续在 `TaskEngine.isEligible` / 各引擎内部按边界规则过滤。
- 叠加后的过滤链为：`card.tier <= maxTier(≤2 when declined)` **AND** `ConsentFilter.allow(card)` **AND** `card.type 匹配`。三者同时满足才入池。HARD_NO 的优先级与年龄门禁互不覆盖——即使成年用户（ageGate 放行），HARD_NO 卡依然被屏蔽；即使无边界否决，decline 用户仍被 ≤2 钳制。

---

## 3. 接口契约（给 android-data / 源源）

沿用 §7（`round13_phase1_architect.md`）格式：模块 / 方法 / 契约 / 错误 / 线程。新增 API-011~013，并修订 API-009。

```
[API-011]（新增·主卡口）
  模块：core/billing/EntitlementManager ← core/profile/AgeGateManager
  方法：EntitlementManager(Context, AgeGateManager)  // 构造注入
        getSelectedMaxTier(): int
  契约：返回值 = min( FeatureGate.resolveEffectiveMaxTier(saved, isPremiumUnlocked),
                      ageGateManager.isAdultContentAllowed() ? PREMIUM_MAX_TIER : FREE_MAX_TIER )
        即 decline/未确认时 effective maxTier 恒 ≤ 2 (FREE_MAX_TIER)；成年时保持原逻辑不变。
        AppContainer 须在 ageGateManager 之后构造 entitlementManager（现有顺序已满足）。
  错误：ageGateManager 为 null 时按 fail-closed 处理（视同未放行，钳到 ≤2），不得 NPE 崩溃后放行。
  线程：任意线程可调用（纯读取 SP + 纯函数，无写入）；与现有调用方线程假设一致。

[API-012]（新增·补充卡口）
  模块：core/billing/EntitlementManager → core/billing/FeatureGate
  方法：canAccessGameMode(mode: GameModeCatalog): boolean
        FeatureGate.canAccessGameMode(mode, isPremiumUnlocked, adultContentAllowed): boolean
  契约：adultContentAllowed == false 且 mode.heatRating >= 3 → 返回 false（不可进入）。
        其余情况保持既有 vipRequired 判定。GameModeCatalog 枚举与数量（36）不得改动。
  错误：mode == null 返回 true（保持既有语义）；heatRating 取值异常时按 >=3 从严处理。
  线程：任意线程；纯判定。

[API-013]（新增·A 项约束传递）
  模块：ui/session/DateNightFragment → core/session/DateNightSessionEngine
  方法：DateNightSessionEngine.loadPool(cards, filter, maxTier)  // 签名不变
  契约：maxTier 由调用方从 getSelectedMaxTier() 取得（DateNightFragment:80 已如此），
        因此 decline 后自动 ≤2；引擎内部 4 阶段 Phase 机 WARM_UP→DEEPEN→PEAK→CLOSING
        不得自行抬高 tier 或覆盖 maxTier。PEAK 阶段在 ≤2 上限内按 round14_tension_spec
        T1/T2 张力手法实现（见 §5.2）。
  错误：maxTier < 1 时按 1 处理；池空回退 ≥4 张（沿用现有回退，见 Phase3 规格）。
  线程：loadPool 在 IO/后台，drawNext 在 UI 线程（沿用现状）。

[API-009]（修订·不改变存储契约）
  模块：core/profile/AgeGateManager
  方法：isAdultContentAllowed(): boolean / isConfirmed() / isDeclined() / confirm(declined)
  契约：存储契约不变（PREF_NAME="velvet_age_gate"，仅 confirmed/declined 两个布尔键）。
        isAdultContentAllowed() 现**同时被 HomeFragment（UI 隐藏）与 EntitlementManager（数据钳制）消费**；
        语义保持 fail-closed：未确认 / 已拒绝 / 读取异常 → false。
  错误：读取异常返回 false（不默认放行）。
  线程：任意线程。
```

---

## 4. 验收标准（新增 AC，给测测写单测的判定点）

沿用 `[API-007]` 编号风格，与 AC-104 / AC-701 / AC-801~808 并列。

```
[AC-901] 主卡口钳制：decline（ageGateManager.isAdultContentAllowed()==false）时，
         entitlementManager.getSelectedMaxTier() 返回值恒 ≤ FeatureGate.FREE_MAX_TIER(2)，
         无论 VIP 是否解锁、无论 saved tier 是 1~4。成年确认后返回值恢复原逻辑（VIP 可达 4）。

[AC-902] fail-closed：AgeGateManager 读取异常（SP 抛错）时，getSelectedMaxTier() 仍 ≤2；
         构造注入 ageGateManager==null 时同样 ≤2，且不得崩溃放行。

[AC-903] 全路径断言（核心可测点）：decline 后，对下列每一个抽卡入口连续抽 200 次，
         每一次返回卡片的 heatRating/tier 均 ≤2，无一张 >2：
         (a) TaskEngine.drawNext(mergedPool, mutual, maxTier=getSelectedMaxTier(), ...)
         (b) ProceduralTaskGenerator.generate(getSelectedMaxTier(), ...)
         (c) SocialGamesEngine.loadPool(allCards, filter, getSelectedMaxTier()) 后 drawNext
         (d) DateNightSessionEngine.loadPool(allCards, filter, getSelectedMaxTier()) 后
             在 WARM_UP/DEEPEN/PEAK/CLOSING 四阶段各 drawNext
         (e) 12 个 social 模式与其余模式经 getSelectedMaxTier 的抽卡
         判定方法：用 FakeSharedPreferences 写入 confirmed=true, declined=true，
         注入到 AgeGateManager → EntitlementManager，取 maxTier 后跑抽卡，断言集合 max(tier)≤2。

[AC-904] 模式入口门禁：decline 后 canAccessGameMode(mode) 对 heatRating>=3 的模式返回 false，
         对 heatRating<=2 的模式保持原 vipRequired 判定；成年后 heatRating>=3 且 VIP 解锁返回 true。

[AC-905] ConsentFilter 正交性：decline + 某卡被 HARD_NO 否决 → 该卡不入池；
         成年（ageGate 放行）+ 同卡 HARD_NO 否决 → 仍不入池。证明两者是 AND 叠加非替代。

[AC-906] 无回归：GameModeCatalog.values().length == 36（不因门禁改变）；
         Phase10/12 数量断言、TagDisplayMapper 映射、FeatureGate.resolveEffectiveMaxTier 既有
         单测（Phase4ReleaseTest:23-25）全部仍通过；AgeGateManagerTest 既有 7 项全通过。

[AC-907] 存储不变：门禁下沉后 AgeGateManager 仍只写 confirmed/declined 两个布尔键，
         PREF_NAME 仍为 "velvet_age_gate"，不新增任何 SP 键、不引入网络/账号/位置权限。
```

**给测测的建议落点**：新增 `AgeGateDowngradeTest.java`（纯 JVM，复用 `FakeContext` / `FakeSharedPreferences`），把 API-011/012 的纯函数部分做成 static helper 以便不依赖 Android 上下文直接单测（参照 `FeatureGate` 既有做法）。`DateNightSessionEngineTest.java` 追加 AC-903(d) 的 PEAK 阶段断言。

---

## 5. 风险与边界

### 5.1 不破坏既有断言

| 断言 | 风险 | 缓解 |
|---|---|---|
| 36 模式数量 | 门禁可能被误实现为"从 catalog 删除高 tier 模式" | 明确只钳 tier 值 / 只改 canAccessGameMode 返回布尔，**不动 `GameModeCatalog.values()`**；AC-906 守护 |
| TagDisplayMapper | 无直接关联 | 不改任何 mode id / tag 键；AC-906 守护 |
| `FeatureGate.resolveEffectiveMaxTier` 单测 | 若把 ageGate 塞进 FeatureGate 纯函数会破坏其"双参数"签名 | **不改 FeatureGate 的 resolveEffectiveMaxTier**，ageGate 钳制放在 EntitlementManager 层（§2.2），FeatureGate 既有 3 条断言原样通过 |
| 36/24/12 硬编码 | 模式未增删 | 无影响 |
| ConsentFilter HARD_NO | 被年龄门禁"覆盖" | 明确 AND 叠加（§2.4），AC-905 守护 |
| A 项 Phase 机 | maxTier 变小可能影响 PEAK 抽卡 | 引擎签名不变，仅 maxTier 值变小；见 §5.2 |

### 5.2 ★ 重点产品问题：decline 后只能 ≤2 火焰，A 项 PEAK 张力会不会塌掉？

**主理人明确要求回答**。结论：**不会塌，但需要切换张力策略——这是"不同用户不同张力上限"的产品边界，不是缺陷。**

**论证**：

1. **TDS 门禁本就分层**：`round14_tension_spec.md` v1.2 §1.2 已裁决——T3/T4 为强制门禁（≥15/20），**T1/T2 仅作目标值、不阻断**。decline 用户被限制在 T1/T2（≤2 火焰），按规范**本就不适用 T3/T4 强制线**。因此门禁下沉与 TDS 规范**逻辑自洽，无冲突**。

2. **T1/T2 有独立的张力空间**：T1（5~9 分）、T2（10~14 分）仍有 D1 延迟满足、D2 呼吸/温度/距离感官、D3 双向凝视、D4 边界共识、D5 私密空间五个维度可用，只是**密度目标更低**。`round14_tension_spec.md` §3.1 明确 T2 可达 SD≥1、MR≥50%、CR≥1 处——这些在 ≤2 火焰内完全可实现。

3. **PEAK 阶段的适配策略**（给源源/悠悠的具体做法）：
   - PEAK 阶段**不靠抬高 tier 制造张力**（这既违规又会在 decline 下失效），改靠 **D1 延迟满足 + D4 即时刹车 + D3 双向凝视** 的情绪密度：例如屏息停顿、"下一张先别翻"、双向问句、"你一开口我们就停"——全部是 T1/T2 允许的手法（RED-C 六手法）。
   - 成年用户（ageGate 放行）PEAK 可用 T3/T4 的高密度文案（仍封顶 Suggestive/Mild）；decline 用户 PEAK 用 T2 密度文案。**两套文案都属于金标准文风，只是浓度不同**——这正是 §3.2"金标准作锚点、不逐字落盘"的价值。
   - 4 阶段 Phase 机（WARM_UP→DEEPEN→PEAK→CLOSING）的**阶段结构与抽卡策略不变**，只是 maxTier 上限从 4 变 2；张力由"情绪密度随阶段递增"（AC-803 的 SD/ED/MR 单调递增）承载，而非"火焰数递增"。

4. **产品边界声明（须写进对用户的解释）**：decline 用户是**主动选择未满 18 岁路径**的用户，其可获得的体验上限本就应低于成年用户——这是合规要求，也是软门禁的意义（不硬阻断 App，但降低强度）。保住这部分用户的张力靠的是 T1/T2 的情绪密度，而非突破 tier 上限。

**一句话给主理人**：门禁下沉后 A 项 PEAK 不会塌——因为张力从来不是靠第 4 火焰撑的，而是靠屏息、留白、双向凝视和"你一开口就停"；这些在 ≤2 火焰内照样能拿满 T2 的 14 分。

### 5.3 其他风险

- **状态时序**：用户在高 tier 模式 Fragment 停留期间被改为 decline（测试/注入场景）。缓解：`getSelectedMaxTier()` 是**每次抽卡实时读取**，不缓存——下一次抽卡立即落入 ≤2 钳制，无需重启 Fragment。（AgeGateManager 的 SP 读取本就是实时的。）
- **VIP 预览构建**：`BuildConfig.VIP_PREVIEW_ENABLED=true` 时默认 tier=4，但 ageGate 钳制在其后执行，decline 仍 ≤2，不受 preview 开关影响。AC-901 覆盖。
- **性能**：每次抽卡多一次 SP 布尔读取（`isAdultContentAllowed` → 两次 `getBoolean`），耗时可忽略（μs 级），且在 IO/现有路径内，不新增阻塞。
- **合规口径**：本方案属"收紧"（decline 后限制更严），不触碰任何 RED 红线，不新增权限/网络/账号；需安安 Phase 4 终裁确认与 RED-B（成年人边界）一致。

---

## 6. 落地分工与待确认

| 角色 | 动作 | 依赖 |
|---|---|---|
| android-data（源源） | 落地 API-011/012：EntitlementManager 构造注入 AgeGateManager、getSelectedMaxTier 叠加钳制、canAccessGameMode 叠加 heatRating 判定；AppContainer 装配 | 本文 §2.2/§2.3 |
| android-test（测测） | 新增 AgeGateDowngradeTest，实现 AC-901~907；DateNightSessionEngineTest 追加 PEAK 断言 | API-011/012/013 |
| android-security（安安） | Phase 4 终裁：确认下沉与 RED-B 一致、fail-closed 到位、无新红线命中 | 本文 §5.3 |
| 架架（本人） | 本次只出方案；重评/定稿后按通知复核 | — |

**待确认项**（交回主理人，不自行推断）：
1. **模式入口补充卡口（§2.3）是否本轮一并落地**？主理人任务卡聚焦"抽卡 ≤2 火焰"（内容层），模式入口卡口是额外防线。若本轮只做主卡口，则 decline 用户仍可能进入 VIP 模式看固定内容池（positions/foreplay 的 addAll）——需主理人裁决是否纳入本 P0 范围。
2. **A 项 PEAK 的 T2 密度文案**由谁撰写？建议 android-data 按 §5.2 策略产出 T2 版 PEAK 提示语（受 RED 红线约束），需主理人确认归属。
3. **既有"成年用户"PEAK 文案是否已经是 T3/T4**？若是，则成年/decline 两套文案需要并存，是否引入按 ageGate 分支选文案的逻辑（涉及 strings key 增量）由主理人裁决。

---

> **方案结束**。全程未修改任何代码、布局、strings.xml 或内容 JSON；本轮唯一写入为本文件。所有判定口径与 `round14_tension_spec.md` v1.2 / RED 红线一致。
> 关联：`round14_tension_spec.md`（v1.2，TDS 门禁分层）、`round13_phase3_architect_spec.md`（A项 Phase 机/B项年龄门/C项日记）、`round13_phase1_security.md`（RED 红线）。

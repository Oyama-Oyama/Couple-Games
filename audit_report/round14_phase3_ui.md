# Round 14 · Phase 3 · UI 私密氛围审计与张力增强（新增需求 5）

> 负责人：**悠悠（android-ui）** · Android Studio 专家团 · UI / 交互专家
> 任务卡：team-lead / 铮铮 下发的「第 5 项：当前软件 UI、游戏题目设计是否有足够的性张力、情侣私密氛围性」UI 侧任务
> 依赖规格：`audit_report/round14_tension_spec.md` v1.1（架架，含 AC-801~808）· `round13_phase1_security.md`（安安 RED 体系）· `round13_phase3_architect_spec.md`
> 工程：`D:\AndroidProject\VelvetCouples`
> 写权限：`res/layout/**` · `res/values*/strings.xml` · `ui/**` Java
> **前序报告**：`audit_report/round13_phase3_ui.md`（已验收）

---

## 0. 结论摘要（精确数字）

| 维度 | 数字 |
|---|---|
| 审计界面总数 | **54** 布局（49 存量 + 5 新增）+ **12** 套 velvet 主题 + **36** Fragment + **684 × 8** 条 strings |
| UI 氛围增强落地处数 | **12** 处（AC-807 五项 + 揭晓弹簧/haptic + 安全条可读性 + 空状态 + 7 组文案张力等，见 §3）|
| 文案张力/红线改写 | **128** 条 = **16 key × 8 语言**（9 处红线残留 + 7 处 T 档增强）|
| 新增 string key | **0** |
| key 总数 | **684 × 8 完全一致**（未变），空串 0，重复 0，占位符 100% 对齐 |
| 权限集合 | `{VIBRATE}`，未新增 |
| 模式数 | **36**，未变；`GameModeCatalog` 未触碰 |
| 校验 | `assembleDebug` / `check_strings_mix` / `verify_content` / `sl_lint` / `dp_lint` / 8 语言 key 自检 —— **全部 EXIT=0** |

**一句话回答第 5 项**：上一轮「删而未补」导致张力浓度不足——动作清单清掉了，但感官、双向凝视、空间限定没有补位。本轮在不回退任何 8 组降调、不新增 key、不越 RED-A2 的前提下，用 **D2 非部位感官 + D3 双向视角互换 + D5 空间限定 + D4 逼近与即时刹车同句** 补足，并以 AC-807 五项把「仪式感」从文案延伸到界面行为。

---

## 1. 任务落地对照

### 1.1 任务 A · consent 空状态（上轮 §6 #7 裁决）

**已落地，方案 A，零新增 key。**

| 项 | 内容 |
|---|---|
| 布局改动 | `res/layout/fragment_consent.xml` 新增 `layout_consent_empty` 卡片（`bg_card_velvet_glow` + 衬线标题 + `consent_desc` 正文，默认 `gone`）|
| Java 改动 | `ui/consent/ConsentFragment.java`：新增 `rulesEmpty` 字段、`updateEmptyState()`；`observeRules()` 回调中补 `renderStageUi()`；私密递机流程中隐藏空状态卡 |
| 文案复用 | `consent_title`（标题）+ `consent_desc`（行动指引「点按任一方切换 YES / SOFT / HARD NO」）|
| 是否「暂无数据」式 | ❌ 否——`consent_desc` 本身是明确的下一步行动指引，符合主理人「不要冷冰冰罗列」约束 |
| key 影响 | **0**（8 语言仍 684）|

**边界处理**：`PRIVATE_SUMMARY` 阶段优先展示隐私汇总卡，不叠加空状态卡；`rulesEmpty` 默认 `true`，避免首帧出现空白列表。

---

### 1.2 任务 B · UI 私密氛围审计与增强

架架 v1.1 把 UI 建议分两批，**AC-807 要求本轮落地 5 项**。落地情况：

| # | AC-807 项 | 状态 | 落点 |
|---|---|---|---|
| 1 | 揭晓前 300~600ms 屏息停顿 | ✅ | `ui/dailyreveal/DailyRevealFragment.java`：450ms `View.postDelayed`；**点击页面任意处可立即跳过**（RED-A3 不强制）；`onDestroyView()` 清 callback |
| 5 | 暂停条「安静地存在」 | ✅ | `fragment_date_night.xml` 的 `tv_dn_safe`：常驻、minHeight 48dp、muted 色、12sp → **13sp**（可读性微增但不抢主操作）。**未移除**，RED-B3 出口完好 |
| 6 | 进度用「间距收窄」而非变红 | ✅ | `fragment_date_night.xml` 四段指示初始 8dp；`DateNightFragment.applyStageSpacing()` 用 `ValueAnimator` 420ms 缓出，按阶段收窄 8→6→4→2dp；**无红色渐变** |
| 7 | 去任务面板感 | ✅ | `tv_dn_progress`（「第 x/y 张」计数）设为 `gone`，阶段位置由 4 段指示承载；全量扫描确认会话布局无「00:20 剩余」「请勿跳过」类提示；真实 TIMED 玩法计时保留（避免功能失真）|
| 8 | 里程碑/徽章会话中隐藏 | ✅ | `activity_main.xml` 的 `tv_game_heat_badge` 设 `gone`（该徽章位于 `layout_game_topbar`，仅会话期可见）；主页 `card_milestones` 不变；强度信息仍由卡面 meta 与进入前模式卡承载 |

> **口径冲突的处理说明**：主理人 Round 14 消息早于架架 v1.1 规格，其中提到暂停条应为「显眼可信的出口」；架架 §5 #5 / AC-807（主理人已批准）要求「低对比度、安静存在、不喧宾夺主」。**以后到的已批准规格为准**，折中为「常驻 48dp + muted 色 + 13sp」——不再是小字，也不与强调色主操作竞争。

**额外增强（非 AC-807 强制，低风险）**

| 项 | 落点 |
|---|---|
| 抽卡揭晓不再瞬切 | `DateNightFragment.drawNext()`：新文案落位后 `HapticHelper.tick` + `PhysicsAnimator.popIn` 轻柔弹簧入场 |
| 空状态温暖化 | §1.1 的 consent 卡采用丝绒发光背景 + 衬线标题 |

**架架标注「下一轮」的 3 项（本轮只出方案、不改代码，符合 AC-807）**

| # | 建议 | 本轮方案 |
|---|---|---|
| 2 | 动效速度分阶段 | 给 `PhysicsAnimator.flipCard3D` 增加时长入参，WARM_UP ~250ms / PEAK ~600ms；不把翻卡节奏做成任务 |
| 3 | 深色主题呼吸光晕 | `bg_card_velvet_glow` 透明度 0.15~0.3、2s 循环 `ValueAnimator`；必须遵循系统「减少动态效果」且不影响 FLAG_SECURE |
| 4 | 长按触觉锚点 | 在现有 `HapticHelper.tick/confirm/countdownFinished` 之外增加更轻的 tick 档；不新增 VIBRATE 之外的权限 |

---

### 1.3 任务 C · 界面文案张力落地

**依据**：架架 §3.2 T4 金标准**仅作结构锚点、未逐字照抄**（v1.1 用途裁决）；§4.3 的 7 个待增强 key 全部处理；`paywall_benefits` 按裁决**保持中性**（商店 / RED-D4）。

结构模板：`空间限定 → 非部位感官 + 视角互换 → 双向问句 → 逼近 + 即时刹车同句`。

**改写示例**

```
tier_desc_4 en（改写后）:
  "Just lamplight and the two of us… the silence carries your breath, or maybe mine.
   Who looked away first? A little nearer — and the moment you say stop, we stop."
tier_desc_4 zh-CN（改写后）:
  "只剩一盏灯和我们…安静里裹着你的呼吸，也许是我的。是谁先移开视线的？
   再近一点——你一开口说停，我们就停。"
```

**自评 TDS（架架五维，1~5/维；正式重评由 team-lead 在定稿后发起）**

| key | D1 延迟 | D2 感官 | D3 双向 | D4 可控 | D5 专属 | 自评 | 档位门槛 | 判定 |
|---|---|---|---|---|---|---|---|---|
| `tier_desc_1` | 1 | 2 | 3 | 0 | 2 | **8** | T1 ≥5 | ✅ |
| `tier_desc_2` | 2 | 3 | 3 | 1 | 2 | **11** | T2 ≥10 | ✅ |
| `tier_desc_3` | 3 | 4 | 4 | 4 | 3 | **18** | T3 ≥15 | ✅ |
| `tier_desc_4` | 4 | 5 | 5 | 5 | 5 | **24** | T4 ≥20 | ✅ |
| `mode_ladder_desc` | 4 | 3 | 2 | 4 | 3 | **16** | T3 ≥15 | ✅ |
| `mode_auction_desc` | 3 | 3 | 3 | 4 | 2 | **15** | T3 ≥15 | ✅ |
| `mode_ldr_sync_desc` | 4 | 3 | 2 | 3 | 2 | **14** | T2 ≥10 | ✅ |

> 架架 §4.1 的基线是 8 组平均 ≈5.6/25；本轮 7 个非功能 key 自评平均 **15.3/25**。正式分数以 team-lead 定稿后重评为准。

**RED 边界自查（全部通过）**

- **零部位词**：扫描 16 个 key × 8 语言，无 `lick/nibble/spank/轻舔/轻咬/惩罚/前戏/foreplay` 等 RED-A2 命中。
- **T3/T4 即时刹车保留**：`tier_desc_3`「你说停，我们就停」、`tier_desc_4`「你一开口说停，我们就停」、`mode_ladder_desc`「哪一步想停随时都能停」、`mode_auction_desc`「任何一方随时都能结束」——P7 / RED-B3 全在。
- **单向施动-受动清除**：`foreplay_lead_fmt` 原「主导：A → 享受：B」改为「A 与 B —— 轮流带着走，随时换」；`mode_roleplay_desc` 原「攻受互换 / lead-receiver」改为「角色随时互换」。
- **RED-C-0**：8 语言强度一致，无任一语言强于英文源；ja 用「そっと / …か」，ko 用 해요体且无汉字/假名，fr 全部 `'` 转义且用 `tu`，es 用 `tú`，pt-BR 用 `você / pra / a gente`。
- **RED-P-1**：`mode_ldr_sync_desc` 新文案无任何 `sync / 同步 / 同期 / 동기화` 数据流词；`mode id "ldr_sync"`、`GameModeCatalog.LDR_SYNC`、`TagDisplayMapper` 的 `"LDR_SYNC"` 键均未动。

### 1.4 额外红线残留修复（Round 13 遗漏项）

全量复扫发现 9 个 key 仍残留 **RED-A2/A3 命中或绩效压迫措辞**，已按「红线优先于调性」直接改写（8 语言）：

| key | 原文（节选） | 改写后（en 节选） | 命中 |
|---|---|---|---|
| `ladder_teaser_header` | `NONSTOP FOREPLAY` / 全程不停挑逗 / 止まらず焦らし続ける | `Ten Steps at Your Pace` | RED-A2 `foreplay` + RED-A3 `nonstop` |
| `ladder_answerer_header` | `ANSWER WITHOUT BREAKING` | `Answer in Your Own Words` | RED-A3 胁迫语义 |
| `ladder_btn_climb_next` | `Survived ✓` | `Together ✓ · Next Step →` | 绩效压迫 |
| `ladder_btn_broke_composure` | `Lost Composure!` / 没忍住破防 / 耐えきれなかった | `Your heart skipped a beat — bonus dare for you` | 绩效羞辱 |
| `heartbeat_forfeit_status` | 没忍住提前松手了 | `lifted first — a new card is waiting` | 绩效羞辱 |
| `heartbeat_sync_success` | `both held the tension` / 都扛住了 | `Our hearts found the same rhythm — look at each other` | 耐力绩效 |
| `foreplay_lead_fmt` | `Leading: A → Receiving: B` | `A & B — take a turn leading, then trade places` | 单向施动-受动 |
| `mode_roleplay_desc` | 攻受互换 / lead-receiver roles | `roles you can swap anytime` | 单向施动-受动 |
| `bomb_ready_prompt` | `Perform the intimate relay fast` | `Share one quiet challenge, then pass the phone before time is up` | RED-A3 催促 |

---

## 2. 第 5 项 UI 氛围评估

> 架架的 TDS 是**文案**评分表，不覆盖界面。以下为我自定的 **5 维 UI 氛围量表（1~5 分，5 为最佳）**，供主理人参考；**不替代**架架/安安对文案的正式评分。

### 2.1 12 套 velvet 主题

评分子维度：**夜色深度 / 材质层次 / 对比安全 / 强调色克制 / 丝绒光晕**。

实测 WCAG 对比度（`text/bg` 16.72~18.05:1，`muted/bg` 7.10~8.80:1，`accent/bg` 7.21~13.71:1）——**全部远超 AA，深色主题没有牺牲可读性**。12 套均为三层表面（`bg` / `surface` / `surface_elevated`）+ 低透明度描边（`0x40~0x66`），不是单纯换色。

| # | 主题 | bg | 夜色 | 层次 | 对比 | 克制 | 光晕 | 均分 |
|---|---|---|---|---|---|---|---|---|
| 1 | Velvet Noir | `#121013` | 5 | 5 | 5 | 5 | 4 | **4.8** |
| 2 | Peach Blush | `#221117` | 4 | 4 | 5 | 3 | 4 | **4.0** |
| 3 | Sakura Silk | `#1E101A` | 4 | 4 | 5 | 3 | 4 | **4.0** |
| 4 | Blush Rosé | `#1A1116` | 5 | 4 | 5 | 4 | 4 | **4.4** |
| 5 | Midnight Plum | `#120E1D` | 5 | 5 | 5 | 4 | 4 | **4.6** |
| 6 | Scarlet Lace | `#180B0F` | 5 | 5 | 5 | 4 | 4 | **4.6** |
| 7 | Violet Mist | `#140E21` | 5 | 5 | 5 | 3 | 4 | **4.4** |
| 8 | Candlelight Amber | `#17110E` | 5 | 4 | 5 | 4 | 5 | **4.6** |
| 9 | Crimson Neon | `#0D0C18` | 5 | 5 | 5 | 3 | 4 | **4.4** |
| 10 | Rouge Velvet | `#180A0E` | 5 | 5 | 5 | 4 | 5 | **4.8** |
| 11 | Moonlit Lace | `#0C0E1C` | 5 | 5 | 5 | 3 | 4 | **4.4** |
| 12 | Honey Dusk | `#180F0A` | 5 | 4 | 5 | 4 | 5 | **4.6** |

**结论**：12 套均达「夜色 / 私密」基本盘（均分 4.0~4.8）。扣分集中在强调色偏亮（Peach / Sakura / Violet / Neon / Moonlit 的 accent/bg 达 10.7~13.7:1），属**风格选择而非缺陷**——暗底上的高亮强调色仍服务于可读性，本轮不强行改色，列入下一轮「呼吸光晕」方案一并校准。

### 2.2 关键界面

量表：**色彩材质 / 揭晓节奏 / 触摸反馈 / 专属感 / 安全出口**（均为改后自评）。

| 界面 | 色彩材质 | 揭晓节奏 | 触摸反馈 | 专属感 | 安全出口 | 均分 | 本轮变化 |
|---|---|---|---|---|---|---|---|
| 主页 `HomeFragment` | 5 | 3 | 4 | 4 | 4 | **4.0** | 无变化 |
| 约会夜 `DateNightFragment` | 5 | **5** | **5** | 5 | 4 | **4.8** | +间距收窄 / +弹簧揭晓 / +tick / 计数隐藏 / 暂停条 13sp |
| 每日揭晓 `DailyRevealFragment` | 5 | **5** | 5 | 4 | 4 | **4.6** | +450ms 屏息停顿（可跳过）|
| 盲配 `BlindMatchFragment` | 5 | 4 | 5 | 4 | 4 | **4.4** | 未改（已有 3D 翻卡 + confirm）|
| 情书 `SealedLetterFragment` | 5 | 4 | 4 | 5 | 4 | **4.4** | 未改（已有延时解锁 + flipCard3D）|
| 边界 `ConsentFragment` | 5 | 3 | 4 | 4 | 5 | **4.2** | +暖色空状态卡 |
| 今晚一句话 `TonightNote*` | 5 | 3 | 4 | 5 | 4 | **4.2** | 无变化（默认折叠摘要已足够克制）|
| 支付页 `PaywallDialogFragment` | 4 | 2 | 3 | 2 | 3 | **2.8** | 文案按裁决保持中性；商店页天然弱氛围，符合 RED-D4 |
| 设置页 `SettingsFragment` | 4 | 2 | 3 | 3 | 4 | **3.2** | 功能页，不追求张力 |

### 2.3 破坏氛围元素专项扫描

| 检查项 | 结果 |
|---|---|
| 广告位 / 第三方 SDK 水印 | ✅ 0（权限集合仅 `{VIBRATE}`，无广告 SDK）|
| 账号登录提示 | ✅ 0（除 Billing 事实陈述外；8 语言 `paywall_play_unavailable` 均已明确「需网络 + Play 账号」，无「no account needed」矛盾表述）|
| 过亮主色 | ⚠️ 5/12 主题强调色偏亮（见 §2.1），列为下轮候选，不阻断 |
| 过大字号 | ✅ 0（标题 22sp、正文 13~17sp，均 sp 随系统缩放）|
| 机械感进度条 | ✅ 会话内计数已隐藏；`progress_social` 为既有进度条，`progressTint` 用 `velvetAccent`（非红色渐变）|
| 功能说明书式措辞 | 684 key 中 27 个（3%）含 `Tap / 请点击 / 设置` 等标记——均为**操作必需提示**，非「用户 / 系统」式冷罗列；专属感标记 52 个（7%）|
| 同步/数据流动词（RED-P-1） | ✅ 显示文案中 0 |

---

## 3. 校验结果

```
$ ./gradlew :app:assembleDebug --offline          → BUILD SUCCESSFUL   EXIT=0
$ python scripts/check_strings_mix.py             → OK: no script contamination   EXIT=0
$ python scripts/verify_content.py                → ✅ 全部通过   EXIT=0
$ python scripts/sl_lint.py                       → OK: no script contamination   EXIT=0
$ python scripts/dp_lint.py                       → OK: no Latin contamination    EXIT=0
$ python /tmp/vc_keycheck.py（一次性，不在 scripts/）→ PASS   EXIT=0
```

**8 语言 key 一致性**：en / zh-CN / zh-TW / ja / ko / es / fr / pt-BR 各 **684**，集合完全一致；空串 0；重复 0；占位符 100% 对齐；新增前缀 key 仍为 32（无新增）。

**应用内 string 引用解析**：662 个 `@string/...` / `R.string...` 引用（已排除 `android.R.string.ok/cancel` 框架资源）在 8 语言下**全部解析成功**。

> **过程中的一次外部失败（已澄清并恢复）**：`verify_content.py` 曾一度 EXIT=1，报 9 项 `fate_cubes.json` ID_MISSING 与 `generator_templates.json` PLACEHOLDER_MISMATCH。经查全部来自 **android-data 于 2026-11-24 10:00 修改的 `assets/content/**`**，日志中**无任何 strings.xml 错误**；我未触碰该域。对方修复后重跑已 EXIT=0。

**硬约束复验**

| 项 | 结果 |
|---|---|
| 8 语言 key 集合一致 / 无空串 | ✅ 684 × 8 |
| 权限集合 | ✅ 仅 `VIBRATE`；本轮 haptic 复用既有 `HapticHelper`，无新权限 |
| 模式数 / `GameModeCatalog` | ✅ 36，未触碰 |
| `core/session/**` `core/journal/**` | ✅ 未触碰（边界维持）|
| `assets/content/**` / `scripts/**` / `app/build.gradle` | ✅ 未触碰；一次性脚本全部在 `/tmp` |

---

## 4. 变更文件清单

**布局（`res/layout/**`）**
- `fragment_consent.xml` — 新增空状态卡
- `fragment_date_night.xml` — 阶段间距 8dp / 暂停条 13sp / `tv_dn_progress` 隐藏
- `activity_main.xml` — `tv_game_heat_badge` 会话中隐藏

**字符串（`res/values*/strings.xml` × 8）**
- 16 key × 8 语言 = **128** 条改写（9 红线残留 + 7 张力增强）；**0 新增 key**

**`ui/**` Java**
- `ui/session/DateNightFragment.java` — `applyStageSpacing()`、`popIn`+tick 揭晓、阶段索引同步
- `ui/dailyreveal/DailyRevealFragment.java` — 450ms 屏息停顿（可跳过）+ 生命周期清理
- `ui/consent/ConsentFragment.java` — 空状态接线

---

## 5. 遗留与建议

| 项 | 状态 |
|---|---|
| 架架「下一轮」3 项（动效速度分阶段 / 深色呼吸光晕 / 长按触觉锚点） | 本轮按 AC-807 **只出方案不改代码**，见 §1.2 |
| 5/12 主题强调色偏亮 | 非缺陷，建议下轮「呼吸光晕」一并校准 |
| `paywall_benefits` 商店文案 TDS=1 | 按主理人裁决**保持中性**，不增强（RED-D4）|
| Phase 4 引擎阶段推进回归 | 已请主理人转达稳稳：抽满暖场 4 张 → 靠近一点 → 心跳（确认无倒计时）→ 收尾 |
| 正式 TDS 重评 | 待主理人在我定稿后发起；本轮自评仅供校准 |

---

## 6. P0 Dialog 按钮失效修复（主理人紧急指派，优先于第 5 项）

### 6.1 现象与根因

稳稳在模拟器实测：**18+ 年龄确认弹窗的两个按钮都只关窗**——不写 `velvet_age_gate.xml`、不恢复 navHost，首启后永久白屏（只剩底栏），重启反复弹窗，App 完全不可用。`TonightNoteDialogFragment` 的保存按钮同样只关窗不保存。

根因（AOSP 生命周期）：两处都是同一错误范式——
1. `setPositiveButton(res, null)` / `setNegativeButton(res, null)` → 传 `null` 时 `AlertController` 走**默认 auto-dismiss**；
2. 在 `onStart()` 中先 `super.onStart()`（DialogFragment 此时已发出 SHOW 消息），**之后**才 `dialog.setOnShowListener(...)`；而 `Dialog.setOnShowListener` 只是把 listener 存入 `mShowMessage`，**不会补发 SHOW 消息** → listener 永不执行 → 按钮走默认 dismiss。

### 6.2 修复（两文件，同一范式）

| 文件 | 改动 |
|---|---|
| `ui/onboarding/AgeGateDialogFragment.java` | `:39` 新增 `declined` 标志；`:54-61` 正按钮改为 `setPositiveButton(..., listener)`，内部 `confirm(false)`（同步落盘）→ 恢复 navHost → `dismissAllowingStateLoss()`；`:62-68` 负按钮改为 listener，内部 `confirm(true)` 落盘 → `showDeclinedState()`，**不 dismiss**（软门禁就地切换）。**删除整个 `onStart()` 的 `setOnShowListener` 段**；`showDeclinedState()` 不再用 `setOnClickListener` 覆盖正按钮，只切 `declined` 标志。 |
| `ui/journal/TonightNoteDialogFragment.java` | `:44-45` 正/负按钮改为 listener：正按钮直调 `save()`（含 `saveNote` + toast，**成功才 dismiss**，失败留弹窗可重试）；负按钮 `dismissAllowingStateLoss()`。`onStart()` 删除 `setOnShowListener`，仅保留 `prefill()`；移除无用 `DialogInterface` import。 |

**写盘契约核对**：`AgeGateManager.confirm()` 使用 `SharedPreferences.edit().commit()`（`:60`）**同步落盘**，满足架架 §2.2「写盘先于关窗」；读失败 fail-closed，不会默认放行。

**第三处同类自查**：全局 grep 确认 `ui/**` 已无 `setOnShowListener`。剩余 7 处 `setButton(..., null)` 全部是「取消 / 关闭 / 继续 / 确定」按钮，默认 dismiss 本就是预期行为，且**无配套 onStart 监听**，不构成同类 bug：

`MainActivity:143`、`CoupleBoardFragment:235`、`HomeFragment:367`、`SealedLetterFragment:205`、`DateNightFragment:280`、`SettingsFragment:126/223`。

**验证**：`./gradlew :app:assembleDebug --offline` → **BUILD SUCCESSFUL，EXIT=0**。运行时复验已请稳稳执行（首启两按钮写盘 + navHost 恢复、软门禁不锁死、今晚一句话保存落 SP）。

---

## 7. TDS 张力重评（架架定稿后正式重评用）

### 7.1 改写前 / 后对照（架架 §4.1 基线 → 我方自评）

| key | 改写前 TDS | 改写后自评 | 门槛 | 判定 |
|---|---|---|---|---|
| `tier_desc_1` | 3 | **8** | T1 ≥5 | ✅ |
| `tier_desc_2` | 3 | **11** | T2 ≥10 | ✅ |
| `tier_desc_3` | 9 | **18** | T3 ≥15 | ✅ |
| `tier_desc_4` | 9（T4 最严重缺口） | **24** | T4 ≥20 | ✅ |
| `mode_ladder_desc` | 8 | **16** | T3 ≥15 | ✅ |
| `mode_auction_desc` | 6 | **15** | T3 ≥15 | ✅ |
| `mode_ldr_sync_desc` | 6 | **14** | T2 ≥10 | ✅ |
| `paywall_benefits` | 1 | **1（豁免，保持中性）** | — | 按裁决 |

7 个非功能 key 自评平均 **15.3/25**（基线 5.6/25）。正式分数以 team-lead 定稿后发起的 AC-801~808 重评为准。

### 7.2 最终文案清单（供架架逐条评分）

| key | en 最终值 |
|---|---|
| `tier_desc_1` | A quiet corner, just us… the conversation wanders and our eyes keep finding each other. |
| `tier_desc_2` | The music slows and my breath catches — or was that yours?… come closer, let's see where the teasing goes. |
| `tier_desc_3` | The room is quiet and close… I hear your breath, or maybe my own. We take our time — and the moment you want to stop, we stop. |
| `tier_desc_4` | Just lamplight and the two of us… the silence carries your breath, or maybe mine. Who looked away first? A little nearer — and the moment you say stop, we stop. |
| `mode_ladder_desc` | Ten quiet steps tonight, just the two of us — a truth or a dare, with your breath setting the pace. Pause on any step you like. |
| `mode_auction_desc` | Tonight your heartbeat runs ahead of the chips — you both bid, and either of you can end the round anytime. |
| `mode_ldr_sync_desc` | One countdown you both watch — the silence between breaths grows shorter. Stop counting whenever you like; the moment is already yours. |

zh-CN 对照：

| key | zh-CN 最终值 |
|---|---|
| `tier_desc_1` | 安静的角落，只剩我们…话慢慢说，目光却总在彼此那里停一下。 |
| `tier_desc_2` | 音乐慢下来了，是我的呼吸乱了，还是你的…？再靠近一点，看看这份试探会走到哪里。 |
| `tier_desc_3` | 夜很静，房间只剩我们…我听见你的呼吸，也许是我自己的。慢慢来——你说停，我们就停。 |
| `tier_desc_4` | 只剩一盏灯和我们…安静里裹着你的呼吸，也许是我的。是谁先移开视线的？再近一点——你一开口说停，我们就停。 |
| `mode_ladder_desc` | 今晚十步，只有我们俩——真心话或小挑战，让呼吸替我们定节奏。哪一步想停，随时都能停。 |
| `mode_auction_desc` | 今晚，心跳比筹码先一步——两个人一起出价，任何一方随时都能结束这一轮。 |
| `mode_ldr_sync_desc` | 两个人一起看着同一个倒计时——呼吸之间的空隙越来越短。不想数了随时说，此刻已经是你们的。 |

其余 6 语言（zh-TW / ja / ko / es / fr / pt-BR）以 `res/values-*/strings.xml` 中上述 key 的落盘值为准，均已按各自语言习惯等价创作，未逐字套金标准。

**红线零增加自查**：16 个改写 key × 8 语言扫描，`lick/nibble/spank/轻舔/轻咬/惩罚/前戏/foreplay/sync/同步` 等 8 组降调关键字**零回退**；T3/T4 即时刹车句式全部保留；无身体部位词；`RED-C-0` 各语言强度不高于英文源。

---

---

*报告完毕 · 悠悠 / android-ui · Round 14 Phase 3 · 新增需求 5 + P0 修复*

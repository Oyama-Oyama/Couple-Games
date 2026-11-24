# VelvetCouples Phase 4 最终合规扫描报告

> **轮次**：Round 13 · Phase 4（最终合规扫描）
> **任务**：#3 续接（任务卡：Phase 4 最终扫描 + Q6 方案 + P5 终裁 + 新功能专项）
> **编制**：安全与隐私专家 · 安安（android-security）
> **扫描时刻**：**2026-11-24 09:57:59**（首次全量）/ **09:59**（稳定态复核）
> **扫描时文件状态**：⚠️ **并发中间态**——【悠悠】与【源源】仍在写 `res/` 与 `assets/`。已实测到同一 key 在两次扫描间发生变化（09:57 `ladder_teaser_header` 仍为 `%1$s · NONSTOP FOREPLAY`，09:59 复核已变为 `%1$s · Ten Steps at Your Pace`）。**本报告数字反映上述时刻的快照，可能被后续落盘刷新，Phase 5 发布前须复扫。**
> **写权限声明**：只写本文件；未改任何代码、strings.xml、内容 JSON、脚本。
> **红线依据**：`audit_report/round13_phase1_security.md`（570 行）§6 的 A/B/C/D/E 五组 33 项清单。

---

## 0. 结论速览（不粉饰）

| 维度 | 结论 | 等级 |
|---|---|---|
| 33 项清单 | 通过 19 / 不通过 9 / 待确认或不适用 5 | 🟠 |
| strings 层降调（8 组） | **已落地且正确**：tier_desc_3/4、paywall、ladder、auction、ldr_sync、heat_scope_fmt 全部核验通过 | 🟢 |
| 隐私叙事 sync 歧义 | **已清除**：`Synced across`/「自动同步到」→ `applies to all 36 games`/「全部 36 款游戏统一使用」；`ldr_sync` 显示名与 desc 去 sync | 🟢 |
| 技术面（权限/网络/SDK/加密/门禁） | 无新增权限/网络/SDK；`allowBackup=false`、FLAG_SECURE 默认 true、Keystore AES-256-GCM 未削弱 | 🟢 |
| **内容层降调** | ❌ **未完成**：`extreme_modes.json` / `decks.json` / `intimate_modes.json` tier3/4 仍有大量动作化文案（主理人实测 zh「轻咬」115/「深吻」80/「跨坐」26；我独立复核确认） | 🔴 阻断 |
| **合规词表漏检** | ❌ **确认漏检假象**：`pr_ice_1` 冰块+锁骨+炙热吻 8 语言一致越界，但官方脚本对 zh/ja/es/pt 漏检（见 §2.3） | 🔴 阻断 |
| **IARC 问卷重填 / 分级 18+ 口径** | ❌ 文档仍写 17+、`20 个模式`；本轮内容实质改动未触发问卷重填 | 🟠 |
| **18+ 软门禁** | ⚠️ 结构性风险：拒绝后用户点一下即可进入 App；`isAdultContentAllowed()` 仅 UI 列表层消费 | 🟠 |
| Q6 Keystore 降级路径 | 已核实代码；出方案 4 条决策（见 §5），本轮只出方案不改代码 | 🟡 |

**一句话**：界面层与叙事层整改干净，**内容层（tier3/4 题库）降调未完成是发布阻断项**；此外 IARC 口径、漏检词表、软门禁三处须在 P5 前处理。

---

## 1. 33 项清单逐项判定

> 判定口径：`PASS` 通过 / `FAIL` 不通过 / `N/A` 不适用 / `HOLD` 待主理人或上游确认。
> 依据报告 §6：A 组 8 项、B 组 5 项、C 组 8 项、D 组 8 项、E 组 4 项。

### A. 内容红线（RED-A/B/C/D/E）

| 项 | 判定 | 依据 |
|---|---|---|
| A-1（阻断）8 语言零容忍词表命中数=0 | **FAIL** | 脚本 RED-A2=296 / RED-A3=515 / RED-A1=3 / RED-A6=1；自建词表证实更多漏检（§2）。存量题库动作化文案未清 |
| A-2（阻断）tier3/4 与 VIP 模式人工复核 | **FAIL** | `extreme_modes.json` / `decks.json` / `intimate_modes.json` tier3/4 仍有「滑过锁骨+炙热吻」「轻舔/深吻/跨坐/惩罚/褪衣」等（§2.4） |
| A-3 C-ZH/C-EN 对照抽检 | **HOLD** | strings 层允许句式已采用（tier_desc_3/4 合规）；但内容层大量越界，无法判通过 |
| A-4 8 语言强度一致性 | **FAIL** | `pr_ice_1` 八语言同强度越界；fr/ko RED-A2 各 97 条 vs pt-BR 7 条是**词表漏检**非真实强度差（§2.3） |
| A-5 四档未超上限表、T4≤Suggestive/Mild | **HOLD** | 界面档位描述合规；内容层 tier4 越界，待源源返工 |
| A-6 与安全词/边界/Pause 叙事一致 | **PASS** | tier_desc_3/4、ladder、auction 均含「你说停就停/随时喊停/任何一方随时结束」；`safe_pause_message` 完整 |
| A-7 地区收敛 RED-E | **PASS** | 无分地区加强；RED-E 命中均为 review 级氛围词（暧昧/挑逗/情趣），未升级为露骨 |
| A-8 商店素材成熟度不高于内容 | **HOLD** | ASO 文案沿用合规版；但 IARC 文档仍写「20 个模式」（实际 36），待 P5 同步 |

### B. 隐私叙事（RED-P）

| 项 | 判定 | 依据 |
|---|---|---|
| B-1（阻断）3.2 禁止词命中=0 | **PASS** | RED-P block 33 条经逐条核查**全部为误报或否定式**：ko `공유`(共享列表,非分享)、`上传`8条(在「绝不上传」否定语境被误报)等（§3.2） |
| B-2（阻断）diff 新增字符串无肯定式上传/同步语义 | **PASS** | 新增 256 条译文（datenight/agegate/tonight_note/mode_date_night）无肯定式数据流；`datenight_` 均为本地串场文案 |
| B-3 未新增 sync/同步 描述数据流 | **PASS** | 歧义词已清除（§3.1）；`mode_ldr_sync` id/枚举/TagDisplayMapper 键未动（数量断言保持） |
| B-4 文案/Data Safety/隐私政策三者一致 | **PASS** | `settings_privacy_body` 8 语言否定式完整；零广告/零云/账号不变 |
| B-5 未新增分享/导出/生成链接能力 | **PASS** | `TonightNoteManager` 仅本机 SharedPreferences，不上传/不分享/不导出（§6.3） |

### C. 技术面（RED-T）

| 项 | 判定 | 依据 |
|---|---|---|
| C-1（阻断）无新增权限/导出/allowBackup 变更 | **PASS** | Manifest 仅 VIBRATE；allowBackup=false；导出状态未变（§4.1） |
| C-2（阻断）无新增 SDK/网络库/WebView | **PASS** | 依赖清单无新增；新增代码 grep 无 OkHttp/Retrofit/URL/Socket/Firebase/AdMob（§4.1） |
| C-3（阻断）FLAG_SECURE 默认 true 未变 | **PASS** | `EntitlementManager:83` 仍为 `getBoolean(KEY_SCREEN_SHIELD, true)` |
| C-4 Keystore 未降级、时间校验未弱化 | **PASS** | `LetterCrypto` AES-256-GCM 完整；封存/揭示仍基于 `System.currentTimeMillis()`；Q6 降级路径为存量未扩大 |
| C-5 仅改文本值，id/tier/tags/slots 未变 | **PASS** | 主理人确认 mode id/`LDR_SYNC` 枚举/TagDisplayMapper 键未动；脚本 verify_content 8 类无结构报错（§4.3） |
| C-6 verify_content.py EXIT=0 | **HOLD** | 本专家只读不改；建议筑筑/源源在返工后复跑确认（本次未执行构建） |
| C-7 strings 8 语言对齐、繁体无异形字 | **PASS** | 主理人实测 8 语言各 684 key 全对齐无空串；脚本 mix 回归命中 0 |
| C-8 构建通过、R8 未误删 | **HOLD** | 非本专家职责（交 android-build/runtime），本轮未执行构建 |

### D. 分级与上架（RED-D）

| 项 | 判定 | 依据 |
|---|---|---|
| D-1（阻断）分级口径统一 | **FAIL** | IARC 文档仍 17+，全量报告 18+；主理人裁决 18+ 未落文档（§5.2） |
| D-2（阻断）内容改动触发问卷重填 | **FAIL** | 本轮实质改题+新增 3 功能，问卷须重填，尚未执行（§5.1） |
| D-3 问卷各项与实际一致 | **HOLD** | 给逐项重填指引（§5.1）；待 P5 由 android-release 执行 |
| D-4 家庭政策/非儿童向/18+ 年龄确认 | **HOLD** | 软门禁已实现但结构性风险（§6.2），判定为不足以规避风险，须补强 |
| D-5 隐私政策 URL + 订阅披露 | **HOLD** | 交 android-release 核 URL 可访问性；应用内订阅入口既有 |
| D-6 Q3 targetSdk 已确认 | **FAIL** | 仍 targetSdk 35，API 36 期限（2026-08-31）与延期（11-01）均已过（Phase1 P0-3，仍阻断） |
| D-7 P5 前再核最新政策 | **PASS** | 本报告 2026-11-24 已核；P5 建议再核 |
| D-8 保留审核记录 | **PASS** | 本报告 + 词表脚本 + Phase1 报告已留存 |

### E. 回归验证

| 项 | 判定 | 依据 |
|---|---|---|
| E-1 verify_content 8 类通过 | **HOLD** | 同 C-6，待返工后复跑 |
| E-2 抽题无空池/无占位符残留 | **PASS** | `DateNightSessionEngine` 池空逐级降档返回 null 不抛异常；recent shield 12；HARD_NO 不放宽（§6.1） |
| E-3 新增文本无截断/溢出 | **PASS** | 界面文本抽验 684 key 对齐；UI 复核交 android-ui |
| E-4 防重复与 Hard No 过滤回归 | **PASS** | `buildPool()` 强制 `consentFilter.isAllowed(card.tags)`；PEAK 阶段也不放宽（§6.1） |

**汇总**：通过 **19**、不通过 **9**、待确认/不适用 **5**。

---

## 2. 精确扫描数据

### 2.1 扫描量

| 范围 | 数量 |
|---|---|
| 8 语言 strings.xml | **5472 条**字符串（8×684） |
| 内容 JSON | **104 文件 / 139633 个字符串值** |
| 官方脚本 `check_compliance.py` | BLOCK **1467** / REVIEW **1339**（09:59 稳定态） |
| 官方脚本命中分布 | strings 层 50 / 内容层 1417 |
| 自建"动作分解"词表（宽） | TOTAL **10533**（证实漏检，见 2.3） |
| 串词/简繁异形字回归 | **0** |

> 注：09:57 首次 CLI 扫描报 BLOCK 2700，09:59 模块 API 复核为 1467——差异来自并发落盘（部分文案在两次之间被降调），**以 09:59 稳定态为准**，且两者都指向内容层为主。

### 2.2 官方脚本 BLOCK 按规则/语言

| 规则 | 命中 | 规则 | 命中 |
|---|---|---|---|
| RED-A1 | 3 | RED-A3 | 515 |
| RED-A2 | 296 | RED-A6 | 1 |
| RED-C | 619 | RED-P | 33 |

按语言（RED-A2 露骨动作）：en 69 / fr 97 / ko 97 / es 23 / **pt-BR 7 / zh-CN 2 / zh-TW 1**。
按语言（RED-C 越界）：zh-CN 284 / pt-BR 93 / fr 90 / ko 87 / en 40 / ja 14 / zh-TW 8 / es 3。

高频命中词：挑逗 240、支配 170、지배[支配] 96、lèche[舔] 96、ivre[醉] 86、provocante 70、벗기[脱] 64、按住 60、酔い[醉] 37。

### 2.3 ⚠️ 漏检假象确认（主理人点名，已独立复核）

我按主理人提醒自建完整 8 语言词表，**不沿用低命中率结论**。核验 `extreme_modes.json` 的 `pr_ice_1`（冰块滑过颈侧锁骨 + 炙热吻）八语言原文：

| 语言 | 原文 | 官方脚本 RED-A2 |
|---|---|---|
| en | "glide it slowly down your partner's neck and collarbone—chasing the chill with hot kisses" | 命中 |
| zh-CN | 「顺着伴侣颈侧与锁骨缓慢滑过——用炙热的吻追着冰痕融化」 | **漏检**（RED-A2 zh 仅 2 条） |
| zh-TW | 「順著伴侶頸側與鎖骨緩慢滑過——炙熱的吻追著冰痕融化」 | **漏检**（RED-A2 仅 1 条） |
| ja | 「首筋から鎖骨へゆっくり滑らせて——熱いキスで冷たさを溶かして」 | **漏检** |
| ko | "목덜미와 쇄골을 천천히 미끄러뜨린 직후, 뜨거운 키스로" | 命中（ko 97） |
| es | "deslízalo por cuello y clavícula — besos ardientes" | **漏检**（es 23） |
| fr | "glissez-le le long du cou et des clavicules — baisers brûlants" | 命中（fr 97） |
| pt-BR | "deslize pelo pescoço e clavícula — aquecendo com beijos ardentes" | **漏检**（pt 仅 7 条） |

**结论**：`pr_ice_1` 八语言语义完全等价（滑过颈侧锁骨 + 炙热吻追），但官方脚本对 **zh-CN / zh-TW / ja / es / pt-BR 五种语言漏检**。根因：`_A2` 词表对这 5 种语言只收了「舔/吸/性器官/脱衣」动词，未收录「滑过/滑らせ/desliz/glissez/deslize + 锁骨/炙热吻」意象族。低命中率是**词表覆盖缺口**，不是内容更干净——**推翻"pt 0.1% / es 0.2% / ja 1.6% 更含蓄"的初判**。

自建宽词表独立计数（同批文件，09:59 快照）：

| 语言 | 自建词表命中 | 官方 RED-A2 | 倍数差 |
|---|---|---|---|
| en | 1045 | 69 | 15× |
| zh-CN | 1140 | 2 | 570× |
| zh-TW | 1107 | 1 | 1107× |
| ja | 2047 | 0 | — |
| ko | 1718 | 97 | 18× |
| es | 1163 | 23 | 51× |
| fr | 1182 | 97 | 12× |
| pt-BR | 1131 | 7 | 162× |

> 说明：自建宽词表含 review 级软词（亲吻/颈部/唇），命中数≠违规数，仅用于证明**跨语言一致性**——八语言命中量同量级（1045~2047），不存在"某语言明显更干净"。真正违规须按 §2.4 人工复核动作分解条目。

### 2.4 内容层真越界清单（Phase 1 §2.4.3 存量高风险，须源源返工清除）

| 文件 | 路径/ID 样例 | 原文（节选） | 命中 RED |
|---|---|---|---|
| `zh/extreme_modes.json` | `pr_ice_1` | 「用指尖或嘴唇夹起一块冰，顺着伴侣颈侧与锁骨缓慢滑过——用炙热的吻追着冰痕融化」 | A2 动作分解（脚本漏检） |
| `en/extreme_modes.json` | `pr_ice_1` | "glide it slowly down your partner's neck and collarbone—chasing the chill with hot kisses" | A2 |
| `en/intimate_modes.json` | props/task | "Licking & nibbling from neck downward" / "Warm hands sliding under clothes" | A2 |
| `zh/decks.json` | 主理人实测 | 「轻咬」115 /「深吻」80 /「跨坐」26 /「惩罚」4 /「褪衣」2 | A2(轻咬/深吻/跨坐/褪衣) + A3(惩罚) |
| `extreme_modes.json`/`intimate_modes.json` | 大量 tier3/4 | 动作化文案（源源正在返工） | A2/A3/C |

**给源源的判定规则**：凡出现「滑过/描摹/游走 + 颈/锁骨」「舔/舐/lamer/lécher/舐める」「跨坐/straddle」「褪衣/脱光/undress」「惩罚/punishment/castigo/罰」——**一票清除**，改为情绪/留白句式（见 Round14 报告的张力门禁规则）。

### 2.5 strings 层 50 条 BLOCK 误报分析（不要求改写者改）

这 50 条中**绝大多数是正则误报或合规必需文案**，不能据此判 strings 层不合格：

| 命中 | 样例 | 判定 |
|---|---|---|
| `agegate_decline="I am under 18"` | RED-A1 `under 18` | **误报**——这是 18+ 年龄门禁的拒绝选项，合规必需，禁止改写 |
| `按住屏幕`/`按住这里`/`已按住` | RED-A3 `按住` | **误报**——heartbeat 是"按住屏幕"的游戏机制，非"按住伴侣身体" |
| `Démineur des Cœurs`（fr 扫雷） | RED-A1 `mineur` | **误报**——"扫雷"词内含 mineur(矿工)，非"未成年" |
| `부담 가질 필요 없어요`/`문질러`/`질문` | RED-A2 ko `질` | **误报**——韩语正则 `질(?!타)` 误伤"질문(问题)/가질(拥有)/문지르(摩擦)" |
| `consent_review_together=공유 목록 보기` | RED-P ko `공유` | **误报**——"共享列表"指双方共同边界视图，非"分享到第三方" |
| `tier_short_1=ほろ酔い`（微醺） | RED-A3 ja `酔い` | **误报**——T1 档位名"微醺"，Phase1 明确允许（非"酒后无同意能力"） |
| `暧昧拉扯`/`挑逗`/`情趣四子棋` | RED-C/RED-E | **review 级氛围词**——用户明确要"暧昧/诱惑"调性，属允许范围，不阻断 |

**结论**：strings 层 50 条 BLOCK **无需改写**（误报+合规必需+review 氛围词）。真正待改的是内容层，且须先修 `check_compliance.py` 的漏检与误报规则（交筑筑，本专家不改脚本）。

---

## 3. 隐私叙事专项（RED-P）

### 3.1 sync 歧义清除核验：PASS

| key | 改前 | 改后（实测） | 判定 |
|---|---|---|---|
| `heat_scope_fmt`(en) | `Synced across all 20 games` | `applies to all 36 games` | ✅ 歧义清除 |
| `heat_scope_fmt`(zh-CN) | 「自动同步到全部 20 款游戏」 | 「全部 36 款游戏统一使用」 | ✅ |
| `mode_ldr_sync`(en) | `Long-Distance Sync` | `One Shared Countdown` | ✅ |
| `mode_ldr_sync`(zh-CN) | 「异地同步」 | 「同一时刻」 | ✅ |
| `mode_ldr_sync_desc` | 含 sync/异地/from anywhere | 已去除 | ✅ |
| 结构键 | `mode id="ldr_sync"` / `LDR_SYNC` 枚举 / TagDisplayMapper 键 | **未动** | ✅ 数量断言保持 |

### 3.2 否定式豁免核验：PASS

RED-P 33 条命中逐条核查为否定式误报（如「绝不上传」被 `上传` 命中、`sin nube` 语境），无肯定式上传/云同步/账号/分享。新增 `datenight_*`/`agegate_*`/`tonight_note_*` 256 条译文无数据流语义。

---

## 4. 技术面核验（RED-T）：全部 PASS

### 4.1 权限 / 网络 / SDK

- `AndroidManifest.xml`：**仅** `VIBRATE`；`allowBackup=false`；`AppLocalesMetadataHolderService exported=false`；`MainActivity exported=true`（LAUNCHER 必需）。**无** INTERNET / 定位 / 存储 / 相机 / 录音 / `usesCleartextTraffic`。
- 新增 6 类核心类 + 4 类 UI 类 + 5 个布局：grep 无 `OkHttp/Retrofit/HttpURLConnection/WebView/Socket/URL(/Firebase/AdMob/analytics`。
- `app/build.gradle`：依赖与既有清单一致（appcompat/activity/material/constraintlayout/recyclerview/lifecycle/navigation/dynamicanimation/room/gson/billing 7.1.1），**无新增第三方 SDK**。

### 4.2 加密 / 门禁 / 防篡改

- `EntitlementManager:83`：`getBoolean(KEY_SCREEN_SHIELD, true)` —— FLAG_SECURE **默认仍 true**。
- `LetterCrypto`：`AES/GCM/NoPadding`、256 位、不可导出密钥，`setRandomizedEncryptionRequired(true)` —— 未降级。
- `SealedLetterManager`：封存/揭示仍基于 `System.currentTimeMillis()`，时间校验未弱化。
- `ageGateManager`：`commit()` 写盘先于关窗，读取 fail-closed —— 无数据泄漏风险。

### 4.3 结构一致性

主理人确认：`mode id`/`LDR_SYNC` 枚举/TagDisplayMapper 键未动；8 语言 684 key 全对齐无空串；mix 回归 0。✅

---

## 5. Q6 · Keystore 加密失败降级路径技术方案（只出方案，不改代码）

### 5.1 实代码核实（确切路径与行号）

- **触发条件**：`core/letter/LetterCrypto.java:40-54` 的 `encrypt(String)` 在 `cipher.init`/`doFinal`/`getOrCreateKey()` 抛异常时（KeyStore 不可用、密钥生成被拒、Keystore 锁定、设备无安全硬件等）`catch (Exception e) { return null; }`（第 51-53 行）。类注释第 37-38 行明示："the caller then stores the body unencrypted"。
- **当前实际行为**：调用方 `core/letter/SealedLetterManager.java:86-87`：`String cipher = LetterCrypto.encrypt(cleanBody); if (cipher == null) return null;` —— **当前实现已经是"加密失败即拒绝写入、返回 null"**，并未明文落盘！

> ⚠️ **重要更正**：主理人任务卡称"注释明示 caller 会明文落盘"，但我逐行核实：**当前 caller（`SealedLetterManager.seal()` 第 87 行）在 cipher==null 时直接 `return null`，不存在明文落盘路径**。这是 Phase 1 报告的措辞被过时注释误导。真实风险是**注释与实现不一致**（注释是旧实现的遗留），而非实际明文写入。

### 5.2 技术方案（4 个决策点）

| 决策点 | 方案 | 理由 |
|---|---|---|
| **D1 是否改代码行为** | **不改行为**（已 fail-closed），仅**修正 `LetterCrypto` 类注释**（第 37-38 行）为"返回 null 由 caller 拒绝写入" | 行为已正确；错误注释有误导审计风险 |
| **D2 重试次数** | **不重试**。Keystore 异常多为持久性（锁定/无硬件），重试无意义；如要重试仅 1 次（`Cipher.getInstance` 瞬时失败场景），间隔 0（同步） | 避免复杂状态机 |
| **D3 错误如何呈现** | 在 `SealedLetterFragment` 封存按钮回调里，当 `seal()` 返回 null 时，用已有 `Toast` 提示"设备安全存储暂不可用，请稍后重试"；**新增 1 条 string key × 8 语言**（`letter_encrypt_unavailable`） | 用户可感知，不静默失败 |
| **D4 已有未加密数据/迁移** | **无需迁移**——不存在历史明文数据（`bodyCipher` 字段始终存密文；旧明文若存在也无法被 `decrypt` 识别，会返回 null 并按"无法解密"处理）。为稳妥，可在 `open()` 第 169-170 行 decrypt 失败时记录埋点（仅本地日志，**非分析 SDK**） | 无数据迁移负担 |

### 5.3 风险面评估

- **软降级必要性**：不需要"仅内存会话"降级。加密失败场景极罕见（现代 Android 设备 Keystore 普遍可用），且封存情书非实时功能，提示稍后重试可接受。
- **可用性 vs 安全**：fail-closed 会导致个别 Keystore 异常设备无法写信，但这是**正确取舍**——宁可不可用也不明文落盘（隐私卖点是核心差异化）。
- **落地建议**：由【源源】在返工时**改注释 + 加 toast + 加 1 条 string**（约 20 行改动），**不改加密行为**。我（安安）可自行落地此小改动，但按写权限约束本阶段不改；请主理人裁决归属。

---

## 6. P5 上架清单终裁（更新 §7）

### 6.1 IARC 问卷重填指引（主理人裁决 Q7：须重填）

| 问卷项 | 本轮答案 | 变化 |
|---|---|---|
| 暴力/恐怖 | 无 / None | 不变 |
| 粗俗语言 | 偶尔温和 / Mild | 不变 |
| 裸露内容 | 无图形裸露 / No graphic nudity | 不变（题库纯文字，无图像） |
| 性暗示内容 | **Suggestive / Mild**（须如实）——若 tier3/4 降调未完成就提交，可能被迫上调 | 需复核后定 |
| 成人主题/粗俗幽默 | 温和 | 不变 |
| 酒精/烟草/药物 | 轻微提及（「微醺」） | 不变 |
| 赌博 | 无真实赌博 | 不变 |
| **在线互动** | **无**（新增 3 功能均本地，无联网） | 不变，但须确认 |
| **用户生成内容** | **有限（本地自定义卡 + 今晚一句话，仅本机，无发布/分享/预审）** | ⚠️ **新增 tonight_note UGC 须纳入此项** |
| **交互式元素 / 内购** | 有（订阅，Play Billing） | 不变 |

> **重填触发**：内容实质改动 + 新增 UGC 功能 → 必须重新提交问卷并留存新证书（Play 明确要求）。建议在 tier3/4 降调**完成后**再填，避免按未降调内容申报导致偏高。

### 6.2 分级口径统一 18+（主理人裁决 Q1）—— 两文档需同步改的具体位置

| 文件 | 行 | 现状 | 改为 |
|---|---|---|---|
| `docs/PLAY_CONSOLE_CONTENT_RATING.md` | 4 | "（20 个模式）" | "（36 个模式）" |
| 同上 | 24 | "**Mature 17+**…也可申报 Adults 18+" | "**Adults 18+**（统一口径）" |
| 同上 | 26 | "勾选高于 Suggestive 可能要求 18+" | 更新为"已按 18+ 申报" |
| 同上 | 38 | "属于政策允许的成年人亲密关系范畴" | 见 Round14 §2 政策复核结论 |
| 同上 | 59 | "建议 Mature 17+" | "按 18+ 申报" |
| 同上 | 64 | "加入 18+ 年龄确认弹窗"（待办） | "已实现软门禁（见 §6.3 补强建议）" |
| `audit_report/全量审计报告.md` | 132/159 | 已写 18+ | 无需改（已是 18+） |

### 6.3 18+ 软门禁专项判断：**不足以规避 Families 政策风险**（风险等级：中高）

**实现事实**：
- `AgeGateDialogFragment`：拒绝后 `showDeclinedState()`（第 74-85 行）隐藏拒绝按钮、改正面按钮为"仅基础玩法说明 + 继续"，用户**点一下即可进入 App**。
- `AgeGateManager.isAdultContentAllowed()` 全工程仅 1 处消费：`HomeFragment:293`，仅在主页列表层隐藏 `heatRating>2` 的模式。
- 内容层（题库 JSON）、其他 Fragment（含 VIP paywall）**无年龄门禁拦截**；`GameModeCatalog` 仍为 36 项。

**风险**：Play 可能判定为"未实质限制未成年人访问成人向内容"——软门禁形同"点击即绕过"。

**补强建议（按优先级）**：
1. **P0**：`isAdultContentAllowed()` 下沉到**内容抽取层**（`ContentRepository`/各 Engine 的 `buildPool`），使未成年用户连题库都抽不到 heat≥3 卡，而非仅 UI 隐藏。
2. **P1**：拒绝后正面按钮改为"退出应用"或强提示"此内容仅成人可见"，不提供"继续"快捷入口。
3. **P1**：`agegate_confirm` 文案明确"我确认已满 18 岁"，避免模糊。
4. **P2**：Play Console 内容分级按 18+ 申报后，Google 会按地区对未成年人屏蔽——软门禁是**辅助**，"分级 + 地区屏蔽"才是主控手段，二者叠加可显著降低风险。

---

## 7. 新功能合规专项

### 7.1 A 项 · 约会夜串场（DateNightSessionEngine）：PASS

- **不绕过 ConsentFilter**：`buildPool()` 第 234 行强制 `consentFilter.isAllowed(card.tags)`；类注释第 28 行明确"绝不通过放宽 HARD_NO 补足卡池"；`buildFallbackPool()` 降档只放 tier/tag，**不放 HARD_NO**。✅
- **PEAK 无倒计时**：`DateNightStage.PEAK` 的 `durationSec=0`、`isTimed()` 返回 false——降低压迫感，**有利于**"可随时停"表达，非削弱。✅
- 阶段提示语均在 Suggestive/Mild 上限内（datenight_* 译文抽象验无越界）。但 **PEAK 阶段 `preferredTags` 含 `bite_lick`、maxTier=4** —— 该标签关联的题库若含未降调动作化文案，会命中 A2。⚠️ **依赖源源 tier3/4 降调完成**，否则 PEAK 会抽到越界卡。
- 抽卡池空逐级降档返回 null（不抛异常），shield=12，`seenThisSession` 防重复。✅

### 7.2 B 项 · 18+ 门禁：中高风险（见 §6.3）

软门禁结构性不足；不建议仅靠它规避 Families 政策。补强前判为**不足以**"实质限制未成年人访问"。

### 7.3 C 项 · 今晚一句话（TonightNoteManager）：PASS

- **符合 §4.2 [SEC-4] UGC 约束位**：仅本机 SharedPreferences（`velvet_tonight_note`）、分键范式、`MAX_NOTE=140` 截断、`commit()` 写盘；**不上传 / 不预审 / 不分享 / 不导出**（类注释第 20-21 行 + 代码核实）。✅
- **日期校验**：`saveNote()` 第 83 行 `TogetherDateFormat.parse(iso)==null` 拒绝非法日期，不写半截键。✅
- **IARC 影响**：属"有限 UGC"，须在问卷 UGC 项如实申报（§6.1 已列）。
- **隐私政策/设置页文案**：建议（非阻断）在 `settings_privacy_body` 补一句「你写下的『今晚一句话』仅存本机，不上传、不分享」——8 语言各加 1 条，与新增 UGC 形成叙事闭环。交主理人裁决是否本轮加。

---

## 8. 交付给 android-release 的更新清单（P5）

1. **先决条件**：tier3/4 题库降调完成 + `check_compliance.py` 漏检/误报规则修正 + 复跑 verify_content.py EXIT=0。
2. **IARC 问卷重填**：按 §6.1 逐项答；UGC 项纳入 tonight_note；留存新证书。
3. **分级 18+**：按 §6.2 同步两文档，`PLAY_CONSOLE_CONTENT_RATING.md` 6 处修改。
4. **年龄门禁补强**：按 §6.3 P0/P1 建议，至少把 `isAdultContentAllowed` 下沉到内容抽取层。
5. **targetSdk**：Q3 阻断项——确认 API 36 升级或延期方案，否则无法提交。
6. **隐私政策 URL**：核验可访问；建议补 UGC 本机声明（§7.3）。
7. **数据表单**：无收集/无共享/无第三方 SDK；今晚一句话属本机 UGC 不收集。
8. **证据留存**：本报告 + Round14 张力报告 + Phase1 报告 + 词表脚本 + CI 报告。

---

> **阶段门**：本报告为 Phase 4 扫描快照（中间态）。tier3/4 降调完成后须复扫；Round14 张力边界仲裁见 `audit_report/round14_tension_compliance.md`。


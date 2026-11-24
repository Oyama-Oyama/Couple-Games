# VelvetCouples Phase 1 内容合规红线报告

> **轮次**：Round 13 · Phase 1（先定红线，Phase 4 续接最终合规扫描）
> **任务**：#3 内容合规红线 / P4 最终合规扫描
> **编制**：安全与隐私专家 · 安安（android-security）
> **核验日期**：2026-11-23
> **工程**：`D:\AndroidProject\VelvetCouples`（Java 17 / targetSdk 35 / v2.1.0 / 包名 `com.velvet.couplesgame`）
> **适用对象**：android-architect（私密调性规范）、android-ui（需求 2/3 界面文本）、android-data（需求 5/6/7 题库与 8 语言）、android-release（P5 上架清单）
> **交付物**：本文件仅含报告与红线，**未修改任何代码、布局、strings.xml、内容 JSON、构建脚本**

---

## 0. 效力声明与结论速览

### 0.1 效力声明

1. 本文件是 **#1/#5/#6 一切补充与改写文案的前置硬约束**：任何文案在写入 `strings.xml` 或 `assets/content/*/*.json` 之前，必须能通过第 2、3 章的判定；不通过即视为不合格，须回退重写。
2. 本文件给出的是**技术侧合规建议与可判定红线**，**不代替法务做合规判定**。所有政策不确定项集中列于第 5 章，**交回主理人（team-lead）确认**，本专家不自行拍板。
3. 本轮**只定红线**，不对存量内容做最终合规结论；存量内容逐条扫描在 Phase 4 执行（清单见第 6 章）。
4. 红线编号体系：`RED-A` 绝对禁止 / `RED-B` 成年人向边界 / `RED-C` 实现路径 / `RED-D` 热度与分级 / `RED-E` 地区差异 / `RED-P` 隐私叙事 / `RED-T` 技术面。编号在 Phase 4 扫描时直接复用。

### 0.2 一句话结论

**用户第 7 项「羞涩又开放、诱惑、暗示、吸引力、刺激感」只能通过情绪密度、留白、悬念、双人视角来实现，绝不能通过增加动作清单或露骨描写来实现**——在 2026-10-29 生效的 Google Play 政策口径下，后者会直接威胁上架资格，并同时摧毁「零权限零网络零广告、数据不出本机」的差异化叙事。

### 0.3 三条最高优先级结论（P0）

| 编号 | 结论 | 影响面 |
|---|---|---|
| **P0-1** | Google Play《不当内容 / 性内容与不雅用语》政策（Developer Program Policy **effective October 29, 2026**）措辞为「不允许包含或宣扬色情内容……**或任何意图满足性欲的内容或服务**（any content or services intended to be sexually gratifying）」。项目 `docs/PLAY_CONSOLE_CONTENT_RATING.md` §2.1 判定「属于政策允许的成年人亲密关系范畴」**可能过于乐观**。存量 `tier_desc_3/4`、`mode_ladder_desc`、`paywall_benefits` 已含「轻舔/轻咬/深吻」「褪衣惩罚/按手控制/俏皮拍打」「nonstop physical foreplay」「Kama Positions / 5-Stage Foreplay」等动作化表述，**属高风险**。本轮改写**必须降调而非加码**。 | 上架资格、ASO 卖点 |
| **P0-2** | 内容分级口径不一致：`PLAY_CONSOLE_CONTENT_RATING.md` 建议 **Mature 17+**（保守可 18+），`全量审计报告` §2.8 建议 **18+**。IARC 明确「申报内容与实际不符可导致下架或停权」，且**内容/功能变化影响问卷答案时必须重新提交问卷**。本轮改题属于内容变化，须纳入 Phase 4 复核。 | 分级申报、地区曝光 |
| **P0-3** | 目标 API 级别：官方要求 **自 2026-08-31 起新应用与应用更新须 target Android 16（API 36）**，延期窗口至 2026-11-01；当前工程 **targetSdk 35**。按核验日期 2026-11-23，两个窗口均已过。此为**阻断级上架合规风险**，须交主理人 / android-build 确认（本专家不改构建脚本）。 | 上架阻断 |

---

## 1. 核验基线（事实与证据）

### 1.1 技术与权限基线（已逐项核验）

| 项 | 现状 | 证据 |
|---|---|---|
| 权限 | 仅 `android.permission.VIBRATE`；**无 INTERNET、无定位、无存储、无相机、无录音、无通知权限** | `app/src/main/AndroidManifest.xml:4` |
| 组件导出 | 仅 `MainActivity`（LAUNCHER 必需）`exported=true`；`AppLocalesMetadataHolderService` `enabled=false / exported=false` | Manifest:26-43 |
| 备份 | `android:allowBackup="false"` | Manifest:17 |
| 外部查询 | `<queries>` 仅声明 `market://` 与 `com.android.vending`（跳转商店评分，**非网络权限**） | Manifest:7-13 |
| FLAG_SECURE | **默认开启**：`EntitlementManager.isScreenShieldEnabled()` 默认值 `true`；`MainActivity.onCreate` 应用；设置页开关可关闭 | `EntitlementManager.java:81-87`、`MainActivity.java:41-42`、`SettingsFragment.java:111-120` |
| 加密 | 密封情书 `LetterCrypto`：AndroidKeyStore `AES/GCM/NoPadding`，256 位密钥、`setRandomizedEncryptionRequired(true)`、128 位 GCM tag、12 字节 IV、密钥不可导出 | `core/letter/LetterCrypto.java:26-96` |
| 时间基准 | 封存/揭示、日期计划、刮刮卡等使用 `System.currentTimeMillis()` | `SealedLetter.java:71,82`、`SealedLetterManager.java:95,172`、`DatePlanManager.java:79` |
| 依赖/SDK | 仅 Play Billing（订阅结算）、Material 3、AppCompat、Gson；**零广告、零分析、零崩溃上报、零云同步 SDK** | `GOOGLE_PLAY_LAUNCH_CHECKLIST.md:28`、`全量审计报告:134` |
| CI 校验 | `scripts/verify_content.py` 共 8 类：JSON 合法性 / ID 对齐 / 占位符一致 / slot 定义 / v21 语法 / 标签截断 / 资源引用 / zh_tw 简繁 | `scripts/verify_content.py:1-189` |
| 内容资产 | 13 类 JSON × 8 语言（en / zh / zh_tw / ja / ko / es / fr / pt），界面字符串各 652 条（历史报告为 441 条，本轮已扩充） | `assets/content/`、8 语言 `values*/strings.xml` |
| 边界机制 | 首启情侣档案（He/She/They + 自定义 Safe Word）、Consent & Comfort 边界页（YES / SOFT / HARD NO，Hard No 全游戏静默屏蔽）、游戏内 Pause / Safe Word 栏、Private Pass 私密否决 | `values/strings.xml:5,12-17,90-100`、`GOOGLE_PLAY_LAUNCH_CHECKLIST.md:27` |

> **注意**：现有 CI **不含「内容合规词表」校验**。第 6 章建议 Phase 4 补入扫描项（脚本改动交 android-build / 主理人决策，本专家不改）。

### 1.2 内容分级基线（两份文档，口径不一致）

| 来源 | 建议分级 | 性暗示主题 | 备注 |
|---|---|---|---|
| `docs/PLAY_CONSOLE_CONTENT_RATING.md:24` | **Mature 17+**（保守可 Adults 18+） | `Suggestive / Mild` | 问卷逐项答复；无图形裸露；无露骨性行为；家庭政策非儿童向；建议首启 18+ 年龄确认 |
| `docs/PLAY_CONSOLE_CONTENT_RATING.md:64` | — | — | 上架待办含「首启加入 18+ 年龄确认弹窗」 |
| `audit_report/全量审计报告.md:21,129-135` | **18+ / 成人内容** | 含露骨性暗示 + 订阅 | 点名 5 火焰「极端身体探索」模式、标签 `bite_lick` / `restraint_light` / `blindfold`、描述 `suck softly and kiss on lower belly` |

**红线 `RED-D` 的立足点**：无论最终申报 17+ 还是 18+，4 档热度内容都必须**始终落在 `Suggestive / Mild` 且「无露骨性行为描写、无图形裸露」区间**。任何一档越界 → 申报失真 → 触发 IARC 重新评级 → 可能被判 18+ 并在 EEA / 澳大利亚 / **巴西** / 新加坡 / 瑞士 / 英国对未成年人屏蔽购买与搜索曝光 → 直接冲突于「零权限零网络」的广泛可得性卖点。

### 1.3 隐私叙事基线（8 语言现有正向表述，可复用、不得反向改写）

| 语言 | 现有正向承诺（`onboarding_step2_promise` / `settings_privacy_body`） |
|---|---|
| en | `100% Offline — no accounts, no cloud, zero tracking` / `never leave this device` / `Encrypted on this device` |
| zh-CN | 「无需注册账号……永远只留在你自己的手机里，绝不上传」 |
| zh-TW | 「純離線／只留在本機／絕不上傳／無需帳號」（以现有繁体文案为准） |
| ja | 「完全オフライン — アカウント不要・外部送信ゼロ」/「端末内のみに保管されます」 |
| ko | 「100% 오프라인 — 계정 가입·클라우드 전송 없음」/「오직 이 기기에만 머뭅니다」 |
| es | 「100% sin conexión — sin cuentas, sin nube, cero rastreo」 |
| fr | 「100% hors ligne — sans compte, sans cloud, zéro suivi」 |
| pt-BR | 「100% offline — sem contas, sem nuvem, zero rastreio」/「nunca saem deste dispositivo」 |

### 1.4 政策时效核验（2026-11-23 检索，第一方来源）

- **性内容与不雅用语**（`support.google.com/googleplay/android-developer/answer/9878810`、`answer/18258653`，Developer Program Policy **effective October 29, 2026**）：不允许包含或宣扬色情内容、**或任何意图满足性欲的内容或服务**；不允许疑似招揽以性行为换取报酬；不允许与性加害行为相关的内容或**传播非自愿性内容**；裸露仅在 EDSA（教育/纪实/科学/艺术）且非滥用的前提下可能豁免。
- **儿童安全**（同政策）：Child Endangerment 属**即时下架**类；「对未成年人的性化」「面向儿童却含成人主题」均禁止。→ 支撑 `RED-A1` 零容忍。
- **Child Safety Standards Policy**：适用于 Anonymous Chat / Random Chat / Social / **Dating** apps。本 App 为**纯本地双人、无在线互动、无陌生人社交、无聊天**，**不属于**该范畴；**前提是本轮不得新增任何在线社交/匹配/聊天能力或文案**（与 `RED-T` 呼应）。
- **内容分级**（`play.google.com/intl/ja/about/storelisting-promotional/content-ratings`）：所有应用须有 IARC 分级；**申报不实可能导致下架或停权**；**内容或功能变化影响问卷答案时必须重新提交问卷**；广告成熟度不得高于应用本身。
- **地区屏蔽**：EEA、澳大利亚、**巴西**、新加坡、瑞士、英国对判定为未成年人的用户屏蔽 18+ 应用购买并从搜索/浏览过滤。韩国由 GRAC 等地区机构生成地区分级（19+ 档）。→ 支撑 `RED-E` 巴西、韩国「宁可更含蓄」。
- **目标 API**（`developer.android.com/distribute/best-practices/develop/target-sdk`、`support.google.com/googleplay/android-developer/answer/11926878`）：自 **2026-08-31** 起新应用与应用更新须 target **Android 16 / API 36**；现有应用须 ≥ API 35 才能继续向更高版本系统的新用户提供；可申请延期至 **2026-11-01**。

> 上述为技术侧政策摘要，**全文以 Play Console 与政策全文为准**；判定归属见第 5 章。

---

## 2. 内容合规红线（写给改写者，可执行、可判定）

### 2.1 `RED-A` 绝对禁止（命中任意一条 = 该条文本不合格，必须删除或重写）

| 编号 | 禁止项 | 判定方法（可机器 + 人工） |
|---|---|---|
| **A1** | **未成年人或任何未成年暗示**：未成年角色、校园/学生/年纪轻暗示、「少女/少年/正太/萝莉/学园/制服/未成年/teen/young/schoolgirl/女子高生/미성년자/menor de edad/mineur」等；含「看起来年轻」类年龄暧昧 | 8 语言词表全库 grep；人工复核角色设定与情境；商店截图/图标同样适用 |
| **A2** | **露骨性行为、性器官、性行为过程或结果描写**；具体性接触动作分解（舔/吸/含/进入等及 8 语言对应词）；性器具、露骨器物 | 词表 + 动作动词清单；重点扫 `extreme_modes.json` / `intimate_modes.json` / `decks.json` 的 `tier 3/4` 与 VIP 模式 |
| **A3** | **非自愿、胁迫、暴力、伤害、失去意识 / 酒后无同意能力**：强迫、不许拒绝、按住/压制/捆绑至无法挣脱、惩罚性身体处置、醉酒/晕/睡着/失去意识仍继续、「惩罚 / punishment / castigo / punition / 罰 / 처벌 / spank / pin / restraint / dominate 强制义」 | 词表 + 语义复核；**凡出现「一方对另一方施加」且无「可随时停止」语境，一律判违规** |
| **A4** | **物化、羞辱化、贬损化表达**：把伴侣当物件/奖品/所有物、贬低身体、羞辱、以服从/支配定义人格 | 语义复核；对照 `RED-B` 双向自愿要求 |
| **A5** | **任何违法内容**：毒品、非自愿影像、性交易/以性行为换取报酬、偷拍、跟踪、泄露第三方隐私 | 词表 + 语义复核 |
| **A6** | **诱导分享私密影像 / 位置 / 第三方数据**：拍下、录下、发给别人、上传、分享到社交平台、发送定位、附近的人、邀请第三人 | 8 语言词表；与 `RED-P` 联合判定；本 App 无相机/位置/网络能力，任何此类文案同时构成**功能叙事失真** |
| **A7** | **「意图满足性欲」导向的功能性表述**：把 App 或题目定位为性满足工具、性行为教程、付费换取性行为 | 语义复核；对照政策 `intended to be sexually gratifying`；**禁止在商店文案、ASO、paywall、模式名/描述中出现** |

> **A1 / A3 / A5 / A6 为「零容忍」**：不因热度档位、语言、付费模式而放宽。

### 2.2 `RED-B` 成年人向边界（正向强制要求）

| 编号 | 要求 | 落地方式 |
|---|---|---|
| **B1** | **仅成年人情侣**：全部文案默认双方为成年人；不得出现任何年龄模糊或年轻化人设 | 8 语言逐语言复核；商店文案与首启说明明确成人向定位 |
| **B2** | **双方自愿、双向、可拒绝**：主语优先「我们 / 一起 / 你愿意的话」，避免单向命令式「让 TA 做……」 | 改写时保留邀约式而非指令式；`RED-C` 例句示范 |
| **B3** | **明确可随时停止**：与既有「安全词 / 边界 / Pause」页呼应；高热度档文案尤其要有「说停就停」的语义出口 | 呼应 `values/strings.xml:14-17` 的 Safe Word、`90-100` 的 Consent & Comfort、`GOOGLE_PLAY_LAUNCH_CHECKLIST.md:27` 的 Pause/Safe Word 栏；**不得与之矛盾** |
| **B4** | **Hard No 边界不可被文案绕过**：不得写「别管边界」「偷偷试」「TA 不知道也没关系」类绕过否决的表述 | 语义复核；Private Pass（私密否决）叙事须保持「静默尊重」 |
| **B5** | **不引入第三人、不引入观众、不引入公开场合的暴露** | 双人视角铁律；排除「让别人看」「公开」类情境 |

### 2.3 `RED-C` 「诱惑 / 暗示 / 刺激」的合规实现路径

#### 2.3.1 唯一允许的实现手法（六条）

1. **含蓄留白**：只写起点与情绪，把结果交给省略号与想象，不写动作终点。
2. **感官意象**：呼吸、心跳、温度、灯光、气味、距离、声音，代替身体部位清单。
3. **悬念**：「留一个问题到灯暗下来再问」「下一张牌先别翻」。
4. **邀约式语气**：「愿意的话……」「要不要……」「你说了算」，代替命令式。
5. **心跳感**：紧张、屏息、脸红、手心温度、时间变慢——用**情绪强度**制造刺激，而非动作强度。
6. **双人视角**：始终「我们 / 两个人 / 彼此」，避免单方施动-受动结构。

> **铁律**：**强度升级只能靠情绪密度、节奏与留白，不能靠动作清单**。任何「让文本更刺激」的需求，若导致新增具体身体动作、部位、器物或强制语态，即判定越界。

#### 2.3.2 中文「允许 / 越界」对照（3 组）

| 组 | ✅ 允许（含蓄、留白、双人、可停） | ❌ 越界（原因） |
|---|---|---|
| C-ZH-1 悬念 vs 指令 | 「今晚留一个只属于我们的问题，等灯暗下来再问。」 | 「现在把衣服脱掉，照着做下一步。」→ 直白指令 + 具体动作清单；违反 A2 / A3 / 铁律 |
| C-ZH-2 感官 vs 动作分解 | 「你靠近的时候，我连呼吸都放轻了半拍。」 | 「轻舔、轻咬耳垂与锁骨……」→ 具体性接触动作分解；违反 A2、A7 |
| C-ZH-3 自愿 vs 胁迫 | 「还想继续吗？你说停，我们就停在这里。」 | 「不许躲，按住手，喝到不清醒也算数。」→ 胁迫 + 失去同意能力 + 强制；违反 A3、A5 |

> 对照组 C-ZH-2 的越界例句**直接取自本工程存量 `values-zh-rCN/strings.xml:246`**，说明降调不是理论要求，而是本轮必须执行的动作。

#### 2.3.3 英文「允许 / 越界」对照（3 组）

| 组 | ✅ 允许 | ❌ 越界（原因） |
|---|---|---|
| C-EN-1 | "Save one question just for us until the lights go low." | "Take it off now and do exactly what the card says." → 直白指令 + 服从框架；违反 A2 / A3 |
| C-EN-2 | "When you lean in, I forget the second half of my sentence." | "Soft licking, nibbling, deep kiss on the lower belly..." → 具体动作分解（A2）；亦对应审计报告 §2.3 病句 `suck softly and kiss on lower belly` |
| C-EN-3 | "Still with me? Say the word and we pause right here." | "Pin their hands / don't let them pull away / too tipsy to say no still counts / punishment." → 非自愿、酒后无同意能力、惩罚（A3） |

#### 2.3.4 其余 6 语言判定原则（不逐字造句，按原则判定）

| 语言 | 判定原则（改写者自查） | 高危词 / 结构（出现即复核） |
|---|---|---|
| **ja** | 用「気配 / 息 / 間 / ふたり / 〜してもいい？」等留白与婉曲；避免英文 `foreplay` / `positions` / `naughty` 的片假名直译；避免临床式直白词 | 「体位」「前戯」「舐める」「押さえつける」「罰」「無理やり」；命令形「〜しろ」 |
| **ko** | 用「설렘 / 숨 / 간격 / 우리 / ~해도 될까?」；`동의·거부` 语义要清晰以呼应边界页；避免直译成人向英文词 | 「전희」「체위」「핥다」「제압」「벌」「억지로」；해라체命令 |
| **zh-TW** | 较温和书面语；用「靠近 / 心跳 / 先停下來好嗎」；避免直白动作词；同时遵守既有简繁异形字 CI 校验 | 「前戲」「體位」「懲罰」「舔」「按住」「制服」；简体字残留 |
| **es** | 用 « si quieres / nos detenemos cuando quieras / cosquillas / acercarse »；西语区（含拉美）对支配/惩罚词敏感，语气务必条件式而非命令式 | « castigo » « sumisión » « dominación » « obligar » « lamer » « inmóvil »；强制祈使式 |
| **fr** | 用法语文学化含蓄 « frisson / souffle / on s'arrête quand tu veux »；**« punition » 比英文 punishments 更刺眼，必须删** | « punition » « soumission » « lécher » « maintenir de force » « ivre »；强制 impératif |
| **pt-BR** | 用 « vontade / chegar mais perto / a gente para quando você quiser »；葡语热情 ≠ 可直白，`não` / consentimento 必须明确 | « preliminares » « submissão » « castigo » « lamber » « imobilizar » « bêbado »；强制 imperativo |

**跨语言总则 `RED-C-0`**：
- **同一 `tier` 的 8 语言强度必须一致，任何语言不得强于英文源文**。
- 若目标语言缺乏对应委婉表达，**一律降一档处理**（宁可更淡），**不允许为凑「刺激感」而直译露骨词**。
- 8 语言必须同时通过 `RED-A` 词表；**机器扫描英语通过不等于其它语言通过**（历史教训：v21 批次 5 语种本地化不同步、繁体异形字遗漏）。

### 2.4 `RED-D` 热度梯度与分级一致性

#### 2.4.1 四档用语强度上限表

| 档 | 中文名 / 英文名 | 允许（上限内） | 强度上限（不可跨越） | 明确禁止 | 分级申报对应 |
|---|---|---|---|---|---|
| **T1** | 微醺初燃 / Soft Glow | 眼神、靠近、轻声、牵手、拥抱、走心对话、**微醺氛围（非醉酒）**、心跳感 | 情绪与氛围；**无任何身体部位指向的接触指令** | 舔/咬/吸、脱衣、惩罚、控制、具体动作分解 | 无性暗示 |
| **T2** | 心跳试探 / Playful Tease | 调皮试探、耳语、慢舞、轻抚（**仅限非敏感部位：手背、发梢、脸颊**）、二选一问答、悬念 | 止于「轻触 + 玩笑式邀约」；**不得出现口腔相关动作** | 舔/咬/吸、深吻、跨坐、褪衣、支配 | 温和（Mild） |
| **T3** | 暧昧拉扯 / Slow Burn | 靠近到能听见呼吸、颈/锁骨**指向但不描写动作**、碰杯、慢节奏、省略号留白 | **止于亲吻与轻触的「暗示」**；不得把动作写成步骤或教程 | 「轻舔 / 轻咬 / 深吻 / 跨坐」等存量动作化表述（见 2.4.3） | `Suggestive / Mild` |
| **T4** | 午夜失控 / After Midnight | **更浓的悬念、更慢的节奏、更强的双人张力、留白后的省略号、更近的距离感** | **仍不得超过 `Suggestive / Mild`**；**不得靠动作升级** | 露骨性行为/性器官、具体动作指令、褪衣惩罚、按手控制、拍打、强制/支配、任何「意图满足性欲」表述 | `Suggestive / Mild`（**上限，不得上调**） |

> **T4 是本轮最大风险点**：用户第 7 项的「刺激感」最容易被误读为「T4 要更露骨」。红线明确：**T4 与 T3 的差别是情绪密度与节奏，不是动作清单**。

#### 2.4.2 分级一致性铁律

- **D1**：四档全部内容必须能支撑 `PLAY_CONSOLE_CONTENT_RATING.md` 中「性暗示内容 = Suggestive / Mild」「无图形裸露」「无露骨性行为」「无真实赌博」「在线互动 = 无」「用户生成内容 = 有限（本地自定义，无发布/分享）」的申报答案。
- **D2**：**任一档越界 → 申报失真 → 必须重新提交 IARC 问卷**（政策明确要求），并可能被上调分级、在部分地区屏蔽曝光。
- **D3**：本轮改题属于「内容变化」，若改动影响问卷任一项答案（性内容、语言、酒精、UGC、互动元素），**须由 android-release 在 P5 触发问卷重填**；不得由改写专家自行判断「影响不大」而跳过。
- **D4**：商店文案、ASO、截图、paywall 的成熟度**不得高于应用内实际内容**（沿用 `GOOGLE_PLAY_LAUNCH_CHECKLIST.md:10` 的「Zero Banned Emojis / Explicit Terms」既有做法）。

#### 2.4.3 存量高风险措辞清单（本轮若触及必须降调；是否纳入本轮范围请主理人决策）

| 位置 | 存量文本 | 风险 | 建议方向 |
|---|---|---|---|
| `values/strings.xml:247`、`values-zh-rCN/strings.xml:246` | `tier_desc_3`：「soft licking, nibbling, neck/collarbone dares & lap tease」/「轻舔、轻咬耳垂锁骨、跨坐互动与深吻」 | A2 动作分解、A7 | 改为感官/悬念式描述，删除具体动作动词 |
| `values/strings.xml:248`、`values-zh-rCN/strings.xml:247` | `tier_desc_4`：「bold physical punishments, clothing dares, playful spanks & pinning hands」/「褪衣惩罚、按手控制、俏皮拍打」 | **A3 胁迫/惩罚 + A2**，且触碰政策「非自愿性内容」 | **必须降调**：改为「更近的距离、更慢的节奏、说停就停」类表述；删除 punishment / spank / pinning / 惩罚 / 按手 |
| `values/strings.xml:399` | `mode_ladder_desc`：「one partner performs nonstop physical foreplay while the other answers sizzling truths」 | A7「意图满足性欲」 | 改为「十步递进的默契挑战」，去掉 foreplay / nonstop physical |
| `values/strings.xml:198`（8 语言 paywall） | `paywall_benefits`：「Kama Positions, 5-Stage Foreplay, Body Map...」/「体位ルーレット・5段階フォアプレイ」等 | A2 / A7；**商店可见文案** | 改为中性游戏化命名（如「默契轮盘 / 十层心动阶梯」） |
| `values/strings.xml:323` | `mode_auction_desc`：「dominance contracts, blindfold control & midnight privileges」 | A3 支配语义 | 保留竞拍玩法，但去掉 dominance / control，补「双方自愿、随时可停」语境 |
| `全量审计报告.md:129` | 标签 `bite_lick` / `restraint_light` / `blindfold`、描述 `suck softly and kiss on lower belly` | A2 / A3 | 属存量内容风险，Phase 4 全量扫描时逐条核；本轮不得新增同类标签或文本 |
| `values/strings.xml:219` 及 8 语言 | `settings_privacy_body` 等隐私承诺 | 正向，**不得反向改写** | 见第 3 章 |

### 2.5 `RED-E` 地区差异红线（8 语言「宁可更含蓄」）

> 总则：**以最严地区为基准做全局收敛**。8 语言共用同一强度上限，任一语言若触发当地审核风险，全局同步降调，不做「分地区加强」。

| 语言 / 地区 | 接受度与审核尺度 | 「宁可更含蓄」的具体建议 |
|---|---|---|
| **zh-CN 简中** | **最严**。中文语境对性暗示审核敏感，直白身体/动作词风险最高 | 只保留「氛围 + 情绪 + 双人 + 可停」；「诱惑」靠灯光、呼吸、留白，不靠动作清单；避免任何「前戏/体位/惩罚」类词；商店文案沿用现有合规 ASO |
| **zh-TW 繁中** | 用语较温和，书面语接受度略高但仍忌直白 | 用「靠近 / 心跳 / 先停下來好嗎」；避免「前戲 / 體位 / 懲罰」；注意简繁异形字 CI（`verify_content.py` 第 8 类） |
| **ja 日本** | 文学化含蓄（艶やか）接受度高，但对「性行為示唆 / 露出」与青少年保护语境敏感；成人向定位必须明确 | 用「気配 / 息 / 間 / ふたり」；禁止「体位」「前戯」等临床/直白片假名；18+ 定位在首启说明中清晰可见 |
| **ko 韩国** | 分级由 GRAC 等地区机构生成，存在 19+ 档，对「청소년 유해」标示敏感；2026 年地区分级规则有调整 | 用「설렘 / 숨 / 우리」；避免「전희 / 체위 / 제압」；`동의·거부` 措辞必须清晰（呼应边界页）；宁可降一档 |
| **pt-BR 巴西** | 葡语表达热情，但 Play 对「sexo explícito」零容忍；且巴西属于对未成年人屏蔽 18+ 内容的地区之一 | 用「vontade / chegar mais perto」；避免 « preliminares » « submissão » « castigo »；**保持含蓄以争取更宽分级与曝光** |
| **fr 法国** | 文学化情色传统接受度高，但 « punition / soumission » 在法语中更刺眼 | 用 « frisson / souffle / on s'arrête quand tu veux »；**必须删除 « punition »** |
| **es 西班牙 / 拉美** | 对 « castigo / dominación / sumisión » 敏感，部分市场文化语境更保守 | 用 « si quieres / nos detenemos cuando quieras »；避免 « castigo » « sumisión »；条件式替代祈使式 |
| **en 全球基准** | 作为源语言，一旦过线会经翻译扩散到 8 语言 | 英文源文先过 `RED-A` 词表与 C-EN 对照，再启动翻译；**不得先翻译后审查** |

> **共同禁止**：任何地区版本都不得出现 `RED-A1` 未成年暗示、不得出现本地对未成年人性内容更严法律的擦边表述；**地区差异只允许「更含蓄」，不允许「更开放」**。

---

## 3. `RED-P` 隐私叙事一致性核查

### 3.1 叙事不变量（三句话，任何语言、任何新增文本都不得违反）

1. **零权限零网络**：App 自身不发起网络请求、不传输用户数据；`AndroidManifest` 仅 `VIBRATE`。
2. **数据不出本机**：昵称、边界设置、自建卡牌、配对记录、密封情书、历史记录**仅存本机**，且 `allowBackup=false`；密封情书经 Keystore AES-256-GCM 加密落盘。
3. **零账号 / 零云同步 / 零广告 / 零追踪 / 零第三方分享**。

> **边界澄清（避免红线写死成自相矛盾）**：Play Billing 是唯一涉及外部交互的既有组件，结算由 Google Play 服务完成，**App 自身未声明 INTERNET 权限，也不上行任何用户内容数据**。上述「零网络」指**数据面**；新增文案不得把 Billing 描述成「云端同步 / 账号 / 上传」，也不得为「零网络」叙事而否认订阅功能的存在。

### 3.2 8 语言「禁止词 / 警惕词」表（出现即判不合格）

> 目标语义：**上传 / 同步到云端 / 云端 / 账号登录 / 分享到 / 发送给好友 / 定位附近 / 附近的人 / 在线匹配陌生人 / 服务器 / 在线备份**。

| 语言 | 禁止 / 警惕词（非穷尽，按语义判定） |
|---|---|
| **en** | upload, sync to cloud, cloud backup, cloud sync, sign in / log in / account, share to / send to a friend, nearby / people near you, location, online match, server, backup online, send data |
| **zh-CN** | 上传、云同步、云端、同步到云、登录/注册账号、分享到、发给好友/对方、附近的人、定位附近、在线匹配、在线配对、服务器、云端备份、数据回传 |
| **zh-TW** | 上傳、雲端同步、同步到雲端、登入/註冊帳號、分享到、傳給好友、附近的人、定位附近、線上配對、伺服器、雲端備份 |
| **ja** | アップロード、クラウド同期、クラウド保存、ログイン、アカウント登録、共有 / シェアする、近くの人、位置情報、オンラインマッチ、サーバー、外部送信（**注**：现有文案用「外部送信ゼロ」是否定式，允许） |
| **ko** | 업로드, 클라우드 동기화 / 클라우드 전송（**注**：现有「전송 없음」是否定式，允许）, 로그인, 계정 가입, 공유하기, 근처 사람, 위치, 온라인 매칭, 서버 |
| **es** | subir, sincronizar en la nube, copia en la nube, iniciar sesión, crear cuenta, compartir con / enviar a, gente cercana, ubicación, servidor, coincidencia en línea |
| **fr** | téléverser, synchroniser sur le cloud, sauvegarde cloud, se connecter, créer un compte, partager avec / envoyer à, personnes à proximité, localisation, serveur |
| **pt-BR** | upload / enviar para nuvem, sincronizar na nuvem, entrar na conta / criar conta, compartilhar com / enviar para, pessoas perto, localização, servidor, match online |

> **判定细则**：否定式表述（「绝不上传 / 无需账号 / 外部送信ゼロ / 클라우드 전송 없음 / sin nube / sans cloud / sem nuvem」）**允许且鼓励**；肯定式或邀约式（「去上传」「分享给 TA」「登录后可同步」）**一律禁止**。

### 3.3 8 语言「允许词」表（与现状一致，可复用）

| 语言 | 允许的正向表述 |
|---|---|
| en | `100% offline`, `on this device`, `never leave this device`, `no account`, `no cloud`, `zero tracking`, `no ads`, `encrypted on this device`, `works offline forever` |
| zh-CN | 纯离线、只留在手机里、绝不上传（否定式）、无需注册账号、本机加密、无广告 |
| zh-TW | 純離線、只留在本機、絕不上傳（否定式）、無需帳號、本機加密、無廣告 |
| ja | 完全オフライン、端末内のみ、外部送信ゼロ、アカウント不要、広告ゼロ |
| ko | 100% 오프라인、이 기기에만、클라우드 전송 없음、계정 없음、광고 없음 |
| es | 100% sin conexión、sin nube、sin cuentas、solo en este dispositivo、cero anuncios |
| fr | 100% hors ligne、sans cloud、sans compte、uniquement sur cet appareil、zéro publicité |
| pt-BR | 100% offline、sem nuvem、sem contas、nunca saem deste dispositivo、zero anúncios |

### 3.4 ⚠️ 歧义词专条（本轮最易踩的隐蔽坑）

| 存量文本 | 位置 | 实际含义 | 红线判定 |
|---|---|---|---|
| `heat_scope_fmt`：「(Synced across all 20 games)」 | `values/strings.xml:244` | **本地**热度档位在所有游戏内统一 | 指本地状态一致，**不是网络同步** |
| 「自动同步到全部 20 款游戏」/「自動同步到全部 20 款遊戲」 | `values-zh-rCN/strings.xml:243`、`values-zh-rTW/strings.xml:243` | 同上 | 同上 |
| `mode_ldr_sync`：「Long-Distance Sync」/「异地同步」/「異地同步」，`mode_ldr_sync_desc`：「Sync a tiny moment together from anywhere」 | `values/strings.xml:467-468` 及 8 语言 | 双人**本地倒计时**玩法，无联网 | **高风险命名**：「Sync / 同步 / from anywhere / 异地」极易被读成远程联网或云同步，与零网络叙事冲突 |
| `msg_card_added`：「Saved privately to your device」 | `values/strings.xml:110` | 本地保存 | ✅ 正向，允许复用 |

**红线 `RED-P-1`**：
- 本轮**不得新增**任何 `sync / synchronized / 同步 / 同期 / 동기화 / синхронизация` 类动词来描述数据流动；
- 若必须表达「全游戏统一」，改用**不含同步歧义**的表述：`applies to all 20 games` / 「全部 20 款游戏统一使用」/「全ゲーム共通」/「모든 게임에 동일하게 적용」；
- `mode_ldr_sync` 系列属**存量**高风险命名，本轮若被触及须改为本地化表述（如「同一时刻 / A Shared Countdown / 함께 카운트다운」）；**是否纳入本轮改动范围，交回主理人确认**（见第 5 章 Q4）；
- 新增任何「分享 / 导出 / 发送给 / 生成链接」类能力或文案 → 同时违反 `RED-A6`、`RED-P` 与 IARC 问卷「UGC 有限、无发布/分享」的申报 → **必须重填问卷**。

### 3.5 判定方法

1. 对 `strings.xml`（8 语言）+ `assets/content/*/*.json`（13 类 × 8 语言）跑 3.2 词表正则（区分肯定式与否定式）。
2. 人工复核本轮 `git diff` 中所有**新增/修改**的字符串值，逐语言确认无 3.2 语义。
3. 交叉核对 `docs/GOOGLE_PLAY_LAUNCH_CHECKLIST.md:28`（Data Safety：零广告 / 零分析 / 零云同步）与 `PLAY_CONSOLE_CONTENT_RATING.md:23,47-49`（在线互动=无、UGC=有限、无收集无共享）——**文案、表单、代码三者必须一致**。

---

## 4. `RED-T` 技术面红线（不得削弱 / 不得新增）

### 4.1 威胁模型

```
[THREAT-001] 内容越界导致上架资格与分级失真
资产：
- Google Play 上架资格与账号信誉
- IARC 内容分级证书与地区曝光（EEA / 巴西 / 韩国等）
- 「零权限零网络零广告、数据不出本机」差异化卖点
- 用户信任与付费口碑
威胁：
- 本轮为追求「诱惑 / 刺激」而引入动作化、露骨或强制语义 → 触碰 2026-10-29 生效的性内容政策
- 4 档热度中任一档越界 → 已申报问卷失真 → 下架 / 停权 / 重新评级
- 8 语言本地化时无意直译出露骨词或未成年暗示
攻击面：
- 8 语言 strings.xml（各 652 条）
- 13 类内容 JSON × 8 语言（tier 3/4 与 VIP 模式）
- 商店文案 / ASO / paywall / 截图
缓解：
- RED-A 零容忍词表 + RED-C 实现路径 + RED-D 分级封顶 + RED-E 地区收敛
- Phase 4 全量 8 语言扫描 + 人工抽样
- 内容改动触发 IARC 问卷重填复核

[THREAT-002] 隐私叙事失真
资产：
- Data Safety 表单「无收集 / 无共享」申报
- 隐私政策 URL 与首启隐私承诺
- allowBackup=false / FLAG_SECURE / Keystore 加密构成的技术事实
威胁：
- 新增「上传 / 同步 / 云端 / 账号 / 分享 / 定位附近」文案，与零权限零网络矛盾 → 申报不实
- 新增能力或 SDK 使 Manifest / 依赖清单与叙事不一致
攻击面：
- 8 语言界面文本与内容 JSON
- AndroidManifest、build.gradle 依赖、第三方 SDK
缓解：
- RED-P 词表 + 3.5 判定方法 + Phase 4 diff 复核
```

### 4.2 权限审查

```
[PERM-001]
权限：android.permission.VIBRATE（现状唯一）
必要性：高（震动反馈，核心交互）
现状：Manifest 仅声明 VIBRATE；无 INTERNET / 定位 / 存储 / 相机 / 录音 / 通知权限
问题：无。符合最小必要原则
修复 / 红线：
- 本轮及任何后续新增内容不得引入任何新 uses-permission
- 尤其禁止 INTERNET、ACCESS_FINE/COARSE_LOCATION、CAMERA、RECORD_AUDIO、
  READ/WRITE_EXTERNAL_STORAGE、POST_NOTIFICATIONS、QUERY_ALL_PACKAGES
- <queries> 维持现状（仅 market:// + com.android.vending）
- 不得新增 exported 组件；AppLocalesMetadataHolderService 保持 enabled=false / exported=false
- 不得修改 android:allowBackup="false"
```

### 4.3 存储审查

```
[STORAGE-001]
数据：密封情书正文、昵称、边界设置、自建卡牌、配对与历史记录
现状：情书经 LetterCrypto = AndroidKeyStore AES-256-GCM（256 位、不可导出密钥）加密落盘；
      allowBackup=false；其余为本地 SharedPreferences / 本地存储
风险：
- LetterCrypto.encrypt() 在 Keystore 拒绝出钥时返回 null，代码注释明示
  「the caller then stores the body unencrypted」——存在明文降级落盘路径
- 该路径若被用于新增内容，将削弱「加密落盘」的隐私叙事
修复 / 红线：
- 不得将 Keystore 加密降级为明文 / 自实现算法 / 硬编码密钥 / 弱模式（ECB、固定 IV）
- 本轮不得扩大上述降级路径的适用面，不得新增任何绕过加密写入的入口
- 建议（交 android-architect / 主理人）：后续改为「加密失败即拒绝写入并提示」，
  尤其在 FLAG_SECURE 之外再补一道兜底；本轮不实施
- 新增文本不得声称「已加密」而实际走明文路径
```

### 4.4 网络审查

```
[NET-001]
接口：无（App 自身零网络接口）
现状：无 INTERNET 权限、无 Network Security Config 明文放行、无 WebView、无 HttpClient/OkHttp；
      唯一外部交互为 Play Billing 结算（由 Google Play 服务托管，不上行用户内容数据）
风险：
- 新增网络调用、第三方 SDK、WebView、Deeplink 或明文流量配置会直接摧毁卖点并使 Data Safety 失真
修复 / 红线：
- 不得新增任何网络调用、OkHttp/Retrofit/Volley/WebView、android:usesCleartextTraffic
- 不得新增第三方 SDK（尤其广告 / 分析 / 崩溃上报 / 推送 / 归因 / 云同步）
- 不得为内容加载引入远程配置、远程题库、A/B 下发
- 新增自定义卡输入仍为本地 UGC，不得新增发布 / 分享 / 导出到外部的能力
- 若 Phase 4 发现 Manifest 或依赖清单出现 INTERNET / 新 SDK，判为阻断级
```

### 4.5 完整性、混淆与既有防护

```
[INTEGRITY-001]
现状：FLAG_SECURE 默认 true（EntitlementManager:83，MainActivity:41-42 应用）；
      密封情书揭示时间基于 System.currentTimeMillis()（SealedLetter / SealedLetterManager /
      DatePlanManager）；R8 与签名配置不在本轮范围
红线：
- 不得将 FLAG_SECURE 默认值改为 false，不得移除 MainActivity.onCreate 的应用逻辑
- 新增文案不得诱导用户关闭防截屏，不得把「关闭防截屏」描述为推荐操作
- 不得移除或弱化任何基于时间的封存 / 揭示 / 等待校验，不得新增「跳过等待 / 立即解锁」
  的文案或入口（防时间回拨作弊既有防线）
- 不得引入可篡改 tier / 边界 / Hard No 过滤逻辑的文案或能力
- 建议（交 android-build）：本轮若改动资源，验证 R8 后关键类与字符串资源未被误删；
  本专家不修改构建脚本与 R8 规则
```

### 4.6 `RED-T` 汇总（可勾选）

- [ ] 未新增任何 `uses-permission`
- [ ] 未新增任何第三方 SDK / 依赖
- [ ] 未新增网络调用 / WebView / 明文流量配置 / Deeplink
- [ ] `allowBackup=false`、组件导出状态、`<queries>` 未变
- [ ] `FLAG_SECURE` 默认 `true` 未变，`MainActivity` 应用逻辑未变
- [ ] Keystore AES-256-GCM 未降级，加密失败降级路径未被扩大
- [ ] 时间封存 / 揭示校验未被移除或弱化
- [ ] 仅改动文本字段值：`id` / `tier` / `tags` / `durationSec` / `slots` / 占位符集合**未变**
- [ ] `scripts/verify_content.py` 8 类校验全部通过（EXIT=0）
- [ ] 8 语言 `strings.xml` 条目数保持对齐，繁体无简体异形字残留

---

## 5. 待主理人确认项（政策不确定项，本专家不自行判定）

| 编号 | 事项 | 建议 | 归属 |
|---|---|---|---|
| **Q1** | 分级口径不一致：17+（`PLAY_CONSOLE_CONTENT_RATING.md`）vs 18+（`全量审计报告`） | 由主理人会同法务 / Play Console 实际问卷结果统一为**唯一口径**，并同步更新两份文档 | team-lead |
| **Q2** | 2026-10-29 生效政策中「any content or services intended to be sexually gratifying」是否适用于本 App 的含蓄亲密提示卡 | 现有文档 §2.1「属于政策允许范畴」的判定建议**重新评估**；本专家仅提示风险，不出具合规结论 | team-lead / 法务 |
| **Q3** | targetSdk 35 → 36 时限（2026-08-31 生效、2026-11-01 延期窗口已过） | 属**阻断级上架风险**，请主理人确认本轮上架计划与升级排期，并转 android-build 评估边缘到边 / 权限变更影响 | team-lead / android-build |
| **Q4** | 存量高风险文案（2.4.3 清单）与 `mode_ldr_sync`「异地同步」歧义命名是否纳入本轮降调范围 | 建议**至少将 `tier_desc_3/4` 与 paywall 动作化命名纳入本轮**，其余由主理人按范围决策；本轮报告不擅自扩大改动范围 | team-lead |
| **Q5** | 首启 18+ 年龄确认弹窗（`PLAY_CONSOLE_CONTENT_RATING.md:64` 待办） | 请主理人确认是否在本轮补入；属降低政策风险与避免归入儿童目录的重要措施 | team-lead / android-release |
| **Q6** | Keystore 加密失败时明文落盘的降级路径 | 建议后续改为「加密失败即拒绝写入」；是否纳入本轮由主理人决策 | team-lead / android-architect |
| **Q7** | 本轮内容改动是否影响 IARC 问卷任一答案 | 由 android-release 在 P5 判定并决定是否重填问卷；改写专家不得自行判断「影响不大」 | android-release |

> 补充说明：Google Play 各政策以 Play Console 全文与账号内通知为准；本报告的第一方来源检索日期为 2026-11-23，**Phase 4 最终扫描前须再次核对最新政策**。

---

## 6. Phase 4 最终合规扫描清单（备用，续接任务使用）

> 使用方法：对**本轮全部改动**（`git diff` 范围：8 语言 `strings.xml` + 13 类 × 8 语言内容 JSON + 文档）逐项勾选；判定方法列给出可复现手段。任何「阻断」项未通过，不得进入 P5 发布。

### A. 内容红线（`RED-A` / `RED-B` / `RED-C` / `RED-D` / `RED-E`）

- [ ] **A-1（阻断）** 8 语言全库跑 `RED-A` 零容忍词表（未成年暗示 / 露骨性行为与器官 / 非自愿与胁迫 / 违法 / 诱导分享 / 意图满足性欲），命中数 = 0
- [ ] **A-2（阻断）** 逐条人工复核 `tier 3/4` 与 9 个 VIP 模式的新增/修改文本，确认无具体动作分解、无强制语态
- [ ] **A-3** 对照 C-ZH / C-EN 各 3 组例句，抽检每语言每档 ≥ 10 条，确认「允许」路径被采用、「越界」句式未出现
- [ ] **A-4** 8 语言强度一致性：同一 `tier` 无任何语言强于英文源文；无「为凑刺激感而直译露骨词」
- [ ] **A-5** 每档用语强度未超 2.4.1 上限表；**T4 仍落在 `Suggestive / Mild`**，未出现动作升级
- [ ] **A-6** 新增文本与「安全词 / 边界 / Pause / Hard No / Private Pass」叙事一致，均保留「可随时停止」语义出口
- [ ] **A-7** `RED-E` 地区收敛已执行：ja / ko / pt-BR / fr / es / zh-CN / zh-TW 无本地高危词，且无「分地区加强」
- [ ] **A-8** 商店文案 / ASO / paywall / 截图成熟度不高于应用内实际内容（沿用 `GOOGLE_PLAY_LAUNCH_CHECKLIST.md:10` 规范）

### B. 隐私叙事（`RED-P`）

- [ ] **B-1（阻断）** 3.2 词表在 8 语言 `strings.xml` + 内容 JSON 中命中数 = 0（区分否定式）
- [ ] **B-2（阻断）** `git diff` 全部新增/修改字符串逐语言人工复核，无「上传 / 同步 / 云端 / 账号 / 分享到 / 定位附近」肯定式语义
- [ ] **B-3** 未新增 `sync / 同步 / 동기화` 类描述数据流动的动词；如需表达「全游戏统一」已改用无歧义表述
- [ ] **B-4** 与 Data Safety 申报（零广告 / 零分析 / 零云同步 / 无收集 / 无共享）、隐私政策 URL、首启隐私承诺三者一致
- [ ] **B-5** 未新增任何「分享 / 导出 / 发送 / 生成链接」能力或文案（否则触发 `RED-A6` 与问卷重填）

### C. 技术面（`RED-T`）

- [ ] **C-1（阻断）** `git diff` 中 `AndroidManifest.xml` 无任何新增 `uses-permission` / 导出组件 / `allowBackup` 变更 / `<queries>` 变更
- [ ] **C-2（阻断）** `app/build.gradle` 依赖清单无新增第三方 SDK；无网络库 / WebView / 明文流量配置
- [ ] **C-3（阻断）** `FLAG_SECURE` 默认值仍为 `true`；`MainActivity` 应用逻辑未变
- [ ] **C-4** `LetterCrypto` 未降级；加密失败降级路径未被扩大；时间封存 / 揭示校验未被弱化
- [ ] **C-5** 内容资产仅改文本值：`id` / `tier` / `tags` / `durationSec` / `slots` / 占位符集合未变
- [ ] **C-6** `python scripts/verify_content.py` EXIT=0（8 类回归全绿）
- [ ] **C-7** 8 语言 `strings.xml` 条目数对齐；繁体无简体异形字残留；法语撇号 `\'` 转义正确
- [ ] **C-8** 构建通过（`./gradlew :app:assembleDebug`）；R8 后关键类与字符串资源未被误删（交 android-build 复核）

### D. 分级与上架（`RED-D` / Play 政策）

- [ ] **D-1（阻断）** 主理人已就 Q1 统一分级口径，两份文档口径一致
- [ ] **D-2（阻断）** 已判定本轮内容改动是否影响 IARC 问卷答案；若影响则由 android-release **重填问卷并留存证书**
- [ ] **D-3** 问卷各项与实际内容一致：暴力=无 / 语言=偶尔温和 / 裸露=无图形裸露 / 性暗示=Suggestive(或有依据的上调) / 酒精=轻微提及 / 赌博=无真实赌博 / 在线互动=无 / UGC=有限无分享
- [ ] **D-4** 家庭政策：非儿童向；首启成人向定位 / 18+ 年龄确认（Q5 结论）已落实
- [ ] **D-5** 隐私政策 URL 可访问；订阅价格 / 自动续订 / 取消方式 / 管理订阅入口齐全（`PLAY_CONSOLE_CONTENT_RATING.md:41-44`）
- [ ] **D-6** **Q3 已确认**：targetSdk 35 → 36 的排期或延期结论明确，且不影响本轮提交计划
- [ ] **D-7** Phase 4 执行前**再次核对 Play 最新政策**（本次检索日期 2026-11-23）
- [ ] **D-8** 保留本轮题库审核记录（本报告 + 扫描结果 + CI 报告）以备 Google 申诉（沿用 `PLAY_CONSOLE_CONTENT_RATING.md:65`）

### E. 回归验证

- [ ] `verify_content.py` 8 类全通过
- [ ] 8 语言各档抽样抽题无空池、无占位符残留（`{p1}` / `{zone}` 等渲染正常）
- [ ] 新增文本在各语言 UI 中无截断 / 溢出（交 android-ui 复核）
- [ ] 防连续重复与边界过滤（Hard No）功能回归正常（交 android-runtime / android-test 复核）

### Play 合规清单（技术建议，非法务判定）

```
[COMPLIANCE-001]
检查项：
- [ ] 数据安全表单已填写并与实际采集一致（无收集 / 无共享 / 无第三方 SDK）
- [ ] 隐私政策链接可访问且与「数据不出本机」叙事一致
- [ ] 广告 ID / 广告 SDK：无（不涉及声明）
- [ ] 后台定位 / 敏感权限：无（仅 VIBRATE，无需用途说明）
- [ ] 目标 API 级别符合当年要求（Q3：2026-08-31 起需 API 36，当前 35 —— 待确认）
- [ ] 第三方 SDK 已核实合规（仅 Play Billing，无分析 / 广告 / 归因）
- [ ] 内容分级问卷如实填写；本轮内容改动后已评估是否重填
- [ ] 订阅披露、管理订阅入口、自动续订告知齐全
- [ ] 家庭政策：未被归入儿童目录，无未成年暗示（RED-A1）
- [ ] 商店截图 / ASO / paywall 无露骨词汇与「意图满足性欲」表述
风险项：
- 分级口径 17+ / 18+ 不一致（Q1）
- 性内容政策 2026-10-29 生效措辞收紧（Q2）
- 存量 tier3/4 与 VIP 模式动作化文案（2.4.3）
- 「异地同步 / Synced」歧义叙事（3.4）
- targetSdk 未达 API 36（Q3）
```

### 安全报告汇总模板（Phase 4 填写）

```
[SEC-REPORT-001]
总计检查项：__（A 8 + B 5 + C 8 + D 8 + E 4，共 33 项）
通过：__  风险：__  阻断：__
高风险（阻断级）：
- A-1 零容忍词表命中
- C-1 / C-2 权限或依赖变更
- D-2 问卷未随内容改动重填
中风险：
- T4 强度越界但未达露骨
- 地区语言直译导致的强度不一致
低风险：
- 歧义词（Synced / 同步）残留
优先级：先清阻断项 → 再清高危 → 最后处理歧义与文案打磨
结论：□ 可进入 P5   □ 需返工
```

---

## 7. 移交 android-release 的上架合规清单（P5 输入）

> 依据角色职责「产出必须给 android-release 提供合规清单」，以下为 P5 可直接使用的条目，均源自本轮红线。

1. **Data Safety**：申报「无数据收集、无数据共享、无第三方 SDK」；与 `GOOGLE_PLAY_LAUNCH_CHECKLIST.md:28`、`PLAY_CONSOLE_CONTENT_RATING.md:47-49` 及实际 Manifest 三方一致。
2. **隐私政策 URL**：必填且可访问；内容须写明「纯离线、无账号、无云同步、数据仅存本机、密封情书本机加密、`allowBackup=false`」，**不得出现上传/同步/分享语义**。
3. **内容分级问卷**：按 D-3 逐项如实作答；**统一 17+ / 18+ 口径（Q1）**；本轮内容改动后评估并按需**重填问卷、留存证书与地区分级结果**。
4. **地区曝光**：注意 EEA / 澳大利亚 / **巴西** / 新加坡 / 瑞士 / 英国对未成年人屏蔽 18+ 内容；韩国 GRAC 地区分级独立生成——分级选择直接影响曝光，需与主理人确认。
5. **家庭政策声明**：非儿童向；落实首启成人向定位 / 18+ 年龄确认（Q5）；商店截图与图标不得含任何未成年暗示（`RED-A1`）。
6. **商店素材**：沿用 `GOOGLE_PLAY_LAUNCH_CHECKLIST.md:10` 的「Zero Banned Emojis / Explicit Terms」；应用名 30 字符、简短说明 80 字符；成熟度不高于应用内实际内容。
7. **订阅合规**：价格、自动续订条款、取消方式、隐私政策链接、服务条款链接、「管理订阅」入口（`PLAY_CONSOLE_CONTENT_RATING.md:41-44`）。
8. **目标 API**：确认 Q3 结论（2026-08-31 起需 API 36，延期至 2026-11-01），避免提交被拒。
9. **证据留存**：本报告、Phase 4 扫描结果、`verify_content.py` CI 报告、问卷证书，作为 Google 申诉材料（`PLAY_CONSOLE_CONTENT_RATING.md:65`）。
10. **第三方 SDK 声明**：仅 Play Billing；零广告 / 零分析 / 零崩溃上报 / 零归因 / 零云同步。

---

## 附录 A 一页速查（给改写者贴在手边）

| 要做 | 不要做 |
|---|---|
| 写情绪、呼吸、灯光、距离、心跳、留白、省略号 | 写具体身体动作、部位清单、器物 |
| 写「我们 / 一起 / 愿意的话 / 你说停就停」 | 写命令式、单方施动、按住/不许躲/惩罚 |
| 用悬念与邀约提升刺激感 | 用露骨描写或动作升级提升刺激感 |
| 8 语言一律 ≤ 英文源文强度，不够就降档 | 为凑「开放 / 诱惑」直译露骨词 |
| 说「只留在本机 / 无需账号 / 绝不上传」 | 说「上传 / 同步 / 云端 / 账号 / 分享到 / 附近的人」 |
| 只改文本字段值 | 改 id / tier / tags / slots / 占位符 / 权限 / 依赖 |
| T4 只加情绪密度 | T4 加动作清单 |

**一句话判据**：*把这句话念给一个陌生人听，如果它描述的是「两个成年人之间的情绪与邀请」→ 允许；如果它描述的是「对身体做了什么」或「让数据去了哪里」→ 越界。*

## 附录 B 证据索引

| 文件 | 引用位置 |
|---|---|
| `app/src/main/AndroidManifest.xml` | 4, 7-13, 17, 26-43 |
| `app/src/main/res/values/strings.xml` | 5, 12-17, 22, 27-35, 90-100, 110, 198, 211-219, 243-248, 323, 399, 467-468, 584 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 28-35, 218, 243-247, 467-468 |
| `app/src/main/res/values-{zh-rTW,ja,ko,es,fr,pt-rBR}/strings.xml` | 22, 196-197, 218, 243, 467-468 |
| `core/letter/LetterCrypto.java` | 26-96 |
| `core/billing/EntitlementManager.java` | 22, 81-87 |
| `MainActivity.java` / `ui/settings/SettingsFragment.java` | 41-42 / 111-120 |
| `SealedLetter.java` / `SealedLetterManager.java` / `DatePlanManager.java` | 时间基准相关行 |
| `scripts/verify_content.py` | 1-189 |
| `docs/PLAY_CONSOLE_CONTENT_RATING.md` | 8-26, 30-55, 59-65, 69-74 |
| `docs/GOOGLE_PLAY_LAUNCH_CHECKLIST.md` | 10-22, 24-28 |
| `audit_report/VelvetCouples_全量审计报告.md` | 21, 62-73, 127-135, 152-160 |
| `audit_report/修复实施报告.md` | 1-19, 67-89 |
| Google Play 政策（第一方，检索于 2026-11-23） | `support.google.com/googleplay/android-developer/answer/9878810`、`answer/18258653`、`answer/11926878`、`play.google.com/intl/ja/about/storelisting-promotional/content-ratings`、`developer.android.com/distribute/best-practices/develop/target-sdk` |

---

> **阶段门**：本报告为 Phase 1 红线定义，**不含对本轮改动的最终合规结论**。Phase 4 收到主理人续接任务后，按第 6 章对全部改动执行最终合规扫描并出具 `[SEC-REPORT-001]`。




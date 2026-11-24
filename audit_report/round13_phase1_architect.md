# VelvetCouples 第 13 轮 · Phase 1 架构基准报告

> **子标题**：需求拆解 / 竞品缺口对标 / 可测量验收标准 / 私密调性规范
> **作者**：架架（android-architect，需求与架构专家）
> **下发**：铮铮（android-studio-expert-team-lead），任务卡 #1
> **日期**：2026-11-23
> **版本**：**v1.1**（同日增量修订；v1.0 为初始交付）。v1.1 依据两位队友 Phase 1 结果对齐：`audit_report/round13_phase1_knowledge.md`（识识：第八轮 8 项销账表 §3.3、ADR-014、未命中项 M-01）与 `audit_report/round13_phase1_security.md`（安安：内容合规红线、情绪密度封顶、存量降调清单、Play 2026-10-29 政策）。**核心结论（推荐落地 3 项、47 条 AC、阶段门）不变**；增量集中在 §4 调性硬约束、§4.10 存量降调清单、§4.11 安全机制呼应、§5 ADR-009/010、§8 风险、§9 待确认。
> **阶段门**：**Phase 1 仅产出基准与清单，不修改任何代码、布局、strings.xml、内容 JSON**（写权限归 ui/data 专家）。第 1 项补充内容的最终落地范围由 **Phase 3 续接任务拍板**，本报告不提前定案、不提前改代码。
> **铁律约束（不可动摇）**：纯本地单机、零网络、零位置权限（manifest 仅 VIBRATE）、所有数据仅存本机；「零权限零网络零广告、数据不出本机」是 Play 商店差异化卖点。任何补充内容不得引入账号 / 云同步 / 地图 SDK / 位置权限 / 第三方 SDK。
> **全程不与用户直接对话**；不能自行推断的事项统一列入 §9 待确认项，交回 team-lead。

---

## 0. 本报告的定位与阶段门

### 0.1 本报告在下游链路中的位置

```
用户 7 项原始需求
        │
        ▼
【本报告 · Phase 1】架架  ← 唯一「判定基准」来源
   ├─ §2 验收标准 (AC-*) ─────────────► team-lead 汇总验收
   ├─ §3 竞品缺口清单 ────────────────► Phase 3 拍板范围（架架续接）
   ├─ §4 私密调性规范 ──┬────────────► android-ui   （需求 2、3：界面文本）
   │                    ├────────────► android-data  （需求 5、6、7：题库文本）
   │                    └── §4.2 否决层► android-security（安安：合规红线终裁）
   ├─ §5 ADR / §6 模块图 / §7 接口契约 ► android-build、android-knowledge
   └─ §8 风险 / §9 待确认 ────────────► team-lead
```

### 0.2 判定方向的统一约定（避免歧义）

| 维度 | 方向 | 说明 |
|---|---|---|
| 用户价值 V | 1~5，**越高越好** | 5 = 直接强化「双人私密夜晚」核心体验 |
| 实现风险 R | 1~5，**越高越差** | 5 = 可能破坏零权限零网络或引入历史陷阱 |
| 工作量 W | 1~5，**越高越重** | 5 = 需新建 8 语言内容资产 + 多模块改造 |
| 零权限零网络 C | PASS / WARN / FAIL | **FAIL 一票否决**，不进入排序 |
| 综合分 S | `S = V×2 − R − W + (PASS ? 2 : WARN ? 0 : −∞)` | 按 S 降序推荐 |

### 0.3 事实基线口径

本报告所有「现有能力」结论均经**源码实证核对**（非引用任务卡描述）：
`GameModeCatalog.java`（36 模式实测枚举）、`ConsentFilter.java` / `ConsentRepository.java`（边界机制）、`DeckFragment.java` + `FavoriteTaskDao` / `CustomTaskDao`（收藏与自定义题）、`values/strings.xml`（关键词全量 grep）、`assets/content/en/*.json`（13 类实测）、历史工作日志十二轮。

---

## 1. 事实基线（先对齐，再设计）

### 1.1 工程与资产实况（已核验）

| 项 | 实测值 | 核验来源 |
|---|---|---|
| 技术栈 | Java 17 / 单 Activity + Navigation Component + ViewBinding / SharedPreferences + LiveData / Room / Material 3 | 任务卡 + 源码 |
| 游戏模式 | **36**（20 原始 + 12 social + DAILY_REVEAL + DATE_PLANNER + SEALED_LETTER + LOVE_GRAPH） | `GameModeCatalog.java` 枚举逐项计数 |
| 界面字符串 | 8 语言 × **652 条** | 441（第二轮）+41（six）+46（nine）+43（ten）+64（eleven）+17（twelve）= 652，自洽 |
| 界面语言目录 | `values`(en) / `values-zh-rCN` / `values-zh-rTW` / `values-ja` / `values-ko` / `values-es` / `values-fr` / `values-pt-rBR` | 任务卡；**pt 目录为 `-rBR` 而非 `-pt`**（历史踩坑） |
| 内容资产 | `assets/content/{en,zh,zh_tw,ja,ko,es,fr,pt}/` × **13 类** JSON | 实测 en 目录 13 个文件：board_themes / draw_words / generator_templates / couple_quiz / fate_cubes / party_duel / scratch_cards / decks / intimate_modes / roleplay_scenarios / extreme_modes / date_planner / sealed_letter |
| 主题 | 12 套 velvet 夜色主题（枚举驱动） | 第三轮 + `AppTheme` |
| 权限 | manifest 仅 `VIBRATE`；`FLAG_SECURE` 防截屏；`allowBackup=false` | 任务卡 + 合规文档 |
| 测试基线 | 全量单测 **131/131 通过**；`verify_content.py` / `dp_lint.py` / `sl_lint.py` / `check_strings_mix.py` EXIT=0；assembleDebug + assembleRelease(R8) SUCCESSFUL | 第十二轮 |
| 抽题基线 | 74,880 次真实抽题 0 空池；连续重复仅 couple_quiz t1 = 1.7% | 全量审计报告 |

### 1.2 ★ 已具备能力清单（决定「缺口」的排除集）

> 这是本轮最关键的一次核对：**下列能力在同类 App 中属于「常见卖点」，但本项目已实现，不得计入「仍然缺失」**。若跳过此步，会把已做过的东西重复推荐，直接误导 Phase 3 拍板。

| 同类 App 常见能力 | 本项目对应实现 | 状态 |
|---|---|---|
| 收藏 / 重温喜欢的卡片（Cloxer Favorites、Twone favorites、Question Games Saved） | `FavoriteTaskDao` / `FavoriteTaskEntity` / `FavoriteTaskAdapter`，`DeckFragment` 双 Tab（`btnTabFavorites`） | ✅ 已具备 |
| 自定义题目 / 自建牌组（Truth or Dare Partygame、Date Night、SyncPlay、Question Games） | `CustomTaskDao` / `CustomTaskEntity` / `CustomTaskAdapter`，`DeckFragment.btnAddCustom` → `ContentRepository.addCustomTask(prompt, tier, DARE)` | ✅ 已具备 |
| 双人边界 / 安全词 / 取双方最低舒适度（Twone「Yes·Maybe·No」+「never beyond the lowest」） | `ConsentRepository`（11 条默认规则，A/B 分别设 `YES`/`SOFT_LIMIT`/`HARD_NO`）+ **`ConsentFilter.replaceRules()` 任一方 HARD_NO 即全局阻断** + onboarding 自定义 Safe Word + 游戏内 Pause/Safe Word bar + Private Pass（隐藏个人否决） | ✅ 已具备（机制等价，粒度为「标签级」而非「活动级」） |
| 每日双人揭晓问答（Paired Daily Questions / unlock mechanic） | `DailyRevealFragment` + `PassPhoneDialog` + 并排揭晓 | ✅ 已具备 |
| 依恋 / 爱的语言轻量测验（Paired） | `LOVE_LANGUAGE` 模式（1 火焰，免费） | ✅ 轻量版已具备 |
| 关系时间线 / 里程碑 / 记忆档案（Paired Timeline、Couple Joy Journal） | `SessionStatDao` / 记忆档案区 / `StreakCalculator` / `BadgeCatalog` / 在一起天数与纪念日 | ✅ 已具备 |
| 约会规划 / 双人互选交集（同类 Date Planner） | `DatePlannerEngine`（A/B 互不可见 → `getMutualMatches()` 取交集）+ `DatePlanManager` | ✅ 已具备 |
| 关系可视化「地图」（Couple Joy map） | `LOVE_GRAPH` 关系星图（**纯 Canvas、无地图 SDK、无位置权限**） | ✅ 已具备且**刻意不做真地理地图** |
| 私密内容加密存储（Twone PIN 区、SyncPlay App Lock 的加密诉求） | `LetterCrypto`（Android Keystore AES-256-GCM）用于密封情书 + `FLAG_SECURE` 全局防截屏 | ⚠️ 部分具备（加密已有，但**无应用级 PIN/生物锁**） |
| 离线 / 无账号 / 无广告 / 数据不出本机 | 全局架构约束 | ✅ 已具备且为**核心差异化卖点** |

### 1.3 历史竞品调研结论回顾（第六、八轮，工作日志原文要点）

- **第六轮**：调研渠道 = Google Play（Couple Games: Fun for Lovers / Darezo / Truth or Dare 2 / Fizzy / Never Have I Ever）、couplemissions.com 对比文、PP 助手 / 豌豆荚 / 吾记日记等中文渠道。去重确定 **12 个新玩法并已全部落地**（conversation_starter / love_language / daily_question / riddle_me / memory_lane / most_likely_to / bucket_list / ldr_sync / forfeit_wheel + scenario_choice / hot_seat / secret_mission）。架构决策：**统一内容池 + 配置驱动引擎 + 数据驱动 Fragment，不新增题库 JSON**（复用 8 语言 decks.json 874 卡，规避数千条翻译质量风险）。
- **第八轮**：同类软件调研建议（Paired / Tandem / Connected / Couple Joy 等）列出 8 项。**按识识 `round13_phase1_knowledge.md` §3.3 逐项销账表（v1.1 采纳为权威口径，替代 v1.0 粗口径）**：
  - **已落地 4 项**：每日双人揭晓问答（`DAILY_REVEAL`）、心情打卡 + 感激留言（`MoodCheckinManager`）、习惯 streak + 成就徽章（`StreakCalculator` / `BadgeCatalog`）、在一起天数 / 纪念日；**另第 10 轮约会规划 `Date Planner`** 落地「约会规划」项，第 11 轮密封情书 / 关系星图亦已落地。第六轮 12 个新玩法全部落地，**均不得重复计入缺口**（历史 ADR-002「统一内容池不新增题库 JSON」、ADR-014「新增模式四件套 + Phase10/12 数量断言 36·24·12」）。
  - **仍未落地 5 项**：`U-01` 依恋 / 性格测验 + AI 洞察、`U-02` 真实地理「爱的地图」、`U-03` 关系日记 / 时间轴、`U-04` 共享日历（跨设备）、`U-05` 一起看 / 同步播放列表。
    - `U-02` 属**主动放弃**：第 11 轮明确判定真实地理地图需网络 / 地图 SDK / 位置权限 / 审图号，已改造为纯 Canvas 抽象关系星图 `LOVE_GRAPH` → **不计入缺口、不得重新推荐**（与 §3.2 一致）。
    - `U-04` / `U-05` 需跨设备同步 / 流媒体 → **FAIL，一票否决**。
    - `U-01` 的 **AI 洞察**需网络（FAIL），但**纯本地问卷**不受限；`U-03` **本地关系日记 / 时间轴**不受限 → 二者可作**纯单机候选**，分别对应 §3.4 的 `E`（暂缓）与推荐 ③「会话日记」。是否采纳属产品范围决策，交 Phase 3 / 用户（§9 #1），架架不自行拍板。
  - **对结论的影响**：无。缺口主线仍是「体验串场层 / 合规入口层 / 回顾层」，且 `U-03` 反向印证推荐 ③ 的方向正确。

### 1.4 本轮补充调研（WebSearch，2026-11-23）

新增对标样本与可借鉴点（详见附录 A；聚合站信息仅供**定性**参考）：

| 竞品 | 定位 | 可借鉴点 | 与零权限零网络的关系 |
|---|---|---|---|
| **Cloxer – Couples Games & Dares** | 离线、无账号、无网络、无广告、无数据收集、Mature 17+ | **① 引导式 Date Night Mode**（选氛围 Warm Up / Deep Dive / Spicy → 每卡计时 → 从头到尾自动串场）② 长按收藏 ③ 7 主题卡组 ④ 一次性买断无订阅 | ✅ 完全同类定位，最贴合 |
| **Twone – gra dla dwojga** | 完全离线、无账号/登录/广告/追踪，PIN 加密 | ① 「Yes·Maybe·No」愿望清单，各自独立填写、**只显示重叠项、任何人的 No 对另一方永不可见** ② 各自设舒适度、游戏取**最低值** ③ 「Stop 永远有效，No 无需解释」 ④ **4-7-8 呼吸暂停屏** ⑤ 对话日记（爱心评估是否被倾听 + 「一句话带走」）⑥ Intimate Zone 18+ PIN 锁 + 防截屏 ⑦ 四档热度 Sensitivity / Desire / Fantasies & Boundaries / Challenges | ✅ 机制与本项目 ConsentFilter 高度同构（印证 §1.2） |
| **Paired** | 需双设备账号配对、订阅 | 每日问答揭晓、1,000+ 测验、依恋风格 / 爱的语言、Timeline、专家 Packs | ❌ 账号 / 云 / 订阅联机 |
| **Couple Joy** | 关系日记、桌面与锁屏小组件 | 关系日记、在一起天数小组件、「当日回忆」 | ⚠️ 锁屏展示私密内容与 `FLAG_SECURE` / 隐私叙事冲突 |
| **Question Games for Couples** | 离线可用 | My Answers 回答档案（按类别/日期筛选）、收藏、导出 PDF/文本 | ⚠️ 导出即数据可离开本机 |
| **Truth or Dare – Partygame / Party Games!** | 离线、零追踪 | 无限自定义题、自定义牌组、本地备份/恢复、5 语言一键切换 | ✅ 自定义题已具备；备份/恢复需产品决策 |
| **Date Night: Couples Games / SyncPlay** | 在线 / 双设备 | Partner Code 联机、AI 生成牌组（Groq）、实时聊天、位置共享、足迹地图 | ❌ 全项硬门槛否决 |
| 中文渠道（情侣游戏离线版 / Couple 离线版） | 离线 | 默契值报告、情感记忆银行、纪念日提醒、**节日限定挑战**、四档难度 | ✅ 节日限定可参考 |

---

## 2. 需求拆解：7 项 → 可验收条目

### 2.1 拆解原则

- **先分清「审计」与「落地」**：7 项中，第 1 项是**净新增落地**（需拍板范围）；第 2~7 项主体是**全量审计**，仅对命中的缺陷做**定点修正**（文本改写属有限落地）。
- **验收标准必须可测量**：一律落到「数量 = 0 / 覆盖率 = 100% / 一致率 ≥ X% / EXIT=0 / 构建成功 / 阈值」，禁止「感觉良好」「基本符合」。
- **判定方法必须可复现**：机器全量扫描（CI 脚本）+ 人工分层抽样 + 双人盲审一致率，三层叠加。
- **责任域落到具体专家**，不写「团队」。

### 2.2 逐项拆解表

#### R1｜补充同类型软件仍然缺失的内容 → **落地（范围待拍板）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-101 | 产出「已具备能力排除集」与「候选缺口清单」各 1 份，候选 ≥ 8 项，每项含 V/R/W/C 四维打分与 S 分排序 | 核对本报告 §1.2 / §3.4 表格完整性 | 架架 | 基准 |
| AC-102 | 明确推荐本轮落地 **2~3 项**，且每项 C = PASS（零权限零网络）、R ≤ 2、不新增 8 语言内容 JSON | 逐项复核 §3.5；`AndroidManifest.xml` 权限白名单比对 | 架架 → team-lead | 基准 |
| AC-103 | 明确「不得推荐」与「暂缓」清单，并逐项写明否决理由 | 核对 §3.2 / §3.6 | 架架 | 基准 |
| AC-104 | **本阶段不得出现任何代码 / 布局 / strings.xml / 内容 JSON 的写入** | 目录清单 + 文件修改时间戳比对（**已实测：该工程非 git 仓库、无 `.git`，`git status` 不可用，须改用目录/时间戳核验**）；本轮仅新增 `audit_report/round13_phase1_architect.md` | 全体 | 阶段门 |
| AC-105 | 落地范围由 Phase 3 拍板并形成 ADR，且需用户确认（不代替用户做产品决策） | 检查 Phase 3 报告含「已确认范围」签署项 | 架架 + team-lead | 待办 |
| AC-106 | 若落地，新增功能不引入任何新权限 / 网络调用 / 第三方 SDK；manifest 权限集合仍为 `{VIBRATE}` | 静态扫描 `AndroidManifest.xml` + `import` 依赖图 + 网络 API 关键词 grep | android-build / android-security | 落地后门禁 |

#### R2｜检测 UI 使用是否符合用户习惯、便于操作 → **审计 + 定点修正**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-201 | 49 个布局 × 8 语言全覆盖走查，输出「可用性问题清单」，每条含 页面 / 控件 / 问题类型 / 严重度 / 建议 | 布局静态解析 + 真机遍历（含 900×1600 与最小宽度） | android-ui | 审计 |
| AC-202 | 触控目标 ≥ 48dp、可点击元素有 contentDescription、文本不被截断/重叠，命中问题数 = 0（或全部登记为已知项并附理由） | 布局 XML 属性扫描 + 真机 uiautomator dump + 截图比对 | android-ui | 审计 |
| AC-203 | 核心链路（首页 → 任一游戏 → 完成一轮 → 返回首页）≤ 3 次点击可达；「递手机」`PassPhoneDialog` 在 36 模式中入口一致 | 导航图 `nav_graph.xml` 路径分析 + 真机走查 | android-ui / android-runtime | 审计 |
| AC-204 | `FLAG_SECURE` 开启状态下不因可用性改动被破坏；返回栈不出现「隐藏底栏的游戏页」回归（历史第三 / 五轮 bug） | NavOptions / `NavigationUI.onNavDestinationSelected` 回归用例 | android-ui / android-test | 回归 |
| AC-205 | 深色 / 12 套主题 / 无障碍（TalkBack 焦点顺序、对比度）可用，问题数 = 0 | 主题枚举遍历 + 无障碍扫描 | android-ui | 审计 |

#### R3｜检测界面内文本是否准确、符合情侣私密软件要求 → **审计 + 文本改写（落地）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-301 | 8 语言 × 652 条 strings.xml **100% 覆盖**；key 集合 8 语言完全一致；无空值、无未转义撇号（fr `'` → `\'`）、无 `&`/`<`/`>` 未转义 | `verify_content.py` 扩展项 + XML 解析 + 历史「法语撇号」专项 | android-ui / android-build | 审计 |
| AC-302 | **事实一致性 = 0 处不符**：界面文案与实现参数一致（时长、数量、模式数、VIP/免费标注、倒计时、占位符）。重点复检历史第五轮「20 秒深吻 vs durationSec=45」类问题 | 文案数值 ↔ 代码常量 / JSON 字段自动比对表 | android-ui / android-data | 审计 |
| AC-303 | **占位符泄漏 = 0**：`%1$s` / `{p1}` / `{zone}` 等在运行时全部被填充；带占位符的 string 不得直接写在 `android:text`（历史第十二轮「运行崩溃」坑） | 布局 XML 扫描 + 运行时冷启动遍历 | android-ui / android-runtime | 审计 |
| AC-304 | 界面文本负面清单（功能说明书式 / 命令式 / 物化 / 羞辱 / 公开场合尴尬）命中数 = 0；正面特征命中 ≥ 4 项/条（按 §4.6 TONE-CHK） | 8 语言关键词黑名单 + 人工抽样复核 | android-ui（依据 §4） | 审计 + 改写 |
| AC-305 | 串词 / 简繁 / 语种混入 = 0：`check_strings_mix.py`、`dp_lint.py`、`sl_lint.py` EXIT=0；zh-TW 无简体专用字、ja 无简体专用字串、ko 无汉字/假名 | 三个既有 lint 脚本全量跑 | android-ui / android-build | 回归 |
| AC-306 | 成人向定位表述在首启 / 隐私承诺页 / 设置页一致，无「面向儿童」误导 | 文案逐页比对 + 合规文档 §2.4 | android-ui / android-security | 审计 |

#### R4｜检查各个游戏、功能是否正常 → **审计（回归）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-401 | **36/36 游戏模式** + 12 项关系功能（揭晓 / 心情 / 徽章 / 纪念日 / 约会计划 / 情书 / 星图 / 主题 / 防截屏 / 好评 / 隐私页 / 边界）冷启动至完成一轮，**崩溃 = 0、ANR = 0** | 真机 + 模拟器遍历；`adb logcat` 崩溃扫描 | android-runtime | 审计 |
| AC-402 | 全量单测 **131/131 通过**（现有基线），新增改动后 failures=0 / errors=0 | `./gradlew :app:testDebugUnitTest --offline` | android-test | 回归 |
| AC-403 | 74,880 次抽题验证：**0 空池**，连续重复率 ≤ 2%（基线 couple_quiz t1 = 1.7%） | `FullCorpusValidationTest` 复跑 + `full_corpus_report.csv` | android-test | 回归 |
| AC-404 | `assembleDebug` 与 `assembleRelease`(R8) 均 BUILD SUCCESSFUL；`verify_content.py` EXIT=0 | 构建命令 + CI | android-build | 回归 |
| AC-405 | 边界链路有效：任一方 `HARD_NO` 标签在全部 36 模式下均不可被抽出（历史 ConsentFilter 契约） | `ConsentFilter.isAllowed()` 单测 + 真机抽题交叉验证 | android-runtime / android-security | 审计 |
| AC-406 | 时序 / 加密功能：密封情书到期可拆封、回拨系统时间**不可**提前拆封、R8 后字段名未被混淆（历史 `proguard-rules.pro` 坑） | `SealedLetterTest` / `SealedLetterStoreContractTest` + 真机 | android-runtime / android-test | 回归 |

#### R5｜检查游戏内题目表述是否准确、无歧义/不正常题目 → **审计 + 题目改写（落地）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-501 | **13 类 × 8 语言内容资产 100% 扫描**：歧义 / 病句 / 场景自相矛盾 / 模板拼接残留 / 截断标签 / 不合理重复 = **0 命中**（或全部登记并附豁免理由） | `verify_content.py` 8 类检查 + 历史 v21 语法规则集 + 人工抽检 ≥ 10% | android-data | 审计 |
| AC-502 | 题目「可实现性 = 0 处异常」：时长 ↔ `durationSec` 一致、道具/场地在纯本地双人同机场景下可完成、无第三人 / 公共场合 / 需联网前提 | 字段比对 + 场景语义人工复核 | android-data | 审计 |
| AC-503 | 跨语言同 ID 语义等价抽检一致率 ≥ 95%；zh-TW 简繁异形字 0 命中；8 语言 ID 集合 `ID_MISSING=0` | ID 对齐脚本 + 双语对照抽样 | android-data | 审计 |
| AC-504 | 参考同类 App（Cloxer / Twone / 中文渠道）输出「题目质量对标结论」≥ 1 份，明确本项目相对优势与需修项 | §1.3 / §1.4 结论复核 | android-data（架架提供基准） | 审计 |
| AC-505 | 改写后 `verify_content.py` / `dp_lint.py` / `sl_lint.py` / `check_strings_mix.py` EXIT=0，且不引入新 ID / 新占位符 | 四脚本全量跑 | android-data / android-test | 回归 |

#### R6｜精确 8 语言界面文本与题目符合对应地区用户习惯 → **审计 + 本地化改写（落地）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-601 | 8 语言各产出 1 份「地区用语基线」（称谓 / 敬语层级 / 禁忌词 / 变体），覆盖 en / zh-CN / zh-TW / ja / ko / es / fr / **pt-BR** | 对照本报告 §4.8 + 母语/近母语复核 | android-data / android-ui | 基准 |
| AC-602 | **变体错误 = 0**：pt-BR 不出现 pt-PT 用词、es 不出现地区歧义俚语、ja 不出现简体专用字串、ko 不出现汉字、zh-TW 不出现大陆用语 | `check_strings_mix.py` 扩展规则 + 人工抽检 | android-data | 审计 |
| AC-603 | 界面与题库「机翻腔 / 串词 / 拉丁词混入 CJK」= 0 命中 | `dp_lint.py` + `check_strings_mix.py` | android-data / android-ui | 回归 |
| AC-604 | 8 语言本地化盲审：每语种抽检 ≥ 30 条，双人一致率 **≥ 90%**，不一致项全部闭环 | 双人独立评分 + 仲裁记录 | android-data / android-ui | 审计 |
| AC-605 | 8 语言 strings.xml key 集合与 13 类 JSON ID 集合均 100% 对齐 | `verify_content.py` | android-build | 回归 |

#### R7｜精确 8 语言界面文本与题目符合私密软件调性（羞涩又开放…） → **审计 + 调性改写（落地）**

| AC | 验收标准（可测量） | 判定方法 | 责任域 | 性质 |
|---|---|---|---|---|
| AC-701 | §4 私密调性规范被 ui / data 两域**共同引用为唯一判定基准**，不另立标准 | 检查下游报告引用编号 | team-lead | 基准 |
| AC-702 | 8 语言界面文本 + 题库文本全量跑 §4.6 `TONE-CHK`：**负面清单命中 = 0**，正面特征 ≥ 4 项/条，热度梯度与所在模式 `heatRating` 匹配率 100% | 机器黑名单 + 正则 + 人工抽检 | android-ui / android-data | 审计 + 改写 |
| AC-703 | 四档热度梯度（微醺初燃 / 心跳试探 / 暧昧拉扯 / 午夜失控）在 1~5 火焰模式间**单调递增**，无「低火焰高刺激」或「高火焰说明书式」错配 | 模式 `heatRating` ↔ 文案梯度标注比对 | android-data | 审计 |
| AC-704 | **合规否决层 0 命中**：未成年人 / 非自愿 / 胁迫 / 酒后失意识 / 暴力伤害 / 违法 / 露骨器官与性行为描写（8 语言黑名单由 android-security 提供） | `ToneVerdict.forbiddenHit` 全量扫描 | android-security（终裁）+ android-data | 硬门禁 |
| AC-705 | 8 语言各产出 ≥ 2 组「好例子 / 坏例子」对照并纳入改写样例库；调性盲审一致率 ≥ 90% | §4.7 对照表 + 双人盲审 | 架架（基准）/ android-data（扩写） | 基准 + 审计 |
| AC-706 | 调性提升不得触发 Play 敏感内容政策升级：分级问卷仍可申报 Mature 17+ / Adults 18+「Suggestive / Mild」，无「Explicit」项 | ASO 文案 + 截图 + 问卷答复复核 | android-release / android-security | 上架门禁 |

### 2.3 全局验收标准（跨需求硬门槛，任一不满足即整体不通过）

```
[AC-G01] AndroidManifest.xml 权限集合 == {android.permission.VIBRATE}
         —— 无 INTERNET / ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION / 任何第三方 SDK
[AC-G02] 全量单测通过率 100%（当前基线 131/131），failures=0 errors=0
[AC-G03] verify_content.py / dp_lint.py / sl_lint.py / check_strings_mix.py 四脚本 EXIT=0
[AC-G04] assembleDebug + assembleRelease(R8) 均 BUILD SUCCESSFUL
[AC-G05] 8 语言 strings.xml key 集合一致；13 类 × 8 语言内容 JSON ID 集合一致（ID_MISSING=0, PLACEHOLDER_MISMATCH=0）
[AC-G06] 合规否决层（§4.2）8 语言全量 0 命中，由 android-security 签署
[AC-G07] 零回归：本轮文本/功能改动不得使 AC-401~AC-406 任一既有基线劣化
[AC-G08] 阶段门：Phase 1 无任何源码/资源写入；Phase 3 之前不落地第 1 项补充内容
```

### 2.4 需求 ↔ 任务 ↔ 责任域映射矩阵

| 需求 | 主责 | 协同 | 依据本报告章节 | 任务号 |
|---|---|---|---|---|
| R1 补充缺失内容 | 架架（基准/范围建议）→ team-lead 拍板 → 用户确认 | build / security / release | §1.2、§3、§5 ADR-002/006 | #1 → Phase 3 |
| R2 UI 习惯 / 易操作 | android-ui | runtime / test | §2.2 R2、§6 模块图 | #5 |
| R3 界面文本准确 / 私密 | android-ui | data / build / security | **§4 全文** | #5 |
| R4 游戏与功能正常 | android-runtime | test / build | §2.2 R4 | #7、#8 |
| R5 题目表述准确 / 无歧义 | android-data | test / 架架（基准） | §1.3、§1.4、§4 | #6、#8 |
| R6 8 语言地区习惯 | android-data / android-ui | build | **§4.8**、§4.7 | #6 |
| R7 8 语言私密调性 | android-data / android-ui | **android-security（否决层）** / release | **§4 全文** | #6、#3、#9 |
| 横切 | android-knowledge（历史陷阱/ADR） | 全体 | §5、§8 | #2 |

---

## 3. 竞品对标：同类型软件中本项目**仍然缺失**的内容（R1）

### 3.1 硬门槛（一票否决，先于任何打分）

```
[C-1] 不得引入 android.permission.INTERNET 或任何网络调用
[C-2] 不得引入账号 / 登录 / 配对码 / 双设备同步
[C-3] 不得引入位置权限、地图 SDK、真实地理地图、审图号相关实现
[C-4] 不得引入云同步 / 云备份 / 远程内容下发
[C-5] 不得引入第三方 SDK（分析 / 广告 / AI / 推送 / 支付以外）
[C-6] 不得使数据以任何形式离开本机（含「导出到社交平台」）
[C-7] 不得破坏 FLAG_SECURE 防截屏与 allowBackup=false
```

> 满足 C-1~C-7 = PASS；触碰边界需产品决策 = WARN；违反任一条 = **FAIL，直接剔除**。

### 3.2 明确「不得推荐」清单（FAIL / 需联网，逐项写明理由）

| 同类 App 功能 | 出处 | 否决理由 |
|---|---|---|
| 双设备配对 / Partner Code 联机、异步 Couple Match | Paired、Date Night、SyncPlay | 需网络 + 账号，违反 C-1/C-2/C-4 |
| AI 生成牌组（Groq 云端）、「AI 洞察」 | Date Night、第八轮调研建议 | 需网络 + 第三方 SDK，违反 C-1/C-5 |
| 实时聊天 / 语音笔记 / 共享相册 / 云 Memory Box | SyncPlay | 需网络 + 存储权限，违反 C-1/C-4/C-6 |
| 位置共享 / 足迹地图 / Cities Explorer | SyncPlay、Couple Joy | 位置权限 + 地图 SDK + 审图号，违反 C-3（第十一轮已明确结论） |
| 共享日历 / 一起看 / 同步播放列表 | 第八轮调研建议 | 需账号 + 云同步，违反 C-2/C-4 |
| 云备份与跨设备恢复、在线专家视频 / Packs | Truth or Dare、Paired | 需网络，违反 C-1/C-4 |
| 社交平台一键分享 / 导出到社媒 | Date Night、Question Games | 数据离开本机，违反 C-6，且与「数据不出本机」叙事冲突 |
| 第三方分析 / 广告 SDK | 部分竞品免费版 | 违反 C-5，且摧毁 Play 差异化卖点 |

### 3.3 「已具备 → 不计入缺口」对照（避免重复建设）

见 §1.2。**结论：同类 App 的「收藏、自定义题、双人边界/安全词、每日揭晓、里程碑、约会规划、关系可视化、离线无账号」八大常见卖点，本项目均已有等价实现**。因此本轮「仍然缺失」聚焦于**体验串场层、合规入口层、回顾层、设备锁层**四类，而非再堆功能数量。

> 架构判断：项目已有 36 个模式，继续增加「第 37 个独立游戏」的边际价值低、翻译风险高；**高价值缺口是把已有 36 个模式串成「一个完整的夜晚」**，以及补齐上架合规入口。这是本轮推荐的主线（见 ADR-R13-003 / ADR-R13-006）。

### 3.4 候选补充项四维打分排序

| # | 候选补充项 | 对标来源 | V | R | W | C | S | 排序 |
|---|---|---|---|---|---|---|---|---|
| **A** | **引导式「约会夜」串场**：选氛围（微醺 / 深谈 / 心跳）→ 每卡计时 → 自动串场（暖场→深入→收尾）→ 结束回顾；复用既有引擎与内容池，**不新增题库 JSON** | Cloxer Date Night Mode；中文渠道「四档难度」 | 5 | 2 | 3 | PASS | **7** | ① |
| **B** | **18+ / 成人向定位与年龄确认（首启一次，本地布尔）** | 合规文档 §3 待办第 6 条；Twone Intimate Zone 18+；Play Families 政策 | 3 | 1 | 1 | PASS | **6** | ② |
| **C** | **「今晚一句话」会话回顾日记**：每轮结束各写一句 + 心情标记，本地存档、按日回看；复用 MoodCheckinManager / LiveData 范式，**无新增内容资产** | Twone Journal；Couple Joy Journal；Question Games My Answers；**历史第八轮未落地项 `U-03` 关系日记 / 时间轴（识识 §3.3 确认为纯单机候选）** | 4 | 2 | 3 | PASS | **5** | ③ |
| H | 应用级 PIN / 生物识别私密区（加密沿用 Keystore） | Twone Intimate Zone；SyncPlay App Lock | 4 | 3 | 3 | PASS | 4 | ④（暂缓） |
| E | 依恋风格 / 默契度测验报告（完整版，**仅纯本地问卷部分**） | Paired / Attached；**历史未落地项 `U-01`（AI 洞察部分 FAIL，纯本地问卷 PASS）** | 4 | 3 | 4 | PASS | 3 | ⑤（暂缓） |
| F | 节日 / 季节限定卡组（情人节、纪念日、跨年） | 中文渠道；Cloxer 7 主题卡组 | 3 | 2 | 3 | PASS | 3 | ⑥（备选） |
| I | 4-7-8 呼吸 / 情绪缓冲暂停屏（并入 A） | Twone Pause Screen | 2 | 1 | 2 | PASS | 3 | 并入 A |
| J | 活动级「想试 / 再说 / 不要」双人交集清单（只显示重叠、隐藏个人否决） | Twone Yes·Maybe·No | 4 | 2 | 3 | PASS | 5→**3**（与 ConsentFilter/DatePlanner 高度重叠，扣减 2） | ⑦（可选增强） |
| D | 桌面 / 锁屏小组件（在一起天数、当日回忆） | Couple Joy | 3 | 3 | 3 | **WARN** | 0 | 暂缓 |
| G | 本地备份 / 导出（换机迁移、PDF 纪念册） | Truth or Dare、Question Games | 3 | 3 | 3 | **WARN** | 0 | 暂缓 |
| — | §3.2 全部联机/AI/云/定位类 | — | — | — | — | **FAIL** | 剔除 | 不推荐 |

> **S 分说明**：J 原始 5 分，但机制与 §1.2 已有能力重复度 > 70%，按「避免重复建设」扣减 2 → 3。H 分数 4 但 R=3（生物识别 API / 权限声明 / 与 FLAG_SECURE 叠加的边界），故列暂缓。
> **v1.1 与识识销账表的交叉印证**：`U-03`（关系日记 / 时间轴）是历史清单中明确「未落地且不受零权限零网络限制」的一项，正好落在推荐 ③；`U-01` 拆分后 AI 洞察归 §3.2 FAIL、纯本地问卷归 `E` 暂缓；`U-02` 为主动放弃、`U-04/U-05` 为 FAIL。**即：历史遗留的 5 个未落地项中，只有一个（U-03）是本轮真正值得且可安全落地的，其余 4 个均已被铁律或既有决策排除** —— 这进一步支持「本轮只做 3 项、不堆功能数量」的判断。

### 3.5 ★ 推荐本轮落地的 3 项（低风险 · 高价值 · 全部 PASS）

> **注意：本节为「建议」，不是定案。** 最终范围由 Phase 3 拍板 + 用户确认（AC-105）。

#### 【推荐 ①｜A】引导式「约会夜」串场模式 —— 主推

- **用户价值**：36 个模式目前是「平铺列表」，用户需自己决定玩什么、玩多久、何时收尾。同类最强对标 Cloxer 的核心卖点正是「从暖场到收尾，App 替你接管整个夜晚」。本项目内容池（8 语言 decks 874 卡）+ 12 类引擎 + `DareTimerHelper` / `HapticHelper.countdownFinished()` 全部现成，串场只做**编排层**，价值/成本比最高。
- **为什么低风险**：沿用第六轮已验证的架构决策「统一内容池 + 配置驱动引擎 + 数据驱动 Fragment，**不新增题库 JSON**」→ 规避 8 语言数千条翻译与 ID 对齐风险（历史 `ID_MISSING=20` 坑）。
- **落地要点（供 Phase 3 参考，本轮不实现）**：新增纯 Java `core/session/DateNightSessionEngine.java`（Phase 机：`WARM_UP → DEEPEN → PEAK → CLOSING`，每阶段按 tier + `ConsentFilter` + tag 偏好抽卡，复用 `SocialGamesEngine` 的 recent shield 保证不立即重复）；单 Fragment + nav action 带 `session_mood` 参数；8 语言仅新增约 12~18 条 strings（氛围名 / 阶段提示 / 收尾语），**不新增 JSON**；写 `recordSessionStat("DATE_NIGHT", …)` 并在 `TagDisplayMapper` 注册（历史第八轮「fallback 显示情侣棋局」坑）。
- **调性契合**：串场的阶段提示语正是 §4 调性规范的最佳落地载体（微醺初燃 → 心跳试探 → 暧昧拉扯 → 午夜失控 与四阶段天然对齐），直接服务需求 3 / 7。
- **可选并入**：I（4-7-8 呼吸暂停屏）作为「随时可停」的产品化表达，成本 ~2。
- **验收**：AC-102 / AC-106 / AC-G01~G07；串场全流程 0 崩溃、8 语言阶段文案负面清单 0 命中。

#### 【推荐 ②｜B】18+ / 成人向定位与年龄确认 —— 合规必补，近零成本

- **性质说明**：**不是竞品功能缺口，是上架合规缺口**（`docs/PLAY_CONSOLE_CONTENT_RATING.md` §3 待办第 6 条明确列出「在应用首次启动加入 18+ 年龄确认弹窗」）。因成本极低、收益确定，纳入本轮推荐。
- **价值**：降低被 Play Families 政策误归儿童目录的风险；与 IARC 分级问卷「Mature 17+ / Adults 18+」自洽；强化「成年人情侣语境」这一 §4.1 首要硬约束的产品表达。
- **落地要点**：首启一次、结果存本地 SharedPreferences 布尔；8 语言各 2~3 条文案；纯 UI + 本地标记，**零权限零网络**；口径 17+ 还是 18+ 由 android-release + android-security 定（见 §9 待确认 #2）。
- **风险**：R=1，唯一需注意的是不要把弹窗写成「功能说明书式冷冰冰」文案（须过 §4 TONE-CHK）。

#### 【推荐 ③｜C】「今晚一句话」会话回顾日记 —— 次推

- **用户价值**：同类 Twone / Couple Joy / Question Games 都有「对话后回看」层；本项目有 `session_stats` / 记忆档案 / 心情打卡，但都是**数值与徽章**，缺「两个人各自留下一句话」的私密情绪存档。差异点明确，不与已有功能重叠。**v1.1 补强**：识识 §3.3 确认历史第八轮「关系日记 / 时间轴」（`U-03`）是唯一未落地且不受零权限零网络限制的遗留项，本项即其**轻量化落地**（只做「每晚一句话」，不做完整时间轴/照片），历史需求与本轮推荐在此闭环。
- **为什么低风险**：内容为**用户自填**（无需 8 语言翻译资产），复用 `MoodCheckinManager` 的 SharedPreferences + LiveData 范式与既有日期工具 `TogetherDateFormat`；新增纯 Java 引擎 + 单 Fragment + 8 语言约 8~10 条 strings。
- **需注意**：属「用户生成内容（UGC）」——合规文档 §1 已申报「有限 UGC，无发布/分享」。必须在 §4.2 否决层预留 UGC 约束位（不上传、不预审、不分享、隐私说明中明示），由 android-security 终裁（见 §9 待确认 #3）。
- **不建议同时做 G（导出）**：导出会把 UGC 带出本机，与「数据不出本机」叙事冲突（WARN）。

### 3.6 建议暂缓项与理由

| 项 | 暂缓理由 |
|---|---|
| H · PIN / 生物识别私密区 | R=3：生物识别 API、权限/声明、与 `FLAG_SECURE` 叠加后的解锁边界需单独设计；虽有 Twone 对标且价值 4，建议 Phase 3 之后独立评估。**注意：本项目已有 Keystore 加密 + 全局防截屏，缺口没有竞品对比下那么大** |
| E · 依恋/默契完整测验（`U-01` 仅纯本地问卷部分） | W=4：需 8 语言新题库 + 心理学表述准确性把关（易踩串词/机翻坑）；且 `LOVE_LANGUAGE` 轻量版已具备，边际收益有限。**`U-01` 的 AI 洞察部分需联网 → 已归 §3.2 FAIL，永久不推荐；纯本地问卷部分保留为 Phase 3 之后的候选，不在本轮范围** |
| F · 节日限定卡组 | 需新增 8 语言内容资产，必跑 `dp_lint`/`check_strings_mix`；建议作为**后续内容运营**而非本轮补齐项 |
| J · 活动级 Yes/Maybe/No | 与 `ConsentFilter`（标签级 A/B 否决 + 任一方 HARD_NO 全局阻断）、`DatePlannerEngine`（互选交集）机制重复度 > 70%；若要做，应定位为「细粒度增强」而非新功能 |
| D · 桌面/锁屏小组件 | WARN：锁屏展示「在一起天数 / 当日回忆」与 `FLAG_SECURE`、`allowBackup=false`、`零权限零网络` 的隐私叙事直接冲突；且私密内容可能暴露给旁人 |
| G · 本地备份/导出 | WARN：导出文件可离开本机；需产品明确「仅本地文件、不联网、不分享」口径后才可评估（§9 待确认 #5） |
| §3.2 全部联机/AI/云/定位 | **FAIL，永久不推荐**（铁律） |

### 3.7 本项待确认（Phase 3 前置）

- 推荐 3 项是否全做、或仅做 ①+②（工期/发布窗口）→ **用户 + team-lead 决策**。
- ① 是否占用「游戏模式编号 37」还是作为「主页会话入口」（架构上建议**后者**：不新增 GameModeCatalog 项，避免 36→37 触发 Phase10/12 硬编码数量断言批量修改，历史多次踩坑）。
- ② 年龄确认口径（17+ / 18+）与是否可跳过 → release + security。
- ③ UGC（会话日记）约束位 → security。

---

## 4. ★ 私密调性规范（需求 3、7 的唯一判定基准）

> 目标：把「**羞涩又开放、充满诱惑 / 暗示 / 吸引力又充满刺激感**」从主观形容词翻译成 **可执行、可判定、可复现** 的文案标准。ui / data 专家**照此改写**，不得另立标准。

### 4.1 硬约束（必须写进规范，位于所有评分之前）

```
[H-1] 仅成年人情侣语境：默认「你们」为成年伴侣，不得出现任何未成年人指涉或校园/亲子框架
[H-2] 不写露骨性行为描写；不写性器官、性行为动作、性器具的直白指名
      —— 一律以「气息、距离、指尖、目光、心跳、夜色、停顿、留半句」等含蓄意象替代
[H-3] 严禁涉及未成年人（任何语言、任何隐喻）
[H-4] 严禁非自愿、胁迫、酒后失去意识、暴力伤害、疼痛羞辱等情节
      —— 「随时可停 / 你说停就停 / No 无需解释」是正向基线，不是可选项
[H-5] 不得含违法内容、不得含真实赌博/药物/酒精鼓励（「微醺」仅作氛围意象，不鼓励饮酒）
[H-6] 不引入任何敏感权限；文案不得暗示联网、定位、账号、分享、上传、云同步
[H-7] 不得制造「公开场合尴尬感」：不出现当众、在他人面前、公共场所被要求互动的情境
[H-8] 不得使用功能说明书式措辞：不出现「用户 / 请点击 / 系统 / 设置 / 时长 / 分数 / 下一张」等 UI 操作语
[H-9] 报告与样例本身不得实写露骨文本 → 露骨类反例一律以 [示意占位] 呈现（见 §4.7 声明）
[H-10] ★ 强度升级唯一合法来源 = 情绪密度 / 留白 / 悬念 / 邀约语气 / 心跳感 / 双人视角 / 节奏；
       严禁靠「增加具体身体动作、身体部位、器物、强制语态、动作清单」加刺激
       （= 安安 RED-C 铁律「强度升级只能靠情绪密度、节奏与留白」）
[H-11] ★ 四档热度全部封顶 IARC「Suggestive / Mild」；T4 与 T3 的差别是情绪密度与节奏，
       不是动作升级。越界即分级申报失真，须重填 IARC 问卷（= 安安 RED-D §2.4.2 D2/D3）
[H-12] ★ 不得新增「上传 / 同步 / 云端 / 账号 / 分享到 / 定位附近」类数据流措辞；
       尤其不得沿用 sync 类动词描述本地功能。存量歧义命名 `mode_ldr_sync`
       （「异地同步 / Long-Distance Sync」）、`heat_scope_fmt`（「Synced across all 20 games」）
       实为本地状态 / 本地倒计时（= 安安 RED-P-1）
[H-13] ★ 必须与既有「安全词 / 边界 / Pause / Hard No / Private Pass」机制呼应，
       保留「说停就停」语义出口；不得写任何绕过否决、弱化边界的表述（= 安安 RED-B3/B4）
```

> **合规红线由【安安 / android-security】终裁**（`round13_phase1_security.md`）；政策层面「Play 是否允许」的最终判定由 **team-lead** 拍板（主理人裁决 Q2「法务判定，工程不等」，取最严路径）。本规范只把红线**翻译成可执行的文案检查项**，不自行做政策判定。

### 4.2 [TONE-§0] 安全合规红线 · 否决层（v1.1 已与 android-security 对齐）

```
执行顺序：TONE-§0 否决层 → §4.6 TONE-CHK 评分层 → 人工盲审层
命中否决层 = 直接拒绝，不进入调性评分，不允许「韵律好」等理由豁免。

┌─ A. 已收到并生效的硬口径（v1.1，来自 android-security）──────────┐
│ [SEC-6]  强度升级只能靠情绪密度、留白、悬念、邀约语气、心跳感、   │
│          双人视角；禁止动作 / 部位 / 器物 / 强制语态清单升级 → H-10 │
│ [SEC-7]  四档全封顶 IARC「Suggestive / Mild」；T4 vs T3 = 情绪    │
│          密度与节奏之差，非动作升级                        → H-11 │
│ [SEC-8]  呼应安全词 / 边界 / Pause / Hard No / Private Pass，      │
│          保留「说停就停」出口；禁止绕过否决的表述          → H-13 │
│ [SEC-9]  禁止新增 sync / upload / cloud / share / nearby 类数据流 │
│          措辞；存量 mode_ldr_sync、heat_scope_fmt 列入降调   → H-12 │
│ [SEC-10] 存量高风险措辞强制降调（tier_desc_3/4、paywall_benefits、│
│          mode_ladder_desc、mode_auction_desc、mode_ldr_sync）→ §4.10│
│ [SEC-11] 政策背景：Play 2026-10-29 生效条款禁止「intended to be  │
│          sexually gratifying」及非自愿性内容；现有                │
│          PLAY_CONSOLE_CONTENT_RATING.md §2.1「属于政策允许范畴」  │
│          判定可能过于乐观 → 政策判定交回 team-lead（§9 #13）      │
│ [SEC-12] 分级口径 17+ vs 18+ 两份文档不一致（主理人已裁决统一    │
│          18+，由 android-release 在 P5 落实）；targetSdk 35 未达   │
│          API 36 属上架阻断（§9 #15）                              │
└───────────────────────────────────────────────────────────────────┘

┌─ B. 待 android-security 填充并冻结（内容归安安，schema 归架架）──┐
│ [SEC-1] forbidden_terms_8lang：8 语言露骨/违规词黑名单           │
│ [SEC-2] 未成年人 / 非自愿 / 胁迫 / 酒后失意识 / 暴力伤害 /      │
│         违法内容 的 8 语言关键词与正则集                       │
│ [SEC-3] Play 政策映射：sexually suggestive content、            │
│         Families Policy、UGC 政策的具体判定口径                 │
│ [SEC-4] UGC（推荐 ③ 会话日记）约束位：不上传/不预审/           │
│         不分享/不导出/隐私说明明示                             │
│ [SEC-5] 分级问卷口径：18+（主理人裁决），「Suggestive / Mild」 │
│         不得升级为 Explicit                                    │
└───────────────────────────────────────────────────────────────────┘

接口：本否决层通过 [API-006] ToneVerdict.forbiddenHit 驱动。
      A 区为已生效硬口径，ui/data 立即可执行，无需等待；
      B 区黑名单由 android-security 维护并在 Step 1 冻结（§9 #11）。
      「SEC-11 政策判定」不进入自动扫描，只作为 team-lead 决策输入。
```

### 4.3 正面特征（7 项，可执行定义 + 判定信号）

| 编号 | 特征 | 可执行定义 | 判定信号（机器可扫 / 人工可判） |
|---|---|---|---|
| P1 | **含蓄留白** | 关键动作不写满，把最后一步交给对方想象 | `…` / `……` / 未完句 / 「那句没说完的话」/ 「剩下的，留给你猜」；至少 1 处 |
| P2 | **第二人称** | 面向「你」说话，不写第三人称说明 | 你 / 妳 / あなた / 너·당신 / tú / tu / você；禁止「用户 / 玩家 / 参与者」 |
| P3 | **邀约式而非命令式** | 疑问、商请、「要不要 / 敢不敢 / 愿意的话」 | `?` / 「要不要」「敢不敢」「…好吗」「愿意的话」/ ¿Te…? / …しない？ / …할래? / …? ；禁止全大写祈使、`MUST`、「请于 N 秒内」 |
| P4 | **感官意象** | 至少 1 个非直白的感官词 | 光 / 影 / 气息 / 温度 / 呼吸 / 心跳 / 声音 / 指尖 / 发梢 / 夜色 / 耳语；≥ 1 个 |
| P5 | **悬念与心跳感** | 制造「下一秒会怎样」的紧张 | 「还不确定」「先别揭晓」「慢一点」「屏住呼吸」「分不清是谁先…」「再近一点」 |
| P6 | **双人共同视角** | 使用「我们 / 两个人 / 只属于你们」 | 我们 / 咱们 / 两个人 / 只对你说 / us / we / 一緒に / 二人 / 우리 / nosotros·nos / on·nous / a gente·nós |
| P7 | **边界共识** | 明确「可随时停、以你为准」 | 「你说停就停」「只要你愿意」「随时可以喊停」/ only if you want / いつでもやめていい / 언제든 괜찮아 / si dices para, paramos / si tu dis stop / se você disser para |

> **评分要求**：每条文案 **P1~P7 命中 ≥ 4 项**；热度 T3 / T4 **必须命中 P7**（否则一票退回）；P2、P3 为**必选项**（缺任一即不通过）。

### 4.4 反面清单（9 类，命中即拒）

| 编号 | 反面类型 | 典型表现 | 处置 |
|---|---|---|---|
| N1 | **直白露骨** | 性器官 / 性行为 / 性器具直白指名 | 否决层 SEC-1，直接拒（样例以 `[示意占位]` 呈现） |
| N2 | **羞辱化 / 物化** | 把对方当物件、评分、比较、「听话」「乖」「奖励/惩罚你」 | 直接拒 |
| N3 | **命令式压迫** | 全大写祈使、`MUST`、「必须在 N 秒内」「不许停」「不许移开」 | 直接拒（**现有资产疑似高发，见 §4.6 抽样观察**） |
| N4 | **功能说明书式** | 「用户 / 请点击 / 点击下一张 / 系统 / 设置 / 时长 20 秒 / 完成度」 | 直接拒 |
| N5 | **公开场合尴尬感** | 当众 / 在他人面前 / 公共场合 / 让第三人看见 | 直接拒（H-7） |
| N6 | **冷冰冰罗列 / 问卷腔** | 纯信息询问、无情绪、无留白，如「请选择你偏好被亲吻的方式：A/B/C/D」 | 退回改写（补 P1/P4/P5） |
| N7 | **越界情境** | 未成年人、非自愿、胁迫、酒后失意识、暴力伤害、违法 | 否决层 SEC-2，直接拒 |
| **N8** | **★ 动作清单升级** | 以「新增具体身体动作 / 部位 / 器物 / 强制语态」来提升热度；或把动作写成步骤、教程、清单 | **直接拒**（H-10 / 安安 RED-C 铁律 / RED-D §2.4.1）。**T1→T4 的强度差只能来自情绪密度、节奏与留白** |
| **N9** | **★ 数据流歧义** | 用 `sync / synchronized / 同步 / 同期 / 동기화 / synchronisation` 描述**本地**行为；或出现肯定式「上传 / 云端 / 账号 / 分享到 / 定位附近」 | **直接拒**（H-12 / 安安 RED-P-1）。**否定式表述允许且鼓励**（「绝不上传 / 无需账号 / 外部送信ゼロ」等） |

> **N8/N9 为 v1.1 新增**（依据安安 RED-C 铁律与 RED-P-1）。二者均为**零容忍**，不因热度档位、语言、付费模式而放宽。

### 4.5 分热度梯度（四档）用语强度差异矩阵

> 与 `GameModeCatalog.heatRating`（1~5 火焰）及 `IntensityTier` 对齐；**即使 T4 也严禁 N1**，刺激感只能来自节奏、留白与心跳，不能来自露骨度。
> **v1.1 铁律（安安 RED-D §2.4.1）**：四档全部封顶 IARC「Suggestive / Mild」；**T4 与 T3 的差别是情绪密度与节奏，不是动作清单**；**禁止靠增加具体身体动作 / 部位 / 器物 / 强制语态来加刺激**（H-10 / N8）。

| 梯度 | 对应火焰 | 允许的空间/身体距离 | 允许的动词强度 | 感官意象 | 句式与节奏 | 边界共识 | 禁止 |
|---|---|---|---|---|---|---|---|
| **T1 微醺初燃** | 1 火焰 | 并肩、对视、隔着一点距离 | 看、听、靠近一点、说给我听、把灯调暗 | 光、影、声音、安静 | 短句、疑问、大量留白；句尾 `…` | 建议有 | 任何身体接触指令、任何「吻」字 |
| **T2 心跳试探** | 2 火焰 | 额头相抵、十指相扣、气息可及 | 要不要、敢不敢、试试、轻声、屏住呼吸 | 气息、温度、手腕、发梢、耳语 | 邀约疑问 + 半句留白；节奏放慢 | 必须隐含、建议明示 | 明确亲吻指令、部位直指 |
| **T3 暧昧拉扯** | 3 火焰 | 呼吸贴近、颈侧/锁骨一带的**含蓄指向** | 慢一点、先别揭晓、留半句、再近一点、忍住 | 心跳、呼吸、颤、夜色、停顿 | 长短句交替、省略号收尾、悬念最强 | **必须明示 P7** | 直白部位、命令式、N1、**N8 动作清单** |
| **T4 午夜失控** | 4~5 火焰 | 「只剩我们」的封闭夜色，**不写具体部位** | 失控感、分不清、别急着停、再近一点 | 心跳先乱、静、颤、夜、呼吸交叠 | 短促 + 断点 + 省略号；**靠节奏与情绪密度而非词汇升级** | **必须明示 P7** | **N1 绝对禁止**；**N8 动作清单绝对禁止**（不得出现露骨升级、不得出现动作步骤/教程）；不得省略「可随时停」；**分级封顶 `Suggestive / Mild`，不得上调** |

**梯度单调递增校验（AC-703）**：同一语言内，T1→T4 的**「情绪密度」「悬念密度」「节奏张力」**须单调不减；若出现「1 火焰文案比 4 火焰更刺激」，判定为错配。
> **v1.1 修正**：原 v1.0 用「身体距离 / 动词强度」作递增轴，**该轴本身即鼓励动作升级，与 H-10 冲突**，故改为「情绪密度 / 悬念密度 / 节奏张力」三轴递增。

### 4.6 可判定检查表 `TONE-CHK`（10 项 + 通过阈值）

```
对每条界面文本 / 题目，按语言独立执行：

[CHK-0] 否决层（§4.2 SEC-1/SEC-2/SEC-6~SEC-10）命中？      → 是 = REJECT（不进入评分）
[CHK-1] P2 第二人称 命中？                          → 否 = REJECT
[CHK-2] P3 邀约式（且非 N3 命令式）命中？           → 否 = REJECT
[CHK-3] N1~N9 反面清单命中数 == 0？                 → 否 = REJECT
[CHK-4] P1/P4/P5/P6/P7 中命中数 + P2 + P3 ≥ 4？    → 否 = 退回改写
[CHK-5] 所属热度梯度（§4.5）词汇越界 == 0？         → 否 = 退回改写
[CHK-6] T3/T4 是否命中 P7 边界共识？                → 否 = REJECT
[CHK-7] 地区用语基线（§4.8）违规 == 0？             → 否 = 退回改写
[CHK-8] ★ 是否存在「动作清单升级」（新增具体动作/部位/器物/
        强制语态以提升热度）？                       → 是 = REJECT（N8 / H-10）
[CHK-9] ★ 是否含 sync/同步/同期 등数据流动词，或肯定式
        上传/云端/账号/分享/定位措辞？              → 是 = REJECT（N9 / H-12）
        （否定式表述如「绝不上传 / 无需账号」允许且鼓励）

【通过阈值】
· 单条：CHK-0~3、CHK-5~9 全部通过，且 CHK-4 满足
· 批次：8 语言全量负面清单命中 = 0；正面特征 ≥ 4 项/条；
        梯度与 heatRating 匹配率 100%；人工抽检双人一致率 ≥ 90%
· 判定三层：① 机器全量（黑名单 + 正则 + lint）
            ② 人工分层抽样（每语种 ≥ 30 条，含各梯度）
            ③ 双人盲审一致率 ≥ 90%，不一致项仲裁闭环
```

**抽样观察（非结论，交 android-data 全量复核）**：本轮为建立基准抽样了 `en/intimate_modes.json` 与 `en/couple_quiz.json`，观察到两类**疑似调性偏差**：
1. `heartbeatPrompts` 中多处 `"Don't let go! Lean in and…" / "Hold the pulse! …"` 属 **N3 命令式压迫**倾向，且 `nibble / lick / bite` 措辞偏直接；
2. `couple_quiz` 的 `"What is {p1}'s favorite way to be kissed when nobody is watching?"` 属 **N6 问卷腔 / 信息询问**，缺 P1 留白与 P5 心跳感。
> 以上为 2 文件抽样，**不构成全量审计结论**；是否命中、如何改写由 android-data 依本规范全量判定，架架不越权下结论。

### 4.7 8 语言「好例子 / 坏例子」对照

> **声明（H-9）**：为满足硬约束，**N1 露骨类反例一律以 `[示意占位]` 呈现，不实写任何违规文本**；其余反例为可安全展示的「命令式 / 说明书式」文本。全部样例为**示范文案（非现有资产原文）**，供 ui/data 专家照着改写。
> **落盘提醒**：fr 所有 `'` 在 strings.xml 中必须写 `\'`（历史 aapt `Invalid unicode escape` 坑）；pt 目录为 `values-pt-rBR`。

#### en (US)

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2 心跳试探） | *Come a little closer… tell me the one thing you've been saving for tonight. We can stop whenever you say so.* | P1 留白 / P2 / P3 / P4 气息感 / P6 we / P7 明示 |
| ❌ 坏（N3+N4） | *USER MUST KISS PARTNER FOR 20 SECONDS. TAP NEXT TO CONTINUE.* | 全大写命令 + 说明书式 + P2/P3 缺失 |
| ✅ 好（T4 午夜失控） | *The night's gone quiet… and I'm still not sure which of us is trembling more. Closer — only if you want to.* | 心跳/悬念/留白/双人视角/P7，**零露骨** |
| ❌ 坏（N1 示意） | *Use your [explicit anatomy] to …* | 否决层 SEC-1，直接拒 |

#### zh-CN（简体中文）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | 再靠近一点点…把那句攒了一整天的话，只说给我听。你说停，我们就停。 | P1 / P2 你 / P3 / P5 攒了一整天 / P6 我们 / P7 |
| ❌ 坏（N3+N4） | 请用户在 20 秒内亲吻伴侣，完成后点击「下一张」。 | 命令 + 说明书式 + 「用户」 |
| ✅ 好（T4） | 夜已经很静了…分不清是谁的心跳先乱的。再近一点——只要你愿意。 | 含蓄、心跳、悬念、P7，零露骨 |
| ❌ 坏（N1 示意） | 用［露骨器官词］…… | 否决层，直接拒 |

#### zh-TW（繁体中文）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | 再靠近一點點…把那句藏了一整天的話，只說給我聽。你說停，我們就停。 | 繁体用字正确（點/話/說/聽/停）；P1/P2/P6/P7 |
| ❌ 坏（N3+N4） | 請使用者於 20 秒內親吻伴侶，結束後點擊「下一張」。 | 命令 + 说明书式；「使用者」非第二人称 |
| ✅ 好（T4） | 夜已經很靜了…分不清是誰的心跳先亂了。再近一點——只要你願意。 | 靜/誰/亂/願意 均繁体；零简体泄漏 |
| ❌ 坏（N1 示意） | 用［露骨器官詞］…… | 否决层，直接拒 |

> ⚠️ zh-TW 专项：严禁简体专用字混入（历史第二轮 5,978 处异形字转换、第十二轮 `SIMPLIFIED_ONLY` 规则）；避免大陆用语（「对象 / 靠谱 / 视频」→「對象 / 可靠 / 影片」）。

#### ja（日本語）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | もう少しだけ、こっちへ…今日ずっと胸にしまっていた一言を、わたしだけにそっと。いつでも「やめよう」って言っていいからね。 | P1「…」/ P3 柔性邀约 / P4 胸・そっと / P7 明示 |
| ❌ 坏（N3+N4） | 20秒以内にパートナーにキスをし、終了後「次へ」をタップしてください。 | 命令 + 说明书式 + 操作语 |
| ✅ 好（T4） | 夜がすっかり静かになったね…どっちの鼓動が先に乱れたのか、もうわからない。あと少しだけ——あなたが望むなら。 | 鼓動/静/留白/悬念/P7；零露骨 |
| ❌ 坏（N1 示意） | ［露骨な器官語］を使って… | 否决层，直接拒 |

> ⚠️ ja 专项：用「そっと / ちょっと / …ね / 胸が高鳴る」营造含蓄，避免把中文「诱惑」直译成过硬的「誘惑」；**严禁简体专用字串混入**（历史 `check_strings_mix.py`：连续 3 个简体专用字报警）。

#### ko（한국어）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | 조금만 더 가까이… 오늘 하루 내내 품어둔 그 한마디, 나한테만 살짝. 언제든 「그만」이라고 해도 괜찮아. | P1 / P3 / P4 살짝 / P7 明示；全谚文无汉字 |
| ❌ 坏（N3+N4） | 20초 이내에 파트너에게 키스하고 완료 후 '다음'을 누르세요. | 命令 + 说明书式 |
| ✅ 好（T4） | 밤이 완전히 고요해졌네… 둘 중 누구의 심장이 먼저 흐트러졌는지 이제 모르겠어. 조금만 더—네가 원한다면. | 심장/고요/悬念/P7；零汉字零露骨 |
| ❌ 坏（N1 示意） | ［노골적인 신체 어휘］로 … | 否决层，直接拒 |

> ⚠️ ko 专项：해요체 柔性敬语；用「설렘 / 두근거림」表达心跳；**出现任何汉字/假名即判串词**（历史第十二轮已修 2 处既有韩语串词）。

#### es（Español，中性拉美/西班牙）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | ¿Te acercas un poco más? …Esa frase que llevas guardada todo el día, dímela solo a mí. Si dices «para», paramos. | P1 / P2 tú / P3 疑问邀约 / P6 paramos / P7 |
| ❌ 坏（N3+N4） | BESA A TU PAREJA DURANTE 20 SEGUNDOS. PULSA «SIGUIENTE» PARA CONTINUAR. | 全大写命令 + 说明书式 |
| ✅ 好（T4） | La noche se ha quedado en silencio… y ya no sé cuál de los dos tiembla más. Un poco más cerca — solo si tú quieres. | 心跳/留白/双人/P7；零露骨 |
| ❌ 坏（N1 示意） | Usa tu [término anatómico explícito] para… | 否决层，直接拒 |

> ⚠️ es 专项：用 **tú**（非 usted）；避免地区歧义俚语（尤其 «coger» 在部分拉美地区有性含义，**禁用**）；用 «acercarse / susurrar / temblar / frisson» 类柔性词。

#### fr（Français）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | Tu t'approches encore un peu ? …Cette phrase que tu gardes depuis ce matin, dis-la-moi à moi seule. Si tu dis « stop », on s'arrête. | P1 / P2 tu / P3 / P6 on / P7 |
| ❌ 坏（N3+N4） | EMBRASSEZ VOTRE PARTENAIRE PENDANT 20 SECONDES. APPUYEZ SUR « SUIVANT ». | 命令 + vous（且非亲密）+ 说明书式 |
| ✅ 好（T4） | La nuit s'est tue… je ne sais plus lequel de nous deux tremble le plus. Encore un peu — seulement si tu le veux. | 留白/心跳/双人/P7；零露骨 |
| ❌ 坏（N1 示意） | Utilise ton [terme anatomique explicite] pour… | 否决层，直接拒 |

> ⚠️ fr 专项：**tutoiement（tu）** 契合亲密，禁用 vous；避免直白 «sexy»，用 «frisson / trouble / trembler»；**落盘 strings.xml 时所有 `'` 必须转义为 `\'`**（历史第一轮构建阻断、第二轮「切勿全局替换」教训）。

#### pt-BR（Português do Brasil）

| | 文案 | 判定 |
|---|---|---|
| ✅ 好（T2） | Você chega só mais um pouquinho? …Aquela frase que ficou guardada o dia inteiro, diz só pra mim. Se você disser «para», a gente para. | P1 / P2 você / P3 / P6 a gente / P7 |
| ❌ 坏（N3+N4） | BEIJE SEU PARCEIRO POR 20 SEGUNDOS. TOQUE EM «PRÓXIMO» PARA CONTINUAR. | 全大写命令 + 说明书式 |
| ✅ 好（T4） | A noite ficou inteira em silêncio… e eu já não sei qual dos dois treme mais. Só mais um pouco — se você quiser. | 心跳/留白/双人/P7；零露骨 |
| ❌ 坏（N1 示意） | Use seu [termo anatômico explícito] para… | 否决层，直接拒 |

> ⚠️ pt 专项：必须 **pt-BR 变体**（você / a gente / pra / só mais um pouquinho），禁用 pt-PT 用词（«telemóvel» / «rapariga» 等）；资源目录为 **`values-pt-rBR`**（历史第十一轮踩坑）。

### 4.8 分地区用语与本地化提醒（服务需求 6）

| 语言 | 称谓 / 敬语 | 推荐调性词 | 禁用 / 高风险 | 工程注意 |
|---|---|---|---|---|
| en-US | 直接 you / we | close, whisper, tremble, quiet, breath | 过度英式俚语（snog 等）；全大写；`naughty` 需克制 | 基准语言，其他 7 语种以其语义为准而非逐字直译 |
| zh-CN | 你 / 我们 | 脸红、耳根热、心跳、气息、悄悄、只对你说 | 「用户 / 请 / 系统」；低俗网络梗；直白器官词 | 无繁体字混入；占位符不得写进 `android:text` |
| zh-TW | 你 / 妳（视语境）/ 我們 | 心跳、耳根熱、悄悄、只對你說 | 简体专用字；大陆用语；「視頻 / 對象 / 靠譜」 | 跑 `SIMPLIFIED_ONLY` 规则；「裡 / 著」用字一致 |
| ja | あなた / そっと / …ね | 胸が高鳴る、鼓動、息、そっと、静か | 「誘惑」过硬直译；简体专用字串；命令形「〜しろ」 | `check_strings_mix.py` 连续 3 简体专用字规则 |
| ko | 해요체（…요）/ 네가 | 설렘、두근거림、고요、살짝 | 任何汉字 / 假名 / 汉字词直译；半말命令形 | 已有 2 处串词修复史，新增必扫 |
| es | tú（非 usted） | acercarse, susurrar, temblar, guardar | `coger`（地区性含义）；区域俚语；usted | 中性西语，避免 es-ES / es-MX 分化 |
| fr | tu（tutoiement，非 vous） | frisson, trouble, trembler, silence | `sexy` 直白；vous；全大写祈使 | **`'` → `\'` 转义**（aapt）；勿全局替换 |
| pt-BR | você / a gente | pra, só mais um pouquinho, trem(er), silêncio | pt-PT 用词；`vocês` 误用；「sair daqui」类歧义 | 目录 `values-pt-rBR`，非 `values-pt` |

### 4.9 判定流程与交付格式

```
[Step 1] android-security 填充 §4.2 黑名单 → 冻结版本（签名 + 日期）
[Step 2] android-ui / android-data 各自导出待审文本
         格式：{ lang, surface("ui"|"content"), file, key_or_id, heatTier, text }
[Step 3] 机器全量跑 TONE-CHK（含既有 4 个 lint 脚本）→ 产出 rejects.csv / rewrites.csv
[Step 4] 改写 → 复跑，直到负面清单命中 = 0
[Step 5] 人工抽检（每语种 ≥ 30 条，覆盖四梯度）+ 双人盲审一致率 ≥ 90%
[Step 6] 交付 ToneVerdict 汇总（见 [API-006]），由 team-lead 验收
```

### 4.10 存量高风险措辞 · 强制降调清单（v1.1 新增，承接主理人裁决 Q4）

> **归属【android-ui】**（`res/values*/strings.xml` 独占）。架架**只给降调约束，不写最终文案**。
> **判定口径**：安安 `RED-A2`（动作分解）/ `RED-A3`（胁迫惩罚）/ `RED-A7`（意图满足性欲）/ `RED-C`（唯一允许手法）/ `RED-D §2.4.3`（存量清单）/ `RED-P-1`（sync 歧义）。
> **原则（主理人裁决 Q2「最严路径」）**：一律**降调**，绝不「加码」；降调方向 = **感官 / 悬念 / 情绪密度 / 双人 / 可停**。

| 组 | key（`values/strings.xml`） | 必须删除 | 降调方向 | 边界约束 |
|---|---|---|---|---|
| 1 | `tier_desc_3`（:247）、`tier_desc_4`（:248） | `licking / nibbling / 轻舔 / 轻咬 / 跨坐 / 深吻 / lap tease`；`punishments / spanks / pinning hands / 惩罚 / 按手 / 拍打 / 褪衣` | 改为**感官 + 悬念 + 距离感**；T4 改为「更近的距离、更慢的节奏、说停就停」类表述 | 只改显示文案；**不改 tier 值、不改 `GameModeCatalog`** |
| 2 | `paywall_benefits`（:198，**商店可见**） | `Kama Positions / 5-Stage Foreplay / 体位 / 前戯 / foreplay / 体位ルーレット` | 改为**中性游戏化命名**（安安建议「默契轮盘 / 十层心动阶梯」类） | 商店成熟度不得高于应用内实际内容（`RED-D4`） |
| 3 | `mode_ladder_desc`（:399） | `foreplay / nonstop physical / performs` | 改为「**十步递进的默契挑战**」类 | 不得暗示单向施动-受动结构（`RED-B2`） |
| 4 | `mode_auction_desc`（:323） | `dominance / control` | 保留竞拍玩法，去支配语义，**补「双方自愿、随时可停」** | `RED-B3` |
| 5 | `mode_ldr_sync`（:467）、`mode_ldr_sync_desc`（:468） | `Sync / 同步 / 异地 / from anywhere` | 改为本地化表述（安安 3.4 举例：「同一时刻 / A Shared Countdown / 함께 카운트다운」） | **只改显示文案**；**不改 mode id / 枚举 / `TagDisplayMapper` 键**，避免 36 模式清单与数量断言回归 |

> **另附架架实测发现（待 team-lead 确认是否并入本轮，§9 #2）**：`heat_scope_fmt`（:244，8 语言）含 **① 数字过时**（写「20 games / 20 款游戏」，**实测 36 模式 / VIP 12 / 免费 24**）、**② `Synced across` / 「自动同步到」的数据流歧义**（`RED-P-1` 明列该 key）；`paywall_benefits` 同组另含 **「9 Extreme VIP」**（**实测 12**）与「all 20 games」。二者均属我方 AC-302 事实准确性缺陷 + 安安 RED-P-1，建议与上表同批处理。

### 4.11 与既有安全机制的呼应关系（v1.11 新增，防止改写与既有机制矛盾）

| 既有机制 | 实现位置 | 改写时的约束 |
|---|---|---|
| 安全词 Safe Word | `OnboardingDialogFragment` + `values/strings.xml:14-17` | 文案**不得**弱化或绕过（`RED-B4`） |
| 边界与舒适度 | `ConsentRepository` 11 条规则 + `ConsentFilter`（任一方 HARD_NO 全局阻断） | **不得**用文案暗示某标签「其实没关系」 |
| Pause / 安全暂停条 | 游戏内 `PassPhoneDialog` 周边 Pause/Safe Word 栏 | **不得移除**；T3/T4 文案必须呼应 |
| Private Pass（私密否决） | 隐藏个人否决的静默尊重叙事 | 保持「静默尊重」，不暴露是谁否决 |
| `heat_scope_fmt` | 首页 `tv_heat_explanation` | 见 §4.10 附注 |

---

## 5. ADR（本轮关键决策）

```
[ADR-R13-001]
标题：Phase 1 只产出基准与清单，不修改任何工程文件
状态：接受
背景：本轮 7 项需求中，第 1 项落地范围尚未拍板；ui/data 专家持有 strings.xml 与
      内容 JSON 的写权限；并发写入存在冲突与回滚风险。
决策：架架本轮唯一写入 = audit_report/round13_phase1_architect.md（新建）。
      不碰代码 / 布局 / strings.xml / 内容 JSON；补充内容范围留到 Phase 3。
备选方案：① 本轮直接实现 2~3 个补充项 ② 先写基准再由下游复用
理由：需求必须先澄清再设计（本职责核心约束）；第 1 项属产品范围决策，不得由架构专家
      代替用户拍板；历史上多轮并行改动曾引发 strings key 与布局引用不一致的编译失败。
影响：下游 ui/data 可立即并行开工（判定基准已就绪）；Phase 3 需架架续接拍板范围。
```

```
[ADR-R13-002]
标题：竞品补充项以「零权限零网络」为一票否决硬门槛，采用四维打分排序
状态：接受
背景：同类 App 主流功能大量依赖账号 / 云 / 定位 / AI（Paired、SyncPlay、Date Night、
      Couple Joy），而本项目「零权限零网络零广告、数据不出本机」是 Play 差异化卖点。
决策：先过 C-1~C-7 硬门槛（FAIL 直接剔除），再按 S = V×2 − R − W + 合规奖励 排序；
      WARN 项降权并列入暂缓。
备选方案：① 只看用户价值排序 ② 照搬竞品功能清单 ③ 全部自研新增模式
理由：①②会把联网功能带进来，直接摧毁核心卖点并触发 Play Data Safety 重新申报；
      ③ 在第 36 个模式之后边际价值低、8 语言翻译风险高（历史 ID_MISSING=20）。
影响：§3.2 的联机/AI/云/定位类功能永久不推荐；上架叙事保持自洽。
```

```
[ADR-R13-003]
标题：先建立「已具备能力排除集」，再定义缺口
状态：接受
背景：源码实证发现 FavoriteTaskDao / CustomTaskDao / ConsentFilter(YES/SOFT_LIMIT/HARD_NO
      任一方否决全局阻断) / Safe Word / DatePlannerEngine 互选 / DailyReveal / LOVE_GRAPH
      已覆盖同类 App 八大常见卖点；若仅凭功能名对照会重复推荐已实现内容。
决策：把「收藏、自定义题、双人边界、每日揭晓、里程碑、约会规划、关系可视化、
      离线无账号」全部排除出缺口；缺口聚焦「串场层 / 合规入口层 / 回顾层 / 设备锁层」。
备选方案：按竞品功能名逐条列为缺口
理由：避免 Phase 3 拍板到已做功能，浪费工期并引入回归风险。
影响：主推项从「再加一个游戏」转为「把 36 个模式串成一个夜晚」+「补上架合规入口」。
```

```
[ADR-R13-004]
标题：私密调性采用「可判定检查表 + 四档热度梯度 + 8 语言对照」，不使用主观形容词
状态：接受
背景：用户第 3、7 项要求是「羞涩又开放、充满诱惑/暗示/吸引力又充满刺激感」，
      属主观描述，不同专家各自理解会导致改写结果不可验收。
决策：定义 P1~P7 正面特征、N1~N7 反面清单、T1~T4 梯度矩阵、TONE-CHK 8 项检查与
      通过阈值（负面 0 命中、正面 ≥ 4 项、梯度匹配 100%、盲审一致率 ≥ 90%），
      并给 8 语言各 2 组好/坏例子。
备选方案：① 给一段风格描述让专家自行把握 ② 只给关键词黑名单
理由：① 不可判定、无法验收；② 只能防「过界」，无法保证「有吸引力」。
影响：ui / data 共用唯一基准；AC-702~AC-706 可机器复现。
```

```
[ADR-R13-005]
标题：合规硬约束内嵌为调性规范的「否决层」，安全红线由 android-security 终裁
状态：接受（约束位预留）
背景：调性「更刺激」的诉求与 Play 敏感内容政策、未成年人保护存在天然张力；
      架架不负责安全合规终裁。
决策：§4.1 写死 H-1~H-9；§4.2 设 [TONE-§0] 否决层，执行顺序先于任何调性评分，
      命中即拒不允许豁免；黑名单内容留空由 android-security 填充（SEC-1~SEC-5）。
备选方案：把合规检查放在改写完成后统一扫
理由：事后扫描会导致大量返工，且「韵律好」可能成为越界文本的豁免借口。
影响：AC-704 为硬门禁；[API-006] ToneVerdict.forbiddenHit 由安安的黑名单驱动。
```

```
[ADR-R13-006]
标题：本轮推荐落地 ① 引导式约会夜串场 ② 18+ 年龄确认 ③ 今晚一句话会话日记
状态：提议（待 Phase 3 拍板 + 用户确认）
背景：§3.4 打分结果 A(7) > B(6) > C(5)，三项均 C=PASS 且 R ≤ 2；
      H/E/F/D/G 因风险、工作量或 WARN 暂缓；联机类 FAIL 剔除。
决策：推荐 ① 为主推（复用既有内容池，不新增题库 JSON）、② 为合规必补（近零成本）、
      ③ 为次推（UGC 自填，无翻译资产）；建议 ① 以「主页会话入口」实现，
      不新增 GameModeCatalog 第 37 项。
备选方案：① 新增第 37 个独立游戏模式 ② 只做 ②+③ ③ 一次性落地全部候选
理由：新增模式会触发 Phase10/12 硬编码 36/free24 数量断言批量修改（历史多次踩坑）
      且需 8 语言新资产；「主页会话入口」零数量断言影响、零新增 JSON。
影响：若拍板，仅新增 ~1 个纯 Java 引擎 + 1~2 个 Fragment + 8 语言约 20~30 条 strings；
      manifest 权限集合不变。
```

```
[ADR-R13-007]
标题：验收采用「机器全量 + 人工分层抽样 + 双人盲审一致率」三层判定
状态：接受
背景：8 语言 × 652 条界面文本 + 13 类 × 8 语言内容资产，纯人工不可行，
      纯机器又无法判定「含蓄留白 / 暧昧拉扯」等语义。
决策：机器跑黑名单/正则/4 个既有 lint 做 100% 覆盖；人工每语种抽检 ≥ 30 条覆盖四梯度；
      双人独立盲审一致率 ≥ 90%，不一致项仲裁闭环。
备选方案：① 全量人工 ② 全量机器 + AI 自评
理由：① 工期不可控；② 无一致率约束，无法证明「符合地区用户习惯」（需求 6）。
影响：AC-604 / AC-702 / AC-705 具备可复现的量化证据链，可供 Play 申诉留档。
```

```
[ADR-R13-008]
标题：新增能力优先复用既有引擎与内容池，新增多语言内容必须过既有 lint 链
状态：接受
背景：历史十二轮沉淀的关键架构资产 —— 第六轮「统一内容池 + 配置驱动引擎」、
      verify_content.py（8 类检查）、dp_lint.py / sl_lint.py / check_strings_mix.py、
      fr 撇号转义、pt-rBR 目录、R8 keep 规则、ConsentFilter 全局否决。
决策：任何补充项不得新建 8 语言题库 JSON（除非 Phase 3 明确批准）；
      新增 strings 必须幂等脚本生成 + 8 语言 key 对齐 + 四脚本 EXIT=0。
备选方案：每个新功能配独立内容资产
理由：历史上每新增一批多语言内容都出现串词（ja 混俄文、ko 混汉字、zh-CN 混英文
      「小moment」），修复成本高于复用。
影响：AC-505 / AC-G03 / AC-G05 成为所有落地的强制门禁。
```

```
[ADR-R13-009]
标题：调性「更刺激」只能由情绪密度实现，禁止动作清单升级；四档全部封顶 Suggestive/Mild
状态：接受（对齐安安 RED-C / RED-D，主理人裁决 Q2「最严路径」）
背景：用户第 7 项要求「充满诱惑、暗示、吸引力又充满刺激感」，最容易被误读为
      「T4 要更露骨 / 动作更具体」。安安红线明确：强度升级只能靠情绪密度、节奏与留白；
      四档全部封顶 IARC「Suggestive / Mild」，T4 与 T3 的差别是情绪密度而非动作升级。
      且 Play 2026-10-29 生效条款禁止「intended to be sexually gratifying」及非自愿性内容。
决策：v1.1 将 H-10~H-13 写入 §4.1 硬约束；反面清单扩为 N1~N9（新增 N8 动作清单升级、
      N9 数据流歧义）；TONE-CHK 扩为 10 项（新增 CHK-8/9）；§4.5 梯度递增轴由
      「身体距离 / 动词强度」改为「情绪密度 / 悬念密度 / 节奏张力」；
      §4.10 列出 5 组存量强制降调 + 1 组实测发现。
备选方案：① 保留原递增轴并加口头提醒 ② 把红线只留给安安、架架规范不体现
理由：①原递增轴本身即在鼓励动作升级，与红线语义冲突（v1.0 自查发现）；
      ②改写专家（ui/data）需要一份**可直接执行**的判定依据，规范不体现红线则必然漏判。
影响：AC-702~AC-706 判据升级；8 语言改写范围扩大；存量 strings 必须降调；
      IARC 问卷重填由 android-release 在 P5 执行，改写专家不得自行判断。
```

```
[ADR-R13-010]
标题：第 1 项补充内容以 A/B/C 三项落地，A 采用「主页会话入口」且不新增 GameModeCatalog 项
状态：接受（主理人裁决，Phase 3 已出实现规格）
背景：主理人裁决 A/B/C 三项全部落地；A 必须采用「主页会话入口」形态以保持 36 模式不变，
      不触发 Phase10/12 硬编码数量断言（历史 ADR-014）；Q6 Keystore 明文降级路径不由架架出方案。
决策：A → 新增 core/session/DateNightSessionEngine（纯 Java Phase 机 WARM_UP→DEEPEN→PEAK→CLOSING）
      + ui/session/DateNightFragment + 主页卡片 + 1 个 nav action；统计键 "DATE_NIGHT"
      独立于 GameModeCatalog，并在 TagDisplayMapper 新增 1 条映射（防历史第八轮 fallback 坑）。
      B → core/profile/AgeGateManager（SP 布尔，首启门，软门禁建议）。
      C → core/journal/TonightNote + TonightNoteManager（SP 分键，非 Gson，规避 R8 字段名坑）。
      三项**均不新增权限、不新增题库 JSON、不新增 Gradle 依赖、不做 Room migration**。
      规格见 audit_report/round13_phase3_architect_spec.md。
备选方案：① 新增第 37 个 GameModeCatalog 模式 ② A 改用 Room 表存储 ③ C 允许导出
理由：①会触发数量断言批量修改（历史多次踩坑）；②SP 分键已足够且无 migration 成本；
      ③导出违反「数据不出本机」与 RED-P，导出/分享能力还会触发 IARC 重填。
影响：GameModeCatalog 保持 36（VIP 12 / 免费 24）；新增 strings 共 32 条 × 8 语言；
      接口契约新增 [API-008]~[API-010]；Phase10/12 断言不动。
```

---

## 6. 模块边界图（文字版，贴合现有 Java 工程实际）

### 6.1 现有分层与依赖方向

```
app  (单 Activity: MainActivity + Navigation Component + ViewBinding)
│
├── ui/                          表现层：Fragment + 自定义 View + Adapter
│   ├── home/                    HomeFragment / GameModeAdapter / ThemeSwatchAdapter
│   ├── settings/ onboarding/ consent/ deck/ paywall/ placeholder/
│   ├── common/                  PassPhoneDialog(递手机) / HapticHelper / PhysicsAnimator
│   │                           / TaskPromptDialogHelper / DareTimerHelper(衔接)
│   ├── <20 个原始游戏 Fragment>  chess / quiz / scratch / auction / ladder / foreplay
│   │                           / positions / props / connect4 / blackjack / bomb
│   │                           / heartbeat / fatecubes / party_duel / blindmatch
│   │                           / bodymap / roleplay / wheel / minesweeper / drawguess
│   ├── social/                  SocialGameFragment（单 Fragment 承载 12 模式，靠 argument 区分）
│   ├── dailyreveal/ dateplanner/ letter/ lovegraph/
│   └── (推荐 ① 落点) session/   ← 新增，不新增 GameModeCatalog 项
│
├── core/                        领域层：优先纯 Java、无 Android 依赖、可单测
│   ├── engine/                  TaskEngine / SocialGamesEngine / ConsentFilter(全局否决)
│   │                           / ModePromptSynthesizer / ProceduralTaskGenerator
│   │                           / PromptHistoryStore / DareTimerHelper
│   ├── model/                   GameModeCatalog(36) / TaskCard / IntensityTier
│   │                           / BoundaryStatus / DeckMeta / PlayerProfile / BoardTheme
│   ├── i18n/                    LocaleManager(→8 语言资产目录) / AssetContentLoader
│   │                           / TagDisplayMapper(记忆档案标签映射)
│   ├── profile/                 CoupleProfileManager / MoodCheckinManager
│   │                           / TogetherDateFormat(纯 Java, UTC 锚定)
│   ├── dateplanner/             DatePlannerEngine / DatePlanManager
│   ├── letter/                  SealedLetter / LetterCrypto(Keystore AES-256-GCM)
│   │                           / SealedLetterManager
│   ├── lovegraph/               LoveGraphEngine(纯 Java, bondScore)
│   ├── streak/                  StreakCalculator / BadgeCatalog
│   ├── theme/                   ThemeManager / AppTheme(12 套)
│   ├── billing/                 FeatureGate / EntitlementManager / PlayBillingBridge
│   │                           / PricingCatalog
│   └── (推荐 ① 落点) session/   DateNightSessionEngine（纯 Java 编排机）
│
├── data/                        持久化层：Room + SharedPreferences (+ Keystore)
│   ├── local/dao/               BlindMatchDao / BoundaryDao / CustomTaskDao
│   │                           / FavoriteTaskDao / SessionStatDao
│   ├── local/entity/            对应 6 个 Entity
│   ├── local/                   AppDatabase / Converters
│   └── repository/              ContentRepository(内容池/收藏/自定义/统计)
│                                / ConsentRepository(11 条边界规则 → ConsentFilter)
│
├── res/                         8 语言 values*（各 652 条）+ 49 布局 + 12 主题
└── assets/content/{en,zh,zh_tw,ja,ko,es,fr,pt}/
                                 13 类只读内容资产（decks 874 卡为核心池）
```

**依赖方向（单向，禁止反向）**：

```
ui ──► core ──► data/repository ──► data/local(Room/SP/Keystore) ──► assets/content(只读)
 │                │
 │                └── core/* 之间：engine → model；禁止 model → engine
 └── ui/common 可被所有 ui 复用；禁止 ui/* 之间互相直接依赖具体 Fragment

禁止：assets/content → 任何代码；core → ui；data → ui；
      任何模块 → 网络层（工程中不存在，且不得创建）；
      任何模块 → 第三方 SDK（除既有 Play Billing）。
注入：AppContainer（Application 级）统一构造并注入 repository / manager，
      Fragment 通过 ((CouplesGameApp) requireActivity().getApplication()).getContainer() 取用。
```

### 6.2 本轮推荐补充项的落点（**若 Phase 3 拍板**，本轮不实现）

| 推荐项 | 新增 | 复用 | 不触碰 |
|---|---|---|---|
| ① 约会夜串场 | `core/session/DateNightSessionEngine`（纯 Java Phase 机）+ `ui/session/DateNightFragment` + 1 个 nav action(带 `session_mood` 参数) + 主页入口卡片 + 8 语言 ~12~18 条 strings | `SocialGamesEngine`（recent shield）、`ContentRepository.loadMergedPoolAsync`、`ConsentFilter`、`DareTimerHelper`、`HapticHelper.countdownFinished()`、`recordSessionStat` | **不新增 GameModeCatalog 项**、不新增内容 JSON、不改数量断言 |
| ② 18+ 年龄确认 | `ui/onboarding/` 内 1 个 Dialog + `core/profile/` 本地布尔标记 + 8 语言 2~3 条 strings | 现有 OnboardingDialogFragment 首启链路 | 不新增权限、不新增网络 |
| ③ 今晚一句话日记 | `core/journal/` 纯 Java 模型 + 1 个 Fragment + 8 语言 ~8~10 条 strings | `MoodCheckinManager`(SP+LiveData 范式)、`TogetherDateFormat`(日期)、`session_stats` | 不新增内容 JSON、不导出/不分享 |

> 三项共同约束：`AndroidManifest.xml` 零改动（权限集合仍 `{VIBRATE}`）；`TagDisplayMapper` 若新增统计键必须同步注册（历史第八轮 fallback「情侣棋局」坑）；`proguard-rules.pro` 若新增 Gson 落盘模型必须加 `-keep`（历史第十一轮 R8 坑）。

---

## 7. 接口契约要点

```
[API-001]
模块：ui/* → core/engine/SocialGamesEngine
方法：drawNext(modeId, tier, preferredTags): TaskCard
契约：interaction ∈ {TASK, TIMED}（第七轮已移除 CHOICE，禁止恢复）；
      recent shield = 12、MIN_AVAILABLE = 4，保证任何池不出现立即重复、不空池
错误：EmptyPoolError → 必须回退到「至少 4 张可用卡」兜底，禁止返回 null
线程：主线程调用，内部同步；统计走 recordSessionStat(modeId.toUpperCase())
```

```
[API-002]
模块：ui/* → data/repository/ContentRepository
方法：loadMergedPoolAsync(callback) / observeFavorites(): LiveData<List<FavoriteTaskEntity>>
      / observeCustomTasks() / addCustomTask(prompt, tier, TaskType) / removeFavorite(id)
      / recordSessionStat(modeKey, a, b, c)
错误：AssetLoadError（8 语言目录缺失 / JSON 非法）→ 回退 en 并上报，禁止崩溃
线程：ioExecutor（沿用现有范式）；LiveData 回主线程 observe
```

```
[API-003]
模块：data/repository/ConsentRepository → core/engine/ConsentFilter
方法：replaceRules(List<BoundaryRuleEntity>) / isAllowed(Collection<String> tags): boolean
      / hasSoftLimit(tags): boolean
契约：任一方 HARD_NO 即全局阻断（取双方最低共识，等同 Twone「never beyond the lowest」）；
      SOFT_LIMIT 仅标记不阻断；11 条默认规则不得擅自删减
错误：规则为空 → 视为「全部允许」但必须记录告警（不得默认全阻断导致空池）
线程：synchronized + CopyOnWriteArraySet；写库走 ioExecutor
```

```
[API-004]
模块：任何新增能力 → AndroidManifest / 权限 / 网络
契约：权限集合恒为 {android.permission.VIBRATE}；
      不得出现 java.net.* / okhttp / retrofit / 地图 SDK / 位置 API / 账号 API 的 import；
      所有新数据仅落 SharedPreferences / Room / Android Keystore；
      allowBackup=false、FLAG_SECURE 保持开启
错误：违反任一 → 构建门禁拒绝（由 android-build 校验）
线程：不适用
```

```
[API-005]
模块：core/i18n/LocaleManager → assets/content/{lang} + res/values*
方法：resolveAssetLangDir(): String ∈ {en, zh, zh_tw, ja, ko, es, fr, pt}
契约：13 类 JSON × 8 语言 ID 集合完全一致（ID_MISSING=0）；
      8 语言 strings.xml key 集合一致（各 652 条 + 新增项同批对齐）；
      fr `'` → `\'`；pt 目录 values-pt-rBR；新增内容必跑 4 个 lint
错误：I18nMismatchError → verify_content.py EXIT≠0，CI 阻断
线程：IO（AssetContentLoader）
```

```
[API-006]
模块：架架(基准) → android-security(黑名单) → android-ui / android-data(执行)
方法：evaluateTone(ToneSample): ToneVerdict
入参：ToneSample { lang, surface: "ui"|"content", file, key_or_id, heatTier: 1..4, text }
出参：ToneVerdict {
        passed: boolean,
        forbiddenHit: List<String>,      // 由 SEC-1/SEC-2 驱动，命中即 passed=false
        negativeHits: List<N1..N7>,      // 必须为 0
        positiveHits: List<P1..P7>,      // ≥ 4，且必含 P2、P3
        heatMatched: boolean,            // 与 §4.5 梯度及 heatRating 一致
        requiresBoundaryConsent: boolean,// T3/T4 必须命中 P7
        reviewerLang: String
      }
契约：执行顺序固定 §4.2 否决层 → §4.6 TONE-CHK → 人工盲审；
      黑名单内容由 android-security 维护，架架只定义 schema
错误：黑名单未冻结 → 拒绝出批次结论
线程：离线批处理（脚本 / CI），无网络
```

```
[API-007]
模块：各专家 → audit_report/ → team-lead
契约：交付物命名 round13_phase{N}_{role}.md；
      每份报告必须引用本报告 AC 编号与 TONE-CHK / P / N / T 编号；
      验收结论必须给「数量 / 覆盖率 / 一致率 / EXIT」，不接受主观表述
错误：缺 AC 引用 → 视为未交付
线程：不适用
```

```
[API-008]   （v1.1 新增 · A 项引导式约会夜串场）
模块：ui/session/DateNightFragment → core/session/DateNightSessionEngine
方法：startSession() / drawNext(): TaskCard / currentStage(): DateNightStage
      / stageProgress(): int / markDone() / finishSession(): SessionResult
契约：纯 Java、无 Android 依赖、构造注入 Random（确定性单测）；
      抽卡须依次满足 tier ≤ min(stage.maxTier, playerMaxTier)
      AND ConsentFilter.isAllowed(tags) AND 命中 stage.preferredTags
      AND 不在会话级 recentIds 的 shield 窗口（shieldSize = min(12, max(0, poolSize-4))，
      与 SocialGamesEngine 同值同算法）；PEAK 阶段不设倒计时；
      **绝不通过放宽 HARD_NO 补足卡池**；统计键 "DATE_NIGHT"（非 GameModeCatalog 项）
错误：池空 → 逐级降档且每档保留 ≥4 张可用卡；全局仍空 → 返回 null 并由 UI 降级提示，
      **不得抛异常**；ConsentFilter 全 HARD_NO 时会话正常结束并以 (0,0,0) 记账
线程：纯计算，主线程调用，不触碰 IO
```

```
[API-009]   （v1.1 新增 · B 项 18+ 年龄确认）
模块：ui/onboarding（首启门） → core/profile/AgeGateManager
方法：isConfirmed(): boolean / confirm(): void / decline(): void / isDeclined(): boolean
契约：首启时若 !isConfirmed()，**必须先于任何成人向内容呈现**完成年龄确认；
      结果存 SharedPreferences 独立 pref 文件的单个 boolean，**写盘先于关窗**；
      口径 18+（主理人裁决 Q1）；架架建议软门禁（decline → 仅保留 heatRating ≤ 2 的温和模式），
      软/硬门禁属产品策略，交 team-lead 确认
错误：SP 读写失败 → 视为「未确认」（fail-closed），**不得默认放行**
线程：主线程读写；无网络、无 IO
```

```
[API-010]   （v1.1 新增 · C 项今晚一句话会话日记）
模块：ui/journal（编辑/回看） → core/journal/TonightNoteManager
方法：saveNote(dateIso, noteA, noteB, moodScore): boolean / observeNotes(): LiveData<List<TonightNote>>
      / getNote(dateIso): TonightNote / getTodayIsoDate(): String
契约：dateIso 由 TogetherDateFormat.toIso() 产出（yyyy-MM-dd）；
      同日重复保存**覆盖**当日记录，不追加；noteA/noteB 允许空串；moodScore ∈ [1,5]（-1 未标）；
      **仅存本机 SharedPreferences（分键范式，非 Gson，避免 R8 字段名坑）；
      不上传 / 不预审 / 不分享 / 不导出**（导出/分享将同时违反 RED-A6、RED-P 并触发 IARC 重填）
错误：日期非法 → 拒绝写入并返回 false（不得写半截键）；SP 写失败 → 保留既有值，不产生半写状态
线程：SP 写入走 AppContainer.ioExecutor；LiveData 回主线程 observe
```

---

## 8. 风险识别与缓解

| ID | 风险 | 等级 | 缓解措施 | 责任域 |
|---|---|---|---|---|
| R-01 | **调性「更刺激」与 Play 合规红线冲突**：T4 若过界可能触发敏感内容政策升级 / 下架 | 🔴 最高 | §4.2 否决层前置；AC-704 硬门禁；AC-706 分级问卷复核；安安终裁 | android-security / android-release |
| R-02 | **8 语言大规模改写引入串词 / 语法错误**（历史：ja 混俄文、ko 混汉字、zh-CN「小moment」、fr 撇号、zh-TW 简体字） | 🟠 高 | 强制跑 `verify_content.py` / `dp_lint.py` / `sl_lint.py` / `check_strings_mix.py`；幂等生成脚本；fr 转义专项 | android-data / android-build |
| R-03 | **新增内容资产导致 8 语言 ID 不对齐**（历史 ID_MISSING=20） | 🟠 高 | ADR-R13-008：不新增题库 JSON；若必须新增则 `verify_content.py` 参与 CONTENT_FILES | android-data |
| R-04 | **补充项引入账号 / 云 / 位置 / 第三方 SDK**，摧毁核心卖点 | 🔴 最高 | C-1~C-7 硬门槛；AC-G01 权限白名单；[API-004] 构建门禁 | android-build / android-security |
| R-05 | **地区文化差异**：ja/ko 敬语、zh-TW 用语、pt-BR 变体、es 区域俚语 | 🟠 高 | §4.8 地区基线；AC-602/604 盲审一致率 ≥ 90%；母语/近母语复核 | android-data / android-ui |
| R-06 | **文本改动波及硬编码单测断言**（历史 Phase10/12 数量断言 20→32→33→34→36、free 24） | 🟡 中 | ADR-R13-006：不新增 GameModeCatalog 项；改动前先跑 131/131 基线 | android-test |
| R-07 | **「数据不出本机」叙事与本地导出 / 备份 / 锁屏小组件冲突** | 🟡 中 | D/G 列暂缓 + WARN；需产品决策（§9 #5/#6） | team-lead / android-release |
| R-08 | **并发写入冲突**：Phase 1 与 ui/data 同时改 strings.xml / JSON | 🟡 中 | ADR-R13-001 阶段门；目录清单 / 时间戳核验 AC-104（工程非 git 仓库） | 全体 |
| R-09 | **UGC 合规**（推荐 ③ 用户自填内容不上传不预审） | 🟡 中 | SEC-4 约束位；隐私说明明示；合规文档已申报「有限 UGC」 | android-security |
| R-10 | **竞品调研来源可信度**：部分结果来自聚合站 / 商店镜像，功能描述可能过时或不实 | 🟢 低 | 结论仅作定性参考；关键项二次核验（§9 #10）；历史第六/八轮调研为主证据 | 架架 / team-lead |
| R-11 | **调性盲审资源不足**导致一致率阈值不可达 | 🟡 中 | 需确认是否有母语审校（§9 #4）；否则明确降级口径并留档 | team-lead |
| R-12 | **Play 2026-10-29 生效政策收紧**：「intended to be sexually gratifying」与「非自愿性内容」被明令禁止，现有 `PLAY_CONSOLE_CONTENT_RATING.md` §2.1「属于政策允许范畴」判定可能过于乐观 | 🔴 最高 | 主理人裁决 Q2「法务判定，工程不等，取最严路径」→ 5 组存量文案**一律降调**；政策最终判定交 team-lead；IARC 问卷由 release 在 P5 重填 | android-security / android-release |
| R-13 | **动作清单式「加码」倾向**：改写者可能把「更刺激」误解为「动作更具体」，导致分级失真 | 🔴 最高 | H-10 + N8 + CHK-8 三重拦截；§4.5 递增轴改为情绪密度；Phase 3 规格 §5.4 明确 A 项 16 条 key 的禁写清单 | android-ui / android-data |
| R-14 | **存量文案数字过时造成事实失真**：`heat_scope_fmt` 写「20 games」、`paywall_benefits` 写「9 Extreme VIP / all 20 games」，实测为 **36 模式 / VIP 12 / 免费 24** | 🟠 高 | 与 5 组降调同批处理（§4.10 附注 / §9 #2 待确认），避免同文件两轮写入冲突 | android-ui / android-release |
| R-15 | **上架阻断（工程侧）**：targetSdk 35 未达 2026-08-31 起要求的 API 36（延期至 11-01 已过）；安安已报 team-lead | 🔴 最高 | 属 android-build / android-release 域，架架仅登记依赖；**不与本轮文案改动耦合**，但需在发布窗口前解决 | android-build / android-release |
| R-16 | **`verify_content.py` 无 per-language key 集合对齐校验**（实测：仅做全局 union，`grep -c "key_set\|align\|per_lang"` = 0），8 语言 key 不对齐 / 空串会**静默通过 CI** | 🟠 高 | §4.6 / Phase 3 规格 §5.3 建议补第 9 类检查；新 key 一律 8 语言非空（禁止空串） | android-build / android-test |

---

## 9. 待确认项清单（交回 android-studio-expert-team-lead）

> 以下不能由架架自行推断，**显式交回**，必要时请主理人向用户澄清。

| # | 待确认项 | 影响 | 建议决策方 |
|---|---|---|---|
| 1 | **第 1 项补充内容的最终落地范围**：推荐 ①②③ 是否全做 / 只做 ①② / 全部暂缓？ | Phase 3 拍板、工期、发布窗口 | 用户 + team-lead |
| 2 | 18+ 年龄确认的**口径**（Mature 17+ 还是 Adults 18+）、是否可跳过、是否强制首启 | IARC 问卷、ASO、合规文档 | android-release + android-security |
| 3 | T4「午夜失控」**上限尺度**：是否允许「吻」以外的含蓄身体指向？现有 `intimate_modes` 的 `nibble / lick / bite` 类措辞是否需整体降级 | 调性改写量、AC-703 梯度 | 用户偏好 + android-security 终裁 |
| 4 | **8 语言人工审校资源**：是否有母语/近母语审校？还是仅「AI + 双人盲审」？ | AC-604 / AC-705 的 ≥90% 一致率可达性 | team-lead |
| 5 | 是否允许**本地导出 / 备份**（同类常见，但数据可离开本机） | 与「数据不出本机」叙事张力；D/G 暂缓与否 | 用户（产品决策） |
| 6 | 是否明确**不做**桌面 / 锁屏小组件（隐私暴露 vs Couple Joy 卖点） | 隐私叙事一致性 | 用户 + android-release |
| 7 | 需求 7「充满诱惑 / 刺激感」与 Play ASO 保守表述的平衡：是否接受更保守的商店文案 | 上架曝光与政策风险 | 用户 + android-release |
| 8 | 本轮 7 项需求的**优先级排序**，以及补充内容是否占用本轮发布窗口 | 全团排期 | team-lead |
| 9 | **改写归属边界**：界面 strings.xml 归 android-ui、内容 JSON 归 android-data —— 是否确认？跨文件同一文案（如模式描述 ↔ 题库）由谁仲裁 | 避免并发写冲突与口径分裂 | team-lead |
| 10 | 本轮 WebSearch 竞品结论（Cloxer / Twone 等来自商店页与聚合站）是否需**二次核验**后再作为 Phase 3 依据 | 推荐项证据强度 | team-lead |
| 11 | §4.2 否决层黑名单由 android-security **何时冻结**（需在所有改写前） | Step 1 阻塞全链路 | android-security → team-lead |
| 12 | 推荐 ① 是否以「主页会话入口」实现（**不新增第 37 个模式**）—— 建议采纳，但属产品呈现决策 | 数量断言 / 主页信息架构 | 用户 + team-lead |
| 13 | **Play 政策适用性的最终判定**：安安指出 2026-10-29 生效条款禁止「intended to be sexually gratifying」与「非自愿性内容」，现有 `PLAY_CONSOLE_CONTENT_RATING.md` §2.1「属于政策允许范畴」可能过于乐观 —— **法务判定，工程不等** | 应用能否上架 / 分级 / 定位 | team-lead（法务） |
| 14 | **分级口径 17+ vs 18+ 两份文档不一致** —— 主理人已裁决统一 **18+**，由 android-release 在 P5 落实文档同步；架架据此把 B 项口径定为 18+ | IARC 问卷 / 商店文案 / B 项弹窗 | android-release（已裁决，待执行） |
| 15 | **targetSdk 35 未达 API 36 属上架阻断** —— 安安已报 team-lead，架架登记为发布窗口依赖 | 能否按时上架 | android-build / android-release |
| 16 | §4.10 附注的 `heat_scope_fmt` / `paywall_benefits` **数字过时（20→36、9→12）+ sync 歧义** 是否并入本轮同批处理 | 事实准确性 + 与 5 组降调同文件写入冲突 | team-lead |
| 17 | **B 项门禁强度**：软门禁（decline → 保留 ≤2 火焰温和模式）还是硬门禁（decline → 全 App 不可用）—— 架架建议**软门禁** | 审核风险 vs 留存 | 用户 + team-lead |
| 18 | A 项 **PEAK 阶段是否设倒计时** —— 架架建议**不设**（避免计时器把节奏变任务，与 N3 压迫感冲突） | 会话体验 | team-lead |
| 19 | C 项是否允许**删除 / 编辑**当日记录 —— 架架建议允许当日覆盖写，**不提供批量导出 / 分享** | UGC 边界 | team-lead + 用户 |

---

## 附录 A：竞品调研来源与可信度

| 来源 | 类型 | 可信度 | 用途 |
|---|---|---|---|
| 历史工作日志第六轮 / 第八轮（2026-11-23） | 项目内部一手记录 | **高** | 主证据：12 个新玩法、8 项调研建议 |
| `docs/PLAY_CONSOLE_CONTENT_RATING.md` / `GOOGLE_PLAY_LAUNCH_CHECKLIST.md` | 项目内部一手文档 | **高** | 合规待办、分级口径、已有 Consent-First UX |
| 项目源码实证（GameModeCatalog / ConsentFilter / DeckFragment / strings.xml / assets） | 一手 | **高** | §1.2 排除集（本报告最关键的核对） |
| Cloxer（Google Play 商店页镜像，Mature 17+，离线无账号） | 商店描述 | 中高 | Date Night Mode / Favorites 对标 |
| Twone（IndieTower 收录，2026-10 首发，波兰） | 第三方收录 | 中 | Yes·Maybe·No / 呼吸屏 / Journal / PIN 区对标 |
| Paired 官网 / AppOSee / Attached 对比文 | 官网 + 第三方 | 中高 | 依恋测验 / Timeline / 需账号配对（否决依据） |
| Couple Joy 官网 / Question Games (MWM) / SyncPlay 官网 | 官网 + 情报站 | 中 | 小组件 / My Answers / 导出 / 联机类否决依据 |
| Truth or Dare – Partygame（Google Play）、Party Games!（FoxData） | 商店页 | 中高 | 自定义题 / 本地备份 / 离线零追踪 |
| 中文渠道聚合站（tujixiazai / ledanji 等） | 聚合站 | **低~中** | 仅作定性参考（默契报告、节日限定、四档难度） |

> ⚠️ 聚合站与商店镜像的功能描述可能滞后或营销化，**结论仅用于定性对标，不作为 Phase 3 落地的唯一依据**（见 §9 #10、风险 R-10）。

## 附录 B：验收标准总表（AC 全量索引）

| 编号组 | 范围 | 条数 | 主责 |
|---|---|---|---|
| AC-101~106 | R1 补充内容（基准 / 阶段门 / 落地门禁） | 6 | 架架 → team-lead |
| AC-201~205 | R2 UI 可用性 | 5 | android-ui |
| AC-301~306 | R3 界面文本准确 / 私密 | 6 | android-ui |
| AC-401~406 | R4 游戏与功能正常 | 6 | android-runtime / android-test |
| AC-501~505 | R5 题目表述质量 | 5 | android-data |
| AC-601~605 | R6 8 语言地区习惯 | 5 | android-data / android-ui |
| AC-701~706 | R7 8 语言私密调性 | 6 | android-data / android-ui / android-security |
| AC-G01~G08 | 全局硬门槛 | 8 | 全体 |
| **合计** | | **47 条** | |

> **v1.1 变化**：AC 条数不变（47 条），但**判据升级** —— TONE-CHK 由 8 项扩为 **10 项**（新增 CHK-8 动作清单升级、CHK-9 数据流歧义）；反面清单由 N1~N7 扩为 **N1~N9**；硬约束由 H-1~H-9 扩为 **H-1~H-13**；接口契约由 7 条扩为 **10 条**（新增 [API-008]~[API-010]，对应 A/B/C 三项）；ADR 由 8 条扩为 **10 条**；待确认由 12 项扩为 **19 项**。

## 附录 C：给下游专家的使用说明

| 专家 | 请直接使用 | 请勿 |
|---|---|---|
| **android-ui**（任务 #5） | §2.2 R2/R3 的 AC-201~306；**§4 全文**（尤其 §4.6 TONE-CHK、§4.7 8 语言对照、§4.8 地区基线）；**§4.10 存量降调 5 组**；§4.11 安全机制呼应；§6 模块图定位布局归属；**Phase 3 规格 §1.7 / §2.4 / §3.5 / §3.6 / §5.4 的 key 语义** | 不要另立调性标准；不要改内容 JSON；**不要自行判断 IARC 是否需重填**（`RED-D3`） |
| **android-data**（任务 #6） | §2.2 R5/R6/R7 的 AC-501~706；§4.5 梯度矩阵、§4.6 检查表、§4.7 对照、§4.8 地区基线；§1.3/§1.4 竞品题目质量对标；§4.6 末尾「抽样观察」作为复核线索；**§4.10 内容层存量降调**（`bite_lick`/`restraint_light`/`blindfold` 等标签与 `suck softly and kiss on lower belly` 类文本） | 不要把抽样观察当全量结论；不要新增题库 JSON（ADR-R13-008）；**Q6 Keystore 明文降级路径由安安出技术方案后你落地，架架不涉及** |
| **android-security**（任务 #3） | **§4.2 A 区 SEC-6~SEC-12 已对齐**；B 区 SEC-1~SEC-5 待你填充并冻结；[API-006] `forbiddenHit` 契约；AC-704 / AC-G06 签署；§4.10 降调清单口径 | 不要改 §4.1 硬约束文本（如需调整请回传架架）；政策判定由 team-lead 拍板，你已明确不自行拍板 —— 保持一致 |
| **android-build**（任务 #4） | [API-004] 权限/网络门禁；AC-G01~G05；§1.1 基线数值；§6.2 落点约束；**§4.6 / R-16：建议为 `verify_content.py` 补第 9 类 per-language key 对齐校验** | 不要在 Phase 1 触发任何源码构建改动 |
| **android-runtime / android-test**（任务 #7/#8） | AC-401~406、AC-G02/G04/G07；§1.1 的 131/131、74,880 次抽题基线；**[API-008]~[API-010] 的错误分支需单测覆盖** | 不要以「历史基线」为由跳过本轮新增链路的回归 |
| **android-knowledge**（任务 #2） | §5 全部 **10** 条 ADR；§8 风险表（对照历史十二轮陷阱库）；**§4 私密调性正例规范已产出，可入 Phase 6 知识库（未命中项 M-01 已闭合）** | — |
| **android-release**（任务 #9） | §3.2 否决清单（ASO 叙事）、AC-706、§9 #2/#5/#6/#7/#13/#14/#15；§4.10 附注的商店可见文案数字修正 | 不要推荐任何联网/账号功能；**IARC 问卷重填由你判定并执行**（`RED-D3`） |
| **team-lead** | §2.4 映射矩阵、附录 B 47 条 AC、§9 十九项待确认；**Phase 3 规格 §9 六项待确认** | 不要在 Phase 3 之前替用户拍板第 1 项范围 |

---

> **报告结束**。本文件为 Phase 1 唯一交付物（v1.1 已含跨专家对齐增量）；未修改任何代码、布局、strings.xml 或内容 JSON。
> **配套文件**：`round13_phase3_architect_spec.md`（A/B/C 实现规格）、`round13_phase1_security.md`（安安红线）、`round13_phase1_knowledge.md`（识识历史销账与陷阱库）。
> 架架待命 Phase 4 / Phase 5 续接任务。

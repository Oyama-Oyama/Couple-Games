# VelvetCouples 第十三轮 · Phase 1 知识检索与本轮检查清单

> 编制：【识识】Android Studio 专家团 · 知识库与规范专家
> 团队：velvet-audit-2026　｜　任务卡：#2（Phase 1 检索 / Phase 6 续接沉淀）
> 日期：2026-11-23（承接同日十二轮工作日志）
> 声明：**本阶段仅做检索与提炼，未修改任何代码、布局、strings.xml、内容 JSON、生成器脚本；未提前追加工作日志**。Phase 6 再执行新陷阱 / 新模板 / 新 ADR 入库与日志追加。

## 0. 数据来源

| # | 文件 | 用途 |
|---|---|---|
| 1 | `C:\Users\ivy\WorkBuddy\spaces\2026_11_23_09-22-39\.workbuddy\memory\2026-11-23.md` | 十二轮工作日志（ADR / 踩坑 / 竞品调研 / 验收口径主来源） |
| 2 | `audit_report\VelvetCouples_全量审计报告.md` | 首轮全量审计结论与上架前行动清单 |
| 3 | `audit_report\修复实施报告.md` | 7 项修复实施状态、剩余已知非缺陷项 |
| 4 | `docs\GOOGLE_PLAY_LAUNCH_CHECKLIST.md` | 上架 ASO 文案、生产就绪口径 |
| 5 | `docs\PLAY_CONSOLE_CONTENT_RATING.md` | 内容分级、敏感内容政策、**未勾选的上架前清单** |
| 6 | `scripts\check_strings_mix.py`（实读） | 8 语言串词 / key 集合规则（含日语「连续 3 个简体专用字」） |
| 7 | `scripts\verify_content.py`（实读） | 8 类内容回归检查 + 13 类 `CONTENT_FILES` 白名单 |

辅助核对：`docs\` 仅 2 份 md；`audit_report\` 仅 2 份 md（另含 structure_result.json / full_corpus_report.csv / love_map_final.png）；`scripts\` 共 14 个 py（`gen_*` 幂等生成器 + 4 个 lint/校验脚本）。**未发现成文的「私密调性规范 / 题库红线 / UI 可用性规范」独立文档** —— 见 §8 未命中项。

### 0.1 当前工程基线（供 Phase 4 直接复用，勿凭记忆写断言）

| 项 | 基线值 | 来源 |
|---|---|---|
| 游戏模式 | **36**（free 24 / VIP 12） | 第 11 轮（34→36，free 22→24） |
| Java 类 / 布局 / 测试类 | 136 / 49 / 26 | 任务卡 |
| 单测 | **131/131 通过**（failures=0 errors=0 skipped=0） | 第 12 轮 |
| strings.xml | **8 语言各 652 条**（key 集合须完全一致） | 任务卡（日志停留在 441 条时代，已演进） |
| 内容资产 | 8 语言 × **13 类 JSON** | `verify_content.py` CONTENT_FILES 实读 |
| 抽题验证 | 624 组合 × 120 次 = **74,880 次，0 空池** | 首轮 + 各轮回归 |
| 连续重复 | 13/14 类 0%；仅 couple_quiz t1 = **1.7%（<2% 可接受）** | 首轮 / 修复报告 §4 |
| 内容审计 | ID_MISSING=0 / PLACEHOLDER_MISMATCH=0 / 语法错误 0 / 截断 0 / 重复组 3 | 修复实施报告 |
| 剩余**非缺陷**项 | ID_EXTRA 15、SLOT_UNDEFINED 80（误报）、DUP_TEXT 66（设计复用） | 修复实施报告 §4，**勿误改** |
| 构建 | assembleDebug BUILD SUCCESSFUL；assembleRelease(R8) 3.4M，**跳过 lint 属离线环境预期** | 第 11、12 轮 |
| 版本 | versionCode 210 / v2.1.0（README 已统一） | 第 2 轮 |

---

## 1. 已确立架构决策（[ADR-INDEX]）

> 领域分类：架构 / 数据 / 交互 / UI / 安全 / 合规 / 调性 / 规范 / 测试 / 构建。状态均为「接受·有效」，最后验证日期 2026-11-23。

| ID | 决策 | 领域 | 确立轮次 | 与本轮 7 项的关联 |
|----|------|------|------|------|
| [ADR-001] | **零权限零网络**：manifest 仅 VIBRATE，无 INTERNET / 位置权限 / 账号 / 云同步 / 地图 SDK；数据不出本机 | 架构·隐私 | 第 3、11 轮 | **需求 1 硬约束**、3、7 |
| [ADR-002] | **统一内容池 + 配置驱动引擎 + 数据驱动 Fragment**，新增游戏不新增题库 JSON（复用已验证 decks.json） | 架构 | 第 6 轮 | 需求 1、4 |
| [ADR-003] | **开放式游戏不设假答案**：移除 CHOICE 交互，仅保留 TASK / TIMED（不硬套选项 + 假揭晓） | 交互·题库 | 第 7 轮 | **需求 5 直接依据** |
| [ADR-004] | 记忆档案唯一写入口 `ContentRepository.recordSessionStat`；所有 modeId 必须在 `TagDisplayMapper` 有映射 | 数据 | 第 8 轮 | 需求 4 |
| [ADR-005] | 密封情书 **Android Keystore AES-256-GCM**，密钥不可导出；`stateOn` 中 `nowMillis < sealedAt` 强制 SEALED 防回拨系统时间 | 安全 | 第 11 轮 | 需求 3、4、7 |
| [ADR-006] | **Gson 落盘模型字段名 = 落盘 JSON key，R8 会改名** → `proguard-rules.pro` 加 `-keep ...$StoredLetter { *; }` + 反射契约测试 | 构建·安全 | 第 11 轮 | 需求 4 |
| [ADR-007] | 「爱的地图」改造为**抽象关系星图**（LoveGraphView 纯 Canvas 自绘）；真实地理地图判定**不适合**（需网络 / 地图 SDK / 位置权限 / 审图号） | 架构·UI | 第 11 轮 | **需求 1 直接依据** |
| [ADR-008] | 日期规则收敛到纯 Java 无 Android 依赖的 `TogetherDateFormat`；**MaterialDatePicker 只用 UTC 毫秒锚定** | UI | 第 12 轮 | 需求 2 |
| [ADR-009] | **FLAG_SECURE 防截屏默认开启**（单 Activity 冷启动即生效，保留用户手动关闭能力） | 安全 | 第 3 轮 | 需求 3、7 |
| [ADR-010] | 新增/修改多语言文案与内容**必跑** `check_strings_mix.py` / `sl_lint.py` / `dp_lint.py` / `verify_content.py`，并纳入 CI | 规范 | 第 2、10、11、12 轮 | 需求 6、7 |
| [ADR-011] | 8 语言 strings.xml **key 集合完全一致**；**法语撇号必须 `\'`**；**葡语目录是 `values-pt-rBR` 不是 `values-pt`** | 规范 | 第 1、11、12 轮 | 需求 6 |
| [ADR-012] | 内容资产 **ID 以 en 为基准 8 语言对齐**（不允许缺 ID，允许语言独有 ID）；新增 JSON 类别须加入 `CONTENT_FILES` | 规范 | 第 2、10、11 轮 | 需求 1、6 |
| [ADR-013] | 项目用 **JUnit 4**（非 5），断言 msg 在前；单测读 assets 用相对路径 `src/main/assets/content/` | 测试 | 第 10 轮 | 需求 4、5 |
| [ADR-014] | 新增游戏模式**四件套**：`GameModeCatalog` + `nav_graph` action/fragment + `TagDisplayMapper` + `Phase10/Phase12` 数量断言 | 规范 | 第 6、8、9、10、11 轮 | 需求 1、4 |
| [ADR-015] | **内容尺度止于含蓄暗示**（亲吻/触碰/害羞告白），无露骨性行为描写；ASO 刻意规避敏感词；分级 Mature 17+（保守 18+） | 合规·调性 | 第 2 轮 + docs | **需求 3、5、7 红线** |
| [ADR-016] | 主页折叠默认**仅显示 4 个模式**，需展开查看全部 36 个 | UI | 第 6 轮 | 需求 2 |
| [ADR-017] | **私密调性**：界面文案用「只对你说」式亲密表达、绑定真实昵称；避免功能罗列式标题与写死「伴侣 A / 伴侣 B」 | 调性 | 第 12 轮 | 需求 3、7 |

---

## 2. 已踩坑清单（按类归档，标注轮次与修法）

> 格式：[PITFALL-xxx] 现象 / 触发条件 / 根因 / 修复 / 来源轮次 / 标签。最后验证日期均 2026-11-23。

### 2.1 多语言串词 / 混字（需求 6、7 高发区）

- **[PITFALL-001]** 繁体 `zh_tw/couple_quiz.json` 18 处「脸红」未转繁体 → 第 2 轮全库 **5,978 处**异形字统一转换。修法要点：**排除一字对多繁体的歧义字 发/只/面/谷/历**；对 里/着/后/余/云/净/干 经上下文核验后转换；仅转真异形字，勿全局简繁替换。标签：`i18n, zh_tw, 简繁`
- **[PITFALL-002]** 第 10 轮 `date_planner.json` 多语言串词（ja 出现 `"blind 品書き三品、addle 見"`、ko 出现「意见」）→ 写 `scripts/dp_lint.py`（扫描 CJK/ko 串中的拉丁词，白名单 + 停用词表），定位 21 处逐一改写。**新增约会计划器内容必跑此 lint**。标签：`i18n, 串词, date_planner, dp_lint`
- **[PITFALL-003]** 第 11 轮 `sealed_letter.json` **俄文/英文混入日文**（`земた`、`fond characterizedます`）→ 写 `scripts/check_strings_mix.py`（CJK 串禁拉丁词；非 CJK 串禁汉字/假名/谚文/西里尔；剥离 `yyyy-MM-dd` / `HH:mm` / `MM/dd` 格式）+ `sl_lint.py` 专查情书内容；顺带修既有 bug：zh-CN `mode_ldr_sync_desc`「小moment」→「小仪式」。标签：`i18n, 串词, sealed_letter, check_strings_mix`
- **[PITFALL-004]** ⭐ 第 12 轮 **日语串词检测误报**：按「单字简体专用字」报警会误报 会/静/断/探/点/素/当/脱/装/灯/离/来 等**日文合法汉字**（37 处误报）→ 改为 **「连续 3 个简体专用字」才报警**（`CJK_RUN_IN_JA`，遇其他汉字或非汉字即清零）；**韩语出现任何汉字/假名即报警**（`CJK_IN_KO`）。顺带修 2 处既有韩语串词：`mode_most_likely_to_desc`「谁」→누가、`date_planner_no_match`「意见」→의견。标签：`i18n, ja, ko, 误报, check_strings_mix`
- **[PITFALL-005]** 法语 `strings.xml` 撇号必须写 `\'`（否则 aapt 报 `Invalid unicode escape`）；**切勿全局替换 `\'`→`'`**，会破坏原文所有撇号。生成器统一用 `esc()` 处理 `'`→`\'`、`&`、`<`、`>`。标签：`i18n, fr, aapt, 转义`

### 2.2 占位符 / 假答案文不对题（需求 5 直接风险）

- **[PITFALL-006]** 首轮占位符**误报澄清**：`{p1}`/`{p2}`（CoupleQuizEngine）/`{lead}`/`{partner}`（RoleplayEngine）/`{zone}`（ModePromptSynthesizer zoneOverride）/ 23 个 slot 均被代码正确替换，**非缺陷**。`SLOT_UNDEFINED 80` 为审计脚本未识别 zoneOverride 的误报。标签：`占位符, 误报, 勿误改`
- **[PITFALL-007]** 首轮/第 2 轮 `generator_templates.json` 的 `mfp_2` 缺 `{whisper_topic}`、`mrp_4` 缺 `{tease_tool}`（ja/ko/es/fr/pt 5 语种）→ 补齐，PLACEHOLDER_MISMATCH 10→0。标签：`占位符, 8语言对齐`
- **[PITFALL-008]** ⭐ 第 7 轮 **假答案文不对题**：CHOICE 交互给开放式题库卡硬套「我/你/都是/都不是」+ 假揭晓（情话谜题 / 回忆探秘 / 谁更有可能），用户明确指出**这些游戏不需要答案** → 移除 CHOICE 类型，12 mode 全改 TASK（开放式：抽题→回应→完成）或 TIMED；删选项按钮 / `onChoicePicked` / revealed 逻辑与布局选项区；Phase16 断言改为 2 种交互且**禁止 CHOICE**。标签：`需求5, 文不对题, CHOICE, 假答案`
- **[PITFALL-009]** ⭐ 第 12 轮 **带 `%1$s` 占位符的 string 不能直接写在布局 `android:text` 里**（运行时缺参数崩溃）→ 布局移除 `android:text`，改由 `HomeFragment.openMoodCheckin()` 运行时 `setText(getString(...))` 填充。标签：`需求2/3, 占位符, 布局, 崩溃`
- **[PITFALL-010]** 第 2 轮 **拼接后缀勿截断原词**：subtitle 差异化曾引入 `"fake na at beside"` 截断；`connect4Actions.shortLabel` 被硬截断到 14 字符（`inner wrist Ki`、`palm and finge`）→ 从 fullDare 智能提取身体部位短语（their X / from X to Y / tease X with 等句型），截断 90→0。标签：`v21, shortLabel, 截断`

### 2.3 内容 ID 对齐 / 题库质量（需求 1、5、6）

- **[PITFALL-011]** 首轮/第 2 轮 5 语种（ja/ko/es/fr/pt）各缺 **37 deck + 10 auction + 27 scratch + fate**（v2/v21 命名混乱）→ **确认是「真缺」非命名问题**（en `auc_v2_04` 与 ja `auc_v21_t1_26` 内容不同），按 en ID 顺序补 111-112 文本字段/语种，ID_MISSING 20→0。保留各语种独有本地化卡（ID_EXTRA 15 合法）。标签：`需求1/6, ID对齐, 8语言`
- **[PITFALL-012]** 首轮/第 2 轮 v21 批次约 4%（98/2241）**模板拼接语法错误**（`"at behind the"`、`"focusing suck softly"`、`"along on"`、地点套地点场景矛盾）+ 72 组重复 → 11 类正则规则覆盖 + 嵌套 `acts[].direction` 的 v21 状态继承 + tip 追加尾句 / subtitle 场景差异化 / optionB 加区分标签，语法 366→0、重复 116→3。标签：`需求5, v21, 病句, 重复`
- **[PITFALL-013]** 第 5 轮 **题目文本时长与倒计时不一致**：`pr_tie_2`（领带深吻卡）8 语言均写「20 秒深吻」但 `durationSec=45` → 统一改 20。判据：**用正则区分主时长与间隔词**（every / cada / 每次 / ごとに），「每 3 秒定格」「未来承诺描述」属正常，**勿误改**。标签：`需求5, durationSec, 一致性`
- **[PITFALL-014]** ⭐ 第 10、11 轮 **新增内容 JSON 必须加入 `verify_content.py` 的 `CONTENT_FILES`**（`date_planner.json` / `sealed_letter.json` 均已加入）才会参与 8 语言 ID 对齐校验；当前共 13 类。标签：`需求1/6, verify_content, CONTENT_FILES`

### 2.4 导航返回 / 栈（需求 2、4）

- **[PITFALL-015]** 第 3 轮 游戏中「安全暂停 → 调整边界」跳 `nav_consent` 用普通 navigate 压栈，**返回落回隐藏底栏的游戏页** → `MainActivity.showSafePauseDialog` 改用 `NavOptions.setPopUpTo(nav_home,false)+setLaunchSingleTop(true)`；`nav_graph.xml` 的 `action_blind_match_to_couple_board` 补 `popUpTo/popUpToInclusive` 防栈累积。标签：`需求2, 导航, 返回栈`
- **[PITFALL-016]** 第 5 轮 手动 NavOptions 与 NavigationUI 栈状态不同步、**底栏无法返回** → 改用 `NavigationUI.onNavDestinationSelected(...)` 模拟真实底栏点击，由 NavigationUI 统一管理栈；移除 NavOptions import。标签：`需求2, NavigationUI`
- **[PITFALL-017]** 第 7 轮 12 游戏共用 `nav_social_game`，`destination.getLabel()` 固定 → topbar **全部显示「今日心语」** → MainActivity 对 `id==nav_social_game` 从 arguments 取 `ARG_MODE_ID`，用 `GameModeCatalog.fromId().titleRes` 动态解析。标签：`需求2, 标题, 动态解析`
- **[PITFALL-018]** 第 8 轮 **新游戏未记入「约会夜记忆档案」**：主因 `TagDisplayMapper.resolveGameModeRes()` 只映射 20 个老键，12 个新键 **fallback 显示为「情侣棋局」**；次因 `drawNext()` 里 `completedInRound=0` 使 `markDone()` 永远 false、数值语义错（A=1, B=roundSize）、退出无兜底 → 补 12 键映射；删清零行（轮内累计、由 resetRound 清零）；Fragment 加 `completedSinceLastWrite` 字段使 A=B=完整完成数；`onDestroyView()` 兜底 flush。标签：`需求4, 记忆档案, TagDisplayMapper`

### 2.5 FLAG_SECURE / 截图 / 真机验证（需求 3、4）

- **[PITFALL-019]** 第 11 轮 **FLAG_SECURE 导致 `adb screencap` 得到 0 字节截图** → 临时在设置页关闭防截屏再截图，验证完**必须恢复开启**。标签：`需求3/4, FLAG_SECURE, 真机`
- **[PITFALL-020]** 第 11 轮 截图工具返回 **600 宽缩放版，设备实为 900 宽**，坐标换算需 ×1.5；PowerShell 控制台中文/GBK 乱码，改用 Git Bash 读取验证。标签：`真机, 坐标, 编码`
- **[PITFALL-021]** 第 12 轮 MCP `type_text` 是**追加**不是替换，须先 `KEYCODE_MOVE_END` + N 次 `KEYCODE_DEL` 清空再输入；Git Bash 里 `adb shell uiautomator dump /sdcard/ui.xml` 会被 MSYS 路径转换，必须加 `MSYS_NO_PATHCONV=1` 且 pull 到当前目录（`/tmp` 在 Git Bash 下不可写）。标签：`真机, adb, MSYS`

### 2.6 R8 / 构建 / 日期 / 自定义 View（需求 2、4）

- **[PITFALL-022]** ⭐ 第 11 轮 **Gson 模型字段名被 R8 改名 → 升级后信件全读不出** → `app/proguard-rules.pro` 加 `-keep class ...SealedLetterManager$StoredLetter { *; }`，并加 `SealedLetterStoreContractTest` 反射校验字段名锁定。标签：`需求4, R8, Gson, 落盘兼容`
- **[PITFALL-023]** 第 11、12 轮 `assembleRelease`（R8）**跳过 lint**：离线环境缺 `lint-gradle:31.7.3` 缓存，**非代码问题**，勿误判为构建失败。标签：`构建, 离线, lint`
- **[PITFALL-024]** ⭐ 首轮 **删/缺 string key 后布局仍引用会编译失败**（`fragment_placeholder.xml` 引用缺失 `placeholder_desc`）→ 8 语言各补 1 条；第 12 轮清理 5 个废弃 key（`settings_set_together_date` 等）时**必须同步改布局**。标签：`需求1/6, 资源引用, 编译阻断`
- **[PITFALL-025]** 第 12 轮 `setSelection(...)` 定义在 **`MaterialDatePicker.Builder` 上，不在 Picker 实例上**（写成 `picker.setSelection()` 编译不过）；`DateValidatorPointBackward.now()` 本就 UTC 锚定，无需改动。标签：`需求2, MaterialDatePicker, 编译`
- **[PITFALL-026]** 第 12 轮 **MaterialDatePicker 只用 UTC 毫秒锚定**，必须 `date.atStartOfDay(ZoneOffset.UTC)` ↔ `Instant.atZone(UTC).toLocalDate()` 双向转换，否则 **off-by-one 一天**；`TogetherDateFormatTest` 遍历 2024 闰年每一天验证往返。标签：`需求2, UTC, off-by-one`
- **[PITFALL-027]** 第 11 轮 `LoveGraphEngine.node()` 工厂**未传 ring 默认全 0** → 外环节点被 `if (n.ring==0) continue` 跳过、**所有外环无连线** → 显式传 ring 参数并删除冗余 `ringOf()` 映射。标签：`需求4, 星图, 真bug`
- **[PITFALL-028]** 第 11 轮 `LoveGraphView` **节点标签互相重叠且被圆圈压住** → 最终改为两遍式 `layoutLabels()`：先算基线 → 3 轮成对松弛消解重叠 → 加「不得向内推回节点」约束（`labelClearance = radius+12sp+11sp`）；另 `drawLabel` 误用 `drawNodes` 局部变量 `selected` 导致编译错误，改方法内自行 `node.id.equals(selectedId)` 判断。标签：`需求2, 标签遮挡, Canvas`

### 2.7 测试 / 断言（需求 4、5）

- **[PITFALL-029]** ⭐ 第 10 轮 项目用 **JUnit 4（非 JUnit5）**：`assertTrue(cond,msg)` 参数顺序相反 → 必须写 `assertTrue(msg, cond)`；`assertNotNull` 同样；`assertEquals(Set)` 需 `(msg, expected, actual)`。单测读 assets 要用相对路径 `src/main/assets/content/`（classloader 拿不到 assets）。标签：`需求4, JUnit4, 断言`
- **[PITFALL-030]** 第 3、4 轮 2 个既有抽题去重测试失败：① `TaskEngine` 的 `RECENT_CARD_SHIELD=36` 大于测试池 16 张，recycle 分支先全部 shield 再全部释放，退化成完全随机 → 新增 `MIN_RECYCLE_AVAILABLE=4`，只 shield 最近一批、始终保留 4 张可用；② `Phase13VarietyEngineTest` 循环第 10 次 `climbNext()` 触发登顶自动 `buildFreshClimb()`，`e2` 进入「全部已见过」回退 → 测试改 `if (i < 9)`（**测试 off-by-one，非引擎缺陷**）。标签：`需求4, 抽题去重, 回归`
- **[PITFALL-031]** 第 6 轮 **测试期望值要按引擎实际 tag 偏好 + block 规则算，不是按 pool 原始卡数**。标签：`需求4, 断言`
- **[PITFALL-032]** 第 11 轮 两处测试断言自己写错（① 误以为原信 body 为空，原信本就持有明文；② 误判到期日当天状态为 SEALED，实为 READY）→ **断言必须先对照被测实现语义再写**。标签：`需求4, 断言语义`
- **[PITFALL-033]** 第 9 轮 `BadgeCatalog` 需 `import R`；Fragment 用了 `daily_reveal_reveal` 但生成脚本**漏定义** → 补 8 语言并**同步修改生成器脚本**（幂等生成器与产出必须一致）。标签：`需求1/6, 生成器, key`
- **[PITFALL-034]** ⭐ 第 3、6、9、10、11 轮 **新增模式必须同步 `Phase10/Phase12` 硬编码数量断言**（20→32→33→34→**36**，free 11→20→21→22→**24**，VIP 9→12，主题 9→12），否则全量单测红。标签：`需求1/4, 数量断言, 回归`

### 2.8 文案调性 / 合规（需求 3、5、7 直接范例）

- **[PITFALL-035]** ⭐ 第 12 轮 心情卡「**心情与感激 / Mood & Gratitude**」是**功能罗列式标题，不贴合情侣软件** → 改「**只对你说**」（8 语言），副标题「只属于你们两个人的小角落，只有你们看得到。」按钮「告诉我你此刻的心情」；感激标签带昵称「关于 **Maya**，我最喜欢的一点」（`mood_gratitude_a_named/b_named`）。**这是「羞涩又开放」调性的正例范式**。标签：`需求3/7, 调性, 正例`
- **[PITFALL-036]** 第 12 轮 情书「谁在写的」写死「**伴侣 A / 伴侣 B**」→ `onViewCreated()` 加 `profileManager.observeState()` 观察者，绑定 `state.partnerA.name / partnerB.name`，**改名后实时刷新**（Alex / Maya → Alex / 小雅）。标签：`需求3/7, 昵称, 实时刷新`
- **[PITFALL-037]** 第 5 轮 Onboarding 伴侣 A 默认代词 `"They"` → 改 `"He"`（`readCurrentState` + `saveProfiles` 双兜底），首次打开显示「他 (He / Him)」。标签：`需求3, Onboarding, 代词`
- **[PITFALL-038]** 首轮 + docs **内容尺度红线**：题库止于亲吻/触碰/含蓄暗示，**无露骨性行为描写、无暴力违法**；最高热度 tier4/VIP 不越界；ASO 文案刻意规避敏感词；分级 **Mature 17+**（保守可 18+）。标签：`需求3/5/7, 合规, 红线, ASO`

---

## 3. 竞品调研结论（第六、八轮）—— 需求 1 的直接输入

### 3.1 当时调研过的同类 App / 渠道（列全）

| 轮次 | 渠道 / 同类 App | 产出 |
|---|---|---|
| 第六轮 | **Google Play**：Couple Games: Fun for Lovers、Darezo、Truth or Dare 2、Fizzy、Never Have I Ever | 去重后确定 **12 个新玩法** |
| 第六轮 | 网页：couplemissions.com 对比文 | 玩法对比 |
| 第六轮 | 中文渠道：PP助手、豌豆荚、吾记日记 等 | 中文侧玩法/调性参考 |
| 第八轮 | **Paired、Tandem、Connected、Couple Joy** 等 | 形成 **8 项关系功能建议**（「待用户决策是否实施」） |
| 第十一轮 | 用户主动问「私密慢递情书 / 爱的地图是否适合」 | **适配性判定**：情书 100% 适合；真实地理地图**不适合** |

### 3.2 已落地功能（逐项销账）

| 来源 | 建议 / 玩法 | 落地情况 |
|---|---|---|
| 第六轮 | 12 个新玩法：`conversation_starter` / `love_language` / `daily_question` / `riddle_me` / `memory_lane` / `most_likely_to` / `bucket_list` / `ldr_sync` / `forfeit_wheel`（免费）+ `scenario_choice` / `hot_seat` / `secret_mission`（VIP） | ✅ 全部落地（SocialGamesEngine + 单 Fragment 宿主 + 12 action），模式 20→32 |
| 第八轮 ① | 每日双人揭晓问答 | ✅ 第 9 轮 `DAILY_REVEAL`（DailyRevealFragment + PassPhoneDialog + 并排揭晓） |
| 第八轮 ③ | 心情追踪 + 感激留言 | ✅ 第 9 轮 `MoodCheckinManager`；第 12 轮标题改「只对你说」 |
| 第八轮 ④ | 习惯 streak + 成就徽章 | ✅ 第 9 轮 `StreakCalculator` + `BadgeCatalog`（5 徽章） |
| 第八轮 ⑥ | 约会规划 | ✅ 第 10 轮 Date Planner（36 活动 × 6 类别 + 纯 Java 引擎 + 8 语言） |
| 第八轮 ⑦ 部分 | 在一起天数 / 纪念日 | ✅ 第 9 轮 `togetherSince` + 第 12 轮 MaterialDatePicker |
| 第八轮 ⑤ 部分 | 爱好的地图 | ⚠️ 第 11 轮改造为**抽象「关系星图」LOVE_GRAPH** 落地（已放弃真实地理地图） |
| 第十一轮 | 私密慢递情书 | ✅ `SEALED_LETTER`（Keystore 加密 + 延时解锁） |
| 增量 | 好评引导 / 记忆档案映射修复 / 3 个暧昧主题 / 倒计时音效 | ✅ 第 3、8 轮 |

### 3.3 ⭐ 尚未落地的竞品建议清单（需求 1 直接输入，务必列全）

> 以下为第八轮明确标注「**待用户决策是否实施**」、以及第十一轮明确判定「不适合/未实现」的部分，截至第十二轮结束**仍未落地**：

| # | 未落地项 | 原始出处 | 现状与约束提示（供 architect / team-lead 判断，不下技术结论） |
|---|---|---|---|
| **U-01** | **依恋 / 性格测验 + AI 洞察** | 第八轮 ② | 完全未落地。**「AI 洞察」需网络，与 [ADR-001] 零网络冲突**；但「纯本地离线问卷 + 固定结果解读」部分未见实现记录，可作为纯单机候选 |
| **U-02** | **真实地理「爱的地图」** | 第八轮 ⑤ / 第十一轮 | 第 11 轮**明确判定不适合**（需网络 / 地图 SDK / 位置权限 / 审图号），已用抽象关系星图替代。属**主动放弃**，非遗漏 |
| **U-03** | **关系日记 / 关系时间轴** | 第八轮 ⑤（「爱好的地图/**关系日记**」） | 未见独立落地记录；现有「约会夜记忆档案」是 `session_stats` 统计，非日记/时间轴形态。纯本地日记与 [ADR-001] 不冲突 |
| **U-04** | **共享日历（跨设备 / 双人同步）** | 第八轮 ⑦ | 「纪念日/在一起天数」已落地，但**跨设备共享日历需账号/云同步，与 [ADR-001] 冲突**；纯本机双人共用的日历视图未见实现 |
| **U-05** | **一起看 / 同步播放列表** | 第八轮 ⑧ | 完全未落地。需网络流媒体/账号，**与 [ADR-001] 冲突**；若要做只能退化为「本地共同片单/待看清单」 |
| **U-06** | **Play Console 上架前 6 项未勾选项** | `PLAY_CONSOLE_CONTENT_RATING.md` §3 | 见 §4.4，属**运营未闭环**，非功能建议，但同样计入「仍缺失的内容」 |

补充结论：第八轮标题为「落地 🟢 4 项关系功能」，与 3.2 中已销账的 ①②③④ + ⑦纪念日相符；**U-01 ~ U-05 从未进入实施轮次**。第六轮的 12 个新玩法与第十一轮的情书/星图均**已全部落地**，不应重复计入缺口。

---

## 4. 四份既有文档的结论与未闭环项（对齐本轮 7 项）

### 4.1 `VelvetCouples_全量审计报告.md`

- 既有结论：核心引擎与防重复机制「可直接用于上架」；曾存在 1 个构建阻断 bug、跨语言题库不同步、v21 语法错误三类必处理问题（**均已在第 2 轮修复闭环**）。
- 与本轮相关：**§2.3** 付费高热度模式病句直接损害付费口碑（→ 需求 5）；**§2.7** 占位符误报澄清（→ 需求 5/6 勿误改）；**§2.8** 18+/成人内容分级与敏感内容政策（→ 需求 3/7 红线）。
- 未闭环：文档 §四 的行动清单为**历史快照**（当时未修），已被 `修复实施报告.md` 全部销账；**该文档本身未回写状态**，引用时须以免误判。

### 4.2 `修复实施报告.md`

- 既有结论：7 项任务 **7/7 完成**，零回归确认。
- 关键口径（本轮必须继承）：**§四「剩余已知项（非缺陷，无需修复）」** —— `ID_EXTRA 15`（各语种独有本地化卡，合法）、`SLOT_UNDEFINED 80`（`{zone}` 运行时替换，误报）、`DUP_TEXT 66`（roleplay 角色常量与通用耳语台词库，设计复用）、`Couple Quiz t1 1.7%`（<2% 可接受）。**本轮任何专家不得把这四类当缺陷「修掉」**。
- 未闭环：CI 校验 8 类回归已建立，但**未覆盖「界面调性 / 地区用词习惯 / 题目歧义」的主观维度**（→ 需求 3/5/6/7 需人工 + 专家判定）。

### 4.3 `GOOGLE_PLAY_LAUNCH_CHECKLIST.md`

- 既有结论：Billing SKU 三项 ID（`velvet_weekly_pass` / `velvet_annual_pass` / `velvet_lifetime_unlock`）；**ASO 文案合规（零禁用 emoji / 零露骨词）**，en/zh-CN/ja-JP 三语商店文案已定稿；Edge-to-Edge(targetSdk35)、自适应图标、Consent-First UX、Data Safety 已就绪。
- 与本轮相关：**ASO 合规文案是「含蓄暗示」尺度的对标基准**（→ 需求 3/7）；Consent-First / Safe Word / Private Pass 属私密软件 UX 基线（→ 需求 2/3）。
- 未闭环：**仅提供 en / zh-CN / ja-JP 三语 ASO 文案，其余 5 语（zh-TW / ko / es / fr / pt-BR）商店文案未见记录**（→ 需求 6/7 可提示）。

### 4.4 `PLAY_CONSOLE_CONTENT_RATING.md`

- 既有结论：IARC 问卷建议答复（性暗示 = **Suggestive / Mild**，无图形裸露、无露骨性行为、无真实赌博、无在线互动）；建议分级 **Mature 17+**；§4 已核验「8 语言约 2.5 万条文本，无露骨性行为描写、无暴力/违法」。
- ⭐ **§3「上架前必办清单」6 项全部未勾选 `[ ]`（未闭环）**：
  1. 内容分级问卷按 §1 如实作答并确认最终分级
  2. 提交**隐私政策 URL**（Data Safety 表单必填）
  3. 核对订阅商品 ID 与 `PlayBillingBridge.java` 一致 + 自动续订披露文案
  4. 应用内「**管理订阅**」入口与取消说明
  5. 商店截图与文案避免露骨词汇
  6. **首次启动 18+ 年龄确认弹窗**（降低政策风险）
  7. 保留题库审核记录（本次报告 + CI 脚本）以备申诉
- 与本轮相关：第 6 项 18+ 弹窗直接影响**需求 2（UI 习惯）/ 需求 3（成年向定位说明）**；「含蓄暗示、止于亲吻/触碰」是**需求 3/5/7 的调性红线**（[ADR-015]）；§2.4 明确要求首次启动有年龄确认或成人向定位说明，避免被归入儿童目录。

---

## 5. ⭐ 本轮检查清单（7 项需求 → 可勾选 checklist，供 Phase 4 test/runtime 直接复用）

> [CHECKLIST-R13]　类型：全量复审（需求 1~7）　适用场景：第十三轮 Phase 3/4 审计与回归　最后更新日期：2026-11-23
> 每项 `[ ]` 后标注历史风险点 ID；**打勾前必须给出证据**（脚本输出 / 截图 / 单测结果 / 具体文件行）。

### 需求 1 · 补充同类型软件仍然缺失的内容
- [ ] 逐项评估 §3.3 的 **U-01 ~ U-05**，明确「可做纯本地离线版」与「必须放弃（违反 [ADR-001]）」的分界 —— `PITFALL-038 / ADR-001`
- [ ] 任何新增功能**不得引入 INTERNET / 位置权限 / 账号 / 云同步 / 地图 SDK / 审图号**；manifest 保持仅 VIBRATE —— `ADR-001 / ADR-007`
- [ ] 新增玩法优先复用统一内容池 + 配置驱动引擎（[ADR-002]）；若确需新增 JSON 类别 → **8 语言 ID 对齐 + 加入 `CONTENT_FILES`** —— `PITFALL-011 / 014 / ADR-012`
- [ ] 新增模式走**四件套**（GameModeCatalog / nav_graph / TagDisplayMapper / Phase10·12 数量断言 36·24·12） —— `PITFALL-018 / 034 / ADR-014`
- [ ] 新增文案 8 语言 key 集合一致 + 生成器幂等且与产出同步 —— `PITFALL-024 / 033 / ADR-011`
- [ ] 不得把「关系星图」回退成真实地理地图 —— `ADR-007`

### 需求 2 · UI 是否符合用户习惯、便于操作
- [ ] 主页折叠默认仅显示 4 个模式 → 验证 **36 个模式均可发现、可达、可返回** —— `ADR-016 / PITFALL-015 / 016`
- [ ] 全库扫描：带 `%1$s` / `%1$d` 的 string **只在运行时 `setText(getString(...))` 填充，布局无直写** —— `PITFALL-009`
- [ ] 「安全暂停 → 调整边界 → 返回」不落回隐藏底栏的游戏页；底栏状态与 NavigationUI 同步 —— `PITFALL-015 / 016`
- [ ] 共用 `nav_social_game` 的模式 topbar 标题**动态正确**，无统一显示「今日心语」 —— `PITFALL-017`
- [ ] 日期选择器：已设日期**回显** + 编辑/清除按钮 + 禁未来 + UTC 无 off-by-one + 本地化 MEDIUM 格式 —— `PITFALL-025 / 026 / ADR-008`
- [ ] 真实昵称在「设置页摘要 / 情书作者选项 / 感激标签」**改名后实时刷新** —— `PITFALL-036`
- [ ] 关系星图 11 个标签**两两分离、无遮挡、无溢出、不压节点** —— `PITFALL-028`
- [ ] 倒计时结束有触感 + 提示音（Vibrator 70ms + TONE_PROP_BEEP 130ms，遵循通知音量/静音） —— 第 3 轮
- [ ] 18+ 年龄确认 / 成人向定位说明在首启可见（**未闭环项**） —— §4.4

### 需求 3 · 界面文本是否准确、符合情侣私密软件要求
- [ ] 无**功能罗列式 / 工具化标题**（对标「心情与感激」→「只对你说」正例） —— `PITFALL-035 / ADR-017`
- [ ] 无写死「伴侣 A / 伴侣 B」，一律真实昵称；Onboarding 默认代词正确 —— `PITFALL-036 / 037`
- [ ] 隐私叙事一致：零权限 / 零网络 / 零广告 / 数据不出本机 —— `ADR-001 / docs`
- [ ] **FLAG_SECURE 默认开启**（冷启动即生效，保留手动关闭） —— `ADR-009 / PITFALL-019`
- [ ] 文案尺度**止于含蓄暗示**，无露骨词、无粗暴直白、无图形裸露描述 —— `PITFALL-038 / ADR-015 / docs §2.1`
- [ ] 8 语言「只属于你们两个人」类私密语气一致，无翻译腔/生硬直译 —— `ADR-017`

### 需求 4 · 各游戏 / 功能是否正常
- [ ] **36 个模式全部可达**，抽题非空池（回归 `FullCorpusValidationTest` 74,880 次，基线 0 空池） —— 首轮
- [ ] 记忆档案：所有 modeId 在 `TagDisplayMapper` 有映射，**不 fallback「情侣棋局」**；完成数 A=B；`onDestroyView` 兜底 flush —— `PITFALL-018 / ADR-004`
- [ ] 密封情书：Keystore 密文落盘、重装不丢、防回拨时间、到期可拆、openedAt 写入 —— `ADR-005 / 第 11 轮真机`
- [ ] **R8 release 构建后 Gson 字段名未被混淆**（反射契约测试通过） —— `PITFALL-022 / ADR-006`
- [ ] 防连续重复：`MIN_RECYCLE_AVAILABLE=4` 生效；couple_quiz t1 1.7% <2% 不恶化 —— `PITFALL-030 / 修复报告 §4`
- [ ] 关系星图外环**有连线**（ring 参数正确） —— `PITFALL-027`
- [ ] 全量单测 **131/131**；模式数变动同步 Phase10/12 断言 —— `PITFALL-034`
- [ ] `assembleDebug` / `assembleRelease` BUILD SUCCESSFUL（release 跳过 lint 属预期，勿误判） —— `PITFALL-023`

### 需求 5 · 题目表述准确、无歧义 / 不正常题目（可参考同类软件）
- [ ] **无「假答案文不对题」**：开放式游戏不得硬套选项 + 假揭晓（禁 CHOICE） —— `PITFALL-008 / ADR-003`
- [ ] 无模板拼接病句（`at behind` / `focusing suck softly` / 地点套地点 / 场景逻辑矛盾） —— `PITFALL-012`
- [ ] `shortLabel` 无截断、无半词 —— `PITFALL-010`
- [ ] **题目文本时长与 `durationSec` 一致**；「每 3 秒定格 / 未来承诺」等间隔词**属正常，勿误改** —— `PITFALL-013`
- [ ] 难度 / tier 与内容热度匹配，tier4/VIP **无越界露骨升级** —— `ADR-015 / docs §4`
- [ ] **勿误改非缺陷项**：重复组 3（角色常量）、`DUP_TEXT 66`（设计复用）、`SLOT_UNDEFINED 80`（误报）、`ID_EXTRA 15`（合法扩展） —— 修复报告 §4
- [ ] 占位符 `{p1}/{p2}/{lead}/{partner}/{zone}` 渲染正确且**勿当 bug 修** —— `PITFALL-006 / 007`

### 需求 6 · 各语言界面文本与题目符合对应地区用户习惯
- [ ] 8 语言 strings.xml **key 集合完全一致**（现各 652 条），`MISSING_KEYS=0` —— `ADR-011 / check_strings_mix.py`
- [ ] 无串词：CJK 串无拉丁词、非 CJK 串无汉字/假名/谚文/西里尔；`yyyy-MM-dd`/`HH:mm` 不误报 —— `PITFALL-002 / 003`
- [ ] **日语保持「连续 3 个简体专用字」规则，勿改回单字报警**；韩语任何汉字/假名即报 —— `PITFALL-004`
- [ ] 繁体无简体异形字残留（「脸红」→「臉紅」；排除歧义字 发/只/面/谷/历） —— `PITFALL-001`
- [ ] 法语撇号 `\'` 转义；葡语目录 `values-pt-rBR` —— `PITFALL-005 / ADR-011`
- [ ] 13 类内容 JSON **8 语言 ID 以 en 为基准对齐**，`ID_MISSING=0`；新增类别已入 `CONTENT_FILES` —— `PITFALL-011 / 014 / ADR-012`
- [ ] 日期 / 数字 / 时长按 `DateTimeFormatter.ofLocalizedDate(MEDIUM).withLocale(locale)` 本地化 —— 第 12 轮
- [ ] 地区用词习惯人工抽检（ja/ko 敬语与语体、es/fr 阴阳性、pt-rBR 巴西葡语用词、zh-TW 繁体用语） —— **无历史自动化覆盖，需专家判定**
- [ ] 商店 ASO 文案补齐 zh-TW / ko / es / fr / pt-BR 五语（现状仅 en / zh-CN / ja-JP） —— §4.3

### 需求 7 · 各语言界面文本与题目符合情侣私密软件要求（羞涩又开放、诱惑暗示、吸引力、刺激感）
- [ ] 调性统一为「**羞涩又开放**」：非功能罗列、非直白露骨、非冷淡工具化；对标「只对你说」范式 —— `PITFALL-035 / ADR-017`
- [ ] 8 语言均带昵称 / 私密称呼 / 第二人称亲密语气 —— `PITFALL-036`
- [ ] 内容与 tier / 热度匹配：高热度有**吸引力与刺激感**但**止于暗示**（亲吻/触碰/轻咬） —— `ADR-015 / docs §2.1`
- [ ] 8 语言调性**一致**，不出现某语言过度直白或过度拘谨 —— **无历史自动化覆盖，需专家判定**
- [ ] UI 内文案不得突破 ASO 合规尺度；无露骨性行为描写 / 无暴力违法 —— `PITFALL-038 / docs §4`
- [ ] 新增/修改情书文案跑 `sl_lint.py`、约会计划跑 `dp_lint.py`、全量跑 `check_strings_mix.py` —— `ADR-010 / PITFALL-002 / 003`

---

## 6. ⭐ 本轮绝对不能重犯的陷阱（不得重犯清单）

> 分 A~E 五类。每条给出「错误做法 → 后果 → 正确做法 → 来源轮次」。**这是本轮硬约束，任何专家改动前须先读。**

### A. 资源与文案改动硬约束
1. **删 / 重命名 string key 后布局仍引用 → 编译失败**。清理 `settings_set_together_date` 等废弃 key 时必须同步改所有 `res/` 引用；新增 `@string/xxx` 必须 8 语言齐全且 key 集合一致。（首轮、第 12 轮 / `PITFALL-024`）
2. **带 `%1$s` / `%1$d` 的字符串不能写进布局 `android:text`** → 运行时缺参数崩溃。布局移除 `android:text`，改运行时 `setText(getString(...))`。（第 12 轮 / `PITFALL-009`）
3. **法语撇号必须 `\'`，且禁止全局替换 `\'`→`'`**（会破坏原文所有撇号）；生成器统一用 `esc()` 处理 `'`、`&`、`<`、`>`。（第 1、2、12 轮 / `PITFALL-005`）
4. **葡语目录是 `values-pt-rBR`，不是 `values-pt`**；内容资产语言目录是 `pt`。（第 11 轮 / `ADR-011`）
5. **生成器脚本必须与产出同步**（漏定义 key 会导致 Fragment 引用未定义资源）；8 语言缺一不可。（第 9 轮 / `PITFALL-033`）
6. **拼接后缀勿截断原词**，`shortLabel` 不得硬截断到固定字符数。（第 2 轮 / `PITFALL-010`）

### B. 内容资产改动硬约束
7. **改任何内容 JSON 必须 8 语言 ID 对齐（en 为基准，不允许缺 ID）并跑 `verify_content.py`**；**新增 JSON 类别必须加入 `CONTENT_FILES`**，否则不参与校验。（第 2、10、11 轮 / `PITFALL-011 / 014`）
8. **新增 / 修改多语言文案必跑 `check_strings_mix.py`**；情书内容加跑 `sl_lint.py`；约会计划器加跑 `dp_lint.py`。（第 10、11、12 轮 / `ADR-010`）
9. **日语串词检测必须用「连续 3 个简体专用字」规则**，改回单字报警会误报 37 处日文合法汉字（会/静/断/探/点/素/当/脱/装/灯/离/来）；**韩语出现任何汉字/假名即报警**。（第 12 轮 / `PITFALL-004`）
10. **繁体转换只转真异形字**，排除歧义字 发/只/面/谷/历，勿做全局简繁替换。（第 2 轮 / `PITFALL-001`）
11. **勿把四类非缺陷「修掉」**：`ID_EXTRA 15`、`SLOT_UNDEFINED 80`、`DUP_TEXT 66`、couple_quiz t1 1.7%。（修复报告 §4 / `PITFALL-006`）

### C. 交互与题目硬约束
12. **不要给开放式题目硬套假答案 / 假揭晓**（「我/你/都是/都不是」+ CHOICE）—— 用户已明确否决；12 个 social 模式仅 TASK / TIMED。（第 7 轮 / `PITFALL-008 / ADR-003`）
13. **题目文本时长必须与 `durationSec` 一致**，但「每 3 秒定格」「未来承诺」等间隔/描述**属正常，勿误改**（用正则区分 every / cada / 每次 / ごとに）。（第 5 轮 / `PITFALL-013`）
14. **界面文案不得用功能罗列式标题 / 写死「伴侣 A / 伴侣 B」**，须亲密表达 + 绑定真实昵称并实时刷新。（第 12 轮 / `PITFALL-035 / 036`）
15. **内容尺度不得越界**：止于含蓄暗示，无露骨性行为描写、无暴力违法；ASO 与 UI 文案均规避敏感词。（`ADR-015 / PITFALL-038`）

### D. 架构 / 导航 / 合规红线
16. **绝对不能引入 INTERNET / 位置权限 / 账号 / 云同步 / 地图 SDK / 审图号** —— 需求 1 补充功能时最易踩；manifest 保持仅 VIBRATE。（第 11 轮 / `ADR-001 / 007`）
17. **新增模式必须走四件套 + 同步 Phase10/12 数量断言（36 / free 24 / VIP 12）**，且 `TagDisplayMapper` 必须映射，否则记忆档案 fallback 显示「情侣棋局」。（第 6、8~11 轮 / `PITFALL-018 / 034 / ADR-014`）
18. **导航跳转用 `NavigationUI.onNavDestinationSelected` 或正确 `NavOptions`（popUpTo + launchSingleTop）**，勿用普通 navigate 压栈导致返回落回游戏页 / 底栏无法返回。（第 3、5 轮 / `PITFALL-015 / 016`）
19. **Gson 落盘模型必须 R8 `-keep` 锁定字段名**，否则升级后历史数据全读不出。（第 11 轮 / `PITFALL-022`）
20. **FLAG_SECURE 保持默认开启**；真机截图临时关闭后**必须恢复**。（第 3、11 轮 / `PITFALL-019`）
21. **MaterialDatePicker 日期一律 UTC 毫秒双向转换**；`setSelection` 在 `Builder` 上不在 Picker 实例上。（第 12 轮 / `PITFALL-025 / 026`）

### E. 构建 / 测试 / 验证硬约束
22. **JUnit 4 断言 msg 在前**：`assertTrue(msg, cond)`、`assertEquals(msg, expected, actual)`；单测读 assets 用相对路径 `src/main/assets/content/`。（第 10 轮 / `PITFALL-029`）
23. **断言先对照被测实现语义再写**（勿误判原信 body 为空 / 到期当天状态）；测试期望值按引擎 tag 偏好 + block 规则算，不按 pool 原始卡数。（第 6、11 轮 / `PITFALL-031 / 032`）
24. **改动后跑全套回归**：`check_strings_mix.py` → `verify_content.py` → `sl_lint.py` → `dp_lint.py` → `assembleDebug` → 全量单测（基线 131/131）→ `assembleRelease`（R8）。**release 跳过 lint 是离线缓存问题，勿当构建失败**。（`PITFALL-023 / ADR-010`）
25. **真机验证陷阱**：`type_text` 是追加需先清空；`adb uiautomator dump` 需 `MSYS_NO_PATHCONV=1`；截图 600 宽需 ×1.5 换算；中文输出用 Git Bash。（第 11、12 轮 / `PITFALL-020 / 021`）

---

## 7. 验收口径 / 命令速查（[TEMPLATE-CHECK] 复用）

```bash
cd D:/AndroidProject/VelvetCouples
python scripts/check_strings_mix.py      # 8 语言串词 + key 集合一致性；期望 OK / EXIT=0
python scripts/verify_content.py         # 8 类内容回归；期望 ✅ / EXIT=0
python scripts/sl_lint.py                # 情书内容串词；期望 OK
python scripts/dp_lint.py                # 约会计划器内容串词；期望 OK
./gradlew :app:assembleDebug --offline   # 期望 BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest --offline   # 期望 131/131 通过
./gradlew :app:assembleRelease --offline # R8；3.4M；跳过 lint 属预期
```

**改动闸门**：任何触及 `res/values*/strings.xml` → 必跑 #1；任何触及 `assets/content/*/` → 必跑 #2（+ 对应 #3/#4）；任何触及 Java / 布局 → 必跑 #5 #6；新增模式 → 追加 #7 数量断言核对。

**验收证据四件套**：脚本输出截图 / 单测 XML 结果 / 真机截图（注意 FLAG_SECURE）/ 具体文件行号。Phase 4 专家打勾时须附其一。

---

## 8. 检索未命中项与路由建议（交回 team-lead）

按「命中知识直接回答，未命中交回路由」原则，以下 4 项**知识库无既有条目**，不臆造结论：

| # | 未命中内容 | 建议路由 |
|---|---|---|
| M-01 | **「私密调性」成文规范 / 量化判定口径**（羞涩又开放、诱惑暗示、吸引力、刺激感）—— 库内仅有反面案例（`PITFALL-035`）与合规红线（`ADR-015`），**无正例库与分级标准** | `android-architect`（任务 #1 正在编制「私密调性规范」）；产出后请回传供 Phase 6 入库 |
| M-02 | **8 语言「地区用户习惯」判定标准**（ja/ko 敬语、es/fr 阴阳性、pt-BR 用词、zh-TW 用语）—— 库内只有串词/转义等**机器可检规则**，无主观本地化准则 | `android-data`（#6）/ `android-ui`（#5） |
| M-03 | **同类软件当前（2026-11）最新功能清单** —— 库内竞品调研停留在同日第六 / 八轮，无外部最新数据 | `android-architect`（#1）；若需联网调研请 team-lead 裁决 |
| M-04 | **内容合规红线细化 / Play 政策逐条复核**（本轮新增文案是否触碰政策） | `android-security`（#3，P4 最终合规扫描） |

另提示 `android-ui`（#5）/ `android-data`（#6）/ `android-runtime`（#7）/ `android-test`（#8）：**§5 检查清单与 §6 不得重犯清单可直接作为各自任务的风险底稿**，尤其 `PITFALL-008 / 009 / 013 / 024 / 034` 与「四类非缺陷勿误改」。

---

## 9. Phase 6 待办预告（本阶段不执行）

- 将本轮新产生的 **[CHECKLIST-R13]、[TEMPLATE-CHECK]、新增 ADR / PITFALL** 去重、归类、标注后入库（五区：陷阱 / 清单 / 模板 / ADR / FAQ）。
- 追加 `2026-11-23.md` 第十三轮工作日志。
- 复盘：把 `全量审计报告.md §四` 标注为**历史快照（已销账）**，避免后续误引；把 `PLAY_CONSOLE_CONTENT_RATING.md §3` 的 7 项未勾选项登记为**未闭环跟踪项**。
- **任务关闭前确认知识已沉淀**，否则本任务不算闭环。

---

## 10. 交叉核对与知识条目暂存区（应 #3 请求 · Phase 6 待正式入库）

> **触发**：android-security（#3）提交三项 Play 政策时效口径核对请求 + 两条历史陷阱沉淀请求。
> **核实方式**：2026-11-23 第一方来源全文检索（来源 URL 见 §10.4），并与 security 报告 §1.4 逐条比对。
> **阶段门声明**：按 team-lead 指令，Phase 1 **仅做检索与格式化暂存 —— 本节条目尚未正式入库、未追加工作日志**；正式入库（五区归类、去重、标注）与日志追加留 Phase 6。
> **效力边界**：本节只登记「可验证事实 / 口径差异 / 待验证项」，**不含任何合规判定**；Q1/Q2/Q3 判定归属仍为 team-lead / 法务（沿用 security 报告第 5 章）。

### 10.1 三项核对结论总表

| 项 | security 报告口径 | 第一方核实结果 | 差异处置 |
|---|---|---|---|
| **1. 性内容政策** | 2026-10-29 生效；含 `intended to be sexually gratifying`；非自愿性内容禁止；Child Endangerment 即时下架；Child Safety Standards 仅适用 Anonymous/Random Chat/Social/Dating | ✅ **全部确认**（`answer/18258653` 首页即标注 "effective October 29, 2026, unless otherwise stated"；中文 `16933379` 同） | ⚠️ **2 处修正**（见 10.3 A/B）：该措辞**非 2026 新增**；政策全文**无「成年人亲密关系」明文豁免**，且违规示例直接覆盖本项目存量表述 |
| **2. target API** | 2026-08-31 起新应用/更新须 API 36；现有应用 ≥ API 35；可延期至 2026-11-01 | ✅ **与官方完全一致**（`answer/11926878`、`developer.android.com/google/play/requirements/target-sdk`） | ➕ **补充 3 项**：平台例外、后果分层、**无后续延期**（下一窗口 2027-08 API 37） |
| **3. IARC** | 申报不实可致下架/停权；内容/功能变化影响问卷答案须重填；6 地区屏蔽 18+；韩国 GRAC 19+ | ✅ **前 3 条全部确认**（`play.google.com/.../content-ratings`、`answer/9898843`、`answer/9859655`）；6 地区正确 | ⚠️ **2 处精确化**（见 10.3 D/E）：全文与摘要措辞宽严不同；「广告成熟度」条文**专指广告**；6 地区补生效时间点；韩国 GRAC 19+ **仅第三方来源，待核** |

### 10.2 待入库知识条目（标准格式 · 状态：已核实·待 Phase 6 入库）

```
[PITFALL-R13-01]
标题：内容分级口径不一致（17+ vs 18+）导致申报基准不明
领域：发布 / 合规
现象：docs/PLAY_CONSOLE_CONTENT_RATING.md:24 建议 Mature 17+（保守可 18+）；
      audit_report/VelvetCouples_全量审计报告.md:21,129-135 建议 18+/成人内容
触发条件：同时存在两份分级建议文档，且本轮改题需判定「内容变化是否影响问卷答案」
根因：两份文档由不同轮次独立编制，未做口径收敛；首轮审计按「含露骨性暗示+订阅」判 18+，
      第二轮合规文档按 IARC 问卷逐项答复判 Mature 17+
修复：由 team-lead 会同法务 / Play Console 实际问卷结果统一为唯一口径，并同步回写两份文档
预防：凡涉及分级的结论只引用「唯一权威文档」；新增分级建议前先检索既有条目去重
来源任务：TASK-2 / TASK-3（security #3 提交）　标签：分级, IARC, 文档一致性, 发布
最后验证日期：2026-11-23　状态：有效·待入库　关联：security 报告 Q1 / P0-2
```

```
[PITFALL-R13-02]
标题：英文过审 ≠ 8 语言过审（合规词表必须按语言各自全量扫描）
领域：i18n / 合规 / 题库
现象：历史 v21 批次由模板拼接产生露骨病句（"suck softly and kiss on lower belly"），
      同时 ja/ko/es/fr/pt 5 语种本地化不同步（各缺 37 deck + 10 intimate + 27 scratch）、
      zh_tw 有 18 处「脸红」及全库 5,978 处简体异形字遗留
触发条件：只审英文源文，或以英文词表扫描结果推断其它 7 语言合规
根因：翻译批次与扩题批次不同步；简繁/串词问题属语言层缺陷，英文词表无法覆盖；
      合规语义（动作分解、胁迫、未成年暗示）在各语言的表达形式不同
修复：8 语言各自跑合规词表 + 人工抽检；同时跑 check_strings_mix.py / sl_lint.py /
      dp_lint.py / verify_content.py；同一 tier 8 语言强度一致，无对应委婉表达一律降档
预防：把「8 语言各自合规扫描」写进 Phase 4 阻断项（security 报告 A-1 / B-1 已列，本条为历史依据）
来源任务：TASK-2 / TASK-3（security #3 提交）　标签：8语言, 合规词表, v21, 简繁, 串词
最后验证日期：2026-11-23　状态：有效·待入库　关联：PITFALL-001 / 002 / 011 / 012 / 038
```

```
[SPEC-R13-01] 政策时效与引用规范
主题：Google Play 政策引用 / 文档时效
规则：
- Developer Program Policy（answer/18258653）当前生效版本标注 "effective October 29, 2026,
  unless otherwise stated"；中文版 answer/16933379 同。
- 引用性内容政策必须引「完整政策（Full Policy）」而非「政策摘要」：answer/9878810 明示
  「政策摘要和行动要点仅为概览…如果两者有冲突，请以完整政策为准」。
- "or any content or services intended to be sexually gratifying"（中：以性取悦为目的的任何
  内容或服务）**并非 2026 新增措辞**，2022-01-17 版 Developer Policy 已含同句。
- 2026 版相对明确/新增：distribute non-consensual sexual content、性掠夺行为、
  目录类应用（catalog apps）豁免细则、Child Safety Standards 章节与即时下架机制。
- 政策全文**无「成年人亲密关系 / 含蓄性暗示」明文豁免**；裸露豁免仅限 EDSA（教育/纪实/
  科学/艺术，且非无端），另有 catalog apps 豁免。
- 常见违规示例含：functionally sexual aids / "sex guides" / explicit text, adult or sexual
  keywords in the store listing or in-app / sexually suggestive poses / sexual depiction of
  body parts —— 与本项目 tier_desc_3/4、paywall「Kama Positions / 5-Stage Foreplay /
  Body Map」、mode_ladder_desc「nonstop physical foreplay」直接对应。
例外：本条为政策事实登记，**不构成对本 App 是否违规的判定**（Q2 归 team-lead / 法务）。
生效日期：2026-11-23（核验日）　来源任务：TASK-2 / TASK-3　状态：有效·待入库
```

```
[SPEC-R13-02] target API 要求、例外与后果分层
主题：目标 API 级别 / 发布门槛
规则：
- 自 2026-08-31 起：新应用与应用更新须 target Android 16 / API 36+ 才能提交到 Google Play。
- 现有应用须 ≥ API 35 才可继续向「设备 OS 版本高于应用 target API」的新用户提供。
- 可申请延期至 2026-11-01（须在 Play Console 通过延期表单申请，非自动）。
- 分平台例外：Wear OS / Android Automotive OS 须 API 35；Android TV / Android XR 须 API 34。
- 组织例外：永久的专用应用（仅特定组织内部用户、仅供内部分发）不受约束。
- **后果分层（勿混为一谈）**：①「提交门槛」不达 API 36 → 新应用/更新不予发布（阻断）；
  ②「现有应用可见性门槛」不达 API 35 → 限制为仅在 ≤ target API 的设备上可见，
  **不是下架**，已安装用户仍可正常收更新。当前 targetSdk 35 **恰好满足②**。
- **无 2026-11-01 之后的进一步延期窗口**：第一方 Android Developers Blog 已明确
  「2027 年 8 月之前以 API 37 为目标平台」「Google Play 新应用和更新须 target API 37，
  2027 年 8 月强制执行」；developer.android.com 首页另提示 2027-04 起生效的自适应质量要求。
例外：本条为门槛事实登记；「本轮是否构成阻断」取决于是否向 Play Console 提交应用更新，
      归 team-lead / android-build（Q3）。
待验证（第三方，未获第一方确认，不作事实入库）：延期为「一次性、须在原截止前申请、
  不可重复」；升 API 36 触发 edge-to-edge 强制 / predictive back / 后台执行 / 运行时权限变更。
生效日期：2026-11-23（核验日）　来源任务：TASK-2 / TASK-3　状态：有效·待入库
```

```
[SPEC-R13-03] IARC 问卷重填与条文适用范围
主题：内容分级 / 问卷重填判据
规则：
- 所有应用须有 IARC 分级；「未进行内容分级的应用将会从 Play 商店下架」。
- "Misrepresentation of your app's content may result in removal or suspension"。
- answer/9859655 列明须完成问卷的三种情形：新提交应用 / 已在架但缺分级的应用 /
  **所有内容或功能变化会影响问卷答案的应用更新**。
- **措辞宽严差异（重要）**：全文（Full Policy）为「改动**影响问卷答案时**才必须重填」
  （"affect the responses to the questionnaire"）；政策摘要 / Key Considerations 更宽，
  为「keep it updated if content in the app changes」「Submit a new questionnaire
  **whenever** your app's content changes」「内容有变时不要忘记重新填写问卷」。
  → Phase 4/P5 建议采**保守口径（内容一变即重填）**，不得由改写专家自行判断「影响不大」。
- **条文适用范围精确化**：「任何广告在内容成熟度上不得显著高于应用主要内容」是
  **专指广告**的 IARC 条文；「商店文案 / ASO / 截图 / paywall 成熟度」属商店列表与
  Inappropriate Ads 政策 + 既有 ASO 规范（GOOGLE_PLAY_LAUNCH_CHECKLIST.md:10），
  引用时勿把二者混为同一条。
- 地区屏蔽（answer/9859655 中文版，第一方）：**仅限 EEA、澳大利亚、巴西（自 2026-03 起）、
  新加坡（自 2026-03 起）、瑞士、英国**，对被判定为未成年人的用户：禁止获取/购买 18+
  内容、从搜索与浏览页面屏蔽或滤除（深层链接除外）。
- 交叉页 developer.android.com/distribute/age-appropriate-experiences/regional-protections
  （最后更新 2026-11-18）：澳/EEA/新/瑞/英限制未成年人下载 18+；巴西另标
  「自 2027-01 起 Play Age Signals API 强制返回年龄信息（Digital ECA）」并同样限制 18+。
  → 巴西有 2026-03（屏蔽）与 2027-01（Age Signals）两个时间点，**勿混淆**。
- 分级申诉：用认证邮件中的链接直接向 IARC 分级机构提出。
待验证（第三方，未获第一方确认）：韩国 GRAC 19+ 档与 2026-08-12 起 GRAC 分级号覆盖规则、
  2026-10 起韩国轻微成人暗示由 All 升 12+；GRAC 为「韩国游戏」分级机构，
  韩国「应用」为 Google Play Rating。security 报告 §2.5 方向一致，但须注明来源等级。
生效日期：2026-11-23（核验日）　来源任务：TASK-2 / TASK-3　状态：有效·待入库
```

```
[OVERDUE-01] 待标记条目（防止过时口径污染知识库）
对象：docs/PLAY_CONSOLE_CONTENT_RATING.md §2.1「属于政策允许的『成年人亲密关系』范畴」
问题：第一方政策全文**无此明文豁免**，该句为文档作者推断；且 2026-10-29 生效版本
      的常见违规示例覆盖应用内成人关键词 / 性指南 / 身体部位性化描绘。
处置建议（Phase 6 执行，本阶段不改文档）：在知识库中把该结论标记为
      「待复核·不得作为合规依据直接引用」，并挂接 SPEC-R13-01；是否回写文档由 team-lead / 法务裁决。
关联：security 报告 Q2 / P0-1；PITFALL-R13-01（分级口径不一致）
最后验证日期：2026-11-23　状态：待标记
```

```
[CHECKLIST-R13-ADD] 本轮检查清单补充项（并入 §5，Phase 4 复用）
- [ ] 【需求 3/7】§2.1「含蓄亲密属政策允许范畴」已由 team-lead / 法务重新评估（Q2），
      未确认前不得作为改写依据 —— 关联 OVERDUE-01 / SPEC-R13-01
- [ ] 【需求 3/5/7】8 语言**各自**跑合规词表，不以英文扫描结果推断其它语言 —— PITFALL-R13-02
- [ ] 【发布】17+ / 18+ 已统一为唯一口径并回写两份文档（Q1）—— PITFALL-R13-01
- [ ] 【发布】本轮内容改动已按保守口径判定是否重填 IARC 问卷（Q7）—— SPEC-R13-03
- [ ] 【构建】targetSdk 35 → 36 排期已确认，且已评估 2027-04 / 2027-08 后续窗口（Q3）—— SPEC-R13-02
最后更新日期：2026-11-23
```

### 10.3 需回传 android-security 的口径修正 / 精确化（5 处）

| # | 类型 | 内容 |
|---|---|---|
| **A** | ⚠️ 修正 | `intended to be sexually gratifying` **并非 2026-10-29 新增/收紧措辞**，2022-01-17 版 Developer Policy 已含同句。故「2026 政策收紧导致旧文档过时」的表述不准确；准确说法是**该措辞长期生效，docs §2.1 从一开始即为推断而非政策明文**。建议 P0-1 措辞相应调整（风险结论不变，甚至更强） |
| **B** | ➕ 强化证据 | 政策「常见违规示例」含 **"sex guides"**、**"explicit text, adult/sexual keywords in the store listing or in-app"**、性暗示姿势/身体部位性化描绘、物化类应用；政策全文**无「成年人亲密关系」明文豁免**（仅 EDSA 裸露 + catalog apps）。这直接对应 2.4.3 的 paywall「Kama Positions / 5-Stage Foreplay / Body Map」与 `tier_desc_3/4`——**Q2 的风险提示证据比现报告更充分** |
| **C** | ➕ 补充 | target API：① 例外＝Wear/Automotive(35)、TV/XR(34)、永久专用应用；② **后果分层**：API 36 是「提交门槛」，API 35 是「现有应用可见性门槛」（当前 35 已满足，不达标是限制可见而非下架）；③ **未发现 2026-11-01 之后的延期**，下一窗口为 2027-08 API 37（第一方博客），另有 2027-04 自适应质量要求 |
| **D** | ⚠️ 精确化 | IARC 重填判据存在**全文 vs 摘要宽严差异**：全文「影响问卷答案才重填」，摘要/Key Considerations「内容一变即重填」。D3 采保守口径合理，建议注明依据；另「广告成熟度不得高于应用内容」是**专指广告**的条文，D4 延伸到 ASO/截图/paywall 建议改注为商店列表政策 + 既有 ASO 规范，避免误引 |
| **E** | ➕ 补充 | 6 地区屏蔽完全正确，补两点：**巴西、新加坡自 2026-03 起**纳入（answer/9859655）；巴西另有 **2027-01 Age Signals API 强制**（regional-protections，更新 2026-11-18），勿与 2026-03 混淆。**韩国 GRAC 19+ 仅第三方来源**，未获第一方确认，建议 §1.4 / §2.5 标注来源等级或交法务核 |

### 10.4 第一方来源索引（核验日 2026-11-23）

| 主题 | URL |
|---|---|
| Developer Program Policy（生效 2026-10-29） | `support.google.com/googleplay/android-developer/answer/18258653` |
| 同上·中文版 | `support.google.com/googleplay/android-developer/answer/16933379` |
| 不当内容（政策摘要 + 完整政策，含中文） | `support.google.com/googleplay/android-developer/answer/9878810` |
| 目标 API 级别要求 | `support.google.com/googleplay/android-developer/answer/11926878` |
| 同上·Android Developers | `developer.android.com/google/play/requirements/target-sdk` |
| 内容分级（IARC，含 Key Considerations） | `play.google.com/intl/ja/about/storelisting-promotional/content-ratings` |
| 内容分级·帮助中心 | `support.google.com/googleplay/android-developer/answer/9898843` |
| 应用/游戏/广告内容分级要求（含地区屏蔽） | `support.google.com/googleplay/android-developer/answer/9859655` |
| 区域性年龄保护（更新 2026-11-18） | `developer.android.com/distribute/age-appropriate-experiences/regional-protections` |
| Android 17 / API 37（2027-08 预告） | `developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17` |

> 第三方来源（2022 版 Developer Policy PDF、targetsdk.com、entalogics、ecorpit、12apptesting、censuus、mangodeveloper）**仅用于交叉参考**，其中未经第一方确认的结论已在 §10.2 明确标为「待验证」，**未作为事实入库**。

---

> 编制人：【识识】　｜　状态：Phase 1 检索完成 + §10 交叉核对暂存（Phase 6 待正式入库）　｜　未修改任何工程文件、未追加工作日志

# Phase 3 任务卡尾部（android-data / 源源）

> 前文任务卡在「你的」处被截断。本文件是**完整剩余指令**，必须与主理人前发任务卡合起来执行。

---

## 0. 关键事实：低命中率是词表漏检造成的假象（**先读，避免误判**）

我用探针脚本 `scripts/probe_risk.py`（只读，主理人已建）统计各语言高危动作词命中率：

```
zh 9.6% | ko 11.3% | zh_tw 9.4% | fr 8.1% | en 5.7% | ja 1.6% | es 0.2% | pt 0.1%
```

**不要据此认为 pt/es/ja 已合规而跳过它们。** 我逐条比对同一内容 ID 后确认：**8 语言的实际露骨度基本一致**。同一 `extreme_modes.json` 的 `pr_ice_1`：

| 语言 | 原文 |
|---|---|
| zh | 用指尖或嘴唇夹起一块冰，顺着伴侣颈侧与锁骨缓慢滑过——紧接着立刻用炙热的吻追着冰痕融化。 |
| en | Hold an ice cube between your lips or fingertips and glide it slowly down your partner's neck and collarbone—immediately… |
| ko | 얼음 조각을 손끝이나 입술로 집어 파트너의 목덜미와 쇄골을 천천히 미끄러뜨린 직후, 뜨거운 키스로 차가움을 녹여주세요. |
| pt | Segure um cubo de gelo nos labios ou dedos e deslize pelo pescoco e clavicula — aquecendo logo atras com beijos ardentes |
| es | Sujeta un cubito de hielo con labios o dedos y deslizalo por cuello y clavicula — persiguiendo el frio al instante |
| ja | 氷を指か唇で挟み、パートナーの首筋から鎖骨へゆっくり滑らせて——直後に熱いキスで冷たさを溶かして。 |

**结论**：低命中率只是词表对 es/pt/ja 覆盖不足造成的漏检。**你必须自己建立覆盖完整的 8 语言高危词表再判定，不得沿用任何现成结论。**

---

## 1. 任务一：8 语言完整高危词表 + 全量机器定位

在 `scripts/` 下新建你自己的脚本（建议 `scripts/gen_content_tone_audit.py`；若与 build 的 `scripts/check_compliance.py` 功能重叠，以**你的**为准并让 build 那边复用，不要各自造两份）：

1. **8 语言完整高危词表**：每语言独立词表，覆盖 RED-A2 动作分解、RED-A3 胁迫与惩罚、RED-A7 意图满足性欲、RED-E 地区高危词（安安 2.3.4 已给出 ja/ko/zh-TW/es/fr/pt 的高危词清单，**你要补全并验证**）。必须含动词变位、复合词、**否定式豁免**（安安 3.2 要求区分肯定式与否定式）。
2. **全量扫描** 8 语言 × 13 类 JSON 的每一个字符串字段，输出命中清单（文件 / JSON 路径 / 字段 / 原文 / 命中词 / 建议档位）。
3. **调性信号统计**：每语言命中 P1~P7 特征的比例、命中 N1~N7 反面的条目、tier 与梯度是否单调（架架 **AC-703** 梯度单调递增校验）。
4. **题目质量维度**（需求 5）：每条题目是否与所属 mode/tier 语义相符、是否存在开放题却配了假答案假揭晓（历史第七轮已移除 CHOICE，**禁止恢复**）、题干隐含时长与 `durationSec` 是否一致、是否存在重复或近似重复条目、占位符是否未替换。

---

## 2. 任务二：题库降调与本地化改写（需求 5 / 6 / 7）

### 2.1 规模现实与分层策略（主理人给定，请照此执行）

每语言约 9,900 条、8 语言合计 **79,772 条**。**不可能逐条人工通读改写**，也不允许抽样敷衍（安安要求「机器扫描英语通过不等于其它语言通过」）。按三层推进：

**L1 · 机器全量定位（8 语言 × 全部字段）**
用任务一的脚本产出精确命中清单，这是不可跳过的地基。

**L2 · 命中项精修（8 语言全量，逐条改写）**
L1 命中清单里的每一条都必须被改写。**高风险区块优先级**（按安安 2.4.3 + 实测分布）：
- `extreme_modes.json`：`positions.setup` / `positions.tip` / `foreplayStages.instruction` / `foreplayStages.stageTitle` / `propChallenges.text` / `connect4Actions.fullDare` / `connect4Actions.shortLabel` / `ladderSteps.foreplayAction`
- `intimate_modes.json`：`heartbeatPrompts.text` / `bombRelays.text` / `blackjackStakes.label` / `bodyMapZones.normalAction` / `hotAction` / `duoAction` / `auctionContracts.title` / `auctionContracts.text`
- `decks.json`：tier 3/4 卡的 text / setup / tip
- `scratch_cards.json` / `party_duel.json` / `roleplay_scenarios.json` / `fate_cubes.json` / `draw_words.json` / `generator_templates.json` / `couple_quiz.json` 中的 tier 3/4 条目

**L3 · 未命中项抽样复核（每语言 ≥30 条，含各梯度）**
按架架 4.6 的人工分层抽样要求，验证机器词表没漏。抽样报告要写清：抽样方法、命中/未命中数、发现的漏检词（补进词表）。

### 2.2 改写方向（安安 RED-C 铁律 + 架架调性规范）

**刺激感只能由情绪密度、节奏与留白实现，绝不由动作清单实现。** 六种允许手法：含蓄留白 / 感官意象 / 悬念 / 邀约式语气 / 心跳感 / 双人共同视角。

- **T3/T4 必须命中 P7 边界语义**（「你说停就停」「只要你愿意」），否则一票退回（架架 CHK-6）。
- **同 tier 的 8 语言强度必须一致，任何语言不得强于英文源文**（安安 RED-C-0）。若某语言缺乏委婉表达，**一律降一档**，不允许为凑刺激感直译露骨词。
- **降调不是变无聊**：架架的 T4 好例子「夜已经很静了…分不清是谁的心跳先乱的。再近一点——只要你愿意。」比存量英文 "bold physical punishments, playful spanks & pinning hands" 更有诱惑力且零露骨。**这是你改写的范式基准**。
- **N6 问卷腔也要改**：架架抽样发现 `couple_quiz` 的「What is {p1}'s favorite way to be kissed when nobody is watching?」属信息询问、缺 P1 留白与 P5 心跳感 → 改写方向是**补留白与悬念**，不是删问题。

### 2.3 8 语言本地化专项（需求 6）

- **fr**：撇号 `'` 在 strings.xml 必须 `\'`（JSON 里不受此限）；用 tu 不用 vous；禁直白 `sexy`；**`punition` 比英文 `punishment` 更刺眼，必须删**。
- **pt-BR**：必须 pt-BR 变体（você / a gente / pra / só mais um pouquinho），禁 pt-PT 用词（telemóvel / rapariga）；禁 `preliminares` / `submissão` / `castigo` / `lamber` / `imobilizar` / `bêbado`。
- **es**：用 tú 不用 usted；**禁 `coger`**（部分拉美地区有性含义）；禁 `castigo` / `sumisión` / `dominación` / `obligar` / `lamer` / `inmóvil`；强制祈使式全改条件式。
- **ja**：用「気配 / 息 / 間 / ふたり / 〜してもいい？」等留白婉曲；禁英文 `foreplay`/`positions` 的片假名直译；禁 `体位`/`前戯`/`舐める`/`押さえつける`/`罰`/`無理やり`；禁命令形 `〜しろ`；**禁简体专用字串混入**（历史规则：连续 3 个简体专用字才报警）。
- **ko**：用「설렘 / 숨 / 간격 / 우리 / ~해도 될까?」；禁 `전희`/`체위`/`핥다`/`제압`/`벌`/`억지로`；해라체命令全改해요체；**出现任何汉字/假名即判串词**；`동의·거부` 措辞要清晰以呼应边界页。
- **zh-TW**：用「靠近 / 心跳 / 先停下來好嗎」；禁 `前戲`/`體位`/`懲罰`/`舔`/`按住`/`制服`；**严禁简体专用字混入**；避免大陆用语（对象→對象、靠谱→可靠、视频→影片）。
- **zh-CN**：最严 —— 只保留「氛围 + 情绪 + 双人 + 可停」；避免任何「前戏/体位/惩罚」类词；「诱惑」靠灯光、呼吸、留白，不靠动作清单。
- **en**：作为源语言，**先过词表再启动翻译**，不得先翻译后审查。

### 2.4 隐私叙事（安安 RED-P）

不得新增任何 `sync / synchronized / 同步 / 同期 / 동기화 / синхронизация` 动词描述数据流动。**本轮 8 组 strings 降调由【悠悠】负责（`res/values*/strings.xml`），不在你的范围**；但**内容 JSON 里若存在同类「同步」表述，你也要改**。

---

## 3. 任务三：新增 A 项的抽卡引擎（不新增题库 JSON）

按架构规格 `round13_phase3_architect_spec.md` §1.2~§1.6、§6、§7 的 `[API-008]`：
- 新建纯 Java 引擎（建议 `core/session/DateNightSessionEngine.java`，**无 Android 依赖**，便于单测）。
- 4 阶段 Phase 机：`WARM_UP → DEEPEN → PEAK → CLOSING`。
- 每阶段抽卡：复用 `core/engine/SocialGamesEngine` 的防重复机制（recent shield = 12、MIN_AVAILABLE = 4）、`core/engine/ConsentFilter`（任一方 HARD_NO 全局阻断）、tag 偏好。
- 池空回退：至少 4 张可用卡兜底，**禁止返回 null**（沿用架架 [API-001] 契约）。
- **不新增 GameModeCatalog 项**（模式数保持 36，避免 Phase10/Phase12 硬编码断言回归）；只新增 1 个纯显示标签 `mode_date_night`，并在 `core/i18n/TagDisplayMapper.java` 注册（避免第八轮「fallback 显示情侣棋局」坑）。
- **PEAK 阶段不设倒计时**（主理人裁决，避免计时器把节奏变任务、与 N3 压迫感冲突）。
- 会话结束接 `recordSessionStat("DATE_NIGHT", …)`。
- **A 项不新增任何题库 JSON**（架构师明确：复用既有内容池）。

**引擎只管抽卡与阶段推进，不产生任何文案**（文案由【悠悠】写 strings，你只调 key）。

---

## 4. 任务四：Q6 —— Keystore 加密失败降级路径

主理人裁决 Q6 纳入本轮，但**技术方案由【安安】在 Phase 4 出**。你这一轮先只做**现状登记与风险标注**：
- 读 `core/letter/LetterCrypto.java`（`encrypt()` 的 catch 分支）与 `core/letter/SealedLetterManager.java` 的调用方，确认「加密失败即明文落盘」的确切代码路径与行号。
- 在你的 Phase 3 报告里单列一节「Q6 现状登记」，写清：确切文件与行号、失败触发条件、当前行为（明文落盘）、影响面（用户写了多少封信会受影响）。
- **本轮不要改这个降级路径的代码**（等安安出方案后，在 Phase 4 返工时落地）。除非安安已经给出方案 —— 若安安已发来方案，则按方案落地并补单测。

---

## 5. 硬约束

- 权限集合恒为 `{android.permission.VIBRATE}`；不得引入网络 / 账号 / 地图 SDK / 位置 / 第三方 SDK。
- **不得改 `res/layout/**` 与 `res/values*/strings.xml`**（【悠悠】独占）。
- **不得改 `scripts/verify_content.py` / `check_strings_mix.py` / `sl_lint.py` / `dp_lint.py`**（build/test 独占）；你只写自己新增的脚本。
- 不得新增或修改 `GameModeCatalog` 模式数量（保持 36）。
- 改内容 JSON 必须 **8 语言 ID 集合完全一致**（en 为基准），改完跑 `verify_content.py` 必须 EXIT=0（历史坑 `ID_MISSING=20`）。
- 新增/修改文案必须跑 `check_strings_mix.py`（内容层用你的脚本 + 该脚本双重把关）EXIT=0。
- **禁止给开放式题目硬套假答案 + 假揭晓**（历史第七轮已移除 CHOICE，禁止恢复）。
- **禁止出现未成年人暗示**（安安 RED-A1，8 语言零容忍）。
- 不得把「用户 / 请点击 / 系统 / 设置 / 时长 N 秒」等 UI 操作语写进内容（架架 H-8 / N4）。

## 6. 交付要求

- 完整产出写入 `D:\AndroidProject\VelvetCouples\audit_report\round13_phase3_data.md`（新建）。
- 报告必须**引用 AC 编号与 P/N/T/RED 编号**（架架 [API-007] 硬要求），并给**精确数字**：
  - 8 语言 × 13 类 JSON 的扫描覆盖率（字段数 / 命中数 / 改写数，**分语言分文件**）
  - L2 命中项改写完成率（分语言），目标 **100%**
  - L3 抽样数与漏检词数
  - 调性统计：各语言 P 特征命中率、N 反面剩余数（目标 0）、梯度单调性通过率
  - 题目质量：文不对题 / 假答案 / 歧义 / 重复的条数与处理结果
- 完成后用 SendMessage 把 400 字以内精简摘要发给 `team-lead`。
- 全程使用简体中文。

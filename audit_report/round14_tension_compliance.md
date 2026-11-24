# VelvetCouples Round 14 张力边界仲裁报告

> **轮次**：Round 14（新增需求 5：性张力 / 情侣私密氛围的合规边界仲裁）
> **任务**：主理人新增任务卡——张力与越界分界线（8 语言对照）/ Play 政策口径复核 / 当前越界量事实核验 / 张力增强合规门禁
> **编制**：安全与隐私专家 · 安安（android-security）
> **核验时刻**：2026-11-24（工作区仍为【源源】/【悠悠】并发写入中间态）
> **写权限声明**：只写本文件；未改任何代码、脚本、strings.xml、内容 JSON。
> **不推翻红线**：本报告完全遵循 `round13_phase1_security.md` 的 RED-A/B/C/D/E/P/T，不修改任何一条。

---

## 0. 接受主理人裁决（并说明我为何不持异议）

主理人裁决「性张力 ≠ 露骨动作，红线不阻挡张力；架架在红线内做张力最大化」——**我完全接受，且这不是妥协**。

我在 Phase 1 §2.3.1 给出的六种允许手法（含蓄留白、感官意象、悬念、邀约式语气、心跳感、双人视角）**本身就是张力的技术定义**；§2.3.1 铁律「强度升级只能靠情绪密度、节奏与留白，不能靠动作清单」正是张力规范的核心。因此：

- **张力是我的红线内部的事，不是红线之外的事**。用户第 5 项需求与合规不冲突——冲突只发生在有人把"张力"误解为"露骨动作升级"时。
- **边界共识语义（"你说停就停"）确是张力放大器**：它制造"可进可退的悬置感"，同时满足 RED-B3。这与主理人判断一致。
- 我唯一坚持的：**张力的载体是情绪与留白，不是身体动作清单**。这条线以下，张力可以做到很强；这条线以上，一律一票否决（见 §4 门禁规则）。

---

## 1. 张力与越界分界线：8 语言对照（每类 2~3 组）

> 框架沿用 Phase 1 §2.3.2 的「允许 / 越界」对照。四类张力型表达：感官张力 / 私密空间张力 / 边界共识张力 / 邀约式张力。
> 非中英语言给该语言的**允许例句 + 越界例句 + 判定原则**（不逐条穷举，原则见 §1.5）。

### 1.1 感官张力（应允许）vs 动作分解（禁止）

| 语言 | ✅ 允许（感官留白） | ❌ 越界（动作分解） |
|---|---|---|
| 中文 | 「呼吸交叠的那一刻，谁都没说话。」/「我的心跳先乱了一拍。」 | 「舔舐/吸吮耳垂」「手指滑过锁骨」 |
| English | "Our breaths met and neither of us spoke." / "My heartbeat gave me away first." | "Licking your earlobe" / "sliding fingers down your collarbone" |
| 日本語 | 「息が重なった瞬間、二人とも黙った。」/「先に鼓動が乱れたのは、たぶん私。」 | 「耳たぶを舐める」「鎖骨を指でなぞる」 |
| 한국어 | "숨이 겹친 순간, 아무 말도 없었어요." / "먼저 심장이 뛴 건 나였어요." | "귓불을 핥다" / "쇄골을 손끝으로 쓸다" |
| Español | "Nuestras respiraciones se encontraron y ninguno habló." | "Lamer el lóbulo de la oreja" / "deslizar por la clavícula" |
| Français | "Nos souffles se sont rejoints, et personne n'a parlé." | "Lécher le lobe de l'oreille" / "glisser sur la clavicule" |
| Português | "Nossas respirações se encontraram e ninguém falou." | "Lamber a orelha" / "deslizar pela clavícula" |
| 繁中 | 「呼吸交疊的那一刻，誰都沒說話。」 | 「舔舐耳垂」「手指滑過鎖骨」 |

**判定原则**：写"呼吸/心跳/沉默/距离变近"这类**状态**=允许；写"用身体部位对另一身体部位做了什么动作"=越界。

### 1.2 私密空间张力（应允许）vs 公开场合暴露（禁止）

| 语言 | ✅ 允许（双人私密空间） | ❌ 越界（公开暴露/第三人） |
|---|---|---|
| 中文 | 「灯暗下来，只剩下我们两个人。」/「这件事只有我们知道。」 | 「在有人经过的地方做」「让别人看见/拍下来」 |
| English | "The lights went low and it was just the two of us." / "Only we know about this." | "Do it where people can see" / "let someone watch / film it" |
| 日本語 | 「灯りが落ちて、二人だけになった。」/「これは二人だけの秘密。」 | 「人のいる場所で」「誰かに見せて/撮って」 |
| 한국어 | "불이 꺼지고 우리 둘만 남았어요." / "이건 우리만 아는 비밀이에요." | "사람 있는 곳에서" / "다른 사람에게 보여주며 찍기" |
| Español | "Se apagaron las luces y solo estábamos los dos." | "Hacerlo donde puedan vernos" / "dejar que alguien mire/grabe" |
| Français | "Les lumières ont baissé, il n'y avait plus que nous deux." | "Le faire où l'on peut nous voir" / "laisser quelqu'un regarder/filmer" |
| Português | "As luzes baixaram e só estávamos nós dois." | "Fazer onde possam ver" / "deixar alguém assistir/filmar" |
| 繁中 | 「燈暗下來，只剩下我們兩個人。」 | 「在有人經過的地方」「讓別人看見/拍下來」 |

**判定原则**：封闭双人空间+保密=允许（强化私密感）；引入观众/公开场合/拍摄/第三人=违反 RED-A6（诱导分享私密影像）+ RED-B5（不引入第三人）。

### 1.3 边界共识张力（应允许且鼓励）vs 绕过 Hard No（禁止）

| 语言 | ✅ 允许（可停=张力放大） | ❌ 越界（绕过否决） |
|---|---|---|
| 中文 | 「你说停，我们就停。」/「今晚你说了算，不想要就跳过。」 | 「别管什么边界」「偷偷试一下，TA 不会知道」 |
| English | "Say the word and we stop." / "You set the pace tonight — skip anything you don't want." | "Ignore the boundaries" / "try it secretly, they won't know" |
| 日本語 | 「止めてと言ったら、止める。」/「今夜はあなたのペースで。」 | 「境界は無視して」「こっそり試して、相手に内緒で」 |
| 한국어 | "멈춰달라 하면 멈출게요." / "오늘은 네 페이스대로, 싫은 건 건너뛰자." | "경계를 무시하고" / "몰래 해서 상대방한테 비밀로" |
| Español | "Si dices alto, paramos." / "Tú marcas el ritmo; salta lo que no quieras." | "Ignora los límites" / "pruébalo a escondidas, no se enterará" |
| Français | "Si tu dis stop, on s'arrête." / "C'est toi qui décides ce soir." | "Ignore les limites" / "essaie en cachette, elle ne saura pas" |
| Português | "Se você disser pare, a gente para." / "Você define o ritmo, pule o que não quiser." | "Ignore os limites" / "tente escondido, ela não vai saber" |
| 繁中 | 「你說停，我們就停。」/「今晚你說了算，不想要就跳過。」 | 「別管什麼邊界」「偷偷試，TA 不會知道」 |

**判定原则**：把"可随时停止"写进文案=RED-B3 合规且**增强**张力（悬置感）；任何教唆忽略 Hard No / 隐瞒 / 偷偷进行=RED-B4 一票否决。

### 1.4 邀约式张力（应允许）vs 命令式压迫（禁止）

| 语言 | ✅ 允许（邀约/挑衅式） | ❌ 越界（命令/压迫） |
|---|---|---|
| 中文 | 「敢不敢再靠近一点？」/「要不要把今晚交给我安排？」 | 「必须照做」「不许拒绝」「快一点」 |
| English | "Dare you come a little closer?" / "Want me to plan tonight for us?" | "Do it now / you must / don't refuse / hurry up" |
| 日本語 | 「もう少し近づいてみる？」/「今夜は私に任せてくれる？」 | 「今すぐして」「拒否は許さない」「早くして」 |
| 한국어 | "조금 더 가까이 해볼래?" / "오늘 밤은 나에게 맡겨줄래?" | "지금 당장 해" / "거부 못 해" / "빨리" |
| Español | "¿Te atreves a acercarte un poco más?" / "¿Me dejas planear la noche?" | "Hazlo ya / debes / no puedes negarte" |
| Français | "Oses-tu venir un peu plus près ?" / "Tu me laisses choisir ce soir ?" | "Fais-le maintenant / tu dois / pas le droit de refuser" |
| Português | "Você ousa chegar mais perto?" / "Me deixa planejar a noite?" | "Faça agora / tem que / não pode recusar" |
| 繁中 | 「敢不敢再靠近一點？」/「要不要把今晚交給我安排？」 | 「必須照做」「不許拒絕」「快一點」 |

**判定原则**：疑问句/条件句/"敢不敢"/"要不要"=邀约张力（双向、可拒绝）；祈使句/强制词/"必须/不许/dare not refuse"=RED-A3 胁迫，一票否决。

### 1.5 跨语言总则

- 8 语言**同一句张力表达强度必须一致**，不得因语言不同而"某语言更露骨"（RED-C-0）。
- 每类允许句式的共同点：**状态描写（呼吸/心跳/距离/灯暗）、疑问或条件语气、双向主语、"可停"出口**。
- 每类越界句式的共同点：**具体身体动作、第三人/公开、强制语态、绕过边界**。

---

## 2. Play 政策口径复核（"性张力"是否提高触碰 sexually gratifying 条款风险）

> **标注：以下为技术侧专业判断，非法务意见。** 最终合规判定交主理人 / 法务。

### 2.1 判断

**含蓄的感官张力本身不提高触碰"any content or services intended to be sexually gratifying"条款的风险；提高风险的是"把张力做成性行为的 substitutes（替代品）"。**

理由：

1. 政策禁止的是**意图满足性欲的内容/服务**（intended to be sexually gratifying）——判据是"内容的功能/目的"，不是"是否让人心动"。呼吸交叠、心跳、留白、邀约属于情绪/浪漫表达，其功能是情感升温与氛围营造，不是性释放。
2. 我在 Phase 1 允许的六种手法产出的文本**止于亲吻/轻触的暗示**，与政策禁止的"露骨色情/性行为描写/性满足服务"有清晰界限。
3. **真正的风险点不在"张力"，而在"标签与包装"**：如 paywall 曾用 `Kama Positions / 5-Stage Foreplay / 体位ルーレット`——这类**明确指向性行为技法**的功能命名，才可能被认定为"intended to be sexually gratifying"。本轮已改为 `Intimacy Roulette / 5-Stage Heat-Up / 亲密默契轮盘 / 5 段升温夜`，风险显著下降。
4. **残留风险**：题库 tier3/4 的 `pr_ice_1`（滑过锁骨+炙热吻追）等动作化文案 + 5 火焰"极端身体探索"定位 + `bite_lick` 标签——这些不是"含蓄张力"，而是**动作描写**，才真正触碰条款边缘。**这部分必须在 P5 前降调完成。**

### 2.2 商店文案定位调整建议（进一步降风险）

| 项 | 建议 | 理由 |
|---|---|---|
| App 定位措辞 | 维持"情侣私密互动桌游 / couples date-night board game"，**不用**"性张力/诱惑工具/成人游戏"作商店主定位 | 张力是 App 内体验，不是商店卖点；商店用"浪漫/私密/桌游"更安全 |
| Short Description | 沿用现行"Offline 2-player date night board game…"，**不加入** suggestive 词 | ASO 已合规，勿回退 |
| 功能命名 | 已改 `Intimacy Roulette / Heat-Up`，保持；**禁止**回退 Foreplay/Kama/体位 | 见 2.1.3 |
| 分级 | 按 18+ 申报，问卷性暗示勾 Suggestive/Mild（若 tier3/4 降调到位） | 如实申报+高分级=双重保险 |
| 截图 | 保持无露骨图，用氛围/桌游画面 | 政策要求截图成熟度≤内容 |

**结论**：只要 tier3/4 题库降调到位、商店文案不含性技法词，含蓄感官张力**可接受**，不必因用户第 5 项需求而整体收紧红线。

---

## 3. 当前落地状态事实核验（独立抽验，不粉饰）

> 主理人实测：zh/decks.json 仍残留「轻咬」115 /「深吻」80 /「跨坐」26 /「惩罚」4 /「褪衣」2；extreme_modes/intimate_modes 同有大量动作化文案。**我独立复核确认。**

### 3.1 官方门禁脚本扫描（2026-11-24，中间态）

- 扫描量：strings **5472 条** / 内容 JSON **104 文件 139633 字符串**。
- BLOCK **1467**（strings 50 / 内容 1417）、REVIEW **1339**、串词回归 0。
- 内容层按语言的 RED-A2（露骨动作）命中：

| 语言 | RED-A2 官方命中 | 自建词表命中 | 说明 |
|---|---|---|---|
| en | 69 | 1045 | 词表覆盖差 15× |
| zh-CN | **2** | 1140 | **漏检**，主理人实测「轻咬/深吻/跨坐」未进词表 |
| zh-TW | **1** | 1107 | **漏检** |
| ja | 0（RED-A2）/ 169（RED-A3） | 2047 | **漏检**，冰/锁骨/キス 未收录 |
| ko | 97 | 1718 | 部分命中 |
| es | 23 | 1163 | **漏检** |
| fr | 97 | 1182 | 部分命中 |
| pt-BR | **7** | 1131 | **漏检** |

### 3.2 关键实证：`pr_ice_1` 八语言越界且多数漏检

`extreme_modes.json` 的 `pr_ice_1` 在**全部 8 语言**为同一动作化内容：用嘴唇/指尖夹冰 → 沿颈侧锁骨滑过 → 炙热吻追冰痕。官方脚本仅对 en/ko/fr 命中 RED-A2，对 **zh-CN/zh-TW/ja/es/pt-BR 五语言漏检**。根因是词表只收"舔/吸/脱衣"，未收"滑过+锁骨+炙热吻"意象族。**低命中率（pt 7 / zh 2）是词表假象，不是内容更含蓄。**

### 3.3 属于 Phase 1 §2.4.3 存量高风险清单的条目（必须清除）

| 文件/ID | 原文节选 | RED |
|---|---|---|
| `zh/extreme_modes.json:pr_ice_1` | 「颈侧与锁骨缓慢滑过——炙热的吻追着冰痕融化」 | A2 |
| `en/intimate_modes.json` | "Licking & nibbling from neck downward" | A2 |
| `zh/decks.json`（源源返工中） | 「轻咬」115 /「深吻」80 /「跨坐」26 /「惩罚」4 /「褪衣」2 | A2+A3 |
| `extreme_modes.json`/`intimate_modes.json` tier3/4 | 大量动作化文案 | A2/A3/C |

> **strings 层 50 条 BLOCK 为误报**（`agegate_decline="I am under 18"`、`按住屏幕`、`Démineur`扫雷、韩语 `질`→질문/문지르、`공유`=共享列表、`ほろ酔い`=微醺），**不要求改写**。

---

## 4. 张力增强的合规门禁规则（给源源与悠悠，可直接落 `check_compliance.py`）

> 规则与 Phase 1 红线同源，供筑筑把"张力增强"落地为可判定门禁。

### 4.1 一票否决（出现即判违规，不得通过）

| 类别 | 中/英示例 | 8 语言判定词族 |
|---|---|---|
| **动作清单** | 「滑过锁骨」「舔舐」「Licking from neck downward」 | 滑过/缓慢滑/游走/描摹 · glide/slide/trail · 滑らせ/なぞ · desliz/glissez · 미끄러뜨리 |
| **部位直指+动作** | 「用指尖/嘴唇夹…」「fingers down your…」 | 指尖/嘴唇夹 · neck/collarbone + 动作动词 |
| **露骨/器物** | 「性器」「跨坐」「褪衣」「foreplay/positions」 | 舔/吸/吮/深喉/性器 · lick/suck/foreplay · 舐/乳首/体位 · 전희/체위 · lamer/preliminares |
| **强制语态** | 「必须/不许拒绝/按住对方」 | must/don't refuse/pin · 强制/胁迫/惩罚 · 억지로/제압 · obligar/castigo |
| **第三人/公开/拍摄** | 「让别人看/拍下来」 | 邀请第三人 · invite a third party · 誰かに見せ/撮 · 공개 · grabar |
| **绕过边界** | 「别管边界/偷偷试」 | ignore boundaries · 境界無視 · 경계 무시 · ignorar límites |

### 4.2 可以做（张力增强的正当路径）

- **情绪密度**：呼吸、心跳、屏息、沉默、视线、脸红、时间变慢。
- **感官意象（非动作）**：灯光变暗、距离变近、声音变轻、温度、气息——只写"状态"，不写"动作施加"。
- **留白与悬念**：省略号、"下一句留到灯暗时"、未完成的问句。
- **双向句式**："我们""彼此""谁先……"——主语是"两个人"，不是"A 对 B 施加"。
- **邀约式**："敢不敢""要不要""你说了算""你说停就停"——疑问/条件语气。
- **私密空间意象**："只剩我们""灯暗下来""这是我们的秘密"——封闭双人空间。

### 4.3 判定口诀（贴给改写者）

> **把这句话念出来：描述的是"两个成年人之间的情绪、距离与邀请"→ 允许（张力）；描述的是"对身体做了什么动作 / 让谁来看 / 不许拒绝"→ 一票否决。**

### 4.4 落地到 `check_compliance.py` 的建议（交筑筑）

1. **补漏检词族**（最高优先）：为 zh-CN/zh-TW/ja/es/pt-BR 的 `_A2` 增加"滑过/锁骨/炙热吻"意象族正则（见 4.1 第一行）；这是当前最大缺口。
2. **修误报**（避免误伤合规文案）：
   - `agegate_*` 系列整体豁免 RED-A1（`under 18` 是门禁必需）；
   - zh「按住屏幕/按住这里」豁免 RED-A3 `按住`；
   - fr `Démineur` 豁免 RED-A1 `mineur`；
   - ko `질` 正则加词边界，排除 `질문/가질/문지르`；
   - `consent_review_together=공유 목록 보기` 豁免 RED-P `공유`；
   - ja `ほろ酔い` 豁免 RED-A3 `酔い`（T1 微醺档位名）。
3. **保留否定式豁免**：`绝不上传/sin nube/外部送信ゼロ` 等正向承诺继续放行（Phase1 §3.2）。
4. **新增"张力标记"**：建议给允许句打 `tension_ok` 标签（review 级不阻断），便于区分"氛围词"与"越界词"，避免暧昧/挑逗等氛围词被误判阻断。

---

## 5. 结论

1. **主理人裁决成立**：张力与红线不冲突，六种允许手法即张力来源；边界共识语义是张力放大器。我无异议。
2. **Play 政策风险**：含蓄感官张力不提高触碰 sexually gratifying 条款风险；真正风险在动作化题库（pr_ice_1 类）与性技法功能命名（已改），须确保 tier3/4 降调到位。
3. **当前越界量**：官方门禁 BLOCK 1467（内容 1417），但存在**重大漏检**——自建词表证实 zh-CN(zh 2 vs 1140)、pt-BR(7 vs 1131)、ja(0 vs 2047) 等 5 语言低命中是词表假象；`pr_ice_1` 八语言一致越界。**内容层降调未完成是发布阻断项。**
4. **门禁规则**：给出 6 类一票否决 + 6 类可做路径 + 口诀 + 4 条 `check_compliance.py` 落地建议。

> 本报告遵循红线，未推翻 RED-A/B/C/D/E/P/T 任一条。扫描为中时间态快照，P5 前须在题库降调后复扫。

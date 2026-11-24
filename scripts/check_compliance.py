#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
合规词表 CI 门禁（Phase 2 / 任务 B）
=====================================

按【安安】Phase 1 内容合规红线报告 `audit_report/round13_phase1_security.md` 的
词表做机器判定，覆盖：

  * §2.1 `RED-A` 绝对禁止（A1 未成年 / A2 露骨 / A3 胁迫 / A5 违法 / A6 诱导分享私密数据）
  * §2.3 `RED-C` 「诱惑 / 暗示 / 刺激」的越界措辞（8 语言高危词）
  * §2.5 `RED-E` 地区差异（zh-CN / pt-BR / ko 严格区，最严地区全局收敛）
  * §3.2 `RED-P` 隐私叙事禁止词（含 §3.4 歧义词专条 `RED-P-1`）

扫描对象：
  * `app/src/main/res/values*/strings.xml`（8 语言）
  * `app/src/main/assets/content/**/*.json`（13 类 × 8 语言）

设计要点
--------
1. **否定式语义豁免**：安安 §3.2 判定细则明确「绝不上传 / 无需账号 /
   外部送信ゼロ / 클라우드 전송 없음 / sin nube / sans cloud / sem nuvem」
   属允许且鼓励的正向隐私承诺。本脚本按 **子句（clause）作用域** 判定否定词，
   并区分「否定」与「肯定式邀约」（「去上传」「分享给 TA」「登录后可同步」判违规）。
   Play Billing 相关语境按 §3.1「边界澄清」豁免（不得为「零网络」叙事否认订阅存在）。
2. **不另起炉灶**：简繁异形字 / 串词规则直接 `import check_strings_mix` 复用其
   `SIMPLIFIED_ONLY` / `LANGS` / `CJK_LANGS` / `strip_formats`，保证
   「日语连续 3 个简体专用字」与「韩语任何汉字/假名即报警」继续生效。
3. **幂等**：只读，不写任何被扫文件；同一输入必然同一输出。
4. **CI 化**：退出码 `0` = 通过，`1` = 阻断，`2` = 用法 / 环境错误。
5. **分级**：`block` 级违规阻断构建；`review` 级只报告不阻断
   （避免「制服 / 学校 / location」等高歧义词造成误伤阻断），可用 `--fail-on-review` 升级。

用法
----
    python scripts/check_compliance.py                 # 全量扫描
    python scripts/check_compliance.py --selftest      # 内置自检
    python scripts/check_compliance.py --lang zh-CN     # 限定语言
    python scripts/check_compliance.py --rule RED-P    # 限定规则
    python scripts/check_compliance.py --strings-only  # 只扫 strings.xml
    python scripts/check_compliance.py --content-only  # 只扫内容 JSON
    python scripts/check_compliance.py --fail-on-review

退出码：0 = 通过；1 = 存在阻断级违规；2 = 参数 / 环境错误。
"""
import argparse
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "app", "src", "main", "res")
CONTENT = os.path.join(ROOT, "app", "src", "main", "assets", "content")

# 复用既有串词 / 简繁异形字规则，不另起炉灶（见模块 docstring 第 2 点）
sys.path.insert(0, HERE)
import check_strings_mix as MIX  # noqa: E402

# ---------------------------------------------------------------------------
# 语言定义
# ---------------------------------------------------------------------------
RES_LANGS = {
    "values": "en",
    "values-zh-rCN": "zh-CN",
    "values-zh-rTW": "zh-TW",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-es": "es",
    "values-fr": "fr",
    "values-pt-rBR": "pt-BR",
}
CONTENT_LANGS = {
    "en": "en", "zh": "zh-CN", "zh_tw": "zh-TW", "ja": "ja",
    "ko": "ko", "es": "es", "fr": "fr", "pt": "pt-BR",
}
ALL_LANGS = ["en", "zh-CN", "zh-TW", "ja", "ko", "es", "fr", "pt-BR"]

# §2.5「以最严地区为基准做全局收敛」：这些地区的 RED-C 命中一律升级为阻断级
STRICT_REGIONS = {"zh-CN", "pt-BR", "ko"}

# ---------------------------------------------------------------------------
# 词表
# ---------------------------------------------------------------------------
# 每条 term：
#   pattern        正则（大小写不敏感）
#   severity       block | review
#   negatable      是否允许否定式语义豁免（仅隐私数据流语义族为 True）
#   allow_context  子句内出现这些词则豁免（如 Billing 语境）
#   require_context 只有子句内出现这些词才判定（用于消歧义）
#
# 术语来源：安安 Phase 1 报告 §2.1 / §2.3.4 / §2.5 / §3.2 / §3.4。

TERMS = []


def T(rule, lang, tid, pattern, severity="block", negatable=False,
      allow_context=None, require_context=None, note=""):
    TERMS.append({
        "rule": rule, "lang": lang, "id": tid, "pattern": pattern,
        "severity": severity, "negatable": negatable,
        "allow_context": [c.lower() for c in (allow_context or [])],
        "require_context": [c.lower() for c in (require_context or [])],
        "note": note,
    })


# ---------- RED-A1 未成年 / 年龄暧昧 ----------
_A1 = {
    "zh-CN": [r"未成年", r"少女", r"少年", r"正太", r"萝莉", r"幼女", r"幼男",
              r"女高中生", r"初中生", r"小学生"],
    "zh-TW": [r"未成年", r"少女", r"少年", r"正太", r"蘿莉", r"幼女",
              r"國中生", r"國小生", r"女高中生"],
    "ja": [r"未成年", r"少女", r"少年", r"正太", r"ロリ(?:コン|シタ)?", r"女子高生",
           r"中学生", r"高校生"],
    "ko": [r"미성년", r"미성년자", r"소녀", r"소년", r"고등학생", r"중학생"],
    "es": [r"menor de edad", r"adolescente", r"chica joven", r"chico joven",
           r"menores? de 1[0-7]"],
    "fr": [r"mineur(?:e|es)?\b", r"underage", r"jeune fille", r"jeune garçon",
           r"lolit[a-z]*"],
    "pt-BR": [r"menor de idade", r"adolescente", r"menores? de 1[0-7]",
              r"jovem de 1[0-7]"],
    "en": [r"\bteens?\b", r"\bteenage[rs]?\b", r"\bunderage\b", r"\bschoolgirl\b",
           r"\bschoolboy\b", r"\byoung girl\b", r"\byoung boy\b",
           r"\blooks? (?:young|younger)\b", r"\bbarely legal\b",
           r"\bunder 1[0-8]\b", r"\bchild(?:ren)?\b", r"\blittle girl\b"],
}
for _lang, _pats in _A1.items():
    for _p in _pats:
        T("RED-A1", _lang, "A1", _p, severity="block",
          note="未成年人或未成年暗示")

# 「制服 / 学校 / 校园」等高歧义词单独降级为 review（安安 §2.1 A1 列出但易误伤）
_A1_REVIEW = {
    "zh-CN": [r"校园", r"校服", r"水手服", r"制服"],
    "zh-TW": [r"校園", r"制服"],
    "ja": [r"学園", r"制服"],
    "ko": [r"교복", r"制服"],
    "en": [r"\bschool uniform\b", r"\buniform\b", r"\bcampus\b"],
    "es": [r"\buniforme escolar\b", r"\bcampus\b"],
    "fr": [r"\buniforme scolaire\b", r"\bcampus\b"],
    "pt-BR": [r"\buniforme escolar\b", r"\bcampus\b"],
}
for _lang, _pats in _A1_REVIEW.items():
    for _p in _pats:
        T("RED-A1", _lang, "A1-review", _p, severity="review",
          note="校园/制服类高歧义词，人工复核")

# ---------- RED-A2 露骨性行为 / 性器官 / 动作分解 ----------
_A2 = {
    "en": [r"\b(?:lick|licking|licked|nibble|nibbling|suck|sucking|slurp|swallow)s?\b",
           r"\b(?:orgasm|climax|ejaculat\w*|penis|vagina|vaginal|genitals?|clitoris)\b",
           r"\b(?:nipples?|areola|areolae)\b",
           r"\bforeplay\b", r"\b(?:deep[- ]throat|blowjob|handjob|titjob)\b",
           r"\b(?:undress|strip naked|strip(?:ped)? off (?:your|his|her|their))\b",
           r"\btake off (?:your|his|her|their) (?:clothes|shirt|pants|underwear|dress|top)\b",
           r"\b(?:cock|pussy)\b"],
    "zh-CN": [r"舔舐", r"轻舔", r"深舔", r"吸吮", r"吮吸", r"深喉", r"乳(?:头|晕|沟)",
              r"阴蒂", r"阴道", r"生殖器", r"性器(?:官)?", r"射精", r"性高潮",
              r"脱光", r"宽衣解带", r"褪(?:去|下)衣(?:服)?", r"插入", r"性交"],
    "zh-TW": [r"舔舐", r"輕舔", r"深舔", r"吸吮", r"深喉", r"乳(?:頭|暈)",
              r"陰蒂", r"陰道", r"生殖器", r"射精", r"性高潮",
              r"脫光", r"褪(?:去|下)衣(?:服)?", r"插入", r"性交"],
    "ja": [r"舐め", r"舐める", r"乳首", r"陰茎", r"膣", r"性器", r"射精", r"絶頂",
           r"全裸", r"脱衣(?:せ|して)", r"入れ(?:る|た)"],
    "ko": [r"핥", r"젖꼬지", r"음경", r"질(?!타)", r"성기", r"사정", r"/orgasm",
           r"벌거벗", r"벗(?:겨|기)", r"삽입"],
    "es": [r"\b(?:lamer|lambe|chupar|succionar|tragarse)\b",
           r"\b(?:pene|vagina|genitales|clítoris|clitoris)\b",
           r"\b(?:eyaculación|orgasmo|clímax)\b",
           r"\b(?:desnud\w+|desvestir|quitarse la ropa)\b",
           r"\bjuego previo\b"],
    "fr": [r"\b(?:lécher|lèche|lècher|suctionner|avaler)\b",
           r"\b(?:pénis|vagin\w*|genitaux?|clitoris)\b",
           r"\b(?:éjaculation|orgasme|point culminant)\b",
           r"\b(?:dénud\w+|déshabill\w+)\b", r"\bjeux préalables\b"],
    "pt-BR": [r"\b(?:lamber|chupar|suggar|engolir)\b",
              r"\b(?:pênis|vagina|genitais|clitóris)\b",
              r"\b(?:ejaculação|orgasmo)\b",
              r"\b(?:nud\w+|despir\w+)\b", r"\bpreliminares\b"],
}
for _lang, _pats in _A2.items():
    for _p in _pats:
        T("RED-A2", _lang, "A2", _p, severity="block", note="露骨性行为 / 性器官 / 动作分解")

# ---------- RED-A3 胁迫 / 非自愿 / 失去意识 ----------
_A3 = {
    "en": [r"\bpunishments?\b", r"\bspank(?:ing|ed)?\b",
           r"\bpin(?:ning|ned)? (?:their|her|his|them|you|down)\b",
           r"\brestrain(?:t|ing|ed)?\b", r"\bdomin(?:ate|ates|ating|ance|ator)\b",
           r"\bcan'?t say no\b", r"\bno consent\b", r"\bwithout consent\b",
           r"\btipsy\b", r"\bdrunk\b", r"\bpassed out\b", r"\bunconscious\b",
           r"\bcan'?t pull away\b", r"\bdon'?t let them\b", r"\bforce(?:d|s)?\b",
           r"\bagainst (?:her|his|their) will\b", r"\bcan'?t refuse\b",
           r"\bhurt(?:ing)? (?:her|him|them)\b", r"\bdegrad(?:e|ing)\b"],
    "zh-CN": [r"惩罚", r"处罚", r"强奸", r"强迫", r"胁迫", r"按住", r"压制",
              r"捆绑", r"束缚", r"无法挣脱", r"不许拒绝", r"不能拒绝",
              r"醉酒", r"酒后", r"喝醉", r"晕倒", r"失去意识", r"昏过去", r"睡着",
              r"强制", r"支配", r"羞辱"],
    "zh-TW": [r"懲罰", r"處罰", r"強姦", r"強迫", r"脅迫", r"按住", r"壓制",
              r"捆綁", r"束縛", r"無法掙脫", r"不許拒絕", r"醉酒", r"酒後",
              r"暈倒", r"失去意識", r"昏過去", r"睡著", r"強制", r"支配", r"羞辱"],
    "ja": [r"罰(?:める|ゲーム)?", r"punishment", r"押さえつける",
           r"縛り", r"拘束", r"無理やり", r"拒絶(?:できない|出来ない)",
           r"酔(?:った|い)", r"眠(?:った|って)", r"意識がない", r"支配"],
    "ko": [r"벌(?:given|주다|값)?", r"punishment", r"처벌", r"강제", r"강박",
           r"제압", r"묶(?:어|여)", r"구속", r"거부할 수 없", r"취(?:해|한)",
           r"만취", r"잠들어", r"의식 없", r"지배", r"모욕"],
    "es": [r"\bcastigo(?:s)?\b", r"\bpunishment\b", r"\bdomina(?:ción|ción|dor)\b",
           r"\bsumisión\b", r"\bobligar\b", r"\binmóvil\b", r"\bsujetar\b",
           r"\batar\b", r"\bno puede rechazar\b", r"\bborracho\b",
           r"\bdesmayo\b", r"\bsin conciencia\b", r"\bforzar\b"],
    "fr": [r"\bpunitions?\b", r"\bch[aâ]timents?\b", r"\bsoumission\b",
           r"\bdomination\b", r"\bmaintenir de force\b", r"\bimmobiliser\b",
           r"\battacher\b", r"\bsans son accord\b", r"\bivre\b",
           r"\b[ée]vanoui\w*\b", r"\bforcer\b"],
    "pt-BR": [r"\bcastigos?\b", r"\bpunishment\b", r"\bsubmiss[aã]o\b",
              r"\bdomina[cç][aã]o\b", r"\bobrigar\b", r"\bimobilizar\b",
              r"\bamarrar\b", r"\bsem consentimento\b", r"\bb[eê]bado\b",
              r"\bdesmaiad\w+\b", r"\bfor[cç]ar\b"],
}
for _lang, _pats in _A3.items():
    for _p in _pats:
        T("RED-A3", _lang, "A3", _p, severity="block", note="胁迫 / 非自愿 / 失去意识")

# ---------- RED-A5 违法内容 ----------
_A5 = {
    "en": [r"\billegal drugs?\b", r"\bcocaine\b", r"\bheroin\b", r"\bmethamphetamine\b",
           r"\bhidden camera\b", r"\bsecretly record\w*\b", r"\bvoyeur\w*\b",
           r"\bspy on\b", r"\bsex work\b", r"\bpay for sex\b", r"\bsell (?:photos|pictures)\b",
           r"\bstalk(?:ing)? (?:her|him|them|you)\b", r"\bfollow (?:her|him|them) home\b",
           r"\bleak(?:ing)? (?:her|his|their) (?:privacy|photos)\b"],
    "zh-CN": [r"毒品", r"冰毒", r"海洛因", r"吸毒", r"偷拍", r"偷窥", r"偷录",
              r"迷奸", r"性交易", r"卖淫", r"嫖娼", r"跟踪", r"泄露(?:隐私|她|他的)",
              r"偷拍视频"],
    "zh-TW": [r"毒品", r"冰毒", r"海洛因", r"吸毒", r"偷拍", r"偷窺", r"偷錄",
              r"迷奸", r"性交易", r"賣淫", r"嫖娼", r"跟蹤", r"洩露(?:隱私|她|他的)"],
    "ja": [r"麻薬", r"覚醒剤", r"コカイン", r"ヘロイン", r"盗撮", r"盗聴", r"盗録",
           r"ストーカー", r"性風俗", r"凌辱"],
    "ko": [r"마약", r"각성제", r"코카인", r"헤로인", r"몰카", r"불법 촬영", r"스토커",
           r"매매", r"성매매"],
    "es": [r"\bdrogas? (?:sales|ilegales)?\b", r"\bcoca[ií]na\b", r"\bhero[ií]na\b",
           r"\bc[aá]mara oculta\b", r"\bgrabar a escondidas\b", r"\bacech\w+\b",
           r"\bprostituci[oó]n\b", r"\bsexo por dinero\b"],
    "fr": [r"\bdrogue?s? (?:ill[ée]gales|dures)\b", r"\bcoca[iï]ne\b",
           r"\bh[ée]ro[iï]ne\b", r"\bcam[ée]ra cach[ée]e\b",
           r"\benregistrer en cachette\b", r"\bstalk\w+\b",
           r"\bprostitution\b", r"\bsexe contre de l'argent\b"],
    "pt-BR": [r"\bdrogas? (?:ilegais|duras)\b", r"\bcoca[ií]na\b", r"\bhero[ií]na\b",
               r"\bc[âa]mera oculta\b", r"\bgravar escondido\b", r"\bstalker\b",
               r"\bprostitui[cç][aã]o\b", r"\bsexo por dinheiro\b"],
}
for _lang, _pats in _A5.items():
    for _p in _pats:
        T("RED-A5", _lang, "A5", _p, severity="block", note="违法内容")

# ---------- RED-A6 诱导分享私密影像 / 位置 / 第三方 ----------
_A6 = {
    "en": [r"\bupload (?:the |your |a )?(?:photo|picture|image|video|clip)s?\b",
           r"\bsend (?:it |them |your )?(?:to|photos?|videos?) (?:a |your )?friend\b",
           r"\bshare (?:to|with) (?:social|instagram|tiktok|whatsapp)\b",
           r"\bpost (?:a|your|the) (?:photo|picture|video)\b",
           r"\binvite (?:a |your )?(?:third|third[- ]party|another) (?:person|people|friend)\b",
           r"\bshare (?:your |the )?location\b", r"\bnearby people\b",
           r"\bpeople near you\b", r"\bsend (?:your |the )?location\b",
           r"\bcreate (?:a )?(?:public |group )?(?:chat|room|link)\b"],
    "zh-CN": [r"上传(?:你的|这|照片|视频|图片)", r"发给别人", r"发给(?:好友|朋友|对方|TA)",
              r"分享到(?:社交|朋友圈|群)", r"发送定位", r"附近的人", r"定位附近",
              r"邀请(?:第三|另一个人|他人)", r"公开(?:场合|照片)", r"晒(?:图|照片)"],
    "zh-TW": [r"上傳(?:你的|這|照片|視頻|圖片)", r"發給別人", r"發給(?:好友|朋友|對方|TA)",
              r"分享到(?:社交|朋友圈|群)", r"傳送定位", r"附近的人", r"定位附近",
              r"邀請(?:第三|另一個人|他人)", r"公開(?:場合|照片)"],
    "ja": [r"アップロードして", r"誰かに送", r"共有して",
           r"位置情報を送", r"近くの(?:人|人達)", r"他の人を招待", r"公開する"],
    "ko": [r"업로드(?:해서|하세요)", r"다른 사람에게(?:보내|공유)",
           r"위치(?:정보|를)? 보내", r"주변 사람", r"다른 사람을 초대",
           r"공개사진"],
    "es": [r"\bsubir (?:tus |las |una |un )?(?:fotos?|im[aá]genes|videos?)\b",
           r"\benviar (?:a|las fotos? a) (?:un |tu )?amig[oa]\b",
           r"\bcompartir con (?:redes sociales|instagram|tiktok)\b",
           r"\bpersonas cercanas\b", r"\benviar tu ubicaci[oó]n\b",
           r"\binvitar a (?:otra|un tercero)\b"],
    "fr": [r"\bt[ée]l[ée]verser (?:tes |vos |une |des )?(?:photos?|images?|vid[ée]os?)\b",
           r"\benvoyer (?:[àa]|tes photos? [àa]) (?:un |une |votre )?ami[ea]?\b",
           r"\bpartager avec (?:les r[ée]seaux sociaux|instagram)\b",
           r"\bpersonnes [àa] proximit[ée]\b", r"\benvoyer votre localisation\b",
           r"\binviter (?:un |une )?troisi[èe]me\b"],
    "pt-BR": [r"\benvi(?:ar|dar) (?:suas |as |uma |um )?(?:fotos?|imagens|v[íi]deos?)",
              r"\benviar para (?:um |seu |sua )?amig[oa]\b",
              r"\bcompartilhar com (?:redes sociais|instagram)\b",
              r"\bpessoas perto\b", r"\benviar sua localiza[cç][aã]o\b",
              r"\bconvidar (?:um |uma )?terceir[oa]\b"],
}
for _lang, _pats in _A6.items():
    for _p in _pats:
        T("RED-A6", _lang, "A6", _p, severity="block", negatable=True,
          note="诱导分享私密影像 / 位置 / 第三方")

# ---------- RED-C 越界措辞（§2.3.4 各语言高危词） ----------
_C = {
    "en": [r"\bforeplay\b", r"\bpositions?\b(?=.{0,24}(?:roulette|game|spinner|card))",
           r"\bblowjob\b", r"\bhandjob\b", r"\bdomin(?:ance|atrix)\b",
           r"\bpunishments?\b", r"\bspank\w*\b", r"\bpinning\b"],
    "zh-CN": [r"前戏", r"体位", r"调情(?:教学|教程)", r"情趣(?:用品|道具)"],
    "zh-TW": [r"前戲", r"體位", r"調情(?:教學|教程)", r"情趣(?:用品|道具)"],
    "ja": [r"体位", r"前戯", r"舐め", r"押さえつける", r"罰", r"無理やり"],
    "ko": [r"전희", r"체위", r"핥다", r"제압", r"벌", r"억지로"],
    "es": [r"castigo", r"sumisi[oó]n", r"domina[cci][oó]n", r"obligar",
           r"lamer", r"inm[oó]vil"],
    "fr": [r"punition", r"soumission", r"l[ée]cher", r"maintenir de force",
           r"ivre", r"positions?"],
    "pt-BR": [r"preliminares", r"submiss[aã]o", r"castigo", r"lamber",
              r"imobilizar", r"b[eê]bado", r"posi[cç][õo]es"],
}
for _lang, _pats in _C.items():
    for _p in _pats:
        T("RED-C", _lang, "C", _p, severity="block", note="越界动作 / 支配 / 惩罚措辞")

# 暧昧但常用于游戏名的词 -> review
_C_REVIEW = {
    "en": [r"\bnaughty\b", r"\bsteamy\b", r"\bsultry\b", r"\bkinky\b"],
    "zh-CN": [r"挑逗", r"暧昧"],
    "zh-TW": [r"挑逗", r"曖昧"],
    "ja": [r"色気"],
    "ko": [r"야한", r"관능", r"유혹"],
    "es": [r"\bsensual\b", r"\bprovoc\w+\b"],
    "fr": [r"\bsensuel\b", r"\bprovocant\b"],
    "pt-BR": [r"\bsensual\b", r"\bprovocante\b"],
}
for _lang, _pats in _C_REVIEW.items():
    for _p in _pats:
        T("RED-C", _lang, "C-review", _p, severity="review",
          allow_context=["connect4", "connect 4", "체커", "チェッカー"],
          note="暧昧措辞，人工复核")

# ---------- RED-E 地区差异：各语言「宁可更含蓄」的专属高危词 ----------
_E = {
    "zh-CN": [r"情趣", r"媚", r"挑逗", r"诱惑", r"性感(?:的|地)?"],
    "zh-TW": [r"情趣", r"挑逗", r"誘惑", r"性感(?:的|地)?"],
    "ja": [r"色気", r"妖艶", r"甘え"],
    "ko": [r"야한", r"관능", r"매력", r"유혹"],
    "es": [r"er[oó]tic[oa]", r"sensual", r"provocante"],
    "fr": [r"[ée]rotique", r"sensuel", r"provocant"],
    "pt-BR": [r"[ée]r[oó]tic[oa]", r"sensual", r"provocante"],
}
for _lang, _pats in _E.items():
    for _p in _pats:
        T("RED-E", _lang, "E", _p, severity="review",
          note="地区差异红线：宁可更含蓄")

# ---------- RED-P 隐私叙事禁止词（§3.2，negatable=True） ----------
# 允许短语（§3.3）：命中这些短语直接豁免（仅对隐私语义族生效）。
ALLOW_PHRASES = {
    "en": [r"100% offline", r"on this device", r"never leave this device",
           r"no account", r"no cloud", r"zero tracking", r"no ads",
           r"encrypted on this device", r"works offline forever",
           r"no accounts", r"no trackers", r"nothing leaves", r"zero ads"],
    "zh-CN": [r"纯离线", r"纯離線", r"只留在手机里", r"只留在本机", r"绝不上传",
              r"絕不上傳", r"无需注册账号", r"無需註冊帳號", r"本机加密", r"本機加密",
              r"无广告", r"無廣告", r"不联网", r"不連網", r"没有服务器", r"沒有伺服器",
              r"零广告", r"零追蹤", r"数据不出本机", r"數據不出本機", r"零追踪"],
    "zh-TW": [r"純離線", r"只留在本機", r"絕不上傳", r"無需帳號", r"本機加密",
              r"無廣告", r"不連網", r"沒有伺服器", r"零廣告", r"零追蹤"],
    "ja": [r"完全オフライン", r"端末内のみ", r"外部送信ゼロ", r"アカウント不要",
           r"広告ゼロ", r"サーバー送信なし", r"送信なし", r"端末内"],
    "ko": [r"100% 오프라인", r"이 기기에만", r"클라우드 전송 없음", r"계정 없음",
           r"광고 없음", r"서버 전송 없음", r"전송 없음"],
    "es": [r"100% sin conexi[oó]n", r"sin nube", r"sin cuentas", r"sin rastreadores",
           r"solo en este dispositivo", r"cero anuncios", r"cero rastreo",
           r"nunca sale", r"nunca salen", r"cero rastreadores"],
    "fr": [r"100% hors ligne", r"sans cloud", r"sans compte", r"z[ée]ro suivi",
           r"uniquement sur cet appareil", r"z[ée]ro publicit[ée]",
           r"jamais ne quitte", r"ne quitte jamais", r"z[ée]ro trackers"],
    "pt-BR": [r"100% offline", r"sem nuvem", r"sem contas", r"sem rastreadores",
              r"nunca saem deste dispositivo", r"zero an[uú]ncios", r"zero rastreio",
              r"nunca sai", r"zero rastreadores"],
}

# Billing 语境（安安 §3.1 边界澄清：Play Billing 是唯一涉及外部交互的既有组件）
BILLING_CONTEXT = [
    "billing", "billingclient", "purchase", "purchases", "subscription",
    "subscribe", "play store", "google play", "paywall", "price", "premium",
    "vip", "checkout", "payment", "receipt", "product",
    "付费", "订阅", "购买", "结算", "内购", "价格", "会员",
    "付費", "訂閱", "購買", "結算", "內購", "價格", "會員",
    "課金", "決済", "購入", "価格", "会員",
    "결제", "구매", "구독", "가격", "유료",
    "facturaci", "suscripci", "comprar", "pago", "precio",
    "facturation", "abonnement", "acheter", "prix",
    "cobran", "assinatura", "comprar", "pre",
]

# GPS / 定位语境（用于区分「地点」与「定位隐私」）
# 注意：不得把被判定词本身放进语境词表，否则 require_context 会自我满足。
GPS_CONTEXT = ["nearby", "gps", "map", "position", "sharing", "share",
               "track", "tracking", "permission", "access", "enable",
               "coordinates", "coords", "ubicacion", "localisation",
               "localizacao", "位置", "定位", "附近", "附近的人"]

_P = {
    "en": [r"\buploads?\b", r"\bsync(?:hroniz\w+)? to (?:the )?cloud\b",
           r"\bcloud backup\b", r"\bcloud sync\b",
           r"\bsign in\b", r"\blog ?in\b", r"\baccounts?\b",
           r"\bshare to\b", r"\bsend to (?:a |your )?friend\b",
           r"\bnearby\b", r"\bonline match\w*\b",
           r"\bservers?\b", r"\bbackup online\b", r"\bsend data\b"],
    "zh-CN": [r"上传", r"云同步", r"雲同步", r"云端", r"雲端", r"同步到云", r"同步到雲",
              r"登录", r"登錄", r"注册账号", r"註冊帳號", r"注册帐号",
              r"分享到", r"分享给", r"发给好友", r"发给对方", r"附近的人", r"定位附近",
              r"在线匹配", r"在线配对", r"服务器", r"伺服器", r"云端备份", r"数据回传"],
    "zh-TW": [r"上傳", r"雲端同步", r"同步到雲端", r"登入", r"註冊帳號",
              r"分享到", r"分享給", r"傳給好友", r"附近的人", r"定位附近", r"線上配對",
              r"伺服器", r"雲端備份"],
    "ja": [r"アップロード", r"クラウド同期", r"クラウド保存", r"ログイン",
           r"アカウント登録", r"シェアする", r"共有する", r"近くの人", r"位置情報",
           r"オンラインマッチ", r"サーバー", r"外部送信"],
    "ko": [r"업로드", r"클라우드 동기화", r"클라우드 전송", r"로그인", r"계정 가입",
           r"계정 만들기", r"공유하기", r"공유", r"근처 사람", r"위치 정보",
           r"온라인 매칭", r"서버", r"전송"],
    "es": [r"\bsubir\s+(?:tus?\s+|las?\s+|una?\s+)?(?:fotos?|im[aá]genes|videos?|archivos?|datos|contactos?|la\s+nube|al\s+servidor)\b",
           r"\bsincronizar en la nube\b", r"\bcopia en la nube\b",
           r"\biniciar sesi[oó]n\b", r"\bcrear cuenta\b",
           r"\bcomparte con\b", r"\bcompartir con\b",
           r"\benvia a\b", r"\benviar a\b",
           r"\bgente cercana\b", r"\bcoincidencia en l[íi]nea\b", r"\bservidor\b"],
    "fr": [r"\bt[ée]l[ée]verser\b", r"\bsynchroniser sur le cloud\b",
           r"\bsauvegarde cloud\b", r"\bse connecter\b", r"\bcr[ée]er un compte\b",
           r"\bpartagez avec\b", r"\bpartager avec\b", r"\benvoyez [àa]\b",
           r"\benvoyer [àa]\b", r"\bpersonnes [àa] proximit[ée]\b",
           r"\bcorrespondance en ligne\b", r"\bservateur\b"],
    "pt-BR": [r"\bupload\b", r"\benviar para nuvem\b", r"\bsincronizar na nuvem\b",
              r"\bcopia na nuvem\b", r"\bentrar na conta\b", r"\bcriar conta\b",
              r"\bcompartilhe com\b", r"\bcompartilhar com\b", r"\benvoie para\b",
              r"\benviar para\b", r"\bpessoas perto\b", r"\bmatch online\b",
              r"\bservidor\b"],
}
for _lang, _pats in _P.items():
    for _p in _pats:
        T("RED-P", _lang, "P", _p, severity="block", negatable=True,
          allow_context=BILLING_CONTEXT,
          note="隐私叙事禁止词（零权限 / 零网络 / 零账号）")

# 「定位 / location」需要 GPS 语境，避免误伤「secret dares, locations & fantasies」
_P_GPS = {
    "en": [r"\blocations?\b"],
    "zh-CN": [r"定位"],
    "zh-TW": [r"定位"],
    "ja": [r"位置情報"],
    "ko": [r"위치"],
    "es": [r"\bubicaci[oó]n\b"],
    "fr": [r"\blocalisation\b"],
    "pt-BR": [r"\blocaliza[cç][aã]o\b"],
}
for _lang, _pats in _P_GPS.items():
    for _p in _pats:
        T("RED-P", _lang, "P-gps", _p, severity="block", negatable=True,
          allow_context=BILLING_CONTEXT, require_context=GPS_CONTEXT,
          note="定位隐私（需 GPS 语境，避免误伤普通「地点」）")

# 「sync / 同步」歧义词（§3.4 RED-P-1）：描述「全游戏统一」不得用同步动词
_SYNC_AMBIG = {
    "en": [r"\bsynchroniz\w+\b", r"\bsync\b", r"\bsynced\b"],
    "zh-CN": [r"同步"],
    "zh-TW": [r"同步"],
    "ja": [r"同期"],
    "ko": [r"동기화"],
    "es": [r"\bsincroniz\w+\b"],
    "fr": [r"\bsynchronis\w+\b", r"\bsynchronisation\b"],
    "pt-BR": [r"\bsincroniz\w+\b"],
}
for _lang, _pats in _SYNC_AMBIG.items():
    for _p in _pats:
        T("RED-P", _lang, "P-sync", _p, severity="review", negatable=True,
          allow_context=BILLING_CONTEXT,
          note="「同步」歧义词：RED-P-1，须改用「全游戏统一」类表述")

# ---------------------------------------------------------------------------
# 否定 / 肯定 语义标记
# ---------------------------------------------------------------------------
NEGATION = {
    "en": [r"\bno\b", r"\bnot\b", r"\bnever\b", r"\bwithout\b", r"\bzero\b",
           r"\bnone\b", r"\bnothing\b", r"\bneither\b", r"\bnor\b",
           r"\bdon'?t\b", r"\bdoesn'?t\b", r"\bwon'?t\b", r"\bcannot\b",
           r"\bcan'?t\b", r"\boffline\b", r"\bonly on\b", r"\bthis device\b",
           r"\bstays?\b", r"\bleave\b"],
    "zh-CN": [r"绝不", r"絕不", r"不会", r"不會", r"不能", r"无需", r"無需",
              r"没有", r"沒有", r"不含", r"不联网", r"不連網", r"零", r"纯", r"純",
              r"仅", r"僅", r"只留在", r"禁止", r"从未", r"從未", r"不", r"免",
              r"本机", r"本機", r"终端内", r"端末内"],
    "zh-TW": [r"絕不", r"不會", r"不能", r"無需", r"沒有", r"不含", r"不連網",
              r"零", r"純", r"僅", r"只留在", r"禁止", r"從未", r"不", r"免",
              r"本機", r"端末内"],
    "ja": [r"ゼロ", r"なし", r"無い", r"ない", r"不要", r"無", r"オフライン",
           r"のみ", r"一切", r"非", r"端末内"],
    "ko": [r"없음", r"없어요", r"없습니다", r"없이", r"零", r"오프라인", r"안",
           r"않", r"不要", r"禁止", r"이 기기에만"],
    "es": [r"\bsin\b", r"\bnunca\b", r"\bcero\b", r"\bno\b", r"\bjam[áa]s\b",
           r"\bningun[oa]\b", r"\boffline\b", r"\b100%"],
    "fr": [r"\bsans\b", r"\bjamais\b", r"\bz[ée]ro\b", r"\baucun\w*\b",
           r"\bpas\b", r"\bhors ligne\b", r"\bne\b", r"\b100%"],
    "pt-BR": [r"\bsem\b", r"\bnunca\b", r"\bzero\b", r"\bnenhum\w*\b",
              r"\bnenhuma\b", r"\boffline\b", r"\b100%", r"\bn[ãa]o\b"],
}

# 肯定式 / 邀约式标记：出现即视为「要求用户去做」，否定式不再豁免。
# 注意：这些必须是「邀约形态」，不能与被判定词本身同形，否则会与否定式
# 正向隐私承诺（如「无需注册账号」）自相矛盾。
AFFIRMATIVE = {
    "en": [r"\bupload (?:your|it|the|a)\b", r"\bshare (?:with|to)\b",
           r"\bsend (?:it|them|your photos?) to\b", r"\bsign up\b",
           r"\blog ?in to\b", r"\bconnect your account\b", r"\bsync (?:your|it) to\b",
           r"\bcreate (?:an? )?account\b"],
    "zh-CN": [r"去上传", r"上传你的", r"上传后", r"分享给", r"分享到", r"发送给",
              r"发到", r"登录后", r"登陆后", r"注册新账号", r"连接你的账号",
              r"同步到", r"立即上传", r"一键上传"],
    "zh-TW": [r"去上傳", r"上傳你的", r"分享給", r"分享到", r"傳送給", r"登入後",
              r"註冊新帳號", r"連接你的帳號", r"同步到", r"一鍵上傳"],
    "ja": [r"アップロードして", r"共有して", r"送信して", r"ログインして",
           r"アカウントを作成", r"登録して"],
    "ko": [r"업로드하세요", r"공유하세요", r"보내세요", r"로그인하세요",
           r"계정을 만들", r"가입하세요"],
    "es": [r"subir tus", r"comparte con", r"comparte tus", r"env[íi]a a",
           r"inicia sesi[oó]n", r"crea una cuenta", r"conecta tu cuenta",
           r"haz login"],
    "fr": [r"t[ée]l[ée]versez", r"partagez avec", r"envoyez [àa]",
           r"connectez-vous", r"cr[ée]ez un compte", r"connectez votre compte"],
    "pt-BR": [r"envie suas", r"compartilhe com", r"envie para", r"fa[çc]a login",
              r"crie uma conta", r"conecte sua conta"],
}

# ---------------------------------------------------------------------------
# 子句切分与判定
# ---------------------------------------------------------------------------
CLAUSE_SPLIT = re.compile(
    r"[。！？!?;\n\r]+|(?<=[一-鿿])，(?=[一-鿿])|[—–]+|•+|\s+-\s+|\.\s+",
    re.M)


def split_clauses(text):
    """把文本切成子句，返回 [(clause_text, start_offset)]。"""
    out = []
    pos = 0
    for m in CLAUSE_SPLIT.finditer(text):
        seg = text[pos:m.start()]
        if seg.strip():
            out.append((seg, pos))
        pos = m.end()
    tail = text[pos:]
    if tail.strip():
        out.append((tail, pos))
    return out


# ---------------------------------------------------------------------------
# 预编译索引（性能）
#
# 内容 JSON 约 14 万条字符串、8 语言共 200+ 条规则，若逐条正则全量匹配会
# 跑到分钟级。这里为每种语言建一个「字面子串预筛索引」：
#   * 从每条规则的正则里抽出保守的字面子串集合（任何一次真实匹配都必然
#     包含其中至少一个），用一条合并正则一次命中即可定位候选规则；
#   * 只有候选规则才真正跑正则。
# `--no-prefilter` 可关闭该优化；`--selftest-deep` 会抽样比对两种模式的
# 结果，确保优化不改变判定结果。
# ---------------------------------------------------------------------------
_SEG_SPLIT = re.compile(r"[\\^$.|?*+()\[\]{}]")
_NON_WORD = re.compile(
    r"[^0-9A-Za-z\u00c0-\u024f\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]+")
_CLASS_RE = re.compile(r"\[[^\]]*\]")


def _required_literals(pattern):
    """解析正则，返回该正则「必然出现」的字面子串集合（保守）。

    规则：
      * 只收集 **无条件必需** 的字面片段；可选组 `(?:...)?` / `(?:...)*` 的内容、
        前瞻 `(?=...)` / `(?!...)` 的内容一律不计入（它们不出现在匹配文本里）；
      * 字符类 `[...]` 取其首个字符，且会 **切断** 字面串（`deep[- ]throat`
        的匹配文本可能是 "deep throat"，不能当作整体字面）；
      * 若某分支没有任何长度 >= 2 的必需字面（例如 `罰(?:める|ゲーム)?` 的
        必需字面只有单字「罰」），调用方须把该规则标记为「总是测试」。

    返回 (segments, always_test)。segments 满足：任何一次真实匹配都必然
    包含其中至少一个，因此可安全用于预筛（只会漏掉候选，不会漏掉命中）。
    """
    branches = [[]]
    cur = []
    stack = []
    run = ""
    i = 0
    n = len(pattern)

    def flush():
        nonlocal run
        if run:
            cur.append(run)
            run = ""

    def close_group(end):
        """end = 匹配到 ')' 之后的位置；返回下一个扫描位置。"""
        nonlocal run
        g = stack.pop() if stack else {"look": False, "runs": []}
        j = end
        optional = False
        if j < n and pattern[j] in "?*":
            optional = True
            j += 1
        if g.get("look"):
            return j
        flush()
        if not optional:
            # 非可选组：内部字面是必需的（取最长者作为代表）
            if g["runs"]:
                cur.append(max(g["runs"], key=len))
            else:
                cur.append("")  # 空串 => 该分支没有必需字面
        return j

    while i < n:
        c = pattern[i]
        if c == "\\":
            nxt = pattern[i + 1] if i + 1 < n else ""
            if nxt in "bBAZ":
                i += 2
                continue
            if nxt in "sSdDwW":
                i += 2
                continue
            if nxt in "pP":
                j = i
                if i + 1 < n and pattern[i + 1] == "{":
                    j = pattern.find("}", i)
                    j = n if j < 0 else j + 1
                else:
                    j = i + 2
                i = j
                continue
            run += nxt
            i += 2
            continue
        if c == "[":
            m = _CLASS_RE.match(pattern, i)
            if m:
                inner = m.group(0)[1:-1]
                if inner.startswith("^"):
                    inner = inner[1:]
                first = inner[0] if inner else ""
                flush()  # 字符类切断字面串
                if first and first not in "\\":
                    cur.append(first)
                i = m.end()
                continue
        if c == "(":
            if pattern.startswith("(?=", i) or pattern.startswith("(?!", i) \
                    or pattern.startswith("(?<=", i) or pattern.startswith("(?<!", i):
                stack.append({"look": True, "runs": []})
                depth = 1
                k = i + 1
                while k < n and depth:
                    if pattern[k] == "\\":
                        k += 2
                        continue
                    if pattern[k] == "(":
                        depth += 1
                    elif pattern[k] == ")":
                        depth -= 1
                    k += 1
                i = k
                continue
            stack.append({"look": False, "runs": []})
            i += 1
            continue
        if c == ")":
            flush()
            g = stack.pop() if stack else {"look": False, "runs": []}
            j = i + 1
            optional = False
            if j < n and pattern[j] in "?*":
                optional = True
                j += 1
            if g.get("look"):
                i = j
                continue
            if not optional:
                if g["runs"]:
                    cur.append(max(g["runs"], key=len))
                else:
                    cur.append("")
            i = j
            continue
        if c == "|":
            flush()
            branches.append(cur)
            cur = []
            i += 1
            continue
        if c in "^$":
            i += 1
            continue
        if c == "?" or c == "*":
            # 裸量词：作用于前一个元素。把前一个字符从当前字面串里剥离，
            # 避免把可选字符当成必需字面（uploads? -> upload）。
            if run:
                run = run[:-1]
                if not run:
                    run = ""
            i += 1
            continue
        if c in "+{}":
            i += 1
            continue
        run += c
        i += 1
    flush()
    branches.append(cur)

    # 每个分支取「最长必需字面」作为该分支的代表；没有任何分支代表时，
    # 该规则必须总是测试。
    segs = set()
    always = False
    for br in branches:
        reps = [b for b in br if b]
        if not reps:
            always = True
            continue
        longest = max(reps, key=len)
        if len(longest) < 2:
            always = True
            continue
        segs.add(longest)
        # 同时保留次长片段的「去词尾」变体，提升预筛命中率
        for b in reps:
            for suf in ("es", "s", "d"):
                if b.endswith(suf) and len(b) - len(suf) >= 3:
                    segs.add(b[:-len(suf)])
    if not segs:
        always = True
    return segs, always


def _segments(pattern):
    """兼容包装：返回可用于预筛的字面子串集合。"""
    segs, _ = _required_literals(pattern)
    return segs



class _Index(object):
    """按语言组织的预编译规则索引。"""

    def __init__(self):
        self.term_re = {}        # term index -> compiled regex
        self.seg_to_terms = {}   # (lang, seg) -> [term index]
        self.seg_re = {}         # lang -> compiled union regex (named groups)
        self.seg_group = {}      # lang -> {group name: seg}
        self.always = {}         # lang -> [term index]（无法预筛的规则）
        self.lang_terms = {}     # lang -> [term index]
        self.neg_re = {}
        self.aff_re = {}
        self.allow_phrase_re = {}
        self.allow_ctx_re = {}
        self.req_ctx_re = {}
        self._build()

    def _build(self):
        for i, t in enumerate(TERMS):
            self.term_re[i] = re.compile(t["pattern"], re.I)
        for lang in ALL_LANGS:
            idxs = [i for i, t in enumerate(TERMS) if t["lang"] == lang]
            self.lang_terms[lang] = idxs
            seg_map = {}
            always = []
            groups = {}
            parts = []
            gi = 0
            for ti in idxs:
                segs, always_test = _required_literals(TERMS[ti]["pattern"])
                if always_test or not segs:
                    always.append(ti)
                if not segs:
                    continue
                for s in segs:
                    key = (lang, s)
                    if key not in seg_map:
                        seg_map[key] = []
                        gname = "g%d" % gi
                        gi += 1
                        groups[gname] = s
                        parts.append("(?P<%s>%s)" % (gname, re.escape(s)))
                    seg_map[key].append(ti)
            self.seg_to_terms.update(seg_map)
            self.seg_group[lang] = groups
            self.always[lang] = always
            self.seg_re[lang] = (re.compile("|".join(parts), re.I) if parts else None)
            self.neg_re[lang] = _compile_any(NEGATION.get(lang, []))
            self.aff_re[lang] = _compile_any(AFFIRMATIVE.get(lang, []))
            self.allow_phrase_re[lang] = _compile_any(ALLOW_PHRASES.get(lang, []))
        # 语境词表（allow / require）按词表内容缓存编译结果
        self.allow_ctx_re = {}
        self.req_ctx_re = {}
        for i, t in enumerate(TERMS):
            if t["allow_context"]:
                self.allow_ctx_re[i] = _compile_any(t["allow_context"])
            if t["require_context"]:
                self.req_ctx_re[i] = _compile_any(t["require_context"])

    def candidates(self, lang, clause):
        """返回该子句可能命中的规则下标集合。"""
        out = set(self.always.get(lang, ()))
        seg_re = self.seg_re.get(lang)
        if seg_re is not None:
            groups = self.seg_group[lang]
            for m in seg_re.finditer(clause):
                g = m.lastgroup
                if not g:
                    continue
                seg = groups.get(g)
                if seg is None:
                    continue
                out.update(self.seg_to_terms.get((lang, seg), ()))
        return out


def _compile_any(patterns):
    if not patterns:
        return None
    try:
        return re.compile("|".join("(?:%s)" % p for p in patterns), re.I)
    except re.error:
        return None


INDEX = None


def _index():
    global INDEX
    if INDEX is None:
        INDEX = _Index()
    return INDEX


def _has(text, patterns):
    low = text.lower()
    return any(re.search(p, low, re.I) for p in patterns)


def _negation_near(clause, lang, start, end, window=28):
    """命中前 / 后窗口内是否有否定词。"""
    idx = _index()
    neg = idx.neg_re.get(lang)
    pre = clause[max(0, start - window):start]
    post = clause[end:end + window]
    return (bool(neg and neg.search(pre)), bool(neg and neg.search(post)))


def scan_text(text, lang, rules=None, source="", loc="", use_prefilter=True):
    """对一段文本做规则判定，返回 Finding 列表。"""
    findings = []
    if not text:
        return findings
    idx = _index()
    allow_re = idx.allow_phrase_re.get(lang)
    aff_re = idx.aff_re.get(lang)
    neg_re = idx.neg_re.get(lang)
    for clause, _off in split_clauses(text):
        phrase_hit = bool(allow_re and allow_re.search(clause))
        aff_in_clause = bool(aff_re and aff_re.search(clause))
        neg_in_clause = bool(neg_re and neg_re.search(clause))
        if use_prefilter:
            cand = idx.candidates(lang, clause)
        else:
            cand = set(idx.lang_terms.get(lang, ()))
        for ti in cand:
            term = TERMS[ti]
            if rules and term["rule"] not in rules:
                continue
            # 允许短语只对隐私语义族生效，且不得与肯定式邀约同句
            if phrase_hit and not aff_in_clause and term["rule"] in ("RED-P", "RED-A6"):
                continue
            ac = idx.allow_ctx_re.get(ti)
            if ac is not None and ac.search(clause):
                continue
            rc = idx.req_ctx_re.get(ti)
            if rc is not None and not rc.search(clause):
                continue
            for m in idx.term_re[ti].finditer(clause):
                severity = term["severity"]
                if term["rule"] == "RED-C" and lang in STRICT_REGIONS:
                    severity = "block"
                if term["negatable"]:
                    pre_neg, post_neg = _negation_near(clause, lang, m.start(), m.end())
                    negated = pre_neg or post_neg or (neg_in_clause and not aff_in_clause)
                    if negated and not aff_in_clause:
                        continue
                findings.append({
                    "rule": term["rule"], "term": term["id"], "lang": lang,
                    "severity": severity, "match": m.group(0),
                    "clause": clause.strip()[:160], "source": source, "loc": loc,
                    "note": term["note"],
                })
    return findings


# ---------------------------------------------------------------------------
# 采集
# ---------------------------------------------------------------------------
def collect_strings(langs):
    """返回 [(lang, source_path, key, text)]"""
    items = []
    for folder, lang in RES_LANGS.items():
        if lang not in langs:
            continue
        path = os.path.join(RES, folder, "strings.xml")
        if not os.path.exists(path):
            continue
        root = ET.parse(path).getroot()
        for node in root.findall("string"):
            name = node.get("name")
            if not name:
                continue
            if node.get("translatable") == "false":
                continue
            items.append((lang, path, name, node.text or ""))
    return items


def _walk_json(node, path, out):
    if isinstance(node, dict):
        for k, v in node.items():
            _walk_json(v, path + "." + str(k), out)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            _walk_json(v, path + "[%d]" % i, out)
    elif isinstance(node, str):
        out.append((path, node))


def collect_content(langs):
    """返回 [(lang, source_path, json_path, text)]"""
    items = []
    for folder, lang in CONTENT_LANGS.items():
        if lang not in langs:
            continue
        for path in sorted(glob.glob(os.path.join(CONTENT, folder, "*.json"))):
            try:
                data = json.load(open(path, encoding="utf-8"))
            except Exception:
                continue
            acc = []
            _walk_json(data, "", acc)
            for jpath, text in acc:
                items.append((lang, path, jpath, text))
    return items


# ---------------------------------------------------------------------------
# 简繁异形字 / 串词回归（复用 check_strings_mix，不另起炉灶）
# ---------------------------------------------------------------------------
def run_mix_regression(langs):
    """复用 check_strings_mix 的规则，返回问题列表。"""
    problems = []
    base = None
    for folder in MIX.LANGS:
        lang = RES_LANGS.get(folder, folder)
        if lang not in langs:
            continue
        path = os.path.join(RES, folder, "strings.xml")
        if not os.path.exists(path):
            problems.append((lang, "MISSING_FILE", path, ""))
            continue
        strings = MIX.load(path)
        if base is None:
            base = strings
        else:
            missing = set(base) - set(strings)
            if missing:
                problems.append((lang, "MISSING_KEYS", ",".join(sorted(missing)[:8]), ""))
        for name, text in strings.items():
            if folder in MIX.CJK_LANGS:
                cleaned = MIX.strip_formats(text)
                for token in MIX.LATIN.findall(cleaned):
                    if token.lower() in MIX.ALLOWED:
                        continue
                    problems.append((lang, name, token, text))
                if folder == "values-ja":
                    run = 0
                    for ch in cleaned:
                        if ch in MIX.SIMPLIFIED_ONLY:
                            run += 1
                            if run >= 3:
                                problems.append((lang, name, "CJK_RUN_IN_JA:" + ch, text))
                                break
                        elif MIX.HAN.match(ch):
                            run = 0
                        else:
                            run = 0
                if folder == "values-ko":
                    for ch in cleaned:
                        if MIX.HAN.match(ch) or MIX.KANA.match(ch):
                            problems.append((lang, name, "CJK_IN_KO:" + ch, text))
                            break
            else:
                for label, pattern in (("HAN", MIX.HAN), ("KANA", MIX.KANA),
                                       ("HANGUL", MIX.HANGUL), ("CYRILLIC", MIX.CYRILLIC)):
                    if pattern.search(text):
                        problems.append((lang, name, label, text))
    return problems


# ---------------------------------------------------------------------------
# 自检
# ---------------------------------------------------------------------------
# (文本, 语言, 期望规则集合 or None=不报)
SELFTEST_CASES = [
    # --- 否定式正向隐私承诺：必须全部放行（安安 §3.2 / §3.3）---
    ("100% Offline — no accounts, no cloud, zero tracking", "en", None),
    ("Everything you enter still stays on this device — no account, no upload, nothing leaves here.", "en", None),
    ("Encrypted on this device", "en", None),
    ("无需注册账号……永远只留在你自己的手机里，绝不上传", "zh-CN", None),
    ("純離線／只留在本機／絕不上傳／無需帳號", "zh-TW", None),
    ("完全オフライン — アカウント不要・外部送信ゼロ", "ja", None),
    ("100% 오프라인 — 계정 가입·클라우드 전송 없음", "ko", None),
    ("100% sin conexión — sin cuentas, sin nube, cero rastreo", "es", None),
    ("100% hors ligne — sans compte, sans cloud, zéro suivi", "fr", None),
    ("100% offline — sem contas, sem nuvem, zero rastreio", "pt-BR", None),
    # --- Play Billing 语境豁免（安安 §3.1 边界澄清）---
    ("Billing needs a connection and a Google Play account — please try again.", "en", None),
    ("결제에는 연결과 Google Play 로그인이 필요해요.", "ko", None),
    # --- 肯定式 / 邀约式：必须拦截 ---
    ("Upload your photos to the cloud", "en", {"RED-P"}),
    ("Share to a friend", "en", {"RED-P"}),
    ("分享给好友", "zh-CN", {"RED-P"}),
    ("上傳到雲端", "zh-TW", {"RED-P"}),
    ("アップロードして", "ja", {"RED-P"}),
    ("업로드하세요", "ko", {"RED-P"}),
    ("Comparte con tus amigos", "es", {"RED-P"}),
    ("Partagez avec vos amis", "fr", {"RED-P"}),
    ("Compartilhe com seus amigos", "pt-BR", {"RED-P"}),
    # --- RED-A1 未成年 ---
    ("a schoolgirl in a uniform", "en", {"RED-A1"}),
    ("未成年の少女", "ja", {"RED-A1"}),
    ("미성년자", "ko", {"RED-A1"}),
    ("menor de edad", "es", {"RED-A1"}),
    ("mineur", "fr", {"RED-A1"}),
    ("menor de idade", "pt-BR", {"RED-A1"}),
    ("未成年少女", "zh-CN", {"RED-A1"}),
    ("未成年少女", "zh-TW", {"RED-A1"}),
    # --- RED-A2 露骨 ---
    ("soft licking and nibbling", "en", {"RED-A2"}),
    ("轻舔、轻咬耳垂", "zh-CN", {"RED-A2"}),
    ("舐める", "ja", {"RED-A2"}),
    ("핥다", "ko", {"RED-A2"}),
    ("前戏", "zh-CN", {"RED-C"}),
    ("体位", "ja", {"RED-C"}),
    ("體位", "zh-TW", {"RED-C"}),
    ("체위", "ko", {"RED-C"}),
    ("punitions", "fr", {"RED-A3", "RED-C"}),
    # --- RED-A3 胁迫 ---
    ("bold physical punishments and pinning hands", "en", {"RED-A3"}),
    ("褪衣惩罚、按手控制", "zh-CN", {"RED-A3"}),
    ("罰", "ja", {"RED-A3"}),
    ("castigo", "es", {"RED-A3"}),
    ("castigo", "pt-BR", {"RED-A3"}),
    # --- RED-A6 诱导分享 ---
    ("发给别人", "zh-CN", {"RED-A6"}),
    ("附近的人", "zh-CN", {"RED-A6"}),
    # --- 歧义词不得误报 ---
    ("Subir a Siguiente Fase →", "es", None),
    ("Superado ✓ · Subir Peldaño →", "es", None),
    ("Rub off metallic foil to reveal secret dares, locations & fantasies.", "en", None),
    ("Locations", "en", None),
    ("Naughty Connect4", "en", None),
]


def _mix_flags(folder, text):
    """按 check_strings_mix 的规则判定一段文本是否触发串词规则。"""
    cleaned = MIX.strip_formats(text)
    if folder == "values-ja":
        run = 0
        for ch in cleaned:
            if ch in MIX.SIMPLIFIED_ONLY:
                run += 1
                if run >= 3:
                    return True
            elif MIX.HAN.match(ch):
                run = 0
            else:
                run = 0
        return False
    if folder == "values-ko":
        return any(MIX.HAN.match(ch) or MIX.KANA.match(ch) for ch in cleaned)
    return False


def selftest(verbose=True):
    """内置自检。返回 (passed, failed, messages)。"""
    passed = 0
    failures = []
    for text, lang, expect in SELFTEST_CASES:
        findings = scan_text(text, lang)
        rules = {f["rule"] for f in findings}
        if expect is None:
            if findings:
                failures.append("误报: [%s] %r -> %s" % (
                    lang, text[:50], [(f["rule"], f["match"]) for f in findings]))
            else:
                passed += 1
        else:
            if rules & expect:
                passed += 1
            else:
                failures.append("漏报: [%s] %r 期望 %s 实际 %s" % (
                    lang, text[:50], sorted(expect), sorted(rules)))
    # 串词 / 简繁异形字规则回归
    mix_cases = [
        ("values-ja", "这时间很短", True),   # 连续 3 个简体专用字 -> 必须报警
        ("values-ja", "ふたりだけの秘密のミッション", False),
        ("values-ko", "같은 질문에 답하고谁 더", True),  # 韩语含汉字 -> 必须报警
        ("values-ko", "오직 이 기기에만 머뭅니다", False),
    ]
    for folder, text, should_flag in mix_cases:
        if _mix_flags(folder, text) == should_flag:
            passed += 1
        else:
            failures.append("串词规则回归: [%s] %r 应报警=%s" % (folder, text[:40], should_flag))
    if verbose:
        print("=== check_compliance.py 自检 ===")
        print("用例总数: %d  通过: %d  失败: %d" % (
            len(SELFTEST_CASES) + len(mix_cases), passed, len(failures)))
        for f in failures:
            print("  x " + f)
        if not failures:
            print("[OK] selftest all passed")
    return passed, len(failures), failures


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def main(argv=None):
    ap = argparse.ArgumentParser(
        description="合规词表 CI 门禁（RED-A / RED-C / RED-E / RED-P）")
    ap.add_argument("--selftest", action="store_true", help="运行内置自检并退出")
    ap.add_argument("--lang", action="append", default=None,
                    help="限定语言（可重复）：%s" % ", ".join(ALL_LANGS))
    ap.add_argument("--rule", action="append", default=None,
                    help="限定规则（可重复）：RED-A1 RED-A2 RED-A3 RED-A5 RED-A6 RED-C RED-E RED-P")
    ap.add_argument("--strings-only", action="store_true", help="只扫描 strings.xml")
    ap.add_argument("--content-only", action="store_true", help="只扫描内容 JSON")
    ap.add_argument("--no-mix-regression", action="store_true",
                    help="跳过简繁异形字 / 串词回归（默认执行）")
    ap.add_argument("--fail-on-review", action="store_true",
                    help="把 review 级命中也视为阻断")
    ap.add_argument("--json", action="store_true", help="以 JSON 输出结果")
    ap.add_argument("--max-print", type=int, default=60, help="最多打印多少条明细")
    args = ap.parse_args(argv)

    if args.selftest:
        _, failed, _ = selftest()
        return 1 if failed else 0

    langs = set(args.lang) if args.lang else set(ALL_LANGS)
    unknown = langs - set(ALL_LANGS)
    if unknown:
        print("未知语言: %s（可选: %s）" % (sorted(unknown), ", ".join(ALL_LANGS)),
              file=sys.stderr)
        return 2
    rules = set(args.rule) if args.rule else None
    if rules:
        valid = {t["rule"] for t in TERMS}
        bad = rules - valid
        if bad:
            print("未知规则: %s" % sorted(bad), file=sys.stderr)
            return 2

    findings = []
    n_strings = 0
    n_content = 0
    n_content_files = 0

    if not args.content_only:
        items = collect_strings(langs)
        n_strings = len(items)
        for lang, path, key, text in items:
            findings.extend(scan_text(text, lang, rules, path, key))

    if not args.strings_only:
        for lang in langs:
            for folder, cl in CONTENT_LANGS.items():
                if cl == lang:
                    n_content_files += len(sorted(glob.glob(os.path.join(CONTENT, folder, "*.json"))))
        items = collect_content(langs)
        n_content = len(items)
        for lang, path, jpath, text in items:
            findings.extend(scan_text(text, lang, rules, path, jpath))

    mix_problems = []
    if not args.no_mix_regression:
        mix_problems = run_mix_regression(langs)

    blocks = [f for f in findings if f["severity"] == "block"]
    reviews = [f for f in findings if f["severity"] != "block"]

    if args.json:
        print(json.dumps({
            "strings_scanned": n_strings,
            "content_strings_scanned": n_content,
            "content_files_scanned": n_content_files,
            "block": blocks, "review": reviews,
            "mix_problems": [{"lang": m[0], "name": m[1], "token": m[2], "text": m[3]}
                             for m in mix_problems],
        }, ensure_ascii=False, indent=2))
    else:
        print("=== 合规词表门禁 ===")
        print("扫描范围: %d 语言 | strings.xml %d 条 | 内容 JSON %d 文件 / %d 个字符串值"
              % (len(langs), n_strings, n_content_files, n_content))
        print("命中: 阻断级 %d 条 | 复核级 %d 条 | 串词/简繁回归 %d 条"
              % (len(blocks), len(reviews), len(mix_problems)))
        if blocks:
            print("[BLOCK] 阻断级违规:")
            for f in blocks[:args.max_print]:
                print("  [%s/%s] %s :: %s | %s"
                      % (f["rule"], f["lang"], f["match"], f["loc"], f["clause"][:90]))
            if len(blocks) > args.max_print:
                print("  ... 还有 %d 条" % (len(blocks) - args.max_print))
        if reviews:
            print("[REVIEW] 复核级命中（不阻断，需人工确认）:")
            for f in reviews[:args.max_print]:
                print("  [%s/%s] %s :: %s | %s"
                      % (f["rule"], f["lang"], f["match"], f["loc"], f["clause"][:90]))
            if len(reviews) > args.max_print:
                print("  ... 还有 %d 条" % (len(reviews) - args.max_print))
        if mix_problems:
            print("[BLOCK] 串词 / 简繁异形字回归:")
            for lang, name, token, text in mix_problems[:args.max_print]:
                print("  [%s] %s: %s :: %s" % (lang, name, token, str(text)[:90]))
            if len(mix_problems) > args.max_print:
                print("  ... 还有 %d 条" % (len(mix_problems) - args.max_print))
        if not blocks and not mix_problems:
            print("[OK] 未发现阻断级违规" + ("（有复核级命中，见上）" if reviews else ""))

    if blocks or mix_problems:
        return 1
    if reviews and args.fail_on_review:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

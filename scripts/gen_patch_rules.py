# -*- coding: utf-8 -*-
"""VelvetCouples · 词表精修补丁（一次性执行脚本，幂等）

用「行级正则定位 + 整行替换」修正 gen_tone_rules.py 里经实测确认的假阳性。
实测依据见 scripts/gen_tone_falsepos.py 的输出。

假阳性清单（已逐条看过上下文）：
  1. A3 es/fr/pt：`nunca` / `jamás` / `jamais` / `em segredo` / `prohibido`
     —— 语境多为「从未一起做过」「心里暗暗期待」「规则游戏里禁止笑」，**不是胁迫**。
  2. A3 zh：裸 `偷偷` / `不许` / `不准` / `必须` —— 「心里偷偷最期待」是 P5 悬念（正向）。
     保留真胁迫词：无法反抗 / 不能拒绝 / 按住手 / 惩罚 / 拍打 / 醉酒 / 睡着时 等。
  3. A3 ko：裸 `몰래`（心里暗暗地，正向）去掉；保留 `술에` / `기절` / `묶` 等。
  4. A3 ja：裸 `こっそり`（正向）去掉；保留 `罰` / `酔` / `押さえ` 等。
  5. N3 en：裸 `immediately`（多为动作节奏副词，非命令式）去掉；保留 `obey` 等。
  6. N9 zh：`上传` 用负向前查排除「马上上传递」子串误命中。
  7. A5 ja：`撮影` 限定为「人前 / 他人 / 挑战」语境，排除普通「撮影する」。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, 'scripts', 'gen_tone_rules.py')

# (定位正则, 替换模板) —— 替换模板里 {cls} 会被替换为定位到的类名
LINE_PATCHES = [
    # A3 es
    (r"^    'es': r'.*no \(.*nunca.*$",
     """    'es': r'\\b(no puedes rechazar|no puede rechazar|no puede escap\\w+|inmoviliz\\w+|sujetar con fuerza|atad\\w+|azot\\w+|castig\\w+|borracho\\w*|desmay\\w+|dormid\\w+ sin|sin saber|a escondid\\w+|sin permiso|obligad\\w+|forzad\\w+)\\b',"""),
    # A3 fr
    (r"^    'fr': r\".*interdis.*$",
     """    'fr': r"\\b(impossible de refus\\w+|ne peut pas refus\\w+|ne peut pas échapp\\w+|immobilis\\w+|maintenir de force|attach\\w+|fess\\w+|chati\\w+|punir\\w+|punition|ivre\\w*|évanoui\\w+|endormi\\w+ sans|en cachette|sans son accord|oblig\\w+|forc\\w+)\\b",'"""),
    # A3 pt
    (r"^    'pt': r'.*não \(.*nunca.*$",
     """    'pt': r'\\b(não pode recusar|não consegue recusar|não consegue fug\\w+|imobiliz\\w+|segurar com força|amarrad\\w+|apanhar|castig\\w+|bebad\\w+|desmai\\w+|dormind\\w+ sem|à socapa|sem o consent\\w+|obrigad\\w+|forçad\\w+)\\b','"""),
    # A3 zh —— 去掉 偷偷 / 不许 / 不准 / 必须 / 强制 / 被迫（裸词），保留真胁迫结构
    (r"^    'zh': r'.*按住手.*$",
     """    'zh': r'无法反抗|不能拒绝|不许躲|不许动|不许停|按住手|压住|捆绑|束缚|惩罚|鞭打|拍打|打到哭|醉酒|酒后|喝醉|晕过去|昏迷|睡着时|背着她|背着他|瞒着|不能动|无法挣脱|不准乱动',"""),
    # A3 ko —— 去掉  몰래 / 秘密
    (r"^    'ko': r'.*손을 눌러.*$",
     """    'ko': r'손을 눌러|누르는|묶|체포|벌\\w*|매\\w*|때리\\w*|주먹|술에|취하여|기절|잠든 채|비밀리에',"""),
    # A3 ja —— 去掉 こっそり
    (r"^    'ja': r'.*許すな.*$",
     """    'ja': r'許すな|ごらんなさい|強制|抵抗でき|断れない|逃げられない|押さえる|縛る|懲罰|鞭|叩|酔|泥酔|昏倒|眠ったまま|知らずに|身動き',"""),
    # N3 en —— 去掉 immediately / NEVER / ALWAYS 等节奏副词
    (r"^    'en': r'.*TAP NOW.*$",
     """    'en': r'\\b(OBEY|COMPLY|TAP NOW)\\b|!{2,}',"""),
    # N9 zh —— 上传 用负向前查，排除「马上上传递」
    (r"^    'zh': r'上传\|云端.*$",
     """    'zh': r'(?<!马)(?<!立刻)上传|云端|云同步|同步到|服务器|在线匹配|附近的人|定位附近|分享给|发给好友|登录账号|注册账号',"""),
    # N9 zh_tw 同理
    (r"^    'zh_tw': r'上傳\|雲端.*$",
     """    'zh_tw': r'(?<!馬)(?<!立刻)上傳|雲端|雲同步|同步到|伺服器|線上配對|附近的人|定位附近|分享給|傳給好友|登入帳號|註冊帳號',"""),
    # A5 ja —— 撮影 限定为「人前 / 他人 / 挑战 / 自撮り以外」
    (r"^    'ja': r'撮影\|録画.*$",
     """    'ja': r'人前.{0,6}撮影|他人の.{0,6}撮影|写真.{0,4}挑戦|アップロード|クラウド同期|クラウド保存|ログイン|アカウント登録|共有する|近くの人に',"""),
]


def main():
    if not os.path.exists(TARGET):
        print('target not found:', TARGET)
        return 1
    src = io.open(TARGET, encoding='utf-8').read()
    lines = src.split('\n')
    applied = 0
    missing = []
    for pat, repl in LINE_PATCHES:
        rx = re.compile(pat)
        hit = False
        for i, l in enumerate(lines):
            if rx.match(l):
                lines[i] = repl
                applied += 1
                hit = True
                break
        if not hit:
            missing.append(pat)
    if missing:
        print('!! NOT FOUND %d pattern(s):' % len(missing))
        for m in missing:
            print('   %s' % m)
        return 2
    out = '\n'.join(lines)
    io.open(TARGET, 'w', encoding='utf-8', newline='\n').write(out)
    print('applied=%d' % applied)
    return 0


if __name__ == '__main__':
    sys.exit(main())

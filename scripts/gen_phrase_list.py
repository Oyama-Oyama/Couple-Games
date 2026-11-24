# -*- coding: utf-8 -*-
"""VelvetCouples · 违规短语（短语级）清单导出（只读）

用途：为「短语替换表」提供精确的 find 串。
按语言输出：命中类 + 短语（动词+身体部位组合 / 胁迫结构 / 命令式结构）的频次。
"""
import io
import json
import os
import re
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_tone_rules import RULES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')

# 每语言：短语级锚点（比整句更细，便于做精确替换）
PHRASE = {
    'en': [
        (r'\b(?:slowly |gently |playfully )?(?:lick\w*|nibbl\w*|suck\w*|bit\w*|tracing wet tongue trails|kissing on)\b', 'A2'),
        (r'\b(?:leaving|trailing|pressing|planting|dragging|drawing) (?:open-mouthed|breathless|wet|slow) kisses?\b', 'A2'),
        (r'\bstraddl\w+\b', 'A2'),
        (r'\bdeep kiss\w*\b', 'A2'),
        (r'\b(?:undress|unbutton)\w*\b', 'A2'),
        (r'\bpin\w*\b|\bpinned\b|\bpress them\b|\bheld? (?:their )?hands? behind\b', 'A3'),
        (r'\bspank\w*\b|\bpunish\w*\b|\btied up\b|\bwithout stopping\b|\bfull surrender\b|\bcannot touch\b', 'A3'),
        (r'\bObey\b', 'N3'),
        (r'\bimmediately\b', 'N3'),
        (r'\bno looking away\b|\bno stopping\b|\bwithout looking away\b', 'N3'),
        (r'\bin public\b|\bpublic place\b|\bon stage\b|\bin front of (?:everyone|others)\b', 'N5'),
        (r'\bfilming ourselves\b|\brecord (?:ourselves|us)\b|\bupload\b', 'A5'),
    ],
    'zh': [
        (r'轻舔|舔舐|吮吻|吸吮|舌尖|深吻|舌吻|轻咬|咬住|含住|跨坐|骑乘|胸贴|褪衣|脱掉|脱衣', 'A2'),
        (r'按住手|按在手|压在|压住|按住对方|无法反抗|不能动|惩罚|拍打|捆绑|束缚|偷偷|背着她|背着他|瞒着|喝醉|睡着时', 'A3'),
        (r'不许|不准|必须|一定要|给我|听好了|照做', 'N3'),
        (r'点击|请点|系统将|完成后|下一张|本局|得分|用户', 'N4'),
        (r'当众|在别人面前|在他人面前|公共场所|公开场合|让第三人|围观|同事面前|朋友面前|办公室', 'N5'),
        (r'上传|云端|云同步|同步到|服务器|附近的人|分享给|发给好友|登录账号|注册账号|拍下|录下', 'N9'),
    ],
    'ja': [
        (r'舐め|舐|舌先|深的キス|深キス|またがる|脱衣|裸|乳首|舐める', 'A2'),
        (r'押さえ|縛|罰|スパンク|飲ま|酔|眠ったまま|知らずに|こっそり|動かせ', 'A3'),
        (r'しなさい|せよ|必ず|だめ', 'N3'),
        (r'ユーザー|タップ|次のカード|スコア|制限時間|システム', 'N4'),
        (r'人前|皆の前|公共の場|人目の前', 'N5'),
        (r'アップロード|クラウド同期|ログイン|アカウント|共有', 'N9'),
    ],
    'ko': [
        (r'핥|때리|씹|혀로|깊은 키스|벗겨|탈의|누가|올라타|삽입|유두', 'A2'),
        (r'누르는|묶|체포|벌\w*|때리\w*|술에|기절|잠든 채|몰래|비밀리에|반드시', 'A3'),
        (r'사용자|탭|다음 카드|점수|제한 시간|시스템', 'N4'),
        (r'남들 앞|사람들 앞에서|공공의 장소', 'N5'),
        (r'업로드|클라우드|로그인|계정|공유', 'N9'),
    ],
    'es': [
        (r'\b(?:lamer|lamido|chup\w+|mord\w+|lengua|beso profundo|a horcajadas|desnud\w+|montar|cabalg\w+|inmoviliz\w+|sujetar con fuerza|dominar)\w*\b', 'A2'),
        (r'\b(?:no puedes rechazar|no puede rechazar|no puede escap\w+|castig\w+|azot\w+|borracho\w*|desmay\w+|sin saber|a escondid\w+|sin permiso|obligad\w+|forzad\w+|prohibido|nunca |jamás)\b', 'A3'),
        (r'\b(?:EMBRASA|PULSA|NO PARES)\b', 'N3'),
        (r'\b(?:usuario|toca |pulsa |el sistema|puntuación|duración)\b', 'N4'),
        (r'\b(?:delante de todos|delante de los demás|lugar público|en público)\b', 'N5'),
        (r'\b(?:sincronizar en la nube|copia en la nube|crear cuenta|compartir con|personas cercanas)\b', 'N9'),
    ],
    'fr': [
        (r'\b(?:lecher|lamper|suçot\w*|mordre|morsure|baiser profond|dévorer|déshabill\w+|monter|chevaucher|immobilis\w+|maintenir de force|contraindre)\w*\b', 'A2'),
        (r'\b(?:impossible de refus\w+|ne peut pas refus\w+|ne peut pas échapp\w+|fess\w+|chati\w+|punir\w+|punition|ivre\w*|évanoui\w+|en cachette|sans son accord|interdis\w+|jamais |oblig\w+|forc\w+)\b', 'A3'),
        (r'\b(?:EMBRASSEZ|APPUYEZ|FAITES)\b', 'N3'),
        (r'\b(?:utilisateur|appuyez|suivant|durée|score)\b', 'N4'),
        (r'\b(?:devant tout le monde|devant les autres|endroit public|en public)\b', 'N5'),
        (r'\b(?:téléverser|synchroniser sur le cloud|créer un compte|partager avec)\b', 'N9'),
    ],
    'pt': [
        (r'\b(?:lamber|chup\w+|morder|língua|beijo profundo|a cavalo|despir\w+|montar|imobiliz\w+|segurar com força|dominar)\w*\b', 'A2'),
        (r'\b(?:não pode recusar|não consegue recusar|não consegue fug\w+|castig\w+|apanhar|bebad\w+|desmai\w+|à socapa|em segredo|sem o consent\w+|proibid\w+|obrigad\w+|forçad\w+|nunca |jamais)\b', 'A3'),
        (r'\b(?:BEIJE|TOQUE|NÃO PARE)\b', 'N3'),
        (r'\b(?:usuário|toque em|pontuação|duração)\b', 'N4'),
        (r'\b(?:na frente de todos|lugar público|em público)\b', 'N5'),
        (r'\b(?:sincronizar na nuvem|criar conta|compartilhar com|pessoas perto)\b', 'N9'),
    ],
}
PHRASE['zh_tw'] = [re.sub(r'轻|轻咬', lambda m: '輕' + m.group(0)[1:], p) if False else p for p, c in PHRASE['zh']]


def texts_of(o, out):
    if isinstance(o, dict):
        for v in o.values():
            if isinstance(v, str):
                out.append(v)
            else:
                texts_of(v, out)
    elif isinstance(o, list):
        for v in o:
            texts_of(v, out)


def main():
    L = sys.argv[1]
    cls_filter = sys.argv[2] if len(sys.argv) > 2 else None
    pats = [(re.compile(p, re.I), c) for p, c in PHRASE.get(L, [])]
    agg = Counter()
    for fn in sorted(os.listdir(os.path.join(BASE, L))):
        d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
        ts = []
        texts_of(d, ts)
        for t in ts:
            for rx, c in pats:
                if cls_filter and c != cls_filter:
                    continue
                for m in rx.finditer(t):
                    agg[(c, m.group(0).lower())] += 1
    print('#### %s' % L)
    bycls = {}
    for (c, s), n in agg.items():
        bycls.setdefault(c, []).append((n, s))
    for c in sorted(bycls):
        print('  -- %s (%d distinct)' % (c, len(bycls[c])))
        for n, s in sorted(bycls[c], reverse=True)[:40]:
            print('     %5d  %s' % (n, s))


if __name__ == '__main__':
    main()

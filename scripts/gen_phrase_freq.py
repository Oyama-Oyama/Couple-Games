# -*- coding: utf-8 -*-
"""VelvetCouples · 递归短语频次统计（用于设计替换表）（只读）

按语言统计「含高危动作词 / 命令式动词」的短语片段出现频次，
输出最常见的片段，作为改写替换表（slot 级）的依据。
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

# 高危短语锚点（各语言），命中后取前后 N 词窗口
ANCHOR = {
    'en': r'lick|nibbl|suck|bite|tongue|deep kiss|straddl|undress|pinned behind|spank|punish|Obey|anchoring hips|Move to|holding hips close|tracing wet|leaving breathless',
    'zh': r'轻咬|轻舔|舔舐|深吻|吮|跨坐|脱掉|褪衣|按住|惩罚|拍打|吮吻|舌尖|胸口|锁骨|含住|咬住',
    'zh_tw': r'輕咬|輕舔|舔舐|深吻|吮|跨坐|脫掉|褪衣|按住|懲罰|拍打|吮吻|舌尖|胸口|鎖骨|含住|咬住',
    'ja': r'舐|噛|吸|舐め|深キス|またが|脱衣|押さ|懲罰|平気',
    'ko': r'핥|때리|씹|흡| 깊게 키스|올라타|벗겨|누르는|벌',
    'es': r'chup|mord|lengua|beso profundo|a horcajadas|desnud|presion|azot|castig',
    'fr': r'lecher|mordre|langue|baiser profond|à califourchon|déshabill|immobilis|chât',
    'pt': r'lamb|chup|morder|lingua|beijo profundo|a cavalo|despir|segurar|castig',
}
WIN = {
    'en': 6, 'zh': 8, 'zh_tw': 8, 'ja': 8, 'ko': 8, 'es': 7, 'fr': 7, 'pt': 7,
}


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
    only = sys.argv[1] if len(sys.argv) > 1 else None
    langs = [only] if only else ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']
    for L in langs:
        pat = ANCHOR.get(L)
        if not pat:
            continue
        rx = re.compile(pat, re.I)
        w = WIN[L]
        c = Counter()
        for fn in sorted(os.listdir(os.path.join(BASE, L))):
            d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
            ts = []
            texts_of(d, ts)
            for t in ts:
                for m in rx.finditer(t):
                    a = max(0, m.start() - 28)
                    b = min(len(t), m.end() + 28)
                    c[t[a:b].replace('\n', ' ')] += 1
        print('########', L, len(c), 'distinct fragments')
        for s, n in c.most_common(40):
            print('  %5d | %s' % (n, s))


if __name__ == '__main__':
    main()

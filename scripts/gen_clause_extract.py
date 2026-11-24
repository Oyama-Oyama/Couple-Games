# -*- coding: utf-8 -*-
"""VelvetCouples · 违规子句（clause）清单提取（只读）

把命中 A2/A3/N3/N5 的文本切成分句，统计 distinct 子句。
子句级改写是最小可验证单元，也是替换表的依据。
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

SPLIT = re.compile(r'[，,;；。.!?！？—–\n]|\s+while\s+|\s+and\s+then\s+|\s+then\s+')


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
    cls = sys.argv[2]
    pat = RULES[cls].get(L)
    rx = re.compile(pat, re.I)
    c = Counter()
    for fn in sorted(os.listdir(os.path.join(BASE, L))):
        d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
        ts = []
        texts_of(d, ts)
        for t in ts:
            if not rx.search(t):
                continue
            for cl in SPLIT.split(t):
                cl = cl.strip()
                if len(cl) < 3:
                    continue
                if rx.search(cl):
                    c[cl] += 1
    print('#### %s / %s  distinct clauses=%d  total=%d' % (L, cls, len(c), sum(c.values())))
    for s, n in c.most_common(120):
        print('  %4d | %s' % (n, s))


if __name__ == '__main__':
    main()

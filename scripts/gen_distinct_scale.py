# -*- coding: utf-8 -*-
"""VelvetCouples · 违规「去重后」规模统计（只读）

对每个规则类统计：命中 item 数 / 命中 distinct 文本串数。
distinct 串数才是手写改写的真实工作量。
"""
import io
import json
import os
import re
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_tone_rules import RULES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']
CLASSES = ['A1', 'A2', 'A3', 'A5', 'N3', 'N4', 'N5', 'N9']


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
    print('%-8s %-5s %8s %8s %8s' % ('lang', 'cls', 'items', 'distinct', 'files'))
    tot_d = Counter()
    for L in LANGS:
        per = defaultdict(lambda: [set(), set(), set()])
        for fn in sorted(os.listdir(os.path.join(BASE, L))):
            d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
            ts = []
            texts_of(d, ts)
            for t in ts:
                for cls in CLASSES:
                    pat = RULES[cls].get(L)
                    if pat and re.search(pat, t, re.I):
                        per[cls][0].add(t)
                        per[cls][1].add(t)
                        per[cls][2].add(fn)
        for cls in CLASSES:
            items, dis, files = per[cls]
            if items:
                print('%-8s %-5s %8d %8d %8d' % (L, cls, len(items), len(dis), len(files)))
                tot_d[cls] += len(dis)
    print()
    print('distinct-sum per class (8 langs):')
    for cls in CLASSES:
        if tot_d[cls]:
            print('   %-5s %6d' % (cls, tot_d[cls]))


if __name__ == '__main__':
    main()

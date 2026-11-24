# -*- coding: utf-8 -*-
"""VelvetCouples · 词表假阳性排查：列出每个规则类实际命中的子串及样例（只读）"""
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
    for L in LANGS:
        print('########', L)
        alltxt = []
        for fn in sorted(os.listdir(os.path.join(BASE, L))):
            d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
            ts = []
            texts_of(d, ts)
            for t in ts:
                alltxt.append((fn, t))
        for cls in CLASSES:
            pat = RULES[cls].get(L)
            if not pat:
                continue
            rx = re.compile(pat, re.I)
            sub = Counter()
            samp = {}
            for fn, t in alltxt:
                for m in rx.finditer(t):
                    g = m.group(0).lower()
                    sub[g] += 1
                    samp.setdefault(g, (fn, t[:90]))
            if not sub:
                continue
            print('  --', cls, '(', sum(sub.values()), 'hits,', len(sub), 'distinct )')
            for g, n in sub.most_common(18):
                print('     %5d  %-28s | %s' % (n, g, samp[g][1].replace('\n', ' ')))


if __name__ == '__main__':
    main()

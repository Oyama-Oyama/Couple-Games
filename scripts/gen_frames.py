# -*- coding: utf-8 -*-
"""VelvetCouples · v21 模板框架（frame）识别（只读）

把 v21 批次的句子归一化为 (frame, slots) 结构，
从而得知「改写一个 slot 值」可以批量修复多少条目。
"""
import io
import json
import os
import re
import sys
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')


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


# 抽取「被引号包裹的槽」——8 语言通用：'...' "..." 「...」 «...» '...'
QUOTE = re.compile(r"'([^']{6,140})'|\"([^\"]{6,140})\"|「([^」]{6,140})」|«([^»]{6,140})»")


def main():
    L = sys.argv[1] if len(sys.argv) > 1 else 'en'
    only_v21 = '--v21' in sys.argv
    c = Counter()
    byfile = defaultdict(Counter)
    for fn in sorted(os.listdir(os.path.join(BASE, L))):
        d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
        ts = []
        texts_of(d, ts)
        for t in ts:
            for m in QUOTE.finditer(t):
                g = next(g for g in m.groups() if g)
                c[g] += 1
                byfile[fn][g] += 1
    print('#### %s  引号槽 distinct=%d  总出现=%d' % (L, len(c), sum(c.values())))
    for s, n in c.most_common(70):
        print('  %5d | %s' % (n, s))


if __name__ == '__main__':
    main()

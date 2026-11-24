# -*- coding: utf-8 -*-
"""VelvetCouples · v21 动作槽词汇表提取（只读）

v21 批次由模板拼接生成，动作槽词汇量很小（约 20~40 个）。
提取全部槽值 -> 用于生成「同槽跨 8 语言一一对应」的改写表。
"""
import io
import json
import os
import re
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')

# 各语言的 v21 动作槽抽取正则
PATS = {
    'en': [
        (r"anchoring hips firmly while focusing on (.+?)\.", 'focus'),
        (r"whisper about (.+?) between breaths\.", 'whisper'),
        (r"use (.+?) while whispering about (.+?)\.", 'use'),
        (r"(.+?) followed by (.+?) and sync slow breaths", 'tipadd'),
        (r"Obey '(.+?)' and ", 'rule'),
        (r"alternate (.+?) with (.+?)", 'mix'),
    ],
    'zh': [
        (r"重点探索(.+?)。", 'focus'),
        (r"耳边低语「(.+?)」", 'whisper'),
        (r"「(.+?)」", 'rule'),
    ],
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
    L = sys.argv[1] if len(sys.argv) > 1 else 'en'
    fn = sys.argv[2] if len(sys.argv) > 2 else 'extreme_modes.json'
    pats = PATS.get(L)
    if not pats:
        print('no pattern for', L)
        return
    d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
    ts = []
    texts_of(d, ts)
    for pat, tag in pats:
        rx = re.compile(pat, re.I)
        c = Counter()
        for t in ts:
            m = rx.search(t)
            if m:
                c[' | '.join(g for g in m.groups() if g)] += 1
        print('==== %s (%s) distinct=%d' % (tag, fn, len(c)))
        for s, n in c.most_common(60):
            print('  %4d | %s' % (n, s))


if __name__ == '__main__':
    main()

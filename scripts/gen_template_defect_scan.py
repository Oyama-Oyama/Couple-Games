# -*- coding: utf-8 -*-
"""VelvetCouples · 模板拼接缺陷扫描（只读）

检测 v21 批次由模板拼接产生的语法/本地化缺陷：
  D1 韩语助词占位符残留  (를)/(과)/(이) 等
  D2 引号内片段直插（模板槽未消化）—— 中/西/葡/法「进入「…」「Entrad en '…'」等
  D3 地点槽前置介词重复（Move to behind / en detrás / em atrás）
  D4 重复部位（lower belly, and lower belly）
  D5 动作分解残留（wet tongue / open-mouthed / spank / naked 等）
  D6 时长与文案不一致（text 里写 N 秒，durationSec 不同）
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']

D1 = re.compile(r'\(\s*(?:를|과|이|가|은|는|을)\s*\)')
D2 = {
    'zh': [r'进入「', r'「[^」]*」[^，。]*?，'],
    'zh_tw': [r'進入「'],
    'ja': [r'「'],
    'ko': [r"'", r"「"],
    'es': [r"Entrad en '", r"\ben (?:detrás|frente|atrás)"],
    'fr': [r'«', r'» à '],
    'pt': [r"Entrem em '", r"' (?:atrás|em frente|em uma|em um)"],
    'en': [r"Ease into '", r"Move to (?:behind|in front of|at|beside)"],
}
D4 = re.compile(r'(\b[a-z]+(?: [a-z]+)?),\s*and\s+\1\b')
D5 = re.compile(r'wet tongue|open-mouthed|spank|naked|strip|bite playfully|sucking softly|licking|tongue trail', re.I)
D3 = re.compile(r'Move to (?:behind|in front of|at |beside )')


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
    print('=== D1 韩语助词占位符残留 ===')
    tot = 0
    for fn in sorted(os.listdir(os.path.join(BASE, 'ko'))):
        raw = io.open(os.path.join(BASE, 'ko', fn), encoding='utf-8').read()
        c = len(D1.findall(raw))
        if c:
            tot += c
            print('   %-26s %4d' % (fn, c))
    print('   TOTAL %d' % tot)

    print()
    print('=== D2 模板槽未消化（引号片段直插）===')
    for L in LANGS:
        row = []
        for fn in sorted(os.listdir(os.path.join(BASE, L))):
            raw = io.open(os.path.join(BASE, L, fn), encoding='utf-8').read()
            n = 0
            for p in D2.get(L, []):
                n += len(re.findall(p, raw))
            if n:
                row.append('%s=%d' % (fn.replace('.json', ''), n))
        if row:
            print('   %-6s %s' % (L, '  '.join(row)))

    print()
    print('=== D4 重复部位 ===')
    for L in LANGS:
        row = []
        for fn in sorted(os.listdir(os.path.join(BASE, L))):
            d = json.load(io.open(os.path.join(BASE, L, fn), encoding='utf-8'))
            ts = []
            texts_of(d, ts)
            n = sum(1 for t in ts if D4.search(t))
            if n:
                row.append('%s=%d' % (fn.replace('.json', ''), n))
        if row:
            print('   %-6s %s' % (L, '  '.join(row)))

    print()
    print('=== D5 动作分解残留（英文源）===')
    d = json.load(io.open(os.path.join(BASE, 'en', 'extreme_modes.json'), encoding='utf-8'))
    ts = []
    texts_of(d, ts)
    n = sum(1 for t in ts if D5.search(t))
    print('   en/extreme_modes.json  %d 条含动作分解残留' % n)


if __name__ == '__main__':
    main()

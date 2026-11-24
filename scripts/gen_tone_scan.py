# -*- coding: utf-8 -*-
"""VelvetCouples · 内容资产情调/合规全量扫描器（只读，不修改任何资产）

用法：
    python scripts/gen_tone_scan.py                # 概览（8 语言分布）
    python scripts/gen_tone_scan.py --lang en -v   # 单语言命中明细
    python scripts/gen_tone_scan.py --file extreme_modes.json --lang zh -v
"""
import io
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_tone_rules import RULES, HEAT4

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']
FILES = sorted(os.listdir(os.path.join(CONTENT, 'en')))

# 判定类别 -> 严重度
SEVERITY = {
    'A1': 'BLOCK',   # 未成年（RED-A1 零容忍）
    'A2': 'HIGH',    # 动作分解（RED-A2）
    'A3': 'BLOCK',   # 胁迫 / 惩罚 / 失去意识（RED-A3）
    'A5': 'BLOCK',   # 违法 / 诱导分享（RED-A5 / A6）
    'N3': 'HIGH',    # 命令式压迫（架架 N3）
    'N4': 'MED',     # 功能说明书式（架架 N4）
    'N5': 'HIGH',    # 公开场合 / 第三人（架架 N5）
    'N9': 'BLOCK',   # 数据流歧义（RED-P-1 / 架架 N9）
    'P7': 'GAP',     # T3/T4 缺边界共识（架架 P7 / RED-B3）
    'GRAD': 'MED',   # 梯度越界（T1 含高热度动作词）
}

COMPILED = {}
for _cls, _per in RULES.items():
    COMPILED[_cls] = {l: re.compile(p, re.I) for l, p in _per.items()}
HEAT4_RX = {l: re.compile(p, re.I) for l, p in HEAT4.items()}

# 条目级 tier 字段名候选
TIER_KEYS = ('tier', 'heat', 'difficulty', 'level')


def load(lang, fname):
    fp = os.path.join(CONTENT, lang, fname)
    if not os.path.exists(fp):
        return None
    with io.open(fp, encoding='utf-8') as f:
        return json.load(f)


def item_tier(item):
    if not isinstance(item, dict):
        return None
    for k in TIER_KEYS:
        if k in item and isinstance(item[k], int):
            return item[k]
    return None


def iter_items(obj, path=''):
    """产出 (item_path, item_dict) —— 列表元素视为条目；dict 顶层键下的 list 视为条目组"""
    if isinstance(obj, dict):
        for k, v in obj.items():
            p = path + '/' + str(k)
            if isinstance(v, list) and v and isinstance(v[0], dict):
                for i, it in enumerate(v):
                    yield '%s/%d' % (p, i), it
            elif isinstance(v, dict):
                for x in iter_items(v, p):
                    yield x
            else:
                yield p, {'__scalar__': v}
    elif isinstance(obj, list):
        for i, it in enumerate(obj):
            p = '%s/%d' % (path, i)
            if isinstance(it, dict):
                yield p, it
            else:
                yield p, {'__scalar__': it}


def item_strings(item):
    """条目内所有文本值"""
    out = []
    if isinstance(item, dict):
        for k, v in item.items():
            if isinstance(v, str):
                out.append((k, v))
            elif isinstance(v, list):
                for i, s in enumerate(v):
                    if isinstance(s, str):
                        out.append(('%s[%d]' % (k, i), s))
                    elif isinstance(s, dict):
                        for kk, vv in s.items():
                            if isinstance(vv, str):
                                out.append(('%s.%s' % (k, kk), vv))
    return out


def scan_file(lang, fname):
    """返回 [(cls, item_path, field, text)]"""
    d = load(lang, fname)
    if d is None:
        return []
    hits = []
    for ipath, item in iter_items(d):
        t = item_tier(item)
        fields = item_strings(item)
        has_p7 = any(COMPILED['P7'].get(lang) and COMPILED['P7'][lang].search(s) for _, s in fields)
        for fld, text in fields:
            for cls in ('A1', 'A2', 'A3', 'A5', 'N3', 'N4', 'N5', 'N9'):
                rx = COMPILED[cls].get(lang)
                if rx and rx.search(text):
                    hits.append((cls, ipath, fld, text))
            if t == 1 and HEAT4_RX.get(lang) and HEAT4_RX[lang].search(text):
                hits.append(('GRAD', ipath, fld, text))
        if t is not None and t >= 3 and not has_p7:
            f0 = fields[0] if fields else ('', '')
            hits.append(('P7', ipath, f0[0], f0[1]))
    return hits


def main():
    argv = sys.argv[1:]
    only_lang = None
    only_file = None
    verbose = '-v' in argv
    if '--lang' in argv:
        only_lang = argv[argv.index('--lang') + 1]
    if '--file' in argv:
        only_file = argv[argv.index('--file') + 1]

    langs = [only_lang] if only_lang else LANGS
    files = [only_file] if only_file else FILES

    grand = {}
    for lang in langs:
        per = {}
        total = 0
        for fname in files:
            hits = scan_file(lang, fname)
            if hits:
                cnt = {}
                for cls, ipath, fld, text in hits:
                    cnt[cls] = cnt.get(cls, 0) + 1
                per[fname] = (cnt, hits)
                total += len(hits)
        for cls in per:
            pass
        grand[lang] = per
        cls_tot = {}
        for fname, (cnt, hits) in per.items():
            for cls, c in cnt.items():
                cls_tot[cls] = cls_tot.get(cls, 0) + c
        print('=== %s  命中 %d ===' % (lang, total))
        for cls in sorted(cls_tot, key=lambda c: -cls_tot[c]):
            print('    %-6s %-6s %5d' % (cls, SEVERITY.get(cls, '?'), cls_tot[cls]))
        if verbose:
            for fname in sorted(per, key=lambda f: -len(per[f][1])):
                print('  -- %s' % fname)
                for cls, ipath, fld, text in per[fname][1]:
                    snippet = text.replace('\n', ' ')
                    if len(snippet) > 150:
                        snippet = snippet[:150] + '…'
                    print('     [%s] %s %s :: %s' % (cls, ipath, fld, snippet))
    print()
    print('--- 汇总 ---')
    for lang in langs:
        cls_tot = {}
        for fname, (cnt, hits) in grand.get(lang, {}).items():
            for cls, c in cnt.items():
                cls_tot[cls] = cls_tot.get(cls, 0) + c
        if cls_tot:
            print('%s: %s' % (lang, '  '.join('%s=%d' % (c, cls_tot[c]) for c in sorted(cls_tot))))
        else:
            print('%s: 0' % lang)


if __name__ == '__main__':
    main()

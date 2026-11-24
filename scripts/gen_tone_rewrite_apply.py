# -*- coding: utf-8 -*-
"""VelvetCouples · 内容资产批量改写执行器

从 gen_tone_rewrite_table.py 读取 REWRITES（槽族 × 8 语言平行替换表），
对 assets/content/{lang}/*.json 的所有字符串字段做**短语级**替换。

硬约束：
  - 只改字符串值，不改 id / tier / tags / durationSec / slots / 结构
  - json 落盘格式：ensure_ascii=False, indent=2，保留原文件末尾换行习惯
  - 幂等：同一替换执行两次结果一致（替换后文本不再命中 find 模式）
  - 逐条记录 (file, field, before, after, family, cls) 供报告与人工复核

用法：
    python scripts/gen_tone_rewrite_apply.py --dry-run          # 预演，不改文件
    python scripts/gen_tone_rewrite_apply.py --lang en          # 单语言
    python scripts/gen_tone_rewrite_apply.py --file decks.json  # 单文件
    python scripts/gen_tone_rewrite_apply.py --commit           # 实际写入
"""
import io
import json
import os
import re
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_tone_rewrite_table import REWRITES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']


def compile_table(lang):
    out = []
    for fam, cls, l, find, repl in REWRITES:
        if l != lang:
            continue
        out.append((fam, cls, re.compile(find, re.I), repl))
    return out


def strings_with_paths(obj, path=''):
    """产出 (json_path, 字符串值) —— 只遍历字段值，不改结构"""
    if isinstance(obj, dict):
        for k, v in obj.items():
            p = path + '/' + str(k)
            if isinstance(v, str):
                yield p, v
            else:
                yield from strings_with_paths(v, p)
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            p = path + '/' + str(i)
            if isinstance(v, str):
                yield p, v
            else:
                yield from strings_with_paths(v, p)


def set_string(obj, path, value):
    """按 json_path 定位并赋值（路径形如 /questions/0/text）"""
    parts = [p for p in path.split('/') if p != '']
    cur = obj
    for part in parts[:-1]:
        if isinstance(cur, list):
            cur = cur[int(part)]
        else:
            cur = cur[part]
    last = parts[-1]
    if isinstance(cur, list):
        cur[int(last)] = value
    else:
        cur[last] = value


def main():
    argv = sys.argv[1:]
    commit = '--commit' in argv
    lang = argv[argv.index('--lang') + 1] if '--lang' in argv else None
    only_file = argv[argv.index('--file') + 1] if '--file' in argv else None
    langs = [lang] if lang else LANGS
    total = Counter()
    log = []

    for L in langs:
        table = compile_table(L)
        if not table:
            print('!! no rewrite rules for', L)
            continue
        files = [only_file] if only_file else sorted(os.listdir(os.path.join(CONTENT, L)))
        for fn in files:
            fp = os.path.join(CONTENT, L, fn)
            if not os.path.exists(fp):
                continue
            raw = io.open(fp, encoding='utf-8').read()
            trailing = raw.endswith('\n')
            data = json.loads(raw)
            changed = 0
            for path, text in list(strings_with_paths(data)):
                cur = text
                for fam, cls, rx, repl in table:
                    nxt = rx.sub(repl, cur)
                    if nxt != cur:
                        log.append((L, fn, path, cls, fam, cur[:120], nxt[:120]))
                        cur = nxt
                if cur != text:
                    set_string(data, path, cur)
                    changed += 1
                    total[(L, fn)] += 1
            if changed and commit:
                out = json.dumps(data, ensure_ascii=False, indent=2)
                if trailing:
                    out += '\n'
                io.open(fp, 'w', encoding='utf-8', newline='\n').write(out)
            if changed:
                print('%-6s %-26s changed=%d%s' % (
                    L, fn, changed, '' if commit else '  (dry-run)'))

    print()
    print('=== 总计 ===')
    for (L, fn), n in sorted(total.items()):
        print('%-6s %-26s %d' % (L, fn, n))
    print('grand total =', sum(total.values()))

    # 写日志供报告引用
    if commit and log:
        logfp = os.path.join(ROOT, 'audit_report', 'round13_rewrite_log.json')
        io.open(logfp, 'w', encoding='utf-8', newline='\n').write(
            json.dumps(log, ensure_ascii=False, indent=1))
        print('log ->', logfp)


if __name__ == '__main__':
    main()

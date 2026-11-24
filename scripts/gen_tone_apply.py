# -*- coding: utf-8 -*-
"""VelvetCouples · 内容资产情调/合规改写执行器（Phase 3 apply 端）

用法：
    python scripts/gen_tone_apply.py --dry-run          # 只报告，不写盘
    python scripts/gen_tone_apply.py --apply             # 实际写盘（先自动备份）
    python scripts/gen_tone_apply.py --apply --lang zh    # 只处理某语言
    python scripts/gen_tone_apply.py --apply --file decks.json

保结构保证：只替换字符串值，不新增/删除/重排任何键，不改 id/tier/tags/slots/durationSec。
"""
import io, json, os, re, shutil, sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_tone_slots import REWRITES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']
BACKUP = os.path.join(ROOT, 'build', 'content_backup_pre_tone')


def compile_slots():
    out = defaultdict(list)
    for fam, lang, find, repl, cls, note in REWRITES:
        out[lang].append((fam, re.compile(find), repl, cls, note))
    # 长模式优先，避免短模式先吃掉长模式
    for lang in out:
        out[lang].sort(key=lambda x: -len(x[1].pattern))
    return out


def walk(o, path, fn):
    """深度遍历 JSON，对每个字符串值调用 fn(path, value) -> new_value"""
    if isinstance(o, dict):
        for k in list(o.keys()):
            o[k] = walk(o[k], path + '.' + str(k), fn)
        return o
    if isinstance(o, list):
        for i in range(len(o)):
            o[i] = walk(o[i], path + '[%d]' % i, fn)
        return o
    if isinstance(o, str):
        return fn(path, o)
    return o


def apply_to_obj(obj, slots, stats):
    def fn(path, val):
        changed = False
        for fam, rx, repl, cls, note in slots:
            if rx.search(val):
                new = rx.sub(repl, val)
                if new != val:
                    stats[(cls, fam)] += 1
                    stats[('lang', path)] = 1
                    val = new
                    changed = True
        return val
    return walk(obj, '', fn)


def _key_paths(o, path='', out=None):
    """收集所有键路径，用于改写前后结构比对（必须完全一致）"""
    if out is None:
        out = []
    if isinstance(o, dict):
        for k in o.keys():
            out.append(path + '.' + str(k))
            _key_paths(o[k], path + '.' + str(k), out)
    elif isinstance(o, list):
        for i, it in enumerate(o):
            out.append(path + '[%d]' % i)
            _key_paths(it, path + '[%d]' % i, out)
    return out


def main():
    apply_mode = '--apply' in sys.argv
    dry = '--dry-run' in sys.argv
    langs = LANGS
    if '--lang' in sys.argv:
        langs = [sys.argv[sys.argv.index('--lang') + 1]]
    only_file = None
    if '--file' in sys.argv:
        only_file = sys.argv[sys.argv.index('--file') + 1]

    slots = compile_slots()
    totals = Counter()
    per_file = defaultdict(Counter)
    struct_changes = []

    for L in langs:
        d = os.path.join(BASE, L)
        files = sorted(os.listdir(d))
        for fn in files:
            if not fn.endswith('.json'):
                continue
            if only_file and fn != only_file:
                continue
            path = os.path.join(d, fn)
            with io.open(path, encoding='utf-8') as fh:
                original = fh.read()
            obj = json.loads(original)
            before_keys = sorted(_key_paths(obj))
            stats = Counter()
            new_obj = apply_to_obj(obj, slots.get(L, []), stats)
            after_keys = sorted(_key_paths(new_obj))
            if before_keys != after_keys:
                struct_changes.append((L, fn, len(before_keys), len(after_keys)))
            new_text = json.dumps(new_obj, ensure_ascii=False, indent=2) + '\n'
            if new_text != original:
                per_file[(L, fn)] = stats
                for k, v in stats.items():
                    totals[k] += v
                if apply_mode:
                    if not os.path.isdir(BACKUP):
                        os.makedirs(BACKUP)
                    bp = os.path.join(BACKUP, L + '__' + fn)
                    if not os.path.exists(bp):
                        shutil.copy2(path, bp)
                    with io.open(path, 'w', encoding='utf-8') as fh:
                        fh.write(new_text)

    print('=== 替换统计（按类别） ===')
    bycls = Counter()
    for (cls, fam), v in totals.items():
        if cls in ('A2', 'A3', 'A5', 'N3', 'N4', 'N5', 'N9'):
            bycls[cls] += v
    for c in sorted(bycls):
        print('  %-4s %6d' % (c, bycls[c]))
    print('=== 替换统计（按语言） ===')
    bylang = Counter()
    for (L, fn), st in per_file.items():
        bylang[L] += sum(v for k, v in st.items() if k[0] in ('A2', 'A3', 'A5', 'N9', 'N5'))
    for L in LANGS:
        if L in bylang:
            print('  %-6s %6d' % (L, bylang[L]))
    print('=== 涉及文件数：%d ===' % len(per_file))
    print('=== 结构变化（应为 0）：%d ===' % len(struct_changes))
    for sc in struct_changes[:10]:
        print('  !! %s' % (sc,))
    if not apply_mode:
        print('\n[dry-run] 未写盘。加 --apply 执行。')
    else:
        print('\n[applied] 已写盘。备份目录：%s' % BACKUP)


if __name__ == "__main__":
    main()

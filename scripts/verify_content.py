#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
内容资产回归校验（CI 用）：
  1. 所有 JSON 合法且可解析
  2. 8 语言 ID 集合对齐（不允许缺 ID；允许语言独有 ID）
  3. generator_templates 占位符跨语言一致
  4. pattern 中 {slot} 均在 slots 中定义（白名单除外）
  5. 文本字段无 v21 模板拼接语法错误
  6. shortLabel 无截断（长度/单词完整性）
  7. 资源引用：布局/菜单中 @string/xxx 均有定义
  8. zh_tw 无简体异形字残留

退出码：0 = 全部通过；1 = 存在问题（阻止 CI）。
用法：python scripts/verify_content.py
"""
import json, os, re, sys, glob
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content')
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')
LANGS = ['en', 'zh', 'zh_tw', 'ja', 'ko', 'es', 'fr', 'pt']
CONTENT_FILES = ['decks.json', 'board_themes.json', 'draw_words.json',
                 'generator_templates.json', 'fate_cubes.json', 'scratch_cards.json',
                 'roleplay_scenarios.json', 'couple_quiz.json', 'party_duel.json',
                 'intimate_modes.json', 'extreme_modes.json', 'date_planner.json',
                 'sealed_letter.json']

errors = []
def err(cat, msg): errors.append((cat, msg))

data = {}
# ---------- 1. JSON 合法性 ----------
for lang in LANGS:
    for f in CONTENT_FILES:
        p = os.path.join(CONTENT, lang, f)
        if not os.path.exists(p):
            err('MISSING_FILE', f'{lang}/{f} 不存在'); continue
        try:
            data[(lang, f)] = json.load(open(p, encoding='utf-8'))
        except Exception as e:
            err('BAD_JSON', f'{lang}/{f} 解析失败: {e}')
if errors:
    print('JSON 校验失败，终止后续检查'); [print(f'  [{c}] {m}') for c, m in errors]; sys.exit(1)

def collect_ids(node, out):
    if isinstance(node, dict):
        if 'id' in node and isinstance(node['id'], str): out.add(node['id'])
        for v in node.values(): collect_ids(v, out)
    elif isinstance(node, list):
        for v in node: collect_ids(v, out)

# ---------- 2. ID 对齐（en 为基准，不允许缺 ID）----------
for f in CONTENT_FILES:
    en_ids = set(); collect_ids(data.get(('en', f), {}), en_ids)
    if not en_ids: continue
    for lang in LANGS[1:]:
        ids = set(); collect_ids(data.get((lang, f), {}), ids)
        missing = en_ids - ids
        if missing:
            err('ID_MISSING', f'{lang}/{f} 缺少 {len(missing)} 个 ID: {sorted(missing)[:5]}')

# ---------- 3/4. 占位符 ----------
PLACEHOLDER = re.compile(r'\{([a-zA-Z0-9_]+)\}')
SLOT_WHITELIST = {'zone', 'p1', 'p2', 'lead', 'partner'}  # 由代码运行时替换
def collect_ph(node):
    res = {}
    def walk(n):
        if isinstance(n, dict):
            eid = n.get('id')
            if eid:
                s = set()
                for k, v in n.items():
                    if isinstance(v, str): s |= set(PLACEHOLDER.findall(v))
                res.setdefault(eid, set()).update(s)
            for v in n.values(): walk(v)
        elif isinstance(n, list):
            for v in n: walk(v)
    walk(node); return res

enph = collect_ph(data[('en', 'generator_templates.json')])
for lang in LANGS[1:]:
    ph = collect_ph(data[(lang, 'generator_templates.json')])
    for eid, eps in enph.items():
        if eid in ph and eps != ph[eid]:
            err('PLACEHOLDER_MISMATCH', f'{lang} 模板 {eid}: en={sorted(eps)} {lang}={sorted(ph[eid])}')

for lang in LANGS:
    d = data[(lang, 'generator_templates.json')]
    slots = set(d.get('slots', {}).keys()) | SLOT_WHITELIST
    def check_slots(n):
        if isinstance(n, dict):
            for k, v in n.items():
                if isinstance(v, str) and k == 'pattern':
                    for phn in PLACEHOLDER.findall(v):
                        if phn not in slots:
                            err('SLOT_UNDEFINED', f'{lang} 模板 {n.get("id","?")} 占位符 {{{phn}}} 未定义')
                else: check_slots(v)
        elif isinstance(n, list):
            for v in n: check_slots(v)
    check_slots(d)

# ---------- 5/6. v21 文本质量 ----------
BAD_PATTERNS = {
    'at behind/in front': r'\bat (?:behind|in front|beside|under)\b',
    '介词+on冗余': r'\b(?:over|across|along|down|up) on\b',
    'focusing动词原形': r'\bfocusing (?:suck|lick|bite|slowly|gently|trail|leave|trace|tease)\b',
    'holding+动词': r'\bholding (?:letting|whispering|staying|keeping)\b',
    '场景嵌套': r" in '[^']+', anchoring",
    '残留Under前缀': r'^(?:Under|At under)\s',
}
def walk_text(n, in_v21=False, path=''):
    if isinstance(n, dict):
        iv = in_v21 or ('v21' in str(n.get('id', '')))
        for k, v in n.items(): yield from walk_text(v, iv, path + '.' + k)
    elif isinstance(n, list):
        for i, v in enumerate(n): yield from walk_text(v, in_v21, path + f'[{i}]')
    elif isinstance(n, str) and in_v21:
        yield path, n

# 仅英文做语法/截断检查（其它语言为各自译文，模式不同）
for f in CONTENT_FILES:
    if ('en', f) not in data: continue
    for path, text in walk_text(data[('en', f)]):
        field = path.split('.')[-1].split('[')[0]
        for name, pat in BAD_PATTERNS.items():
            m = re.search(pat, text, re.I if name not in ('残留Under前缀',) else 0)
            if m:
                err('V21_GRAMMAR', f'en/{f} {path} 命中[{name}]: {text[:70]}'); break
        if field == 'shortLabel' and re.match(r'^[a-z]', text) and len(text) >= 13 and not re.search(r'[.!?s]$', text):
            err('LABEL_TRUNC', f'en/{f} {path} 标签疑似截断: {text!r}')

# ---------- 7. 资源引用 ----------
defined_strings = set()
for p in glob.glob(os.path.join(RES, 'values*', 'strings.xml')):
    try:
        for r in ET.parse(p).getroot().findall('string'):
            if r.get('name'): defined_strings.add(r.get('name'))
    except Exception as e:
        err('BAD_XML', f'{p}: {e}')
for p in (glob.glob(os.path.join(RES, 'layout', '*.xml')) +
          glob.glob(os.path.join(RES, 'menu', '*.xml')) +
          glob.glob(os.path.join(RES, 'navigation', '*.xml')) +
          glob.glob(os.path.join(RES, 'xml', '*.xml'))):
    txt = open(p, encoding='utf-8').read()
    for name in re.findall(r'@string/([A-Za-z0-9_]+)', txt):
        if name not in defined_strings:
            err('MISSING_STRING', f'{os.path.basename(p)} 引用 @string/{name} 未定义')

# ---------- 8. zh_tw 简体异形字 ----------
S2T = {'脸':'臉','红':'紅','刚':'剛','显':'顯','标':'標','温':'溫','说':'說','让':'讓',
       '从':'從','边':'邊','过':'過','声':'聲','动':'動','静':'靜','轻':'輕','点':'點',
       '关':'關','内':'內','软':'軟','紧':'緊','兴':'興','奋':'奮','续':'續','缓':'緩',
       '压':'壓','贴':'貼','湿':'濕','转':'轉','须':'須','应':'應','该':'該','记':'記',
       '忆':'憶','恋':'戀','约':'約','习':'習','抚':'撫','际':'際','双':'雙','谁':'誰',
       '对':'對','吗':'嗎','听':'聽','导':'導','满':'滿','热':'熱','启':'啟','间':'間',
       '这':'這','个':'個','时':'時','给':'給','们':'們','么':'麼','样':'樣','为':'為',
       '话':'話','觉':'覺','还':'還','进':'進','层':'層','现':'現','尝':'嘗','别':'別',
       '牵':'牽','拢':'攏','张':'張','写':'寫','励':'勵','顺':'順','势':'勢','叠':'疊',
       '讲':'講','风':'風','飞':'飛','长':'長','里':'裡','着':'著','后':'後','余':'餘',
       '云':'雲','净':'淨','干':'乾','关于':'關於','我们':'我們','独处':'獨處','什么':'什麼'}
for f in CONTENT_FILES:
    d = data.get(('zh_tw', f), {})
    def check_tw(n, path=''):
        if isinstance(n, dict):
            for k, v in n.items(): check_tw(v, path + '.' + k)
        elif isinstance(n, list):
            for i, v in enumerate(n): check_tw(v, path + f'[{i}]')
        elif isinstance(n, str):
            for ch in n:
                if ch in S2T and len(S2T[ch]) == 1:
                    err('TW_SIMPLIFIED', f'zh_tw/{f} {path} 简体字[{ch}]应为[{S2T[ch]}]: {n[:50]}')
                    break
    check_tw(d)

# ---------- 汇总 ----------
from collections import Counter
print('=== 内容资产回归校验 ===')
if not errors:
    print('✅ 全部通过')
    sys.exit(0)
print(f'❌ 发现 {len(errors)} 个问题:')
for cat, cnt in Counter(c for c, _ in errors).most_common():
    print(f'  {cat}: {cnt}')
for cat, msg in errors[:40]:
    print(f'  [{cat}] {msg}')
if len(errors) > 40: print(f'  ... 还有 {len(errors) - 40} 个')
sys.exit(1)

import json, io, glob, re

ZH = ['轻舔','轻咬','深吻','跨坐','褪衣','惩罚','拍打','按住','捆绑','束缚',
      '不许','不准','必须','亲吻','舌头','脱掉','胸贴','压住','骑乘','扒开']
ZHT = ['輕舔','輕咬','深吻','跨坐','褪衣','懲罰','拍打','按住','綁綁','束縛',
       '不許','不準','必須','親吻','舌頭','脫掉','胸貼','壓住','騎乘','扒開']
EN = ['lick','nibbl','suck','deep kiss','foreplay','punish','spank','pinning',
      'restrain','undress','strip','touch your','must','straddle','hold her']
JA = ['舐め','吸う','前戯','脱衣','体位','責め','押さえつける','舐']
KO = ['핥다','전희','체위','제압','벌','무리하게','벗겨','키스']
ES = ['lamer','chupar','castigo','suminis','domina','obligar','inmovil']
FR = ['punition','soumission','lecher','maintient','ivre','baiser']
PT = ['preliminares','submiss','castigo','lamber','imobiliz','bebado']
PAT = {'zh': ZH, 'zh_tw': ZHT, 'en': EN, 'ja': JA, 'ko': KO,
       'es': ES, 'fr': FR, 'pt': PT}

for L, words in PAT.items():
    rx = re.compile('|'.join(re.escape(w) for w in words), re.I)
    hit = tot = 0
    per = []
    for fp in sorted(glob.glob('app/src/main/assets/content/%s/*.json' % L)):
        d = json.load(io.open(fp, encoding='utf-8'))
        fh = fc = 0

        def rec(o):
            global fh, fc
            if isinstance(o, dict):
                for v in o.values():
                    if isinstance(v, str):
                        fc += 1
                        if rx.search(v):
                            fh += 1
                    else:
                        rec(v)
            elif isinstance(o, list):
                for it in o:
                    rec(it)
        rec(d)
        hit += fh
        tot += fc
        per.append((fp.split('/')[-1], fh, fc))
    print('=== %s  hit=%d / %d (%.1f%%)' % (L, hit, tot, 100.0 * hit / tot))
    for n, h, c in sorted(per, key=lambda x: -x[1])[:9]:
        print('    %-26s %5d / %5d' % (n, h, c))

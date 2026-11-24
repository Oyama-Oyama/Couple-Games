# -*- coding: utf-8 -*-
"""追加 P0/P1/P3 关键槽族（幂等）"""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, 'scripts', 'gen_tone_rewrite_table.py')

BLOCK = '''

# ==========================================================================
# P0 · A3 胁迫/惩罚/失去意识 —— 必须清零
# ==========================================================================
_add('P0-PINNED', 'A3', {
    'en': (r'pinned behind their back|hands pinned behind',
           'resting quietly at their sides'),
    'zh': (r'双手背在身后|手背在身后|按住手',
           '双手轻轻交握放在身前'),
    'zh_tw': (r'雙手背在身後|手背在身後|按住手',
              '雙手輕輕交握放在身前'),
    'ja': (r'手を背中で組まれ|押さえつけ',
           '手を前にそっと重ねて'),
    'ko': (r'등 뒤로 손을 묶|손을 눌러',
           '손을 앞에 살짝 모아'),
    'es': (r'manos sujetas a la espalda|inmovilizar',
           'manos suavemente entrelazadas al frente'),
    'fr': (r'mains immobilisées derrière le dos|immobiliser',
           'mains doucement croisées devant soi'),
    'pt': (r'mãos presas atrás das costas|imobilizar',
           'mãos suavemente entrelaçadas à frente'),
})

_add('P0-PRESS-WALL', 'A3', {
    'en': (r'press them softly against the wall by the hips',
           'stand close together, unhurried, by the wall'),
    'zh': (r'轻抵在卧室墙面的姿势',
           '在卧室墙边轻轻相依的姿态'),
    'zh_tw': (r'輕抵在臥室牆面的姿勢',
              '在臥室牆邊輕輕相依的姿勢'),
    'ja': (r'寝室の壁に背中を預けて密着する体勢',
           '寝室の壁にもたれて、ゆっくり寄り添う体勢'),
    'ko': (r'침실 벽에 기대어 밀착하는 자세',
           '침실 벽에 기대어 천천히 가까이하는 자세'),
    'es': (r'pegada suavemente contra la pared',
           'de pie, cerca de la pared, sin prisa'),
    'fr': (r'plaqu(e|é)(e)? doucement contre le mur',
           'se tenir près du mur, sans hâte'),
    'pt': (r'encostado(a) na parede do quarto',
           'de pé junto à parede, sem pressa'),
})

_add('P0-CANNOT-TOUCH', 'A3', {
    'en': (r'Interlock their fingers behind their neck so they cannot touch back',
           'Let your fingers rest behind their neck, both of you still'),
    'zh': (r'双手扣在脑后不许回碰',
           '指尖轻轻搭在脑后，两个人都静静的'),
    'zh_tw': (r'雙手扣在腦後不許回碰',
              '指尖輕輕搭在腦後，兩個人都靜靜的'),
    'ja': (r'首の後ろで指を組ませ、触り返せないように',
           '首の後ろに指をそっと添えて、二人で静かに'),
    'ko': (r'목 뒤로 손을 맞잡아 다시 만지지 못하게',
           '목 뒤에 손을 살짝 얹고, 둘 다 가만히'),
    'es': (r'entrelazar sus dedos tras la nuca para que no pueda devolver el contacto',
           'dejar los dedos tras la nuca, los dos quietos'),
    'fr': (r'doigts enlacés derrière la nuque pour qu il ne puisse pas toucher',
           'doigts posés derrière la nuque, tous deux immobiles'),
    'pt': (r'entrelaçar os dedos atrás da nuca para que não possa tocar de volta',
           'dedos pousados atrás da nuca, os dois quietos'),
})

_add('P0-SURRENDER', 'A3', {
    'en': (r'lying back blindfolded in full surrender|full surrender',
           'lying back, eyes closed, letting go completely'),
    'zh': (r'仰躺蒙眼完全交出主动权',
           '仰躺着闭上眼，把节奏交给这一刻'),
    'zh_tw': (r'仰躺蒙眼完全交出主動權',
              '仰躺著閉上眼，把節奏交給這一刻'),
    'ja': (r'目隠しをして完全に身をゆだねる',
           '目を閉じて、流れに身をまかせて'),
    'ko': (r'눈을 가리고 완전히 맡김',
           '눈을 감고, 흐름에 천천히 맡기기'),
    'es': (r'tumbada vendada entregando el control',
           'tumbada con los ojos cerrados, dejándose llevar'),
    'fr': (r'allong(e|é)(e)? yeux bandés en total abandon',
           'allongé, les yeux fermés, en confiance'),
    'pt': (r'deitado(a) vendado(a) em entrega total',
           'deitado, de olhos fechados, deixando-se levar'),
})

_add('P0-PUNISH', 'A3', {
    'en': (r'punishment|punish(?:ing|ment)?',
           'playful dare'),
    'zh': (r'惩罚',
           '小挑战'),
    'zh_tw': (r'懲罰',
              '小挑戰'),
    'ja': (r'罰|お仕置き',
           'ちょっとした挑戦'),
    'ko': (r'벌칙|벌을',
           '작은 도전'),
    'es': (r'castigo|azotes|azote',
           'reto divertido'),
    'fr': (r'punition|fess(e|ées)|chatier',
           'petit défi'),
    'pt': (r'castigo|apanhar',
           'desafio divertido'),
})

# ==========================================================================
# P3 · N3 命令式 → 邀约式
# ==========================================================================
_add('P3-OBEY', 'N3', {
    'en': (r'\\bObey\\b|\\bComply\\b',
           'Follow'),
    'zh': (r'必须|一定要|照做|给我',
           '愿意的话就'),
    'zh_tw': (r'必須|一定要|照做|給我',
              '願意的話就'),
    'ja': (r'必ず|しなさい',
           'よかったら'),
    'ko': (r'반드시|해야 한다',
           '원한다면'),
    'es': (r'\\bPULSA\\b|\\bOBEDECE\\b',
           'si quieres'),
    'fr': (r'\\bOBEISSEZ\\b|\\bFAITES\\b',
           'si tu veux'),
    'pt': (r'\\bOBEDEÇA\\b|\\bTOQUE\\b',
           'se quiser'),
})

_add('P3-NO-LOOK', 'N3', {
    'en': (r'never break eye contact for a single second|no looking away|without looking away|no stopping|without stopping',
           'let the eye contact stay as long as it feels good'),
    'zh': (r'不许移开视线|不准移开|不许停',
           '愿意的话就多对视一会儿'),
    'zh_tw': (r'不許移開視線|不准移開|不許停',
              '願意的話就多對視一會兒'),
    'ja': (r'視線を逸らさないで|止めないで',
           '目が合う時間を楽しんで'),
    'ko': (r'시선을 돌리지 마|멈추지 마',
           '눈이 마주치는 시간을 즐겨요'),
    'es': (r'no apartes la mirada|no pares',
           'deja que la mirada dure lo que quieras'),
    'fr': (r'ne détourne pas le regard|n arrête pas',
           'laisse le regard durer autant que tu veux'),
    'pt': (r'não desvie o olhar|não pare',
           'deixe o olhar durar o quanto quiser'),
})

# ==========================================================================
# P0 · N5 公开场合 / 第三人 → 私密空间
# ==========================================================================
_add('P0-PUBLIC', 'N5', {
    'en': (r'in a totally normal, public place|in public|public place',
           'somewhere that feels like ours alone'),
    'zh': (r'公共场所|公开场合|在别人面前|在他人面前|办公室',
           '只有我们两个人的地方'),
    'zh_tw': (r'公共場所|公開場合|在別人面前|在他人面前|辦公室',
              '只有我們兩個人的地方'),
    'ja': (r'人前|皆の前|公共の場',
           '二人だけがいる場所'),
    'ko': (r'남들 앞|사람들 앞에서|공공의 장소',
           '둘만 있는 공간'),
    'es': (r'en público|lugar público|delante de todos',
           'en un lugar que sea solo nuestro'),
    'fr': (r'en public|endroit public|devant tout le monde',
           'dans un endroit qui n appartient qu à nous'),
    'pt': (r'em público|lugar público|na frente de todos',
           'em um lugar que seja só nosso'),
})

# ==========================================================================
# P0 · N9 数据流歧义 → 本地表述（否定式豁免由词表处理）
# ==========================================================================
_add('P0-SYNC', 'N9', {
    'en': (r'synced across|cloud sync|share with|nearby people',
           'shared only between the two of you'),
    'zh': (r'自动同步到|同步到|云端|附近的人|分享给好友',
           '只属于你们两个人'),
    'zh_tw': (r'自動同步到|同步到|雲端|附近的人|分享給好友',
              '只屬於你們兩個人'),
    'ja': (r'クラウド同期|クラウド保存|近くの人に|共有する',
           '二人だけのもの'),
    'ko': (r'클라우드 동기화|공유하기|근처 사람',
           '둘만의 것'),
    'es': (r'sincronizar en la nube|compartir con|personas cercanas',
           'solo entre los dos'),
    'fr': (r'synchroniser sur le cloud|partager avec|personnes à proximité',
           'entre nous deux seulement'),
    'pt': (r'sincronizar na nuvem|compartilhar com|pessoas perto',
           'só entre vocês dois'),
})

# ==========================================================================
# P1 · 剩余 A2 槽族
# ==========================================================================
_add('S-BITE-SOOTHE', 'A2', {
    'en': (r'bite playfully then soothe',
           'press lightly, then soothe with a kiss'),
    'zh': (r'先俏皮咬一口再安抚',
           '轻轻碰一下，再温柔地安抚'),
    'zh_tw': (r'先俏皮咬一口再安撫',
              '輕輕碰一下，再溫柔地安撫'),
    'ja': (r'軽く噛んでから落ち着かせる',
           'そっと触れて、やさしく鎮める'),
    'ko': (r'장난스럽게 깨문 뒤 달래기',
           '가볍게 스친 뒤, 다독이기'),
    'es': (r'morder juguetón y calmar',
           'rozar levemente y acariciar'),
    'fr': (r'mordre joueur puis apaiser',
           'effleurer puis apaiser'),
    'pt': (r'morder brincalhão e acalmar',
           'roçar levemente e acalmar'),
})

_add('S-GENTLE-NIBBLE', 'A2', {
    'en': (r'gently nibble|playful nibble|nibble',
           'soft kiss'),
    'zh': (r'轻咬',
           '轻吻'),
    'zh_tw': (r'輕咬',
              '輕吻'),
    'ja': (r'甘噛み|噛み',
           'やさしいキス'),
    'ko': (r'깨물|씹',
           '부드러운 입맞춤'),
    'es': (r'mordisque\w*|morder|mordisco',
           'beso suave'),
    'fr': (r'morsure|mordre',
           'baiser tendre'),
    'pt': (r'morder|mordida',
           'beijo suave'),
})

_add('S-DEEP-KISS', 'A2', {
    'en': (r'deep kiss|breathless kiss',
           'unhurried, lingering kiss'),
    'zh': (r'深吻',
           '绵长的吻'),
    'zh_tw': (r'深吻',
              '綿長的吻'),
    'ja': (r'深いキス|とろけるキス',
           'ゆっくり寄り添うキス'),
    'ko': (r'딥키스|깊은 키스',
           '천천히 스며드는 입맞춤'),
    'es': (r'beso profundo',
           'beso pausado y tierno'),
    'fr': (r'baiser profond',
           'baiser lent et tendre'),
    'pt': (r'beijo profundo',
           'beijo lento e terno'),
})

_add('S-STRADDLE', 'A2', {
    'en': (r'straddling your lap face-to-face|straddl\w+',
           'sitting close, face to face'),
    'zh': (r'跨坐',
           '面对面靠近坐着'),
    'zh_tw': (r'跨坐',
              '面對面靠近坐著'),
    'ja': (r'またがる|跨がる',
           '向かい合わせで寄り添う'),
    'ko': (r'무릎 위에 마주 보고 앉는 자세|올라타',
           '마주 보고 가까이 앉기'),
    'es': (r'a horcajadas|montar|cabalg\w+',
           'sentados muy cerca, cara a cara'),
    'fr': (r'à califourchon|chevaucher',
           'assis très près, face à face'),
    'pt': (r'a cavalo|montar',
           'sentados bem perto, frente a frente'),
})

_add('S-UNDRESS', 'A2', {
    'en': (r'undress(?:ing)?|unbutton(?:ing)?|strip',
           'loosen'),
    'zh': (r'褪衣|脱掉|脱衣',
           '松开'),
    'zh_tw': (r'褪衣|脫掉|脫衣',
              '鬆開'),
    'ja': (r'脱衣|脱がせる',
           'ゆるめる'),
    'ko': (r'탈의|벗겨',
           '느슨하게'),
    'es': (r'desnud\w*',
           'aflojar'),
    'fr': (r'déshabill\w*',
           'desserrer'),
    'pt': (r'despir\w*',
           'afrouxar'),
})

_add('S-TONGUE-OPEN', 'A2', {
    'en': (r'open-mouthed kiss|wet kiss|tongue trail',
           'soft, lingering kiss'),
    'zh': (r'舌吻|舔|吮',
           '温柔的吻'),
    'zh_tw': (r'舌吻|舔|吮',
              '溫柔的吻'),
    'ja': (r'舌先|舐め',
           'やさしいキス'),
    'ko': (r'혀로|핥',
           '부드러운 입맞춤'),
    'es': (r'beso con lengua|lengua',
           'beso tierno'),
    'fr': (r'baiser avec la langue|langue',
           'baiser tendre'),
    'pt': (r'beijo com língua|língua',
           'beijo terno'),
})
'''

MARK = "# P0 · A3"
src = io.open(TARGET, encoding='utf-8').read()
if 'P0-PINNED' in src:
    print('already applied')
else:
    io.open(TARGET, 'w', encoding='utf-8', newline='\n').write(src.rstrip() + '\n' + BLOCK)
    print('appended', len(BLOCK), 'chars')

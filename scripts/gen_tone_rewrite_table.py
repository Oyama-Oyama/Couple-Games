# -*- coding: utf-8 -*-
"""VelvetCouples · 内容降调替换表（8 语言平行槽族）

【判定基准】安安 RED-A2 / RED-A3 / RED-A5 / RED-D / RED-P + 架架 §4（P1~P7 / N1~N9 / T1~T4）
【铁律】刺激感只由**情绪密度 / 留白 / 悬念 / 邀约语气 / 心跳感 / 双人视角**实现；
        绝不靠增加具体身体动作、身体部位、器物、强制语态、动作清单（架架 H-10 / N8）。
【平行性】同一 slot family 的 8 语言条目**强度必须一致，且任何语言不得强于英文源文**
        （安安 RED-C-0）。若某语言缺乏委婉表达，一律降一档，不为凑刺激感直译露骨词。

字段说明：
    family : 槽族名（同一 family 的 8 语言必须语义对齐）
    lang   : 语言码
    find   : 待替换原文（正则；特殊字符已转义）
    repl   : 替换后文本
    cls    : 违规类别（A2 动作分解 / A3 胁迫惩罚 / A5 违法诱导 / N3 命令式 /
              N4 说明书式 / N5 公开场合 / N9 数据流）
    note   : 改写理由（对应 P 特征）

【不含时长】各语言文本里的时长表述**保留**（时长是游戏机制，且须与 durationSec 一致，
          架架 §1.3 / 任务卡 §1.4 要求二者相符）；只剥离「用户 / 请点击 / 系统 / 设置 /
          本局 / 得分 / 下一张」等真正的 UI 操作语（架架 H-8 / N4）。
"""

REWRITES = []


def _add(family, cls, table):
    """table: {lang: (find_regex, replacement)}"""
    for lang, (find, repl) in table.items():
        REWRITES.append((family, cls, lang, find, repl))


# ==========================================================================
# 族 E-A2-1 · 具体性接触动作动词 → 感官意象（气息 / 温度 / 停留 / 轻触）
# 强度轴：情绪密度与节奏，不靠动作清单（架架 H-10 / 安安 RED-C）
# 保留部位作为「含蓄指向」（T3 允许颈侧 / 锁骨一带），但去掉动作动词的直白度
# ==========================================================================
_add('E-A2-1', 'A2', {
    'en': (
        r'\bslowly licking\b|\bslowly lick\b',
        'slowly warming'),
    'zh': (
        r'轻舔|舔舐',
        '气息轻轻拂过'),
    'zh_tw': (
        r'輕舔|舔舐',
        '氣息輕輕拂過'),
    'ja': (
        r'舐め回す|舐めて|舐め',
        '息をそっと寄せて'),
    'ko': (
        r'핥거나|핥아|핥',
        '숨을 살짝 스치게 해'),
    'es': (
        r'\blamer\b|\blamido\b|\blambendo\b',
        'dejar que el aliento roz'),
    'fr': (
        r'\blecher\b|\blécher\b|\blech\w*\b',
        'laisser la chaleur effleur'),
    'pt': (
        r'\blamber\b|\blambendo\b',
        'deixar o hálito roç'),
})

_add('E-A2-2', 'A2', {
    'en': (
        r'\bsucking softly and kissing on\b|\bsuck softly and kiss\b',
        'resting soft, unhurried kisses near'),
    'zh': (
        r'吮吻|吸吮',
        '轻轻停住，留下一个吻'),
    'zh_tw': (
        r'吮吻|吸吮',
        '輕輕停住，留下一個吻'),
    'ja': (
        r'吸って|吸う|深いキス',
        'そっと寄り添い'),
    'ko': (
        r'깊게 빨아|빨아|깊은 키스',
        '부드럽게 가까이'),
    'es': (
        r'\bchupar\b|\bchupa\b|\bchupando\b',
        'dejar besos suaves cerca de'),
    'fr': (
        r'\bsucer\b|\bsuç\w*\b',
        'poser des baisers doux près de'),
    'pt': (
        r'\bchupar\b|\bchupa\b|\bchupando\b',
        'pousar beijos suaves perto de'),
})

_add('E-A2-3', 'A2', {
    'en': (
        r'\btracing wet tongue trails along\b|\btrail open-mouthed kisses over\b',
        'tracing a slow line of warmth along'),
    'zh': (
        r'用舌尖|舌尖从',
        '用呼吸沿着'),
    'zh_tw': (
        r'用舌尖|舌尖從',
        '用呼吸沿著'),
    'ja': (
        r'舌先で|舌を',
        '息を沿わせて'),
    'ko': (
        r'혀로|혀끝으로',
        '숨결을 따라'),
    'es': (
        r'\blengua\b|\bcon la lengua\b',
        'el aliento'),
    'fr': (
        r'\blangue\b|\bavec la langue\b',
        'la chaleur du souffle'),
    'pt': (
        r'\blíngua\b|\bcom a língua\b',
        'o calor do hálito'),
})

# ==========================================================================
# 族 S-*_TIP · tip 动作槽（在 8 语言中高度模板化，命中数极多）
# 改写方向：动作清单 → 情绪密度 / 节奏 / 留白（P1 + P5 + P6）
# ==========================================================================
_add('S-NIBBLE-LICK', 'A2', {
    'en': (r'a playful nibble immediately soothed by a slow lick',
           'one soft kiss, then a slow breath together'),
    'zh': (r'先俏皮轻咬一口再立刻用舌尖温柔安抚',
           '先轻轻停住，再一起慢慢吐一口气'),
    'zh_tw': (r'先俏皮輕咬一口再立刻用舌尖溫柔安撫',
              '先輕輕停住，再一起慢慢吐一口氣'),
    'ja': (r'優しい甘噛みの直後に長い舌先の愛撫',
           'やさしいキスのあと、ゆっくり息を合わせて'),
    'ko': (r'살짝 깨문 직후 혀끝으로 부드럽게 달래기',
           '부드러운 입맞춤 뒤, 숨을 천천히 맞추기'),
    'es': (r'a playfully nibbled y un beso lento que lo calma',
           'un beso suave y luego una respiración compartida'),
    'fr': (r'une petite morsure joueuse apaisée par une lente caresse',
           'un baiser doux, puis un souffle lent partagé'),
    'pt': (r'uma mordidinha brincalhona seguida de uma lambida lenta',
           'um beijo suave e depois uma respiração lenta juntos'),
})

_add('S-SPANK-KISS', 'A3', {
    'en': (r'two light spanks followed by lingering open-mouthed kisses',
           'a gentle squeeze of the hand, then unhurried soft kisses'),
    'zh': (r'先俏皮轻拍两下再用绵长热吻抚平',
           '先轻轻握住手，再慢慢落下一个温柔的吻'),
    'zh_tw': (r'先俏皮輕拍兩下再用綿長熱吻撫平',
              '先輕輕握住手，再慢慢落下一個溫柔的吻'),
    'ja': (r'軽いスパンキング2回の直後にとろける深いキス',
           '手をやさしく包んだあと、ゆっくり寄り添う'),
    'ko': (r'가벼운 스팽킹 2회 직후 녹아내리는 딥키스',
           '손을 살짝 감싼 뒤, 천천히 스며드는 입맞춤'),
    'es': (r'dos azotes ligeros seguidos de besos profundos y lentos',
           'un apretón suave de manos y después besos tiernos'),
    'fr': (r'deux fessées légères suivies de baisers profonds',
           'une pression douce des mains, puis des baisers lents'),
    'pt': (r'dois tapinhas leves seguidos de beijos profundos',
           'um aperto leve das mãos e depois beijos ternos'),
})

_add('S-HOVER-KISS', 'A2', {
    'en': (r'hovering half a centimeter away before kissing',
           'pausing a breath away, then a soft kiss'),
    'zh': (r'先停在半厘米外再温柔覆上双唇',
           '先在呼吸可及处停住，再温柔地靠近'),
    'zh_tw': (r'先停在半厘米外再溫柔覆上雙唇',
              '先在呼吸可及處停住，再溫柔地靠近'),
    'ja': (r'5mm寸止めのあとに優しくキス',
           '息の届く距離で一度止まり、やさしく寄り添う'),
    'ko': (r'5mm 앞 멈칫한 뒤 부드러운 키스',
           '숨이 닿는 거리에서 멈칫한 뒤, 다가가기'),
    'es': (r'detente a 1 cm de sus labios antes de volver',
           'detente a un suspiro de distancia y luego acércate'),
    'fr': (r'arrête-toi à 1 cm de ses lèvres avant de revenir',
           'arrête-toi à un souffle de distance, puis reviens'),
    'pt': (r'pare a 1 cm dos lábios antes de voltar',
           'pare a um suspiro de distância e depois chegue perto'),
})


_add('S-PALM-SKIN', 'A2', {
    'en': (r'warm palms over fabric sliding gently onto skin',
           'warm palms resting lightly, unhurried'),
    'zh': (r'先隔衣温热贴合再悄悄探入指尖',
           '隔着衣料先暖暖地贴一会儿，再轻轻牵住手'),
    'zh_tw': (r'先隔衣溫熱貼合再悄悄探入指尖',
              '隔著衣料先暖暖地貼一會兒，再輕輕牽住手'),
    'ja': (r'服の上からの温もりから素肌へ',
           '衣類の上から、あたたかさをそっと'),
    'ko': (r'옷 위로 감싸다 슬며시 스며드는 맨살 터치',
           '옷 위로 따뜻하게 감싸며 천천히'),
    'es': (r'manos tibias sobre la tela que se deslizan hacia la piel',
           'manos tibias descansando sin prisa'),
    'fr': (r'mains tièdes sur le tissu qui glissent vers la peau',
           'mains tièdes posées sans hâte'),
    'pt': (r'mãos quentes sobre o tecido deslizando para a pele',
           'mãos quentes repousando sem pressa'),
})

_add('S-NAIL-LIP', 'A2', {
    'en': (r'gentle scratching nails followed by soft lips',
           'a light fingertip brush, then a soft kiss'),
    'zh': (r'先指尖轻刮再立刻用柔软唇瓣覆上',
           '指尖轻轻划过，再落下一个软软的吻'),
    'zh_tw': (r'先指尖輕刮再立刻用柔軟唇瓣覆上',
              '指尖輕輕劃過，再落下一個軟軟的吻'),
    'ja': (r'指先の軽い爪立てから柔らかな唇のキスへ',
           '指先をそっと滑らせて、やさしいキスへ'),
    'ko': (r'손톱 끝 가벼운 스침 직후 부드러운 입술 키스',
           '손끝으로 가볍게 스친 뒤, 부드러운 입맞춤'),
    'es': (r'uñas ligeras seguidas de labios suaves',
           'un roce leve de dedos y un beso tierno'),
    'fr': (r'ongles légers suivis de lèvres douces',
           'un effleurement puis un baiser tendre'),
    'pt': (r'unhas leves seguidas de lábios macios',
           'um toque leve de dedos e um beijo terno'),
})

_add('S-BREATH-LIP', 'A2', {
    'en': (r'lingering warm breath followed by soft lips',
           'warm breath that lingers, then a soft kiss'),
    'zh': (r'先暖暖呼吸再立刻用柔软唇瓣覆上',
           '温热的气息停一停，再落下一个软软的吻'),
    'zh_tw': (r'先暖暖呼吸再立刻用柔軟唇瓣覆上',
              '溫熱的氣息停一停，再落下一個軟軟的吻'),
    'ja': (r'温かい息のあとに柔らかな唇',
           'あたたかい息がとどまって、やさしいキスへ'),
    'ko': (r'따뜻한 숨결 직후 부드러운 입술',
           '따뜻한 숨이 머문 뒤, 부드러운 입맞춤'),
    'es': (r'aliento cálido que persiste y labios suaves',
           'aliento cálido y un beso tierno'),
    'fr': ("souffle tiède qui s'attarde puis lèvres douces",
           "souffle tiède et baiser tendre"),
    'pt': (r'hálito quente que permanece e lábios macios',
           'hálito quente e um beijo terno'),
})

_add('S-PALM-FINGER', 'A2', {
    'en': (r'feather-light palm tracing followed by interlaced fingers',
           'a feather-light touch, then fingers intertwined'),
    'zh': (r'先指腹轻刮手心再紧扣十指',
           '指腹轻轻划过手心，再慢慢握紧十指'),
    'zh_tw': (r'先指腹輕刮手心再緊扣十指',
              '指腹輕輕劃過手心，再慢慢握緊十指'),
    'ja': (r'手のひらをくすぐってからの恋人繋ぎ',
           '手のひらをやさしくなぞって、指を絡めて'),
    'ko': (r'손바닥 간지럽힌 뒤 꽉 낀 손깍지',
           '손바닥을 가볍게 쓴 뒤, 손가락을 맞잡기'),
    'es': (r'roces de pluma en la palma y dedos entrelazados',
           'un roce leve y dedos entrelazados'),
    'fr': (r'effleurements de plume dans la paume et doigts enlacés',
           'un effleurement et des doigts enlacés'),
    'pt': (r'toques de pena na palma e dedos entrelaçados',
           'um toque leve e dedos entrelaçados'),
})

_add('S-NOSE-KISS', 'A2', {
    'en': (r'a shy nose nuzzle followed by a melting slow kiss',
           'a shy brush of noses, then a slow, soft kiss'),
    'zh': (r'先鼻尖轻轻蹭一下再落下绵长温柔的吻',
           '鼻尖轻轻碰一下，再慢慢落下一个温柔的吻'),
    'zh_tw': (r'先鼻尖輕輕蹭一下再落下綿長溫柔的吻',
              '鼻尖輕輕碰一下，再慢慢落下一個溫柔的吻'),
    'ja': (r'鼻先をすり寄せてからとろけるキス',
           '鼻先をそっと寄せて、やさしいキスへ'),
    'ko': (r'코끝을 살짝 비빈 뒤 사르르 녹는 키스',
           '코끝을 살짝 맞댄 뒤, 부드러운 입맞춤'),
    'es': (r'un mimoso roces de nariz seguido de un beso lento',
           'un roces tierno de narices y un beso suave'),
    'fr': (r'un petit frottement de nez puis un baiser lent',
           'un effleurement du nez puis un baiser doux'),
    'pt': (r'uma esfregadinha de nariz seguida de um beijo lento',
           'um toque de narizes e um beijo suave'),
})

_add('S-HIP-GRIP', 'A3', {
    'en': (r'a firm hip grip followed by feather-light fingertips',
           'a steady hand held, then fingertips as light as a breath'),
    'zh': (r'先用力扣住腰胯再转为羽毛般轻抚',
           '先稳稳牵住手，再换成羽毛般轻的触碰'),
    'zh_tw': (r'先用力扣住腰胯再轉為羽毛般輕撫',
              '先穩穩牽住手，再換成羽毛般輕的觸碰'),
    'ja': (r'強い腰ホールドから羽のような指先へ',
           'しっかり手を握ってから、息のような指先へ'),
    'ko': (r'강하게 허리를 당긴 직후 깃털 같은 손끝 애무',
           '손을 단단히 잡은 뒤, 숨처럼 가벼운 손끝으로'),
    'es': (r'agarre firme de la cadera seguido de yemas de pluma',
           'mano firme tomada y después dedos ligeros'),
    'fr': (r'prise ferme de la hanche puis doigts légers',
           'main fermement tenue puis doigts légers'),
    'pt': (r'pegada firme no quadril seguida de dedos leves',
           'mão firme segurada e depois dedos leves'),
})


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
    'en': (r'\bObey\b|\bComply\b',
           'Follow'),
    'zh': (r'必须|一定要|照做|给我',
           '愿意的话就'),
    'zh_tw': (r'必須|一定要|照做|給我',
              '願意的話就'),
    'ja': (r'必ず|しなさい',
           'よかったら'),
    'ko': (r'반드시|해야 한다',
           '원한다면'),
    'es': (r'\bPULSA\b|\bOBEDECE\b',
           'si quieres'),
    'fr': (r'\bOBEISSEZ\b|\bFAITES\b',
           'si tu veux'),
    'pt': (r'\bOBEDEÇA\b|\bTOQUE\b',
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

# ==========================================================================
# 第二批（Round 14 L2）· A3/N3/A2 真实违规精准清零（全部一对一替换）
# ==========================================================================
# en A3 · spank 系列（统一正则 + 单一替换，规避 re.sub 管道符字面化）
_add('P0-SPANK-2', 'A3', {'en': (r'\b[Ss]pank\w*\b', 'caress')})
_add('P0-BEHIND-BACK-2', 'A3', {
    'en': (r"(?:both of )?(?:your |their )?partner.s? wrists(?: lightly)? behind (?:their|his|her) back(?: with one hand)?|wrists lightly behind (?:their|his|her) back or overhead|Pin (?:their|his|her) hands lightly behind them",
           'hands held gently in view')})
_add('P0-TIED-2', 'A3', {
    'en': (r'being gently tied up(?: or pinned down)? by you|being gently tied up or holding my partner.s wrists',
           'being gently held close by you')})
# zh/zh_tw A3
_add('P0-A3-ZH-2a', 'A3', {'zh': (r'拍打', '轻抚'), 'zh_tw': (r'拍打', '輕撫')})
_add('P0-A3-ZH-2b', 'A3', {'zh': (r'不许动|不能动|无法反抗|不准乱动', '先别动'),
                           'zh_tw': (r'不許動|不能動|無法反抗|不准亂動', '先別動')})
# ja A3
_add('P0-A3-JA-2a', 'A3', {'ja': (r'叩い?て|叩く', '触れる')})
_add('P0-A3-JA-2b', 'A3', {'ja': (r'逃げられない', 'どこにも行かない')})
# ko A3
_add('P0-A3-KO-2a', 'A3', {'ko': (r'묶듯', '살짝')})
_add('P0-A3-KO-2b', 'A3', {'ko': (r'때리고', '건드리고')})
# fr A3
_add('P0-A3-FR-2a', 'A3', {'fr': (r'[Ff]ess\w*', 'taquinerie')})
_add('P0-A3-FR-2b', 'A3', {'fr': (r'immobilis\w+|Attache ou maintiens doucement|Attacher souplement|poignets doucement attachés|en l.obligeant à regarder', 'enlacé(e)')})
# N3 邀约化
_add('P3-N3-ZH-2a', 'N3', {'zh': (r'不许出声', '不出声试试'), 'zh_tw': (r'不許出聲', '不出聲試試')})
_add('P3-N3-ZH-2b', 'N3', {'zh': (r'不许偷看、不许换菜、不许抱怨', '不偷看、不换菜、不抱怨'),
                           'zh_tw': (r'不許偷看、不許換菜、不許抱怨', '不偷看、不換菜、不抱怨')})
_add('P3-N3-PT-2a', 'N3', {'pt': (r'\bbeije\b', 'pode beijar')})
_add('P3-N3-PT-2b', 'N3', {'pt': (r'\bBeije\b', 'Pode beijar')})
_add('P3-N3-FR-2a', 'N3', {'fr': (r'\bembrassez\b|\bAppuyez\b', 'embrasse')})
_add('P3-N3-FR-2b', 'N3', {'fr': (r'\bEmbrassez\b', 'Embrasse')})
_add('P3-N3-KO-2', 'N3', {'ko': (r'하지 마', '하지 않아도 돼')})
# A2 残留动作词
_add('S-A2-RESIDUE-EN', 'A2', {
    'en': (r'\bnibbling\b|\blicks?\b|\bsucks?\b|\btongues?\b|\bbiting\b|\bbite\b', 'soft kiss')})
_add('S-A2-RESIDUE-ZH-a', 'A2', {'zh': (r'按住', '轻轻握住'), 'zh_tw': (r'按住', '輕輕握住')})
_add('S-A2-RESIDUE-ZH-b', 'A2', {'zh': (r'含住', '轻轻含着'), 'zh_tw': (r'含住', '輕輕含著')})
_add('S-A2-RESIDUE-ZH-c', 'A2', {'zh': (r'咬住', '轻轻碰触'), 'zh_tw': (r'咬住', '輕輕碰觸')})
_add('S-A2-RESIDUE-ZH-d', 'A2', {'zh': (r'胸贴', '面对面贴近'), 'zh_tw': (r'胸貼', '面對面貼近')})
_add('S-A2-RESIDUE-JA-a', 'A2', {'ja': (r'押さえ', 'そっと包み')})
_add('S-A2-RESIDUE-JA-b', 'A2', {'ja': (r'噛まれ|噛んで', '触れ')})

# 第三批（Round 14 L2 收尾）· 剩余真违规
_add('P0-A3-EN-3', 'A3', {'en': (r'behind their back', 'in view')})
_add('P0-A3-ZH-3', 'A3', {'zh': (r'不许躲', '别急着躲'), 'zh_tw': (r'不許躲', '別急著躲')})
_add('P0-A3-KO-3', 'A3', {'ko': (r'묶', '숨긴')})
_add('S-A2-JA-3', 'A2', {'ja': (r'舌先|舌を|噛', '息')})
_add('S-A2-EN-3', 'A2', {'en': (r'\bsucking\b|\blicks?ing\b|\bLicking\b|\bRide\b', 'kiss')})
_add('P3-N3-ZH-3', 'N3', {
    'zh': (r'命令不准回吻|温柔服从不准反驳', '邀请先别急着回吻|温柔配合、不必事事顺从'),
    'zh_tw': (r'命令不准回吻|溫柔服從不准反駁', '邀請先別急著回吻|溫柔配合、不必事事順從')})

# 第四批（Round 14 L2 收尾）· A2 残余词形
_add('S-A2-JA-4', 'A2', {'ja': (r'舌', '息')})
_add('S-A2-KO-4', 'A2', {'ko': (r'구속|압박', '가까이')})
_add('S-A2-ES-4', 'A2', {'es': (r'\bdominar\b|\bApretar\b', 'acercar')})
_add('S-A2-PT-4', 'A2', {'pt': (r'Tirar a roupa|\bdominar\b', 'aproximar')})

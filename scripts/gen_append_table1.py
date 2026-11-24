# -*- coding: utf-8 -*-
"""追加剩余槽族到 gen_tone_rewrite_table.py（一次性，幂等）"""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, 'scripts', 'gen_tone_rewrite_table.py')

BLOCK = r'''

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
    'fr': (r'souffle tiède qui s'attarde puis lèvres douces',
           'souffle tiède et baiser tendre'),
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
'''

MARK = "# ==========================================================================\n# 族 E-A2-1"
src = io.open(TARGET, encoding='utf-8').read()
if 'S-HIP-GRIP' in src:
    print('already applied')
else:
    src = src.rstrip() + '\n' + BLOCK
    io.open(TARGET, 'w', encoding='utf-8', newline='\n').write(src)
    print('appended, total chars', len(src))

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 12 个新游戏的 8 语言 strings.xml 文案（幂等，重复运行不产生重复项）。"""
import os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')

MODES = [
    ('most_likely_to',  'Most Likely To',      '谁更有可能',        '誰更有可能',        'どっちがやりそう？', '누가 더 그럴까?',      'Quién es más probable',      'Qui est le plus susceptible',      'Quem é mais provável'),
    ('love_language',   'Love Language',       '爱的语言',          '愛的語言',          '愛の言語',           '사랑의 언어',         'Lenguaje de amor',           'Langage de l\'amour',             'Linguagem do amor'),
    ('daily_question',  'Today\'s Question',   '今日心语',          '今日心語',          '今日の質問',         '오늘의 질문',         'Pregunta de hoy',            'Question du jour',                'Pergunta do dia'),
    ('bucket_list',     'Our Bucket List',     '共同心愿单',        '共同心願單',        'ふたりのやりたいこと', '우리의 버킷리스트',  'Nuestra lista de deseos',     'Notre liste à deux',              'Nossa lista a dois'),
    ('riddle_me',       'Riddle Me',           '情话谜题',          '情話謎題',          'なぞなぞラブ',       '사랑 수수께끼',       'Adivina y enamora',          'Devine et séduis',                'Adivinhe e seduza'),
    ('hot_seat',        'Hot Seat',            '快问三连',          '快問三連',          '連続3問',            '연속 3문제',          'Silla caliente',              'Siège brûlant',                   'Banco quente'),
    ('secret_mission',  'Secret Mission',      '私密任务',          '私密任務',          '秘密のミッション',   '비밀 미션',           'Misión secreta',              'Mission secrète',                 'Missão secreta'),
    ('ldr_sync',        'Long-Distance Sync',  '异地同步',          '異地同步',          '同時シンクロ',       '원격 싱크',           'Sincronía a distancia',       'Synchro à distance',              'Sincronia à distância'),
    ('scenario_choice', 'Scenario Choice',     '情境抉择',          '情境抉擇',          'もしもの選択',       '상황 선택',           'Elige el escenario',          'Choix de scénario',               'Escolha de cenário'),
    ('memory_lane',     'Memory Lane',         '回忆探秘',          '回憶探秘',          '思い出の道',         '추억 길',             'Camino de recuerdos',         'Chemin des souvenirs',            'Caminho das memórias'),
    ('conversation_starter','Conversation Starter','心事话题',      '心事話題',          '心の話題',           '마음의 화제',         'Para empezar a hablar',       'Pour lancer la conversation',     'Para começar a conversar'),
    ('forfeit_wheel',   'Forfeit Wheel',       '惩罚转盘',          '懲罰轉盤',          '罰ゲームルーレット', '벌칙 룰렛',           'Ruleta de castigos',          'Roulette des gages',              'Roleta de castigos'),
]

DESCS = {
'en': [
 'Answer the same question and reveal who is more likely. A quick chemistry check.',
 'Pick the expression that makes you feel most loved, then compare your love languages.',
 'One meaningful question a day. Answer separately, then reveal what your partner said.',
 'Draw a wish, mark if you both want to try it, and collect your shared dreams.',
 'A playful riddle with a sweet twist. Guess together and reveal the answer.',
 'Three rapid-fire questions against the clock. Keep your answers honest and quick.',
 'Draw a private mission just for the two of you. Complete it before drawing again.',
 'Sync a tiny moment together from anywhere - a timer counts you both in.',
 'Face a what-if situation and choose what you would really do. Then compare.',
 'Dig into the little memories only the two of you share.',
 'A gentle prompt to skip the small talk and start a real conversation.',
 'Spin for a playful forfeit. Keep it light, consensual and fun.',
],
'zh': [
 '回答同一个问题，揭晓谁更有可能——快速检验你们的默契。',
 '选出最让你感到被爱的表达方式，然后对比彼此的爱的语言。',
 '每天一个有深度的问题。各自作答，再揭晓对方的答案。',
 '抽取一个愿望，标记双方是否想一起实现，收集共同的梦想。',
 '带甜蜜反转的趣味谜题，一起猜、一起揭晓答案。',
 '限时三连快问，答案要诚实也要快。',
 '抽取一个只属于你们两人的私密任务，完成后再抽下一个。',
 '无论相隔多远，同步完成一个小moment——倒计时带你们一起进入。',
 '面对一个假设情境，选出你真正会做的事，然后对比答案。',
 '挖掘只有你们两人才懂的那些小回忆。',
 '一个温和的话题，跳过寒暄，开启真正的对话。',
 '转动轻松的小惩罚，保持自愿、轻快、好玩。',
],
'zh_tw': [
 '回答同一個問題，揭曉誰更有可能——快速檢驗你們的默契。',
 '選出最讓你感到被愛的表達方式，然後比對彼此的愛的語言。',
 '每天一個有深度的問題。各自作答，再揭曉對方的答案。',
 '抽取一個願望，標記雙方是否想一起實現，收集共同的夢想。',
 '帶甜蜜反轉的趣味謎題，一起猜、一起揭曉答案。',
 '限時三連快問，答案要誠實也要快。',
 '抽取一個只屬於你們兩人的私密任務，完成後再抽下一個。',
 '無論相隔多遠，同步完成一個小時刻——倒數計時帶你們一起進入。',
 '面對一個假設情境，選出你真正會做的事，然後比對答案。',
 '挖掘只有你們兩人才懂的那些小回憶。',
 '一個溫和的話題，跳過寒暄，開啟真正的對話。',
 '轉動輕鬆的小懲罰，保持自願、輕快、好玩。',
],
'ja': [
 '同じ質問に答えて、どちらがやりそうかを披露。相性を手軽にチェック。',
 'いちばん愛を感じる表現を選んで、ふたりの愛の言語を比べよう。',
 '毎日ひとつ、深い質問。別々に答えてから、相手の答えを披露。',
 '願い事を引いて、ふたりで挑戦したいかをマーク。共通の夢を集めよう。',
 '甘いオチつきのなぞなぞ。一緒に考えて、答えを披露。',
 '時間制限つきの連続3問。正直に、素早く答えて。',
 'ふたりだけの秘密のミッションを引こう。終わらせてから次へ。',
 '離れていても、小さな瞬間をシンクロ。カウントダウンでふたり同時に。',
 'もしもの場面で、本当にどうするかを選択。その後で答えを比べよう。',
 'ふたりだけが知っている、小さな思い出を掘り起こそう。',
 '世間話を飛び越えて、本当の会話を始めるためのやさしいきっかけ。',
 '軽い罰ゲームを回そう。自愿で、楽しく、あくまでゲームとして。',
],
'ko': [
 '같은 질문에 답하고谁 더 그럴지 공개하세요. 궁합을 가볍게 확인하는 게임.',
 '가장 사랑받는다고 느끼는 표현을 고르고, 서로의 사랑의 언어를 비교해 보세요.',
 '하루에 하나, 깊이 있는 질문. 따로 답한 뒤 상대의 답을 공개하세요.',
 '소원을 뽑고 함께 해보고 싶은지 표시하세요. 둘만의 버킷리스트를 모으세요.',
 '달콤한 반전이 있는 퀴즈. 함께 맞히고 정답을 공개하세요.',
 '시간 제한 연속 3문제. 정직하고 빠르게 답하세요.',
 '둘만의 비밀 미션을 뽑으세요. 완료한 뒤 다음으로 넘어가세요.',
 '멀리 떨어져 있어도 작은 순간을 동기화. 카운트다운으로 함께 시작하세요.',
 '상황을 가정하고 정말 어떻게 할지 선택한 뒤 답을 비교하세요.',
 '둘만 아는 작은 추억을 찾아보세요.',
 '가벼운 잡담을 건너뛰고 진짜 대화를 시작하는 부드러운 질문.',
 '가벼운 벌칙 룰렛. 자발적으로, 즐겁게, 게임으로만 즐기세요.',
],
'es': [
 'Respondan lo mismo y descubran quién es más probable. Una prueba rápida de química.',
 'Elige la expresión que más te hace sentir amado y comparen sus lenguajes de amor.',
 'Una pregunta profunda al día. Respondan por separado y luego revelen sus respuestas.',
 'Saca un deseo, marquen si ambos quieren intentarlo y reúnan sus sueños compartidos.',
 'Un acertijo juguetón con un giro dulce. Adivinen juntos y revelen la respuesta.',
 'Tres preguntas rápidas contra el reloj. Responde con honestidad y rapidez.',
 'Saca una misión privada solo para los dos. Cómplétala antes de sacar otra.',
 'Sincronicen un pequeño momento desde donde estén: una cuenta atrás los une.',
 'Enfrenten una situación hipotética y elijan qué harían realmente. Luego comparen.',
 'Profundicen en esos pequeños recuerdos que solo ustedes comparten.',
 ' Una invitación suave para dejar la charla superficial y empezar una conversación real.',
 'Gira la ruleta de castigos ligeros. Que sea consentido, breve y divertido.',
],
'fr': [
 'Répondez à la même question et découvrez qui est le plus susceptible. Un test de complicité express.',
 'Choisissez l\'expression qui vous fait vous sentir le plus aimé, puis comparez vos langages de l\'amour.',
 'Une question profonde par jour. Répondez séparément, puis révélez vos réponses.',
 'Tirez un souhait, indiquez si vous voulez tous les deux le réaliser et rassemblez vos rêves communs.',
 'Une devinette ludique avec une touche sucrée. Devinez à deux et révélez la réponse.',
 'Trois questions rapides contre la montre. Répondez honnêtement et vite.',
 'Tirez une mission privée rien que pour vous deux. Accomplissez-la avant d\'en tirer une autre.',
 'Synchronisez un petit moment où que vous soyez : un compte à rebours vous réunit.',
 'Facez une situation hypothétique et choisissez ce que vous feriez vraiment. Puis comparez.',
 'Plongez dans ces petits souvenirs que vous seuls partagez.',
 'Une invitation douce pour passer de la petite conversation à un vrai échange.',
 'Faites tourner la roue des gages légers. Consenti, bref et amusant.',
],
'pt': [
 'Respondam à mesma pergunta e descubram quem é mais provável. Um teste rápido de sintonia.',
 'Escolha a expressão que mais faz você se sentir amado e comparem suas linguagens do amor.',
 'Uma pergunta profunda por dia. Respondam separadamente e depois revelem as respostas.',
 'Tire um desejo, marquem se os dois querem realizá-lo e reúnam seus sonhos compartilhados.',
 'Uma charada divertida com um toque doce. Adivinhem juntos e revelem a resposta.',
 'Três perguntas rápidas contra o relógio. Responda com honestidade e agilidade.',
 'Tire uma missão privada só para vocês dois. Conclua antes de tirar outra.',
 'Sincronizem um pequeno momento de onde estiverem: uma contagem regressiva os une.',
 'Enfrentem uma situação hipotética e escolham o que fariam de verdade. Depois comparem.',
 'Mergulhem nas pequenas memórias que só vocês dois compartilham.',
 'Um convite suave para deixar a conversa superficial e começar uma conversa de verdade.',
 'Gire a roleta de castigos leves. Consentido, rápido e divertido.',
],
}

COMMON = {
'en': [('social_reveal','Reveal answer'),('social_done','Done'),('social_next','Next one'),('social_skip','Skip'),('social_again','Play again'),('social_match','You match!'),('social_miss','Different answers - interesting!'),('social_choice_me','Me'),('social_choice_you','You'),('social_choice_both','Both'),('social_choice_neither','Neither'),('social_timeup','Time is up'),('social_round','Round'),('social_of','of'),('social_result','Result'),('social_start','Start'),('social_hint','Take your time and be honest.')],
'zh': [('social_reveal','揭晓答案'),('social_done','完成了'),('social_next','下一张'),('social_skip','跳过'),('social_again','再来一轮'),('social_match','答案一致！'),('social_miss','答案不同——有意思！'),('social_choice_me','我'),('social_choice_you','你'),('social_choice_both','都是'),('social_choice_neither','都不是'),('social_timeup','时间到'),('social_round','第'),('social_of','/'),('social_result','结果'),('social_start','开始'),('social_hint','慢慢来，诚实作答。')],
'zh_tw': [('social_reveal','揭曉答案'),('social_done','完成了'),('social_next','下一張'),('social_skip','跳過'),('social_again','再來一輪'),('social_match','答案一致！'),('social_miss','答案不同——有意思！'),('social_choice_me','我'),('social_choice_you','你'),('social_choice_both','都是'),('social_choice_neither','都不是'),('social_timeup','時間到'),('social_round','第'),('social_of','/'),('social_result','結果'),('social_start','開始'),('social_hint','慢慢來，誠實作答。')],
'ja': [('social_reveal','答えを見る'),('social_done','完了'),('social_next','次へ'),('social_skip','スキップ'),('social_again','もう一度'),('social_match','一致したよ！'),('social_miss','違う答え、面白いね！'),('social_choice_me','私'),('social_choice_you','あなた'),('social_choice_both','両方'),('social_choice_neither','どちらも'),('social_timeup','時間切れ'),('social_round','ラウンド'),('social_of','/'),('social_result','結果'),('social_start','スタート'),('social_hint','焦らず正直に答えてね。')],
'ko': [('social_reveal','답 공개'),('social_done','완료'),('social_next','다음'),('social_skip','건너뛰기'),('social_again','한 번 더'),('social_match','답이 같아요!'),('social_miss','다른 답이네요, 재밌어요!'),('social_choice_me','나'),('social_choice_you','너'),('social_choice_both','둘 다'),('social_choice_neither','둘 다 아님'),('social_timeup','시간 다 됐어요'),('social_round','라운드'),('social_of','/'),('social_result','결과'),('social_start','시작'),('social_hint','천천히 솔직하게 답하세요.')],
'es': [('social_reveal','Ver respuesta'),('social_done','Hecho'),('social_next','Siguiente'),('social_skip','Saltar'),('social_again','Jugar de nuevo'),('social_match','¡Coinciden!'),('social_miss','Respuestas distintas, ¡interesante!'),('social_choice_me','Yo'),('social_choice_you','Tú'),('social_choice_both','Los dos'),('social_choice_neither','Ninguno'),('social_timeup','Se acabó el tiempo'),('social_round','Ronda'),('social_of','de'),('social_result','Resultado'),('social_start','Empezar'),('social_hint','Tómate tu tiempo y sé honesto.')],
'fr': [('social_reveal','Voir la réponse'),('social_done','Fait'),('social_next','Suivant'),('social_skip','Passer'),('social_again','Rejouer'),('social_match','Vous êtes d\'accord !'),('social_miss','Réponses différentes, intéressant !'),('social_choice_me','Moi'),('social_choice_you','Toi'),('social_choice_both','Les deux'),('social_choice_neither','Aucun des deux'),('social_timeup','C\'est fini'),('social_round','Tour'),('social_of','sur'),('social_result','Résultat'),('social_start','Commencer'),('social_hint','Prends ton temps et sois honnête.')],
'pt': [('social_reveal','Ver resposta'),('social_done','Pronto'),('social_next','Próxima'),('social_skip','Pular'),('social_again','Jogar de novo'),('social_match','Vocês combinam!'),('social_miss','Respostas diferentes, interessante!'),('social_choice_me','Eu'),('social_choice_you','Você'),('social_choice_both','Os dois'),('social_choice_neither','Nenhum'),('social_timeup','Tempo esgotado'),('social_round','Rodada'),('social_of','de'),('social_result','Resultado'),('social_start','Começar'),('social_hint','Sem pressa e com honestidade.')],
}

# 法语撇号转义：Android strings.xml 中 ' 必须写成 \'
def esc(v):
    return v.replace("'", "\\'")

DIRS = {
'en':'values','zh':'values-zh-rCN','zh_tw':'values-zh-rTW','ja':'values-ja','ko':'values-ko',
'es':'values-es','fr':'values-fr','pt':'values-pt-rBR',
}

def build_entries(lang):
    idx = ['en','zh','zh_tw','ja','ko','es','fr','pt'].index(lang)
    out = []
    for m in MODES:
        out.append(('mode_' + m[0], m[1 + idx]))
        out.append(('mode_' + m[0] + '_desc', DESCS[lang][MODES.index(m)]))
    out.extend(COMMON[lang])
    return out

for lang, dname in DIRS.items():
    path = os.path.join(RES, dname, 'strings.xml')
    txt = open(path, encoding='utf-8').read()
    # 幂等：移除旧的社会游戏条目
    names = [n for n, _ in build_entries(lang)]
    pattern = re.compile(r'\s*<string name="(?:' + '|'.join(re.escape(n) for n in names) + r')">.*?</string>', re.S)
    txt = pattern.sub('', txt)
    block = '\n\n    <!-- Social games (12 modes sourced from comparable couple apps) -->\n'
    for n, v in build_entries(lang):
        block += '    <string name="%s">%s</string>\n' % (n, esc(v))
    txt = txt.replace('</resources>', block + '</resources>')
    open(path, 'w', encoding='utf-8').write(txt)
    print('updated', path, 'entries=', len(names))

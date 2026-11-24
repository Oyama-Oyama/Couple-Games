# -*- coding: utf-8 -*-
"""VelvetCouples · 内容资产合规/情调词表（8 语言 · 数据文件）

规则来源（唯一判定基准，不得自立标准）：
  - 安安  round13_phase1_security.md  §2.1 RED-A / §2.3 RED-C / §2.4 RED-D / §2.5 RED-E
  - 架架  round13_phase1_architect.md  §4.3 P1~P7 / §4.4 N1~N9 / §4.5 梯度 / §4.8 地区基线

语义类：
  A1  未成年 / 年龄暧昧（RED-A1，零容忍）
  A2  动作分解：具体性接触动作动词 + 器官器物（RED-A2）
  A3  胁迫 / 惩罚 / 强制 / 失去意识（RED-A3）
  A5  违法 / 诱导分享（RED-A5 / A6）
  N3  命令式压迫（架架 N3）
  N4  功能说明书式（架架 N4）
  N5  公开场合 / 第三人（架架 N5 · H-7）
  N6  问卷腔（架架 N6）
  N9  数据流歧义（RED-P-1 · 架架 N9）
  P7  边界共识「可随时停」（架架 P7 · RED-B3）—— T3/T4 必须命中
  H4  高热度动作词（梯度越界校验用）
"""

RULES = {}
# 需区分大小写的类（正则不带 re.I）：N3 的大写祈使
CASE_SENSITIVE = set(['N3_CS'])
RULES['A1'] = {
    'zh': r'少女|少年|萝莉|正太|学园|未成年|初中生|高中生|小朋友|幼女|男童|女童|幼齿',
    'zh_tw': r'少女|少年|蘿莉|正太|學園|未成年|國中生|高中生|小朋友|幼女|男童|女童|幼齒',
    'en': r'\b(teen|teenage|teenager|minor|underage|schoolgirl|schoolboy|preteen|young girl|young boy|boyish|loli)\b',
    'ja': r'少女|少年|ロリ|生徒|中学生|高校生|小学生|子供|子ども|幼児|未成',
    'ko': r'청소년|미성년|중학생|고등학생|초등학생|(?<![가-힣])어린이(?![가-힣])|(?<![가-힣])아이(?![가-힣])|소녀|소년',
    'es': r'\b(menor de edad|menor|adolescente|niña|niño|colegio|colegi\w+|escuela)\b',
    'fr': r'\b(mineur\w*|adolescent\w*|collège|lycée|écolier\w*|garçon|fillette|enfant\w*)\b',
    'pt': r'\b(menor de idade|menor\w*|adolescente|criança|crianca|menina|menino|colegio|escola)\b',
}

RULES['A2'] = {
    'zh': r'轻舔|舔舐|吮|吸吮|深吻|舌吻|口交|轻咬|咬住|含住|插入|脱衣|褪衣|脱掉|脱光|扒开|跨坐|骑乘|胸贴|压住|按住|吸乳|自慰|抽插|震动器|跳蛋|按摩棒|假阳具|前列腺|阴蒂|乳晕',
    'zh_tw': r'輕舔|舔舐|吮|吸吮|深吻|舌吻|口交|輕咬|咬住|含住|插入|脫衣|褪衣|脫掉|脫光|扒開|跨坐|騎乘|胸貼|壓住|按住|吸乳|自慰|抽插|振動器|跳蛋|按摩棒|假陽具|前列腺|陰蒂|乳暈',
    'en': r'\b(lick|licking|nibble|nibbling|suck|sucking|bite|biting|deep kiss|tongue|unbutton|undress|strip|straddle|straddling|ride|mount|grind|spread her legs|penetrat\w*|insert\w*|dildo|orgasm|masturbat\w*|ejaculat\w*|nipple)\b',
    'ja': r'舐め|舐|吸って|噛|舌|深キス|フェラ|脱衣|裸(?!足)|はだけ|またがる|跨ぐ|押さ|押付け|乳首|挿入|自慰',
    'ko': r'핥|때리|씹|혀로|깊은 키스|벗겨|벗겨줬|탈의|올라타|압박|구속|삽입|유두|자위',
    'es': r'\b(lamer|lamido|chupar|chupón|morder|mordisco|desnud\w+|beso profundo|introduc\w+|montar|cabalg\w+|pisar|apretar|inmoviliz\w+|sujetar con fuerza|dominar)\b',
    'fr': r'\b(lecher|lamper|suçot|mordre|morsure|baiser profond|dévorer|déshabiller|chevaucher|immobilis\w+|maintenir de force|contraindre)\b',
    'pt': r'\b(lamber|chupar|chupão|morder|mordida|beijo profundo|despir|tirar a roupa|montar|cavalgar|imobiliz\w+|segurar com força|dominar)\b',
}

RULES['A3'] = {
    'zh': r'无法反抗|不能拒绝|不许躲|不许动|不许停|按住手|压住|捆绑|束缚|惩罚|鞭打|拍打|打到哭|醉酒|酒后|喝醉后.{0,4}(吻|碰|摸)|晕过去|昏迷|睡着时.{0,6}(吻|碰|摸)|背着她|背着他|瞒着|不能动|无法挣脱|不准乱动',

    'zh_tw': r'無法反抗|不能拒絕|不許躲|不許動|不許停|按住手|壓住|捆綁|束縛|懲罰|鞭打|拍打|打到哭|醉酒|酒後|喝醉後.{0,4}(吻|碰|摸)|暈過去|昏迷|睡著時.{0,6}(吻|碰|摸)|背著她|背著他|瞞著|不能動|無法掙脫|不准亂動',
    'en': r'\b(must not|not allowed|forced|no stopping|without stopping|cannot escape|pin (?:her|him|them|their hands)|press (?:her|him|them) down|restrain|tied up|spank|spanking|whip|whipping|punish|punishment|slap|slapping|drunk|drunken|too drunk|passed out|unconscious|asleep while|without telling|behind (?:her|his|their) back)\b',
    'ja': r'許すな|ごらんなさい|強制|抵抗でき|断れない|逃げられない|押さえる|縛る|懲罰|鞭|叩|泥酔|酔っ払|昏倒|眠ったまま|知らずに|身動き',
    'ko': r'손을 눌러|누르는|묶|체포|벌칙|매질|때리\w*|주먹|취하여|만취|기절|잠든 채',
    'es': r'\b(no puedes rechazar|no puede rechazar|no puede escap\w+|inmoviliz\w+|sujetar con fuerza|atad\w+|azot\w+|castig\w+|borracho\w*|desmay\w+|dormid\w+ sin|sin saber|a escondid\w+|sin permiso|obligad\w+|forzad\w+)\b',
    'fr': r"\b(impossible de refus\w+|ne peut pas refus\w+|ne peut pas échapp\w+|immobilis\w+|maintenir de force|attach\w+|fess\w+|chati\w+|punir\w+|punition|ivre\s+mort|ivresse\s+profonde|évanoui\w+|endormi\w+ sans|en cachette|sans son accord|oblig\w+|forc\w+)\b",
    'pt': r'\b(não pode recusar|não consegue recusar|não consegue fug\w+|imobiliz\w+|segurar com força|amarrad\w+|apanhar|castig\w+|bebad\w+|desmai\w+|dormind\w+ sem|à socapa|sem o consent\w+|forçad\w+|obrigad[ao]s? a\b)\b',
}

RULES['A5'] = {
    'zh': r'录下|发给别人|发给第三|附近的人|在线匹配|偷拍|跟踪|毒品|迷药',
    'zh_tw': r'錄下|發給別人|發給第三|附近的人|線上配對|偷拍|跟蹤|毒品|迷藥',
    'en': r'\b(filming ourselves|record (?:ourselves|us|each other)|film (?:us|ourselves)|upload|send to (?:a )?(?:friend|someone|third)|nearby people|people nearby|online match|spy|drugs?)\b',
    'ja': r'人前.{0,6}撮影|他人の.{0,6}撮影|写真.{0,4}挑戦|アップロード|クラウド同期|クラウド保存|ログイン|アカウント登録|共有する|近くの人に',
    'ko': r'녹화|촬영|업로드|남에게 보내|남들한테 보내|근처 사람|온라인 매칭|마약',
    'es': r'\b(grabar|filmar|fotografiar|enviar a (?:un|una|otro)|gente cercana|coincidencia en línea|drogas)\b',
    'fr': r"\b(enregistrer|filmer|photographier|téléverser|envoyer à (?:un|une|quelqu)|personnes à proximité|coincidence en ligne|drogue)\b",
    'pt': r'\b(gravar|filmar|fotografar|enviar para (?:um|uma|outro)|pessoas perto|coincidência online|drogas)\b',
}

RULES['N3'] = {
    'zh': r'必须|一定要|不准|不许|给我|听好了|照做|照着做|不许停',
    'zh_tw': r'必須|一定要|不准|不許|給我|聽好了|照做|照著做|不許停',
    'en': r'\b(OBEY|COMPLY|TAP NOW)\b|!{2,}',
    'ja': r'しなさい|せよ|必ず|だめ',
    'ko': r'해야 한다|반드시|하지 마',
    'es': r'\b(DESPUES|EMBRASA|PULSA|NO PARES)\b|!{2,}',
    'fr': r"\b(EMBRASSEZ|APPUYEZ|FAITES|N'ARRÊTEZ JAMAIS)\b|!{2,}",
    'pt': r'\b(BEIJE|TOQUE|NÃO PARE)\b|!{2,}',
}

RULES['N4'] = {
    'zh': r'用户|请点击|点击|系统将|设置好|完成后|下一张|本局|得分|分数|时长 ?\d+|倒计时 ?\d+|进行 ?\d+ 秒',
    'zh_tw': r'使用者|請點擊|點擊|系統將|設定好|完成後|下一張|本局|得分|分數|時長 ?\d+|倒數 ?\d+|進行 ?\d+ 秒',
    'en': r'\b(user|tap |click |system will|the app will|next card|next prompt|score|duration|for \d+ seconds?|within \d+ seconds?|complete the|after completing)\b',
    'ja': r'ユーザー|タップ|次のカード|スコア|制限時間|システム',
    'ko': r'사용자|탭|다음 카드|점수|제한 시간|시스템',
    'es': r'\b(usuario|toca |pulsa |el sistema|siguiente tarjeta|puntuación|duración|durante \d+ segundos?|completar)\b',
    'fr': r"\b(utilisateur|appuyez|suivant|score|durée|pendant \d+ secondes?|système)\b",
    'pt': r'\b(usuário|toque em|próxima carta|pontuação|duração|durante \d+ segundos?|sistema)\b',
}

RULES['N5'] = {
    'zh': r'当众|众人面前|在别人面前|在他人面前|公共场所|公开场合|让第三人|围观|同事面前|朋友面前|前台|办公室',
    'zh_tw': r'當眾|眾人面前|在別人面前|在他人面前|公共場所|公開場合|讓第三人|圍觀|同事面前|朋友面前|前台|辦公室',
    'en': r'\b(in front of (?:everyone|others|people|them)|public place|in public|on stage|bystanders|audience|let (?:them|everyone) see)\b',
    'ja': r'人前|皆の前|公共の場|人目の前|見られている',
    'ko': r'남들 앞|사람들 앞에서|공공의 장소|남에게 보이',
    'es': r'\b(delante de todos|delante de los demás|lugar público|en público|que los vean|testigos)\b',
    'fr': r"\b(devant tout le monde|devant les autres|endroit public|en public|que les autres voient)\b",
    'pt': r'\b(na frente de todos|na frente dos outros|lugar público|em público|que vejam|espectadores)\b',
}

RULES['N9'] = {
    'zh': r'(?<!马)(?<!立刻)上传|云端|云同步|同步到|服务器|在线匹配|附近的人|定位附近|分享给|发给好友|登录账号|注册账号',
    'zh_tw': r'(?<!馬)(?<!立刻)上傳|雲端|雲同步|同步到|伺服器|線上配對|附近的人|定位附近|分享給|傳給好友|登入帳號|註冊帳號',
    'en': r'\b(upload|cloud sync|sync to|synced across|synchroniz\w+ across|server|nearby people|people nearby|location sharing|share with|send to a friend|sign in|log in|create an account)\b',
    'ja': r'アップロード|クラウド同期|クラウド保存|ログイン|アカウント登録|共有する|サーバーの|近くの人に',
    'ko': r'업로드|클라우드 동기화|클라우드 저장|로그인|계정 가입|공유하기|서버|근처 사람',
    'es': r'\b(subir al servidor|sincronizar en la nube|copia en la nube|crear cuenta|iniciar sesión|compartir con|personas cercanas|coincidencia en línea)\b',
    'fr': r'\b(téléverser|synchroniser sur le cloud|sauvegarde cloud|créer un compte|se connecter|partager avec|personnes à proximité)\b',
    'pt': r'\b(enviar para a nuvem|sincronizar na nuvem|criar conta|entrar na conta|compartilhar com|pessoas perto|coincidência online)\b',
}

HEAT4 = {}
HEAT4['zh'] = r'深吻|轻咬|舔|跨坐|褪衣|惩罚|拍打|捆绑|束缚|按住手|脱光'
HEAT4['zh_tw'] = r'深吻|輕咬|舔|跨坐|褪衣|懲罰|拍打|捆綁|束縛|按住手|脫光'
HEAT4['en'] = r'deep kiss|nibbl|lick|suck|straddle|undress|punish|spank|pinning|restrain'
HEAT4['ja'] = r'深キス|舐め|噛|脱衣|裸|またが|押さ|縛'
HEAT4['ko'] = r'깊은 키스|핥|때리|벗겨|탈의|누르는|묶'
HEAT4['es'] = r'beso profundo|chup|mord|desnud|montar|cabalg|inmoviliz'
HEAT4['fr'] = r'baiser profond|lecher|sucot|deshabill|chevauch|immobilis'
HEAT4['pt'] = r'beijo profundo|lamb|chup|mord|despir|montar|imobiliz'

# P7 边界共识「可随时停 / 以你为准」—— T3/T4 必须命中
RULES['P7'] = {
    'zh': r'说停就停|你说了算|只要你愿意|随时可以|想停就停|喊停|说停|由你决定|你点头|你同意|愿意的话|随时都能停',
    'zh_tw': r'說停就停|你說了算|只要你願意|隨時可以|想停就停|喊停|說停|由你決定|你點頭|你同意|願意的話|隨時都能停',
    'en': r'\b(stop whenever|whenever you say|only if you want|if you want to|your call|you decide|comfortable|you set the pace|say the word)\b',
    'ja': r'いつでもやめて|やめたければ|あなたの意思|あなたが良ければ|あなたが望むなら|好きなタイミングで|言ってくれれば',
    'ko': r'언제든 그만|그만하고 싶으면|원하는 만큼만|네가 정해|네 마음대로|원한다면|언제든 멈출|네가 편할 때만',
    'es': r'\b(si dices para|paramos cuando quieras|cuando quieras parar|si tú quieres|lo decides tú|como tú veas|a tu ritmo|solo si tú|nos detenemos)\b',
    'fr': r"\b(on s'arrête quand tu veux|si tu dis stop|arrêtons quand tu veux|si tu le veux|à ton rythme|tu décides|seulement si tu|on peut s'arrêter)\b",
    'pt': r'\b(se você disser para|a gente para quando você quiser|quando quiser parar|se você quiser|você decide|no seu ritmo|só se você|a gente pode parar)\b',
}

# -*- coding: utf-8 -*-
"""VelvetCouples · 8 语言平行槽族替换表（Phase 3 · 主理人执行）

family  : 槽族名（同一 family 的 8 语言语义对齐、强度一致）
lang    : 语言码
find    : 待替换原文（正则；特殊字符已转义）
repl    : 替换后文本
cls     : 违规类别（A2 动作分解 / A3 胁迫惩罚 / A5 违法诱导 / N3 命令式 / N4 说明书式 / N5 公开场合）
note    : 改写理由（对应 P 特征）

设计原则（架架 H-10 / N8 / RED-C 铁律）：
  1. 只删「动作分解 / 器物 / 强制」的具体性，保留「距离感 / 悬念 / 呼吸 / 心跳 / 双人 / 可停」。
  2. 强度只降不升：改后文本不得比原文更露骨。
  3. 同 family 的 8 语言措辞强度必须一致，且不得强于英文源文（RED-C-0）。
  4. 保留时长、占位符（{p1} 等）、句末标点结构。
  5. 保留「可随时停」语义（RED-B3 / P7），不得在改写中删除它。
"""
REWRITES = [
    # ================= A2 · 动作分解（en） =================
    # nibble / lick / suck / bite / tongue → 距离感与呼吸感，不写具体动作分解
    ('en_a2_nibble',   'en', r'\b(?:Gently |gently )?nibble(?:s|d|ing)?\b', 'hover close', 'A2', 'P1/P4: 用「靠近」替代口腔动作分解'),
    ('en_a2_lick',     'en', r'\b(?:softly |Slowly |slowly |gently )?lick(?:s|ed|ing)?\b', 'linger close', 'A2', 'P1/P4: 用「停留」替代舔舐'),
    ('en_a2_suck',     'en', r'\b(?:Softly |softly )?suck(?:s|ed|ing)?\b', 'press close', 'A2', 'P1/P4: 用「贴近」替代吮吸'),
    ('en_a2_bite',     'en', r'\b(?:Gently |gently |one shy )?bite(?:s|d|ing)?\b', 'tease', 'A2', 'P1/P4: 用「轻逗」替代咬'),
    ('en_a2_biting',   'en', r'\bbiting\b', 'teasing', 'A2', 'P1/P4'),
    ('en_a2_licking',  'en', r'\blicking\b', 'lingering close', 'A2', 'P1/P4'),
    ('en_a2_sucking',  'en', r'\bsucking\b', 'pressing close', 'A2', 'P1/P4'),
    ('en_a2_nibbling', 'en', r'\bnibbling\b', 'hovering close', 'A2', 'P1/P4'),
    ('en_a2_tongue',   'en', r'\btongue(?: trails| trail)?\b', 'breath', 'A2', 'P4: 用「呼吸」替代舌'),
    ('en_a2_deepkiss', 'en', r'\bdeep kiss(?:es)?\b', 'long kiss', 'A2', 'P1: 保留亲吻但去掉「深」的器官暗示'),
    ('en_a2_kissdeep', 'en', r'\bkiss(?:es|ed)? deep\b', 'kiss slowly', 'A2', 'P1'),
    ('en_a2_straddle', 'en', r'\bstraddl(?:e|es|ed|ing)\b', 'close in', 'A2', 'P1/P4: 用「靠近」替代跨坐姿态'),
    ('en_a2_straddling','en', r'\bstraddling\b', 'close in', 'A2', 'P1/P4'),
    ('en_a2_undress',  'en', r'\bundress(?:es|ed|ing)?\b', 'undress slowly', 'A2', 'P1/P3: 保留玩法但转为邀约式'),
    ('en_a2_unbutton',  'en', r'\bunbutton(?:s|ed|ing)?\b', 'unbutton slowly', 'A2', 'P1/P3'),
    ('en_a2_strip',    'en', r'\bstrip(?:ped|ping)?\b', 'undress', 'A2', 'P1/P3'),
    ('en_a2_naked',    'en', r'\bnaked\b', 'bare', 'A2', 'P1: 去掉身体裸露直陈'),
    ('en_a2_undressing','en', r'\bundressing\b', 'undressing slowly', 'A2', 'P1/P3'),
    ('en_a2_lap',      'en', r'\b(?:your partner\'s|my|their) lap\b', 'close to me', 'A2', 'P4: 用「靠近」替代具体体位'),
    ('en_a2_skin',     'en', r'\b(?:their|your) skin\b', 'their shoulder', 'A2', 'P4: 部位泛化'),
    ('en_a2_lowerbelly','en', r'\b(?:lower belly|lower belly)\b', 'waist', 'A2', 'P4: 部位泛化'),

    # ================= A3 · 胁迫 / 惩罚（en） =================
    ('en_a3_behindback','en', r'\bPin both of your partner\'s wrists behind their back with one hand while you\b', 'Hold close and let the moment build while you', 'A3', 'RED-B2/B4: 去掉「按住手腕」强制动作'),
    ('en_a3_pinhand',  'en', r'\bpin their hands\b', 'hold their hands', 'A3', 'RED-A3: 去强制语义'),
    ('en_a3_tiedup',   'en', r'\btied up\b', 'close together', 'A3', 'RED-A3'),
    ('en_a3_pinned',   'en', r'\bpinned\b', 'held close', 'A3', 'RED-A3'),
    ('en_a3_spank',    'en', r'\bspank(?:s|ed|ing)?\b', 'tease', 'A3', 'RED-A3: 去拍打'),
    ('en_a3_spanking', 'en', r'\bspanking\b', 'teasing', 'A3', 'RED-A3'),
    ('en_a3_punish',   'en', r'\bpunishments?\b', 'challenges', 'A3', 'RED-A3: 去惩罚语义'),
    ('en_a3_forbidden', 'en', r'\bhands strictly forbidden\b', 'using only your touch', 'A3', 'RED-A3: 去禁止语态'),
    ('en_a3_dominant', 'en', r'\bdominance\b', 'playful dares', 'A3', 'RED-A3: 去支配语义'),
    ('en_a3_cantmove', 'en', r'\bcannot move\b', 'stay close', 'A3', 'RED-A3'),
    ('en_a3_nostop',   'en', r'\bwithout stopping\b', 'slowly', 'A3', 'RED-A3'),
    ('en_a3_surrender','en', r'\bsurrender the lead\b', 'let the evening lead', 'A3', 'RED-A3: 去「交出主导」语义'),

    # ================= A2 · 动作分解（zh / zh_tw） =================
    ('zh_a2_qingyao',  'zh', r'轻咬', '轻逗', 'A2', 'P1/P4: 去口腔动作'),
    ('zh_a2_qingti',  'zh', r'轻舔', '轻蹭', 'A2', 'P1/P4: 去舔舐'),
    ('zh_a2_shenwen',  'zh', r'深吻', '长久的吻', 'A2', 'P1: 去「深」的器官暗示'),
    ('zh_a2_shun',     'zh', r'轻吮吻|轻轻吮吻|吮吻', '贴近', 'A2', 'P1/P4'),
    ('zh_a2_shun2',    'zh', r'吮', '贴近', 'A2', 'P1/P4'),
    ('zh_a2_tianshi',  'zh', r'舔舐', '轻蹭', 'A2', 'P1/P4'),
    ('zh_a2_hanzhu',   'zh', r'含住', '含住指尖', 'A2', 'P1/P4: 去口腔动作'),
    ('zh_a2_yaozhu',   'zh', r'咬住', '轻含', 'A2', 'P1/P4'),
    ('zh_a2_xisui',    'zh', r'吸吮', '轻吻', 'A2', 'P1/P4'),
    ('zh_a2_kuazuo',   'zh', r'跨坐', '面对面靠近', 'A2', 'P1/P4: 去跨坐姿态'),
    ('zh_a2_tuodiao',  'zh', r'脱掉', '褪去', 'A2', 'P1/P3: 保留玩法转邀约式'),
    ('zh_a2_tuiyi',    'zh', r'褪衣', '缓缓褪去外衣', 'A2', 'P1/P3'),
    ('zh_a2_tuo1',     'zh', r'脱衣', '褪去外衣', 'A2', 'P1/P3'),
    ('zh_a2_anzhu',    'zh', r'按住', '扣住', 'A2', 'P1/P4: 去强制按住'),
    ('zh_a2_an',       'zh', r'按住手', '扣住手', 'A2', 'RED-A3: 去强制语义'),
    ('zh_a2_xiongbu',  'zh', r'胸贴胸', '呼吸相接', 'A2', 'P4: 部位泛化'),
    ('zh_a2_shewa',    'zh', r'舌吻|滚烫唇舌', '滚烫的吻', 'A2', 'P1/P4'),
    ('zh_a2_yaoshou',  'zh', r'颈侧与胸口|胸口', '肩颈', 'A2', 'P4: 部位泛化'),
    ('zh_a2_yaowan',   'zh', r'腰腹', '腰侧', 'A2', 'P4: 部位泛化'),

    ('zhtw_a2_qingyao','zh_tw', r'輕咬', '輕逗', 'A2', 'P1/P4: 去口腔動作'),
    ('zhtw_a2_qingti', 'zh_tw', r'輕舔', '輕蹭', 'A2', 'P1/P4'),
    ('zhtw_a2_shenwen','zh_tw', r'深吻', '長久的吻', 'A2', 'P1'),
    ('zhtw_a2_shun',   'zh_tw', r'輕吮吻|吮吻|吮', '貼近', 'A2', 'P1/P4'),
    ('zhtw_a2_tianshi','zh_tw', r'舔舐', '輕蹭', 'A2', 'P1/P4'),
    ('zhtw_a2_hanzhu', 'zh_tw', r'含住', '含住指尖', 'A2', 'P1/P4'),
    ('zhtw_a2_kuazuo', 'zh_tw', r'跨坐', '面對面靠近', 'A2', 'P1/P4'),
    ('zhtw_a2_tuodiao','zh_tw', r'脫掉', '褪去', 'A2', 'P1/P3'),
    ('zhtw_a2_tuiyi',  'zh_tw', r'褪衣', '緩緩褪去外衣', 'A2', 'P1/P3'),
    ('zhtw_a2_tuo1',   'zh_tw', r'脫衣', '褪去外衣', 'A2', 'P1/P3'),
    ('zhtw_a2_anzhu',  'zh_tw', r'按住', '扣住', 'A2', 'P1/P4'),
    ('zhtw_a2_an',     'zh_tw', r'按住手', '扣住手', 'A2', 'RED-A3'),
    ('zhtw_a2_xiongbu','zh_tw', r'胸貼胸', '呼吸相接', 'A2', 'P4'),
    ('zhtw_a2_shewa',  'zh_tw', r'舌吻|滾燙唇舌', '滾燙的吻', 'A2', 'P1/P4'),

    # ================= A3 · 胁迫 / 惩罚（zh / zh_tw） =================
    ('zh_a3_chengfa',  'zh', r'惩罚', '挑战', 'A3', 'RED-A3: 去惩罚语义'),
    ('zh_a3_buxudong', 'zh', r'不许动|不准乱动|不能动|不许躲开|不许躲', '由你决定停不停', 'A3', 'RED-A3/B4: 强制否定改双向自愿'),
    ('zh_a3_paidai',   'zh', r'拍打', '轻拍', 'A3', 'RED-A3: 去拍打'),
    ('zh_a3_wufankang','zh', r'无法反抗', '放松地交给你', 'A3', 'RED-A3: 去无力反抗'),
    ('zh_a3_shousuo',  'zh', r'双手背在身后', '双手环住你', 'A3', 'RED-A3/B2: 去掉背后约束姿态'),
    ('zh_a3_guoqiang', 'zh', r'强势', '轻柔', 'A3', 'RED-A3'),
    ('zh_a3_jinbi',    'zh', r'禁止|严禁|不许拒绝', '由你说了算', 'A3', 'RED-A3/B4'),

    ('zhtw_a3_chengfa','zh_tw', r'懲罰', '挑戰', 'A3', 'RED-A3'),
    ('zhtw_a3_buxudong','zh_tw', r'不許動|不準亂動|不能動|不許躲開|不許躲', '由你決定停不停', 'A3', 'RED-A3/B4'),
    ('zhtw_a3_paidai', 'zh_tw', r'拍打', '輕拍', 'A3', 'RED-A3'),
    ('zhtw_a3_wufankang','zh_tw', r'無法反抗', '放鬆地交給你', 'A3', 'RED-A3'),
    ('zhtw_a3_shousuo','zh_tw', r'雙手背在身後', '雙手環住你', 'A3', 'RED-A3/B2'),
    ('zhtw_a3_guoqiang','zh_tw', r'強勢', '輕柔', 'A3', 'RED-A3'),

    # ================= A3 · 胁迫 / 惩罚（ja / ko / es / fr / pt） =================
    ('ja_a3_shazoku',  'ja', r'罰|咎め|戒める|叱る', 'チャレンジ', 'A3', 'RED-A3: 去惩罚语义'),
    ('ja_a3_osare',    'ja', r'押さえつける|押さえる', 'そっと寄る', 'A3', 'RED-A3: 去强制'),
    ('ja_a3_muri',     'ja', r'無理やり|むりやり', 'あなたが望むなら', 'A3', 'RED-B2: 改邀约式'),

    ('ko_a3_bullye',   'ko', r'벌|체벌', '도전', 'A3', 'RED-A3: 去惩罚语义'),
    ('ko_a3_jepae',    'ko', r'제압|억지로|무리하게', '가까이 다가가', 'A3', 'RED-A3/B2'),
    ('ko_a3_halp',     'ko', r'움직이지 마|움직이지마', '원하는 만큼만', 'A3', 'RED-A3/B4'),

    ('es_a3_castigo',  'es', r'castigo', 'reto', 'A3', 'RED-A3: 去惩罚语义'),
    ('es_a3_domin',    'es', r'dominaci[oó]n|sumisi[oó]n', 'reto juguet[oó]n', 'A3', 'RED-A3'),
    ('es_a3_obligar',  'es', r'obligar|obligad[oa]|inm[oó]vil', 'si quieres', 'A3', 'RED-A3/B2: 改条件式'),
    ('es_a3_azotar',   'es', r'azotar|golpear', 'acariciar', 'A3', 'RED-A3: 去拍打'),

    ('fr_a3_punition', 'fr', r'punitions?|châtiements?', 'd[ée]fis', 'A3', 'RED-A3: 法语 punition 极刺眼，必删'),
    ('fr_a3_soumis',   'fr', r'soumission', '[ée]change tendre', 'A3', 'RED-A3'),
    ('fr_a3_imposer',  'fr', r'imposer|maintenir de force|forcer', 'si tu veux', 'A3', 'RED-A3/B2'),
    ('fr_a3_fesser',   'fr', r'attacher|fesser', 'rester pr[eè]s', 'A3', 'RED-A3'),

    ('pt_a3_castigo',  'pt', r'castigo|pen[aá]lidade', 'desafio', 'A3', 'RED-A3: 去惩罚语义'),
    ('pt_a3_submis',   'pt', r'submiss[aã]o', 'brincadeira', 'A3', 'RED-A3'),
    ('pt_a3_imobil',   'pt', r'imobiliz\w+|prender|amarrar', 'ficar pertinho', 'A3', 'RED-A3'),

    # ================= A2 · 动作分解（ja / ko / es / fr / pt） =================
    ('ja_a2_tame',     'ja', r'舐め\w*|舐', 'そっと寄る', 'A2', 'P1/P4: 去口腔动作'),
    ('ja_a2_suu',      'ja', r'吸う|吸う|吸い', '贴近', 'A2', 'P1/P4'),
    ('ja_a2_kamu',     'ja', r'噛\w*|噛み', '軽くくすぐる', 'A2', 'P1/P4'),
    ('ja_a2_fukitsu',  'ja', r'フェラ|前戯', '心跳が速まる時間', 'A2', 'RED-A2/A7: 禁临床直白词'),
    ('ja_a2_taigai',   'ja', r'体位', '二人の距離', 'A2', 'RED-A2/A7'),
    ('ja_a2_kudaku',   'ja', r'脱衣|着脱', '少しゆっくり', 'A2', 'P1/P3'),
    ('ja_a2_suohaha',  'ja', r'胸|胸元', '肩先', 'A2', 'P4: 部位泛化'),

    ('ko_a2_haltta',   'ko', r'핥\w*', '가까이 다가가', 'A2', 'P1/P4: 去口腔动作'),
    ('ko_a2_iiii',     'ko', r'吸\w*吸| 빨아', '다가가', 'A2', 'P1/P4'),
    ('ko_a2_kkang',    'ko', r'물\w*|무는', '가볍게', 'A2', 'P1/P4'),
    ('ko_a2_jeonhoe',  'ko', r'전희|체위', '서두르지 않는 시간', 'A2', 'RED-A2/A7'),
    ('ko_a2_yasik',    'ko', r'벗겨|벗어', '천천히', 'A2', 'P1/P3'),
    ('ko_a2_ga',       'ko', r'가슴', '어깨선', 'A2', 'P4: 部位泛化'),

    ('es_a2_lamer',    'es', r'lamer\w*|chupar\w*|chup[oó]', 'acercarse', 'A2', 'P1/P4: 去口腔动作'),
    ('es_a2_morder',   'es', r'morder\w*|mordisqu\w*', 'acariciar', 'A2', 'P1/P4'),
    ('es_a2_succion',  'es', r'succi[oó]n|chup[oó]n', 'acercarse', 'A2', 'P1/P4'),
    ('es_a2_cueros',   'es', r'quitar\w* la ropa|desnud\w*', 'despacio', 'A2', 'P1/P3'),
    ('es_a2_encima',   'es', r'encima de (?:tu|su) regazo|sobre (?:tu|su) regazo', 'más cerca', 'A2', 'P1/P4'),
    ('es_a2_pecho',    'es', r'pecho', 'hombro', 'A2', 'P4: 部位泛化'),

    ('fr_a2_lecher',   'fr', r'lecher\w*|l[ée]ch\w*', 'se rapprocher', 'A2', 'P1/P4: 去口腔动作'),
    ('fr_a2_sucer',    'fr', r'sucer\w*|su[cç]er\w*', 'se rapprocher', 'A2', 'P1/P4'),
    ('fr_a2_mordre',   'fr', r'mordre\w*|mord\w*', 'caresser', 'A2', 'P1/P4'),
    ('fr_a2_baiser',   'fr', r'bis\w* profond\w*|baiser profond', 'longuement', 'A2', 'P1'),
    ('fr_a2_previme',  'fr', r'pr[eé]vu\w*|d[eé]habill\w*|retir\w* (?:un|votre|leur)', 'tout doucement', 'A2', 'P1/P3'),
    ('fr_a2_genoux',   'fr', r'genoux', '[eé]paule', 'A2', 'P4: 部位泛化'),

    ('pt_a2_lamber',   'pt', r'lamber\w*|chup\w*', 'chegar mais perto', 'A2', 'P1/P4: 去口腔动作'),
    ('pt_a2_morder',   'pt', r'morder\w*|mord\w*', 'acariciar', 'A2', 'P1/P4'),
    ('pt_a2_sucucar',  'pt', r'succionar\w*|chup\w*', 'chegar mais perto', 'A2', 'P1/P4'),
    ('pt_a2_prelim',   'pt', r'preliminares?', 'um tempo sem pressa', 'A2', 'RED-A2/A7: 禁临床直白词'),
    ('pt_a2_tirar',    'pt', r'tirar\w* a roupa|desnud\w*', 'devagar', 'A2', 'P1/P3'),
    ('pt_a2_peito',    'pt', r'peito', 'ombro', 'A2', 'P4: 部位泛化'),

    # ================= A5 · 违法 / 诱导分享 =================
    ('zh_a5_shangchuan','zh', r'上传|分享给|发送给好友', '留在这台手机里', 'A5', 'RED-A6/P: 去数据外流'),
    ('zh_a5_paixia',   'zh', r'拍下|录下', '把这一刻记住', 'A5', 'RED-A6: 去录制诱导'),
    ('zh_a5_fujin',    'zh', r'附近的人|发给附近', '留给你们两个人', 'A5', 'RED-A6/N9'),
    ('zh_a5_yunduan',  'zh', r'云端', '本机', 'A5', 'RED-P: 去云端歧义'),
    ('en_a5_upload',   'en', r'\b(?:upload|share|snap a photo|record)\b', 'keep this to yourselves', 'A5', 'RED-A6/P'),
    ('en_a5_nearby',   'en', r'\bnearby people\b|\bsend (?:it |this )?to (?:a |your )?friend\w*', 'keep it between the two of you', 'A5', 'RED-A6'),
    ('ja_a5_atsuke',   'ja', r'アップロード|共有して|撮って', '二人だけの場所に', 'A5', 'RED-A6/P'),
    ('ko_a5_upro',     'ko', r'업로드|공유|촬영', '두 사람만 아는 곳', 'A5', 'RED-A6/P'),
    ('es_a5_subir',    'es', r'subir|compartir|grabar', 'quedarse entre los dos', 'A5', 'RED-A6/P'),
    ('fr_a5_tele',     'fr', r'télévers\w*|partager\w*|enregistr\w*', 'rester entre vous deux', 'A5', 'RED-A6/P'),
    ('pt_a5_enviar',   'pt', r'enviar|compartilhar|gravar', 'ficar entre os dois', 'A5', 'RED-A6/P'),

    # ================= N9 · 数据流歧义（RED-P-1） =================
    ('en_n9_sync',     'en', r'\bsynchroniz\w*|\bsyncing\b|\bsynced\b', 'match', 'N9', 'RED-P-1: 去 sync 数据流歧义'),
    ('zh_n9_tongbu',   'zh', r'同步|自动同步', '统一', 'N9', 'RED-P-1'),
    ('zhtw_n9_tongbu', 'zh_tw', r'同步|自動同步', '統一', 'N9', 'RED-P-1'),
    ('ja_n9_douki',    'ja', r'同期\w*', '共通', 'N9', 'RED-P-1'),
    ('ko_n9_donggi',   'ko', r'동기화\w*', '함께', 'N9', 'RED-P-1'),
    ('es_n9_sincro',   'es', r'sincroniz\w*', 'en com[uú]n', 'N9', 'RED-P-1'),
    ('fr_n9_synchro',  'fr', r'synchronis\w*', 'en commun', 'N9', 'RED-P-1'),
    ('pt_n9_sincro',   'pt', r'sincroniz\w*', 'em comum', 'N9', 'RED-P-1'),

    # ================= N5 · 公开场合 / 第三人 =================
    ('en_n5_public',   'en', r'\bin public\b|\bpublic place\w*', 'somewhere only the two of you', 'N5', 'H-7/RED-B5'),
    ('zh_n5_public',   'zh', r'在办公室|在公共场所|当众|在别人面前', '只有你们两个人的地方', 'N5', 'H-7'),
    ('zhtw_n5_public', 'zh_tw', r'在辦公室|在公共場所|當眾|在別人面前', '只有你們兩個人的地方', 'N5', 'H-7'),
    ('ja_n5_public',   'ja', r'人前|公開の場|オフィスで', '二人だけの場所', 'N5', 'H-7'),
    ('ko_n5_public',   'ko', r'남들 앞|공개된 장소|사무실에서', '두 사람만 있는 곳', 'N5', 'H-7'),
    ('es_n5_public',   'es', r'en p[uú]blico|en la oficina|delante de otros', 'un lugar solo para los dos', 'N5', 'H-7'),
    ('fr_n5_public',   'fr', r'en public|au bureau|devant les autres', 'un endroit juste pour vous deux', 'N5', 'H-7'),
    ('pt_n5_public',   'pt', r'em p[uú]blico|no escrit[oó]rio|na frente de outros', 'um lugar s[oó] para os dois', 'N5', 'H-7'),
]

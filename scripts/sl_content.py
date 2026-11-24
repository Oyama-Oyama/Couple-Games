# -*- coding: utf-8 -*-
"""Source data for assets/content/{lang}/sealed_letter.json (8 languages).

Every language carries the identical id set so verify_content.py can assert
full ID alignment against the English baseline.
"""

LANGS = ["en", "zh", "zh_tw", "ja", "ko", "es", "fr", "pt"]

# --- Wax seal styles -------------------------------------------------------
# id, symbol, label per language
SEALS = [
    {
        "id": "seal_crimson",
        "symbol": "❤",
        "label": {
            "en": "Crimson wax",
            "zh": "绯红蜡封",
            "zh_tw": "緋紅蠟封",
            "ja": "深紅の封蝋",
            "ko": "진홍 밀랍",
            "es": "Lacre carmesí",
            "fr": "Cire cramoisi",
            "pt": "Lacre carmesim",
        },
    },
    {
        "id": "seal_rose",
        "symbol": "❀",
        "label": {
            "en": "Rose wax",
            "zh": "玫瑰蜡封",
            "zh_tw": "玫瑰蠟封",
            "ja": "バラ色の封蝋",
            "ko": "장미빛 밀랍",
            "es": "Lacre de rosa",
            "fr": "Cire rose",
            "pt": "Lacre rosa",
        },
    },
    {
        "id": "seal_gold",
        "symbol": "✦",
        "label": {
            "en": "Gold wax",
            "zh": "鎏金蜡封",
            "zh_tw": "鎏金蠟封",
            "ja": "金の封蝋",
            "ko": "황금 밀랍",
            "es": "Lacre dorado",
            "fr": "Cire d'or",
            "pt": "Lacre dourado",
        },
    },
    {
        "id": "seal_midnight",
        "symbol": "☾",
        "label": {
            "en": "Midnight wax",
            "zh": "午夜蓝蜡封",
            "zh_tw": "午夜藍蠟封",
            "ja": "真夜中の封蝋",
            "ko": "한밤중 밀랍",
            "es": "Lacre de medianoche",
            "fr": "Cire de minuit",
            "pt": "Lacre da meia-noite",
        },
    },
    {
        "id": "seal_lavender",
        "symbol": "❁",
        "label": {
            "en": "Lavender wax",
            "zh": "薰衣草蜡封",
            "zh_tw": "薰衣草蠟封",
            "ja": "ラベンダーの封蝋",
            "ko": "라벤더 밀랍",
            "es": "Lacre de lavanda",
            "fr": "Cire lavande",
            "pt": "Lacre lavanda",
        },
    },
    {
        "id": "seal_ember",
        "symbol": "✧",
        "label": {
            "en": "Ember wax",
            "zh": "余烬蜡封",
            "zh_tw": "餘燼蠟封",
            "ja": "熾火の封蝋",
            "ko": "불씨 밀랍",
            "es": "Lacre de brasa",
            "fr": "Cire de braise",
            "pt": "Lacre de brasa",
        },
    },
]

# --- Writing prompts -------------------------------------------------------
# id, text per language
PROMPTS = [
    {
        "id": "prompt_today",
        "text": {
            "en": "Something I noticed about you today that I never said out loud.",
            "zh": "今天我注意到你，却一直没敢说出口的那件事。",
            "zh_tw": "今天我注意到你，卻一直沒敢說出口的那件事。",
            "ja": "今日あなたに見つけて、言えなかったこと。",
            "ko": "오늘 네게서 발견했지만 말하지 못한 것.",
            "es": "Algo que noté de ti hoy y nunca dije en voz alta.",
            "fr": "Ce que j'ai remarqué chez toi aujourd'hui sans jamais le dire.",
            "pt": "Algo que notei em você hoje e nunca disse em voz alta.",
        },
    },
    {
        "id": "prompt_thankyou",
        "text": {
            "en": "Thank you for the small thing you did that you thought nobody noticed.",
            "zh": "谢谢你做过的那件小事，你以为没人看见。",
            "zh_tw": "謝謝你做過的那件小事，你以為沒人看見。",
            "ja": "誰も見ていないと思っていた、小さな優しさをありがとう。",
            "ko": "아무도 몰랐다고 생각한 네 작은 다정함, 고마워.",
            "es": "Gracias por ese gesto pequeño que creíste que nadie notó.",
            "fr": "Merci pour ce petit geste que tu pensais que personne n'avait vu.",
            "pt": "Obrigado pelo pequeno gesto que você achou que ninguém notou.",
        },
    },
    {
        "id": "prompt_first",
        "text": {
            "en": "The very first thing I felt when I saw you. No editing, just the truth.",
            "zh": "第一次见到你时我的感觉。不修饰，只有实话。",
            "zh_tw": "第一次見到你時我的感覺。不修飾，只有實話。",
            "ja": "初めてあなたを見たときの気持ち。飾らず、本音だけ。",
            "ko": "처음 네를 봤을 때 느낀 것. 꾸미지 말고 사실만.",
            "es": "Lo primero que sentí al verte. Sin editar, solo la verdad.",
            "fr": "Ce que j'ai ressenti la première fois en te voyant. Sans filtre, juste la vérité.",
            "pt": "O que senti da primeira vez que te vi. Sem editar, só a verdade.",
        },
    },
    {
        "id": "prompt_future",
        "text": {
            "en": "A promise about a day I want to live with you someday.",
            "zh": "关于将来某个想和你一起过的日子，我的一个承诺。",
            "zh_tw": "關於將來某個想和你一起過的日子，我的一個承諾。",
            "ja": "いつかあなたと過ごしたい一日についての約束。",
            "ko": "언젠가 너와 함께 살고 싶은 하루에 대한 약속.",
            "es": "Una promesa sobre un día que algún día quiero vivir contigo.",
            "fr": "Une promesse sur un jour où je voudrais vivre avec toi.",
            "pt": "Uma promessa sobre um dia em que quero viver com você.",
        },
    },
    {
        "id": "prompt_quarrel",
        "text": {
            "en": "The thing I wanted to say after our last argument.",
            "zh": "上一次争吵之后，我一直想说的话。",
            "zh_tw": "上一次爭吵之後，我一直想說的話。",
            "ja": "最後に言い争った後、言いたかったこと。",
            "ko": "마지막으로 다툰 뒤에 말하고 싶었던 것.",
            "es": "Lo que quise decir después de nuestra última discusión.",
            "fr": "Ce que j'ai voulu dire après notre dernière dispute.",
            "pt": "O que eu quis dizer depois da nossa última discussão.",
        },
    },
    {
        "id": "prompt_fear",
        "text": {
            "en": "Something I was afraid to say, so I am writing it down instead.",
            "zh": "有些话我说不出口，所以写在这里。",
            "zh_tw": "有些話我說不出口，所以寫在這裡。",
            "ja": "口に出せなかったことを、代わりに書きます。",
            "ko": "말하지 못했던 걸 대신 적어둡니다.",
            "es": "Algo que me daba miedo decir, así que lo escribo aquí.",
            "fr": "Une chose que je n'osais pas dire, alors je l'écris.",
            "pt": "Algo que eu tinha medo de dizer, então escrevo aqui.",
        },
    },
    {
        "id": "prompt_favourite",
        "text": {
            "en": "The version of you that nobody else gets to see.",
            "zh": "只有我能看到的那一面的你。",
            "zh_tw": "只有我能看到的那一面的你。",
            "ja": "他の誰にも見せない、あなたの姿。",
            "ko": "다른 누구도 보지 못하는 네 모습.",
            "es": "La versión de ti que nadie más llega a ver.",
            "fr": "La version de toi que personne d'autre ne voit.",
            "pt": "A versão de você que mais ninguém vê.",
        },
    },
    {
        "id": "prompt_ridiculous",
        "text": {
            "en": "The most ridiculous thing I love about us.",
            "zh": "关于我们之间最荒唐、却让我很爱的一点。",
            "zh_tw": "關於我們之間最荒唐、卻讓我很愛的一點。",
            "ja": "私たちの間でいちばん場違いで、それでも好きなこと。",
            "ko": "우리 사이에서 제일 엉뚱하면서도 좋아하는 것.",
            "es": "Lo más absurdo de nosotros y que aun así amo.",
            "fr": "Ce qui est le plus absurde chez nous et que j'adore.",
            "pt": "A coisa mais absurda entre nós e que eu amo assim mesmo.",
        },
    },
    {
        "id": "prompt_letter_you",
        "text": {
            "en": "A letter to you, from the version of me you have not met yet.",
            "zh": "写给你的一封信，来自那个你还没遇见的自己。",
            "zh_tw": "寫給你的一封信，來自那個你還沒遇見的自己。",
            "ja": "まだ出会っていない自分からの、あなたへの手紙。",
            "ko": "아직 만나지 못한 나에게서, 너에게 쓰는 편지.",
            "es": "Una carta para ti, de la versión de mí que todavía no conoces.",
            "fr": "Une lettre pour toi, de la version de moi que tu n'as pas encore rencontrée.",
            "pt": "Uma carta para você, da versão de mim que você ainda não conheceu.",
        },
    },
    {
        "id": "prompt_forgiven",
        "text": {
            "en": "Something I would like you to forgive me for.",
            "zh": "有些事，我希望你能原谅我。",
            "zh_tw": "有些事，我希望你能原諒我。",
            "ja": "許してほしいことを、書きます。",
            "ko": "용서해줬으면 하는 것.",
            "es": "Algo que me gustaría que me perdonaras.",
            "fr": "Une chose que j'aimerais que tu me pardonnes.",
            "pt": "Algo que eu gostaria que você me perdoasse.",
        },
    },
]

# --- Reveal delay presets --------------------------------------------------
# id, days, label per language
DELAYS = [
    {
        "id": "delay_1d",
        "days": 1,
        "label": {
            "en": "Tomorrow",
            "zh": "明天",
            "zh_tw": "明天",
            "ja": "明日",
            "ko": "내일",
            "es": "Mañana",
            "fr": "Demain",
            "pt": "Amanhã",
        },
    },
    {
        "id": "delay_3d",
        "days": 3,
        "label": {
            "en": "In 3 days",
            "zh": "3 天后",
            "zh_tw": "3 天後",
            "ja": "3 日後",
            "ko": "3일 후",
            "es": "En 3 días",
            "fr": "Dans 3 jours",
            "pt": "Em 3 dias",
        },
    },
    {
        "id": "delay_7d",
        "days": 7,
        "label": {
            "en": "In a week",
            "zh": "一周后",
            "zh_tw": "一週後",
            "ja": "1 週間後",
            "ko": "일주일 후",
            "es": "En una semana",
            "fr": "Dans une semaine",
            "pt": "Em uma semana",
        },
    },
    {
        "id": "delay_14d",
        "days": 14,
        "label": {
            "en": "In 2 weeks",
            "zh": "两周后",
            "zh_tw": "兩週後",
            "ja": "2 週間後",
            "ko": "2주 후",
            "es": "En 2 semanas",
            "fr": "Dans 2 semaines",
            "pt": "Em 2 semanas",
        },
    },
    {
        "id": "delay_30d",
        "days": 30,
        "label": {
            "en": "In a month",
            "zh": "一个月后",
            "zh_tw": "一個月後",
            "ja": "1 か月後",
            "ko": "한 달 후",
            "es": "En un mes",
            "fr": "Dans un mois",
            "pt": "Em um mês",
        },
    },
    {
        "id": "delay_100d",
        "days": 100,
        "label": {
            "en": "In 100 days",
            "zh": "一百天后",
            "zh_tw": "一百天後",
            "ja": "100 日後",
            "ko": "100일 후",
            "es": "En 100 días",
            "fr": "Dans 100 jours",
            "pt": "Em 100 dias",
        },
    },
]

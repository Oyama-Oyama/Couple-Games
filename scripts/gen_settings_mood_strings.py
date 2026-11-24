# -*- coding: utf-8 -*-
"""Generate / refresh the strings for the settings date picker, the mood card
and the sealed-letter author options across all eight languages.

Idempotent: existing keys are replaced in place, missing keys are appended.
Usage: python scripts/gen_settings_mood_strings.py
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

LANGS = {
    "values": "en",
    "values-zh-rCN": "zh-CN",
    "values-zh-rTW": "zh-TW",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-es": "es",
    "values-fr": "fr",
    "values-pt-rBR": "pt-BR",
}

# key -> {lang -> value}
STRINGS = {
    # ---- Settings: together-since date picker ----
    "settings_together_date_label": {
        "en": "Together since",
        "zh-CN": "在一起的日子",
        "zh-TW": "在一起的日子",
        "ja": "在一起的日子",
        "ko": "함께한 날",
        "es": "Juntos desde",
        "fr": "Ensemble depuis",
        "pt-BR": "Juntos desde",
    },
    "settings_together_date_unset": {
        "en": "Not set yet",
        "zh-CN": "还没设置",
        "zh-TW": "還沒設定",
        "ja": "まだ設定されていません",
        "ko": "아직 설정되지 않았어요",
        "es": "Aún no la has fijado",
        "fr": "Pas encore définie",
        "pt-BR": "Ainda não definida",
    },
    "settings_together_date_set": {
        "en": "Pick a date",
        "zh-CN": "选择日期",
        "zh-TW": "選擇日期",
        "ja": "日付を選ぶ",
        "ko": "날짜 고르기",
        "es": "Elige una fecha",
        "fr": "Choisir une date",
        "pt-BR": "Escolher uma data",
    },
    "settings_together_date_edit": {
        "en": "Edit date",
        "zh-CN": "修改日期",
        "zh-TW": "修改日期",
        "ja": "日付を変更",
        "ko": "날짜 수정",
        "es": "Editar fecha",
        "fr": "Modifier la date",
        "pt-BR": "Editar data",
    },
    "settings_together_date_picker_title": {
        "en": "When did you get together?",
        "zh-CN": "你们是什么时候在一起的？",
        "zh-TW": "你們是什麼時候在一起的？",
        "ja": "ふたりはいつ一緒になりましたか",
        "ko": "두 분은 언제 함께가 되었나요",
        "es": "¿Cuándo empezaron a estar juntos?",
        "fr": "Quand avez-vous commencé ensemble ?",
        "pt-BR": "Quando vocês ficaram juntos?",
    },
    "settings_together_date_clear_msg": {
        "en": "Your together-since date will be cleared.",
        "zh-CN": "将清除你们「在一起的日子」。",
        "zh-TW": "將清除你們「在一起的日子」。",
        "ja": "在一起の日付を消去します。",
        "ko": "함께한 날짜를 지워요.",
        "es": "Se borrará la fecha de «juntos desde».",
        "fr": "La date « ensemble depuis » sera effacée.",
        "pt-BR": "A data de « juntos desde » será apagada.",
    },
    "settings_together_date_cleared": {
        "en": "Date cleared",
        "zh-CN": "日期已清除",
        "zh-TW": "日期已清除",
        "ja": "日付を消去しました",
        "ko": "날짜를 지웠어요",
        "es": "Fecha borrada",
        "fr": "Date effacée",
        "pt-BR": "Data apagada",
    },
    "home_together_since_fmt": {
        "en": "Since %1$s",
        "zh-CN": "始于 %1$s",
        "zh-TW": "始於 %1$s",
        "ja": "%1$s より",
        "ko": "%1$s부터",
        "es": "Desde el %1$s",
        "fr": "Depuis le %1$s",
        "pt-BR": "Desde %1$s",
    },
    # ---- Mood card: couples-only private wording ----
    "home_mood_checkin_title": {
        "en": "Between Us",
        "zh-CN": "只对你说",
        "zh-TW": "只對你說",
        "ja": "ふたりだけの気持ち",
        "ko": "우리 둘만의",
        "es": "Entre Nosotros",
        "fr": "Entre Nous",
        "pt-BR": "Só Entre Nós",
    },
    "home_mood_checkin_subtitle": {
        "en": "A private corner for the two of you - only you can read it.",
        "zh-CN": "只属于你们两个人的小角落，只有你们看得到。",
        "zh-TW": "只屬於你們兩個人的小角落，只有你們看得到。",
        "ja": "ふたりだけの小さな場所。読めるのはあなたたちだけ。",
        "ko": "두 사람만 아는 곳. 오직 두 분만 볼 수 있어요.",
        "es": "Un rincón privado para los dos: solo vosotros lo leéis.",
        "fr": "Un petit coin privé pour vous deux : vous seul pouvez le lire.",
        "pt-BR": "Um cantinho privado para os dois: só vocês leem.",
    },
    "home_mood_btn": {
        "en": "Tell me how you feel",
        "zh-CN": "告诉我你此刻的心情",
        "zh-TW": "告訴我你此刻的心情",
        "ja": "今の気持ちを教えて",
        "ko": "지금 기분을 알려줘",
        "es": "Cuéntame cómo te sientes",
        "fr": "Dis-moi comment tu te sens",
        "pt-BR": "Me conta como você está",
    },
    "mood_edit_today": {
        "en": "Edit our note",
        "zh-CN": "修改我们的小记",
        "zh-TW": "修改我們的小記",
        "ja": "今日のメモを編集",
        "ko": "오늘 메모 수정",
        "es": "Editar nuestra nota",
        "fr": "Modifier notre note",
        "pt-BR": "Editar nossa nota",
    },
    "mood_gratitude_a_named": {
        "en": "One thing I love about %1$s",
        "zh-CN": "关于 %1$s，我最喜欢的一点",
        "zh-TW": "關於 %1$s，我最喜歡的一點",
        "ja": "%1$s の好きなところ",
        "ko": "%1$s(으)에게 마음에 드는 점",
        "es": "Algo que me encanta de %1$s",
        "fr": "Ce que j\'aime chez %1$s",
        "pt-BR": "Algo que eu amo em %1$s",
    },
    "mood_gratitude_b_named": {
        "en": "One thing I love about us today",
        "zh-CN": "今天我爱着我们的一点",
        "zh-TW": "今天我愛著我們的一點",
        "ja": "今日のふたりについて好きだったこと",
        "ko": "오늘 우리가 좋았던 점",
        "es": "Algo que me encantó de nosotros hoy",
        "fr": "Ce que j\'aime de nous aujourd\'hui",
        "pt-BR": "Algo que eu amei em nós hoje",
    },
    # ---- Sealed letter: author options use nicknames ----
    "letter_author_label": {
        "en": "Who is writing this letter?",
        "zh-CN": "这封信是谁写的？",
        "zh-TW": "這封信是誰寫的？",
        "ja": "この手紙はふたりのどちらが書きましたか",
        "ko": "이 편지는 누가 썼나요",
        "es": "¿Quién escribe esta carta?",
        "fr": "Qui écrit cette lettre ?",
        "pt-BR": "Quem está escrevendo esta carta?",
    },
    # Fallback labels for the two author radio buttons. At runtime they are
    # replaced by the couple's own nicknames; these only prevent a flash of
    # the generic "Partner A/B" wording before the profile is read.
    "letter_author_a": {
        "en": "Partner A",
        "zh-CN": "伴侣 A",
        "zh-TW": "伴侶 A",
        "ja": "ふたり目のパートナー",
        "ko": "파트너 A",
        "es": "Pareja A",
        "fr": "Partenaire A",
        "pt-BR": "Parceiro A",
    },
    "letter_author_b": {
        "en": "Partner B",
        "zh-CN": "伴侣 B",
        "zh-TW": "伴侶 B",
        "ja": "もうひとりのパートナー",
        "ko": "파트너 B",
        "es": "Pareja B",
        "fr": "Partenaire B",
        "pt-BR": "Parceiro B",
    },
}


def esc(value):
    return (value.replace("\\", "\\\\")
                 .replace("'", "\\'")
                 .replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;"))


def main():
    problems = []
    for folder, lang in LANGS.items():
        path = os.path.join(RES, folder, "strings.xml")
        if not os.path.exists(path):
            problems.append(f"MISSING FILE {path}")
            continue
        text = io.open(path, encoding="utf-8").read()
        replaced = 0
        appended = 0
        for key, by_lang in STRINGS.items():
            value = by_lang[lang]
            pattern = re.compile(
                r'(<string name="%s">)(.*?)(</string>)' % re.escape(key), re.S)
            block = '<string name="%s">%s</string>' % (key, esc(value))
            if pattern.search(text):
                text = pattern.sub(lambda m: block, text, count=1)
                replaced += 1
            else:
                text = text.replace(
                    "</resources>", "    " + block + "\n</resources>")
                appended += 1
        io.open(path, "w", encoding="utf-8").write(text)
        print(f"{folder}: replaced={replaced} appended={appended}")

    if problems:
        for p in problems:
            print(p)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

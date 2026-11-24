#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Append strings for the 4 relationship features (daily reveal / mood / streak+badges /
days-together) to all 8 strings.xml files. Idempotent: skips when the first key already exists."""
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
FIRST_KEY = "mode_daily_reveal"

# Each dict maps key -> localized value. Order is identical across all languages.
COMMON_KEYS = [
    "mode_daily_reveal", "mode_daily_reveal_desc", "daily_reveal_start", "daily_reveal_next",
    "daily_reveal_again", "daily_reveal_reveal", "daily_reveal_answer_hint", "daily_reveal_answer_required",
    "daily_reveal_turn_a", "daily_reveal_turn_b", "daily_reveal_revealed",
    "home_mood_checkin_title", "home_mood_btn", "home_mood_save", "mood_how_feel",
    "mood_1", "mood_2", "mood_3", "mood_4", "mood_5",
    "mood_gratitude_a", "mood_gratitude_b", "mood_gratitude_hint", "mood_required",
    "msg_mood_saved", "mood_done_today", "mood_pending_today", "mood_edit_today",
    "home_milestones_title", "home_streak_fmt", "home_badges_fmt",
    "badge_first_night", "badge_five_nights", "badge_streak_3", "badge_streak_7", "badge_days_14",
    "home_together_fmt", "home_together_anniversary_today", "home_together_unset",
    "settings_set_together_date", "settings_together_date_hint", "settings_together_date_msg",
    "settings_together_date_invalid", "settings_together_date_saved", "settings_together_date_clear",
    "action_cancel", "action_save",
]

DATA = {
    "values": [
        "Daily Two-Player Reveal", "One question, two private answers, revealed together.", "Start today's reveal",
        "I've answered", "Another question", "Reveal our answers", "Write your honest answer...", "Please write an answer first",
        "Your turn - write your answer privately", "Pass the phone. Now it's your turn", "Both answers revealed",
        "Mood & Gratitude", "How is your heart today?", "Save check-in", "How are you feeling?",
        "Low", "Meh", "Okay", "Good", "Amazing",
        "One thing I appreciate about you", "One thing I loved about us today", "Write a short note... (optional)",
        "Pick a mood first", "Saved. Thank you for sharing.", "Checked in today - beautiful", "Check in today", "Edit today's check-in",
        "Our Milestones", "Mood streak: %1$d day(s)", "Badges unlocked: %1$d/%2$d - %3$s",
        "First Night", "Five Nights", "3-Day Streak", "7-Day Streak", "14 Days Together",
        "Together for %1$d days - %2$d day(s) to anniversary", "Together for %1$d days - happy anniversary today!",
        "Set your 'together since' date in Settings to see this",
        "Set our together date", "yyyy-MM-dd", "Enter the date you became a couple (yyyy-MM-dd).",
        "Invalid date. Use yyyy-MM-dd and a date that is not in the future.",
        "Date saved", "Clear date", "Cancel", "Save",
    ],
    "values-zh-rCN": [
        "每日双人揭晓", "一个问题，两份私密作答，一起揭晓。", "开始今日揭晓",
        "我答好了", "再来一题", "揭晓答案", "写下你真实的答案……", "请先写下答案",
        "轮到你了——请私密作答", "把手机交给对方，现在轮到你了", "双方答案已揭晓",
        "心情与感激", "今天心情如何？", "保存打卡", "你现在感觉怎么样？",
        "低落", "一般", "还行", "不错", "超棒",
        "一件欣赏对方的事", "一件今天让你们心动的事", "写一句短话……（可选）",
        "请先选择心情", "已保存，谢谢你的分享。", "今日已打卡，真棒", "今日打卡", "修改今日打卡",
        "我们的里程碑", "心情连续打卡：%1$d 天", "已解锁徽章：%1$d/%2$d - %3$s",
        "第一次约会夜", "第五个约会夜", "连续 3 天", "连续 7 天", "相恋 14 天",
        "已相恋 %1$d 天 - 距纪念日还有 %2$d 天", "已相恋 %1$d 天 - 今天就是纪念日！",
        "在设置中填写「在一起日期」即可显示",
        "设置在一起日期", "yyyy-MM-dd", "输入你们确定关系的日期（yyyy-MM-dd）。",
        "日期无效，请使用 yyyy-MM-dd 且不能是未来日期。",
        "日期已保存", "清除日期", "取消", "保存",
    ],
    "values-zh-rTW": [
        "每日雙人揭曉", "一個問題、兩份私密作答，一起揭曉。", "開始今日揭曉",
        "我答好了", "再來一題", "揭曉答案", "寫下你真實的答案……", "請先寫下答案",
        "輪到你了——請私密作答", "把手機交給對方，現在輪到你了", "雙方答案已揭曉",
        "心情與感激", "今天心情如何？", "儲存打卡", "你現在感覺怎麼樣？",
        "低落", "普通", "還行", "不錯", "超棒",
        "一件欣賞對方的事", "一件今天讓你們心動的事", "寫一句短話……（可選）",
        "請先選擇心情", "已儲存，謝謝你的分享。", "今日已打卡，真棒", "今日打卡", "修改今日打卡",
        "我們的里程碑", "心情連續打卡：%1$d 天", "已解鎖徽章：%1$d/%2$d - %3$s",
        "第一次約會夜", "第五個約會夜", "連續 3 天", "連續 7 天", "相戀 14 天",
        "已相戀 %1$d 天 - 距紀念日還有 %2$d 天", "已相戀 %1$d 天 - 今天就是紀念日！",
        "在設定中填寫「在一起日期」即可顯示",
        "設定在一起日期", "yyyy-MM-dd", "輸入你們確定關係的日期（yyyy-MM-dd）。",
        "日期無效，請使用 yyyy-MM-dd 且不能是未來日期。",
        "日期已儲存", "清除日期", "取消", "儲存",
    ],
    "values-ja": [
        "毎日のふたり開示", "ひとつの質問にふたりで答えて、一緒に見せ合おう。", "今日の開示を始める",
        "答えたよ", "もう一問", "答えを見せる", "正直な答えを書いてね…", "先に答えを書いてください",
        "あなたの番です・こっそり答えてね", "スマホを渡して。次はあなたの番", "ふたりの答えを公開しました",
        "気持ちと感謝", "今日の気分はどう？", "記録を保存", "今の気分はいかが？",
        "低落", "まあまあ", "普通", "いい感じ", "最高",
        "相手の好きなところ", "今日のふたりの嬉しかったこと", "短く一言…（任意）",
        "まず気分を選んでね", "保存しました。シェアありがとう。", "今日は記録済み・素敵", "今日の記録", "今日の記録を編集",
        "ふたりの記録", "気分の連続記録：%1$d 日", "解放したバッジ：%1$d/%2$d - %3$s",
        "初めての夜", "5回目の夜", "3日連続", "7日連続", "14日一緒に",
        "一緒に%1$d日 - 記念日まであと%2$d日", "一緒に%1$d日 - 今日は記念日！",
        "設定で「付き合った日」を入力すると表示されます",
        "付き合った日を設定", "yyyy-MM-dd", "付き合った日を入力（yyyy-MM-dd）。",
        "日付が無効です。yyyy-MM-dd で未来以外を指定してください。",
        "日付を保存しました", "日付を消去", "キャンセル", "保存",
    ],
    "values-ko": [
        "매일 두 사람 공개", "질문 하나에 각자 답하고 함께 확인해요.", "오늘 공개 시작",
        "답했어요", "다른 질문", "답 공개하기", "솔직한 답을 적어 주세요...", "답을 먼저 적어 주세요",
        "당신 차례예요 - 몰래 답해 주세요", "폰을 건네주세요. 이제 상대 차례", "두 사람의 답을 공개했어요",
        "기분과 감사", "오늘 마음은 어때요?", "기록 저장", "지금 기분이 어떤가요?",
        "우울", "그저 그래", "보통", "좋음", "최고",
        "상대에게 감사한 점", "오늘 둘이서 좋았던 일", "짧게 적어 주세요... (선택)",
        "기분을 먼저 골라 주세요", "저장했어요. 공유해 주셔서 감사해요.", "오늘 기록 완료 - 멋져요", "오늘 기록", "오늘 기록 수정",
        "우리의 기록", "기분 연속 기록: %1$d일", "획득 배지: %1$d/%2$d - %3$s",
        "첫 데이트 나이트", "다섯 번째 밤", "3일 연속", "7일 연속", "함께 14일",
        "함께 %1$d일 - 기념일까지 %2$d일", "함께 %1$d일 - 오늘이 기념일이에요!",
        "설정에서 '함께 시작한 날'을 입력하면 표시돼요",
        "함께 시작한 날 설정", "yyyy-MM-dd", "연인이 된 날을 입력하세요 (yyyy-MM-dd).",
        "날짜가 올바르지 않아요. yyyy-MM-dd 형식의 미래가 아닌 날짜를 입력하세요.",
        "날짜 저장됨", "날짜 지우기", "취소", "저장",
    ],
    "values-es": [
        "Revelación diaria a dos", "Una pregunta, dos respuestas privadas, reveladas juntos.", "Empezar la revelación de hoy",
        "Ya respondí", "Otra pregunta", "Revelar respuestas", "Escribe tu respuesta sincera...", "Escribe una respuesta primero",
        "Tu turno: responde en privado", "Pasa el móvil. Ahora te toca a ti", "Ambas respuestas reveladas",
        "Ánimo y gratitud", "¿Cómo está tu corazón hoy?", "Guardar registro", "¿Cómo te sientes ahora?",
        "Bajo", "Regular", "Bien", "Muy bien", "Genial",
        "Algo que admiro de ti", "Algo que me encantó de nosotros hoy", "Escribe una nota breve... (opcional)",
        "Elige un ánimo primero", "Guardado. Gracias por compartir.", "Registrado hoy - qué bien", "Registrar hoy", "Editar el registro de hoy",
        "Nuestros hitos", "Racha de ánimo: %1$d día(s)", "Insignias: %1$d/%2$d - %3$s",
        "Primera noche", "Quinta noche", "Racha de 3 días", "Racha de 7 días", "14 días juntos",
        "Juntos %1$d días - %2$d día(s) para el aniversario", "Juntos %1$d días - ¡feliz aniversario hoy!",
        "Configura la fecha de 'juntos desde' en Ajustes para ver esto",
        "Fijar fecha de inicio", "yyyy-MM-dd", "Introduce la fecha en que empezasteis (yyyy-MM-dd).",
        "Fecha no válida. Usa yyyy-MM-dd y una fecha que no sea futura.",
        "Fecha guardada", "Borrar fecha", "Cancelar", "Guardar",
    ],
    "values-fr": [
        "Révélation quotidienne à deux", "Une question, deux réponses privées, dévoilées ensemble.", "Commencer la révélation du jour",
        "J'ai répondu", "Une autre question", "Révéler", "Écris ta réponse sincère...", "Écris d'abord une réponse",
        "À toi - réponds en privé", "Passe le téléphone. À toi maintenant", "Les deux réponses sont révélées",
        "Humeur et gratitude", "Comment va ton coeur aujourd'hui ?", "Enregistrer", "Comment te sens-tu ?",
        "Triste", "Bof", "Correct", "Bien", "Super",
        "Une chose que j'apprécie chez toi", "Un moment qu j'ai adoré aujourd'hui", "Écris un court mot... (facultatif)",
        "Choisis d'abord une humeur", "Enregistré. Merci d'avoir partagé.", "Noté aujourd'hui - bravo", "Noter aujourd'hui", "Modifier la note du jour",
        "Nos étapes", "Série d'humeur : %1$d jour(s)", "Badges débloqués : %1$d/%2$d - %3$s",
        "Première soirée", "Cinquième soirée", "Série de 3 jours", "Série de 7 jours", "14 jours ensemble",
        "Ensemble depuis %1$d jours - %2$d jour(s) avant l'anniversaire", "Ensemble depuis %1$d jours - joyeux anniversaire !",
        "Définis la date d'anniversaire dans Réglages pour l'afficher",
        "Définir la date", "yyyy-MM-dd", "Saisis la date de votre rencontre (yyyy-MM-dd).",
        "Date invalide. Utilise yyyy-MM-dd avec une date non future.",
        "Date enregistrée", "Effacer la date", "Annuler", "Enregistrer",
    ],
    "values-pt-rBR": [
        "Revelação diária a dois", "Uma pergunta, duas respostas privadas, reveladas juntas.", "Começar a revelação de hoje",
        "Já respondi", "Outra pergunta", "Revelar respostas", "Escreva sua resposta sincera...", "Escreva uma resposta primeiro",
        "Sua vez - responda em particular", "Passe o celular. Agora é sua vez", "As duas respostas foram reveladas",
        "Humor e gratidão", "Como está seu coração hoje?", "Salvar registro", "Como você está se sentindo?",
        "Baixo", "Mais ou menos", "Ok", "Bom", "Ótimo",
        "Algo que admiro em você", "Algo que amei em nós hoje", "Escreva uma nota curta... (opcional)",
        "Escolha um humor primeiro", "Salvo. Obrigado por compartilhar.", "Registrado hoje - que bom", "Registrar hoje", "Editar o registro de hoje",
        "Nossas conquistas", "Sequência de humor: %1$d dia(s)", "Distintivos: %1$d/%2$d - %3$s",
        "Primeira noite", "Quinta noite", "Sequência de 3 dias", "Sequência de 7 dias", "14 dias juntos",
        "Juntos há %1$d dias - %2$d dia(s) para o aniversário", "Juntos há %1$d dias - feliz aniversário hoje!",
        "Defina a data de 'juntos desde' em Ajustes para ver isto",
        "Definir data de início", "yyyy-MM-dd", "Informe a data em que vocês ficaram juntos (yyyy-MM-dd).",
        "Data inválida. Use yyyy-MM-dd com uma data que não seja futura.",
        "Data salva", "Limpar data", "Cancelar", "Salvar",
    ],
}


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main():
    for lang, values in DATA.items():
        assert len(values) == len(COMMON_KEYS), f"{lang}: {len(values)} != {len(COMMON_KEYS)}"
        path = os.path.join(ROOT, lang, "strings.xml")
        with open(path, encoding="utf-8") as f:
            txt = f.read()
        if f'name="{FIRST_KEY}"' in txt:
            print(f"{lang}: skipped (already present)")
            continue
        block = ""
        for key, value in zip(COMMON_KEYS, values):
            block += f'    <string name="{key}">{esc(value)}</string>\n'
        txt = txt.replace("</resources>", block + "</resources>")
        with open(path, "w", encoding="utf-8") as f:
            f.write(txt)
        print(f"{lang}: added {len(COMMON_KEYS)} strings")


if __name__ == "__main__":
    main()

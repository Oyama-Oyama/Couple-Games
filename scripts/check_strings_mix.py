# -*- coding: utf-8 -*-
"""Detect mixed-script contamination inside the string resources.

Rules:
  * CJK / Korean strings must not contain Latin words outside a whitelist of
    established loanwords (VIP, Google Play, pronouns shown in English, ...).
  * Date / time format patterns (yyyy-MM-dd, HH:mm) are stripped before the
    scan so they never trip the rule.
  * Non-CJK strings must not contain Han, Kana, Hangul or Cyrillic characters.
  * Every language must define the same keys as the base set.

Usage: python scripts/check_strings_mix.py
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

LANGS = ["values", "values-zh-rCN", "values-zh-rTW", "values-ja",
         "values-ko", "values-es", "values-fr", "values-pt-rBR"]

CJK_LANGS = {"values-zh-rCN", "values-zh-rTW", "values-ja", "values-ko"}
LATIN = re.compile(r"[A-Za-z]{2,}")
HAN = re.compile(r"[一-鿿]")
KANA = re.compile(r"[぀-ヿ]")
HANGUL = re.compile(r"[가-힯]")
CYRILLIC = re.compile(r"[Ѐ-ӿ]")

# Format patterns that legitimately appear inside localised copy.
FORMAT_PATTERNS = [
    re.compile(r"yyyy[-\s/]?MM[-\s/]?dd", re.I),
    re.compile(r"HH:mm"),
    re.compile(r"MM[-/]dd"),
]

# Established loanwords and product names used across every language.
ALLOWED = {
    "vip", "google", "play", "store", "ok", "en", "cafe", "velvet", "ai",
    "he", "him", "she", "her", "they", "them", "ta", "app", "android",
    "wifi", "link", "chat", "video", "call", "date", "night",
    # Established UI terms used as-is across languages.
    "ng", "yes", "no", "pass", "bgm", "off", "bf", "lv", "km", "cm",
}

# Simplified-Chinese-only characters that never appear in modern Japanese
# copy. Used to catch a Chinese sentence silently pasted into a Japanese
# string (the Latin-word rule cannot see it).
SIMPLIFIED_ONLY = set(
    "谁这说时们会后为单双关无长门间见头应场报当录态优传儿团图圆围声备够"
    "夺妇忆怀惊惯战执扩扫扬扰抢护挥换据授掉排控探摄摆敌敢断显术机权条"
    "来极构枪柜标栋样桥检概欢气没洁浅温满灯炉点独狮猪献环现珍产毕异疗"
    "监盖盘眼矿码碍礼种积称稳竞笔签简糖系素红级组细织绍经结给络统继"
    "维综编缘网罗习联职脑脚脱脸腾舰艰艳艺节苏范荡荣药莱莲获营蒙蓝虑"
    "虚虽蚀蜡补装裤观规视览觉触誉计订认讨训议记讲许论证评识译试诗诚"
    "话询该详语误诱请诸读课调谈谊谋谐谨贝负贡财责贤败货质贩贪贫购贯"
    "贱贴贵贷贸费贺贼资赋赏赔赖赛赞赠赵趋跃践车轧轨转轮软轰轴轻载辆"
    "辈辉输辑辖辞辩边辽达迁迅过迈运还进远违连迟适选逊递逻遗邓邮邹"
    "郑邻郁酝酱酿释鉴针钉钙钛钝钞钟钠钢钥钦钧钩钮钱钳钻钾铁铃铅铎铛"
    "铜铝铠铡铢铣铤铨铬铭铮铰铲银铸铺链销锁锂锄锅锈锋锌锐锑错锚锡锣"
    "锤锥锦键锯锰锲锵锻镀镁镇镉镊镍镑镖镜镰镶闪闭问闯闰闲闷闸闹闻阀"
    "阁阅阔队阴阵阶际陆陈陕陨险随隐难雏雾霉静韦韩韬页顶项顺须顽顾顿"
    "预颅领颇颈颊频颓颖颗题颜额颠颤风飘飞饥饭饮饰饱饲饶饺饼饿馅馆馈"
    "馒马驭驱驳驴驶驹驻驼驾骂骄骆骇验骏骑骗骚骤鱼鲁鲍鲜鲤鲨鲸鳄鸟鸡"
    "鸣鸥鸦鸭鸳鸵鸽鸿鹃鹅鹉鹊鹏鹤鹰麦黄齐齿龄龙龟"
)


def strip_formats(text):
    for pattern in FORMAT_PATTERNS:
        text = pattern.sub(" ", text)
    return text


def load(path):
    strings = {}
    root = ET.parse(path).getroot()
    for node in root.findall("string"):
        name = node.get("name")
        if name:
            strings[name] = node.text or ""
    return strings


def main():
    problems = []
    base = None
    for lang in LANGS:
        path = os.path.join(RES, lang, "strings.xml")
        if not os.path.exists(path):
            problems.append((lang, "MISSING_FILE", path, ""))
            continue
        strings = load(path)
        if base is None:
            base = strings
        else:
            missing = set(base) - set(strings)
            if missing:
                problems.append((lang, "MISSING_KEYS", ",".join(sorted(missing)[:8]), ""))

        for name, text in strings.items():
            if lang in CJK_LANGS:
                cleaned = strip_formats(text)
                for token in LATIN.findall(cleaned):
                    if token.lower() in ALLOWED:
                        continue
                    problems.append((lang, name, token, text))
                # Japanese must not contain simplified-Chinese-only characters
                # that never occur in Japanese copy. Single characters are
                # ambiguous (会, 静, 探 are all valid Japanese kanji), so only
                # flag them when several appear close together, which is the
                # signature of a pasted Chinese sentence.
                if lang == "values-ja":
                    run = 0
                    for ch in cleaned:
                        if ch in SIMPLIFIED_ONLY:
                            run += 1
                            if run >= 3:
                                problems.append((lang, name, "CJK_RUN_IN_JA:" + ch, text))
                                break
                        elif HAN.match(ch):
                            run = 0
                        else:
                            run = 0
                if lang == "values-ko":
                    for ch in cleaned:
                        if HAN.match(ch) or KANA.match(ch):
                            problems.append((lang, name, "CJK_IN_KO:" + ch, text))
                            break
            else:
                for label, pattern in (("HAN", HAN), ("KANA", KANA),
                                       ("HANGUL", HANGUL), ("CYRILLIC", CYRILLIC)):
                    if pattern.search(text):
                        problems.append((lang, name, label, text))

    if not problems:
        print("OK: no script contamination in string resources")
        return 0

    print(f"FOUND {len(problems)} problems:")
    for lang, name, token, text in problems:
        print(f"  [{lang}] {name}: '{token}' :: {text}")
    return 1


if __name__ == "__main__":
    sys.exit(main())

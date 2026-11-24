# -*- coding: utf-8 -*-
"""Detect mixed-script contamination inside CJK/Korean sealed-letter strings.

CJK strings must not contain Latin words other than a small whitelist of
accepted loanwords. Any other Latin token is a generation artefact and must
be fixed before the content ships.

Also rejects Cyrillic / Hangul / Han characters leaking into the wrong
language slot, which is how a stray Japanese or Russian fragment sneaks in.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sl_content import LANGS, SEALS, PROMPTS, DELAYS  # noqa: E402

CJK_LANGS = ["zh", "zh_tw", "ja", "ko"]
LATIN = re.compile(r"[A-Za-z]{2,}")

# Loanwords that legitimately appear inside CJK copy.
ALLOWED = {"en", "ok", "cafe", "mvp"}

# CJK scripts that must not appear in a non-CJK language slot.
HAN = re.compile(r"[一-鿿]")
KANA = re.compile(r"[぀-ヿ]")
HANGUL = re.compile(r"[가-힯]")
CYRILLIC = re.compile(r"[Ѐ-ӿ]")


def collect():
    """Yield (entry_id, field, lang, text) for every translatable string."""
    for seal in SEALS:
        for lang, text in seal["label"].items():
            yield seal["id"], "label", lang, text
    for prompt in PROMPTS:
        for lang, text in prompt["text"].items():
            yield prompt["id"], "text", lang, text
    for delay in DELAYS:
        for lang, text in delay["label"].items():
            yield delay["id"], "label", lang, text


def main():
    problems = []
    for entry_id, field, lang, text in collect():
        if lang not in CJK_LANGS:
            # Non-CJK languages must not contain Han / Kana / Hangul / Cyrillic.
            for name, pattern in (("HAN", HAN), ("KANA", KANA),
                                  ("HANGUL", HANGUL), ("CYRILLIC", CYRILLIC)):
                if pattern.search(text):
                    problems.append((entry_id, field, lang, name, text))
            continue
        for token in LATIN.findall(text):
            if token.lower() in ALLOWED:
                continue
            problems.append((entry_id, field, lang, token, text))

    # Every language must define every string of an entry.
    for entry_id, field, lang, text in collect():
        pass
    for entry in SEALS:
        missing = set(LANGS) - set(entry["label"])
        if missing:
            problems.append((entry["id"], "label", "missing", ",".join(sorted(missing)), ""))
    for entry in PROMPTS:
        missing = set(LANGS) - set(entry["text"])
        if missing:
            problems.append((entry["id"], "text", "missing", ",".join(sorted(missing)), ""))
    for entry in DELAYS:
        missing = set(LANGS) - set(entry["label"])
        if missing:
            problems.append((entry["id"], "label", "missing", ",".join(sorted(missing)), ""))

    if not problems:
        print("OK: no script contamination in sealed-letter strings")
        return 0

    print(f"FOUND {len(problems)} problems:")
    for pid, field, lang, token, text in problems:
        print(f"  [{pid}/{field}/{lang}] '{token}' :: {text}")
    return 1


if __name__ == "__main__":
    sys.exit(main())

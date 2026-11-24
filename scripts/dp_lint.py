# -*- coding: utf-8 -*-
"""Detect mixed-script contamination inside CJK/Korean date-planner strings.

CJK strings should not contain Latin words other than a small whitelist of
accepted loanwords. Any other Latin token is a generation artefact and must be
fixed before the content ships.
"""
import re
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dp_activities_part1 import PART1  # noqa: E402
from dp_activities_part2 import PART2  # noqa: E402

CJK_LANGS = ["zh", "zh_tw", "ja", "ko"]
LATIN = re.compile(r"[A-Za-z]{2,}")

# Loanwords / names that legitimately appear inside CJK copy.
ALLOWED = {
    "en", "wine", "hot", "horror", "ok", "cafe",
    "the", "and", "for", "with", "from", "that", "this", "your", "you",
    "happy", "ending", "small", "one", "no", "not", "it", "is", "in", "on",
    "fairy", "lights", "blind", "tasting",
    "to", "of", "a", "at", "we", "us", "my", "me", "i",
}

# Common English tokens that betray an untranslated fragment.
STOPWORDS = set("""the a an and or but of to in on at for with without from by as
is are was were be been being do does did have has had will would can could
should shall may might must not no yes if then than that this these those
it its his her their our your my me we you they he she i am do did done
one two three four five six seven eight nine ten twelve all every each
what when where why how who which very more most much many some any
each other another other another one thing things time day days night
week weeks month year hour hours minute minutes room house home bed
food eat drink meal dinner lunch breakfast coffee drink music song
phone phones screen screens lights blanket fort museum cafe market
station train car drive sea water coast sunset sunrise moon stars
touch kiss hug hold hand hands eye eyes smile smile
write read book letter poem question questions
game play win winner choose pick
please let lets get go come make cook
""".split())


def main():
    problems = []
    for entry in PART1 + PART2:
        for field in ("title", "detail"):
            for lang in CJK_LANGS:
                text = entry[field].get(lang, "")
                for token in LATIN.findall(text):
                    low = token.lower()
                    if low in ALLOWED:
                        continue
                    problems.append((entry["id"], field, lang, token, text))

    if not problems:
        print("OK: no Latin contamination in CJK strings")
        return 0

    print(f"FOUND {len(problems)} suspicious tokens:")
    for pid, field, lang, token, text in problems:
        print(f"  [{pid}/{field}/{lang}] '{token}' :: {text}")
    return 1


if __name__ == "__main__":
    sys.exit(main())

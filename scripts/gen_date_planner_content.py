# -*- coding: utf-8 -*-
"""Generate assets/content/{lang}/date_planner.json for all 8 languages.

Source of truth: dp_activities_part1.py + dp_activities_part2.py.
Every language file carries the identical id set so verify_content.py can
assert full ID alignment against the English baseline.
"""
import io
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

from dp_activities_part1 import PART1  # noqa: E402
from dp_activities_part2 import PART2  # noqa: E402

LANGS = ["en", "zh", "zh_tw", "ja", "ko", "es", "fr", "pt"]
ASSET_DIRS = {
    "en": "en", "zh": "zh", "zh_tw": "zh_tw", "ja": "ja",
    "ko": "ko", "es": "es", "fr": "fr", "pt": "pt",
}

CATEGORY_ORDER = ["dining", "outdoor", "indoor", "culture", "getaway", "ritual"]


def main():
    activities = PART1 + PART2
    base = os.path.join(ROOT, "app", "src", "main", "assets", "content")

    for lang in LANGS:
        rows = []
        for act in activities:
            rows.append({
                "id": act["id"],
                "category": act["category"],
                "tier": act["tier"],
                "durationMin": act["durationMin"],
                "tags": act["tags"],
                "title": act["title"][lang],
                "detail": act["detail"][lang],
            })
        rows.sort(key=lambda r: (CATEGORY_ORDER.index(r["category"]), r["id"]))

        payload = {
            "version": 1,
            "categories": CATEGORY_ORDER,
            "activities": rows,
        }
        out_dir = os.path.join(base, ASSET_DIRS[lang])
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "date_planner.json")
        with io.open(out_path, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
        print(f"{lang}: {len(rows)} activities -> {out_path}")

    # sanity: identical id sets across languages
    idsets = []
    for lang in LANGS:
        p = os.path.join(base, ASSET_DIRS[lang], "date_planner.json")
        d = json.load(io.open(p, encoding="utf-8"))
        idsets.append({a["id"] for a in d["activities"]})
    for i in range(1, len(idsets)):
        if idsets[i] != idsets[0]:
            missing = idsets[0] - idsets[i]
            extra = idsets[i] - idsets[0]
            print(f"MISMATCH vs en at index {i}: missing={missing} extra={extra}")
            return 1
    print("ID alignment across all 8 languages: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())

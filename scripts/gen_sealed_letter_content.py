# -*- coding: utf-8 -*-
"""Generate assets/content/{lang}/sealed_letter.json for all 8 languages.

Source of truth: sl_content.py.
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

from sl_content import LANGS, SEALS, PROMPTS, DELAYS  # noqa: E402

ASSET_DIRS = {lang: lang for lang in LANGS}


def main():
    base = os.path.join(ROOT, "app", "src", "main", "assets", "content")

    for lang in LANGS:
        seals = [
            {
                "id": s["id"],
                "symbol": s["symbol"],
                "label": s["label"][lang],
            }
            for s in SEALS
        ]
        prompts = [
            {
                "id": p["id"],
                "text": p["text"][lang],
            }
            for p in PROMPTS
        ]
        delays = [
            {
                "id": d["id"],
                "days": d["days"],
                "label": d["label"][lang],
            }
            for d in DELAYS
        ]
        payload = {
            "version": 1,
            "seals": seals,
            "prompts": prompts,
            "delays": delays,
        }
        out_dir = os.path.join(base, ASSET_DIRS[lang])
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "sealed_letter.json")
        with io.open(out_path, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
        print(f"{lang}: {len(seals)} seals / {len(prompts)} prompts / {len(delays)} delays")

    # sanity: identical id sets across languages
    idsets = []
    for lang in LANGS:
        p = os.path.join(base, ASSET_DIRS[lang], "sealed_letter.json")
        d = json.load(io.open(p, encoding="utf-8"))
        idsets.append({
            "seals": {s["id"] for s in d["seals"]},
            "prompts": {p_["id"] for p_ in d["prompts"]},
            "delays": {x["id"] for x in d["delays"]},
        })
    for i in range(1, len(idsets)):
        if idsets[i] != idsets[0]:
            print(f"MISMATCH vs en at index {i}")
            return 1
    print("ID alignment across all 8 languages: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())

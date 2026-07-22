#!/usr/bin/env python3
"""Verify resource parity for the user-facing resources introduced by PR #105."""
from __future__ import annotations
import re, sys
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "android/app/src/main/res"
KEYS = [k for k in (Path(__file__).with_name("pr105_localization_resources.txt")).read_text().splitlines() if k]
LOCALES = {"es": "values-es", "hi": "values-hi", "mr": "values-mr"}
PLACEHOLDER = re.compile(r"%(?:([1-9]\d*)\$)?(?:[-#+ 0,(<]*)?(?:\d+)?(?:\.\d+)?([a-zA-Z%])")
VALID_QUANTITIES = {"zero", "one", "two", "few", "many", "other"}

def parse(directory: str):
    path = ROOT / directory / "strings.xml"
    root = ET.parse(path).getroot()
    result, duplicates = {}, []
    for node in root:
        name = node.get("name")
        if not name: continue
        if name in result: duplicates.append(name)
        result[name] = node
    return result, duplicates

def signature(node):
    texts = ["".join(node.itertext())] if node.tag != "plurals" else ["".join(x.itertext()) for x in node]
    return sorted((int(i or 0), t) for text in texts for i,t in PLACEHOLDER.findall(text) if t != "%")

def main():
    errors=[]
    base, dup = parse("values")
    if dup: errors.append(f"values duplicate resources: {', '.join(dup)}")
    for key in KEYS:
        if key not in base: errors.append(f"inventory key absent from English: {key}")
    for code,directory in LOCALES.items():
        values, duplicates=parse(directory)
        if duplicates: errors.append(f"{directory} duplicate resources: {', '.join(duplicates)}")
        for key in KEYS:
            if key not in values:
                errors.append(f"{directory} missing resource: {key}"); continue
            en, tr=base[key],values[key]
            if en.tag != tr.tag: errors.append(f"{directory} type mismatch {key}: {en.tag} != {tr.tag}")
            if not "".join(tr.itertext()).strip(): errors.append(f"{directory} empty resource: {key}")
            if tr.tag != "plurals" and signature(en) != signature(tr): errors.append(f"{directory} placeholder mismatch: {key}")
            if tr.tag == "plurals":
                quantities={x.get('quantity') for x in tr}
                invalid=quantities-VALID_QUANTITIES
                if invalid: errors.append(f"{directory} invalid plural quantities {key}: {sorted(invalid)}")
                required={"one","other"} | ({"many"} if code=="es" else set())
                missing=required-quantities
                if missing: errors.append(f"{directory} plural {key} missing: {sorted(missing)}")
                en_items={item.get("quantity"): item for item in en}
                for item in tr:
                    source=en_items.get(item.get("quantity"), en_items.get("other"))
                    if source is not None and signature(source) != signature(item):
                        errors.append(f"{directory} plural placeholder mismatch: {key}/{item.get('quantity')}")
    if errors:
        print("Localization parity failed:")
        print("\n".join(f"- {e}" for e in errors)); return 1
    print(f"Localization parity passed: {len(KEYS)} resources across English, Spanish, Hindi, and Marathi.")
    return 0
if __name__ == "__main__": sys.exit(main())

#!/usr/bin/env python3
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path('app/src/main/res/values/strings.xml')
LOCALES = [
    Path('app/src/main/res/values-es/strings.xml'),
    Path('app/src/main/res/values-hi/strings.xml'),
    Path('app/src/main/res/values-mr/strings.xml'),
]
# Matches Android format tokens like %s, %d, %,d, %1$s, %1$,d, %.1f, %1$.10f
PH_RE = re.compile(r'%(?:\d+\$)?(?:,)?(?:\.\d+)?[dfs]')
BRAND_TERMS = [
    'Scyra', 'The Shell', 'The Blue', 'Beyond Blue', 'Flow', 'Soft Flow', 'Stillwater',
    'Pearls', 'Scyra Points', 'Surge', 'Arc', 'Shell Chest', 'Focus Room',
    'Great Blue', 'Open Blue', 'Deeper Reef', 'Sunlit Reef', 'Voyage Hall',
    'Discovery Journal', 'Badges'
]
ASCII_WORD_RE = re.compile(r'[A-Za-z]{3,}')


def load(path: Path):
    root = ET.parse(path).getroot()
    strings, plurals, arrays = {}, {}, {}
    for child in root:
        name = child.get('name')
        if not name:
            continue
        if child.tag == 'string':
            strings[name] = ''.join(child.itertext())
        elif child.tag == 'plurals':
            plurals[name] = {i.get('quantity'): ''.join(i.itertext()) for i in child.findall('item')}
        elif child.tag == 'string-array':
            arrays[name] = [''.join(i.itertext()) for i in child.findall('item')]
    return strings, plurals, arrays


def sig(text: str):
    return tuple(sorted(PH_RE.findall(text or '')))


def suspicious_english_value(value: str) -> bool:
    cleaned = value
    for t in BRAND_TERMS:
        cleaned = cleaned.replace(t, ' ')
    cleaned = PH_RE.sub(' ', cleaned)
    words = ASCII_WORD_RE.findall(cleaned)
    return len(words) >= 4


base_strings, base_plurals, base_arrays = load(BASE)
errors = []
english_hits = []

for locale_path in LOCALES:
    loc_strings, loc_plurals, loc_arrays = load(locale_path)

    for k, v in base_strings.items():
        if k not in loc_strings:
            errors.append(f'{locale_path}: missing string {k}')
            continue
        if not loc_strings[k].strip():
            errors.append(f'{locale_path}: empty string {k}')
        if sig(v) != sig(loc_strings[k]):
            errors.append(f'{locale_path}: placeholder mismatch {k}: {sig(v)} != {sig(loc_strings[k])}')
        if suspicious_english_value(loc_strings[k]):
            english_hits.append((locale_path, k, loc_strings[k]))

    for k, v in base_plurals.items():
        if k not in loc_plurals:
            errors.append(f'{locale_path}: missing plurals {k}')
            continue
        if set(v) != set(loc_plurals[k]):
            errors.append(f'{locale_path}: plural quantities mismatch {k}')
        for q in v:
            if q in loc_plurals[k] and sig(v[q]) != sig(loc_plurals[k][q]):
                errors.append(f'{locale_path}: plural placeholder mismatch {k}.{q}')

    for k, v in base_arrays.items():
        if k not in loc_arrays:
            errors.append(f'{locale_path}: missing string-array {k}')
            continue
        if len(v) != len(loc_arrays[k]):
            errors.append(f'{locale_path}: array length mismatch {k}')
        for i, (bv, lv) in enumerate(zip(v, loc_arrays[k])):
            if sig(bv) != sig(lv):
                errors.append(f'{locale_path}: array placeholder mismatch {k}[{i}]')

if english_hits:
    print('Potential untranslated English strings (review required):')
    for lp, k, v in english_hits[:200]:
        print(f'- {lp}:{k} => {v}')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('String parity check passed.')

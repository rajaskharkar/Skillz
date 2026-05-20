#!/usr/bin/env python3
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
BASE = Path('app/src/main/res/values/strings.xml')
LOCALES = [Path('app/src/main/res/values-es/strings.xml'),Path('app/src/main/res/values-hi/strings.xml'),Path('app/src/main/res/values-mr/strings.xml')]
PH_RE = re.compile(r'%(?:\d+\$)?[,]?(?:\.\d+)?[dfs]')
ALLOW_TERMS = ['Scyra','The Shell','The Blue','Beyond Blue','Flow','Soft Flow','Stillwater','Pearls','Scyra Points','Surge','Arc','Shell Chest','Focus Room','Great Blue','Open Blue','Deeper Reef','Sunlit Reef','Voyage Hall','Discovery Journal','Badges']
ASCII_WORD_RE = re.compile(r"[A-Za-z][A-Za-z']+")

def find_duplicates(path):
 root=ET.parse(path).getroot(); names={'string':[],'plurals':[],'string-array':[]}
 for c in root:
  if c.tag in names and c.get('name'): names[c.tag].append(c.get('name'))
 d=[]
 for tag,vals in names.items():
  for n,cnt in Counter(vals).items():
   if cnt>1: d.append(f'{path}: duplicate <{tag}> name="{n}" ({cnt}x)')
 return d

def load(path):
 root=ET.parse(path).getroot();s={};p={};a={}
 for c in root:
  n=c.get('name')
  if not n: continue
  if c.tag=='string': s[n]=''.join(c.itertext())
  elif c.tag=='plurals': p[n]={i.get('quantity'):''.join(i.itertext()) for i in c.findall('item')}
  elif c.tag=='string-array': a[n]=[''.join(i.itertext()) for i in c.findall('item')]
 return s,p,a

def sig(text): return tuple(sorted(PH_RE.findall((text or '').replace('%%',''))))

def suspicious_english(text):
 cleaned=(text or '').replace('%%',' ')
 for term in ALLOW_TERMS: cleaned=cleaned.replace(term,' ')
 cleaned=PH_RE.sub(' ',cleaned)
 words=ASCII_WORD_RE.findall(cleaned)
 if len(words)<3: return False
 ratio=sum(1 for w in words if all('a'<=ch.lower()<='z' or ch=="'" for ch in w))/len(words)
 return ratio>0.8

errs=[]; warns=[]
for p in [BASE,*LOCALES]: errs.extend(find_duplicates(p))
bs,bp,ba=load(BASE)
for lp in LOCALES:
 ls,lpz,la=load(lp)
 for k,v in bs.items():
  if k not in ls: errs.append(f'{lp}: missing string {k}'); continue
  if not ls[k].strip(): errs.append(f'{lp}: empty string {k}')
  if sig(v)!=sig(ls[k]): errs.append(f'{lp}: placeholder mismatch {k}: {sig(v)} != {sig(ls[k])}')
  if suspicious_english(ls[k]): warns.append(f'{lp}:{k} => {ls[k]}')
 for k,v in bp.items():
  if k not in lpz: errs.append(f'{lp}: missing plurals {k}'); continue
  if set(v)!=set(lpz[k]): errs.append(f'{lp}: plural quantities mismatch {k}')
  for q in v:
   if q in lpz[k] and sig(v[q])!=sig(lpz[k][q]): errs.append(f'{lp}: plural placeholder mismatch {k}.{q}')
 for k,v in ba.items():
  if k not in la: errs.append(f'{lp}: missing string-array {k}'); continue
  if len(v)!=len(la[k]): errs.append(f'{lp}: array length mismatch {k}')
  for i,(bv,lv) in enumerate(zip(v,la[k])):
   if sig(bv)!=sig(lv): errs.append(f'{lp}: array placeholder mismatch {k}[{i}]')
if warns:
 print('Untranslated-English report (warning only):')
 for w in warns[:300]: print('-',w)
if errs:
 print('\n'.join(errs)); sys.exit(1)
print('String parity check passed.')

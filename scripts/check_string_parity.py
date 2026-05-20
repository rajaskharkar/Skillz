#!/usr/bin/env python3
import re,sys,xml.etree.ElementTree as ET
from pathlib import Path
BASE=Path('app/src/main/res/values/strings.xml')
LOCALES=[Path('app/src/main/res/values-es/strings.xml'),Path('app/src/main/res/values-hi/strings.xml'),Path('app/src/main/res/values-mr/strings.xml')]
PH_RE=re.compile(r'%(?:\d+\$)?(?:,d|[ds])')

def load(path):
 root=ET.parse(path).getroot();s={};p={};a={}
 for c in root:
  n=c.get('name')
  if not n: continue
  if c.tag=='string': s[n]=''.join(c.itertext())
  elif c.tag=='plurals': p[n]={i.get('quantity'):''.join(i.itertext()) for i in c.findall('item')}
  elif c.tag=='string-array': a[n]=[''.join(i.itertext()) for i in c.findall('item')]
 return s,p,a

def sig(t): return tuple(sorted(PH_RE.findall(t or '')))
bs,bp,ba=load(BASE);errs=[]
for lp in LOCALES:
 ls,lpz,la=load(lp)
 for k,v in bs.items():
  if k not in ls: errs.append(f'{lp}: missing string {k}'); continue
  if not ls[k].strip(): errs.append(f'{lp}: empty string {k}')
  if sig(v)!=sig(ls[k]): errs.append(f'{lp}: placeholder mismatch {k}: {sig(v)} != {sig(ls[k])}')
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
if errs: print('\n'.join(errs)); sys.exit(1)
print('String parity check passed.')

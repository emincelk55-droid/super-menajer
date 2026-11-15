#!/usr/bin/env python3
"""Çeviri parçasını (index|english) ana JSON'a yazar, kod parçalarının korunduğunu denetler.
Kullanım: i18n_merge.py <ana.json> <parca.py>"""
import json, re, sys, runpy
def tokens(s):
    out = []; i = 0
    while i < len(s):
        if s.startswith('${', i):
            d = 0; j = i
            while j < len(s):
                if s.startswith('${', j): d += 1; j += 2; continue
                if s[j] == '{': d += 1
                elif s[j] == '}':
                    d -= 1
                    if d == 0: break
                j += 1
            out.append(s[i:j + 1]); i = j + 1; continue
        m = re.match(r'\$[a-zA-Z_][a-zA-Z0-9_]*', s[i:])
        if m: out.append(m.group(0)); i += len(m.group(0)); continue
        i += 1
    return sorted(out)
d = json.load(open(sys.argv[1], encoding='utf-8'))
keys = list(d)
T = runpy.run_path(sys.argv[2])['T']
bad = 0; n = 0
for line in T.strip('\n').split('\n'):
    idx, _, en = line.partition('|')
    force = idx.endswith('!')
    k = keys[int(idx.rstrip('!'))]
    if not en.strip(): continue
    # '!' ile işaretli satırda ${...} içindeki metin literalleri de çevrilmiştir (kod yapısı aynı)
    if not force and tokens(k) != tokens(en):
        print('KOD UYUMSUZ', idx, k, '=>', en); bad += 1; continue
    if en.count('"') % 2 or '\\' in en.replace('\\"', ''):
        pass
    d[k] = en; n += 1
json.dump(d, open(sys.argv[1], 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(n, 'çeviri eklendi,', bad, 'hata')

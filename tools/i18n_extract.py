#!/usr/bin/env python3
"""Kotlin kaynaklarından çevrilecek metinleri çıkarır / L("tr","en") ile değiştirir.
Kullanım: i18n_extract.py extract <out.json> <dosyalar...>
          i18n_extract.py apply <ceviri.json> <dosyalar...>
Ceviri dosyası: {"turkce literal (tırnaksız ham kaynak)": "english literal"}"""
import json, re, sys

TR = set("çğıöşüÇĞİÖŞÜ")

def literals(src):
    """(start, end, raw) - tek satırlık "..." literalleri; ${...} içindeki iç içe stringleri atlar."""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith('//', i):
            j = src.find('\n', i); i = n if j < 0 else j; continue
        if src.startswith('/*', i):
            j = src.find('*/', i); i = n if j < 0 else j + 2; continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3); i = n if j < 0 else j + 3; continue
        if c == "'":
            # karakter literali
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == '\\' else 1
            i = j + 1; continue
        if c == '"':
            j = i + 1; depth = 0
            while j < n:
                ch = src[j]
                if ch == '\\': j += 2; continue
                if depth == 0 and ch == '"': break
                if ch == '\n' and depth == 0: break
                if src.startswith('${', j): depth += 1; j += 2; continue
                if depth > 0:
                    if ch == '{': depth += 1
                    elif ch == '}': depth -= 1
                    elif ch == '"':
                        # iç içe string: kapanışa kadar atla
                        k = j + 1
                        while k < n and src[k] != '"':
                            k += 2 if src[k] == '\\' else 1
                        j = k + 1; continue
                j += 1
            if j < n and src[j] == '"':
                out.append((i, j + 1, src[i + 1:j]))
            i = j + 1; continue
        i += 1
    return out

def visible(raw):
    text = re.sub(r'\$\{[^}]*\}|\$[a-zA-Z_][a-zA-Z0-9_.]*', '', raw)
    letters = re.findall(r'[A-Za-zçğıöşüÇĞİÖŞÜ]', text)
    if len(letters) < 2: return False
    if re.fullmatch(r'[a-z_][a-zA-Z0-9_.]*', raw): return False  # anahtar/kimlik
    if re.fullmatch(r'[A-Z0-9_]+', raw) and not (set(raw) & TR) and len(raw) <= 4: return False  # kısa kod
    if raw.startswith('#') or raw.startswith('/') or '://' in raw: return False
    return True

def context_ok(src, s, e):
    before = src[max(0, s - 40):s]
    after = src[e:e + 12]
    line_start = src.rfind('\n', 0, s) + 1
    line = src[line_start:src.find('\n', e) if src.find('\n', e) >= 0 else len(src)]
    if 'const val' in line or line.strip().startswith('@') or 'import ' in line: return False
    if re.search(r'(==|!=)\s*$', before) or re.match(r'\s*(==|!=)', after): return False
    if re.search(r'\bin\s*$', before) and not re.search(r'for\s*\(', line): return False
    if re.match(r'\s*in\b', after): return False
    if re.match(r'\s*->', after): return False          # when dalı
    if re.match(r'\s+to\b', after): return False         # map anahtarı
    if re.search(r'(getString|getBoolean|getInt|putString|putBoolean|putInt|getSharedPreferences|startsWith|endsWith|contains|removePrefix|removeSuffix|substringAfter|substringBefore|split|replace|Regex)\(\s*$', before): return False
    if re.search(r'(label|contentDescription)\s*=\s*$', before): return False
    if re.search(r'\bL\(\s*$', before) or re.search(r'L\("[^"]*",\s*$', before): return False
    return True

def candidates(path):
    src = open(path, encoding='utf-8').read()
    return src, [(s, e, raw) for s, e, raw in literals(src) if visible(raw) and context_ok(src, s, e)]

if __name__ == '__main__':
    mode = sys.argv[1]
    if mode == 'extract':
        out = {}
        for p in sys.argv[3:]:
            _, cs = candidates(p)
            for _, _, raw in cs: out.setdefault(raw, "")
        json.dump(out, open(sys.argv[2], 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
        print(len(out), 'metin')
    elif mode == 'apply':
        tr = json.load(open(sys.argv[2], encoding='utf-8'))
        total = 0
        for p in sys.argv[3:]:
            src, cs = candidates(p)
            for s, e, raw in reversed(cs):
                en = tr.get(raw)
                if not en: continue
                src = src[:s] + 'L("' + raw + '", "' + en + '")' + src[e:]
                total += 1
            open(p, 'w', encoding='utf-8').write(src)
        print(total, 'değiştirildi')

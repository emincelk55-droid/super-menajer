# Usage: python3 check.py L10.txt  -> validates format, prints per-team counts
import sys, re
DET = {"KL","SLB","SGB","STP","DOS","MO","OOS","SLK","SGK","SF"}
errs = 0; teams = []; cur = None
for n, line in enumerate(open(sys.argv[1], encoding="utf-8"), 1):
    line = line.rstrip("\n")
    if not line or line.startswith("#"): continue
    f = line.split("|")
    def err(m):
        global errs; errs += 1; print(f"line {n}: {m}: {line}")
    if f[0] == "LEAGUE":
        if len(f) != 4: err("LEAGUE needs 4 fields")
    elif f[0] == "TEAM":
        if len(f) != 11: err("TEAM needs 11 fields"); continue
        if len(f[2]) != 3: err("SHORT must be 3 letters")
        if not f[5].isdigit(): err("capacity int")
        if not re.fullmatch(r"#[0-9A-Fa-f]{6}", f[8]) or not re.fullmatch(r"#[0-9A-Fa-f]{6}", f[9]): err("colors #RRGGBB")
        if not (f[10].isdigit() and 40 <= int(f[10]) <= 95): err("rep 40-95")
        cur = [f[1], {"KL":0,"DF":0,"MF":0,"FW":0}, 0]; teams.append(cur)
    elif f[0] == "P":
        if cur is None: err("P before TEAM"); continue
        if len(f) not in (11, 12): err("P needs 11 or 12 fields"); continue
        if not f[1].isdigit(): err("shirt int")
        if not (f[3].isdigit() and 1975 <= int(f[3]) <= 2011): err("birth year")
        if not re.fullmatch(r"[A-Z]{2}", f[4]): err("nation ISO2")
        if f[5] not in DET: err("DET")
        if f[6] != "-" and f[6] not in DET: err("DET2")
        if f[7] not in ("R","L","B"): err("foot")
        if not (f[8].isdigit() and f[9].isdigit() and 35 <= int(f[8]) <= 95 and int(f[8]) <= int(f[9]) <= 96): err("ovr/pot")
        a = f[10].split(",")
        if len(a) != 6 or not all(x.isdigit() and 1 <= int(x) <= 99 for x in a): err("six attrs")
        g = "KL" if f[5]=="KL" else "DF" if f[5] in ("SLB","SGB","STP") else "MF" if f[5] in ("DOS","MO","OOS") else "FW"
        cur[1][g] += 1; cur[2] += 1
    else: err("unknown line type")
for name, c, tot in teams:
    bad = tot < 22 or tot > 30 or c["KL"] < 2 or c["DF"] < 6 or c["MF"] < 6 or c["FW"] < 3
    print(("!! " if bad else "   ") + f"{name}: {tot} players {c}")
    if bad: errs += 1
print(f"{len(teams)} teams, errors: {errs}")

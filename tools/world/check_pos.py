# Usage: python3 check_pos.py <id>   validates tools/world/pos/L<id>.pos against app/src/main/resources/world/L<id>.txt
import sys, os
root = os.path.dirname(os.path.abspath(__file__))
lid = sys.argv[1]
DET = {"KL","SLB","SGB","STP","DOS","MO","OOS","SLK","SGK","SF"}
players = []
for l in open(os.path.join(root, "../../app/src/main/resources/world/L%s.txt" % lid), encoding="utf-8"):
    f = l.rstrip("\n").split("|")
    if f[0] == "P": players.append((f[2], f[5]))
rows = [l.rstrip("\n") for l in open(os.path.join(root, "pos/L%s.pos" % lid), encoding="utf-8") if l.strip() and not l.startswith("#")]
errs = 0
if len(rows) != len(players):
    print(f"row count {len(rows)} != players {len(players)}"); errs += 1
multi = 0
for i, ((name, main), r) in enumerate(zip(players, rows)):
    f = r.split("|")
    if len(f) != 2 or f[0] != name: print(f"row {i+1}: name mismatch: expected '{name}' got '{r}'"); errs += 1; continue
    d = f[1].split(",")
    if any(x not in DET for x in d): print(f"row {i+1}: bad DET: {r}"); errs += 1
    if d[0] != main: print(f"row {i+1}: first DET must be main {main}: {r}"); errs += 1
    if len(d) != len(set(d)) or len(d) > 6: print(f"row {i+1}: duplicates/too many: {r}"); errs += 1
    if "KL" in d and len(d) > 1: print(f"row {i+1}: goalkeeper mixed: {r}"); errs += 1
    if len(d) > 1: multi += 1
print(f"{len(rows)} rows, {multi} with multiple positions, errors: {errs}")

# Usage: python3 merge_pos.py <id> [<id>...]  -> adds positions from pos/L<id>.pos as 12th field of each P line
import sys, os
root = os.path.dirname(os.path.abspath(__file__))
for lid in sys.argv[1:]:
    lf = os.path.join(root, "../../app/src/main/resources/world/L%s.txt" % lid)
    rows = [l.rstrip("\n").split("|") for l in open(os.path.join(root, "pos/L%s.pos" % lid), encoding="utf-8") if l.strip() and not l.startswith("#")]
    out, i = [], 0
    for l in open(lf, encoding="utf-8"):
        f = l.rstrip("\n").split("|")
        if f[0] == "P":
            assert rows[i][0] == f[2], (lid, i, rows[i][0], f[2])
            f = f[:11] + [rows[i][1]]
            i += 1
        out.append("|".join(f))
    assert i == len(rows)
    open(lf, "w", encoding="utf-8").write("\n".join(out) + "\n")
    print(f"L{lid}: {i} players merged")

"""Süper Menajer logosu: tek tanımdan Android ikon vektörü ve oyun içi çizim verisi üretir.

Kullanım: python3 -I tools/gen_logo.py
Çıktılar:
  app/src/main/res/drawable/ic_launcher_fg.xml       (uyarlanabilir ikon ön yüzü, 108x108)
  app/src/main/java/com/supermenajer/game/ui/LogoArt.kt  (GameCrest'in çizdiği katmanlar)
Tasarım 108 birimlik tuvalde, ikon güvenli alanı (merkezden 33 birim) içinde kalır.
"""
import math
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")

# Kalkan kutusu
X0, Y0, W, H = 30.0, 23.0, 48.0, 63.0


def X(f):
    return X0 + W * f


def Y(f):
    return Y0 + H * f


def r2(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def shield(inset=0.0):
    """Kalkan çevresi; inset kadar içeri çekilmiş (yaklaşık)."""
    x0, y0, w, h = X0 + inset, Y0 + inset * 1.15, W - 2 * inset, H - 2.3 * inset

    def x(f):
        return x0 + w * f

    def y(f):
        return y0 + h * f

    return (
        f"M{r2(x(.5))},{r2(y(0))} "
        f"Q{r2(x(.78))},{r2(y(.07))} {r2(x(1))},{r2(y(.1))} "
        f"L{r2(x(.97))},{r2(y(.55))} "
        f"Q{r2(x(.9))},{r2(y(.86))} {r2(x(.5))},{r2(y(1))} "
        f"Q{r2(x(.1))},{r2(y(.86))} {r2(x(.03))},{r2(y(.55))} "
        f"L{r2(x(0))},{r2(y(.1))} "
        f"Q{r2(x(.22))},{r2(y(.07))} {r2(x(.5))},{r2(y(0))} Z"
    )


def circle(cx, cy, r):
    return f"M{r2(cx - r)},{r2(cy)} a{r2(r)},{r2(r)} 0 1,0 {r2(2 * r)},0 a{r2(r)},{r2(r)} 0 1,0 {r2(-2 * r)},0 Z"


def star(cx, cy, ro, ri, rot=-90):
    pts = []
    for i in range(10):
        a = math.radians(rot + i * 36)
        r = ro if i % 2 == 0 else ri
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return "M" + " L".join(f"{r2(px)},{r2(py)}" for px, py in pts) + " Z"


def poly(cx, cy, r, n=5, rot=-90):
    pts = [(cx + r * math.cos(math.radians(rot + i * 360 / n)), cy + r * math.sin(math.radians(rot + i * 360 / n))) for i in range(n)]
    return "M" + " L".join(f"{r2(px)},{r2(py)}" for px, py in pts) + " Z"


def leaf(cx, cy, length, width, angle):
    """Defne yaprağı: iki yaydan mercek biçimi."""
    a = math.radians(angle)
    dx, dy = math.cos(a) * length / 2, math.sin(a) * length / 2
    nx, ny = -math.sin(a) * width, math.cos(a) * width
    sx, sy = cx - dx, cy - dy
    ex, ey = cx + dx, cy + dy
    return (f"M{r2(sx)},{r2(sy)} Q{r2(cx + nx)},{r2(cy + ny)} {r2(ex)},{r2(ey)} "
            f"Q{r2(cx - nx)},{r2(cy - ny)} {r2(sx)},{r2(sy)} Z")


BCX, BCY, BR = 54.0, 57.5, 9.8  # top

layers = []  # (pathData, fill) — fill: renk "#AARRGGBB" ya da ("lin", x1,y1,x2,y2, [(ofs, renk)])

GOLD = ("lin", X(0), Y(0), X(1), Y(1), [(0, "#FFFFF1B0"), (0.45, "#FFE2B33C"), (1, "#FF9C6E12")])
# 1) Altın dış kalkan
layers.append((shield(0), GOLD))
# 2) İç kalkan: zümrütten laciverde
layers.append((shield(2.6), ("lin", 54, Y(0.05), 54, Y(1), [(0, "#FF19D9A2"), (0.5, "#FF0B7F6A"), (1, "#FF0B1A3A")])))
# 3) Saha çizgileri (ince, yarı saydam): orta yuvarlak ve orta çizgi
layers.append(("STROKE:" + circle(BCX, BCY, 16.5), "#40FFFFFF"))
# 4) Üst altın şerit ve yıldızlar
band = f"M{r2(X(.1))},{r2(Y(.17))} L{r2(X(.9))},{r2(Y(.17))} L{r2(X(.88))},{r2(Y(.27))} L{r2(X(.12))},{r2(Y(.27))} Z"
layers.append((band, ("lin", X(.1), Y(.17), X(.9), Y(.27), [(0, "#FFB8891A"), (0.5, "#FFFFE58A"), (1, "#FFB8891A")])))
for i, sx in enumerate((X(.32), X(.5), X(.68))):
    big = i == 1
    layers.append((star(sx, Y(.22), 3.3 if big else 2.5, 1.35 if big else 1.0), "#FF0B1A3A"))
# 5) Defne dalı: topun iki yanında altın yapraklar
leaves = []
for side in (-1, 1):
    for k in range(7):
        t = math.radians(118 + k * 20) if side < 0 else math.radians(62 - k * 20)
        r = 14.0
        cx, cy = BCX + r * math.cos(t), BCY + r * math.sin(t) - 1
        ang = math.degrees(t) + (90 if side < 0 else -90) + side * 25
        leaves.append(leaf(cx, cy, 7.0, 1.9, ang))
layers.append((" ".join(leaves), ("lin", 54, BCY - 14, 54, BCY + 14, [(0, "#FFFFF1B0"), (1, "#FFC79A22")])))
# 6) Top: beyaz daire, ortada lacivert beşgen, kenarlarda kesik beşgenler (kırpılmış), çizgiler
layers.append((circle(BCX, BCY, BR), "#FFFFFFFF"))
layers.append((poly(BCX, BCY, BR * 0.3), "#FF0B1A3A"))


def clipped_poly(cx, cy, r, rot):
    """Beşgenin top dairesi içinde kalan kısmı: kenarlar sık örneklenir, dışarıdaki noktalar çembere çekilir
    (vektör kırpma her cihazda güvenilir olmadığı için kesim önceden hesaplanır)."""
    pts = [(cx + r * math.cos(math.radians(rot + i * 72)), cy + r * math.sin(math.radians(rot + i * 72))) for i in range(5)]
    out = []
    for i in range(5):
        (ax, ay), (bx, by) = pts[i], pts[(i + 1) % 5]
        for k in range(12):
            t = k / 12
            px, py = ax + (bx - ax) * t, ay + (by - ay) * t
            d = math.hypot(px - BCX, py - BCY)
            lim = BR - 0.35
            if d > lim:
                px, py = BCX + (px - BCX) * lim / d, BCY + (py - BCY) * lim / d
            out.append((px, py))
    return "M" + " L".join(f"{r2(x)},{r2(y)}" for x, y in out) + " Z"


edge = []
for i in range(5):
    a = math.radians(-90 + 36 + i * 72)
    edge.append(clipped_poly(BCX + BR * 1.0 * math.cos(a), BCY + BR * 1.0 * math.sin(a), BR * 0.25, math.degrees(a) + 180))
layers.append((" ".join(edge), "#FF0B1A3A"))
lines = []
for i in range(5):
    a = math.radians(-90 + i * 72)
    x1, y1 = BCX + BR * 0.3 * math.cos(a), BCY + BR * 0.3 * math.sin(a)
    x2, y2 = BCX + BR * 0.74 * math.cos(a), BCY + BR * 0.74 * math.sin(a)
    lines.append(f"M{r2(x1)},{r2(y1)} L{r2(x2)},{r2(y2)}")
layers.append(("STROKE:" + " ".join(lines), "#FF0B1A3A"))
layers.append(("STROKE:" + circle(BCX, BCY, BR), "#FF0B1A3A"))
# 7) Parlama: kalkanın sol üstünde yarı saydam ışık
shine = (f"M{r2(X(.06))},{r2(Y(.12))} Q{r2(X(.3))},{r2(Y(.06))} {r2(X(.5))},{r2(Y(.03))} "
         f"L{r2(X(.5))},{r2(Y(.12))} Q{r2(X(.26))},{r2(Y(.2))} {r2(X(.1))},{r2(Y(.55))} Z")
layers.append((shine, "#26FFFFFF"))


def xml():
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           "<!-- tools/gen_logo.py ile üretildi: altın çerçeveli kalkan, yıldızlı şerit, defne dalı ve top. -->",
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="108dp" android:height="108dp"',
           '    android:viewportWidth="108" android:viewportHeight="108">']
    indent = "    "
    for d, fill in layers:
        if d.startswith("CLIP:"):
            out.append(f'{indent}<group>')
            indent += "    "
            out.append(f'{indent}<clip-path android:pathData="{d[5:]}" />')
            continue
        if d == "ENDCLIP":
            indent = indent[:-4]
            out.append(f"{indent}</group>")
            continue
        if d.startswith("STROKE:"):
            out.append(f'{indent}<path android:strokeColor="{fill}" android:strokeWidth="0.9" android:strokeLineCap="round" android:pathData="{d[7:]}" />')
            continue
        if isinstance(fill, str):
            out.append(f'{indent}<path android:fillColor="{fill}" android:pathData="{d}" />')
        else:
            _, x1, y1, x2, y2, stops = fill
            out.append(f'{indent}<path android:pathData="{d}">')
            out.append(f'{indent}    <aapt:attr name="android:fillColor">')
            out.append(f'{indent}        <gradient android:type="linear" android:startX="{r2(x1)}" android:startY="{r2(y1)}" android:endX="{r2(x2)}" android:endY="{r2(y2)}">')
            for o, c in stops:
                out.append(f'{indent}            <item android:offset="{o}" android:color="{c}" />')
            out.append(f"{indent}        </gradient>")
            out.append(f"{indent}    </aapt:attr>")
            out.append(f"{indent}</path>")
    out.append("</vector>")
    return "\n".join(out) + "\n"


def kotlin():
    def col(c):
        return "0x" + c[1:]

    rows = []
    for d, fill in layers:
        if d.startswith("CLIP:"):
            rows.append(f'        LogoLayer.Clip("{d[5:]}"),')
        elif d == "ENDCLIP":
            rows.append("        LogoLayer.EndClip,")
        elif d.startswith("STROKE:"):
            rows.append(f'        LogoLayer.Stroke("{d[7:]}", {col(fill)}),')
        elif isinstance(fill, str):
            rows.append(f'        LogoLayer.Fill("{d}", {col(fill)}),')
        else:
            _, x1, y1, x2, y2, stops = fill
            st = ", ".join(f"{o}f to {col(c)}" for o, c in stops)
            rows.append(f'        LogoLayer.Gradient("{d}", {r2(x1)}f, {r2(y1)}f, {r2(x2)}f, {r2(y2)}f, listOf({st})),')
    return f"""package com.supermenajer.game.ui

// tools/gen_logo.py ile üretildi; elle düzenleme. Uygulama ikonuyla birebir aynı logo (108 birimlik tuval).

sealed class LogoLayer {{
    class Fill(val d: String, val color: Long) : LogoLayer()
    class Gradient(val d: String, val x1: Float, val y1: Float, val x2: Float, val y2: Float, val stops: List<Pair<Float, Long>>) : LogoLayer()
    class Stroke(val d: String, val color: Long) : LogoLayer()
    class Clip(val d: String) : LogoLayer()
    object EndClip : LogoLayer()
}}

object LogoArt {{
    /** Logonun kapladığı kutu (108'lik tuvalde): x, y, genişlik, yükseklik. */
    val bounds = floatArrayOf({r2(X0)}f, {r2(Y0)}f, {r2(W)}f, {r2(H)}f)

    val layers: List<LogoLayer> = listOf(
{chr(10).join(rows)}
    )
}}
"""


open(os.path.join(ROOT, "app/src/main/res/drawable/ic_launcher_fg.xml"), "w").write(xml())
open(os.path.join(ROOT, "app/src/main/java/com/supermenajer/game/ui/LogoArt.kt"), "w").write(kotlin())
print("ok", len(layers), "katman")

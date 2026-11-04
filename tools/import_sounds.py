"""Gerçek kayıtlardan oyun seslerini keser (kaynaklar ve lisanslar: docs/SES_KAYNAKLARI.md).

Kullanım: python3 -I tools/import_sounds.py <indirilen_dosyalar_klasörü> app/src/main/res/raw
Klasörde beklenen dosyalar (Wikimedia Commons'tan, mono 22050 Hz WAV'a çevrilmiş):
  fra_usa.wav   — Match amical France-USA, 58241 spectateurs (CC0)
  aut_swe.wav   — WWS Football Austria vs. Sweden (CC BY 4.0)
  whistle_m.wav — SpliceSound referee whistle (CC0)
  carefree.mp3  — Kevin MacLeod, Carefree (CC BY 3.0)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 22050


def load(path):
    with wave.open(path) as w:
        assert w.getframerate() == SR and w.getnchannels() == 1
        return np.frombuffer(w.readframes(w.getnframes()), np.int16) / 32768.0


def cut(x, a, b):
    return x[int(a * SR):int(b * SR)].copy()


def fade(x, fin=0.05, fout=0.3):
    i, o = int(fin * SR), int(fout * SR)
    if i:
        x[:i] *= np.linspace(0, 1, i)
    if o:
        x[-o:] *= np.linspace(1, 0, o) ** 1.5
    return x


def norm(x, peak):
    return x / (np.max(np.abs(x)) or 1) * peak


def loop(x, cross=1.5):
    """Sonu başına çapraz geçişle eklenir: döngüde dikiş duyulmaz."""
    c = int(cross * SR)
    head = x[:c] * np.linspace(0, 1, c) + x[-c:] * np.linspace(1, 0, c)
    return np.concatenate([head, x[c:-c]])


def save(path, x, q=5):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as t:
        tmp = t.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", str(q), path], check=True)
    os.remove(tmp)
    print(os.path.basename(path), f"{len(x) / SR:.1f} sn")


def main():
    src, out = sys.argv[1], sys.argv[2]
    fra = load(os.path.join(src, "fra_usa.wav"))
    aut = load(os.path.join(src, "aut_swe.wav"))
    wh = load(os.path.join(src, "whistle_m.wav"))

    # Tribün: 58 bin kişilik statta maç sırası uğultu (SoundPool sınırı ~1 MB: 22 sn mono)
    save(os.path.join(out, "sfx_crowd_loop.ogg"), norm(loop(cut(fra, 2.0, 25.5)), 0.5))
    # Gol: Avusturya-İsveç maçında tribünün 15 dB birden patladığı an
    save(os.path.join(out, "sfx_goal.ogg"), norm(fade(cut(aut, 58.2, 67.2), 0.08, 2.0), 0.95))
    # Kaçan pozisyonda tribün tepkisi
    save(os.path.join(out, "sfx_ooh.ogg"), norm(fade(cut(fra, 30.4, 36.0), 0.15, 1.6), 0.8))

    # Düdükler: tek gerçek üfleme (0.05-0.6 sn) ve salon yankısı
    blow = fade(cut(wh, 0.05, 0.62), 0.005, 0.06)
    tail = fade(cut(wh, 0.05, 1.4), 0.005, 0.5)

    def seq(*parts):
        return norm(np.concatenate(parts), 0.75)

    gap = lambda s: np.zeros(int(s * SR))
    save(os.path.join(out, "sfx_whistle_kickoff.ogg"), seq(tail))
    save(os.path.join(out, "sfx_whistle_half.ogg"), seq(blow, gap(0.12), tail))
    save(os.path.join(out, "sfx_whistle_full.ogg"), seq(blow, gap(0.1), blow, gap(0.1), tail))

    # Menü müziği: ilk 64 saniye, yumuşak giriş/çıkış (döngüde baştan başlar)
    tmp = os.path.join(tempfile.gettempdir(), "carefree_mono.wav")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", os.path.join(src, "carefree.mp3"), "-ac", "1", "-ar", str(SR), tmp], check=True)
    music = fade(cut(load(tmp), 0.0, 64.0), 0.5, 3.0)
    save(os.path.join(out, "music_menu.ogg"), norm(music, 0.7), q=4)


if __name__ == "__main__":
    main()

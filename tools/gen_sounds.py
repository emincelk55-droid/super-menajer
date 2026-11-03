"""Oyunun ses efektlerini ve menü müziğini sıfırdan sentezler (lisans gerektirmez).

Kullanım: python3 tools/gen_sounds.py app/src/main/res/raw
Çıktılar OGG Vorbis (ffmpeg + libvorbis gerekir).
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 22050
rng = np.random.default_rng(7)


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def bandpass(x, lo, hi):
    """FFT ile bant geçiren süzgeç (yumuşak kenarlı)."""
    n = len(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    X = np.fft.rfft(x)
    gain = 1 / (1 + (lo / np.maximum(f, 1)) ** 4) / (1 + (f / hi) ** 4)
    return np.fft.irfft(X * gain, n)


def env(n, attack, release, sustain=None):
    a = int(attack * SR)
    r = int(release * SR)
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a) if a else e[:a]
    if r:
        e[-r:] *= np.linspace(1, 0, r) ** 2
    return e


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(path, x, quality=4):
    x = np.clip(x, -1, 1)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", str(quality), path], check=True)
    os.remove(wav)


# ---------- Arayüz ----------

def click():
    t = t_axis(0.035)
    x = np.sin(2 * np.pi * 1700 * t) * np.exp(-t * 160) + 0.15 * rng.standard_normal(len(t)) * np.exp(-t * 400)
    return norm(x, 0.5)


def notify():
    out = []
    for f, d in [(880, 0.11), (1320, 0.22)]:
        t = t_axis(d)
        tone = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * 2 * f * t)
        out.append(tone * np.exp(-t * 9) * env(len(t), 0.005, 0.03))
    return norm(np.concatenate(out), 0.55)


# ---------- Maç ----------

def whistle(sec):
    """Hakem düdüğü: ~2.9 kHz, içindeki bilyenin titreşimi (genlik + frekans)."""
    t = t_axis(sec)
    trill = 1 + 0.35 * np.sin(2 * np.pi * 32 * t)
    f = 2900 + 60 * np.sin(2 * np.pi * 32 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = (np.sin(ph) + 0.25 * np.sin(2 * ph) + 0.08 * np.sin(3 * ph)) * trill
    x += 0.08 * bandpass(rng.standard_normal(len(t)), 2000, 5000)
    return x * env(len(t), 0.015, 0.06)


def gap(sec):
    return np.zeros(int(sec * SR))


def whistle_kickoff():
    return norm(whistle(0.75), 0.7)


def whistle_half():
    return norm(np.concatenate([whistle(0.35), gap(0.12), whistle(0.9)]), 0.7)


def whistle_full():
    return norm(np.concatenate([whistle(0.3), gap(0.1), whistle(0.3), gap(0.1), whistle(1.3)]), 0.7)


def crowd_noise(sec, bright=1.0):
    """Kalabalık uğultusu: kahverengi gürültü + konuşma formantlarında rastgele mırıltılar."""
    n = int(sec * SR)
    base = np.cumsum(rng.standard_normal(n))
    base -= np.convolve(base, np.ones(400) / 400, mode="same")
    base = bandpass(base, 150, 1400 * bright)
    murmur = np.zeros(n)
    for _ in range(int(sec * 18)):
        s = rng.integers(0, n - SR // 3)
        d = rng.integers(SR // 12, SR // 3)
        burst = bandpass(rng.standard_normal(d), 300 + rng.random() * 300, 900 + rng.random() * 1200 * bright)
        murmur[s:s + d] += burst * np.hanning(d) * (0.4 + rng.random())
    return norm(base, 1) * 0.7 + norm(murmur, 1) * 0.5


def crowd_loop():
    sec = 10.0
    x = crowd_noise(sec + 1.0)
    t = t_axis(sec + 1.0)
    x *= 0.8 + 0.2 * np.sin(2 * np.pi * t / 5.5)
    # Ses sorunsuz dönsün: sondaki 1 saniye başa çapraz geçişle eklenir
    n = int(sec * SR)
    fade = int(1.0 * SR)
    head = x[:fade] * np.linspace(0, 1, fade) + x[n:n + fade] * np.linspace(1, 0, fade)
    y = np.concatenate([head, x[fade:n]])
    return norm(np.tanh(2 * norm(y, 1)), 0.45)


def goal_roar():
    sec = 4.5
    t = t_axis(sec)
    x = crowd_noise(sec, bright=2.2)
    shape = np.minimum(t / 0.25, 1) * np.exp(-np.maximum(t - 1.6, 0) * 1.1)
    # Tezahürat dalgaları
    shape *= 1 + 0.15 * np.sin(2 * np.pi * 2.2 * t)
    # Yumuşak sıkıştırma: tepeler bastırılır, kükreme dolgunlaşır
    y = np.tanh(3 * norm(x * shape, 1)) / np.tanh(3)
    return norm(y, 0.9)


def ooh():
    """Kaçan pozisyonda tribünden "ooo": kalabalık sesi /o/ formantına süzülür, yükselip düşer."""
    sec = 1.6
    t = t_axis(sec)
    x = crowd_noise(sec)
    voice = bandpass(rng.standard_normal(len(t)), 380, 800) * 1.4
    shape = np.sin(np.pi * np.minimum(t / sec, 1)) ** 1.5
    return norm((x * 0.6 + voice) * shape, 0.75)


def kick():
    t = t_axis(0.12)
    f = 140 * np.exp(-t * 18) + 60
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(ph) * np.exp(-t * 30) + 0.4 * bandpass(rng.standard_normal(len(t)), 800, 4000) * np.exp(-t * 120)
    return norm(x, 0.7)


# ---------- Menü müziği ----------

def music():
    """Sakin, motive edici menü döngüsü: 4 akorluk pad, arpej, bas ve hafif davul (108 BPM, 32 ölçü vuruş)."""
    bpm = 108
    beat = 60 / bpm
    bars = 8
    total = bars * 4 * beat
    n = int(total * SR)
    out = np.zeros(n)
    # Am - F - C - G (her akor 2 ölçü)
    chords = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]

    def hz(m):
        return 440 * 2 ** ((m - 69) / 12)

    def add(sig, start):
        s = int(start * SR)
        e = min(n, s + len(sig))
        out[s:e] += sig[: e - s]

    for ci, ch in enumerate(chords):
        start = ci * 8 * beat
        dur = 8 * beat
        t = t_axis(dur)
        pad = sum(np.sin(2 * np.pi * hz(m) * t + np.sin(2 * np.pi * 0.3 * t)) + 0.4 * np.sin(2 * np.pi * hz(m + 12) * t * 1.002) for m in ch)
        add(pad * env(len(t), 0.6, 0.8) * 0.10, start)
        # Bas: her vuruşta kök nota
        for b in range(8):
            tb = t_axis(beat * 0.9)
            bass = np.sin(2 * np.pi * hz(ch[0] - 12) * tb) * np.exp(-tb * 3)
            add(bass * 0.35, start + b * beat)
        # Arpej: sekizlik notalar
        for k in range(16):
            m = ch[[0, 1, 2, 1][k % 4]] + 12 + (12 if k % 8 == 7 else 0)
            ta = t_axis(beat / 2)
            tone = np.sin(2 * np.pi * hz(m) * ta) + 0.3 * np.sin(2 * np.pi * hz(m) * 2 * ta)
            add(tone * np.exp(-ta * 7) * 0.12, start + k * beat / 2)
    # Davul: 1 ve 3'te tok vuruş, 2 ve 4'te hafif el çırpma
    for b in range(bars * 4):
        if b % 2 == 0:
            add(kick() * 0.45, b * beat)
        else:
            tc = t_axis(0.12)
            clap = bandpass(rng.standard_normal(len(tc)), 900, 5000) * np.exp(-tc * 35)
            add(clap * 0.12, b * beat)
    # Döngü başı ve sonu yumuşak birleşsin
    f = int(0.05 * SR)
    out[:f] *= np.linspace(0, 1, f)
    out[-f:] *= np.linspace(1, 0, f)
    return norm(out, 0.6)


def main():
    outdir = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res/raw"
    os.makedirs(outdir, exist_ok=True)
    sounds = {
        "sfx_click": click(), "sfx_notify": notify(), "sfx_whistle_kickoff": whistle_kickoff(),
        "sfx_whistle_half": whistle_half(), "sfx_whistle_full": whistle_full(), "sfx_crowd_loop": crowd_loop(),
        "sfx_goal": goal_roar(), "sfx_ooh": ooh(), "sfx_kick": kick(),
    }
    for name, x in sounds.items():
        save(os.path.join(outdir, name + ".ogg"), x)
    save(os.path.join(outdir, "music_menu.ogg"), music(), quality=3)


if __name__ == "__main__":
    main()

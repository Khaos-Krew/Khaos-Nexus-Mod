"""Procedural audio for 1.2 (original, generated here; released CC0 with the mod - see docs/AUDIO_LICENSE.md).
Music: hollow_king_theme (D minor dirge: drone, choir pad, timpani, bell motif; seamless loop), underthrone_camp (ambient).
SFX: king_roar, king_phase, crownfall_rise, crownfall_impact, shard_strike, king_death.
Writes mono Ogg Vorbis via ffmpeg. Run: python3 gen_sounds.py"""
import numpy as np, subprocess, os, wave
SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/khaosnexus/sounds')
rng = np.random.default_rng(1207)

def t_(d): return np.arange(int(d * SR)) / SR
def hz(n): return 440.0 * 2 ** ((n - 69) / 12)          # midi -> Hz
def env(n, a, r, s=1.0):
    e = np.ones(n) * s; A = min(n, int(a * SR)); R = min(n - A, int(r * SR))
    if A: e[:A] = np.linspace(0, s, A)
    if R: e[n - R:] = np.linspace(s, 0, R) * (e[n - R] if n - R < n else 1)
    return e
def lowpass(x, cut):
    a = np.exp(-2 * np.pi * cut / SR); y = np.empty_like(x); z = 0.0
    for i in range(len(x)): z = (1 - a) * x[i] + a * z; y[i] = z
    return y
def lp_fast(x, cut, passes=2):   # vectorised one-pole via exponential smoothing in blocks (good enough for pads)
    from numpy.lib.stride_tricks import sliding_window_view
    k = max(1, int(SR / cut / 2)); ker = np.ones(k) / k
    for _ in range(passes): x = np.convolve(x, ker, mode='same')
    return x
def saw(f, d, det=0.0):
    t = t_(d); return 2 * ((t * f * (1 + det)) % 1) - 1
def reverb(x, dec=2.2, mix=0.35):
    ir_n = int(dec * SR); ir = rng.standard_normal(ir_n) * np.exp(-np.linspace(0, 7, ir_n)); ir = lp_fast(ir, 3000, 1); ir /= np.abs(ir).sum() / 6
    wet = np.fft.irfft(np.fft.rfft(x, len(x) + ir_n) * np.fft.rfft(ir, len(x) + ir_n))[:len(x)]
    return (1 - mix) * x + mix * wet / (np.abs(wet).max() + 1e-9) * np.abs(x).max()
def norm(x, peak=0.89): return x / (np.abs(x).max() + 1e-9) * peak

def choir(notes, d, vib=5.2):
    t = t_(d); out = np.zeros_like(t)
    for n in notes:
        f = hz(n)
        for v, det in enumerate((-0.006, 0.0, 0.0055)):
            ph = 2 * np.pi * f * (1 + det) * t + 0.004 * f / vib * np.sin(2 * np.pi * vib * t + v)
            voice = sum(np.sin(ph * h) * a for h, a in ((1, 1), (2, 0.42), (3, 0.24), (4, 0.11), (5, 0.07)))   # "ah" formant-ish
            out += voice
    return out * env(len(t), d * 0.3, d * 0.35) / (len(notes) * 3)

def bell(n, d=3.0, amp=1.0):
    t = t_(d); f = hz(n)
    m = np.sin(2 * np.pi * f * 1.4 * t) * 2.2 * np.exp(-t * 2.5)
    return amp * np.sin(2 * np.pi * f * t + m) * np.exp(-t * 1.6)

def timpani(n, d=1.6, amp=1.0):
    t = t_(d); f = hz(n) * (1 + 0.08 * np.exp(-t * 30))
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3.2)
    hit = lp_fast(rng.standard_normal(len(t)), 900) * np.exp(-t * 28) * 3
    return amp * (body + hit)

def place(buf, x, at):
    i = int(at * SR); j = min(len(buf), i + len(x)); buf[i:j] += x[:j - i]

def theme():
    bpm = 72; beat = 60 / bpm; bars = 24; L = bars * 4 * beat
    mix = np.zeros(int(L * SR))
    prog = [(50, 53, 57), (46, 50, 53), (43, 46, 50), (45, 49, 52)]   # Dm  Bb  Gm  A
    for b in range(bars):
        ch = prog[(b // 2) % 4]; at = b * 4 * beat
        place(mix, choir([c + 12 for c in ch], 4 * beat + 1.0) * 0.55, at)
        place(mix, lp_fast(saw(hz(ch[0] - 24), 4 * beat + 0.5) + saw(hz(ch[0] - 24), 4 * beat + 0.5, 0.004), 380) * env(int((4 * beat + 0.5) * SR), 0.4, 0.6) * 0.32, at)
        for k in range(4):   # dirge pulse: heavy on 1, lighter on 3, rolls before the turn
            if k in (0, 2): place(mix, timpani(ch[0] - 12, amp=0.9 if k == 0 else 0.55), at + k * beat)
        if b % 8 == 7:
            for r in range(6): place(mix, timpani(ch[0] - 12, 0.6, 0.25 + r * 0.08), at + 3 * beat + r * beat / 6)
    motif = [(62, 0, 2), (65, 2, 1), (64, 3, 1), (62, 4, 2), (60, 6, 1), (57, 7, 1), (58, 8, 3), (57, 11, 1), (55, 12, 2), (57, 14, 2)]
    for rep in (4, 8, 16, 20):
        for n, s, d in motif: place(mix, bell(n + 12, 2.8, 0.42), (rep * 4 + s) * beat)
    for rep in (12,):   # middle section: the motif low in the choir
        for n, s, d in motif: place(mix, choir([n], d * beat + 0.6) * 0.7, (rep * 4 + s) * beat)
    mix = reverb(mix, 2.8, 0.38)
    xf = int(1.5 * SR); mix[:xf] = mix[:xf] * np.linspace(0, 1, xf) + mix[-xf:] * np.linspace(1, 0, xf)   # seamless loop seam
    return norm(mix[:-xf], 0.8)

def camp():
    L = 64.0; mix = np.zeros(int(L * SR))
    for k, ch in enumerate([(50, 57, 62), (48, 55, 60), (46, 53, 58), (45, 52, 57)]):
        place(mix, choir(list(ch), 17.0) * 0.35, k * 16.0)
    wind = lp_fast(rng.standard_normal(len(mix)), 220) * (0.5 + 0.5 * np.sin(2 * np.pi * t_(L) / 13.0)); mix += wind * 0.08
    for at, n in ((4, 74), (11, 69), (19, 72), (27, 67), (36, 74), (44, 70), (52, 69), (59, 65)): place(mix, bell(n, 4.0, 0.22), at)
    mix = reverb(mix, 3.5, 0.45); xf = int(2 * SR)
    mix[:xf] = mix[:xf] * np.linspace(0, 1, xf) + mix[-xf:] * np.linspace(1, 0, xf)
    return norm(mix[:-xf], 0.6)

def roar():
    d = 2.2; t = t_(d); f = 70 + 45 * np.exp(-((t - 0.5) ** 2) / 0.2)
    ph = 2 * np.pi * np.cumsum(f) / SR; g = (np.sin(ph) + 0.6 * np.sin(2 * ph + 0.3) + 0.4 * np.sign(np.sin(3 * ph)))
    rough = 1 + 0.5 * np.sin(2 * np.pi * 31 * t); noise = lp_fast(rng.standard_normal(len(t)), 1200) * 0.8
    x = (g * rough + noise) * env(len(t), 0.18, 1.2)
    return norm(reverb(x, 1.6, 0.3))

def phase():
    d = 4.0; x = choir([50, 57, 62, 65], d) * 1.2
    x += sum(bell(n, d, 0.6) for n in (74, 81)) ; place(x, timpani(38, 2.0, 1.4), 0.0)
    rise = lp_fast(rng.standard_normal(len(x)), 2500) * np.linspace(0, 1, len(x)) ** 2 * 0.3; x = x + rise * env(len(x), 0.1, 0.4)
    return norm(reverb(x, 2.5, 0.4))

def crownfall_rise():
    d = 1.0; t = t_(d); f = 300 * 2 ** (t * 2.2)
    shimmer = sum(np.sin(2 * np.pi * np.cumsum(f * k) / SR) / k for k in (1, 1.5, 2.01, 3.02))
    return norm(shimmer * (t / d) ** 1.5 * env(len(t), 0.0, 0.08) + lp_fast(rng.standard_normal(len(t)), 4000) * (t / d) ** 3 * 0.4, 0.7)

def impact():
    d = 2.4; t = t_(d)
    boom = np.sin(2 * np.pi * np.cumsum(45 + 80 * np.exp(-t * 14)) / SR) * np.exp(-t * 2.2) * 1.4
    crack = lp_fast(rng.standard_normal(len(t)), 5000, 1) * np.exp(-t * 18)
    glass = sum(np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t * rng.uniform(5, 11)) for f in rng.uniform(2200, 6400, 14)) * 0.12
    return norm(reverb(boom + crack + glass, 1.8, 0.3))

def shard():
    d = 1.0; t = t_(d)
    glass = sum(np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t * rng.uniform(7, 15)) for f in rng.uniform(1800, 7000, 12)) * 0.2
    thud = np.sin(2 * np.pi * np.cumsum(90 + 120 * np.exp(-t * 25)) / SR) * np.exp(-t * 9)
    return norm(glass + thud + lp_fast(rng.standard_normal(len(t)), 6000, 1) * np.exp(-t * 35) * 0.6, 0.8)

def death():
    d = 6.0; t = t_(d); x = np.zeros_like(t)
    for n in (62, 57, 53, 50):
        c = choir([n, n - 7], d) ; x += c * np.interp(t, [0, d], [1.0, 0.4])
    f = 60 * 2 ** (-t / 3); x += np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(t), 0.5, 2.5) * 0.9
    x += lp_fast(rng.standard_normal(len(t)), 300) * env(len(t), 1.0, 3.0) * 0.6
    place(x, bell(74, 5.0, 0.8), 0.2); place(x, bell(62, 5.0, 0.6), 2.5)
    return norm(reverb(x, 3.0, 0.45))

def write(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2'); tmp = path + '.wav'
    with wave.open(tmp, 'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '5', path], check=True); os.remove(tmp)
    print(path, f'{len(x) / SR:.1f}s')

if __name__ == '__main__':
    write(f'{OUT}/king/king_roar.ogg', roar()); write(f'{OUT}/king/king_phase.ogg', phase())
    write(f'{OUT}/king/crownfall_rise.ogg', crownfall_rise()); write(f'{OUT}/king/crownfall_impact.ogg', impact())
    write(f'{OUT}/king/shard_strike.ogg', shard()); write(f'{OUT}/king/king_death.ogg', death())
    write(f'{OUT}/music/hollow_king_theme.ogg', theme()); write(f'{OUT}/music/underthrone_camp.ogg', camp())

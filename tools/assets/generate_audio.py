"""Synthesizes every original NULLSTAR sound effect.

Run from the repository root:  python -I tools/assets/generate_audio.py
Requires numpy and soundfile (libsndfile with Vorbis). All sounds are generated from
oscillators, filtered noise and synthetic reverb with fixed seeds; no recordings or
third-party samples are used. Output: mono 44.1 kHz Ogg Vorbis, mastered to -3 dBFS peak before encoding.
"""
import math
import os

import numpy as np
import soundfile as sf

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nullstar", "sounds")
SR = 44100
RNG = np.random.default_rng(0x534F554E)


# ------------------------------------------------------------------ primitives
def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def noise(dur):
    return RNG.standard_normal(int(dur * SR))


def band(x, lo, hi, soft=.15):
    """FFT band-pass with smooth raised-cosine shoulders."""
    n = len(x)
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    lo_w, hi_w = max(1.0, lo * soft), max(1.0, hi * soft)
    rise = np.clip((f - (lo - lo_w)) / (2 * lo_w), 0, 1) if lo > 0 else np.ones_like(f)
    fall = np.clip(((hi + hi_w) - f) / (2 * hi_w), 0, 1)
    mask = (.5 - .5 * np.cos(rise * math.pi)) * (.5 - .5 * np.cos(fall * math.pi))
    return np.fft.irfft(spec * mask, n)


def sweep(f0, f1, dur, curve=1.0, shape="sin"):
    t = t_axis(dur)
    k = (t / dur) ** curve
    freq = f0 * (f1 / f0) ** k if f0 > 0 and f1 > 0 else f0 + (f1 - f0) * k
    phase = 2 * math.pi * np.cumsum(freq) / SR
    if shape == "saw":
        return 2 * ((phase / (2 * math.pi)) % 1) - 1
    return np.sin(phase)


def tone(freq, dur, partials=((1, 1),), detune=0.0):
    t = t_axis(dur)
    out = np.zeros_like(t)
    for ratio, amp in partials:
        out += amp * np.sin(2 * math.pi * freq * ratio * (1 + detune * RNG.uniform(-1, 1)) * t + RNG.uniform(0, 6.28))
    return out


def env(dur, attack, release, hold_curve=1.0):
    t = t_axis(dur)
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    r = np.clip((dur - t) / max(release, 1e-4), 0, 1)
    return (a * r) ** hold_curve


def decay(dur, tau, attack=.002):
    t = t_axis(dur)
    return np.clip(t / attack, 0, 1) * np.exp(-t / tau)


def place(buf, clip, at):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    if i < len(buf):
        buf[i:j] += clip[:j - i]
    return buf


def metal_hit(dur, base, tau, brightness=1.0):
    """Inharmonic struck-metal partials with a noise contact transient."""
    ratios = [1, 1.59, 2.14, 2.76, 3.47, 4.18, 5.43]
    out = np.zeros(int(dur * SR))
    for i, r in enumerate(ratios):
        out += tone(base * r, dur) * decay(dur, tau / (1 + i * .45)) * (brightness ** i) / (1 + i * .6)
    contact = band(noise(dur), 300, 6000) * decay(dur, .012)
    thud = band(noise(dur), 30, 220) * decay(dur, .09) * 2.2
    return out * .5 + contact * .6 + thud


def crackle(dur, density, lo=1500, hi=9000, tau=.004):
    out = np.zeros(int(dur * SR))
    count = int(density * dur)
    for _ in range(count):
        at = RNG.uniform(0, dur - .02)
        click = band(noise(.03), lo, hi) * decay(.03, tau) * RNG.uniform(.2, 1)
        place(out, click, at)
    return out


def reverb(x, t60=2.0, mix=.35, predelay=.02, tone_hi=6000):
    n_ir = int(t60 * SR)
    ir = band(RNG.standard_normal(n_ir), 80, tone_hi) * np.exp(-6.9 * np.arange(n_ir) / n_ir)
    ir = np.concatenate([np.zeros(int(predelay * SR)), ir])
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    size = len(x) + len(ir)
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(x)]
    return x * (1 - mix) + wet * mix


def master(x, peak_db=-3.0, drive=1.4, fade_in=.003, fade_out=.05):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9)
    x = np.tanh(x * drive) / np.tanh(drive)  # gentle saturation keeps transients dense without clipping
    n = len(x)
    fi, fo = int(fade_in * SR), int(fade_out * SR)
    x[:fi] *= np.linspace(0, 1, fi)
    x[n - fo:] *= np.linspace(1, 0, fo)
    return x / (np.max(np.abs(x)) + 1e-9) * 10 ** (peak_db / 20)


def write(name, x, **kw):
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = master(x, **kw).astype(np.float32)
    sf.write(path, data, SR, format="OGG", subtype="VORBIS")
    print("wrote", os.path.relpath(path, ROOT), "%.2fs" % (len(data) / SR))


# ------------------------------------------------------------------ HELIOS: warm, heavy, mechanical
def helios():
    d = 1.0
    x = band(noise(d), 40, 160) * decay(d, .08) * 1.5
    place(x, tone(660, .12, ((1, 1), (2, .3))) * env(.12, .005, .08) * .5, .05)
    place(x, tone(990, .16, ((1, 1), (2, .25))) * env(.16, .005, .12) * .5, .2)
    place(x, sweep(220, 440, .6) * env(.6, .2, .3) * .25, .35)
    write("helios/activate", reverb(x, 1.2, .3))

    d = 3.2
    t = t_axis(d)
    hum_f = 52 * (1 + .18 * (t / d) ** 2)
    hum = np.sin(2 * math.pi * np.cumsum(hum_f) / SR)
    hum += .5 * np.sin(2 * math.pi * np.cumsum(hum_f * 2) / SR) + .25 * np.sin(2 * math.pi * np.cumsum(hum_f * 3.02) / SR)
    pulses = np.zeros(len(t))
    for i, at in enumerate(np.arange(.3, d - .3, .5)):
        place(pulses, band(noise(.5), 60, 400) * decay(.5, .12) * (.4 + i * .12), at)
    air = band(noise(d), 800, 5000) * (t / d) ** 2 * .12
    x = hum * env(d, 1.6, .3) * (.6 + .4 * t / d) + pulses + air
    write("helios/charge", reverb(x, 2.5, .3))

    d = 2.3
    x = np.zeros(int(d * SR))
    servo_track = np.zeros(len(x))
    place(servo_track, band(sweep(160, 520, 1.9, .8, "saw"), 120, 2400) * env(1.9, .2, .4) * .22, .1)
    x += servo_track
    for i, at in enumerate((.2, .9, 1.55)):
        place(x, metal_hit(1.2, 92 - i * 11, .45, .8) * (.8 + .2 * i), at)
    whine = np.zeros(len(x))
    place(whine, sweep(900, 2600, 1.0, 1.5) * env(1.0, .6, .1) * .08, 1.3)
    x += whine
    write("helios/align", reverb(x, 2.2, .35))

    d = 1.4
    x = metal_hit(1.4, 70, .6, .85) * 1.2
    write("helios/ring_lock", reverb(x, 1.8, .35))

    d = .8
    x = tone(46, d, ((1, 1), (2, .4))) * decay(d, .18, .03) + band(noise(d), 200, 1500) * decay(d, .08) * .35
    write("helios/pulse", reverb(x, 1.4, .25))

    d = 1.8
    t = t_axis(d)
    transient = band(noise(d), 100, 9000) * decay(d, .05) * 2.2
    drop = sweep(190, 38, d, .35) * decay(d, .45, .002) * 1.6
    flutter = 1 + .35 * np.sin(2 * math.pi * 23 * t) * np.sin(2 * math.pi * 3.1 * t)
    roar = band(noise(d), 180, 3200) * env(d, .05, .5) * flutter * .9
    whine = sweep(2400, 1700, d) * env(d, .02, .8) * .12
    x = transient + drop + roar + whine
    write("helios/fire", reverb(x, 2.4, .3))

    d = 1.7
    t = t_axis(d)
    rush = np.zeros(len(t))
    n = noise(d)
    for k, (lo, hi) in enumerate(((250, 700), (500, 1400), (900, 2600), (1600, 4200))):
        w = np.clip(t / d * 4 - k, 0, 1)
        rush += band(n, lo, hi) * w * (.5 + .2 * k)
    x = rush * (t / d) ** 1.5 + crackle(d, 60, 2000, 8000) * (t / d) * .8 + sweep(70, 120, d) * (t / d) ** 2 * .4
    write("helios/descent", reverb(x, 1.5, .2))

    d = 5.5
    t = t_axis(d)
    sub = sweep(72, 24, d, .3) * decay(d, 1.4, .003) * 2.4
    blast = band(noise(d), 60, 2400) * decay(d, .35, .002) * 2.0
    snap = band(noise(d), 1500, 12000) * decay(d, .02) * 1.2
    debris = crackle(d, 120, 600, 5000, .01) * np.exp(-t / 1.6) * 1.4
    rumble = band(noise(d), 25, 140) * np.exp(-t / 2.2) * 1.4
    x = sub + blast + snap + debris + rumble
    write("helios/impact", reverb(x, 3.8, .4, .03), drive=2.2)

    d = 3.0
    t = t_axis(d)
    x = band(noise(d), 22, 110) * env(d, .25, 1.6) * 1.6 + band(noise(d), 120, 600) * np.exp(-t / .6) * .4
    write("helios/aftershock", reverb(x, 2.5, .3))

    d = 5.0
    t = t_axis(d)
    rumble = band(noise(d), 30, 180) * np.exp(-t / 2.4)
    embers = crackle(d, 45, 2500, 9000, .003) * (1 - t / d) * .7
    hiss = band(noise(d), 3000, 9000) * np.exp(-t / 1.8) * .1
    x = rumble + embers + hiss
    write("helios/aftermath", reverb(x, 2.0, .3), drive=1.1)

    d = 1.4
    x = sweep(1300, 160, 1.2, .6) * env(1.2, .01, .5) * .4
    x = np.concatenate([x, np.zeros(int(.2 * SR))])
    place(x, metal_hit(.9, 140, .25, .7) * .8, .95)
    write("helios/shutdown", reverb(x, 1.6, .3))


# ------------------------------------------------------------------ SINGULARITY: compressed, uncanny
def singularity():
    d = 1.2
    t = t_axis(d)
    chord = sum(tone(f, d) for f in (55, 58.27, 82.4, 87.3)) * env(d, .5, .5)
    x = chord + band(noise(d), 40, 300) * env(d, .6, .5) * .4
    write("singularity/activate", reverb(x, 2.6, .4))

    d = 4.2
    t = t_axis(d)
    beat = np.sin(2 * math.pi * 40 * t) + np.sin(2 * math.pi * 41.3 * t) + .5 * np.sin(2 * math.pi * 80.9 * t)
    wobble = 1 + .5 * np.sin(2 * math.pi * (1.2 + 2.5 * t / d) * t)
    pressure = band(noise(d), 60, 500) * (t / d) ** 2 * .6
    whisper = band(noise(d), 1800, 5000) * (.5 + .5 * np.sin(2 * math.pi * .7 * t)) * .06
    x = beat * wobble * env(d, 1.2, .3) * .8 + pressure + whisper
    write("singularity/charge", reverb(x, 3.0, .35))

    d = 6.2
    t = t_axis(d)
    swirl = np.zeros(len(t))
    for k in range(5):
        f0 = 60 * 2 ** k
        swirl += sweep(f0, f0 * 1.9, d, 1.6) * np.exp(-((k - 2) / 1.6) ** 2) * .4
    sub = np.sin(2 * math.pi * np.cumsum(30 + 18 * (t / d) ** 2) / SR) * (.4 + .6 * t / d)
    inward = band(noise(d), 200, 2400) * (.3 + .7 * (t / d) ** 3) * (1 + .6 * np.sin(2 * math.pi * (2 + 9 * t / d) * t)) * .5
    x = (swirl + sub + inward) * env(d, .8, .02)
    x[int((d - .45) * SR):] = 0  # the brief, deliberate silence before collapse
    write("singularity/pull", reverb(x, 2.0, .25, .01), fade_out=.005)

    d = 4.0
    t = t_axis(d)
    inhale = np.zeros(len(t))
    pre = .16
    place(inhale, (band(noise(pre), 300, 6000) * np.linspace(0, 1, int(pre * SR)) ** 4) * 1.4, 0)
    hit = np.zeros(len(t))
    boom = sweep(55, 18, d - pre, .25) * decay(d - pre, 1.1, .002) * 2.8 + band(noise(d - pre), 40, 900) * decay(d - pre, .25) * 2.0
    place(hit, boom, pre)
    secondary = np.zeros(len(t))
    place(secondary, band(noise(1.5), 400, 10000) * decay(1.5, .06) * 1.6 + band(noise(1.5), 50, 400) * decay(1.5, .4) * 1.2, pre + .26)
    x = inhale + hit + secondary
    write("singularity/implode", reverb(x, 4.0, .45, .02), drive=2.4)

    d = 5.0
    t = t_axis(d)
    res = tone(36, d, ((1, 1), (1.5, .3), (2.01, .25))) * np.exp(-t / 1.9)
    ring = np.sin(2 * math.pi * 220 * t) * np.sin(2 * math.pi * 3.3 * t) * np.exp(-t / 1.2) * .15
    x = res * (1 + .3 * np.sin(2 * math.pi * .8 * t)) + ring + band(noise(d), 40, 200) * np.exp(-t / 1.5) * .5
    write("singularity/aftermath", reverb(x, 3.0, .4))

    d = 1.0
    t = t_axis(d)
    x = sweep(120, 45, d, .6) * decay(d, .3, .01) + band(noise(d), 150, 900) * decay(d, .15) * .5
    write("singularity/collapse", reverb(x, 1.8, .4))


# ------------------------------------------------------------------ GLASSFALL: crystalline, precise
def bell(f, dur, tau, bright=.7):
    ratios = [1, 2.32, 4.25, 6.63, 9.38]
    return sum(tone(f * r, dur) * decay(dur, tau / (1 + i * .7)) * bright ** i for i, r in enumerate(ratios))


def shatter(dur, density, tau=.12):
    out = np.zeros(int(dur * SR))
    for _ in range(int(density * dur)):
        at = RNG.exponential(dur / 6)
        if at > dur - .2:
            continue
        f = RNG.uniform(2500, 9000)
        place(out, bell(f, .2, .03, .5) * RNG.uniform(.1, .5) * math.exp(-at / tau / 6), at)
    return out


def glassfall():
    d = 1.0
    x = np.zeros(int(d * SR))
    for i, f in enumerate((1318, 1760, 2349, 2637)):
        place(x, bell(f, .6, .25) * .4, .06 * i)
    write("glassfall/activate", reverb(x, 1.8, .4))

    d = 2.7
    t = t_axis(d)
    shimmer = sum(bell(f, d, 1.4, .5) * (1 + .4 * np.sin(2 * math.pi * (5 + i) * t)) for i, f in enumerate((587, 880, 1174, 1567)))
    x = shimmer * env(d, 1.2, .4) * .4 + sweep(400, 1600, d, 2) * env(d, 1.5, .2) * .08
    write("glassfall/charge", reverb(x, 2.4, .4))

    d = 1.2
    x = np.zeros(int(d * SR))
    for at in np.arange(.02, 1.0, .09):
        place(x, metal_hit(.12, 600 + RNG.uniform(-80, 80), .03, .6) * .35, at)
    place(x, band(noise(.8), 1500, 7000) * env(.8, .2, .3) * .2, .2)
    place(x, metal_hit(.6, 240, .2, .7), .95 if .95 < d - .3 else d - .4)
    write("glassfall/align", reverb(x, 1.6, .3))

    d = 2.2
    t = t_axis(d)
    x = sweep(200, 900, d, 1.5, "saw") * env(d, .4, .3) * .2 + band(noise(d), 4000, 12000) * env(d, 1.2, .2) * .08 + bell(1760, d, .9) * .25
    write("glassfall/barrage", reverb(x, 2.0, .35))

    d = .9
    t = t_axis(d)
    zap = np.sin(2 * math.pi * np.cumsum(1800 * np.exp(-t * 9) + 140) / SR + 4 * np.sin(2 * math.pi * 330 * t)) * decay(d, .12)
    x = zap + band(noise(d), 2000, 12000) * decay(d, .025) * 1.4 + band(noise(d), 60, 300) * decay(d, .08)
    write("glassfall/launch", reverb(x, 1.2, .25))

    d = 1.55
    t = t_axis(d)
    whistle = sweep(3200, 700, d, .9) * env(d, .5, .03) * .5
    air = band(noise(d), 1000, 5000) * (t / d) ** 2 * .5
    x = whistle + air
    write("glassfall/whistle", x, fade_out=.01)

    d = 2.2
    t = t_axis(d)
    crack = band(noise(d), 1200, 14000) * decay(d, .015) * 2.0
    thud = sweep(110, 35, d, .3) * decay(d, .3) * 1.4
    x = crack + thud + shatter(d, 70) + band(noise(d), 80, 800) * decay(d, .2) * .6
    write("glassfall/strike", reverb(x, 1.8, .3), drive=1.8)

    d = 5.5
    t = t_axis(d)
    fracture = np.zeros(len(t))
    for i in range(6):
        place(fracture, band(noise(.4), 2000, 14000) * decay(.4, .02) * (1 - i * .12), .03 * i)
    boom = np.zeros(len(t))
    place(boom, sweep(80, 22, d - .12, .3) * decay(d - .12, 1.3) * 2.4 + band(noise(d - .12), 40, 1200) * decay(d - .12, .4) * 1.6, .12)
    x = fracture * 1.6 + boom + shatter(d, 160, .5) * 1.2 + bell(392, d, 2.4, .6) * .3
    write("glassfall/final", reverb(x, 3.5, .4), drive=2.2)

    d = 5.0
    t = t_axis(d)
    x = shatter(d, 80, 1.4) * .9 + bell(523, d, 2.8, .5) * .25 * np.exp(-t / 2) + band(noise(d), 60, 300) * np.exp(-t / 1.2) * .3
    write("glassfall/aftermath", reverb(x, 2.6, .45))


# ------------------------------------------------------------------ interface
def interface():
    d = .45
    x = np.zeros(int(d * SR))
    place(x, tone(1480, .08, ((1, 1), (3, .15))) * env(.08, .003, .05) * .6, 0)
    place(x, tone(1975, .14, ((1, 1), (3, .15))) * env(.14, .003, .1) * .6, .1)
    write("ui/lock", reverb(x, .6, .15))

    d = .22
    x = tone(880, d, ((1, 1), (2.5, .2))) * decay(d, .04) * .5 + band(noise(d), 2000, 8000) * decay(d, .008) * .3
    write("ui/select", reverb(x, .4, .1))


if __name__ == "__main__":
    helios(); singularity(); glassfall(); interface()

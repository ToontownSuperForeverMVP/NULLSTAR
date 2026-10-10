# Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
# SPDX-License-Identifier: GPL-3.0-only

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
    # Block-wise writing: a single huge write overflows the native stack inside libsndfile's Vorbis encoder for
    # the Part 3 long-form beds. The decoded samples are identical either way.
    with sf.SoundFile(path, "w", SR, 1, format="OGG", subtype="VORBIS") as out:
        for i in range(0, len(data), SR):
            out.write(data[i:i + SR])
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


# ------------------------------------------------------------------ COSMIC and colossal releases (Part 2.5B)
def cosmic():
    """The cosmic act, the star mothership and the immense planetary-scale releases.

    These carry the colossal Helios timeline: ascent, the scale montage, the mothership reveal, its deployment
    and alignment, the charge, the firing transient and the planetary invocation. Everything is generated from
    the same oscillators, filtered noise and synthetic reverb as the rest of the set.
    """
    # 1. Planetary ascent: a rising drone while the air thins away underneath.
    d = 6.0
    t = t_axis(d)
    k = t / d
    rise = np.sin(2 * math.pi * np.cumsum(34 + 96 * k ** 1.7) / SR) * (.45 + .55 * k)
    rise += .4 * np.sin(2 * math.pi * np.cumsum(68 + 192 * k ** 1.7) / SR) * (.4 + .6 * k)
    strata = np.zeros(len(t))
    for i, (lo, hi) in enumerate(((28, 110), (90, 400), (300, 1300), (1200, 4200), (3000, 9500))):
        strata += band(noise(d), lo, hi) * np.clip(k * 3.4 - i, 0, 1) * (.5 + .14 * i)
    x = rise * env(d, 1.8, .7) * 1.1 + strata * .5 + sweep(90, 620, d, 2.2) * env(d, 2.4, .5) * .12
    write("cosmic/ascend", reverb(x, 4.4, .45, .05), fade_out=.4)

    # 2. Reveal: an enormous low swell with a slow harmonic bloom as a world shrinks into a point.
    d = 7.0
    t = t_axis(d)
    k = t / d
    root = 27.5
    swell = sum(tone(root * r, d, ((1, 1),)) * decay(d, 2.6) * a for r, a in ((1, 1.0), (1.5, .5), (2, .45), (2.99, .3), (4, .22), (5.98, .14)))
    swell = swell * env(d, 2.2, 1.6) + sum(np.sin(2 * math.pi * root * r * t) * .12 * env(d, 3, 1) for r in (8.02, 12.01))
    drift = band(noise(d), 40, 700) * env(d, 2.6, 1.4) * (1 + .4 * np.sin(2 * math.pi * .18 * t)) * .55
    shimmer = band(noise(d), 2400, 9000) * (k ** 3) * .05
    write("cosmic/reveal", reverb(swell * .8 + drift + shimmer, 6.5, .5, .07), fade_out=.5)

    # 3. Vast: pressure you feel more than hear, with sparse distant shimmer above it.
    d = 8.0
    t = t_axis(d)
    k = t / d
    sub = np.sin(2 * math.pi * np.cumsum(19 + 7 * np.sin(2 * math.pi * .12 * t)) / SR) * (.6 + .4 * k)
    sub += .35 * np.sin(2 * math.pi * np.cumsum(28.4 + 3 * np.sin(2 * math.pi * .19 * t)) / SR)
    pressure = band(noise(d), 16, 90) * (.4 + .6 * k)
    shard = np.zeros(int(d * SR))
    for at in np.arange(.7, d - .6, 1.35):
        place(shard, bell(RNG.uniform(1800, 5200), .5, .12, .5) * RNG.uniform(.05, .18), float(at))
    write("cosmic/vast", reverb(sub * 1.2 + pressure, 7.5, .5, .1), drive=1.15, fade_out=.6)

    # 4. Mothership: an ancient, monumental machine waking up.
    d = 7.0
    t = t_axis(d)
    x = np.zeros(int(d * SR))
    body = np.sin(2 * math.pi * np.cumsum(24 + 5 * (t / d) ** 2) / SR) * env(d, 2.4, 1.2) * 1.1
    body += .4 * np.sin(2 * math.pi * np.cumsum(48.6 + 9 * (t / d) ** 2) / SR) * env(d, 2.8, 1.2)
    x += body
    for i, at in enumerate((.35, 1.4, 2.2, 3.6, 5.1, 6.0)):
        place(x, metal_hit(1.6, 44 - i * 3, .7, .75) * (.9 - i * .07), at)
    place(x, band(sweep(60, 300, 4.5, 1.4, "saw"), 40, 1400) * env(4.5, 2.0, 1.6) * .16, 1.6)
    place(x, band(noise(3.0), 300, 6000) * env(3.0, 1.4, 1.2) * .1, 3.8)
    write("cosmic/mothership", reverb(x, 6.0, .5, .08), drive=1.5, fade_out=.5)

    # 5. Deployment: every colossal subsystem locking into register across the hull.
    d = 5.2
    t = t_axis(d)
    x = np.zeros(int(d * SR))
    for i, at in enumerate((.05, .55, .95, 1.45, 1.8, 2.35, 2.7, 3.2, 3.55, 4.1)):
        place(x, metal_hit(1.5, 62 - i * 2.4, .5 - i * .02, .8) * (.85 - i * .03), at)
    for i, at in enumerate((.2, 1.1, 2.0, 3.0, 4.0)):
        place(x, band(sweep(120, 520, 1.1, .8, "saw"), 100, 2600) * env(1.1, .2, .3) * .2, at)
    x += band(noise(d), 30, 200) * env(d, .6, 1.2) * .7
    place(x, sweep(700, 3400, 4.6, 1.6) * env(4.6, 3.0, 1.0) * .1, .4)
    write("cosmic/deploy", reverb(x, 4.0, .4, .04), drive=1.7, fade_out=.4)

    # 6. Charge: contained stellar energy building to a point of no return.
    d = 6.0
    t = t_axis(d)
    k = t / d
    core = np.sin(2 * math.pi * np.cumsum(46 * (1 + 3.2 * k ** 2)) / SR) * env(d, 1.4, .5) * 1.2
    core += .45 * np.sin(2 * math.pi * np.cumsum(92 * (1 + 3.0 * k ** 2)) / SR) * env(d, 1.4, .5) * (k ** 2)
    train = np.zeros(len(t))
    at = .2
    step = .42
    while at < d - .35:
        place(train, band(noise(.4), 70, 420) * decay(.4, .1) * (.35 + .65 * (at / d)), at)
        at += step
        step = max(.08, step * .86)
    plasma = band(noise(d), 900, 8000) * (k ** 3) * .22
    write("cosmic/charge", reverb(core + train + plasma, 4.6, .4, .05), drive=1.9, fade_out=.4)

    # 7. Alignment: focusing channels igniting and the aperture locking shut.
    d = 2.8
    x = np.zeros(int(d * SR))
    for i, at in enumerate((.05, .7, 1.35, 2.05)):
        place(x, metal_hit(1.4, 78 - i * 8, .5, .85) * (.9 - i * .1), at)
    place(x, band(sweep(160, 900, 2.2, 1.2, "saw"), 120, 3600) * env(2.2, .3, .6) * .18, .1)
    place(x, tone(38, 1.6, ((1, 1), (2, .35))) * decay(1.6, .5, .01) * 1.3, 1.1)
    write("cosmic/align", reverb(x, 3.2, .4, .03), drive=1.8)

    # 8. Fire: the terrifying transient of a stellar lance leaving its aperture.
    d = 5.5
    t = t_axis(d)
    snap = band(noise(d), 300, 14000) * decay(d, .04) * 2.4
    drop = sweep(260, 26, d, .3) * decay(d, .9, .002) * 2.2
    roar = band(noise(d), 90, 4200) * env(d, .05, 1.6) * (1 + .3 * np.sin(2 * math.pi * 17 * t)) * 1.3
    lance = sweep(3600, 900, d, .8) * env(d, .02, 2.2) * .18
    sub = np.sin(2 * math.pi * np.cumsum(31 - 9 * (t / d)) / SR) * decay(d, 1.4, .003) * 1.6
    write("cosmic/fire", reverb(snap + drop + roar + lance + sub, 5.5, .45, .02), drive=2.4, fade_out=.5)

    # 9. Invoke: planetary-scale impact, aftershock and the settlement of an enormous ash column.
    d = 8.0
    t = t_axis(d)
    contact = band(noise(d), 200, 16000) * decay(d, .02) * 2.0
    sub = sweep(86, 15, d, .25) * decay(d, 1.9, .003) * 2.8
    blast = band(noise(d), 40, 2600) * decay(d, .55, .002) * 2.2
    rumble = band(noise(d), 18, 120) * np.exp(-t / 3.2) * 1.8
    debris = crackle(d, 90, 500, 5000, .012) * np.exp(-t / 2.4) * 1.5
    column = band(noise(d), 200, 900) * np.exp(-t / 3.6) * .5
    x = contact + sub + blast + rumble + debris + column
    write("cosmic/invoke", reverb(x, 6.5, .45, .04), drive=2.6, fade_out=.6)


def colossal_releases():
    """Singularity and Glassfall releases at colossal scale."""
    # Singularity vacuum: enormous low-frequency pressure rather than a shock.
    d = 6.5
    t = t_axis(d)
    k = t / d
    drag = np.sin(2 * math.pi * np.cumsum(21 - 6 * k) / SR) * (.5 + .5 * k)
    drag += .4 * np.sin(2 * math.pi * np.cumsum(31.6 - 4 * k) / SR)
    suction = band(noise(d), 25, 260) * (k ** 1.6)
    whirl = np.zeros(len(t))
    for i in range(6):
        f = 48 * 2 ** i
        whirl += sweep(f, f * 2.6, d, 1.5) * np.exp(-((i - 2) / 1.7) ** 2) * .3
    x = drag * env(d, 1.6, .6) * 1.2 + suction + whirl * env(d, 1.2, .4) * (k ** 2)
    x[int((d - .5) * SR):] = 0
    write("singularity/vacuum", reverb(x, 5.0, .45, .03), drive=1.7, fade_out=.01)

    # Singularity release: the violent pop of a region of spacetime giving up.
    d = 6.5
    t = t_axis(d)
    inhale = np.zeros(len(t))
    pre = .2
    place(inhale, band(noise(pre), 200, 7000) * np.linspace(0, 1, int(pre * SR)) ** 4 * 1.6, 0)
    out = np.zeros(len(t))
    place(out, sweep(62, 16, d - pre, .22) * decay(d - pre, 1.6, .002) * 3.0
          + band(noise(d - pre), 30, 1400) * decay(d - pre, .35, .002) * 2.2, pre)
    place(out, band(noise(2.0), 500, 13000) * decay(2.0, .05) * 1.5, pre + .22)
    place(out, band(noise(d - pre), 18, 90) * np.exp(-np.arange(int((d - pre) * SR)) / SR / 3.4) * 1.6, pre)
    write("singularity/release", reverb(inhale + out, 6.0, .45, .02), drive=2.6, fade_out=.5)

    # Glassfall lattice charge: heavy bombardment machinery loading its rails.
    d = 4.2
    t = t_axis(d)
    x = np.zeros(int(d * SR))
    for i, at in enumerate((.05, .5, .95, 1.4, 1.85, 2.3, 2.75, 3.2)):
        place(x, metal_hit(.7, 130 - i * 5, .22, .7) * (.8 - i * .04), at)
    x += band(sweep(70, 320, d, 1.3, "saw"), 60, 1800) * env(d, 1.8, .6) * .2
    x += band(noise(d), 2200, 10000) * (t / d) ** 2.4 * .16
    x += bell(1046, d, 1.2, .5) * env(d, 1.0, .8) * .12
    write("glassfall/lattice_charge", reverb(x, 3.4, .4, .03), drive=1.9, fade_out=.35)

    # Glassfall dome: a continent of crystal detonating under its own pressure shell.
    d = 6.5
    t = t_axis(d)
    fracture = np.zeros(len(t))
    for i in range(9):
        place(fracture, band(noise(.5), 1500, 15000) * decay(.5, .022) * (1.2 - i * .1), .028 * i)
    boom = np.zeros(len(t))
    place(boom, sweep(90, 17, d - .2, .25) * decay(d - .2, 1.8) * 2.6
          + band(noise(d - .2), 35, 1500) * decay(d - .2, .5) * 1.9, .2)
    dome = bell(261.6, d, 3.2, .6) * .3 + bell(523, d, 2.4, .5) * .18
    x = fracture * 1.5 + boom + shatter(d, 220, .8) * 1.4 + dome
    write("glassfall/dome", reverb(x, 5.5, .42, .03), drive=2.5, fade_out=.5)


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


# ------------------------------------------------------------------ PART 3: long-form beds for the extended timelines
def drone(dur, base, k_curve, partials=((1, 1), (2, .4), (3, .18)), wobble=.0):
    """Slowly evolving harmonic drone; the pitch path follows base * k_curve(t / dur)."""
    t = t_axis(dur)
    out = np.zeros_like(t)
    for ratio, amp in partials:
        f = base * ratio * k_curve(t / dur) * (1 + wobble * np.sin(2 * math.pi * (.07 + .03 * ratio) * t))
        out += amp * np.sin(2 * math.pi * np.cumsum(f) / SR + RNG.uniform(0, 6.28))
    return out


def wind(dur, lo, hi, gust=.3):
    t = t_axis(dur)
    g = 1 + gust * np.sin(2 * math.pi * .13 * t) + gust * .6 * np.sin(2 * math.pi * .31 * t + 1)
    return band(noise(dur), lo, hi) * g


def pad(x, dur):
    """Zero-pads or trims a clip to an exact duration."""
    n = int(dur * SR)
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:n]


def part3_beds():
    """Phase-length beds for the Part 3 timelines (180 s Helios, 90 s Singularity, 100 s Glassfall). Accent times are
    the fractions the renderer uses, so sound and picture land together at the default operator durations."""
    flat = lambda u: 1 + 0 * u
    # Helios: ascent and the punch through the cloud deck (ASCENT 7 s + EXOATMOSPHERIC 4 s, punch at 3.5 s).
    d = 11.5; t = t_axis(d); k = t / d
    x = wind(d, 120, 2400, .25) * np.clip(1 - np.abs(k - .3) / .3, 0, 1) ** 1.5 * .9
    x += band(noise(d), 1500, 9000) * np.exp(-((t - 3.5) / .35) ** 2) * 1.4
    place(x, sweep(180, 40, 2.2, .5) * decay(2.2, .7, .01) * 1.1, 3.4)
    x += drone(d, 55, lambda u: 1 + .5 * u, wobble=.004) * np.clip((k - .3) / .25, 0, 1) * env(d, .01, 2.0) * .55
    x += band(noise(d), 4000, 11000) * np.clip((k - .45) / .4, 0, 1) * .07
    write("cosmic/rise", reverb(x, 5.0, .45, .05), drive=1.3, fade_out=1.2)

    # Helios: planet and solar system (7 s + 6 s): warm and vast, the sun swelling at the second act.
    d = 13.5; t = t_axis(d)
    x = drone(d, 41.2, lambda u: 1 + .02 * np.sin(u * 6), ((1, 1), (1.5, .45), (2, .5), (3, .2), (4, .1)), .003) * env(d, 2.5, 2.5) * .8
    x += drone(d, 164.8, flat, ((1, .4), (1.25, .25), (1.5, .3)), .004) * env(d, 4, 3) * np.clip((t - 6.5) / 2.5, 0, 1) * .35
    x += band(noise(d), 2500, 9000) * env(d, 3, 3) * .05
    place(x, band(noise(4.0), 200, 3000) * env(4.0, 1.6, 2.2) * .3, 6.6)
    write("cosmic/voyage", reverb(x, 7.0, .55, .08), drive=1.2, fade_out=1.5)

    # Helios: leaving the Milky Way (7 s): shimmering, widening, colder.
    d = 7.5; t = t_axis(d)
    x = drone(d, 110, lambda u: 1 - .05 * u, ((1, .5), (1.498, .4), (2.003, .3), (2.997, .2), (4.01, .12)), .006) * env(d, 1.5, 2) * .6
    x += drone(d, 27.5, flat, ((1, 1), (2, .3)), .002) * env(d, 2, 2) * .7
    x += band(noise(d), 5000, 12000) * (.5 + .5 * np.sin(2 * math.pi * 3.1 * t)) * env(d, 2, 2) * .05
    write("cosmic/galaxy", reverb(x, 8.0, .6, .1), drive=1.15, fade_out=1.4)

    # Helios: the observable boundary (6 s): retreat, the turn, the dive and the pass-through at 93 percent.
    d = 6.5; t = t_axis(d)
    x = drone(d, 33, lambda u: 1 + 1.2 * np.clip((u - .62) / .3, 0, 1) ** 2, ((1, 1), (2, .4)), .002) * env(d, 1.2, .2) * .8
    x += band(noise(d), 300, 6000) * np.clip((t - 3.7) / 1.9, 0, 1) ** 2.5 * (t < 5.6) * 1.1
    place(x, band(noise(1.4), 100, 14000) * decay(1.4, .18, .003) * 2.2, 5.58)
    place(x, sweep(120, 22, 1.0, .4) * decay(1.0, .5) * 1.4, 5.6)
    write("cosmic/boundary", reverb(x, 6.0, .5, .04), drive=1.8, fade_out=.6)

    # Helios: mothership presence (12 s): monumental and ancient, star-hum through the hull.
    d = 12.5; t = t_axis(d)
    x = drone(d, 29.1, lambda u: 1 + .08 * u, ((1, 1), (1.5, .35), (2, .45), (2.5, .15)), .002) * env(d, 3, 2) * 1.0
    x += drone(d, 220, flat, ((1, .2), (1.005, .2))) * (.5 + .5 * np.sin(2 * math.pi * .4 * t)) * env(d, 5, 3) * .12
    for i, at in enumerate((1.2, 3.4, 5.9, 8.1, 10.6)):
        place(x, metal_hit(2.6, 31 - i * 1.5, 1.4, .6) * (.55 + .05 * i), at)
    write("cosmic/presence", reverb(x, 7.5, .55, .09), drive=1.4, fade_out=1.4)

    # Helios: deployment (18 s): armour 0-22 %, petals 14-62 %, rings 40-72 %, alignment 58-86 %, pylons 70-100 %.
    d = 18.5; t = t_axis(d); k = t / 18.0
    x = band(noise(d), 25, 160) * env(d, 1, 2) * .55
    for i in range(6):
        at = .3 + i * .55
        place(x, band(sweep(90, 260, .9, 1, "saw"), 60, 1600) * env(.9, .1, .3) * .25, at)
        place(x, metal_hit(1.4, 70 - i * 3, .5, .8) * .5, at + .8)
    x += band(sweep(40, 110, d, 1.2, "saw"), 30, 900) * np.clip(1 - np.abs(k - .38) / .26, 0, 1) * .35
    x += band(noise(d), 200, 1400) * (.5 + .5 * np.sin(2 * math.pi * .9 * t)) * np.clip(1 - np.abs(k - .56) / .17, 0, 1) * .3
    for i, at in enumerate((10.8, 12.0, 13.1, 14.3, 15.4)):
        place(x, metal_hit(1.8, 52 - i * 2.5, .7, .85) * .85, at)
    x += band(sweep(300, 900, d, 1, "saw"), 200, 2400) * np.clip((k - .7) / .3, 0, 1) * .12
    place(x, tone(30, 2.2, ((1, 1), (2, .3))) * decay(2.2, .7, .01) * 1.4, 17.6)
    write("cosmic/unfold", reverb(x, 5.0, .45, .05), drive=1.7, fade_out=.8)

    # Helios: the crucible (18 s): the star wrung out, containment clamps between 45 and 90 percent.
    d = 18.5; t = t_axis(d); k = np.clip(t / 18.0, 0, 1)
    x = drone(d, 36, lambda u: 1 + 1.6 * u ** 2, ((1, 1), (2, .5), (3, .25)), .004) * env(d, 2, .6) * 1.0
    x += band(noise(d), 60, 1800) * (k ** 1.6) * (1 + .4 * np.sin(2 * math.pi * (2 + 6 * k) * t)) * .7
    x += band(noise(d), 2500, 10000) * (k ** 3) * .2
    for i, at in enumerate((8.1, 10.0, 11.8, 13.5, 15.0, 16.2)):
        place(x, metal_hit(1.6, 40 + i * 2, .6, .9) * 1.0, at)
        place(x, tone(26, 1.0) * decay(1.0, .35, .005) * 1.2, at)
    write("cosmic/crucible", reverb(x, 5.5, .45, .05), drive=2.0, fade_out=.6)

    # Helios: alignment (8 s): the iris grinds open, the barrel locks at 62-70 percent, then the held breath.
    d = 8.5; t = t_axis(d)
    x = np.zeros(int(d * SR))
    place(x, band(sweep(70, 140, 4.0, 1, "saw"), 50, 1200) * env(4.0, .4, .6) * .45, .8)
    place(x, metal_hit(2.4, 34, 1.0, .9) * 1.6, 5.0)
    place(x, tone(22, 2.4, ((1, 1), (2, .2))) * decay(2.4, .9, .004) * 1.8, 5.0)
    x += tone(1760, d) * np.clip((t - 6.0) / 1.5, 0, 1) * .02
    write("cosmic/lock", reverb(x, 5.0, .45, .04), drive=1.8, fade_out=.3)

    # Helios: the lance (10 s): breath held until 12 percent, discharge, roar, the ride and the atmospheric blow.
    d = 10.5
    x = np.zeros(int(d * SR))
    seg = 9.3; tt = t_axis(seg)
    ride = band(noise(seg), 80, 5000) * (np.exp(-tt / 2.6) * 1.6 + .5 * np.clip((tt - 3.0) / 5.5, 0, 1) ** 2)
    ride += sweep(240, 22, seg, .25) * decay(seg, 1.4, .002) * 2.4
    ride += band(noise(seg), 200, 16000) * decay(seg, .05) * 2.8
    ride += sweep(4200, 700, seg, .6) * env(seg, .02, 3) * .16
    place(x, ride, 1.2)
    place(x, band(noise(1.0), 60, 9000) * decay(1.0, .25, .003) * 1.8, 9.6)
    write("cosmic/lance", reverb(x, 6.0, .45, .03), drive=2.4, fade_out=.5)

    # Helios: the fall through the atmosphere (7 s), tearing the cloud deck at 3 s.
    d = 7.5; t = t_axis(d); k = t / d
    x = band(noise(d), 50, 3000) * (.3 + 1.1 * k ** 1.4) + sweep(900, 140, d, 1.2) * env(d, .5, .4) * .2
    place(x, band(noise(1.6), 300, 6000) * env(1.6, .2, 1.0) * .9, 3.0)
    x += drone(d, 31, lambda u: 1 - .3 * u, ((1, 1), (2, .3))) * k * .9
    write("helios/fall", reverb(x, 4.0, .4, .03), drive=2.0, fade_out=.3)

    # Helios: the pressure wall (10 s): approaching, passing near 4 s, rolling away.
    d = 10.5; t = t_axis(d)
    near = np.exp(-((t - 4.0) / 1.6) ** 2)
    x = band(noise(d), 25, 1600) * (.4 + 1.4 * near) + crackle(d, 140, 400, 4000, .01) * (.2 + near)
    x += band(noise(d), 18, 90) * np.exp(-np.clip(t - 4, 0, None) / 4) * 1.4
    write("helios/pressure", reverb(x, 5.0, .45, .04), drive=1.9, fade_out=1.0)

    # Helios: the annular eruption passes (10 s): distant booms walking around the horizon.
    d = 10.5
    x = band(noise(d), 20, 140) * env(d, 1, 3) * .8
    for i in range(11):
        at = .4 + i * .85 + RNG.uniform(-.2, .2)
        place(x, (band(noise(2.6), 30, 900) * decay(2.6, .5, .01) + sweep(70, 25, 2.6, .5) * decay(2.6, .6)) * (.5 + .5 * RNG.uniform()), at)
    write("helios/eruptions", reverb(x, 7.0, .55, .08), drive=1.6, fade_out=1.2)

    # Helios: the ruin (28 s): wind across the basin, fires, settling rubble, distant aftershocks, then a clearing.
    d = 28.0; t = t_axis(d); k = t / d
    x = wind(d, 80, 1800, .35) * (.6 - .3 * k) + crackle(d, 60, 800, 6000, .006) * (.5 - .35 * k)
    x += band(noise(d), 18, 70) * (.6 - .4 * k)
    for at in (3.5, 9.0, 15.5, 22.0):
        place(x, band(noise(3.5), 20, 400) * decay(3.5, .9, .05) * .9, at)
    x += drone(d, 55, flat, ((1, .3), (1.5, .2), (2, .15)), .003) * np.clip((k - .6) / .3, 0, 1) * .4
    write("helios/ruin", reverb(x, 6.0, .5, .08), drive=1.25, fade_out=3.0)

    # Singularity: gravity (11 s), resonance (8 s), compression with its hush (11 s) and the sink (30 s).
    d = 11.5; t = t_axis(d); k = t / d
    x = drone(d, 24, lambda u: 1 - .25 * u, ((1, 1), (1.41, .4), (2, .35), (2.83, .15)), .01) * env(d, 2.5, .8) * 1.1
    x += band(noise(d), 30, 300) * (.3 + .7 * k) * (1 + .5 * np.sin(2 * math.pi * (.5 + 2 * k) * t)) * .6
    write("singularity/gravity", reverb(x, 6.0, .5, .06), drive=1.8, fade_out=.8)
    d = 8.0
    x = drone(d, 55, lambda u: 1 + .1 * u, ((1, .8), (1.012, .8), (2, .3), (2.03, .3))) * env(d, 1, 1) * .8
    x += drone(d, 330, lambda u: 1 - .2 * u, ((1, .15), (1.06, .15)), .02) * env(d, 2, 1) * .5
    write("singularity/resonance", reverb(x, 5.0, .5, .05), drive=1.6, fade_out=.8)
    d = 11.5; t = t_axis(d); k = np.clip(t / 11.25, 0, 1)
    hush = np.clip(1 - np.clip((k - .5) / .08, 0, 1) + np.clip((k - .72) / .04, 0, 1), 0, 1)
    x = drone(d, 30, lambda u: 1 + 2.2 * u ** 2, ((1, 1), (2, .4), (3, .2)), .008) * hush * .9
    x += band(noise(d), 80, 3000) * (k ** 2) * hush * .6
    x += band(sweep(60, 900, d, 2.0, "saw"), 40, 4000) * np.clip((k - .74) / .26, 0, 1) ** 2 * .5
    x += tone(2093, d) * np.clip(1 - hush, 0, 1) * .015
    write("singularity/compression", reverb(x, 5.0, .45, .04), drive=1.9, fade_out=.1)
    d = 30.0; t = t_axis(d); k = t / d
    x = drone(d, 22, lambda u: 1 - .15 * u, ((1, 1), (1.5, .3), (2, .3)), .006) * np.exp(-t / 14) * 1.0
    x += crackle(d, 40, 300, 3000, .02) * (.6 - .5 * k) + wind(d, 60, 900, .4) * .35
    for at in (2.0, 6.5, 12.0, 19.0):
        place(x, tone(41, 4.0, ((1, 1), (2.7, .3))) * decay(4.0, 1.2, .02) * .5, at)
    write("singularity/sink", reverb(x, 7.0, .55, .08), drive=1.3, fade_out=3.0)

    # Glassfall: the platform waking (12.5 s) and the shard field (30 s).
    d = 13.0; t = t_axis(d); k = t / d
    x = drone(d, 36.7, lambda u: 1 + .1 * u, ((1, 1), (2, .4), (3, .2)), .003) * env(d, 2, 1) * .8
    for i in range(9):
        place(x, bell(440 * (1.5 ** (i % 4)) / 2, 3.0, 1.2, .6) * .08, .8 + i * 1.3)
        place(x, metal_hit(1.2, 60 + i * 4, .4, .8) * .4, 1.0 + i * 1.3)
    x += band(noise(d), 3000, 10000) * (k ** 2) * .06
    write("glassfall/platform", reverb(x, 5.0, .5, .05), drive=1.6, fade_out=.8)
    d = 30.0; t = t_axis(d)
    x = wind(d, 200, 3000, .3) * .3
    for i in range(46):
        at = RNG.uniform(.2, d - 3.5)
        place(x, bell(RNG.uniform(900, 3200), 3.0, RNG.uniform(.4, 1.4), .5) * RNG.uniform(.03, .1) * (1 - at / d * .7), at)
    x += drone(d, 73.4, flat, ((1, .4), (1.5, .3), (2, .2)), .002) * np.exp(-t / 16) * .5
    write("glassfall/shardfield", reverb(x, 7.0, .6, .08), drive=1.2, fade_out=3.0)


# The generator shares one seeded RNG stream, so the Part 2 set is generated first and in its original order:
# the new cosmic/colossal sounds, then the Part 3 beds, are appended and cannot disturb any validated existing asset.
if __name__ == "__main__":
    helios(); singularity(); glassfall(); interface(); cosmic(); colossal_releases()
    part3_beds()

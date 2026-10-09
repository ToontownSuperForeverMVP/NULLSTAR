"""Generates every original NULLSTAR texture procedurally.

Run from the repository root:  python -I tools/assets/generate_textures.py
Requires numpy and Pillow. Output is deterministic (fixed seeds) and is committed under
src/main/resources/assets/nullstar/textures. No third-party image content is used.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nullstar", "textures")
RNG = np.random.default_rng(0x4E554C4C)


def save(rel, rgba):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img = np.clip(rgba, 0, 255).astype(np.uint8)
    mode = "RGBA" if img.shape[2] == 4 else "RGB"
    Image.fromarray(img, mode).save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT), img.shape[1], "x", img.shape[0])


def periodic_noise(h, w, fy, fx, rng):
    """Tileable value noise with smooth cosine interpolation."""
    grid = rng.random((fy, fx))
    ys = np.arange(h) * fy / h
    xs = np.arange(w) * fx / w
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    ty = (1 - np.cos((ys - y0) * math.pi)) * .5
    tx = (1 - np.cos((xs - x0) * math.pi)) * .5
    y1 = (y0 + 1) % fy
    x1 = (x0 + 1) % fx
    a = grid[y0][:, x0]
    b = grid[y0][:, x1]
    c = grid[y1][:, x0]
    d = grid[y1][:, x1]
    top = a + (b - a) * tx[None, :]
    bottom = c + (d - c) * tx[None, :]
    return top + (bottom - top) * ty[:, None]


def fbm(h, w, base_y, base_x, octaves, rng, gain=.5):
    total = np.zeros((h, w))
    amp, norm = 1.0, 0.0
    for o in range(octaves):
        total += periodic_noise(h, w, base_y * 2 ** o, base_x * 2 ** o, rng) * amp
        norm += amp
        amp *= gain
    return total / norm


def normalize(a):
    return (a - a.min()) / max(1e-9, a.max() - a.min())


def radial(size):
    c = (size - 1) / 2
    y, x = np.mgrid[0:size, 0:size]
    return np.sqrt((x - c) ** 2 + (y - c) ** 2) / c


# ---------------------------------------------------------------- energy materials
def plasma():
    s = 256
    low = normalize(fbm(s, s, 4, 4, 5, RNG))
    high = normalize(fbm(s, s, 8, 8, 4, RNG, .6))
    ridged = 1 - np.abs(normalize(fbm(s, s, 6, 6, 5, RNG)) * 2 - 1)
    ridged = ridged ** 3
    rgba = np.stack([low * 255, high * 255, ridged * 255, np.full((s, s), 255.0)], -1)
    save("effect/plasma.png", rgba)


def flow():
    h, w = 256, 64
    streak = normalize(fbm(h, w, 2, 16, 4, RNG, .55))
    fine = normalize(fbm(h, w, 6, 32, 3, RNG))
    filaments = (1 - np.abs(fine * 2 - 1)) ** 6
    body = normalize(streak * .7 + filaments * .5)
    rgba = np.stack([body * 255, filaments * 255, streak * 255, np.full((h, w), 255.0)], -1)
    save("effect/flow.png", rgba)


def glow():
    s = 128
    r = radial(s)
    core = np.exp(-(r / .18) ** 2)
    halo = np.exp(-(r / .55) ** 2) * .55
    a = np.clip(core + halo, 0, 1) * np.clip(1 - r, 0, 1) ** .5
    rgba = np.stack([np.full((s, s), 255.0)] * 3 + [a * 255], -1)
    save("effect/glow.png", rgba)


def shock():
    """u wraps around the ring, v runs from trailing (0) to leading edge (1)."""
    h, w = 64, 256
    v = np.linspace(0, 1, h)[:, None]
    ripple = normalize(fbm(h, w, 2, 12, 4, RNG))
    lead = np.exp(-((v - .88) / .06) ** 2)
    wake = np.clip(v / .88, 0, 1) ** 2.2 * .55 * (.55 + .45 * ripple)
    bands = (.5 + .5 * np.sin(v * 38 + ripple * 6)) * .18 * v
    edge = np.clip((1 - v) / .1, 0, 1)
    a = np.clip(lead + wake + bands, 0, 1) * edge
    rgba = np.stack([np.full((h, w), 255.0)] * 3 + [a * 255], -1)
    save("effect/shock.png", rgba)


def smoke():
    s = 128
    sheet = np.zeros((256, 256, 4))
    for i in range(4):
        n = normalize(fbm(s, s, 4, 4, 5, RNG, .55))
        r = radial(s) + (n - .5) * .55
        mask = np.clip(1 - r, 0, 1) ** 1.4
        shade = .55 + .45 * normalize(fbm(s, s, 3, 3, 4, RNG))
        a = np.clip(mask * (.7 + 1.1 * n), 0, 1)
        y, x = divmod(i, 2)
        sheet[y * s:(y + 1) * s, x * s:(x + 1) * s] = np.stack([shade * 255] * 3 + [a * 255], -1)
    save("effect/smoke.png", sheet)


def voronoi(s, count, rng):
    pts = rng.random((count, 2)) * s
    y, x = np.mgrid[0:s, 0:s].astype(float)
    d1 = np.full((s, s), 1e9)
    d2 = np.full((s, s), 1e9)
    for py, px in pts:
        for oy in (-s, 0, s):
            for ox in (-s, 0, s):
                d = np.hypot(x - px - ox, y - py - oy)
                closer = d < d1
                d2 = np.where(closer, d1, np.minimum(d2, d))
                d1 = np.where(closer, d, d1)
    return d1, d2


def void():
    """Black event-horizon disc with a crisp but antialiased edge (drawn camera-facing)."""
    s = 128
    r = radial(s)
    a = np.clip((1 - r) / .06, 0, 1)
    rgba = np.stack([np.zeros((s, s))] * 3 + [a * 255], -1)
    save("effect/void.png", rgba)


def halo():
    """Uneven luminous photon ring: thin bright band with angular noise plus a soft outer glow."""
    s = 256
    c = (s - 1) / 2
    y, x = np.mgrid[0:s, 0:s].astype(float)
    r = np.hypot(x - c, y - c) / c
    ang = np.arctan2(y - c, x - c)
    wobble = .03 * np.sin(ang * 3 + 1) + .02 * np.sin(ang * 7 + 2) + .012 * np.sin(ang * 13)
    band = np.exp(-((r - .78 - wobble) / .025) ** 2)
    glow_ = np.exp(-((r - .8) / .12) ** 2) * .35
    intensity = .7 + .3 * np.sin(ang * 5 + .5) * np.sin(ang * 2)
    a = np.clip((band + glow_) * intensity, 0, 1) * (r < 1)
    rgba = np.stack([np.full((s, s), 255.0)] * 3 + [a * 255], -1)
    save("effect/halo.png", rgba)


def crystal():
    s = 128
    d1, d2 = voronoi(s, 22, RNG)
    edge = np.clip(1 - (d2 - d1) / 3.0, 0, 1)
    interior = normalize(d1)
    inclusions = normalize(fbm(s, s, 4, 4, 4, RNG)) ** 3
    v = np.clip(.35 + interior * .35 + edge * .65 + inclusions * .35, 0, 1)
    rgba = np.stack([v * 255, v * 255, v * 255, np.full((s, s), 255.0)], -1)
    save("effect/crystal.png", rgba)


# ---------------------------------------------------------------- cosmic scale materials
def star():
    """Soft additive point light: bright core, four-point diffraction cross, wide halo."""
    s = 128
    c = (s - 1) / 2
    y, x = np.mgrid[0:s, 0:s].astype(float)
    dx, dy = (x - c) / c, (y - c) / c
    r = np.hypot(dx, dy)
    core = np.exp(-(r / .1) ** 2)
    halo = np.exp(-(r / .42) ** 2) * .38
    cross = (np.exp(-(np.abs(dx) / .015) ** 2) + np.exp(-(np.abs(dy) / .015) ** 2)) * np.exp(-(r / .6) ** 2) * .3
    a = np.clip(core + halo + cross, 0, 1) * np.clip(1 - r, 0, 1) ** .35
    rgba = np.stack([np.full((s, s), 255.0)] * 3 + [a * 255], -1)
    save("effect/star.png", rgba)


def gas():
    """Soft tiling nebula/vapour cloud: turbulent rgb detail with a radial alpha falloff."""
    s = 256
    coarse = normalize(fbm(s, s, 3, 3, 5, RNG, .55))
    fine = normalize(fbm(s, s, 7, 7, 4, RNG, .5))
    filaments = (1 - np.abs(normalize(fbm(s, s, 11, 11, 4, RNG)) * 2 - 1)) ** 4
    radius = radial(s) + (coarse - .5) * .5
    mask = np.clip(1 - radius, 0, 1) ** 1.25
    a = np.clip(mask * (.55 + .9 * coarse + .5 * filaments), 0, 1)
    rgba = np.stack([(coarse * .6 + fine * .4) * 255, (fine * .55 + filaments * .45) * 255, (filaments * .7 + coarse * .3) * 255, a * 255], -1)
    save("effect/gas.png", rgba)


def lance():
    """Colossal beam fabric. u wraps the circumference, v runs along the beam and tiles vertically."""
    h, w = 256, 96
    ridged = 1 - np.abs(normalize(fbm(h, w, 5, 3, 5, RNG, .55)) * 2 - 1)
    ribbed = 1 - np.abs(normalize(fbm(h, w, 14, 2, 3, RNG)) * 2 - 1)
    streaks = normalize(fbm(h, w, 3, 26, 4, RNG, .6))
    body = normalize(ridged ** 1.6 * .55 + ribbed ** 2 * .5)
    rgba = np.stack([np.clip(body * 1.1, 0, 1) * 255, np.clip(ribbed * .9 + streaks * .35, 0, 1) * 255,
                     np.clip(streaks * .8 + ribbed * .4, 0, 1) * 255, np.full((h, w), 255.0)], -1)
    save("effect/lance.png", rgba)


def rubble():
    s = 64
    d1, d2 = voronoi(s, 14, RNG)
    cracks = np.clip((d2 - d1) / 2.5, 0, 1)
    grain = normalize(fbm(s, s, 8, 8, 3, RNG))
    v = (.55 + .3 * grain) * (.45 + .55 * cracks)
    rgba = np.stack([v * 255] * 3 + [np.full((s, s), 255.0)], -1)
    save("effect/rubble.png", rgba)


# ---------------------------------------------------------------- machinery hulls
def hull(name, base, accent, seam, panel_px, style, seed):
    """Alpha < 255 marks emissive pixels; the hull shader reads (1 - alpha) as emission."""
    rng = np.random.default_rng(seed)
    s = 256
    weather = normalize(fbm(s, s, 8, 8, 5, rng))
    grime = normalize(fbm(s, s, 3, 3, 4, rng))
    rgb = np.zeros((s, s, 3))
    for c in range(3):
        rgb[..., c] = base[c] * (.72 + .38 * weather) * (.8 + .25 * grime)
    alpha = np.full((s, s), 255.0)
    y, x = np.mgrid[0:s, 0:s]
    # Irregular panel subdivision: recursive splits give mechanical, non-grid plating.
    rects = [(0, 0, s, s)]
    out = []
    while rects:
        ry, rx, rh, rw = rects.pop()
        if (rh > panel_px * 2 or rw > panel_px * 2) and rng.random() < .9:
            if rh > rw:
                cut = int(rh * rng.uniform(.3, .7)) // 4 * 4
                rects += [(ry, rx, cut, rw), (ry + cut, rx, rh - cut, rw)]
            else:
                cut = int(rw * rng.uniform(.3, .7)) // 4 * 4
                rects += [(ry, rx, rh, cut), (ry, rx + cut, rh, rw - cut)]
        else:
            out.append((ry, rx, rh, rw))
    for ry, rx, rh, rw in out:
        tone = rng.uniform(.82, 1.12)
        sl = (slice(ry, ry + rh), slice(rx, rx + rw))
        rgb[sl] *= tone
        # Bevel: lit top/left edge, shadowed bottom/right edge, recessed seam line.
        rgb[ry:ry + 1, rx:rx + rw] = np.minimum(255, rgb[ry:ry + 1, rx:rx + rw] * 1.55)
        rgb[ry:ry + rh, rx:rx + 1] = np.minimum(255, rgb[ry:ry + rh, rx:rx + 1] * 1.4)
        rgb[ry + rh - 1:ry + rh, rx:rx + rw] *= .35
        rgb[ry:ry + rh, rx + rw - 1:rx + rw] *= .45
        feature = rng.random()
        if feature < .22 and rh >= 12 and rw >= 12:  # rivets
            for py in (ry + 3, ry + rh - 4):
                for px in range(rx + 3, rx + rw - 3, 6):
                    rgb[py, px] = np.minimum(255, rgb[py, px] * 1.8)
                    rgb[py + 1, px] *= .5
        elif feature < .40 and rh >= 10 and rw >= 14:  # vent slots
            for py in range(ry + 3, ry + rh - 3, 3):
                rgb[py, rx + 3:rx + rw - 3] *= .25
        elif feature < .52 and rh >= 8 and rw >= 20:  # accent stripe
            band = slice(ry + rh // 2 - 1, ry + rh // 2 + 2)
            for c in range(3):
                rgb[band, rx + 2:rx + rw - 2, c] = accent[c] * (.8 + .2 * weather[band, rx + 2:rx + rw - 2])
        elif feature < .70 and rh >= 6 and rw >= 6:  # emissive seam inlay
            if style == "chevron":
                cy, cx = ry + rh // 2, rx + rw // 2
                m = (np.abs(np.abs(x[sl] - cx) - (y[sl] - ry) * .8) < 1.1)
            else:
                m = np.zeros((rh, rw), bool)
                m[rh // 2, 2:rw - 2] = True
            region_rgb = rgb[sl]
            region_a = alpha[sl]
            for c in range(3):
                region_rgb[..., c] = np.where(m, seam[c], region_rgb[..., c])
            region_a[m] = 40
        elif feature < .78 and rh >= 10 and rw >= 10:  # indicator lamp
            cy, cx = ry + 4, rx + rw - 5
            rgb[cy - 1:cy + 2, cx - 1:cx + 2] = seam
            alpha[cy - 1:cy + 2, cx - 1:cx + 2] = 0
    # Fine scratches.
    for _ in range(140):
        sy, sx = rng.integers(0, s, 2)
        length = rng.integers(3, 12)
        rgb[sy, sx:min(s, sx + length)] = np.minimum(255, rgb[sy, sx:min(s, sx + length)] * 1.25)
    save("machine/" + name + ".png", np.dstack([rgb, alpha]))


# ---------------------------------------------------------------- items and interface
def controller():
    p = np.zeros((16, 16, 4))
    body = (34, 38, 46)
    for yy in range(4, 15):
        for xx in range(4, 12):
            p[yy, xx] = (*body, 255)
    p[4, 4:12] = (70, 78, 92, 255)
    p[4:15, 4] = (60, 66, 78, 255)
    p[14, 4:12] = (18, 20, 24, 255)
    p[4:15, 11] = (20, 22, 27, 255)
    for yy in range(6, 10):  # holographic screen
        for xx in range(6, 10):
            t = (yy - 6) / 3
            p[yy, xx] = (60 + 40 * t, 210, 255, 255)
    p[7, 7] = p[8, 8] = (220, 250, 255, 255)
    p[11, 6] = (255, 196, 70, 255)
    p[11, 8] = (180, 110, 255, 255)
    p[11, 10 - 1] = (90, 230, 255, 255)
    p[12:14, 6:10] = (48, 54, 64, 255)
    for yy in range(0, 4):  # antenna with gold emitter tip
        p[yy, 9] = (120, 128, 140, 255)
    p[0, 9] = (255, 214, 90, 255)
    p[1, 8] = p[1, 10] = (255, 180, 60, 160)
    save("item/controller.png", p)


def icons():
    s = 32
    sheet = np.zeros((s, s * 3, 4))
    y, x = np.mgrid[0:s, 0:s].astype(float)
    c = (s - 1) / 2
    r = np.hypot(x - c, y - c)
    ang = np.arctan2(y - c, x - c)
    # Helios: gold core with asymmetric vanes.
    a = np.zeros((s, s, 4))
    core = np.clip(1 - r / 5, 0, 1)
    vanes = ((np.abs(np.sin(ang * 3 + .4)) > .82) & (r > 7) & (r < 15)) * (1 - (np.abs(ang - .9) < .5))
    ring = (np.abs(r - 10) < 1).astype(float)
    v = np.clip(core * 1.4 + vanes * .9 + ring * .7, 0, 1)
    a[..., 0], a[..., 1], a[..., 2] = 255 * v, (190 + 60 * core) * v, (70 + 160 * core) * v
    a[..., 3] = np.clip(v * 320, 0, 255)
    sheet[:, 0:s] = a
    # Singularity: dark core, violet accretion ring, uneven arcs.
    b = np.zeros((s, s, 4))
    disc = np.exp(-((r - 9 - np.sin(ang * 2) * 1.2) / 2.2) ** 2)
    arcs = (np.abs(r - 13.5) < .9) * (np.sin(ang * 3 + 1) > .2)
    v = np.clip(disc + arcs * .8, 0, 1)
    b[..., 0], b[..., 1], b[..., 2] = 170 * v + 40 * disc, 70 * v, 255 * v
    b[..., 3] = np.clip(v * 300 + (r < 6) * 255, 0, 255)
    b[r < 6, :3] = 8
    sheet[:, s:2 * s] = b
    # Glassfall: falling crystal shard over lattice lines.
    g = np.zeros((s, s, 4))
    shard = (np.abs(x - c) * 2.4 + np.abs(y - 12) * .55 < 7.5) & (y > 2) & (y < 27)
    facet = shard & (x > c)
    lattice = ((np.abs((x + y) % 10 - 5) < .6) | (np.abs((x - y) % 10 - 5) < .6)) & (y < 9)
    g[shard] = (160, 245, 255, 255)
    g[facet] = (90, 200, 255, 255)
    g[lattice & ~shard] = (60, 170, 210, 200)
    g[(np.abs(x - c) < 1) & (y > 26)] = (220, 255, 255, 220)
    sheet[:, 2 * s:3 * s] = g
    save("gui/weapons.png", sheet)


def mod_icon():
    s = 128
    y, x = np.mgrid[0:s, 0:s].astype(float)
    c = (s - 1) / 2
    r = np.hypot(x - c, y - c) / c
    ang = np.arctan2(y - c, x - c)
    sky = np.stack([10 + 18 * (1 - y / s), 12 + 10 * (1 - y / s), 26 + 30 * (1 - y / s)], -1)
    beam = np.exp(-((x - c) / 3.5) ** 2) * (y > c)
    core = np.exp(-(r / .12) ** 2)
    ring = np.exp(-((r - .42) / .03) ** 2) * (.6 + .4 * np.sin(ang * 6))
    vanes = (np.abs(np.sin(ang * 3 + .5)) > .9) * (r > .5) * (r < .85)
    glow_ = np.clip(core * 1.6 + beam * 1.2 + ring + vanes * .5, 0, 1.5)
    img = sky + np.stack([255 * glow_, 200 * glow_, 90 * glow_], -1)
    img[..., 1] += core * 55
    img[..., 2] += core * 160
    save("../icon.png", np.dstack([img, np.full((s, s), 255.0)]))


if __name__ == "__main__":
    if not os.path.isdir(os.path.join(ROOT, "src")):
        sys.exit("run from the NULLSTAR repository")
    plasma(); flow(); glow(); shock(); smoke(); void(); halo(); crystal(); rubble()
    star(); gas(); lance()
    hull("helios_hull", (58, 56, 54), (196, 150, 52), (255, 196, 78), 22, "line", 11)
    hull("glassfall_hull", (54, 62, 72), (140, 170, 190), (110, 236, 255), 18, "chevron", 23)
    hull("singularity_hull", (26, 22, 32), (90, 60, 130), (196, 110, 255), 26, "line", 37)
    hull("cosmic_hull", (46, 44, 50), (150, 126, 84), (255, 214, 140), 34, "line", 53)
    controller(); icons(); mod_icon()

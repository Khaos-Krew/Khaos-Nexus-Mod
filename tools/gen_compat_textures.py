"""Hand-shaded 32x32 item textures for the companion-mod items (Ars, Relics, Artifacts, Apotheosis gems).
Shapes are drawn per pixel with a light from the top-left: base ramp, rim highlight, dark outline, specular dots."""
import math, os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'khaosnexus', 'textures', 'item')
N = 32
VOID = [(18, 8, 30), (46, 18, 74), (88, 36, 132), (140, 72, 196), (196, 132, 240), (240, 214, 255)]
RIFT = [(30, 6, 10), (82, 14, 24), (150, 30, 44), (206, 58, 70), (246, 120, 120), (255, 214, 200)]
GOLD = [(40, 26, 8), (96, 64, 18), (156, 110, 36), (206, 160, 64), (240, 210, 120), (255, 246, 210)]
STEEL = [(16, 16, 22), (40, 40, 52), (72, 72, 90), (112, 112, 132), (164, 164, 184), (226, 226, 240)]
TEAL = [(6, 24, 30), (14, 60, 70), (26, 110, 120), (60, 170, 172), (130, 226, 214), (220, 255, 246)]

def ramp(pal, t):
    t = max(0.0, min(0.999, t)); return pal[int(t * len(pal))]

def new(): return Image.new('RGBA', (N, N), (0, 0, 0, 0))

def shade(img, mask, pal, lx=-0.6, ly=-0.8, bias=0.0):
    """mask(x, y) -> (inside, nx, ny) with a surface normal; Lambert light + outline."""
    px = img.load()
    for y in range(N):
        for x in range(N):
            m = mask(x + 0.5, y + 0.5)
            if not m: continue
            nx, ny = m
            l = 0.55 + 0.45 * (-(nx * lx + ny * ly)) + bias
            px[x, y] = ramp(pal, l) + (255,)
    outline(img, pal[0])

def outline(img, c):
    px = img.load(); edge = []
    for y in range(N):
        for x in range(N):
            if px[x, y][3] == 0 and any(0 <= x + dx < N and 0 <= y + dy < N and px[x + dx, y + dy][3] > 0 and px[x + dx, y + dy][:3] != c
                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for x, y in edge: px[x, y] = c + (255,)

def spec(img, pts, c=(255, 255, 255)):
    px = img.load()
    for x, y in pts:
        if px[x, y][3]: px[x, y] = c + (255,)

def gem(pal, facets=6, r=11, cx=16, cy=16, rot=0.0):
    """Faceted gem: polygon, each facet lit by its angle; a bright table in the middle."""
    def m(x, y):
        dx, dy = x - cx, y - cy; a = math.atan2(dy, dx) - rot; d = math.hypot(dx, dy)
        seg = 2 * math.pi / facets; ang = (a % seg) - seg / 2
        rr = r * math.cos(seg / 2) / math.cos(ang)
        if d > rr: return None
        if d < rr * 0.42: return (-0.4, -0.5)           # table
        mid = math.floor(a / seg) * seg + seg / 2 + rot
        return (math.cos(mid), math.sin(mid))
    img = new(); shade(img, m, pal); spec(img, [(cx - 3, cy - 4), (cx - 2, cy - 4), (cx - 3, cy - 3)]); return img

def shard():
    img = new()
    def m(x, y):  # tall crystal leaning right, two faces
        u = (x - 16) + (y - 16) * 0.35
        w = 6.5 * (1 - abs(y - 16) / 15.0)
        if abs(y - 16) > 14 or abs(u) > w: return None
        return (-0.8, -0.3) if u < 0 else (0.7, 0.2)
    shade(img, m, VOID)
    def m2(x, y):  # small side crystal
        u = (x - 9) + (y - 23) * 0.6; w = 3.5 * (1 - abs(y - 23) / 7.0)
        if abs(y - 23) > 6 or abs(u) > w: return None
        return (-0.8, -0.3) if u < 0 else (0.7, 0.2)
    s2 = new(); shade(s2, m2, RIFT); img = Image.alpha_composite(s2, img)
    spec(img, [(14, 6), (14, 7), (13, 9)]); return img

def glyph():
    img = new(); px = img.load()
    for y in range(N):  # dark rune disc with rim light
        for x in range(N):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if d < 14.5:
                t = 0.25 + 0.25 * (1 - d / 14.5) - 0.12 * ((x + y) / 64.0)
                px[x, y] = ramp(VOID, t) + (255,)
                if d > 13: px[x, y] = VOID[3] + (255,)
    for i in range(-9, 10):  # jagged rift tear across the disc
        x = 16 + i; y = int(16 + i * 0.25 + 2.2 * math.sin(i * 1.3))
        for k, c in ((0, RIFT[4]), (-1, RIFT[3]), (1, RIFT[3]), (2, RIFT[1])):
            if 0 <= y + k < N: px[x, y + k] = c + (255,)
    spec(img, [(12, 14), (13, 14)], RIFT[5]); outline(img, VOID[0]); return img

def pendant():
    img = new(); px = img.load()
    for t in range(0, 360, 3):  # chain arc
        a = math.radians(t / 2.0 + 180); x = int(16 + 12 * math.cos(a)); y = int(13 + 11 * math.sin(a) * -1)
        if 0 <= x < N and 0 <= y < N and y < 14: px[x, y] = GOLD[3 if t % 12 < 6 else 2] + (255,)
    g = gem(RIFT, facets=4, r=8, cx=16, cy=21, rot=math.pi / 4)
    def ring(x, y):
        d = math.hypot(x - 16, y - 21)
        if 7.5 < d < 10.5: return ((x - 16) / d, (y - 21) / d)
        return None
    r = new(); shade(r, ring, GOLD)
    img = Image.alpha_composite(img, r); img = Image.alpha_composite(img, g); return img

def lantern():
    img = new()
    def body(x, y):
        if 9 <= x <= 22 and 10 <= y <= 25: return ((x - 15.5) / 8, -0.2)
        return None
    glass = new(); px = glass.load()
    for y in range(12, 24):
        for x in range(11, 21):
            d = math.hypot(x - 15.5, y - 18)
            px[x, y] = ramp(TEAL, 0.95 - d / 9) + (255,)
    frame = new(); shade(frame, body, STEEL)
    fpx = frame.load()
    for y in range(12, 24):
        for x in range(11, 21): fpx[x, y] = (0, 0, 0, 0)
    for x in range(12, 20): fpx[x, 5] = STEEL[3] + (255,)
    for y in range(5, 10): fpx[12, y] = STEEL[2] + (255,); fpx[19, y] = STEEL[2] + (255,)
    img = Image.alpha_composite(glass, frame); spec(img, [(13, 14), (13, 15)], TEAL[5]); return img

def anklet():
    img = new()
    def band(x, y):
        dx, dy = (x - 16) / 12.0, (y - 18) / 7.0; d = math.hypot(dx, dy)
        if 0.72 < d < 1.0: return (dx / d, dy / d)
        return None
    shade(img, band, VOID)
    for cx, cy in ((7, 22), (16, 25), (25, 22)):
        g = gem(RIFT, facets=4, r=3, cx=cx, cy=cy, rot=math.pi / 4); img = Image.alpha_composite(img, g)
    return img

def band():
    img = new()
    def ring(x, y):
        dx, dy = (x - 16) / 11.0, (y - 18) / 9.0; d = math.hypot(dx, dy)
        if 0.62 < d < 1.0: return (dx / d, dy / d)
        return None
    shade(img, ring, STEEL)
    g = gem(RIFT, facets=6, r=6, cx=16, cy=9); img = Image.alpha_composite(img, g); return img


def glyph_sense():
    img = glyph(); px = img.load()
    for y in range(N):  # recolour the tear teal and add a compass needle
        for x in range(N):
            r, g, b, a = px[x, y]
            if a and r > g + 60: px[x, y] = ramp(TEAL, (r / 255.0)) + (255,)
    for i in range(-8, 9):
        px[16 + i // 3, 16 - i] = (GOLD[4] if i > 0 else STEEL[4]) + (255,)
    return img

def chain():
    img = new()
    for k, (cx, cy) in enumerate(((9, 9), (14, 14), (19, 19), (24, 24))):
        def link(x, y, cx=cx, cy=cy, k=k):
            dx, dy = x - cx, y - cy
            if k % 2: dx, dy = dy * 0.6 + dx * 0.4, dy
            d = math.hypot(dx / 4.5, dy / 3.0)
            if 0.55 < d < 1.0: return (dx / 5, dy / 5)
            return None
        l = new(); shade(l, link, STEEL); img = Image.alpha_composite(img, l)
    px = img.load()
    for x, y in ((26, 26), (27, 27), (27, 25), (25, 28)): px[x, y] = RIFT[4] + (255,)  # broken, glowing end
    return img

def plume():
    img = new()
    def f(x, y):
        u = (x - 16) - (y - 16) * 0.55; t = (y - 4) / 24.0
        if not (0 <= t <= 1): return None
        w = 6 * math.sin(math.pi * min(1, t * 1.15))
        if abs(u) > w: return None
        return (-0.7, -0.2) if u < 0 else (0.6, 0.3)
    shade(img, f, VOID)
    px = img.load()
    for y in range(6, 29): px[int(16 + (y - 16) * 0.55), y] = GOLD[4] + (255,)
    return img

def coin():
    img = new()
    def m(x, y):
        dx, dy = (x - 16) / 11.0, (y - 16) / 11.0; d = math.hypot(dx, dy)
        if d > 1: return None
        return (dx, dy) if d > 0.78 else (-0.3, -0.3)
    shade(img, m, GOLD); px = img.load()
    for i in range(-4, 5): px[16 + i // 2, 16 + i] = GOLD[1] + (255,); px[16 - i // 2 + 1, 16 + i] = GOLD[1] + (255,)
    spec(img, [(11, 9), (12, 9), (10, 10)]); return img

CLASS_PALS = {'bulwark': STEEL, 'shadowfang': VOID, 'arcanum': TEAL, 'hawkeye': [(10, 30, 10), (24, 70, 24), (40, 120, 40), (80, 176, 70), (150, 226, 120), (230, 255, 210)],
              'lifethread': [(30, 20, 10), (90, 60, 20), (170, 130, 40), (230, 200, 90), (255, 240, 170), (255, 255, 240)], 'bloodrage': RIFT, 'legion': GOLD,
              'phylactery': [(10, 20, 14), (20, 50, 34), (40, 90, 60), (90, 150, 110), (160, 210, 170), (230, 255, 236)]}

def save(img, name):
    p = os.path.join(OUT, name + '.png'); os.makedirs(os.path.dirname(p), exist_ok=True); img.save(p)

if __name__ == '__main__':
    save(shard(), 'rift_source_shard'); save(glyph(), 'rift_rend'); save(pendant(), 'rift_hunters_sigil')
    save(lantern(), 'rift_lantern'); save(anklet(), 'voidstep_anklet'); save(band(), 'gatebreaker_band')
    save(gem(RIFT, 6), 'gems/riftheart'); save(gem(VOID, 8, rot=0.2), 'gems/voidglass'); save(gem(GOLD, 5, rot=-math.pi / 2), 'gems/gatekeeper')
    print('ok')
    save(glyph_sense(), 'glyph_gatesense'); save(chain(), 'wardens_broken_chain'); save(plume(), 'guardians_plume'); save(coin(), 'hunters_lucky_coin')
    for i, (n, pal) in enumerate(CLASS_PALS.items()): save(gem(pal, (4, 5, 6, 7, 8)[i % 5], r=10, rot=i * 0.4), 'gems/' + n)

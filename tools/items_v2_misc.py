from gen_items_v2 import *

def disc_glyph(ramp_rim, ramp_face, emblem):
    """Ars-style glyph: bevelled rune disc (rim + recessed face) with an emblem."""
    s = Sprite()
    s.shape(ell(16, 16, 14.5, 14.5), ramp_rim, normal='dome')
    s.shape(ell(16, 16, 11, 11), ramp_face, normal=(0.25, 0.3, 1), bias=-0.1)  # recessed: lit from the opposite side
    for a in range(0, 360, 45):  # rim studs
        x = round(16 + 12.8 * math.cos(math.radians(a))); y = round(16 + 12.8 * math.sin(math.radians(a))); s.px([(x - 1, y - 1) if a in (0, 90) else (x - 1, y - 1)], ramp_rim, 5)
    emblem(s); return s.image()

def rift_rend():
    def em(s):
        pts = [(8, 18), (11, 14), (13, 17), (16, 11), (18, 16), (21, 12), (24, 15)]
        for p0, p1 in zip(pts, pts[1:]):
            s.line(p0, p1, 'rift', 3, w=2); s.line((p0[0], p0[1] - 1), (p1[0], p1[1] - 1), 'rift', 5)
        s.px([(16, 10), (15, 12)], 'rift', 5)
    return disc_glyph('void', 'night', em)

def gatesense():
    def em(s):
        s.shape(ell(16, 16, 7.5, 4.2), 'teal', normal='dome')       # the eye
        s.shape(ell(16, 16, 3, 3), 'gold', normal='dome'); s.px([(16, 16)], 'night', 1); s.px([(15, 15)], 'white', 5)
        s.line((16, 6), (16, 9), 'gold', 5); s.line((16, 23), (16, 26), 'steel', 3)   # compass needle N/S
        s.px([(15, 7), (17, 7)], 'gold', 4)
    return disc_glyph('teal', 'night', em)

def chain():
    """Warden's Broken Chain: two heavy rings joined by an edge-on link on a diagonal, the lower ring snapped with a glowing rift break."""
    s = Sprite()
    a = math.radians(45)
    def ring(cx, cy): return ell(cx, cy, 8.2, 5.6, a) & ~ell(cx, cy, 5.0, 2.6, a)
    s.shape(ring(9, 9), 'iron', normal='dome', bias=-0.05)
    low = ring(23, 23) & ~(((X - Y) ** 2 < 10) & (X + Y > 50))
    s.shape(low, 'iron', normal='dome', bias=-0.05)
    s.shape(ell(16, 16, 6.5, 2.2, a), 'iron', normal='dome', bias=0.15)
    s.px([(16, 16), (17, 17)], 'iron', 1)
    for x, y, t in ((26, 27, 5), (27, 26, 5), (25, 28, 4), (28, 25, 4), (28, 29, 3), (29, 28, 3), (30, 30, 4)): s.px([(x, y)], 'rift', t)
    for x, y in ((5, 4), (4, 6), (13, 12), (19, 18)): s.px([(x, y)], 'iron', 5)
    return s.image()

def rift_core():
    """Rift Core: octahedral void crystal caged in iron prongs, a burning rift heart."""
    s = Sprite()
    s.shape(poly([(16, 2), (27, 16), (16, 30), (5, 16)]), 'void', normal=facet_normals(16, 16, 4, rot=math.pi / 4, table=2.5))
    s.shape(ell(16, 16, 4.5, 4.5), 'rift', normal='dome', bias=0.1, rim=False)
    s.px([(15, 15), (16, 15), (15, 16)], 'rift', 5); s.px([(15, 14)], 'white', 5)
    for (x0, y0), (x1, y1) in (((16, 1), (16, 6)), ((16, 26), (16, 31)), ((3, 16), (8, 16)), ((24, 16), (29, 16))):
        s.line((x0, y0), (x1, y1), 'iron', 3)
    s.px([(16, 1), (3, 16)], 'iron', 5); s.px([(12, 6), (11, 7), (10, 9)], 'void', 5)
    return s.image()

def rift_essence():
    """Rift Essence: a swirling crimson-violet vortex wisp with a bright eye and trailing motes."""
    s = Sprite()
    s.shape(ell(16, 16, 11.5, 11.5), 'void', normal='dome', bias=-0.25)
    for arm in range(3):
        for k in range(26):
            t = k / 25; ang = arm * 2.094 + t * 4.2; r = 1.5 + t * 10
            x, y = round(16 + r * math.cos(ang)), round(16 + r * math.sin(ang))
            if s.idx[y, x] >= 0: s.px([(x, y)], 'rift', 5 if t < .25 else (4 if t < .6 else 2))
    s.px([(16, 16), (15, 16), (16, 15)], 'white', 5)
    for x, y, tn in ((4, 6, 4), (27, 26, 4), (28, 7, 3), (5, 27, 3)): s.px([(x, y)], 'rift', tn)
    return s.image()

def plume():
    """Guardian's Plume: a curved violet crest feather with barbs, gold quill set in a small helm socket."""
    s = Sprite()
    up = [(27, 3), (29, 8), (28, 14), (25, 20), (20, 25), (14, 28), (10, 28)]
    lo = [(10, 28), (8, 24), (9, 18), (12, 12), (17, 7), (22, 4)]
    vane = poly(up + lo)
    s.shape(vane, 'void', normal='dome')
    for i in range(6):  # barb grooves, parallel, lit consistently
        x0 = 24 - i * 2.8; y0 = 7 + i * 3.2
        s.line((round(x0 - 1), round(y0)), (round(x0 - 6), round(y0 - 2)), 'void', 2)
        s.line((round(x0 + 1), round(y0 + 1)), (round(x0 + 5), round(y0 + 3)), 'void', 2)
    s.line((26, 5), (8, 28), 'gold', 4); s.line((25, 6), (10, 26), 'gold', 5)
    s.shape(ell(6.5, 27, 3.2, 2.4, -0.7), 'steel', normal='dome')
    s.px([(24, 6), (25, 7)], 'void', 5); return s.image()

def coin():
    """Hunter's Lucky Coin: thick gold coin seen at a slight tilt, milled rim, embossed rift gate, edge visible below."""
    s = Sprite()
    s.shape(ell(16, 18, 13, 12), 'bronze', normal=(0, 0.6, 0.8))           # edge thickness
    s.shape(ell(16, 16, 13, 12), 'gold', normal=(-0.2, -0.25, 1), bias=0.1)
    s.shape(ell(16, 16, 10, 9), 'gold', normal=(0.3, 0.35, 1), bias=-0.12)   # recessed field
    gate = poly([(12, 22), (12, 13), (14, 10), (18, 10), (20, 13), (20, 22)]) & ~poly([(14, 22), (14, 14), (15, 12), (17, 12), (18, 14), (18, 22)])
    s.shape(gate, 'gold', normal='dome', bias=0.1)
    s.shape(poly([(15, 21), (15, 15), (16, 13), (17, 15), (17, 21)]), 'void', normal=(0, 0, 1))
    s.px([(16, 16)], 'void', 5); s.px([(9, 10), (10, 9), (11, 8)], 'gold', 5)
    return s.image()

def shard():
    s = Sprite()
    s.shape(poly([(17, 2), (23, 9), (20, 28), (14, 30), (11, 14)]), 'void', normal=facet_normals(17, 14, 4, rot=0.3))
    s.shape(poly([(8, 17), (12, 21), (11, 29), (7, 30), (5, 23)]), 'rift', normal=facet_normals(9, 24, 3, rot=0.5))
    s.shape(poly([(24, 19), (27, 22), (26, 28), (22, 29)]), 'void', normal=facet_normals(25, 24, 3))
    s.line((17, 4), (15, 26), 'void', 5); s.px([(16, 6), (16, 7)], 'white', 5); return s.image()

def sigil():
    """Rift Hunter's Sigil: gold medallion on a chain, crimson rift gem, engraved rays."""
    s = Sprite()
    for i in range(14):
        a = math.pi * (0.1 + 0.8 * i / 13); x = round(16 - 11 * math.cos(a)); y = round(11 - 8 * math.sin(a))
        s.px([(x, y)], 'gold', 4 if i % 2 else 2)
    s.shape(poly([(16, 9), (25, 14), (25, 22), (16, 29), (7, 22), (7, 14)]), 'gold', normal=facet_normals(16, 19, 6, rot=-math.pi / 2))
    s.shape(ell(16, 19, 4.6, 4.6), 'rift', normal=facet_normals(16, 19, 6, table=1.8))
    s.px([(14, 17), (15, 17)], 'rift', 5); s.px([(10, 14), (11, 13)], 'gold', 5); return s.image()

def lantern():
    s = Sprite()
    s.shape(poly([(13, 2), (19, 2), (19, 4), (13, 4)]) | (ell(16, 3, 3, 2.5) & ~ell(16, 3, 1.5, 1.2)), 'iron', normal='dome')
    s.shape(poly([(9, 7), (23, 7), (24, 9), (8, 9)]), 'iron', normal='dome')
    s.shape(poly([(10, 9), (22, 9), (22, 25), (10, 25)]), 'teal', normal=(0, 0, 1))
    glow = np.hypot(X - 16, Y - 17); s.idx[(glow < 6) & (s.idx >= 0) & (s.ramp == 'teal')] = 4; s.idx[(glow < 3) & (s.ramp == 'teal')] = 5
    for x in (10, 16, 22): s.shape(poly([(x - 1, 9), (x + 1, 9), (x + 1, 25), (x - 1, 25)]), 'iron', normal='dome')
    s.shape(poly([(8, 25), (24, 25), (23, 28), (9, 28)]), 'iron', normal='dome')
    s.shape(poly([(15, 14), (17, 14), (18, 18), (16, 21), (14, 18)]), 'white', normal=(0, 0, 1))
    return s.image()

def anklet():
    s = Sprite(); band = ell(16, 17, 13, 7) & ~ell(16, 17, 10, 4.4)
    s.shape(band, 'void', normal='dome')
    for cx, cy in ((7, 21), (16, 24), (25, 21)):
        s.shape(poly([(cx, cy - 3), (cx + 3, cy), (cx, cy + 3), (cx - 3, cy)]), 'rift', normal=facet_normals(cx, cy, 4, rot=math.pi / 4))
        s.px([(cx - 1, cy - 1)], 'rift', 5)
    s.line((8, 12), (13, 10), 'void', 5); return s.image()

def band():
    s = Sprite(); ring = ell(16, 20, 11, 9) & ~ell(16, 20, 7.5, 5.8)
    s.shape(ring, 'steel', normal='dome')
    s.shape(poly([(11, 10), (21, 10), (23, 13), (9, 13)]), 'steel', normal='dome')
    s.shape(poly(([(16 + 6 * math.cos(a), 8 + 5 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 9)[:-1]])), 'rift', normal=facet_normals(16, 8, 8, table=2))
    s.px([(14, 6), (15, 6)], 'rift', 5); s.line((8, 17), (10, 14), 'steel', 5); return s.image()

def nexus_token():
    """Nexus Token: gold-rimmed void medallion with a glowing rift eye, faceted bezel."""
    s = Sprite()
    s.shape(ell(16, 17, 13.5, 13.5), 'bronze', normal=(0, 0.6, 0.8))
    s.shape(ell(16, 16, 13.5, 13.5), 'gold', normal='dome', bias=0.1)
    s.shape(ell(16, 16, 10.5, 10.5), 'void', normal=(0.3, 0.35, 1), bias=-0.15)
    s.shape(ell(16, 16, 7.5, 4.6), 'void', normal='dome', bias=0.25)
    s.shape(ell(16, 16, 4.2, 3.2), 'pink', normal='dome', bias=0.2)
    s.px([(15, 15), (16, 15), (15, 16), (16, 16), (17, 16)], 'white', 5)
    for i in range(8):
        a = math.pi / 4 * i; x = round(16 + 12 * math.cos(a)); y = round(16 + 12 * math.sin(a))
        s.px([(x, y)], 'gold', 5 if (math.cos(a) < 0 and math.sin(a) < 0.1) else 2)
    s.px([(8, 7), (9, 6), (10, 6)], 'gold', 5)
    return s.image()

def void_orb():
    """Void Orb: dark glassy sphere, glowing rift core, two-arm spiral, hard rim light top-left."""
    s = Sprite()
    s.shape(ell(16, 16, 12.5, 12.5), 'void', normal='dome', bias=-0.3)
    s.shape(ell(17, 17, 6, 6), 'pink', normal='dome', bias=-0.1, rim=False)
    s.shape(ell(17, 17, 3, 3), 'pink', normal='dome', bias=0.35, rim=False, shadow=False)
    for arm in (0, math.pi):
        pts = []
        for k in range(30):
            t = k / 29; a = arm + 0.3 + t * 3.6; r = 3 + t * 8
            pts.append((round(17 + r * math.cos(a)), round(17 + r * math.sin(a)) - (1 if t > .5 else 0)))
        for q, pt in enumerate(pts):
            if 0 <= pt[0] < 32 and 0 <= pt[1] < 32 and s.idx[pt[1], pt[0]] >= 0:
                s.px([pt], 'void' if q > 18 else 'pink', 4 if q > 18 else 3)
    s.px([(16, 16), (17, 16), (16, 17)], 'white', 5)
    s.px([(9, 9), (10, 8), (11, 8), (9, 10), (8, 11)], 'void', 5); s.px([(10, 9)], 'white', 5)
    return s.image()

ITEMS = dict(rift_core=rift_core, rift_essence=rift_essence, nexus_token=nexus_token, void_orb=void_orb, rift_rend=rift_rend, glyph_gatesense=gatesense, wardens_broken_chain=chain, guardians_plume=plume, hunters_lucky_coin=coin,
             rift_source_shard=shard, rift_hunters_sigil=sigil, rift_lantern=lantern, voidstep_anklet=anklet, gatebreaker_band=band)
if __name__ == '__main__':
    for k, f in ITEMS.items(): save(f(), k)

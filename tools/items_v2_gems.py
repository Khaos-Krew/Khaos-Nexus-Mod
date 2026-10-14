from gen_items_v2 import *

def gem_cut(outline_pts, ramp, cx, cy, facets, rot=0.0, table=4.0, spec=None, extra=None):
    s = Sprite(); m = poly(outline_pts)
    s.shape(m, ramp, normal=facet_normals(cx, cy, facets, rot, table))
    a = np.arctan2(Y - cy, X - cx) - rot; seg = np.floor(a / (2 * math.pi / facets)); d0 = np.hypot(X - cx, Y - cy)
    edge = m & ((seg != np.roll(seg, 1, 1)) | (seg != np.roll(seg, 1, 0))) & (d0 > table + 0.8)
    s.idx[edge] = np.where((X - cx + Y - cy)[edge] < 0, np.minimum(s.idx[edge] + 1, 5), np.maximum(s.idx[edge] - 1, 1))
    if table:
        tb = m & (d0 >= table - 0.5) & (d0 < table + 0.6); s.idx[tb] = np.where((X - cx + Y - cy)[tb] > 0, 5, 2)
    # table rim: brighten the ring just outside the table on the lit half
    d = np.hypot(X - cx, Y - cy); ring = m & (d >= table) & (d < table + 1.1) & ((X - cx) + (Y - cy) < 0)
    s.idx[ring] = np.minimum(s.idx[ring] + 1, 5)
    if extra: extra(s)
    s.px(spec or [(int(cx - table * 0.5), int(cy - table * 0.6)), (int(cx - table * 0.5) + 1, int(cy - table * 0.6))], ramp, 5)
    return s.image()

def ngon(cx, cy, r, n, rot=0.0, sx=1.0, sy=1.0):
    return [(cx + r * sx * math.cos(rot + 2 * math.pi * i / n), cy + r * sy * math.sin(rot + 2 * math.pi * i / n)) for i in range(n)]

def bulwark():   # tank: heater-shield cut, steel-blue, with a central boss stud
    pts = [(6, 5), (26, 5), (26, 15), (23, 22), (16, 28), (9, 22), (6, 15)]
    def ex(s):
        s.shape(ell(16, 14, 3.2, 3.2), 'white', normal='dome')
        s.line((16, 6), (16, 9), 'cyan', 5); s.line((16, 19), (16, 26), 'cyan', 2)
    return gem_cut(pts, 'cyan', 16, 14, 7, rot=-math.pi / 2, table=0, extra=ex, spec=[(9, 8), (10, 8), (9, 9)])

def shadowfang():  # assassin: curved fang
    pts = [(6, 3), (13, 4), (20, 8), (25, 14), (27, 21), (26, 29), (21, 25), (15, 21), (10, 15), (7, 9)]
    return gem_cut(pts, 'void', 15, 13, 5, rot=0.6, table=0, spec=[(12, 7), (13, 8), (14, 9)],
                   extra=lambda s: s.line((13, 7), (20, 20), 'void', 4))

def arcanum():   # caster: tall hexagonal prism with an inner rune
    pts = ngon(16, 16, 14, 6, rot=-math.pi / 2, sx=0.86)
    def ex(s):
        s.line((16, 11), (16, 21), 'white', 5); s.line((12, 14), (20, 18), 'teal', 5); s.line((12, 18), (20, 14), 'teal', 4)
    return gem_cut(pts, 'teal', 16, 16, 6, rot=-math.pi / 2, table=5.5, extra=ex)

def hawkeye():   # ranger: marquise (eye) cut with an iris
    m_pts = [(3, 16), (8, 10), (16, 7), (24, 10), (29, 16), (24, 22), (16, 25), (8, 22)]
    def ex(s):
        s.shape(ell(16, 16, 4.5, 4.5), 'amber', normal='dome'); s.shape(ell(16, 16, 1.8, 2.6), 'night', normal=(0, 0, 1))
        s.px([(14, 14)], 'white', 5)
    return gem_cut(m_pts, 'green', 16, 16, 8, table=0, extra=ex, spec=[(9, 12), (10, 11), (11, 11)])

def lifethread():  # lifebinder: heart cut, warm gold-white, with a thread
    t = np.linspace(0, 2 * math.pi, 40)
    pts = [(16 + 11.5 * (math.sin(a) ** 3), 15 - (10 * math.cos(a) - 4 * math.cos(2 * a) - 1.6 * math.cos(3 * a) - 0.6 * math.cos(4 * a))) for a in t]
    def ex(s):
        s.line((6, 12), (26, 18), 'white', 4); s.line((7, 13), (25, 19), 'white', 2)
    return gem_cut(pts, 'pink', 16, 14, 8, table=4.5, extra=ex, spec=[(10, 9), (11, 9), (10, 10)])

def bloodrage():  # berserker: jagged burst
    pts = []
    for i in range(16):
        r = 13 if i % 2 == 0 else 6.5; a = -math.pi / 2 + i * math.pi / 8
        pts.append((16 + r * math.cos(a), 16.5 + r * math.sin(a)))
    def ex(s): s.shape(ell(16, 16.5, 3.5, 3.5), 'amber', normal='dome')
    return gem_cut(pts, 'blood', 16, 16.5, 8, rot=-math.pi / 2, table=0, extra=ex, spec=[(14, 14), (15, 14)])

def legion():   # summoner: three linked triangle gems
    s = Sprite()
    for cx, cy, r in ((16, 10, 8), (9, 22, 7), (23, 22, 7)):
        s.shape(poly(ngon(cx, cy, r, 3, rot=-math.pi / 2)), 'amber', normal=facet_normals(cx, cy, 3, rot=-math.pi / 2, table=2.3))
    for cx, cy in ((16, 11), (9, 23), (23, 23)): s.px([(cx - 1, cy - 1)], 'amber', 5)
    s.line((12, 17), (13, 18), 'gold', 4); s.line((20, 17), (19, 18), 'gold', 4); s.line((14, 23), (17, 23), 'gold', 4)
    return s.image()

def phylactery():  # necromancer: coffin cut, sickly green with a skull mark
    pts = [(12, 3), (20, 3), (25, 11), (22, 29), (10, 29), (7, 11)]
    def ex(s):
        s.shape(ell(16, 14, 4, 3.6), 'bone', normal='dome'); s.shape(poly([(14, 17), (18, 17), (17.5, 20), (14.5, 20)]), 'bone', normal=(0, -0.3, 1))
        s.px([(14, 14), (18, 14)], 'night', 1); s.px([(15, 14), (17, 14)], 'sick', 4)
    return gem_cut(pts, 'sick', 16, 15, 6, rot=-math.pi / 2, table=0, extra=ex, spec=[(10, 7), (11, 6), (10, 8)])

def riftheart():  # 1.0: crimson round brilliant
    return gem_cut(ngon(16, 16, 13.5, 8, rot=math.pi / 8), 'rift', 16, 16, 8, rot=math.pi / 8, table=5.5)
def voidglass():  # 1.0: violet emerald (step) cut
    s = Sprite(); m = poly([(9, 4), (23, 4), (28, 9), (28, 23), (23, 28), (9, 28), (4, 23), (4, 9)])
    s.shape(m, 'void', normal=facet_normals(16, 16, 8, rot=math.pi / 8, table=0))
    inner = poly([(11, 8), (21, 8), (24, 11), (24, 21), (21, 24), (11, 24), (8, 21), (8, 11)]); s.shape(inner, 'void', normal=(-0.1, -0.15, 1), bias=0.05)
    s.shape(poly([(13, 12), (19, 12), (20, 13), (20, 19), (19, 20), (13, 20), (12, 19), (12, 13)]), 'void', normal=(0.2, 0.25, 1), bias=-0.15)
    s.px([(10, 6), (11, 6), (12, 6), (9, 7)], 'void', 5); return s.image()
def gatekeeper():  # 1.0: gold trillion with a keyhole
    def ex(s):
        s.shape(ell(16, 15, 2.2, 2.2), 'night', normal=(0, 0, 1)); s.shape(poly([(15, 16), (17, 16), (18, 21), (14, 21)]), 'night', normal=(0, 0, 1))
    return gem_cut(ngon(16, 17, 13, 3, rot=-math.pi / 2), 'gold', 16, 17, 6, rot=-math.pi / 2, table=0, extra=ex, spec=[(12, 11), (13, 10)])

GEMS = dict(bulwark=bulwark, shadowfang=shadowfang, arcanum=arcanum, hawkeye=hawkeye, lifethread=lifethread, bloodrage=bloodrage,
            legion=legion, phylactery=phylactery, riftheart=riftheart, voidglass=voidglass, gatekeeper=gatekeeper)
if __name__ == '__main__':
    for k, f in GEMS.items(): save(f(), 'gems/' + k)

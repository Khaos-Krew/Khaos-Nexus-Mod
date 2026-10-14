from gen_items_v2 import *
RAMPS['rankE'] = R('#1a1a1e', '#46464e', '#77777f', '#a9a9ae', '#d6d6d8', '#ffffff')
RAMPS['rankD'] = RAMPS['green']; RAMPS['rankC'] = RAMPS['cyan']; RAMPS['rankB'] = RAMPS['void']; RAMPS['rankA'] = RAMPS['amber']; RAMPS['rankS'] = RAMPS['rift']
RAMPS['moncov'] = R('#06040c', '#140c22', '#22143a', '#321e54', '#4a2e78', '#6e4aa8')
RAMPS['blue_leather'] = R('#0a0c1a', '#18203e', '#243466', '#344c8e', '#5070b4', '#86a4dc')
RAMPS['plum'] = R('#12060e', '#32122a', '#521e46', '#743064', '#9a4a86', '#c87cb4')
RAMPS['oxblood'] = R('#120406', '#360c12', '#5a141e', '#7e1e2a', '#a8323c', '#d46a66')
RAMPS['forest'] = R('#06100a', '#122a1a', '#1e442a', '#2c623c', '#428456', '#70b07e')

# cover ramp, trim ramp, gem ramp, ornament level 0..6
TIERS = {
 'e': ('leather', 'iron', 'rankE', 0), 'd': ('forest', 'bronze', 'rankD', 1), 'c': ('blue_leather', 'steel', 'rankC', 2),
 'b': ('plum', 'gold', 'rankB', 3), 'a': ('oxblood', 'gold', 'rankA', 4), 's': ('night', 'gold', 'rankS', 5), 'monarch': ('moncov', 'white', 'void', 6)}

def tome(cover, trim, gem, lvl):
    s = Sprite()
    # page block (right & bottom edge visible, 3/4 view) then cover
    pages = poly([(9, 6), (27, 4), (27, 25), (9, 29)]); s.shape(pages, 'paper', normal=(0.6, 0.3, 0.75))
    for y in range(7, 27, 2): s.px([(26, y)], 'paper', 2)
    cov = poly([(5, 5), (24, 3), (24, 25), (5, 28)]); s.shape(cov, cover, normal=(-0.15, -0.2, 1), bias=0.04)
    spine = poly([(3, 6), (6, 5), (6, 28), (3, 28)]); s.shape(spine, cover, normal=(-0.9, -0.1, 0.4))
    for y in (9, 15, 21): s.line((3, y), (5, y), trim, 4)                   # spine bands
    # bevel: inner frame line on the cover
    frame = poly([(8, 7), (22, 5.5), (22, 23.5), (8, 25.5)]) & ~poly([(9, 8), (21, 6.5), (21, 22.5), (9, 24.5)])
    if lvl >= 1: s.shape(frame, trim, normal=(-0.2, -0.2, 1), rim=False, shadow=False)
    if lvl >= 1:  # metal corners
        for pts in ([(5, 5), (10, 4.5), (5, 10)], [(24, 3), (19, 3.5), (24, 8)], [(5, 28), (5, 23), (10, 27.5)], [(24, 25), (24, 20), (19, 25.6)]):
            s.shape(poly(pts), trim, normal='dome')
    if lvl >= 2:  # clasp on the open edge
        s.shape(poly([(22, 12), (27, 12), (27, 16), (22, 16)]), trim, normal='dome'); s.px([(26, 13)], trim, 5)
    cx, cy = 15, 15
    r = 3.2 + 0.5 * min(lvl, 4)
    if lvl >= 3:  # sunburst setting
        for a in range(0, 360, 45):
            x = round(cx + (r + 2.2) * math.cos(math.radians(a))); y = round(cy + (r + 2.2) * math.sin(math.radians(a))); s.px([(x, y)], trim, 4 if a in (180, 225, 270) else 2)
    s.shape(poly([(cx + r * math.cos(a), cy + r * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 9)[:-1] + math.pi / 8]), gem,
            normal=facet_normals(cx, cy, 8, rot=math.pi / 8, table=r * 0.45))
    s.px([(cx - 1, cy - 2), (cx - 2, cy - 1)], gem, 5)
    if lvl >= 4:  # glowing runes on the cover
        for x, y in ((9, 21), (11, 21), (13, 20), (19, 9), (17, 8), (20, 19)): s.px([(x, y)], gem, 4)
    if lvl >= 5:  # S: crown of rift shards over the top edge
        for x, h in ((8, 3), (13, 4), (18, 3)): s.shape(poly([(x, 4.5), (x + 1.5, 4.5 - h), (x + 3, 4.5)]), gem, normal=facet_normals(x + 1.5, 3, 2))
    if lvl >= 6:  # Monarch: void flames along the spine + twin eyes
        for y in range(8, 27, 3): s.px([(2, y), (1, y + 1)], 'void', 4)
        s.px([(12, 11), (18, 10)], 'void', 5)
    return s.image()

if __name__ == '__main__':
    for k, (c, t, g, l) in TIERS.items(): save(tome(c, t, g, l), 'hunter_tome_' + k)

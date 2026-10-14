"""1.2 saga items (32x32 house style, same light/ramps/outline rules as gen_items_v2)."""
from gen_items_v2 import *

def crown_shard():
    """A jagged violet crystal shard broken off a crown: a gilded band fragment with one tine clamps its base."""
    s = Sprite()
    s.shape(poly([(17, 2), (23, 9), (21, 20), (14, 25), (10, 18), (12, 8)]), 'void', normal='facet' and facet_normals(16, 13, 6, 0.3))
    s.shape(poly([(17, 2), (19, 9), (16, 20), (14, 25), (12, 8)]), 'pink', normal=(-0.45, -0.5, 0.8), bias=0.05)  # lit facet
    s.shape(poly([(24, 13), (28, 17), (25, 22), (21, 20)]), 'void', normal=(0.5, -0.2, 0.8))                       # side splinter
    s.shape(poly([(7, 20), (25, 22), (24, 27), (8, 26)]), 'gold', normal='dome')                                     # band fragment
    s.shape(poly([(9, 20), (11, 14), (13, 20)]), 'gold', normal='dome')                                              # broken tine
    for x in (11, 16, 21): s.shape(ell(x, 24, 1.4, 1.4), 'rift', normal='dome')                                       # set gems
    s.line((15, 5), (14, 14), 'white', 5); s.px([(16, 4), (13, 16)], 'white', 4)                                    # specular edge
    s.px([(22, 11), (20, 17)], 'void', 5)
    for x, y in ((26, 5), (5, 12), (28, 26)): s.px([(x, y)], 'pink', 5)                                             # sparkles
    s.px([(9, 23), (23, 25)], 'gold', 5)
    return s.image()

if __name__ == '__main__':
    save(crown_shard(), 'crown_shard')

def choir_mask():
    """Vaelith's Choir Mask: a pale glass face caught mid-song, crowned with crystal spines, one cheek cracked and glowing."""
    s = Sprite()
    for x0, top, w in ((8, 4, 3.2), (12, 1, 3.4), (16, 0, 3.8), (20, 1, 3.4), (24, 4, 3.2)):   # crown spines (behind)
        s.shape(poly([(x0 - w / 2, 12), (x0, top), (x0 + w / 2, 12)]), 'cyan', normal=(-0.4, -0.4, 0.8))
        s.px([(round(x0 - 0.5), top + 2)], 'white', 5)
    s.shape(ell(16, 19.5, 10, 11.5), 'white', normal='dome')                                    # the face
    s.shape(ell(11.5, 17, 2.6, 1.6, -0.25) | ell(20.5, 17, 2.6, 1.6, 0.25), 'night', normal=(0, 0, 1), rim=False)   # eyes
    s.shape(ell(16, 25.5, 2.4, 3.0), 'night', normal=(0, 0, 1), rim=False)                      # singing mouth
    s.px([(16, 27), (15, 26)], 'cyan', 3)                                                         # glow inside the mouth
    s.px([(11, 17), (20, 17)], 'cyan', 5); s.px([(12, 17), (21, 17)], 'cyan', 4)                  # eye light
    for p0, p1 in (((22, 9), (24, 14)), ((24, 14), (23, 19)), ((23, 19), (26, 23))):               # glowing crack
        s.line(p0, p1, 'cyan', 4)
    s.px([(24, 13), (24, 18)], 'white', 5)
    s.line((9, 9), (8, 18), 'white', 5); s.px([(10, 8), (12, 7)], 'white', 5)                     # rim light
    for x, y in ((4, 6), (28, 8), (5, 27)): s.px([(x, y)], 'cyan', 5)
    return s.image()

def singing_glass():
    """Singing Glass: three resonant crystal prisms fused on a dark root, ringed by faint sound arcs."""
    s = Sprite()
    s.shape(ell(16, 26, 9, 3.5), 'night', normal='dome')
    s.shape(poly([(10, 26), (8, 13), (11, 8), (14, 14), (14, 26)]), 'cyan', normal=facet_normals(11, 18, 4, 0.2))
    s.shape(poly([(19, 26), (19, 10), (22, 5), (25, 11), (23, 26)]), 'cyan', normal=facet_normals(22, 16, 4, 0.5))
    s.shape(poly([(13, 26), (13, 8), (16, 2), (19, 8), (19, 26)]), 'teal', normal=facet_normals(16, 14, 5, 0.0))
    s.line((15, 5), (14, 22), 'white', 5); s.line((21, 8), (20, 20), 'white', 4); s.line((10, 11), (9, 18), 'white', 4)
    for x, y in ((5, 9), (27, 6), (6, 20), (27, 18), (16, 0)): s.px([(x, y)], 'cyan', 5)   # resonance glints
    for x, y in ((4, 10), (6, 10), (5, 8), (5, 11)): s.px([(x, y)], 'cyan', 3)
    return s.image()

def censer():
    """The Hierophant's Censer: a pierced gold censer on its chain, purple ash-light leaking through the slits."""
    s = Sprite()
    for i, y in enumerate(range(0, 9, 3)):   # chain: alternating face-on / edge-on links
        if i % 2 == 0: s.px([(15, y), (17, y), (15, y + 1), (17, y + 1), (16, y + 2), (16, y - 1 if y else 0)], 'iron', 4)
        else: s.px([(16, y), (16, y + 1), (16, y + 2)], 'iron', 3)
    s.shape(poly([(12, 9), (20, 9), (22, 12), (10, 12)]), 'gold', normal='dome')                   # cap
    s.shape(ell(16, 19, 9, 8.5), 'gold', normal='dome')                                            # bowl
    s.shape(poly([(13, 27), (19, 27), (21, 30), (11, 30)]), 'gold', normal='dome')                 # foot
    for x in (11, 14, 18, 21):                                                                      # glowing slits
        s.line((x, 15), (x, 22 - abs(x - 16) // 3), 'void', 4)
        s.px([(x, 17), (x, 18)], 'pink', 5)
    s.line((8, 19), (24, 19), 'bronze', 1)                                                          # band
    for x in (10, 16, 22): s.px([(x, 19)], 'rift', 4)
    for x, y, t in ((24, 9, 3), (26, 6, 2), (23, 4, 3), (9, 7, 2)): s.px([(x, y)], 'void', t + 1)  # ash wisps
    s.px([(12, 14), (13, 13)], 'gold', 5)
    return s.image()

def profane_sigil():
    """Profane Sigil: a blackened iron seal burning with the Hierophant's broken-crown mark."""
    from items_v2_misc import disc_glyph
    def em(s):
        crown = [(9, 20), (11, 12), (14, 16), (16, 9), (18, 16), (21, 12), (23, 20)]
        for p0, p1 in zip(crown, crown[1:]): s.line(p0, p1, 'void', 4, w=2)
        s.line((9, 21), (23, 21), 'void', 4); s.line((10, 23), (22, 23), 'void', 3)
        s.line((16, 9), (17, 24), 'night', 1)   # the crack through the crown
        for x, y in ((16, 8), (11, 11), (21, 11)): s.px([(x, y)], 'pink', 5)
    return disc_glyph('iron', 'night', em)

def egg(base, spot, glow):
    s = Sprite(); s.shape(ell(16, 17, 10, 13), base, normal='dome')
    for x, y, r in ((12, 10, 2.2), (20, 14, 2.6), (13, 21, 2.4), (21, 23, 1.8), (16, 27, 1.6)): s.shape(ell(x, y, r, r * 0.9), spot, normal='dome')
    s.px([(11, 7), (10, 9)], base, 5); s.px([(20, 13), (13, 20)], glow, 5)
    return s.image()

if __name__ == '__main__':
    save(choir_mask(), 'vaelith_choir_mask'); save(singing_glass(), 'singing_glass')
    save(censer(), 'hierophant_censer'); save(profane_sigil(), 'profane_sigil')
    save(egg('cyan', 'white', 'cyan'), 'vaelith_spawn_egg'); save(egg('blood', 'void', 'pink'), 'fallen_hierophant_spawn_egg')

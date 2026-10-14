"""1.2 items, part 2 (32x32 house style): Wisp Bell, the Glass Choir and Profane Vestment armor pieces, General spawn eggs."""
from gen_items_v2 import *
from items_v3_saga import egg

def wisp_bell():
    """Wisp Bell: a small silver handbell with a crown-shard clapper; a wisp of violet light curls from its mouth."""
    s = Sprite()
    s.shape(poly([(14, 3), (18, 3), (18, 9), (14, 9)]), 'steel', normal='dome')                      # handle
    s.shape(ell(16, 3, 2.6, 2.0), 'void', normal='dome')                                               # handle gem
    s.shape(poly([(12, 9), (20, 9), (22, 15), (24, 22), (8, 22), (10, 15)]), 'steel', normal='dome')   # bell body
    s.shape(poly([(6, 22), (26, 22), (25, 25), (7, 25)]), 'white', normal='dome')                      # lip
    s.shape(poly([(15, 25), (17, 25), (18, 28), (16, 30), (14, 28)]), 'void', normal=facet_normals(16, 27, 4, 0.3))  # clapper shard
    s.line((11, 11), (9, 20), 'white', 5); s.px([(12, 10)], 'white', 5)                                # specular
    s.line((10, 18), (22, 18), 'gold', 3); 
    for x in (12, 16, 20): s.px([(x, 18)], 'void', 4)
    for x, y, t in ((22, 27, 4), (24, 28, 3), (26, 27, 4), (27, 25, 5), (25, 30, 3), (8, 28, 3)): s.px([(x, y)], 'pink', t)
    return s.image()

# ---------------------------------------------------------------- Glass Choir (Vaelith): pale steel, glass spines, cyan heart
def choir_helmet():
    s = Sprite()
    for x0, top, w in ((9, 6, 2.4), (12.5, 3, 2.6), (16, 1, 3.0), (19.5, 3, 2.6), (23, 6, 2.4)):        # crystal crown
        s.shape(poly([(x0 - w / 2, 12), (x0, top), (x0 + w / 2, 12)]), 'cyan', normal=(-0.4, -0.4, 0.8)); s.px([(round(x0 - 0.5), top + 2)], 'white', 5)
    s.shape(poly([(7, 11), (25, 11), (26, 20), (23, 27), (9, 27), (6, 20)]), 'steel', normal='dome')     # cap
    s.shape(poly([(9, 15), (23, 15), (22, 24), (16, 27), (10, 24)]), 'white', normal='dome')           # mask
    s.line((10, 18), (14, 18), 'night', 1); s.line((18, 18), (22, 18), 'night', 1)                       # eye slits
    s.px([(11, 18), (20, 18)], 'cyan', 5); s.shape(ell(16, 23.5, 1.4, 1.6), 'night', normal=(0, 0, 1), rim=False)
    s.line((7, 12), (25, 12), 'gold', 4)
    for x in (4, 28): s.shape(poly([(x, 13), (x + (3 if x < 16 else -3), 17), (x, 22)]), 'cyan', normal=(0, -0.3, 0.9))  # fins
    s.line((8, 14), (7, 20), 'white', 5)
    return s.image()

def choir_chestplate():
    s = Sprite()
    s.shape(poly([(4, 6), (11, 4), (16, 6), (21, 4), (28, 6), (28, 13), (24, 13), (24, 28), (8, 28), (8, 13), (4, 13)]), 'steel', normal='dome')
    s.shape(poly([(9, 8), (23, 8), (22, 19), (16, 22), (10, 19)]), 'white', normal='dome')            # breastplate
    for y in (22, 24, 26): s.line((9, y), (23, y), 'steel', 2)
    s.shape(poly([(16, 10), (19, 14), (16, 18), (13, 14)]), 'cyan', normal=facet_normals(16, 14, 4, 0.785))   # heart crystal
    s.px([(15, 12)], 'white', 5)
    s.line((10, 6), (22, 6), 'gold', 4); s.line((4, 6), (4, 12), 'white', 4)
    for x in (5, 26): s.shape(poly([(x, 3), (x + 2, 0), (x + 3, 5)]), 'cyan', normal=(-0.3, -0.5, 0.8))
    s.line((10, 9), (10, 17), 'white', 5)
    return s.image()

def choir_leggings():
    s = Sprite()
    s.shape(poly([(7, 4), (25, 4), (25, 9), (7, 9)]), 'steel', normal='dome')                         # belt
    s.shape(poly([(7, 9), (15, 9), (14, 29), (8, 29)]), 'white', normal='dome'); s.shape(poly([(17, 9), (25, 9), (24, 29), (18, 29)]), 'white', normal='dome')
    for x in (11, 21): s.shape(poly([(x - 2, 17), (x + 2, 17), (x + 2, 21), (x - 2, 21)]), 'steel', normal='dome'); s.px([(x, 19)], 'cyan', 5)
    s.line((7, 6), (25, 6), 'gold', 4); s.px([(12, 6), (20, 6)], 'cyan', 5)
    s.line((8, 10), (8, 27), 'steel', 5); s.line((18, 10), (18, 27), 'steel', 5)
    return s.image()

def choir_boots():
    s = Sprite()
    for x0 in (4, 17):
        s.shape(poly([(x0 + 2, 9), (x0 + 9, 9), (x0 + 9, 22), (x0 + 11, 26), (x0, 26), (x0 + 1, 21)]), 'white', normal='dome')
        s.shape(poly([(x0 + 1, 20), (x0 + 10, 20), (x0 + 11, 26), (x0, 26)]), 'steel', normal='dome')
        s.line((x0 + 2, 10), (x0 + 9, 10), 'gold', 4)
        s.shape(poly([(x0 + 9, 13), (x0 + 12, 9), (x0 + 11, 15)]), 'cyan', normal=(0.2, -0.5, 0.8))      # glass spur
        s.line((x0 + 3, 12), (x0 + 3, 19), 'white', 5)
    return s.image()

# ---------------------------------------------------------------- Profane Vestments (Hierophant): maroon velvet, gold, violet sigil
def vestment_helmet():
    s = Sprite()
    s.shape(poly([(11, 2), (16, 6), (21, 2), (22, 11), (10, 11)]), 'paper', normal='dome')            # mitre leaves
    s.line((16, 4), (16, 11), 'gold', 4); s.shape(poly([(16, 6), (18, 8), (16, 10), (14, 8)]), 'void', normal='dome'); s.px([(16, 8)], 'pink', 5)
    s.shape(ell(16, 20, 10.5, 9.5) | poly([(9, 14), (16, 9), (23, 14)]), 'blood', normal='dome')     # hood
    s.shape(poly([(6, 24), (11, 22), (11, 30), (5, 30)]) | poly([(26, 24), (21, 22), (21, 30), (27, 30)]), 'blood', normal='dome')   # drapes
    s.shape(ell(16, 22, 5.2, 6.2), 'night', normal=(0, 0, 1), rim=False)                               # face shadow
    s.px([(14, 21), (18, 21)], 'pink', 5); s.px([(14, 22), (18, 22)], 'void', 4)
    s.line((9, 13), (16, 10), 'gold', 4); s.line((16, 10), (23, 13), 'gold', 3); s.line((7, 16), (6, 23), 'blood', 5)
    return s.image()

def vestment_chestplate():
    s = Sprite()
    s.shape(poly([(3, 7), (10, 4), (22, 4), (29, 7), (28, 14), (24, 13), (24, 29), (8, 29), (8, 13), (4, 14)]), 'blood', normal='dome')
    s.shape(poly([(3, 7), (29, 7), (28, 10), (4, 10)]), 'void', normal='dome')                         # mantle
    for x in (12, 20): s.line((x, 10), (x, 28), 'gold', 4)                                              # stole
    s.shape(poly([(16, 14), (20, 18), (16, 22), (12, 18)]), 'gold', normal='dome'); s.shape(poly([(16, 16), (18, 18), (16, 20), (14, 18)]), 'void', normal='dome'); s.px([(16, 18)], 'pink', 5)
    for x in range(9, 24, 3): s.px([(x, 28)], 'blood', 1)                                                # torn hem
    s.line((9, 12), (9, 26), 'blood', 5); s.line((4, 8), (12, 6), 'pink', 4)
    return s.image()

def vestment_leggings():
    s = Sprite()
    s.shape(poly([(7, 3), (25, 3), (25, 8), (7, 8)]), 'leather', normal='dome'); s.line((7, 5), (25, 5), 'gold', 4)
    for x0, x1 in ((6, 15), (17, 26)):
        s.shape(poly([(x0 + 1, 8), (x1, 8), (x1 + 1, 28), (x0, 28)]), 'blood', normal='dome')
        for x in range(x0, x1 + 1, 2): s.px([(x, 28 + (x % 3 == 0))], 'blood', 2)
        s.line((x0 + (x1 - x0) // 2, 10), (x0 + (x1 - x0) // 2, 26), 'gold', 3)
    s.px([(16, 5)], 'pink', 5); s.line((8, 9), (7, 26), 'blood', 5)
    return s.image()

def vestment_boots():
    s = Sprite()
    for x0 in (4, 17):
        s.shape(poly([(x0 + 1, 8), (x0 + 9, 8), (x0 + 9, 21), (x0 + 11, 26), (x0, 26)]), 'blood', normal='dome')
        s.shape(poly([(x0, 22), (x0 + 11, 22), (x0 + 11, 26), (x0, 26)]), 'gold', normal='dome')
        s.line((x0 + 1, 9), (x0 + 9, 9), 'gold', 4); s.px([(x0 + 5, 24)], 'void', 4)
        s.line((x0 + 2, 11), (x0 + 2, 20), 'blood', 5)
    return s.image()

if __name__ == '__main__':
    save(wisp_bell(), 'wisp_bell')
    for n, f in (('helmet', choir_helmet), ('chestplate', choir_chestplate), ('leggings', choir_leggings), ('boots', choir_boots)): save(f(), 'choir_' + n)
    for n, f in (('helmet', vestment_helmet), ('chestplate', vestment_chestplate), ('leggings', vestment_leggings), ('boots', vestment_boots)): save(f(), 'vestment_' + n)
    save(egg('iron', 'rift', 'rift'), 'general_blade_spawn_egg'); save(egg('teal', 'night', 'teal'), 'general_arcanist_spawn_egg')
    save(egg('leather', 'gold', 'amber'), 'gate_mimic_spawn_egg')

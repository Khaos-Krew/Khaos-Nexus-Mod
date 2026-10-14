"""1.2 Hollow King drops (32x32 house style): Kingsbane, the Hollow Crown, Morvane's Heart-Ember, King's egg."""
from gen_items_v2 import *
from items_v3_saga import egg

def kingsbane():
    """Kingsbane: a royal greatsword, diagonal; dark steel blade with a pale-gold fuller rune, crown crossguard, ember pommel."""
    s = Sprite()
    s.shape(poly([(27, 2), (30, 2), (30, 5), (13, 22), (9, 19)]), 'steel', normal=facet_normals(20, 11, 10, 0.4))  # blade
    s.line((28, 4), (12, 20), 'gold', 3)
    for i in range(4): s.px([(25 - i * 4, 7 + i * 4)], 'amber', 5)
    s.shape(poly([(6, 15), (9, 13), (18, 22), (16, 25)]), 'gold', normal='dome')       # crossguard
    for x, y in ((6, 13), (8, 11), (17, 25), (19, 23)): s.px([(x, y)], 'gold', 5)       # crown prongs
    s.px([(12, 19)], 'amber', 5)
    s.shape(poly([(10, 21), (12, 23), (6, 29), (4, 27)]), 'leather', normal='dome')      # grip
    for i in range(3): s.px([(9 - i * 2, 24 + i * 2)], 'gold', 3)
    s.shape(ell(4, 29, 2.2, 2.2), 'amber', normal='dome'); s.px([(3, 28)], 'white', 5)   # pommel ember
    s.line((29, 3), (27, 5), 'white', 5)
    return s.image()

def hollow_crown():
    """The Hollow Crown: a tall tarnished-gold crown, front-on, its gem sockets empty and burning pale gold."""
    s = Sprite()
    s.shape(poly([(5, 20), (27, 20), (27, 27), (5, 27)]), 'gold', normal='dome')          # band
    for x, h in ((6, 8), (11, 12), (16, 4), (21, 12), (26, 8)):                          # prongs
        s.shape(poly([(x - 2, 20), (x + 2, 20), (x + 0.6, h + 2), (x, h), (x - 0.6, h + 2)]), 'gold', normal='dome')
        s.shape(ell(x, h, 1.5, 1.5), 'amber', normal='dome')
    s.shape(poly([(4, 26), (28, 26), (28, 29), (4, 29)]), 'bronze', normal='dome')
    for x in (9, 16, 23):                                                                # empty sockets
        s.shape(ell(x, 23.5, 2.0, 2.0), 'night', normal='dome'); s.px([(x, 23)], 'amber', 5)
    s.line((6, 21), (26, 21), 'white', 4); s.px([(16, 5)], 'white', 5)
    return s.image()

def heart_ember():
    """Morvane's Heart-Ember: the burning core from the King's hollow chest, caged in two gold ribs."""
    s = Sprite()
    s.shape(ell(16, 17, 7.5, 9), 'amber', normal='dome')
    s.shape(ell(15, 15, 3.5, 4.5), 'white', normal='dome')
    for sx in (-1, 1):
        s.line((16 + sx * 9, 8), (16 + sx * 9, 26), 'gold', 3, w=2)
        for y in (11, 16, 21): s.line((16 + sx * 9, y), (16 + sx * 4, y + 1), 'gold', 4)
    s.line((16, 2), (16, 7), 'iron', 3); s.shape(ell(16, 2.5, 1.6, 1.4), 'gold', normal='dome')
    for x, y in ((8, 28), (24, 29), (5, 18), (27, 14)): s.px([(x, y)], 'amber', 4)
    return s.image()

def awakened_kingsbane():
    """Awakened Kingsbane: night-steel blade with a blazing fuller, burning pommel heart and drifting embers."""
    s = Sprite()
    s.shape(poly([(27, 2), (30, 2), (30, 5), (13, 22), (9, 19)]), 'night', normal=facet_normals(20, 11, 10, 0.4))
    s.line((28, 4), (12, 20), 'amber', 5); s.line((27, 4), (12, 19), 'gold', 4)
    s.shape(poly([(6, 15), (9, 13), (18, 22), (16, 25)]), 'gold', normal='dome')
    for x, y in ((6, 13), (8, 11), (17, 25), (19, 23)): s.px([(x, y)], 'amber', 5)
    s.px([(12, 19)], 'white', 5)
    s.shape(poly([(10, 21), (12, 23), (6, 29), (4, 27)]), 'blood', normal='dome')
    for i in range(3): s.px([(9 - i * 2, 24 + i * 2)], 'gold', 4)
    s.shape(ell(4, 29, 2.4, 2.4), 'amber', normal='dome'); s.px([(3, 28), (4, 29)], 'white', 5)
    for x, y in ((24, 2), (31, 8), (20, 4), (15, 13), (26, 11)): s.px([(x, y)], 'amber', 4)
    return s.image()

def awakened_crown():
    """The Awakened Crown: the Hollow Crown with every socket filled with a burning heart-ember and a pale halo."""
    s = Sprite()
    s.shape(poly([(5, 20), (27, 20), (27, 27), (5, 27)]), 'gold', normal='dome')
    for x, h in ((6, 8), (11, 12), (16, 4), (21, 12), (26, 8)):
        s.shape(poly([(x - 2, 20), (x + 2, 20), (x + 0.6, h + 2), (x, h), (x - 0.6, h + 2)]), 'gold', normal='dome')
        s.shape(ell(x, h, 1.6, 1.6), 'amber', normal='dome'); s.px([(x, h)], 'white', 5)
    s.shape(poly([(4, 26), (28, 26), (28, 29), (4, 29)]), 'bronze', normal='dome')
    for x in (9, 16, 23):
        s.shape(ell(x, 23.5, 2.0, 2.0), 'amber', normal='dome'); s.px([(x, 23)], 'white', 5)
    s.line((6, 21), (26, 21), 'white', 4)
    for x, y in ((3, 10), (29, 10), (16, 1), (2, 18), (30, 17), (9, 4), (23, 4)): s.px([(x, y)], 'amber', 5)
    return s.image()

if __name__ == '__main__':
    save(awakened_kingsbane(), 'awakened_kingsbane'); save(awakened_crown(), 'awakened_crown')
    save(kingsbane(), 'kingsbane'); save(hollow_crown(), 'hollow_crown'); save(heart_ember(), 'heart_ember')
    save(egg('blood', 'gold', 'amber'), 'hollow_king_spawn_egg')

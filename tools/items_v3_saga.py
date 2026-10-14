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

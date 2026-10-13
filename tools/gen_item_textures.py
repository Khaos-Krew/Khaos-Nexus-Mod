"""Procedural 16x16 item textures (pixel-art style: hard outline, 3-tone shading, highlight)."""
import math, json, os
from PIL import Image
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'khaosnexus')
TEX = os.path.join(ROOT, 'textures', 'item'); MOD = os.path.join(ROOT, 'models', 'item')

def save(name, img):
    os.makedirs(TEX, exist_ok=True); os.makedirs(MOD, exist_ok=True)
    img.save(os.path.join(TEX, name + '.png'))
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'khaosnexus:item/{name}'}},
              open(os.path.join(MOD, name + '.json'), 'w'), indent=2)

def void_orb():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5; r = math.hypot(dx, dy)
            if r > 6.6: continue
            if r > 5.7: px[x, y] = (40, 8, 52, 255); continue                     # outline
            l = max(0.0, 1 - math.hypot(dx + 2, dy + 2) / 9)                        # light from top-left
            c = (int(120 + 110 * l), int(30 + 50 * l), int(170 + 85 * l), 255)
            if abs((dx * 0.8 - dy) % 5 - 2.5) < 0.5 and r < 5: c = (255, 110, 245, 255)   # glowing cracks
            if abs(dy - dx * 0.25) < 0.6 and r > 3.5: c = (200, 60, 230, 255)              # rune band
            px[x, y] = c
    px[5, 4] = px[4, 5] = (255, 230, 255, 255); px[5, 5] = (240, 190, 255, 255)
    save('void_orb', im)

def nexus_gate():
    """A jagged rift tear held in a dark iron frame with corner rivets; crimson core, white-hot centre, sparks."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    iron, iron_l, iron_d = (58, 52, 62, 255), (98, 90, 104, 255), (24, 20, 28, 255)
    for y in range(1, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (1, 14)
            if edge: px[x, y] = iron_d if (x == 12 or y == 14) else iron_l if (x == 3 or y == 1) else iron
            elif x in (4, 11) or y in (2, 13): px[x, y] = iron
    for x, y in ((4, 2), (11, 2), (4, 13), (11, 13)): px[x, y] = (150, 120, 70, 255)    # brass rivets
    wid = [0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0, 0]                                   # tear half-width per row
    off = [0, 0, 0, 0, 1, 0, 0, -1, 0, 1, 0, 0, -1, 0, 0, 0]                                 # zig-zag
    for y in range(3, 13):
        cx = 7.5 + off[y]
        for x in range(5, 11):
            d = abs(x - cx)
            if d <= wid[y] + 0.5:
                t = d / (wid[y] + 0.5)
                px[x, y] = (255, 236, 240, 255) if t < 0.3 else (255, 120, 140, 255) if t < 0.65 else (196, 36, 58, 255)
            elif d <= wid[y] + 1.5:
                px[x, y] = (90, 14, 30, 255)
    for x, y in ((2, 4), (13, 9), (1, 11), (14, 3)): px[x, y] = (255, 90, 110, 255)         # sparks
    save('nexus_gate', im)

def nexus_token():
    """Brass-rimmed coin with a violet rift-eye inlay; top-left light, dark bottom-right rim."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5; r = math.hypot(dx, dy)
            if r > 7.1: continue
            lit = (-dx - dy) / 10
            if r > 6.2: px[x, y] = (70, 46, 20, 255); continue                                    # outline
            if r > 4.9:                                                                          # brass rim
                k = 0.75 + 0.5 * lit; px[x, y] = (min(255, int(205 * k)), min(255, int(160 * k)), int(70 * k), 255)
                if (x + y) % 3 == 0: px[x, y] = (min(255, int(230 * k)), min(255, int(190 * k)), int(90 * k), 255)  # milled edge
                continue
            k = 0.8 + 0.4 * lit; px[x, y] = (int(78 * k), int(26 * k), int(120 * k), 255)          # violet face
            e = (dx / 3.6) ** 2 + (dy / 1.9) ** 2                                                # rift eye
            if e < 1: px[x, y] = (210, 120, 255, 255) if e > 0.45 else (255, 240, 255, 255) if abs(dx) < 0.8 else (150, 60, 220, 255)
    px[5, 4] = (255, 245, 210, 255); px[4, 5] = (250, 225, 160, 255)
    save('nexus_token', im)

EGGS = {  # name: (shell, dark, spot, eye/glow, motif)
    'rift_ghoul': ('#3A3A46', '#16141B', '#6A3694', '#C12F41', 'claws'),
    'rift_stalker': ('#2A1F3C', '#0D0914', '#E8E2D8', '#E0485A', 'mask'),
    'void_caster': ('#301C35', '#110B15', '#7520A1', '#E070FF', 'orb'),
    'rift_brute': ('#463442', '#140B10', '#8A45A0', '#E04A5C', 'crystals'),
    'rift_warden': ('#3A2E32', '#120D10', '#AD2530', '#FF3B47', 'visor'),
    'spire_guardian': ('#4A4246', '#120D0F', '#7F18A9', '#B04BE0', 'spire'),
}

def egg(name, shell, dark, spot, glow, motif):
    h = lambda c: tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
    shell, dark, spot, glow = h(shell), h(dark), h(spot), h(glow)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
    rnd = __import__('random').Random(name)
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.6; dy = (y - 8.6) / (7.2 if y >= 8.6 else 7.9)    # egg: narrower top
            r = math.hypot(dx * (1 + max(0, -dy) * 0.18), dy)
            if r > 1.0: continue
            if r > 0.86: px[x, y] = dark; continue
            lit = 1.0 + 0.35 * (-dx - dy) / 2
            px[x, y] = tuple(min(255, int(c * lit)) for c in shell[:3]) + (255,)
    for _ in range(7):                                                            # spots
        x, y = rnd.randint(4, 11), rnd.randint(4, 13)
        if px[x, y][3] and px[x, y] != dark: px[x, y] = spot
    def P(x, y, c):
        if px[x, y][3]: px[x, y] = c
    if motif == 'claws':
        for x in (6, 8, 10):
            for k in range(3): P(x - k // 2, 9 + k, (200, 190, 200, 255))
        P(6, 6, glow); P(9, 6, glow)
    elif motif == 'mask':
        for y in range(5, 10):
            for x in range(6, 10): P(x, y, (232, 226, 216, 255))
        P(6, 7, glow); P(9, 7, glow); P(8, 5, (60, 50, 60, 255)); P(7, 9, (60, 50, 60, 255))
    elif motif == 'orb':
        for y in range(7, 11):
            for x in range(6, 10):
                if math.hypot(x - 7.5, y - 8.5) < 2.1: P(x, y, glow)
        P(7, 8, (255, 230, 255, 255))
    elif motif == 'crystals':
        for x, top in ((6, 6), (8, 4), (10, 7)):
            for y in range(top, top + 4): P(x, y, spot)
            P(x, top, glow)
    elif motif == 'visor':
        for x in range(5, 11): P(x, 8, glow)
        for x in range(5, 11): P(x, 7, dark)
    elif motif == 'spire':
        for y in range(3, 9): P(7, y, glow if y < 5 else spot)
        P(6, 8, spot); P(8, 8, spot)
    px[5, 4] = (255, 255, 255, 180) if px[5, 4][3] else px[5, 4]
    save(name + '_spawn_egg', im)

if __name__ == '__main__':
    void_orb(); nexus_gate(); nexus_token()
    for n, a in EGGS.items(): egg(n, *a)

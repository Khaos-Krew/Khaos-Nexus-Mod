"""Unique skins for the named NPCs (Wandering Trader layout, 64x64): recolours the vanilla trader by material
class (hood/robe, trim, coat, gold, skin, hair) with per-NPC palettes, then paints role details.
Usage: python3 npc_skins.py VANILLA_PNG OUTDIR"""
import sys, os, colorsys
from PIL import Image
SRC, OUT = sys.argv[1], sys.argv[2]; os.makedirs(OUT, exist_ok=True)
def cls(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255); h *= 360
    if s < 0.12: return 'grey'
    if 190 <= h <= 250: return 'robe'
    if 70 <= h < 170: return 'coat'
    if (h < 12 or h > 340) and s > 0.45 and l < 0.45: return 'trim'
    if 35 <= h < 60 and s > 0.55 and l > 0.35: return 'gold'
    if 12 <= h < 40 and l > 0.45: return 'skin'
    return 'hair'
REF = {'robe': (62, 96, 160), 'coat': (52, 74, 42), 'trim': (120, 20, 14), 'gold': (230, 178, 40), 'skin': (186, 132, 100), 'hair': (90, 60, 40), 'grey': (128, 128, 128)}
NPC = {
 'vendor/alchemist': dict(robe=(54, 120, 84), coat=(70, 40, 96), trim=(170, 140, 40), gold=(130, 230, 120), hair=(220, 220, 210), eye=(160, 80, 220), mark='flask'),
 'vendor/armorer': dict(robe=(110, 116, 128), coat=(60, 60, 70), trim=(30, 60, 140), gold=(200, 205, 215), hair=(60, 40, 30), eye=(70, 120, 200), mark='plate'),
 'vendor/smith': dict(robe=(70, 50, 36), coat=(120, 76, 40), trim=(220, 100, 20), gold=(255, 150, 40), hair=(40, 30, 26), skin=(170, 110, 80), eye=(255, 140, 30), mark='apron'),
 'exile/captain': dict(robe=(96, 22, 30), coat=(48, 50, 58), trim=(200, 200, 210), gold=(220, 180, 70), hair=(150, 90, 50), eye=(60, 140, 200), mark='pauldron'),
 'exile/keeper': dict(robe=(200, 196, 182), coat=(120, 96, 70), trim=(160, 120, 40), gold=(250, 210, 90), hair=(110, 100, 96), eye=(120, 200, 230), mark='rope'),
 'exile/quartermaster': dict(robe=(40, 92, 96), coat=(110, 82, 50), trim=(70, 40, 22), gold=(200, 160, 90), hair=(170, 60, 30), eye=(70, 160, 90), mark='satchel'),
}
def shade(base, src, ref):
    lum = lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]
    k = lum(src) / max(lum(ref), 1); return tuple(max(0, min(255, int(v * k))) for v in base)
base = Image.open(SRC).convert('RGBA')
for name, P in NPC.items():
    im = base.copy(); px = im.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = px[x, y]
            if a == 0: continue
            c = cls(r, g, b)
            if c in P: px[x, y] = shade(P[c], (r, g, b), REF[c]) + (a,)
    e = P['eye']
    for x in (9, 10, 13, 14): pass
    px[9, 12] = px[14, 12] = e + (255,)                     # eyes (head front 8..16, y 8..18)
    m = P['mark']; C = P['gold'] + (255,); D = P['trim'] + (255,)
    if m == 'flask':                                        # potion flask on the chest (body front 22..30, y 22..32)
        for y in range(25, 30): px[25, y] = px[26, y] = (120, 230, 140, 255)
        px[25, 24] = px[26, 24] = (200, 200, 210, 255)
    elif m == 'plate':
        for x in range(22, 30):
            for y in range(22, 28): px[x, y] = shade((190, 195, 205), (200, 200, 200) if (x + y) % 3 else (160, 160, 160), (180, 180, 180)) + (255,)
    elif m == 'apron':
        for x in range(23, 29):
            for y in range(24, 37): px[x, y] = (96 - (y % 2) * 8, 66, 40, 255)
        px[24, 30] = px[27, 33] = (40, 34, 30, 255)          # soot
    elif m == 'pauldron':
        for x in range(44, 52):
            for y in range(20, 23): px[x, y] = (180, 184, 196, 255) if x % 2 else (140, 144, 156, 255)
        px[26, 24] = px[25, 25] = px[27, 25] = px[26, 26] = C   # crest star
    elif m == 'rope':
        for x in range(22, 30): px[x, 31] = C
        px[26, 32] = px[26, 33] = C
    elif m == 'satchel':
        for i in range(8): px[22 + i, 22 + i] = D
        for x in range(26, 30):
            for y in range(30, 34): px[x, y] = D
    im.save(f'{OUT}/{name.replace("/", "_")}.png')
    print(name)

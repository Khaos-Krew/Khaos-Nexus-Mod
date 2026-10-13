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

if __name__ == '__main__':
    void_orb()

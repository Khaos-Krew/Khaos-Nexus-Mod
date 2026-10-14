"""Seal Gate block faces (32x32): obsidian ashlar, gilded crown sigil, violet seal crack. Painted at 4x, Lanczos + light unsharp."""
from PIL import Image, ImageDraw, ImageFilter
import random, os
R = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/khaosnexus/textures/block/')
def face(top):
    random.seed(7 if top else 3); S = 128; im = Image.new('RGB', (S, S), (16, 12, 22)); d = ImageDraw.Draw(im)
    for y in range(0, S, 32):
        off = 16 * ((y // 32) % 2)
        for x in range(-32, S, 64):
            c = random.randint(28, 42); d.rectangle([x + off + 3, y + 3, x + off + 60, y + 28], fill=(c, c - 8, c + 12))
            d.rectangle([x + off + 3, y + 3, x + off + 60, y + 5], fill=(c + 24, c + 16, c + 34))
            d.rectangle([x + off + 3, y + 26, x + off + 60, y + 28], fill=(c - 14, c - 18, c - 6))
            for _ in range(5):
                px, py = x + off + random.randint(6, 56), y + random.randint(7, 24); d.rectangle([px, py, px + 2, py + 2], fill=(c + 10, c + 4, c + 20))
    gold, mid, dk = (250, 214, 110), (222, 160, 50), (110, 60, 18)
    if top:
        d.ellipse([18, 18, 110, 110], outline=dk, width=12); d.ellipse([20, 20, 108, 108], outline=mid, width=8); d.arc([20, 20, 108, 108], 180, 270, fill=gold, width=4)
        d.ellipse([40, 40, 88, 88], fill=(70, 22, 120)); d.ellipse([50, 50, 78, 78], fill=(160, 80, 230)); d.ellipse([58, 56, 68, 66], fill=(245, 220, 255))
    else:
        crown = [(26, 82), (34, 42), (48, 64), (64, 30), (80, 64), (94, 42), (102, 82)]
        d.polygon(crown, fill=dk); d.polygon([(p[0], p[1] + 3) for p in crown[:1]] + crown[1:-1] + [crown[-1]], fill=mid)
        d.line(crown[1:-1], fill=gold, width=3)
        d.rectangle([26, 82, 102, 96], fill=mid, outline=dk, width=3); d.line([(29, 85), (99, 85)], fill=gold, width=2)
        for x in (40, 64, 88): d.ellipse([x - 6, 84, x + 6, 95], fill=(170, 24, 48), outline=(70, 8, 20), width=2); d.point([(x - 2, 87)], fill=(255, 190, 190))
        pts = [(64, 0), (60, 18), (68, 34), (62, 52), (66, 70)]
        d.line(pts, fill=(90, 30, 170), width=8); d.line(pts, fill=(170, 100, 255), width=4); d.line([(p[0] - 1, p[1]) for p in pts], fill=(240, 215, 255), width=1)
        d.line([(62, 98), (67, 112), (60, 128)], fill=(170, 100, 255), width=5)
    im = im.resize((32, 32), Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=1, percent=40, threshold=2))
    im.save(R + ('seal_gate_top.png' if top else 'seal_gate.png'), optimize=True)
face(True); face(False)

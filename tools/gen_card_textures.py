"""Hunter Card item, Guild Desk block and Guild Clerk villager textures. Painted at 8x, Lanczos down,
light unsharp-mask, lossless PNG (owner image rule). Shared void-purple / rift-red palette."""
import os, random
from PIL import Image, ImageDraw, ImageFilter
R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'khaosnexus', 'textures')
S = 8
VOID = [(14, 8, 22), (30, 18, 46), (52, 30, 78), (84, 50, 120), (128, 84, 170), (182, 140, 222)]
RED = [(60, 10, 20), (122, 22, 40), (196, 46, 68), (255, 120, 130)]
GOLD = [(70, 50, 24), (122, 90, 46), (186, 146, 80), (240, 214, 150)]
WOOD = [(36, 22, 18), (62, 40, 30), (92, 62, 44), (128, 92, 64)]
rnd = random.Random(11)

def fin(img, size):
    img = img.resize(size, Image.LANCZOS)
    a = img.getchannel('A').point(lambda v: 255 if v >= 110 else 0)
    rgb = img.convert('RGB').filter(ImageFilter.UnsharpMask(radius=1, percent=45, threshold=2)); rgb.putalpha(a)
    return rgb

def bevel(d, box, ramp, w):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=ramp[2])
    d.rectangle([x0, y0, x1, y0 + w], fill=ramp[4 if len(ramp) > 4 else 3])   # lit top
    d.rectangle([x0, y0, x0 + w, y1], fill=ramp[3])                           # lit left
    d.rectangle([x0, y1 - w, x1, y1], fill=ramp[0])                           # shadow bottom
    d.rectangle([x1 - w, y0, x1, y1], fill=ramp[1])                           # shadow right

def card():
    n = 32 * S; im = Image.new('RGBA', (n, n)); d = ImageDraw.Draw(im)
    box = [3 * S, 7 * S, 29 * S - 1, 25 * S - 1]
    d.rounded_rectangle([box[0] + S, box[1] + S, box[2] + S, box[3] + S], 2 * S, fill=(0, 0, 0, 110))   # drop shadow
    d.rounded_rectangle(box, 2 * S, fill=VOID[1], outline=VOID[0], width=S)
    for _ in range(500):                                                                             # mottled card stock
        x, y = rnd.randrange(box[0] + S, box[2] - S), rnd.randrange(box[1] + S, box[3] - S)
        d.rectangle([x, y, x + S, y + S // 2], fill=VOID[rnd.choice((1, 2, 2))])
    d.line([box[0] + 2 * S, box[1] + S, box[2] - 2 * S, box[1] + S], fill=VOID[4], width=S)       # top highlight
    d.line([box[0] + 2 * S, box[3] - S, box[2] - 2 * S, box[3] - S], fill=VOID[0], width=S)
    d.rectangle([box[0] + S, box[1] + 3 * S, box[2] - S, box[1] + 5 * S], fill=RED[1])            # rift-red band
    d.line([box[0] + S, box[1] + 3 * S, box[2] - S, box[1] + 3 * S], fill=RED[2], width=S // 2)
    p = [5 * S, 12 * S, 12 * S, 21 * S]                                                           # portrait frame
    bevel(d, p, GOLD, S // 2); d.rectangle([p[0] + S, p[1] + S, p[2] - S, p[3] - S], fill=VOID[0])
    cx = (p[0] + p[2]) // 2
    d.ellipse([cx - 2 * S, p[1] + 2 * S, cx + 2 * S, p[1] + 5 * S], fill=VOID[3])                 # silhouette
    d.rounded_rectangle([cx - 3 * S, p[1] + 5 * S, cx + 3 * S, p[3] - S], S, fill=VOID[3])
    for i, w in enumerate((12, 9, 11)):                                                          # text lines
        y = 13 * S + i * 3 * S
        d.rectangle([14 * S, y, (14 + w) * S, y + S], fill=VOID[4] if i == 0 else VOID[3])
    d.polygon([(24 * S, 19 * S), (26 * S, 21 * S), (24 * S, 23 * S), (22 * S, 21 * S)], fill=RED[2], outline=RED[3])  # rank gem
    d.point([(24 * S - S // 2, 20 * S)], fill=RED[3])
    for (x, y) in ((box[0] + 2 * S, box[3] - 3 * S), (box[2] - 6 * S, box[1] + 2 * S)):        # edge wear
        d.rectangle([x, y, x + S, y + S // 2], fill=VOID[4])
    return fin(im, (32, 32))

def desk(face):
    n = 32 * S; im = Image.new('RGBA', (n, n), WOOD[1]); d = ImageDraw.Draw(im)
    if face == 'top':
        for i in range(4):                                                                      # planks
            bevel(d, [0, i * 8 * S, n - 1, (i + 1) * 8 * S - 1], WOOD, S // 2)
            for _ in range(40):
                x = rnd.randrange(0, n); y = i * 8 * S + rnd.randrange(S, 7 * S)
                d.line([x, y, x + rnd.randrange(3, 9) * S, y], fill=WOOD[rnd.choice((0, 1, 3))], width=S // 2)
        d.rectangle([6 * S, 6 * S, 26 * S, 22 * S], fill=VOID[1], outline=GOLD[2], width=S)        # guild ledger cloth
        d.rectangle([9 * S, 9 * S, 16 * S, 19 * S], fill=(222, 210, 186)); d.rectangle([16 * S, 9 * S, 23 * S, 19 * S], fill=(206, 194, 170))
        for i in range(4): d.line([10 * S, (11 + i * 2) * S, 15 * S, (11 + i * 2) * S], fill=(90, 80, 70), width=S // 2)
        d.ellipse([20 * S, 23 * S, 24 * S, 27 * S], fill=RED[1], outline=RED[2], width=S // 2)        # wax seal
        d.line([24 * S, 5 * S, 28 * S, 1 * S], fill=GOLD[3], width=S)                                 # quill
    elif face == 'side':
        bevel(d, [0, 0, n - 1, 6 * S], WOOD, S // 2)
        for i in range(3):
            bevel(d, [i * 11 * S, 7 * S, (i + 1) * 11 * S - S, 30 * S], WOOD, S)
            d.rectangle([i * 11 * S + 3 * S, 14 * S, i * 11 * S + 7 * S, 16 * S], fill=GOLD[2])         # drawer handles
        d.rectangle([0, 10 * S, n, 12 * S], fill=VOID[2]); d.rectangle([12 * S, 9 * S, 20 * S, 13 * S], fill=RED[2])  # guild sash
        d.rectangle([0, 30 * S, n, n], fill=WOOD[0])
    else:
        for _ in range(300):
            x, y = rnd.randrange(n), rnd.randrange(n); d.rectangle([x, y, x + 2 * S, y + S], fill=WOOD[rnd.choice((0, 1, 2))])
    return fin(im, (32, 32))

def clerk(zombie=False):
    """64x64 villager overlay: void-purple guild coat with rift-red sash, gold trim, monocle."""
    n = 64 * S; im = Image.new('RGBA', (n, n)); d = ImageDraw.Draw(im)
    def r(x0, y0, x1, y1, c): d.rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=c)
    # body (16,20)-(40,38) coat
    r(16, 20, 40, 38, VOID[2]); r(16, 20, 40, 22, VOID[3]); r(16, 36, 40, 38, VOID[1])
    r(26, 26, 30, 38, GOLD[2]); r(20, 28, 40, 30, RED[2]); r(20, 28, 40, 29, RED[3])           # front trim, sash
    # robe (0,38)-(24,58)
    r(0, 38, 24, 58, VOID[2]); r(0, 38, 24, 40, VOID[3]); r(0, 56, 24, 58, GOLD[1])
    for x in range(1, 24, 3): r(x, 41, x + 1, 56, VOID[1])                                     # cloth folds
    # arms (44,22)-(60,30) sleeves
    r(44, 22, 60, 30, VOID[2]); r(44, 28, 60, 30, GOLD[2])
    # head: hat brim/band (32,0) and a gold monocle on the face (8..16, 8..16)
    r(8, 11, 10, 13, GOLD[3]); r(9, 12, 10, 13, (200, 220, 240))
    r(32, 8, 64, 10, VOID[1])
    for _ in range(120):
        x, y = rnd.randrange(16, 40), rnd.randrange(20, 38)
        if (x, y) not in ((26, 26),): r(x, y, x + 1, y + 1, VOID[rnd.choice((1, 3))] + (90,))
    out = im.resize((64, 64), Image.LANCZOS)
    a = out.getchannel('A').point(lambda v: 255 if v >= 110 else 0); rgb = out.convert('RGB').filter(ImageFilter.UnsharpMask(1, 35, 2)); rgb.putalpha(a)
    return rgb

def save(img, *p):
    path = os.path.join(R, *p); os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path, optimize=True)

save(card(), 'item', 'hunter_card.png')
for f in ('top', 'side', 'bottom'): save(desk(f), 'block', f'guild_desk_{f}.png')
save(clerk(), 'entity', 'villager', 'profession', 'guild_clerk.png')
save(clerk(True), 'entity', 'zombie_villager', 'profession', 'guild_clerk.png')
print('ok')

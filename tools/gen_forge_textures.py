"""Hunter Forge block, Rift Essence and Rift Core textures. Painted at 8x, Lanczos down, light unsharp-mask,
lossless PNG (owner image rule). Shared void-purple / rift-red palette."""
import os, random, math
from PIL import Image, ImageDraw
from gen_card_textures import R, S, VOID, RED, GOLD, fin, bevel
rnd = random.Random(23)
STONE = [(18, 14, 22), (34, 28, 40), (52, 44, 60), (74, 64, 84), (102, 90, 114)]
EMBER = [(90, 20, 10), (170, 50, 20), (240, 120, 40), (255, 210, 120)]

def bricks(d, n, ramp, rows=4):
    h = n // rows
    for r in range(rows):
        off = (r % 2) * (n // 4)
        for c in range(-1, 3):
            x0 = c * (n // 2) + off; box = [x0 + S // 2, r * h + S // 2, x0 + n // 2 - S // 2, (r + 1) * h - S // 2]
            bevel(d, box, ramp, S // 2)
            for _ in range(6):                                                        # grain + chips
                lo, hi = max(0, box[0]), min(n - S, box[2])
                if hi <= lo:
                    continue
                x = rnd.randrange(lo, hi); y = rnd.randrange(box[1] + S, box[3] - S)
                d.rectangle([x, y, x + S, y + S // 2], fill=ramp[rnd.choice((0, 1, 3))])

def forge(face):
    n = 32 * S; im = Image.new('RGBA', (n, n), STONE[0]); d = ImageDraw.Draw(im)
    if face == 'bottom':
        bricks(d, n, STONE, 4); return fin(im, (32, 32))
    if face == 'top':
        bevel(d, [0, 0, n - 1, n - 1], STONE, S)                                       # rim
        a = [4 * S, 4 * S, 28 * S - 1, 28 * S - 1]; bevel(d, a, GOLD, S // 2)          # anvil plate frame
        d.rectangle([a[0] + S, a[1] + S, a[2] - S, a[3] - S], fill=STONE[2])
        for _ in range(140):
            x, y = rnd.randrange(a[0] + S, a[2] - S), rnd.randrange(a[1] + S, a[3] - S)
            d.rectangle([x, y, x + S, y + S // 2], fill=STONE[rnd.choice((1, 3, 3, 4))])
        c = n // 2                                                                     # rift rune in the middle
        d.ellipse([c - 7 * S, c - 7 * S, c + 7 * S, c + 7 * S], outline=VOID[4], width=S)
        d.polygon([(c, c - 5 * S), (c + 3 * S, c), (c, c + 5 * S), (c - 3 * S, c)], fill=VOID[3], outline=VOID[5])
        d.line([c, c - 4 * S, c, c + 4 * S], fill=RED[3], width=S // 2)
        for (x, y) in ((6 * S, 6 * S), (25 * S, 6 * S), (6 * S, 25 * S), (25 * S, 25 * S)):  # rivets
            d.rectangle([x, y, x + S, y + S], fill=GOLD[3]); d.point([(x + S, y + S)], fill=GOLD[0])
        return fin(im, (32, 32))
    bricks(d, n, STONE, 4)
    d.rectangle([0, 0, n - 1, 3 * S], fill=GOLD[1]); d.line([0, S // 2, n, S // 2], fill=GOLD[3], width=S // 2)   # gold band
    d.line([0, 3 * S, n, 3 * S], fill=GOLD[0], width=S // 2)
    if face == 'front':                                                                # glowing furnace mouth
        m = [8 * S, 13 * S, 24 * S - 1, 27 * S - 1]
        d.rounded_rectangle([m[0] - S, m[1] - S, m[2] + S, m[3] + S], 3 * S, fill=GOLD[0])
        d.rounded_rectangle(m, 3 * S, fill=EMBER[0])
        for i, col in enumerate(EMBER[1:]):
            inset = (i + 1) * 2 * S
            d.rounded_rectangle([m[0] + inset, m[1] + inset + S, m[2] - inset, m[3] - S], 2 * S, fill=col)
        for _ in range(18):                                                            # void sparks in the fire
            x, y = rnd.randrange(m[0] + 2 * S, m[2] - 2 * S), rnd.randrange(m[1] + 2 * S, m[3] - 2 * S)
            d.rectangle([x, y, x + S, y + S], fill=rnd.choice((VOID[4], VOID[5], EMBER[3])))
        d.line([m[0], m[3] + S, m[2], m[3] + S], fill=GOLD[3], width=S // 2)
    else:                                                                              # side: purple crystal vents
        for cx in (9 * S, 23 * S):
            d.rectangle([cx - 2 * S, 10 * S, cx + 2 * S, 24 * S], fill=STONE[0])
            d.polygon([(cx, 11 * S), (cx + S, 16 * S), (cx, 23 * S), (cx - S, 16 * S)], fill=VOID[3], outline=VOID[5])
            d.line([cx, 12 * S, cx, 20 * S], fill=VOID[5], width=S // 2)
    for _ in range(10):                                                                # edge wear
        x = rnd.choice((0, n - S)); y = rnd.randrange(4 * S, n - S)
        d.rectangle([x, y, x + S, y + S], fill=STONE[4])
    return fin(im, (32, 32))

def essence():
    n = 32 * S; im = Image.new('RGBA', (n, n)); d = ImageDraw.Draw(im); c = n // 2
    for r, col in ((11, (60, 20, 90, 90)), (9, VOID[2]), (7, VOID[3]), (5, VOID[4]), (3, VOID[5])):   # glowing wisp
        d.ellipse([c - r * S, c - r * S + S, c + r * S, c + r * S + S], fill=col)
    for k in range(3):                                                                 # swirl strands
        a0 = k * 2.1
        pts = [(c + math.cos(a0 + t / 6) * (2 + t / 2) * S, c + math.sin(a0 + t / 6) * (2 + t / 2) * S) for t in range(14)]
        d.line(pts, fill=RED[2], width=S)
    d.ellipse([c - 2 * S, c - 2 * S, c + S, c + S], fill=(255, 235, 255))
    return fin(im, (32, 32))

def core():
    n = 32 * S; im = Image.new('RGBA', (n, n)); d = ImageDraw.Draw(im); c = n // 2
    pts = [(c, 3 * S), (c + 10 * S, c), (c, n - 3 * S), (c - 10 * S, c)]
    d.polygon([(x + S, y + S) for x, y in pts], fill=(0, 0, 0, 110))                    # shadow
    d.polygon(pts, fill=VOID[2], outline=VOID[0])
    d.polygon([(c, 3 * S), (c + 10 * S, c), (c, c)], fill=VOID[4])                      # lit facet
    d.polygon([(c, 3 * S), (c - 10 * S, c), (c, c)], fill=VOID[3])
    d.polygon([(c, n - 3 * S), (c + 10 * S, c), (c, c)], fill=VOID[1])                  # shadow facet
    d.ellipse([c - 3 * S, c - 3 * S, c + 3 * S, c + 3 * S], fill=RED[1])                # red heart
    d.ellipse([c - 2 * S, c - 2 * S, c + S, c + S], fill=RED[2]); d.rectangle([c - S, c - S, c, c], fill=RED[3])
    d.line([c - 6 * S, c - 3 * S, c - 2 * S, c - 7 * S], fill=VOID[5], width=S // 2)    # edge highlight
    return fin(im, (32, 32))

if __name__ == '__main__':
    for f in ('top', 'side', 'front', 'bottom'):
        forge(f).save(os.path.join(R, 'block', f'hunter_forge_{f}.png'), optimize=True)
    essence().save(os.path.join(R, 'item', 'rift_essence.png'), optimize=True)
    core().save(os.path.join(R, 'item', 'rift_core.png'), optimize=True)
    print('ok')

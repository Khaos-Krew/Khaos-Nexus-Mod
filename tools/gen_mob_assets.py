"""v2: generates Rift mob LayerDefinitions (RiftMobLayers.java) and shaded box-UV textures.
Specs follow /workspace/khaos-nexus/mod-art/concepts/concepts.md (units: model px, 16 px = 1 block).
Textures are painted per face at native resolution (pixel art) and saved as lossless PNG."""
import math, random, os
from PIL import Image, ImageDraw, ImageFilter
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main')
TEX = os.path.join(ROOT, 'resources/assets/khaosnexus/textures/')
JAVA = os.path.join(ROOT, 'java/com/khaoskrew/khaosnexus/client/mob/RiftMobLayers.java')
def hx(h): return tuple(int(h[i:i+2], 16) for i in (1, 3, 5))
R = math.radians
HUM_LEGS = [('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')]),
            ('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')])]
MOBS = {
 'rift_ghoul': dict(ts=128, pal=dict(base='#25252E', dark='#110E13', accent='#6A3694'), glow='#C12F41', eye='#A45ADA', eyes='hollow', parts=[
   ('body', 'root', (0, 0, 0), (10, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body')]),
   ('head', 'root', (0, 0, -1), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'skull')]),
   ('right_arm', 'root', (-5, 2, -1), (0, 0, 0), [(-3, -2, -2, 4, 14, 4, 'limb'), (-3, 12, -2, 1, 5, 1, 'claw'), (-1.5, 12, -2, 1, 5, 1, 'claw'), (0, 12, -2, 1, 5, 1, 'claw')]),
   ('left_arm', 'root', (5, 2, -1), (0, 0, 0), [(-1, -2, -2, 4, 14, 4, 'limb'), (-1, 12, -2, 1, 5, 1, 'claw'), (0.5, 12, -2, 1, 5, 1, 'claw'), (2, 12, -2, 1, 5, 1, 'claw')]),
   ('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 10, 4, 'limb'), (-2, 10, -4, 4, 2, 6, 'foot')]),
   ('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 10, 4, 'limb'), (-2, 10, -4, 4, 2, 6, 'foot')])]),
 'rift_stalker': dict(ts=128, pal=dict(base='#25212E', dark='#0D0914', accent='#5E3F7C', robe='#2A1F3C'), glow='#C23346', eye='#C23346', eyes='slit', parts=[
   ('body', 'root', (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body'), (-5, 0, -3, 10, 14, 6, 'robe')]),
   ('head', 'root', (0, 0, 0), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'face'), (-5, -9, -5, 10, 10, 10, 'hood'), (-1, -12, 0, 2, 3, 2, 'hood')]),
   ('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'robe'), (-2, 9, -1, 2, 18, 1, 'blade')]),
   ('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -2, 4, 12, 4, 'robe'), (0, 9, -1, 2, 18, 1, 'blade')])] + HUM_LEGS),
 'void_caster': dict(ts=128, pal=dict(base='#261F2B', dark='#110B15', accent='#7520A1', robe='#301C35'), glow='#8E2745', eye='#B35AE0', orb='#CA59EE', eyes='dots', parts=[
   ('body', 'root', (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body'), (-6, 10, -4, 12, 14, 8, 'robe'), (-5, 0, -3, 10, 2, 6, 'collar')]),
   ('head', 'root', (0, 0, 0), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'face'), (-5, -9, -5, 10, 10, 10, 'hood'), (-3, -15, -2, 6, 6, 6, 'hood')]),
   ('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'limb'), (-4, 3, -3, 6, 8, 6, 'sleeve')]),
   ('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -2, 4, 12, 4, 'limb'), (-2, 3, -3, 6, 8, 6, 'sleeve')]),
   ('orb', 'root', (0, 9, -8), (0, 0, 0), [(-2, -2, -2, 4, 4, 4, 'orb')])] + HUM_LEGS),
 'rift_brute': dict(ts=128, pal=dict(base='#2E212B', dark='#140B10', accent='#463442'), glow='#B13648', eye='#E04A5C', crystal='#8A45A0', eyes='dots', parts=[
   ('body', 'root', (0, -10, 0), (0, 0, 0), [(-9, 0, -5, 18, 18, 10, 'chest'), (-2, 13, -6, 4, 4, 1, 'sigil'),
        (-6, -3, 1, 2, 4, 2, 'spike'), (3, -4, 0, 3, 6, 3, 'spike'), (-2, 2, 5, 3, 6, 3, 'spike')]),
   ('head', 'root', (0, -8, -2), (0, 0, 0), [(-6, -12, -6, 12, 12, 12, 'skull'), (-5, -15, -1, 2, 4, 2, 'spike'), (2, -16, 0, 3, 5, 3, 'spike')]),
   ('right_arm', 'root', (-13, -8, 0), (0, 0, 0), [(-4, -2, -4, 8, 18, 8, 'limb'), (-5, 14, -5, 10, 8, 10, 'fist'), (-3, -6, -2, 3, 5, 3, 'spike')]),
   ('left_arm', 'root', (13, -8, 0), (0, 0, 0), [(-4, -2, -4, 8, 18, 8, 'limb'), (-5, 14, -5, 10, 8, 10, 'fist'), (0, -6, -1, 3, 5, 3, 'spike')]),
   ('right_leg', 'root', (-4.5, 8, 0), (0, 0, 0), [(-4, 0, -4, 8, 16, 8, 'limb')]),
   ('left_leg', 'root', (4.5, 8, 0), (0, 0, 0), [(-4, 0, -4, 8, 16, 8, 'limb')])]),
 # ~2.5x humanoid raid boss: 80 px (5 blocks) tall
 'rift_warden': dict(ts=256, pal=dict(base='#2A2327', dark='#120D10', accent='#5C5053', cape='#4A141C'), glow='#AD2530', eye='#FF3B47', eyes='visor', crystal='#A15FC0', parts=[
   ('body', 'root', (0, -36, 0), (0, 0, 0), [(-10, 0, -5, 20, 30, 10, 'armor'), (-4, 6, -6, 8, 8, 1, 'rune')]),
   ('head', 'root', (0, -36, 0), (0, 0, 0), [(-10, -20, -10, 20, 20, 20, 'helm'),
        (-9, -30, -2, 3, 10, 3, 'crown'), (-2, -34, -2, 4, 14, 4, 'crown'), (6, -30, -2, 3, 10, 3, 'crown')]),
   ('right_arm', 'root', (-15, -33, 0), (0, 0, 0), [(-5, -3, -5, 10, 30, 10, 'armor'), (-6, -4, -6, 12, 8, 12, 'pauldron')]),
   ('sword', 'right_arm', (0, 26, -2), (-30, 0, 0), [(-1.5, -2, -2, 3, 3, 10, 'grip'), (-5, -3, -4, 10, 5, 2, 'guard'), (-1, -3.5, -56, 2, 7, 52, 'greatblade')]),
   ('left_arm', 'root', (15, -33, 0), (0, 0, 0), [(-5, -3, -5, 10, 30, 10, 'armor'), (-6, -4, -6, 12, 8, 12, 'pauldron')]),
   ('cape', 'body', (0, 0, 5), (6, 0, 0), [(-13, 0, 0, 26, 44, 1, 'cape')]),
   ('right_leg', 'root', (-5, -6, 0), (0, 0, 0), [(-5, 0, -5, 10, 30, 10, 'armor')]),
   ('left_leg', 'root', (5, -6, 0), (0, 0, 0), [(-5, 0, -5, 10, 30, 10, 'armor')])]),
 # ~2x humanoid spire boss: 64 px (4 blocks) tall + spire helm
 'spire_guardian': dict(ts=256, pal=dict(base='#2D2528', dark='#120D0F', accent='#5A5054', cape='#4C151E'), glow='#962933', eye='#B04BE0', core='#7F18A9', eyes='visor', parts=[
   ('body', 'root', (0, -24, 0), (0, 0, 0), [(-8, 0, -4, 16, 24, 8, 'armor'), (-3, 5, -5, 6, 6, 1, 'core')]),
   ('head', 'root', (0, -24, 0), (0, 0, 0), [(-8, -16, -8, 16, 16, 16, 'helm'), (-4, -22, -4, 8, 6, 8, 'helmspire'),
        (-3, -27, -3, 6, 5, 6, 'helmspire'), (-2, -31, -2, 4, 4, 4, 'helmspire'), (-1, -37, -1, 2, 6, 2, 'helmspire')]),
   ('right_arm', 'root', (-12, -22, 0), (0, 0, 0), [(-4, -2, -4, 8, 24, 8, 'armor'), (-6, -5, -6, 12, 8, 12, 'pauldron')]),
   ('halberd', 'right_arm', (0, 20, -6), (0, 0, 0), [(-1.5, -44, -1, 3, 60, 2, 'shaft'), (1.5, -42, -1, 10, 16, 2, 'axehead'), (-1, -50, -1, 2, 6, 2, 'axehead')]),
   ('left_arm', 'root', (12, -22, 0), (0, 0, 0), [(-4, -2, -4, 8, 24, 8, 'armor'), (-6, -5, -6, 12, 8, 12, 'pauldron')]),
   ('cape', 'body', (0, 1, 4), (5, 0, 0), [(-8, 0, 0, 16, 28, 1, 'cape')]),
   ('right_leg', 'root', (-4, 0, 0), (0, 0, 0), [(-4, 0, -4, 8, 24, 8, 'armor')]),
   ('left_leg', 'root', (4, 0, 0), (0, 0, 0), [(-4, 0, -4, 8, 24, 8, 'armor')])]),
}

def pack(mob):
    TS = mob['ts']; x = y = rowh = 0; out = []
    for part in mob['parts']:
        for b in part[4]:
            w, h, d = (math.ceil(v) for v in b[3:6])
            fw, fh = 2 * (w + d), d + h
            if x + fw > TS: x, y, rowh = 0, y + rowh, 0
            out.append((x, y)); x += fw; rowh = max(rowh, fh)
    assert y + rowh <= TS, 'texture sheet overflow'
    return out

def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

LIGHT = {'top': 1.18, 'front': 1.0, 'back': 0.9, 'right': 0.86, 'left': 0.86, 'bottom': 0.62}
def mul(c, k): return tuple(max(0, min(255, int(q * k))) for q in c[:3])
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

class Painter:
    def __init__(s, name, mob):
        s.mob = mob; s.TS = mob['ts']; s.rnd = random.Random(name + 'v2')
        s.base = Image.new('RGBA', (s.TS, s.TS), (0, 0, 0, 0)); s.glow = s.base.copy()
        s.bp = s.base.load(); s.gp = s.glow.load()
        p = mob['pal']; s.P = {k: hx(v) for k, v in p.items()}
        s.gc = hx(mob['glow']); s.ec = hx(mob['eye'])
        s.noise = {}
    def n(s, x, y):  # 2px clustered value noise
        k = (x // 2, y // 2)
        if k not in s.noise: s.noise[k] = s.rnd.random()
        return s.noise[k] * 0.6 + s.rnd.random() * 0.4
    def px(s, x, y, c, a=255): s.bp[x, y] = tuple(c[:3]) + (a,)
    def gl(s, x, y, c, k=1.0): s.gp[x, y] = mul(c, k) + (255,)
    def fill(s, f, fname, c0, c1, grad=True, bevel=True):
        fx, fy, fw, fh = f; L = LIGHT[fname]
        for yy in range(fy, fy + fh):
            t = (yy - fy) / max(1, fh - 1)
            for xx in range(fx, fx + fw):
                c = mix(c0, c1, s.n(xx, yy))
                k = L * ((1.08 - 0.22 * t) if grad and fname not in ('top', 'bottom') else 1.0)
                if bevel and fw > 2 and fh > 2:
                    if yy == fy or xx == fx: k *= 1.18
                    elif yy == fy + fh - 1 or xx == fx + fw - 1: k *= 0.7
                s.px(xx, yy, mul(c, k))
    def vein(s, fx, fy, fw, fh, k=0.8, branches=2):
        """Deliberate rift vein: a trunk up the centre with a few angled branches."""
        cx = fx + fw // 2; x = cx
        for yy in range(fy + fh - 2, fy + fh // 4, -1):
            s.gl(x, yy, s.gc, k)
            if s.rnd.random() < 0.25: x = max(fx + 1, min(fx + fw - 2, x + s.rnd.choice((-1, 1))))
        for _ in range(branches):
            by = s.rnd.randrange(fy + fh // 3, fy + fh - 2); bx = cx; dx = s.rnd.choice((-1, 1))
            for i in range(max(2, fw // 3)):
                bx += dx; by -= s.rnd.choice((0, 1))
                if fx < bx < fx + fw - 1 and fy < by < fy + fh: s.gl(bx, by, s.gc, k * 0.8)

    def box(s, u, v, b):
        w, h, d = (math.ceil(q) for q in b[3:6]); role = b[6]; F = faces(u, v, w, h, d); P = s.P
        base, dark, acc = P['base'], P['dark'], P['accent']
        for fname, f in F.items():
            fx, fy, fw, fh = f
            if role in ('robe', 'hood', 'sleeve', 'collar'):
                rc = P.get('robe', acc); s.fill(f, fname, mul(rc, 0.75), mul(rc, 1.15))
                if fname not in ('top', 'bottom'):
                    for xx in range(fx, fx + fw):  # cloth folds
                        if (xx - fx) % 3 == 0:
                            for yy in range(fy + 1, fy + fh): s.px(xx, yy, mul(s.bp[xx, yy], 0.72))
                    if role == 'robe' and fh > 6:  # ragged hem + rune trim
                        for xx in range(fx, fx + fw):
                            cut = s.rnd.choice((0, 0, 1, 2))
                            for yy in range(fy + fh - cut, fy + fh): s.px(xx, yy, (0, 0, 0), 0)
                            if (xx - fx) % 2 == 0: s.gl(xx, fy + fh - 4, s.gc, 0.7)
                    if role in ('sleeve', 'collar') and fh > 2:
                        for xx in range(fx, fx + fw): s.gl(xx, fy + fh - 1, s.gc, 0.6)
            elif role in ('armor', 'helm', 'pauldron', 'helmspire'):
                s.fill(f, fname, mul(base, 0.9), mul(acc, 0.95))
                if fname not in ('top', 'bottom') and fw > 4 and fh > 4:
                    step = 8 if fh > 16 else 6
                    for yy in range(fy + step, fy + fh - 1, step):  # plate seams + highlight
                        for xx in range(fx, fx + fw):
                            s.px(xx, yy, mul(dark, 0.9)); s.px(xx, yy + 1, mul(s.bp[xx, yy + 1], 1.25))
                    for (xx, yy) in ((fx + 1, fy + 1), (fx + fw - 2, fy + 1), (fx + 1, fy + fh - 2), (fx + fw - 2, fy + fh - 2)):
                        s.px(xx, yy, mul(acc, 1.6))  # rivets
                    if fname == 'front' and role in ('armor', 'pauldron') and fw >= 8:
                        cx = fx + fw // 2  # engraved rune line
                        for yy in range(fy + 3, fy + min(fh - 2, 3 + fh // 3)): s.gl(cx, yy, s.gc, 0.55)
                if role == 'helm' and fname == 'front': s.eyes(f)
            elif role == 'cape':
                cc = P['cape']; s.fill(f, fname, mul(cc, 0.7), mul(cc, 1.15), bevel=False)
                for xx in range(fx, fx + fw):
                    if (xx - fx) % 4 in (0, 1):
                        for yy in range(fy, fy + fh): s.px(xx, yy, mul(s.bp[xx, yy], 0.75 if (xx - fx) % 4 == 0 else 1.15))
                    if fh > 6:
                        cut = s.rnd.choice((0, 1, 2, 3))
                        for yy in range(fy + fh - cut, fy + fh): s.px(xx, yy, (0, 0, 0), 0)
            elif role in ('greatblade', 'blade', 'axehead'):
                steel = (120, 112, 124) if role != 'blade' else mul(base, 1.6)
                s.fill(f, fname, mul(steel, 0.8), mul(steel, 1.15))
                if role == 'blade': [s.gl(xx, yy, s.gc, 0.9) for xx in range(fx, fx + fw) for yy in range(fy, fy + fh) if (xx == fx or yy == fy)]
                elif fname in ('top', 'bottom', 'right', 'left') and fw * fh > 30:  # rune channel down the blade
                    horiz = fw > fh; mid = (fy + fh // 2) if horiz else (fx + fw // 2)
                    for i in range((fw if horiz else fh)):
                        if i % 5 != 4:
                            (s.gl(fx + i, mid, s.gc, 0.85) if horiz else s.gl(mid, fy + i, s.gc, 0.85))
            elif role in ('grip', 'shaft', 'guard'):
                s.fill(f, fname, (40, 30, 34), (70, 56, 60))
                if role == 'shaft':
                    for yy in range(fy, fy + fh, 6): [s.px(xx, yy, (110, 100, 106)) for xx in range(fx, fx + fw)]
            elif role in ('rune', 'sigil', 'core'):
                s.fill(f, fname, mul(dark, 0.8), dark)
                gcol = hx(s.mob.get('core', s.mob['eye'])) if role == 'core' else s.gc
                for xx in range(fx, fx + fw):
                    for yy in range(fy, fy + fh):
                        inner = fx < xx < fx + fw - 1 and fy < yy < fy + fh - 1
                        if fname == 'front' and (inner and ((xx - fx) == fw // 2 or (yy - fy) == fh // 2 or abs((xx - fx) - (yy - fy)) == 0)):
                            s.gl(xx, yy, gcol, 1.0)
                        elif fname == 'front' and role == 'core': s.gl(xx, yy, gcol, 0.55)
            elif role == 'orb':
                oc = hx(s.mob['orb'])
                for xx in range(fx, fx + fw):
                    for yy in range(fy, fy + fh):
                        e = xx in (fx, fx + fw - 1) or yy in (fy, fy + fh - 1)
                        s.px(xx, yy, mul(oc, 0.5)); s.gl(xx, yy, oc, 0.7 if e else 1.0)
            elif role in ('spike', 'crown'):
                cc = hx(s.mob.get('crystal', '#7A3A8A')) if role == 'spike' else mul(acc, 1.1)
                s.fill(f, fname, mul(cc, 0.55), mul(cc, 1.1))
                if role == 'spike' and fname in ('front', 'right', 'left'):
                    for yy in range(fy, fy + fh): s.gl(fx, yy, cc, 0.5)
                if role == 'crown' and fname == 'front': s.gl(fx + fw // 2, fy + 1, s.gc, 0.8)
            else:  # flesh/stone: body, chest, limb, fist, foot, claw, skull, face
                s.fill(f, fname, mul(dark, 1.1), mul(base, 1.2))
                if role == 'claw': s.fill(f, fname, (70, 64, 72), (120, 110, 120))
                if role == 'chest' and fname == 'front':
                    for yy in range(fy + 2, fy + fh - 4):  # pectoral split + belt
                        s.px(fx + fw // 2, yy, mul(dark, 0.8))
                    for xx in range(fx, fx + fw):
                        s.px(xx, fy + fh - 4, mul(dark, 0.6)); s.px(xx, fy + fh - 3, (60, 44, 40)); s.px(xx, fy + fh - 2, (60, 44, 40))
                    s.vein(fx, fy, fw, fh - 4, 0.75, 4)
                elif role == 'body' and fname == 'front': s.vein(fx, fy, fw, fh, 0.75, 2)
                elif role == 'limb' and fname == 'front' and fh >= 10 and s.mob['eyes'] != 'dots' or (role == 'limb' and fname == 'front' and fw >= 8):
                    s.vein(fx, fy, fw, fh, 0.6, 1)
                if role == 'fist' and fname == 'front':
                    for xx in range(fx + 1, fx + fw - 1, 3): s.px(xx, fy + 1, mul(base, 1.6))  # knuckles
                if role in ('skull', 'face') and fname == 'front': s.eyes(f)
    def eyes(s, f):
        fx, fy, fw, fh = f; cx = fx + fw // 2; sc = max(1, fw // 8); ey = fy + fh // 2 - sc
        kind = s.mob['eyes']; dark = (5, 3, 7)
        if kind == 'visor':
            for xx in range(fx + 2 * sc, fx + fw - 2 * sc):
                for yy in range(ey, ey + sc + 1): s.px(xx, yy, dark); s.gl(xx, yy, s.ec, 1.0 if yy == ey else 0.6)
            for yy in range(ey + sc + 1, fy + fh - sc): s.px(cx, yy, dark)  # nasal slit
        elif kind == 'slit':
            for xx in range(cx - 2 * sc, cx + 2 * sc): s.px(xx, ey, dark); s.gl(xx, ey, s.ec, 1.0)
        else:
            for side in (-1, 1):
                x0 = cx + (sc if side > 0 else -3 * sc)
                for xx in range(x0, x0 + 2 * sc):
                    for yy in range(ey - (sc if kind == 'hollow' else 0), ey + sc):
                        s.px(xx, yy, dark)
                    s.gl(xx, ey, s.ec, 1.0)
            if kind == 'hollow':
                for xx in range(cx - 2 * sc, cx + 2 * sc): s.px(xx, fy + fh - 2, dark)  # jaw seam
    def save(s, name):
        os.makedirs(TEX + 'entity', exist_ok=True)
        s.base.save(TEX + f'entity/{name}.png', optimize=True)
        s.glow.save(TEX + f'entity/{name}_glow.png', optimize=True)
        w = s.glow.copy(); p = w.load()
        for yy in range(s.TS):
            for xx in range(s.TS):
                r, g, b, a = p[xx, yy]
                if a: L = min(255, max(r, g, b) + 60); p[xx, yy] = (L, L, L, a)
        w.save(TEX + f'entity/{name}_glow_rank.png', optimize=True)

def paint(name, mob):
    pt = Painter(name, mob)
    boxes = [b for p in mob['parts'] for b in p[4]]
    for (u, v), b in zip(pack(mob), boxes): pt.box(u, v, b)
    # hood openings: cut the hood front so the face (and its eyes) shows through
    for (u, v), b in zip(pack(mob), boxes):
        if b[6] == 'hood' and b[3] >= 8:
            fx, fy, fw, fh = faces(u, v, *(math.ceil(q) for q in b[3:6]))['front']
            for xx in range(fx + 2, fx + fw - 2):
                for yy in range(fy + 2, fy + fh): pt.px(xx, yy, (0, 0, 0), 0)
    pt.save(name)

def java():
    f = lambda v: f'{float(v)}F'
    out = ['package com.khaoskrew.khaosnexus.client.mob;', '',
           '// GENERATED by tools/gen_mob_assets.py from the concept specs. Do not edit by hand.',
           'import net.minecraft.client.model.geom.PartPose;', 'import net.minecraft.client.model.geom.builders.*;', '',
           'public final class RiftMobLayers {', '    private RiftMobLayers() {}', '']
    for name, mob in MOBS.items():
        meth = ''.join(p.title() for p in name.split('_'))
        out += [f'    public static LayerDefinition create{meth}() {{', '        MeshDefinition mesh = new MeshDefinition();',
                '        PartDefinition root = mesh.getRoot();']
        it = iter(pack(mob))
        for pname, parent, piv, rot, boxes in mob['parts']:
            cb = 'CubeListBuilder.create()'
            for b in boxes:
                u, v = next(it)
                cb += f'.texOffs({u}, {v}).addBox({f(b[0])}, {f(b[1])}, {f(b[2])}, {f(b[3])}, {f(b[4])}, {f(b[5])})'
            pose = f'PartPose.offsetAndRotation({f(piv[0])}, {f(piv[1])}, {f(piv[2])}, {f(R(rot[0]))}, {f(R(rot[1]))}, {f(R(rot[2]))})'
            var = pname if parent == 'root' else pname
            out.append(f'        PartDefinition p_{pname} = {"root" if parent == "root" else "p_" + parent}.addOrReplaceChild("{pname}", {cb}, {pose});')
        out += [f'        return LayerDefinition.create(mesh, {mob["ts"]}, {mob["ts"]});', '    }', '']
    out.append('}')
    open(JAVA, 'w').write('\n'.join(out) + '\n')

def token():
    S = 8; n = 16 * S; im = Image.new('RGBA', (n, n)); d = ImageDraw.Draw(im)
    d.ellipse([S, S, n - S - 1, n - S - 1], fill=(120, 50, 170, 255), outline=(40, 16, 60, 255), width=S)
    d.ellipse([S * 4, S * 4, n - S * 4 - 1, n - S * 4 - 1], outline=(230, 190, 255, 255), width=S)
    c = n // 2; d.polygon([(c, S * 5), (c + S * 2, c), (c, n - S * 5), (c - S * 2, c)], fill=(255, 230, 255, 255))
    im = im.resize((16, 16), Image.LANCZOS).filter(ImageFilter.UnsharpMask(1, 60, 2))
    im.save(TEX + 'item/nexus_token.png', optimize=True)

if __name__ == '__main__':
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    for k, m in MOBS.items(): paint(k, m)
    java(); token(); print('ok')

"""Generates Rift mob LayerDefinitions (RiftMobLayers.java) and matching box-UV textures.
Specs follow /workspace/khaos-nexus/mod-art/concepts/concepts.md (units: model px)."""
import math, random, os
from PIL import Image, ImageDraw, ImageFilter
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main')
TEX = os.path.join(ROOT, 'resources/assets/khaosnexus/textures/')
JAVA = os.path.join(ROOT, 'java/com/khaoskrew/khaosnexus/client/mob/RiftMobLayers.java')
TS = 128
def hx(h): return tuple(int(h[i:i+2], 16) for i in (1, 3, 5))
D = math.radians
# part: name, parent, pivot(x,y,z), rot(x,y,z deg), boxes[(x,y,z,w,h,d,role)]
HUM_LEGS = [('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')]),
            ('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')])]
MOBS = {
 'rift_ghoul': dict(pal=['#25252E', '#110E13', '#6A3694'], glow='#C12F41', eye='#A45ADA', eyes='hollow', parts=[
   ('body', 'root', (0, 0, 0), (10, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body')]),
   ('head', 'root', (0, 0, -1), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'head')]),
   ('right_arm', 'root', (-5, 2, -1), (0, 0, 0), [(-3, -2, -2, 4, 14, 4, 'limb'), (-3, 12, -2, 1, 5, 1, 'claw'), (-1.5, 12, -2, 1, 5, 1, 'claw'), (0, 12, -2, 1, 5, 1, 'claw')]),
   ('left_arm', 'root', (5, 2, -1), (0, 0, 0), [(-1, -2, -2, 4, 14, 4, 'limb'), (-1, 12, -2, 1, 5, 1, 'claw'), (0.5, 12, -2, 1, 5, 1, 'claw'), (2, 12, -2, 1, 5, 1, 'claw')]),
   ('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 10, 4, 'limb'), (-2, 10, -4, 4, 2, 6, 'foot')]),
   ('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 10, 4, 'limb'), (-2, 10, -4, 4, 2, 6, 'foot')])]),
 'rift_stalker': dict(pal=['#25212E', '#0D0914', '#2A1F3C'], glow='#C23346', eye='#C23346', eyes='slit', parts=[
   ('body', 'root', (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body'), (-5, 0, -3, 10, 14, 6, 'robe')]),
   ('head', 'root', (0, 0, 0), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'head'), (-5, -9, -5, 10, 10, 10, 'hood'), (-1, -12, 0, 2, 3, 2, 'hood')]),
   ('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'robe'), (-2, 9, -1, 2, 18, 1, 'blade')]),
   ('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -2, 4, 12, 4, 'robe'), (0, 9, -1, 2, 18, 1, 'blade')])] + HUM_LEGS),
 'void_caster': dict(pal=['#261F2B', '#110B15', '#301C35'], glow='#8E2745', eye='#7520A1', eyes='dots', parts=[
   ('body', 'root', (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, 'body'), (-6, 10, -4, 12, 14, 8, 'robe'), (-5, 0, -3, 10, 2, 6, 'robe')]),
   ('head', 'root', (0, 0, 0), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'head'), (-5, -9, -5, 10, 10, 10, 'hood'), (-3, -15, -2, 6, 6, 6, 'hood')]),
   ('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'limb'), (-4, 3, -3, 6, 8, 6, 'robe')]),
   ('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -2, 4, 12, 4, 'limb'), (-2, 3, -3, 6, 8, 6, 'robe')]),
   ('orb', 'root', (0, 9, -8), (0, 0, 0), [(-2, -2, -2, 4, 4, 4, 'orb')])] + HUM_LEGS),
 'rift_brute': dict(pal=['#2E212B', '#140B10', '#463442'], glow='#B13648', eye='#B13648', eyes='dots', crystal='#6D3678', parts=[
   ('body', 'root', (0, -10, 0), (0, 0, 0), [(-9, 0, -5, 18, 18, 10, 'body'), (-2, 13, -6, 4, 4, 1, 'sigil'),
        (-6, -3, 1, 2, 4, 2, 'spike'), (3, -4, 0, 3, 6, 3, 'spike'), (-2, 2, 5, 3, 6, 3, 'spike')]),
   ('head', 'root', (0, -8, -2), (0, 0, 0), [(-6, -12, -6, 12, 12, 12, 'head'), (-5, -15, -1, 2, 4, 2, 'spike'), (2, -16, 0, 3, 5, 3, 'spike')]),
   ('right_arm', 'root', (-13, -8, 0), (0, 0, 0), [(-4, -2, -4, 8, 18, 8, 'limb'), (-5, 14, -5, 10, 8, 10, 'fist'), (-3, -6, -2, 3, 5, 3, 'spike')]),
   ('left_arm', 'root', (13, -8, 0), (0, 0, 0), [(-4, -2, -4, 8, 18, 8, 'limb'), (-5, 14, -5, 10, 8, 10, 'fist'), (0, -6, -1, 3, 5, 3, 'spike')]),
   ('right_leg', 'root', (-4.5, 8, 0), (0, 0, 0), [(-4, 0, -4, 8, 16, 8, 'limb')]),
   ('left_leg', 'root', (4.5, 8, 0), (0, 0, 0), [(-4, 0, -4, 8, 16, 8, 'limb')])]),
}

def pack(mob):
    """Shelf-pack box UV footprints into a TS x TS sheet."""
    x = y = rowh = 0; out = []
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

def crack(dr, x0, y0, w, h, col, rnd, n=1):
    for _ in range(n):
        x, y = x0 + rnd.randrange(max(1, w)), y0
        while y < y0 + h:
            dr.point((x, y), fill=col)
            x = min(x0 + w - 1, max(x0, x + rnd.choice((-1, 0, 0, 1)))); y += 1

def paint(name, mob):
    rnd = random.Random(name)
    base = Image.new('RGBA', (TS, TS), (0, 0, 0, 0)); glow = base.copy()
    db, dg = ImageDraw.Draw(base), ImageDraw.Draw(glow)
    pal = [hx(c) for c in mob['pal']]; gc = hx(mob['glow']) + (255,); ec = hx(mob['eye']) + (255,)
    cc = hx(mob.get('crystal', mob['eye'])) + (255,)
    boxes = [b for p in mob['parts'] for b in p[4]]
    for (u, v), b in zip(pack(mob), boxes):
        w, h, d = (math.ceil(q) for q in b[3:6]); role = b[6]
        for fname, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for yy in range(fy, fy + fh):
                for xx in range(fx, fx + fw):
                    if role in ('robe', 'hood'): c = pal[2]
                    elif role == 'spike': c = tuple(int(q * 0.6) for q in cc[:3])
                    else: c = pal[0] if rnd.random() < 0.7 else pal[1]
                    j = rnd.randint(-6, 6)
                    edge = xx in (fx, fx + fw - 1) or yy in (fy, fy + fh - 1)
                    k = 0.7 if edge and fw > 2 and fh > 2 else 1.0
                    db.point((xx, yy), fill=tuple(max(0, min(255, int((q + j) * k))) for q in c) + (255,))
            if role == 'blade': dg.rectangle([fx, fy, fx + fw - 1, fy + fh - 1], fill=gc)
            elif role in ('orb', 'sigil'): dg.rectangle([fx, fy, fx + fw - 1, fy + fh - 1], fill=ec if role == 'orb' else gc)
            elif role == 'spike' and fname != 'bottom':
                dg.rectangle([fx, fy, fx + fw - 1, fy + fh - 1], fill=cc[:3] + (150,))
            elif role in ('body', 'limb', 'fist') and fname in ('front', 'right', 'left', 'back') and fh > 3:
                crack(dg, fx, fy, fw, fh, gc, rnd, n=2 if role == 'body' else 1)
            elif role == 'robe' and fname != 'top' and fh > 4:
                dg.line([fx, fy + fh - 1, fx + fw - 1, fy + fh - 1], fill=gc)
            if role == 'head' and fname == 'front':
                cx, ey = fx + fw // 2, fy + fh // 2 - (1 if fh >= 8 else 0)
                s = max(1, fw // 8)
                if mob['eyes'] == 'slit': dg.rectangle([cx - 2 * s, ey, cx + 2 * s - 1, ey + s - 1], fill=ec)
                else:
                    if mob['eyes'] == 'hollow':
                        db.rectangle([cx - 3 * s, ey - s, cx - 1, ey + s], fill=(6, 4, 8, 255))
                        db.rectangle([cx, ey - s, cx + 3 * s - 1, ey + s], fill=(6, 4, 8, 255))
                    dg.rectangle([cx - 2 * s, ey, cx - s - 1 + s // 2, ey + s - 1], fill=ec)
                    dg.rectangle([cx + s, ey, cx + 2 * s - 1 + s // 2, ey + s - 1], fill=ec)
        if role == 'hood' and w >= 8:  # dark face opening, glow sits on inner head
            fx, fy, fw, fh = faces(u, v, w, h, d)['front']
            db.rectangle([fx + 2, fy + 2, fx + fw - 3, fy + fh - 1], fill=(0, 0, 0, 0))
    os.makedirs(TEX + 'entity', exist_ok=True)
    base.save(TEX + f'entity/{name}.png', optimize=True)
    glow.save(TEX + f'entity/{name}_glow.png', optimize=True)
    w = glow.copy(); px = w.load()
    for yy in range(TS):
        for xx in range(TS):
            r, g, b, a = px[xx, yy]
            if a: L = max(r, g, b); m = min(255, L + 90); px[xx, yy] = (m, m, m, a)
    w.save(TEX + f'entity/{name}_glow_rank.png', optimize=True)

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
            pose = f'PartPose.offsetAndRotation({f(piv[0])}, {f(piv[1])}, {f(piv[2])}, {f(D(rot[0]))}, {f(D(rot[1]))}, {f(D(rot[2]))})'
            out.append(f'        root.addOrReplaceChild("{pname}", {cb}, {pose});')
        out += [f'        return LayerDefinition.create(mesh, {TS}, {TS});', '    }', '']
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

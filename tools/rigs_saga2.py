"""1.2 Hollow Crown bosses, v2 rework (replaces rigs_saga.py output).
Built bone by bone (no stock humanoid): tapered limbs from stacked shrinking boxes, layered robes/plates, sculpted faces,
orbiting accessories, a translucent glass layer (Vaelith) and emissive runes. Textures are painted at 4x and Lanczos'd down
into a 512px sheet (k=2 -> 2 texels per model px) with the same lighting ramp as the 1.1 bosses.
Animations are authored here with anticipation / impact / follow-through, and every attack also gets a baked
`freeze_<anim>` pose at its impact frame so screenshots and the clip check look at the real mid-swing.
Run: python3 rigs_saga2.py"""
import math, sys, json, random
from PIL import Image, ImageFilter
import gen_geo_assets as G
Rig, kf = G.Rig, G.kf
R0 = [0, 0, 0]
WB = 0

def taper(r, bone, x0, z0, y0, y1, w0, w1, d0, d1, n, role, x1=None, z1=None, inflate=0.0):
    """Stack n boxes from y0 to y1 whose width/depth (and centre) interpolate: a tapered, optionally bent limb."""
    x1 = x0 if x1 is None else x1; z1 = z0 if z1 is None else z1; h = (y1 - y0) / n
    for i in range(n):
        t = (i + 0.5) / n
        r.box(bone, (x0 + (x1 - x0) * t, y0 + h * (i + 0.5), z0 + (z1 - z0) * t),
              (w0 + (w1 - w0) * t, abs(h) + 0.25, d0 + (d1 - d0) * t), role, inflate=inflate)

def shard(r, bone, base, length, w, rot, role='glass', tip='spike'):
    """A crystal: thick body + thin tip, both hinged on the base point so they lean together."""
    x, y, z = base; sgn = 1 if length > 0 else -1; L = abs(length)
    r.box(bone, (x, y + sgn * L * 0.33, z), (w, L * 0.66, w * 0.7), role, rot=rot, pivot=base)
    r.box(bone, (x, y + sgn * L * 0.83, z), (w * 0.5, L * 0.36, w * 0.4), tip, rot=rot, pivot=base)

# ============================================================== Vaelith, the Glass Choir
def rig_vaelith():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0)); r.bone('body', 'root', (0, 24, 0))
    # --- skirt: dark robe column tapering to a point, wrapped in three rings of hanging glass shards
    r.bone('skirt', 'body', (0, 25, 0))
    taper(r, 'skirt', 0, 0.3, 25, 5, 11, 2.5, 8, 2.2, 6, 'robe')
    rings = ((12, 5.6, 18, 14, 24, 'glass'), (9, 4.4, 12, 24, 23.5, 'spike'), (8, 6.4, 8, 42, 25.5, 'glass'))
    for ri, (n, rad, ln, ang, y, role) in enumerate(rings):
        for i in range(n):
            a = 2 * math.pi * (i + 0.5 * ri) / n; cx, cz = math.sin(a) * rad, math.cos(a) * rad * 0.78
            l = ln * (0.78 + 0.44 * ((i * 7 + ri * 3) % 5) / 4)
            shard(r, 'skirt', (cx, y, cz), -l, 2.4 - 0.4 * ri, (math.cos(a) * ang, 0, -math.sin(a) * ang), role)
    B('skirt', (0, 25.3, 0), (12.4, 2.6, 8.8), 'pauldron'); B('skirt', (0, 23.6, 0), (10, 1.2, 7.4), 'glass')
    for i in range(5): B('skirt', (-4 + i * 2, 25.3, -4.6), (1.1, 1.1, 0.5), 'core')
    for side, s in (('right', -1), ('left', 1)):   # glass ribbons trailing from the back of the waist (3-link chains)
        prev = 'skirt'; y = 25
        for j, ln in enumerate((8, 8, 7)):
            bn = r.bone(f'ribbon_{side}{j}', prev, (s * 2.6, y, 4.4)); B(bn, (s * 2.6, y - ln / 2, 4.5), (2.6, ln, 0.5), 'veil' if j < 2 else 'veilhem')
            prev = bn; y -= ln
    # --- torso: narrow waist, ribcage of glass plates, flaring to sharp shoulders
    r.bone('torso', 'body', (0, 25.5, 0))
    taper(r, 'torso', 0, 0.3, 25.5, 33, 7, 9.6, 5.2, 6.4, 3, 'body')
    for i in range(3):
        for s in (-1, 1): B('torso', (s * (3.2 + i * 0.4), 27.2 + i * 2.1, -2.7), (2.6, 1.0, 1.0), 'glass', rot=(0, 0, s * 12))
    B('torso', (0, 29, -3.0), (2, 6, 0.8), 'breastplate')
    r.bone('chest', 'torso', (0, 33, 0))
    taper(r, 'chest', 0, 0.4, 33, 46, 9.4, 13.0, 5.8, 6.6, 4, 'chest')
    for s in (-1, 1):
        B('chest', (s * 3.0, 41.6, -3.6), (5.6, 7.4, 1.0), 'breastplate', rot=(-6, s * 18, s * -16))     # V-shaped pectoral plates
        B('chest', (s * 2.6, 36.4, -3.5), (3.8, 3.6, 0.9), 'breastplate', rot=(-2, s * 12, s * 10))
        for j in range(3): B('chest', (s * (5.6 - j * 0.2), 35.5 + j * 2.6, -1.0), (1.0, 2.0, 4.6), 'glass', rot=(0, 0, s * (20 - j * 6)))   # flank ribs
    B('chest', (0, 40.5, -5.1), (3.6, 3.6, 1.2), 'core'); B('chest', (0, 40.5, -5.2), (5.4, 5.4, 1.6), 'glass', rot=(0, 0, 45))
    for i in range(8):   # collar: crystal ruff flaring out around the neck
        a = 2 * math.pi * i / 8; x, z = math.sin(a) * 4.0, math.cos(a) * 3.0
        shard(r, 'chest', (x, 45.5, z), 5.5 - (i % 2) * 1.5, 1.6, (-math.cos(a) * 30, 0, math.sin(a) * 30), 'glass')
    for s in (-1, 1):   # dorsal fins
        for j in range(3): shard(r, 'chest', (s * (1.6 + j * 1.6), 43 - j * 3, 3.6), 9 - j * 2, 1.7, (-35, 0, s * (8 + j * 10)), 'spike', 'glass')
    # --- neck, head, mask, crown, halo
    r.bone('neck', 'chest', (0, 46, -0.3)); taper(r, 'neck', 0, -0.3, 45.5, 50, 3.4, 2.6, 3.2, 2.6, 2, 'body')
    r.bone('head', 'neck', (0, 50, -0.4))
    B('head', (0, 52, -0.3), (4.2, 4, 4.8), 'skull'); B('head', (0, 56, 0), (6.2, 5, 6.4), 'skull'); B('head', (0, 59.2, 0.7), (5, 1.8, 5), 'skull')
    B('head', (0, 55, -3.4), (5.6, 7.6, 0.8), 'mask'); B('head', (0, 57.6, -3.95), (6.0, 0.9, 0.8), 'mask')
    B('head', (0, 55.4, -3.95), (0.8, 2.6, 0.6), 'mask'); B('head', (0, 51.7, -2.8), (3.4, 1.6, 0.8), 'mask')
    for s in (-1, 1):
        B('head', (s * 2.75, 54.4, -3.0), (1.0, 4.4, 1.4), 'mask', rot=(0, s * 28, 0))
        B('head', (s * 1.45, 56.4, -3.86), (1.5, 0.5, 0.3), 'core')
        for j in range(2): B('head', (s * (1.2 + j * 0.5), 55.2 - j * 1.2, -3.86), (0.3, 1.1, 0.25), 'core')   # glass tears
    B('head', (0, 53.0, -3.86), (1.5, 1.7, 0.3), 'void'); B('head', (0, 53.0, -3.9), (0.8, 0.9, 0.25), 'core')   # the singing mouth
    for i, (x, h, rz) in enumerate(((-2.6, 5, 30), (-1.3, 8, 14), (0, 11, 0), (1.3, 8, -14), (2.6, 5, -30))):
        shard(r, 'head', (x, 59.5, 0.8), h, 1.5, (-22, 0, rz), 'spike', 'glass')
    r.bone('halo', 'head', (0, 58, 5.2))
    for i in range(20):   # choir halo: a wide ring of glass blades with a glowing inner circlet
        a = 2 * math.pi * i / 20; x, y = math.sin(a) * 10.5, 58 + math.cos(a) * 10.5
        B('halo', (x, y, 5.4), (1.2, 3.4 if i % 2 else 4.8, 0.6), 'glass' if i % 2 else 'spike', rot=(0, 0, -math.degrees(a)))
        xi, yi = math.sin(a) * 8.4, 58 + math.cos(a) * 8.4
        B('halo', (xi, yi, 5.4), (2.8, 0.6, 0.4), 'core', rot=(0, 0, -math.degrees(a) + 90))
    # --- arms: long, crystalline, tapered; glass bracers; finger-blades; a conducting blade in each hand
    for side, s in (('right', -1), ('left', 1)):
        x = s * 8.8
        arm = r.bone(f'{side}_arm', 'chest', (x, 44.5, 0))
        for j, (w, d, rz, y) in enumerate(((6.4, 7.4, 22, 46.2), (5.4, 6.6, 34, 44.6), (4.0, 5.4, 46, 43.2))):
            B(arm, (x + s * (0.4 + j * 1.3), y, 0), (w, 1.7, d), 'pauldron', rot=(0, 0, -s * rz))
        shard(r, arm, (x + s * 1.2, 46.6, 0.6), 8, 1.6, (-12, 0, -s * 24), 'spike', 'glass'); shard(r, arm, (x + s * 2.4, 46, -0.4), 5.5, 1.3, (8, 0, -s * 48), 'glass')
        taper(r, arm, x, 0, 44, 33.5, 3.6, 2.6, 3.6, 2.7, 3, 'limb')
        B(arm, (x + s * 1.55, 39.5, 0), (0.9, 6.5, 2.6), 'glass')
        fa = r.bone(f'{side}_forearm', arm, (x, 33.5, 0))
        shard(r, fa, (x, 33.5, 1.4), -5, 1.2, (-140, 0, 0), 'spike')   # elbow spur pointing back
        taper(r, fa, x, 0, 33.5, 22.5, 2.9, 2.1, 2.9, 2.1, 3, 'limb')
        B(fa, (x, 27.2, 0), (3.7, 6.4, 3.7), 'glass'); B(fa, (x, 30.6, 0), (4.0, 0.8, 4.0), 'pauldron'); B(fa, (x, 24.1, 0), (3.9, 0.8, 3.9), 'pauldron')
        shard(r, fa, (x + s * 1.6, 25.5, 0.4), 8, 1.2, (0, 0, -s * 10), 'spike', 'glass')
        hd = r.bone(f'{side}_hand', fa, (x, 22.5, 0))
        B(hd, (x, 21, -0.2), (2.5, 3, 1.7), 'limb')
        for j in range(4): B(hd, (x + (j - 1.5) * 0.62, 18.2, -0.7 - 0.1 * j), (0.5, 3.6, 0.5), 'spike', rot=(14 + j * 5, 0, s * (j - 1.5) * 5), pivot=(x, 20, -0.5))
        B(hd, (x - s * 1.4, 20, -1.0), (0.5, 2.6, 0.5), 'spike', rot=(30, 0, s * 25))
        bt = r.bone(f'{side}_baton', hd, (x, 19.6, -0.6))
        B(bt, (x, 19.6, -1.6), (1.1, 1.1, 3.2), 'grip'); B(bt, (x, 19.6, -3.6), (2.2, 2.2, 1.0), 'core')
        B(bt, (x, 19.6, -8.6), (1.5, 0.9, 9.0), 'glass'); B(bt, (x, 19.6, -15.2), (0.9, 0.6, 4.6), 'spike'); B(bt, (x, 19.6, -18.0), (0.5, 0.4, 1.6), 'core')
    # --- the choir: four singing masks on a slow orbit, and a loose storm of floating shards
    r.bone('choir', 'root', (0, 46, 0))
    for i in range(4):
        a = 2 * math.pi * i / 4 + math.pi / 4; x, z = math.sin(a) * 18, math.cos(a) * 18; yaw = math.degrees(a)
        bn = r.bone(f'choir_mask{i}', 'choir', (x, 46, z))
        B(bn, (x, 46, z), (4.6, 6.4, 0.8), 'mask', rot=(0, yaw, 0)); B(bn, (x, 48.2, z), (5.0, 0.8, 1.0), 'mask', rot=(0, yaw, 0))
        B(bn, (x, 44.2, z), (1.4, 1.6, 1.0), 'core', rot=(0, yaw, 0))
        for k in (-1, 1): B(bn, (x + math.cos(a) * k * 1.1, 46.6, z - math.sin(a) * k * 1.1), (1.0, 0.45, 1.0), 'core', rot=(0, yaw, 0))
        B(bn, (x, 51, z), (1.2, 3.4, 0.6), 'glass', rot=(0, yaw, 0)); B(bn, (x, 40.4, z), (3.6, 4.4, 0.4), 'veilhem', rot=(0, yaw, 0))
    r.bone('shards', 'root', (0, 40, 0))
    rnd = random.Random(7)
    for i in range(12):
        a = 2 * math.pi * i / 12; rad = 11 + rnd.random() * 3; y = 30 + (i * 5) % 26
        shard(r, 'shards', (math.sin(a) * rad, y, math.cos(a) * rad), 4 + rnd.random() * 3, 1.3, (rnd.uniform(-40, 40), 0, rnd.uniform(-40, 40)), 'glass')
    return r

# ============================================================== The Fallen Hierophant
def rig_hierophant():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    # --- legs (mostly hidden): the front robe panels ride on the thighs/shins so the hem strides with him
    for side, s in (('right', -1), ('left', 1)):
        x = s * 3.6
        lg = r.bone(f'{side}_leg', 'root', (x, 22, 0)); taper(r, lg, x, 0, 22, 11, 4.6, 4.0, 4.6, 4.0, 2, 'limb')
        B(lg, (s * 4.1, 16.6, -3.9), (8.0, 11.2, 1.1), 'robe'); B(lg, (s * 7.6, 16.6, -0.2), (1.1, 11.2, 7.8), 'robe')
        sh = r.bone(f'{side}_shin', lg, (x, 11, 0)); taper(r, sh, x, 0, 11, 1.5, 3.8, 3.4, 3.8, 3.4, 2, 'limb')
        B(sh, (x, 1, -1.2), (4.4, 2, 7), 'gauntlet')
        B(sh, (s * 4.3, 6.4, -4.25), (8.6, 10.4, 1.1), 'robe'); B(sh, (s * 8.0, 6.4, -0.3), (1.1, 10.4, 8.6), 'robe')
        B(sh, (s * 4.4, 0.9, -4.45), (8.9, 1.8, 1.2), 'capehem'); B(sh, (s * 8.2, 0.9, -0.3), (1.2, 1.8, 8.9), 'capehem')
        B(sh, (s * 4.4, 3.0, -4.9), (6.4, 0.9, 0.3), 'rune'); B(sh, (s * 4.4, 4.6, -4.85), (8.4, 0.7, 0.3), 'collar')
    r.bone('hips', 'root', (0, 22, 0))
    B('hips', (0, 22, 0), (16, 3.4, 10.4), 'belt')
    for i, x in enumerate((-5.5, -2, 2, 5.5)): B('hips', (x, 22, -5.4), (1.6, 1.6, 0.6), 'guard')
    B('hips', (6.5, 19.2, -4.3), (2.4, 3.2, 2.2), 'belt'); B('hips', (-6.6, 18.6, -3.4), (2.0, 2.4, 2.0), 'mask')   # pouch, skull trinket
    for nm, z in (('robe_back', 4.6),):
        r.bone(nm, 'hips', (0, 22, z)); B(nm, (0, 16.3, z + 0.4), (16, 11.4, 1.2), 'robe'); B(nm, (0, 16.3, z + 1.05), (3.2, 11.4, 0.3), 'collar')
        r.bone('robe_back2', nm, (0, 10.6, z + 0.4)); B('robe_back2', (0, 5.6, z + 0.7), (17, 10.4, 1.2), 'robe'); B('robe_back2', (0, 0.9, z + 0.8), (17.4, 1.8, 1.3), 'capehem')
        B('robe_back2', (0, 3.0, z + 1.4), (12, 0.9, 0.3), 'rune')
    r.bone('tabard', 'hips', (0, 21, -5.4))
    B('tabard', (0, 12.4, -5.6), (4.6, 17, 0.6), 'collar'); B('tabard', (0, 14.5, -6.0), (3.4, 3.4, 0.4), 'sigil'); B('tabard', (0, 3.4, -5.6), (3.2, 2.0, 0.6), 'capehem')
    # --- torso (hunched), mantle, stole, sigil plaque, prayer beads, broken spinal spires
    r.bone('torso', 'hips', (0, 22.5, 0), (20, 0, 0))
    taper(r, 'torso', 0, 0, 22.5, 34, 12.4, 14.4, 8.4, 9.6, 2, 'robe')
    r.bone('chest', 'torso', (0, 34, 0))
    taper(r, 'chest', 0, 0.2, 34, 46, 14.4, 18.2, 9.6, 11.4, 3, 'robe')
    for j, (y, w, d, role) in enumerate(((46.2, 21, 13.4, 'cape'), (43.6, 22.4, 14.2, 'cape'), (41.2, 23.2, 14.8, 'capehem'))):
        B('chest', (0, y, 0.2), (w, 2.8, d), role)
    B('chest', (0, 40.2, -7.45), (21.6, 0.9, 0.4), 'collar')
    for s in (-1, 1):
        B('chest', (s * 3.4, 37, -6.0), (2.4, 18, 0.7), 'collar', rot=(0, 0, s * -6))
        B('chest', (s * 3.4, 28.4, -6.3), (2.8, 2.4, 0.6), 'capehem')
        shard(r, 'chest', (s * 5.2, 46.5, 3.6), 11, 2.0, (-24, 0, s * -16), 'helmspire', 'crown'); shard(r, 'chest', (s * 2.4, 46.5, 4.8), 7, 1.6, (-38, 0, s * -6), 'helmspire', 'crown')
    B('chest', (0, 37.4, -6.25), (6.4, 6.4, 0.8), 'guard', rot=(0, 0, 45)); B('chest', (0, 37.4, -6.75), (4.6, 4.6, 0.4), 'sigil', rot=(0, 0, 45))
    for i in range(9):
        t = (i - 4) / 4; B('chest', (t * 6.4, 42.6 - (1 - t * t) * 4.2, -6.6 - (1 - t * t) * 0.3), (1.2, 1.2, 1.2), 'orb')
    # --- head: deep layered hood, gaunt cracked porcelain face, hanging tatters, a tall two-leaf mitre
    r.bone('head', 'chest', (0, 46.5, -2.6))
    B('head', (0, 52.6, 0.6), (11.4, 12, 11.6), 'hood'); B('head', (0, 52.2, 3.2), (12.4, 10.6, 6.4), 'hood')
    B('head', (0, 58.0, -5.2), (12.2, 2.0, 3.0), 'hood', rot=(-18, 0, 0)); B('head', (0, 59.4, -2.4), (11.0, 1.6, 4), 'hood')
    for s in (-1, 1): B('head', (s * 5.7, 51.6, -4.0), (1.4, 10, 3.2), 'hood', rot=(0, s * 10, 0))
    B('head', (0, 52.4, -5.15), (9, 10.4, 0.4), 'void')
    B('head', (0, 53.2, -5.6), (6.6, 6.2, 0.8), 'mask'); B('head', (0, 55.1, -6.15), (6.2, 1.0, 0.7), 'mask')
    B('head', (0, 53.5, -6.1), (0.9, 2.6, 0.5), 'mask')
    for s in (-1, 1):
        B('head', (s * 2.4, 52.4, -5.9), (1.2, 2.0, 0.6), 'mask', rot=(0, s * 20, 0)); B('head', (s * 1.5, 54.0, -6.05), (1.5, 0.8, 0.25), 'core')
    B('head', (0, 49.6, -5.5), (4.2, 1.2, 0.7), 'mask'); B('head', (0, 50.6, -5.75), (3.0, 0.8, 0.3), 'void')   # hanging jaw, black gap
    for i, (x, ln) in enumerate(((-2.6, 6), (-1.3, 8), (0, 9.5), (1.3, 7.5), (2.6, 5.5))):
        B('head', (x, 48.4 - ln / 2, -5.4 + abs(x) * 0.15), (1.2, ln, 0.5), 'veilhem')
    r.bone('mitre0', 'head', (0, 58.6, 0.6))
    for j in range(4):   # front leaf and back leaf, tapering and parting into the mitre's V peak
        w = 10 - j * 1.8; y = 59.6 + j * 3.2
        B('mitre0', (0, y, -1.0 - j * 0.35), (w, 3.4, 1.4), 'mitre', rot=(-4 - j * 2, 0, 0))
        B('mitre0', (0, y, 2.4 + j * 0.35), (w, 3.4, 1.4), 'mitre', rot=(4 + j * 2, 0, 0))
        B('mitre0', (0, y, 0.7), (w - 0.6, 3.4, 2.4), 'void')
    B('mitre0', (0, 59.0, 0.7), (10.6, 1.8, 5.6), 'crown'); B('mitre0', (0, 66, -2.25), (0.9, 12, 0.4), 'crown')
    B('mitre0', (0, 63.6, -2.35), (3.6, 3.6, 0.3), 'rune', rot=(0, 0, 45)); B('mitre0', (0, 63.6, -2.45), (1.6, 1.6, 0.3), 'core', rot=(0, 0, 45))
    r.bone('mitre1', 'mitre0', (0, 72, 0.7), (0, 0, 9))
    B('mitre1', (0, 73.6, -1.6), (2.4, 3.4, 1.2), 'mitre', rot=(-12, 0, 0)); B('mitre1', (0, 73.6, 3.0), (2.4, 3.4, 1.2), 'mitre', rot=(12, 0, 0))
    B('mitre1', (0.6, 76.6, 0.7), (1.8, 2.2, 1.8), 'crown', rot=(0, 45, 20))
    for s in (-1, 1):   # lappets: two ribbons hanging down the back of the mitre
        bn = r.bone(f'lappet_{side_name(s)}', 'head', (s * 2.6, 58.4, 5.2)); B(bn, (s * 2.6, 52.8, 5.6), (2.0, 11.2, 0.4), 'collar'); B(bn, (s * 2.6, 46.6, 5.6), (2.4, 1.6, 0.5), 'capehem')
    # --- arms: bulky shoulders, flaring bell sleeves with gold cuffs, skeletal clawed hands
    for side, s in (('right', -1), ('left', 1)):
        x = s * 10.6
        arm = r.bone(f'{side}_arm', 'chest', (x, 45, 0))
        B(arm, (x, 43.2, 0), (6.8, 6.2, 7.4), 'robe'); taper(r, arm, x, 0, 43, 36, 6.2, 6.6, 6.2, 6.6, 2, 'sleeve')
        fa = r.bone(f'{side}_forearm', arm, (x, 36, 0))
        taper(r, fa, x, 0, 36, 27, 6.6, 8.6, 6.6, 8.6, 4, 'sleeve')
        B(fa, (x, 27.6, 0), (9.0, 1.3, 9.0), 'collar'); B(fa, (x, 26.95, 0), (7.0, 0.2, 7.0), 'void')
        B(fa, (x, 27.0, 3.4), (7.6, 9.0, 1.2), 'capehem', rot=(14, 0, 0), pivot=(x, 31, 3.4))   # long drape hanging off the back of the sleeve
        hd = r.bone(f'{side}_hand', fa, (x, 27.5, -0.4))
        B(hd, (x, 25.6, -0.4), (3.0, 3.2, 2.4), 'claw')
        for j in range(4):
            fx = x + (j - 1.5) * 0.75
            B(hd, (fx, 22.6, -0.9), (0.6, 3.2, 0.6), 'claw', rot=(20, 0, 0), pivot=(fx, 24.2, -0.6)); B(hd, (fx, 20.4, -2.2), (0.5, 2.2, 0.5), 'claw', rot=(55, 0, 0), pivot=(fx, 21.4, -1.4))
        B(hd, (x - s * 1.7, 24.4, -1.2), (0.6, 2.6, 0.6), 'claw', rot=(35, 0, s * 25))
    # --- the censer staff: banded haft, caged head with a burning gem, crescent horns, censer on a 3-link chain
    x, z = -10.6, -0.6
    r.bone('staff', 'right_hand', (x, 25.6, z))
    B('staff', (x, 0.5, z), (1.2, 4, 1.2), 'crown')
    taper(r, 'staff', x, z, 2.5, 74, 2.0, 2.4, 2.0, 2.4, 6, 'shaft')
    for y in (10, 20, 36, 48, 60, 70): B('staff', (x, y, z), (3.0, 1.2, 3.0), 'guard')
    for y in range(24, 33, 2): B('staff', (x, y, z), (2.7, 0.9, 2.7), 'belt', rot=(0, 45, 0))   # grip wrapping
    B('staff', (x, 75.5, z), (4.4, 3.0, 4.4), 'guard'); B('staff', (x, 77.4, z), (5.6, 1.0, 5.6), 'crown')
    for i in range(4):
        a = math.pi / 2 * i + math.pi / 4; dx, dz = math.sin(a), math.cos(a)
        B('staff', (x + dx * 2.6, 80.6, z + dz * 2.6), (1.1, 6, 1.1), 'crown', rot=(dz * 22, 0, -dx * 22), pivot=(x + dx * 2.2, 77.8, z + dz * 2.2))
        B('staff', (x + dx * 3.2, 86, z + dz * 3.2), (1.0, 4.4, 1.0), 'crown', rot=(-dz * 26, 0, dx * 26), pivot=(x + dx * 3.6, 83.6, z + dz * 3.6))
    B('staff', (x, 82.4, z), (3.0, 3.0, 3.0), 'core', rot=(45, 45, 0)); B('staff', (x, 82.4, z), (4.2, 4.2, 4.2), 'orbshell', rot=(45, 45, 0))
    for s in (-1, 1): shard(r, 'staff', (x + s * 2.4, 77, z), 7, 1.2, (0, 0, -s * 50), 'crown', 'crown')
    B('staff', (x, 89.6, z), (1.0, 3, 1.0), 'crown')
    B('staff', (x - 3.2, 74.6, z), (5.6, 1.0, 1.0), 'guard')
    r.bone('censer', 'staff', (x - 5.6, 74.6, z))
    for j in range(3): B('censer', (x - 5.6, 73.2 - j * 1.9, z), (0.6, 1.6, 0.6), 'guard', rot=(0, 45 * j, 0))
    B('censer', (x - 5.6, 67.4, z), (4.6, 3.6, 4.6), 'orb'); B('censer', (x - 5.6, 69.8, z), (3.4, 1.4, 3.4), 'crown'); B('censer', (x - 5.6, 70.9, z), (1.6, 1.0, 1.6), 'crown')
    B('censer', (x - 5.6, 65.2, z), (3.0, 1.0, 3.0), 'crown')
    for k in range(4):
        a = math.pi / 2 * k; B('censer', (x - 5.6 + math.sin(a) * 2.36, 67.6, z + math.cos(a) * 2.36), (1.0, 1.6, 1.0), 'core', rot=(0, math.degrees(a), 0))
    B('censer', (x - 5.6, 64, z), (2.4, 2.6, 2.4), 'smoke')
    # --- off hand: a profane sigil burning above the palm; three more orbit behind his back
    r.bone('sigil_hand', 'left_hand', (10.6, 22, -3))
    B('sigil_hand', (10.6, 20.6, -3.8), (5.6, 5.6, 0.4), 'rune', rot=(0, 0, 45)); B('sigil_hand', (10.6, 20.6, -3.9), (2.4, 2.4, 0.4), 'core')
    for k in range(4):
        a = math.pi / 2 * k; B('sigil_hand', (10.6 + math.sin(a) * 4.4, 20.6 + math.cos(a) * 4.4, -3.8), (1.2, 1.2, 0.4), 'core', rot=(0, 0, 45))
    r.bone('sigils', 'root', (0, 50, 9))
    for k in range(3):
        a = 2 * math.pi * k / 3; px, py = math.sin(a) * 9, 50 + math.cos(a) * 9
        B('sigils', (px, py, 9.5), (4.4, 4.4, 0.4), 'rune', rot=(0, 0, 45 + math.degrees(a))); B('sigils', (px, py, 9.4), (1.8, 1.8, 0.4), 'core')
    # --- cape: long tattered train in three links
    prev, y = 'chest', 44
    for j, (ln, w) in enumerate(((12, 18), (12, 19), (11, 20))):
        bn = r.bone(f'cape{j + 1}', prev, (0, y, 6.4), (6, 0, 0) if j == 0 else (0, 0, 0))
        B(bn, (0, y - ln / 2, 6.6), (w, ln, 1.0), 'cape' if j < 2 else 'capehem'); B(bn, (0, y - ln / 2, 7.15), (3.0, ln, 0.2), 'rune' if j == 1 else 'collar')
        prev, y = bn, y - ln
    return r

def side_name(s): return 'right' if s < 0 else 'left'

PAL = {
 'vaelith': dict(who='vaelith', k=2, ts=[512, 512], boss=True, pal=dict(base='#6B7FA6', dark='#1A2038', accent='#A8DDF2', robe='#2C3E6C', cape='#7FB8DA'),
                 trim='#D6F1FA', glow='#7FE0FF', eye='#F2FCFF', core='#9AF4FF', crystal='#B4ECFF', eyes='visor'),
 'fallen_hierophant': dict(who='fallen_hierophant', k=2, ts=[512, 512], boss=True, pal=dict(base='#3A2F2A', dark='#120C0B', accent='#6E5A3C', robe='#5A1A2A', cape='#3A0E1A'),
                 trim='#C9A048', glow='#B83CF0', eye='#F07CFF', core='#C65CFF', orb='#7A5A2E', eyes='visor'),
}

# ------------------------------------------------------------------ painting: extra roles on top of the shared Painter
class Painter(G.Painter):
    # ---- hand-tuned materials per boss (consistent top-left key light, soft bottom AO, bevelled edges)
    def shade(s, fn, t, u):
        return G.LIGHT[fn] * (1.12 - 0.3 * t) * (1.0 + (0.06 if u < 0.25 else -0.04 if u > 0.8 else 0))
    def style(s, F, role):
        k = s.k; name = s.mob['who']; e = max(1, k // 2)
        def each(fn_body):
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); ed = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy)
                        fn_body(fn, xx, yy, u, t, ed, fw, fh)
        if name == 'vaelith' and role in ('chest', 'body', 'limb', 'skull', 'jaw'):
            lo, hi = (16, 20, 44), (58, 74, 122)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                facet = int((xx * 0.7 + yy * 1.3) / (5 * k)) % 3       # broad diagonal facets
                kk = s.shade(fn, t, u) * (0.82, 1.0, 1.22)[facet]
                c = G.ramp(G.mix(lo, hi, 0.45 + 0.3 * s.n(xx, yy, 3 * k)), kk)
                if ed < e: c = G.mix(c, (170, 222, 245), 0.55 if fn in ('up', 'north') else 0.3)
                elif int((xx * 0.7 + yy * 1.3)) % (5 * k) == 0: c = G.mix(c, (150, 210, 240), 0.5); s.gl(xx, yy, s.gc, 0.35)
                s.px(xx, yy, c)
            each(f); return True
        if name == 'vaelith' and role in ('pauldron', 'breastplate', 'belt', 'gauntlet'):
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                c = G.ramp(G.mix((112, 160, 198), (200, 232, 248), 0.5 + 0.4 * (1 - t) * s.n(xx, yy, 2 * k)), kk)
                if ed < e: c = (236, 250, 255) if (fn in ('up', 'north') and t < 0.5) else G.mul(c, 0.62)
                elif ed < 2 * e: c = G.mul(c, 1.08)
                if fh > 6 * k and abs(u - 0.5) < 0.03: c = G.mix(c, (255, 255, 255), 0.4); s.gl(xx, yy, s.gc, 0.4)
                s.px(xx, yy, c)
            each(f); return True
        if name == 'vaelith' and role == 'mask':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.0 - 0.18 * abs(u - 0.5))
                c = G.ramp(G.mix((214, 228, 244), (250, 252, 255), 0.6 + 0.3 * s.n(xx, yy, 3 * k)), max(kk, 1.0))
                if ed < e: c = G.mix(c, (120, 170, 210), 0.5)
                s.px(xx, yy, c); s.gl(xx, yy, (200, 236, 255), 0.42 if fn == 'north' else 0.25)
            each(f); return True
        if name == 'fallen_hierophant' and role in ('robe', 'sleeve', 'hood', 'cape', 'capehem'):
            base = {'robe': (104, 22, 40), 'sleeve': (96, 20, 38), 'hood': (70, 14, 28), 'cape': (58, 12, 26), 'capehem': (58, 12, 26)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                folds = max(1, fw // (3 * k))
                fold = 0.5 + 0.5 * math.sin(u * math.pi * 2 * folds + (xx // fw) * 0.0) if fn not in ('up', 'down') else 0.5
                kk = s.shade(fn, t, u) * (0.78 + 0.34 * fold)
                c = G.ramp(G.mix(G.mul(base, 0.7), G.mul(base, 1.25), s.n(xx, yy, 2 * k)), kk)
                if (xx + yy) % 2 == 0: c = G.mul(c, 0.94)                                   # velvet weave
                a = 255
                if role == 'capehem' and fn not in ('up', 'down') and t > 0.4:           # torn hem
                    if s.n(xx, yy, k) > 0.25 + 1.1 * (1 - t): a = 0
                    elif t > 0.75: c = G.mul(c, 0.7)
                if ed < e and fn not in ('up', 'down') and (t < 0.1): c = G.mul(c, 1.25)
                s.px(xx, yy, c, a)
            each(f); return True
        if name == 'fallen_hierophant' and role in ('collar', 'crown', 'guard'):
            gold = (196, 150, 64)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                c = G.ramp(G.mix(G.mul(gold, 0.62), G.mul(gold, 1.15), s.n(xx, yy, k)), kk)
                if ed < e: c = G.mul(c, 0.5)
                elif role == 'collar' and fw > 3 * k and fh > 3 * k:
                    cx, cy = (xx - F[fn][0]) % (3 * k) - 1.5 * k, (yy - F[fn][1]) % (3 * k) - 1.5 * k
                    if abs(abs(cx) + abs(cy) - k) < 0.7: c = G.mul(c, 0.55)                # embroidered diamonds
                    elif abs(cx) + abs(cy) < 0.6 * k: c = (250, 222, 140)
                s.px(xx, yy, c)
            each(f); return True
        if name == 'fallen_hierophant' and role == 'mask':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.0 - 0.25 * abs(u - 0.5))
                c = G.ramp(G.mix((176, 166, 150), (232, 224, 206), 0.5 + 0.4 * s.n(xx, yy, 2 * k)), kk)
                if ed < e: c = G.mul(c, 0.65)
                s.px(xx, yy, c)
            each(f)
            for fn in ('north',):   # glowing cracks across the porcelain
                fx, fy, fw, fh = F[fn]
                if fw < 4 * k: continue
                x = fx + fw // 3
                for yy in range(fy, fy + fh):
                    if fx <= x < fx + fw: s.px(x, yy, (40, 10, 46)); s.gl(x, yy, s.gc, 0.9)
                    x += s.rnd.choice((-1, 0, 1))
            return True
        return False
    def cube(s, uv, c):
        role = c['role']; k = s.k
        w, h, d = G.cdims(c, k); F = G.faces(uv[0], uv[1], w, h, d)
        if role in ('glass', 'veil', 'veilhem'):
            gc = G.hx(s.mob['crystal']) if role == 'glass' else G.hx(s.mob['pal']['cape'])
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                L = G.LIGHT[fn]
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); edge = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy) < max(1, k // 2)
                        streak = ((xx - fx) + (yy - fy) * 0.6) % (6 * k) < k * 0.9
                        kk = L * (1.15 - 0.35 * t) * (0.9 + 0.2 * s.n(xx, yy))
                        col = G.ramp(G.mix(G.mul(gc, 0.55), gc, 0.4 + 0.6 * (1 - t)), kk)
                        a = 150 + int(50 * (1 - t))
                        if role == 'veilhem' and t > 0.55 and s.n(xx, yy, 2) > 0.2 + 0.9 * (1 - t): a = 0
                        if edge: col = G.mix(col, (240, 252, 255), 0.55); a = 235
                        if streak and role == 'glass': col = G.mix(col, (255, 255, 255), 0.45); a = max(a, 205)
                        if role != 'glass': a = int(a * 0.85)
                        s.px(xx, yy, col, a)
                        if a and (edge or (streak and role == 'glass')): s.gl(xx, yy, G.hx(s.mob['glow']), 0.35 if edge else 0.22)
            return
        if role == 'void':
            for fn, (fx, fy, fw, fh) in F.items():
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw): s.px(xx, yy, G.mix((6, 3, 9), (22, 10, 28), s.n(xx, yy)))
            return
        if role == 'mitre':   # ivory and gold vestment cloth with embroidered bands
            for fn, f in F.items():
                fx, fy, fw, fh = f
                if fw <= 0 or fh <= 0: continue
                s.fill(f, fn, (150, 132, 108), (214, 198, 168))
                tr = G.hx(s.mob['trim'])
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        if xx - fx < k or fx + fw - 1 - xx < k: s.px(xx, yy, G.ramp(tr, G.LIGHT[fn] * (0.8 + 0.3 * s.n(xx, yy))))
                        elif (yy - fy) % (6 * k) < k // 2 + 1: s.dk(xx, yy, 0.72)
            return
        if s.style(F, role): return
        super().cube(uv, c)
        if role == 'rune':   # rune plates read as carved glyph rings, not flat squares
            for fn in ('north', 'south'):
                fx, fy, fw, fh = F[fn]
                if fw < 4 or fh < 4: continue
                cx, cy = fx + fw / 2, fy + fh / 2
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        rr = math.hypot((xx - cx) / (fw / 2), (yy - cy) / (fh / 2))
                        ang = math.atan2(yy - cy, xx - cx)
                        on = 0.62 < rr < 0.8 or (rr < 0.55 and (abs(xx - cx) < k * 0.6 or abs(yy - cy) < k * 0.6)) or (0.8 < rr < 0.98 and int((ang + 4) * 4) % 3 == 0)
                        if on: s.px(xx, yy, (30, 6, 30)); s.gl(xx, yy, s.gc, 1.0)
                        else: s.dk(xx, yy, 0.55)

def downsample(name, mob, rig, hi, lo_k, hi_uvs, lo_uvs):
    """As G.downsample, but translucent roles keep their graded alpha instead of the cutout threshold."""
    W, H = mob['ts']; base = Image.new('RGBA', (W, H), (0, 0, 0, 0)); glow = base.copy()
    cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
    for (hu, hv), (lu, lv), c in zip(hi_uvs, lo_uvs, cs):
        hf = G.faces(hu, hv, *G.cdims(c, hi.k)); lf = G.faces(lu, lv, *G.cdims(c, lo_k)); soft = c['role'] in ('glass', 'veil', 'veilhem', 'smoke')
        for fn, (x, y, w, h) in lf.items():
            X, Y, Wd, Hd = hf[fn]
            if w <= 0 or h <= 0 or Wd <= 0 or Hd <= 0: continue
            b = hi.base.crop((X, Y, X + Wd, Y + Hd)).resize((w, h), Image.LANCZOS)
            a = b.getchannel('A') if soft else b.getchannel('A').point(lambda v: 255 if v >= 110 else 0)
            rgb = b.convert('RGB').filter(ImageFilter.UnsharpMask(radius=1, percent=45, threshold=2)); rgb.putalpha(a)
            base.paste(rgb, (x, y))
            g = hi.glow.crop((X, Y, X + Wd, Y + Hd)).resize((w, h), Image.LANCZOS)
            ga = g.getchannel('A').point(lambda v: 0 if v < 40 else min(255, int(v * 1.6)))
            g.putalpha(ga); glow.paste(g, (x, y))
    hi.base, hi.glow = base, glow; hi.bp, hi.gp = base.load(), glow.load()
    hi.save(name)

# ------------------------------------------------------------------ animation helpers
def ch(pairs, smooth=True): return kf([(t, list(v)) for t, v in pairs], smooth)
def lin(pairs): return kf([(t, list(v)) for t, v in pairs], False)
def loop(T, amp, axis, base=(0, 0, 0), phase=0.0, n=8):
    return kf(G.wave(T, amp, axis, n=n, phase=phase, base=base))
def spin(T, turns, axis=1):
    v = [0, 0, 0]; v[axis] = 360 * turns; return lin([(0, R0), (T, v)])
def hold(v, T): return lin([(0, v), (T, v)])
def A(length, bones, loop_=False): return dict(loop=loop_, length=length, bones=bones)

# ------------------------------------------------------------------ Vaelith animations
VG = {'right_arm': [-62, 0, -20], 'right_forearm': [-28, 0, 0], 'left_arm': [-62, 0, 20], 'left_forearm': [-28, 0, 0],
      'right_baton': [22, 0, 0], 'left_baton': [22, 0, 0], 'chest': [-4, 0, 0], 'head': [-6, 0, 0]}

def vaelith_anims():
    def floaty(T, lift=3, amp=1.2): return {'position': kf([(T * i / 8, [0, round(lift + amp * math.sin(2 * math.pi * i / 8), 3), 0]) for i in range(9)])}
    def common(T, turns=1):
        return {'halo': {'rotation': spin(T, turns, 2)}, 'shards': {'rotation': spin(T, -turns, 1)}, 'choir': {'rotation': spin(T, turns * 0.5, 1)},
                'ribbon_right0': {'rotation': loop(T, 6, 0, (8, 0, 0))}, 'ribbon_left0': {'rotation': loop(T, 6, 0, (8, 0, 0), 1.4)},
                'ribbon_right1': {'rotation': loop(T, 9, 0, (4, 0, 0), 0.8)}, 'ribbon_left1': {'rotation': loop(T, 9, 0, (4, 0, 0), 2.2)},
                'ribbon_right2': {'rotation': loop(T, 12, 0, (2, 0, 0), 1.6)}, 'ribbon_left2': {'rotation': loop(T, 12, 0, (2, 0, 0), 3.0)}}
    out = {}
    T = 3.0; b = common(T)
    b.update({'root': floaty(T), 'skirt': {'rotation': loop(T, 3, 2, phase=0.5)}, 'chest': {'rotation': loop(T, 2.5, 0)}, 'head': {'rotation': loop(T, 4, 2, phase=1)},
              'right_arm': {'rotation': loop(T, 4, 0, (-8, 0, -10))}, 'left_arm': {'rotation': loop(T, 4, 0, (-8, 0, 10), 2)},
              'right_forearm': {'rotation': loop(T, 5, 0, (-20, 0, 0), 1)}, 'left_forearm': {'rotation': loop(T, 5, 0, (-20, 0, 0), 3)}})
    out['idle'] = A(T, b, True)
    T = 1.6; b = common(T, 0.5)
    b.update({'root': floaty(T, 3.5, 0.8), 'body': {'rotation': hold([10, 0, 0], T)}, 'skirt': {'rotation': loop(T, 4, 0, (-14, 0, 0))},
              'right_arm': {'rotation': loop(T, 7, 0, (-10, 0, -10))}, 'left_arm': {'rotation': loop(T, 7, 0, (-10, 0, 10), math.pi)},
              'right_forearm': {'rotation': hold([-15, 0, 0], T)}, 'left_forearm': {'rotation': hold([-15, 0, 0], T)}, 'head': {'rotation': hold([-10, 0, 0], T)},
              'right_baton': {'rotation': hold([WB, 0, 0], T)}, 'left_baton': {'rotation': hold([WB, 0, 0], T)}})
    out['walk'] = A(T, b, True)
    for an, T in (('idle_combat', 2.4), ('walk_combat', 1.2)):
        b = common(T, 1); b['root'] = floaty(T, 4 if an == 'idle_combat' else 4.5, 1.0)
        for bn, base in VG.items(): b[bn] = {'rotation': loop(T, 5 if 'arm' in bn else 2, 2 if 'arm' in bn else 0, tuple(base), phase=len(bn))}
        b['skirt'] = {'rotation': loop(T, 4, 0, (12 if an == 'walk_combat' else 2, 0, 0))}
        if an == 'walk_combat': b['body'] = {'rotation': hold([8, 0, 0], T)}
        out[an] = A(T, b, True)
    g = lambda bn: VG.get(bn, R0)
    # ATTACK "Downbeat": rises and draws both blades up behind the head (0.45), holds a beat, then cuts down (impact 0.7)
    out['attack'] = A(1.2, {
        'root': {'position': ch([(0, [0, 4, 0]), (0.45, [0, 8, 0]), (0.58, [0, 8.5, 0]), (0.7, [0, 2, 0]), (0.85, [0, 1.5, 0]), (1.2, [0, 4, 0])])},
        'body': {'rotation': ch([(0, R0), (0.45, [-10, 0, 0]), (0.58, [-12, 0, 0]), (0.7, [16, 0, 0]), (0.85, [12, 0, 0]), (1.2, R0)])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.45, [-168, 0, -28]), (0.58, [-172, 0, -30]), (0.7, [-42, 0, -14]), (0.85, [-36, 0, -14]), (1.2, g('right_arm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.45, [-168, 0, 28]), (0.58, [-172, 0, 30]), (0.7, [-42, 0, 14]), (0.85, [-36, 0, 14]), (1.2, g('left_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.45, [-40, 0, 0]), (0.7, [-8, 0, 0]), (1.2, g('right_forearm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.45, [-40, 0, 0]), (0.7, [-8, 0, 0]), (1.2, g('left_forearm'))])},
        'right_baton': {'rotation': hold(g('right_baton'), 1.2)}, 'left_baton': {'rotation': hold(g('left_baton'), 1.2)},
        'chest': {'rotation': ch([(0, g('chest')), (0.45, [-14, 0, 0]), (0.7, [14, 0, 0]), (1.2, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.45, [-18, 0, 0]), (0.7, [10, 0, 0]), (1.2, g('head'))])},
        'skirt': {'rotation': ch([(0, R0), (0.58, [-10, 0, 0]), (0.75, [18, 0, 0]), (1.2, R0)])},
        'halo': {'scale': ch([(0, [1, 1, 1]), (0.58, [1.15, 1.15, 1]), (0.7, [0.9, 0.9, 1]), (1.2, [1, 1, 1])])}})
    # SWEEP "Glissando": winds the right blade far behind, twists, and rakes it horizontally across (impact 0.6)
    out['sweep'] = A(1.1, {
        'body': {'rotation': ch([(0, R0), (0.4, [0, 38, 0]), (0.6, [0, -42, 0]), (0.75, [0, -48, 0]), (1.1, R0)])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-90, 0, -95]), (0.6, [-92, 0, 10]), (0.75, [-88, 0, 18]), (1.1, g('right_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.4, [-30, 0, 0]), (0.6, [-6, 0, 0]), (1.1, g('right_forearm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-30, 0, 30]), (0.6, [-50, 0, 60]), (1.1, g('left_arm'))])},
        'right_baton': {'rotation': ch([(0, g('right_baton')), (0.4, [10, 0, 0]), (0.6, [-6, 0, 0]), (1.1, g('right_baton'))])},
        'head': {'rotation': ch([(0, g('head')), (0.4, [0, -25, 0]), (0.6, [0, 15, 0]), (1.1, g('head'))])},
        'skirt': {'rotation': ch([(0, R0), (0.4, [0, -20, 6]), (0.7, [0, 25, -6]), (1.1, R0)])}})
    # SPIN "Shard Storm" (impact 0.7)
    out['spin'] = A(1.4, {
        'root': {'rotation': lin([(0, R0), (0.45, [0, 35, 0]), (0.75, [0, -200, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])]),
                 'position': ch([(0, [0, 4, 0]), (0.45, [0, 2, 0]), (0.7, [0, 9, 0]), (1.0, [0, 7, 0]), (1.4, [0, 4, 0])])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-90, 0, -82]), (1.05, [-90, 0, -86]), (1.4, g('right_arm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-90, 0, 82]), (1.05, [-90, 0, 86]), (1.4, g('left_arm'))])},
        'right_forearm': {'rotation': hold([0, 0, 0], 1.4)}, 'left_forearm': {'rotation': hold([0, 0, 0], 1.4)},
        'shards': {'rotation': lin([(0, R0), (1.4, [0, -720, 0])]), 'scale': ch([(0, [1, 1, 1]), (0.7, [1.6, 1.2, 1.6]), (1.4, [1, 1, 1])])},
        'choir': {'rotation': lin([(0, R0), (1.4, [0, -540, 0])])}, 'skirt': {'rotation': ch([(0, R0), (0.7, [0, 0, 14]), (1.4, R0)])}})
    # VOLLEY: draws back, hurls both hands forward to loose the shards (release 0.6 = tick 12)
    out['volley'] = A(1.1, {
        'root': {'position': ch([(0, [0, 4, 0]), (0.4, [0, 6, 0]), (0.6, [0, 3, 0]), (1.1, [0, 4, 0])])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-20, 0, -45]), (0.6, [-98, 0, -6]), (0.75, [-100, 0, -4]), (1.1, g('right_arm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-20, 0, 45]), (0.6, [-98, 0, 6]), (0.75, [-100, 0, 4]), (1.1, g('left_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.4, [-70, 0, 0]), (0.6, [0, 0, 0]), (1.1, g('right_forearm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.4, [-70, 0, 0]), (0.6, [0, 0, 0]), (1.1, g('left_forearm'))])},
        'chest': {'rotation': ch([(0, R0), (0.4, [-14, 0, 0]), (0.6, [12, 0, 0]), (1.1, R0)])},
        'shards': {'scale': ch([(0, [1, 1, 1]), (0.4, [0.6, 1, 0.6]), (0.6, [1.8, 1, 1.8]), (1.1, [1, 1, 1])])}})
    # BARRIER: arms spread, the halo and shards swell into a ward
    out['barrier'] = A(1.2, {
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-10, 0, -100]), (0.9, [-12, 0, -104]), (1.2, g('right_arm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-10, 0, 100]), (0.9, [-12, 0, 104]), (1.2, g('left_arm'))])},
        'right_forearm': {'rotation': hold([-10, 0, 0], 1.2)}, 'left_forearm': {'rotation': hold([-10, 0, 0], 1.2)},
        'head': {'rotation': ch([(0, g('head')), (0.4, [-22, 0, 0]), (1.2, g('head'))])},
        'halo': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.6, 1.6, 1]), (0.9, [1.6, 1.6, 1]), (1.2, [1, 1, 1])])},
        'shards': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.5, 1.3, 1.5]), (0.9, [1.5, 1.3, 1.5]), (1.2, [1, 1, 1])])}})
    # ROAR (phase change, "the choir breaks"): curls in, then flings everything outward, head thrown back
    out['roar'] = A(2.0, {
        'root': {'position': ch([(0, [0, 4, 0]), (0.5, [0, 1, 0]), (0.8, [0, 10, 0]), (1.6, [0, 9, 0]), (2.0, [0, 4, 0])])},
        'body': {'rotation': ch([(0, R0), (0.5, [18, 0, 0]), (0.8, [-14, 0, 0]), (1.6, [-12, 0, 0]), (2.0, R0)])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.5, [-40, 0, 10]), (0.8, [-150, 0, -70]), (1.6, [-150, 0, -72]), (2.0, g('right_arm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.5, [-40, 0, -10]), (0.8, [-150, 0, 70]), (1.6, [-150, 0, 72]), (2.0, g('left_arm'))])},
        'head': {'rotation': ch([(0, g('head')), (0.5, [20, 0, 0]), (0.8, [-35, 0, 0]), (1.6, [-32, 0, 0]), (2.0, g('head'))])},
        'halo': {'scale': ch([(0, [1, 1, 1]), (0.5, [0.8, 0.8, 1]), (0.8, [1.8, 1.8, 1]), (1.6, [1.7, 1.7, 1]), (2.0, [1, 1, 1])]), 'rotation': spin(2.0, 2, 2)},
        'shards': {'scale': ch([(0, [1, 1, 1]), (0.5, [0.5, 0.8, 0.5]), (0.8, [2.0, 1.4, 2.0]), (1.6, [1.9, 1.4, 1.9]), (2.0, [1, 1, 1])])},
        'choir': {'rotation': lin([(0, R0), (2.0, [0, 540, 0])]), 'scale': ch([(0, [1, 1, 1]), (0.8, [1.4, 1, 1.4]), (2.0, [1, 1, 1])])}})
    # DEATH: shudders, the halo falls apart, sinks and topples, glass dimming out
    out['death'] = A(2.5, {
        'root': {'position': ch([(0, [0, 4, 0]), (0.3, [0, 6, 0]), (1.2, [0, -8, 0]), (2.5, [0, -16, 0])]), 'rotation': ch([(0, R0), (0.3, [0, 0, 6]), (0.5, [0, 0, -6]), (1.6, [0, 0, 70]), (2.5, [0, 0, 84])])},
        'head': {'rotation': ch([(0, R0), (0.4, [-30, 0, 0]), (1.4, [30, 0, 20]), (2.5, [35, 0, 20])])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-120, 0, -40]), (1.4, [-10, 0, -20]), (2.5, [0, 0, -15])])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-120, 0, 40]), (1.4, [-10, 0, 20]), (2.5, [0, 0, 15])])},
        'halo': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.5, 1.5, 1]), (1.2, [0.01, 0.01, 0.01]), (2.5, [0.01, 0.01, 0.01])])},
        'shards': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.8, 0.6, 1.8]), (1.4, [0.01, 0.01, 0.01]), (2.5, [0.01, 0.01, 0.01])])},
        'choir': {'scale': ch([(0, [1, 1, 1]), (1.0, [0.01, 0.01, 0.01]), (2.5, [0.01, 0.01, 0.01])])}})
    return out

# ------------------------------------------------------------------ Hierophant animations
HG = {'right_arm': [-48, 0, -12], 'right_forearm': [-30, 0, 0], 'staff': [52, 0, 22], 'left_arm': [-72, 0, 22], 'left_forearm': [-24, 0, 0],
      'chest': [6, 0, 0], 'head': [-12, 0, 0]}

def hierophant_anims():
    def common(T):
        return {'censer': {'rotation': loop(T, 16, 0, phase=1.2)}, 'sigils': {'rotation': spin(T, 1, 2)},
                'sigil_hand': {'rotation': spin(T, -1, 2)},
                'robe_back': {'rotation': loop(T, 3, 0, (4, 0, 0))}, 'robe_back2': {'rotation': loop(T, 5, 0, (3, 0, 0), 1)},
                'cape1': {'rotation': loop(T, 3, 0, (0, 0, 0))}, 'cape2': {'rotation': loop(T, 6, 0, (4, 0, 0), 0.9)}, 'cape3': {'rotation': loop(T, 9, 0, (6, 0, 0), 1.8)},
                'lappet_right': {'rotation': loop(T, 6, 0, (8, 0, 0), 0.3)}, 'lappet_left': {'rotation': loop(T, 6, 0, (8, 0, 0), 1.3)},
                'mitre1': {'rotation': loop(T, 3, 2, (0, 0, 0), 0.6)}, 'tabard': {'rotation': loop(T, 4, 0, (2, 0, 0), 0.4)}}
    def stride(T, amp=24, knee=20, bob=0.8):
        return {'right_leg': {'rotation': loop(T, amp, 0)}, 'left_leg': {'rotation': loop(T, amp, 0, phase=math.pi)},
                'right_shin': {'rotation': kf([(T * i / 8, [max(0, knee * math.sin(2 * math.pi * i / 8 + 1.2)), 0, 0]) for i in range(9)])},
                'left_shin': {'rotation': kf([(T * i / 8, [max(0, knee * math.sin(2 * math.pi * i / 8 + 1.2 + math.pi)), 0, 0]) for i in range(9)])},
                'root': {'position': kf([(T * i / 8, [0, round(-bob * abs(math.sin(2 * math.pi * i / 8)), 3), 0]) for i in range(9)])}}
    out = {}
    T = 3.2; b = common(T)
    b.update({'chest': {'rotation': loop(T, 2.5, 0)}, 'head': {'rotation': loop(T, 3, 1, (-8, 0, 0), 1)},
              'right_arm': {'rotation': hold([-14, 0, -8], T)}, 'right_forearm': {'rotation': hold([-22, 0, 0], T)}, 'staff': {'rotation': hold([36, 0, -8], T)},
              'left_arm': {'rotation': loop(T, 3, 0, (-30, 0, 14))}, 'left_forearm': {'rotation': loop(T, 4, 0, (-36, 0, 0), 1)}})
    out['idle'] = A(T, b, True)
    T = 1.6; b = common(T); b.update(stride(T, 20))
    b.update({'chest': {'rotation': loop(T / 2, 2, 0)}, 'right_arm': {'rotation': hold([-14, 0, -8], T)}, 'staff': {'rotation': hold([36, 0, -8], T)},
              'right_forearm': {'rotation': hold([-22, 0, 0], T)}, 'left_arm': {'rotation': loop(T, 12, 0, (-20, 0, 12), math.pi)},
              'left_forearm': {'rotation': hold([-30, 0, 0], T)}, 'head': {'rotation': hold([-8, 0, 0], T)}})
    out['walk'] = A(T, b, True)
    for an, T in (('idle_combat', 2.6), ('walk_combat', 1.3)):
        b = common(T)
        if an == 'walk_combat': b.update(stride(T, 20, 16, 0.6))
        for bn, base in HG.items(): b[bn] = {'rotation': loop(T, 2 if bn != 'staff' else 1, 0, tuple(base), phase=len(bn))}
        out[an] = A(T, b, True)
    g = lambda bn: HG.get(bn, R0)
    # ATTACK "Gavel": lifts the staff overhead with both arms, rises onto his toes, holds, then hammers it down (impact 0.7)
    out['attack'] = A(1.2, {
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.45, [-152, 0, -48]), (0.58, [-158, 0, -50]), (0.7, [-62, 0, -14]), (0.85, [-56, 0, -14]), (1.2, g('right_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.45, [-6, 0, 0]), (0.7, [-20, 0, 0]), (1.2, g('right_forearm'))])},
        'staff': {'rotation': ch([(0, g('staff')), (0.45, [30, 0, 62]), (0.58, [26, 0, 64]), (0.7, [72, 0, 20]), (0.85, [76, 0, 18]), (1.2, g('staff'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.45, [-40, 0, 32]), (0.7, [-84, 0, 14]), (1.2, g('left_arm'))])},
        'chest': {'rotation': ch([(0, g('chest')), (0.45, [-14, 0, 0]), (0.58, [-16, 0, 0]), (0.7, [24, 0, 0]), (0.85, [26, 0, 0]), (1.2, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.45, [-24, 0, 0]), (0.7, [4, 0, 0]), (1.2, g('head'))])},
        'root': {'position': ch([(0, R0), (0.45, [0, 1.6, 0]), (0.58, [0, 1.8, 0]), (0.7, [0, -1.4, 0]), (0.85, [0, -1.2, 0]), (1.2, R0)])},
        'censer': {'rotation': ch([(0, R0), (0.5, [-45, 0, 0]), (0.8, [55, 0, 0]), (1.0, [-20, 0, 0]), (1.2, R0)])}})
    # SWEEP "Thurible": swings the censer end of the staff in a flat arc across his front (impact 0.65)
    out['sweep'] = A(1.2, {
        'chest': {'rotation': ch([(0, g('chest')), (0.45, [4, 40, 0]), (0.65, [8, -34, 0]), (0.85, [8, -40, 0]), (1.2, g('chest'))])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.45, [-84, 0, -70]), (0.65, [-86, 0, -8]), (0.85, [-84, 0, -4]), (1.2, g('right_arm'))])},
        'staff': {'rotation': ch([(0, g('staff')), (0.45, [84, 0, 30]), (0.65, [86, 0, 20]), (1.2, g('staff'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.45, [-60, 0, 40]), (0.65, [-40, 0, 50]), (1.2, g('left_arm'))])},
        'censer': {'rotation': ch([(0, R0), (0.45, [0, 0, 40]), (0.7, [0, 0, -60]), (0.95, [0, 0, 25]), (1.2, R0)])},
        'head': {'rotation': ch([(0, g('head')), (0.45, [-10, -20, 0]), (0.65, [-10, 16, 0]), (1.2, g('head'))])}})
    # SPIN "Sermon of Ash" (impact 0.7)
    out['spin'] = A(1.4, {
        'root': {'rotation': lin([(0, R0), (0.5, [0, 35, 0]), (0.75, [0, -200, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.4, [-90, 0, -70]), (1.05, [-90, 0, -75]), (1.4, g('right_arm'))])},
        'staff': {'rotation': ch([(0, g('staff')), (0.4, [90, 0, 0]), (1.05, [90, 0, 0]), (1.4, g('staff'))])},
        'censer': {'rotation': ch([(0, R0), (0.4, [-30, 0, 0]), (0.75, [-70, 0, 0]), (1.05, [-40, 0, 0]), (1.4, R0)])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.5, [-40, 0, 40]), (1.4, g('left_arm'))])},
        'chest': {'rotation': hold(g('chest'), 1.4)}, 'head': {'rotation': hold(g('head'), 1.4)}})
    # BARRIER: plants the staff, raises the burning sigil overhead; the back sigils swell
    out['barrier'] = A(1.2, {
        'right_arm': {'rotation': hold([-50, 0, -22], 1.2)}, 'right_forearm': {'rotation': hold(g('right_forearm'), 1.2)}, 'staff': {'rotation': hold([55, 0, 25], 1.2)},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-160, 0, 30]), (0.9, [-162, 0, 32]), (1.2, g('left_arm'))])},
        'head': {'rotation': ch([(0, g('head')), (0.4, [-26, 0, 0]), (0.9, [-26, 0, 0]), (1.2, g('head'))])},
        'chest': {'rotation': hold(g('chest'), 1.2)},
        'sigil_hand': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.8, 1.8, 1.8]), (0.9, [1.8, 1.8, 1.8]), (1.2, [1, 1, 1])])},
        'sigils': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.5, 1.5, 1]), (0.9, [1.5, 1.5, 1]), (1.2, [1, 1, 1])])}})
    # ROAR (phase change, "the profane rite"): hunches in, then throws back his head and both arms, sigils flare
    out['roar'] = A(2.0, {
        'chest': {'rotation': ch([(0, g('chest')), (0.5, [26, 0, 0]), (0.85, [-18, 0, 0]), (1.6, [-16, 0, 0]), (2.0, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.5, [14, 0, 0]), (0.85, [-34, 0, 0]), (1.6, [-32, 0, 0]), (2.0, g('head'))])},
        'right_arm': {'rotation': hold(g('right_arm'), 2.0)}, 'right_forearm': {'rotation': hold(g('right_forearm'), 2.0)}, 'staff': {'rotation': hold(g('staff'), 2.0)},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.5, [-30, 0, 10]), (0.85, [-150, 0, 60]), (1.6, [-152, 0, 62]), (2.0, g('left_arm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.85, [-6, 0, 0]), (2.0, g('left_forearm'))])},
        'sigils': {'scale': ch([(0, [1, 1, 1]), (0.5, [0.7, 0.7, 1]), (0.85, [2.0, 2.0, 1]), (1.6, [1.9, 1.9, 1]), (2.0, [1, 1, 1])]), 'rotation': spin(2.0, 1.5, 2)},
        'root': {'position': ch([(0, R0), (0.5, [0, -1.4, 0]), (0.85, [0, 1.2, 0]), (2.0, R0)])}})
    # DEATH: staggers, drops to his knees leaning on the staff, then pitches forward
    out['death'] = A(2.5, {
        'root': {'position': ch([(0, R0), (0.6, [0, -2, 0]), (1.2, [0, -9, 0]), (2.5, [0, -12, -6])]), 'rotation': ch([(0, R0), (1.2, [6, 0, 0]), (2.0, [70, 0, 0]), (2.5, [84, 0, 0])])},
        'right_leg': {'rotation': ch([(0, R0), (0.6, [-30, 0, 0]), (1.2, [-80, 0, 0]), (2.5, [-80, 0, 0])])}, 'left_leg': {'rotation': ch([(0, R0), (0.8, [-20, 0, 0]), (1.2, [-75, 0, 0]), (2.5, [-75, 0, 0])])},
        'right_shin': {'rotation': ch([(0, R0), (1.2, [90, 0, 0]), (2.5, [90, 0, 0])])}, 'left_shin': {'rotation': ch([(0, R0), (1.2, [88, 0, 0]), (2.5, [88, 0, 0])])},
        'chest': {'rotation': ch([(0, g('chest')), (0.4, [-20, 0, 0]), (1.2, [20, 0, 0]), (2.5, [10, 0, 0])])},
        'head': {'rotation': ch([(0, g('head')), (0.4, [-30, 0, 0]), (1.4, [30, 0, 0]), (2.5, [20, 0, 0])])},
        'right_arm': {'rotation': ch([(0, g('right_arm')), (1.2, [-70, 0, -10]), (2.5, [-120, 0, -10])])}, 'staff': {'rotation': ch([(0, g('staff')), (1.2, [40, 0, 10]), (2.5, [10, 0, 0])])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-120, 0, 40]), (1.4, [-20, 0, 10]), (2.5, [-100, 0, 10])])},
        'sigils': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.6, 1.6, 1]), (1.4, [0.01, 0.01, 0.01]), (2.5, [0.01, 0.01, 0.01])])},
        'sigil_hand': {'scale': ch([(0, [1, 1, 1]), (1.0, [0.01, 0.01, 0.01]), (2.5, [0.01, 0.01, 0.01])])}})
    return out

# impact frames used for the frozen review poses (seconds); matches RiftBoss/RiftMob strike timing
FREEZE = {'attack': 0.7, 'sweep': 0.62, 'spin': 0.72, 'volley': 0.6, 'barrier': 0.6, 'roar': 1.0, 'death': 2.5,
          'attack_windup': ('attack', 0.5)}

def bake_freezes(name):
    import clipcheck as C
    p = G.out('animations', 'entity', f'{name}.animation.json'); js = json.load(open(p)); an = js['animations']
    bones, order, _ = C.load(name)
    for fz, spec in FREEZE.items():
        src, t = (spec if isinstance(spec, tuple) else (fz, spec))
        key = f'animation.{name}.{src}'
        if key not in an: continue
        layers = [an[f'animation.{name}.idle_combat'], an[key]]
        bb = {}
        for n in order:
            e = {}
            for chn in ('rotation', 'position', 'scale'):
                v = None
                for L in layers:
                    c = L['bones'].get(n, {}).get(chn)
                    if c is not None:
                        tt = t % L['animation_length'] if L.get('loop') else t
                        if L is layers[0]: tt = 0.0
                        v = C.sample(c, tt, L['animation_length']) or v
                if v is None: v = [1, 1, 1] if chn == 'scale' else [0, 0, 0]
                e[chn] = [round(q, 3) for q in v]
            bb[n] = e
        an[f'animation.{name}.freeze_{fz}'] = {'loop': True, 'animation_length': 1.0, 'bones': bb}
    json.dump(js, open(p, 'w'), indent=1)

def build(name):
    m = PAL[name]; rig = {'vaelith': rig_vaelith, 'fallen_hierophant': rig_hierophant}[name]()
    m = dict(m, ts=list(m['ts'])); uvs = G.pack(rig, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, 256]); huvs = G.pack(rig, hm['k'], hm['ts']); pt = Painter(name, hm)
    cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    downsample(name, m, rig, pt, m['k'], huvs, uvs); G.geo(name, rig, m['k'], m['ts'], uvs)
    G.write_anims(name, {'vaelith': vaelith_anims, 'fallen_hierophant': hierophant_anims}[name]())
    bake_freezes(name)
    print(name, len(rig.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    for n in sys.argv[1:] or ['vaelith', 'fallen_hierophant']: build(n)

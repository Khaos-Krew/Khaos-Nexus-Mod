"""1.1.2 polish pass: the Spire Guardian, rebuilt to the Hollow King v3 standard.
A tall sentinel in lilac-steel plate: rounded overlapping lames, domed pauldrons, a faceless tower helm with a burning
violet visor slit and a segmented spire crest, a caged violet core in the breastplate with emissive rune-work, a
cloth-baked split cape, and a true two-handed halberd (both fists solved onto the shaft every 1/20 s, boss3.bake) with
a weighted set: sentry carry, overhead chop, wide reaping sweep, spin, planted barrier, roar.
Run: python3 rig_guardian3.py [nopaint]"""
import math, sys
import numpy as np
import gen_geo_assets as G
import rigs_saga2 as S
import king_ik as K
import boss3 as B3
from boss3 import octo, otaper, plate, horn, SW, mv, R0
Rig = G.Rig
NAME = 'spire_guardian'
HX, HY = 12.2, 26.6
FY, FZ = HY - 2.0, -0.4
SH = (12.2, 46.6)
CAPE = dict(prefix='cape', halves=[('R', -1, -3.4), ('L', 1, 3.4)], top=48.0, z=5.8, overlap=0.6,
            segs=[(8, 9.6), (8, 10.2), (8, 10.8), (7.5, 11.4), (7, 12.0), (6.5, 12.6)])

def B_(r, bone, c, size, role, rot=None, pivot=None): r.box(bone, c, size, role, rot=rot, pivot=pivot)

def fist(r, bone, x, s):
    fy = FY
    octo(r, bone, (x, fy + 0.1, FZ), (4.0, 3.8, 3.8), 'gauntlet', k=0.92, bev=0.18)
    for f in range(4): B_(r, bone, (x - s * 0.2, fy - 1.75, -1.95 + f * 1.0 + FZ + 0.4), (3.8, 1.0, 0.86), 'breastplate' if f % 2 else 'gauntlet')
    B_(r, bone, (x - s * 2.15, fy + 0.45, -1.8), (1.0, 2.5, 1.5), 'gauntlet')
    B_(r, bone, (x + s * 1.95, fy + 0.25, FZ), (0.6, 2.9, 3.3), 'crown')
    B_(r, bone, (x, fy + 2.1, FZ), (3.6, 0.7, 3.3), 'breastplate')

def halberd_parts(r, bone, x, f, z):
    """Halberd built along +Y from the right-fist centre f: butt spike, banded shaft, crescent axe, back hook, top spike."""
    B_(r, bone, (x, f - 27.4, z), (1.2, 3.0, 1.2), 'edge'); octo(r, bone, (x, f - 25.4, z), (2.4, 1.6, 2.4), 'crown')
    otaper(r, bone, x, z, f - 24.6, f + 34.0, 1.8, 1.8, 1.8, 1.8, 6, 'shaft')
    for y in (-20, -13.2, -6.0, 6.0, 14, 22, 30): octo(r, bone, (x, f + y, z), (2.3, 0.6, 2.3), 'crown')
    for y in (-10.0, 2.0, 18.0): B_(r, bone, (x, f + y, z), (2.0, 4.4, 2.0), 'grip')
    octo(r, bone, (x, f + 35.0, z), (3.2, 2.4, 3.2), 'guard'); B_(r, bone, (x, f + 35.0, z - 1.8), (1.4, 1.4, 0.4), 'core', rot=(0, 0, 45))
    # crescent axe on the front (-Z), flat normal X
    for j, (yy, ln, zz) in enumerate(((36.4, 12.0, -3.0), (36.8, 13.6, -5.0), (37.0, 14.6, -7.0), (37.0, 14.4, -9.0), (36.8, 12.8, -10.8), (36.6, 10.4, -12.2))):
        B_(r, bone, (x, f + yy, z + zz), (1.0 if j < 4 else 0.8, ln, 2.2), 'axehead')
    B_(r, bone, (x, f + 36.8, z - 13.2), (0.5, 9.6, 1.4), 'edge')
    for yy in (31.4, 42.2): B_(r, bone, (x, f + yy, z - 12.6), (0.5, 2.0, 1.2), 'edge', rot=(30 if yy > 40 else -30, 0, 0))
    B_(r, bone, (x, f + 37.2, z - 6.2), (1.2, 4.6, 4.6), 'rune', rot=(45, 0, 0))
    for j, (yy, zz, ln) in enumerate(((38.0, 2.8, 2.2), (37.0, 4.6, 1.8), (35.6, 6.0, 1.4))):   # back hook curving down
        B_(r, bone, (x, f + yy, z + zz), (1.0, ln + 1.0, 1.8 - j * 0.3), 'axehead')
    B_(r, bone, (x, f + 34.4, z + 6.6), (0.6, 2.0, 0.8), 'edge')
    S.taper(r, bone, x, z, f + 36.2, f + 49.0, 1.2, 0.5, 2.6, 0.8, 3, 'axehead')                     # top spike
    S.taper(r, bone, x, z, f + 36.2, f + 49.4, 0.4, 0.2, 3.0, 1.0, 3, 'edge')
    for zz in (-1.8, 1.8): B_(r, bone, (x, f + 32.4, z + zz * 1.4), (0.5, 3.6, 0.5), 'rune')         # ribbon sigils

def rig():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 4.2
        lg = r.bone(f'{side}_leg', 'root', (x, 24, 0))
        otaper(r, lg, x, 0, 24, 13.0, 6.0, 5.0, 6.2, 5.2, 3, 'plate')
        for j in range(3):
            plate(r, lg, (x + s * 0.9, 22.6 - j * 2.6, -0.3 - j * 0.15), (6.6 - j * 0.3, 3.2, 7.0 - j * 0.3), 'breastplate', rot=(0, 0, s * (10 + j * 3)), pivot=(x, 24, 0), bev=0.5, raise_=0.25)
        octo(r, lg, (x, 13.0, -1.3), (5.0, 3.8, 4.6), 'breastplate')
        B(lg, (x, 13.0, -3.75), (2.4, 2.4, 0.6), 'crown', rot=(0, 0, 45)); B(lg, (x, 13.0, -4.1), (1.0, 1.0, 0.3), 'core', rot=(0, 0, 45))
        sh = r.bone(f'{side}_shin', lg, (x, 12, 0))
        otaper(r, sh, x, 0, 12, 2.6, 5.0, 4.2, 5.2, 4.4, 3, 'plate')
        plate(r, sh, (x, 7.6, -2.4), (4.2, 8.0, 1.0), 'breastplate', bev=0.6, raise_=0.4)
        B(sh, (x, 7.6, -3.15), (0.5, 7.0, 0.3), 'rune')
        for j in range(4): octo(r, sh, (x, 1.9 - j * 0.4, -0.9 - j * 1.5), (5.2 - j * 0.4, 1.4, 5.0 - j * 0.65), 'gauntlet' if j % 2 else 'breastplate')
        B(sh, (x, 0.5, -6.5), (2.6, 1.0, 1.2), 'crown')
    r.bone('hips', 'root', (0, 24, 0))
    octo(r, 'hips', (0, 24.4, 0), (13.4, 3.4, 9.6), 'belt')
    B('hips', (0, 24.4, -5.1), (3.8, 3.8, 0.9), 'crown', rot=(0, 0, 45)); B('hips', (0, 24.4, -5.6), (2.0, 2.0, 0.3), 'core', rot=(0, 0, 45))
    for s in (-1, 1): plate(r, 'hips', (s * 6.8, 23.4, 0), (1.2, 4.2, 8.0), 'breastplate', rot=(0, 0, s * 7), bev=0.4, raise_=0.2)
    r.bone('tabard', 'hips', (0, 23, -5.0))
    B('tabard', (0, 14.0, -5.3), (6.0, 18.0, 0.6), 'robe'); B('tabard', (0, 14.0, -5.65), (0.9, 17.0, 0.2), 'crown')
    B('tabard', (0, 18.0, -5.8), (3.4, 3.4, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 4.6, -5.3), (5.6, 2.6, 0.6), 'capehem')
    r.bone('torso', 'hips', (0, 25.5, 0))
    otaper(r, 'torso', 0, 0, 25.5, 35, 11.2, 13.0, 8.0, 9.0, 2, 'plate')
    for j in range(3):
        octo(r, 'torso', (0, 27.0 + j * 2.7, -0.15 * j), (11.8 + j * 0.8, 2.6, 8.8 + j * 0.4), 'breastplate', k=0.86)
        B('torso', (0, 28.0 + j * 2.7, -4.6 - j * 0.3), (6.4 + j, 0.4, 0.3), 'crown')
    r.bone('chest', 'torso', (0, 35, 0))
    otaper(r, 'chest', 0, 0.6, 35, 47, 14.8, 19.6, 8.6, 9.4, 3, 'plate')
    octo(r, 'chest', (0, 41.4, 3.6), (16.4, 9.6, 3.0), 'breastplate', k=0.9)
    for s in (-1, 1):
        for j, (dy, yaw) in enumerate(((0, -10), (3.8, -22), (-3.8, -4))):
            plate(r, 'chest', (s * (6.8 + 0.5 * j), 41.0 + dy, -4.5 - (0.2 if j == 0 else 0)), (5.8, 4.8 if j else 5.2, 2.6), 'breastplate',
                  rot=(0, s * yaw, s * 3), pivot=(s * 9.8, 41.0, -4.4), bev=0.5, raise_=0.3)
        B('chest', (s * 3.9, 41.0, -5.95), (0.6, 12.4, 0.6), 'crown')
    # caged core
    r.bone('core', 'chest', (0, 41.4, -5.6))
    octo(r, 'core', (0, 41.4, -5.6), (3.6, 4.4, 2.0), 'core', k=0.9)
    for j in range(3): B('core', (-1.4 + j * 1.4, 41.4, -6.75), (0.5, 6.4, 0.5), 'crown')
    for yy in (38.4, 44.4): B('core', (0, yy, -6.4), (5.2, 0.7, 1.4), 'crown')
    B('core', (0, 41.4, -6.9), (6.4, 6.4, 0.3), 'rune', rot=(0, 0, 45))
    for j in range(2): octo(r, 'chest', (0, 47.4 + j * 1.3, 0.2), (11.0 - j * 1.8, 1.6, 9.4 - j * 1.4), 'guard' if j == 0 else 'helm')
    octo(r, 'chest', (0, 47.8, 0.5), (15.0, 2.2, 10.6), 'pauldron', k=0.9)
    for side, s in (('right', -1), ('left', 1)):
        x = s * HX
        arm = r.bone(f'{side}_arm', 'chest', (x, SH[1], 0))
        for j in range(4):
            octo(r, arm, (x + s * 0.6 * j, 49.2 - j * 1.9, 0.2), (11.0 - j * 0.9, 2.4, 10.8 - j * 0.7), 'pauldron', rz=s * (8 + j * 8), k=0.88)
            B(arm, (x + s * (0.6 * j + 0.25), 48.9 - j * 1.9, -5.4 + j * 0.35), (7.2 - j * 0.8, 0.5, 0.3), 'crown', rot=(0, 0, s * (8 + j * 8)))
        octo(r, arm, (x + s * 0.4, 50.8, 0.2), (8.0, 1.4, 8.0), 'pauldron', rz=s * 6)
        B(arm, (x + s * 2.2, 52.6, 0.2), (1.2, 4.4, 1.2), 'crown', rot=(0, 0, s * -16))
        B(arm, (x + s * 2.8, 55.0, 0.2), (0.9, 0.9, 0.9), 'core', rot=(45, 45, 0))
        otaper(r, arm, x, 0, 45.4, 36.0, 5.6, 5.2, 5.6, 5.2, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 35.6, 0))
        octo(r, fa, (x, 35.4, 0.6), (5.6, 3.4, 5.0), 'breastplate')
        plate(r, fa, (x + s * 2.9, 35.4, 0.6), (0.8, 3.2, 3.6), 'breastplate', bev=0.3, raise_=0.2)
        otaper(r, fa, x, 0, 34.4, HY + 2.8, 4.8, 5.6, 4.8, 5.6, 2, 'plate')
        octo(r, fa, (x, HY + 2.5, 0), (6.0, 1.5, 5.6), 'crown', k=0.9)
        B(fa, (x, HY + 5.2, -3.0), (2.6, 4.2, 0.3), 'rune')
        hd = r.bone(f'{side}_hand', fa, (x, HY, FZ))
        B(hd, (x, HY + 1.0, FZ), (4.4, 1.2, 4.2), 'gauntlet')
        fist(r, hd, x, s)
    # head: tower helm, vertical burning visor, cheek wings, segmented spire crest
    r.bone('head', 'chest', (0, 48.4, -0.5))
    octo(r, 'head', (0, 54.4, -0.5), (8.4, 10.4, 9.0), 'helm', k=0.86)
    octo(r, 'head', (0, 60.0, -0.5), (7.0, 1.2, 7.8), 'helm', k=0.86)
    for s in (-1, 1):
        plate(r, 'head', (s * 2.0, 54.0, -4.9), (4.2, 8.8, 1.0), 'visorplate', rot=(0, s * -22, 0), pivot=(0, 54.0, -5.7), bev=0.4, raise_=0.2)
        plate(r, 'head', (s * 4.4, 54.4, -0.4), (0.8, 8.2, 7.0), 'breastplate', bev=0.4, raise_=0.2)
        for j in range(3):   # swept cheek wings
            B('head', (s * (4.9 + j * 0.9), 56.6 + j * 1.6, 1.4 + j * 0.9), (0.8, 3.2 - j * 0.6, 4.4 - j * 0.9), 'crown', rot=(-20, 0, s * -12))
        B('head', (s * 1.15, 55.2, -6.0), (0.9, 1.0, 0.3), 'void', rot=(0, s * -22, 0), pivot=(0, 55.2, -6.4))
    B('head', (0, 54.4, -6.25), (0.9, 8.0, 0.4), 'void'); B('head', (0, 54.8, -6.45), (0.5, 6.2, 0.3), 'eye')
    B('head', (0, 49.6, -5.6), (5.4, 1.4, 1.0), 'crown')
    prev = 'head'; y = 60.6
    for i, (w, h) in enumerate(((5.6, 5.0), (4.4, 5.0), (3.4, 4.6), (2.4, 4.4), (1.4, 5.0))):
        b = r.bone(f'spire{i}', prev, (0, y, -0.5), (-4 if i else 0, 0, 0))
        octo(r, b, (0, y + h / 2, -0.5), (w, h, w), 'helmspire' if i % 2 == 0 else 'helm', k=0.88)
        B(b, (0, y + 0.3, -0.5), (w + 0.8, 0.6, w + 0.8), 'crown')
        if i == 1: B(b, (0, y + h / 2, -0.5 - w / 2 - 0.2), (1.2, 1.2, 0.4), 'core', rot=(0, 0, 45))
        prev, y = b, y + h
    B('spire4', (0, y + 0.8, -0.5), (0.8, 1.6, 0.8), 'core', rot=(45, 45, 0))
    r.bone('halberd', 'right_hand', (-HX, FY, FZ), (-90, 0, 0))
    halberd_parts(r, 'halberd', -HX, FY, FZ)
    B3.cape_bones(r, CF, sigil='sigil')
    return r

def spheres(fk, R, Q):
    Cm = fk.world('chest', R, Q); Hm = fk.world('hips', R, Q); Tm = fk.world('torso', R, Q)
    sph = [(K.xf(Cm, (0, 41, -0.4)), 8.0), (K.xf(Cm, (-4.6, 43, -0.4)), 6.6), (K.xf(Cm, (4.6, 43, -0.4)), 6.6), (K.xf(Tm, (0, 30, 0.4)), 6.4), (K.xf(Hm, (0, 23, 0.4)), 6.8)]
    for side, xx in (('right', -4.2), ('left', 4.2)):
        lm = fk.world(f'{side}_leg', R, Q); sm = fk.world(f'{side}_shin', R, Q)
        sph += [(K.xf(lm, (xx, 20, 0)), 4.2), (K.xf(lm, (xx, 15, 0)), 3.9), (K.xf(sm, (xx, 9, 0)), 3.4), (K.xf(sm, (xx, 4, 0)), 3.2)]
    return sph

CF = B3.Cfg(weapon='halberd', HX=HX, FY=FY, FZ=FZ, SH=SH, gap=12.0, reach=22.6, ax=(-28.0, 50.0),
            widths=[(36.0, 13.0), (36.0, 6.5), (37.0, 6.5), (30, 1.0)], hazards=[((0, 36.0, -7.0), 7.0)],
            fa_pts=(HY + 1.6, HY + 2.6, HY + 4.2, HY + 6, 32.0, 34.4, 36.0), fa_rad=4.2, cape=dict(CAPE, spheres=spheres))

PAL = {NAME: dict(who=NAME, k=3, ts=[768, 768], boss=True,
       pal=dict(base='#2D2528', dark='#120D0F', accent='#5A5054', robe='#3A1846', cape='#2E1238'),
       trim='#B8B8D0', glow='#B04BE0', eye='#E6B8FF', core='#C26CFF', crystal='#B04BE0', eyes='visor',
       mat=dict(metal={'plate': (70, 66, 84), 'breastplate': (96, 92, 112), 'pauldron': (90, 86, 106), 'helm': (84, 80, 100), 'helmspire': (100, 96, 118),
                       'gauntlet': (60, 56, 72), 'visorplate': (104, 100, 122), 'belt': (50, 40, 60), 'shaft': (48, 38, 44), 'axehead': (120, 118, 138)},
                spec=(226, 222, 246), rim=(176, 172, 200), trim=((126, 126, 150), (214, 216, 236), (255, 255, 255)), rune=(222, 150, 255),
                trimmed=('breastplate', 'pauldron', 'visorplate'), etched=('breastplate', 'pauldron', 'helm', 'helmspire', 'axehead'), glowing=('breastplate', 'pauldron', 'helmspire'),
                veined=('plate',), vein=(200, 110, 255),
                cloth={'robe': (66, 26, 80), 'cape': (52, 20, 64), 'capehem': (52, 20, 64)}, sheen=(150, 90, 190), hem=(200, 200, 220),
                eye=((210, 140, 255), (250, 236, 255)), core=((190, 90, 255), (246, 220, 255)), edge=((184, 184, 204), (244, 244, 252)),
                horn=((40, 30, 50), (110, 96, 130))))}

# ------------------------------------------------------------------ animation
CARRY = SW((-12.6, 27.4, -4.6), (0.0, 1.0, -0.06))           # sentry: halberd upright at his right side, butt on the floor
GUARD = SW((-5.0, 31.0, -12.0), (0.18, 0.78, -0.6))           # two-handed diagonal guard, head up and forward
PLANT = SW((-2.0, 30.0, -13.0), (0.0, 1.0, -0.04))            # planted upright before him
REST_L = {'left_arm': [-4, 0, 6], 'left_forearm': [-12, 0, 0], 'left_hand': [0, 0, 0]}

def author(rg):
    A = {}; base = B3.base
    BODY = ['chest', 'torso', 'head', 'hips', 'right_leg', 'left_leg', 'right_shin', 'left_shin']
    T = 4.0
    A['idle'] = base(T, True, [(0, CARRY), (T / 2, mv(CARRY, (0, 0.3, 0))), (T, CARRY)],
        body={'chest': [(0, [2, 0, 0]), (T / 2, [0, 0, 0]), (T, [2, 0, 0])], 'head': [(0, [-4, 0, 0]), (T / 2, [-2, -8, 0]), (T, [-4, 0, 0])],
              'right_leg': [(0, [-2, 0, 2]), (T, [-2, 0, 2])], 'left_leg': [(0, [2, 0, -2]), (T, [2, 0, -2])]},
        pos={'root': [(0, [0, 0, 0]), (T / 2, [0, -0.4, 0]), (T, [0, 0, 0])]},
        lfree=[(-1, T + 1)], lf={bn: [(0, v), (T / 2, list(np.add(v, [-3, 0, 2]))), (T, v)] for bn, v in REST_L.items()})
    T = 1.5; st = B3.stride(T, 20, 22, 1.2)
    A['walk'] = base(T, True, [(T * i / 4, mv(CARRY, (0, 0.6 if i % 2 else 0, -2.0))) for i in range(5)],
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'torso': [(t, [2, v[1] * 0.4, 0]) for t, v in st['sway']], 'chest': [(t, [3, v[1], 0]) for t, v in st['sway']],
              'head': [(0, [-4, 0, 0]), (T, [-4, 0, 0])]},
        pos={'root': st['root_pos']}, lfree=[(-1, T + 1)],
        lf={'left_arm': [(T * i / 8, [-18 * math.sin(2 * math.pi * i / 8) - 4, 0, 6]) for i in range(9)], 'left_forearm': [(0, [-16, 0, 0]), (T, [-16, 0, 0])], 'left_hand': [(0, R0), (T, R0)]})
    T = 3.0
    A['idle_combat'] = base(T, True, [(0, GUARD), (T / 2, mv(GUARD, (0, -0.8, 0.3))), (T, GUARD)],
        body={'chest': [(0, [6, 0, 0]), (T / 2, [8, -4, 0]), (T, [6, 0, 0])], 'head': [(0, [-10, 0, 0]), (T / 2, [-8, 4, 0]), (T, [-10, 0, 0])],
              'torso': [(0, [4, 8, 0]), (T, [4, 8, 0])], 'hips': [(0, [0, -8, 0]), (T, [0, -8, 0])],
              'right_leg': [(0, [12, 8, 4]), (T, [12, 8, 4])], 'left_leg': [(0, [-22, 8, -4]), (T, [-22, 8, -4])],
              'right_shin': [(0, [18, 0, 0]), (T, [18, 0, 0])], 'left_shin': [(0, [24, 0, 0]), (T, [24, 0, 0])]},
        pos={'root': [(0, [0, -1.4, 0]), (T / 2, [0, -1.9, 0]), (T, [0, -1.4, 0])]})
    IC = A['idle_combat']['body']; g = lambda bn: list(IC[bn][0][1]) if bn in IC else [0, 0, 0]
    T = 1.3; st = B3.stride(T, 18, 20, 1.0)
    A['walk_combat'] = base(T, True, [(T * i / 4, mv(GUARD, (0, -0.6 if i % 2 else 0, 0))) for i in range(5)],
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'chest': [(t, [8, v[1], 0]) for t, v in st['sway']], 'head': [(0, [-10, 0, 0]), (T, [-10, 0, 0])], 'torso': [(0, [5, 0, 0]), (T, [5, 0, 0])]},
        pos={'root': st['root_pos']})
    def act(T, keys, body, pos=None, end=True, **kw):
        bb = {}
        for bn in BODY:
            bb[bn] = [(0, g(bn))] + body.get(bn, []) + ([(T, g(bn))] if end else [])
            if len(bb[bn]) == 1: bb[bn].append((T, g(bn)))
        pr = (pos or {}).get('root', [])
        pp = {'root': [(0, [0, -1.4, 0])] + pr + ([(T, [0, -1.4, 0])] if end or not pr else [])}
        return base(T, False, [(0, GUARD)] + keys + ([(T, GUARD)] if end else []), bb, pp, **kw)
    # ATTACK "Spire Fall": halberd hauled back over the right shoulder, then a two-handed overhead chop (impact 0.7)
    A['attack'] = act(1.2, [
        (0.2, SW((-6.0, 44.0, -8.0), (-0.1, 0.95, 0.2))), (0.42, SW((-5.0, 50.0, -2.0), (-0.05, 0.3, 0.95))),
        (0.55, SW((-5.0, 51.0, -1.0), (-0.05, 0.15, 0.99))), (0.62, SW((-3.0, 46.0, -14.0), (0.0, 0.55, -0.83))), (0.7, SW((-1.5, 30.0, -17.0), (0.0, -0.3, -0.95))),
        (0.9, SW((-1.5, 29.0, -16.5), (0.0, -0.32, -0.95))), (1.05, SW((-2.0, 30.0, -14.0), (0.05, 0.4, -0.9)))],
        {'chest': [(0.42, [-14, 12, 0]), (0.55, [-16, 12, 0]), (0.7, [26, -4, 0]), (0.9, [28, -4, 0])],
         'torso': [(0.42, [-6, 10, 0]), (0.7, [12, 0, 0]), (0.9, [12, 0, 0])],
         'head': [(0.42, [-6, 0, 0]), (0.7, [-12, 0, 0]), (0.9, [-12, 0, 0])],
         'hips': [(0.42, [0, 4, 0]), (0.7, [0, -8, 0]), (0.9, [0, -8, 0])],
         'right_leg': [(0.42, [22, 0, 4]), (0.7, [20, 0, 4]), (0.9, [20, 0, 4])], 'right_shin': [(0.42, [22, 0, 0]), (0.7, [30, 0, 0]), (0.9, [30, 0, 0])],
         'left_leg': [(0.42, [-14, 0, -4]), (0.7, [-42, 0, -4]), (0.9, [-42, 0, -4])], 'left_shin': [(0.42, [10, 0, 0]), (0.7, [48, 0, 0]), (0.9, [48, 0, 0])]},
        {'root': [(0.42, [0, 1.0, 0.8]), (0.55, [0, 1.4, 1.0]), (0.7, [0, -4.0, -2.6]), (0.9, [0, -4.2, -2.6])]})
    # SWEEP "Reaping Arc": cocks the halberd past the right hip, then a flat two-handed reap across the front (impact 0.7)
    N_UP = (0, 1, 0)
    A['sweep'] = act(1.2, [
        (0.2, SW((-6.0, 33.0, -14.0), (-0.45, 0.45, -0.77), N_UP)), (0.45, SW((-9.0, 31.0, -5.0), (-0.85, 0.1, 0.5), N_UP)), (0.55, SW((-9.5, 31.0, -4.0), (-0.8, 0.1, 0.58), N_UP)),
        (0.7, SW((0.0, 30.0, -14.0), (0.2, 0.0, -0.98), N_UP)), (0.85, SW((7.0, 30.0, -10.0), (0.9, 0.0, -0.44), N_UP)), (1.0, SW((6.0, 30.0, -10.0), (0.88, 0.05, -0.46), N_UP))],
        {'chest': [(0.45, [4, 30, 0]), (0.55, [4, 32, 0]), (0.7, [8, -8, 0]), (0.85, [8, -34, 0]), (1.0, [8, -32, 0])],
         'torso': [(0.45, [4, 16, 0]), (0.7, [6, -4, 0]), (0.85, [6, -18, 0]), (1.0, [6, -16, 0])],
         'head': [(0.45, [-10, -26, 0]), (0.7, [-10, 6, 0]), (0.85, [-10, 26, 0]), (1.0, [-10, 24, 0])],
         'left_leg': [(0.7, [-30, 0, -6]), (1.0, [-30, 0, -6])], 'left_shin': [(0.7, [36, 0, 0]), (1.0, [36, 0, 0])]},
        {'root': [(0.45, [0, -0.8, 0]), (0.7, [0, -2.8, -1.2]), (1.0, [0, -2.6, -1.2])]})
    # SPIN "Spire Wheel": halberd out flat to his right, one full heavy turn (impact 0.7)
    SPN = SW((-8.0, 31.0, -8.0), (-0.9, 0.05, -0.42), N_UP)
    A['spin'] = act(1.4, [(0.35, SPN), (1.05, SPN)],
        {'chest': [(0.35, [4, 14, 0]), (1.05, [4, 14, 0])], 'head': [(0.35, [-10, -10, 0]), (1.05, [-10, -10, 0])],
         'right_leg': [(0.35, [-10, 0, 8]), (1.05, [-10, 0, 8])], 'left_leg': [(0.35, [-10, 0, -8]), (1.05, [-10, 0, -8])],
         'right_shin': [(0.35, [20, 0, 0]), (1.05, [20, 0, 0])], 'left_shin': [(0.35, [20, 0, 0]), (1.05, [20, 0, 0])]},
        {'root': [(0.35, [0, -2.6, 0]), (1.05, [0, -2.6, 0])]},
        extra={'root': {'rotation': S.lin([(0, R0), (0.38, [0, 30, 0]), (0.72, [0, -200, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])])}})
    # BARRIER: plants the halberd upright before him and raises the left fist; the core flares
    A['barrier'] = act(1.2, [(0.35, PLANT), (0.9, PLANT)],
        {'head': [(0.35, [-22, 0, 0]), (0.9, [-22, 0, 0])], 'chest': [(0.35, [-6, 0, 0]), (0.9, [-6, 0, 0])]},
        lfree=[(0.12, 1.05)],
        lf={'left_arm': [(0.35, [-168, 0, 22]), (0.9, [-170, 0, 24])], 'left_forearm': [(0.35, [-8, 0, 0]), (0.9, [-8, 0, 0])], 'left_hand': [(0.35, R0), (0.9, R0)]},
        extra={'core': {'scale': S.ch([(0, [1, 1, 1]), (0.35, [1.5, 1.5, 1.5]), (0.9, [1.5, 1.5, 1.5]), (1.2, [1, 1, 1])])}})
    # ROAR: hunches over the planted halberd, then rears back; the core and spire flare
    A['roar'] = act(2.4, [(0.4, PLANT), (0.6, mv(PLANT, (0, -2, 0))), (1.0, PLANT), (1.9, PLANT)],
        {'chest': [(0.6, [24, 0, 0]), (1.0, [-18, 0, 0]), (1.9, [-16, 0, 0])], 'head': [(0.6, [16, 0, 0]), (1.0, [-34, 0, 0]), (1.9, [-32, 0, 0])],
         'torso': [(0.6, [10, 0, 0]), (1.0, [-6, 0, 0]), (1.9, [-6, 0, 0])]},
        {'root': [(0.6, [0, -2.6, 0]), (1.0, [0, 0.3, 0]), (1.9, [0, 0.2, 0])]},
        extra={'core': {'scale': S.ch([(0, [1, 1, 1]), (0.6, [0.8, 0.8, 0.8]), (1.0, [1.7, 1.7, 1.7]), (1.9, [1.6, 1.6, 1.6]), (2.4, [1, 1, 1])])}})
    return A

def anims(rg):
    A = author(rg); ik = B3.bake(CF, rg, A); cl = B3.cloth(CF, rg, A, ik)
    out = {}
    for name, a in A.items():
        T = a['T']; b = {}
        for bn, ks in a['body'].items(): b.setdefault(bn, {})['rotation'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in a['pos'].items(): b.setdefault(bn, {})['position'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in ik[name].items(): b.setdefault(bn, {})['rotation'] = S.ch(ks)
        b['halberd'] = {'rotation': S.hold(R0, T)}
        for bn, ks in cl[name].items(): b.setdefault(bn, {})['rotation'] = S.ch([(round(t, 4), [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, chn in a['extra'].items():
            for k_, v in chn.items(): b.setdefault(bn, {})[k_] = v
        b.setdefault('tabard', {'rotation': S.loop(T, 4, 0, (2, 0, 0), 0.4)})
        for i in range(1, 5): b.setdefault(f'spire{i}', {'rotation': S.loop(T, 2.0, 0, (-4, 0, 0), 0.3 * i)})
        out[name] = S.A(T, b, a['loop'])
    return out

FREEZE = {'idle_combat': ('idle_combat', 0.0), 'attack_windup': ('attack', 0.5), 'attack': ('attack', 0.7), 'sweep': ('sweep', 0.7),
          'spin': ('spin', 0.7), 'barrier': ('barrier', 0.6), 'roar': ('roar', 1.2)}

def build(paint=True):
    S.PAL.update(PAL)
    m = dict(PAL[NAME], ts=list(PAL[NAME]['ts'])); rg = rig(); uvs = G.pack(rg, m['k'], m['ts'])
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    if paint:
        hm = dict(m, k=m['k'] * 2, ts=[m['ts'][0] * 2, m['ts'][1] * 2]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = B3.Painter3(NAME, hm)
        for uv, c in zip(huvs, cs): pt.cube(uv, c)
        S.downsample(NAME, m, rg, pt, m['k'], huvs, uvs)
    G.geo(NAME, rg, m['k'], m['ts'], uvs)
    G.write_anims(NAME, anims(rg))
    import freeze_v3; freeze_v3.run(NAME, FREEZE)
    print(NAME, len(rg.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    build(paint='nopaint' not in sys.argv)

"""1.1.2 polish pass: Khaos Warden of the Rift, rebuilt to the Hollow King v3 standard.
Sculpted gunmetal plate (octagonal limbs, overlapping lames, domed five-lame pauldrons), a fanged great helm with a
burning visor slit, swept horns and a spiked crown, a blood-core breastplate with emissive rift veins, a two-handed
greatsword gripped by both fists every 1/20 s (boss3.bake), a cloth-baked split cape, and a weighted animation set
(draw from the back scabbard, two-handed chop, ground slam, lunge thrust, roar over the planted blade).
Bones the renderer swaps are kept: sword / right_grip (drawn) vs right_open + sword_back (sheathed).
Run: python3 rig_warden3.py [nopaint]"""
import math, sys
import numpy as np
import gen_geo_assets as G
import rigs_saga2 as S
import king_ik as K
import boss3 as B3
from boss3 import octo, otaper, plate, horn, SW, mv, R0
Rig = G.Rig
NAME = 'rift_warden'
HX, HY = 15.0, 32.0
FY, FZ = HY - 2.4, -0.4
SH = (15.0, 58.0)
BZ = 13.6                                   # scabbard depth on the back
CAPE = dict(prefix='cape', halves=[('R', -1, -4.6), ('L', 1, 4.6)], top=59.6, z=7.0, overlap=0.8,
            segs=[(10, 12.6), (10, 13.2), (10, 13.8), (9.5, 14.4), (9, 15.0), (8.5, 15.6)])

def fist(r, bone, x, s):
    fy = FY
    octo(r, bone, (x, fy + 0.15, FZ), (5.0, 4.6, 4.6), 'gauntlet', k=0.92, bev=0.18)
    for f in range(4): B_(r, bone, (x - s * 0.25, fy - 2.1, -2.2 + f * 1.25 + FZ + 0.4), (4.6, 1.2, 1.05), 'breastplate' if f % 2 else 'gauntlet')
    B_(r, bone, (x - s * 2.65, fy + 0.5, -2.1), (1.2, 3.0, 1.9), 'gauntlet')          # thumb over the grip
    B_(r, bone, (x + s * 2.4, fy + 0.3, FZ), (0.7, 3.5, 4.0), 'crown')                # knuckle plate
    B_(r, bone, (x, fy + 2.6, FZ), (4.4, 0.8, 4.0), 'breastplate')

def B_(r, bone, c, size, role, rot=None, pivot=None): r.box(bone, c, size, role, rot=rot, pivot=pivot)

def sword_parts(r, bone, x, f, z):
    """Two-handed greatsword built along +Y from the right-fist centre f."""
    octo(r, bone, (x, f - 11.6, z), (3.4, 1.6, 3.4), 'crown'); B_(r, bone, (x, f - 11.6, z), (2.6, 2.6, 2.6), 'core', rot=(45, 0, 45))
    B_(r, bone, (x, f - 12.9, z), (1.2, 1.0, 1.2), 'crown')
    otaper(r, bone, x, z, f - 10.8, f + 2.8, 2.1, 2.3, 2.1, 2.3, 3, 'grip')
    for y in (-10.6, -5.2, 0.4): octo(r, bone, (x, f + y, z), (2.7, 0.5, 2.7), 'crown')
    plate(r, bone, (x, f + 3.9, z), (3.2, 2.4, 18.0), 'guard', bev=0.5, raise_=0.0)
    for sz_ in (-1, 1):
        B_(r, bone, (x, f + 3.0, z + sz_ * 9.6), (2.4, 3.4, 2.0), 'guard', rot=(sz_ * -28, 0, 0))
        B_(r, bone, (x, f + 1.4, z + sz_ * 10.4), (1.4, 1.4, 1.4), 'core', rot=(45, 45, 0))
    octo(r, bone, (x, f + 4.0, z), (3.8, 3.8, 6.0), 'guard'); B_(r, bone, (x, f + 4.0, z - 3.2), (2.0, 2.0, 0.4), 'core', rot=(0, 0, 45))
    B_(r, bone, (x, f + 4.0, z + 3.2), (2.0, 2.0, 0.4), 'core', rot=(0, 0, 45))
    B_(r, bone, (x, f + 7.4, z), (2.4, 3.6, 7.6), 'greatblade')                                       # ricasso
    S.taper(r, bone, x, z, f + 9.0, f + 56.0, 1.9, 1.3, 8.6, 6.2, 6, 'greatblade')
    S.taper(r, bone, x, z, f + 9.0, f + 56.0, 0.6, 0.45, 9.6, 7.2, 6, 'edge')
    B_(r, bone, (x, f + 58.6, z), (1.4, 5.2, 4.6), 'greatblade'); B_(r, bone, (x, f + 62.2, z), (1.0, 3.0, 2.4), 'greatblade')
    B_(r, bone, (x, f + 58.6, z), (0.5, 5.6, 5.4), 'edge'); B_(r, bone, (x, f + 62.4, z), (0.4, 3.4, 3.0), 'edge')
    B_(r, bone, (x, f + 31.0, z), (2.1, 42.0, 1.3), 'rune')                                           # burning fuller
    for y in (14, 24, 34, 44): B_(r, bone, (x, f + y, z), (2.2, 0.6, 3.2), 'crown')

def rig():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 5.4
        lg = r.bone(f'{side}_leg', 'root', (x, 30, 0))
        otaper(r, lg, x, 0, 30, 16.4, 7.6, 6.4, 7.8, 6.6, 3, 'plate')
        for j in range(3):
            plate(r, lg, (x + s * 1.1, 28.2 - j * 3.3, -0.4 - j * 0.2), (8.4 - j * 0.4, 4.0, 9.0 - j * 0.4), 'breastplate', rot=(0, 0, s * (10 + j * 3)), pivot=(x, 30, 0), bev=0.6, raise_=0.3)
        octo(r, lg, (x, 16.4, -1.6), (6.4, 5.0, 5.8), 'breastplate')
        B(lg, (x, 16.4, -4.6), (3.0, 3.0, 0.7), 'crown', rot=(0, 0, 45)); B(lg, (x, 16.4, -5.0), (1.3, 1.3, 0.3), 'core', rot=(0, 0, 45))
        plate(r, lg, (x + s * 3.5, 16.4, -1.0), (1.1, 4.4, 4.8), 'breastplate', bev=0.4, raise_=0.2)
        sh = r.bone(f'{side}_shin', lg, (x, 15, 0))
        otaper(r, sh, x, 0, 15, 3.4, 6.4, 5.4, 6.6, 5.6, 3, 'plate')
        plate(r, sh, (x, 9.4, -3.0), (5.4, 11.0, 1.2), 'breastplate', bev=0.7, raise_=0.4)
        B(sh, (x, 9.4, -3.85), (0.7, 9.8, 0.3), 'rune')
        for j in range(4):
            octo(r, sh, (x, 2.4 - j * 0.55, -1.2 - j * 1.9), (6.6 - j * 0.45, 1.8, 6.4 - j * 0.8), 'gauntlet' if j % 2 else 'breastplate')
        B(sh, (x, 0.6, -8.2), (3.2, 1.2, 1.6), 'crown')
    r.bone('hips', 'root', (0, 30, 0))
    octo(r, 'hips', (0, 30.6, 0), (17.0, 4.2, 12.0), 'belt')
    B('hips', (0, 30.6, -6.3), (4.8, 4.8, 1.0), 'guard', rot=(0, 0, 45)); B('hips', (0, 30.6, -6.9), (2.4, 2.4, 0.4), 'core', rot=(0, 0, 45))
    for s in (-1, 1): plate(r, 'hips', (s * 8.6, 29.4, 0), (1.4, 5.4, 10.0), 'breastplate', rot=(0, 0, s * 7), bev=0.5, raise_=0.2)
    r.bone('tabard', 'hips', (0, 29, -6.4))
    B('tabard', (0, 19.0, -6.6), (7.0, 20.0, 0.7), 'robe'); B('tabard', (0, 19.0, -7.0), (1.2, 19.0, 0.2), 'crown')
    B('tabard', (0, 23.4, -7.1), (4.0, 4.0, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 8.4, -6.6), (6.6, 3.0, 0.7), 'capehem')
    r.bone('torso', 'hips', (0, 31.5, 0))
    otaper(r, 'torso', 0, 0, 31.5, 43, 14.4, 16.6, 10.2, 11.0, 2, 'plate')
    for j in range(3):
        octo(r, 'torso', (0, 33.4 + j * 3.4, -0.2 * j), (15.0 + j * 1.0, 3.0, 11.2 + j * 0.4), 'breastplate', k=0.86)
        B('torso', (0, 34.6 + j * 3.4, -5.9 - j * 0.3), (8.0 + j, 0.5, 0.3), 'crown')
    r.bone('chest', 'torso', (0, 43, 0))
    otaper(r, 'chest', 0, 0.8, 43, 58, 18.6, 25.0, 10.6, 11.6, 3, 'plate')
    octo(r, 'chest', (0, 51.0, 4.4), (21.0, 12.0, 3.6), 'breastplate', k=0.9)
    for s in (-1, 1):
        for j, (dy, yaw) in enumerate(((0, -10), (4.6, -22), (-4.6, -4))):
            plate(r, 'chest', (s * (8.4 + 0.6 * j), 50.6 + dy, -5.6 - (0.25 if j == 0 else 0)), (7.4, 6.0 if j else 6.6, 3.0), 'breastplate',
                  rot=(0, s * yaw, s * 3), pivot=(s * 12.0, 50.6, -5.4), bev=0.6, raise_=0.35)
        B('chest', (s * 4.9, 50.6, -7.3), (0.7, 15.0, 0.7), 'crown')
        B('chest', (s * 9.4, 57.2, -6.2), (7.0, 0.7, 0.7), 'crown', rot=(0, 0, s * -10))
    plate(r, 'chest', (0, 50.6, -6.1), (8.6, 14.0, 1.6), 'breastplate', bev=0.6, raise_=0.4)
    r.bone('heart', 'chest', (0, 51, -7.2))
    B('heart', (0, 51, -7.4), (4.0, 4.6, 1.6), 'core', rot=(0, 0, 45)); B('heart', (0, 51, -7.8), (6.2, 6.2, 0.4), 'rune', rot=(0, 0, 45))
    B('heart', (0, 51, -7.9), (0.8, 9.4, 0.3), 'rune'); B('heart', (0, 51, -7.9), (9.4, 0.8, 0.3), 'rune')
    for j in range(2): octo(r, 'chest', (0, 58.6 + j * 1.6, 0.3), (14.0 - j * 2.2, 2.0, 12.0 - j * 1.8), 'guard' if j == 0 else 'helm')
    octo(r, 'chest', (0, 59.0, 0.6), (19.0, 2.6, 13.4), 'pauldron', k=0.9)            # gorget collar
    # arms: five-lame domed pauldrons with spikes, couters, flared gauntlets
    for side, s in (('right', -1), ('left', 1)):
        x = s * HX
        arm = r.bone(f'{side}_arm', 'chest', (x, SH[1], 0))
        for j in range(5):
            octo(r, arm, (x + s * 0.75 * j, 61.4 - j * 2.25, 0.3), (14.4 - j * 1.15, 2.8, 14.0 - j * 0.95), 'pauldron', rz=s * (8 + j * 7), k=0.88)
            B(arm, (x + s * (0.75 * j + 0.3), 61.0 - j * 2.25, -6.9 + j * 0.45), (9.4 - j * 1.0, 0.55, 0.35), 'crown', rot=(0, 0, s * (8 + j * 7)))
        octo(r, arm, (x + s * 0.5, 63.2, 0.3), (10.4, 1.8, 10.4), 'pauldron', rz=s * 6)
        for j, zz in enumerate((-3.6, 0.0, 3.6)):
            B(arm, (x + s * (3.6 + (0.6 if j == 1 else 0)), 66.0 + (1.2 if j == 1 else 0), zz), (1.6, 6.0 + (2.0 if j == 1 else 0), 1.6), 'crown', rot=(0, 0, s * -22))
        otaper(r, arm, x, 0, 57, 44.4, 7.2, 6.6, 7.2, 6.6, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 44, 0))
        octo(r, fa, (x, 43.6, 0.8), (7.2, 4.4, 6.4), 'breastplate')
        plate(r, fa, (x + s * 3.7, 43.6, 0.8), (1.0, 4.0, 4.6), 'breastplate', bev=0.4, raise_=0.2)
        otaper(r, fa, x, 0, 42.6, HY + 3.4, 6.0, 7.0, 6.0, 7.0, 2, 'plate')
        octo(r, fa, (x, HY + 3.0, 0), (7.6, 1.8, 7.0), 'crown', k=0.9)
        B(fa, (x, HY + 6.4, -3.7), (3.2, 5.2, 0.4), 'rune')
        hd = r.bone(f'{side}_hand', fa, (x, HY, FZ))
        B(hd, (x, HY + 1.2, FZ), (5.6, 1.4, 5.4), 'gauntlet')                 # wrist under the cuff
        if side == 'left': fist(r, hd, x, s)
        else:
            gb = r.bone('right_grip', hd, (x, FY, FZ)); fist(r, gb, x, s)
            ob = r.bone('right_open', hd, (x, HY, FZ))                       # open hand while the sword is on his back
            octo(r, ob, (x, FY + 0.4, FZ), (4.8, 4.4, 4.6), 'gauntlet', k=0.92, bev=0.18)
            for f_, zz in enumerate((-1.8, -0.6, 0.6, 1.8)):
                B(ob, (x + s * 0.3, FY - 3.6 - (0.4 if f_ in (1, 2) else 0), zz + FZ), (1.6, 3.8, 1.0), 'gauntlet')
                B(ob, (x + s * 0.6, FY - 5.8 - (0.4 if f_ in (1, 2) else 0), zz + FZ), (1.4, 1.2, 0.9), 'crown')
            B(ob, (x - s * 2.4, FY - 1.4, FZ - 1.6), (1.2, 3.4, 1.6), 'gauntlet', rot=(0, 0, s * 18))
    # head: fanged great helm, burning visor slit, cheek plates, swept horns, spiked crown
    r.bone('head', 'chest', (0, 60.4, -0.6))
    octo(r, 'head', (0, 67.0, -0.6), (11.0, 12.0, 11.6), 'helm', k=0.86)
    octo(r, 'head', (0, 73.6, -0.6), (9.6, 1.6, 10.2), 'helm', k=0.86); octo(r, 'head', (0, 74.8, -0.6), (7.4, 1.0, 7.8), 'helm', k=0.86)
    for s in (-1, 1):
        plate(r, 'head', (s * 2.6, 66.4, -6.6), (5.4, 9.0, 1.3), 'visorplate', rot=(0, s * -24, 0), pivot=(0, 66.4, -7.5), bev=0.45, raise_=0.25)
        B('head', (s * 2.25, 68.6, -7.6), (3.6, 1.1, 0.35), 'void', rot=(0, s * -24, 0), pivot=(0, 68.6, -8.1))
        B('head', (s * 2.2, 68.6, -7.8), (2.9, 0.6, 0.25), 'eye', rot=(0, s * -24, 0), pivot=(0, 68.6, -8.1))
        for j in range(3): B('head', (s * 1.9, 64.6 - j * 1.1, -7.75), (0.6, 0.6, 0.35), 'void', rot=(0, s * -24, 0), pivot=(0, 64.6, -8.1))
        plate(r, 'head', (s * 5.8, 66.2, -0.8), (1.1, 9.0, 9.2), 'breastplate', bev=0.5, raise_=0.25)
        horn(r, 'head', (s * 5.4, 71.0, 0.2), s, n=10, L=2.0, w0=2.8, dw=0.2, out=1.15)
    B('head', (0, 66.8, -8.45), (0.8, 10.2, 0.8), 'crown')
    for i, xx in enumerate((-3.2, -1.9, -0.65, 0.65, 1.9, 3.2)):                               # fanged grille
        big = i in (0, 5)
        B('head', (xx, 61.9 - (0.5 if big else 0), -6.9), (0.8, 2.2 if big else 1.4, 0.6), 'edge')
    for xx, h in ((-4.2, 3.4), (-2.1, 5.2), (0, 7.6), (2.1, 5.2), (4.2, 3.4)):                  # spiked crown
        B('head', (xx, 75.4 + h / 2, -3.6), (1.4, h, 1.4), 'crown', rot=(-10, 0, -xx * 3), pivot=(xx, 75.4, -3.6))
        B('head', (xx, 75.4 + h + 0.4, -3.6 - h * 0.17), (0.9, 0.9, 0.9), 'core', rot=(45, 45, 0))
    octo(r, 'head', (0, 75.2, -0.6), (10.0, 1.0, 10.6), 'crown')
    # scabbard on the back: hilt over the right shoulder, point toward the left hip (outside the cape)
    r.bone('scabbard', 'chest', (0, 46, BZ), (0, 90, -146))
    fb = 46 - 27.0
    B('scabbard', (0, fb + 33.0, BZ), (2.6, 50.0, 10.6), 'cape')
    for yy in (fb + 9.0, fb + 57.0): B('scabbard', (0, yy, BZ), (3.2, 2.6, 11.4), 'guard')
    for yy in (fb + 22, fb + 42): B('scabbard', (0, yy, BZ), (3.0, 1.4, 11.2), 'crown')
    r.bone('sword_back', 'scabbard', (0, fb, BZ))
    sword_parts(r, 'sword_back', 0, fb, BZ)
    B('chest', (0, 50, -6.9), (2.4, 30, 0.8), 'belt', rot=(0, 0, 40)); B('chest', (0, 50, 6.4), (2.4, 30, 0.8), 'belt', rot=(0, 0, -40))
    # sword in hand: blade leaves the fist perpendicular to the forearm
    r.bone('sword', 'right_grip', (-HX, FY, FZ), (-90, 0, 0))
    sword_parts(r, 'sword', -HX, FY, FZ)
    B3.cape_bones(r, CF, sigil='sigil')
    return r

def spheres(fk, R, Q):
    Cm = fk.world('chest', R, Q); Hm = fk.world('hips', R, Q); Tm = fk.world('torso', R, Q); Sm = fk.world('sword_back', R, Q)
    sph = [(K.xf(Cm, (0, 51, 0)), 10.0), (K.xf(Cm, (-6.5, 54, 0)), 8.6), (K.xf(Cm, (6.5, 54, 0)), 8.6), (K.xf(Tm, (0, 37, 0.5)), 8.0), (K.xf(Hm, (0, 29, 0.5)), 8.4)]
    sph += [(K.xf(Sm, (0, 19 + y, BZ)), 5.8) for y in range(-6, 62, 4)]
    for side, xx in (('right', -5.4), ('left', 5.4)):
        lm = fk.world(f'{side}_leg', R, Q); sm = fk.world(f'{side}_shin', R, Q)
        sph += [(K.xf(lm, (xx, 25, 0)), 5.2), (K.xf(lm, (xx, 19, 0)), 4.8), (K.xf(sm, (xx, 11, 0)), 4.2), (K.xf(sm, (xx, 5, 0)), 4.0)]
    return sph

CF = B3.Cfg(weapon='sword', HX=HX, FY=FY, FZ=FZ, SH=SH, gap=5.4, reach=27.0, ax=(-12.5, 63.5),
            widths=[(1.4, 10.8), (3.0, 10.2), (4.0, 9.0), (8, 4.0), (30, 3.6), (50, 3.0)], hazards=[((0, -11.6, 0), 5.6), ((0, 3.9, -9.0), 4.0), ((0, 3.9, 9.0), 4.0)],
            fa_pts=(HY + 3.4, HY + 5, HY + 7, 39.5, 42.5, 44.5), fa_rad=4.7, cape=dict(CAPE, spheres=spheres))

PAL = {NAME: dict(who=NAME, k=3, ts=[768, 768], boss=True,
       pal=dict(base='#2A2327', dark='#120D10', accent='#5C5053', robe='#4A141C', cape='#4A141C'),
       trim='#8A6A40', glow='#FF3B47', eye='#FF6A6A', core='#FF5560', crystal='#A15FC0', eyes='visor',
       mat=dict(metal={'plate': (50, 46, 52), 'breastplate': (66, 60, 66), 'pauldron': (62, 56, 62), 'helm': (58, 52, 58), 'gauntlet': (44, 40, 46), 'visorplate': (70, 64, 70), 'belt': (40, 32, 34)},
                spec=(196, 178, 184), rim=(150, 132, 136), trim=((120, 84, 50), (206, 150, 92), (255, 214, 160)), rune=(255, 86, 90),
                trimmed=('breastplate', 'pauldron', 'visorplate'), etched=('breastplate', 'pauldron', 'helm'), glowing=('breastplate', 'pauldron'),
                veined=('plate', 'helm'), vein=(255, 60, 66),
                cloth={'robe': (104, 20, 30), 'cape': (88, 16, 26), 'capehem': (88, 16, 26)}, sheen=(170, 56, 70), hem=(150, 110, 70),
                eye=((255, 120, 110), (255, 236, 226)), core=((255, 70, 80), (255, 210, 200)), edge=((170, 160, 170), (236, 230, 236)),
                horn=((26, 18, 22), (96, 70, 72))))}

# ------------------------------------------------------------------ animation
def hilt_pose(rg, R=None, Q=None):
    """Right-fist grip target on the sheathed sword (root space, rest root) for the draw / sheathe."""
    fk = K.FK(rg); R = R or {}; Q = Q or {}
    m = fk.world('sword_back', R, Q); P = K.xf(m, (0, 46 - 27.0, BZ)); D = K.xd(m, (0, 1, 0)); N = K.xd(m, (1, 0, 0))
    Rm = np.linalg.inv(fk.world('root', R, Q))
    return SW(tuple(K.xf(Rm, P)), tuple(K.xd(Rm, D)), tuple(K.xd(Rm, N)))

GUARD = SW((1.0, 39.0, -13.5), (0.1, 0.86, -0.5))
PLANT = SW((-1.0, 45.0, -24.0), (0.0, -0.94, -0.34))
REST_R = {'right_arm': [-4, 0, 7], 'right_forearm': [-14, 0, 0], 'right_hand': [0, 0, 0]}
REST_L = {'left_arm': [-4, 0, -7], 'left_forearm': [-14, 0, 0], 'left_hand': [0, 0, 0]}

def author(rg):
    A = {}; base = B3.base
    BODY = ['chest', 'torso', 'head', 'hips', 'right_leg', 'left_leg', 'right_shin', 'left_shin']
    def held(d, T): return {bn: [(0, v), (T, v)] for bn, v in d.items()}
    # IDLE (sheathed): slow heavy breath, open hands at his sides
    T = 4.0
    A['idle'] = base(T, True, None,
        body={'chest': [(0, [2, 0, 0]), (T / 2, [0, 0, 0]), (T, [2, 0, 0])], 'head': [(0, [-4, 0, 0]), (T / 2, [-2, 8, 0]), (T, [-4, 0, 0])],
              'right_leg': [(0, [-2, 0, 3]), (T, [-2, 0, 3])], 'left_leg': [(0, [3, 0, -3]), (T, [3, 0, -3])],
              'right_shin': [(0, [3, 0, 0]), (T, [3, 0, 0])], 'left_shin': [(0, [2, 0, 0]), (T, [2, 0, 0])]},
        pos={'root': [(0, [0, 0, 0]), (T / 2, [0, -0.5, 0]), (T, [0, 0, 0])]},
        rf={bn: [(0, v), (T / 2, list(np.add(v, [-3, 0, -2]))), (T, v)] for bn, v in REST_R.items()},
        lf={bn: [(0, v), (T / 2, list(np.add(v, [-3, 0, 2]))), (T, v)] for bn, v in REST_L.items()})
    # WALK (sheathed): heavy stride, arms swinging against the legs
    T = 1.5; st = B3.stride(T, 20, 24, 1.6)
    A['walk'] = base(T, True, None,
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'torso': [(t, [3, v[1] * 0.4, 0]) for t, v in st['sway']], 'chest': [(t, [4, v[1], 0]) for t, v in st['sway']],
              'head': [(0, [-4, 0, 0]), (T, [-4, 0, 0])]},
        pos={'root': st['root_pos']},
        rf={'right_arm': [(T * i / 8, [-18 * math.sin(2 * math.pi * i / 8 + math.pi) - 4, 0, 7]) for i in range(9)], 'right_forearm': [(0, [-18, 0, 0]), (T, [-18, 0, 0])], 'right_hand': [(0, R0), (T, R0)]},
        lf={'left_arm': [(T * i / 8, [-18 * math.sin(2 * math.pi * i / 8) - 4, 0, -7]) for i in range(9)], 'left_forearm': [(0, [-18, 0, 0]), (T, [-18, 0, 0])], 'left_hand': [(0, R0), (T, R0)]})
    # IDLE_COMBAT: two-handed guard, blade angled up before him, weight sunk into a staggered stance
    T = 3.0
    A['idle_combat'] = base(T, True, [(0, GUARD), (T / 2, mv(GUARD, (0, -0.9, 0.3))), (T, GUARD)],
        body={'chest': [(0, [8, 0, 0]), (T / 2, [10, -4, 0]), (T, [8, 0, 0])], 'head': [(0, [-12, 0, 0]), (T / 2, [-10, 4, 0]), (T, [-12, 0, 0])],
              'torso': [(0, [4, 8, 0]), (T, [4, 8, 0])], 'hips': [(0, [0, -8, 0]), (T, [0, -8, 0])],
              'right_leg': [(0, [14, 8, 4]), (T, [14, 8, 4])], 'left_leg': [(0, [-22, 8, -4]), (T, [-22, 8, -4])],
              'right_shin': [(0, [18, 0, 0]), (T, [18, 0, 0])], 'left_shin': [(0, [24, 0, 0]), (T, [24, 0, 0])]},
        pos={'root': [(0, [0, -1.8, 0]), (T / 2, [0, -2.4, 0]), (T, [0, -1.8, 0])]})
    IC = A['idle_combat']['body']; g = lambda bn: list(IC[bn][0][1]) if bn in IC else [0, 0, 0]
    T = 1.3; st = B3.stride(T, 18, 22, 1.2)
    A['walk_combat'] = base(T, True, [(T * i / 4, mv(GUARD, (0, -0.7 if i % 2 else 0, 0))) for i in range(5)],
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'chest': [(t, [9, v[1], 0]) for t, v in st['sway']], 'head': [(0, [-12, 0, 0]), (T, [-12, 0, 0])], 'torso': [(0, [5, 0, 0]), (T, [5, 0, 0])]},
        pos={'root': st['root_pos']})
    def act(T, keys, body, pos=None, end=True, start=None, **kw):
        bb = {}
        for bn in BODY:
            bb[bn] = [(0, g(bn))] + body.get(bn, []) + ([(T, g(bn))] if end else [])
            if len(bb[bn]) == 1: bb[bn].append((T, g(bn)))
        pr = (pos or {}).get('root', [])
        pp = {'root': [(0, [0, -1.8, 0])] + pr + ([(T, [0, -1.8, 0])] if end or not pr else [])}
        return base(T, False, [(0, start or GUARD)] + keys + ([(T, GUARD)] if end else []), bb, pp, **kw)
    # DRAW: reaches over his right shoulder, takes the hilt (0.3-0.4), pulls the blade free overhead and settles into guard
    HP = hilt_pose(rg)
    HPa = mv(HP, (0, -3.0, 2.0), D=tuple(np.array(HP['D']) * 1.0))
    REST_SW = SW((-HX - 0.5, FY + 1.0, -2.0), (0.0, -0.2, -0.98))
    out = SW((-6.0, 74.0, -4.0), (0.1, 0.6, -0.8))
    UP1 = SW((-16.0, 80.0, 8.0), (0.15, -0.45, 0.88)); UP2 = SW((-12.0, 82.0, -4.0), (-0.15, 0.85, 0.5)); UP3 = SW((-9.0, 68.0, -18.0), (0.0, 0.85, -0.52))
    A['draw'] = base(1.3, False, [(0, REST_SW), (0.18, SW((-19.0, 50.0, -2.0), (0.0, 0.4, 0.9))), (0.3, HPa), (0.4, HP), (0.55, UP1),
                                  (0.7, UP2), (0.85, UP3), (1.05, mv(GUARD, (0, 3.0, -1.0))), (1.3, GUARD)],
        body={'chest': [(0, [2, 0, 0]), (0.35, [-6, -14, 0]), (0.6, [-10, -6, 0]), (0.85, [12, 6, 0]), (1.3, g('chest'))],
              'torso': [(0, [0, 0, 0]), (0.35, [-2, -8, 0]), (0.85, [5, 6, 0]), (1.3, g('torso'))],
              'head': [(0, [-4, 0, 0]), (0.35, [-2, -18, 0]), (0.85, [-12, 2, 0]), (1.3, g('head'))],
              'hips': [(0, [0, 0, 0]), (0.85, [0, -6, 0]), (1.3, g('hips'))],
              'right_leg': [(0, [-2, 0, 3]), (0.85, [12, 8, 4]), (1.3, g('right_leg'))], 'left_leg': [(0, [3, 0, -3]), (0.85, [-20, 8, -4]), (1.3, g('left_leg'))],
              'right_shin': [(0, [3, 0, 0]), (0.85, [18, 0, 0]), (1.3, g('right_shin'))], 'left_shin': [(0, [2, 0, 0]), (0.85, [24, 0, 0]), (1.3, g('left_shin'))]},
        pos={'root': [(0, [0, 0, 0]), (0.85, [0, -2.2, 0]), (1.3, [0, -1.8, 0])]}, noclear=[(0, 0.4)],
        lfree=[(-1, 1.0)], lf={'left_arm': [(0, REST_L['left_arm']), (0.35, [-10, 0, -12]), (0.65, [-34, 0, -18]), (0.85, [-50, 0, -12])],
                                 'left_forearm': [(0, REST_L['left_forearm']), (0.35, [-20, 0, 0]), (0.65, [-40, 0, 0]), (0.85, [-50, 0, 0])],
                                 'left_hand': [(0, R0), (0.85, R0)]})
    # SHEATHE: lifts the blade, slides it home over the right shoulder (0.4), lets the hand fall
    A['sheathe'] = base(1.0, False, [(0, GUARD), (0.12, UP3), (0.22, UP2), (0.32, UP1), (0.42, HP), (0.6, SW((-19.0, 50.0, -2.0), (0.0, 0.4, 0.9))), (1.0, REST_SW)],
        body={'chest': [(0, g('chest')), (0.35, [-6, -14, 0]), (1.0, [2, 0, 0])], 'torso': [(0, g('torso')), (0.35, [-2, -8, 0]), (1.0, [0, 0, 0])],
              'head': [(0, g('head')), (0.35, [-2, -18, 0]), (1.0, [-4, 0, 0])], 'hips': [(0, g('hips')), (1.0, [0, 0, 0])],
              'right_leg': [(0, g('right_leg')), (1.0, [-2, 0, 3])], 'left_leg': [(0, g('left_leg')), (1.0, [3, 0, -3])],
              'right_shin': [(0, g('right_shin')), (1.0, [3, 0, 0])], 'left_shin': [(0, g('left_shin')), (1.0, [2, 0, 0])]},
        pos={'root': [(0, [0, -1.8, 0]), (1.0, [0, 0, 0])]}, noclear=[(0.42, 1.0)],
        lfree=[(0.12, 2.0)], lf={'left_arm': [(0.25, [-30, 0, -14]), (0.6, [-10, 0, -10]), (1.0, REST_L['left_arm'])],
                                  'left_forearm': [(0.25, [-40, 0, 0]), (0.6, [-20, 0, 0]), (1.0, REST_L['left_forearm'])], 'left_hand': [(0.25, R0), (1.0, R0)]},
        rfree=[(0.75, 2.0)], rf={'right_arm': [(1.0, REST_R['right_arm'])], 'right_forearm': [(1.0, REST_R['right_forearm'])], 'right_hand': [(1.0, R0)]})
    # ATTACK "Rift Cleaver": hoists the blade over his right shoulder, hangs, then a two-handed chop (impact 0.8)
    A['attack'] = act(1.5, [
        (0.25, SW((-8.0, 62.0, -15.0), (-0.15, 0.95, 0.2))), (0.5, SW((-11.0, 78.0, -1.0), (-0.3, 0.35, 0.89))),
        (0.64, SW((-11.0, 79.0, 0.0), (-0.3, 0.25, 0.92))), (0.72, SW((-4.0, 70.0, -26.0), (-0.05, 0.6, -0.8))), (0.8, SW((-1.5, 38.0, -21.5), (0.0, -0.5, -0.86))),
        (1.05, SW((-1.5, 37.0, -21.0), (0.0, -0.52, -0.85))), (1.25, SW((-3.0, 41.0, -22.0), (0.05, 0.3, -0.95)))],
        {'chest': [(0.5, [-16, 12, 0]), (0.64, [-18, 12, 0]), (0.8, [28, -4, 0]), (1.05, [30, -4, 0])],
         'torso': [(0.5, [-8, 10, 0]), (0.8, [14, 0, 0]), (1.05, [14, 0, 0])],
         'head': [(0.5, [-6, 0, 0]), (0.8, [-14, 0, 0]), (1.05, [-14, 0, 0])],
         'hips': [(0.5, [0, 4, 0]), (0.8, [0, -8, 0]), (1.05, [0, -8, 0])],
         'right_leg': [(0.5, [24, 0, 4]), (0.8, [22, 0, 4]), (1.05, [22, 0, 4])], 'right_shin': [(0.5, [24, 0, 0]), (0.8, [34, 0, 0]), (1.05, [34, 0, 0])],
         'left_leg': [(0.5, [-14, 0, -4]), (0.8, [-46, 0, -4]), (1.05, [-46, 0, -4])], 'left_shin': [(0.5, [10, 0, 0]), (0.8, [52, 0, 0]), (1.05, [52, 0, 0])]},
        {'root': [(0.5, [0, 1.4, 1.0]), (0.64, [0, 1.8, 1.2]), (0.8, [0, -5.6, -3.4]), (1.05, [0, -5.8, -3.4])]})
    # SLAM "Riftbreaker": blade raised point-down overhead, drops to a crouch and drives it into the floor before him (impact 0.9)
    DRIVE = SW((-2.5, 47.0, -30.0), (0.0, -0.99, -0.12))
    A['slam'] = act(1.6, [(0.3, SW((-3.0, 60.0, -18.0), (0.0, 0.8, -0.6))), (0.55, SW((-3.0, 80.0, -9.0), (0.0, 0.1, -0.99))),
                          (0.75, SW((-3.0, 82.0, -8.0), (0.0, -0.35, -0.94))), (0.9, DRIVE), (1.2, DRIVE)],
        {'chest': [(0.55, [-16, 0, 0]), (0.75, [-20, 0, 0]), (0.9, [26, 0, 0]), (1.2, [26, 0, 0])],
         'torso': [(0.75, [-6, 0, 0]), (0.9, [12, 0, 0]), (1.2, [12, 0, 0])],
         'head': [(0.55, [-24, 0, 0]), (0.9, [-6, 0, 0]), (1.2, [-6, 0, 0])],
         'hips': [(0.9, [0, 0, 0]), (1.2, [0, 0, 0])],
         'right_leg': [(0.55, [-6, 0, 0]), (0.9, [30, 0, 8]), (1.2, [30, 0, 8])], 'right_shin': [(0.55, [8, 0, 0]), (0.9, [70, 0, 0]), (1.2, [70, 0, 0])],
         'left_leg': [(0.55, [8, 0, 0]), (0.9, [-62, 0, -6]), (1.2, [-62, 0, -6])], 'left_shin': [(0.55, [6, 0, 0]), (0.9, [64, 0, 0]), (1.2, [64, 0, 0])]},
        {'root': [(0.55, [0, 2.4, 0]), (0.75, [0, 3.0, 0]), (0.9, [0, -8.0, -2.0]), (1.2, [0, -8.0, -2.0])]})
    # LUNGE: coils low, then a driving two-handed thrust as he leaps (leap 0.5, lands ~0.85)
    THR = SW((-1.0, 44.0, -22.5), (0.04, 0.06, -1.0))
    A['lunge'] = act(1.3, [(0.35, SW((-13.0, 38.0, -16.0), (-0.25, 0.25, -0.93))), (0.6, THR), (0.85, mv(THR, (0, -2.0, -1.0))), (1.0, THR), (1.15, SW((-4.0, 42.0, -22.0), (0.08, 0.5, -0.86)))],
        {'chest': [(0.35, [14, 24, 0]), (0.6, [18, -6, 0]), (0.85, [20, -6, 0])], 'torso': [(0.35, [8, 12, 0]), (0.6, [10, -4, 0]), (0.85, [10, -4, 0])],
         'head': [(0.35, [-22, -18, 0]), (0.6, [-24, 4, 0]), (0.85, [-24, 4, 0])],
         'right_leg': [(0.35, [30, 0, 6]), (0.6, [36, 0, 4]), (0.85, [30, 0, 4])], 'right_shin': [(0.35, [44, 0, 0]), (0.6, [20, 0, 0]), (0.85, [30, 0, 0])],
         'left_leg': [(0.35, [-30, 0, -6]), (0.6, [-56, 0, -4]), (0.85, [-50, 0, -4])], 'left_shin': [(0.35, [44, 0, 0]), (0.6, [30, 0, 0]), (0.85, [46, 0, 0])]},
        {'root': [(0.35, [0, -6.0, 2.0]), (0.6, [0, -3.0, -4.0]), (0.85, [0, -5.0, -4.0])]})
    # ROAR: plants the blade before him, hunches over the pommel, then rears back with a howl
    A['roar'] = act(2.4, [(0.2, mv(PLANT, (0, 5, -3), D=(0.0, -0.6, -0.8))), (0.4, PLANT), (0.6, mv(PLANT, (0, -4, -2))), (1.0, PLANT), (1.9, PLANT), (2.2, mv(PLANT, (0, 5, -3), D=(0.0, -0.6, -0.8)))],
        {'chest': [(0.6, [14, 0, 0]), (1.0, [-20, 0, 0]), (1.9, [-18, 0, 0])], 'head': [(0.6, [6, 0, 0]), (1.0, [-38, 0, 0]), (1.9, [-36, 0, 0])],
         'torso': [(0.6, [10, 0, 0]), (1.0, [-6, 0, 0]), (1.9, [-6, 0, 0])]},
        {'root': [(0.6, [0, -3.4, 0]), (1.0, [0, 0.4, 0]), (1.9, [0, 0.2, 0])]})
    return A

def anims(rg):
    A = author(rg); ik = B3.bake(CF, rg, A); cl = B3.cloth(CF, rg, A, ik)
    out = {}
    for name, a in A.items():
        T = a['T']; b = {}
        for bn, ks in a['body'].items(): b.setdefault(bn, {})['rotation'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in a['pos'].items(): b.setdefault(bn, {})['position'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in ik[name].items(): b.setdefault(bn, {})['rotation'] = S.ch(ks)
        b['sword'] = {'rotation': S.hold(R0, T)}
        for bn, ks in cl[name].items(): b.setdefault(bn, {})['rotation'] = S.ch([(round(t, 4), [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, chn in a['extra'].items():
            for k_, v in chn.items(): b.setdefault(bn, {})[k_] = v
        b.setdefault('tabard', {'rotation': S.loop(T, 4, 0, (2, 0, 0), 0.4)})
        b.setdefault('heart', {'scale': S.loop(T, 0.06, 0, (1, 1, 1)) if False else S.hold([1, 1, 1], T)})
        out[name] = S.A(T, b, a['loop'])
    return out

FREEZE = {'idle_combat': ('idle_combat', 0.0), 'draw': ('draw', 0.75), 'attack_windup': ('attack', 0.5), 'attack': ('attack', 0.8),
          'slam': ('slam', 0.9), 'lunge': ('lunge', 0.7), 'roar': ('roar', 1.2)}

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

"""1.2 final boss, art pass v3: Morvane, the Hollow King.
Sculpted plate (octagonal limbs, bevelled overlapping lames, domed pauldrons), a bascinet-style great helm with a
burning visor slit, swept horns and a crest, the hollow chest with its heart-ember, a true two-handed Kingsbane
(both fists solved onto the grip every 1/20 s by king_ik), a split cape driven by a verlet cloth bake, and a weighted
dedicated animation set. Crownfall drives the blade into the floor in front of him. Run: python3 rig_king.py"""
import math, sys
import numpy as np
import gen_geo_assets as G
import rigs_saga2 as S
import rigs_generals as RG
import king_ik as K
Rig = G.Rig
R0 = [0, 0, 0]
HY = 30.8                                # hand pivot height
GRIP_R, GRIP_L = HY - 1.8, HY - 6.2      # staff-local Y of the right / left fist centres on the grip
SWORD_X, SWORD_Z = -11.6, -0.4

# ------------------------------------------------------------------ sculpting helpers
def octo(r, bone, c, size, role, ry=0.0, rz=0.0, pivot=None, k=0.82, bev=0.22):
    """Rounded prism along Y: a bevelled cross plus a 45-degree diamond; reads as a 16-gon at mob scale."""
    w, h, d = size; b = min(w, d) * bev; rot = (0, ry, rz); rr = rot if any(rot) else None
    r.box(bone, c, (w, h, d - 2 * b), role, rot=rr, pivot=pivot)
    r.box(bone, c, (w - 2 * b, h, d), role, rot=rr, pivot=pivot)
    m = min(w, d) * k
    r.box(bone, c, (m * (w / min(w, d)) ** 0.25, h - 0.02, m * (d / min(w, d)) ** 0.25), role, rot=(0, ry + 45, rz), pivot=pivot)

def otaper(r, bone, x0, z0, y0, y1, w0, w1, d0, d1, n, role):
    h = (y1 - y0) / n
    for i in range(n):
        t = (i + 0.5) / n
        octo(r, bone, (x0, y0 + h * (i + 0.5), z0), (w0 + (w1 - w0) * t, abs(h) + 0.3, d0 + (d1 - d0) * t), role)

def plate(r, bone, c, size, role, rot=None, pivot=None, bev=0.5, raise_=0.35):
    """Bevelled slab: chamfered rim and a raised centre panel (front = -Z face)."""
    w, h, d = size; b = min(bev, w / 4, h / 4)
    r.box(bone, c, (w, h - 2 * b, d), role, rot=rot, pivot=pivot or c)
    r.box(bone, c, (w - 2 * b, h, d), role, rot=rot, pivot=pivot or c)
    if raise_: r.box(bone, (c[0], c[1], c[2] - raise_ / 2), (w - 2 * b, h - 2 * b, d + raise_), role, rot=rot, pivot=pivot or c)

def horn(r, bone, base, s, n=9):
    """Swept horn: axis-aligned overlapping links stepping out, up, then curling back (no cube rotation, so no
    renderer sign ambiguity)."""
    p = np.array(base, float); ang = 10.0
    for i in range(n):
        L = 1.5; w = 2.0 - i * 0.16
        ang += 16 if i < 5 else 6
        d = np.array([s * (0.9 if i < 4 else 0.25), math.cos(math.radians(ang)) * L, math.sin(math.radians(ang)) * L])
        p = p + d
        r.box(bone, tuple(p), (w, w + 0.4, w), 'horn' if i < n - 1 else 'crown')

def rig():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 3.8
        lg = r.bone(f'{side}_leg', 'root', (x, 30, 0))
        otaper(r, lg, x, 0, 30, 16.4, 5.4, 4.6, 5.6, 4.8, 3, 'plate')
        for j in range(3):   # overlapping tassets, each lame tucked under the one above
            plate(r, lg, (x + s * 0.9, 28.4 - j * 2.5, -0.3 - j * 0.15), (6.0 - j * 0.3, 3.0, 6.6 - j * 0.3), 'breastplate', rot=(0, 0, s * (10 + j * 3)), pivot=(x, 30, 0), bev=0.5, raise_=0.25)
        octo(r, lg, (x, 16.4, -1.2), (4.6, 3.6, 4.2), 'breastplate')                       # domed poleyn
        B(lg, (x, 16.4, -3.5), (2.2, 2.2, 0.6), 'crown', rot=(0, 0, 45)); B(lg, (x, 16.4, -3.85), (1.0, 1.0, 0.3), 'core', rot=(0, 0, 45))
        plate(r, lg, (x + s * 2.6, 16.4, -0.8), (0.9, 3.2, 3.6), 'breastplate', bev=0.4, raise_=0.2)
        sh = r.bone(f'{side}_shin', lg, (x, 15, 0))
        otaper(r, sh, x, 0, 15, 3.0, 4.6, 3.8, 4.8, 4.0, 3, 'plate')
        plate(r, sh, (x, 9.4, -2.2), (3.8, 9.6, 1.0), 'breastplate', bev=0.6, raise_=0.4)   # greave shell
        B(sh, (x, 9.4, -2.95), (0.5, 8.6, 0.3), 'crown')
        for j in range(4):   # sabaton lames stepping forward
            octo(r, sh, (x, 2.2 - j * 0.45, -1.0 - j * 1.4), (4.8 - j * 0.35, 1.4, 4.6 - j * 0.6), 'gauntlet' if j % 2 else 'breastplate')
        B(sh, (x, 0.5, -5.9), (2.4, 1.0, 1.2), 'breastplate')
    r.bone('hips', 'root', (0, 30, 0))
    octo(r, 'hips', (0, 30.4, 0), (12.4, 3.2, 8.8), 'belt')
    B('hips', (0, 30.4, -4.7), (3.8, 3.8, 0.9), 'crown', rot=(0, 0, 45)); B('hips', (0, 30.4, -5.2), (2.0, 2.0, 0.3), 'core', rot=(0, 0, 45))
    for s in (-1, 1): plate(r, 'hips', (s * 6.3, 29.4, 0), (1.2, 4.0, 7.4), 'breastplate', rot=(0, 0, s * 7), bev=0.4, raise_=0.2)
    r.bone('robe_back', 'hips', (0, 30, 4.4))
    B('robe_back', (0, 23.0, 4.9), (10.8, 14.0, 0.8), 'robe', rot=(8, 0, 0), pivot=(0, 30, 4.4))
    for s in (-1, 1): B('robe_back', (s * 4.8, 23.0, 5.05), (1.2, 13.6, 0.9), 'crown', rot=(8, 0, 0), pivot=(0, 30, 4.4))
    r.bone('robe_back2', 'robe_back', (0, 16.2, 6.4))
    B('robe_back2', (0, 9.8, 6.7), (11.4, 12.8, 0.8), 'capehem', rot=(10, 0, 0), pivot=(0, 16.2, 6.4))
    r.bone('tabard', 'hips', (0, 29, -4.6))
    B('tabard', (0, 19.8, -4.9), (5.4, 18.4, 0.6), 'robe'); B('tabard', (0, 19.8, -5.25), (1.0, 17.4, 0.2), 'crown')
    B('tabard', (0, 23.0, -5.4), (3.2, 3.2, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 9.6, -4.9), (5.0, 2.6, 0.6), 'capehem')
    r.bone('orb', 'hips', (9.4, 26.4, 1.2))
    octo(r, 'orb', (9.4, 26.4, 1.2), (3.8, 3.8, 3.8), 'orb', k=0.9); B('orb', (9.4, 26.4, 1.2), (2.2, 2.2, 2.2), 'core', rot=(45, 45, 0))
    B('orb', (9.4, 26.4, 1.2), (4.8, 0.5, 4.8), 'crown', rot=(0, 45, 0)); B('orb', (9.4, 29.0, 1.2), (0.8, 1.6, 0.8), 'crown'); B('orb', (9.4, 29.4, 1.2), (2.2, 0.6, 0.6), 'crown')
    r.bone('torso', 'hips', (0, 31.5, 0))
    otaper(r, 'torso', 0, 0, 31.5, 41, 10.4, 12.4, 7.6, 8.8, 2, 'plate')
    for j in range(3):   # waist lames overlapping upward
        octo(r, 'torso', (0, 33.0 + j * 2.9, -0.15 * j), (10.8 + j * 0.8, 2.4, 8.2 + j * 0.4), 'breastplate', k=0.86)
        B('torso', (0, 34.0 + j * 2.9, -4.25 - j * 0.3), (6.0 + j, 0.4, 0.3), 'crown')
    r.bone('chest', 'torso', (0, 41, 0))
    otaper(r, 'chest', 0, 2.0, 41, 54, 13.8, 19.4, 6.4, 7.6, 3, 'plate')
    octo(r, 'chest', (0, 48.5, 3.6), (16.0, 10.0, 3.4), 'breastplate', k=0.9)            # backplate swell
    B('chest', (0, 47.5, -1.3), (12.6, 11.6, 1.2), 'void')
    for s in (-1, 1):   # split breastplate halves, each three angled panels for a curved read
        for j, (dy, yaw) in enumerate(((0, -10), (3.6, -22), (-3.6, -4))):
            plate(r, 'chest', (s * (6.6 + 0.5 * j), 47.6 + dy, -2.7 - (0.2 if j == 0 else 0)), (5.6, 4.6 if j else 5.0, 3.4), 'breastplate',
                  rot=(0, s * yaw, s * 3), pivot=(s * 9.4, 47.6, -2.6), bev=0.5, raise_=0.3)
        B('chest', (s * 3.85, 47.6, -4.6), (0.6, 12.4, 0.6), 'crown')
        B('chest', (s * 7.4, 53.2, -3.8), (5.4, 0.6, 0.6), 'crown', rot=(0, 0, s * -10))
        for j in range(5): B('chest', (s * 2.4, 52.4 - j * 2.3, -3.4), (3.8 - j * 0.25, 0.9, 0.9), 'rib', rot=(0, s * 14, s * -10))
    B('chest', (0, 47.6, -3.6), (0.9, 11.0, 0.9), 'rib')
    r.bone('heart', 'chest', (0, 47, -1.6))
    B('heart', (0, 47, -1.8), (3.4, 3.8, 2.6), 'core', rot=(0, 45, 0)); B('heart', (0, 47, -1.8), (4.8, 5.0, 0.4), 'rune', rot=(0, 0, 45))
    B('heart', (0, 47, -1.8), (2.0, 6.6, 0.3), 'rune'); B('heart', (0, 47, -1.8), (6.6, 2.0, 0.3), 'rune')
    for j in range(2): octo(r, 'chest', (0, 54.6 + j * 1.3, 0.2), (10.8 - j * 1.6, 1.6, 9.2 - j * 1.4), 'guard' if j == 0 else 'breastplate')
    octo(r, 'chest', (0, 55.4, 0.6), (15.2, 2.6, 11.6), 'fur', k=0.9)          # ermine mantle: a rounded collar
    octo(r, 'chest', (0, 57.0, 1.4), (11.6, 1.6, 9.6), 'fur', k=0.9)
    B('chest', (0, 56.2, 5.6), (14.0, 3.2, 2.4), 'fur')
    # head: great helm, snouted visor with a burning slit, breaths, cheek plates, swept horns, crest
    r.bone('head', 'chest', (0, 55.4, -0.8))
    octo(r, 'head', (0, 60.0, -0.4), (7.8, 8.4, 8.4), 'helm', k=0.86)
    octo(r, 'head', (0, 64.6, -0.4), (6.8, 1.2, 7.4), 'helm', k=0.86); octo(r, 'head', (0, 65.5, -0.4), (5.2, 0.8, 5.8), 'helm', k=0.86)
    for s in (-1, 1):
        plate(r, 'head', (s * 1.85, 59.2, -4.7), (3.9, 6.6, 1.0), 'visorplate', rot=(0, s * -24, 0), pivot=(0, 59.2, -5.4), bev=0.35, raise_=0.2)
        B('head', (s * 1.6, 61.0, -5.5), (2.6, 0.8, 0.3), 'void', rot=(0, s * -24, 0), pivot=(0, 61.0, -5.9))
        B('head', (s * 1.55, 61.0, -5.65), (2.0, 0.45, 0.2), 'eye', rot=(0, s * -24, 0), pivot=(0, 61.0, -5.9))
        for j in range(3): B('head', (s * 1.4, 57.4 - j * 0.9, -5.55), (0.5, 0.5, 0.3), 'void', rot=(0, s * -24, 0), pivot=(0, 57.4, -5.9))
        plate(r, 'head', (s * 4.1, 59.2, -0.6), (0.8, 6.4, 6.6), 'breastplate', bev=0.4, raise_=0.2)
        horn(r, 'head', (s * 3.9, 63.4, 0.4), s)
    B('head', (0, 59.6, -6.15), (0.6, 7.4, 0.6), 'crown')
    for j in range(5): B('head', (0, 66.2 + (0.6 if 0 < j < 4 else 0), -3.0 + j * 1.6), (0.6, 1.4 if 0 < j < 4 else 0.8, 1.7), 'crown')
    B('head', (0, 64.9, -0.4), (8.4, 0.8, 9.0), 'crown')
    r.bone('mitre0', 'head', (0, 69.0, -0.4))
    for k in range(10):
        a = 2 * math.pi * k / 10; px, pz = math.sin(a) * 4.6, -0.4 + math.cos(a) * 4.6
        B('mitre0', (px, 69.0, pz), (3.2, 1.8, 0.9) if abs(math.cos(a)) > 0.6 else (0.9, 1.8, 3.2), 'crown', rot=(0, math.degrees(a), 0))
        ln = 4.6 if k % 2 == 0 else 2.6
        B('mitre0', (px, 69.9 + ln / 2, pz), (1.0, ln, 1.0), 'crown', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 69.9, pz))
        B('mitre0', (px, 69.9 + ln + 0.5, pz), (1.3 if k % 2 == 0 else 1.0,) * 3, 'core', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 69.9, pz))
    r.bone('mitre1', 'mitre0', (0, 69.0, -0.4))
    B('mitre1', (0, 72.0, -0.4), (1.8, 1.8, 1.8), 'core', rot=(45, 45, 0))
    # arms: domed lamellar pauldrons, couters, flared gauntlets
    for side, s in (('right', -1), ('left', 1)):
        x = s * 11.6
        arm = r.bone(f'{side}_arm', 'chest', (x, 52.5, 0))
        for j in range(5):
            octo(r, arm, (x + s * 0.55 * j, 55.2 - j * 1.7, 0.2), (10.6 - j * 0.9, 2.2, 10.4 - j * 0.7), 'pauldron', rz=s * (8 + j * 7), k=0.88)
            B(arm, (x + s * (0.55 * j + 0.2), 54.9 - j * 1.7, -5.2 + j * 0.35), (7.0 - j * 0.8, 0.45, 0.3), 'crown', rot=(0, 0, s * (8 + j * 7)))
        octo(r, arm, (x + s * 0.4, 56.6, 0.2), (7.6, 1.4, 7.6), 'pauldron', rz=s * 6)
        for j in range(2): B(arm, (x + s * (2.6 + j * 2.0), 58.6 + j * 0.3, 0.2), (1.1, 4.4 - j * 1.4, 1.1), 'crown', rot=(0, 0, s * -22))
        otaper(r, arm, x, 0, 50, 42, 4.8, 4.6, 4.8, 4.6, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 42, 0))
        octo(r, fa, (x, 41.8, 0.6), (5.2, 3.2, 4.6), 'breastplate')
        plate(r, fa, (x + s * 2.7, 41.8, 0.6), (0.8, 3.0, 3.4), 'breastplate', bev=0.3, raise_=0.2)
        otaper(r, fa, x, 0, 41, HY + 2.6, 4.4, 5.0, 4.4, 5.0, 2, 'plate')
        octo(r, fa, (x, HY + 2.4, 0), (5.4, 1.4, 5.0), 'crown', k=0.9)
        B(fa, (x, HY + 4.6, -2.85), (2.4, 4.0, 0.3), 'rune')
        hd = r.bone(f'{side}_hand', fa, (x, HY, -0.4)); fy = HY - 1.8   # the grip runs fore-aft (local Z) through the fist
        octo(r, hd, (x, fy + 0.1, -0.4), (3.6, 3.4, 3.4), 'gauntlet', k=0.92, bev=0.18)
        for f in range(4): B(hd, (x - s * 0.2, fy - 1.55, -1.75 + f * 0.9), (3.4, 0.9, 0.78), 'breastplate' if f % 2 else 'gauntlet')   # curled fingers under the grip
        B(hd, (x - s * 1.95, fy + 0.4, -1.6), (0.9, 2.2, 1.4), 'gauntlet')                              # thumb over the grip
        B(hd, (x + s * 1.75, fy + 0.2, -0.4), (0.5, 2.6, 3.0), 'crown')                                 # knuckle plate
        B(hd, (x, fy + 1.9, -0.4), (3.2, 0.6, 3.0), 'breastplate')
    # Kingsbane: two-handed royal greatsword. Coordinates run from the right fist (f): the guard sits just above it,
    # the left fist takes the grip below, the pommel caps it.
    x, z, f = SWORD_X, SWORD_Z, GRIP_R
    r.bone('staff', 'right_hand', (x, f, z), (-90, 0, 0))   # the blade leaves the fist perpendicular to the forearm
    B('staff', (x, f - 9.2, z), (2.4, 2.4, 2.4), 'core', rot=(45, 0, 45)); octo(r, 'staff', (x, f - 9.2, z), (3.2, 1.0, 3.2), 'crown')
    otaper(r, 'staff', x, z, f - 8.2, f + 2.6, 1.5, 1.6, 1.5, 1.6, 2, 'grip')
    octo(r, 'staff', (x, f - 8.0, z), (2.0, 0.6, 2.0), 'crown')
    plate(r, 'staff', (x, f + 3.4, z), (2.6, 2.0, 14.0), 'crown', bev=0.4, raise_=0.0)
    for sz_ in (-1, 1):
        B('staff', (x, f + 5.2, z + sz_ * 7.4), (2.0, 4.2, 1.6), 'crown', rot=(sz_ * 26, 0, 0))
        B('staff', (x, f + 7.4, z + sz_ * 8.2), (1.2, 1.2, 1.2), 'core', rot=(45, 45, 0))
    octo(r, 'staff', (x, f + 5.2, z), (3.2, 3.2, 5.0), 'crown'); B('staff', (x, f + 5.2, z - 2.75), (1.6, 1.6, 0.4), 'core', rot=(0, 0, 45))
    S.taper(r, 'staff', x, z, f + 6.4, f + 40.8, 1.4, 1.0, 6.8, 5.0, 5, 'greatblade')
    S.taper(r, 'staff', x, z, f + 6.4, f + 40.8, 0.5, 0.4, 7.6, 5.8, 5, 'edge')
    B('staff', (x, f + 44.2, z), (1.0, 7, 3.4), 'greatblade'); B('staff', (x, f + 48.4, z), (0.8, 2.6, 1.4), 'greatblade')
    B('staff', (x, f + 44.2, z), (0.4, 7.4, 4.0), 'edge'); B('staff', (x, f + 48.4, z), (0.35, 3.0, 2.0), 'edge')
    B('staff', (x, f + 23.8, z), (1.55, 34, 1.2), 'rune')
    for y in (10.8, 18.8, 26.8, 34.8): B('staff', (x, f + y, z), (1.6, 0.6, 2.6), 'crown')
    r.bone('censer', 'staff', (x, f + 3.4, z - 7.0))
    cz = z - 7.0
    for j in range(3): B('censer', (x, f + 2.0 - j * 1.9, cz), (0.6, 1.6, 0.6), 'guard', rot=(0, 45 * j, 0))
    B('censer', (x, f - 3.6, cz), (3.6, 3.6, 0.8), 'crown'); B('censer', (x, f - 4.05, cz - 0.45), (2.2, 2.2, 0.2), 'core', rot=(0, 0, 45))
    r.bone('sigil_hand', 'left_forearm', (15.0, HY + 5, 0))   # crown rune that flares off the left vambrace when he casts
    B('sigil_hand', (15.2, HY + 5, 0), (0.3, 3.0, 3.0), 'rune', rot=(45, 0, 0)); B('sigil_hand', (15.3, HY + 5, 0), (0.3, 1.4, 1.4), 'core', rot=(45, 0, 0))
    r.bone('sigils', 'chest', (0, 54, 12))
    for k in range(3):
        a = 2 * math.pi * k / 3; px, py = math.sin(a) * 11, 54 + math.cos(a) * 11
        B('sigils', (px, py, 13.2), (2.0, 6.0, 1.0), 'crown', rot=(0, 0, math.degrees(-a))); B('sigils', (px, py, 12.9), (1.6, 1.6, 1.6), 'core', rot=(45, 45, 0))
    # cape: two overlapping halves x 6 segments, cloth-baked
    for half, s in (('R', -1), ('L', 1)):
        prev, y = 'chest', 55.0
        for j, (ln, w) in enumerate(CAPE):
            bn = r.bone(f'cape{half}{j + 1}', prev, (s * 0.2, y, 6.4))
            B(bn, (s * (w / 2 - 0.7), y - ln / 2, 6.6 + 0.02 * (s + 1)), (w, ln + 0.3, 0.8), 'cape' if j < 4 else 'capehem')
            B(bn, (s * (w - 0.9), y - ln / 2, 6.85), (1.4, ln + 0.3, 1.0), 'fur')
            if j == 1: B(bn, (s * 2.4, y - ln / 2, 7.05), (3.6, 3.6, 0.3), 'sigil', rot=(0, 0, 45))
            if j == 0: B(bn, (s * (w / 2 - 0.7), y - 0.6, 6.95), (w, 1.6, 1.4), 'fur')
            prev, y = bn, y - ln
    return r
CAPE = ((9, 11.0), (9, 11.6), (9, 12.2), (9, 12.8), (8.5, 13.4), (8, 14.0))

PAL = {'hollow_king': dict(who='hollow_king', k=3, ts=[768, 768], boss=True,
       pal=dict(base='#3A3A44', dark='#0E0C12', accent='#6E6A78', robe='#5E0E1E', cape='#4A0A18'),
       trim='#D8B04A', glow='#FFD06A', eye='#FFF6D8', core='#FFE29A', orb='#6A1A2A', crystal='#F0E0B0', eyes='visor')}

class Painter(RG.Painter):
    def style(s, F, role):
        k = s.k
        def each(fb):
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); ed = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy)
                        fb(fn, xx, yy, u, t, ed, fw, fh)
        if role == 'fur':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((196, 190, 180), (248, 244, 236), s.n(xx, yy, max(1, k // 2)))
                c = G.ramp(c, s.shade(fn, t, u) * (0.88 + 0.22 * s.n(xx, yy, 1)))
                if (yy + (xx // max(1, k)) % 3) % max(2, k) == 0: c = G.mul(c, 0.9)
                gx, gy = (xx - F[fn][0]) % (5 * k), (yy - F[fn][1]) % (6 * k)
                if fw > 3 * k and abs(gx - 2.5 * k) < 0.6 * k and abs(gy - 3 * k) < 1.2 * k: c = (24, 20, 20)
                s.px(xx, yy, c)
            each(f); return True
        if role in ('plate', 'breastplate', 'pauldron', 'helm', 'gauntlet', 'visorplate'):
            e = max(1, k // 2)
            base = {'plate': (52, 52, 62), 'breastplate': (68, 66, 78), 'pauldron': (64, 62, 74), 'helm': (60, 58, 70), 'gauntlet': (46, 46, 54), 'visorplate': (72, 70, 82)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                spec = math.exp(-((t - 0.28) / 0.16) ** 2) * (0.35 if fn != 'down' else 0.0)
                kk = s.shade(fn, t, u) * (1.04 - 0.26 * t ** 1.4 if fn not in ('up', 'down') else 1.0) * (0.92 + 0.16 * math.sin(u * math.pi))
                c = G.ramp(G.mix(G.mul(base, 0.72), G.mul(base, 1.28), s.n(xx, yy, 2 * k)), kk)
                c = G.mix(c, (190, 192, 214), spec * 0.55)
                if (xx * 7 + yy * 3) % 29 == 0: c = G.mul(c, 1.22)
                big = fw > 4 * k and fh > 4 * k
                if ed < e: c = G.mul(c, 0.42)
                elif ed < 2 * e and fn in ('north', 'up'): c = G.mix(c, (150, 150, 172), 0.35)
                elif role in ('breastplate', 'pauldron', 'visorplate') and big and ed < 3 * e: c = G.ramp((214, 170, 78), kk)
                elif big and role in ('breastplate', 'pauldron', 'helm'):
                    lx, ly = (xx - F[fn][0]) / k, (yy - F[fn][1]) / k
                    w = math.sin(lx * 1.3 + math.sin(ly * 0.9) * 1.6) * math.cos(ly * 1.1)
                    if abs(w) < 0.06 and ed > 3 * e:
                        if role == 'breastplate' and fn == 'north' and ((xx // (3 * k)) + (yy // (3 * k))) % 3 == 0:
                            c = (255, 196, 96); s.gl(xx, yy, s.gc, 0.85)        # emissive rune-work
                        else: c = G.ramp((190, 150, 66), kk * 0.95)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'horn':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.ramp(G.mix((28, 22, 26), (92, 74, 70), 0.3 + 0.5 * s.n(xx, yy, k)), s.shade(fn, t, u))
                if (yy // max(1, k)) % 3 == 0: c = G.mul(c, 0.7)
                if ed < 1: c = G.mul(c, 0.6)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'eye':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((255, 236, 170), (255, 255, 246), 1 - abs(t - 0.5) * 2); s.px(xx, yy, c); s.gl(xx, yy, (255, 240, 200), 1.0)
            each(f); return True
        if role == 'edge':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                s.px(xx, yy, G.ramp(G.mix((176, 182, 200), (236, 240, 250), 0.4 + 0.4 * s.n(xx, yy, k)), s.shade(fn, t, u)))
            each(f); return True
        if role == 'rib':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.ramp(G.mix((176, 166, 146), (226, 218, 198), s.n(xx, yy, 1)), s.shade(fn, t, u))
                if ed < 1: c = G.mul(c, 0.6)
                s.px(xx, yy, c); s.gl(xx, yy, s.gc, 0.12)
            each(f); return True
        if role in ('crown', 'core'):
            e = max(1, k // 2)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                if role == 'core':
                    c = G.mix((255, 206, 110), (255, 250, 226), 0.5 + 0.5 * (1 - abs(u - 0.5) * 2) * (1 - abs(t - 0.5) * 2))
                    s.px(xx, yy, c); s.gl(xx, yy, s.gc, 0.95); return
                c = G.ramp(G.mix((146, 104, 38), (238, 196, 98), s.n(xx, yy, k)), kk)
                c = G.mix(c, (255, 236, 170), math.exp(-((t - 0.3) / 0.18) ** 2) * 0.4)
                if ed < e: c = G.mul(c, 0.55)
                elif s.n(xx * 3, yy * 3, 1) > 0.84: c = G.mix(c, (70, 60, 40), 0.5)
                s.px(xx, yy, c)
            each(f); return True
        if role in ('robe', 'cape', 'capehem'):
            base = {'robe': (112, 22, 36), 'cape': (94, 16, 30), 'capehem': (94, 16, 30)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                folds = max(1, fw // (3 * k))
                fold = 0.5 + 0.5 * math.sin(u * math.pi * 2 * folds + t * 0.8) if fn not in ('up', 'down') else 0.5
                kk = s.shade(fn, t, u) * (0.68 + 0.48 * fold)
                c = G.ramp(G.mix(G.mul(base, 0.62), G.mul(base, 1.32), s.n(xx, yy, 2 * k)), kk)
                if fold > 0.92: c = G.mix(c, (176, 70, 86), 0.18)
                a = 255
                if role == 'capehem' and fn not in ('up', 'down') and t > 0.45:
                    if s.n(xx, yy, k) > 0.28 + 1.1 * (1 - t): a = 0
                    elif t > 0.78: c = G.mul(c, 0.6)
                if role == 'robe' and fn in ('north', 'south') and 0.86 < t < 0.95:
                    c = G.mix(c, (216, 176, 74), 0.75)
                    if (xx // k) % 3 == 0: c = G.mul(c, 0.7)
                s.px(xx, yy, c, a)
            each(f); return True
        return super().style(F, role)

# ================================================================== animation authoring
# Sword poses are authored as (grip point, blade direction, flat normal) in CHEST space; the baker solves both arm
# chains onto the grip every 1/20 s, so the fists never leave the hilt.
def nrm(v): v = np.array(v, float); return v / np.linalg.norm(v)
def SW(P, D, N=(1, 0, 0)): return dict(P=P, D=D, N=N)
# Targets are in ROOT space (feet on y=0, front = -Z, his right = -X). Blade: right fist -> tip 53 px, pommel 11 px.
GUARD = SW((-3.0, 39.0, -14.0), (0.1, 0.9, -0.42))         # high guard: point up before his face, pommel clear of the belt
REST = SW((0.0, 42.0, -15.0), (0.0, -0.95, -0.3))         # at rest: both hands stacked on the hilt, point set in the floor
CARRY = SW((-12.5, 30.0, -8.0), (-0.1, -0.55, -0.83))     # walking: carried low in the right hand, point forward
PLANT = SW((-1.0, 44.0, -16.0), (0.0, -0.9, -0.43))        # point planted in the floor before him
REST_L = {'left_arm': [(0, [-8, 0, 12])], 'left_forearm': [(0, [-34, 0, 0])], 'left_hand': [(0, [0, 0, 0])]}   # hand over the orb

def stride(T, amp=22, knee=24, bob=1.2, n=16):
    out = {}
    for side, ph in (('right', 0.0), ('left', math.pi)):
        out[f'{side}_leg'] = [(T * i / n, [amp * math.sin(2 * math.pi * i / n + ph), 0, 0]) for i in range(n + 1)]
        out[f'{side}_shin'] = [(T * i / n, [max(0, knee * math.sin(2 * math.pi * i / n + ph + 1.3)), 0, 0]) for i in range(n + 1)]
    out['root_pos'] = [(T * i / n, [0, -bob * abs(math.sin(2 * math.pi * i / n)) + bob * 0.5, 0]) for i in range(n + 1)]
    out['hips'] = [(T * i / n, [0, 6 * math.sin(2 * math.pi * i / n), 0]) for i in range(n + 1)]
    out['sway'] = [(T * i / n, [0, -5 * math.sin(2 * math.pi * i / n), 0]) for i in range(n + 1)]
    return out

def interp(keys, t, loop_T=None):
    """Catmull-Rom over [(t, vec)] (matches clipcheck.sample)."""
    ks = keys
    if loop_T: t = t % loop_T if t < loop_T else loop_T
    if t <= ks[0][0]: return np.array(ks[0][1], float)
    if t >= ks[-1][0]: return np.array(ks[-1][1], float)
    for i in range(len(ks) - 1):
        a, b = ks[i], ks[i + 1]
        if a[0] <= t <= b[0]:
            f = (t - a[0]) / (b[0] - a[0]) if b[0] > a[0] else 0
            p0 = np.array(ks[max(0, i - 1)][1], float); p1 = np.array(a[1], float); p2 = np.array(b[1], float); p3 = np.array(ks[min(len(ks) - 1, i + 2)][1], float)
            return 0.5 * (2 * p1 + (-p0 + p2) * f + (2 * p0 - 5 * p1 + 4 * p2 - p3) * f * f + (-p0 + 3 * p1 - 3 * p2 + p3) * f ** 3)
    return np.array(ks[-1][1], float)

def sword_keys(pairs): return [(t, list(p['P']) + list(nrm(p['D'])) + list(nrm(p['N']))) for t, p in pairs]
def mv(sw, dP=(0, 0, 0), D=None): return SW(tuple(np.add(sw['P'], dP)), D or sw['D'], sw['N'])

def author():
    A = {}
    def base(T, lp, sword, body=None, pos=None, extra=None, free=None, lf=None):
        return dict(T=T, loop=lp, sword=sword_keys(sword), body=body or {}, pos=pos or {}, extra=extra or {}, free=free or [], lf=lf or {})
    def restl(T, sway=0.0):
        return {bn: [(0, v[0][1]), (T / 2, list(np.add(v[0][1], [sway, 0, 0]))), (T, v[0][1])] for bn, v in REST_L.items()}
    T = 4.0   # IDLE: leaning on the planted Kingsbane, both hands on the hilt, slow heavy breath
    A['idle'] = base(T, True, [(0, REST), (T / 2, mv(REST, (0, -0.5, 0))), (T, REST)],
        body={'chest': [(0, [4, 0, 0]), (T / 2, [2, 0, 0]), (T, [4, 0, 0])], 'head': [(0, [-6, 0, 0]), (T / 2, [-4, 6, 0]), (T, [-6, 0, 0])],
              'torso': [(0, [2, 0, 0]), (T, [2, 0, 0])],
              'right_leg': [(0, [-4, 0, 3]), (T, [-4, 0, 3])], 'left_leg': [(0, [6, 0, -3]), (T, [6, 0, -3])],
              'right_shin': [(0, [6, 0, 0]), (T, [6, 0, 0])], 'left_shin': [(0, [3, 0, 0]), (T, [3, 0, 0])]},
        pos={'root': [(0, [0, -0.3, 0]), (T / 2, [0, -0.8, 0]), (T, [0, -0.3, 0])]})
    T = 1.8; st = stride(T, 20, 22, 1.4)   # WALK: heavy stride, blade carried low and forward, left arm swinging
    A['walk'] = base(T, True, [(T * i / 4, mv(CARRY, (0, 0.8 if i % 2 else 0, 0))) for i in range(5)],
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'torso': [(t, [3, v[1] * 0.4, 0]) for t, v in st['sway']], 'chest': [(t, [5, v[1], 0]) for t, v in st['sway']],
              'head': [(0, [-6, 0, 0]), (T, [-6, 0, 0])]},
        pos={'root': st['root_pos']}, free=[(-1, T + 1)],
        lf={'left_arm': [(T * i / 4, [(-18 if i % 4 == 0 else 14 if i % 4 == 2 else -2), 0, 10]) for i in range(5)], 'left_forearm': [(0, [-30, 0, 0]), (T, [-30, 0, 0])], 'left_hand': [(0, R0), (T, R0)]})
    T = 3.0   # IDLE_COMBAT: two-handed high guard, weight sunk into a staggered stance
    A['idle_combat'] = base(T, True, [(0, GUARD), (T / 2, mv(GUARD, (0, -0.8, 0.3))), (T, GUARD)],
        body={'chest': [(0, [6, 0, 0]), (T / 2, [8, -4, 0]), (T, [6, 0, 0])], 'head': [(0, [-10, 0, 0]), (T / 2, [-8, 4, 0]), (T, [-10, 0, 0])],
              'torso': [(0, [4, 6, 0]), (T, [4, 6, 0])], 'hips': [(0, [0, -6, 0]), (T, [0, -6, 0])],
              'right_leg': [(0, [12, 6, 4]), (T, [12, 6, 4])], 'left_leg': [(0, [-22, 6, -4]), (T, [-22, 6, -4])],
              'right_shin': [(0, [18, 0, 0]), (T, [18, 0, 0])], 'left_shin': [(0, [24, 0, 0]), (T, [24, 0, 0])]},
        pos={'root': [(0, [0, -1.6, 0]), (T / 2, [0, -2.2, 0]), (T, [0, -1.6, 0])]})
    IC = A['idle_combat']['body']; g = lambda bn: list(IC[bn][0][1]) if bn in IC else [0, 0, 0]
    T = 1.4; st = stride(T, 18, 20, 1.0)
    A['walk_combat'] = base(T, True, [(T * i / 4, mv(GUARD, (0, -0.6 if i % 2 else 0, 0))) for i in range(5)],
        body={'right_leg': st['right_leg'], 'left_leg': st['left_leg'], 'right_shin': st['right_shin'], 'left_shin': st['left_shin'],
              'hips': st['hips'], 'chest': [(t, [8, v[1], 0]) for t, v in st['sway']], 'head': [(0, [-10, 0, 0]), (T, [-10, 0, 0])], 'torso': [(0, [5, 0, 0]), (T, [5, 0, 0])]},
        pos={'root': st['root_pos']})
    BODY = ['chest', 'torso', 'head', 'hips', 'right_leg', 'left_leg', 'right_shin', 'left_shin']
    def act(T, keys, body, pos=None, end=True, **kw):
        """Action anim: every body bone starts (and ends) at the combat guard so blends are seamless."""
        bb = {}
        for bn in BODY:
            bb[bn] = [(0, g(bn))] + body.get(bn, []) + ([(T, g(bn))] if end else [])
            if len(bb[bn]) == 1: bb[bn].append((T, g(bn)))
        pr = (pos or {}).get('root', [])
        pp = {'root': [(0, [0, -1.6, 0])] + pr + ([(T, [0, -1.6, 0])] if end or not pr else [])}
        return base(T, False, [(0, GUARD)] + keys + ([(T, GUARD)] if end else []), bb, pp, **kw)
    # ATTACK "Sentence": rears back hoisting the blade over his right shoulder, hangs, then hews down (impact 0.7)
    A['attack'] = act(1.45, [
        (0.25, SW((-6.0, 52.0, -10.0), (-0.1, 0.95, 0.2))), (0.48, SW((-5.0, 60.0, -5.0), (-0.05, 0.3, 0.95))),
        (0.6, SW((-5.0, 61.0, -4.0), (-0.05, 0.15, 0.99))), (0.65, SW((-3.0, 54.0, -18.0), (0.0, 0.5, -0.86))), (0.7, SW((-1.0, 34.0, -19.0), (0.0, -0.5, -0.86))),
        (0.95, SW((-1.0, 33.0, -18.5), (0.0, -0.52, -0.85))), (1.15, SW((-2.0, 36.0, -16.0), (0.05, 0.3, -0.95)))],
        {'chest': [(0.48, [-16, 12, 0]), (0.6, [-18, 12, 0]), (0.7, [28, -4, 0]), (0.95, [30, -4, 0])],
         'torso': [(0.48, [-8, 10, 0]), (0.7, [14, 0, 0]), (0.95, [14, 0, 0])],
         'head': [(0.48, [-4, 0, 0]), (0.7, [-12, 0, 0]), (0.95, [-12, 0, 0])],
         'hips': [(0.48, [0, 4, 0]), (0.7, [0, -8, 0]), (0.95, [0, -8, 0])],
         'right_leg': [(0.48, [24, 0, 4]), (0.7, [22, 0, 4]), (0.95, [22, 0, 4])], 'right_shin': [(0.48, [24, 0, 0]), (0.7, [34, 0, 0]), (0.95, [34, 0, 0])],
         'left_leg': [(0.48, [-14, 0, -4]), (0.7, [-46, 0, -4]), (0.95, [-46, 0, -4])], 'left_shin': [(0.48, [10, 0, 0]), (0.7, [52, 0, 0]), (0.95, [52, 0, 0])]},
        {'root': [(0.48, [0, 1.2, 1.0]), (0.6, [0, 1.6, 1.2]), (0.7, [0, -5.0, -3.0]), (0.95, [0, -5.2, -3.0])]})
    # SWEEP "Reaping": blade cocked out past the right hip, then a flat two-handed reap across the front (impact 0.62)
    A['sweep'] = act(1.4, [
        (0.2, SW((-7.0, 40.0, -18.0), (-0.45, 0.45, -0.77), (0, 1, 0))), (0.42, SW((-9.0, 38.0, -8.0), (-0.85, 0.1, 0.5), (0, 1, 0))), (0.5, SW((-9.5, 38.0, -7.0), (-0.8, 0.1, 0.58), (0, 1, 0))),
        (0.62, SW((0.0, 37.0, -17.0), (0.2, 0.0, -0.98), (0, 1, 0))), (0.78, SW((8.0, 37.0, -12.0), (0.9, 0.0, -0.44), (0, 1, 0))),
        (1.0, SW((7.0, 37.0, -12.0), (0.88, 0.05, -0.46), (0, 1, 0)))],
        {'chest': [(0.42, [4, 30, 0]), (0.5, [4, 32, 0]), (0.62, [8, -8, 0]), (0.78, [8, -34, 0]), (1.0, [8, -32, 0])],
         'torso': [(0.42, [4, 16, 0]), (0.62, [6, -4, 0]), (0.78, [6, -18, 0]), (1.0, [6, -16, 0])],
         'head': [(0.42, [-10, -26, 0]), (0.62, [-10, 6, 0]), (0.78, [-10, 26, 0]), (1.0, [-10, 24, 0])],
         'left_leg': [(0.62, [-30, 0, -6]), (1.0, [-30, 0, -6])], 'left_shin': [(0.62, [36, 0, 0]), (1.0, [36, 0, 0])]},
        {'root': [(0.42, [0, -1.0, 0]), (0.62, [0, -3.4, -1.5]), (1.0, [0, -3.2, -1.5])]})
    # SPIN "Wheel of Ash": blade out flat to his right, one full heavy turn (impact 0.7)
    SPN = SW((-9.0, 38.0, -10.0), (-0.9, 0.05, -0.42), (0, 1, 0))
    A['spin'] = act(1.5, [(0.38, SPN), (1.1, SPN)],
        {'chest': [(0.38, [4, 14, 0]), (1.1, [4, 14, 0])], 'head': [(0.38, [-10, -10, 0]), (1.1, [-10, -10, 0])],
         'right_leg': [(0.38, [-10, 0, 8]), (1.1, [-10, 0, 8])], 'left_leg': [(0.38, [-10, 0, -8]), (1.1, [-10, 0, -8])],
         'right_shin': [(0.38, [20, 0, 0]), (1.1, [20, 0, 0])], 'left_shin': [(0.38, [20, 0, 0]), (1.1, [20, 0, 0])]},
        {'root': [(0.38, [0, -3.0, 0]), (1.1, [0, -3.0, 0])]},
        extra={'root': {'rotation': S.lin([(0, R0), (0.4, [0, 30, 0]), (0.75, [0, -200, 0]), (1.05, [0, -360, 0]), (1.5, [0, -360, 0])])}})
    # VOLLEY: plants the blade, the left hand flings three crown shards (impact 0.6)
    A['volley'] = act(1.2, [(0.3, PLANT), (0.9, PLANT)],
        {'chest': [(0.35, [-6, 20, 0]), (0.6, [10, -18, 0]), (0.85, [10, -16, 0])], 'head': [(0.35, [-12, -16, 0]), (0.6, [-14, 12, 0])],
         'left_leg': [(0.6, [-20, 0, -4]), (0.9, [-20, 0, -4])], 'left_shin': [(0.6, [24, 0, 0]), (0.9, [24, 0, 0])]},
        free=[(0.12, 1.02)],
        lf={'left_arm': [(0.2, [-30, 0, 45]), (0.35, [-40, 30, 70]), (0.48, [-48, 20, 76]), (0.6, [-96, -10, 34]), (0.85, [-94, -10, 36])],
            'left_forearm': [(0.35, [-90, 0, 0]), (0.6, [-6, 0, 0]), (0.85, [-8, 0, 0])], 'left_hand': [(0.35, R0), (0.6, [-20, 0, 0]), (0.85, [-20, 0, 0])]},
        extra={'sigil_hand': {'scale': S.ch([(0, [0.01] * 3), (0.3, [1.6] * 3), (0.7, [1.6] * 3), (1.2, [0.01] * 3)])}})
    # BARRIER: plants the blade point-down before him and raises the left fist; the sigils swell
    A['barrier'] = act(1.3, [(0.4, PLANT), (0.95, PLANT)],
        {'head': [(0.4, [-24, 0, 0]), (0.95, [-24, 0, 0])], 'chest': [(0.4, [-6, 0, 0]), (0.95, [-6, 0, 0])]},
        free=[(0.15, 1.15)],
        lf={'left_arm': [(0.4, [-168, 0, 22]), (0.95, [-170, 0, 24])], 'left_forearm': [(0.4, [-8, 0, 0]), (0.95, [-8, 0, 0])], 'left_hand': [(0.4, R0), (0.95, R0)]},
        extra={'sigil_hand': {'scale': S.ch([(0, [0.01] * 3), (0.4, [2.0] * 3), (0.95, [2.0] * 3), (1.3, [0.01] * 3)])},
               'sigils': {'scale': S.ch([(0, [1, 1, 1]), (0.4, [1.5, 1.5, 1]), (0.95, [1.5, 1.5, 1]), (1.3, [1, 1, 1])])}})
    # ROAR (phase change): hunches over the planted blade, then rears back with a howl; the sigils flare
    A['roar'] = act(2.1, [(0.5, mv(PLANT, (0, -3, 0))), (0.9, PLANT), (1.7, PLANT)],
        {'chest': [(0.5, [26, 0, 0]), (0.9, [-18, 0, 0]), (1.7, [-16, 0, 0])], 'head': [(0.5, [16, 0, 0]), (0.9, [-36, 0, 0]), (1.7, [-34, 0, 0])],
         'torso': [(0.5, [10, 0, 0]), (0.9, [-6, 0, 0]), (1.7, [-6, 0, 0])]},
        {'root': [(0.5, [0, -3.0, 0]), (0.9, [0, 0.4, 0]), (1.7, [0, 0.2, 0])]},
        extra={'sigils': {'scale': S.ch([(0, [1, 1, 1]), (0.5, [0.7, 0.7, 1]), (0.9, [2.0, 2.0, 1]), (1.7, [1.9, 1.9, 1]), (2.1, [1, 1, 1])]), 'rotation': S.spin(2.1, 1.5, 2)}})
    # CROWNFALL: hoists Kingsbane point-down overhead as the crown rises, drops to one knee and drives the blade deep
    # into the floor in front of him (impact 0.9; frozen for review at 0.9)
    DRIVE = SW((-4.5, 35.0, -20.0), (0.0, -0.85, -0.53))
    A['crownfall'] = act(1.6, [
        (0.22, SW((-2.0, 50.0, -17.0), (0.0, 0.75, -0.66))), (0.45, SW((-2.0, 64.0, -10.0), (0.0, 0.1, -0.99))), (0.68, SW((-2.0, 68.0, -9.0), (0.0, -0.35, -0.94))),
        (0.9, DRIVE), (1.25, DRIVE)],
        {'chest': [(0.45, [-14, 0, 0]), (0.68, [-18, 0, 0]), (0.9, [22, 0, 0]), (1.25, [22, 0, 0])],
         'torso': [(0.68, [-6, 0, 0]), (0.9, [12, 0, 0]), (1.25, [12, 0, 0])],
         'head': [(0.45, [-24, 0, 0]), (0.9, [-14, 0, 0]), (1.25, [-14, 0, 0])],
         'hips': [(0.9, [0, 0, 0]), (1.25, [0, 0, 0])],
         'right_leg': [(0.45, [-8, 0, 0]), (0.9, [-4, 0, 4]), (1.25, [-4, 0, 4])], 'right_shin': [(0.45, [10, 0, 0]), (0.9, [96, 0, 0]), (1.25, [96, 0, 0])],
         'left_leg': [(0.45, [10, 0, 0]), (0.9, [-80, 0, -4]), (1.25, [-80, 0, -4])], 'left_shin': [(0.45, [8, 0, 0]), (0.9, [84, 0, 0]), (1.25, [84, 0, 0])]},
        {'root': [(0.45, [0, 2.0, 0]), (0.68, [0, 2.6, 0]), (0.9, [0, -12.0, 0]), (1.25, [0, -12.0, 0])]},
        extra={'mitre0': {'position': S.ch([(0, R0), (0.6, [0, 6, 0]), (0.9, [0, 1, 0]), (1.6, R0)]), 'rotation': S.spin(1.6, 1, 1)},
               'sigils': {'rotation': S.spin(1.6, 2, 2)}})
    # DEATH: staggers, sinks onto both knees leaning on the planted blade, and stays down
    KNEEL = SW((-1.0, 38.0, -16.0), (0.0, -0.8, -0.6))
    A['death'] = act(2.5, [(0.6, PLANT), (1.3, KNEEL), (2.5, KNEEL)],
        {'chest': [(0.4, [-20, 0, 0]), (1.3, [20, 0, 0]), (2.5, [26, 0, 0])], 'head': [(0.4, [-30, 0, 0]), (1.4, [30, 0, 0]), (2.5, [32, 0, 0])],
         'right_leg': [(0.6, [-30, 0, 0]), (1.3, [-80, 0, 0]), (2.5, [-80, 0, 0])], 'left_leg': [(0.8, [-20, 0, 0]), (1.3, [-76, 0, 0]), (2.5, [-76, 0, 0])],
         'right_shin': [(1.3, [96, 0, 0]), (2.5, [96, 0, 0])], 'left_shin': [(1.3, [92, 0, 0]), (2.5, [92, 0, 0])]},
        {'root': [(0.6, [0, -2, 0]), (1.3, [0, -13, 0]), (2.5, [0, -13, 0])]}, end=False)
    return A

# ------------------------------------------------------------------ the bake: IK + cloth
REACH = 22.6
ARMS_R = ['right_arm', 'right_forearm', 'right_hand']; ARMS_L = ['left_arm', 'left_forearm', 'left_hand']
LIM_ARM = ([-220, -120, -150], [90, 120, 90]); LIM_FA = ([-140, -90, -10], [0, 90, 10]); LIM_HAND = ([-100, -170, -90], [100, 170, 90])
W_AX, W_FI = 8, 1.5


CLEAR_BONES = ['chest', 'torso', 'hips', 'head', 'right_leg', 'left_leg', 'right_shin', 'left_shin']
def body_boxes(rig_):
    """Per bone: list of (inverse cube rotation about pivot, min, max) in bone-rest space, for staff clearance."""
    out = {}
    for bn in CLEAR_BONES:
        L = []
        for c in rig_.bones[bn]['cubes']:
            o = np.array(c['origin'], float); sz = np.array(c['size'], float)
            if c.get('rot'):
                Rm = K.rot(c['rot'][0], -c['rot'][1], c['rot'][2]); pv = np.array(c['pivot'], float)
            else: Rm, pv = np.eye(3), np.zeros(3)
            L.append((Rm.T, pv, o - 0.4, o + sz + 0.4))
        out[bn] = L
    return out

def staff_hits(fk, boxes, R, Q):
    ms = fk.world('staff', R, Q)
    pts = [K.xf(ms, (SWORD_X, GRIP_R + y, SWORD_Z)) for y in np.arange(-9.5, 49, 1.5)]
    pts += [K.xf(ms, (SWORD_X, GRIP_R + y, SWORD_Z + dz)) for y in (5.2, 20, 35) for dz in (-7.5, -4, 4, 7.5)]
    pts += [K.xf(ms, (SWORD_X + dx, GRIP_R + y, SWORD_Z + dz)) for y in (-10.4, -9.2, -8.0, 3.4) for dx in (-1.4, 1.4) for dz in (-1.4, 1.4)]
    hits = []
    for bn, L in boxes.items():
        Mi = np.linalg.inv(fk.world(bn, R, Q))
        for pw in pts:
            pl = K.xf(Mi, pw)
            for RT, pv, lo, hi in L:
                q = RT @ (pl - pv) + pv
                if np.all(q > lo) and np.all(q < hi): hits.append((bn, pw)); break
    return hits

def sample_body(a, t):
    R, Q = {}, {}; L = a['T'] if a['loop'] else None
    for bn, ks in a['body'].items(): R[bn] = interp(ks, t, L)
    for bn, ks in a['pos'].items(): Q[bn] = interp(ks, t, L)
    if 'rotation' in a['extra'].get('root', {}):
        rk = sorted(((float(k), v['post'] if isinstance(v, dict) else v) for k, v in a['extra']['root']['rotation'].items()))
        R['root'] = interp(rk, t)
    return R, Q

def bake(rig_, A, verbose=False):
    fk = K.FK(rig_); out = {}; boxes = body_boxes(rig_)
    loR = LIM_ARM[0] + LIM_FA[0] + LIM_HAND[0]; hiR = LIM_ARM[1] + LIM_FA[1] + LIM_HAND[1]
    loL = [LIM_ARM[0][0], -LIM_ARM[1][1], -LIM_ARM[1][2]] + LIM_FA[0] + LIM_HAND[0]
    hiL = [LIM_ARM[1][0], -LIM_ARM[0][1], -LIM_ARM[0][2]] + LIM_FA[1] + LIM_HAND[1]
    for name, a in A.items():
        T = a['T']; n = max(2, int(round(T * 20))); ts = [T * i / n for i in range(n + 1)]
        keys = {bn: [] for bn in ARMS_R + ARMS_L}
        xr = np.array([-50, 0, -10, -40, 0, 0, 20, 0, 0], float); xl = np.array([-60, 0, 20, -60, 0, 0, 0, 0, 0], float)
        worst = wl = 0.0
        for t in ts:
            R, Q = sample_body(a, t)
            sw = interp(a['sword'], t, T if a['loop'] else None); P, D, N = sw[:3], nrm(sw[3:6]), np.array(sw[6:9])
            N = nrm(N - D * (N @ D))
            Cm = fk.world('chest', R, Q); Rm = fk.world('root', R, Q)
            Pw, Dw, Nw = K.xf(Rm, P), K.xd(Rm, D), K.xd(Rm, N)
            shR = K.xf(fk.world('right_arm', R, Q), (-11.6, 52.5, 0)); shL = K.xf(fk.world('left_arm', R, Q), (11.6, 52.5, 0))
            home = (shR + shL) / 2 + K.xd(Cm, (0, -10, -9))
            for _ in range(60):   # keep both fists within reach: slide the grip toward the chest if needed
                if np.linalg.norm(Pw - shR) <= REACH and np.linalg.norm(Pw - Dw * (GRIP_R - GRIP_L) - shL) <= REACH: break
                Pw = Pw + (home - Pw) * 0.05
            def fr(x):
                RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = x[0:3], x[3:6], x[6:9]
                m = fk.world('staff', RR, Q)
                return np.concatenate([(K.xf(m, (SWORD_X, GRIP_R, SWORD_Z)) - Pw) * 3, (K.xd(m, (0, 1, 0)) - Dw) * 10, (K.xd(m, (1, 0, 0)) - Nw) * 3, x * 0.004])
            seeds = [xr] if t > 0 else [xr, np.array([-90, 0, -20, -60, 0, 0, 0, 0, 0.0]), np.array([-150, 0, -10, -20, 0, 0, 0, 0, 0.0])]
            xr = min((K.solve(fr, sd, loR, hiR) for sd in seeds), key=lambda q: q[1])[0]
            RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = xr[0:3], xr[3:6], xr[6:9]
            for _c in range(16):   # clearance: push the grip away from any body part the blade or pommel enters
                hs = staff_hits(fk, boxes, RR, Q)
                if not hs: break
                ctr = np.mean([K.xf(fk.world(bn, RR, Q), (0, 40, 0)) for bn, _ in hs], axis=0)
                away = np.mean([pw for _, pw in hs], axis=0) - ctr; away[1] *= 0.3
                Pw = Pw + nrm(away + K.xd(Rm, (0, 0, -1.5))) * 1.5
                xr = K.solve(fr, xr, loR, hiR)[0]
                RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = xr[0:3], xr[3:6], xr[6:9]
            ms = fk.world('staff', RR, Q); mh = fk.world('right_hand', RR, Q)
            er = float(np.linalg.norm(K.xf(ms, (SWORD_X, GRIP_R, SWORD_Z)) - Pw)); worst = max(worst, er); el = 0.0
            free = any(t0 < t < t1 for t0, t1 in a['free'])
            if not free:
                tgt = K.xf(ms, (SWORD_X, GRIP_L, SWORD_Z)); ax = K.xd(ms, (0, 1, 0)); sa = K.xf(ms, (SWORD_X, GRIP_R, SWORD_Z)); sd = ax; fing = K.xd(mh, (0, 1, 0))
                def fl(x):
                    RL = dict(RR); RL['left_arm'], RL['left_forearm'], RL['left_hand'] = x[0:3], x[3:6], x[6:9]
                    m = fk.world('left_hand', RL, Q); mf = fk.world('left_forearm', RL, Q)
                    pen = []
                    for yy in (HY + 2.4, HY + 5, 38.5, 41.5):
                        q = K.xf(mf, (11.6, yy, 0)); u = np.clip((q - sa) @ sd, -12.0, 50.0)
                        pen.append(max(0.0, 3.6 - np.linalg.norm(sa + sd * u - q)) * 6)
                    return np.concatenate([(K.xf(m, (11.6, GRIP_R, SWORD_Z)) - tgt) * 3, pen, (K.xd(m, (0, 0, -1)) - ax) * W_AX, (K.xd(m, (0, 1, 0)) - nrm(shL - tgt)) * W_FI, x * 0.004])
                seeds = [xl] if t > 0 else [xl, np.array([-90, 0, 30, -60, 0, 0, 0, 0, 0.0]), np.array([-150, 0, 10, -20, 0, 0, 0, 0, 0.0])]
                xl = min((K.solve(fl, sd, loL, hiL) for sd in seeds), key=lambda q: q[1])[0]
                RL = dict(RR); RL['left_arm'], RL['left_forearm'], RL['left_hand'] = xl[0:3], xl[3:6], xl[6:9]
                el = float(np.linalg.norm(K.xf(fk.world('left_hand', RL, Q), (11.6, GRIP_R, SWORD_Z)) - tgt)); wl = max(wl, el)
                for i, bn in enumerate(ARMS_L): keys[bn].append((t, [round(float(v), 2) for v in xl[3 * i:3 * i + 3]]))
            for i, bn in enumerate(ARMS_R): keys[bn].append((t, [round(float(v), 2) for v in xr[3 * i:3 * i + 3]]))
            if verbose and (er > 1.0 or el > 1.0): print('   miss', name, round(t, 2), round(er, 1), round(el, 1))
        for t0, t1 in a['free']:   # free-hand windows: authored gesture between the solved grips
            for bn in ARMS_L:
                pre = [k for k in keys[bn] if k[0] <= t0]; post = [k for k in keys[bn] if k[0] >= t1]
                mid = [(float(t_), [round(float(q), 2) for q in v_]) for t_, v_ in a['lf'].get(bn, [])] or [((t0 + t1) / 2, pre[-1][1] if pre else [0, 0, 0])]
                keys[bn] = pre + mid + post
        out[name] = keys
        print(f'  ik {name:12s} grip error right {worst:.2f}px left {wl:.2f}px')
    return out

def cloth(rig_, A, ik):
    """Verlet bake for the two cape halves: 7 particles per half, gravity, drag, a back-breeze, collisions against
    spheres riding the back, hips and legs. Each segment's direction becomes its X/Z hinge angle."""
    fk = K.FK(rig_); res = {}
    lens = [c[0] for c in CAPE]
    for name, a in A.items():
        T = a['T']; dt = 1 / 60; steps = int(round(T / dt)); loops = 3 if a['loop'] else 1
        def full_pose(t):
            R, Q = sample_body(a, t)
            for bn, ks in ik[name].items(): R[bn] = interp(ks, t, T if a['loop'] else None)
            return R, Q
        R, Q = full_pose(0); m = fk.world('chest', R, Q)
        parts = {h: np.array([K.xf(m, (s * 4.0, 55.0 - sum(lens[:i]), 7.0 + 0.2 * i)) for i in range(7)]) for h, s in (('R', -1), ('L', 1))}
        prev = {h: parts[h].copy() for h in parts}; rec = {h: [] for h in 'RL'}
        for L in range(loops):
            for i in range(steps + 1):
                t = i * dt; R, Q = full_pose(min(t, T))
                Cm = fk.world('chest', R, Q); Hm = fk.world('hips', R, Q); Tm = fk.world('torso', R, Q)
                sph = [(K.xf(Cm, (0, 48, -0.5)), 8.6), (K.xf(Cm, (-5, 50, -0.5)), 7.0), (K.xf(Cm, (5, 50, -0.5)), 7.0), (K.xf(Tm, (0, 36, 0.5)), 6.6), (K.xf(Hm, (0, 29, 0.5)), 6.6)]
                for side, xx in (('right', -3.8), ('left', 3.8)):
                    lm = fk.world(f'{side}_leg', R, Q); sm = fk.world(f'{side}_shin', R, Q)
                    sph += [(K.xf(lm, (xx, 25, 0)), 4.0), (K.xf(lm, (xx, 19.5, 0)), 3.7), (K.xf(sm, (xx, 11, 0)), 3.2), (K.xf(sm, (xx, 5, 0)), 3.0)]
                for h, s in (('R', -1), ('L', 1)):
                    p, q = parts[h], prev[h]
                    anchor = K.xf(Cm, (s * 4.0, 55.0, 6.8))
                    breeze = K.xd(Cm, (0, 0, 1)) * (10.0 + 4.0 * math.sin(t * 2.1 + (0 if h == 'R' else 1.3)))
                    nxt = p + (p - q) * 0.96 + (np.array([0, -70.0, 0]) + breeze) * dt * dt
                    for _ in range(6):
                        nxt[0] = anchor
                        for j in range(6):
                            d = nxt[j + 1] - nxt[j]; dl = np.linalg.norm(d) + 1e-9; diff = (dl - lens[j]) / dl
                            if j == 0: nxt[j + 1] -= d * diff
                            else: nxt[j] += d * diff * 0.5; nxt[j + 1] -= d * diff * 0.5
                        for j in range(1, 7):
                            for c, rad in sph:
                                v = nxt[j] - c; dl = np.linalg.norm(v)
                                if dl < rad + 0.5: nxt[j] = c + v / (dl + 1e-9) * (rad + 0.5)
                            if nxt[j][1] < 0.8: nxt[j][1] = 0.8
                    prev[h], parts[h] = p, nxt
                    if L == loops - 1 and i % 3 == 0: rec[h].append((min(t, T), Cm.copy(), nxt.copy()))
        ch = {}
        for h in 'RL':
            for j in range(6): ch[f'cape{h}{j + 1}'] = []
            for t, Cm, pts in rec[h]:
                PM = Cm[:3, :3]
                for j in range(6):
                    d = PM.T @ nrm(pts[j + 1] - pts[j])                  # segment direction in its parent's frame
                    rx = math.degrees(math.atan2(d[2], -d[1]))          # tilt of -Y toward +Z (backward) about X
                    rz = math.degrees(math.atan2(d[0], math.hypot(d[1], d[2])))
                    Rm = K.rot(rx, 0, 0) if True else None
                    ang = np.clip(np.array([rx, 0.0, -rz]), [-80, -5, -45], [150, 5, 45])
                    ch[f'cape{h}{j + 1}'].append((t, ang))
                    PM = PM @ K.rot(-ang[0], ang[1], -ang[2])
        res[name] = ch
    return res

def anims(rig_):
    A = author(); ik = bake(rig_, A); cl = cloth(rig_, A, ik)
    out = {}
    for name, a in A.items():
        T = a['T']; b = {}
        for bn, ks in a['body'].items(): b.setdefault(bn, {})['rotation'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in a['pos'].items(): b.setdefault(bn, {})['position'] = S.ch([(t, [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, ks in ik[name].items(): b.setdefault(bn, {})['rotation'] = S.ch(ks)
        b['staff'] = {'rotation': S.hold(R0, T)}
        for bn, ks in cl[name].items(): b.setdefault(bn, {})['rotation'] = S.ch([(round(t, 4), [round(float(q), 2) for q in v]) for t, v in ks])
        for bn, chn in a['extra'].items():
            for k_, v in chn.items(): b.setdefault(bn, {})[k_] = v
        b.setdefault('sigil_hand', {}).setdefault('scale', S.hold([0.01] * 3, T))
        b.setdefault('sigils', {}).setdefault('rotation', S.spin(T, 1 if a['loop'] else 0.5, 2))
        b.setdefault('orb', {'position': S.loop(T, 0.8, 1), 'rotation': S.spin(T, 1, 1)})
        b.setdefault('censer', {'rotation': S.loop(T, 10, 0, phase=1.2)})
        b.setdefault('mitre0', {'position': S.loop(T, 0.6, 1)})
        b.setdefault('mitre1', {'rotation': S.loop(T, 3, 2)})
        b.setdefault('robe_back', {'rotation': S.loop(T, 3, 0, (4, 0, 0))}); b.setdefault('robe_back2', {'rotation': S.loop(T, 5, 0, (3, 0, 0), 1)})
        b.setdefault('tabard', {'rotation': S.loop(T, 4, 0, (2, 0, 0), 0.4)})
        out[name] = S.A(T, b, a['loop'])
    for nm in ('attack', 'roar'):
        L = out[nm]['length']; out[nm]['bones']['mitre0'] = {'position': S.ch([(0, R0), (0.2, [0, 4.0, 0]), (L - 0.2, [0, 4.0, 0]), (L, R0)])}
    return out

def build(paint=True):
    name = 'hollow_king'; S.PAL.update(PAL)
    m = dict(PAL[name], ts=list(PAL[name]['ts'])); rg = rig(); uvs = G.pack(rg, m['k'], m['ts'])
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    if paint:
        hm = dict(m, k=m['k'] * 2, ts=[m['ts'][0] * 2, m['ts'][1] * 2]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = Painter(name, hm)
        for uv, c in zip(huvs, cs): pt.cube(uv, c)
        S.downsample(name, m, rg, pt, m['k'], huvs, uvs)
    G.geo(name, rg, m['k'], m['ts'], uvs)
    G.write_anims(name, anims(rg))
    S.FREEZE['crownfall'] = 0.9
    S.bake_freezes(name)
    print(name, len(rg.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    build(paint='nopaint' not in sys.argv)

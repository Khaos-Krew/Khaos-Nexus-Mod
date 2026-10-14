"""1.2 Crownless Generals (ambush mini-bosses for A/S gates): the Oathbroken Blade and the Ashen Arcanist.
Built with the rigs_saga2 helpers on the Hierophant's bone layout so his authored, clip-checked animation set drives
both (the 'staff' bone is the weapon, 'censer' its hanging trophy, 'sigil_hand'/'sigils' the off-hand and back relics).
Each has a crown motif that is broken or missing: they served the Hollow Crown and were cast out.
Run: python3 rigs_generals.py"""
import math, sys, json, random
import gen_geo_assets as G
import rigs_saga2 as S
Rig, kf, taper, shard, side_name = G.Rig, G.kf, S.taper, S.shard, S.side_name
R0 = [0, 0, 0]

def skirt(r, tiers, seed, ytop_shift=0.0):
    """Flaring layered strips (Hierophant method): tier0 on hips, tier1 on thighs/back panel, tier2 on shins/back2."""
    rnd = random.Random(seed)
    for tier, (n, rx, rz, ytop, ln, flare, role) in enumerate(tiers):
        ytop += ytop_shift
        for i in range(n):
            a = 2 * math.pi * (i + 0.5 * (tier % 2)) / n; ca, sa = math.cos(a), math.sin(a)
            cx, cz = sa * rx, ca * rz
            L = ln + (rnd.uniform(-0.6, 2.4) if role == 'capehem' else rnd.uniform(-0.4, 0.8))
            if tier == 0: bone = 'hips'
            elif cz > rz * 0.45: bone = 'robe_back' if tier == 1 else 'robe_back2'
            else: bone = (('right_leg' if cx < 0 else 'left_leg') if tier == 1 else ('right_shin' if cx < 0 else 'left_shin'))
            w = 2 * math.pi * math.hypot(rx, rz) / 2 / n * 1.18
            size = (w, L, 0.9) if abs(ca) >= abs(sa) else (0.9, L, w * 0.9)
            r.box(bone, (cx, ytop - L / 2, cz), size, role, rot=(ca * flare, 0, -sa * flare), pivot=(cx, ytop, cz))

def legs(r, armored):
    B = r.box
    for side, s in (('right', -1), ('left', 1)):
        x = s * 3.6
        lg = r.bone(f'{side}_leg', 'root', (x, 22, 0)); taper(r, lg, x, 0, 22, 11, 4.8, 4.2, 4.8, 4.2, 2, 'plate' if armored else 'limb')
        sh = r.bone(f'{side}_shin', lg, (x, 11, 0)); taper(r, sh, x, 0, 11, 1.5, 4.0, 3.6, 4.0, 3.6, 2, 'plate' if armored else 'limb')
        if armored:
            B(lg, (x, 11.6, -2.3), (3.6, 3.2, 1.2), 'guard'); B(lg, (x, 11.6, -2.9), (1.4, 1.4, 0.6), 'crown', rot=(0, 0, 45))
            B(sh, (x, 6.5, -2.1), (3.4, 7, 0.6), 'plate')
        B(sh, (x, 1, -1.2), (4.6, 2, 7), 'gauntlet')
        if armored: B(sh, (x, 1.8, -4.4), (3.2, 1.2, 1.4), 'guard')
    r.bone('hips', 'root', (0, 22, 0)); r.bone('robe_back', 'hips', (0, 22, 4.6)); r.bone('robe_back2', 'robe_back', (0, 10.6, 5.0))

def cape(r, links, roles=('cape', 'cape', 'capehem'), z=6.4):
    prev, y = 'chest', 44
    for j, (ln, w) in enumerate(links):
        bn = r.bone(f'cape{j + 1}', prev, (0, y, z), (6, 0, 0) if j == 0 else (0, 0, 0))
        r.box(bn, (0, y - ln / 2, z + 0.2), (w, ln, 1.0), roles[j]); r.box(bn, (0, y - ln / 2, z + 0.75), (2.4, ln, 0.2), 'rune' if j == 1 else 'guard')
        prev, y = bn, y - ln

# ============================================================== The Oathbroken Blade
def rig_blade():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0)); legs(r, True)
    B('hips', (0, 22, 0), (15, 3.4, 10), 'belt'); B('hips', (0, 22, -5.2), (3.2, 3.2, 0.8), 'guard')
    skirt(r, ((10, 7.6, 5.4, 22.4, 6.0, 10, 'plate'), (14, 8.3, 6.0, 17.4, 6.4, 12, 'capehem')), 17)
    r.bone('tabard', 'hips', (0, 21, -5.4))
    B('tabard', (0, 13.6, -5.9), (5.4, 14.6, 0.6), 'cape'); B('tabard', (0, 15.8, -6.3), (3.2, 3.2, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 5.6, -5.9), (5.0, 2.4, 0.6), 'capehem')
    # torso: barrel breastplate, plackart, gorget, back plate
    r.bone('torso', 'hips', (0, 22.5, 0), (6, 0, 0))
    taper(r, 'torso', 0, 0, 22.5, 34, 12.4, 14.6, 8.6, 9.8, 2, 'plate')
    for j in range(3): B('torso', (0, 24.2 + j * 3.4, -4.7 - j * 0.12), (13.4 + j * 0.6, 3.2, 1.2), 'breastplate')
    r.bone('chest', 'torso', (0, 34, 0))
    taper(r, 'chest', 0, 0.2, 34, 46, 14.6, 18.6, 9.8, 11.4, 3, 'plate')
    B('chest', (0, 41, -5.6), (16.4, 9.6, 1.4), 'breastplate'); B('chest', (0, 41, -6.35), (1.2, 9.0, 0.3), 'rune')
    B('chest', (0, 46.6, -0.4), (11.6, 2.6, 10.4), 'guard')   # gorget
    for s in (-1, 1):   # broken crown shards jutting from the back plate
        shard(r, 'chest', (s * 4.6, 45.6, 5.0), 9, 1.8, (-30, 0, s * -18), 'helmspire', 'crown')
    # head: great helm with a T-visor, a broken crown floating just above (mitre0)
    r.bone('head', 'chest', (0, 46.5, -1.6))
    B('head', (0, 52.2, -0.6), (9.6, 10.4, 10), 'helm'); B('head', (0, 57.6, -0.6), (8.4, 1.4, 8.8), 'helm')
    B('head', (0, 52.6, -5.7), (7.6, 1.2, 0.4), 'void'); B('head', (0, 50.6, -5.7), (1.2, 3.6, 0.4), 'void')
    B('head', (0, 53.0, -5.95), (8.0, 0.4, 0.2), 'core'); B('head', (0, 52.0, -5.75), (10.2, 0.6, 0.4), 'guard')
    for s in (-1, 1):
        B('head', (s * 5.0, 50.6, -2), (0.8, 6, 6), 'plate'); B('head', (s * 3.6, 47.6, -5.4), (2.6, 2.0, 0.8), 'visorplate')
    B('head', (0, 58.6, 1.0), (1.4, 1.2, 8), 'crown'); B('head', (0, 57.4, 6.4), (2.2, 6, 1.0), 'cape')   # crest + crest tail
    r.bone('mitre0', 'head', (0, 61.2, -0.6))
    for k in range(9):   # crown ring tilted, three prongs snapped off
        a = 2 * math.pi * k / 9; px, pz = math.sin(a) * 5.4, -0.6 + math.cos(a) * 5.4
        B('mitre0', (px, 60.8, pz), (3.8, 1.4, 1.0) if abs(math.cos(a)) > 0.5 else (1.0, 1.4, 3.8), 'crown', rot=(0, math.degrees(a), 0))
        ln = 1.2 if k in (2, 5, 6) else 3.6
        B('mitre0', (px, 62.2 + ln / 2, pz), (1.0, ln, 1.0), 'crown', rot=(math.cos(a) * -10, 0, math.sin(a) * 10), pivot=(px, 61.6, pz))
        if k % 3 == 0: B('mitre0', (px * 1.02, 60.8, pz * 1.02 - 0.012), (1.2, 1.2, 1.2), 'core', rot=(45, 45, 0))
    r.bone('mitre1', 'mitre0', (0, 61.2, -0.6))
    # arms: tiered pauldrons, couters, vambraces, gauntlets
    for side, s in (('right', -1), ('left', 1)):
        x = s * 10.4
        arm = r.bone(f'{side}_arm', 'chest', (x, 45, 0))
        for j in range(3): B(arm, (x + s * 0.4 * j, 46.0 - j * 2.4, 0), (8.2 - j * 0.6, 3.2, 8.6 - j * 0.4), 'pauldron', rot=(0, 0, s * (12 + j * 6)))
        B(arm, (x + s * 2.8, 49.4, 0), (1.4, 4.6, 1.4), 'crown', rot=(0, 0, s * -18))
        taper(r, arm, x, 0, 42, 36, 5.2, 5.0, 5.2, 5.0, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 36, 0))
        B(fa, (x, 35.6, 1.0), (5.0, 3.6, 4.0), 'guard'); taper(r, fa, x, 0, 35, 28.5, 5.0, 5.8, 5.0, 5.8, 2, 'plate')
        B(fa, (x, 28.6, 0), (6.4, 1.4, 6.4), 'guard')
        hd = r.bone(f'{side}_hand', fa, (x, 27.5, -0.4))
        B(hd, (x, 25.6, -0.4), (3.6, 3.6, 3.6), 'gauntlet'); B(hd, (x, 23.6, -1.4), (3.4, 1.6, 2.0), 'gauntlet')
    # greatsword: the 'staff' bone (same hand pivot as the Hierophant's staff, so his swings carry it)
    x, z = -10.4, -0.6
    r.bone('staff', 'right_hand', (x, 25.6, z))
    B('staff', (x, 17.4, z), (2.6, 2.6, 2.6), 'crown', rot=(45, 0, 45)); taper(r, 'staff', x, z, 18.4, 31.6, 1.6, 1.6, 1.6, 1.6, 3, 'grip')
    B('staff', (x, 32.4, z), (2.4, 1.8, 10.4), 'guard')
    for s in (-1, 1): B('staff', (x, 33.4, z + s * 5.6), (1.8, 3.0, 1.6), 'crown', rot=(s * 20, 0, 0))
    B('staff', (x, 34.0, z), (2.8, 2.4, 4.2), 'guard')
    taper(r, 'staff', x, z, 35, 76, 1.2, 1.0, 5.0, 3.8, 5, 'greatblade')
    B('staff', (x, 79.0, z), (0.9, 6, 2.6), 'greatblade'); B('staff', (x, 82.6, z), (0.7, 2.2, 1.2), 'greatblade')
    B('staff', (x, 55, z), (1.4, 36, 1.0), 'rune')
    r.bone('censer', 'staff', (x, 32.4, z - 5.0))   # a snapped crown hanging from the crossguard on a chain
    cz = z - 5.0
    for j in range(3): B('censer', (x, 31.0 - j * 1.9, cz), (0.6, 1.6, 0.6), 'guard', rot=(0, 45 * j, 0))
    for k in range(6):
        a = 2 * math.pi * k / 6; B('censer', (x + math.sin(a) * 1.8, 25.6, cz + math.cos(a) * 1.8), (1.0, 1.6 if k != 2 else 0.8, 1.0), 'crown', rot=(0, math.degrees(a), 0))
    B('censer', (x, 24.8, cz), (3.4, 0.8, 3.4), 'crown')
    r.bone('sigil_hand', 'left_hand', (10.4, 22, -3))   # an ember of the broken oath above the left fist
    B('sigil_hand', (10.4, 20.8, -3.6), (2.6, 2.6, 2.6), 'core', rot=(45, 45, 0)); B('sigil_hand', (10.4, 20.8, -3.6), (3.6, 3.6, 0.4), 'rune', rot=(0, 0, 45))
    r.bone('sigils', 'root', (0, 50, 9))
    for k in range(3):   # three broken crown prongs orbiting his back
        a = 2 * math.pi * k / 3; px, py = math.sin(a) * 8.5, 50 + math.cos(a) * 8.5
        B('sigils', (px, py, 10.4), (1.6, 4.6, 1.0), 'crown', rot=(0, 0, math.degrees(-a))); B('sigils', (px, py, 10.2), (1.4, 1.4, 1.4), 'core', rot=(45, 45, 0))
    cape(r, ((12, 17), (12, 18), (10, 19)), z=6.2)
    return r

# ============================================================== The Ashen Arcanist
def rig_arcanist():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0)); legs(r, False)
    B('hips', (0, 22, 0), (14.4, 3.0, 9.6), 'belt')
    for x in (-4.2, 0, 4.2): B('hips', (x, 19.2, -5.0), (1.8, 4.4, 0.6), 'mask')   # bone talismans on cords
    skirt(r, ((12, 7.6, 5.4, 22.6, 8.8, 7, 'robe'), (14, 8.2, 5.8, 15.6, 9.4, 10, 'robe'), (16, 8.9, 6.4, 8.2, 7.6, 13, 'capehem')), 23)
    r.bone('tabard', 'hips', (0, 21, -5.4))
    B('tabard', (0, 11.8, -5.5), (3.6, 18, 0.5), 'collar'); B('tabard', (0, 3.0, -5.5), (3.0, 2.0, 0.5), 'capehem')
    r.bone('torso', 'hips', (0, 22.5, 0), (10, 0, 0))
    taper(r, 'torso', 0, 0, 22.5, 34, 11.6, 13.2, 7.8, 8.8, 2, 'robe')
    r.bone('chest', 'torso', (0, 34, 0))
    taper(r, 'chest', 0, 0.2, 34, 46, 13.2, 16.4, 8.8, 10.4, 3, 'robe')
    B('chest', (0, 46.0, 0.2), (17.6, 2.6, 11.6), 'cape'); B('chest', (0, 43.8, 0.2), (18.6, 2.4, 12.2), 'capehem')
    for s in (-1, 1):   # tall flared collar wings behind the hood
        B('chest', (s * 5.4, 51.6, 3.4), (5.2, 11, 0.8), 'collar', rot=(-14, s * 28, s * -8), pivot=(s * 4, 46, 3.4))
        B('chest', (s * 3.0, 38.6, -5.4), (2.0, 14, 0.6), 'collar')
    B('chest', (0, 39.6, -5.5), (4.2, 4.2, 0.6), 'guard', rot=(0, 0, 45)); B('chest', (0, 39.6, -5.9), (2.6, 2.6, 0.3), 'core', rot=(0, 0, 45))
    r.bone('head', 'chest', (0, 46.5, -2.0))
    B('head', (0, 52.4, 0.2), (10.6, 11.4, 10.8), 'hood'); B('head', (0, 58.2, -3.6), (10.6, 1.8, 3.6), 'hood', rot=(-16, 0, 0))
    B('head', (0, 52.4, -5.2), (8.4, 9.8, 0.4), 'void')
    B('head', (0, 52.6, -5.5), (6.0, 7.6, 0.6), 'mask'); B('head', (0, 56.0, -5.6), (5.0, 0.8, 0.5), 'mask')
    for s in (-1, 1): B('head', (s * 1.5, 53.6, -5.85), (1.6, 0.9, 0.2), 'core')
    B('head', (0, 51.0, -5.85), (0.6, 4.0, 0.2), 'void')   # crack
    for i, (x, ln) in enumerate(((-2.0, 5), (0, 7.5), (2.0, 5.5))): B('head', (x, 47.8 - ln / 2, -5.0), (1.4, ln, 0.5), 'capehem')
    r.bone('mitre0', 'head', (0, 55, 6.4))
    for k in range(11):   # a broken halo of crown prongs standing behind the head, two missing
        if k in (3, 4): continue
        a = math.pi * (k / 10 - 0.5) * 1.6; px, py = math.sin(a) * 8.2, 55 + math.cos(a) * 8.2
        B('mitre0', (px, py, 6.6), (2.2, 1.6, 0.8), 'crown', rot=(0, 0, -math.degrees(a)))
        B('mitre0', (px * 1.22, 55 + (py - 55) * 1.22, 6.6), (0.9, 2.8 if k % 2 else 1.6, 0.6), 'crown', rot=(0, 0, -math.degrees(a)))
    B('mitre0', (0, 63.4, 6.4), (1.6, 1.6, 1.6), 'core', rot=(45, 45, 0))
    r.bone('mitre1', 'mitre0', (0, 63, 6.4))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 9.6
        arm = r.bone(f'{side}_arm', 'chest', (x, 45, 0))
        B(arm, (x, 43.4, 0), (6.0, 5.6, 6.6), 'robe'); B(arm, (x + s * 0.6, 45.6, 0), (6.6, 1.6, 7.2), 'guard')
        taper(r, arm, x, 0, 43, 36, 5.4, 5.8, 5.4, 5.8, 2, 'sleeve')
        fa = r.bone(f'{side}_forearm', arm, (x, 36, 0))
        taper(r, fa, x, 0, 36, 27, 5.8, 8.2, 5.8, 8.2, 4, 'sleeve')
        B(fa, (x, 27.6, 0), (8.6, 1.2, 8.6), 'guard'); B(fa, (x, 27.0, 0), (6.6, 0.2, 6.6), 'void')
        B(fa, (x, 26.6, 3.2), (7.0, 9.6, 1.0), 'capehem', rot=(14, 0, 0), pivot=(x, 31, 3.2))
        hd = r.bone(f'{side}_hand', fa, (x, 27.5, -0.4))
        B(hd, (x, 25.6, -0.4), (2.8, 3.0, 2.2), 'claw')
        for j in range(4):
            fx = x + (j - 1.5) * 0.7; B(hd, (fx, 22.8, -0.9), (0.5, 3.0, 0.5), 'claw', rot=(18, 0, 0), pivot=(fx, 24.2, -0.6))
    x, z = -9.6, -0.6
    r.bone('staff', 'right_hand', (x, 25.6, z))   # the ash rod: banded haft, a caged ember orb, floating shards
    taper(r, 'staff', x, z, 7.0, 70, 1.6, 2.2, 1.6, 2.2, 6, 'shaft')
    for y in (8, 18, 40, 54, 66): B('staff', (x, y, z), (2.8, 1.0, 2.8), 'guard')
    for y in range(23, 32, 2): B('staff', (x, y, z), (2.5, 0.8, 2.5), 'belt')
    for i in range(5):
        a = 2 * math.pi * i / 5; dx, dz = math.sin(a), math.cos(a)
        B('staff', (x + dx * 2.8, 75, z + dz * 2.8), (0.9, 9, 0.9), 'crown', rot=(dz * -16, 0, dx * 16), pivot=(x + dx * 1.6, 70.6, z + dz * 1.6))
    B('staff', (x, 76, z), (3.4, 3.4, 3.4), 'core', rot=(45, 45, 0)); B('staff', (x, 76, z), (4.6, 4.6, 4.6), 'orbshell', rot=(45, 45, 0))
    for s in (-1, 1): shard(r, 'staff', (x + s * 4.4, 80, z), 5, 1.2, (0, 0, -s * 24), 'spike', 'spike')
    B('staff', (x - 2.8, 66.4, z), (4.6, 0.9, 0.9), 'guard')
    r.bone('censer', 'staff', (x - 4.8, 66.4, z))   # a hanging ash lantern
    for j in range(2): B('censer', (x - 4.8, 65.2 - j * 1.8, z), (0.6, 1.5, 0.6), 'guard', rot=(0, 45 * j, 0))
    B('censer', (x - 4.8, 60.4, z), (3.4, 4.0, 3.4), 'orb'); B('censer', (x - 4.8, 62.8, z), (2.6, 1.0, 2.6), 'crown'); B('censer', (x - 4.8, 58.0, z), (2.4, 0.8, 2.4), 'crown')
    B('censer', (x - 4.8, 60.4, z), (2.0, 2.8, 2.0), 'core')
    r.bone('sigil_hand', 'left_hand', (9.6, 22, -3))   # an open grimoire hovering over the left palm
    B('sigil_hand', (9.6, 19.8, -4.0), (5.2, 0.6, 3.8), 'belt', rot=(-20, 0, 0))
    B('sigil_hand', (8.4, 20.4, -4.2), (2.4, 0.4, 3.4), 'mask', rot=(-20, 0, 8)); B('sigil_hand', (10.8, 20.4, -4.2), (2.4, 0.4, 3.4), 'mask', rot=(-20, 0, -8))
    B('sigil_hand', (9.6, 23.0, -4.6), (2.4, 2.4, 0.3), 'rune', rot=(0, 0, 45))
    r.bone('sigils', 'root', (0, 46, 10))
    for k in range(4):
        a = 2 * math.pi * k / 4 + 0.4; px, py = math.sin(a) * 10, 46 + math.cos(a) * 10
        B('sigils', (px, py, 10.4), (3.8, 3.8, 0.4), 'rune', rot=(0, 0, 45 + math.degrees(a))); B('sigils', (px, py, 10.3), (1.6, 1.6, 0.4), 'core')
    cape(r, ((13, 15), (12, 16), (11, 17)), z=5.8)
    return r

PAL = {
 'general_blade': dict(who='general_blade', k=2, ts=[512, 512], boss=True, pal=dict(base='#4A4A52', dark='#16141A', accent='#8A8A96', robe='#3A1C1C', cape='#5A1414'),
                       trim='#B88A3A', glow='#FF5A3A', eye='#FFB070', core='#FF7A40', orb='#6A5A40', crystal='#C0C0CC', eyes='visor'),
 'general_arcanist': dict(who='general_arcanist', k=2, ts=[512, 512], boss=True, pal=dict(base='#3E4244', dark='#101214', accent='#6A7476', robe='#2A3436', cape='#1C2628'),
                       trim='#9AB8B0', glow='#5AF0C8', eye='#C8FFF0', core='#6AFFD0', orb='#3A4A46', crystal='#8AF8E0', eyes='visor'),
}

def arcanist_extra(out):
    """Arcanist volley: thrusts the grimoire hand forward and fans three ash shards (impact 0.6, matches RiftBoss volley)."""
    g = lambda bn: S.HG.get(bn, R0); ch, hold, A = S.ch, S.hold, S.A
    out['volley'] = A(1.1, {
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.4, [-30, 0, 30]), (0.6, [-78, 0, 18]), (0.8, [-76, 0, 18]), (1.1, g('left_arm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.4, [-50, 0, 0]), (0.6, [-6, 0, 0]), (1.1, g('left_forearm'))])},
        'chest': {'rotation': ch([(0, g('chest')), (0.4, [0, -18, 0]), (0.6, [10, 14, 0]), (1.1, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.6, [-4, 10, 0]), (1.1, g('head'))])},
        'right_arm': {'rotation': hold(g('right_arm'), 1.1)}, 'staff': {'rotation': hold(g('staff'), 1.1)},
        'sigil_hand': {'scale': ch([(0, [1, 1, 1]), (0.5, [1.2, 1.2, 1.2]), (0.65, [1.35, 1.35, 1.35]), (1.1, [1, 1, 1])])},
        'sigils': {'rotation': S.spin(1.1, 1, 2)}})
    return out

class Painter(S.Painter):
    """Arcanist: ash-teal velvet and tarnished silver, painted like the Hierophant's cloth/metal (folds, weave, torn hems)."""
    def style(s, F, role):
        if s.mob['who'] != 'general_arcanist': return super().style(F, role)
        k = s.k; e = max(1, k // 2)
        def each(fn_body):
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); ed = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy)
                        fn_body(fn, xx, yy, u, t, ed, fw, fh)
        if role in ('robe', 'sleeve', 'hood', 'cape', 'capehem'):
            base = {'robe': (66, 76, 78), 'sleeve': (60, 70, 72), 'hood': (40, 46, 50), 'cape': (34, 42, 46), 'capehem': (34, 42, 46)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                folds = max(1, fw // (3 * k))
                fold = 0.5 + 0.5 * math.sin(u * math.pi * 2 * folds) if fn not in ('up', 'down') else 0.5
                kk = s.shade(fn, t, u) * (0.78 + 0.34 * fold)
                c = G.ramp(G.mix(G.mul(base, 0.7), G.mul(base, 1.25), s.n(xx, yy, 2 * k)), kk)
                if (xx + yy) % 2 == 0: c = G.mul(c, 0.94)
                a = 255
                if role == 'capehem' and fn not in ('up', 'down') and t > 0.4:
                    if s.n(xx, yy, k) > 0.25 + 1.1 * (1 - t): a = 0
                    elif t > 0.75: c = G.mul(c, 0.62)
                if role in ('robe', 'sleeve') and fn not in ('up', 'down') and t > 0.88: c = G.mix(c, (120, 150, 146), 0.5)   # pale hem band
                s.px(xx, yy, c, a)
            each(f); return True
        if role in ('collar', 'crown', 'guard'):
            met = (150, 166, 160)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                c = G.ramp(G.mix(G.mul(met, 0.55), G.mul(met, 1.1), s.n(xx, yy, k)), kk)
                if ed < e: c = G.mul(c, 0.5)
                elif role == 'collar' and fw > 3 * k and fh > 3 * k:
                    cx, cy = (xx - F[fn][0]) % (3 * k) - 1.5 * k, (yy - F[fn][1]) % (3 * k) - 1.5 * k
                    if abs(abs(cx) + abs(cy) - k) < 0.7: c = G.mul(c, 0.55)
                    elif abs(cx) + abs(cy) < 0.6 * k: c = (150, 255, 220); s.gl(xx, yy, s.gc, 0.6)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'mask':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.0 - 0.25 * abs(u - 0.5))
                c = G.ramp(G.mix((150, 150, 144), (212, 210, 200), 0.5 + 0.4 * s.n(xx, yy, 2 * k)), kk)
                if ed < e: c = G.mul(c, 0.65)
                s.px(xx, yy, c)
            each(f); return True
        return super().style(F, role)

def build(name):
    S.PAL.update(PAL)
    m = PAL[name]; rig = {'general_blade': rig_blade, 'general_arcanist': rig_arcanist}[name]()
    m = dict(m, ts=list(m['ts'])); uvs = G.pack(rig, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, 256]); huvs = G.pack(rig, hm['k'], hm['ts']); pt = Painter(name, hm)
    cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rig, pt, m['k'], huvs, uvs); G.geo(name, rig, m['k'], m['ts'], uvs)
    an = S.hierophant_anims()
    if name == 'general_arcanist': arcanist_extra(an)
    G.write_anims(name, an)
    S.bake_freezes(name)
    print(name, len(rig.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    for n in sys.argv[1:] or ['general_blade', 'general_arcanist']: build(n)

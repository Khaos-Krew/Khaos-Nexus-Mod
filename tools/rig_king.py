"""1.2 final boss: Morvane, the Hollow King, crowned lord of the Underthrone. Built on the Hierophant bone layout (his
authored, clip-checked set drives the King) plus the Arcanist volley and a new 'crownfall' special.
Design: tarnished-gold plate over a HOLLOW chest (split breastplate, cage of ribs, a burning heart-ember hung inside);
crimson velvet robes in three ragged tiers; ermine mantle; layered royal pauldrons; gaunt crowned helm with a pale
burning gaze; an intact, towering Hollow Crown floating above with its gem sockets empty and glowing; the Kingsbane
greatsword with a hanging royal seal; an orb of dominion in the off hand; three crown shards orbiting his back; a long
crimson train trimmed in ermine. Run: python3 rig_king.py"""
import math, random
import gen_geo_assets as G
import rigs_saga2 as S
import rigs_generals as RG
Rig, taper, shard = G.Rig, S.taper, S.shard
R0 = [0, 0, 0]

def rig():
    """v2: heroic proportions. Long armoured legs (hip at 30), tapered waist, broad chest and pauldrons, small helm."""
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 3.5
        lg = r.bone(f'{side}_leg', 'root', (x, 30, 0))
        taper(r, lg, x, 0, 30, 16, 5.0, 4.4, 5.2, 4.6, 3, 'plate')
        for j in range(3):   # layered tassets hung from the hip, splayed outward
            B(lg, (x + s * 0.6, 28.6 - j * 2.6, -0.2), (5.8 - j * 0.3, 3.0, 6.0 - j * 0.3), 'breastplate', rot=(0, 0, s * (8 + j * 3)), pivot=(x, 30, 0))
        B(lg, (x, 16.2, -2.6), (3.6, 3.2, 1.2), 'breastplate'); B(lg, (x, 16.2, -3.3), (1.6, 1.6, 0.4), 'crown', rot=(0, 0, 45))   # knee cop
        B(lg, (x + s * 2.5, 16.2, -0.6), (0.8, 2.6, 3.0), 'breastplate')   # cop wing
        sh = r.bone(f'{side}_shin', lg, (x, 15, 0))
        taper(r, sh, x, 0, 15, 2.2, 4.4, 3.6, 4.6, 3.8, 3, 'plate')
        B(sh, (x, 9.5, -2.3), (3.4, 9.0, 0.8), 'breastplate'); B(sh, (x, 9.5, -2.75), (0.6, 8.4, 0.2), 'crown')   # greave ridge
        B(sh, (x, 1.1, -1.4), (4.6, 2.2, 7.4), 'gauntlet'); B(sh, (x, 1.9, -4.6), (3.6, 1.4, 1.6), 'breastplate')   # sabaton
        B(sh, (x, 0.8, -5.6), (2.6, 1.0, 1.0), 'breastplate')
    r.bone('hips', 'root', (0, 30, 0))
    B('hips', (0, 30.2, 0), (11.6, 3.0, 8.4), 'belt'); B('hips', (0, 30.2, -4.4), (3.6, 3.6, 0.8), 'crown', rot=(0, 0, 45))
    B('hips', (0, 30.2, -4.85), (1.8, 1.8, 0.3), 'core', rot=(0, 0, 45))
    for s in (-1, 1): B('hips', (s * 5.9, 29.4, 0), (1.0, 3.6, 7.0), 'breastplate', rot=(0, 0, s * 6))
    # robes: long tattered back panel in two hinged pieces + a front tabard falling between the legs
    r.bone('robe_back', 'hips', (0, 30, 4.2))
    B('robe_back', (0, 23.0, 4.6), (10.4, 14.0, 0.8), 'robe', rot=(8, 0, 0), pivot=(0, 30, 4.2))
    for s in (-1, 1): B('robe_back', (s * 4.6, 23.0, 4.75), (1.2, 13.6, 0.9), 'crown', rot=(8, 0, 0), pivot=(0, 30, 4.2))
    r.bone('robe_back2', 'robe_back', (0, 16.2, 6.1))
    B('robe_back2', (0, 9.6, 6.4), (11.0, 13.0, 0.8), 'capehem', rot=(10, 0, 0), pivot=(0, 16.2, 6.1))
    r.bone('tabard', 'hips', (0, 29, -4.4))
    B('tabard', (0, 19.6, -4.7), (5.2, 18.6, 0.6), 'robe'); B('tabard', (0, 19.6, -5.05), (1.0, 17.6, 0.2), 'crown')
    B('tabard', (0, 23.0, -5.2), (3.2, 3.2, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 9.2, -4.7), (4.8, 2.6, 0.6), 'capehem')
    r.bone('orb', 'hips', (8.6, 27.0, -1.0))   # orb of dominion hovering at his left hip while he wields Kingsbane
    B('orb', (8.6, 27.0, -1.0), (3.6, 3.6, 3.6), 'orb'); B('orb', (8.6, 27.0, -1.0), (2.2, 2.2, 2.2), 'core')
    B('orb', (8.6, 29.4, -1.0), (0.8, 1.4, 0.8), 'crown'); B('orb', (8.6, 29.8, -1.0), (2.0, 0.6, 0.6), 'crown'); B('orb', (8.6, 27.0, -1.0), (4.6, 0.5, 4.6), 'crown')
    # torso: tapered waist up into a broad hollow chest
    r.bone('torso', 'hips', (0, 31.5, 0), (4, 0, 0))
    taper(r, 'torso', 0, 0, 31.5, 41, 10.4, 12.2, 7.4, 8.6, 3, 'plate')
    for j in range(3): B('torso', (0, 33.0 + j * 2.8, -3.9 - j * 0.15), (9.8 + j * 0.9, 2.6, 1.2), 'breastplate')
    r.bone('chest', 'torso', (0, 41, 0))
    taper(r, 'chest', 0, 2.0, 41, 54, 13.6, 19.6, 6.0, 7.4, 3, 'plate')   # back half (solid)
    B('chest', (0, 47.5, -1.4), (12.6, 11.6, 1.2), 'void')                 # the hollow
    for s in (-1, 1):
        B('chest', (s * 6.6, 47.6, -2.6), (5.8, 12.6, 3.6), 'breastplate', rot=(0, s * -10, s * 3), pivot=(s * 9.4, 47.6, -2.6))
        B('chest', (s * 3.9, 47.6, -4.5), (0.6, 12.0, 0.6), 'crown')
        B('chest', (s * 7.4, 53.0, -3.6), (5.0, 0.6, 0.6), 'crown', rot=(0, 0, s * -10))
        for j in range(5):
            y = 52.4 - j * 2.3
            B('chest', (s * 2.4, y, -3.4), (3.8 - j * 0.25, 0.9, 0.9), 'rib', rot=(0, s * 14, s * -10))
    B('chest', (0, 47.6, -3.6), (0.9, 11.0, 0.9), 'rib')                     # sternum
    r.bone('heart', 'chest', (0, 47, -1.6))
    B('heart', (0, 47, -1.8), (3.4, 3.8, 2.6), 'core', rot=(0, 45, 0)); B('heart', (0, 47, -1.8), (4.6, 4.8, 0.4), 'rune', rot=(0, 0, 45))
    B('heart', (0, 50.2, -1.6), (0.4, 2.2, 0.4), 'guard')
    B('chest', (0, 54.4, 0.2), (10.4, 2.4, 9.0), 'guard')                    # gorget
    for s in (-1, 1): B('chest', (s * 6.8, 55.0, 0.6), (8.4, 3.4, 11.4), 'fur', rot=(0, 0, s * 12))   # ermine mantle
    B('chest', (0, 55.8, 3.8), (13.4, 3.8, 4.6), 'fur')
    # head: smaller gaunt helm (heroic scale), crown hovering ~1.5px above the circlet
    r.bone('head', 'chest', (0, 55, -0.8))
    B('head', (0, 59.6, -0.6), (7.6, 8.6, 8.2), 'helm'); B('head', (0, 64.1, -0.6), (6.8, 0.8, 7.4), 'helm')
    B('head', (0, 59.0, -4.75), (6.0, 7.0, 0.6), 'mask')
    for s in (-1, 1):
        B('head', (s * 1.5, 60.4, -5.1), (1.9, 1.0, 0.3), 'void'); B('head', (s * 1.5, 60.4, -5.25), (1.2, 0.5, 0.2), 'core')
        B('head', (s * 4.0, 58.6, -1.4), (0.8, 5.6, 5.6), 'breastplate'); B('head', (s * 2.7, 55.8, -4.9), (1.8, 2.2, 0.8), 'visorplate')
        B('head', (s * 4.3, 61.8, -0.6), (0.6, 2.4, 0.6), 'crown', rot=(0, 0, s * -20))
    B('head', (0, 57.0, -5.15), (3.0, 0.5, 0.3), 'void'); B('head', (0, 59.6, -5.3), (7.2, 0.5, 0.3), 'crown')
    B('head', (0, 64.6, -0.6), (8.2, 1.0, 8.8), 'crown')                      # circlet band (top 65.1)
    r.bone('mitre0', 'head', (0, 67.4, -0.6))
    for k in range(10):
        a = 2 * math.pi * k / 10; px, pz = math.sin(a) * 4.6, -0.6 + math.cos(a) * 4.6
        B('mitre0', (px, 67.4, pz), (3.2, 1.8, 0.9) if abs(math.cos(a)) > 0.6 else (0.9, 1.8, 3.2), 'crown', rot=(0, math.degrees(a), 0))
        ln = 4.6 if k % 2 == 0 else 2.6
        B('mitre0', (px, 68.3 + ln / 2, pz), (1.0, ln, 1.0), 'crown', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 68.3, pz))
        B('mitre0', (px, 68.3 + ln + 0.5, pz), (1.3 if k % 2 == 0 else 1.0,) * 3, 'core', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 68.3, pz))
        B('mitre0', (px * 1.04, 67.4, pz * 1.04 - 0.02), (0.9, 0.9, 0.9), 'core', rot=(45, 45, 0))
    r.bone('mitre1', 'mitre0', (0, 67.4, -0.6))
    B('mitre1', (0, 70.2, -0.6), (1.8, 1.8, 1.8), 'core', rot=(45, 45, 0))
    # arms: broad four-tier pauldrons, gauntlets; the right hand closes in a fist around Kingsbane's grip
    for side, s in (('right', -1), ('left', 1)):
        x = s * 11.6
        arm = r.bone(f'{side}_arm', 'chest', (x, 52.5, 0))
        for j in range(4): B(arm, (x + s * 0.5 * j, 54.0 - j * 2.0, 0.2), (10.4 - j * 0.9, 2.8, 10.2 - j * 0.6), 'pauldron', rot=(0, 0, s * (12 + j * 7)))
        for j in range(4): B(arm, (x + s * (0.5 * j + 0.2), 54.0 - j * 2.0, -5.15 + j * 0.3), (9.4 - j * 0.9, 0.5, 0.3), 'crown', rot=(0, 0, s * (12 + j * 7)))
        for j in range(2): B(arm, (x + s * (2.4 + j * 1.8), 57.2 + j * 0.4, 0.2), (1.2, 4.6 - j * 1.2, 1.2), 'crown', rot=(0, 0, s * -22))
        taper(r, arm, x, 0, 50, 43, 4.8, 4.6, 4.8, 4.6, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 43, 0))
        B(fa, (x, 42.6, 1.0), (4.8, 3.4, 4.0), 'breastplate'); taper(r, fa, x, 0, 42, 35.5, 4.6, 5.6, 4.6, 5.6, 2, 'plate')
        B(fa, (x, 35.6, 0), (6.4, 1.4, 6.4), 'crown'); B(fa, (x, 39.0, -2.9), (2.6, 4.0, 0.3), 'crown')
        hd = r.bone(f'{side}_hand', fa, (x, 34.5, -0.4))
        B(hd, (x, 33.0, 0.6), (3.8, 3.4, 2.6), 'gauntlet')                     # back of the fist
        for f in range(4): B(hd, (x, 34.0 - f * 0.85, -1.4), (3.9, 0.75, 1.6), 'gauntlet')   # curled fingers in front of the grip
        B(hd, (x - s * 2.0, 32.6, -0.8), (0.9, 2.2, 1.6), 'gauntlet')          # thumb
        B(hd, (x, 34.4, 0.4), (4.2, 0.6, 3.6), 'crown')
    # Kingsbane: a long two-handed royal greatsword, grip running through the right fist
    x, z = -11.6, -0.4
    r.bone('staff', 'right_hand', (x, 32.6, z))
    B('staff', (x, 21.4, z), (2.8, 2.8, 2.8), 'core', rot=(45, 0, 45)); B('staff', (x, 21.4, z), (3.6, 1.0, 3.6), 'crown', rot=(0, 45, 0))
    taper(r, 'staff', x, z, 22.6, 37.6, 1.5, 1.5, 1.5, 1.5, 4, 'grip')
    for y in (25, 29, 33): B('staff', (x, y, z), (1.9, 0.5, 1.9), 'crown')
    B('staff', (x, 38.4, z), (2.6, 2.0, 13.6), 'crown')
    for sz_ in (-1, 1):
        B('staff', (x, 40.0, z + sz_ * 7.2), (2.0, 4.0, 1.6), 'crown', rot=(sz_ * 26, 0, 0))
        B('staff', (x, 42.2, z + sz_ * 8.0), (1.2, 1.2, 1.2), 'core', rot=(45, 45, 0))
    B('staff', (x, 40.2, z), (3.2, 3.2, 5.0), 'crown'); B('staff', (x, 40.2, z - 2.7), (1.6, 1.6, 0.4), 'core', rot=(0, 0, 45))
    taper(r, 'staff', x, z, 41.5, 92, 1.4, 1.0, 6.8, 5.0, 6, 'greatblade')
    B('staff', (x, 95.4, z), (1.0, 7, 3.4), 'greatblade'); B('staff', (x, 99.6, z), (0.8, 2.6, 1.4), 'greatblade')
    B('staff', (x, 66, z), (1.55, 48, 1.2), 'rune')
    for y in (46, 56, 66, 76, 86): B('staff', (x, y, z - 0.0), (1.6, 0.6, 2.6), 'crown')   # filigree bands across the fuller
    r.bone('censer', 'staff', (x, 38.4, z - 6.8))   # royal seal on a chain from the crossguard
    cz = z - 6.8
    for j in range(3): B('censer', (x, 37.0 - j * 1.9, cz), (0.6, 1.6, 0.6), 'guard', rot=(0, 45 * j, 0))
    B('censer', (x, 31.4, cz), (3.6, 3.6, 0.8), 'crown'); B('censer', (x, 31.4, cz - 0.45), (2.2, 2.2, 0.2), 'core', rot=(0, 0, 45))
    r.bone('sigil_hand', 'left_hand', (11.6, 30, -2))   # a crown rune burning above the left fist
    B('sigil_hand', (11.6, 29.0, -2.4), (3.0, 3.0, 0.3), 'rune', rot=(0, 0, 45)); B('sigil_hand', (11.6, 29.0, -2.5), (1.4, 1.4, 0.3), 'core', rot=(0, 0, 45))
    r.bone('sigils', 'root', (0, 54, 10))
    for k in range(3):
        a = 2 * math.pi * k / 3; px, py = math.sin(a) * 11, 54 + math.cos(a) * 11
        B('sigils', (px, py, 11.2), (2.0, 6.0, 1.0), 'crown', rot=(0, 0, math.degrees(-a))); B('sigils', (px, py, 10.9), (1.6, 1.6, 1.6), 'core', rot=(45, 45, 0))
    # cape: five hinged segments, widening, ermine-edged, the last two torn
    prev, y = 'chest', 54.5
    for j, (ln, w) in enumerate(((11, 20), (11, 21.5), (11, 23), (10, 24), (9, 25))):
        bn = r.bone(f'cape{j + 1}', prev, (0, y, 5.8), (5, 0, 0) if j == 0 else (0, 0, 0))
        B(bn, (0, y - ln / 2, 6.0), (w, ln, 0.9), 'cape' if j < 3 else 'capehem')
        for s in (-1, 1): B(bn, (s * (w / 2 - 0.6), y - ln / 2, 6.25), (1.4, ln, 1.1), 'fur')
        if j == 1: B(bn, (0, y - ln / 2, 6.6), (5.0, 5.0, 0.3), 'sigil', rot=(0, 0, 45))
        if j == 2: B(bn, (0, y - ln / 2, 6.55), (0.8, ln, 0.2), 'crown')
        prev, y = bn, y - ln
    return r

PAL = {'hollow_king': dict(who='hollow_king', k=2, ts=[512, 512], boss=True,
       pal=dict(base='#3A3A44', dark='#0E0C12', accent='#6E6A78', robe='#5E0E1E', cape='#4A0A18'),
       trim='#D8B04A', glow='#FFD06A', eye='#FFF6D8', core='#FFE29A', orb='#6A1A2A', crystal='#F0E0B0', eyes='visor')}

class Painter(RG.Painter):
    def style(s, F, role):
        if s.mob['who'] != 'hollow_king': return super().style(F, role)
        k = s.k
        def each(fb):
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); ed = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy)
                        fb(fn, xx, yy, u, t, ed, fw, fh)
        if role == 'fur':      # ermine: soft white tufts, black tail-tip spots
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((198, 192, 182), (246, 242, 234), s.n(xx, yy, max(1, k // 2)))
                c = G.ramp(c, s.shade(fn, t, u) * (0.9 + 0.2 * s.n(xx, yy, 1)))
                gx, gy = (xx - F[fn][0]) % (5 * k), (yy - F[fn][1]) % (6 * k)
                if fw > 3 * k and abs(gx - 2.5 * k) < 0.6 * k and abs(gy - 3 * k) < 1.2 * k: c = (24, 20, 20)
                s.px(xx, yy, c)
            each(f); return True
        if role in ('plate', 'breastplate', 'pauldron', 'helm', 'gauntlet', 'visorplate'):   # blackened steel, gold filigree
            e = max(1, k // 2)
            base = {'plate': (54, 54, 64), 'breastplate': (66, 64, 76), 'pauldron': (62, 60, 72), 'helm': (58, 56, 68), 'gauntlet': (48, 48, 56), 'visorplate': (66, 64, 76)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.06 - 0.18 * t if fn not in ('up', 'down') else 1.0)
                c = G.ramp(G.mix(G.mul(base, 0.7), G.mul(base, 1.3), s.n(xx, yy, 2 * k)), kk)
                if (xx * 7 + yy * 3) % 23 == 0: c = G.mul(c, 1.25)   # fine scratches
                big = fw > 4 * k and fh > 4 * k
                if ed < e: c = G.mul(c, 0.45)
                elif role in ('breastplate', 'pauldron') and ed < e + max(1, k // 2) and fw > 4 * k and fh > 4 * k: c = G.ramp((214, 170, 78), kk)   # gilded edge
                elif big and role in ('breastplate', 'pauldron', 'helm'):
                    lx, ly = (xx - F[fn][0]) / k, (yy - F[fn][1]) / k
                    w = math.sin(lx * 1.3 + math.sin(ly * 0.9) * 1.6) * math.cos(ly * 1.1)
                    if abs(w) < 0.07 and ed > 2 * e: c = G.ramp((190, 150, 66), kk * 0.95)   # filigree scrollwork
                s.px(xx, yy, c)
            each(f); return True
        if role == 'rib':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.ramp(G.mix((176, 166, 146), (226, 218, 198), s.n(xx, yy, 1)), s.shade(fn, t, u))
                if ed < 1: c = G.mul(c, 0.6)
                s.px(xx, yy, c); s.gl(xx, yy, s.gc, 0.12)
            each(f); return True
        if role in ('crown', 'core'):   # tarnished royal gold with dark wear in the recesses; cores burn pale gold
            e = max(1, k // 2)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                if role == 'core':
                    c = G.mix((255, 214, 120), (255, 248, 220), 0.5 + 0.5 * (1 - abs(u - 0.5) * 2) * (1 - abs(t - 0.5) * 2))
                    s.px(xx, yy, c); s.gl(xx, yy, s.gc, 0.9); return
                c = G.ramp(G.mix((150, 108, 40), (236, 192, 92), s.n(xx, yy, k)), kk)
                if ed < e: c = G.mul(c, 0.55)
                elif s.n(xx * 3, yy * 3, 1) > 0.82: c = G.mix(c, (70, 60, 40), 0.5)   # tarnish
                s.px(xx, yy, c)
            each(f); return True
        if role in ('robe', 'cape', 'capehem'):   # crimson velvet: deep folds, nap sheen, gold embroidered hem
            base = {'robe': (110, 22, 36), 'cape': (92, 16, 30), 'capehem': (92, 16, 30)}[role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                folds = max(1, fw // (3 * k))
                fold = 0.5 + 0.5 * math.sin(u * math.pi * 2 * folds) if fn not in ('up', 'down') else 0.5
                kk = s.shade(fn, t, u) * (0.72 + 0.42 * fold)
                c = G.ramp(G.mix(G.mul(base, 0.65), G.mul(base, 1.3), s.n(xx, yy, 2 * k)), kk)
                a = 255
                if role == 'capehem' and fn not in ('up', 'down') and t > 0.45:
                    if s.n(xx, yy, k) > 0.28 + 1.1 * (1 - t): a = 0
                    elif t > 0.78: c = G.mul(c, 0.6)
                if role == 'robe' and fn not in ('up', 'down') and 0.84 < t < 0.95:
                    c = G.mix(c, (216, 176, 74), 0.75)
                    if (xx // k) % 3 == 0: c = G.mul(c, 0.7)
                s.px(xx, yy, c, a)
            each(f); return True
        return super().style(F, role)

SX = 222
def anims():
    an = S.hierophant_anims(); RG.arcanist_extra(an)
    ch, hold, A, spin = S.ch, S.hold, S.A, S.spin
    g = lambda bn: S.HG.get(bn, R0)
    # CROWNFALL: raises Kingsbane high in both hands while the Hollow Crown rises and spins, then drives it into the floor
    # (impact 0.9). Shards rain where hunters stand (RiftBoss/HollowKing time the falls off the impact).
    T = 1.6
    K = [0, -8, -1.5]
    an['crownfall'] = A(T, {
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.5, [-160, 0, -6]), (0.7, [-168, 0, -4]), (0.9, [-72, 0, -26]), (1.25, [-72, 0, -26]), (T, g('right_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.5, [-14, 0, 0]), (0.9, [-15, 0, 0]), (1.25, [-15, 0, 0]), (T, g('right_forearm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.5, [-150, 0, 18]), (0.7, [-158, 0, 16]), (0.9, [-70, 0, 22]), (1.25, [-70, 0, 22]), (T, g('left_arm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.5, [-20, 0, 0]), (0.9, [-20, 0, 0]), (T, g('left_forearm'))])},
        'staff': {'rotation': ch([(0, g('staff')), (0.5, [175, 0, 8]), (0.7, [178, 0, 6]), (0.9, [SX, 0, -12]), (1.25, [SX, 0, -12]), (T, g('staff'))])},
        'chest': {'rotation': ch([(0, g('chest')), (0.5, [-14, 0, 0]), (0.9, [26, 0, 0]), (1.25, [24, 0, 0]), (T, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.5, [-22, 0, 0]), (0.9, [-14, 0, 0]), (T, g('head'))])},
        'right_leg': {'rotation': ch([(0, R0), (0.5, [-10, 0, 0]), (0.9, [-62, 0, 0]), (1.25, [-62, 0, 0]), (T, R0)])},
        'right_shin': {'rotation': ch([(0, R0), (0.9, [62, 0, 0]), (1.25, [62, 0, 0]), (T, R0)])},
        'left_leg': {'rotation': ch([(0, R0), (0.5, [6, 0, 0]), (0.9, [28, 0, 0]), (1.25, [28, 0, 0]), (T, R0)])},
        'left_shin': {'rotation': ch([(0, R0), (0.9, [74, 0, 0]), (1.25, [74, 0, 0]), (T, R0)])},
        'root': {'position': ch([(0, R0), (0.5, [0, 1.5, 0]), (0.9, K), (1.25, K), (T, R0)])},
        'mitre0': {'position': ch([(0, R0), (0.6, [0, 4, 0]), (0.9, [0, 1, 0]), (T, R0)]), 'rotation': spin(T, 1, 1)},
        'sigils': {'rotation': spin(T, 2, 2)}, 'censer': {'rotation': ch([(0, R0), (0.9, [40, 0, 0]), (1.25, [-20, 0, 0]), (T, R0)])}})
    for nm in ('attack', 'roar'):   # the floating crown drifts up out of the swing's arc
        L = an[nm]['length']; mb = an[nm]['bones'].setdefault('mitre0', {})
        mb['position'] = ch([(0, R0), (0.18, [0, -3.5, 0]), (L - 0.2, [0, -3.5, 0]), (L, R0)])
    for n in ('cape1', 'cape2', 'cape3', 'cape4', 'cape5'):
        an['crownfall']['bones'][n] = {'rotation': ch([(0, R0), (0.9, [-14, 0, 0]), (1.2, [10, 0, 0]), (T, R0)])}
    for nm, a_ in an.items():   # the train flows: segments 4-5 follow segment 3 (compounding), the orb bobs
        b = a_['bones']
        if 'cape3' in b and nm != 'crownfall':
            b.setdefault('cape4', b['cape3']); b.setdefault('cape5', b['cape3'])
        elif nm not in ('crownfall', 'death'):
            L = a_['length']
            b.setdefault('cape4', {'rotation': S.loop(L, 6, 0, phase=0.6)}); b.setdefault('cape5', {'rotation': S.loop(L, 8, 0, phase=1.2)})
        if nm != 'death': b.setdefault('orb', {'position': S.loop(a_['length'], 0.8, 1), 'rotation': spin(a_['length'], 1, 1)})
    return an

def build():
    name = 'hollow_king'; S.PAL.update(PAL)
    m = dict(PAL[name], ts=list(PAL[name]['ts'])); rg = rig(); uvs = G.pack(rg, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, m['ts'][1] * 4]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = Painter(name, hm)
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rg, pt, m['k'], huvs, uvs); G.geo(name, rg, m['k'], m['ts'], uvs)
    G.write_anims(name, anims())
    S.FREEZE['crownfall'] = 0.9
    S.bake_freezes(name)
    print(name, len(rg.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    build()

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
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0)); RG.legs(r, True)
    for side, s in (('right', -1), ('left', 1)):   # gilded knee crowns
        B(f'{side}_leg', (s * 3.6, 12.8, -2.9), (2.4, 1.2, 0.6), 'crown')
    B('hips', (0, 22, 0), (15.4, 3.6, 10.4), 'belt'); B('hips', (0, 22, -5.4), (3.6, 3.6, 0.8), 'crown', rot=(0, 0, 45))
    B('hips', (0, 22, -5.85), (1.6, 1.6, 0.3), 'core', rot=(0, 0, 45))
    RG.skirt(r, ((12, 7.8, 5.6, 22.6, 6.0, 9, 'plate'), (14, 8.4, 6.0, 17.2, 8.4, 11, 'robe'), (16, 9.1, 6.6, 9.6, 7.4, 14, 'capehem')), 41)
    r.bone('tabard', 'hips', (0, 21, -5.6))
    B('tabard', (0, 12.6, -6.0), (6.0, 16.6, 0.6), 'robe'); B('tabard', (0, 12.6, -6.35), (1.2, 15.4, 0.2), 'crown')
    B('tabard', (0, 15.4, -6.5), (3.4, 3.4, 0.4), 'sigil', rot=(0, 0, 45)); B('tabard', (0, 3.6, -6.0), (5.6, 2.4, 0.6), 'capehem')
    # torso: plackart + hollow chest
    r.bone('torso', 'hips', (0, 22.5, 0), (6, 0, 0))
    taper(r, 'torso', 0, 0, 22.5, 34, 12.6, 14.8, 8.8, 10.0, 2, 'plate')
    for j in range(3): B('torso', (0, 24.0 + j * 3.4, -4.8 - j * 0.12), (13.6 + j * 0.6, 3.0, 1.2), 'breastplate')
    r.bone('chest', 'torso', (0, 34, 0))
    B('chest', (0, 40, 2.2), (17.0, 12.0, 6.6), 'plate')                      # back half (solid)
    B('chest', (0, 40, -1.6), (12.0, 11.0, 1.2), 'void')                      # the hollow
    for s in (-1, 1):                                                       # split breastplate halves leave the cage open
        B('chest', (s * 5.9, 40, -2.6), (5.2, 12.0, 3.4), 'breastplate', rot=(0, s * -8, 0), pivot=(s * 8.5, 40, -2.6))
        B('chest', (s * 3.4, 40, -4.5), (0.6, 11.4, 0.6), 'crown')
        for j in range(4):                                                  # ribs arcing in from each half
            y = 44.6 - j * 2.6
            B('chest', (s * 2.4, y, -3.6), (3.6, 0.9, 0.9), 'rib', rot=(0, s * 14, s * -12))
    B('chest', (0, 46.2, -0.2), (12.4, 2.6, 10.6), 'guard')                  # gorget
    r.bone('heart', 'chest', (0, 40, -1.4))                                 # burning heart-ember hung in the cage
    B('heart', (0, 40, -1.6), (3.2, 3.6, 2.6), 'core', rot=(0, 45, 0)); B('heart', (0, 43.0, -1.4), (0.4, 2.4, 0.4), 'guard')
    for s in (-1, 1):   # ermine mantle over the shoulders
        B('chest', (s * 6.4, 47.4, 0.4), (7.6, 3.4, 12.6), 'fur', rot=(0, 0, s * 10))
    B('chest', (0, 48.2, 3.8), (13.0, 3.6, 5.0), 'fur')
    # head: gaunt crowned helm
    r.bone('head', 'chest', (0, 46.5, -1.6))
    B('head', (0, 52.0, -0.4), (9.0, 10.0, 9.6), 'helm'); B('head', (0, 57.2, -0.4), (8.0, 1.2, 8.4), 'helm')
    B('head', (0, 51.4, -5.0), (7.0, 8.0, 0.6), 'mask')
    for s in (-1, 1):
        B('head', (s * 1.8, 53.0, -5.4), (2.2, 1.1, 0.3), 'void'); B('head', (s * 1.8, 53.0, -5.55), (1.4, 0.6, 0.2), 'core')
        B('head', (s * 4.8, 50.6, -1.8), (0.8, 6.4, 6.4), 'plate'); B('head', (s * 3.2, 47.4, -5.2), (2.2, 2.6, 0.8), 'visorplate')
    B('head', (0, 49.0, -5.45), (3.6, 0.6, 0.3), 'void')                     # grim mouth slit
    B('head', (0, 52.0, -5.6), (8.6, 0.5, 0.3), 'crown')                    # circlet brow
    B('head', (0, 57.8, -0.4), (9.4, 1.4, 9.8), 'crown')                    # worn crown band on the helm
    # the Hollow Crown: an intact towering crown floating above, sockets empty and burning
    r.bone('mitre0', 'head', (0, 61.0, -0.4))
    for k in range(10):
        a = 2 * math.pi * k / 10; px, pz = math.sin(a) * 4.7, -0.4 + math.cos(a) * 4.7
        B('mitre0', (px, 61.2, pz), (3.8, 2.0, 1.0) if abs(math.cos(a)) > 0.6 else (1.0, 2.0, 3.8), 'crown', rot=(0, math.degrees(a), 0))
        ln = 4.4 if k % 2 == 0 else 2.8
        B('mitre0', (px, 62.2 + ln / 2, pz), (1.2, ln, 1.2), 'crown', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 62.2, pz))
        if k % 2 == 0:
            B('mitre0', (px, 62.2 + ln + 0.5, pz), (1.4, 1.4, 1.4), 'core', rot=(math.cos(a) * -12, 0, math.sin(a) * 12), pivot=(px, 62.2, pz))
        B('mitre0', (px * 1.04, 61.2, pz * 1.04 - 0.02), (1.0, 1.0, 1.0), 'core', rot=(45, 45, 0))   # empty socket, burning
    r.bone('mitre1', 'mitre0', (0, 61.0, -0.4))
    B('mitre1', (0, 64.6, -0.4), (2.2, 2.2, 2.2), 'core', rot=(45, 45, 0))   # the hollow at its heart
    # arms
    for side, s in (('right', -1), ('left', 1)):
        x = s * 10.6
        arm = r.bone(f'{side}_arm', 'chest', (x, 45, 0))
        for j in range(4): B(arm, (x + s * 0.4 * j, 46.4 - j * 2.2, 0), (8.8 - j * 0.7, 3.0, 9.0 - j * 0.5), 'pauldron', rot=(0, 0, s * (10 + j * 6)))
        B(arm, (x + s * 0.4, 47.6, -4.4), (6.6, 0.8, 0.5), 'crown', rot=(0, 0, s * 10))
        for j in range(2): B(arm, (x + s * (2.2 + j * 1.6), 49.4 + j * 0.6, 0), (1.2, 4.4 - j, 1.2), 'crown', rot=(0, 0, s * -20))
        taper(r, arm, x, 0, 42, 36, 5.2, 5.0, 5.2, 5.0, 2, 'plate')
        fa = r.bone(f'{side}_forearm', arm, (x, 36, 0))
        B(fa, (x, 35.6, 1.0), (5.0, 3.6, 4.0), 'guard'); taper(r, fa, x, 0, 35, 28.5, 5.0, 6.0, 5.0, 6.0, 2, 'plate')
        B(fa, (x, 28.6, 0), (6.6, 1.4, 6.6), 'crown')
        hd = r.bone(f'{side}_hand', fa, (x, 27.5, -0.4))
        B(hd, (x, 25.6, -0.4), (3.8, 3.8, 3.8), 'gauntlet'); B(hd, (x, 23.6, -1.4), (3.6, 1.6, 2.0), 'gauntlet')
    # Kingsbane: royal greatsword on the 'staff' bone
    x, z = -10.6, -0.6
    r.bone('staff', 'right_hand', (x, 25.6, z))
    B('staff', (x, 17.2, z), (2.8, 2.8, 2.8), 'core', rot=(45, 0, 45)); taper(r, 'staff', x, z, 18.4, 31.4, 1.7, 1.7, 1.7, 1.7, 3, 'grip')
    B('staff', (x, 32.2, z), (2.6, 2.0, 12.0), 'crown')
    for sz_ in (-1, 1): B('staff', (x, 33.6, z + sz_ * 6.4), (2.0, 3.6, 1.6), 'crown', rot=(sz_ * 24, 0, 0))
    B('staff', (x, 34.0, z), (3.0, 2.8, 4.6), 'crown'); B('staff', (x, 34.0, z - 2.5), (1.4, 1.4, 0.4), 'core', rot=(0, 0, 45))
    taper(r, 'staff', x, z, 35, 78, 1.3, 1.0, 5.6, 4.0, 5, 'greatblade')
    B('staff', (x, 81.0, z), (0.9, 6, 2.8), 'greatblade'); B('staff', (x, 84.8, z), (0.7, 2.4, 1.2), 'greatblade')
    B('staff', (x, 56, z), (1.5, 38, 1.0), 'rune')
    r.bone('censer', 'staff', (x, 32.2, z - 6.0))      # the royal seal on a chain from the crossguard
    cz = z - 6.0
    for j in range(3): B('censer', (x, 30.8 - j * 1.9, cz), (0.6, 1.6, 0.6), 'guard', rot=(0, 45 * j, 0))
    B('censer', (x, 25.2, cz), (3.6, 3.6, 0.8), 'crown'); B('censer', (x, 25.2, cz - 0.45), (2.2, 2.2, 0.2), 'core', rot=(0, 0, 45))
    r.bone('sigil_hand', 'left_hand', (10.6, 22, -3))     # orb of dominion above the left palm
    B('sigil_hand', (10.6, 20.4, -3.6), (3.6, 3.6, 3.6), 'orb'); B('sigil_hand', (10.6, 20.4, -3.6), (2.2, 2.2, 2.2), 'core')
    B('sigil_hand', (10.6, 22.8, -3.6), (0.8, 2.0, 0.8), 'crown'); B('sigil_hand', (10.6, 23.2, -3.6), (2.0, 0.6, 0.6), 'crown')
    r.bone('sigils', 'root', (0, 50, 9))
    for k in range(3):   # three crown shards orbiting his back
        a = 2 * math.pi * k / 3; px, py = math.sin(a) * 9.5, 50 + math.cos(a) * 9.5
        B('sigils', (px, py, 10.6), (2.0, 5.6, 1.0), 'crown', rot=(0, 0, math.degrees(-a))); B('sigils', (px, py, 10.3), (1.6, 1.6, 1.6), 'core', rot=(45, 45, 0))
    # train: crimson velvet, ermine edged
    prev, y = 'chest', 45
    for j, (ln, w) in enumerate(((13, 19), (13, 20.5), (12, 22))):
        bn = r.bone(f'cape{j + 1}', prev, (0, y, 6.6), (6, 0, 0) if j == 0 else (0, 0, 0))
        B(bn, (0, y - ln / 2, 6.8), (w, ln, 1.0), 'cape' if j < 2 else 'capehem')
        for s in (-1, 1): B(bn, (s * (w / 2 - 0.6), y - ln / 2, 7.0), (1.4, ln, 1.2), 'fur')
        if j == 1: B(bn, (0, y - ln / 2, 7.4), (4.4, 4.4, 0.3), 'sigil', rot=(0, 0, 45))
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

def anims():
    an = S.hierophant_anims(); RG.arcanist_extra(an)
    ch, hold, A, spin = S.ch, S.hold, S.A, S.spin
    g = lambda bn: S.HG.get(bn, R0)
    # CROWNFALL: raises Kingsbane high in both hands while the Hollow Crown rises and spins, then drives it into the floor
    # (impact 0.9). Shards rain where hunters stand (RiftBoss/HollowKing time the falls off the impact).
    T = 1.6
    an['crownfall'] = A(T, {
        'right_arm': {'rotation': ch([(0, g('right_arm')), (0.5, [-160, 0, -6]), (0.75, [-168, 0, -4]), (0.9, [-52, 0, -10]), (1.2, [-54, 0, -10]), (T, g('right_arm'))])},
        'right_forearm': {'rotation': ch([(0, g('right_forearm')), (0.5, [-14, 0, 0]), (0.9, [-24, 0, 0]), (T, g('right_forearm'))])},
        'left_arm': {'rotation': ch([(0, g('left_arm')), (0.5, [-150, 0, 18]), (0.75, [-158, 0, 16]), (0.9, [-56, 0, 14]), (1.2, [-60, 0, 18]), (T, g('left_arm'))])},
        'left_forearm': {'rotation': ch([(0, g('left_forearm')), (0.5, [-20, 0, 0]), (0.9, [-26, 0, 0]), (T, g('left_forearm'))])},
        'staff': {'rotation': ch([(0, g('staff')), (0.5, [175, 0, 8]), (0.75, [178, 0, 6]), (0.9, [232, 0, 8]), (1.2, [230, 0, 8]), (T, g('staff'))])},
        'chest': {'rotation': ch([(0, g('chest')), (0.5, [-14, 0, 0]), (0.9, [22, 0, 0]), (1.2, [18, 0, 0]), (T, g('chest'))])},
        'head': {'rotation': ch([(0, g('head')), (0.5, [-22, 0, 0]), (0.9, [-6, 0, 0]), (T, g('head'))])},
        'root': {'position': ch([(0, R0), (0.5, [0, 1.5, 0]), (0.9, [0, -2.4, -1.5]), (1.2, [0, -2.2, -1.2]), (T, R0)])},
        'mitre0': {'position': ch([(0, R0), (0.6, [0, 6, 0]), (0.9, [0, 2, 0]), (T, R0)]), 'rotation': spin(T, 1, 1)},
        'sigils': {'rotation': spin(T, 2, 2)}, 'censer': {'rotation': ch([(0, R0), (0.9, [40, 0, 0]), (1.2, [-20, 0, 0]), (T, R0)])}})
    for nm in ('attack', 'roar'):   # the floating crown drifts up out of the swing's arc
        L = an[nm]['length']; mb = an[nm]['bones'].setdefault('mitre0', {})
        mb['position'] = ch([(0, R0), (0.18, [0, -3.5, 0]), (L - 0.2, [0, -3.5, 0]), (L, R0)])
    for n in ('cape1', 'cape2', 'cape3'):
        an['crownfall']['bones'][n] = {'rotation': ch([(0, R0), (0.9, [-14, 0, 0]), (1.2, [10, 0, 0]), (T, R0)])}
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

"""Gate Mimic: a rift-touched treasure chest that waits among the real ones in C+ gate dungeons. Banded oak chest with a
gold latch; when it wakes the lid hinges up on rows of ivory fangs, a long tongue lolls out, a single amber eye burns on
the back wall, and four jointed chitin legs unfold from under the base. Run: python3 rig_mimic.py"""
import math
import gen_geo_assets as G
import rigs_saga2 as S
Rig, kf = G.Rig, G.kf
R0 = [0, 0, 0]

def rig():
    """v2 (Cataclysm-style pass): warped, splintered oak box with riveted iron straps and corner caps, dangling chains,
    a fleshy maw lined with two staggered rows of uneven fangs top and bottom, a big amber eye in the lid plus two small
    eyes in the throat, a three-segment tongue, and six multi-jointed tapered chitin legs (coxa / femur / tibia / claws)."""
    import random
    rnd = random.Random(11)
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    r.bone('base', 'root', (0, 0, 0))
    B('base', (0, 0.5, 0), (14, 1, 14), 'wood')
    B('base', (0, 5.5, -6.5), (14, 9, 1), 'wood'); B('base', (0, 5.5, 6.5), (14, 9, 1), 'wood')
    B('base', (-6.5, 5.5, 0), (1, 9, 12), 'wood'); B('base', (6.5, 5.5, 0), (1, 9, 12), 'wood')
    B('base', (2.5, 3.2, -7.15), (5.5, 2.2, 0.5), 'wood', rot=(0, 0, 5))           # warped, sprung plank on the front
    B('base', (-3.0, 7.4, 7.15), (4.5, 1.8, 0.5), 'wood', rot=(0, 0, -6))          # and one on the back
    # flesh lining inside the box
    B('base', (0, 1.25, 0), (12, 0.5, 12), 'maw')
    B('base', (0, 5.6, -5.75), (12, 8.2, 0.5), 'maw'); B('base', (0, 5.6, 5.75), (12, 8.2, 0.5), 'maw')
    B('base', (-5.75, 5.6, 0), (0.5, 8.2, 11), 'maw'); B('base', (5.75, 5.6, 0), (0.5, 8.2, 11), 'maw')
    # iron: straps, rim band, corner caps, rivets
    for x in (-4.5, 4.5):
        B('base', (x, 5.0, -7.1), (2, 10, 0.4), 'band'); B('base', (x, 5.0, 7.1), (2, 10, 0.4), 'band')
        for y in (2.0, 4.6, 7.2):
            B('base', (x, y, -7.45), (0.7, 0.7, 0.4), 'rivet'); B('base', (x, y, 7.45), (0.7, 0.7, 0.4), 'rivet')
    for x in (-7.1, 7.1): B('base', (x, 9.2, 0), (0.4, 1.6, 14.4), 'band')
    B('base', (0, 9.2, -7.1), (14.4, 1.6, 0.4), 'band'); B('base', (0, 9.2, 7.1), (14.4, 1.6, 0.4), 'band')
    for sx in (-1, 1):
        for sz in (-1, 1):
            B('base', (sx * 6.8, 1.6, sz * 6.8), (1.4, 3.2, 1.4), 'band', inflate=0.1)
            B('base', (sx * 7.3, 3.6, sz * 7.3), (0.5, 1.6, 0.5), 'splinter', rot=(sz * 14, 0, -sx * 18))      # splinters off the corners
            B('base', (sx * 7.2, 6.4, sz * 7.35), (0.4, 1.2, 0.4), 'splinter', rot=(sz * -12, 0, sx * 24))
    # fangs: outer row big and uneven, inner row smaller and staggered (front, then sides)
    def fangs(bone, y0, sgn, outer_z, inner_z, side_x):
        x = -6.0
        while x <= 6.01:
            h = rnd.uniform(1.8, 3.2); w = rnd.uniform(0.9, 1.3)
            if not (sgn > 0 and abs(x) < 1.9):        # broken gap in the lower front row where the tongue lolls out
                B(bone, (x, y0 + sgn * h / 2, outer_z), (w, h, 0.9), 'teeth', rot=(sgn * -6, 0, rnd.uniform(-9, 9)))
            x += rnd.uniform(1.45, 1.75)
        x = -5.2
        while x <= 5.21:
            h = rnd.uniform(1.0, 1.9)
            if not (sgn > 0 and abs(x) < 2.4):
                B(bone, (x, y0 + sgn * h / 2, inner_z), (0.8, h, 0.8), 'teeth', rot=(sgn * 8, 0, rnd.uniform(-12, 12)))
            x += rnd.uniform(1.7, 2.1)
        for sx in (-1, 1):
            z = -3.8
            while z <= 4.2:
                h = rnd.uniform(1.2, 2.4)
                B(bone, (sx * side_x, y0 + sgn * h / 2, z), (0.9, h, 1.0), 'teeth', rot=(0, 0, sx * sgn * rnd.uniform(4, 12)))
                z += rnd.uniform(1.8, 2.3)
    fangs('base', 10.0, 1, -5.6, -4.5, 5.5)
    r.bone('eyes_back', 'base', (0, 8, 5.4))
    for x in (-3.0, 3.0):
        B('eyes_back', (x, 8.2, 5.35), (2.0, 2.0, 0.4), 'core'); B('eyes_back', (x, 8.2, 5.1), (0.5, 1.4, 0.2), 'pupil')
    # tongue: throat at the back wall; lashes up and out over the front rim
    r.bone('tongue', 'base', (0, 5, 4))
    B('tongue', (0, 5, 0), (4.4, 1.3, 8), 'tongue')
    r.bone('tongue_tip', 'tongue', (0, 5, -4))
    B('tongue_tip', (0, 5, -6), (3.6, 1.1, 4), 'tongue')
    r.bone('tongue_end', 'tongue_tip', (0, 5, -8))
    B('tongue_end', (0, 5, -9.6), (2.6, 0.9, 3.2), 'tongue'); B('tongue_end', (0, 5, -11.3), (1.6, 0.7, 1.0), 'tongue')
    # chains hanging off the front strap ends
    for sx in (-1, 1):
        bn = r.bone(f'chain_{"r" if sx < 0 else "l"}', 'base', (sx * 4.5, 1.2, -7.6))
        for j in range(5):
            y = 0.6 - j * 1.15
            B(bn, (sx * 4.5, y, -7.6), (0.45, 1.4, 1.0) if j % 2 == 0 else (1.0, 1.4, 0.45), 'chain')
        B(bn, (sx * 4.5, -5.4, -7.6), (1.5, 1.6, 1.0), 'band')   # snapped shackle
    # lid, hinged at the back top edge
    r.bone('lid', 'base', (0, 10, 7))
    B('lid', (0, 12, 0), (14, 4, 14), 'wood', pivot=(0, 10, 7))
    B('lid', (-2.5, 14.15, 0.5), (6, 0.5, 13), 'wood', rot=(0, 0, 3))     # warped top board
    B('lid', (0, 10.25, 0), (12.6, 0.5, 12.6), 'maw')                    # flesh roof of the mouth
    for x in (-4.5, 4.5):
        B('lid', (x, 12.2, 0), (2, 4.3, 14.4), 'band')
        for z in (-5, -1.7, 1.7, 5): B('lid', (x, 14.55, z), (0.7, 0.4, 0.7), 'rivet')
    B('lid', (0, 10.9, 0), (14.4, 1.4, 14.4), 'band')
    for sx in (-1, 1):
        for sz in (-1, 1): B('lid', (sx * 6.8, 13.0, sz * 6.8), (1.4, 2.4, 1.4), 'band', inflate=0.1)
    B('lid', (0, 10.6, -7.5), (2.6, 3.4, 0.8), 'latch'); B('lid', (0, 9.6, -7.8), (1.2, 1.6, 0.6), 'latch')
    fangs('lid', 10.0, -1, -5.6, -4.5, 5.5)
    r.bone('eye', 'lid', (0, 10, 0))
    B('eye', (0, 9.75, 0.5), (6.4, 0.5, 6.4), 'maw'); B('eye', (0, 9.3, 0.5), (4.8, 0.6, 4.8), 'core'); B('eye', (0, 8.9, 0.5), (1.2, 0.4, 3.2), 'pupil')
    # six legs, three a side, fanned; coxa -> femur (rises to a high knee) -> tibia (long taper to the ground) -> claws
    for sx in (-1, 1):
        for zi, (z0, fan) in enumerate(((-4.6, 34), (0.0, 0), (4.6, -34))):
            n = ('r' if sx < 0 else 'l') + 'fmb'[zi]
            r.bone(f'leg_{n}', 'base', (sx * 8.2, 3.0, z0), (0, sx * fan * FAN, 0))
            B(f'leg_{n}', (sx * 9.0, 3.0, z0), (2.4, 2.8, 2.8), 'chitin')
            B(f'leg_{n}', (sx * 8.7, 4.6, z0), (1.6, 0.8, 1.6), 'band')
            femur = r.bone(f'femur_{n}', f'leg_{n}', (sx * 10.0, 3.3, z0))
            ang = math.degrees(math.atan2(5.6, 3.6))
            for t, (w, L) in ((0.27, (2.3, 3.6)), (0.72, (1.9, 3.4))):
                cx, cy = 10.0 + 3.6 * t, 3.3 + 5.6 * t
                B(femur, (sx * cx, cy, z0), (L, w, w), 'chitin', rot=(0, 0, sx * ang * ZS), pivot=(sx * cx, cy, z0))
            B(femur, (sx * 11.8, 7.4, z0), (0.6, 2.0, 0.6), 'teeth', rot=(0, 0, -sx * 20 * ZS), pivot=(sx * 11.8, 7.4, z0))
            tib = r.bone(f'tibia_{n}', femur, (sx * 13.6, 8.9, z0))
            B(tib, (sx * 13.6, 8.9, z0), (2.2, 2.2, 2.2), 'chitin')
            ang2 = math.degrees(math.atan2(-14.0, 2.4))
            for t, w, L in ((0.17, 1.8, 5.2), (0.5, 1.4, 5.0), (0.82, 1.0, 4.6)):
                cx, cy = 13.6 + 2.4 * t, 8.9 - 14.0 * t
                B(tib, (sx * cx, cy, z0), (L, w, w), 'chitin', rot=(0, 0, sx * ang2 * ZS), pivot=(sx * cx, cy, z0))
            B(tib, (sx * 15.8, -5.1, z0 - 0.5), (0.5, 1.6, 0.5), 'teeth', rot=(-25, 0, 0), pivot=(sx * 15.8, -5.1, z0 - 0.5))
            B(tib, (sx * 15.8, -5.1, z0 + 0.5), (0.5, 1.6, 0.5), 'teeth', rot=(25, 0, 0), pivot=(sx * 15.8, -5.1, z0 + 0.5))
    return r

FAN = 1    # sign conventions, checked in game
ZS = -1
LEGS = [s + p for s in 'rl' for p in 'fmb']

PAL = dict(who='gate_mimic', k=2, ts=[256, 256], boss=False, pal=dict(base='#6A4524', dark='#22140A', accent='#8A6A3A', robe='#4A2A1A', cape='#3A2214'),
           trim='#D8A84A', glow='#FFB43A', eye='#FFD27A', core='#FFC04A', crystal='#E8DCC0', eyes='slit')

class Painter(S.Painter):
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
        if role == 'wood':      # oak planks: horizontal boards, grain streaks, dark seams, edge wear
            def f(fn, xx, yy, u, t, ed, fw, fh):
                plank = 3 * k
                pos = (yy if fn not in ('up', 'down') else xx) // plank
                grain = 0.5 + 0.5 * math.sin(((xx if fn not in ('up', 'down') else yy) * 0.35) + pos * 2.1 + 3 * s.n(xx, yy, k))
                c = G.mix((92, 58, 30), (150, 102, 56), 0.35 + 0.4 * grain + 0.15 * ((pos * 37) % 5) / 5)
                seam = ((yy if fn not in ('up', 'down') else xx) % plank) == 0
                kk = s.shade(fn, t, u) * (0.55 if seam else 1.0) * (0.85 if ed < k // 2 else 1.0)
                c = G.ramp(c, kk)
                if s.n(xx, yy, max(1, k // 2)) > 0.86: c = G.mul(c, 0.45)            # cracks / knots
                if fn not in ('up', 'down') and t > 0.82 and s.n(xx, yy, k) > 0.55: c = G.mix(c, (44, 40, 26), 0.5)   # damp rot at the bottom
                s.px(xx, yy, c)
            each(f); return True
        if role in ('band', 'latch'):
            met = (58, 56, 60) if role == 'band' else (214, 168, 74)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.25 if ed < max(1, k // 3) else 1.0)
                c = G.ramp(G.mix(G.mul(met, 0.75), G.mul(met, 1.2), s.n(xx, yy, k)), kk)
                rivet = role == 'band' and fw >= 2 * k and fh >= 2 * k and (xx - F[fn][0]) % (3 * k) in (k, k + 1) and (yy - F[fn][1]) % (3 * k) in (k, k + 1)
                if rivet: c = G.mul(c, 1.6)
                s.px(xx, yy, c)
                if role == 'latch' and fn == 'north' and ed > k // 2: s.gl(xx, yy, (255, 190, 80), 0.25)
            each(f); return True
        if role == 'maw':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((40, 6, 10), (96, 18, 26), s.n(xx, yy, k)); s.px(xx, yy, c)
                if s.n(xx, yy, k) > 0.78: s.gl(xx, yy, (160, 30, 40), 0.25)
            each(f); return True
        if role == 'tongue':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                mid = abs(u - 0.5) < 0.06 and fn == 'up'
                c = G.mix((150, 40, 58), (214, 92, 108), s.n(xx, yy, k)); c = G.mul(c, 0.7 if mid else s.shade(fn, t, u))
                s.px(xx, yy, c)
            each(f); return True
        if role == 'teeth':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((196, 182, 150), (246, 238, 214), s.n(xx, yy, k)); s.px(xx, yy, G.ramp(c, s.shade(fn, t, u) * (0.8 + 0.2 * (1 - t))))
            each(f); return True
        if role == 'leg':      # chitin: dark lacquered segments with amber joint glow
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((26, 18, 16), (70, 48, 36), s.n(xx, yy, k)); kk = s.shade(fn, t, u) * (1.3 if ed < max(1, k // 3) else 1.0)
                s.px(xx, yy, G.ramp(c, kk))
                if fn not in ('up', 'down') and (t < 0.08 or t > 0.92): s.gl(xx, yy, s.gc, 0.45)
            each(f); return True
        if role == 'core':     # amber iris: hot centre, darker rim, veins; fully emissive
            def f(fn, xx, yy, u, t, ed, fw, fh):
                r = min(1.0, math.hypot(u - 0.5, t - 0.5) * 2)
                c = G.mix((255, 226, 120), (176, 74, 10), r)
                if s.n(xx, yy, k) > 0.8 and r > 0.4: c = G.mul(c, 0.6)
                s.px(xx, yy, c); s.gl(xx, yy, c, 1.0 - 0.5 * r)
            each(f); return True
        if role == 'chitin':   # lacquered dark chitin over wood: glossy top highlight, amber seams at segment ends
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix((24, 16, 14), (78, 52, 38), s.n(xx, yy, k))
                kk = s.shade(fn, t, u) * (1.45 if (fn == 'up' or (fn not in ('down',) and t < 0.18)) else 1.0) * (0.75 if ed < max(1, k // 3) else 1.0)
                c = G.ramp(c, kk)
                if fn in ('east', 'west') and ed < max(1, k // 2): c = G.mix(c, (150, 84, 34), 0.4); s.gl(xx, yy, s.gc, 0.2)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'splinter':
            def f(fn, xx, yy, u, t, ed, fw, fh):
                s.px(xx, yy, G.ramp(G.mix((150, 110, 66), (206, 168, 112), s.n(xx, yy, k)), s.shade(fn, t, u)))
            each(f); return True
        if role in ('rivet', 'chain'):
            met = (150, 146, 140) if role == 'rivet' else (70, 68, 72)
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u) * (1.5 if (u < 0.35 and t < 0.35) else 1.0)
                s.px(xx, yy, G.ramp(G.mix(G.mul(met, 0.7), met, s.n(xx, yy, k)), kk))
            each(f); return True
        if role == 'pupil':
            def f(fn, xx, yy, u, t, ed, fw, fh): s.px(xx, yy, (12, 4, 2))
            each(f); return True
        return False

def anims():
    A, ch, loop, hold = S.A, S.ch, S.loop, S.hold
    LEG = lambda n: f'leg_{n}'
    hide = {LEG(n): {'scale': hold([0, 0, 0], 1.0)} for n in LEGS}
    hide.update({'tongue': {'scale': hold([0, 0, 0], 1.0)}, 'eye': {'scale': hold([0, 0, 0], 1.0)}, 'eyes_back': {'scale': hold([0, 0, 0], 1.0)},
                 'chain_r': {'scale': hold([0, 0, 0], 1.0)}, 'chain_l': {'scale': hold([0, 0, 0], 1.0)}})
    LASH = {'tongue': [-54, 0, 0], 'tongue_tip': [28, 0, 0], 'tongue_end': [52, 0, 0]}
    out = {'dormant': A(1.0, hide, True)}
    # tripod gait: (rf, lm, rb) vs (lf, rm, lb); femur lifts (z) while the coxa swings (y)
    tri = {'rf': 0, 'lm': 0, 'rb': 0, 'lf': math.pi, 'rm': math.pi, 'lb': math.pi}
    def legs(T, swing, lift, base_lift=0):
        b = {}
        for n in LEGS:
            sx = -1 if n[0] == 'r' else 1; ph = tri[n]
            b[f'leg_{n}'] = {'rotation': loop(T, swing, 1, (0, 0, 0), ph)}
            b[f'femur_{n}'] = {'rotation': kf([(T * i / 8, [0, 0, round(sx * ZS * (base_lift + lift * max(0, math.sin(2 * math.pi * i / 8 + ph))), 2)]) for i in range(9)])}
            b[f'tibia_{n}'] = {'rotation': kf([(T * i / 8, [0, 0, round(-sx * ZS * 0.6 * lift * max(0, math.sin(2 * math.pi * i / 8 + ph)), 2)]) for i in range(9)])}
        return b
    chains = lambda T, amp: {'chain_r': {'rotation': loop(T, amp, 0, (0, 0, 0))}, 'chain_l': {'rotation': loop(T, amp, 0, (0, 0, 0), 1.3)}}
    T = 2.0; b = {'root': {'position': loop(T, 0.4, 1, (0, 5.5, 0))}, 'lid': {'rotation': loop(T, 6, 0, (-16, 0, 0))},
                  'tongue': {'scale': hold([0, 0, 0], T)}, 'base': {'rotation': loop(T, 1.5, 2)}}
    b.update(legs(T, 3, 4)); b.update(chains(T, 6)); out['idle'] = A(T, b, True)
    T = 0.6; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.15, [0, 6.6, 0]), (0.3, [0, 5.5, 0]), (0.45, [0, 6.6, 0]), (0.6, [0, 5.5, 0])])},
                  'lid': {'rotation': ch([(0, [-10, 0, 0]), (0.15, [-26, 0, 0]), (0.3, [-10, 0, 0]), (0.45, [-26, 0, 0]), (0.6, [-10, 0, 0])])},
                  'tongue': {'scale': hold([0, 0, 0], T)}, 'base': {'rotation': loop(T, 3, 2)}}
    b.update(legs(T, 18, 22)); b.update(chains(T, 22)); out['walk'] = A(T, b, True)
    # BITE: rears back with the maw gaping past vertical and the tongue out (0.1-0.35), lunges and SNAPS shut (impact 0.45)
    T = 0.8; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.25, [0, 7.8, 3]), (0.45, [0, 6.0, -5]), (0.8, [0, 5.5, 0])])},
                  'base': {'rotation': ch([(0, R0), (0.25, [-16, 0, 0]), (0.45, [10, 0, 0]), (0.8, R0)])},
                  'lid': {'rotation': ch([(0, [-14, 0, 0]), (0.22, [-96, 0, 0]), (0.36, [-92, 0, 0]), (0.45, [0, 0, 0]), (0.55, [-6, 0, 0]), (0.8, [-14, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.08, [0, 0, 0]), (0.18, [1, 1, 1]), (0.36, [1, 1, 1]), (0.41, [0, 0, 0])], False), 'rotation': hold(LASH['tongue'], T)},
                  'tongue_tip': {'rotation': ch([(0, [10, 0, 0]), (0.22, LASH['tongue_tip']), (0.3, [22, 0, 0]), (0.4, [10, 0, 0])])},
                  'tongue_end': {'rotation': ch([(0, [20, 0, 0]), (0.22, LASH['tongue_end']), (0.3, [48, 0, 0]), (0.4, [20, 0, 0])])}}
    for n in LEGS:
        sx = -1 if n[0] == 'r' else 1; front = n[1] == 'f'
        b[f'femur_{n}'] = {'rotation': ch([(0, R0), (0.25, [0, 0, sx * ZS * (24 if front else -6)]), (0.45, [0, 0, sx * ZS * (-10 if front else 8)]), (0.8, R0)])}
    b.update(chains(T, 30)); out['attack'] = A(T, b, False)
    # WAKE: rattles shut, legs burst out, maw flies open, eyes open, tongue unrolls
    T = 1.2; b = {'root': {'position': ch([(0, R0), (0.3, [0, 1, 0]), (0.6, [0, 7.5, 0]), (1.2, [0, 5.5, 0])])},
                  'lid': {'rotation': ch([(0, R0), (0.15, [-6, 0, 0]), (0.25, [0, 0, 0]), (0.35, [-10, 0, 0]), (0.45, [0, 0, 0]), (0.7, [-98, 0, 0]), (1.0, [-90, 0, 0]), (1.2, [-16, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.62, [0, 0, 0]), (0.76, [1, 1, 1]), (1.05, [1, 1, 1]), (1.12, [0, 0, 0])], False), 'rotation': hold(LASH['tongue'], T)},
                  'tongue_tip': {'rotation': ch([(0.6, [10, 0, 0]), (0.85, LASH['tongue_tip']), (1.2, LASH['tongue_tip'])])},
                  'tongue_end': {'rotation': ch([(0.6, [10, 0, 0]), (0.85, LASH['tongue_end']), (1.2, LASH['tongue_end'])])},
                  'eye': {'scale': ch([(0, [0, 0, 0]), (0.55, [0, 0, 0]), (0.75, [1, 1, 1])])}, 'eyes_back': {'scale': ch([(0, [0, 0, 0]), (0.6, [0, 0, 0]), (0.8, [1, 1, 1])])},
                  'chain_r': {'scale': ch([(0, [0, 0, 0]), (0.45, [0, 0, 0]), (0.5, [1, 1, 1])], False), 'rotation': ch([(0.5, [40, 0, 0]), (0.8, [-25, 0, 0]), (1.2, R0)])},
                  'chain_l': {'scale': ch([(0, [0, 0, 0]), (0.45, [0, 0, 0]), (0.5, [1, 1, 1])], False), 'rotation': ch([(0.5, [36, 0, 0]), (0.8, [-20, 0, 0]), (1.2, R0)])},
                  'base': {'rotation': ch([(0, R0), (0.15, [0, 0, 4]), (0.25, [0, 0, -4]), (0.35, [0, 0, 3]), (0.45, R0)])}}
    for n in LEGS:
        sx = -1 if n[0] == 'r' else 1
        b[f'leg_{n}'] = {'scale': ch([(0, [0, 0, 0]), (0.45, [0, 0, 0]), (0.62, [1.1, 1.1, 1.1]), (0.78, [1, 1, 1])])}
        b[f'femur_{n}'] = {'rotation': ch([(0.45, [0, 0, sx * ZS * 40]), (0.7, [0, 0, sx * ZS * -8]), (1.2, R0)])}
    out['roar'] = A(T, b, False)
    # DEATH: legs buckle and splay, chest crashes down tilted, maw gapes, tongue lolls, eyes fade
    T = 2.4; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.3, [0, 6.5, 0]), (0.7, [0, 0.6, 0]), (2.4, [0, 0.6, 0])])},
                  'base': {'rotation': ch([(0, R0), (0.7, [0, 0, 6]), (0.9, [0, 0, 9]), (2.4, [0, 0, 9])])},
                  'lid': {'rotation': ch([(0, [-14, 0, 0]), (0.5, [-60, 0, 0]), (0.9, [-100, 0, 0]), (1.1, [-92, 0, 0]), (2.4, [-94, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.75, [1, 1, 1])], False), 'rotation': ch([(0.5, [-50, 0, 0]), (2.4, [-48, 0, 0])])},
                  'tongue_tip': {'rotation': ch([(0.5, [10, 0, 0]), (1.0, [28, 0, 0]), (2.4, [30, 0, 0])])}, 'tongue_end': {'rotation': ch([(0.5, [20, 0, 0]), (1.1, [56, 0, 0]), (2.4, [54, 0, 0])])},
                  'eye': {'scale': ch([(0, [1, 1, 1]), (1.2, [1, 1, 1]), (2.0, [0.2, 0.2, 1]), (2.4, [0, 0, 0])])},
                  'eyes_back': {'scale': ch([(0, [1, 1, 1]), (1.0, [1, 1, 1]), (1.8, [0.1, 0.1, 1]), (2.2, [0, 0, 0])])}}
    for n in LEGS:
        sx = -1 if n[0] == 'r' else 1
        b[f'femur_{n}'] = {'rotation': ch([(0, R0), (0.6, [0, 0, sx * ZS * -34]), (2.4, [0, 0, sx * ZS * -40])])}
        b[f'tibia_{n}'] = {'rotation': ch([(0, R0), (0.6, [0, 0, sx * ZS * 50]), (2.4, [0, 0, sx * ZS * 58])])}
    b.update(chains(T, 8)); out['death'] = A(T, b, False)
    out['idle_combat'] = out['idle']; out['walk_combat'] = out['walk']
    return out

def build():
    name = 'gate_mimic'; S.PAL[name] = PAL; rg = rig()
    m = dict(PAL); uvs = G.pack(rg, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, m['ts'][1] * 4]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = Painter(name, hm)
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rg, pt, m['k'], huvs, uvs); G.geo(name, rg, m['k'], m['ts'], uvs)
    G.write_anims(name, anims())
    keep = dict(S.FREEZE); S.FREEZE['attack'] = 0.28; S.FREEZE['attack_windup'] = ('attack', 0.18)   # the bite review pose is the full gape
    S.bake_freezes(name); S.FREEZE.clear(); S.FREEZE.update(keep)
    import os; g = G.out('textures', 'entity', 'gate_mimic_glow_rank.png')
    if os.path.exists(g): os.remove(g)
    print(name, len(rg.order), 'bones', len(cs), 'cubes', m['ts'])

if __name__ == '__main__':
    build()

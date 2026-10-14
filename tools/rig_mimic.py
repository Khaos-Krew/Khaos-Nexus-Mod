"""Gate Mimic: a rift-touched treasure chest that waits among the real ones in C+ gate dungeons. Banded oak chest with a
gold latch; when it wakes the lid hinges up on rows of ivory fangs, a long tongue lolls out, a single amber eye burns on
the back wall, and four jointed chitin legs unfold from under the base. Run: python3 rig_mimic.py"""
import math
import gen_geo_assets as G
import rigs_saga2 as S
Rig, kf = G.Rig, G.kf
R0 = [0, 0, 0]

def rig():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    r.bone('base', 'root', (0, 0, 0))
    # hollow box: floor + four walls, so fangs and tongue live inside the maw
    B('base', (0, 0.5, 0), (14, 1, 14), 'wood')
    B('base', (0, 5.5, -6.5), (14, 9, 1), 'wood'); B('base', (0, 5.5, 6.5), (14, 9, 1), 'wood')
    B('base', (-6.5, 5.5, 0), (1, 9, 12), 'wood'); B('base', (6.5, 5.5, 0), (1, 9, 12), 'wood')
    B('base', (0, 1.1, 0), (12, 0.2, 12), 'maw')               # gullet floor (dark flesh)
    for x in (-4.5, 4.5):                                         # iron straps
        B('base', (x, 5.0, -7.05), (2, 10, 0.3), 'band'); B('base', (x, 5.0, 7.05), (2, 10, 0.3), 'band')
    for x in (-7.05, 7.05): B('base', (x, 9.2, 0), (0.3, 1.6, 14), 'band')
    B('base', (0, 9.2, -7.05), (14, 1.6, 0.3), 'band'); B('base', (0, 9.2, 7.05), (14, 1.6, 0.3), 'band')
    for i in range(6):                                            # lower fangs along the front rim, pointing up
        B('base', (-5 + i * 2, 10.6, -5.6), (1.0, 1.6, 0.9), 'teeth', rot=(0, 0, (i % 2 * 2 - 1) * 6))
    for z in (-3, 0, 3):
        for x in (-5.6, 5.6): B('base', (x, 10.4, z), (0.9, 1.2, 1.0), 'teeth')
    r.bone('tongue', 'base', (0, 6, 4))                          # throat at the back wall; lashes out over the front rim
    B('tongue', (0, 6, 0), (4.2, 1.2, 8), 'tongue')
    r.bone('tongue_tip', 'tongue', (0, 6, -4))
    B('tongue_tip', (0, 6, -6), (3.4, 1.0, 4), 'tongue')
    r.bone('tongue_end', 'tongue_tip', (0, 6, -8))
    B('tongue_end', (0, 6, -9.5), (2.6, 0.9, 3), 'tongue')
    # lid, hinged at the back top edge
    r.bone('lid', 'base', (0, 10, 7))
    B('lid', (0, 12, 0), (14, 4, 14), 'wood', pivot=(0, 10, 7))
    for x in (-4.5, 4.5): B('lid', (x, 12.2, 0), (2, 4.2, 14.3), 'band')
    B('lid', (0, 10.9, 0), (14.3, 1.4, 14.3), 'band')
    B('lid', (0, 10.6, -7.4), (2.4, 3.2, 0.8), 'latch'); B('lid', (0, 10.0, -7.7), (1.0, 1.4, 0.5), 'latch')
    r.bone('eye', 'lid', (0, 10, 0))                                # one amber eye in the lid's gullet, seen when it gapes
    B('eye', (0, 9.5, -0.5), (6.0, 1.0, 6.0), 'maw'); B('eye', (0, 8.9, -0.5), (4.2, 1.0, 4.2), 'core'); B('eye', (0, 8.3, -0.5), (1.2, 0.6, 2.8), 'pupil')
    for i in range(7):                                            # upper fangs hang down inside the front rim
        B('lid', (-6 + i * 2, 9.2, -5.6), (1.0, 1.8, 0.9), 'teeth', rot=(0, 0, (i % 2 * 2 - 1) * 5))
    for z in (-2, 2):
        for x in (-5.6, 5.6): B('lid', (x, 9.4, z), (0.9, 1.4, 1.0), 'teeth')
    # legs: spider-like; a thick thigh runs out along the diagonal from the chest corner to a raised knee, the shin
    # drops from the knee to a bone claw (legs are scaled to zero while it sleeps)
    for sx in (-1, 1):
        for sz in (-1, 1):
            n = ('r' if sx < 0 else 'l') + ('f' if sz < 0 else 'b')
            hx, hz = sx * 7.4, sz * 7.4
            r.bone(f'leg_{n}', 'base', (hx, 2.2, hz))
            B(f'leg_{n}', (sx * 8.75, 2.4, sz * 8.75), (2.0, 2.0, 5.0), 'leg', rot=(0, -sx * sz * 45, 0), pivot=(sx * 8.75, 2.4, sz * 8.75))
            B(f'leg_{n}', (sx * 9.0, 3.6, sz * 9.0), (0.8, 0.8, 3.0), 'teeth', rot=(0, -sx * sz * 45, 0), pivot=(sx * 9.0, 3.6, sz * 9.0))   # dorsal spine
            r.bone(f'shin_{n}', f'leg_{n}', (sx * 10.9, 2.4, sz * 10.9))
            B(f'shin_{n}', (sx * 10.9, 2.4, sz * 10.9), (2.4, 2.4, 2.4), 'leg', rot=(0, 45, 0))
            B(f'shin_{n}', (sx * 11.1, -1.4, sz * 11.1), (1.6, 6.6, 1.6), 'leg', rot=(0, 45, 0))
            B(f'shin_{n}', (sx * 11.2, -5.0, sz * 11.2), (1.2, 1.4, 1.2), 'teeth', rot=(0, 45, 0))
    return r

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
                s.px(xx, yy, G.ramp(c, kk))
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
        if role == 'pupil':
            def f(fn, xx, yy, u, t, ed, fw, fh): s.px(xx, yy, (12, 4, 2))
            each(f); return True
        return False

def anims():
    A, ch, loop, hold = S.A, S.ch, S.loop, S.hold
    tuck = {f'leg_{n}': {'scale': hold([0, 0, 0], 1.0)} for n in ('rf', 'rb', 'lf', 'lb')}
    hide = {'tongue': {'scale': hold([0, 0, 0], 1.0)}, 'eye': {'scale': hold([0, 0, 0], 1.0)}}
    LASH = {'tongue': [-46, 0, 0], 'tongue_tip': [36, 0, 0], 'tongue_end': [64, 0, 0]}
    out = {}
    out['dormant'] = A(1.0, dict(tuck, **hide), True)
    legs = lambda T, amp, ph: {f'leg_{n}': {'rotation': loop(T, amp, 0, (0, 0, 0), ph + i * 1.6)} for i, n in enumerate(('rf', 'lb', 'lf', 'rb'))}
    shins = lambda T, amp, ph: {f'shin_{n}': {'rotation': loop(T, amp, 0, (0, 0, 0), ph + i * 1.6 + 0.8)} for i, n in enumerate(('rf', 'lb', 'lf', 'rb'))}
    T = 2.0; b = {'root': {'position': hold([0, 5.5, 0], T)}, 'lid': {'rotation': loop(T, 5, 0, (-12, 0, 0))},
                  'tongue': {'scale': hold([0, 0, 0], T)},
                  'base': {'rotation': loop(T, 1.5, 2)}}
    b.update(legs(T, 4, 0)); b.update(shins(T, 5, 0)); out['idle'] = A(T, b, True)
    T = 0.6; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.15, [0, 7.0, 0]), (0.3, [0, 5.5, 0]), (0.45, [0, 7.0, 0]), (0.6, [0, 5.5, 0])])},
                  'lid': {'rotation': ch([(0, [-8, 0, 0]), (0.15, [-22, 0, 0]), (0.3, [-8, 0, 0]), (0.45, [-22, 0, 0]), (0.6, [-8, 0, 0])])},
                  'tongue': {'scale': hold([0, 0, 0], T)}, 'base': {'rotation': loop(T, 4, 2)}}
    b.update(legs(T, 28, 0)); b.update(shins(T, 22, 0)); out['walk'] = A(T, b, True)
    # bite: rear back with the lid flung open, lunge and SNAP shut (impact 0.45)
    T = 0.8; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.25, [0, 7.5, 3]), (0.45, [0, 6.0, -5]), (0.8, [0, 5.5, 0])])},
                  'base': {'rotation': ch([(0, R0), (0.25, [-14, 0, 0]), (0.45, [10, 0, 0]), (0.8, R0)])},
                  'lid': {'rotation': ch([(0, [-12, 0, 0]), (0.25, [-78, 0, 0]), (0.4, [-70, 0, 0]), (0.45, [0, 0, 0]), (0.55, [-6, 0, 0]), (0.8, [-12, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.1, [0, 0, 0]), (0.22, [1, 1, 1]), (0.36, [1, 1, 1]), (0.42, [0, 0, 0])], False), 'rotation': hold(LASH['tongue'], T)},
                  'tongue_tip': {'rotation': ch([(0, [10, 0, 0]), (0.25, LASH['tongue_tip']), (0.4, [10, 0, 0])])}, 'tongue_end': {'rotation': ch([(0, [20, 0, 0]), (0.25, LASH['tongue_end']), (0.4, [20, 0, 0])])}}
    for i, n in enumerate(('rf', 'lf')): b[f'leg_{n}'] = {'rotation': ch([(0, R0), (0.25, [-26, 0, 0]), (0.45, [18, 0, 0]), (0.8, R0)])}
    for n in ('rb', 'lb'): b[f'leg_{n}'] = {'rotation': ch([(0, R0), (0.25, [16, 0, 0]), (0.45, [-14, 0, 0]), (0.8, R0)])}
    out['attack'] = A(T, b, False)
    # wake: legs burst out (scale), lid flies open, tongue unrolls, eye opens (1.0s)
    T = 1.2; b = {'root': {'position': ch([(0, R0), (0.3, [0, 1, 0]), (0.6, [0, 7.5, 0]), (1.2, [0, 5.5, 0])])},
                  'lid': {'rotation': ch([(0, R0), (0.15, [-6, 0, 0]), (0.25, [0, 0, 0]), (0.35, [-10, 0, 0]), (0.45, [0, 0, 0]), (0.7, [-82, 0, 0]), (1.0, [-74, 0, 0]), (1.2, [-12, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.62, [0, 0, 0]), (0.78, [1, 1, 1]), (1.05, [1, 1, 1]), (1.12, [0, 0, 0])], False), 'rotation': hold(LASH['tongue'], T)},
                  'tongue_tip': {'rotation': ch([(0.6, [10, 0, 0]), (0.85, LASH['tongue_tip']), (1.2, LASH['tongue_tip'])])}, 'tongue_end': {'rotation': ch([(0.6, [10, 0, 0]), (0.9, [72, 0, 0]), (1.0, LASH['tongue_end']), (1.2, LASH['tongue_end'])])},
                  'eye': {'scale': ch([(0, [0, 0, 0]), (0.55, [0, 0, 0]), (0.75, [1, 1, 1])])}, 'base': {'rotation': ch([(0, R0), (0.15, [0, 0, 4]), (0.25, [0, 0, -4]), (0.35, [0, 0, 3]), (0.45, R0)])}}
    for n in ('rf', 'rb', 'lf', 'lb'):
        b[f'leg_{n}'] = {'scale': ch([(0, [0, 0, 0]), (0.45, [0, 0, 0]), (0.65, [1.1, 1.1, 1.1]), (0.8, [1, 1, 1])])}
    out['roar'] = A(T, b, False)
    # death: legs buckle, chest crashes down, lid gapes, tongue slumps, eye fades
    T = 2.4; b = {'root': {'position': ch([(0, [0, 5.5, 0]), (0.3, [0, 6.5, 0]), (0.7, [0, 0, 0]), (2.4, [0, 0, 0])])},
                  'base': {'rotation': ch([(0, R0), (0.7, [0, 0, 6]), (0.9, [0, 0, 9]), (2.4, [0, 0, 9])])},
                  'lid': {'rotation': ch([(0, [-12, 0, 0]), (0.5, [-60, 0, 0]), (0.9, [-96, 0, 0]), (1.1, [-90, 0, 0]), (2.4, [-92, 0, 0])])},
                  'tongue': {'scale': ch([(0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.75, [1, 1, 1])], False), 'rotation': ch([(0.5, [-20, 0, 0]), (1.0, [-44, 0, 0]), (2.4, [-42, 0, 0])])},
                  'tongue_tip': {'rotation': ch([(0.5, [10, 0, 0]), (1.0, [30, 0, 0]), (2.4, [34, 0, 0])])}, 'tongue_end': {'rotation': ch([(0.5, [20, 0, 0]), (1.1, [66, 0, 0]), (2.4, [62, 0, 0])])},
                  'eye': {'scale': ch([(0, [1, 1, 1]), (1.2, [1, 1, 1]), (2.0, [0.2, 0.2, 1]), (2.4, [0, 0, 0])])}}
    for n in ('rf', 'rb', 'lf', 'lb'):
        b[f'leg_{n}'] = {'rotation': ch([(0, R0), (0.6, [0, 0, 50 if n[0] == 'l' else -50]), (2.4, [0, 0, 60 if n[0] == 'l' else -60])])}
        b[f'shin_{n}'] = {'rotation': ch([(0, R0), (0.6, [0, 0, 40 if n[0] == 'l' else -40]), (2.4, [0, 0, 50 if n[0] == 'l' else -50])])}
    out['death'] = A(T, b, False)
    out['idle_combat'] = out['idle']; out['walk_combat'] = out['walk']
    return out

if __name__ == '__main__':
    name = 'gate_mimic'; S.PAL[name] = PAL; rg = rig()
    m = dict(PAL); uvs = G.pack(rg, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, m['ts'][1] * 4]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = Painter(name, hm)
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rg, pt, m['k'], huvs, uvs); G.geo(name, rg, m['k'], m['ts'], uvs)
    G.write_anims(name, anims()); S.bake_freezes(name)
    print(name, len(rg.order), 'bones', len(cs), 'cubes')

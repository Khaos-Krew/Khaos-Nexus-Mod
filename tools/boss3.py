"""v3 sculpted-boss framework (generalised from the Hollow King art pass, rig_king.py).
Shared by the 1.1.2 polish rigs (rig_warden3.py, rig_guardian3.py):
  - sculpting helpers: octo (rounded prism), otaper, plate (bevelled slab), horn (axis-aligned swept links)
  - Painter3: King-grade materials (specular plate, bevelled rims, trim, emissive rune-work, folded cloth, torn hems)
    driven by a per-boss material palette
  - bake(): per-tick (1/20 s) IK of both fists onto the weapon grip, reach slide, automatic weapon clearance against
    the body, and left-forearm clearance from the weapon axis and listed hazards
  - cloth(): verlet bake for split capes
Weapons are authored in ROOT space as (P grip point of the right fist, D weapon axis, N flat normal)."""
import math
import numpy as np
import gen_geo_assets as G
import rigs_generals as RG
import king_ik as K
R0 = [0, 0, 0]

# ------------------------------------------------------------------ sculpting helpers
def octo(r, bone, c, size, role, ry=0.0, rz=0.0, pivot=None, k=0.82, bev=0.22):
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
    w, h, d = size; b = min(bev, w / 4, h / 4)
    r.box(bone, c, (w, h - 2 * b, d), role, rot=rot, pivot=pivot or c)
    r.box(bone, c, (w - 2 * b, h, d), role, rot=rot, pivot=pivot or c)
    if raise_: r.box(bone, (c[0], c[1], c[2] - raise_ / 2), (w - 2 * b, h - 2 * b, d + raise_), role, rot=rot, pivot=pivot or c)

def horn(r, bone, base, s, n=9, L=1.5, w0=2.0, dw=0.16, out=0.9, ang0=10.0, curl=(16, 6), tip='crown', role='horn'):
    p = np.array(base, float); ang = ang0
    for i in range(n):
        w = w0 - i * dw
        ang += curl[0] if i < n * 0.55 else curl[1]
        d = np.array([s * (out if i < n * 0.45 else out * 0.28), math.cos(math.radians(ang)) * L, math.sin(math.radians(ang)) * L])
        p = p + d
        r.box(bone, tuple(p), (w, w + 0.4, w), role if i < n - 1 else tip)

# ------------------------------------------------------------------ painter
class Painter3(RG.Painter):
    """mob['mat'] palette keys: metal{role: rgb}, spec, rim, trim(lo, hi, hl), rune, runegl, cloth{role: rgb}, hem,
    fur(lo, hi), eye(lo, hi), core(lo, hi), edge(lo, hi), horn(lo, hi), vein."""
    def style(s, F, role):
        M = s.mob['mat']; k = s.k; e = max(1, k // 2)
        def each(fb):
            for fn, (fx, fy, fw, fh) in F.items():
                if fw <= 0 or fh <= 0: continue
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        u = (xx - fx) / max(1, fw - 1); ed = min(xx - fx, fx + fw - 1 - xx, yy - fy, fy + fh - 1 - yy)
                        fb(fn, xx, yy, u, t, ed, fw, fh)
        if role == 'fur' and 'fur' in M:
            lo, hi = M['fur']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix(lo, hi, s.n(xx, yy, max(1, k // 2)))
                c = G.ramp(c, s.shade(fn, t, u) * (0.88 + 0.22 * s.n(xx, yy, 1)))
                if (yy + (xx // max(1, k)) % 3) % max(2, k) == 0: c = G.mul(c, 0.86)
                s.px(xx, yy, c)
            each(f); return True
        if role in M['metal']:
            base = M['metal'][role]; trim_lo, trim_hi, trim_hl = M['trim']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                spec = math.exp(-((t - 0.28) / 0.16) ** 2) * (0.35 if fn != 'down' else 0.0)
                kk = s.shade(fn, t, u) * (1.04 - 0.26 * t ** 1.4 if fn not in ('up', 'down') else 1.0) * (0.92 + 0.16 * math.sin(u * math.pi))
                c = G.ramp(G.mix(G.mul(base, 0.72), G.mul(base, 1.28), s.n(xx, yy, 2 * k)), kk)
                c = G.mix(c, M['spec'], spec * 0.55)
                if (xx * 7 + yy * 3) % 29 == 0: c = G.mul(c, 1.22)
                big = fw > 4 * k and fh > 4 * k
                if ed < e: c = G.mul(c, 0.42)
                elif ed < 2 * e and fn in ('north', 'up'): c = G.mix(c, M['rim'], 0.35)
                elif role in M.get('trimmed', ()) and big and ed < 3 * e: c = G.ramp(trim_hi, kk)
                elif big and role in M.get('etched', ()):
                    lx, ly = (xx - F[fn][0]) / k, (yy - F[fn][1]) / k
                    w = math.sin(lx * 1.3 + math.sin(ly * 0.9) * 1.6) * math.cos(ly * 1.1)
                    if abs(w) < 0.06 and ed > 3 * e:
                        if role in M.get('glowing', ()) and fn == 'north' and ((xx // (3 * k)) + (yy // (3 * k))) % 3 == 0:
                            c = M['rune']; s.gl(xx, yy, s.gc, 0.85)
                        else: c = G.ramp(trim_lo, kk * 0.95)
                if 'vein' in M and role in M.get('veined', ()) and big and ed > 2 * e:
                    v = abs(math.sin(xx * 0.21 / k * 3 + math.sin(yy * 0.17 / k * 3) * 2.2))
                    if v < 0.035: c = M['vein']; s.gl(xx, yy, s.gc, 0.75)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'horn' and 'horn' in M:
            lo, hi = M['horn']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.ramp(G.mix(lo, hi, 0.3 + 0.5 * s.n(xx, yy, k)), s.shade(fn, t, u))
                if (yy // max(1, k)) % 3 == 0: c = G.mul(c, 0.7)
                if ed < 1: c = G.mul(c, 0.6)
                s.px(xx, yy, c)
            each(f); return True
        if role == 'eye':
            lo, hi = M['eye']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                c = G.mix(lo, hi, 1 - abs(t - 0.5) * 2); s.px(xx, yy, c); s.gl(xx, yy, hi, 1.0)
            each(f); return True
        if role == 'edge':
            lo, hi = M['edge']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                s.px(xx, yy, G.ramp(G.mix(lo, hi, 0.4 + 0.4 * s.n(xx, yy, k)), s.shade(fn, t, u)))
            each(f); return True
        if role in ('crown', 'core', 'guard'):
            trim_lo, trim_hi, trim_hl = M['trim']
            def f(fn, xx, yy, u, t, ed, fw, fh):
                kk = s.shade(fn, t, u)
                if role == 'core':
                    lo, hi = M['core']
                    c = G.mix(lo, hi, 0.5 + 0.5 * (1 - abs(u - 0.5) * 2) * (1 - abs(t - 0.5) * 2))
                    s.px(xx, yy, c); s.gl(xx, yy, s.gc, 0.95); return
                c = G.ramp(G.mix(G.mul(trim_lo, 0.8), trim_hi, s.n(xx, yy, k)), kk)
                c = G.mix(c, trim_hl, math.exp(-((t - 0.3) / 0.18) ** 2) * 0.4)
                if ed < e: c = G.mul(c, 0.55)
                elif s.n(xx * 3, yy * 3, 1) > 0.84: c = G.mul(c, 0.5)
                s.px(xx, yy, c)
            each(f); return True
        if role in M.get('cloth', {}):
            base = M['cloth'][role]
            def f(fn, xx, yy, u, t, ed, fw, fh):
                folds = max(1, fw // (3 * k))
                fold = 0.5 + 0.5 * math.sin(u * math.pi * 2 * folds + t * 0.8) if fn not in ('up', 'down') else 0.5
                kk = s.shade(fn, t, u) * (0.68 + 0.48 * fold)
                c = G.ramp(G.mix(G.mul(base, 0.62), G.mul(base, 1.32), s.n(xx, yy, 2 * k)), kk)
                if fold > 0.92: c = G.mix(c, M.get('sheen', (176, 70, 86)), 0.18)
                a = 255
                if role == 'capehem' and fn not in ('up', 'down') and t > 0.45:
                    if s.n(xx, yy, k) > 0.28 + 1.1 * (1 - t): a = 0
                    elif t > 0.78: c = G.mul(c, 0.6)
                if 'hem' in M and fn in ('north', 'south') and 0.86 < t < 0.95 and role != 'capehem':
                    c = G.mix(c, M['hem'], 0.75)
                s.px(xx, yy, c, a)
            each(f); return True
        return super().style(F, role)

# ------------------------------------------------------------------ animation authoring helpers
def nrm(v): v = np.array(v, float); return v / np.linalg.norm(v)
def SW(P, D, N=(1, 0, 0)): return dict(P=P, D=D, N=N)
def mv(sw, dP=(0, 0, 0), D=None): return SW(tuple(np.add(sw['P'], dP)), D or sw['D'], sw['N'])
def sword_keys(pairs): return [(t, list(p['P']) + list(nrm(p['D'])) + list(nrm(p['N']))) for t, p in pairs]

def interp(keys, t, loop_T=None):
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

def stride(T, amp=22, knee=24, bob=1.2, n=16):
    out = {}
    for side, ph in (('right', 0.0), ('left', math.pi)):
        out[f'{side}_leg'] = [(T * i / n, [amp * math.sin(2 * math.pi * i / n + ph), 0, 0]) for i in range(n + 1)]
        out[f'{side}_shin'] = [(T * i / n, [max(0, knee * math.sin(2 * math.pi * i / n + ph + 1.3)), 0, 0]) for i in range(n + 1)]
    out['root_pos'] = [(T * i / n, [0, -bob * abs(math.sin(2 * math.pi * i / n)) + bob * 0.5, 0]) for i in range(n + 1)]
    out['hips'] = [(T * i / n, [0, 6 * math.sin(2 * math.pi * i / n), 0]) for i in range(n + 1)]
    out['sway'] = [(T * i / n, [0, -5 * math.sin(2 * math.pi * i / n), 0]) for i in range(n + 1)]
    return out

def base(T, lp, sword, body=None, pos=None, extra=None, lfree=None, lf=None, rfree=None, rf=None, noclear=None):
    """lfree / rfree: windows where that arm plays authored keys (lf / rf) instead of the IK grip.
    sword=None: no weapon in hand (both arms authored)."""
    return dict(T=T, loop=lp, sword=sword_keys(sword) if sword else None, body=body or {}, pos=pos or {}, extra=extra or {},
                free=lfree or [], lf=lf or {}, rfree=rfree or [], rf=rf or {}, noclear=noclear or [])

def sample_body(a, t):
    R, Q = {}, {}; L = a['T'] if a['loop'] else None
    for bn, ks in a['body'].items(): R[bn] = interp(ks, t, L)
    for bn, ks in a['pos'].items(): Q[bn] = interp(ks, t, L)
    if 'rotation' in a['extra'].get('root', {}):
        rk = sorted(((float(k), v['post'] if isinstance(v, dict) else v) for k, v in a['extra']['root']['rotation'].items()))
        R['root'] = interp(rk, t)
    return R, Q

# ------------------------------------------------------------------ the bake
ARMS_R = ['right_arm', 'right_forearm', 'right_hand']; ARMS_L = ['left_arm', 'left_forearm', 'left_hand']
LIM_ARM = ([-220, -120, -150], [90, 120, 90]); LIM_FA = ([-140, -90, -10], [0, 90, 10]); LIM_HAND = ([-100, -170, -90], [100, 170, 90])
W_AX, W_FI = 8, 1.5
CLEAR_BONES = ['chest', 'torso', 'hips', 'head', 'right_leg', 'left_leg', 'right_shin', 'left_shin']

class Cfg:
    """weapon: bone; HX, FY, FZ: fist centre (|x|, y, z) in hand space = weapon pivot; SH: shoulder (|x|, y);
    gap: right->left fist spacing along the weapon axis (left fist sits at -gap); ax: (lo, hi) weapon extent along
    its axis from the right fist; widths: [(y, half-width along the flat)] for clearance; hazards: weapon-local points the
    left forearm keeps clear of, with radius; fa_pts: left-forearm sample heights; reach."""
    def __init__(s, **kw): s.__dict__.update(kw)

def body_boxes(rig_, bones):
    out = {}
    for bn in bones:
        L = []
        for c in rig_.bones[bn]['cubes']:
            o = np.array(c['origin'], float); sz = np.array(c['size'], float)
            if c.get('rot'):
                Rm = K.rot(c['rot'][0], -c['rot'][1], c['rot'][2]); pv = np.array(c['pivot'], float)
            else: Rm, pv = np.eye(3), np.zeros(3)
            L.append((Rm.T, pv, o - 0.4, o + sz + 0.4))
        out[bn] = L
    return out

def weapon_pts(cf, ms):
    X, Y, Z = -cf.HX, cf.FY, cf.FZ
    pts = [K.xf(ms, (X, Y + y, Z)) for y in np.arange(cf.ax[0], cf.ax[1], 1.5)]
    pts += [K.xf(ms, (X, Y + y, Z + dz)) for y, w in cf.widths for dz in (-w, -w / 2, w / 2, w)]
    return pts

def staff_hits(cf, fk, boxes, R, Q):
    ms = fk.world(cf.weapon, R, Q); pts = weapon_pts(cf, ms); hits = []
    for bn, L in boxes.items():
        Mi = np.linalg.inv(fk.world(bn, R, Q))
        for pw in pts:
            pl = K.xf(Mi, pw)
            for RT, pv, lo, hi in L:
                q = RT @ (pl - pv) + pv
                if np.all(q > lo) and np.all(q < hi): hits.append((bn, pw)); break
    return hits

def bake(cf, rig_, A, verbose=False):
    fk = K.FK(rig_); out = {}; boxes = body_boxes(rig_, getattr(cf, 'clear', CLEAR_BONES))
    loR = LIM_ARM[0] + LIM_FA[0] + LIM_HAND[0]; hiR = LIM_ARM[1] + LIM_FA[1] + LIM_HAND[1]
    loL = [LIM_ARM[0][0], -LIM_ARM[1][1], -LIM_ARM[1][2]] + LIM_FA[0] + LIM_HAND[0]
    hiL = [LIM_ARM[1][0], -LIM_ARM[0][1], -LIM_ARM[0][2]] + LIM_FA[1] + LIM_HAND[1]
    X, Y, Z = -cf.HX, cf.FY, cf.FZ
    for name, a in A.items():
        T = a['T']; n = max(2, int(round(T * 20))); ts = [T * i / n for i in range(n + 1)]
        keys = {bn: [] for bn in ARMS_R + ARMS_L}
        if a['sword'] is None:
            for bn in ARMS_R + ARMS_L:
                src = a['rf'] if bn in ARMS_R else a['lf']
                keys[bn] = [(float(t_), [round(float(q), 2) for q in v_]) for t_, v_ in src.get(bn, [(0, R0), (T, R0)])]
            out[name] = keys; print(f'  ik {name:12s} (no weapon in hand)'); continue
        xr = np.array([-50, 0, -10, -40, 0, 0, 20, 0, 0], float); xl = np.array([-60, 0, 20, -60, 0, 0, 0, 0, 0], float)
        worst = wl = 0.0
        for t in ts:
            R, Q = sample_body(a, t)
            sw = interp(a['sword'], t, T if a['loop'] else None); P, D, N = sw[:3], nrm(sw[3:6]), np.array(sw[6:9])
            N = nrm(N - D * (N @ D))
            Cm = fk.world('chest', R, Q); Rm = fk.world('root', R, Q)
            Pw, Dw, Nw = K.xf(Rm, P), K.xd(Rm, D), K.xd(Rm, N)
            shR = K.xf(fk.world('right_arm', R, Q), (-cf.SH[0], cf.SH[1], 0)); shL = K.xf(fk.world('left_arm', R, Q), (cf.SH[0], cf.SH[1], 0))
            home = (shR + shL) / 2 + K.xd(Cm, (0, -10, -9))
            two = not any(t0 < t < t1 for t0, t1 in a['free'])
            for _ in range(60):
                if np.linalg.norm(Pw - shR) <= cf.reach and (not two or np.linalg.norm(Pw - Dw * cf.gap - shL) <= cf.reach): break
                Pw = Pw + (home - Pw) * 0.05
            def fr(x):
                RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = x[0:3], x[3:6], x[6:9]
                m = fk.world(cf.weapon, RR, Q)
                return np.concatenate([(K.xf(m, (X, Y, Z)) - Pw) * 3, (K.xd(m, (0, 1, 0)) - Dw) * 10, (K.xd(m, (1, 0, 0)) - Nw) * 3, x * 0.004])
            SEEDS_R = [np.array([-90, 0, -20, -60, 0, 0, 0, 0, 0.0]), np.array([-150, 0, -10, -20, 0, 0, 0, 0, 0.0]), np.array([-170, 30, -20, -100, 0, 0, 0, 0, 0.0]),
                       np.array([-30, 0, -10, -30, 0, 0, 20, 0, 0.0]), np.array([-120, -40, -40, -90, 0, 0, 0, 0, 0.0])]
            best = K.solve(fr, xr, loR, hiR)
            if t == 0 or best[1] > 9.0:
                alt = min([K.solve(fr, sd, loR, hiR) for sd in SEEDS_R], key=lambda q: q[1])
                if t == 0 or alt[1] < best[1] * 0.5: best = alt
            xr = best[0]
            RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = xr[0:3], xr[3:6], xr[6:9]
            nocl = any(t0 <= t <= t1 for t0, t1 in a['noclear'])
            for _c in range(0 if nocl else 16):
                hs = staff_hits(cf, fk, boxes, RR, Q)
                if not hs: break
                ctr = np.mean([K.xf(fk.world(bn, RR, Q), (0, cf.SH[1] - 12, 0)) for bn, _ in hs], axis=0)
                away = np.mean([pw for _, pw in hs], axis=0) - ctr; away[1] *= 0.3
                Pw = Pw + nrm(away + K.xd(Rm, (0, 0, -1.5))) * 1.5
                xr = K.solve(fr, xr, loR, hiR)[0]
                RR = dict(R); RR['right_arm'], RR['right_forearm'], RR['right_hand'] = xr[0:3], xr[3:6], xr[6:9]
            ms = fk.world(cf.weapon, RR, Q); mh = fk.world('right_hand', RR, Q)
            er = float(np.linalg.norm(K.xf(ms, (X, Y, Z)) - Pw)); worst = max(worst, er); el = 0.0
            if two:
                tgt = K.xf(ms, (X, Y - cf.gap, Z)); ax = K.xd(ms, (0, 1, 0)); sa = K.xf(ms, (X, Y, Z)); sd = ax
                hz = [(K.xf(ms, (X + p[0], Y + p[1], Z + p[2])), rad) for p, rad in cf.hazards]
                def fl(x):
                    RL = dict(RR); RL['left_arm'], RL['left_forearm'], RL['left_hand'] = x[0:3], x[3:6], x[6:9]
                    m = fk.world('left_hand', RL, Q); mf = fk.world('left_forearm', RL, Q)
                    pen = []
                    for yy in cf.fa_pts:
                        q = K.xf(mf, (cf.HX, yy, 0)); u = np.clip((q - sa) @ sd, cf.ax[0], cf.ax[1])
                        pen.append(max(0.0, cf.fa_rad - np.linalg.norm(sa + sd * u - q)) * 14)
                        for cp, rad in hz: pen.append(max(0.0, rad - np.linalg.norm(cp - q)) * 14)
                    return np.concatenate([(K.xf(m, (cf.HX, Y, Z)) - tgt) * 3, pen, (K.xd(m, (0, 0, -1)) - ax) * W_AX, (K.xd(m, (0, 1, 0)) - nrm(shL - tgt)) * W_FI, x * 0.004])
                SEEDS_L = [np.array([-90, 0, 30, -60, 0, 0, 0, 0, 0.0]), np.array([-150, 0, 10, -20, 0, 0, 0, 0, 0.0]), np.array([-60, 0, 20, -60, 0, 0, 0, 0, 0.0]),
                           np.array([-40, 0, 40, -100, 0, 0, 0, 0, 0.0])]
                bl = K.solve(fl, xl, loL, hiL)
                if t == 0 or bl[1] > 30.0:
                    alt = min([K.solve(fl, sd_, loL, hiL) for sd_ in SEEDS_L], key=lambda q: q[1])
                    if t == 0 or alt[1] < bl[1] * 0.5: bl = alt
                xl = bl[0]
                RL = dict(RR); RL['left_arm'], RL['left_forearm'], RL['left_hand'] = xl[0:3], xl[3:6], xl[6:9]
                el = float(np.linalg.norm(K.xf(fk.world('left_hand', RL, Q), (cf.HX, Y, Z)) - tgt)); wl = max(wl, el)
                for i, bn in enumerate(ARMS_L): keys[bn].append((t, [round(float(v), 2) for v in xl[3 * i:3 * i + 3]]))
            for i, bn in enumerate(ARMS_R): keys[bn].append((t, [round(float(v), 2) for v in xr[3 * i:3 * i + 3]]))
            if verbose and (er > 1.0 or el > 1.0): print('   miss', name, round(t, 2), round(er, 1), round(el, 1))
        for wins, arms, src in ((a['free'], ARMS_L, a['lf']), (a['rfree'], ARMS_R, a['rf'])):
            for t0, t1 in wins:
                for bn in arms:
                    pre = [k for k in keys[bn] if k[0] <= t0]; post = [k for k in keys[bn] if k[0] >= t1]
                    mid = [(float(t_), [round(float(q), 2) for q in v_]) for t_, v_ in src.get(bn, [])] or [((t0 + t1) / 2, pre[-1][1] if pre else [0, 0, 0])]
                    keys[bn] = pre + mid + post
        out[name] = keys
        print(f'  ik {name:12s} grip error right {worst:.2f}px left {wl:.2f}px')
    return out

def cloth(cf, rig_, A, ik):
    """cf.cape: dict(halves=[('R', -1, x_anchor), ...], segs=[(len, width)], top=y, z=z, spheres=fn(fk, R, Q) -> [(c, r)],
    breeze, prefix). Verlet: gravity, drag, back-breeze, body-sphere collisions, floor."""
    C = cf.cape; fk = K.FK(rig_); res = {}; lens = [s_[0] for s_ in C['segs']]; ns = len(lens)
    for name, a in A.items():
        T = a['T']; dt = 1 / 60; steps = int(round(T / dt)); loops = 3 if a['loop'] else 1
        def full_pose(t):
            R, Q = sample_body(a, t)
            for bn, ks in ik[name].items(): R[bn] = interp(ks, t, T if a['loop'] else None)
            return R, Q
        R, Q = full_pose(0); m = fk.world('chest', R, Q)
        parts = {h: np.array([K.xf(m, (ax_, C['top'] - sum(lens[:i]), C['z'] + 0.3 + 0.2 * i)) for i in range(ns + 1)]) for h, s, ax_ in C['halves']}
        prev = {h: parts[h].copy() for h in parts}; rec = {h: [] for h in parts}
        for L in range(loops):
            for i in range(steps + 1):
                t = i * dt; R, Q = full_pose(min(t, T))
                Cm = fk.world('chest', R, Q); sph = C['spheres'](fk, R, Q)
                for h, s, ax_ in C['halves']:
                    p, q = parts[h], prev[h]
                    anchor = K.xf(Cm, (ax_, C['top'], C['z'] + 0.2))
                    breeze = K.xd(Cm, (0, 0, 1)) * (C.get('breeze', 10.0) + 4.0 * math.sin(t * 2.1 + (0 if s < 0 else 1.3)))
                    nxt = p + (p - q) * 0.96 + (np.array([0, -70.0, 0]) + breeze) * dt * dt
                    for _ in range(6):
                        nxt[0] = anchor
                        for j in range(ns):
                            d = nxt[j + 1] - nxt[j]; dl = np.linalg.norm(d) + 1e-9; diff = (dl - lens[j]) / dl
                            if j == 0: nxt[j + 1] -= d * diff
                            else: nxt[j] += d * diff * 0.5; nxt[j + 1] -= d * diff * 0.5
                        for j in range(1, ns + 1):
                            for c, rad in sph:
                                v = nxt[j] - c; dl = np.linalg.norm(v)
                                if dl < rad + 0.5: nxt[j] = c + v / (dl + 1e-9) * (rad + 0.5)
                            if nxt[j][1] < 0.8: nxt[j][1] = 0.8
                    prev[h], parts[h] = p, nxt
                    if L == loops - 1 and i % 3 == 0: rec[h].append((min(t, T), Cm.copy(), nxt.copy()))
        ch = {}
        for h in rec:
            for j in range(ns): ch[f"{C['prefix']}{h}{j + 1}"] = []
            for t, Cm, pts in rec[h]:
                PM = Cm[:3, :3]
                for j in range(ns):
                    d = PM.T @ nrm(pts[j + 1] - pts[j])
                    rx = math.degrees(math.atan2(d[2], -d[1]))
                    rz = math.degrees(math.atan2(d[0], math.hypot(d[1], d[2])))
                    ang = np.clip(np.array([rx, 0.0, -rz]), [-80, -5, -45], [150, 5, 45])
                    ch[f"{C['prefix']}{h}{j + 1}"].append((t, ang))
                    PM = PM @ K.rot(-ang[0], ang[1], -ang[2])
        res[name] = ch
    return res

def cape_bones(r, cf, parent='chest', fur=None, sigil=None):
    """Builds cape<H><j> bone chains matching cf.cape."""
    C = cf.cape
    for h, s, ax_ in C['halves']:
        prev, y = parent, C['top']
        for j, (ln, w) in enumerate(C['segs']):
            bn = r.bone(f"{C['prefix']}{h}{j + 1}", prev, (ax_, y, C['z']))
            cx = ax_ + (s * (w / 2 - C.get('overlap', 0.7)) if len(C['halves']) > 1 else 0)
            r.box(bn, (cx, y - ln / 2, C['z'] + 0.2 + 0.02 * (s + 1)), (w, ln + 0.3, 0.8), 'cape' if j < len(C['segs']) - 2 else 'capehem')
            if fur: r.box(bn, (ax_ + s * (w - 0.9), y - ln / 2, C['z'] + 0.45), (1.4, ln + 0.3, 1.0), fur) if len(C['halves']) > 1 else None
            if sigil and j == 1: r.box(bn, (ax_ + s * 2.6, y - ln / 2, C['z'] + 0.7), (3.6, 3.6, 0.3), sigil, rot=(0, 0, 45))
            prev, y = bn, y - ln

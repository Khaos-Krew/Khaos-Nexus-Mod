"""Weapon clipping checker for GeckoLib bosses. Evaluates geo + animation the way GeckoLib does
(pivots/origins mirrored on X, rotation X/Y negated, Z then Y then X about the pivot) and reports
weapon cube samples that fall inside body cubes, per animation and time step."""
import json, math, sys, os
import numpy as np
A = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'khaosnexus')

def rot(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Z @ Y @ X

def M(R, t=(0, 0, 0)):
    m = np.eye(4); m[:3, :3] = R; m[:3, 3] = t; return m
def T(v): return M(np.eye(3), v)

def sample(ch, t, length):
    if not isinstance(ch, dict) or 'vector' in ch: return None
    ks = sorted(((float(k), (v['post'] if isinstance(v, dict) else v)) for k, v in ch.items()), key=lambda x: x[0])
    if t <= ks[0][0]: return ks[0][1]
    if t >= ks[-1][0]: return ks[-1][1]
    for i in range(len(ks) - 1):
        a, b = ks[i], ks[i + 1]
        if a[0] <= t <= b[0]:
            f = (t - a[0]) / (b[0] - a[0]) if b[0] > a[0] else 0
            # catmull-rom with neighbours
            p0 = ks[max(0, i - 1)][1]; p3 = ks[min(len(ks) - 1, i + 2)][1]
            return [0.5 * ((2 * a[1][j]) + (-p0[j] + b[1][j]) * f + (2 * p0[j] - 5 * a[1][j] + 4 * b[1][j] - p3[j]) * f * f
                           + (-p0[j] + 3 * a[1][j] - 3 * b[1][j] + p3[j]) * f ** 3) for j in range(3)]
    return ks[-1][1]

def load(name):
    g = json.load(open(f'{A}/geo/entity/{name}.geo.json'))['minecraft:geometry'][0]['bones']
    an = json.load(open(f'{A}/animations/entity/{name}.animation.json'))['animations']
    return {b['name']: b for b in g}, [b['name'] for b in g], an

def pose(bones, order, layers, t):
    W = {}
    for n in order:
        b = bones[n]; p = np.array([-b['pivot'][0], b['pivot'][1], b['pivot'][2]], float)
        br = b.get('rotation', [0, 0, 0]); r = [0, 0, 0]; q = [0, 0, 0]
        for an, tt in layers:  # later controller overrides a channel it animates (GeckoLib)
            ch = an['bones'].get(n, {})
            if 'rotation' in ch: r = sample(ch['rotation'], tt, an['animation_length']) or r
            if 'position' in ch: q = sample(ch['position'], tt, an['animation_length']) or q
        rx, ry, rz = -br[0] - r[0], -br[1] - r[1], br[2] + r[2]; pos = [-q[0], q[1], q[2]]
        local = T(pos) @ T(p) @ M(rot(rx, ry, rz)) @ T(-p)
        W[n] = (W[b['parent']] if b.get('parent') else np.eye(4)) @ local
    return W

def cubes(bones, n, W):
    out = []
    for c in bones[n].get('cubes', []):
        o = np.array([-(c['origin'][0] + c['size'][0]), c['origin'][1], c['origin'][2]], float); s = np.array(c['size'], float)
        inf = c.get('inflate', 0); o -= inf; s += 2 * inf
        cm = np.eye(4)
        if 'rotation' in c:
            cp = np.array([-c['pivot'][0], c['pivot'][1], c['pivot'][2]], float); cr = c['rotation']
            cm = T(cp) @ M(rot(-cr[0], -cr[1], cr[2])) @ T(-cp)
        out.append((W[n] @ cm, o, s))
    return out

def pts(cl, step=0.8):
    P = []
    for m, o, s in cl:
        g = [np.linspace(o[i], o[i] + s[i], max(2, int(s[i] / step) + 1)) for i in range(3)]
        xx, yy, zz = np.meshgrid(*g, indexing='ij'); q = np.stack([xx.ravel(), yy.ravel(), zz.ravel(), np.ones(xx.size)])
        P.append((m @ q)[:3].T)
    return np.concatenate(P) if P else np.zeros((0, 3))

def inside(P, cl, margin=0.3):
    hit = np.zeros(len(P), bool)
    for m, o, s in cl:
        inv = np.linalg.inv(m); q = (inv @ np.c_[P, np.ones(len(P))].T)[:3].T
        hit |= np.all((q > o + margin) & (q < o + s - margin), axis=1)
    return hit

def descendants(bones, order, root):
    out = {root}
    for n in order:
        if bones[n].get('parent') in out: out.add(n)
    return out

def check(name, weapon, body, combos):
    bones, order, an = load(name)
    wb = [b for b in descendants(bones, order, weapon)]
    worst = []
    for label, layer_names in combos:
        anims = [an[f'animation.{name}.{x}'] for x in layer_names]
        L = max(a['animation_length'] for a in anims)
        for i in range(int(L / 0.05) + 1):
            t = i * 0.05; W = pose(bones, order, [(a, t % a['animation_length'] if a.get('loop') else t) for a in anims], t)
            P = pts([c for b in wb for c in cubes(bones, b, W)])
            for bb in body:
                n = int(inside(P, cubes(bones, bb, W)).sum())
                if n: worst.append((label, round(t, 2), bb, n))
    return worst

if __name__ == '__main__':
    name = sys.argv[1]
    if name == 'spire_guardian':
        weapon, body = 'halberd', ['head', 'chest', 'torso', 'hips', 'jaw', 'spire0', 'tasset_front', 'right_leg', 'left_leg']
        combos = [('idle_combat', ['idle_combat']), ('walk_combat', ['walk_combat']), ('idle', ['idle']), ('walk', ['walk']),
                  ('attack', ['idle_combat', 'attack']), ('spin', ['idle_combat', 'spin']), ('sweep', ['idle_combat', 'sweep']),
                  ('barrier', ['idle_combat', 'barrier']), ('roar', ['idle_combat', 'roar'])]
    else:
        weapon, body = sys.argv[2], ['head', 'chest', 'torso', 'hips', 'jaw', 'right_leg', 'left_leg']
        combos = [(x, ['idle_combat', x]) if x not in ('idle_combat', 'walk_combat', 'idle', 'walk') else (x, [x])
                  for x in ['idle_combat', 'walk_combat', 'attack', 'slam', 'lunge', 'draw', 'roar']]
    res = check(name, weapon, body, combos)
    agg = {}
    for l, t, b, n in res: agg.setdefault((l, b), []).append((t, n))
    for (l, b), v in sorted(agg.items()): print(f'{l:12s} {b:12s} frames={len(v):3d} max_pts={max(n for _, n in v)} t={v[0][0]}..{v[-1][0]}')
    print('CLEAN' if not res else f'{len(res)} clipping frame-bone hits')

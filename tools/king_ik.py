"""Forward kinematics + numeric IK for the Hollow King art pass (v3).
FK works in rig authoring space (the space rig_king.py builds in) and matches clipcheck.py / GeckoLib once
write_anims() has flipped the channels: local = T(pos') T(p) R(-x, y, -z) T(-p), pos' = (-qx, qy, qz), R = Rz Ry Rx.
Rest rotations follow clipcheck: R(rest_x - x, y - rest_y, rest_z - z)."""
import math
import numpy as np

def rot(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X

def local(pivot, r, q=(0, 0, 0), rest=(0, 0, 0)):
    m = np.eye(4); R = rot(rest[0] - r[0], r[1] - rest[1], rest[2] - r[2]); p = np.array(pivot, float)
    m[:3, :3] = R; m[:3, 3] = p - R @ p + np.array([-q[0], q[1], q[2]])
    return m

class FK:
    def __init__(s, rig):
        s.b = {n: (rig.bones[n]['parent'], rig.bones[n]['pivot'], rig.bones[n]['rotation']) for n in rig.order}
    def chain(s, n):
        out = []
        while n: out.append(n); n = s.b[n][0]
        return out[::-1]
    def world(s, n, R, Q=None):
        Q = Q or {}; m = np.eye(4)
        for bn in s.chain(n): m = m @ local(s.b[bn][1], R.get(bn, (0, 0, 0)), Q.get(bn, (0, 0, 0)), s.b[bn][2])
        return m

def xf(m, p): return (m @ np.array([p[0], p[1], p[2], 1.0]))[:3]
def xd(m, d): return m[:3, :3] @ np.array(d, float)

def solve(f, x0, lo, hi, iters=40, lam=1e-2):
    """Levenberg-Marquardt with numeric jacobian and box limits."""
    x = np.array(x0, float); lo = np.array(lo, float); hi = np.array(hi, float)
    x = np.clip(x, lo, hi); r = f(x); cost = r @ r
    for _ in range(iters):
        J = np.zeros((len(r), len(x)))
        for i in range(len(x)):
            d = np.zeros(len(x)); d[i] = 0.5
            J[:, i] = (f(np.clip(x + d, lo, hi)) - r) / 0.5
        A = J.T @ J; g = J.T @ r; ok = False
        for _k in range(12):
            step = np.linalg.solve(A + lam * np.diag(np.diag(A) + 1e-6), -g)
            xn = np.clip(x + step, lo, hi); rn = f(xn); cn = rn @ rn
            if cn < cost: x, r, cost = xn, rn, cn; lam = max(lam / 3, 1e-6); ok = True; break
            lam *= 4
        if not ok or np.abs(step).max() < 1e-3: break
    return x, cost

"""Khaos Nexus item art v2 (32x32, house size). Each sprite is built from layered shapes; every layer is lit from the
top-left with a fixed light, quantised to a hand-picked 6-tone hue-shifted ramp (no gradients/noise), then gets a
1px rim highlight on lit edges, a core shadow on unlit edges and a selective outline (darkest tone of the touching
layer, not black). Specular pixels and details are placed by hand. Deterministic, no randomness."""
import math, os
import numpy as np
from PIL import Image

N = 32
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'khaosnexus', 'textures', 'item')
L = np.array([-0.55, -0.7, 0.85]); L /= np.linalg.norm(L)

def hx(s): s = s.lstrip('#'); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))
def R(*c): return [hx(x) for x in c]
# 6-tone ramps: [outline, shadow, mid-dark, mid, light, highlight], hue-shifted
RAMPS = {
 'steel':  R('#14121c', '#2c2b3c', '#4a4c60', '#6f7488', '#a3a9bb', '#e4e8f0'),
 'iron':   R('#120f14', '#2a2229', '#463b42', '#675a5e', '#938583', '#c9bdb4'),
 'gold':   R('#2a1406', '#6a3410', '#a8601a', '#d99a2b', '#f4cf5a', '#fff4b8'),
 'bronze': R('#1e0e08', '#4f2614', '#80461f', '#b06f32', '#d9a05a', '#f6d79a'),
 'void':   R('#0d0618', '#2a0f45', '#4b1d74', '#7232a8', '#a764d8', '#e2b8ff'),
 'rift':   R('#1a0508', '#4d0d16', '#8a1726', '#c82e3c', '#f06a5e', '#ffc7a8'),
 'teal':   R('#04161c', '#0b3a44', '#126a72', '#21a3a0', '#5fd9c4', '#d2fff2'),
 'cyan':   R('#060f24', '#0f2a5c', '#1a4f9a', '#2c86d2', '#69c4f2', '#d4f4ff'),
 'green':  R('#061608', '#123a14', '#21661f', '#3c9a2c', '#7fd04c', '#dcffa8'),
 'bone':   R('#14160c', '#34392a', '#5a6248', '#8a936c', '#bfc79a', '#f2f4d6'),
 'sick':   R('#04140c', '#0b3420', '#145a34', '#23884a', '#4fc46a', '#c2ffb8'),
 'leather':R('#170b08', '#3a1d14', '#5c3020', '#80482c', '#a8693e', '#d49a64'),
 'paper':  R('#2a2014', '#5e4c34', '#8e7a58', '#bba77c', '#ddcfa4', '#fbf4d8'),
 'blood':  R('#160204', '#420710', '#7a0c1a', '#b8162a', '#e8443e', '#ffb09a'),
 'white':  R('#1c1a24', '#4a4858', '#7c7a8c', '#aeacbc', '#d8d6e2', '#ffffff'),
 'amber':  R('#200c02', '#5a2606', '#96460a', '#d07414', '#f4a83a', '#ffe2a0'),
 'pink':   R('#1e0612', '#521232', '#8a2456', '#c24886', '#ec84b4', '#ffd2ea'),
 'night':  R('#05050c', '#12121e', '#1e1e30', '#2c2c44', '#44446a', '#7070a0'),
}

class Sprite:
    def __init__(self):
        self.idx = np.full((N, N), -1, int); self.ramp = np.full((N, N), '', object); self.layer = np.full((N, N), -1, int); self.n = 0
    def grid(self):
        y, x = np.mgrid[0:N, 0:N] + 0.5; return x, y
    def shape(self, mask, ramp, normal='dome', bias=0.0, rim=True, shadow=True, flat=None, height=None):
        """normal: 'dome' (from distance to edge), 'facet' (sector normals about centre), or (nx,ny,nz) flat tilt.
        height: optional array to use instead of distance for dome normals."""
        m = mask.astype(bool)
        if not m.any(): return m
        if (isinstance(normal, str) and normal == 'dome') or height is not None:
            h = height if height is not None else dist(m)
            h = np.minimum(h, 3.2) if height is None else h
            gy, gx = np.gradient(h.astype(float))
            nrm = np.dstack([-gx, -gy, np.full_like(gx, 0.9)])
        elif isinstance(normal, tuple) and len(normal) == 3:
            nrm = np.broadcast_to(np.array(normal, float), (N, N, 3)).copy()
        else:
            nrm = normal
        nrm = nrm / np.linalg.norm(nrm, axis=2, keepdims=True)
        lam = np.clip((nrm @ L), 0, 1)
        tone = np.clip(np.floor((0.05 + 1.05 * lam + bias) * 4.2) + 1, 1, 4).astype(int)
        if rim:  # lit edge: pixel whose up/left neighbour is outside this layer
            up = np.roll(m, 1, 0); up[0] = False; lf = np.roll(m, 1, 1); lf[:, 0] = False
            tone[m & (~up | ~lf)] = np.minimum(tone[m & (~up | ~lf)] + 1, 5)
        if shadow:
            dn = np.roll(m, -1, 0); dn[-1] = False; rt = np.roll(m, -1, 1); rt[:, -1] = False
            e = m & (~dn | ~rt) & ~(~np.roll(m, 1, 0) | ~np.roll(m, 1, 1))
            tone[e] = np.maximum(tone[e] - 1, 1)
        self.idx[m] = tone[m]; self.ramp[m] = ramp; self.n += 1; self.layer[m] = self.n
        return m
    def px(self, pts, ramp, tone):
        for x, y in pts:
            if 0 <= x < N and 0 <= y < N: self.idx[y, x] = tone; self.ramp[y, x] = ramp; self.layer[y, x] = self.layer[y, x] if self.layer[y, x] > 0 else 99
    def line(self, p0, p1, ramp, tone, w=1):
        (x0, y0), (x1, y1) = p0, p1; n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        pts = set()
        for i in range(n):
            t = i / max(1, n - 1); x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
            for dx in range(w):
                pts.add((x + dx, y))
        self.px(pts, ramp, tone)
    def image(self, inner_lines=True):
        img = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = img.load()
        filled = self.idx >= 0
        for y in range(N):
            for x in range(N):
                if filled[y, x]:
                    p[x, y] = RAMPS[self.ramp[y, x]][self.idx[y, x]] + (255,)
        if inner_lines:  # selective inner outline where an upper layer meets a lower one (bottom/right only)
            for y in range(N - 1):
                for x in range(N - 1):
                    if not filled[y, x]: continue
                    for dx, dy in ((1, 0), (0, 1)):
                        a, b = self.layer[y, x], self.layer[y + dy, x + dx]
                        if filled[y + dy, x + dx] and a > 0 and b > 0 and a > b and a != 99 and self.ramp[y, x] != self.ramp[y + dy, x + dx]:
                            p[x + dx, y + dy] = RAMPS[self.ramp[y, x]][1] + (255,)
        out = []
        for y in range(N):
            for x in range(N):
                if filled[y, x]: continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < N and 0 <= yy < N and filled[yy, xx]:
                        out.append((x, y, self.ramp[yy, xx])); break
        for x, y, r in out: p[x, y] = RAMPS[r][0] + (255,)
        return img

def dist(m):
    ys, xs = np.nonzero(~m)
    if len(xs) == 0: return np.full(m.shape, 9.0)
    out = np.zeros(m.shape)
    yy, xx = np.nonzero(m)
    border = np.c_[xs, ys]
    # include virtual outside border
    ext = [(x, -1) for x in range(N)] + [(x, N) for x in range(N)] + [(-1, y) for y in range(N)] + [(N, y) for y in range(N)]
    border = np.r_[border, np.array(ext)]
    for x, y in zip(xx, yy):
        out[y, x] = np.sqrt(((border - [x, y]) ** 2).sum(1)).min()
    return out

X, Y = np.mgrid[0:N, 0:N][::-1] + 0.5
def poly(pts):
    """Even-odd polygon fill sampled at pixel centres."""
    m = np.zeros((N, N), bool); pts = list(pts)
    for i in range(len(pts)):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % len(pts)]
        cond = ((y0 > Y) != (y1 > Y)) & (X < (x1 - x0) * (Y - y0) / ((y1 - y0) if y1 != y0 else 1e-9) + x0)
        m ^= cond
    return m
def ell(cx, cy, rx, ry, a=0.0):
    c, s = math.cos(a), math.sin(a); dx, dy = X - cx, Y - cy; u = dx * c + dy * s; v = -dx * s + dy * c
    return (u / rx) ** 2 + (v / ry) ** 2 <= 1
def facet_normals(cx, cy, n, rot=0.0, table=0.0, rr=None):
    """Gem facets: each angular sector gets one tilted normal; a flat table inside `table` radius."""
    a = np.arctan2(Y - cy, X - cx) - rot; seg = 2 * math.pi / n; mid = np.floor(a / seg) * seg + seg / 2 + rot
    nrm = np.dstack([np.cos(mid) * 0.9, np.sin(mid) * 0.9, np.full((N, N), 0.75)])
    if table:
        d = np.hypot(X - cx, Y - cy); t = d < table
        nrm[t] = [-0.15, -0.2, 1.0]
    return nrm

def save(img, name):
    p = os.path.join(ROOT, name + '.png'); os.makedirs(os.path.dirname(p), exist_ok=True); img.save(p, optimize=True)

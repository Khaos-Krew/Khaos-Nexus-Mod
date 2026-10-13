"""v3 (GeckoLib): generates geo models, animations and 2x-density textures for the 6 Rift mobs,
plus the animated Nexus Rift (gate) visuals. Specs: /workspace/khaos-nexus/mod-art/concepts/concepts.md.
Units are model px (16 px = 1 block), y-up, feet at y=0, the model faces north (-Z).
Every texture is painted per face in Python and saved as lossless PNG."""
import json, math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'khaosnexus')
def out(*p):
    path = os.path.join(RES, *p); os.makedirs(os.path.dirname(path), exist_ok=True); return path
def hx(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
def mul(c, k): return tuple(max(0, min(255, int(q * k))) for q in c[:3])
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

# ----------------------------------------------------------------------------- rigs
class Rig:
    def __init__(self): self.bones = {}; self.order = []
    def bone(self, name, parent, pivot, rot=(0, 0, 0)):
        self.bones[name] = dict(name=name, parent=parent, pivot=list(pivot), rotation=list(rot), cubes=[]); self.order.append(name); return name
    def cube(self, bone, origin, size, role, inflate=0.0, rot=None, pivot=None):
        c = dict(origin=list(origin), size=list(size), role=role, inflate=inflate)
        if rot:
            c['rot'] = list(rot)
            c['pivot'] = list(pivot) if pivot else [origin[0] + size[0] / 2, origin[1] + size[1] / 2, origin[2] + size[2] / 2]
        self.bones[bone]['cubes'].append(c)
        return c
    def box(self, bone, center, size, role, rot=None, pivot=None, inflate=0.0):
        o = (center[0] - size[0] / 2, center[1] - size[1] / 2, center[2] - size[2] / 2)
        return self.cube(bone, o, size, role, inflate, rot, pivot or center)

def humanoid(r, L, hipx, leg, torso, hs, arm, sx, hand_h, hunch=0, hip_role='belt'):
    tw, th, td = torso; aw, ah, ad = arm; lw, ld = leg
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * hipx
        r.bone(f'{side}_leg', 'root', (x, L, 0)); r.cube(f'{side}_leg', (x - lw / 2, L / 2, -ld / 2), (lw, L / 2, ld), 'limb')
        r.bone(f'{side}_shin', f'{side}_leg', (x, L / 2, 0)); r.cube(f'{side}_shin', (x - lw / 2 + 0.25, 0, -ld / 2 + 0.25), (lw - 0.5, L / 2, ld - 0.5), 'limb')
    r.bone('hips', 'root', (0, L, 0)); r.cube('hips', (-tw / 2, L - 1, -td / 2), (tw, 3, td), hip_role)
    r.bone('torso', 'hips', (0, L, 0), (hunch, 0, 0)); r.cube('torso', (-tw / 2 + 0.5, L, -td / 2 + 0.5), (tw - 1, th * 0.45, td - 1), 'body')
    cy = L + th * 0.45
    r.bone('chest', 'torso', (0, cy, 0)); r.cube('chest', (-tw / 2, cy, -td / 2), (tw, th * 0.55, td), 'chest')
    ny = L + th
    r.bone('head', 'chest', (0, ny, 0)); r.cube('head', (-hs / 2, ny + hs * 0.3, -hs / 2), (hs, hs * 0.7, hs), 'skull')
    r.bone('jaw', 'head', (0, ny + hs * 0.3, hs * 0.3)); r.cube('jaw', (-hs / 2 + 0.5, ny, -hs / 2 + 0.5), (hs - 1, hs * 0.3, hs - 1), 'jaw')
    sy = ny - 2
    for side, s in (('right', -1), ('left', 1)):
        x = s * sx
        r.bone(f'{side}_arm', 'chest', (x, sy, 0)); r.cube(f'{side}_arm', (x - aw / 2, sy - ah * 0.5, -ad / 2), (aw, ah * 0.5 + 2, ad), 'limb')
        ey = sy - ah * 0.5
        r.bone(f'{side}_forearm', f'{side}_arm', (x, ey, 0)); r.cube(f'{side}_forearm', (x - aw / 2 + 0.25, ey - (ah * 0.5 - hand_h), -ad / 2 + 0.25), (aw - 0.5, ah * 0.5 - hand_h, ad - 0.5), 'limb')
        hy = ey - (ah * 0.5 - hand_h)
        r.bone(f'{side}_hand', f'{side}_forearm', (x, hy, 0)); r.cube(f'{side}_hand', (x - aw / 2, hy - hand_h, -ad / 2), (aw, hand_h, ad), 'fist')
    return dict(L=L, th=th, hs=hs, sy=sy, ny=ny, sx=sx, aw=aw, ah=ah, tw=tw, td=td, hand_y=sy - ah)

def rig_ghoul():
    r = Rig(); d = humanoid(r, 12, 2, (4, 4), (8, 12, 4), 8, (4, 17, 4), 6, 3, hunch=24, hip_role='body')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 6; hy = d['hand_y']
        for i, ox in enumerate((-1.5, 0, 1.5)):
            b = r.bone(f'{side}_claw{i}', f'{side}_hand', (x + ox, hy, -1), (-15, 0, 0)); r.cube(b, (x + ox - 0.5, hy - 5, -1.5), (1, 5, 1), 'claw')
        r.cube(f'{side}_shin', (s * 2 - 2, 0, -4), (4, 2, 6), 'foot')
        r.cube(f'{side}_arm', (x - 1 + s, d['sy'], -1.5), (2, 3, 3), 'spike')  # shoulder bone shard
    for i in range(3): r.cube('chest', (-3, 17 + i * 2, -2.6), (6, 1, 0.6), 'rib')
    for i in range(4): r.cube('chest', (-0.5, 18 + i * 1.6, 2), (1, 1, 1.5), 'spike')
    return r, d

def rig_stalker():
    r = Rig(); d = humanoid(r, 12, 1.9, (4, 4), (8, 12, 4), 8, (4, 12, 4), 5, 2, hip_role='robe')
    r.cube('head', (-5, d['ny'] - 1, -5), (10, 10, 10), 'hood', 0.0)
    r.bone('hood_tip', 'head', (0, d['ny'] + 9, 2), (-25, 0, 0)); r.cube('hood_tip', (-1, d['ny'] + 9, 1), (2, 4, 2), 'hood')
    r.cube('chest', (-5, 17, -3), (10, 7, 6), 'robe')
    r.cube('chest', (-4.5, 23, -3.5), (9, 2, 7), 'collar')
    for name, o, s in (('robe_front', (-5, -2, -3.4), (10, 14, 1)), ('robe_back', (-5, -2, 2.4), (10, 14, 1)),
                       ('robe_right', (-5.6, -2, -3), (1, 13, 6)), ('robe_left', (4.6, -2, -3), (1, 13, 6))):
        r.bone(name, 'hips', (0, 12, o[2] + 0.5)); r.cube(name, (o[0], o[1] + 0, o[2]), (s[0], s[1], s[2]), 'robe')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 5; hy = d['hand_y']
        r.cube(f'{side}_arm', (x - 2.5, d['sy'] - 4, -2.5), (5, 6, 5), 'sleeve')
        b = r.bone(f'{side}_blade', f'{side}_hand', (x, hy + 1, 0), (20, 0, s * 8))
        r.cube(b, (x - 0.5, hy - 18, -1), (1, 19, 2), 'blade'); r.cube(b, (x - 1.5, hy - 1, -1.5), (3, 1, 3), 'guard')
    return r, d

def rig_caster():
    r = Rig(); d = humanoid(r, 12, 1.9, (4, 4), (8, 12, 4), 8, (4, 12, 4), 5, 2, hip_role='robe')
    ny = d['ny']
    r.cube('head', (-5, ny - 1, -5), (10, 10, 10), 'hood')
    r.bone('hood_cone', 'head', (0, ny + 9, 0)); r.cube('hood_cone', (-3, ny + 9, -3), (6, 4, 6), 'hood')
    r.bone('hood_cone2', 'hood_cone', (0, ny + 13, 0.5), (-18, 0, 0)); r.cube('hood_cone2', (-2, ny + 13, -1.5), (4, 4, 4), 'hood')
    r.bone('hood_cone3', 'hood_cone2', (0, ny + 17, 1), (-25, 0, 0)); r.cube('hood_cone3', (-1, ny + 17, 0), (2, 3, 2), 'hood')
    r.cube('chest', (-6, 22, -4), (12, 3, 8), 'collar')
    r.cube('chest', (-4.5, 17, -2.5), (9, 6, 5), 'robe')
    r.bone('robe_upper', 'hips', (0, 12, 0)); r.cube('robe_upper', (-6, 4, -4), (12, 9, 8), 'robe')
    r.bone('robe_lower', 'robe_upper', (0, 4, 0)); r.cube('robe_lower', (-7, 0, -5), (14, 6, 10), 'robe')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 5; r.cube(f'{side}_forearm', (x - 3, d['sy'] - 12, -3), (6, 8, 6), 'sleeve')
    r.bone('orb', 'chest', (0, 15, -9)); r.cube('orb', (-1.5, 13.5, -10.5), (3, 3, 3), 'orb'); r.cube('orb', (-2, 13, -11), (4, 4, 4), 'orbshell', 0.2)
    for i in range(4):
        a = i * math.pi / 2; b = r.bone(f'orb_rune{i}', 'orb', (0, 15, -9))
        r.cube(b, (math.cos(a) * 4 - 0.5, 15 + math.sin(a) * 4 - 0.5, -9.5), (1, 1, 1), 'orb')
    return r, d

def rig_brute():
    r = Rig(); d = humanoid(r, 16, 4.5, (8, 8), (16, 18, 10), 12, (8, 22, 8), 13, 8, hip_role='belt')
    ny = d['ny']
    r.cube('chest', (-9, 24, -6), (18, 10, 12), 'chest', 0.0)
    r.bones['head']['pivot'][1] -= 3
    for c in r.bones['head']['cubes'] + r.bones['jaw']['cubes']: c['origin'][1] -= 3
    r.bones['jaw']['pivot'][1] -= 3
    r.cube('jaw', (-4.5, ny - 3, -6.6), (1, 3, 1), 'claw'); r.cube('jaw', (3.5, ny - 3, -6.6), (1, 3, 1), 'claw')
    r.cube('hips', (-2, 14, -5.6), (4, 4, 1), 'sigil')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 13
        r.cube(f'{side}_arm', (x - 5, d['sy'] - 2, -5), (10, 6, 10), 'pauldron')
        r.cube(f'{side}_hand', (x - 5, d['hand_y'] - 1, -5), (10, 9, 10), 'fist')
        for i, (ox, oz, sz) in enumerate(((-2, -2, 3), (1, 1, 4), (-1, 2, 2))):
            r.cube(f'{side}_arm', (x + ox, d['sy'] + 4, oz), (sz * 0.7, sz * 1.6, sz * 0.7), 'spike')
        r.cube(f'{side}_forearm', (x + s * 3.5, d['sy'] - 16, -1), (2, 4, 2), 'spike')
        r.cube(f'{side}_shin', (s * 4.5 - 4.5, 4, -4.8), (9, 4, 1), 'plate')
    for (ox, oy, oz, sz) in ((-4, 2, -2, 3), (2, 3, 0, 4), (-1, 0, 3, 2)):
        r.cube('head', (ox, ny + 9 + oy - 3, oz), (sz * 0.7, sz * 1.6, sz * 0.7), 'spike')
    for (ox, oy) in ((-6, 0), (-2, 3), (2, 1), (5, 4), (0, -3)):
        r.cube('chest', (ox, 26 + oy, 5.5), (2.5, 6, 2.5), 'spike')
    return r, d

SWORD_TILT = -10
def rig_warden():
    """Khaos Warden of the Rift (v4, hand-laid): layered plate armour, flared pauldrons, horned crowned helm,
    fanged visor, segmented gauntlets with fingers, tattered cape, tapered greatsword."""
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    for side, s in (('right', -1), ('left', 1)):
        x = s * 5.5
        r.bone(f'{side}_leg', 'root', (x, 30, 0))
        B(f'{side}_leg', (x, 22.5, 0), (8, 15, 8), 'limb')
        B(f'{side}_leg', (x, 24, -4.3), (9, 10, 1.5), 'plate', rot=(-4, 0, 0))
        B(f'{side}_leg', (x + s * 4.3, 24, 0), (1.5, 11, 8), 'plate', rot=(0, 0, s * 4))
        r.bone(f'{side}_shin', f'{side}_leg', (x, 15, 0))
        B(f'{side}_shin', (x, 8, 0), (7.5, 15, 7.5), 'limb')
        B(f'{side}_shin', (x, 8.5, -4.1), (8.5, 10, 1.5), 'gauntlet')
        B(f'{side}_shin', (x, 15, -4.4), (6, 5, 3), 'gauntlet', rot=(-15, 0, 0))
        B(f'{side}_shin', (x, 16.5, -6.3), (2, 4.5, 2), 'crown', rot=(-55, 0, 0))
        B(f'{side}_shin', (x, 2, -1.5), (9, 4, 12), 'gauntlet')
        B(f'{side}_shin', (x, 1.3, -8.3), (8, 2.6, 3.5), 'gauntlet', rot=(12, 0, 0))
    r.bone('hips', 'root', (0, 30, 0))
    B('hips', (0, 31, 0), (20, 4, 11), 'belt'); B('hips', (0, 31, -6.0), (5.6, 4.6, 0.8), 'guard'); B('hips', (0, 31, -6.3), (3.4, 2.8, 0.6), 'rune')
    for s_ in (-1, 1): B('hips', (s_ * 7.5, 31, -5.8), (1.4, 4.2, 0.8), 'guard')
    r.bone('tasset_front', 'hips', (0, 31, -6))
    r.bone('tasset_back', 'hips', (0, 31, 6))
    for s in (-1, 1):
        B('tasset_front', (s * 4.7, 27, -6.4), (9, 7, 1), 'pauldron', rot=(-8, 0, s * -5))
        B('tasset_front', (s * 4.5, 21.5, -6.9), (8, 6, 1), 'pauldron', rot=(-12, 0, s * -7))
        B('tasset_back', (s * 4.7, 26, 6.4), (9, 9, 1), 'pauldron', rot=(8, 0, s * 5))
    for side, s in (('right', -1), ('left', 1)):
        r.bone(f'tasset_{side}', 'hips', (s * 10.5, 31, 0))
        B(f'tasset_{side}', (s * 11, 26, 0), (1, 10, 9), 'pauldron', rot=(0, 0, s * 10))
    r.bone('torso', 'hips', (0, 30, 0))
    for i, w in enumerate((17, 17.5, 18)):
        B('torso', (0, 32.5 + i * 4, 0), (w, 4, 9 + i * 0.5), 'plate')
    r.bone('chest', 'torso', (0, 43, 0))
    B('chest', (0, 51.5, 0), (20, 17, 11), 'chest')
    for s in (-1, 1):
        B('chest', (s * 4.8, 53, -6), (10, 9, 2.5), 'breastplate', rot=(0, s * -12, 0))
    B('chest', (0, 50, -7), (2, 13, 1.5), 'crown'); B('chest', (0, 47.5, -7.4), (5, 6, 1), 'rune')
    B('chest', (0, 60, 0), (14, 4, 13), 'helm'); B('chest', (0, 52, 6.2), (18, 14, 2), 'plate')
    for i in range(3): B('chest', (0, 46 + i * 5, 7.6), (2, 5, 2), 'crown', rot=(40, 0, 0))
    B('chest', (0, 60.5, 5), (22, 4, 6), 'cape')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 15
        r.bone(f'{side}_arm', 'chest', (x, 58, 0))
        B(f'{side}_arm', (x, 51, 0), (9, 14, 9), 'limb'); B(f'{side}_arm', (x, 52, 0), (10, 8, 10), 'gauntlet')
        B(f'{side}_arm', (s * 16, 61, 0), (15, 6, 15), 'pauldron', rot=(0, 0, s * 18))
        B(f'{side}_arm', (s * 18, 57.5, 0), (13, 5, 13), 'pauldron', rot=(0, 0, s * 28))
        B(f'{side}_arm', (s * 19.5, 54.5, 0), (11, 4, 11), 'pauldron', rot=(0, 0, s * 38))
        for j, z in enumerate((-4, 0, 4)):
            B(f'{side}_arm', (s * (13 + j * 0.5), 66 + (1 if j == 1 else 0), z), (2.5, 8 + (2 if j == 1 else 0), 2.5), 'crown', rot=(0, 0, s * -18))
        r.bone(f'{side}_forearm', f'{side}_arm', (x, 44, 0))
        B(f'{side}_forearm', (x, 37, 0), (8.5, 14, 8.5), 'limb'); B(f'{side}_forearm', (x, 38, 0), (10, 9, 10), 'gauntlet')
        B(f'{side}_forearm', (x, 33.5, 0), (11, 3, 11), 'gauntlet')
        B(f'{side}_forearm', (x, 44, 4.5), (6, 6, 4), 'gauntlet', rot=(20, 0, 0))
        B(f'{side}_forearm', (x, 44.5, 7.6), (2, 6, 2), 'crown', rot=(60, 0, 0))
        r.bone(f'{side}_hand', f'{side}_forearm', (x, 32, 0))
        B(f'{side}_hand', (x, 29, 0), (8, 6, 8), 'fist'); B(f'{side}_hand', (x, 30, -3.6), (9, 3, 3.5), 'gauntlet')
        for i in range(4):
            fx_ = x + s * 0.2; fy_ = 31.2 - i * 2.1
            B(f'{side}_hand', (fx_, fy_, -4.6), (7.6, 1.8, 2.0), 'gauntlet')          # curled finger
            B(f'{side}_hand', (fx_ + s * 3.4, fy_, -4.9), (1.2, 1.2, 1.2), 'crown')   # knuckle stud
        B(f'{side}_hand', (x - s * 4.6, 28, -1.5), (2, 4.5, 2.2), 'gauntlet', rot=(0, 0, s * -30))
    r.bone('head', 'chest', (0, 60, 0))
    B('head', (0, 70, 0), (16, 16, 16), 'helm')
    B('head', (0, 73.5, -7.6), (17, 3, 4), 'helmspire', rot=(-10, 0, 0))
    B('head', (0, 69, -8.4), (15, 5, 1.5), 'visorplate')
    for s in (-1, 1): B('head', (s * 8.6, 67, -1), (2, 11, 13), 'helm', rot=(0, s * -8, 0))
    for xx, h in ((-6, 8), (-3, 11), (0, 15), (3, 11), (6, 8)):
        B('head', (xx, 78 + h / 2, -1), (2.6, h, 2.6), 'crown', rot=(0, 0, -xx * 3))
    for s in (-1, 1):
        for (c, sz, rt) in (((s * 10, 75, 1), 3.6, (-30, 0, s * -20)), ((s * 12.5, 77.8, 3.2), 3.0, (-50, 0, s * -35)), ((s * 13.6, 81.2, 5.6), 2.2, (-70, 0, s * -50))):
            B('head', c, (sz, sz, 6), 'spike', rot=rt)
    r.bone('jaw', 'head', (0, 62, -2))
    B('jaw', (0, 62.8, -3.5), (14, 5.5, 9), 'helm', rot=(8, 0, 0))
    for i, xx in enumerate((-5, -3, -1, 1, 3, 5)):
        big = i in (0, 5)
        B('jaw', (xx, 65.3 + (0.9 if big else 0), -8.25), (1.1 if big else 0.9, 3.2 if big else 1.8, 1), 'claw', rot=(-8, 0, 0))
    r.bone('cape1', 'chest', (0, 60, 6.5), (6, 0, 0)); B('cape1', (0, 52.5, 7), (24, 15, 1), 'cape')
    r.bone('cape2', 'cape1', (0, 45, 7)); B('cape2', (0, 37.5, 7.2), (25, 15, 1), 'cape')
    r.bone('cape3', 'cape2', (0, 30, 7.2)); B('cape3', (0, 23, 7.4), (26, 14, 1), 'capehem')
    def sword_parts(bone, x, g, z):
        """Greatsword built around its grip centre (x, g, z); blade points +y."""
        B(bone, (x, g, z), (2.4, 13, 2.4), 'grip'); B(bone, (x, g - 7.4, z), (3.6, 2.4, 3.6), 'guard')
        B(bone, (x, g - 9.0, z), (2, 1.2, 2), 'rune')
        B(bone, (x, g + 7.6, z), (13, 2.2, 4.2), 'guard'); B(bone, (x, g + 7.6, z), (3.4, 3.4, 4.6), 'rune')
        for s_ in (-1, 1): B(bone, (x + s_ * 7.6, g + 9.2, z), (3.6, 2.6, 3), 'guard', rot=(0, 0, s_ * 32))
        B(bone, (x, g + 13.5, z), (2.8, 9, 8.6), 'greatblade')
        B(bone, (x, g + 35.5, z), (2.2, 35, 8), 'greatblade')
        for (yy, h, w) in ((56, 6, 6), (61.5, 5, 4), (66, 4, 2)):
            B(bone, (x, g + yy, z), (2.0, h, w), 'greatblade')
    x = -15
    r.bone('sword', 'right_hand', (x, 29, -0.5), (SWORD_TILT, 0, 6))
    sword_parts('sword', x, 29, -0.5)
    # sheathed on the back: grip over the right shoulder, tip toward the left hip
    BACK_Z = 12.5; g = 52 - 30
    r.bone('scabbard', 'chest', (0, 52, BACK_Z), (0, 0, 145))
    B('scabbard', (0, g + 41, BACK_Z), (3.6, 50, 10), 'cape')
    B('scabbard', (0, g + 16.5, BACK_Z), (4.4, 3, 10.8), 'guard')
    B('scabbard', (0, g + 66.5, BACK_Z), (4.0, 3, 6), 'guard')
    for yy in (g + 30, g + 50): B('scabbard', (0, yy, BACK_Z), (4.2, 1.6, 10.6), 'belt')
    r.bone('sword_back', 'scabbard', (0, 52, BACK_Z))
    sword_parts('sword_back', 0, g, BACK_Z)
    B('chest', (0, 52, -6.6), (3, 30, 1), 'belt', rot=(0, 0, 38)); B('chest', (0, 52, 7.6), (3, 30, 1), 'belt', rot=(0, 0, -38))
    B('chest', (0, 52, -7.2), (3.4, 3.4, 0.8), 'guard')
    for s_ in (-1, 1):  # fist wraps around the grip
        B('right_hand', (x, 29, -0.5 + s_ * 1.6), (6.4, 4.8, 0.9), 'gauntlet')
    d = dict(L=30, th=30, hs=16, sy=58, ny=60, sx=15, aw=10, ah=30, tw=20, td=10, hand_y=26)
    return r, d

def rig_guardian():
    r = Rig(); d = humanoid(r, 24, 4, (8, 8), (16, 24, 8), 16, (8, 24, 8), 12, 6, hip_role='belt')
    ny = d['ny']; sy = d['sy']
    r.cube('head', (-8.5, ny + 4, -8.5), (17, 3, 17), 'helm')
    for i, (w, h) in enumerate(((10, 6), (7, 6), (5, 5), (3, 5), (1.5, 6))):
        b = r.bone(f'spire{i}', 'head' if i == 0 else f'spire{i - 1}', (0, ny + 16 + sum(hh for _, hh in ((10, 6), (7, 6), (5, 5), (3, 5), (1.5, 6))[:i]), 0), (-3 if i else 0, 0, 0))
        y0 = r.bones[b]['pivot'][1]; r.cube(b, (-w / 2, y0, -w / 2), (w, h, w), 'helmspire')
    r.cube('chest', (-7, 36, -5), (14, 10, 2), 'breastplate'); r.cube('chest', (-3, 38, -5.6), (6, 6, 1), 'core')
    for t, (w, h, oy) in enumerate(((13, 7, 0), (11, 5, 5))):
        for side, s in (('right', -1), ('left', 1)):
            x = s * 12; r.cube(f'{side}_arm', (x - w / 2, sy - 1 + oy, -w / 2), (w, h, w), 'pauldron')
    for side, s in (('right', -1), ('left', 1)):
        x = s * 12; r.cube(f'{side}_hand', (x - 4.5, d['hand_y'] - 0.5, -4.5), (9, 7, 9), 'gauntlet')
        r.cube(f'{side}_shin', (s * 4 - 4.5, 4, -4.5), (9, 6, 9), 'gauntlet')
    for name, o, s in (('tasset_front', (-7, 14, -5), (14, 9, 1)), ('tasset_back', (-7, 14, 4), (14, 9, 1))):
        r.bone(name, 'hips', (0, 24, o[2])); r.cube(name, o, s, 'pauldron')
    r.bone('cape1', 'chest', (0, 47, 4.5), (5, 0, 0)); r.cube('cape1', (-8, 37, 4.5), (16, 10, 1), 'cape')
    r.bone('cape2', 'cape1', (0, 37, 4.5)); r.cube('cape2', (-8, 27, 4.5), (16, 10, 1), 'cape')
    r.bone('cape3', 'cape2', (0, 27, 4.5)); r.cube('cape3', (-8, 19, 4.5), (16, 8, 1), 'capehem')
    hy = d['hand_y']; x = -12
    r.bone('halberd', 'right_hand', (x, hy + 3, 0), (0, 0, 0))
    r.cube('halberd', (x - 1.5, hy - 18, -1.5), (3, 62, 3), 'shaft')
    r.cube('halberd', (x - 12, hy + 26, -1), (11, 16, 2), 'axehead'); r.cube('halberd', (x + 1.5, hy + 30, -1), (4, 6, 2), 'axehead')
    r.cube('halberd', (x - 1, hy + 44, -1), (2, 8, 2), 'axehead')
    return r, d

MOBS = {
 'rift_ghoul': dict(rig=rig_ghoul, k=4, ts=[512, 512], pal=dict(base='#25252E', dark='#110E13', accent='#6A3694'), glow='#C12F41', eye='#A45ADA', eyes='hollow'),
 'rift_stalker': dict(rig=rig_stalker, k=4, ts=[512, 512], pal=dict(base='#25212E', dark='#0D0914', accent='#5E3F7C', robe='#2A1F3C'), glow='#C23346', eye='#E0485A', eyes='slit'),
 'void_caster': dict(rig=rig_caster, k=4, ts=[512, 512], pal=dict(base='#261F2B', dark='#110B15', accent='#7520A1', robe='#301C35'), glow='#8E2745', eye='#B35AE0', orb='#CA59EE', eyes='dots'),
 'rift_brute': dict(rig=rig_brute, k=2, ts=[256, 256], pal=dict(base='#2E212B', dark='#140B10', accent='#463442'), glow='#B13648', eye='#E04A5C', crystal='#8A45A0', eyes='dots'),
 'rift_warden': dict(rig=rig_warden, k=2, ts=[512, 512], boss=True, pal=dict(base='#2A2327', dark='#120D10', accent='#5C5053', cape='#4A141C'), trim='#6E5A3C', glow='#AD2530', eye='#FF3B47', crystal='#A15FC0', eyes='visor'),
 'spire_guardian': dict(rig=rig_guardian, k=2, ts=[512, 512], boss=True, pal=dict(base='#2D2528', dark='#120D0F', accent='#5A5054', cape='#4C151E'), glow='#962933', eye='#B04BE0', core='#7F18A9', eyes='visor'),
}

# ----------------------------------------------------------------------------- uv packing + painting
def cdims(c, k): return [max(1, math.ceil(v * k)) if v > 0 else 0 for v in c['size']]
def pack(rig, k, ts):
    x = y = rowh = 0; res = []
    for b in rig.order:
        for c in rig.bones[b]['cubes']:
            w, h, d = cdims(c, k); fw, fh = 2 * (w + d), d + h
            if x + fw > ts[0]: x, y, rowh = 0, y + rowh, 0
            res.append((x, y)); x += fw; rowh = max(rowh, fh)
    need = y + rowh
    while ts[1] < need: ts[1] *= 2  # grow the sheet (power of two) instead of failing
    return res
def faces(u, v, w, h, d):
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}
LIGHT = {'up': 1.2, 'north': 1.0, 'south': 0.88, 'east': 0.84, 'west': 0.84, 'down': 0.6}

class Painter:
    def __init__(s, name, mob):
        s.mob = mob; s.k = mob['k']; W, H = mob['ts']; s.rnd = random.Random(name + 'v3')
        s.base = Image.new('RGBA', (W, H), (0, 0, 0, 0)); s.glow = s.base.copy(); s.bp = s.base.load(); s.gp = s.glow.load()
        s.P = {k: hx(v) for k, v in mob['pal'].items()}; s.gc = hx(mob['glow']); s.ec = hx(mob['eye']); s.noise = {}
    def n(s, x, y, sc=3):
        key = (x // sc, y // sc)
        if key not in s.noise: s.noise[key] = s.rnd.random()
        return s.noise[key] * 0.55 + s.rnd.random() * 0.25 + 0.1
    def px(s, x, y, c, a=255): s.bp[x, y] = tuple(c[:3]) + (a,)
    def gl(s, x, y, c, k=1.0): s.gp[x, y] = mul(c, k) + (255,)
    def dk(s, x, y, k): r, g, b, a = s.bp[x, y]; s.bp[x, y] = mul((r, g, b), k) + (a,)
    def fill(s, f, fn, c0, c1, ao=True):
        fx, fy, fw, fh = f; L = LIGHT[fn]; e = max(1, s.k // 2)
        for yy in range(fy, fy + fh):
            t = (yy - fy) / max(1, fh - 1)
            for xx in range(fx, fx + fw):
                c = mix(c0, c1, min(1, s.n(xx, yy)))
                kk = L
                if fn not in ('up', 'down'): kk *= (1.1 - 0.25 * t) if ao else 1.0
                if fw > 2 * e and fh > 2 * e:
                    if yy - fy < e: kk *= 1.3
                    elif xx - fx < e: kk *= 1.12
                    elif fy + fh - 1 - yy < e: kk *= 0.6
                    elif fx + fw - 1 - xx < e: kk *= 0.8
                s.px(xx, yy, mul(c, kk))
    def scratches(s, f, n=3, k=1.25):
        fx, fy, fw, fh = f
        for _ in range(n):
            if fw < 6 or fh < 6: return
            x, y = s.rnd.randrange(fx + 2, fx + fw - 2), s.rnd.randrange(fy + 2, fy + fh - 2)
            for i in range(s.rnd.randint(2, 2 + s.k * 2)):
                if fx <= x < fx + fw and fy <= y < fy + fh: s.dk(x, y, k)
                x += 1; y += s.rnd.choice((0, 1))
    def vein(s, f, k=0.8, branches=3):
        fx, fy, fw, fh = f; cx = fx + fw // 2; x = cx
        for yy in range(fy + fh - 2, fy + fh // 5, -1):
            s.gl(x, yy, s.gc, k); s.dk(x, yy, 0.5)
            if s.rnd.random() < 0.3: x = max(fx + 1, min(fx + fw - 2, x + s.rnd.choice((-1, 1))))
        for _ in range(branches):
            by = s.rnd.randrange(fy + fh // 3, fy + fh - 2); bx = cx; dx = s.rnd.choice((-1, 1))
            for i in range(max(3, fw // 3)):
                bx += dx; by -= s.rnd.choice((0, 1, 1))
                if fx < bx < fx + fw - 1 and fy < by < fy + fh: s.gl(bx, by, s.gc, k * 0.75); s.dk(bx, by, 0.6)
    def eyes(s, f):
        fx, fy, fw, fh = f; cx = fx + fw // 2; sc = max(1, fw // 10); ey = fy + int(fh * 0.45); dark = (5, 3, 7)
        kind = s.mob['eyes']
        if kind == 'visor':
            for xx in range(fx + 2 * sc, fx + fw - 2 * sc):
                for yy in range(ey, ey + sc + 1): s.px(xx, yy, dark); s.gl(xx, yy, s.ec, 1.0 if yy <= ey + sc // 2 else 0.55)
            for yy in range(ey + sc + 1, fy + fh - sc):
                for xx in range(cx - sc // 2, cx + sc // 2 + 1): s.px(xx, yy, dark)
        elif kind == 'slit':
            for xx in range(cx - 3 * sc, cx + 3 * sc):
                for yy in range(ey, ey + max(1, sc // 2) + 1): s.px(xx, yy, dark); s.gl(xx, yy, s.ec, 1.0)
        else:
            for side in (-1, 1):
                x0 = cx + (sc if side > 0 else -3 * sc)
                for xx in range(x0, x0 + 2 * sc):
                    for yy in range(ey - (sc if kind == 'hollow' else 0), ey + sc):
                        s.px(xx, yy, dark)
                    for yy in range(ey, ey + max(1, sc - 1)): s.gl(xx, yy, s.ec, 1.0)
                s.gl(x0, ey, (255, 255, 255), 1.0)
    def cube(s, uv, c):
        w, h, d = cdims(c, s.k); F = faces(uv[0], uv[1], w, h, d); role = c['role']; P = s.P
        base, dark, acc = P['base'], P['dark'], P['accent']; k = s.k
        for fn, f in F.items():
            fx, fy, fw, fh = f
            if fw <= 0 or fh <= 0: continue
            side = fn not in ('up', 'down')
            if role in ('robe', 'hood', 'sleeve', 'collar'):
                rc = P.get('robe', acc); s.fill(f, fn, mul(rc, 0.7), mul(rc, 1.2))
                if s.mob.get('trim'):  # cloth weave + stitched seam
                    for xx in range(fx, fx + fw):
                        for yy in range(fy, fy + fh):
                            if (xx + yy) % 2 == 0: s.dk(xx, yy, 0.9)
                            if (xx // 2 + yy // 2) % 9 == 0: s.dk(xx, yy, 1.12)
                    if side and fh > 3 * k:
                        for xx in range(fx + 1, fx + fw - 1, 3): s.px(xx, fy + max(1, k // 2), mul(hx(s.mob['trim']), 1.3))
                if side:
                    for xx in range(fx, fx + fw):
                        ph = ((xx - fx) / k) % 3
                        kk = 0.7 if ph < 0.6 else (1.18 if ph < 1.2 else 1.0)
                        for yy in range(fy, fy + fh): s.dk(xx, yy, kk)
                    if role == 'robe' and fh > 4 * k:
                        for xx in range(fx, fx + fw):
                            cut = s.rnd.choice((0, 0, 1, 2, 3)) * max(1, k // 2)
                            for yy in range(fy + fh - cut, fy + fh): s.px(xx, yy, (0, 0, 0), 0)
                        ty = fy + fh - 3 * k
                        for xx in range(fx + k, fx + fw - k):
                            if ((xx - fx) // k) % 3 != 2: s.gl(xx, ty, s.gc, 0.55); s.dk(xx, ty, 0.6)
                    if role in ('sleeve', 'collar'):
                        for xx in range(fx, fx + fw):
                            s.gl(xx, fy + fh - 1, s.gc, 0.5)
                if role == 'hood' and fn == 'north' and w >= 8 * k:
                    for xx in range(fx + 2 * k, fx + fw - 2 * k):
                        for yy in range(fy + 2 * k, fy + fh): s.px(xx, yy, (0, 0, 0), 0)
            elif role in ('helm', 'pauldron', 'helmspire', 'breastplate', 'gauntlet', 'plate', 'visorplate'):
                metal = mix(base, acc, 0.55)
                s.fill(f, fn, mul(metal, 0.65), mul(acc, 1.25))
                if side and fw > 3 * k and fh > 3 * k:
                    step = 5 * k
                    for yy in range(fy + step, fy + fh - 1, step):
                        for xx in range(fx, fx + fw): s.px(xx, yy, mul(dark, 0.9)); s.dk(xx, yy + 1, 1.35)
                    for (xx, yy) in ((fx + k, fy + k), (fx + fw - 1 - k, fy + k), (fx + k, fy + fh - 1 - k), (fx + fw - 1 - k, fy + fh - 1 - k)):
                        s.px(xx, yy, mul(acc, 1.9)); s.px(min(xx + 1, fx + fw - 1), yy, mul(dark, 0.8))
                    s.scratches(f, 4, 1.35)
                    if fn == 'north' and role in ('pauldron', 'breastplate', 'gauntlet') and fw >= 6 * k:
                        cx = fx + fw // 2  # engraved rune
                        for yy in range(fy + 2 * k, fy + min(fh - k, 2 * k + fh // 2)):
                            s.gl(cx, yy, s.gc, 0.55); s.dk(cx, yy, 0.5)
                        for xx in range(cx - 2 * k, cx + 2 * k + 1): s.gl(xx, fy + 3 * k, s.gc, 0.5)
                if side and s.mob.get('trim') and role in ('pauldron', 'breastplate', 'helmspire', 'gauntlet') and fw > 3 * k and fh > 3 * k:
                    tc = hx(s.mob['trim'])
                    for xx in range(fx, fx + fw):
                        for yy in list(range(fy, fy + max(1, k // 2))) + list(range(fy + fh - max(1, k // 2), fy + fh)):
                            s.px(xx, yy, mul(tc, 1.25 if yy < fy + k else 0.8))
                    for xx in range(fx + 2 * k, fx + fw - 2 * k, 3 * k):  # trim rivets
                        s.px(xx, fy + fh - k, mul(tc, 1.8))
                if side and s.mob.get('trim'):
                    for _ in range(max(1, fw * fh // (60 * k))):  # wear: bright chips on edges, dark pits
                        xx = s.rnd.randrange(fx, fx + fw); yy = s.rnd.choice((fy, fy + fh - 1, s.rnd.randrange(fy, fy + fh)))
                        s.dk(xx, yy, s.rnd.choice((1.6, 0.55)))
                if role == 'helm' and fn == 'north' and fh > 8 * k: s.eyes(f)
                if role == 'visorplate' and fn == 'north':
                    ey = fy + fh // 2
                    for xx in range(fx + k, fx + fw - k):
                        for yy in range(ey - k // 2, ey + k // 2 + 1): s.px(xx, yy, (6, 3, 6)); s.gl(xx, yy, s.ec, 1.0 if yy <= ey else 0.6)
            elif role == 'mask':
                s.fill(f, fn, (196, 190, 186), (238, 234, 228))
                if side or fn == 'up':
                    x, y = fx + fw // 2, fy
                    for _ in range(max(2, fw * fh // (24 * k * k))):   # cracks leaking rift light
                        x = s.rnd.randrange(fx, fx + fw); y = s.rnd.randrange(fy, fy + fh)
                        for i in range(s.rnd.randint(3, 4 + 2 * k)):
                            if fx <= x < fx + fw and fy <= y < fy + fh:
                                s.px(x, y, (60, 40, 70)); s.gl(x, y, s.gc, 0.9)
                            x += s.rnd.choice((-1, 0, 1)); y += 1
            elif role == 'smoke':
                for yy in range(fy, fy + fh):
                    t = (yy - fy) / max(1, fh - 1)
                    for xx in range(fx, fx + fw):
                        nz = s.n(xx, yy, 2)
                        a = int(max(0, min(255, 255 * (1.05 - t * 0.8) * (0.55 + 0.6 * nz))))
                        c = mix((10, 6, 16), (46, 26, 64), nz)
                        s.px(xx, yy, c, a if fn not in ('up',) else 200)
                        if nz > 0.82 and t < 0.7: s.gl(xx, yy, s.gc, 0.25)
            elif role in ('cape', 'capehem'):
                cc = P.get('cape', P.get('robe', P['accent'])); s.fill(f, fn, mul(cc, 0.65), mul(cc, 1.2), ao=False)
                for xx in range(fx, fx + fw):  # cloth weave
                    for yy in range(fy, fy + fh):
                        if (xx + yy) % 2 == 0: s.dk(xx, yy, 0.9)
                        if (xx // 2 + yy // 2) % 7 == 0: s.dk(xx, yy, 1.1)
                if side and s.mob.get('trim'):
                    for xx in range(fx, fx + fw):
                        for yy in range(fy, fy + k // 2): s.px(xx, yy, hx(s.mob['trim']))
                for xx in range(fx, fx + fw):
                    ph = ((xx - fx) / k) % 4
                    kk = 0.65 if ph < 0.8 else (1.25 if ph < 1.6 else 1.0)
                    for yy in range(fy, fy + fh): s.dk(xx, yy, kk)
                    if role == 'capehem' and side:
                        cut = s.rnd.choice((0, 1, 2, 3, 4)) * k
                        for yy in range(fy + fh - cut, fy + fh): s.px(xx, yy, (0, 0, 0), 0)
                        s.gl(xx, fy + fh - 5 * k, s.gc, 0.35)
            elif role in ('greatblade', 'blade', 'axehead'):
                steel = (125, 116, 130) if role != 'blade' else mul(base, 1.7)
                s.fill(f, fn, mul(steel, 0.75), mul(steel, 1.2))
                if role == 'blade':
                    for xx in range(fx, fx + fw):
                        for yy in range(fy, fy + fh):
                            if xx == fx or xx == fx + fw - 1: s.gl(xx, yy, s.gc, 0.95)
                else:
                    for xx in range(fx, fx + fw): s.dk(xx, fy, 1.5)  # edge highlight
                    if side and fw * fh > 40 * k:
                        horiz = fw > fh; mid = (fy + fh // 2) if horiz else (fx + fw // 2)
                        for i in range(fw if horiz else fh):
                            if (i // k) % 6 != 5:
                                (s.gl(fx + i, mid, s.gc, 0.9) if horiz else s.gl(mid, fy + i, s.gc, 0.9))
            elif role in ('grip', 'shaft', 'guard'):
                s.fill(f, fn, (38, 28, 32), (74, 58, 62))
                for yy in range(fy, fy + fh, 3 * k):
                    for xx in range(fx, fx + fw): s.px(xx, yy, (112, 100, 108))
            elif role in ('rune', 'sigil', 'core'):
                s.fill(f, fn, mul(dark, 0.8), dark)
                gcol = hx(s.mob.get('core', s.mob['eye'])) if role == 'core' else s.gc
                if fn == 'north':
                    for xx in range(fx, fx + fw):
                        for yy in range(fy, fy + fh):
                            u, v = (xx - fx) / fw - 0.5, (yy - fy) / fh - 0.5
                            on = abs(u) < 0.08 or abs(v) < 0.08 or abs(abs(u) - abs(v)) < 0.06
                            if role == 'core': s.gl(xx, yy, gcol, max(0.25, 1 - 2.2 * math.hypot(u, v)))
                            elif on and max(abs(u), abs(v)) < 0.42: s.gl(xx, yy, gcol, 1.0)
            elif role in ('orb', 'orbshell'):
                oc = hx(s.mob['orb'])
                for xx in range(fx, fx + fw):
                    for yy in range(fy, fy + fh):
                        u, v = (xx - fx) / max(1, fw - 1) - 0.5, (yy - fy) / max(1, fh - 1) - 0.5; r = math.hypot(u, v)
                        if role == 'orb':
                            body = mix((38, 8, 52), mul(oc, 0.7), max(0, 0.7 - r))
                            if s.rnd.random() < 0.12: body = mul(body, 1.6)
                            s.px(xx, yy, body)
                            crack = (abs(math.sin(u * 9 + v * 4)) < 0.12) or (abs(math.sin(v * 11 - u * 3)) < 0.1)
                            if crack: s.gl(xx, yy, mix(oc, (255, 255, 255), 0.35), 1.0); s.dk(xx, yy, 0.4)
                            elif r < 0.25: s.gl(xx, yy, oc, 0.45)
                        else:
                            a = 70 if r < 0.45 else 150; s.px(xx, yy, mul(oc, 0.35), a); s.gl(xx, yy, oc, 0.35 + 0.4 * r)
            elif role in ('spike', 'crown'):
                cc = hx(s.mob.get('crystal', '#7A3A8A')) if role == 'spike' else mul(acc, 1.15)
                s.fill(f, fn, mul(cc, 0.5), mul(cc, 1.15))
                if role == 'spike' and side:
                    for yy in range(fy, fy + fh): s.gl(fx, yy, cc, 0.55); s.gl(fx + fw // 2, yy, cc, 0.3)
                if role == 'crown' and fn == 'north':
                    for yy in range(fy, fy + k): s.gl(fx + fw // 2, yy, s.gc, 0.85)
            elif role == 'rib':
                s.fill(f, fn, (88, 78, 86), (130, 118, 124))
            elif role == 'belt':
                s.fill(f, fn, (52, 38, 34), (84, 64, 56))
                if side:
                    for xx in range(fx, fx + fw, 4 * k):
                        for yy in range(fy + k, fy + fh - k): s.px(xx, yy, (140, 120, 90))
            else:  # stone/flesh: body, chest, limb, fist, foot, claw, skull, jaw
                s.fill(f, fn, mul(dark, 1.1), mul(base, 1.3))
                if role == 'claw': s.fill(f, fn, (78, 70, 80), (138, 126, 134))
                if side: s.scratches(f, 2 if not s.mob.get('trim') else 5, 0.7)
                if side and s.mob.get('trim') and fw > 2 * k:
                    for xx in range(fx, fx + fw): s.dk(xx, fy, 1.35)
                if role == 'chest' and fn == 'north':
                    cx = fx + fw // 2
                    for yy in range(fy + k, fy + fh - k): s.dk(cx, yy, 0.6)
                    for xx in range(fx + k, fx + fw - k): s.dk(xx, fy + fh // 2, 0.75)
                    s.vein(f, 0.75, 3 if fw > 30 else 2)
                elif role == 'body' and fn == 'north' and s.mob['eyes'] in ('hollow', 'dots'): s.vein(f, 0.6, 1)
                elif role == 'limb' and fn == 'north' and s.mob['eyes'] in ('hollow', 'dots') and fh > 6 * k: s.vein(f, 0.5, 1)
                if role == 'fist' and fn == 'north':
                    for xx in range(fx + k, fx + fw - k, 3 * k):
                        for i in range(k): s.px(xx + i, fy + k, mul(base, 1.9))
                if role == 'skull' and fn == 'north': s.eyes(f)
                if role == 'jaw' and fn == 'north':
                    for xx in range(fx + k, fx + fw - k, max(2, k)): s.px(xx, fy, (150, 140, 140))  # teeth
    def save(s, name):
        if s.mob.get('trim'):  # soft 1px halo around emissive lines so the glow reads cleanly
            halo = s.glow.filter(ImageFilter.MaxFilter(3)); hp = halo.load(); W, H = s.glow.size
            for yy in range(H):
                for xx in range(W):
                    if s.gp[xx, yy][3] == 0 and hp[xx, yy][3] and s.bp[xx, yy][3]:
                        r_, g_, b_, _ = hp[xx, yy]; s.gp[xx, yy] = (r_ * 2 // 7, g_ * 2 // 7, b_ * 2 // 7, 255)
        s.base.save(out('textures', 'entity', f'{name}.png'), optimize=True)
        s.glow.save(out('textures', 'entity', f'{name}_glow.png'), optimize=True)
        w = s.glow.copy(); p = w.load(); W, H = w.size
        for yy in range(H):
            for xx in range(W):
                r, g, b, a = p[xx, yy]
                if a: L = min(255, max(r, g, b) + 60); p[xx, yy] = (L, L, L, a)
        w.save(out('textures', 'entity', f'{name}_glow_rank.png'), optimize=True)

def geo(name, rig, k, ts, uvs):
    bones = []; it = iter(uvs)
    for bn in rig.order:
        b = rig.bones[bn]; cubes = []
        for c in b['cubes']:
            u, v = next(it); w, h, d = cdims(c, k); F = faces(u, v, w, h, d)
            uv = {fn: {'uv': [f[0], f[1]], 'uv_size': [f[2], f[3]]} for fn, f in F.items() if f[2] > 0 and f[3] > 0}
            cb = {'origin': c['origin'], 'size': c['size'], 'uv': uv}
            if c['inflate']: cb['inflate'] = c['inflate']
            if c.get('rot'):
                cb['pivot'] = [-c['pivot'][0], c['pivot'][1], c['pivot'][2]]
                cb['rotation'] = [-c['rot'][0], c['rot'][1], c['rot'][2]]
            cubes.append(cb)
        e = {'name': bn, 'pivot': [-b['pivot'][0], b['pivot'][1], b['pivot'][2]]}
        if b['parent']: e['parent'] = b['parent']
        if any(b['rotation']): e['rotation'] = [-b['rotation'][0], b['rotation'][1], b['rotation'][2]]
        for cb in cubes: cb['origin'] = [-(cb['origin'][0] + cb['size'][0]), cb['origin'][1], cb['origin'][2]]
        if cubes: e['cubes'] = cubes
        bones.append(e)
    model = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {
        'identifier': f'geometry.{name}', 'texture_width': ts[0], 'texture_height': ts[1],
        'visible_bounds_width': 6, 'visible_bounds_height': 7, 'visible_bounds_offset': [0, 2.5, 0]}, 'bones': bones}]}
    json.dump(model, open(out('geo', 'entity', f'{name}.geo.json'), 'w'), indent=1)

# ----------------------------------------------------------------------------- animations
def kf(pairs, smooth=True):
    return {f'{t:.4g}': ({'post': v, 'lerp_mode': 'catmullrom'} if smooth else v) for t, v in pairs}
def wave(period, amp, axis, n=8, phase=0.0, base=(0, 0, 0)):
    res = []
    for i in range(n + 1):
        t = period * i / n; s = math.sin(2 * math.pi * i / n + phase) * amp
        v = list(base); v[axis] += round(s, 3); res.append((t, v))
    return res
def anim_set(name, d, rig):
    has = lambda b: b in rig.bones
    A = {}
    # idle: breathing chest, head drift, arms sway, cloth drift
    B = {'chest': {'rotation': kf(wave(3.2, 2.2, 0, phase=0)), 'scale': kf([(t, [1 + 0.012 * math.sin(2 * math.pi * t / 3.2)] * 3) for t in [i * 0.4 for i in range(9)]])},
         'head': {'rotation': kf(wave(3.2, 2.5, 0, phase=math.pi / 2))},
         'jaw': {'rotation': kf(wave(3.2, 3, 0, phase=math.pi, base=(3, 0, 0)))},
         'right_arm': {'rotation': kf(wave(3.2, 2.5, 2, phase=0.3, base=(0, 0, 3)))},
         'left_arm': {'rotation': kf(wave(3.2, 2.5, 2, phase=0.3 + math.pi, base=(0, 0, -3)))},
         'right_forearm': {'rotation': kf(wave(3.2, 3, 0, base=(-8, 0, 0)))}, 'left_forearm': {'rotation': kf(wave(3.2, 3, 0, phase=1, base=(-8, 0, 0)))}}
    for c in ('cape1', 'cape2', 'cape3', 'robe_front', 'robe_back', 'hood_tip', 'hood_cone2', 'hood_cone3', 'tasset_front', 'tasset_back'):
        if has(c): B[c] = {'rotation': kf(wave(2.6, 3 if 'cape' in c else 2, 0, phase=len(c), base=(4 if 'cape' in c else 0, 0, 0)))}
    if has('orb'):
        B['orb'] = {'position': kf(wave(2.0, 1.2, 1)), 'rotation': kf([(0, [0, 0, 0]), (2.0, [0, 360, 0])], smooth=False)}
        for i in range(4): B[f'orb_rune{i}'] = {'rotation': kf([(0, [0, 0, 0]), (2.0, [0, 0, 360])], smooth=False)}
    if name == 'void_caster':
        B['right_arm']['rotation'] = kf(wave(3.2, 3, 0, base=(-45, -20, 0))); B['left_arm']['rotation'] = kf(wave(3.2, 3, 0, phase=1, base=(-45, 20, 0)))
    for bn in rig.order:
        if '_wisp' in bn or '_chain' in bn:
            amp = 6 if '_wisp' in bn else 7
            B[bn] = {'rotation': kf(wave(2.4 if '_wisp' in bn else 2.0, amp, 0, phase=len(bn) + (2 if 'left' in bn else 0)))}
    if has('robe_front_wisp0'):
        B['root'] = {'position': kf(wave(3.2, 0.8, 1))}
        for c in ('robe_front', 'robe_back', 'robe_left', 'robe_right'):
            B[c] = {'rotation': kf(wave(3.2, 4, 0, phase=len(c)))}
    if has('sigil_ring'):
        B['sigil_ring'] = {'rotation': kf([(0, [0, 0, 0]), (3.2, [0, 360, 0])], smooth=False)}
        B['root'] = {'position': kf(wave(3.2, 1.0, 1))}  # hover bob
    if has('orb_orbit'):
        B['orb_orbit'] = {'rotation': kf([(0, [0, 0, 0]), (3.2, [0, -360, 0])], smooth=False)}
        B['orb_band'] = {'rotation': kf([(0, [20, 0, 15]), (3.2, [20, 360, 15])], smooth=False)}
    A['idle'] = dict(loop=True, length=3.2, bones=B)
    # walk: thigh/shin stride, counter arm swing, body bob + sway
    T = 1.0 if not MOBS[name].get('boss') and name != 'rift_brute' else 1.4
    stride = 30 if name != 'rift_brute' else 22
    W = {'right_leg': {'rotation': kf(wave(T, stride, 0))}, 'left_leg': {'rotation': kf(wave(T, stride, 0, phase=math.pi))},
         'right_shin': {'rotation': kf([(T * i / 8, [max(0, math.sin(2 * math.pi * i / 8 + 1.2)) * 35, 0, 0]) for i in range(9)])},
         'left_shin': {'rotation': kf([(T * i / 8, [max(0, math.sin(2 * math.pi * i / 8 + 1.2 + math.pi)) * 35, 0, 0]) for i in range(9)])},
         'hips': {'position': kf(wave(T / 2, 0.6, 1, n=4)), 'rotation': kf(wave(T, 3, 1))},
         'chest': {'rotation': kf(wave(T, 4, 1, phase=math.pi, base=(4, 0, 0)))},
         'head': {'rotation': kf(wave(T, 3, 1))}}
    if name != 'void_caster':
        W['right_arm'] = {'rotation': kf(wave(T, stride * 0.8, 0, phase=math.pi))}; W['left_arm'] = {'rotation': kf(wave(T, stride * 0.8, 0))}
        W['right_forearm'] = {'rotation': kf(wave(T, 8, 0, base=(-15, 0, 0)))}; W['left_forearm'] = {'rotation': kf(wave(T, 8, 0, phase=math.pi, base=(-15, 0, 0)))}
    else:
        W['right_arm'] = {'rotation': kf(wave(T, 4, 0, base=(-45, -20, 0)))}; W['left_arm'] = {'rotation': kf(wave(T, 4, 0, base=(-45, 20, 0)))}
    for c in ('cape1', 'cape2', 'cape3', 'robe_front', 'robe_back', 'tasset_front', 'tasset_back'):
        if has(c): W[c] = {'rotation': kf(wave(T / 2, 5, 0, n=4, base=(14 if 'cape' in c or 'back' in c else -8, 0, 0)))}
    if name == 'rift_ghoul': W['chest']['rotation'] = kf(wave(T, 6, 2, base=(10, 0, 0)))
    if has('robe_front_wisp0'):  # phantom glides: no legs, tails stream back, chains trail
        for k_ in ('right_leg', 'left_leg', 'right_shin', 'left_shin'): W.pop(k_, None)
        for bn in rig.order:
            if '_wisp' in bn: W[bn] = {'rotation': kf(wave(T, 8, 0, base=(18, 0, 0)))}
            if '_chain' in bn: W[bn] = {'rotation': kf(wave(T, 10, 0, base=(22, 0, 0)))}
    if has('sigil_ring'):  # hovers instead of walking: glide forward-lean, sigils keep orbiting
        for k_ in ('right_leg', 'left_leg', 'right_shin', 'left_shin'): W.pop(k_, None)
        W['sigil_ring'] = {'rotation': kf([(0, [0, 0, 0]), (T, [0, 120, 0])], smooth=False)}
        W['orb_orbit'] = {'rotation': kf([(0, [0, 0, 0]), (T, [0, -112.5, 0])], smooth=False)}
        W['hips'] = {'rotation': kf(wave(T, 3, 0, base=(6, 0, 0)))}
    A['walk'] = dict(loop=True, length=T, bones=W)
    # attack: windup -> strike -> recover
    if name == 'rift_brute':
        atk = {'right_arm': [(0, [0, 0, 0]), (0.45, [-160, 0, -10]), (0.6, [-20, 0, 0]), (1.0, [0, 0, 0])],
               'left_arm': [(0, [0, 0, 0]), (0.45, [-160, 0, 10]), (0.6, [-20, 0, 0]), (1.0, [0, 0, 0])],
               'chest': [(0, [0, 0, 0]), (0.45, [-18, 0, 0]), (0.6, [28, 0, 0]), (1.0, [0, 0, 0])],
               'head': [(0, [0, 0, 0]), (0.45, [-15, 0, 0]), (0.6, [10, 0, 0]), (1.0, [0, 0, 0])],
               'jaw': [(0, [0, 0, 0]), (0.45, [25, 0, 0]), (0.8, [0, 0, 0])]}; L = 1.0
    elif name == 'rift_stalker':
        atk = {'right_arm': [(0, [0, 0, 0]), (0.25, [-100, -40, -30]), (0.4, [-60, 50, 10]), (0.7, [0, 0, 0])],
               'left_arm': [(0, [0, 0, 0]), (0.25, [-100, 40, 30]), (0.4, [-60, -50, -10]), (0.7, [0, 0, 0])],
               'chest': [(0, [0, 0, 0]), (0.25, [-8, 0, 0]), (0.4, [14, 0, 0]), (0.7, [0, 0, 0])]}; L = 0.7
    elif name == 'void_caster':
        atk = {'right_arm': [(0, [-45, -20, 0]), (0.35, [-20, -35, 0]), (0.5, [-95, 0, 0]), (0.9, [-45, -20, 0])],
               'left_arm': [(0, [-45, 20, 0]), (0.35, [-20, 35, 0]), (0.5, [-95, 0, 0]), (0.9, [-45, 20, 0])],
               'chest': [(0, [0, 0, 0]), (0.35, [-10, 0, 0]), (0.5, [12, 0, 0]), (0.9, [0, 0, 0])]}; L = 0.9
        atkpos = {'orb': [(0, [0, 0, 0]), (0.35, [0, 2, 3]), (0.5, [0, 0, -8]), (0.9, [0, 0, 0])]}
    elif name == 'rift_warden':
        atk = {'right_arm': [(0, [0, 0, 0]), (0.6, [-170, 0, -15]), (0.85, [-40, 0, 0]), (1.5, [0, 0, 0])],
               'left_arm': [(0, [0, 0, 0]), (0.6, [-150, 0, 20]), (0.85, [-40, 0, 0]), (1.5, [0, 0, 0])],
               'chest': [(0, [0, 0, 0]), (0.6, [-20, 0, 0]), (0.85, [30, 0, 0]), (1.5, [0, 0, 0])],
               'hips': [(0, [0, 0, 0]), (0.85, [8, 0, 0]), (1.5, [0, 0, 0])],
               'head': [(0, [0, 0, 0]), (0.6, [-10, 0, 0]), (0.85, [12, 0, 0]), (1.5, [0, 0, 0])],
               'cape1': [(0, [6, 0, 0]), (0.6, [30, 0, 0]), (0.85, [-5, 0, 0]), (1.5, [6, 0, 0])]}; L = 1.5
    elif name == 'spire_guardian':
        atk = {'right_arm': [(0, [0, 0, 0]), (0.5, [-120, 60, -20]), (0.75, [-70, -70, 10]), (1.2, [0, 0, 0])],
               'left_arm': [(0, [0, 0, 0]), (0.5, [-60, 30, 0]), (0.75, [-60, -30, 0]), (1.2, [0, 0, 0])],
               'chest': [(0, [0, 0, 0]), (0.5, [0, 35, 0]), (0.75, [8, -40, 0]), (1.2, [0, 0, 0])],
               'hips': [(0, [0, 0, 0]), (0.5, [0, 10, 0]), (0.75, [0, -10, 0]), (1.2, [0, 0, 0])]}; L = 1.2
    else:  # ghoul claw swipe
        atk = {'right_arm': [(0, [0, 0, 0]), (0.25, [-130, 0, -30]), (0.4, [-30, 0, 20]), (0.7, [0, 0, 0])],
               'left_arm': [(0, [0, 0, 0]), (0.35, [-130, 0, 30]), (0.5, [-30, 0, -20]), (0.8, [0, 0, 0])],
               'chest': [(0, [12, 0, 0]), (0.25, [0, 15, 0]), (0.45, [20, -15, 0]), (0.8, [12, 0, 0])],
               'jaw': [(0, [0, 0, 0]), (0.3, [30, 0, 0]), (0.7, [0, 0, 0])]}; L = 0.8
    # follow-through layer: anticipation lean, weight shift onto the front foot at the strike frame,
    # overshoot on recovery, and cloth/chains lagging behind the motion.
    # windup peak (arms fully raised) and impact frame (swing lands); RiftMob.strikeDelay() must match the impact
    TP = {'rift_brute': 0.45, 'void_caster': 0.35, 'rift_warden': 0.6, 'spire_guardian': 0.5}.get(name, 0.25)
    TS = {'rift_brute': 0.6, 'void_caster': 0.5, 'rift_warden': 0.85, 'spire_guardian': 0.75}.get(name, 0.4)
    def arc(a_w, a_s, a_o):
        return [(0, [0, 0, 0]), (TP, a_w), (TS, a_s), (TS + (L - TS) * 0.45, a_o), (L, [0, 0, 0])]
    extra = {'torso': arc([-8, 0, 0], [12, 0, 0], [-3, 0, 0]),
             'right_leg': arc([8, 0, 0], [-22, 0, 0], [-6, 0, 0]), 'left_leg': arc([-6, 0, 0], [14, 0, 0], [4, 0, 0]),
             'right_shin': arc([4, 0, 0], [18, 0, 0], [5, 0, 0]),
             'cape1': arc([10, 0, 0], [32, 0, 0], [4, 0, 0]), 'cape2': arc([6, 0, 0], [24, 0, 0], [2, 0, 0]),
             'robe_front': arc([6, 0, 0], [-18, 0, 0], [4, 0, 0]), 'robe_back': arc([-6, 0, 0], [22, 0, 0], [-4, 0, 0]),
             'tasset_front': arc([4, 0, 0], [-12, 0, 0], [3, 0, 0])}
    for bn in rig.order:
        if '_chain' in bn: extra[bn] = arc([-15, 0, 0], [35, 0, 0], [-8, 0, 0])
        if '_wisp' in bn: extra[bn] = arc([8, 0, 0], [24, 0, 0], [-5, 0, 0])
    for bn, v in extra.items():
        if has(bn) and bn not in atk: atk[bn] = v
    AB = {b: {'rotation': kf(v)} for b, v in atk.items() if has(b)}
    if name == 'void_caster': AB['orb'] = {'position': kf(atkpos['orb'])}
    A['attack'] = dict(loop=False, length=L, bones=AB)
    if MOBS[name].get('boss'):
        R = {'head': [(0, [0, 0, 0]), (0.5, [-35, 0, 0]), (1.8, [-30, 0, 0]), (2.4, [0, 0, 0])],
             'jaw': [(0, [0, 0, 0]), (0.5, [30, 0, 0]), (1.8, [28, 0, 0]), (2.4, [0, 0, 0])],
             'chest': [(0, [0, 0, 0]), (0.5, [-15, 0, 0]), (1.8, [-12, 0, 0]), (2.4, [0, 0, 0])],
             'right_arm': [(0, [0, 0, 0]), (0.5, [-20, 0, -55]), (1.8, [-25, 0, -60]), (2.4, [0, 0, 0])],
             'left_arm': [(0, [0, 0, 0]), (0.5, [-20, 0, 55]), (1.8, [-25, 0, 60]), (2.4, [0, 0, 0])],
             'cape1': [(0, [6, 0, 0]), (0.5, [40, 0, 0]), (1.8, [35, 0, 0]), (2.4, [6, 0, 0])],
             'cape2': [(0, [0, 0, 0]), (0.6, [20, 0, 0]), (1.8, [18, 0, 0]), (2.4, [0, 0, 0])]}
        RB = {b: {'rotation': kf(v)} for b, v in R.items() if has(b)}
        RB['root'] = {'scale': kf([(0, [1, 1, 1]), (0.5, [1.04, 1.04, 1.04]), (1.8, [1.04, 1.04, 1.04]), (2.4, [1, 1, 1])])}
        A['roar'] = dict(loop=False, length=2.4, bones=RB)
        if has('sword_back'):  # STUB for the attack-animation pass: reach over the shoulder, pull, bring forward
            # draw: reach over the right shoulder (grab frame 0.4s = 8 ticks, where the renderer swaps the sword bones),
            # rip it free with a torso twist, swing it round in an arc and settle into guard
            A['draw'] = dict(loop=False, length=1.1, bones={
                'right_arm': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-170, 0, 25]), (0.45, [-175, 0, 30]), (0.7, [-95, 0, -20]), (0.9, [-30, 0, -8]), (1.1, [0, 0, 0])])},
                'right_forearm': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-70, 0, 0]), (0.45, [-75, 0, 0]), (0.7, [-20, 0, 0]), (1.1, [0, 0, 0])])},
                'chest': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-6, -22, 0]), (0.7, [6, 18, 0]), (1.1, [0, 0, 0])])},
                'head': {'rotation': kf([(0, [0, 0, 0]), (0.35, [0, -25, 0]), (0.7, [0, 10, 0]), (1.1, [0, 0, 0])])},
                'left_arm': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-20, 0, 20]), (0.8, [-10, 0, 8]), (1.1, [0, 0, 0])])},
                'cape1': {'rotation': kf([(0, [6, 0, 0]), (0.45, [20, 0, 0]), (0.8, [28, 0, 0]), (1.1, [6, 0, 0])])}})
            A['sheathe'] = dict(loop=False, length=1.0, bones={
                'right_arm': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-175, 0, 30]), (0.5, [-170, 0, 25]), (1.0, [0, 0, 0])])},
                'right_forearm': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-75, 0, 0]), (1.0, [0, 0, 0])])},
                'chest': {'rotation': kf([(0, [0, 0, 0]), (0.35, [-4, -18, 0]), (1.0, [0, 0, 0])])},
                'head': {'rotation': kf([(0, [0, 0, 0]), (0.4, [0, -20, 0]), (1.0, [0, 0, 0])])}})
    def bones_of(spec):
        out_ = {}
        for bn, chans in spec.items():
            if not has(bn): continue
            out_[bn] = {ch: kf(v, smooth=sm) for ch, (v, sm) in chans.items()}
        return out_
    R0 = [0, 0, 0]
    if name == 'rift_warden':
        # SLAM: rise onto the toes with the greatsword overhead (0.75s), crash it down at 0.9s (impact tick 18), hold, recover
        A['slam'] = dict(loop=False, length=1.6, bones=bones_of({
            'right_arm': {'rotation': ([(0, R0), (0.75, [-178, 0, 12]), (0.9, [-30, 0, 0]), (1.2, [-35, 0, 0]), (1.6, R0)], True)},
            'left_arm': {'rotation': ([(0, R0), (0.75, [-170, 0, -12]), (0.9, [-35, 0, 0]), (1.2, [-38, 0, 0]), (1.6, R0)], True)},
            'chest': {'rotation': ([(0, R0), (0.75, [-18, 0, 0]), (0.9, [24, 0, 0]), (1.2, [20, 0, 0]), (1.6, R0)], True)},
            'torso': {'rotation': ([(0, R0), (0.75, [-6, 0, 0]), (0.9, [12, 0, 0]), (1.6, R0)], True)},
            'head': {'rotation': ([(0, R0), (0.75, [-15, 0, 0]), (0.9, [10, 0, 0]), (1.6, R0)], True)},
            'right_leg': {'rotation': ([(0, R0), (0.75, [6, 0, 0]), (0.9, [-28, 0, 0]), (1.3, [-25, 0, 0]), (1.6, R0)], True)},
            'left_leg': {'rotation': ([(0, R0), (0.9, [18, 0, 0]), (1.3, [15, 0, 0]), (1.6, R0)], True)},
            'cape1': {'rotation': ([(0, [6, 0, 0]), (0.75, [-4, 0, 0]), (0.9, [45, 0, 0]), (1.6, [6, 0, 0])], True)},
            'root': {'position': ([(0, R0), (0.75, [0, 1.5, 0]), (0.9, [0, -2.5, 0]), (1.25, [0, -2, 0]), (1.6, R0)], True)}}))
        # LUNGE: crouch (0.45s), launch at 0.5s (tick 10) with the sword trailing, cleave forward mid-air, land
        A['lunge'] = dict(loop=False, length=1.3, bones=bones_of({
            'right_arm': {'rotation': ([(0, R0), (0.45, [35, 0, 15]), (0.6, [50, 0, 20]), (0.85, [-120, 0, 5]), (1.0, [-40, 0, 0]), (1.3, R0)], True)},
            'left_arm': {'rotation': ([(0, R0), (0.45, [30, 0, -15]), (0.85, [-60, 0, 0]), (1.3, R0)], True)},
            'chest': {'rotation': ([(0, R0), (0.45, [22, 0, 0]), (0.6, [-10, 0, 0]), (0.85, [18, 0, 0]), (1.3, R0)], True)},
            'right_leg': {'rotation': ([(0, R0), (0.45, [-35, 0, 0]), (0.6, [25, 0, 0]), (0.9, [-20, 0, 0]), (1.3, R0)], True)},
            'left_leg': {'rotation': ([(0, R0), (0.45, [-30, 0, 0]), (0.6, [30, 0, 0]), (0.9, [10, 0, 0]), (1.3, R0)], True)},
            'right_shin': {'rotation': ([(0, R0), (0.45, [45, 0, 0]), (0.6, [10, 0, 0]), (1.3, R0)], True)},
            'left_shin': {'rotation': ([(0, R0), (0.45, [40, 0, 0]), (0.6, [10, 0, 0]), (1.3, R0)], True)},
            'cape1': {'rotation': ([(0, [6, 0, 0]), (0.6, [60, 0, 0]), (1.0, [30, 0, 0]), (1.3, [6, 0, 0])], True)},
            'root': {'position': ([(0, R0), (0.45, [0, -3, 0]), (0.6, R0), (1.3, R0)], True)}}))
    if name == 'spire_guardian':
        # SPIN: wind the halberd back (0.5s), whirl a full turn (impact 0.7s = tick 14), settle
        A['spin'] = dict(loop=False, length=1.4, bones=bones_of({
            'root': {'rotation': ([(0, R0), (0.5, [0, 40, 0]), (0.62, [0, -60, 0]), (0.75, [0, -200, 0]), (0.9, [0, -330, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])], False)},
            'right_arm': {'rotation': ([(0, R0), (0.5, [-70, 0, 40]), (0.75, [-85, 0, 70]), (1.0, [-80, 0, 60]), (1.4, R0)], True)},
            'left_arm': {'rotation': ([(0, R0), (0.5, [-60, 0, -20]), (0.75, [-80, 0, -50]), (1.0, [-70, 0, -40]), (1.4, R0)], True)},
            'chest': {'rotation': ([(0, R0), (0.5, [-6, 25, 0]), (0.75, [8, -10, 0]), (1.4, R0)], True)},
            'right_leg': {'rotation': ([(0, R0), (0.5, [-15, 0, 10]), (1.0, [-10, 0, 8]), (1.4, R0)], True)},
            'left_leg': {'rotation': ([(0, R0), (0.5, [12, 0, -10]), (1.0, [8, 0, -8]), (1.4, R0)], True)}}))
        # BARRIER: plant the halberd, arms wide, face up while the ward forms (tick 8)
        A['barrier'] = dict(loop=False, length=1.2, bones=bones_of({
            'right_arm': {'rotation': ([(0, R0), (0.4, [-150, 0, 35]), (0.9, [-150, 0, 38]), (1.2, R0)], True)},
            'left_arm': {'rotation': ([(0, R0), (0.4, [-150, 0, -35]), (0.9, [-150, 0, -38]), (1.2, R0)], True)},
            'head': {'rotation': ([(0, R0), (0.4, [-25, 0, 0]), (0.9, [-25, 0, 0]), (1.2, R0)], True)},
            'chest': {'rotation': ([(0, R0), (0.4, [-10, 0, 0]), (1.2, R0)], True)},
            'root': {'scale': ([(0, [1, 1, 1]), (0.4, [1.05, 1.05, 1.05]), (0.9, [1.05, 1.05, 1.05]), (1.2, [1, 1, 1])], True)}}))
    js = {'format_version': '1.8.0', 'animations': {}}
    for an, a in A.items():
        js['animations'][f'animation.{name}.{an}'] = {'loop': a['loop'], 'animation_length': a['length'], 'bones': a['bones']}
    # bedrock mirrors X: flip rotation y/z signs to match the mirrored pivots
    for a in js['animations'].values():
        for b in a['bones'].values():
            if 'rotation' in b:
                for key, val in b['rotation'].items():
                    v = val['post'] if isinstance(val, dict) else val
                    v[0] = -v[0]
                    v[1], v[2] = -v[1], -v[2]
                    v[0] = -v[0]
    json.dump(js, open(out('animations', 'entity', f'{name}.animation.json'), 'w'), indent=1)

# ----------------------------------------------------------------------------- the rift (gate)
FR = 16; RW, RH = 256, 256
def rift_textures():
    rnd = random.Random(7); base = Image.new('RGBA', (RW, RH * FR)); core = base.copy()
    jag = [rnd.uniform(-1, 1) for _ in range(64)]
    for f in range(FR):
        t = f / FR * 2 * math.pi
        S = 2; n = 128 * S; m = 256 * S
        im = Image.new('RGBA', (n, m)); cim = Image.new('RGBA', (n, m)); p = im.load(); cp = cim.load()
        for y in range(m):
            yy = y / m; prof = math.sin(math.pi * yy) ** 0.8
            ji = int(yy * 63); jag_v = jag[ji] * (1 - (yy * 63 - ji)) + jag[min(63, ji + 1)] * (yy * 63 - ji)
            cx = 0.5 + 0.06 * math.sin(yy * 9 + 1) + 0.03 * jag_v
            hw = 0.42 * prof * (0.85 + 0.15 * math.sin(yy * 23 + t) + 0.12 * jag_v) + 0.004
            for x in range(n):
                xx = x / n; dxn = (xx - cx) / hw if hw > 0 else 9
                if abs(dxn) > 1.0: continue
                edge = 1 - abs(dxn)
                u, v = (xx - cx) * 2.2, (yy - 0.5) * 1.0
                r = math.hypot(u, v); a = math.atan2(v, u)
                sw = 0.5 + 0.5 * math.sin(a * 2 + r * 9 - t)
                sw2 = 0.5 + 0.5 * math.sin(a * -1 + r * 14 + t * 2 + yy * 3)
                lum = 0.22 + 0.38 * sw * sw + 0.22 * sw2 ** 3
                lum *= 0.55 + 0.45 * min(1, r * 2.5)  # darker vortex centre
                rim = max(0, 1 - edge / 0.16) ** 1.4
                lum = min(1, lum + rim * 0.75)
                al = min(255, int(235 * min(1, edge / 0.05)))
                g = int(255 * lum); p[x, y] = (g, g, g, al)
                hot = max(0, 1 - edge / 0.05)
                if hot > 0.05: cp[x, y] = (255, 255, 255, int(140 * hot))
        im = im.resize((128, 256), Image.LANCZOS)
        cim = cim.resize((128, 256), Image.LANCZOS)
        base.paste(im, (0, f * RH)); core.paste(cim, (0, f * RH))
        # glow blob (64,0,64,64)
        g = Image.new('RGBA', (128, 128)); gp = g.load()
        for y in range(128):
            for x in range(128):
                u, v = (x - 63.5) / 64, (y - 63.5) / 64; r = math.hypot(u * 1.4, v)
                a = max(0, 1 - r) ** 2 * (0.85 + 0.15 * math.sin(t + r * 6)); gp[x, y] = (255, 255, 255, int(120 * a))
        base.paste(g, (128, f * RH))
        # shard (64,64,32,32) + spark (96,64,32,32)
        sh = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(sh)
        d.polygon([(16, 1), (24, 12), (19, 31), (10, 26), (7, 10)], fill=(235, 235, 235, 255), outline=(255, 255, 255, 255))
        d.line([(16, 3), (15, 28)], fill=(255, 255, 255, 255), width=2)
        sh = sh.resize((64, 64), Image.LANCZOS); base.paste(sh, (128, f * RH + 128))
        sp = Image.new('RGBA', (32, 32)); spp = sp.load()
        for y in range(32):
            for x in range(32):
                r = math.hypot(x - 15.5, y - 15.5) / 16; spp[x, y] = (255, 255, 255, int(255 * max(0, 1 - r) ** 3))
        sp = sp.resize((64, 64), Image.LANCZOS); base.paste(sp, (192, f * RH + 128))
    for img, nm in ((base, 'nexus_rift'), (core, 'nexus_rift_core')):
        img.save(out('textures', 'block', f'{nm}.png'), optimize=True)
        json.dump({'animation': {'frametime': 2, 'interpolate': True}}, open(out('textures', 'block', f'{nm}.png.mcmeta'), 'w'))

def plane(origin, size, region):
    u, v, w, h = region; uv = {'north': {'uv': [u, v], 'uv_size': [w, h]}, 'south': {'uv': [u + w, v], 'uv_size': [-w, h]}}
    return {'origin': [-(origin[0] + size[0]), origin[1], origin[2]], 'size': [size[0], size[1], 0], 'uv': uv}
def rift_geo():
    TEAR = (0, 0, 128, 256); GLOW = (128, 0, 128, 128); SHARD = (128, 128, 64, 64); SPARK = (192, 128, 64, 64)
    bones = [{'name': 'rift', 'pivot': [0, 0, 0]},
             {'name': 'glow_back', 'parent': 'rift', 'pivot': [0, 48, 1.5], 'cubes': [plane((-34, 0, 1.5), (68, 96, 0), GLOW)]},
             {'name': 'tear', 'parent': 'rift', 'pivot': [0, 48, 0], 'cubes': [plane((-20, 8, 0), (40, 80, 0), TEAR)]},
             {'name': 'glow_front', 'parent': 'rift', 'pivot': [0, 48, -1.6], 'cubes': [plane((-22, 14, -1.6), (44, 68, 0), GLOW)]}]
    rnd = random.Random(3)
    for i in range(12):
        side = -1 if i % 2 else 1; y = 14 + i * 6 + rnd.uniform(-2, 2); x = side * (10 + 12 * math.sin(math.pi * (y - 8) / 80)) + rnd.uniform(-2, 2)
        sz = rnd.uniform(2.5, 4.5); reg = SHARD if i % 3 else SPARK; z = rnd.uniform(-2.5, 2.5)
        bones.append({'name': f'shard{i}', 'parent': 'rift', 'pivot': [-x, y, z], 'cubes': [plane((x - sz / 2, y - sz, z), (sz, sz * 1.6, 0), reg)]})
    model = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {'identifier': 'geometry.nexus_rift', 'texture_width': RW, 'texture_height': RH,
             'visible_bounds_width': 6, 'visible_bounds_height': 7, 'visible_bounds_offset': [0, 3, 0]}, 'bones': bones}]}
    json.dump(model, open(out('geo', 'block', 'nexus_rift.geo.json'), 'w'), indent=1)
    A = {}
    B = {'glow_back': {'scale': kf([(t, [1 + 0.07 * math.sin(2 * math.pi * t / 2.4), 1 + 0.04 * math.sin(2 * math.pi * t / 2.4), 1]) for t in [i * 0.3 for i in range(9)]])},
         'tear': {'scale': kf([(t, [1 + 0.035 * math.sin(2 * math.pi * t / 1.2), 1, 1]) for t in [i * 0.15 for i in range(9)]])},
         'glow_front': {'scale': kf([(t, [1 + 0.1 * math.sin(2 * math.pi * t / 1.6), 1 + 0.05 * math.sin(2 * math.pi * t / 1.6), 1]) for t in [i * 0.2 for i in range(9)]])}}
    for i in range(12):
        B[f'shard{i}'] = {'position': kf(wave(2.4 + (i % 4) * 0.3, 2.5, 1, phase=i)), 'rotation': kf([(0, [0, 0, 0]), (3.6, [0, 0, 360 if i % 2 else -360])], smooth=False)}
    A['open'] = dict(loop=True, length=3.6, bones=B)
    C = {b: {'scale': kf([(0, [0.06, 0.35, 1]), (1.0, [0.06, 0.35, 1])], smooth=False)} for b in ('tear',)}
    C.update({b: {'scale': kf([(0, [0.2, 0.3, 1]), (1.0, [0.2, 0.3, 1])], smooth=False)} for b in ('glow_back', 'glow_front')})
    C.update({f'shard{i}': {'scale': kf([(0, [0, 0, 0]), (1.0, [0, 0, 0])], smooth=False)} for i in range(12)})
    A['closed'] = dict(loop=True, length=1.0, bones=C)
    js = {'format_version': '1.8.0', 'animations': {f'animation.nexus_rift.{k}': {'loop': a['loop'], 'animation_length': a['length'], 'bones': a['bones']} for k, a in A.items()}}
    json.dump(js, open(out('animations', 'block', 'nexus_rift.animation.json'), 'w'), indent=1)

if __name__ == '__main__':
    import rigs_v4; rigs_v4.install(sys.modules[__name__])
    for name, m in MOBS.items():
        rig, d = m['rig']()
        uvs = pack(rig, m['k'], m['ts']); pt = Painter(name, m)  # pack first: it may grow m['ts']
        cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
        for uv, c in zip(uvs, cs): pt.cube(uv, c)
        pt.save(name); geo(name, rig, m['k'], m['ts'], uvs); anim_set(name, d, rig)
        print(name, len(rig.order), 'bones', len(cs), 'cubes', m['ts'])
    rift_textures(); rift_geo(); print('rift ok')

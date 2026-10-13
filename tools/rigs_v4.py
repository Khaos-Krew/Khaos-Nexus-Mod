"""v4 (Blockbench pass) rigs for the five non-Warden mobs. Imported by gen_geo_assets.py, which owns the
painter/exporters. Same bone names as v3 so the animation files keep working."""
import math

def install(G):
    Rig, humanoid = G.Rig, G.humanoid

    def rig_ghoul():
        r = Rig(); d = humanoid(r, 12, 2, (3.5, 3.5), (7, 12, 4), 7, (3, 17, 3), 5.5, 3, hunch=-24, hip_role='body'); B = r.box
        ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5.5
            for i, ox in enumerate((-1.1, 0, 1.1)):
                b = r.bone(f'{side}_claw{i}', f'{side}_hand', (x + ox, hy, -1), (-15, 0, 0))
                B(b, (x + ox, hy - 2.5, -1), (0.8, 5, 0.9), 'claw')
                B(b, (x + ox, hy - 5.6, -1.9), (0.6, 2.4, 0.7), 'claw', rot=(-35, 0, 0))
            B(f'{side}_arm', (x, sy + 1, 0), (3.4, 2.5, 3.4), 'spike')
            B(f'{side}_forearm', (x, sy - 8.6, 1.9), (1.2, 2.6, 1.2), 'spike', rot=(45, 0, 0))
            B(f'{side}_forearm', (x, hy + 1.6, 0), (3.6, 1.6, 3.6), 'robe')
            B(f'{side}_shin', (s * 2, 1, -1.5), (3.6, 2, 6), 'foot')
            for t in (-1, 0, 1): B(f'{side}_shin', (s * 2 + t * 1.1, 0.5, -5), (0.7, 1, 2), 'claw', rot=(15, 0, 0))
            B(f'{side}_shin', (s * 2, 6.2, -1.9), (2, 2, 1), 'spike')
            B('head', (s * 3.9, ny + 4.5, 1.5), (0.8, 1.4, 4), 'spike', rot=(25, s * 25, 0))
            B('chest', (s * 2.4, ny - 3.5, 2.3), (3, 4, 0.8), 'rib', rot=(0, s * 15, 0))
            B('head', (s * 2.2, ny + 3.6, -3.7), (1.6, 1, 0.6), 'rib')
        for i in range(4):
            for s in (-1, 1): B('chest', (s * 1.9, 17.5 + i * 1.6, -2.3), (2.6, 0.8, 0.6), 'rib', rot=(0, 0, s * -12))
        B('chest', (0, 19.5, -2.4), (0.8, 6, 0.6), 'rib')
        for i in range(5): B('chest', (0, 15.5 + i * 1.8, 2.2), (0.9, 1.2, 1.6), 'spike', rot=(30, 0, 0))
        B('head', (0, ny + 5.2, -3.6), (7.4, 1.2, 1.4), 'skull')
        for xx in (-2.4, -1.2, 0, 1.2, 2.4): B('head', (xx, ny + 1.9, -3.55), (0.6, 0.9, 0.5), 'claw')
        for xx in (-1.8, -0.6, 0.6, 1.8): B('jaw', (xx, ny + 2.0, -3.1), (0.6, 0.8, 0.5), 'claw')
        for s in (-1, 1): B('jaw', (s * 2.6, ny + 2.4, -3.1), (0.7, 1.8, 0.6), 'claw')
        r.bone('loin_front', 'hips', (0, 12, -2.2)); B('loin_front', (0, 9, -2.3), (4.6, 6, 0.4), 'robe')
        r.bone('loin_back', 'hips', (0, 12, 2.2)); B('loin_back', (0, 9.5, 2.3), (5, 5, 0.4), 'robe')
        return r, d

    def rig_stalker():
        r = Rig(); d = humanoid(r, 12, 1.9, (3.6, 3.6), (7.5, 12, 4), 7.5, (3.4, 12, 3.4), 5, 2, hip_role='robe'); B = r.box
        ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        B('head', (0, ny + 4.4, 0.4), (9.4, 9, 9.4), 'hood'); B('head', (0, ny + 8.9, 0.8), (8.4, 1.2, 8.4), 'hood')
        B('head', (0, ny + 3.6, -3.9), (6.4, 4.6, 0.6), 'helm'); B('head', (0, ny + 4.6, -4.25), (5.2, 0.8, 0.3), 'visorplate')
        r.bone('hood_tip', 'head', (0, ny + 9, 2), (-25, 0, 0)); B('hood_tip', (0, ny + 10.5, 3), (2.4, 4, 2.4), 'hood')
        B('hood_tip', (0, ny + 13, 4.2), (1.4, 2.4, 1.4), 'hood', rot=(-25, 0, 0))
        B('chest', (0, 20.5, 0), (9.4, 7, 5.6), 'robe'); B('chest', (0, 24, 0), (9, 2, 6.6), 'collar')
        r.bone('scarf', 'chest', (0, 24, 3.2), (12, 0, 0))
        B('scarf', (-1.4, 19.5, 3.5), (2.2, 9, 0.4), 'cape', rot=(0, 0, 6)); B('scarf', (1.4, 18.5, 3.6), (2.2, 11, 0.4), 'capehem', rot=(0, 0, -8))
        for name, c, sz in (('robe_front', (0, 5, -2.9), (8.8, 14, 0.8)), ('robe_back', (0, 5, 2.9), (8.8, 14, 0.8)),
                            ('robe_right', (-5, 5.5, 0), (0.8, 13, 5.4)), ('robe_left', (5, 5.5, 0), (0.8, 13, 5.4))):
            r.bone(name, 'hips', (0, 12, c[2])); B(name, c, sz, 'robe')
        B('hips', (0, 11.4, 0), (8.6, 1.6, 5), 'belt'); B('hips', (0, 11.4, -2.6), (1.8, 1.8, 0.4), 'rune')
        for s in (-1, 1): B('hips', (s * 3.2, 9.6, 2.7), (0.8, 4, 1.4), 'grip', rot=(0, 0, s * 20))
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5
            B(f'{side}_arm', (x, sy - 0.5, 0), (4.6, 5, 4.6), 'sleeve')
            B(f'{side}_arm', (x + s * 1.2, sy + 1.6, 0), (3, 1.2, 4.2), 'gauntlet', rot=(0, 0, s * 18))
            B(f'{side}_forearm', (x, sy - 9, 0), (3.9, 4, 3.9), 'gauntlet')
            for i in range(3): B(f'{side}_forearm', (x, sy - 7.6 - i * 1.2, -2.0), (3, 0.3, 0.3), 'belt')
            b = r.bone(f'{side}_blade', f'{side}_hand', (x, hy + 1, 0), (0, 0, 0))  # reverse grip along the forearm
            B(b, (x, hy + 1, 0), (1.4, 3, 1.4), 'grip'); B(b, (x, hy + 2.8, 0), (2.4, 0.6, 2.4), 'guard')
            B(b, (x + s * 2.0, hy + 8.5, 0.4), (0.5, 11, 1.6), 'blade', rot=(0, 0, s * -4))
            B(b, (x + s * 2.4, hy + 14.6, 0.7), (0.4, 2.4, 1.0), 'blade', rot=(-20, 0, s * -10))
        return r, d

    def rig_caster():
        r = Rig(); d = humanoid(r, 12, 1.9, (3.6, 3.6), (7.5, 12, 4), 7.5, (3.4, 12, 3.4), 5, 2, hip_role='robe'); B = r.box
        ny, sy = d['ny'], d['sy']
        B('head', (0, ny + 4.4, 0.4), (9.6, 9, 9.6), 'hood'); B('head', (0, ny + 3.4, -3.95), (6, 4, 0.5), 'helm')
        for s in (-1, 1): B('head', (s * 1.4, ny + 4.4, -4.3), (1.3, 0.9, 0.3), 'visorplate')
        r.bone('hood_cone', 'head', (0, ny + 9, 0)); B('hood_cone', (0, ny + 11, 0.3), (6.4, 4, 6.4), 'hood')
        r.bone('hood_cone2', 'hood_cone', (0, ny + 13, 0.5), (-18, 0, 0)); B('hood_cone2', (0, ny + 15, 0.6), (4.4, 4, 4.4), 'hood')
        r.bone('hood_cone3', 'hood_cone2', (0, ny + 17, 1), (-25, 0, 0)); B('hood_cone3', (0, ny + 18.5, 1.1), (2.4, 3, 2.4), 'hood')
        B('hood_cone3', (0, ny + 20.5, 1.5), (1.2, 1.4, 1.2), 'orb')
        B('chest', (0, 23.5, 0), (12, 3, 8), 'collar')
        for s in (-1, 1):
            for j in range(3): B('chest', (s * (3 + j * 1.6), 26 + j * 0.4, 1.8), (1, 3 + j, 1), 'crown', rot=(-15, 0, s * (15 + j * 12)))
        B('chest', (0, 20, 0), (9, 6, 5), 'robe'); B('chest', (0, 19.5, -2.6), (3, 4, 0.4), 'rune')
        r.bone('robe_upper', 'hips', (0, 12, 0)); B('robe_upper', (0, 8.5, 0), (12, 9, 8), 'robe')
        B('robe_upper', (0, 12.4, 0), (12.6, 1.6, 8.6), 'belt'); B('robe_upper', (0, 12.4, -4.4), (2.2, 2.2, 0.4), 'rune')
        for s in (-1, 1): B('robe_upper', (s * 3.6, 9.6, -4.3), (1.6, 3, 0.4), 'rune')
        r.bone('robe_lower', 'robe_upper', (0, 4, 0)); B('robe_lower', (0, 3, 0), (14, 6, 10), 'robe')
        B('robe_lower', (0, 0.4, 0), (14.6, 0.8, 10.6), 'collar')
        B('robe_upper', (6.8, 7, 1.5), (1.6, 4.4, 3.4), 'breastplate', rot=(0, 0, -6))
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5
            B(f'{side}_forearm', (x, sy - 8, 0), (6, 8, 6), 'sleeve'); B(f'{side}_forearm', (x, sy - 11.4, 0), (6.8, 1.4, 6.8), 'collar')
            B(f'{side}_arm', (x, sy, 0), (4.8, 4, 4.8), 'sleeve')
        r.bone('orb', 'chest', (0, 15, -9)); B('orb', (0, 15, -9), (3, 3, 3), 'orb'); B('orb', (0, 15, -9), (4, 4, 4), 'orbshell', inflate=0.2)
        for i in range(4):
            a = i * math.pi / 2; b = r.bone(f'orb_rune{i}', 'orb', (0, 15, -9))
            B(b, (math.cos(a) * 4.2, 15 + math.sin(a) * 4.2, -9), (1.2, 1.2, 0.4), 'orb', rot=(0, 0, 45))
        return r, d

    def rig_brute():
        r = Rig(); d = humanoid(r, 16, 4.5, (8, 8), (16, 18, 10), 12, (8, 22, 8), 13, 8, hip_role='belt'); B = r.box
        ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        r.cube('chest', (-9, 24, -6), (18, 10, 12), 'chest')
        r.bones['head']['pivot'][1] -= 3
        for c in r.bones['head']['cubes'] + r.bones['jaw']['cubes']: c['origin'][1] -= 3
        r.bones['jaw']['pivot'][1] -= 3
        B('head', (0, ny + 6.6, -5.9), (12.6, 2.4, 2.4), 'plate', rot=(-10, 0, 0))
        for s in (-1, 1):
            B('jaw', (s * 4.2, ny - 0.6, -5.8), (1.4, 4, 1.4), 'claw', rot=(-8, 0, s * -8))
            B('jaw', (s * 4.4, ny + 1.8, -6.0), (1.0, 1.6, 1.0), 'claw', rot=(-20, 0, s * -20))
        for xx in (-2.4, -0.8, 0.8, 2.4): B('jaw', (xx, ny - 0.4, -5.7), (1, 1, 0.6), 'claw')
        B('hips', (0, 15.5, -5.4), (4.4, 4.4, 1), 'sigil')
        r.bone('loin_front', 'hips', (0, 15, -5.4)); B('loin_front', (0, 10.5, -5.5), (8, 9, 0.6), 'robe')
        for side, s in (('right', -1), ('left', 1)):
            x = s * 13
            B(f'{side}_arm', (x + s * 1, sy + 1, 0), (11, 7, 11), 'pauldron', rot=(0, 0, s * 12))
            for (ox, oz, h, rz) in ((-2, -2, 6, -10), (1, 1.5, 8, 5), (-1, 3, 4, 20), (2.5, -2.5, 5, 25)):
                B(f'{side}_arm', (x + s * ox, sy + 6 + h / 2, oz), (2.2, h, 2.2), 'spike', rot=(rz / 2, 0, s * rz))
            B(f'{side}_hand', (x, hy + 3.5, 0), (10, 9, 10), 'fist')
            for i in range(3): B(f'{side}_hand', (x + (i - 1) * 3, hy + 1, -5.3), (1.8, 2.6, 1.8), 'spike', rot=(-30, 0, 0))
            B(f'{side}_forearm', (x, hy + 9.4, 0), (9.6, 2.6, 9.6), 'gauntlet')
            for i in range(3): B(f'{side}_forearm', (x + s * 4.6, hy + 7.6 - i * 1.6, -2), (0.8, 1.4, 1.6), 'guard', rot=(0, 0, i * 30))
            B(f'{side}_forearm', (x + s * 3.5, sy - 16, -1), (2, 4, 2), 'spike', rot=(0, 0, s * -30))
            B(f'{side}_shin', (s * 4.5, 6, -4.4), (8.6, 4.6, 1.4), 'plate', rot=(-6, 0, 0))
            for t in (-1, 0, 1): B(f'{side}_shin', (s * 4.5 + t * 2.4, 0.8, -4.6), (1.8, 1.6, 1.8), 'claw')
        for (ox, oy, oz, sz, rz) in ((-4, 2, -2, 3, -20), (2, 3, 0, 4, 15), (-1, 0, 3, 2, 5)):
            B('head', (ox, ny + 9 + oy - 3 + sz * 0.8, oz), (sz * 0.7, sz * 1.6, sz * 0.7), 'spike', rot=(0, 0, rz))
        for (ox, oy, rz) in ((-6, 0, -25), (-2, 3, -8), (2, 1, 10), (5, 4, 25), (0, -3, 0), (-4, 6, -15), (3, 7, 18)):
            B('chest', (ox, 29 + oy, 6.4), (2.5, 6, 2.5), 'spike', rot=(35, 0, rz))
        for (ox, oy) in ((-3, 30), (3.5, 28), (0, 32)): B('chest', (ox, oy, -6.4), (1.6, 2.6, 1), 'spike', rot=(-20, 0, ox * 5))
        return r, d

    def rig_guardian():
        r = Rig(); d = humanoid(r, 24, 4, (8, 8), (16, 24, 8), 16, (8, 24, 8), 12, 6, hip_role='belt'); B = r.box
        ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        B('head', (0, ny + 5.5, 0), (17, 3, 17), 'helm'); B('head', (0, ny + 8.6, -8.4), (13, 2.2, 1.6), 'visorplate')
        B('head', (0, ny + 4.0, -8.5), (2, 7, 1.2), 'helmspire')
        for s in (-1, 1): B('head', (s * 8.4, ny + 6, -1), (1.8, 10, 14), 'helm', rot=(0, s * -6, 0))
        hs = ((10, 6), (7, 6), (5, 5), (3, 5), (1.5, 6))
        for i, (w, h) in enumerate(hs):
            b = r.bone(f'spire{i}', 'head' if i == 0 else f'spire{i - 1}', (0, ny + 16 + sum(hh for _, hh in hs[:i]), 0), (-3 if i else 0, 0, 0))
            y0 = r.bones[b]['pivot'][1]; B(b, (0, y0 + h / 2, 0), (w, h, w), 'helmspire')
            if i < 3:
                for s in (-1, 1): B(b, (s * (w / 2 + 0.6), y0 + h * 0.7, 0), (1.2, h * 0.9, 1.2), 'crown', rot=(0, 0, s * -20))
        B('chest', (0, 41, -4.8), (14, 10, 2), 'breastplate'); B('chest', (0, 41, -5.9), (6, 6, 1), 'core')
        for s in (-1, 1): B('chest', (s * 3.6, 41, -6.0), (1, 8, 0.6), 'crown')
        B('chest', (0, 47.2, 0), (13, 3, 10.4), 'helm')
        for t, (w, h, oy) in enumerate(((13, 7, 0), (11, 5, 5), (9, 3, 8.5))):
            for side, s in (('right', -1), ('left', 1)):
                x = s * 12; B(f'{side}_arm', (x + s * (t * 1.4), sy + 2.5 - oy * 0.9, 0), (w, h, w), 'pauldron', rot=(0, 0, s * (12 + t * 10)))
        for side, s in (('right', -1), ('left', 1)):
            x = s * 12
            B(f'{side}_arm', (x + s * 2, sy + 7, 0), (2, 6, 2), 'crown', rot=(0, 0, s * -15))
            B(f'{side}_hand', (x, hy + 3, 0), (9, 7, 9), 'gauntlet')
            for i in range(4): B(f'{side}_hand', (x, hy + 5.6 - i * 1.7, -4.4), (6.6, 1.5, 1.8), 'gauntlet')
            for i in range(4): B(f'{side}_hand', (x + s * 3.0, hy + 5.6 - i * 1.7, -4.7), (1, 1, 1), 'crown')
            B(f'{side}_forearm', (x, hy + 9, 0), (9, 3, 9), 'gauntlet')
            B(f'{side}_shin', (s * 4, 7, 0), (9, 6, 9), 'gauntlet')
            B(f'{side}_shin', (s * 4, 12.4, -4.6), (5, 4, 2), 'gauntlet', rot=(-15, 0, 0)); B(f'{side}_shin', (s * 4, 13.4, -6), (1.6, 3.6, 1.6), 'crown', rot=(-55, 0, 0))
            B(f'{side}_shin', (s * 4, 1.5, -1.5), (8.4, 3, 11), 'gauntlet')
        for name, c, sz in (('tasset_front', (0, 18.5, -5), (14, 9, 1)), ('tasset_back', (0, 18.5, 4.6), (14, 9, 1))):
            r.bone(name, 'hips', (0, 24, c[2])); B(name, c, sz, 'pauldron', rot=(-8 if 'front' in name else 8, 0, 0))
        B('hips', (0, 24, -4.6), (4.4, 3.6, 0.8), 'guard'); B('hips', (0, 24, -5.0), (2.6, 2.2, 0.5), 'core')
        r.bone('cape1', 'chest', (0, 47, 4.5), (5, 0, 0)); B('cape1', (0, 42, 5), (16, 10, 1), 'cape')
        r.bone('cape2', 'cape1', (0, 37, 4.5)); B('cape2', (0, 32, 5), (16.5, 10, 1), 'cape')
        r.bone('cape3', 'cape2', (0, 27, 4.5)); B('cape3', (0, 23, 5), (17, 8, 1), 'capehem')
        x = -12; g = hy + 3  # halberd shaft across the closed right fist, angled forward and across the body
        gb = G.grip_fist(r, 'right_hand', (x, g, 0), (-45, 0, -25), 8.6, -1)
        r.bone('halberd', gb, (x, g, 0), (0, 0, 0))
        B('halberd', (x, g + 12, 0), (2.4, 62, 2.4), 'shaft'); B('halberd', (x, g - 19.6, 0), (3.2, 2.2, 3.2), 'guard')
        for yy in (g + 34, g + 40): B('halberd', (x, yy, 0), (3.2, 1.4, 3.2), 'guard')
        B('halberd', (x - 6, g + 37, 0), (9, 12, 1.6), 'axehead'); B('halberd', (x - 10.4, g + 37, 0), (2.4, 16, 1.4), 'axehead')
        for s_ in (-1, 1): B('halberd', (x - 9.8, g + 37 + s_ * 8.2, 0), (2, 3.6, 1.2), 'axehead', rot=(0, 0, s_ * -35))
        B('halberd', (x + 3.6, g + 38, 0), (4, 2.2, 1.4), 'axehead', rot=(0, 0, -20)); B('halberd', (x + 6.2, g + 36.6, 0), (2, 1.6, 1), 'axehead', rot=(0, 0, -40))
        B('halberd', (x, g + 47, 0), (2, 6, 2), 'axehead'); B('halberd', (x, g + 51.6, 0), (1.4, 4, 1.4), 'axehead'); B('halberd', (x, g + 54.6, 0), (0.8, 2.6, 0.8), 'axehead')
        B('halberd', (x - 1.3, g + 37, 0), (0.4, 10, 0.8), 'rune')
        return r, d

    import random as _r

    def wraps(r, bone, x, y0, y1, w, d, n, role='robe', z=0):
        for i in range(n):
            y = y0 + (y1 - y0) * (i + 0.5) / n
            r.box(bone, (x, y, z), (w + 0.5, 0.7, d + 0.5), role, rot=(0, 0, ((-1) ** i) * 6))

    def tatters(r, bone, y_top, x0, x1, z, n, length, role='robe', seed=1, depth_axis=False):
        rnd = _r.Random(seed)
        for i in range(n):
            x = x0 + (x1 - x0) * (i + 0.5) / n; h = length * rnd.uniform(0.6, 1.0)
            r.box(bone, (x, y_top - h / 2, z), ((x1 - x0) / n * 0.85, h, 0.35), role, rot=(rnd.uniform(-6, 6), 0, rnd.uniform(-8, 8)))

    def more_ghoul(r, d):
        r.bones['head']['rotation'] = [20, 0, 0]  # look ahead despite the hunch
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5.5
            wraps(r, f'{side}_arm', x, sy - 7, sy - 1, 3, 3, 3)
            wraps(r, f'{side}_forearm', x, hy + 3, hy + 7, 3, 3, 2)
            wraps(r, f'{side}_shin', s * 2, 2.5, 5, 3.5, 3.5, 2)
            for i in range(3): B(f'{side}_hand', (x + (i - 1) * 1.1, hy + 0.3, -1.6), (0.9, 0.8, 0.8), 'rib')       # knuckles
            for i in range(2): B(f'{side}_leg', (s * 2 + s * 1.8, 8 + i * 2, -0.5), (0.5, 1.8, 2.6), 'rib')          # sinew
            B(f'{side}_arm', (x + s * 1.6, sy - 4, 0), (0.5, 4, 2), 'rib')
        for i in range(3): B('chest', (0, ny - 0.8 + i * 0.5, 1.4 + i * 0.4), (1.2, 1, 1.2), 'spike', rot=(40, 0, 0))     # neck vertebrae
        for i, (xx, rz) in enumerate(((-2, -25), (-1, -10), (0, 0), (1, 10), (2, 25))):
            B('head', (xx, ny + 7.2, 1.2 + abs(xx) * 0.2), (0.6, 2.4 - abs(xx) * 0.3, 0.6), 'spike', rot=(-40, 0, rz))  # quills
        tatters(r, 'loin_front', 6.2, -2.3, 2.3, -2.3, 4, 3, seed=3)
        tatters(r, 'loin_back', 7.2, -2.5, 2.5, 2.3, 4, 3, seed=4)
        for s in (-1, 1): B('head', (s * 1.5, ny + 4.3, -3.75), (1.4, 0.4, 0.3), 'rib')
        B('chest', (0, 15.5, -2.2), (5, 0.8, 0.8), 'rib')

    def more_stalker(r, d):
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        tatters(r, 'robe_front', -1.5, -4.4, 4.4, -3.1, 6, 3.5, seed=5)
        tatters(r, 'robe_back', -1.5, -4.4, 4.4, 3.1, 6, 4, seed=6)
        for s in (-1, 1):
            for i in range(3): B(f'robe_{"right" if s < 0 else "left"}', (s * 5.1, -1.2, -2 + i * 2), (0.4, 2.4 + i * 0.4, 1.6), 'robe', rot=(i * 4 - 4, 0, 0))
        for k_, (xx, zz) in enumerate(((-4, -4.6), (-2, -4.8), (2, -4.8), (4, -4.6), (-4.6, 0), (4.6, 0))):
            B('head', (xx, ny + 0.5, zz + (0 if zz else 0)), (2, 1.2, 1.2 if zz else 6), 'hood', rot=(10 if zz else 0, 0, 0))  # hood rim layers
        for s in (-1, 1): B('chest', (0, 19.5, s * 3.0), (2, 11, 0.5), 'belt', rot=(0, 0, s * 35))     # cross straps
        B('chest', (0, 19.5, -3.3), (1.8, 1.8, 0.4), 'guard')
        for xx in (-3, 0.5, 3.4): B('hips', (xx, 10.6, -2.9), (1.8, 2, 1), 'belt')                     # pouches
        for s in (-1, 1): B('head', (s * 2.7, ny + 2.0, -4.25), (0.5, 0.5, 0.3), 'crown')              # mask rivets
        for s in (-1, 1): B('head', (s * 2.7, ny + 5.6, -4.25), (0.5, 0.5, 0.3), 'crown')
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5; lx = s * 1.9
            wraps(r, f'{side}_shin', lx, 2.5, 5.5, 3.1, 3.1, 3, role='belt')
            wraps(r, f'{side}_leg', lx, 7, 11, 3.6, 3.6, 2, role='belt')
            B(f'{side}_shin', (lx, 1, -0.6), (3.8, 2, 4.8), 'gauntlet'); B(f'{side}_shin', (lx, 0.6, -3.2), (3, 1.2, 1), 'gauntlet', rot=(15, 0, 0))
            for i in range(3): B(f'{side}_hand', (x, hy + 0.2 + i * 0.6, -1.8), (2.8, 0.5, 0.6), 'gauntlet')      # fingers on the grip
            B(f'{side}_arm', (x + s * 1.4, sy - 1, 0), (2.4, 4, 4.8), 'robe', rot=(0, 0, s * 10))                  # shoulder cloth
            B(f'{side}_arm', (x + s * 1.9, sy - 3.4, 0), (1.8, 3, 4.2), 'robe', rot=(0, 0, s * 16))
            B(f'{side}_blade', (x + s * 2.0, hy + 3.1, 0.4), (1.0, 0.6, 2.0), 'guard')
            B(f'{side}_blade', (x, hy - 0.8, 0), (1.0, 0.8, 1.0), 'rune')                                          # pommel gem
        for i in range(2): B('scarf', ((-1) ** i * 2.8, 17 + i, 3.7), (1.4, 8 - i * 2, 0.3), 'capehem', rot=(0, 0, (-1) ** i * 14))

    def more_caster(r, d):
        B = r.box; ny, sy = d['ny'], d['sy']
        tatters(r, 'robe_lower', 0.2, -7, 7, -5.1, 8, 2.5, role='collar', seed=7)
        tatters(r, 'robe_lower', 0.2, -7, 7, 5.1, 8, 2.5, role='collar', seed=8)
        for i in range(9):  # bead necklace
            a = math.pi * (0.15 + 0.7 * i / 8); B('chest', (math.cos(a) * 3.6, 22.2 - math.sin(a) * 2.6, -2.8), (0.8, 0.8, 0.8), 'orb' if i == 4 else 'claw')
        for xx in (-4, -2, 0, 2, 4): B('head', (xx, ny - 0.2, -4.7), (2, 1.2, 1.2), 'hood', rot=(12, 0, 0))   # hood rim
        for s in (-1, 1):
            B('chest', (s * 4.6, 24.6, 0), (4, 1.6, 8.6), 'robe', rot=(0, 0, s * -18))                       # shoulder mantle
            B('chest', (s * 5.6, 23.4, 0), (3, 1.4, 8), 'collar', rot=(0, 0, s * -30))
        for i in range(4): B('robe_upper', (7.6, 9.4 - i * 0.9, 1.5 + (i % 2) * 0.5), (0.5, 0.8, 0.5), 'guard')     # grimoire chain
        B('robe_upper', (7.5, 7, 1.5), (0.4, 3.6, 2.6), 'rune')
        for xx in (-4.5, -1.5, 1.5, 4.5): B('robe_upper', (xx, 10.6, -4.4), (0.6, 2.8, 0.4), 'collar')         # belt tassels
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5; hy = sy - 12
            for i in range(4): B(f'{side}_hand', (x + (i - 1.5) * 0.8, hy - 1.6, -1.2), (0.6, 2.2, 0.7), 'rib', rot=(-20, 0, 0))   # thin fingers
            for i in range(3): B(f'{side}_forearm', (x, sy - 5 - i * 2.4, -3.1), (5.6, 0.6, 0.4), 'collar')     # sleeve trims
            B(f'{side}_forearm', (x + s * 3.1, sy - 8, 0), (0.4, 7, 5), 'robe')
        for i in range(4):
            a = i * math.pi / 2 + math.pi / 4
            B('orb', (math.cos(a) * 2.9, 15 + math.sin(a) * 2.9, -9), (0.8, 0.8, 0.8), 'orbshell')
        for (xx, yy) in ((-6.5, 27), (6.5, 27), (0, 29.5)): B('chest', (xx, yy, 3), (1, 2.2, 0.4), 'rune', rot=(0, 0, xx * 4))  # floating sigils

    def more_brute(r, d):
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']; rnd = _r.Random(9)
        for i in range(10):
            xx = rnd.uniform(-7, 7); yy = rnd.uniform(24, 36); h = rnd.uniform(2, 5)
            B('chest', (xx, yy, 6.2), (1.6, h, 1.6), 'spike', rot=(rnd.uniform(20, 50), 0, xx * 3))
        for side, s in (('right', -1), ('left', 1)):
            x = s * 13
            for i in range(4): B(f'{side}_hand', (x + (i - 1.5) * 2.4, hy + 5.2, -5.2), (2, 1.2, 0.6), 'claw')        # finger ridges
            for i in range(4): B(f'{side}_hand', (x + (i - 1.5) * 2.4, hy + 3.4, -5.2), (2, 1.0, 0.6), 'claw')
            for i in range(3): B(f'{side}_forearm', (x, sy - 10 - i * 2.6, -4.2), (7, 0.8, 0.4), 'plate')              # cracked plating bands
            B(f'{side}_shin', (s * 4.5, 8.8, -4.8), (2, 3, 2), 'spike', rot=(-40, 0, 0))                              # knee crystal
            B(f'{side}_shin', (s * 4.5 + s * 2, 8.4, -4.4), (1.2, 2, 1.2), 'spike', rot=(-30, 0, s * -25))
            for i in range(3): B(f'{side}_arm', (x + s * 4.4, sy - 4 - i * 2.6, 1), (1.2, 2, 1.2), 'spike', rot=(0, 0, s * -40))
            for i in range(3): B(f'{side}_forearm', (x + s * 5.4, hy + 4.4 - i * 1.4, -2 - i * 0.6), (0.6, 1.2, 1.6), 'guard', rot=(i * 40, 0, 0))
        for xx in (-4, -1.4, 1.4, 4): B('jaw', (xx, ny + 0.8, -5.9), (0.8, 1.2, 0.6), 'claw')
        for s in (-1, 1): B('head', (s * 5.4, ny + 7.6, -5), (1.4, 3.6, 1.4), 'spike', rot=(-20, 0, s * -35))         # brow horns
        r.bone('loin_back', 'hips', (0, 15, 5.4)); B('loin_back', (0, 10.5, 5.5), (8, 9, 0.6), 'robe')
        tatters(r, 'loin_front', 6.2, -4, 4, -5.6, 4, 3, seed=10); tatters(r, 'loin_back', 6.2, -4, 4, 5.6, 4, 3, seed=11)

    def boost(fn, more):
        def w():
            r, d = fn(); more(r, d); return r, d
        return w
    rig_ghoul = boost(rig_ghoul, more_ghoul); rig_stalker = boost(rig_stalker, more_stalker)
    rig_caster = boost(rig_caster, more_caster); rig_brute = boost(rig_brute, more_brute)

    def shift(r, dy):
        for b in r.bones.values():
            if b['name'] == 'root': continue
            b['pivot'][1] += dy
            for c in b['cubes']:
                c['origin'][1] += dy
                if 'pivot' in c: c['pivot'][1] += dy

    def pose(r, **rots):
        for bn, rot in rots.items(): r.bones[bn]['rotation'] = list(rot)

    def stalker_silhouette(r, d):
        """Lean assassin: crouched, hunched forward, broad shoulders over a narrow waist, splayed bent legs."""
        B = r.box
        t = r.bones['torso']['cubes'][0]; t['size'][0] = 5.4; t['origin'][0] = -2.7      # narrow waist
        B('chest', (0, 22.2, 0), (11.5, 4.2, 5.2), 'robe')                                # broad shoulders
        for s in (-1, 1): B('chest', (s * 5.8, 23.6, 0), (3.2, 2.2, 5.6), 'gauntlet', rot=(0, 0, s * -20))
        pose(r, torso=(26, 0, 0), head=(-18, 0, 9),
             right_leg=(-30, 0, 14), left_leg=(-30, 0, -14), right_shin=(52, 0, 0), left_shin=(52, 0, 0),
             right_arm=(-35, 0, 38), left_arm=(-35, 0, -38), right_forearm=(-35, 0, 0), left_forearm=(-35, 0, 0),
             right_blade=(150, 0, 25), left_blade=(150, 0, -25),
             robe_front=(-22, 0, 0), robe_back=(28, 0, 0), robe_right=(0, 0, 22), robe_left=(0, 0, -22),
             scarf=(55, 0, 15), hood_tip=(-45, 0, 0))
        for bn in ('right_arm', 'left_arm'):  # arms hang from the wider shoulders
            s = -1 if bn.startswith('right') else 1
            for b in (bn, bn.replace('arm', 'forearm'), bn.replace('arm', 'hand'), bn.replace('arm', 'blade')):
                r.bones[b]['pivot'][0] += s * 1.2
                for c in r.bones[b]['cubes']:
                    c['origin'][0] += s * 1.2
                    if 'pivot' in c: c['pivot'][0] += s * 1.2
        for i, (xx, h) in enumerate(((-4, 7), (-1.5, 9), (1.5, 8), (4, 6))):           # ragged flared panel strips
            B('robe_front', (xx, -3 - h / 2 + 2, -3.4), (2, h, 0.35), 'robe', rot=(-10, 0, xx * 3))
            B('robe_back', (xx, -3 - h / 2 + 1, 3.4), (2.2, h + 2, 0.35), 'capehem', rot=(14, 0, xx * 3))
        shift(r, -2.2)

    def caster_silhouette(r, d):
        """Hovering caster: tapered robe (narrow top, wide hem), no legs, wide drooping sleeves held out, head forward."""
        B = r.box
        for bn in ('right_leg', 'left_leg', 'right_shin', 'left_shin'): r.bones[bn]['cubes'] = []
        r.bones['robe_upper']['cubes'][0].update(origin=[-4.5, 4, -3.5], size=[9, 9, 7])
        for i, (w, dd, y) in enumerate(((11, 9, 3.5), (14, 11, 0.5), (17.5, 13.5, -2.5))):  # tiers flaring outward
            B('robe_lower', (0, y, 0), (w, 3.2, dd), 'robe')
        for i in range(10):
            a = 2 * math.pi * i / 10
            B('robe_lower', (math.sin(a) * 8.4, -4.2, math.cos(a) * 6.4), (3.4, 2.4, 0.4), 'capehem', rot=(18 * math.cos(a), math.degrees(a), 0))
        for side, s in (('right', -1), ('left', 1)):
            x = s * 5; sy = d['sy']
            B(f'{side}_forearm', (x, sy - 9.5, -0.5), (8.5, 9, 8.5), 'sleeve')                       # wide drooping sleeve
            B(f'{side}_forearm', (x, sy - 15, 1.6), (7.5, 4, 5), 'capehem', rot=(25, 0, 0))            # hanging sleeve tip
            B(f'{side}_forearm', (x, sy - 14.4, -0.5), (9, 0.8, 9), 'collar')
        pose(r, torso=(-20, 0, 0), head=(10, 0, 0), right_arm=(0, 0, 32), left_arm=(0, 0, -32), hood_cone=(-12, 0, 0))
        for bn in ['orb'] + [f'orb_rune{i}' for i in range(4)]:   # rebuild the orb as a free-floating void pearl
            del r.bones[bn]; r.order.remove(bn)
        r.bone('orb_orbit', 'root', (0, 22, 0))
        oy, oz = 22, -11
        r.bone('orb', 'orb_orbit', (0, oy, oz))
        R = 2.9                                            # stepped sphere: horizontal slabs, each a cross + a 45deg-rotated octagon
        for k_ in range(-3, 4):
            yy = k_ * 0.8; rr = math.sqrt(max(R * R - yy * yy, 0.5)); w = 2 * rr; h = 0.82
            B('orb', (0, oy + yy, oz), (w, h, w * 0.42), 'orb'); B('orb', (0, oy + yy, oz), (w * 0.42, h, w), 'orb')
            B('orb', (0, oy + yy, oz), (w * 0.72, h, w * 0.72), 'orb', rot=(0, 45, 0))
        for rx, ry in ((45, 0), (0, 45), (45, 45)):         # translucent rotated shells round off the corners
            B('orb', (0, oy, oz), (4.9, 4.9, 4.9), 'orbshell', rot=(rx, ry, 0))
        r.bone('orb_band', 'orb', (0, oy, oz), (20, 0, 15))
        for i in range(10):
            a_ = 2 * math.pi * i / 10
            B('orb_band', (math.cos(a_) * 3.2, oy, oz + math.sin(a_) * 3.2), (1.3, 0.7, 0.6), 'rune', rot=(0, -math.degrees(a_), 0))
        for i in range(4):
            a_ = i * math.pi / 2; bn = r.bone(f'orb_rune{i}', 'orb', (0, oy, oz))
            B(bn, (math.cos(a_) * 4.6, oy + math.sin(a_) * 4.6, oz), (0.9, 1.4, 0.6), 'orbshell', rot=(0, 0, 45))
        r.bone('sigil_ring', 'root', (0, 16, 0))
        for i in range(6):
            a = 2 * math.pi * i / 6
            B('sigil_ring', (math.cos(a) * 13, 16 + (i % 2) * 5, math.sin(a) * 13), (2, 3, 0.4), 'rune', rot=(0, -math.degrees(a) + 90, 0))
        shift(r, 3.0)

    def phantom(r, d):
        """Rift Phantom (entity id rift_stalker): solid hunched upper body with broad tattered shoulders, a cracked porcelain
        mask, and below the waist only swaying shadow-smoke tails. Chain-blades trail from both wrists."""
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        for bn in ('right_leg', 'left_leg', 'right_shin', 'left_shin', 'hood_tip', 'right_blade', 'left_blade', 'scarf'):
            r.bones[bn]['cubes'] = []
        r.bones['head']['cubes'] = [c for c in r.bones['head']['cubes'] if c['role'] not in ('hood', 'helm', 'visorplate')]
        r.bones['hips']['cubes'] = [c for c in r.bones['hips']['cubes'] if c['role'] in ('belt', 'rune')]
        # mask: porcelain plate with brow, cheek shards and a split chin
        B('head', (0, ny + 4.2, -4.0), (7.2, 7.4, 0.9), 'mask')
        B('head', (0, ny + 7.4, -4.4), (6.6, 1.2, 0.6), 'mask', rot=(-12, 0, 0))
        for s in (-1, 1):
            B('head', (s * 3.3, ny + 2.6, -3.9), (1.4, 3.4, 1), 'mask', rot=(0, s * -18, 0))
            B('head', (s * 1.6, ny + 4.9, -4.5), (2.0, 0.6, 0.3), 'visorplate', rot=(0, 0, s * -8))   # slit eyes
        B('head', (0, ny + 0.7, -3.8), (3, 1.4, 0.8), 'mask', rot=(8, 0, 0))
        for i, (xx, h) in enumerate(((-3.6, 6), (-1.2, 8), (1.2, 7), (3.6, 5))):                 # ragged hair/shroud behind
            B('head', (xx, ny + 6 - h / 2 + 2, 3.6), (2.2, h, 0.5), 'smoke', rot=(20, 0, xx * 4))
        for s in (-1, 1):                                                                           # tattered mantle
            for j in range(4):
                B('chest', (s * (3.5 + j * 1.6), 23.4 - j * 0.6, 0), (2.4, 4 + j, 6), 'robe', rot=(0, 0, s * (-12 - j * 9)))
                B('chest', (s * (3.8 + j * 1.6), 19.6 - j * 1.4, 2.9), (1.6, 3 + j, 0.4), 'capehem', rot=(10, 0, s * (-8 - j * 6)))
        # smoke tails: layered translucent bones that sway (reuse the old robe bone names + chained wisps)
        for name, (x, z, w) in (('robe_front', (0, -1.6, 6)), ('robe_back', (0, 1.8, 7)), ('robe_right', (-2.6, 0, 3.4)), ('robe_left', (2.6, 0, 3.4))):
            r.bones[name]['cubes'] = []; r.bones[name]['pivot'] = [x, 12, z]
            B(name, (x, 8.5, z), (w, 7, 3.5), 'smoke')
            prev = name
            for k_ in range(3):
                wn = f'{name}_wisp{k_}'; y0 = 5 - k_ * 3.6
                r.bone(wn, prev, (x, y0, z), (8 if 'back' in name else -6 if 'front' in name else 0, 0, 0))
                B(wn, (x, y0 - 1.8, z), (w * (0.8 - k_ * 0.18), 3.8, 3.0 - k_ * 0.6), 'smoke')
                prev = wn
        # chain-blades from both wrists: 5 links then a curved blade
        for side, sgn in (('right', -1), ('left', 1)):
            x = sgn * 6.2; prev = f'{side}_hand'; y = hy + 0.5
            B(prev, (x, hy + 1.6, 0), (4.2, 1.4, 4.2), 'guard')                                    # wrist shackle
            for k_ in range(5):
                cn = f'{side}_chain{k_}'
                r.bone(cn, prev, (x, y, 0), (55, 0, sgn * 22) if k_ == 0 else (0, 0, 0))   # cancel arm pose so links hang
                B(cn, (x, y - 1.0, 0), (0.6 if k_ % 2 else 1.2, 2.1, 1.2 if k_ % 2 else 0.6), 'guard')
                prev = cn; y -= 2.0
            bn = f'{side}_chainblade'; r.bone(bn, prev, (x, y, 0), (0, 0, 0))
            B(bn, (x, y - 0.6, 0), (1.6, 1.4, 1.6), 'grip')
            for k_, (dy, dz, rx) in enumerate(((-3.4, -0.6, 10), (-7.0, -2.0, 30), (-9.8, -4.4, 55), (-11.2, -7.0, 75))):
                B(bn, (x, y + dy, dz), (0.5, 4.2 - k_ * 0.6, 1.8 - k_ * 0.3), 'blade', rot=(rx, 0, 0))
        pose(r, torso=(-28, 0, 0), head=(18, 0, 7), right_arm=(-20, 0, 22), left_arm=(-20, 0, -22),
             right_forearm=(-25, 0, 0), left_forearm=(-25, 0, 0))
        for bn in ('right_arm', 'left_arm'):
            sg = -1 if bn.startswith('right') else 1
            for b in (bn, bn.replace('arm', 'forearm'), bn.replace('arm', 'hand')):
                r.bones[b]['pivot'][0] += sg * 1.2
                for c in r.bones[b]['cubes']:
                    c['origin'][0] += sg * 1.2
                    if 'pivot' in c: c['pivot'][0] += sg * 1.2
        t = r.bones['torso']['cubes'][0]; t['size'][0] = 5.4; t['origin'][0] = -2.7
        B('chest', (0, 22.2, 0), (11.5, 4.2, 5.2), 'robe')
        shift(r, 2.0)

    rig_stalker = boost(rig_stalker, phantom); rig_caster = boost(rig_caster, caster_silhouette)

    for name, fn, trim in (('rift_ghoul', rig_ghoul, '#4A3A44'), ('rift_stalker', rig_stalker, '#5A3F6A'), ('void_caster', rig_caster, '#7A5A2E'),
                           ('rift_brute', rig_brute, '#4E3A30'), ('spire_guardian', rig_guardian, '#7A6040')):
        G.MOBS[name]['rig'] = fn; G.MOBS[name]['trim'] = trim

"""v5 polish pass for the weaker mobs (ghoul, phantom, void caster, minions): broader silhouettes, splayed stances,
weapons angled across the forearm and extra surface detail. Runs after rigs_v4 / rigs_minions; bone names unchanged
so the existing animations keep working."""
import math


def install(G):
    def move(r, bones, dx=0.0, dy=0.0, dz=0.0):
        for bn in bones:
            b = r.bones[bn]
            b['pivot'][0] += dx; b['pivot'][1] += dy; b['pivot'][2] += dz
            for c in b['cubes']:
                c['origin'][0] += dx; c['origin'][1] += dy; c['origin'][2] += dz
                if 'pivot' in c:
                    c['pivot'][0] += dx; c['pivot'][1] += dy; c['pivot'][2] += dz

    def chain(r, root):
        out = [root]
        for bn in r.order:
            b = r.bones[bn]
            if b.get('parent') in out and bn not in out:
                out.append(bn)
        return out

    def wrap(name, fn):
        base = G.MOBS[name]['rig']

        def w():
            r, d = base(); fn(r, d); return r, d
        G.MOBS[name]['rig'] = w

    def ghoul(r, d):
        """Feral: heavy hunched shoulders over a thin waist, long arms swung wide, knees bent in a wide crouch."""
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        for side, s in (('right', -1), ('left', 1)):
            move(r, chain(r, f'{side}_arm'), dx=s * 1.4)
            B('chest', (s * 5.2, sy + 1.6, 0.6), (4.6, 3.4, 5.2), 'robe', rot=(0, 0, s * -18))          # trapezius mass
            for j, (h, rz) in enumerate(((3.6, -55), (2.6, -75), (1.8, -95))):                         # shoulder bone spurs
                B('chest', (s * (6.6 + j * 0.9), sy + 3 - j * 0.9, 1 + j * 0.6), (1.1, h, 1.1), 'spike', rot=(20, 0, s * rz))
            B(f'{side}_forearm', (x := s * 6.9, hy + 4.5, 1.8), (0.8, 3.2, 0.8), 'spike', rot=(60, 0, 0))  # elbow hook
            B(f'{side}_leg', (s * 2.4, 9.6, 1.6), (3.2, 3.2, 1.2), 'rib', rot=(0, 0, s * 8))            # hamstring
        for i in range(6):                                                                              # back hump + dorsal spines
            y = 17 + i * 1.6
            B('chest', (0, y, 2.6 + math.sin(i / 5 * math.pi) * 1.0), (6.4 - i * 0.4, 1.8, 2), 'robe')
            B('chest', (0, y + 0.6, 4.0 + math.sin(i / 5 * math.pi)), (0.9, 2.2 + (i % 2), 0.9), 'spike', rot=(50, 0, 0))
        for s in (-1, 1):                                                                               # swept-back horns
            B('head', (s * 3.2, ny + 6.4, 1.0), (1.2, 1.2, 4.2), 'spike', rot=(-25, s * 15, 0))
            B('head', (s * 3.5, ny + 7.4, 3.6), (0.8, 0.8, 2.6), 'spike', rot=(-50, s * 20, 0))
        r.bones['right_arm']['rotation'] = [-25, 0, 22]; r.bones['left_arm']['rotation'] = [-25, 0, -22]
        r.bones['right_forearm']['rotation'] = [-30, 0, 0]; r.bones['left_forearm']['rotation'] = [-30, 0, 0]
        r.bones['right_leg']['rotation'] = [-18, 0, 12]; r.bones['left_leg']['rotation'] = [-18, 0, -12]
        r.bones['right_shin']['rotation'] = [30, 0, 0]; r.bones['left_shin']['rotation'] = [30, 0, 0]

    def phantom(r, d):
        """Rift Phantom: cowl framing the mask, carved mask detail, clawed fingers, spiked shoulder shackles."""
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        B('head', (0, ny + 5.4, 0.6), (8.6, 8.6, 6.4), 'robe')                                         # cowl behind the mask
        B('head', (0, ny + 9.6, 1.4), (6.4, 1.6, 5.2), 'robe', rot=(-15, 0, 0))
        for s in (-1, 1):
            B('head', (s * 4.4, ny + 4.6, -2.8), (1.0, 7.6, 2.4), 'robe', rot=(0, s * 20, 0))           # cowl lip
            B('head', (s * 2.4, ny + 3.2, -4.55), (0.4, 1.8, 0.25), 'visorplate', rot=(0, 0, s * 12))   # tear lines
            B('head', (s * 2.9, ny + 8.6, -4.0), (0.8, 2.6, 0.8), 'mask', rot=(-20, 0, s * -28))       # mask horns
        B('head', (0.9, ny + 6.2, -4.6), (0.3, 2.6, 0.2), 'smoke', rot=(0, 0, -20))                      # crack
        B('head', (0, ny + 2.0, -4.5), (2.4, 0.3, 0.25), 'smoke')                                        # stitched mouth
        for i in range(4):
            B('head', (-0.9 + i * 0.6, ny + 2.0, -4.6), (0.15, 0.9, 0.15), 'smoke')
        for side, s in (('right', -1), ('left', 1)):
            x = s * 6.2
            for i in range(4):                                                                           # talon fingers
                B(f'{side}_hand', (x + (i - 1.5) * 0.9, hy - 1.2, -1.4), (0.5, 2.6, 0.6), 'claw', rot=(-25, 0, 0))
            for j in range(3):
                B(f'{side}_arm', (x + s * 1.2, sy + 1.8 + j * 0.4, -1.6 + j * 1.6), (0.9, 2.2, 0.9), 'spike', rot=(0, 0, s * -30))
            B(f'{side}_arm', (x, sy - 1, 0), (5.2, 1.2, 5.2), 'guard')                                   # shoulder shackle
        for i in range(5):                                                                               # rib-like chest straps
            B('chest', (0, 17.6 + i * 1.1, -2.9), (5.6 - abs(i - 2) * 0.8, 0.5, 0.4), 'belt')

    def caster(r, d):
        """Void Caster: drop the tall hood spire for a swept cowl with crown spikes, wide flared mantle."""
        B = r.box; ny, sy = d['ny'], d['sy']
        if 'hood_cone3' in r.bones:
            r.bones['hood_cone3']['cubes'] = []
        r.bones['hood_cone2']['cubes'] = []
        r.bones['hood_cone']['rotation'] = [-38, 0, 0]
        B('hood_cone2', (0, ny + 13.6, 1.4), (4.0, 2.6, 5.0), 'hood')                                   # short swept point
        for s in (-1, 1):
            for j in range(3):                                                                           # crown spikes around the cowl
                B('head', (s * (2.6 + j * 1.5), ny + 9.6 - j * 0.9, -1.5 + j * 0.8), (0.9, 3.2 - j * 0.6, 0.9), 'crown',
                  rot=(-10, 0, s * (-12 - j * 16)))
            for j in range(3):                                                                           # flared high mantle
                B('chest', (s * (6.2 + j * 1.9), 25.2 - j * 1.3, 0.4), (3.0, 1.4, 8.6 - j * 1.2), 'collar',
                  rot=(0, 0, s * (-20 - j * 14)))
                B('chest', (s * (6.6 + j * 1.9), 23.4 - j * 1.3, 0.4), (2.6, 2.6, 0.5), 'rune' if j == 1 else 'robe',
                  rot=(0, 0, s * (-20 - j * 14)))
        B('chest', (0, 25.6, 3.4), (8, 4.6, 1), 'collar', rot=(-15, 0, 0))                              # standing back collar

    def thrall(r, d):
        """Bone Thrall: shoulder armour both sides, torn back cloak, splayed stance; cleaver held across the forearm."""
        B = r.box; ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        B('chest', (-4.6, sy + 0.9, 0), (3.4, 2.2, 3.8), 'pauldron', rot=(0, 0, 18))                    # right scrap pauldron
        for i in range(3):
            B('chest', (-4.8 - i * 0.6, sy - 0.2 - i * 0.5, -1.9), (0.6, 0.6, 0.6), 'trim')             # rivets
        B('chest', (0, 24.2, 0.6), (8, 1.2, 3.2), 'cape')                                                # mantle strap
        r.bone('cloak', 'chest', (0, 24, 1.8), (8, 0, 0))
        for i, (xx, h) in enumerate(((-2.6, 10), (-0.9, 12), (0.9, 11), (2.6, 9))):
            B('cloak', (xx, 24 - h / 2, 2.0 + i % 2 * 0.2), (1.8, h, 0.35), 'cape', rot=(0, 0, (i - 1.5) * 3))
        for side, s in (('right', -1), ('left', 1)):
            B(f'{side}_forearm', (s * 5, hy + 4.2, 0), (2.8, 2.2, 2.8), 'cape')                         # forearm wraps
            B(f'{side}_shin', (s * 2, 5.4, -1.2), (2.4, 2.0, 0.6), 'trim')                               # greave scrap
        r.bones['right_leg']['rotation'] = [0, 0, 6]; r.bones['left_leg']['rotation'] = [0, 0, -6]
        r.bones['right_arm']['rotation'] = [-32, 0, 10]
        if 'right_grip' in r.bones:                                                                      # angle across the forearm
            r.bones['right_grip']['rotation'] = [-58, 0, -20]

    def familiar(r, d):
        """Rift Familiar: spiked rift mane and chest plate for a stronger profile."""
        B = r.box
        for i in range(5):
            B('chest', (0, 15.2 + i * 0.1, -3.6 + i * 1.1), (0.9, 2.4 - abs(i - 2) * 0.3, 0.9), 'spike', rot=(-35, 0, 0))
        for i in range(4):
            B('body', (0, 14.0, 0 + i * 2.0), (0.8, 1.8 - i * 0.2, 0.8), 'spike', rot=(-30, 0, 0))
        for s in (-1, 1):
            B('chest', (s * 3.9, 11.4, -4.0), (0.6, 4.2, 3.0), 'claw', rot=(0, s * 10, 0))

    # v9 texture pass: every mob at 32 px per block (k=2) with one shared void-purple / rift-red palette.
    for name, m in G.MOBS.items():
        m['k'] = 2; m['ts'] = [512, 512] if name in ('rift_warden', 'spire_guardian', 'rift_brute') else [256, 256]
        m['glow'] = '#C42E44'; m['crystal'] = '#8A45C0'
        if name not in ('rift_warden', 'rift_brute'): m['eye'] = '#B44BE6'
    G.MOBS['rift_familiar']['pal'] = dict(base='#2A2338', dark='#100C18', accent='#5A3A8A', cape='#3A2A5A', robe='#3A2A5A')
    G.MOBS['rift_familiar']['glow'] = '#A24BE0'
    G.MOBS['bone_thrall']['core'] = '#B44BE6'; G.MOBS['bone_thrall']['glow'] = '#A24BE0'

    wrap('rift_ghoul', ghoul); wrap('rift_stalker', phantom); wrap('void_caster', caster)
    wrap('bone_thrall', thrall); wrap('rift_familiar', familiar)

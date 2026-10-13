"""Class minion rigs: the Necromancer's Bone Thrall (humanoid skeleton with a rusted cleaver) and the Summoner's
Rift Familiar (spectral rift-wolf). Imported by gen_geo_assets.py; registered into MOBS and given their own anims."""
import math

def install(G):
    Rig, humanoid, kf, wave = G.Rig, G.humanoid, G.kf, G.wave

    def rig_thrall():
        r = Rig(); d = humanoid(r, 12, 2, (2.4, 2.4), (6, 11, 3), 7, (2.2, 15, 2.2), 5, 2.4, hunch=-8, hip_role='belt'); B = r.box
        ny, sy, hy = d['ny'], d['sy'], d['hand_y']
        r.bones['torso']['cubes'] = []; r.bones['chest']['cubes'] = []
        B('torso', (0, 15, 0.6), (1.6, 6, 1.6), 'rib')                                                 # spine
        for i in range(5): B('torso', (0, 13 + i * 1.3, 1.2), (1.1, 0.7, 1.4), 'spike')                # vertebra knobs
        for i in range(5):                                                                             # rib cage
            y = 18 + i * 1.3; w = 6.4 - abs(i - 2) * 0.6
            for s in (-1, 1):
                B('chest', (s * w / 4, y, -1.2), (w / 2, 0.8, 0.6), 'rib', rot=(0, 0, s * -10))
                B('chest', (s * w / 2, y, 0.2), (0.6, 0.8, 2.6), 'rib')
        B('chest', (0, 20.5, -1.4), (1.0, 7, 0.8), 'rib')                                              # sternum
        B('chest', (0, 18, 0.8), (1.6, 10, 1.6), 'rib')
        B('chest', (0, ny - 0.6, 0), (7.4, 1.2, 2.8), 'rib')                                           # collarbone
        for s in (-1, 1):                                                                              # rusted pauldron (left) / bone shard (right)
            if s > 0:
                B('chest', (s * 4.6, sy + 0.8, 0), (3.6, 2.4, 4), 'pauldron', rot=(0, 0, -15))
                B('chest', (s * 4.6, sy + 2.4, 0), (2.2, 1.0, 3), 'trim', rot=(0, 0, -15))
            else:
                B('chest', (s * 4.4, sy + 1.4, 0), (1.2, 3.2, 1.2), 'spike', rot=(0, 0, 20))
        r.bones['hips']['cubes'] = []
        B('hips', (0, 12, 0), (5.6, 2, 2.6), 'rib'); B('hips', (0, 11, -1.4), (6.2, 1.2, 0.6), 'belt')
        r.bone('loin_front', 'hips', (0, 11, -1.6)); B('loin_front', (0, 8.2, -1.7), (4, 5.4, 0.4), 'cape')
        r.bone('loin_back', 'hips', (0, 11, 1.6)); B('loin_back', (0, 7.6, 1.7), (5, 6.4, 0.4), 'cape')
        for nm in ('right_leg', 'left_leg', 'right_shin', 'left_shin', 'right_arm', 'left_arm', 'right_forearm', 'left_forearm'):
            for c in r.bones[nm]['cubes']: c['role'] = 'rib'
        for side, s in (('right', -1), ('left', 1)):
            x = s * 2
            B(f'{side}_leg', (x, 6.1, -0.2), (2.2, 1.2, 2.2), 'claw')                                 # knee cap
            B(f'{side}_shin', (x, 0.6, -1.2), (2.6, 1.2, 4), 'claw')                                  # foot
            B(f'{side}_shin', (x, 3.4, 0), (2.8, 2.2, 2.8), 'cape', rot=(0, s * 10, 0))               # wraps
            xa = s * 5
            B(f'{side}_arm', (xa, sy - 7.5, 0), (2.4, 1.2, 2.4), 'claw')                               # elbow
            for i, ox in enumerate((-0.7, 0.0, 0.7)):
                B(f'{side}_hand', (xa + ox, hy - 3.3, -0.6), (0.5, 1.8, 0.5), 'claw')
        # skull: brow ridge, hollow cheeks, cracked crown, broken horn
        hs = 7; r.bones['head']['cubes'] = []
        B('head', (0, ny + 4.6, 0), (6.6, 5.2, 6.4), 'skull')
        B('head', (0, ny + 6.0, -3.3), (6.8, 1.2, 0.6), 'rib')
        for s in (-1, 1):
            B('head', (s * 3.4, ny + 3.4, -2.2), (0.6, 2.2, 2.4), 'rib')
            B('head', (s * 3.2, ny + 7.6, 0.5), (1, 2.2, 1), 'spike', rot=(0, 0, s * -25))
        B('head', (0, ny + 7.5, -1), (0.4, 1.0, 4), 'visorplate')                                     # crack glow
        r.bones['jaw']['cubes'] = []
        B('jaw', (0, ny + 1.2, -0.6), (5.6, 1.6, 5), 'jaw')
        for xx in (-1.8, -0.6, 0.6, 1.8): B('jaw', (xx, ny + 2.2, -2.9), (0.6, 0.8, 0.5), 'claw')
        # rusted cleaver in the right hand (blade points forward/down like the vanilla held pose)
        xr = -5; g = hy - 1.2   # cleaver: handle across the fist, blade forward
        gb = G.grip_fist(r, 'right_hand', (xr, g, 0), (-80, 0, 0), 3.0, -1, fist='rib', finger='claw', stud='claw')
        r.bone('cleaver', gb, (xr, g, 0))
        B('cleaver', (xr, g - 0.6, 0), (1.1, 4.6, 1.1), 'grip')
        B('cleaver', (xr, g + 2.0, 0), (2.0, 0.8, 1.6), 'guard')
        B('cleaver', (xr, g + 6.6, 1.0), (0.5, 8.2, 4.2), 'blade')      # blade below the spine
        B('cleaver', (xr, g + 6.6, -1.2), (0.7, 8.4, 0.6), 'trim')      # thick spine
        for i in range(3): B('cleaver', (xr, g + 4.2 + i * 2.6, 2.9), (0.6, 1.0, 0.6), 'rune')
        # soul fire wisps in the chest cavity
        r.bone('soul', 'chest', (0, 20, 0)); B('soul', (0, 20, 0), (2, 2, 2), 'core'); B('soul', (0, 21.6, 0), (1, 1.4, 1), 'core')
        return r, d

    def rig_familiar():
        """Spectral rift-wolf. Model faces north (-Z). 4 two-segment legs, chest/body, head+jaw+ears, wisp tail."""
        r = Rig(); B = r.box; r.bone('root', None, (0, 0, 0))
        r.bone('body', 'root', (0, 10, 2)); B('body', (0, 10.5, 3.5), (6.5, 6.5, 9), 'body')
        r.bone('chest', 'body', (0, 11, -2)); B('chest', (0, 11.2, -3), (7.5, 7.5, 6), 'chest')
        B('chest', (0, 14.4, -2.5), (8.2, 2.4, 6.6), 'cape')                                          # mane ruff
        for i in range(5): B('chest', (-3.2 + i * 1.6, 15.6, -1.0 + (i % 2)), (1.1, 2.6, 1.1), 'spike', rot=(25, 0, (i - 2) * 12))
        for i in range(4): B('body', (0, 14.2, 0.5 + i * 2.2), (1.0, 1.6, 1.2), 'spike', rot=(30, 0, 0))
        for s in (-1, 1):
            B('body', (s * 3.3, 10.8, 3.5), (0.5, 3.6, 6.6), 'rune')                                  # rift-lines on the flanks
            for i in range(3): B('chest', (s * 3.85, 9.8 + i * 1.2, -3.5), (0.4, 0.6, 3), 'rib')
        r.bone('head', 'chest', (0, 13, -5.5))
        B('head', (0, 13, -8.4), (5.6, 5, 5), 'skull')
        B('head', (0, 12.2, -12.2), (3, 2.4, 3.6), 'skull')                                           # snout
        B('head', (0, 13.6, -13.9), (1.4, 0.9, 0.6), 'visorplate')                                    # nose
        B('head', (0, 15.4, -8.6), (5.8, 1.0, 4.8), 'cape')                                           # brow fur
        for s in (-1, 1):
            r.bone(f'{s}_ear'.replace('-1', 'right').replace('1', 'left'), 'head', (s * 1.8, 15.4, -7.8))
        for nm, s in (('right_ear', -1), ('left_ear', 1)):
            B(nm, (s * 1.9, 16.8, -7.8), (1.6, 3, 1.0), 'cape', rot=(-10, 0, s * 12))
            B(nm, (s * 1.9, 18.6, -7.9), (0.8, 1.2, 0.6), 'spike', rot=(-10, 0, s * 12))
        r.bone('jaw', 'head', (0, 11.2, -9))
        B('jaw', (0, 10.8, -11.6), (2.6, 1.0, 4), 'jaw')
        for s in (-1, 1):
            B('jaw', (s * 1.0, 11.6, -13.2), (0.4, 0.9, 0.4), 'claw')
            B('head', (s * 1.0, 10.8, -13.4), (0.4, 1.0, 0.4), 'claw')
        for s, z in ((-1, -2.5), (1, -2.5), (-1, 6.5), (1, 6.5)):
            front = z < 0; side = 'right' if s < 0 else 'left'; nm = f"{'front' if front else 'back'}_{side}_leg"
            x = s * 2.4
            r.bone(nm, 'chest' if front else 'body', (x, 10, z))
            B(nm, (x, 7.6, z), (2.4, 5.4, 2.6), 'limb')
            r.bone(nm + '_low', nm, (x, 5, z), (0 if front else 12, 0, 0))
            B(nm + '_low', (x, 2.8, z + (0 if front else 0.6)), (1.8, 4.6, 1.8), 'limb')
            B(nm + '_low', (x, 0.5, z - 0.5), (2.4, 1.0, 3.0), 'claw')                                # paw
            for t in (-0.7, 0, 0.7): B(nm + '_low', (x + t, 0.4, z - 2.2), (0.5, 0.6, 0.8), 'claw')
        prev = 'body'; y, z = 12.5, 8
        for k_ in range(4):
            nm = f'tail_wisp{k_}'; r.bone(nm, prev, (0, y, z), (-40 if k_ == 0 else -8, 0, 0))
            B(nm, (0, y, z + 1.6), (2.4 - k_ * 0.4, 2.4 - k_ * 0.4, 3.4), 'smoke' if k_ > 1 else 'cape')
            prev = nm; z += 3.2
        return r, d_familiar

    d_familiar = dict(L=10, th=8, hs=5, sy=12, ny=13, sx=3, aw=2, ah=8, tw=7, td=9, hand_y=0)
    G.MOBS['bone_thrall'] = dict(rig=rig_thrall, k=4, ts=[256, 256], pal=dict(base='#B8AE98', dark='#4A4236', accent='#5A3A2A', cape='#3A2E28'),
                                 trim='#7A5A3A', glow='#3FE0A0', eye='#5CFFC8', core='#4CF0B0', eyes='hollow')
    G.MOBS['rift_familiar'] = dict(rig=rig_familiar, k=4, ts=[256, 256], pal=dict(base='#2C3150', dark='#11131F', accent='#4A5AA0', cape='#3A4170', robe='#3A4170'),
                                   glow='#59C8FF', eye='#A8F4FF', crystal='#6A8CFF', eyes='dots')

    def familiar_anims(name, rig):
        has = lambda b: b in rig.bones
        A = {}
        I = {'body': {'scale': kf([(t, [1, 1 + 0.02 * math.sin(2 * math.pi * t / 2.4), 1]) for t in [i * 0.3 for i in range(9)]])},
             'head': {'rotation': kf(wave(2.4, 4, 0))}, 'jaw': {'rotation': kf(wave(2.4, 3, 0, base=(4, 0, 0)))},
             'right_ear': {'rotation': kf(wave(1.2, 5, 2))}, 'left_ear': {'rotation': kf(wave(1.2, 5, 2, phase=1))}}
        for k_ in range(4): I[f'tail_wisp{k_}'] = {'rotation': kf(wave(1.6, 10, 1, phase=k_ * 0.8))}
        A['idle'] = dict(loop=True, length=2.4, bones=I)
        T = 0.6; W = {}
        for leg, ph in (('front_right_leg', 0), ('back_left_leg', 0), ('front_left_leg', math.pi), ('back_right_leg', math.pi)):
            W[leg] = {'rotation': kf(wave(T, 32, 0, phase=ph))}
            W[leg + '_low'] = {'rotation': kf([(T * i / 8, [max(0, math.sin(2 * math.pi * i / 8 + ph + 1.4)) * -30 + (12 if 'back' in leg else 0), 0, 0]) for i in range(9)])}
        W['body'] = {'position': kf(wave(T / 2, 0.6, 1, n=4)), 'rotation': kf(wave(T, 3, 0))}
        W['head'] = {'rotation': kf(wave(T / 2, 4, 0, n=4))}
        for k_ in range(4): W[f'tail_wisp{k_}'] = {'rotation': kf(wave(T, 12, 1, phase=k_ * 0.9, base=(-8 if k_ == 0 else 4, 0, 0)))}
        A['walk'] = dict(loop=True, length=T, bones=W)
        # pounce-bite: crouch (0.25s), lunge + jaw snap at 0.4s (impact tick 7, matches RiftMob default)
        Z = [0, 0, 0]
        A['attack'] = dict(loop=False, length=0.8, bones={
            'body': {'rotation': kf([(0, Z), (0.25, [8, 0, 0]), (0.4, [-10, 0, 0]), (0.8, Z)]),
                     'position': kf([(0, Z), (0.25, [0, -1.5, 1.5]), (0.4, [0, 1, -3]), (0.8, Z)])},
            'head': {'rotation': kf([(0, Z), (0.25, [-20, 0, 0]), (0.4, [15, 0, 0]), (0.8, Z)])},
            'jaw': {'rotation': kf([(0, Z), (0.25, [40, 0, 0]), (0.4, [0, 0, 0]), (0.5, [20, 0, 0]), (0.8, Z)])},
            'front_right_leg': {'rotation': kf([(0, Z), (0.25, [20, 0, 0]), (0.4, [-50, 0, 0]), (0.8, Z)])},
            'front_left_leg': {'rotation': kf([(0, Z), (0.25, [20, 0, 0]), (0.4, [-45, 0, 0]), (0.8, Z)])},
            'back_right_leg': {'rotation': kf([(0, Z), (0.25, [-25, 0, 0]), (0.4, [30, 0, 0]), (0.8, Z)])},
            'back_left_leg': {'rotation': kf([(0, Z), (0.25, [-25, 0, 0]), (0.4, [30, 0, 0]), (0.8, Z)])},
            'tail_wisp0': {'rotation': kf([(0, [0, 0, 0]), (0.4, [-25, 0, 0]), (0.8, [0, 0, 0])])}})
        for a in A.values():
            a['bones'] = {b: v for b, v in a['bones'].items() if has(b)}
        return A
    G.CUSTOM_ANIMS = {'rift_familiar': familiar_anims}

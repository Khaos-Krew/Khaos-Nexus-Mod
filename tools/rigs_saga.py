"""SUPERSEDED by rigs_saga2.py (1.2 v2 rework). Kept for reference; do not run, it would overwrite the v2 assets."""
import sys; sys.exit("use rigs_saga2.py") if __name__ == "__main__" else None
"""1.2 Hollow Crown bosses (GeckoLib, same pipeline as the 1.1 bosses: 4x paint -> Lanczos -> unsharp, lossless PNG).
- Vaelith, the Glass Choir: a floating crystal conductor. Glass-shard skirt instead of legs, a crown of crystal spines,
  four singing masks orbiting on a choir ring, a crystal baton in each hand.
- The Fallen Hierophant: a hunched, corrupted high priest. Deep hood and broken mitre, tattered vestments and cape,
  a censer-staff (two-handed in combat) with a hanging censer, a sigil burning in the off hand.
Run: python3 rigs_saga.py   (writes only these two mobs; the 1.1 mobs are untouched)."""
import math, sys, copy
import gen_geo_assets as G
import rigs_v4, rigs_minions, rigs_v5
rigs_v4.install(G); rigs_minions.install(G); rigs_v5.install(G)
Rig, humanoid, kf, wave = G.Rig, G.humanoid, G.kf, G.wave

def rig_vaelith():
    r = Rig(); d = humanoid(r, 22, 3.0, (5, 5), (12, 22, 7), 12, (5, 28, 5), 9.5, 5, hip_role='robe')
    B = r.box; L, ny, sy, hy = d['L'], d['ny'], d['sy'], d['hand_y']
    # glass skirt: a ring of long shards flaring out from the waist, hiding the legs (it floats)
    for i in range(12):
        a = 2 * math.pi * i / 12; cx, cz = math.sin(a) * 5.6, math.cos(a) * 3.8
        ln = 20 - (i % 3) * 3
        B('hips', (cx, L - ln / 2 + 1, cz), (2.6, ln, 2.2), 'spike', rot=(math.cos(a) * 14, 0, -math.sin(a) * 14))
    B('hips', (0, L + 1, 0), (13, 3, 8), 'robe')
    # chest: faceted glass plate, glowing heart, shoulder spines
    B('chest', (0, L + 16, -3.7), (8, 8, 1.2), 'breastplate'); B('chest', (0, L + 16, -4.4), (3.5, 3.5, 1), 'core')
    for side, s in (('right', -1), ('left', 1)):
        for j, (h, rz) in enumerate(((8, 25), (6, 45), (4.5, 65))):
            B('chest', (s * (5.5 + j * 1.3), sy + 3 + j * 0.4, 0.5 - j * 0.6), (1.6, h, 1.6), 'spike', rot=(-10, 0, -s * rz))
        B(f'{side}_arm', (s * 9.5, sy + 0.5, 0), (6.5, 4, 6.5), 'pauldron')
        B(f'{side}_hand', (s * 9.5, hy + 2.5, 0), (5.6, 4.6, 5.6), 'gauntlet')
    # face: a smooth glass mask with a thin choir mouth; crown of crystal spines
    B('head', (0, ny + 7, -6.3), (9, 9, 0.8), 'mask')
    for i, (x, h, rz) in enumerate(((-5, 7, 28), (-2.5, 10, 12), (0, 13, 0), (2.5, 10, -12), (5, 7, -28))):
        B('head', (x, ny + 12 + h / 2, -1), (1.8, h, 1.8), 'crown', rot=(-12, 0, rz))
    # the choir: four singing masks orbiting on a ring at shoulder height
    r.bone('choir', 'root', (0, sy + 2, 0))
    for i in range(4):
        a = 2 * math.pi * i / 4 + math.pi / 4; x, z = math.sin(a) * 19, math.cos(a) * 19
        bn = r.bone(f'choir_mask{i}', 'choir', (x, sy + 2, z))
        B(bn, (x, sy + 2, z), (5, 7, 1.4), 'mask', rot=(0, math.degrees(a), 0))
        B(bn, (x, sy - 3.5, z), (3, 4, 1), 'smoke', rot=(0, math.degrees(a), 0))
    # batons: long crystal wands pointing forward-down from each hand
    for side, s in (('right', -1), ('left', 1)):
        x = s * 9.5; bn = r.bone(f'{side}_baton', f'{side}_hand', (x, hy + 1, 0))
        B(bn, (x, hy + 1, -1.5), (1.4, 1.4, 4), 'grip')
        B(bn, (x, hy + 1, -11), (1.0, 1.0, 15), 'blade')
        B(bn, (x, hy + 1, -19), (1.8, 1.8, 2.2), 'core')
    return r, d

def rig_hierophant():
    r = Rig(); d = humanoid(r, 22, 4.0, (7, 7), (14, 22, 9), 12, (6, 26, 6), 10.5, 5, hunch=14, hip_role='belt')
    B = r.box; L, ny, sy, hy = d['L'], d['ny'], d['sy'], d['hand_y']
    # deep hood with a cracked porcelain face, broken mitre rising out of it
    B('head', (0, ny + 7, 0.6), (14.5, 13, 14.5), 'hood')
    B('head', (0, ny + 5.5, -6.8), (8.5, 7, 1), 'mask')
    prev = 'head'; y = ny + 13.5
    for i, (w, h, rx) in enumerate(((10, 5, -6), (8.5, 5, -8), (6.5, 5, -10), (3.5, 4, -12))):
        bn = r.bone(f'mitre{i}', prev, (0, y, 0.5), (rx if i else 0, 0, 0)); r.cube(bn, (-w / 2, y, -w / 2 + 0.5), (w, h, w * 0.7), 'helmspire')
        prev = bn; y += h
    # vestments: long robe panels (front/back sway), stole and corrupted sigil plate
    for name, z in (('robe_front', -4.8), ('robe_back', 4.8)):
        r.bone(name, 'hips', (0, L, z)); r.cube(name, (-7.5, 1.5, z - 0.75), (15, L - 1.5, 1.5), 'robe')
    for s in (-1, 1): B('hips', (s * 7.2, L / 2 + 1, 0), (1.5, L - 2, 8), 'robe')
    B('chest', (0, sy - 3, -4.9), (7, 9, 1), 'sigil')
    for s in (-1, 1): B('chest', (s * 3.2, sy - 6, -5.2), (2.2, 20, 0.8), 'collar')
    for side, s in (('right', -1), ('left', 1)):
        B(f'{side}_arm', (s * 10.5, sy - 4, 0), (7.4, 12, 7.4), 'sleeve')
        B(f'{side}_forearm', (s * 10.5, hy + 7, 0), (7.6, 6, 7.6), 'sleeve')
    r.bone('cape1', 'chest', (0, sy + 2, 4.8), (6, 0, 0)); r.cube('cape1', (-8, sy - 9, 4.8), (16, 11, 1), 'cape')
    r.bone('cape2', 'cape1', (0, sy - 9, 4.8)); r.cube('cape2', (-8, sy - 20, 4.8), (16, 11, 1), 'cape')
    r.bone('cape3', 'cape2', (0, sy - 20, 4.8)); r.cube('cape3', (-8, sy - 29, 4.8), (16, 9, 1), 'capehem')
    # off hand: a burning sigil held out
    r.bone('sigil_hand', 'left_hand', (10.5, hy, 0)); B('sigil_hand', (10.5, hy - 1, -4), (5, 5, 0.6), 'rune')
    # the censer-staff: long haft, crowned head, censer swinging on a chain under the head
    x = -10.5; r.bone('staff', 'right_hand', (x, hy + 2, 0))
    B('staff', (x, hy + 18, 0), (2.2, 66, 2.2), 'shaft')
    top = hy + 51
    for i in range(4):
        a = math.pi / 2 * i; B('staff', (x + math.sin(a) * 2.2, top + 3, math.cos(a) * 2.2), (1.2, 7, 1.2), 'crown', rot=(math.cos(a) * 20, 0, -math.sin(a) * 20))
    B('staff', (x, top + 1, 0), (4.5, 2, 4.5), 'guard')
    r.bone('censer', 'staff', (x, top - 1, 0))
    B('censer', (x, top - 3.5, 0), (0.6, 5, 0.6), 'guard'); B('censer', (x, top - 8, 0), (4.5, 4.5, 4.5), 'orb'); B('censer', (x, top - 8, 0), (2, 2, 2), 'core')
    return r, d

G.MOBS['vaelith'] = dict(rig=rig_vaelith, k=2, ts=[512, 512], boss=True,
    pal=dict(base='#5A6C8C', dark='#151A2E', accent='#9ED6EE', robe='#334A78'), trim='#C9E8F5', glow='#6FD8FF', eye='#EFFBFF', core='#8AF0FF', crystal='#A8E8FF', eyes='visor')
G.MOBS['fallen_hierophant'] = dict(rig=rig_hierophant, k=2, ts=[512, 512], boss=True,
    pal=dict(base='#2E2622', dark='#0F0B0A', accent='#5E4E36', robe='#3A1620', cape='#260C12'), trim='#B4903C', glow='#A42ED8', eye='#E86AF5', core='#B848FF', orb='#B05CE8', eyes='visor')

def base_set(rig, d):
    """The guardian's full move set (idle/walk/attack/roar/spin/sweep/barrier + combat loops), retargeted onto this rig."""
    cap = {}
    old = G.write_anims; G.write_anims = lambda n, A: cap.setdefault('A', A)
    try:
        G.anim_set('spire_guardian', d, rig)
    finally:
        G.write_anims = old
    return cap['A']

R0 = [0, 0, 0]
def ch(pairs, smooth=True): return kf(pairs, smooth)
def loop(period, amp, axis, base, phase=0.0, n=8): return kf(wave(period, amp, axis, n=n, phase=phase, base=base))

def vaelith_anims(name, rig):
    d = rig_vaelith()[1]; A = base_set(rig, d)
    float_ = lambda T: {'position': kf([(T * i / 8, [0, round(3 + 1.2 * math.sin(2 * math.pi * i / 8), 3), 0]) for i in range(9)])}
    choir = lambda T, turns=1: {'rotation': kf([(0, [0, 0, 0]), (T, [0, 360 * turns, 0])], smooth=False)}
    for an in A:
        b = A[an]['bones']; T = A[an]['length']
        for leg in ('right_leg', 'left_leg', 'right_shin', 'left_shin'): b.pop(leg, None)   # it floats: no stepping
        b['root'] = dict(b.get('root', {}), **float_(T))
        b['choir'] = choir(T, 1 if T >= 2 else 0.5)
        b['hips'] = {'rotation': loop(T, 3, 2, (0, 0, 0))}
    # conductor guard: batons raised forward, elbows out; tilted in the combat loops
    G_ = {'right_arm': [-55, 0, -18], 'right_forearm': [-25, 0, 0], 'left_arm': [-55, 0, 18], 'left_forearm': [-25, 0, 0],
          'right_baton': [20, 0, 0], 'left_baton': [20, 0, 0], 'chest': [-4, 0, 0], 'head': [-6, 0, 0]}
    for an, T in (('idle_combat', 3.0), ('walk_combat', 1.3)):
        b = A[an]['bones']
        for bn, base in G_.items(): b[bn] = {'rotation': loop(T, 4 if 'arm' in bn else 2, 2 if 'arm' in bn else 0, tuple(base), phase=len(bn))}
        b['root'] = dict(float_(T)); b['choir'] = choir(T, 1)
    # ATTACK: both batons cut down in a conducted downbeat (impact 0.7s)
    A['attack'] = dict(loop=False, length=1.2, bones={
        'right_arm': {'rotation': ch([(0, [-55, 0, -18]), (0.45, [-165, 0, -25]), (0.7, [-40, 0, -12]), (1.2, [-55, 0, -18])])},
        'left_arm': {'rotation': ch([(0, [-55, 0, 18]), (0.45, [-165, 0, 25]), (0.7, [-40, 0, 12]), (1.2, [-55, 0, 18])])},
        'right_forearm': {'rotation': ch([(0, [-25, 0, 0]), (0.45, [-10, 0, 0]), (0.7, [-35, 0, 0]), (1.2, [-25, 0, 0])])},
        'left_forearm': {'rotation': ch([(0, [-25, 0, 0]), (0.45, [-10, 0, 0]), (0.7, [-35, 0, 0]), (1.2, [-25, 0, 0])])},
        'chest': {'rotation': ch([(0, [-4, 0, 0]), (0.45, [-14, 0, 0]), (0.7, [14, 0, 0]), (1.2, [-4, 0, 0])])},
        'head': {'rotation': ch([(0, R0), (0.45, [-15, 0, 0]), (0.7, [8, 0, 0]), (1.2, R0)])},
        'choir': {'rotation': kf([(0, R0), (1.2, [0, 180, 0])], smooth=False)}, 'root': float_(1.2)})
    # SPIN (shard storm): batons out wide, the whole body and choir whirl (impact 0.7s)
    A['spin'] = dict(loop=False, length=1.4, bones={
        'root': {'rotation': kf([(0, R0), (0.5, [0, 30, 0]), (0.75, [0, -200, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])], smooth=False), 'position': kf([(0, [0, 3, 0]), (0.5, [0, 7, 0]), (1.0, [0, 6, 0]), (1.4, [0, 3, 0])])},
        'right_arm': {'rotation': ch([(0, [-55, 0, -18]), (0.4, [-90, 0, -80]), (1.05, [-90, 0, -85]), (1.4, [-55, 0, -18])])},
        'left_arm': {'rotation': ch([(0, [-55, 0, 18]), (0.4, [-90, 0, 80]), (1.05, [-90, 0, 85]), (1.4, [-55, 0, 18])])},
        'choir': {'rotation': kf([(0, R0), (1.4, [0, -720, 0])], smooth=False), 'scale': ch([(0, [1, 1, 1]), (0.7, [1.35, 1, 1.35]), (1.4, [1, 1, 1])])}})
    # VOLLEY: draws the batons back, then thrusts both forward to loose glass shards (release 0.6s = tick 12)
    A['volley'] = dict(loop=False, length=1.1, bones={
        'right_arm': {'rotation': ch([(0, [-55, 0, -18]), (0.4, [-20, 0, -40]), (0.6, [-95, 0, -8]), (1.1, [-55, 0, -18])])},
        'left_arm': {'rotation': ch([(0, [-55, 0, 18]), (0.4, [-20, 0, 40]), (0.6, [-95, 0, 8]), (1.1, [-55, 0, 18])])},
        'right_forearm': {'rotation': ch([(0, [-25, 0, 0]), (0.4, [-60, 0, 0]), (0.6, [0, 0, 0]), (1.1, [-25, 0, 0])])},
        'left_forearm': {'rotation': ch([(0, [-25, 0, 0]), (0.4, [-60, 0, 0]), (0.6, [0, 0, 0]), (1.1, [-25, 0, 0])])},
        'chest': {'rotation': ch([(0, R0), (0.4, [-10, 0, 0]), (0.6, [10, 0, 0]), (1.1, R0)])}, 'root': float_(1.1),
        'choir': {'rotation': kf([(0, R0), (1.1, [0, 270, 0])], smooth=False)}})
    A['sweep'] = copy.deepcopy(A['attack'])
    return A

def hierophant_anims(name, rig):
    d = rig_hierophant()[1]; A = base_set(rig, d)
    for an in A:   # censer always swings a beat behind; the sigil hand pulses
        T = A[an]['length']; b = A[an]['bones']
        b['censer'] = {'rotation': loop(T, 14, 0, (0, 0, 0), phase=1.2, n=8)}
        b['sigil_hand'] = {'scale': kf([(T * i / 4, [1 + 0.12 * (i % 2)] * 3) for i in range(5)])}
    # guard: staff gripped in the right hand in front of the body, slanted out to the right; off hand raised with the sigil
    G_ = {'right_arm': [-50, 0, -14], 'right_forearm': [-30, 0, 0], 'staff': [55, 0, 25], 'left_arm': [-70, 0, 20], 'left_forearm': [-20, 0, 0],
          'chest': [6, 0, 0], 'head': [-10, 0, 0]}
    for an, T in (('idle_combat', 3.0), ('walk_combat', 1.3)):
        b = A[an]['bones']
        for bn, base in G_.items(): b[bn] = {'rotation': loop(T, 2 if bn != 'staff' else 1, 0, tuple(base), phase=len(bn))}
    # ATTACK: staff raised high and brought down like a gavel (impact 0.7s)
    A['attack'] = dict(loop=False, length=1.2, bones={
        'right_arm': {'rotation': ch([(0, [-50, 0, -14]), (0.45, [-150, 0, -50]), (0.7, [-60, 0, -14]), (1.2, [-50, 0, -14])])},
        'right_forearm': {'rotation': ch([(0, [-30, 0, 0]), (0.45, [-5, 0, 0]), (0.7, [-20, 0, 0]), (1.2, [-30, 0, 0])])},
        'staff': {'rotation': ch([(0, [55, 0, 25]), (0.45, [30, 0, 50]), (0.7, [70, 0, 20]), (1.2, [55, 0, 25])])},
        'left_arm': {'rotation': ch([(0, [-70, 0, 20]), (0.45, [-40, 0, 30]), (0.7, [-80, 0, 15]), (1.2, [-70, 0, 20])])},
        'chest': {'rotation': ch([(0, [6, 0, 0]), (0.45, [-12, 0, 0]), (0.7, [22, 0, 0]), (1.2, [6, 0, 0])])},
        'censer': {'rotation': ch([(0, R0), (0.5, [-40, 0, 0]), (0.8, [50, 0, 0]), (1.2, R0)])}})
    # SPIN -> "Sermon of Ash": staff planted, censer swung in a full circle around him (impact 0.7s)
    A['spin'] = dict(loop=False, length=1.4, bones={
        'root': {'rotation': kf([(0, R0), (0.5, [0, 35, 0]), (0.75, [0, -200, 0]), (1.0, [0, -360, 0]), (1.4, [0, -360, 0])], smooth=False)},
        'right_arm': {'rotation': ch([(0, [-50, 0, -14]), (0.4, [-90, 0, -70]), (1.05, [-90, 0, -75]), (1.4, [-50, 0, -14])])},
        'staff': {'rotation': ch([(0, [55, 0, 25]), (0.4, [90, 0, 0]), (1.05, [90, 0, 0]), (1.4, [55, 0, 25])])},
        'censer': {'rotation': ch([(0, R0), (0.4, [-30, 0, 0]), (0.75, [-70, 0, 0]), (1.05, [-40, 0, 0]), (1.4, R0)])},
        'left_arm': {'rotation': ch([(0, [-70, 0, 20]), (0.5, [-40, 0, 40]), (1.4, [-70, 0, 20])])}})
    # BARRIER: plants the staff out to the side and raises the burning sigil overhead
    A['barrier'] = dict(loop=False, length=1.2, bones={
        'right_arm': {'rotation': ch([(0, [-50, 0, -14]), (0.4, [-50, 0, -24]), (0.9, [-50, 0, -24]), (1.2, [-50, 0, -14])])},
        'staff': {'rotation': ch([(0, [55, 0, 25]), (0.4, [55, 0, 25]), (0.9, [55, 0, 25]), (1.2, [55, 0, 25])])},
        'left_arm': {'rotation': ch([(0, [-70, 0, 20]), (0.4, [-160, 0, 30]), (0.9, [-160, 0, 32]), (1.2, [-70, 0, 20])])},
        'head': {'rotation': ch([(0, [-10, 0, 0]), (0.4, [-25, 0, 0]), (0.9, [-25, 0, 0]), (1.2, [-10, 0, 0])])},
        'sigil_hand': {'scale': ch([(0, [1, 1, 1]), (0.4, [1.8, 1.8, 1.8]), (0.9, [1.8, 1.8, 1.8]), (1.2, [1, 1, 1])])}})
    A['roar']['bones']['staff'] = {'rotation': kf([(0, [55, 0, 25]), (A['roar']['length'], [55, 0, 25])], smooth=False)}
    A['roar']['bones'].pop('right_arm', None); A['roar']['bones'].pop('right_forearm', None)   # keep the staff hand in guard while he roars
    for an in ('idle', 'walk'):   # unarmed loops: haft held a little out from the body so it never brushes the hood
        T = A[an]['length']; A[an]['bones']['staff'] = {'rotation': kf([(0, [0, 0, -10]), (T, [0, 0, -10])], smooth=False)}
    A['sweep'] = copy.deepcopy(A['attack'])
    return A

G.CUSTOM_ANIMS.update({'vaelith': vaelith_anims, 'fallen_hierophant': hierophant_anims})

if __name__ == '__main__':
    HI = 4
    for name in (sys.argv[1:] or ['vaelith', 'fallen_hierophant']):
        m = G.MOBS[name]; rig, d = m['rig']()
        uvs = G.pack(rig, m['k'], m['ts'])
        hm = dict(m, k=m['k'] * HI, ts=[m['ts'][0] * HI, 256]); huvs = G.pack(rig, hm['k'], hm['ts']); pt = G.Painter(name, hm)
        cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
        for uv, c in zip(huvs, cs): pt.cube(uv, c)
        G.downsample(name, m, rig, pt, m['k'], huvs, uvs); G.geo(name, rig, m['k'], m['ts'], uvs); G.anim_set(name, d, rig)
        print(name, len(rig.order), 'bones', len(cs), 'cubes', m['ts'])

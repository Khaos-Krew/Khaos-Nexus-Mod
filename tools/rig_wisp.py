"""Rift Wisp companion: a small crown-shard spirit. Faceted glass shell around a burning core, three orbiting motes,
a ribbon tail of fading glass. Same painter as the saga bosses (glass keeps graded alpha). Run: python3 rig_wisp.py"""
import math, json
import gen_geo_assets as G
import rigs_saga2 as S
Rig, kf = G.Rig, G.kf

def rig():
    r = Rig(); B = r.box
    r.bone('root', None, (0, 0, 0))
    r.bone('body', 'root', (0, 8, 0))
    B('body', (0, 8, 0), (2.6, 2.6, 2.6), 'core', rot=(45, 45, 0)); B('body', (0, 8, 0), (1.4, 1.4, 1.4), 'core')
    r.bone('shell', 'body', (0, 8, 0))
    for i in range(6):   # six glass petals as a faceted shell (top/bottom spikes + four sides)
        a = math.pi / 2 * i
        if i < 4:
            dx, dz = math.sin(a), math.cos(a)
            B('shell', (dx * 2.3, 8, dz * 2.3), (2.2 if abs(dz) > 0.5 else 0.6, 3.6, 0.6 if abs(dz) > 0.5 else 2.2), 'glass', rot=(dz * 12, 0, -dx * 12))
        else:
            sg = 1 if i == 4 else -1
            B('shell', (0, 8 + sg * 2.8, 0), (1.6, 2.4, 1.6), 'glass', rot=(0, 45, 0))
            B('shell', (0, 8 + sg * 4.4, 0), (0.7, 1.4, 0.7), 'spike', rot=(0, 45, 0))
    B('shell', (0, 8, 0), (5.4, 0.5, 5.4), 'crown', rot=(0, 45, 0)); B('shell', (0, 8, 0), (4.4, 0.6, 4.4), 'void', rot=(0, 45, 0))
    for k in range(4): a = math.pi / 2 * k + math.pi / 4; B('shell', (math.sin(a) * 3.1, 8.5, math.cos(a) * 3.1), (0.5, 1.2, 0.5), 'crown')
    r.bone('motes', 'body', (0, 8, 0))
    for k in range(3):
        a = 2 * math.pi * k / 3; B('motes', (math.sin(a) * 5, 8 + (k - 1) * 0.8, math.cos(a) * 5), (0.9, 0.9, 0.9), 'core', rot=(45, 45, 0))
    prev, y = 'body', 6.0
    for j in range(3):
        bn = r.bone(f'tail{j}', prev, (0, y, 0.6 + j * 1.6))
        w = 1.8 - j * 0.45
        B(bn, (0, y - 0.6, 1.4 + j * 1.6), (w, w, 2.0), 'glass', rot=(-20, 0, 0)); 
        prev, y = bn, y - 0.9
    return r

PAL = dict(who='rift_wisp', k=4, ts=[128, 128], boss=False, pal=dict(base='#6A4A9A', dark='#1A1030', accent='#C8A0FF', robe='#3A2A5A', cape='#B8A0FF'),
           trim='#E8D0FF', glow='#C070FF', eye='#FFFFFF', core='#E0B0FF', crystal='#C8B0FF', eyes='visor')

def anims():
    T = 2.0; spin, loop, A, ch = S.spin, S.loop, S.A, S.ch
    bob = {'position': kf([(T * i / 8, [0, round(0.8 * math.sin(2 * math.pi * i / 8), 3), 0]) for i in range(9)])}
    tail = lambda j: {'rotation': loop(T, 10 + 4 * j, 0, (0, 0, 0), 0.7 * j)}
    idle = {'body': bob, 'shell': {'rotation': spin(T * 2, 1, 1)}, 'motes': {'rotation': spin(T, -1, 1)}, 'tail0': tail(0), 'tail1': tail(1), 'tail2': tail(2)}
    pulse = dict(idle); pulse = {k: v for k, v in idle.items()}
    pulse['body'] = {'scale': ch([(0, [1, 1, 1]), (0.25, [1.5, 1.5, 1.5]), (0.6, [1, 1, 1])])}
    return {'idle': A(T, idle, True), 'walk': A(T, idle, True), 'pulse': A(0.6, pulse)}

if __name__ == '__main__':
    name = 'rift_wisp'; S.PAL[name] = PAL; rg = rig()
    m = dict(PAL); uvs = G.pack(rg, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, 512]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = S.Painter(name, hm)
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rg, pt, m['k'], huvs, uvs); G.geo(name, rg, m['k'], m['ts'], uvs)
    G.write_anims(name, anims())
    import shutil; shutil.copy(G.out('textures', 'entity', 'rift_wisp_glow.png'), G.out('textures', 'entity', 'rift_wisp_glowmask.png'))
    print(name, len(cs), 'cubes')

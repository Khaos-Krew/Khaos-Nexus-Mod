"""Bake freeze_<pose> review animations (DebugPose) for the simple rift mobs (idle/walk/attack sets).
Poses: idle, walk, attack_windup, attack - each layered over idle as GeckoLib plays them."""
import json, sys
import gen_geo_assets as G, clipcheck as C
POSES = {'idle': ('idle', 0.0), 'walk': ('walk', 0.25), 'attack_windup': ('attack', 0.4), 'attack': ('attack', 0.65)}   # t as a fraction of length
def run(name):
    p = G.out('animations', 'entity', f'{name}.animation.json'); js = json.load(open(p)); an = js['animations']
    bones, order, _ = C.load(name)
    for fz, (src, fr) in POSES.items():
        L1 = an[f'animation.{name}.{src}']; t = fr * L1['animation_length']; layers = [an[f'animation.{name}.idle'], L1]
        bb = {}
        for n in order:
            e = {}
            for chn in ('rotation', 'position', 'scale'):
                v = None
                for L in layers:
                    c = L['bones'].get(n, {}).get(chn)
                    if c is not None:
                        tt = 0.0 if L is layers[0] and L is not L1 else (t % L['animation_length'] if L.get('loop') else t)
                        v = C.sample(c, tt, L['animation_length']) or v
                if v is None: v = [1, 1, 1] if chn == 'scale' else [0, 0, 0]
                e[chn] = [round(q, 3) for q in v]
            bb[n] = e
        an[f'animation.{name}.freeze_{fz}'] = {'loop': True, 'animation_length': 1.0, 'bones': bb}
    json.dump(js, open(p, 'w'), indent=1); print(name, 'freezes', list(POSES))
if __name__ == '__main__':
    for n in sys.argv[1:]: run(n)

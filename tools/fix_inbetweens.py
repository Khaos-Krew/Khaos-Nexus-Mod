"""Second pass: wherever clipcheck still finds weapon points in the body (usually in-betweens), insert a weapon
keyframe at that time and give it the smallest rotation that clears the body. Repeats until clean."""
import json, itertools, sys, os
import numpy as np
sys.path.insert(0, os.path.dirname(__file__)); import clipcheck as C
from fix_weapon_clip import clip_at, set_kf

def run(name, weapon, body, combos, rounds=6):
    for r in range(rounds):
        bones, order, an = C.load(name)
        wb = list(C.descendants(bones, order, weapon)); fixed = 0
        for label, layers in combos:
            if len(layers) < 2: continue
            base, a = an[f'animation.{name}.{layers[0]}'], an[f'animation.{name}.{layers[1]}']
            ch = a['bones'].setdefault(weapon, {}).setdefault('rotation', {}); Lh = a['animation_length']
            for i in range(int(Lh / 0.05) + 1):
                t = round(i * 0.05, 3)
                lay = lambda x: [(base, x % base['animation_length']), (a, x)]
                n, _ = clip_at(bones, order, lay(t), t, wb, body)
                if not n: continue
                v0 = C.sample(ch, t, Lh) or [0, 0, 0]
                best = None
                for d in sorted(itertools.product(range(-90, 91, 10), range(-60, 61, 15), range(-90, 91, 10)), key=lambda d: sum(x * x for x in d)):
                    set_kf(ch, t, [v0[0] + d[0], v0[1] + d[1], v0[2] + d[2]])
                    tot = sum(clip_at(bones, order, lay(x), x, wb, body)[0] for x in (t, max(0, t - 0.025), min(Lh, t + 0.025)))
                    if tot == 0: best = d; break
                if best is None: set_kf(ch, t, v0)
                else: fixed += 1
        json.dump({'format_version': '1.8.0', 'animations': an}, open(f'{C.A}/animations/entity/{name}.animation.json', 'w'), indent=1)
        left = C.check(name, weapon, body, combos)
        print(name, 'round', r, 'fixed', fixed, 'left', len(left)); sys.stdout.flush()
        if not left: return True
    return False

if __name__ == '__main__':
    gbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'spire0', 'tasset_front', 'right_leg', 'left_leg']
    g = [(x, ['idle_combat', x]) for x in ('attack', 'spin', 'sweep', 'barrier', 'roar')]
    run('spire_guardian', 'halberd', gbody, g)
    wbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'right_leg', 'left_leg']
    w = [(x, ['idle_combat', x]) for x in ('attack', 'slam', 'lunge', 'draw', 'roar')]
    run('rift_warden', 'sword', wbody, w)

"""Post-pass for sculpt6 mobs whose bulkier arms/held weapon clip the legs: adds an outward roll to both
upper arms in idle/walk/attack (the hand/weapon chain follows, grips unchanged), then re-bakes freezes.
Usage: python3 splay_arms.py MOB DEG"""
import json, sys, os
sys.path.insert(0, os.path.dirname(__file__)); import clipcheck as C, freeze_mobs
def splay(name, deg):
    p = f'{C.A}/animations/entity/{name}.animation.json'; d = json.load(open(p))
    for k, a in d['animations'].items():
        if k.split('.')[-1] not in ('idle', 'walk', 'attack'): continue
        for arm, s in (('right_arm', 1), ('left_arm', -1)):
            r = a.setdefault('bones', {}).setdefault(arm, {}).setdefault('rotation', [0, 0, 0])
            if isinstance(r, dict):
                for kf in r.values():
                    v = kf['post'] if isinstance(kf, dict) and 'post' in kf else kf
                    v[2] = round(float(v[2]) + s * deg, 2)
            else: r[2] = round(float(r[2]) + s * deg, 2)
    json.dump(d, open(p, 'w'), indent=1); freeze_mobs.run(name)
if __name__ == '__main__': splay(sys.argv[1], float(sys.argv[2]))

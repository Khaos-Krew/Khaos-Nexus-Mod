"""Post-pass after gen_geo_assets.py: removes weapon clipping on the bosses.
1. Guardian combat stance: searches arm poses so the halberd is held two-handed on a diagonal (both hands on the
   haft, blade up and out to the side, clear of head/neck/torso), writes it into idle_combat/walk_combat/draw end.
2. Every action animation: per keyframe, rotates the weapon bone about its grip (hand attachment unchanged) by the
   smallest correction that leaves no weapon point inside the body, checked at keyframes and in-betweens.
Run: python3 tools/gen_geo_assets.py --anims rift_warden spire_guardian && python3 tools/fix_weapon_clip.py"""
import json, itertools, math, sys, os
import numpy as np
sys.path.insert(0, os.path.dirname(__file__)); import clipcheck as C

def fmt(v): return [round(float(x), 2) for x in v]

def clip_at(bones, order, layers, t, wb, body):
    W = C.pose(bones, order, layers, t)
    P = C.pts([c for b in wb for c in C.cubes(bones, b, W)], step=1.0)
    return sum(int(C.inside(P, C.cubes(bones, bb, W), margin=-0.4).sum()) for bb in body), W  # 0.4px clearance

def set_kf(ch, t, v):
    k = f'{t:.4g}'
    if k in ch and isinstance(ch[k], dict): ch[k]['post'] = fmt(v)
    else: ch[k] = {'post': fmt(v), 'lerp_mode': 'catmullrom'}

def kf_times(a):
    ts = set()
    for b in a['bones'].values():
        for chn in b.values():
            if isinstance(chn, dict): ts |= {float(k) for k in chn}
    return sorted(ts)

def fix_action(name, an, bones, order, base_name, act, weapon, body, log):
    a = an[f'animation.{name}.{act}']; base = an[f'animation.{name}.{base_name}']
    wb = list(C.descendants(bones, order, weapon))
    ch = a['bones'].setdefault(weapon, {}).setdefault('rotation', {})
    times = kf_times(a)
    for t in times:
        if f'{t:.4g}' not in ch: set_kf(ch, t, C.sample(ch, t, a['animation_length']) if ch else [0, 0, 0])
    L = a['animation_length']
    deltas = sorted(itertools.product(range(-135, 136, 15), range(-90, 91, 15), range(-90, 91, 15)), key=lambda d: d[0] ** 2 + d[1] ** 2 + d[2] ** 2)
    prev = None
    for t in sorted(float(k) for k in ch):
        key = f'{t:.4g}'; v0 = list(ch[key]['post'] if isinstance(ch[key], dict) else ch[key])
        probe = [t] + ([round((prev + t) / 2, 3), round(prev + (t - prev) * 0.25, 3), round(prev + (t - prev) * 0.75, 3)] if prev is not None else [])
        best = None
        for d in deltas:
            set_kf(ch, t, [v0[0] + d[0], v0[1] + d[1], v0[2] + d[2]])
            tot = sum(clip_at(bones, order, [(base, x % base['animation_length']), (a, x)], x, wb, body)[0] for x in probe)
            if tot == 0: best = d; break
            if best is None or tot < best[1]: best = (d, tot)
        if isinstance(best, tuple) and len(best) == 2 and isinstance(best[0], tuple):
            set_kf(ch, t, [v0[0] + best[0][0], v0[1] + best[0][1], v0[2] + best[0][2]]); log.append((act, t, 'residual', best[1]))
        else:
            set_kf(ch, t, [v0[0] + best[0], v0[1] + best[1], v0[2] + best[2]])
            if any(best): log.append((act, t, 'delta', best))
        prev = t

def guardian_stance(an, bones, order, body):
    name = 'spire_guardian'; wb = list(C.descendants(bones, order, 'halberd'))
    ic = an[f'animation.{name}.idle_combat']
    best = None
    head = None
    for ra in itertools.product(range(-20, -81, -10), range(-10, 31, 10)):          # right arm pitch, roll (hand low, front)
        for la in itertools.product(range(-60, -131, -10), range(-50, 1, 10)):       # left arm pitch, roll (hand high)
            for hr in itertools.product(range(-60, 61, 15), range(-60, 61, 30), range(-60, 61, 15)):
                pass
            break
        break
    # analytic-ish search: for each arm pose, find the halberd rotation that puts the haft through the left hand
    cands = []
    for ra in itertools.product(range(-25, -76, -10), range(0, 31, 10)):
        for la in itertools.product(range(-70, -131, -15), range(-45, 1, 15)):
            static = {'right_arm': [ra[0], 0, ra[1]], 'left_arm': [la[0], 0, la[1]], 'right_forearm': [-30, 0, 0], 'left_forearm': [-35, 0, 0]}
            fake = {'animation_length': 1, 'loop': True, 'bones': {k: {'rotation': {'0': v}} for k, v in static.items()}}
            W = C.pose(bones, order, [(fake, 0)], 0)
            lh = C.pts(C.cubes(bones, 'left_hand', W)).mean(0); piv = W['halberd'] @ np.array([-bones['halberd']['pivot'][0], bones['halberd']['pivot'][1], bones['halberd']['pivot'][2], 1])
            dist = np.linalg.norm(lh - piv[:3])
            if not 14 <= dist <= 30: continue
            cands.append((static, lh, piv[:3], dist))
    print('arm candidates', len(cands))
    for static, lh, piv, dist in cands:
        for hr in itertools.product(range(-90, 91, 15), range(-90, 91, 30), range(-90, 91, 15)):
            st = dict(static); st['halberd'] = list(hr)
            fake = {'animation_length': 1, 'loop': True, 'bones': {k: {'rotation': {'0': v}} for k, v in st.items()}}
            W = C.pose(bones, order, [(fake, 0)], 0)
            # haft axis: local +y of halberd in world
            ax = (W['halberd'][:3, :3] @ np.array([0, 1, 0])); ax /= np.linalg.norm(ax)
            off = lh - piv; perp = np.linalg.norm(off - ax * off.dot(ax))
            if perp > 2.2 or off.dot(ax) < 10: continue
            P = C.pts([c for b in wb for c in C.cubes(bones, b, W)], step=1.0)
            n = sum(int(C.inside(P, C.cubes(bones, bb, W), margin=-0.6).sum()) for bb in body)
            if n: continue
            tip = piv + ax * 52; hc = C.pts(C.cubes(bones, 'head', W)).mean(0)
            side = abs(tip[0] - hc[0]); up = tip[1] - hc[1]; front = -tip[2]
            score = -perp * 2 + min(side, 22) + min(up, 20) * 0.6 - abs(ax[2]) * 4
            if up < 0 or side < 12: continue
            if best is None or score > best[0]: best = (score, st, perp, side, up, tip)
    return best

STANCE = {'right_arm': [-45, 0, 30], 'left_arm': [-100, 0, 0], 'right_forearm': [-30, 0, 0], 'left_forearm': [-35, 0, 0], 'halberd': [-15, -60, -45]}

def anchor_actions(ga):
    """Guardian actions start and end in the combat stance, so blending in/out never sweeps the haft through the head."""
    for act in ('attack', 'spin', 'sweep', 'barrier', 'roar'):
        a = ga[f'animation.spire_guardian.{act}']; L = a['animation_length']
        for bn, v in STANCE.items():
            ch = a['bones'].setdefault(bn, {}).setdefault('rotation', {})
            for t in (0, L):
                k = f'{t:.4g}'; ch[k] = {'post': list(v), 'lerp_mode': 'catmullrom'}

def main():
    log = []
    gb, go, ga = C.load('spire_guardian')
    if '--actions' in sys.argv:
        anchor_actions(ga)
        gbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'spire0', 'tasset_front', 'tasset_back', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'cape1']
        for act in ('attack', 'spin', 'sweep', 'barrier', 'roar'):
            fix_action('spire_guardian', ga, gb, go, 'idle_combat', act, 'halberd', gbody, log)
        json.dump({'format_version': '1.8.0', 'animations': ga}, open(f'{C.A}/animations/entity/spire_guardian.animation.json', 'w'), indent=1)
        wb_, wo, wa = C.load('rift_warden')
        wbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'cape1']
        for act in ('slam', 'draw', 'roar'):
            fix_action('rift_warden', wa, wb_, wo, 'idle_combat', act, 'sword', wbody, log)
        json.dump({'format_version': '1.8.0', 'animations': wa}, open(f'{C.A}/animations/entity/rift_warden.animation.json', 'w'), indent=1)
        for l in log: print(l)
        return
    gbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'spire0', 'tasset_front', 'tasset_back', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'cape1']
    best = guardian_stance(ga, gb, go, gbody)
    print('stance', best and {k: v for k, v in best[1].items()}, best and [round(float(x), 1) for x in best[2:5]])
    st = best[1]
    for an_ in ('idle_combat', 'walk_combat'):
        a = ga[f'animation.spire_guardian.{an_}']
        for bn, v in st.items():
            ch = a['bones'].setdefault(bn, {}).setdefault('rotation', {})
            if not ch: ch['0'] = {'post': v, 'lerp_mode': 'catmullrom'}
            # re-centre the existing wave on the new base pose
            vals = [(k, (x['post'] if isinstance(x, dict) else x)) for k, x in ch.items()]
            mean = np.mean([vv for _, vv in vals], axis=0)
            for k, vv in vals:
                nv = fmt(np.array(v) + (np.array(vv) - mean) * (0.35 if bn in ('halberd', 'left_arm', 'right_arm') else 1))
                if isinstance(ch[k], dict): ch[k]['post'] = nv
                else: ch[k] = nv
    for act in ('attack', 'spin', 'sweep', 'barrier', 'roar', 'walk_combat', 'idle_combat'):
        fix_action('spire_guardian', ga, gb, go, 'idle_combat', act, 'halberd', gbody, log)
    json.dump({'format_version': '1.8.0', 'animations': ga}, open(f'{C.A}/animations/entity/spire_guardian.animation.json', 'w'), indent=1)
    wb_, wo, wa = C.load('rift_warden')
    wbody = ['head', 'chest', 'torso', 'hips', 'jaw', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'cape1']
    for act in ('idle_combat', 'walk_combat', 'attack', 'slam', 'lunge', 'draw', 'roar'):
        fix_action('rift_warden', wa, wb_, wo, 'idle_combat', act, 'sword', wbody, log)
    json.dump({'format_version': '1.8.0', 'animations': wa}, open(f'{C.A}/animations/entity/rift_warden.animation.json', 'w'), indent=1)
    for l in log: print(l)

if __name__ == '__main__':
    main()

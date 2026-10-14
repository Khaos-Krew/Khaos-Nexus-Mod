"""Clip check for the 1.1.2 v3 bosses: weapon points inside body cubes, per animation (layered over idle_combat for
actions, as GeckoLib plays them). The sheathed set checks the back sword; drawn anims check the sword in hand.
Usage: python3 clip3.py rift_warden|spire_guardian"""
import sys, clipcheck as C
CAPES = [f'cape{h}{j}' for h in 'RL' for j in range(1, 7)]
BODY = ['head', 'chest', 'torso', 'hips', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'left_arm', 'left_forearm', 'tabard']
SPECS = {
 'rift_warden': [('sword', BODY + CAPES, ['idle_combat', 'walk_combat', 'attack', 'slam', 'lunge', 'roar', ('draw', 0.42, 9), ('sheathe', 0, 0.38)]),
                 ('sword_back', ['head', 'chest', 'right_arm', 'left_arm'] + CAPES, ['idle', 'walk']),
                 ('right_open', ['head', 'chest', 'torso', 'hips', 'right_leg'], ['idle', 'walk', ('draw', 0, 0.4), ('sheathe', 0.42, 9)]),
                 ('left_hand', ['head', 'chest', 'torso', 'hips', 'right_leg', 'left_leg'], ['idle', 'walk', 'draw', 'sheathe', 'roar'])],
 'spire_guardian': [('halberd', BODY + CAPES + ['spire0'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'sweep', 'spin', 'barrier', 'roar'])],
}
name = sys.argv[1]; total = 0
for weapon, body, anims in SPECS[name]:
    win = {a[0]: a[1:] for a in anims if isinstance(a, tuple)}; anims = [a[0] if isinstance(a, tuple) else a for a in anims]
    combos = [(a, [a]) if a in ('idle', 'walk', 'idle_combat', 'walk_combat') else (a, ['idle_combat', a]) for a in anims]
    bones, order, an = C.load(name)
    body = [b for b in body if b in bones]
    res = C.check(name, weapon, body, combos)
    res = [x for x in res if x[1] <= an[f'animation.{name}.{x[0]}']['animation_length'] + 1e-6 and (x[0] not in win or win[x[0]][0] <= x[1] <= win[x[0]][1])]
    total += len(res)
    agg = {}
    for l, t, b, n in res: agg.setdefault((l, b), []).append((t, n))
    for (l, b), v in sorted(agg.items()): print(f'{name} {weapon:10s} {l:12s} {b:12s} frames={len(v):3d} max={max(n for _, n in v)} t={v[0][0]}..{v[-1][0]}')
print('CLEAN' if total == 0 else f'{total} hits')

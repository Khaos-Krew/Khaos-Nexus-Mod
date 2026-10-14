import sys, clipcheck as C
specs = {
 'vaelith': (['right_baton', 'left_baton'], ['head', 'neck', 'chest', 'torso', 'skirt'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'volley', 'sweep', 'barrier', 'roar']),
 'fallen_hierophant': (['staff'], ['head', 'chest', 'torso', 'hips', 'mitre0', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'tabard', 'left_arm', 'left_forearm'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'sweep', 'barrier', 'roar']),
}
total = 0
for name in sys.argv[1:] or specs:
    weapons, body, anims = specs[name]
    combos = [(a, [a]) if a in ('idle', 'walk', 'idle_combat', 'walk_combat') else (a, ['idle_combat', a]) for a in anims]
    for w in weapons:
        res = C.check(name, w, body, combos); total += len(res)
        agg = {}
        for l, t, b, n in res: agg.setdefault((l, b), []).append((t, n))
        for (l, b), v in sorted(agg.items()): print(f'{name} {w:12s} {l:12s} {b:10s} frames={len(v):3d} max={max(n for _, n in v)} t={v[0][0]}..{v[-1][0]}')
print('CLEAN' if total == 0 else f'{total} hits')

import sys, clipcheck as C
specs = {
 'vaelith': (['right_baton', 'left_baton'], ['head', 'neck', 'chest', 'torso', 'skirt'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'volley', 'sweep', 'barrier', 'roar']),
 'fallen_hierophant': (['staff'], ['head', 'chest', 'torso', 'hips', 'mitre0', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'tabard', 'left_arm', 'left_forearm'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'sweep', 'barrier', 'roar']),
 'general_blade': (['staff'], ['head', 'chest', 'torso', 'hips', 'mitre0', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'tabard', 'left_arm', 'left_forearm'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'sweep', 'barrier', 'roar']),
 'general_arcanist': (['staff'], ['head', 'chest', 'torso', 'hips', 'mitre0', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'tabard', 'left_arm', 'left_forearm'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'sweep', 'barrier', 'roar', 'volley']),
 'hollow_king': (['staff'], ['head', 'chest', 'torso', 'hips', 'mitre0', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'tabard', 'left_arm', 'left_forearm'], ['idle', 'walk', 'idle_combat', 'walk_combat', 'attack', 'spin', 'sweep', 'barrier', 'roar', 'volley', 'crownfall']),
 # legs (whole chains), tongue chain, eyes and chains against the chest body
 'gate_mimic': ([f'leg_{s}{p}' for s in 'rl' for p in 'fmb'], ['base'], ['idle', 'walk', 'attack', 'roar', 'death']),
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
# Gate Mimic tongue: checked only while it is scaled in (it is hidden inside the shut maw otherwise)
if 'gate_mimic' in (sys.argv[1:] or specs):
    for a, (t0, t1) in {'roar': (0.76, 1.05), 'attack': (0.18, 0.36), 'death': (0.75, 2.4)}.items():
        res = [x for x in C.check('gate_mimic', 'tongue', ['base'], [(a, [a])]) if t0 <= x[1] <= t1]
        for x in res: print('gate_mimic tongue', x)
        total += len(res)
print('CLEAN' if total == 0 else f'{total} hits')

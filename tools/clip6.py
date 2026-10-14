"""Clip check for the sculpt6 mobs: weapon/hand-held bones vs torso/head/legs, every anim (layered over idle or
idle_combat). Usage: python3 clip6.py [mob...]"""
import sys, clipcheck as C
CORE = ['head', 'chest', 'torso', 'hips', 'right_leg', 'left_leg', 'right_shin', 'left_shin']
SPECS = {'bone_thrall': [('cleaver', CORE)], 'rift_stalker': [('right_blade', CORE), ('left_blade', CORE)],
         'rift_ghoul': [('right_claw0', CORE), ('left_claw0', CORE)], 'void_caster': [('orb', CORE)],
         'rift_brute': [('right_hand', CORE), ('left_hand', CORE)],
         'general_blade': [('staff', CORE)], 'general_arcanist': [('staff', CORE)], 'gate_mimic': [('tongue', ['base', 'lid'])]}
total = 0
for name in sys.argv[1:] or SPECS:
    bones, order, an = C.load(name)
    base = 'idle_combat' if f'animation.{name}.idle_combat' in an else 'idle'
    anims = [k.split('.')[-1] for k in an if '.freeze_' not in k and not k.endswith(('.death', '.dormant'))]
    combos = [(a, [a]) if a in ('idle', 'walk', 'idle_combat', 'walk_combat') else (a, [base, a]) for a in anims]
    n = 0
    for w, body in SPECS.get(name, []):
        res = C.check(name, w, [b for b in body if b in bones], combos)
        agg = {}
        for l, t, b, k in res: agg.setdefault((l, b), []).append(k)
        for (l, b), v in sorted(agg.items()): print(f'{name} {w} {l} {b} frames={len(v)} max={max(v)}')
        n += len(res)
    print(name, 'CLEAN' if n == 0 else f'{n} hits'); total += n

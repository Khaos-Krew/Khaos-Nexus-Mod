"""1.2 armor sets (GeckoLib armor): the Glass Choir (from Vaelith) and the Profane Vestments (from the Fallen Hierophant).
Rig space is the player model (y-up, feet at 0, right side at -x); G.geo mirrors x into Bedrock space.
Pieces sit 0.6-1.2 px off the body so nothing z-fights the skin; painted with the bosses' own materials (same Painter,
`who` set to the source boss) so the sets read as trophies of those fights. Run: python3 armor_sets.py"""
import math, json, os, shutil
import gen_geo_assets as G
import rigs_saga2 as S
Rig = G.Rig
taper, shard = S.taper, S.shard

def bones(r):
    r.bone('armorHead', None, (0, 24, 0)); r.bone('armorBody', None, (0, 24, 0))
    r.bone('armorRightArm', None, (-5, 22, 0)); r.bone('armorLeftArm', None, (5, 22, 0))
    r.bone('armorRightLeg', None, (-1.9, 12, 0)); r.bone('armorLeftLeg', None, (1.9, 12, 0))
    r.bone('armorRightBoot', None, (-1.9, 12, 0)); r.bone('armorLeftBoot', None, (1.9, 12, 0))

SIDES = (('Right', -1), ('Left', 1))

def choir():
    r = Rig(); B = r.box; bones(r)
    # --- helm: pale steel skull-cap, a smooth singing mask, glass fins and a crown of crystal spines
    B('armorHead', (0, 28.6, 0.6), (9.4, 9.0, 8.4), 'pauldron'); B('armorHead', (0, 33.3, 0.2), (8.6, 0.8, 8.4), 'pauldron')
    B('armorHead', (0, 27.8, -4.4), (8.8, 7.4, 0.8), 'mask'); B('armorHead', (0, 28.9, -4.85), (7.0, 0.8, 0.2), 'void'); B('armorHead', (0, 27.2, -4.9), (0.8, 2.6, 0.2), 'glass')
    for s in (-1, 1):
        B('armorHead', (s * 4.95, 28.2, 0), (0.6, 6.4, 7.0), 'belt'); shard(r, 'armorHead', (s * 5.0, 30.4, 1.6), 5, 1.4, (-20, 0, s * -55), 'glass')
    for x, h in ((-3.0, 3.0), (-1.5, 4.4), (0, 5.6), (1.5, 4.4), (3.0, 3.0)):
        shard(r, 'armorHead', (x, 33.6, -3.0 + abs(x) * 0.3), h, 1.1, (-12, 0, -x * 5), 'glass')
    B('armorHead', (0, 33.0, -4.4), (8.0, 1.0, 1.0), 'crown')
    # --- chest: layered breastplate, glass heart crystal, collar ring, two glass fins on the back
    B('armorBody', (0, 18.2, 0), (9.2, 12.4, 5.2), 'breastplate')
    B('armorBody', (0, 23.4, 0), (10.0, 1.6, 6.0), 'pauldron')
    for j in range(3): B('armorBody', (0, 14.0 + j * 1.6, -2.75), (8.6 - j * 0.4, 1.2, 0.5), 'belt')
    B('armorBody', (0, 19.8, -2.9), (3.0, 3.0, 0.8), 'glass', rot=(0, 0, 45)); B('armorBody', (0, 19.8, -3.15), (1.4, 1.4, 0.4), 'core', rot=(0, 0, 45))
    for s in (-1, 1): shard(r, 'armorBody', (s * 2.2, 20.5, 2.7), 7, 1.6, (34, 0, s * 24), 'glass')
    B('armorBody', (0, 12.6, 0), (9.4, 1.4, 5.4), 'belt')
    # --- arms: three-lame pauldrons with a glass spike, glass-trimmed vambraces
    for side, s in SIDES:
        bn = f'armor{side}Arm'; x = s * 6
        for j in range(3): B(bn, (x + s * 0.3, 23.2 - j * 1.5, 0), (5.4 - j * 0.3, 1.6, 5.6 - j * 0.3), 'pauldron', rot=(0, 0, s * (10 + 6 * j)))
        shard(r, bn, (x + s * 1.6, 24, 0), 4.2, 1.2, (0, 0, s * -30), 'glass')
        B(bn, (x, 14.4, 0), (4.8, 4.0, 4.8), 'gauntlet'); B(bn, (x, 16.6, 0), (5.0, 0.6, 5.0), 'crown')
        B(bn, (x + s * 2.5, 14.6, 0), (0.5, 3.4, 2.2), 'glass')
    # --- legs: tassets + knee cops
    for side, s in SIDES:
        bn = f'armor{side}Leg'; x = s * 1.9
        B(bn, (x, 9.4, 0), (4.6, 5.2, 4.6), 'breastplate'); B(bn, (x + s * 0.5, 10.4, -0.2), (4.0, 3.6, 5.0), 'belt', rot=(0, 0, s * 6))
        B(bn, (x, 6.2, -2.4), (3.4, 2.6, 0.8), 'pauldron'); B(bn, (x, 6.2, -2.9), (1.2, 1.2, 0.3), 'glass', rot=(0, 0, 45))
        B(bn, (x, 4.6, 0), (4.5, 3.4, 4.5), 'gauntlet')
    # --- boots: pointed sabatons with a glass heel spur
    for side, s in SIDES:
        bn = f'armor{side}Boot'; x = s * 1.9
        B(bn, (x, 1.6, -0.3), (4.8, 3.4, 5.2), 'gauntlet'); B(bn, (x, 0.6, -2.9), (3.6, 1.2, 1.4), 'pauldron'); B(bn, (x, 3.4, 0), (5.0, 0.6, 5.0), 'crown')
        shard(r, bn, (x, 1.8, 2.6), 3.0, 0.9, (-70, 0, 0), 'glass')
    return r

def vestments():
    r = Rig(); B = r.box; bones(r)
    # --- hood with a small two-leaf mitre; the face stays open
    B('armorHead', (0, 33.0, 0.4), (9.6, 1.4, 9.4), 'hood'); B('armorHead', (0, 28.4, 4.7), (9.6, 9.0, 0.8), 'hood')
    for s in (-1, 1): B('armorHead', (s * 4.7, 28.4, 0.4), (0.8, 9.0, 9.4), 'hood'); B('armorHead', (s * 4.0, 28.0, -4.4), (1.6, 8.4, 0.8), 'hood')
    B('armorHead', (0, 32.6, -4.3), (9.6, 1.6, 1.0), 'collar')
    for j in range(3):
        w = 6.4 - j * 1.4; y = 34.6 + j * 2.0
        B('armorHead', (0, y, -1.4), (w, 2.2, 0.9), 'mitre', rot=(-6, 0, 0)); B('armorHead', (0, y, 2.0), (w, 2.2, 0.9), 'mitre', rot=(6, 0, 0))
        B('armorHead', (0, y, 0.3), (w - 0.6, 2.2, 2.6), 'mitre')
    B('armorHead', (0, 34.0, 0.3), (7.0, 1.0, 4.4), 'crown'); B('armorHead', (0, 36.2, -1.95), (1.6, 1.6, 0.2), 'rune', rot=(0, 0, 45))
    # --- robe chest: velvet cuirass, gold stole, mantle collar, sigil, short tattered back cape
    B('armorBody', (0, 18.2, 0), (9.0, 12.4, 5.0), 'robe')
    B('armorBody', (0, 23.6, 0), (10.6, 1.8, 6.2), 'cape'); B('armorBody', (0, 22.2, 0), (11.0, 1.4, 6.6), 'capehem')
    for s in (-1, 1): B('armorBody', (s * 2.0, 17.4, -2.65), (1.6, 11.2, 0.4), 'collar')
    B('armorBody', (0, 19.6, -2.75), (3.0, 3.0, 0.5), 'guard', rot=(0, 0, 45)); B('armorBody', (0, 19.6, -3.05), (1.8, 1.8, 0.3), 'sigil', rot=(0, 0, 45))
    B('armorBody', (0, 12.8, 0), (9.4, 1.6, 5.4), 'belt')
    B('armorBody', (0, 16.0, 2.9), (8.0, 14.0, 0.6), 'capehem')
    # --- arms: flaring bell sleeves with gold cuffs
    for side, s in SIDES:
        bn = f'armor{side}Arm'; x = s * 6
        B(bn, (x, 22.6, 0), (5.0, 3.2, 5.2), 'cape'); B(bn, (x + s * 0.2, 24.0, 0), (5.4, 0.8, 5.6), 'collar')
        taper(r, bn, x, 0, 20.6, 13.6, 4.8, 5.8, 4.8, 5.8, 3, 'sleeve')
        B(bn, (x, 13.2, 0), (6.0, 0.8, 6.0), 'collar')
    # --- legs: front/back robe panels with torn hems that stride with each leg
    for side, s in SIDES:
        bn = f'armor{side}Leg'; x = s * 1.9
        B(bn, (x, 8.4, 0), (4.6, 7.4, 4.6), 'robe')
        B(bn, (x, 4.2, -2.55), (4.4, 7.0, 0.5), 'capehem', rot=(-4, 0, 0), pivot=(x, 8, -2.55))
        B(bn, (x, 4.2, 2.55), (4.4, 7.0, 0.5), 'capehem', rot=(4, 0, 0), pivot=(x, 8, 2.55))
        B(bn, (x, 11.4, 0), (4.8, 1.0, 4.8), 'collar')
    # --- boots: wrapped feet with gilded toe caps
    for side, s in SIDES:
        bn = f'armor{side}Boot'; x = s * 1.9
        B(bn, (x, 1.8, 0), (4.6, 3.6, 4.8), 'robe'); B(bn, (x, 0.6, -2.0), (4.4, 1.2, 1.4), 'guard'); B(bn, (x, 3.8, 0), (4.9, 0.6, 5.0), 'collar')
    return r

SETS = {
 'choir_armor': (choir, dict(S.PAL['vaelith'], who='vaelith', k=4, ts=[256, 256])),
 'vestment_armor': (vestments, dict(S.PAL['fallen_hierophant'], who='fallen_hierophant', k=4, ts=[256, 256])),
}

def build(name):
    fn, m = SETS[name]; rg = fn()
    uvs = G.pack(rg, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * 4, ts=[m['ts'][0] * 4, 512]); huvs = G.pack(rg, hm['k'], hm['ts']); pt = S.Painter(name, hm)
    cs = [c for b in rg.order for c in rg.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    S.downsample(name, m, rg, pt, m['k'], huvs, uvs); G.geo(name, rg, m['k'], m['ts'], uvs)
    res = G.RES
    os.makedirs(os.path.join(res, 'geo', 'item', 'armor'), exist_ok=True); os.makedirs(os.path.join(res, 'textures', 'item', 'armor'), exist_ok=True)
    os.makedirs(os.path.join(res, 'animations', 'item', 'armor'), exist_ok=True)
    shutil.move(os.path.join(res, 'geo', 'entity', f'{name}.geo.json'), os.path.join(res, 'geo', 'item', 'armor', f'{name}.geo.json'))
    shutil.move(os.path.join(res, 'textures', 'entity', f'{name}.png'), os.path.join(res, 'textures', 'item', 'armor', f'{name}.png'))
    shutil.move(os.path.join(res, 'textures', 'entity', f'{name}_glow.png'), os.path.join(res, 'textures', 'item', 'armor', f'{name}_glowmask.png'))
    json.dump({'format_version': '1.8.0', 'animations': {f'animation.{name}.idle': {'loop': True, 'animation_length': 1.0, 'bones': {}}}},
              open(os.path.join(res, 'animations', 'item', 'armor', f'{name}.animation.json'), 'w'), indent=1)
    print(name, len(cs), 'cubes')

if __name__ == '__main__':
    for n in SETS: build(n)

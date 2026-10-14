"""v6 sculpted pass for every non-boss mob (1.1 rift/gate mobs, minions), the Crownless Generals and the Gate Mimic.
Bone names and animations are untouched, so grips/attacks keep working; the geometry is re-sculpted on top:
  * bevel: every solid box becomes a rounded octagonal prism (two crossed boxes + a 45-degree core for big ones)
  * lamellae: armour cubes get an overlapping lower plate band and a raised rim (layered plates)
  * per-mob detail (faces, helms, trims) in DETAIL
and painted with boss3.Painter3 (palette metals, rim light, etched/emissive runes) over each mob's own painter.
Usage: python3 sculpt6.py [mob ...]"""
import math, os, sys, copy
import gen_geo_assets as G
import rigs_saga2 as S
import boss3 as B3

NOBEVEL = {'eye', 'rune', 'core', 'smoke', 'glass', 'veil', 'veilhem', 'void', 'sigil', 'claw', 'spike', 'blade', 'edge', 'chain'}
ARMOUR = {'plate', 'pauldron', 'breastplate', 'guard', 'gauntlet', 'helm', 'visorplate', 'greave', 'belt'}

def bevel(r, minw=1.9, bev=0.2):
    for bn in r.order:
        out = []
        for c in r.bones[bn]['cubes']:
            w, h, d = c['size']; m = min(w, d)
            if c['role'] in NOBEVEL or m < minw or h < 0.8 or c.get('_bev'):
                out.append(c); continue
            b = m * bev; o = c['origin']
            c1 = copy.deepcopy(c); c1['size'] = [w, h, d - 2 * b]; c1['origin'] = [o[0], o[1], o[2] + b]
            c2 = copy.deepcopy(c); c2['size'] = [w - 2 * b, h, d]; c2['origin'] = [o[0] + b, o[1], o[2]]
            out += [c1, c2]
            if m >= 3.6 and not c.get('rot'):    # 45-degree core rounds the silhouette further
                k = m * 0.8; cx, cy, cz = o[0] + w / 2, o[1] + h / 2, o[2] + d / 2
                sw, sd = k * (w / m) ** 0.25, k * (d / m) ** 0.25
                c3 = dict(origin=[cx - sw / 2, o[1] + 0.01, cz - sd / 2], size=[sw, h - 0.02, sd], role=c['role'], inflate=c.get('inflate', 0.0),
                          rot=[0, 45, 0], pivot=[cx, cy, cz])
                out.append(c3)
            for q in out[-3:]: q['_bev'] = True
        r.bones[bn]['cubes'] = out

def lamellae(r):
    """Armour cubes tall enough get an overlapping lower band (slightly proud) and a raised rim along the top edge."""
    for bn in r.order:
        add = []
        for c in r.bones[bn]['cubes']:
            if c['role'] not in ARMOUR or c.get('_lam') or c.get('_bev') is None: continue
            w, h, d = c['size']
            if h < 3.0 or min(w, d) < 2.4 or c.get('rot'): continue
            o = c['origin']; c['_lam'] = True
            add.append(dict(origin=[o[0] - 0.25, o[1], o[2] - 0.25], size=[w + 0.5, h * 0.36, d + 0.5], role=c['role'], inflate=0.0, _lam=True))
            add.append(dict(origin=[o[0] - 0.12, o[1] + h - 0.6, o[2] - 0.12], size=[w + 0.24, 0.6, d + 0.24], role='crown', inflate=0.0, _lam=True))
        r.bones[bn]['cubes'] += add

def head_box(r, bone='head'):
    cs = [c for c in r.bones.get(bone, {}).get('cubes', []) if not c.get('rot')]
    return max(cs, key=lambda c: c['size'][0] * c['size'][1] * c['size'][2]) if cs else None

def face(r, bone='head', eyes=True, brow='crown', tusks=None, crest=None):
    """Sculpted face on the head's main block: brow ridge, cheek plates, glowing eye sockets, optional tusks/crest."""
    hb = head_box(r, bone)
    if not hb: return
    (x0, y0, z0), (w, h, d) = hb['origin'], hb['size']; cx = x0 + w / 2; fz = z0
    B = r.box
    B(bone, (cx, y0 + h * 0.66, fz - 0.35), (w * 0.92, h * 0.12, 0.7), brow)
    for s in (-1, 1):
        B(bone, (cx + s * w * 0.33, y0 + h * 0.34, fz - 0.25), (w * 0.26, h * 0.22, 0.5), 'visorplate', rot=(0, 0, s * 8))
        if eyes:
            B(bone, (cx + s * w * 0.2, y0 + h * 0.52, fz - 0.12), (w * 0.16, h * 0.08, 0.3), 'eye')
        if tusks:
            B(bone, (cx + s * w * 0.28, y0 + 0.4, fz - 0.6), (0.7, 2.2, 0.7), tusks, rot=(-25, 0, s * -18))
    if crest:
        for i in range(4):
            B(bone, (cx, y0 + h + 0.4 + i * 0.1, z0 + d * (0.2 + i * 0.2)), (0.8, 1.6 - i * 0.25, d * 0.18), crest, rot=(-15, 0, 0))

def has_eye(r, bone='head'): return any(c['role'] == 'eye' for c in r.bones.get(bone, {}).get('cubes', []))

# ---- per-mob detail -----------------------------------------------------------------------------------------------
def d_thrall(r):
    face(r, eyes=not has_eye(r), brow='trim', tusks=None, crest='spike')
def d_brute(r):
    face(r, eyes=not has_eye(r), tusks='claw', crest='spike')
def d_familiar(r):
    face(r, eyes=not has_eye(r), brow='spike')
def d_ghoul(r):
    face(r, eyes=not has_eye(r), brow='rib', tusks='claw')
def d_stalker(r):
    pass   # the mask is already carved (rigs_v5); bevel + palette only
def d_caster(r):
    face(r, eyes=not has_eye(r), brow='crown')
def d_general(r):
    face(r, 'head', eyes=not has_eye(r), brow='crown', crest='crown')
def d_mimic(r): pass

def metals(base, accent, dark):
    hx = G.hx; b, a, d = hx(base), hx(accent), hx(dark)
    lift = lambda c, k: tuple(min(255, int(v * k)) for v in c)
    return {'plate': lift(b, 1.1), 'pauldron': lift(a, 1.0), 'breastplate': lift(a, 1.1), 'guard': lift(a, 0.9), 'gauntlet': lift(b, 0.95),
            'helm': lift(a, 1.05), 'visorplate': lift(a, 1.2), 'greave': lift(b, 1.0), 'belt': lift(d, 2.2)}

def mat(m):
    P = m['pal']; glow = G.hx(m['glow']); eye = G.hx(m.get('eye', m['glow'])); core = G.hx(m.get('core', m['glow']))
    trim = G.hx(m.get('trim') or '#B8A06A')
    return dict(metal=metals(P['base'], P['accent'], P['dark']), spec=(232, 226, 240), rim=G.mul(trim, 1.1),
                trim=(G.mul(trim, 0.7), trim, (255, 250, 230)), rune=G.mix(glow, (255, 255, 255), 0.25),
                trimmed=('pauldron', 'breastplate', 'helm', 'visorplate'), etched=('pauldron', 'breastplate', 'helm', 'plate', 'greave'),
                glowing=('pauldron', 'breastplate', 'helm'), veined=('plate',), vein=glow,
                eye=(G.mul(eye, 0.7), G.mix(eye, (255, 255, 255), 0.6)), core=(G.mul(core, 0.8), G.mix(core, (255, 255, 255), 0.6)),
                edge=((170, 170, 186), (240, 240, 250)), horn=((40, 30, 44), (120, 100, 120)))

DETAIL = {'bone_thrall': d_thrall, 'rift_brute': d_brute, 'rift_familiar': d_familiar, 'rift_ghoul': d_ghoul, 'rift_stalker': d_stalker,
          'void_caster': d_caster, 'general_blade': d_general, 'general_arcanist': d_general, 'gate_mimic': d_mimic}

def sculpt(name, r):
    DETAIL[name](r); bevel(r); lamellae(r)
    for bn in r.order:
        for c in r.bones[bn]['cubes']:
            c.pop('_bev', None); c.pop('_lam', None)

def paint(name, m, rig, base_cls, ds, hi=4, keep_h=False):
    PC = type('P6', (B3.Painter3, base_cls), {}) if base_cls is not B3.Painter3 else B3.Painter3
    m = dict(m, ts=list(m['ts'])); m.setdefault('who', name); m.setdefault('mat', mat(m)); uvs = G.pack(rig, m['k'], m['ts'])
    hm = dict(m, k=m['k'] * hi, ts=[m['ts'][0] * hi, m['ts'][1] * hi if keep_h else 256]); huvs = G.pack(rig, hm['k'], hm['ts']); pt = PC(name, hm)
    cs = [c for b in rig.order for c in rig.bones[b]['cubes']]
    for uv, c in zip(huvs, cs): pt.cube(uv, c)
    ds(name, m, rig, pt, m['k'], huvs, uvs); G.geo(name, rig, m['k'], m['ts'], uvs)
    return len(cs), m['ts']

V1 = ('bone_thrall', 'rift_brute', 'rift_familiar', 'rift_ghoul', 'rift_stalker', 'void_caster')

def build(name):
    if name in V1:
        import rigs_v4, rigs_minions, rigs_v5, freeze_mobs
        if not getattr(G, '_v6', False):
            rigs_v4.install(G); rigs_minions.install(G); rigs_v5.install(G); G._v6 = True
        mob = G.MOBS[name]; rig, d = mob['rig'](); sculpt(name, rig)
        m = dict(mob, k=3)
        n, ts = paint(name, m, rig, G.Painter, G.downsample)
        G.anim_set(name, d, rig); freeze_mobs.run(name)
    elif name.startswith('general_'):
        import rigs_generals as RG
        S.PAL.update(RG.PAL); rig = {'general_blade': RG.rig_blade, 'general_arcanist': RG.rig_arcanist}[name](); sculpt(name, rig)
        m = dict(RG.PAL[name], k=3); n, ts = paint(name, m, rig, RG.Painter, S.downsample)
        an = S.hierophant_anims()
        if name == 'general_arcanist': RG.arcanist_extra(an)
        G.write_anims(name, an); S.bake_freezes(name)
    elif name == 'gate_mimic':
        import rig_mimic as RM
        S.PAL[name] = RM.PAL; rig = RM.rig(); sculpt(name, rig)
        m = dict(RM.PAL, k=3); n, ts = paint(name, m, rig, RM.Painter, S.downsample, keep_h=True)
        G.write_anims(name, RM.anims())
        keep = dict(S.FREEZE); S.FREEZE['attack'] = 0.28; S.FREEZE['attack_windup'] = ('attack', 0.18)
        S.bake_freezes(name); S.FREEZE.clear(); S.FREEZE.update(keep)
        g = G.out('textures', 'entity', 'gate_mimic_glow_rank.png')
        if os.path.exists(g): os.remove(g)
    print(name, len(rig.order), 'bones', n, 'cubes', ts)

if __name__ == '__main__':
    for nm in sys.argv[1:] or list(DETAIL): build(nm)

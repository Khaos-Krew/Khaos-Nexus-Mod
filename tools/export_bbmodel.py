"""Writes Blockbench (.bbmodel, Bedrock format) sources from the rigs in gen_geo_assets.py.
Usage: export_bbmodel.py NAME [stage]   stages: body, armor, helm, cape, sword, texture (default: full)."""
import base64, json, os, sys, uuid
sys.path.insert(0, os.path.dirname(__file__))
import gen_geo_assets as G
STAGES = ['body', 'armor', 'helm', 'cape', 'sword', 'texture']
def stage_of(bone, role):
    if bone in ('sword', 'halberd') or 'blade' in bone: return 'sword'
    if bone.startswith('cape'): return 'cape'
    if bone in ('head', 'jaw') or bone.startswith(('spire', 'hood')): return 'helm'
    if role in ('limb', 'chest', 'plate', 'belt', 'fist', 'body', 'skull', 'foot', 'robe'): return 'body'
    return 'armor'
def export(name, stage='texture'):
    m = G.MOBS[name]; rig, d = m['rig'](); k = m['k']; uvs = G.pack(rig, k, m['ts']); W, H = m['ts']
    lim = STAGES.index(stage); it = iter(uvs); elements = []; groups = {}
    for bn in rig.order:
        b = rig.bones[bn]
        rot = [b['rotation'][0], -b['rotation'][1], b['rotation'][2]]
        groups[bn] = {'name': bn, 'origin': b['pivot'], 'rotation': rot, 'uuid': str(uuid.uuid4()), 'export': True, 'isOpen': False, 'children': []}
        for c in b['cubes']:
            u, v = next(it)
            if STAGES.index(stage_of(bn, c['role'])) > lim: continue
            w, h, dd = G.cdims(c, k); F = G.faces(u, v, w, h, dd)
            faces = {fn: ({'uv': [f[0], f[1], f[0] + f[2], f[1] + f[3]], 'texture': 0} if lim >= 5 else {'uv': [f[0], f[1], f[0] + f[2], f[1] + f[3]]}) for fn, f in F.items()}
            o = c['origin']; s = c['size']
            e = {'name': c['role'], 'type': 'cube', 'box_uv': False, 'uuid': str(uuid.uuid4()), 'inflate': c['inflate'],
                 'from': o, 'to': [o[0] + s[0], o[1] + s[1], o[2] + s[2]], 'origin': c.get('pivot', b['pivot']),
                 'rotation': [c['rot'][0], -c['rot'][1], c['rot'][2]] if c.get('rot') else [0, 0, 0], 'faces': faces, 'color': len(elements) % 8}
            elements.append(e); groups[bn]['children'].append(e['uuid'])
    roots = []
    for bn in rig.order:
        p = rig.bones[bn]['parent']
        (groups[p]['children'] if p else roots).append(groups[bn])
    tex = []
    if lim >= 5:
        path = G.out('textures', 'entity', f'{name}.png')
        tex.append({'name': f'{name}.png', 'id': '0', 'width': W, 'height': H, 'uv_width': W, 'uv_height': H, 'render_mode': 'default',
                    'uuid': str(uuid.uuid4()), 'source': 'data:image/png;base64,' + base64.b64encode(open(path, 'rb').read()).decode()})
    bb = {'meta': {'format_version': '4.10', 'model_format': 'bedrock', 'box_uv': False}, 'name': name,
          'model_identifier': name, 'visible_box': [6, 7, 2.5], 'resolution': {'width': W, 'height': H},
          'elements': elements, 'outliner': roots, 'textures': tex}
    dst = os.path.join(os.path.dirname(__file__), '..', 'art', 'blockbench', f'{name}.bbmodel')
    json.dump(bb, open(dst, 'w')); return dst, len(elements)
if __name__ == '__main__':
    print(*export(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else 'texture'))

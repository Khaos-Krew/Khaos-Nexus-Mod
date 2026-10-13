"""Rift dungeon jigsaw pieces: writes structure templates (.nbt) and template pools.

Run from the repo root: python3 tools/gen_dungeon_pieces.py
Every doorway is a jigsaw named and targeting khaosnexus:door. Markers are vanilla light blocks
the game replaces after placement: level 1 mob, 2 loot chest, 3 boss, 4 player spawn.
Pools live in data/khaosnexus/worldgen/template_pool/dungeon/<tier>/ so datapacks can add pieces.
"""
import gzip, io, json, os, random, struct

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'khaosnexus')
TPL = os.path.join(ROOT, 'structure', 'dungeon')
POOL = os.path.join(ROOT, 'worldgen', 'template_pool', 'dungeon')

# --- minimal NBT writer -------------------------------------------------------------------
class L(list):  # typed list: L(tag, items)
    def __init__(self, tag, items): super().__init__(items); self.tag = tag
def I(v): return ('i', v)
def _w(out, tag, v):
    if tag == 'c':
        for k, (t, x) in v.items():
            out.write(bytes([TAGS[t]])); _s(out, k); _w(out, t, x)
        out.write(b'\0')
    elif tag == 'l':
        out.write(bytes([TAGS[v.tag]])); out.write(struct.pack('>i', len(v)))
        for x in v: _w(out, v.tag, x)
    elif tag == 'i': out.write(struct.pack('>i', v))
    elif tag == 's': _s(out, v)
    elif tag == 'b': out.write(struct.pack('>b', v))
def _s(out, s):
    b = s.encode(); out.write(struct.pack('>H', len(b))); out.write(b)
TAGS = {'b': 1, 'i': 3, 's': 8, 'l': 9, 'c': 10}
def comp(**kw):  # kw values are (tag, value)
    return kw

# --- piece builder -----------------------------------------------------------------------
FACING = {'n': 'north', 's': 'south', 'w': 'west', 'e': 'east'}

class Piece:
    def __init__(self, name, w, h, d, seed):
        self.name, self.w, self.h, self.d = name, w, h, d
        self.b = {}
        self.nbt = {}
        self.r = random.Random(seed)
    def set(self, x, y, z, state, nbt=None):
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            self.b[(x, y, z)] = state
            if nbt: self.nbt[(x, y, z)] = nbt
            else: self.nbt.pop((x, y, z), None)
    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, state() if callable(state) else state)
    def shell(self):
        r = self.r
        wall = lambda: r.choice(['minecraft:polished_blackstone_bricks'] * 6 + ['minecraft:cracked_polished_blackstone_bricks', 'minecraft:gilded_blackstone'] * 1)
        floor = lambda: r.choice(['minecraft:deepslate_tiles'] * 7 + ['minecraft:cracked_deepslate_tiles', 'minecraft:crying_obsidian'])
        self.fill(0, 0, 0, self.w - 1, self.h - 1, self.d - 1, wall)
        self.fill(1, 1, 1, self.w - 2, self.h - 2, self.d - 2, 'minecraft:air')
        self.fill(1, 0, 1, self.w - 2, 0, self.d - 2, floor)
        self.fill(1, self.h - 1, 1, self.w - 2, self.h - 1, self.d - 2, 'minecraft:deepslate_bricks')
        for x, z in [(1, 1), (1, self.d - 2), (self.w - 2, 1), (self.w - 2, self.d - 2)]:
            self.fill(x, 1, z, x, self.h - 2, z, 'minecraft:obsidian')
        # rift veins along the wall base
        for _ in range((self.w + self.d) // 3):
            x, z = r.randrange(self.w), r.randrange(self.d)
            if self.b.get((x, 1, z), '').startswith('minecraft:polished'):
                self.set(x, 1, z, 'minecraft:amethyst_block')
    def door(self, side, pool):
        cx, cz = self.w // 2, self.d // 2
        x, z = {'n': (cx, 0), 's': (cx, self.d - 1), 'w': (0, cz), 'e': (self.w - 1, cz)}[side]
        for dx in (-1, 0, 1):
            for y in (1, 2, 3):
                if side in 'ns': self.set(x + dx, y, z, 'minecraft:air')
                else: self.set(x, y, z + dx, 'minecraft:air')
        self.set(x, 1, z, 'minecraft:jigsaw[orientation=%s_up]' % FACING[side], comp(
            id=('s', 'minecraft:jigsaw'), name=('s', 'khaosnexus:door'), target=('s', 'khaosnexus:door'),
            pool=('s', pool), final_state=('s', 'minecraft:air'), joint=('s', 'rollable'),
            placement_priority=('i', 0), selection_priority=('i', 0)))
        # frame the doorway with crying obsidian so pieces read as one dungeon
        for dy in (1, 2, 3, 4):
            for off in (-2, 2):
                p = (x + off, dy, z) if side in 'ns' else (x, dy, z + off)
                if p in self.b and dy < self.h - 1: self.set(*p, 'minecraft:crying_obsidian')
    def marker(self, x, y, z, level):
        self.set(x, y, z, 'minecraft:light[level=%d]' % level)
    def lantern(self, x, z):
        self.set(x, self.h - 2, z, 'minecraft:soul_lantern[hanging=true]')
    def save(self):
        palette, idx, blocks = [], {}, []
        for pos in sorted(self.b):
            st = self.b[pos]
            if st not in idx:
                idx[st] = len(palette)
                name, _, props = st.partition('[')
                entry = {'Name': ('s', name)}
                if props:
                    entry['Properties'] = ('c', {k: ('s', v) for k, v in (p.split('=') for p in props[:-1].split(','))})
                palette.append(entry)
            blk = {'pos': ('l', L('i', list(pos))), 'state': ('i', idx[st])}
            if pos in self.nbt: blk['nbt'] = ('c', self.nbt[pos])
            blocks.append(blk)
        root = {'DataVersion': ('i', 3955), 'size': ('l', L('i', [self.w, self.h, self.d])),
                'palette': ('l', L('c', palette)), 'blocks': ('l', L('c', blocks)), 'entities': ('l', L('c', []))}
        buf = io.BytesIO(); buf.write(b'\x0a'); _s(buf, ''); _w(buf, 'c', root)
        path = os.path.join(TPL, self.name + '.nbt'); os.makedirs(os.path.dirname(path), exist_ok=True)
        with gzip.open(path, 'wb') as f: f.write(buf.getvalue())

# --- pieces ------------------------------------------------------------------------------
def hall(tier, name, w, d, doors, seed):
    p = Piece(f'{tier}/{name}', w, 6, d, seed); p.shell()
    for s in doors: p.door(s, f'khaosnexus:dungeon/{tier}/rooms')
    p.lantern(w // 2, d // 2)
    return p

def combat(tier, name, doors, seed):
    p = Piece(f'{tier}/{name}', 15, 8, 15, seed); p.shell()
    for s in doors: p.door(s, f'khaosnexus:dungeon/{tier}/halls')
    p.fill(6, 1, 6, 8, 2, 8, 'minecraft:polished_blackstone_bricks')
    p.fill(7, 3, 7, 7, 5, 7, 'minecraft:crying_obsidian')
    p.set(7, 6, 7, 'minecraft:purple_stained_glass')
    for x, z in [(3, 3), (11, 3), (3, 11), (11, 11)]:
        p.marker(x, 1, z, 1); p.lantern(x, z)
    p.marker(7, 3, 4, 1) if False else None
    return p

def trap(tier, seed):
    p = Piece(f'{tier}/trap_hall', 7, 6, 15, seed); p.shell()
    for s in 'ns': p.door(s, f'khaosnexus:dungeon/{tier}/halls')
    arrows = comp(Items=('l', L('c', [comp(Slot=('b', i), id=('s', 'minecraft:arrow'), count=('i', 16)) for i in range(4)])), id=('s', 'minecraft:dispenser'))
    for z in range(3, 12, 2):
        p.set(3, 0, z, 'minecraft:dispenser[facing=up,triggered=false]', arrows)
        p.set(3, 1, z, 'minecraft:polished_blackstone_pressure_plate[powered=false]')
    for z in range(2, 13):
        p.set(1, 0, z, 'minecraft:magma_block'); p.set(5, 0, z, 'minecraft:magma_block')
    p.marker(3, 1, 13, 1)
    p.lantern(3, 7)
    return p

def loot(tier, seed):
    p = Piece(f'{tier}/loot_vault', 9, 6, 9, seed); p.shell()
    p.door('s', f'khaosnexus:dungeon/{tier}/halls')
    p.fill(3, 0, 3, 5, 0, 5, 'minecraft:amethyst_block')
    p.marker(4, 1, 4, 2)
    for x, z in [(2, 2), (6, 2)]: p.set(x, 1, z, 'minecraft:amethyst_cluster[facing=up,waterlogged=false]')
    p.lantern(4, 4)
    return p

def boss(tier, seed):
    p = Piece(f'{tier}/boss_chamber', 23, 12, 23, seed); p.shell()
    p.door('s', f'khaosnexus:dungeon/{tier}/halls')
    for x in range(1, 22):
        for z in range(1, 22):
            d = ((x - 11) ** 2 + (z - 11) ** 2) ** 0.5
            if 7.5 <= d < 8.5: p.set(x, 0, z, 'minecraft:crying_obsidian')
            elif d < 2.5: p.set(x, 0, z, 'minecraft:obsidian')
    for x, z in [(4, 4), (18, 4), (4, 18), (18, 18), (11, 3), (3, 11), (19, 11)]:
        p.fill(x, 1, z, x, 10, z, 'minecraft:obsidian')
        p.set(x, 5, z, 'minecraft:purple_stained_glass'); p.set(x, 6, z, 'minecraft:amethyst_block')
    for x, z in [(7, 7), (15, 7), (7, 15), (15, 15)]: p.lantern(x, z)
    p.marker(11, 1, 11, 3)
    return p

def entrance(tier, seed):
    p = Piece(f'{tier}/entrance', 11, 7, 11, seed); p.shell()
    p.door('n', f'khaosnexus:dungeon/{tier}/halls')
    p.fill(4, 0, 6, 6, 0, 8, 'minecraft:crying_obsidian')
    p.marker(5, 1, 7, 4)
    p.lantern(5, 5)
    return p

def cap(tier, seed):
    p = Piece(f'{tier}/dead_end', 5, 6, 4, seed); p.shell()
    p.door('s', 'minecraft:empty')
    p.set(2, 1, 1, 'minecraft:chiseled_polished_blackstone'); p.set(2, 2, 1, 'minecraft:soul_lantern[hanging=false]')
    return p

def pool(tier, name, entries, fallback):
    data = {'fallback': fallback, 'elements': [{'weight': w, 'element': {
        'element_type': 'minecraft:single_pool_element', 'location': f'khaosnexus:dungeon/{tier}/{loc}',
        'projection': 'rigid', 'processors': 'minecraft:empty'}} for loc, w in entries]}
    path = os.path.join(POOL, tier, name + '.json'); os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(data, open(path, 'w'), indent=2)

for t, tier in enumerate(['linear', 'branching']):
    s = 100 * t
    entrance(tier, s + 1).save(); cap(tier, s + 2).save()
    hall(tier, 'hall_straight', 5, 9, 'ns', s + 3).save()
    hall(tier, 'hall_corner', 5, 5, 'se', s + 4).save()
    hall(tier, 'hall_long', 5, 15, 'ns', s + 5).save()
    combat(tier, 'combat_room', 'sn', s + 6).save()
    combat(tier, 'combat_side', 'sw', s + 7).save()
    trap(tier, s + 8).save(); loot(tier, s + 9).save(); boss(tier, s + 10).save()
    halls = [('hall_straight', 4), ('hall_corner', 3), ('hall_long', 2)]
    rooms = [('combat_room', 4), ('combat_side', 3), ('trap_hall', 2), ('loot_vault', 1), ('boss_chamber', 1)]
    if tier == 'branching':
        hall(tier, 'hall_tee', 9, 5, 'swe', s + 11).save()
        hall(tier, 'hall_cross', 9, 9, 'nswe', s + 12).save()
        combat(tier, 'combat_hub', 'nswe', s + 13).save()
        halls += [('hall_tee', 3), ('hall_cross', 1)]
        rooms += [('combat_hub', 2)]
    fb = f'khaosnexus:dungeon/{tier}/caps'
    pool(tier, 'start', [('entrance', 1)], 'minecraft:empty')
    pool(tier, 'halls', halls, fb)
    pool(tier, 'rooms', rooms, fb)
    pool(tier, 'caps', [('dead_end', 1)], 'minecraft:empty')
print('ok')

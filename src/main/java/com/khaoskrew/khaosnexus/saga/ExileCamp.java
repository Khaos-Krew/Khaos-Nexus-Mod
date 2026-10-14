package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.khaoskrew.khaosnexus.spire.SpireVendors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Exile Camp: a floating shard of rock in the dark under the world. A soul-fire plaza with vendors, four
 * hunter tents, lantern posts, the Seal Arch (north) holding the Seal Gate, the exit Spire Gate (south), and the
 * broken Throne on its own rock in the distance. Deterministic, built once per world on first entry.
 */
public final class ExileCamp {
    public static final BlockPos ORIGIN = new BlockPos(0, 80, 0);
    public static final int RADIUS = 26;

    private ExileCamp() {}

    public static BlockPos arrival(BlockPos o) { return o.offset(0, 0, 9); }
    public static BlockPos sealGatePos(BlockPos o) { return o.offset(0, 1, -17); }
    public static BlockPos exitPos(BlockPos o) { return o.offset(0, 1, 16); }

    public static boolean built(ServerLevel l, BlockPos o) {
        return l.getBlockState(sealGatePos(o)).is(ModRegistries.SEAL_GATE.get());
    }

    static int h(int x, int z) { // stable hash noise 0..99
        int n = x * 73856093 ^ z * 19349663; n = (n ^ (n >>> 13)) * 1274126177; return Math.floorMod(n ^ (n >>> 16), 100);
    }

    static void set(ServerLevel l, BlockPos p, BlockState s) { l.setBlock(p, s, 2); }

    static void island(ServerLevel l, BlockPos o, int radius, int depth) {
        for (int x = -radius - 3; x <= radius + 3; x++) for (int z = -radius - 3; z <= radius + 3; z++) {
            double d = Math.sqrt(x * x + z * z);
            double edge = radius + (h(x / 3, z / 3) % 5) - 2;
            if (d > edge) continue;
            int down = (int) Math.round(depth * Math.pow(1 - d / (edge + 1), 0.7)) + 1 + h(x, z) % 3;
            for (int y = 0; y < down; y++) {
                int r = h(x + y * 7, z - y * 3);
                BlockState s = y == 0 ? top(x, z, d, radius) : y < 3 ? Blocks.DEEPSLATE.defaultBlockState()
                        : r < 8 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : r < 30 ? Blocks.BLACKSTONE.defaultBlockState()
                        : r < 45 ? Blocks.BASALT.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
                set(l, o.offset(x, -1 - y, z), s);
            }
            if (h(x * 3, z * 5) < 4 && d > 4) { // hanging roots/chains under the rock
                for (int y = 0; y < 2 + h(x, z) % 4; y++) set(l, o.offset(x, -1 - down - y, z), (h(z, x) % 2 == 0 ? Blocks.CHAIN : Blocks.POINTED_DRIPSTONE).defaultBlockState());
            }
        }
    }

    static BlockState top(int x, int z, double d, int radius) {
        int r = h(x, z);
        if (d <= 9) return (r < 20 ? Blocks.POLISHED_BLACKSTONE_BRICKS : r < 30 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES).defaultBlockState();
        if (Math.abs(x) <= 1 || Math.abs(z) <= 1) return Blocks.POLISHED_DEEPSLATE.defaultBlockState(); // paths
        if (d > radius - 3 && r < 50) return Blocks.TUFF.defaultBlockState();
        return (r < 20 ? Blocks.SOUL_SOIL : r < 32 ? Blocks.CRIMSON_NYLIUM : r < 60 ? Blocks.COARSE_DIRT : r < 75 ? Blocks.DEEPSLATE : Blocks.TUFF).defaultBlockState();
    }

    public static void build(ServerLevel l, BlockPos o) {
        // clear the air above first so rebuilding is idempotent
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-RADIUS - 3, 0, -RADIUS - 3), o.offset(RADIUS + 3, 16, RADIUS + 3))) set(l, p, Blocks.AIR.defaultBlockState());
        island(l, o, RADIUS, 20);
        flora(l, o);
        // central soul bonfire
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (Math.abs(x) == 2 || Math.abs(z) == 2) set(l, o.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        set(l, o, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (Direction d : Direction.Plane.HORIZONTAL) set(l, o.relative(d, 2).above(), Blocks.SOUL_LANTERN.defaultBlockState());
        for (int[] t : new int[][] {{-14, -7}, {14, -7}, {-14, 7}, {14, 7}}) tent(l, o.offset(t[0], 0, t[1]), t[0] < 0 ? Direction.EAST : Direction.WEST, t[1] < 0);
        for (int[] p : new int[][] {{-4, 12}, {4, 12}, {-4, -12}, {4, -12}, {-11, 0}, {11, 0}, {-19, -1}, {19, 1}}) post(l, o.offset(p[0], 0, p[1]));
        plazaLights(l, o);
        for (int[] m : new int[][] {{-8, 11, 1}, {8, 11, -1}, {-21, 5, 1}, {21, -5, -1}, {-21, -5, 1}, {21, 5, -1}}) stall(l, o.offset(m[0], 0, m[1]), m[2], h(m[0], m[1]));
        for (int[] f : new int[][] {{-9, -10}, {9, -10}, {0, 21}}) hearth(l, o.offset(f[0], 0, f[1]));
        for (int[] c : new int[][] {{17, -17}, {-18, -15}, {-17, 18}, {18, 17}, {24, -9}, {-24, 9}, {6, -23}, {-7, 23}}) crystal(l, o.offset(c[0], 0, c[1]), h(c[0], c[1]));
        sealArch(l, o.offset(0, 0, -17));
        exitDais(l, o.offset(0, 0, 16));
        throne(l, o.offset(0, 6, -46));
        banners(l, o);
        underside(l, o);
        fillLight(l, o);
        // vendors on the plaza (same spots as Spire towns)
        SpireVendors.spawn(l, o, 50, GateRank.S);
        ExileQuest.ensure(l, o);
    }

    static void flora(ServerLevel l, BlockPos o) {
        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) {
            BlockPos g = o.offset(x, -1, z); BlockState s = l.getBlockState(g); int r = h(x * 11, z * 7);
            if (s.is(Blocks.CRIMSON_NYLIUM) && r < 30) set(l, g.above(), (r < 18 ? Blocks.CRIMSON_ROOTS : Blocks.CRIMSON_FUNGUS).defaultBlockState());
            else if (s.is(Blocks.SOUL_SOIL) && r < 1) set(l, g.above(), Blocks.SOUL_FIRE.defaultBlockState());
            else if (s.is(Blocks.COARSE_DIRT) && r < 10) set(l, g.above(), Blocks.DEAD_BUSH.defaultBlockState());
            else if (s.is(Blocks.TUFF) && r < 3) { set(l, g.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState()); }
            else if (s.is(Blocks.COARSE_DIRT) && r < 40) {   // moss and azalea pockets soften the rock
                set(l, g, Blocks.MOSS_BLOCK.defaultBlockState());
                int q = h(z * 5, x * 13);
                set(l, g.above(), (q < 35 ? Blocks.MOSS_CARPET : q < 50 ? Blocks.FERN : q < 58 ? Blocks.AZALEA : q < 63 ? Blocks.FLOWERING_AZALEA : Blocks.AIR).defaultBlockState());
            }
        }
    }

    static BlockState lit(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }

    /** Invisible light sources (level 11) on a grid, so the whole camp stays readable in the eternal night. */
    static void fillLight(ServerLevel l, BlockPos o) {
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 11);
        for (int x = -RADIUS; x <= RADIUS; x += 5) for (int z = -RADIUS; z <= RADIUS; z += 5) {
            if (x * x + z * z > (RADIUS - 2) * (RADIUS - 2)) continue;
            BlockPos p = o.offset(x, 3, z);
            if (l.getBlockState(p).isAir() && !l.getBlockState(o.offset(x, -1, z)).isAir()) set(l, p, light);
        }
    }

    /** A ring of lantern strings around the plaza: corner posts with chains slung between them. */
    static void plazaLights(ServerLevel l, BlockPos o) {
        for (int[] c : new int[][] {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            BlockPos p = o.offset(c[0], 0, c[1]);
            set(l, p, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            for (int y = 1; y <= 3; y++) set(l, p.above(y), Blocks.DARK_OAK_FENCE.defaultBlockState());
            set(l, p.above(4), Blocks.LANTERN.defaultBlockState());
        }
        for (int i = -6; i <= 6; i++) for (int side : new int[] {-7, 7}) {
            set(l, o.offset(i, 4, side), Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.X));
            set(l, o.offset(side, 4, i), Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.Z));
            if (Math.floorMod(i, 3) == 0) {
                set(l, o.offset(i, 3, side), (i % 2 == 0 ? Blocks.LANTERN : Blocks.SOUL_LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true));
                set(l, o.offset(side, 3, i), (i % 2 == 0 ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
        // the N-S road posts get a string too
        for (int z : new int[] {12, -12}) for (int x = -3; x <= 3; x++) {
            set(l, o.offset(x, 4, z), Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.X));
            if (x % 2 == 0) set(l, o.offset(x, 3, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    /** A market stall: fence frame, striped wool awning, barrel counter, stacked crates, a hanging lantern and a banner. */
    static void stall(ServerLevel l, BlockPos c, int face, int seed) {
        net.minecraft.world.level.block.Block[][] awn = {{Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL}, {Blocks.CYAN_WOOL, Blocks.BLACK_WOOL}, {Blocks.RED_WOOL, Blocks.YELLOW_WOOL}, {Blocks.MAGENTA_WOOL, Blocks.GRAY_WOOL}};
        net.minecraft.world.level.block.Block[] a = awn[seed % awn.length];
        for (int x = -1; x <= 1; x++) for (int z = -2; z <= 2; z++) set(l, c.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int[] k : new int[][] {{-1, -2}, {1, -2}, {-1, 2}, {1, 2}}) for (int y = 0; y <= 2; y++) set(l, c.offset(k[0], y, k[1]), Blocks.SPRUCE_FENCE.defaultBlockState());
        for (int x = -1; x <= 1; x++) for (int z = -2; z <= 2; z++) set(l, c.offset(x, 3, z), a[Math.floorMod(z, 2)].defaultBlockState());
        for (int z = -2; z <= 2; z++) set(l, c.offset(face * 2, 3, z), (z % 2 == 0 ? Blocks.SPRUCE_TRAPDOOR : a[0]).defaultBlockState());   // awning lip over the front
        for (int z = -1; z <= 1; z++) set(l, c.offset(face, 0, z), (z == 0 ? Blocks.BARREL : Blocks.SPRUCE_SLAB).defaultBlockState());
        set(l, c.offset(face, 1, -1), Blocks.FLOWER_POT.defaultBlockState()); set(l, c.offset(face, 1, 1), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
        set(l, c.offset(-face, 0, -1), Blocks.BARREL.defaultBlockState()); set(l, c.offset(-face, 1, -1), Blocks.BARREL.defaultBlockState());
        set(l, c.offset(-face, 0, 1), Blocks.CHEST.defaultBlockState()); set(l, c.offset(-face, 0, 0), Blocks.COMPOSTER.defaultBlockState());
        set(l, c.offset(0, 2, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(l, c.offset(face * 2, 0, 2), Blocks.PURPLE_BANNER.defaultBlockState());
    }

    /** A small open campfire ringed by log benches. */
    static void hearth(ServerLevel l, BlockPos c) {
        set(l, c.below(), Blocks.MUD_BRICKS.defaultBlockState());
        set(l, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, false));
        set(l, c.offset(2, 0, 0), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
        set(l, c.offset(-2, 0, 0), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
        set(l, c.offset(0, 0, 2), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
        set(l, c.offset(1, 0, -2), Blocks.BARREL.defaultBlockState());
        set(l, c.offset(-1, 0, -2), Blocks.CAULDRON.defaultBlockState());
    }

    /** A glowing crystal outcrop: amethyst spires with clusters and a hidden light. */
    static void crystal(ServerLevel l, BlockPos c, int seed) {
        int tall = 3 + seed % 4;
        for (int y = 0; y < tall; y++) set(l, c.above(y), (y == 0 ? Blocks.BUDDING_AMETHYST : Blocks.AMETHYST_BLOCK).defaultBlockState());
        set(l, c.above(tall), Blocks.AMETHYST_CLUSTER.defaultBlockState());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int hh = 1 + (seed + d.get2DDataValue() * 7) % Math.max(1, tall - 1);
            for (int y = 0; y < hh; y++) set(l, c.relative(d).above(y), Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(l, c.relative(d).above(hh), Blocks.LARGE_AMETHYST_BUD.defaultBlockState());
            set(l, c.relative(d, 2), Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState().setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING, Direction.UP));
        }
        set(l, c.offset(1, tall, 1), Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13));
        set(l, c.offset(-1, 1, -1), Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** Hanging purple banners on the arch pillars and tents. */
    static void banners(ServerLevel l, BlockPos o) {
        for (int x : new int[] {-4, 4}) set(l, o.offset(x, 6, -16), Blocks.PURPLE_WALL_BANNER.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.SOUTH));
        for (int[] t : new int[][] {{-14, -7}, {14, -7}, {-14, 7}, {14, 7}}) set(l, o.offset(t[0], 3, t[1] + (t[1] < 0 ? -5 : 5)), Blocks.BLACK_BANNER.defaultBlockState());
    }

    /** Glowing life under the rock: spore blossoms and lit cave vines hang into the void. */
    static void underside(ServerLevel l, BlockPos o) {
        for (int x = -RADIUS + 4; x <= RADIUS - 4; x += 3) for (int z = -RADIUS + 4; z <= RADIUS - 4; z += 3) {
            if (h(x * 7, z * 3) > 30) continue;
            BlockPos p = o.offset(x, -1, z);
            while (!l.getBlockState(p).isAir() && p.getY() > o.getY() - 30) p = p.below();
            if (p.getY() <= o.getY() - 30) continue;
            if (h(x, z * 9) < 12) { set(l, p, Blocks.SPORE_BLOSSOM.defaultBlockState()); continue; }
            int len = 2 + h(z, x) % 5;
            for (int i = 0; i < len; i++) set(l, p.below(i), (i == len - 1 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CaveVines.BERRIES, (i + x) % 2 == 0));
        }
    }

    static void tent(ServerLevel l, BlockPos c, Direction face, boolean red) {
        BlockState cloth = (red ? Blocks.RED_WOOL : Blocks.BLACK_WOOL).defaultBlockState();
        for (int x = -2; x <= 2; x++) for (int z = -3; z <= 3; z++) set(l, c.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int z = -3; z <= 3; z++) {
            for (int y = 0; y <= 3; y++) {
                int w = 3 - y; // A-frame: two sloped cloth walls
                set(l, c.offset(-w, y, z), cloth); set(l, c.offset(w, y, z), cloth);
            }
        }
        for (int y = 0; y <= 4; y++) { set(l, c.offset(0, y, -4), Blocks.DARK_OAK_FENCE.defaultBlockState()); set(l, c.offset(0, y, 4), Blocks.DARK_OAK_FENCE.defaultBlockState()); }
        set(l, c.offset(0, 4, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
        for (int z = -3; z <= 3; z++) set(l, c.offset(0, 4, z), Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
        int open = face == Direction.EAST ? 1 : -1; // the side facing the plaza is open: clear one cloth wall
        for (int z = -2; z <= 2; z++) for (int y = 0; y <= 1; y++) set(l, c.offset(open * (3 - y), y, z), Blocks.AIR.defaultBlockState());
        set(l, c.offset(-open, 0, -2), Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH).setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        set(l, c.offset(-open, 0, -1), Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH).setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        set(l, c.offset(-open, 0, 1), Blocks.BARREL.defaultBlockState());
        set(l, c.offset(-open, 0, 2), Blocks.SMITHING_TABLE.defaultBlockState());
    }

    static void post(ServerLevel l, BlockPos p) {
        set(l, p, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        for (int y = 1; y <= 2; y++) set(l, p.above(y), Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(l, p.above(3), Blocks.CHAIN.defaultBlockState());
        set(l, p.above(4), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    /** A broken arch of crying obsidian and gilded stone; the Seal Gate sits in its mouth. */
    static void sealArch(ServerLevel l, BlockPos c) {
        for (int x = -6; x <= 6; x++) for (int z = -2; z <= 1; z++) set(l, c.offset(x, -1, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int x = -3; x <= 3; x++) set(l, c.offset(x, 0, 2), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        for (int side : new int[] {-1, 1}) {
            for (int y = 0; y <= 11; y++) for (int t = 0; t <= 1; t++) {
                int x = side * (4 + t);
                if (side > 0 && y > 8 && t == 1) continue; // the east pillar is broken off
                set(l, c.offset(x, y, 0), (y % 4 == 3 ? Blocks.CHISELED_POLISHED_BLACKSTONE : t == 0 && y % 3 == 1 ? Blocks.CRYING_OBSIDIAN : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            }
        }
        for (int x = -5; x <= 3; x++) { // lintel, cracked open on the east
            int y = 12 - Math.max(0, Math.abs(x) - 2) / 2;
            set(l, c.offset(x, y, 0), (Math.abs(x) % 3 == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            set(l, c.offset(x, y - 1, 0), (Math.abs(x) <= 1 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE).defaultBlockState());
        }
        set(l, c.offset(0, 10, 0), Blocks.GILDED_BLACKSTONE.defaultBlockState());
        for (int x : new int[] {-3, 3}) for (int y = 4; y <= 9; y++) set(l, c.offset(x, y, 0), Blocks.CHAIN.defaultBlockState()); // broken seal chains
        set(l, c.offset(0, 1, 0), ModRegistries.SEAL_GATE.get().defaultBlockState());
        set(l, c.offset(0, 0, 0), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        for (int x : new int[] {-2, 2}) { set(l, c.offset(x, 0, 1), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState()); set(l, c.offset(x, 1, 1), Blocks.SOUL_LANTERN.defaultBlockState()); }
        // rubble from the broken pillar
        for (int[] r : new int[][] {{6, 0, 2}, {7, 0, 1}, {6, 1, 1}, {8, 0, 3}}) set(l, c.offset(r[0], r[1], r[2]), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
    }

    static void exitDais(ServerLevel l, BlockPos c) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) set(l, c.offset(x, 0, z), (Math.abs(x) == 2 || Math.abs(z) == 2 ? Blocks.POLISHED_DEEPSLATE_SLAB : Blocks.DEEPSLATE_TILES).defaultBlockState());
        set(l, c.above(), ModRegistries.SPIRE_GATE.get().defaultBlockState());
        for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) { set(l, c.offset(k[0], 1, k[1]), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState()); set(l, c.offset(k[0], 2, k[1]), Blocks.SOUL_LANTERN.defaultBlockState()); }
    }

    /** The Hollow King's empty throne on a far rock: the saga's goal, always in view from the camp. */
    static void throne(ServerLevel l, BlockPos c) {
        island(l, c, 9, 14);
        for (int x = -3; x <= 3; x++) for (int z = -2; z <= 2; z++) set(l, c.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int x = -2; x <= 2; x++) set(l, c.offset(x, 1, 0), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        for (int y = 1; y <= 9; y++) for (int x = -2; x <= 2; x++) if (y > 2 || Math.abs(x) == 2) set(l, c.offset(x, y, -1), (y == 9 && Math.abs(x) % 2 == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.BLACKSTONE).defaultBlockState());
        for (int x : new int[] {-2, 0, 2}) set(l, c.offset(x, 10, -1), Blocks.GOLD_BLOCK.defaultBlockState());
        for (int y = 1; y <= 3; y++) for (int x : new int[] {-3, 3}) set(l, c.offset(x, y, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        set(l, c.offset(0, 2, 0), Blocks.CHAIN.defaultBlockState());
        for (int i = 0; i < 6; i++) set(l, c.offset(-6 + i * 2, 1, 3 + (i % 2)), Blocks.SOUL_CAMPFIRE.defaultBlockState());
    }
}

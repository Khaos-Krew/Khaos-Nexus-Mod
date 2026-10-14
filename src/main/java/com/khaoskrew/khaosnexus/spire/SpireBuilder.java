package com.khaoskrew.khaosnexus.spire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Builds Spire towns and level arenas. Themes are datapack files in khaosnexus_spire_themes with block ids
 * ("floor", "accent", "wall", "pillar", "light", "trim"), a portal "rank" tint, a "bosses" list (cycled by level),
 * a "final_boss" for the last level, and optional "floors" ([n, ...]) to pin a theme to specific floors.
 * Unpinned themes rotate floor by floor.
 */
public final class SpireBuilder {
    public static final int ARENA_HALF = 15;
    public static final int TOWN_HALF = 16;
    public static final int WALL_HEIGHT = 8;

    public record Theme(ResourceLocation id, BlockState floor, BlockState accent, BlockState wall, BlockState pillar,
            BlockState light, BlockState trim, GateRank rank, List<ResourceLocation> bosses, ResourceLocation finalBoss) {}

    private SpireBuilder() {}

    private static BlockState block(JsonObject o, String key, Block fallback) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(o, key, ""));
        Block b = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        return (b == null || b == Blocks.AIR ? fallback : b).defaultBlockState();
    }

    public static Theme parse(ResourceLocation id, JsonObject o) {
        List<ResourceLocation> bosses = new ArrayList<>();
        for (var e : GsonHelper.getAsJsonArray(o, "bosses", new com.google.gson.JsonArray())) {
            ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
            if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                bosses.add(rl);
            }
        }
        ResourceLocation fin = ResourceLocation.tryParse(GsonHelper.getAsString(o, "final_boss", "khaosnexus:spire_guardian"));
        if (fin == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(fin)) {
            fin = ResourceLocation.fromNamespaceAndPath("khaosnexus", "spire_guardian");
        }
        if (bosses.isEmpty()) {
            bosses.add(fin);
        }
        return new Theme(id, block(o, "floor", Blocks.POLISHED_DEEPSLATE), block(o, "accent", Blocks.DEEPSLATE_TILES),
                block(o, "wall", Blocks.DEEPSLATE_BRICKS), block(o, "pillar", Blocks.POLISHED_BASALT),
                block(o, "light", Blocks.SEA_LANTERN), block(o, "trim", Blocks.CHISELED_DEEPSLATE),
                GateRank.byName(GsonHelper.getAsString(o, "rank", "S")), bosses, fin);
    }

    public static Theme fallback() {
        return parse(ResourceLocation.fromNamespaceAndPath("khaosnexus", "fallback"), new JsonObject());
    }

    /** Theme for a floor: a file pinned to the floor, else rotation over unpinned files sorted by id. */
    public static Theme themeFor(int floor) {
        List<Map.Entry<ResourceLocation, JsonObject>> rotation = new ArrayList<>();
        for (var e : JsonPool.SPIRE_THEMES.all().entrySet()) {
            var pinned = GsonHelper.getAsJsonArray(e.getValue(), "floors", null);
            if (pinned != null) {
                for (var f : pinned) {
                    if (f.getAsInt() == floor) {
                        return parse(e.getKey(), e.getValue());
                    }
                }
            } else {
                rotation.add(e);
            }
        }
        if (rotation.isEmpty()) {
            return fallback();
        }
        rotation.sort(Map.Entry.comparingByKey());
        var e = rotation.get(Math.floorMod(floor - 1, rotation.size()));
        return parse(e.getKey(), e.getValue());
    }

    public static ResourceLocation bossFor(Theme theme, int level, int levels) {
        return level >= levels ? theme.finalBoss() : theme.bosses().get(Math.floorMod(level - 1, theme.bosses().size()));
    }

    private static void fill(ServerLevel l, BlockPos a, BlockPos b, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            l.setBlock(p, s, Block.UPDATE_CLIENTS);
        }
    }

    private static void chunks(ServerLevel l, BlockPos o, int half) {
        for (int x = (o.getX() - half) >> 4; x <= (o.getX() + half) >> 4; x++) {
            for (int z = (o.getZ() - half) >> 4; z <= (o.getZ() + half) >> 4; z++) {
                l.getChunk(x, z);
            }
        }
    }

    /** Portal position for an arena or town at origin o: centred on the north side. */
    public static BlockPos portalPos(BlockPos o, boolean town) {
        return o.offset(0, 0, -(town ? TOWN_HALF : ARENA_HALF) + 3);
    }

    /** Player arrival spot: the south side. */
    public static BlockPos arrival(BlockPos o, boolean town) {
        return o.offset(0, 0, (town ? TOWN_HALF : ARENA_HALF) - 3);
    }

    public static void buildArena(ServerLevel l, BlockPos o, Theme t) {
        int h = ARENA_HALF;
        chunks(l, o, h + 1);
        fill(l, o.offset(-h, 0, -h), o.offset(h, WALL_HEIGHT + 4, h), Blocks.AIR.defaultBlockState());
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                BlockState s = (ring % 4 == 0 || (Math.abs(dx) == Math.abs(dz) && ring < h - 1)) ? t.accent() : t.floor();
                l.setBlock(o.offset(dx, -1, dz), s, Block.UPDATE_CLIENTS);
                l.setBlock(o.offset(dx, -2, dz), t.wall(), Block.UPDATE_CLIENTS);
            }
        }
        l.setBlock(o.offset(0, -1, 0), t.trim(), Block.UPDATE_CLIENTS);
        // Walls with a trim band and recessed lights.
        for (int i = -h; i <= h; i++) {
            for (int y = 0; y <= WALL_HEIGHT; y++) {
                BlockState s = y == 4 || y == WALL_HEIGHT ? t.trim() : t.wall();
                boolean lamp = y == 2 && Math.floorMod(i, 4) == 0 && Math.abs(i) < h;
                for (BlockPos p : new BlockPos[] {o.offset(i, y, -h), o.offset(i, y, h), o.offset(-h, y, i), o.offset(h, y, i)}) {
                    l.setBlock(p, lamp ? t.light() : s, Block.UPDATE_CLIENTS);
                }
            }
        }
        // Eight pillars with light caps.
        int[][] pillars = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}, {-11, 0}, {11, 0}, {-6, 11}, {5, 11}};
        for (int[] p : pillars) {
            fill(l, o.offset(p[0], 0, p[1]), o.offset(p[0] + 1, 5, p[1] + 1), t.pillar());
            fill(l, o.offset(p[0], 6, p[1]), o.offset(p[0] + 1, 6, p[1] + 1), t.light());
        }
        // Portal dais.
        BlockPos portal = portalPos(o, false);
        fill(l, portal.offset(-2, -1, -1), portal.offset(2, -1, 1), t.trim());
        l.setBlock(portal.offset(-3, 0, 0), t.light(), Block.UPDATE_CLIENTS);
        l.setBlock(portal.offset(3, 0, 0), t.light(), Block.UPDATE_CLIENTS);
    }

    private static int noise(int x, int z) {
        int n = x * 73856093 ^ z * 19349663;
        n ^= n >>> 13;
        return Math.floorMod(n * 0x5bd1e995, 100);
    }

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    /** A village-style safe town: textured plaza and paths, a roofed well, peaked huts with porches, lamp posts,
     * bannered vendor stalls, a walled and posted perimeter, garden beds, the checkpoint shrine, exit gate and portal dais. */
    public static void buildTown(ServerLevel l, BlockPos o) {
        int h = TOWN_HALF;
        chunks(l, o, h + 1);
        fill(l, o.offset(-h, 0, -h), o.offset(h, 10, h), Blocks.AIR.defaultBlockState());
        fill(l, o.offset(-h, -3, -h), o.offset(h, -2, h), Blocks.DIRT.defaultBlockState());
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                int n = noise(dx, dz), ax = Math.abs(dx), az = Math.abs(dz);
                boolean path = ax <= 1 || az <= 1;
                boolean pathEdge = !path && (ax == 2 || az == 2);
                boolean plaza = ax <= 4 && az <= 4;
                boolean edge = ax == h || az == h;
                BlockState s;
                if (edge) s = st(n < 30 ? Blocks.MOSSY_STONE_BRICKS : n < 45 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
                else if (plaza) s = (ax == 4 || az == 4) ? st(Blocks.POLISHED_ANDESITE)
                        : (ax == az && ax >= 2) ? st(Blocks.CHISELED_STONE_BRICKS)
                        : st(n < 20 ? Blocks.CRACKED_STONE_BRICKS : n < 35 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
                else if (path) s = st(n < 18 ? Blocks.COARSE_DIRT : n < 26 ? Blocks.GRAVEL : Blocks.DIRT_PATH);
                else if (pathEdge && n < 35) s = st(n < 15 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT);
                else s = st(Blocks.GRASS_BLOCK);
                l.setBlock(o.offset(dx, -1, dz), s, Block.UPDATE_CLIENTS);
                if (edge) {
                    boolean post = (Math.floorMod(dx, 4) == 0 && az == h) || (Math.floorMod(dz, 4) == 0 && ax == h) || (ax == h && az == h);
                    if (post) {
                        fill(l, o.offset(dx, 0, dz), o.offset(dx, 1, dz), st(Blocks.STONE_BRICK_WALL));
                        l.setBlock(o.offset(dx, 2, dz), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        l.setBlock(o.offset(dx, 0, dz), st(Blocks.SPRUCE_FENCE), Block.UPDATE_ALL);
                    }
                } else if (!plaza && !path && !pathEdge && s.is(Blocks.GRASS_BLOCK)) {
                    BlockState deco = n < 22 ? st(Blocks.SHORT_GRASS) : n < 26 ? st(Blocks.POPPY) : n < 30 ? st(Blocks.CORNFLOWER)
                            : n < 33 ? st(Blocks.OXEYE_DAISY) : n < 35 ? st(Blocks.FERN) : null;
                    if (deco != null) l.setBlock(o.offset(dx, 0, dz), deco, Block.UPDATE_CLIENTS);
                }
            }
        }
        // Roofed well: brick rim, water, posts with a chain and lantern, stair roof.
        fill(l, o.offset(-1, 0, -1), o.offset(1, 0, 1), st(Blocks.STONE_BRICKS));
        l.setBlock(o, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        l.setBlock(o.below(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            fill(l, o.offset(c[0], 1, c[1]), o.offset(c[0], 2, c[1]), st(Blocks.STRIPPED_SPRUCE_LOG));
        }
        for (int dx = -2; dx <= 2; dx++) {
            l.setBlock(o.offset(dx, 3, -1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
            l.setBlock(o.offset(dx, 3, 1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), Block.UPDATE_CLIENTS);
            l.setBlock(o.offset(dx, 3, 0), st(Blocks.SPRUCE_PLANKS), Block.UPDATE_CLIENTS);
            l.setBlock(o.offset(dx, 4, 0), Blocks.SPRUCE_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        l.setBlock(o.above(2), st(Blocks.CHAIN), Block.UPDATE_ALL);
        // Four huts in the corners.
        for (int[] c : new int[][] {{-10, -10}, {10, -10}, {-10, 10}, {10, 10}}) {
            hut(l, o.offset(c[0], 0, c[1]), c[1] < 0 ? Direction.SOUTH : Direction.NORTH);
        }
        // Lamp posts along the paths: wall base, fence, lantern under a little cap.
        for (int[] c : new int[][] {{-3, -7}, {3, -7}, {-3, 7}, {3, 7}, {-7, -3}, {7, -3}, {-7, 3}, {7, 3}}) {
            l.setBlock(o.offset(c[0], 0, c[1]), st(Blocks.COBBLESTONE_WALL), Block.UPDATE_ALL);
            fill(l, o.offset(c[0], 1, c[1]), o.offset(c[0], 2, c[1]), st(Blocks.SPRUCE_FENCE));
            l.setBlock(o.offset(c[0], 3, c[1]), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(o.offset(c[0], 4, c[1]), Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // Vendor stalls: barrels and crates, striped awning, posts with purple guild banners.
        for (int side : new int[] {-1, 1}) {
            BlockPos stall = o.offset(side * 6, 0, 0);
            for (int dz = -3; dz <= 3; dz++) {
                int n = noise(side * 6, dz);
                l.setBlock(stall.offset(0, 0, dz), n < 50 ? st(Blocks.BARREL) : st(Blocks.SPRUCE_PLANKS), Block.UPDATE_CLIENTS);
                if (n % 3 == 0) l.setBlock(stall.offset(0, 1, dz), n < 50 ? st(Blocks.FLOWER_POT) : st(Blocks.CANDLE), Block.UPDATE_ALL);
                l.setBlock(stall.offset(side, 3, dz), (dz & 1) == 0 ? st(Blocks.PURPLE_WOOL) : st(Blocks.WHITE_WOOL), Block.UPDATE_CLIENTS);
                l.setBlock(stall.offset(0, 3, dz), (dz & 1) == 0 ? st(Blocks.WHITE_WOOL) : st(Blocks.PURPLE_WOOL), Block.UPDATE_CLIENTS);
            }
            for (int dz : new int[] {-3, 3}) {
                fill(l, stall.offset(side, 0, dz), stall.offset(side, 2, dz), st(Blocks.STRIPPED_SPRUCE_LOG));
                Direction face = dz < 0 ? Direction.NORTH : Direction.SOUTH;
                l.setBlock(stall.offset(side, 2, dz).relative(face), Blocks.PURPLE_WALL_BANNER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, face), Block.UPDATE_ALL);
            }
        }
        // Garden beds by the huts.
        for (int[] c : new int[][] {{-13, -6}, {13, -6}, {-13, 6}, {13, 6}}) {
            for (int dz = -1; dz <= 1; dz++) {
                l.setBlock(o.offset(c[0], -1, c[1] + dz), st(Blocks.FARMLAND), Block.UPDATE_CLIENTS);
                l.setBlock(o.offset(c[0], 0, c[1] + dz), (dz == 0 ? Blocks.CARROTS : Blocks.WHEAT).defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CropBlock.AGE, 7), Block.UPDATE_CLIENTS);
            }
            l.setBlock(o.offset(c[0], -1, c[1] + 2), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Checkpoint shrine south of the plaza, next to the exit gate.
        BlockPos shrine = o.offset(4, 0, h - 4);
        fill(l, shrine.offset(-1, -1, -1), shrine.offset(1, -1, 1), st(Blocks.POLISHED_BLACKSTONE_BRICKS));
        l.setBlock(shrine, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
        l.setBlock(shrine.above(), Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            l.setBlock(shrine.offset(c[0], 0, c[1]), st(Blocks.POLISHED_BLACKSTONE_WALL), Block.UPDATE_ALL);
            l.setBlock(shrine.offset(c[0], 1, c[1]), Blocks.SOUL_TORCH.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos exit = o.offset(-4, 0, h - 4);
        fill(l, exit.offset(-1, -1, -1), exit.offset(1, -1, 1), st(Blocks.CHISELED_STONE_BRICKS));
        l.setBlock(exit, ModRegistries.SPIRE_GATE.get().defaultBlockState(), Block.UPDATE_ALL);
        // Portal dais: chiselled base, amethyst-capped obelisks either side.
        BlockPos portal = portalPos(o, true);
        fill(l, portal.offset(-2, -1, -1), portal.offset(2, -1, 1), st(Blocks.CHISELED_STONE_BRICKS));
        for (int side : new int[] {-3, 3}) {
            fill(l, portal.offset(side, 0, 0), portal.offset(side, 2, 0), st(Blocks.POLISHED_DEEPSLATE));
            l.setBlock(portal.offset(side, 3, 0), st(Blocks.AMETHYST_BLOCK), Block.UPDATE_CLIENTS);
            l.setBlock(portal.offset(side, 4, 0), Blocks.AMETHYST_CLUSTER.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static BlockPos checkpointPos(BlockPos town) {
        return town.offset(4, 0, TOWN_HALF - 4);
    }

    public static BlockPos exitPos(BlockPos town) {
        return town.offset(-4, 0, TOWN_HALF - 4);
    }

    /** Vendor spots behind the stall counters. */
    public static List<BlockPos> vendorSpots(BlockPos town) {
        return List.of(town.offset(-7, 0, -2), town.offset(-7, 0, 2), town.offset(7, 0, 0));
    }

    private static void hut(ServerLevel l, BlockPos c, Direction door) {
        fill(l, c.offset(-2, -1, -2), c.offset(2, -1, 2), st(Blocks.COBBLESTONE));
        fill(l, c.offset(-2, 0, -2), c.offset(2, 0, 2), st(Blocks.STONE_BRICKS));
        fill(l, c.offset(-2, 1, -2), c.offset(2, 3, 2), st(Blocks.OAK_PLANKS));
        fill(l, c.offset(-1, 0, -1), c.offset(1, 3, 1), Blocks.AIR.defaultBlockState());
        fill(l, c.offset(-1, -1, -1), c.offset(1, -1, 1), st(Blocks.SPRUCE_PLANKS));
        for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            fill(l, c.offset(k[0], 0, k[1]), c.offset(k[0], 3, k[1]), st(Blocks.SPRUCE_LOG));
        }
        // Peaked stair roof along the door axis, with overhanging eaves.
        Direction ridge = door.getClockWise();
        for (int t = -3; t <= 3; t++) {
            for (int a = -3; a <= 3; a++) {
                BlockPos p = c.relative(ridge, t).relative(door, a).atY(c.getY() + 4 + Math.max(0, 2 - Math.abs(a)));
                BlockState roof = a == 0 ? st(Blocks.SPRUCE_PLANKS)
                        : Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, a < 0 ? door : door.getOpposite());
                l.setBlock(p, roof, Block.UPDATE_CLIENTS);
            }
        }
        for (int side : new int[] {-2, 2}) {           // gable ends close the attic
            BlockPos g = c.relative(ridge, side);
            fill(l, g.relative(door).above(4), g.relative(door.getOpposite()).above(4), st(Blocks.STRIPPED_SPRUCE_LOG));
            l.setBlock(g.above(5), st(Blocks.STRIPPED_SPRUCE_LOG), Block.UPDATE_CLIENTS);
        }
        BlockPos d = c.relative(door, 2);
        fill(l, d, d.above(), Blocks.AIR.defaultBlockState());
        l.setBlock(d, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, door.getOpposite()), Block.UPDATE_CLIENTS);
        l.setBlock(d.above(), Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, door.getOpposite())
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        BlockPos porch = d.relative(door);
        l.setBlock(porch.below(), st(Blocks.SPRUCE_PLANKS), Block.UPDATE_CLIENTS);
        l.setBlock(d.above(2).relative(door), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), Block.UPDATE_ALL);
        l.setBlock(d.above(2), st(Blocks.SPRUCE_PLANKS), Block.UPDATE_CLIENTS);
        for (Direction side : new Direction[] {door.getClockWise(), door.getCounterClockWise()}) {
            BlockPos w = c.relative(side, 2).above(2);
            l.setBlock(w, Blocks.GLASS_PANE.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(w.relative(side).below(), Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), Block.UPDATE_CLIENTS);
            l.setBlock(w.relative(side), st(Blocks.POTTED_RED_TULIP), Block.UPDATE_ALL);
        }
        l.setBlock(c.above(3), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), Block.UPDATE_ALL);
        l.setBlock(c.relative(door.getOpposite()), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_CLIENTS);
        l.setBlock(c.relative(door.getOpposite()).relative(door.getClockWise()), st(Blocks.BARREL), Block.UPDATE_CLIENTS);
        l.setBlock(c.relative(door.getOpposite()).relative(door.getCounterClockWise()), st(Blocks.BOOKSHELF), Block.UPDATE_CLIENTS);
    }
}

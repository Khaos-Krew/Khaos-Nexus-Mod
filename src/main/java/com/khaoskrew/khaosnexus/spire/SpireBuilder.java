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
        int[][] pillars = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}, {-11, 0}, {11, 0}, {0, 11}, {-4, -11}};
        for (int[] p : pillars) {
            if (p[1] == -11) {
                continue; // keep the portal approach clear
            }
            fill(l, o.offset(p[0], 0, p[1]), o.offset(p[0] + 1, 5, p[1] + 1), t.pillar());
            fill(l, o.offset(p[0], 6, p[1]), o.offset(p[0] + 1, 6, p[1] + 1), t.light());
        }
        // Portal dais.
        BlockPos portal = portalPos(o, false);
        fill(l, portal.offset(-2, -1, -1), portal.offset(2, -1, 1), t.trim());
        l.setBlock(portal.offset(-3, 0, 0), t.light(), Block.UPDATE_CLIENTS);
        l.setBlock(portal.offset(3, 0, 0), t.light(), Block.UPDATE_CLIENTS);
    }

    /** A small village-style safe town: plaza, well, huts, lamp posts, vendor stalls, checkpoint and gates. */
    public static void buildTown(ServerLevel l, BlockPos o) {
        int h = TOWN_HALF;
        chunks(l, o, h + 1);
        fill(l, o.offset(-h, 0, -h), o.offset(h, 10, h), Blocks.AIR.defaultBlockState());
        fill(l, o.offset(-h, -3, -h), o.offset(h, -2, h), Blocks.DIRT.defaultBlockState());
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                boolean path = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                boolean plaza = Math.abs(dx) <= 4 && Math.abs(dz) <= 4;
                boolean edge = Math.abs(dx) == h || Math.abs(dz) == h;
                BlockState s = edge ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        : plaza ? Blocks.STONE_BRICKS.defaultBlockState()
                        : path ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                l.setBlock(o.offset(dx, -1, dz), s, Block.UPDATE_CLIENTS);
                if (edge) {
                    l.setBlock(o.offset(dx, 0, dz), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        // Well in the plaza.
        fill(l, o.offset(-1, 0, -1), o.offset(1, 0, 1), Blocks.COBBLESTONE.defaultBlockState());
        l.setBlock(o, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            fill(l, o.offset(c[0], 1, c[1]), o.offset(c[0], 2, c[1]), Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(l, o.offset(-1, 3, -1), o.offset(1, 3, 1), Blocks.OAK_SLAB.defaultBlockState());
        // Four huts in the corners.
        for (int[] c : new int[][] {{-10, -10}, {10, -10}, {-10, 10}, {10, 10}}) {
            hut(l, o.offset(c[0], 0, c[1]), c[1] < 0 ? Direction.SOUTH : Direction.NORTH);
        }
        // Lamp posts along the paths.
        for (int[] c : new int[][] {{-3, -7}, {3, -7}, {-3, 7}, {3, 7}, {-7, -3}, {7, -3}, {-7, 3}, {7, 3}}) {
            fill(l, o.offset(c[0], 0, c[1]), o.offset(c[0], 2, c[1]), Blocks.SPRUCE_FENCE.defaultBlockState());
            l.setBlock(o.offset(c[0], 3, c[1]), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Vendor stalls: a counter and a striped awning west and east of the plaza.
        for (int side : new int[] {-1, 1}) {
            BlockPos stall = o.offset(side * 6, 0, 0);
            fill(l, stall.offset(0, 0, -3), stall.offset(0, 0, 3), Blocks.BARREL.defaultBlockState());
            for (int dz = -3; dz <= 3; dz++) {
                l.setBlock(stall.offset(side, 3, dz), (dz & 1) == 0 ? Blocks.RED_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState(), Block.UPDATE_CLIENTS);
                l.setBlock(stall.offset(0, 3, dz), (dz & 1) == 0 ? Blocks.WHITE_WOOL.defaultBlockState() : Blocks.RED_WOOL.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            fill(l, stall.offset(side, 0, -3), stall.offset(side, 2, -3), Blocks.SPRUCE_FENCE.defaultBlockState());
            fill(l, stall.offset(side, 0, 3), stall.offset(side, 2, 3), Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        // Checkpoint shrine south of the plaza, next to the exit gate.
        BlockPos shrine = o.offset(4, 0, h - 4);
        l.setBlock(shrine, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
        l.setBlock(shrine.above(), Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        l.setBlock(o.offset(-4, 0, h - 4), ModRegistries.SPIRE_GATE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos portal = portalPos(o, true);
        fill(l, portal.offset(-2, -1, -1), portal.offset(2, -1, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
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
        fill(l, c.offset(-2, 0, -2), c.offset(2, 3, 2), Blocks.OAK_PLANKS.defaultBlockState());
        fill(l, c.offset(-1, 0, -1), c.offset(1, 3, 1), Blocks.AIR.defaultBlockState());
        for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            fill(l, c.offset(k[0], 0, k[1]), c.offset(k[0], 3, k[1]), Blocks.SPRUCE_LOG.defaultBlockState());
        }
        BlockState roof = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        fill(l, c.offset(-3, 4, -3), c.offset(3, 4, 3), roof);
        fill(l, c.offset(-2, 4, -2), c.offset(2, 4, 2), Blocks.SPRUCE_PLANKS.defaultBlockState());
        fill(l, c.offset(-1, 5, -1), c.offset(1, 5, 1), roof);
        BlockPos d = c.relative(door, 2);
        fill(l, d, d.above(), Blocks.AIR.defaultBlockState());
        for (Direction side : new Direction[] {door.getClockWise(), door.getCounterClockWise()}) {
            l.setBlock(c.relative(side, 2).above(), Blocks.GLASS_PANE.defaultBlockState(), Block.UPDATE_ALL);
        }
        l.setBlock(c.above(3), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), Block.UPDATE_ALL);
        l.setBlock(c.relative(door.getOpposite()), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}

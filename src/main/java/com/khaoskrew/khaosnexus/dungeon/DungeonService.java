package com.khaoskrew.khaosnexus.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Builds instanced gate dungeons from the jigsaw pools in data/khaosnexus/worldgen/template_pool/dungeon.
 * E and D use the linear pools, C and up the branching ones. Depth grows with rank.
 */
public final class DungeonService {
    public static final ResourceLocation DOOR = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "door");
    public static final String RETURN = "khaosnexus_dungeon_return";
    private static final int ATTEMPTS = 48;

    private DungeonService() {}

    public record Layout(BlockPos spawn, float yaw, @Nullable BlockPos boss, List<BlockPos> mobs, List<BlockPos> loot, BoundingBox box, int pieces) {}

    public static int depth(GateRank rank) {
        return new int[] {4, 5, 6, 7, 8, 10}[rank.ordinal()];
    }

    public static int minPieces(GateRank rank) {
        return new int[] {5, 7, 10, 13, 16, 20}[rank.ordinal()];
    }

    public static String tier(GateRank rank) {
        return rank.ordinal() < 2 ? "linear" : "branching";
    }

    @Nullable
    public static ServerLevel level(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(DungeonRules.GATE_DUNGEON);
    }

    /** Leases a free instance region: a wiped, reused slot when one is available, else a new one. */
    public static DungeonInstances.Lease allocate(ServerLevel dungeon) {
        return DungeonInstances.get(dungeon).lease(dungeon.getGameTime());
    }

    /** Ends an instance: players out, then the region is wiped over the next ticks and goes back on the free list. */
    public static void release(ServerLevel dungeon, DungeonInstances.Lease lease, @Nullable BoundingBox box) {
        if (box != null) {
            evacuate(dungeon, box);
        }
        DungeonInstances.get(dungeon).release(lease.slot(), lease.gen(), box);
    }

    @Nullable
    public static Layout build(ServerLevel level, GateRank rank, BlockPos origin, RandomSource random) {
        var registry = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Optional<Holder.Reference<StructureTemplatePool>> start = registry.getHolder(ResourceKey.create(Registries.TEMPLATE_POOL,
                ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "dungeon/" + tier(rank) + "/start")));
        if (start.isEmpty()) {
            KhaosNexus.LOGGER.error("Dungeon start pool for {} is missing", tier(rank));
            return null;
        }
        var generator = level.getChunkSource().getGenerator();
        List<StructurePiece> best = null;
        int bestScore = -1;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                    random.nextLong(), new ChunkPos(origin), level, b -> true);
            Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(context, start.get(), Optional.of(DOOR),
                    depth(rank), origin, false, Optional.empty(), 120, PoolAliasLookup.EMPTY,
                    DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
            if (stub.isEmpty()) {
                continue;
            }
            List<StructurePiece> pieces = stub.get().getPiecesBuilder().build().pieces();
            List<StructurePiece> bossRooms = pieces.stream().filter(p -> name(p).contains("boss_chamber")).toList();
            long bosses = bossRooms.size();
            // The boss should sit deep in the dungeon, not right behind the entrance.
            boolean deep = bosses == 1 && Math.sqrt(bossRooms.get(0).getBoundingBox().getCenter().distSqr(origin)) >= 24 + 8 * rank.ordinal();
            // Size first: a boss chamber that capped the dungeon at 3 pieces must not beat a full layout.
            boolean big = pieces.size() >= minPieces(rank);
            int score = (big ? 2000 : 0) + (bosses == 1 ? 1000 : 0) + (deep ? 500 : 0) + Math.min(pieces.size(), minPieces(rank) * 2);
            if (score > bestScore) {
                best = pieces;
                bestScore = score;
            }
            if (deep && pieces.size() >= minPieces(rank)) {
                break;
            }
        }
        if (best == null) {
            return null;
        }
        BoundingBox box = null;
        for (StructurePiece piece : best) {
            if (piece instanceof PoolElementStructurePiece pool) {
                pool.place(level, level.structureManager(), generator, random, BoundingBox.infinite(), origin, false);
                box = box == null ? piece.getBoundingBox() : encapsulate(box, piece.getBoundingBox());
            }
        }
        if (box == null) {
            return null;
        }
        BlockPos spawn = null;
        BlockPos boss = null;
        List<BlockPos> mobs = new ArrayList<>();
        List<BlockPos> loot = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (StructurePiece piece : best) {
            BoundingBox b = piece.getBoundingBox();
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    for (int z = b.minZ(); z <= b.maxZ(); z++) {
                        BlockState state = level.getBlockState(cursor.set(x, y, z));
                        if (!state.is(Blocks.LIGHT)) {
                            continue;
                        }
                        BlockPos pos = cursor.immutable();
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        switch (state.getValue(LightBlock.LEVEL)) {
                            case 1 -> mobs.add(pos);
                            case 2 -> loot.add(pos);
                            case 3 -> boss = pos;
                            case 4 -> spawn = pos;
                            default -> { }
                        }
                    }
                }
            }
        }
        if (spawn == null) {
            spawn = origin.above();
        }
        if (boss == null && !mobs.isEmpty()) {
            // No boss chamber made it in: the combat spot farthest from the entrance hosts the boss.
            BlockPos from = spawn;
            boss = mobs.stream().max((a, c) -> Double.compare(a.distSqr(from), c.distSqr(from))).get();
            mobs.remove(boss);
        }
        KhaosNexus.LOGGER.info("Gate dungeon rank {} at {}: {} pieces, {} mob spots, {} chests, boss {}",
                rank, origin.toShortString(), best.size(), mobs.size(), loot.size(), boss == null ? "none" : boss.toShortString());
        // The entrance template's doorway faces north; turn the arrival view with the piece's rotation.
        float yaw = switch (best.get(0).getRotation()) {
            case CLOCKWISE_90 -> -90.0F;
            case CLOCKWISE_180 -> 0.0F;
            case COUNTERCLOCKWISE_90 -> 90.0F;
            default -> 180.0F;
        };
        return new Layout(spawn, yaw, boss, mobs, loot, box, best.size());
    }

    /** Fills chests, spawns mobs. Returns the boss, which the gate tracks for its clear. */
    @Nullable
    public static Mob populate(ServerLevel level, GateRank rank, Layout layout, RandomSource random, DungeonInstances.Lease lease) {
        DungeonInstances.get(level).setBox(lease.slot(), lease.gen(), layout.box());
        // Leftovers from an older instance in this slot (an entity section that was unloaded during the wipe).
        for (net.minecraft.world.entity.Entity old : level.getEntities((net.minecraft.world.entity.Entity) null,
                net.minecraft.world.phys.AABB.of(layout.box()).inflate(16.0D), e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            old.discard();
        }
        ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "dungeon/" + rank.name().toLowerCase(java.util.Locale.ROOT)));
        for (BlockPos pos : layout.loot()) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            RandomizableContainer.setBlockEntityLootTable(level, random, pos, table);
        }
        List<? extends String> ids = rank.tuning().mobs.get();
        int perSpot = rank.ordinal() >= 3 ? 2 : 1;
        for (BlockPos pos : layout.mobs()) {
            for (int i = 0; i < perSpot && !ids.isEmpty(); i++) {
                EntityType.byString(ids.get(random.nextInt(ids.size())))
                        .ifPresent(type -> {
                            Mob mob = GateArena.spawnMob(level, rank, type, pos, random.nextFloat() * 360.0F, 1.0D);
                            if (mob != null) {
                                mob.getPersistentData().putLong(DungeonInstances.GEN_KEY, lease.gen());
                            }
                        });
            }
        }
        if (layout.boss() == null) {
            return null;
        }
        List<? extends String> bosses = Config.DUNGEON_BOSSES.get();
        String id = bosses.isEmpty() ? "khaosnexus:rift_brute" : bosses.get(Math.min(rank.ordinal(), bosses.size() - 1));
        EntityType<?> type = EntityType.byString(id).orElse(com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_BRUTE.get());
        boolean custom = type.builtInRegistryHolder().key().location().getPath().startsWith("rift_brute");
        Mob boss = GateArena.spawnMob(level, rank, type, layout.boss(), 180.0F, custom ? 3.0D : 1.0D);
        if (boss != null) {
            boss.addTag("khaosnexus_dungeon_boss");
            boss.getPersistentData().putLong(DungeonInstances.GEN_KEY, lease.gen());
            boss.setCustomName(net.minecraft.network.chat.Component.translatable("khaosnexus.dungeon.boss_name",
                    net.minecraft.network.chat.Component.translatable(rank.translationKey()), boss.getType().getDescription()));
        }
        return boss;
    }

    public static void sendIn(ServerPlayer player, ServerLevel dungeon, BlockPos spawn, float yaw, Level from, BlockPos gate) {
        if (player.level() != dungeon) {
            CompoundTag back = new CompoundTag();
            back.putString("Dim", from.dimension().location().toString());
            back.putLong("Pos", gate.asLong());
            player.getPersistentData().put(RETURN, back);
        }
        player.teleportTo(dungeon, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, yaw, 0.0F);
    }

    /** Sends a player back to the gate they came through. False when they have no return point. */
    public static boolean sendBack(ServerPlayer player) {
        CompoundTag back = player.getPersistentData().getCompound(RETURN);
        if (!back.contains("Dim")) {
            return false;
        }
        player.getPersistentData().remove(RETURN);
        ResourceLocation dim = ResourceLocation.tryParse(back.getString("Dim"));
        ServerLevel target = dim == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (target == null) {
            target = player.server.overworld();
        }
        BlockPos gate = BlockPos.of(back.getLong("Pos"));
        Vec3 out = Vec3.atBottomCenterOf(gate).add(0.0D, 0.0D, 2.0D);
        player.teleportTo(target, out.x, gate.getY(), out.z, player.getYRot(), 0.0F);
        player.fallDistance = 0.0F;
        return true;
    }

    /** Every player standing in this instance goes back to their gate. */
    public static void evacuate(ServerLevel dungeon, BoundingBox box) {
        BoundingBox wide = box.inflatedBy(16);
        for (ServerPlayer player : List.copyOf(dungeon.players())) {
            if (wide.isInside(player.blockPosition()) && !sendBack(player)) {
                player.teleportTo(player.server.overworld(), player.server.overworld().getSharedSpawnPos().getX() + 0.5D,
                        player.server.overworld().getSharedSpawnPos().getY(), player.server.overworld().getSharedSpawnPos().getZ() + 0.5D, 0.0F, 0.0F);
            }
        }
    }

    /** Debug view: strips the roof of every column so a layout can be seen from above. */
    public static void cutaway(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.maxY(); y >= box.minY(); y--) {
                    if (!level.getBlockState(c.set(x, y, z)).isAir()) {
                        if (level.getBlockState(c).is(Blocks.DEEPSLATE_BRICKS)) {
                            level.setBlock(c, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static BoundingBox encapsulate(BoundingBox a, BoundingBox b) {
        return new BoundingBox(Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
    }

    private static String name(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece pool ? pool.getElement().toString() : "";
    }
}

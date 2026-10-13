package com.khaoskrew.khaosnexus.gate;

import java.util.List;
import java.util.Optional;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class GateArena {
    private static final double ANNOUNCE_DISTANCE_SQR = 32.0D * 32.0D;

    private GateArena() {}

    public static boolean spawnWave(ServerLevel level, NexusGateBlockEntity gate) {
        GateRank rank = gate.getRank();
        Config.RankValues tuning = rank.tuning();
        List<? extends String> mobIds = tuning.mobs.get();
        if (mobIds.isEmpty()) {
            KhaosNexus.LOGGER.warn("Nexus Gate at {} has an empty mob list for rank {}", gate.getBlockPos().toShortString(), rank);
            return false;
        }
        int count = tuning.mobCount.get();
        int radius = Config.ARENA_RADIUS.get();
        BlockPos origin = gate.getBlockPos();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            String id = mobIds.get(i % mobIds.size());
            Optional<EntityType<?>> type = EntityType.byString(id);
            if (type.isEmpty()) {
                KhaosNexus.LOGGER.warn("Nexus Gate skipped unknown mob id {}", id);
                continue;
            }
            BlockPos anchor = findSpawn(level, origin, i, count, radius);
            double dx = origin.getX() + 0.5D - (anchor.getX() + 0.5D);
            double dz = origin.getZ() + 0.5D - (anchor.getZ() + 0.5D);
            float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            Mob mob = spawnMob(level, rank, type.get(), anchor, yaw, 1.0D);
            if (mob == null) {
                continue;
            }
            gate.rememberMob(mob.getUUID(), anchor);
            spawned++;
        }
        if (spawned == 0) {
            return false;
        }
        level.playSound(null, origin, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.0F);
        level.sendParticles(ParticleTypes.PORTAL,
                origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D,
                40, 0.4D, 0.6D, 0.4D, 0.15D);
        KhaosNexus.LOGGER.info("Nexus Gate at {} opened at rank {} with {} mobs", origin.toShortString(), rank, spawned);
        return true;
    }

    /** Spawns one rank-scaled gate mob. Returns null when the type is not a mob or the spawn was cancelled. */
    @org.jetbrains.annotations.Nullable
    public static Mob spawnMob(ServerLevel level, GateRank rank, EntityType<?> type, BlockPos anchor, float yaw, double extraHealth) {
        Config.RankValues tuning = rank.tuning();
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            KhaosNexus.LOGGER.warn("Nexus Gate skipped non-mob id {}", EntityType.getKey(type));
            return null;
        }
        mob.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, yaw, 0.0F);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(anchor), MobSpawnType.EVENT, null);
        if (mob.isSpawnCancelled()) {
            mob.discard();
            return null;
        }
        scale(mob, tuning.healthMultiplier.get() * extraHealth, tuning.damageMultiplier.get());
        if (mob instanceof com.khaoskrew.khaosnexus.mob.RiftMob rift) {
            rift.setGateRank(rank);
        }
        mob.setPersistenceRequired();
        mob.setCustomName(Component.translatable("khaosnexus.gate.mob_name", Component.translatable(rank.translationKey())));
        if (!level.addFreshEntity(mob)) {
            return null;
        }
        return mob;
    }

    public static void leash(Entity entity, BlockPos anchor) {
        double limit = Config.leashRadius();
        if (entity.distanceToSqr(Vec3.atCenterOf(anchor)) <= limit * limit) {
            return;
        }
        entity.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
    }

    public static void dropReward(ServerLevel level, BlockPos pos, GateRank rank) {
        Config.RankValues tuning = rank.tuning();
        ResourceLocation itemId = ResourceLocation.tryParse(tuning.rewardItem.get());
        int rewardCount = tuning.rewardCount.get();
        if (itemId != null && rewardCount > 0) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, rewardCount);
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            } else {
                KhaosNexus.LOGGER.warn("Nexus Gate reward item {} is not a registered item", itemId);
            }
        }
        int xp = tuning.rewardXp.get();
        if (xp > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos).add(0.0D, 0.5D, 0.0D), xp);
        }
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.1F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                16, 0.4D, 0.4D, 0.4D, 0.02D);
        KhaosNexus.LOGGER.info("Nexus Gate at {} closed at rank {}", pos.toShortString(), rank);
    }

    public static void announce(ServerLevel level, BlockPos pos, Component message, boolean actionBar) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= ANNOUNCE_DISTANCE_SQR) {
                player.displayClientMessage(message, actionBar);
            }
        }
    }

    private static void scale(Mob mob, double healthMultiplier, double damageMultiplier) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double max = Math.max(1.0D, health.getBaseValue() * healthMultiplier);
            health.setBaseValue(max);
            mob.setHealth((float) max);
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(Math.max(0.0D, damage.getBaseValue() * damageMultiplier));
        }
    }

    private static BlockPos findSpawn(ServerLevel level, BlockPos origin, int index, int count, int radius) {
        double angle = (Math.PI * 2.0D * index) / Math.max(1, count);
        int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
        int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 3; dy >= -6; dy--) {
            cursor.set(x, origin.getY() + dy, z);
            BlockPos below = cursor.below();
            BlockState ground = level.getBlockState(below);
            if (ground.isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
                    && level.getBlockState(cursor.above()).getCollisionShape(level, cursor.above()).isEmpty()) {
                return cursor.immutable();
            }
        }
        return origin.above();
    }
}

package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.bounty.Bounties;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;

public final class SpireService {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "spire"));
    public static final String BOSS = "khaosnexus_spire";
    public static final String FLOOR = "khaosnexus_spire_floor";

    private SpireService() {}

    public static void enter(ServerPlayer player) {
        ServerLevel spire = player.server.getLevel(DIMENSION);
        if (spire == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.missing"), false);
            return;
        }
        SpireData data = SpireData.get(player.server.overworld());
        HunterProfile profile = HunterRanks.profile(player);
        if (profile.spireSeason != data.season) {
            profile.spireSeason = data.season;
            profile.spireFloor = 1;
            profile.spireCheckpoint = 1;
        }
        int floor = profile.spireCheckpoint;
        for (ServerPlayer member : PartyService.onlineMembers(player)) {
            floor = Math.max(floor, HunterRanks.profile(member).spireCheckpoint);
        }
        generate(spire, floor, data.season);
        BlockPos pos = origin(floor, data.season);
        player.teleportTo(spire, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.spire.entered", floor), false);
    }

    public static void leave(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);
        player.displayClientMessage(Component.translatable("khaosnexus.spire.left"), false);
    }

    public static void checkpoint(ServerPlayer player) {
        if (player.server.getLevel(DIMENSION) == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.missing"), false);
            return;
        }
        HunterProfile profile = HunterRanks.profile(player);
        profile.spireFloor = Math.max(profile.spireFloor, profile.spireCheckpoint);
        enterAt(player, profile.spireCheckpoint);
    }

    public static void newSeason(ServerPlayer admin) {
        SpireData data = SpireData.get(admin.server.overworld());
        data.season++;
        data.bestFloor = 0;
        data.holder = "";
        data.setDirty();
        admin.displayClientMessage(Component.translatable("khaosnexus.spire.season", data.season), false);
    }

    public static void status(ServerPlayer player) {
        SpireData data = SpireData.get(player.server.overworld());
        HunterProfile profile = HunterRanks.profile(player);
        player.displayClientMessage(Component.translatable(
                "khaosnexus.spire.status",
                profile.spireFloor,
                profile.spireCheckpoint,
                data.bestFloor,
                data.holder.isEmpty() ? Component.translatable("khaosnexus.spire.nobody") : Component.literal(data.holder)), false);
    }

    public static void onBossKilled(ServerLevel level, Mob boss, ServerPlayer killer) {
        if (!boss.getPersistentData().getBoolean(BOSS) || killer == null) {
            return;
        }
        int floor = boss.getPersistentData().getInt(FLOOR);
        SpireData data = SpireData.get(killer.server.overworld());
        if (floor > data.bestFloor) {
            data.bestFloor = floor;
            data.holder = killer.getGameProfile().getName();
            data.setDirty();
        }
        for (ServerPlayer player : PartyService.onlineMembers(killer)) {
            if (player.level() != level || player.distanceToSqr(boss) > 48.0D * 48.0D) {
                if (player != killer) {
                    continue;
                }
            }
            HunterProfile profile = HunterRanks.profile(player);
            profile.spireBest = Math.max(profile.spireBest, floor);
            if (floor >= profile.spireFloor) {
                profile.spireFloor = floor + 1;
                int interval = Config.SPIRE_CHECKPOINT.get();
                if (floor % interval == 0) {
                    profile.spireCheckpoint = floor;
                    player.displayClientMessage(Component.translatable("khaosnexus.spire.checkpoint", floor), false);
                    Titles.unlock(player, Titles.SPIRE_CLIMBER);
                }
            }
            Bounties.onSpireFloor(player, floor);
            player.displayClientMessage(Component.translatable("khaosnexus.spire.cleared", floor), false);
            HunterRanks.sync(player);
        }
        if (killer.level().dimension() == DIMENSION) {
            enterAt(killer, floor + 1);
        }
    }

    public static void kickOnDeath(ServerPlayer player) {
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        int weakness = Config.SPIRE_DEATH_WEAKNESS.get();
        if (weakness > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 0));
        }
        player.displayClientMessage(Component.translatable("khaosnexus.spire.death"), false);
        leave(player);
    }

    public static BlockPos origin(int floor, int season) {
        int x = Math.floorMod(season, 40) * 96;
        return new BlockPos(x, 64, Math.max(1, floor) * 48);
    }

    public static void generate(ServerLevel level, int floor, int season) {
        BlockPos origin = origin(floor, season);
        level.getChunkAt(origin);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.setBlockAndUpdate(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(dx, 2, dz), Blocks.AIR.defaultBlockState());
            }
        }
        if (floor % Config.SPIRE_CHECKPOINT.get() == 0) {
            level.setBlockAndUpdate(origin.offset(-5, 0, -5), Blocks.GOLD_BLOCK.defaultBlockState());
        }
        Mob boss = EntityType.ZOMBIE.create(level);
        if (boss == null) {
            return;
        }
        BlockPos spawn = origin.offset(0, 0, 3);
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 180.0F, 0.0F);
        EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(20.0D + floor * Config.SPIRE_HEALTH_PER_FLOOR.get());
            boss.setHealth((float) health.getValue());
        }
        boss.setCustomName(Component.translatable("khaosnexus.spire.boss", floor));
        boss.setPersistenceRequired();
        boss.getPersistentData().putBoolean(BOSS, true);
        boss.getPersistentData().putInt(FLOOR, floor);
        level.addFreshEntity(boss);
    }

    private static void enterAt(ServerPlayer player, int floor) {
        ServerLevel spire = player.server.getLevel(DIMENSION);
        if (spire == null) {
            return;
        }
        int season = SpireData.get(player.server.overworld()).season;
        generate(spire, floor, season);
        BlockPos pos = origin(floor, season);
        player.teleportTo(spire, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), 0.0F);
    }
}

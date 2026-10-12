package com.khaoskrew.khaosnexus.raid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.bounty.Bounties;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.neoforged.neoforge.event.EventHooks;

public final class RaidService {
    public static final String BOSS = "khaosnexus_raid";
    private static final Map<UUID, Map<UUID, Float>> DAMAGE = new ConcurrentHashMap<>();

    private RaidService() {}

    public static boolean start(ServerPlayer player, BlockPos pos) {
        int size = PartyService.size(player);
        if (size < Config.RAID_MIN_PARTY.get()) {
            player.displayClientMessage(Component.translatable("khaosnexus.raid.need_party", Config.RAID_MIN_PARTY.get()), false);
            return false;
        }
        if (size > Config.RAID_MAX_PARTY.get()) {
            player.displayClientMessage(Component.translatable("khaosnexus.raid.too_many", Config.RAID_MAX_PARTY.get()), false);
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Ravager boss = EntityType.RAVAGER.create(level);
        if (boss == null) {
            return false;
        }
        BlockPos spawn = pos.above();
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double scaled = health.getBaseValue() * (1.0D + Config.RAID_HEALTH_SCALE.get() * (size - 1));
            health.setBaseValue(scaled);
            boss.setHealth((float) health.getValue());
        }
        boss.setCustomName(Component.translatable("khaosnexus.raid.boss"));
        boss.setPersistenceRequired();
        boss.getPersistentData().putBoolean(BOSS, true);
        if (!level.addFreshEntity(boss)) {
            return false;
        }
        DAMAGE.put(boss.getUUID(), new ConcurrentHashMap<>());
        for (ServerPlayer member : PartyService.onlineMembers(player)) {
            member.displayClientMessage(Component.translatable("khaosnexus.raid.started", size), false);
        }
        return true;
    }

    public static void recordDamage(Mob boss, ServerPlayer player, float amount) {
        if (!boss.getPersistentData().getBoolean(BOSS) || amount <= 0.0F) {
            return;
        }
        DAMAGE.computeIfAbsent(boss.getUUID(), ignored -> new ConcurrentHashMap<>())
                .merge(player.getUUID(), amount, Float::sum);
    }

    public static void onBossKilled(ServerLevel level, Mob boss, ServerPlayer killer) {
        if (!boss.getPersistentData().getBoolean(BOSS)) {
            return;
        }
        Map<UUID, Float> damage = DAMAGE.remove(boss.getUUID());
        if (damage == null) {
            damage = Map.of();
        }
        long week = level.getDayTime() / 24000L / 7L;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean dealt = damage.getOrDefault(player.getUUID(), 0.0F) > 0.0F || player == killer;
            if (!dealt) {
                continue;
            }
            HunterProfile profile = HunterRanks.profile(player);
            if (profile.raidWeek == week) {
                player.displayClientMessage(Component.translatable("khaosnexus.raid.lockout"), false);
                continue;
            }
            profile.raidWeek = week;
            HunterRanks.sync(player);
            placeLoot(level, player);
            Bounties.onRaidClear(player);
            Titles.unlock(player, Titles.RAID_VICTOR);
            player.displayClientMessage(Component.translatable("khaosnexus.raid.loot"), false);
        }
    }

    private static void placeLoot(ServerLevel level, ServerPlayer player) {
        BlockPos spot = player.blockPosition().relative(player.getDirection());
        if (!level.getBlockState(spot).isAir()) {
            spot = player.blockPosition().above();
        }
        level.setBlockAndUpdate(spot, ModRegistries.PERSONAL_LOOT.get().defaultBlockState());
        if (level.getBlockEntity(spot) instanceof PersonalLootBlockEntity loot) {
            loot.setReward(player.getUUID(), Config.RAID_REWARD.get(), Config.RAID_REWARD_COUNT.get());
        }
    }
}

package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.bounty.Bounties;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The Nexus Spire: floors of {@code levelsPerFloor} boss arenas, each floor opening with a safe town (level 0).
 * Beating a level's boss lights the arena's portal to the next level; the last level's portal leads to the next floor's town.
 * Profile mapping: spireFloor = floor in progress, endgame "SpireLevel" = next level on it, spireCheckpoint = last town
 * reached, spireBest = highest floor fully cleared. Leaderboards and the server record use total levels cleared.
 */
public final class SpireService {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "spire"));
    public static final String BOSS = "khaosnexus_spire";
    public static final String FLOOR = "khaosnexus_spire_floor";
    public static final String LEVEL = "khaosnexus_spire_level";
    public static final String LEVEL_KEY = "SpireLevel";
    private static final String MIGRATED = "SpireV2";

    private SpireService() {}

    public static int levels() {
        return Config.SPIRE_LEVELS.get();
    }

    /** Total levels cleared when standing before (floor, level). */
    public static int stage(int floor, int level) {
        return (Math.max(1, floor) - 1) * levels() + Math.max(1, level);
    }

    public static int nextLevel(HunterProfile p) {
        return Math.max(1, p.endgame.getInt(LEVEL_KEY));
    }

    /** One-time conversion from the single-boss floors: old floors become levels. */
    public static void migrate(HunterProfile p) {
        if (p.endgame.getBoolean(MIGRATED)) {
            return;
        }
        p.endgame.putBoolean(MIGRATED, true);
        if (p.spireFloor <= 1 && p.spireBest <= 0) {
            p.endgame.putInt(LEVEL_KEY, 1);
            return;
        }
        int oldCleared = Math.max(p.spireBest, p.spireFloor - 1);
        p.spireBest = oldCleared / levels();
        p.spireCheckpoint = Math.max(1, (Math.max(1, p.spireCheckpoint) - 1) / levels() + 1);
        p.spireFloor = p.spireCheckpoint;
        p.endgame.putInt(LEVEL_KEY, 1);
    }

    /** Arena or town origin. Seasons are 1024 blocks apart on X, floors 256 apart on Z, levels 64 apart on X. */
    public static BlockPos origin(int floor, int level, int season) {
        int x = Math.floorMod(season, 40) * 1024 + Math.max(0, level) * 64;
        return new BlockPos(x, 64, Math.max(1, floor) * 256);
    }

    public static void enter(ServerPlayer player) {
        ServerLevel spire = player.server.getLevel(DIMENSION);
        if (spire == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.missing"), false);
            return;
        }
        if (!canEnter(player)) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.rank_low",
                    Component.translatable(minRank().translationKey()), Component.translatable(HunterRanks.rankOf(player).translationKey()))
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
            return;
        }
        SpireData data = SpireData.get(player.server.overworld());
        HunterProfile profile = HunterRanks.profile(player);
        migrate(profile);
        if (profile.spireSeason != data.season) {
            profile.spireSeason = data.season;
            profile.spireFloor = 1;
            profile.spireCheckpoint = 1;
            profile.endgame.putInt(LEVEL_KEY, 1);
        }
        int floor = profile.spireCheckpoint;
        for (ServerPlayer member : PartyService.onlineMembers(player)) {
            floor = Math.max(floor, HunterRanks.profile(member).spireCheckpoint);
        }
        com.khaoskrew.khaosnexus.grave.SafeGraves.saveSpireEntry(player);
        goTo(player, spire, floor, resumeLevel(profile, floor), data.season);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.spire.entered", floor), false);
    }

    public static com.khaoskrew.khaosnexus.gate.GateRank minRank() {
        return com.khaoskrew.khaosnexus.gate.GateRank.byName(Config.SPIRE_MIN_RANK.get().trim().toUpperCase(java.util.Locale.ROOT));
    }

    /** Creative players always pass; others need at least {@code [spire] minRank}. */
    public static boolean canEnter(ServerPlayer player) {
        return player.isCreative() || HunterRanks.rankOf(player).ordinal() >= minRank().ordinal();
    }

    public static void leave(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        ServerLevel target = overworld;
        var entry = com.khaoskrew.khaosnexus.grave.SafeGraves.anchorOf(player);
        if (entry != null && player.server.getLevel(entry.dim()) != null && entry.dim() != DIMENSION) {
            target = player.server.getLevel(entry.dim());
            spawn = entry.pos();
        }
        player.getPersistentData().remove(com.khaoskrew.khaosnexus.grave.SafeGraves.SPIRE_ENTRY);
        com.khaoskrew.khaosnexus.dungeon.DimensionSafety.teleport(player, target.dimension(), spawn, player.getYRot(), target.dimension() != DIMENSION);
        player.displayClientMessage(Component.translatable("khaosnexus.spire.left"), false);
    }

    public static void checkpoint(ServerPlayer player) {
        ServerLevel spire = player.server.getLevel(DIMENSION);
        if (spire == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.missing"), false);
            return;
        }
        HunterProfile profile = HunterRanks.profile(player);
        migrate(profile);
        goTo(player, spire, profile.spireCheckpoint, Config.SPIRE_TOWNS.get() ? 0 : 1, SpireData.get(player.server.overworld()).season);
    }

    /** Where re-entering a floor lands: the next unbeaten level when the hunter stopped mid-floor (saved on the profile, so it survives restarts), otherwise the town. */
    public static int resumeLevel(HunterProfile profile, int floor) {
        if (floor == profile.spireFloor && nextLevel(profile) > 1) {
            return nextLevel(profile);
        }
        return Config.SPIRE_TOWNS.get() ? 0 : 1;
    }

    public static void newSeason(ServerPlayer admin) {
        SpireData data = SpireData.get(admin.server.overworld());
        data.season++;
        data.bestFloor = 0;
        data.holder = "";
        data.setDirty();
        admin.displayClientMessage(Component.translatable("khaosnexus.spire.season", data.season), false);
    }

    public static Component where(int stage) {
        int floor = (stage - 1) / levels() + 1;
        int level = (stage - 1) % levels() + 1;
        return Component.translatable("khaosnexus.spire.where", floor, level);
    }

    public static void status(ServerPlayer player) {
        SpireData data = SpireData.get(player.server.overworld());
        HunterProfile profile = HunterRanks.profile(player);
        migrate(profile);
        player.displayClientMessage(Component.translatable(
                "khaosnexus.spire.status2",
                profile.spireFloor, nextLevel(profile), levels(),
                profile.spireCheckpoint,
                profile.spireBest,
                data.bestFloor <= 0 ? Component.literal("-") : where(data.bestFloor),
                data.holder.isEmpty() ? Component.translatable("khaosnexus.spire.nobody") : Component.literal(data.holder)), false);
    }

    /** Builds (if needed) and moves the player to a floor's town (level 0) or a level arena. */
    public static void goTo(ServerPlayer player, ServerLevel spire, int floor, int level, int season) {
        BlockPos o = origin(floor, level, season);
        boolean town = level == 0;
        if (town) {
            buildTown(spire, floor, season, HunterRanks.rankOf(player));
            HunterProfile p = HunterRanks.profile(player);
            if (floor > p.spireCheckpoint && (floor - 1) % Config.SPIRE_CHECKPOINT.get() == 0) {
                p.spireCheckpoint = floor;
                player.displayClientMessage(Component.translatable("khaosnexus.spire.checkpoint", floor), false);
                Titles.unlock(player, Titles.SPIRE_CLIMBER);
            }
            p.spireFloor = Math.max(p.spireFloor, floor);
            HunterRanks.sync(player);
        } else if (!bossAlive(spire, o)) {
            generate(spire, floor, level, season);
        }
        BlockPos at = SpireBuilder.arrival(o, town);
        com.khaoskrew.khaosnexus.dungeon.DimensionSafety.teleportExact(player, spire, net.minecraft.world.phys.Vec3.atBottomCenterOf(at), 180.0F);
        player.displayClientMessage(town ? Component.translatable("khaosnexus.spire.town", floor)
                : Component.translatable("khaosnexus.spire.level_enter", floor, level, levels()), true);
    }

    static boolean bossAlive(ServerLevel spire, BlockPos o) {
        return !spire.getEntitiesOfClass(Mob.class, new AABB(o).inflate(SpireBuilder.ARENA_HALF, 10, SpireBuilder.ARENA_HALF),
                m -> m.isAlive() && m.getPersistentData().getBoolean(BOSS)).isEmpty();
    }

    public static void buildTown(ServerLevel spire, int floor, int season, com.khaoskrew.khaosnexus.gate.GateRank rank) {
        BlockPos o = origin(floor, 0, season);
        SpireBuilder.buildTown(spire, o);
        placePortal(spire, SpireBuilder.portalPos(o, true), floor, 1, SpireBuilder.themeFor(floor).rank(), true);
        SpireVendors.spawn(spire, o, floor, rank);
    }

    private static void placePortal(ServerLevel spire, BlockPos pos, int floor, int level, com.khaoskrew.khaosnexus.gate.GateRank rank, boolean lit) {
        spire.setBlock(pos, ModRegistries.SPIRE_PORTAL.get().defaultBlockState(), Block.UPDATE_ALL);
        if (spire.getBlockEntity(pos) instanceof SpirePortalBlockEntity portal) {
            portal.configure(floor, level, rank, lit);
        }
    }

    /** Builds a level arena with its boss and an unlit portal to the next level (or next floor's town). */
    public static Mob generate(ServerLevel spire, int floor, int level, int season) {
        BlockPos o = origin(floor, level, season);
        SpireBuilder.Theme theme = SpireBuilder.themeFor(floor);
        for (Mob old : spire.getEntitiesOfClass(Mob.class, new AABB(o).inflate(SpireBuilder.ARENA_HALF + 2, 12, SpireBuilder.ARENA_HALF + 2),
                m -> m.getPersistentData().getBoolean(BOSS))) {
            old.discard();
        }
        SpireBuilder.buildArena(spire, o, theme);
        boolean last = level >= levels();
        placePortal(spire, SpireBuilder.portalPos(o, false), last ? floor + 1 : floor, last ? (Config.SPIRE_TOWNS.get() ? 0 : 1) : level + 1, theme.rank(), false);
        ResourceLocation bossId = SpireBuilder.bossFor(theme, level, levels());
        Entity created = BuiltInRegistries.ENTITY_TYPE.get(bossId).create(spire);
        if (!(created instanceof Mob boss)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        BlockPos spawn = o.offset(0, 0, -3);
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        boss.getPersistentData().putBoolean(com.khaoskrew.khaosnexus.compat.ApothCompat.BOSS_FLAG, true);
        EventHooks.finalizeMobSpawn(boss, spire, spire.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null);
        int stage = stage(floor, level);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            com.khaoskrew.khaosnexus.endgame.BigHealth.set(boss, Math.max(health.getBaseValue(), 20.0D + stage * Config.SPIRE_HEALTH_PER_FLOOR.get()));
        }
        boss.setCustomName(Component.translatable("khaosnexus.spire.boss2", floor, level));
        if (last) {
            com.khaoskrew.khaosnexus.endgame.InfiniteSpire.apply(boss, season, floor);
        } else {
            double s = com.khaoskrew.khaosnexus.endgame.InfiniteSpire.scale(floor);
            if (s != 1.0D && boss.getAttribute(Attributes.MAX_HEALTH) != null) {
                com.khaoskrew.khaosnexus.endgame.BigHealth.set(boss, com.khaoskrew.khaosnexus.endgame.BigHealth.effectiveMax(boss) * s);
                AttributeInstance atk = boss.getAttribute(Attributes.ATTACK_DAMAGE);
                if (atk != null) {
                    atk.setBaseValue(atk.getBaseValue() * s);
                }
            }
        }
        boss.setPersistenceRequired();
        boss.getPersistentData().putBoolean(BOSS, true);
        boss.getPersistentData().putInt(FLOOR, floor);
        boss.getPersistentData().putInt(LEVEL, level);
        spire.addFreshEntity(boss);
        return boss;
    }

    public static void onBossKilled(ServerLevel level, Mob boss, ServerPlayer killer) {
        if (!boss.getPersistentData().getBoolean(BOSS) || killer == null) {
            return;
        }
        int floor = boss.getPersistentData().getInt(FLOOR);
        int lvl = Math.max(1, boss.getPersistentData().getInt(LEVEL));
        int stage = stage(floor, lvl);
        boolean lastLevel = lvl >= levels();
        SpireData data = SpireData.get(killer.server.overworld());
        if (stage > data.bestFloor) {
            data.bestFloor = stage;
            data.holder = killer.getGameProfile().getName();
            data.setDirty();
        }
        for (net.minecraft.world.item.ItemStack bonus : com.khaoskrew.khaosnexus.endgame.InfiniteSpire.rewards(boss)) {
            if (!killer.getInventory().add(bonus)) {
                killer.drop(bonus, false);
            }
        }
        for (ServerPlayer player : PartyService.onlineMembers(killer)) {
            if (player != killer && (player.level() != level || player.distanceToSqr(boss) > 48.0D * 48.0D)) {
                continue;
            }
            HunterProfile profile = HunterRanks.profile(player);
            migrate(profile);
            com.khaoskrew.khaosnexus.endgame.Leaderboards.recordSpire(player, data.season, stage);
            boolean progressed = floor > profile.spireFloor || (floor == profile.spireFloor && lvl >= nextLevel(profile));
            if (progressed) {
                if (lastLevel) {
                    profile.spireFloor = floor + 1;
                    profile.endgame.putInt(LEVEL_KEY, 1);
                    profile.spireBest = Math.max(profile.spireBest, floor);
                    com.khaoskrew.khaosnexus.compat.ProgressHooks.spireFloor(player, floor);
                    Bounties.onSpireFloor(player, floor);
                    player.displayClientMessage(Component.translatable("khaosnexus.spire.cleared", floor), false);
                } else {
                    profile.spireFloor = floor;
                    profile.endgame.putInt(LEVEL_KEY, lvl + 1);
                }
            }
            player.displayClientMessage(Component.translatable("khaosnexus.spire.level_cleared", floor, lvl), true);
            HunterRanks.sync(player);
        }
        BlockPos portalPos = SpireBuilder.portalPos(origin(floor, lvl, data.season), false);
        if (level.getBlockEntity(portalPos) instanceof SpirePortalBlockEntity portal) {
            portal.setLit(true);
            level.playSound(null, portalPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
    }

    /** A hunter steps through a portal. Only lit portals work, and only to levels the hunter (or the party) has opened. */
    public static void usePortal(ServerPlayer player, SpirePortalBlockEntity portal) {
        if (!portal.isLit()) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.portal_dormant"), true);
            return;
        }
        ServerLevel spire = player.server.getLevel(DIMENSION);
        if (spire == null) {
            return;
        }
        HunterProfile p = HunterRanks.profile(player);
        migrate(p);
        int floor = portal.targetFloor();
        int level = portal.targetLevel();
        // Party members who did not land the kill still follow: being next to a lit portal is enough.
        if (floor > p.spireFloor || (floor == p.spireFloor && level > nextLevel(p))) {
            p.spireFloor = floor;
            p.endgame.putInt(LEVEL_KEY, Math.max(1, level));
        }
        if (level == 1 && floor == p.spireFloor && nextLevel(p) > 1) {
            level = nextLevel(p); // the town portal skips levels this hunter already beat
        }
        goTo(player, spire, floor, level, SpireData.get(player.server.overworld()).season);
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
}

package com.khaoskrew.khaosnexus.endgame;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class EndgameGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void weeklyModifiersRollAndApply(GameTestHelper helper) {
        helper.assertTrue(WeeklyModifiers.all().size() >= 6, "bundled weekly modifiers load");
        long w = WeeklyModifiers.week(1_760_000_000_000L);
        helper.assertTrue(WeeklyModifiers.week(1_760_000_000_000L + 3600_000L) == w, "same week within an hour");
        List<? extends String> force = Config.WEEKLY_FORCE.get();
        String ranks = Config.WEEKLY_RANKS.get();
        try {
            Config.WEEKLY_FORCE.set(List.of());
            var a = WeeklyModifiers.active(w);
            helper.assertTrue(a.equals(WeeklyModifiers.active(w)), "the roll is deterministic per week");
            helper.assertTrue(a.size() == Config.WEEKLY_COUNT.get() && a.stream().distinct().count() == a.size(), "count distinct picks");
            Config.WEEKLY_FORCE.set(List.of("khaosnexus:ironhide"));
            Config.WEEKLY_RANKS.set("S");
            helper.assertTrue(WeeklyModifiers.active().size() == 1, "forced modifier list");
            helper.assertTrue(WeeklyModifiers.applies(GateRank.S) && !WeeklyModifiers.applies(GateRank.A), "rank filter");
            Mob plain = GateArena.spawnMob(helper.getLevel(), GateRank.A, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(1, 2, 1)), 0F, 1.0D);
            Mob boosted = GateArena.spawnMob(helper.getLevel(), GateRank.S, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(3, 2, 3)), 0F, 1.0D);
            double ratio = boosted.getMaxHealth() / plain.getMaxHealth();
            double expected = GateRank.S.tuning().healthMultiplier.get() / GateRank.A.tuning().healthMultiplier.get() * 1.6D;
            helper.assertTrue(Math.abs(ratio - expected) < 0.01D, "Ironhide adds 60% health only in S gates");
            helper.assertTrue(WeeklyModifiers.scaleXp(GateRank.S, 100) == 120 && WeeklyModifiers.scaleXp(GateRank.A, 100) == 100, "xp bonus by rank");
            helper.assertTrue(WeeklyModifiers.bonusCores(GateRank.S) == 1, "bonus rift cores");
            plain.discard();
            boosted.discard();
            Config.WEEKLY_FORCE.set(List.of("khaosnexus:missing"));
            helper.assertTrue(WeeklyModifiers.active().isEmpty(), "unknown forced ids are ignored");
        } finally {
            Config.WEEKLY_FORCE.set(force);
            Config.WEEKLY_RANKS.set(ranks);
        }
        helper.assertTrue(WeeklyModifiers.parse(ResourceLocation.parse("t:x"), new com.google.gson.JsonObject()).mobHealth() == 1.0D, "missing fields default to 1");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 100)
    public static void redGatesAndDungeonBreaks(GameTestHelper helper) {
        double chance = Config.RED_GATE_CHANCE.get();
        try {
            Config.RED_GATE_CHANCE.set(1.0D);
            helper.assertTrue(RedGates.rollRed(GateRank.S, net.minecraft.util.RandomSource.create(1)), "S can roll red");
            helper.assertFalse(RedGates.rollRed(GateRank.C, net.minecraft.util.RandomSource.create(1)), "C is not in redGateRanks");
            Config.RED_GATE_CHANCE.set(0.0D);
            helper.assertFalse(RedGates.rollRed(GateRank.S, net.minecraft.util.RandomSource.create(1)), "chance 0 never rolls red");
        } finally {
            Config.RED_GATE_CHANCE.set(chance);
        }
        helper.assertTrue(RedGates.breakDelay(true) * 2 == RedGates.breakDelay(false), "red gates break twice as fast");
        helper.assertTrue(RedGates.canBreak(GateRank.S) && !RedGates.canBreak(GateRank.E), "break min rank");
        helper.assertTrue(RedGates.scaleXp(80, true) == 160 && RedGates.scaleXp(80, false) == 80, "red xp multiplier");

        Mob boss = GateArena.spawnMob(helper.getLevel(), GateRank.S, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(1, 2, 1)), 0F, 1.0D, true);
        float before = boss.getMaxHealth();
        double dmg = boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue();
        RedGates.empower(boss);
        helper.assertTrue(Math.abs(boss.getMaxHealth() - before * Config.RED_BOSS_HEALTH.get()) < 0.01D, "red boss health");
        helper.assertTrue(Math.abs(boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue()
                - dmg * Config.RED_BOSS_DAMAGE.get()) < 1e-6, "red boss damage");
        boss.discard();

        BlockPos gatePos = new BlockPos(2, 2, 2);
        helper.setBlock(gatePos, com.khaoskrew.khaosnexus.registry.ModRegistries.NEXUS_GATE.get());
        com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity gate = helper.getBlockEntity(gatePos);
        gate.setRank(GateRank.D);
        gate.setRed(true);
        var tag = gate.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tag.getBoolean("Red"), "red flag is saved");
        helper.assertTrue(gate.breakOpen(helper.getLevel()), "break spawns the wave in the world");
        helper.assertTrue(gate.isBroken() && gate.isActive(), "broken gate is active");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void leaderboardsRankAndKeepBest(GameTestHelper helper) {
        Leaderboards lb = new Leaderboards();
        String clear = Leaderboards.clearBoard(GateRank.S);
        java.util.UUID a = java.util.UUID.randomUUID();
        java.util.UUID b = java.util.UUID.randomUUID();
        lb.submit(clear, a, "Alpha", 2400);
        lb.submit(clear, b, "Beta", 1800);
        lb.submit(clear, a, "Alpha", 3000);
        helper.assertTrue(lb.top(clear, 10).get(0).name().equals("Beta"), "fastest time first");
        helper.assertTrue(lb.top(clear, 10).get(1).value() == 2400, "a slower run does not replace a best time");
        String spire = Leaderboards.spireBoard(1);
        lb.submit(spire, a, "Alpha", 12);
        lb.submit(spire, b, "Beta", 30);
        helper.assertTrue(lb.top(spire, 1).get(0).name().equals("Beta"), "highest floor first");
        String weekly = Leaderboards.weeklyBoard(5);
        lb.increment(weekly, a, "Alpha");
        lb.increment(weekly, a, "Alpha");
        helper.assertTrue(lb.top(weekly, 1).get(0).value() == 2, "weekly counter");
        var tag = lb.save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(tag.getCompound("Boards").getList(clear, 10).size() == 2, "boards save");
        for (int i = 0; i < 60; i++) {
            lb.submit(clear, java.util.UUID.randomUUID(), "P" + i, 5000 + i);
        }
        helper.assertTrue(lb.top(clear, 100).size() == 50, "boards trim to 50");
        helper.assertTrue(Leaderboards.format(clear, lb.top(clear, 2)).getString().contains("1:30"), "times print as m:ss");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void infiniteSpireScalesAndAffixes(GameTestHelper helper) {
        int from = Config.SPIRE_SCALE_FROM.get();
        helper.assertTrue(InfiniteSpire.scale(from) == 1.0D, "no extra scaling up to spireScaleFrom");
        helper.assertTrue(Math.abs(InfiniteSpire.scale(from + 10) - (1.0D + 10 * Config.SPIRE_SCALE_PER_FLOOR.get())) < 1e-9, "linear per-floor scaling");
        helper.assertTrue(InfiniteSpire.scale(10_000_000 / 100) <= Config.SPIRE_SCALE_CAP.get(), "scaling is capped");
        helper.assertTrue(JsonPool.SPIRE_AFFIXES.all().size() >= 5, "bundled affixes load");
        helper.assertTrue(InfiniteSpire.affix(1, 11) == null, "non-boss floors have no affix");
        ResourceLocation a = InfiniteSpire.affix(1, 40);
        helper.assertTrue(a != null && a.equals(InfiniteSpire.affix(1, 40)), "boss-floor affix is deterministic");
        helper.assertFalse("titanic".equals(String.valueOf(InfiniteSpire.affix(1, 10)).replace("khaosnexus:", "")), "min_floor respected");
        Mob boss = EntityType.ZOMBIE.create(helper.getLevel());
        boss.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        double hp = boss.getMaxHealth();
        InfiniteSpire.apply(boss, 1, from + 9);
        helper.assertTrue(boss.getMaxHealth() > hp, "deep floors are tougher");
        boss.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void aurasUnlockByAchievement(GameTestHelper helper) {
        helper.assertTrue(JsonPool.AURAS.all().size() >= 6, "bundled auras load");
        var p = new com.khaoskrew.khaosnexus.hunter.HunterProfile();
        helper.assertTrue(Auras.unlockedIds(p).isEmpty(), "a new hunter has no auras");
        p.addTitle(RedGates.TITLE);
        p.spireBest = 100;
        p.endgame.putInt("BossRushWins", 1);
        var ids = Auras.unlockedIds(p).stream().map(ResourceLocation::getPath).toList();
        helper.assertTrue(ids.contains("red_gate") && ids.contains("spire_100") && ids.contains("boss_rush"), "title, floor and stat unlocks");
        helper.assertFalse(ids.contains("hunter_s") || ids.contains("national"), "rank and prestige still locked");
        for (var a : JsonPool.AURAS.all().values()) {
            helper.assertTrue(Auras.particle(a) != null, "every bundled aura has a simple particle");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    @SuppressWarnings("removal")
    public static void masteryTracksAndSidegrades(GameTestHelper helper) {
        var p = new com.khaoskrew.khaosnexus.hunter.HunterProfile();
        p.classId = "tank";
        Mastery.onXp(p, GateRank.A, 500);
        helper.assertTrue(Mastery.level(p, "tank") == 0, "XP below S does not count");
        Mastery.onXp(p, GateRank.S, Config.MASTERY_XP_PER_LEVEL.get() * 2);
        helper.assertTrue(Mastery.level(p, "tank") == 2, "S-rank XP fills mastery");
        Mastery.onXp(p, GateRank.S, 10_000_000);
        helper.assertTrue(Mastery.level(p, "tank") == Config.MASTERY_MAX_LEVEL.get(), "mastery caps");
        ResourceLocation bulwark = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "tank_bulwark");
        helper.assertTrue(Mastery.pick(p, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "assassin_ghost")) != null, "other class perk refused");
        helper.assertTrue(Mastery.pick(p, bulwark) == null, "pick perk");
        helper.assertTrue("khaosnexus.mastery.group".equals(Mastery.pick(p, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "tank_vanguard"))), "one perk per group");
        var player = helper.makeMockServerPlayerInLevel();
        com.khaoskrew.khaosnexus.hunter.HunterProfile live = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(player);
        live.classId = "tank";
        live.endgame = p.endgame.copy();
        double armor = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        Mastery.apply(player);
        helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == armor + 3, "perk modifiers applied");
        Mastery.reset(live);
        Mastery.apply(player);
        helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == armor, "reset removes modifiers");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    @SuppressWarnings("removal")
    public static void prestigeRanksAndSSGates(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var p = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(player);
        p.xp = com.khaoskrew.khaosnexus.hunter.HunterRanks.threshold(GateRank.S);
        int basePoints = com.khaoskrew.khaosnexus.skill.SkillService.totalPoints(p);
        helper.assertFalse(Prestige.tryPromote(player), "no SS without achievements");
        p.addTitle(RedGates.TITLE);
        p.spireBest = Config.SS_SPIRE.get();
        p.endgame.putInt(Prestige.RUSH_WINS, Config.SS_RUSH.get());
        helper.assertTrue(Prestige.tryPromote(player) && Prestige.level(p) == Prestige.SS, "promoted to SS");
        helper.assertTrue(p.hasTitle(Prestige.TITLE_SS), "SS title");
        helper.assertTrue(com.khaoskrew.khaosnexus.skill.SkillService.totalPoints(p) == basePoints + Config.PRESTIGE_SKILL_POINTS.get(), "bonus skill point");
        helper.assertFalse(Prestige.missing(p).isEmpty(), "National needs more");
        p.spireBest = Config.NATIONAL_SPIRE.get();
        p.endgame.putInt(Prestige.RUSH_WINS, Config.NATIONAL_RUSH.get());
        p.endgame.putInt(Prestige.CHALLENGES, Config.NATIONAL_CHALLENGES.get());
        helper.assertTrue(Prestige.tryPromote(player) && Prestige.level(p) == Prestige.NATIONAL, "promoted to National");
        helper.assertFalse(Prestige.tryPromote(player), "National is the top");
        helper.assertTrue(Auras.unlockedIds(p).stream().anyMatch(r -> r.getPath().equals("national")), "National aura unlocks");
        double chance = Config.SS_GATE_CHANCE.get();
        try {
            Config.SS_GATE_CHANCE.set(1.0D);
            helper.assertTrue(Prestige.rollSS(GateRank.S, net.minecraft.util.RandomSource.create(2)), "S rolls SS");
            helper.assertFalse(Prestige.rollSS(GateRank.A, net.minecraft.util.RandomSource.create(2)), "only S gates");
        } finally {
            Config.SS_GATE_CHANCE.set(chance);
        }
        Mob boss = GateArena.spawnMob(helper.getLevel(), GateRank.S, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(1, 2, 1)), 0F, 1.0D, true);
        float hp = boss.getMaxHealth();
        Prestige.empowerSS(boss, net.minecraft.util.RandomSource.create(3));
        helper.assertTrue(boss.getMaxHealth() >= hp * Config.SS_GATE_SCALE.get() * 0.5D && boss.getPersistentData().getBoolean("khaosnexus.ss"), "SS boss empowered");
        boss.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void bossRushRosterAndProgress(GameTestHelper helper) {
        ResourceLocation def = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "default");
        var fights = BossRush.roster(def, 42L);
        helper.assertTrue(fights.size() == 5, "five fights even when boss mods are missing");
        helper.assertTrue(fights.equals(BossRush.roster(def, 42L)), "seeded shuffle");
        helper.assertTrue(fights.stream().allMatch(t -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t) != null), "only registered bosses");
        helper.assertTrue(BossRush.roster(ResourceLocation.parse("t:none"), 1L).isEmpty(), "missing roster is empty");
        var run = BossRush.testRun(fights, 3);
        var server = helper.getLevel().getServer();
        for (int i = 0; i < 4; i++) {
            BossRush.onBossDown(server, run);
        }
        helper.assertTrue(BossRush.running(run) && run.stage == 4, "run continues until the last boss");
        BossRush.onBossDown(server, run);
        helper.assertFalse(BossRush.running(run), "run ends after the last boss");
        helper.assertTrue(Leaderboards.lowerBetter(Leaderboards.rushBoard()), "rush board is a time board");
        BossRush.buildArena(helper.getLevel(), helper.absolutePos(new BlockPos(0, 40, 0)));
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(0, 39, 0))).is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS), "arena floor");
        helper.succeed();
    }
}

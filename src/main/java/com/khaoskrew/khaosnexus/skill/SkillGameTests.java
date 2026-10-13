package com.khaoskrew.khaosnexus.skill;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.forge.HunterForge;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class SkillGameTests {
    private static ServerPlayer tank(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        HunterRanks.setXp(player, 0);
        ClassService.equip(player, "tank");
        HunterRanks.profile(player).skills = "";
        helper.assertTrue(ClassService.equipped(player) == HunterClass.byId("tank"), "tank equipped");
        helper.assertTrue(SkillTrees.tree(HunterClass.byId("tank")).size() >= 6, "tank tree loaded from data");
        return player;
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void skillSpendAndRankLock(GameTestHelper helper) {
        ServerPlayer player = tank(helper);
        HunterClass c = ClassService.equipped(player);
        helper.assertTrue(SkillService.spend(player, "primary_power") == SkillService.Result.NO_POINTS, "no points at level 1");
        HunterRanks.setXp(player, 30);
        int total = SkillService.totalPoints(HunterRanks.profile(player));
        helper.assertTrue(total >= 1, "levels grant points");
        helper.assertTrue(SkillService.spend(player, "primary_power") == SkillService.Result.OK, "spend works");
        helper.assertTrue(SkillService.rank(HunterRanks.profile(player), c, "primary_power") == 1, "rank 1");
        helper.assertTrue(SkillService.available(HunterRanks.profile(player), c) == total - 1, "point consumed");
        helper.assertTrue(SkillService.mult(player, true, SkillTrees.Stat.POWER) > 1.0D, "power multiplier applies");
        helper.assertTrue(SkillService.spend(player, "capstone") == SkillService.Result.RANK_LOCKED, "capstone locked below A");
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.A));
        helper.assertTrue(SkillService.check(HunterRanks.profile(player), c, "capstone") != SkillService.Result.RANK_LOCKED,
                "rank A lifts the lock");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void skillRespecCostsRiftCore(GameTestHelper helper) {
        ServerPlayer player = tank(helper);
        HunterClass c = ClassService.equipped(player);
        HunterRanks.setXp(player, 50);
        SkillService.spend(player, "primary_power");
        SkillService.spend(player, "secondary_power");
        helper.assertTrue(SkillService.spent(HunterRanks.profile(player), c) == 2, "two points spent");
        player.getInventory().clearContent();
        helper.assertTrue(SkillService.respec(player) == SkillService.Result.NO_RESPEC_ITEM, "respec needs a rift core");
        player.getInventory().add(new ItemStack(HunterForge.RIFT_CORE.get(), 2));
        helper.assertTrue(SkillService.respec(player) == SkillService.Result.OK, "respec with core");
        helper.assertTrue(player.getInventory().countItem(HunterForge.RIFT_CORE.get()) == 1, "one core consumed");
        helper.assertTrue(SkillService.spent(HunterRanks.profile(player), c) == 0, "points refunded");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void skillCapstoneBoostsBothActives(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ClassService.equip(player, "caster");
        HunterRanks.profile(player).skills = "";
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.S));
        helper.assertTrue(SkillService.level(HunterRanks.profile(player).xp) == com.khaoskrew.khaosnexus.Config.SKILL_MAX_LEVEL.get(),
                "S rank reaches the level cap");
        for (String id : new String[] {"primary_power", "secondary_power", "passive_a"}) {
            helper.assertTrue(SkillService.spend(player, id) == SkillService.Result.OK, "spend " + id);
        }
        double before = SkillService.mult(player, false, SkillTrees.Stat.POWER);
        helper.assertTrue(SkillService.spend(player, "capstone") == SkillService.Result.OK, "capstone at S rank");
        helper.assertTrue(SkillService.mult(player, true, SkillTrees.Stat.POWER) > 1.3D, "capstone boosts the primary");
        helper.assertTrue(SkillService.mult(player, false, SkillTrees.Stat.POWER) > before + 0.2D, "capstone boosts the secondary");
        int cost = 0;
        for (SkillTrees.Node n : SkillTrees.tree(HunterClass.byId("caster"))) cost += n.cost() * n.maxRank();
        helper.assertTrue(cost <= SkillService.totalPoints(HunterRanks.profile(player)), "a capped hunter can finish one tree");
        helper.succeed();
    }
}

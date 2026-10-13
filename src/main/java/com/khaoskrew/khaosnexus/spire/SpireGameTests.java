package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class SpireGameTests {
    private static BlockPos far(GameTestHelper helper, int dx) {
        return helper.absolutePos(new BlockPos(0, 40, 0)).offset(dx, 0, 0);
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void spireThemesRotateAndArenasBuild(GameTestHelper helper) {
        helper.assertTrue(JsonPool.SPIRE_THEMES.all().size() >= 5, "bundled themes load");
        var t1 = SpireBuilder.themeFor(1);
        var t2 = SpireBuilder.themeFor(2);
        helper.assertFalse(t1.id().equals(t2.id()), "the style changes per floor");
        helper.assertTrue(t1.id().equals(SpireBuilder.themeFor(1 + JsonPool.SPIRE_THEMES.all().size()).id()), "themes rotate");
        helper.assertTrue(SpireBuilder.bossFor(t1, SpireService.levels(), SpireService.levels()).equals(t1.finalBoss()), "last level gets the final boss");
        BlockPos o = far(helper, 0);
        SpireBuilder.buildArena(helper.getLevel(), o, t1);
        var l = helper.getLevel();
        helper.assertTrue(l.getBlockState(o.offset(SpireBuilder.ARENA_HALF, 1, 0)).is(t1.wall().getBlock()), "walls");
        helper.assertTrue(l.getBlockState(o.offset(-9, 6, -9)).is(t1.light().getBlock()), "pillar light caps");
        helper.assertFalse(l.getBlockState(o.offset(0, -1, 0)).isAir(), "floor");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void spireTownHasVendorsCheckpointAndGate(GameTestHelper helper) {
        BlockPos o = far(helper, 80);
        var l = helper.getLevel();
        SpireBuilder.buildTown(l, o);
        SpireVendors.spawn(l, o, 3, GateRank.S);
        helper.assertTrue(l.getBlockState(SpireBuilder.checkpointPos(o)).is(Blocks.LODESTONE), "checkpoint shrine");
        helper.assertTrue(l.getBlockState(SpireBuilder.exitPos(o)).is(ModRegistries.SPIRE_GATE.get()), "exit gate");
        var npcs = l.getEntitiesOfClass(WanderingTrader.class, new AABB(o).inflate(20), n -> n.getPersistentData().getBoolean(SpireVendors.TAG));
        helper.assertTrue(npcs.size() == 3, "three vendors");
        helper.assertTrue(npcs.stream().anyMatch(n -> n.getPersistentData().getBoolean(SpireVendors.REPAIR)), "a repair smith");
        helper.assertTrue(npcs.stream().allMatch(n -> n.isNoAi() && n.isInvulnerable()), "vendors stay put and safe");
        npcs.forEach(n -> n.discard());
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void spireVendorTradesScaleWithFloorAndRank(GameTestHelper helper) {
        var armorer = JsonPool.SPIRE_VENDORS.get(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "b_armorer"));
        var regs = helper.getLevel().registryAccess();
        var low = SpireVendors.offers(armorer, 1, GateRank.C, regs);
        var high = SpireVendors.offers(armorer, 7, GateRank.S, regs);
        helper.assertTrue(high.size() > low.size(), "rank and floor unlock more trades");
        var sharp = regs.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        int lo = low.get(0).getResult().getEnchantments().getLevel(sharp);
        int hi = high.get(0).getResult().getEnchantments().getLevel(sharp);
        helper.assertTrue(hi > lo && hi <= 5, "gear enchantments scale with the floor, capped");
        helper.assertTrue(high.get(0).getBaseCostA().getCount() > low.get(0).getBaseCostA().getCount(), "prices scale too");
        var alch = JsonPool.SPIRE_VENDORS.get(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "a_alchemist"));
        var potions = SpireVendors.offers(alch, 1, GateRank.E, regs);
        helper.assertTrue(potions.get(0).getResult().is(Items.POTION)
                && potions.get(0).getResult().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) != null, "potion trades carry contents");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(sword.getMaxDamage() / 2);
        helper.assertTrue(SpireVendors.quarters(sword) == 2, "repair is priced per quarter of durability");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 100)
    @SuppressWarnings("removal")
    public static void spirePortalLightsAndAdvances(GameTestHelper helper) {
        var l = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        HunterProfile p = HunterRanks.profile(player);
        SpireService.migrate(p);
        var boss = SpireService.generate(l, 1, 1, 1);
        helper.assertTrue(boss != null, "level boss spawns");
        BlockPos portalPos = SpireBuilder.portalPos(SpireService.origin(1, 1, 1), false);
        SpirePortalBlockEntity portal = (SpirePortalBlockEntity) l.getBlockEntity(portalPos);
        helper.assertTrue(portal != null && !portal.isLit(), "portal starts dormant");
        helper.assertTrue(portal.targetFloor() == 1 && portal.targetLevel() == 2, "portal leads to level 2");
        player.teleportTo(boss.getX(), boss.getY(), boss.getZ() + 2);
        boss.hurt(player.damageSources().playerAttack(player), 1.0E7F);
        helper.assertTrue(portal.isLit(), "killing the boss lights the portal");
        helper.assertTrue(SpireService.nextLevel(p) == 2 && p.spireFloor == 1, "progress moves to level 2");
        var last = SpireService.generate(l, 1, SpireService.levels(), 1);
        SpirePortalBlockEntity lastPortal = (SpirePortalBlockEntity) l.getBlockEntity(SpireBuilder.portalPos(SpireService.origin(1, SpireService.levels(), 1), false));
        helper.assertTrue(lastPortal.targetFloor() == 2 && lastPortal.targetLevel() == 0, "the last level leads to the next floor's town");
        last.discard();
        helper.assertTrue(SpireService.stage(2, 1) == SpireService.levels() + 1, "stage numbering");
        helper.assertTrue(SpireService.where(SpireService.stage(3, 4)).getString().equals("F3-L4"), "board label");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void spireOldSavesMigrate(GameTestHelper helper) {
        HunterProfile p = new HunterProfile();
        p.spireFloor = 31;
        p.spireBest = 30;
        p.spireCheckpoint = 30;
        SpireService.migrate(p);
        helper.assertTrue(p.spireBest == 3 && p.spireCheckpoint == 3 && p.spireFloor == 3 && SpireService.nextLevel(p) == 1, "30 old floors become 3 cleared floors");
        SpireService.migrate(p);
        helper.assertTrue(p.spireBest == 3, "migration runs once");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void spireLevelProgressSurvivesRestart(GameTestHelper helper) {
        HunterProfile p = new HunterProfile();
        SpireService.migrate(p);
        p.spireFloor = 4;
        p.spireCheckpoint = 4;
        p.endgame.putInt(SpireService.LEVEL_KEY, 7);
        HunterProfile reloaded = new HunterProfile();
        reloaded.deserializeNBT(helper.getLevel().registryAccess(), p.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(reloaded.spireFloor == 4 && SpireService.nextLevel(reloaded) == 7, "floor and level are saved with the profile");
        helper.assertTrue(SpireService.resumeLevel(reloaded, 4) == 7, "re-entering mid-floor resumes at the next unbeaten level");
        reloaded.endgame.putInt(SpireService.LEVEL_KEY, 1);
        helper.assertTrue(SpireService.resumeLevel(reloaded, 4) == 0, "a fresh floor starts in its town");
        helper.succeed();
    }
}

package com.khaoskrew.khaosnexus.forge;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class ForgeGameTests {
    @SuppressWarnings("removal")
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void forgeUpgradesAndGatesByRank(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HunterRanks.setXp(player, 0);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(ForgeRecipes.match(sword) != null, "weapons recipe loads from data and accepts a sword");
        helper.assertTrue(ForgeRecipes.match(new ItemStack(Items.DIRT)) == null, "dirt has no forge recipe");

        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.COPPER_INGOT, 5));
        player.getInventory().add(new ItemStack(HunterForge.RIFT_ESSENCE.get(), 2));
        helper.assertTrue(ForgeRecipes.upgrade(player, sword, true, false).status() == ForgeRecipes.Status.PREVIEW, "sneak previews");
        helper.assertTrue(ForgeRecipes.level(sword) == 0, "preview does not forge");
        ForgeRecipes.Result r = ForgeRecipes.upgrade(player, sword, false, false);
        helper.assertTrue(r.status() == ForgeRecipes.Status.FORGED, "E-rank hunter forges +1 with E-tier materials, got " + r.status());
        helper.assertTrue(ForgeRecipes.level(sword) == 1, "sword is +1");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 1, "4 copper spent");
        helper.assertTrue(player.getInventory().countItem(HunterForge.RIFT_ESSENCE.get()) == 0, "2 essence spent");
        ItemAttributeModifiers mods = sword.get(DataComponents.ATTRIBUTE_MODIFIERS);
        helper.assertTrue(mods != null && mods.modifiers().stream().anyMatch(e -> e.modifier().id().getPath().startsWith("forge_")),
                "forge modifier applied");
        helper.assertTrue(mods.modifiers().size() >= 3, "vanilla sword modifiers kept");

        helper.assertTrue(ForgeRecipes.upgrade(player, sword, false, false).status() == ForgeRecipes.Status.RANK_LOCKED, "+2 needs rank D");
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.D));
        helper.assertTrue(ForgeRecipes.upgrade(player, sword, false, false).status() == ForgeRecipes.Status.MISSING, "+2 needs materials");
        player.getInventory().add(new ItemStack(Items.COPPER_INGOT, 6));
        player.getInventory().add(new ItemStack(HunterForge.RIFT_ESSENCE.get(), 4));
        helper.assertTrue(ForgeRecipes.upgrade(player, sword, false, false).status() == ForgeRecipes.Status.MISSING, "E-tier copper can't pay a D-tier level");
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 6));
        helper.assertTrue(ForgeRecipes.upgrade(player, sword, false, false).status() == ForgeRecipes.Status.FORGED, "iron pays the D-tier level");
        helper.assertTrue(ForgeRecipes.level(sword) == 2, "sword is +2");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 7, "copper untouched by the D level");
        helper.succeed();
    }
}

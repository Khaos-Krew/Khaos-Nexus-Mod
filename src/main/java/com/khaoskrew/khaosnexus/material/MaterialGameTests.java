package com.khaoskrew.khaosnexus.material;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class MaterialGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void tiersResolveAndRollByRank(GameTestHelper helper) {
        helper.assertTrue(MaterialTiers.tierOf(Items.COPPER_INGOT) == GateRank.E, "copper is E tier");
        helper.assertTrue(MaterialTiers.tierOf(Items.IRON_INGOT) == GateRank.D, "iron is D tier");
        helper.assertTrue(MaterialTiers.tierOf(Items.GOLD_INGOT) == GateRank.C, "gold is C tier");
        helper.assertTrue(MaterialTiers.tierOf(Items.DIRT) == null, "dirt is not a material");
        helper.assertFalse(MaterialTiers.forRank(GateRank.E).isEmpty(), "E tier has materials");
        boolean loot = Config.MATERIAL_LOOT.get();
        try {
            Config.MATERIAL_LOOT.set(true);
            RandomSource random = RandomSource.create(7);
            for (int i = 0; i < 20; i++) {
                List<ItemStack> roll = MaterialTiers.roll(GateRank.S, random);
                helper.assertFalse(roll.isEmpty(), "S gates always roll materials (falls back to lower tiers)");
                for (ItemStack s : roll) {
                    helper.assertTrue(MaterialTiers.tierOf(s.getItem()) != null, "rolled items are known materials");
                }
                for (ItemStack s : MaterialTiers.roll(GateRank.E, random)) {
                    GateRank t = MaterialTiers.tierOf(s.getItem());
                    helper.assertTrue(t.ordinal() <= GateRank.D.ordinal(), "E gates roll at most one tier up");
                }
            }
            Config.MATERIAL_LOOT.set(false);
            helper.assertTrue(MaterialTiers.roll(GateRank.S, random).isEmpty(), "tiered loot off rolls nothing");
        } finally {
            Config.MATERIAL_LOOT.set(loot);
        }
        helper.succeed();
    }
}

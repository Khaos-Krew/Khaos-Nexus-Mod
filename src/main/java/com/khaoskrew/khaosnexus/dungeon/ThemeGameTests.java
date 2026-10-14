package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.mob.ModEntities;
import com.khaoskrew.khaosnexus.mob.RiftBoss;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 1.1: new dungeon themes load, hazards apply and are warded, boss combat stance follows the drawn weapon. */
@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class ThemeGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    @SuppressWarnings("removal")
    public static void themesAndHazards(GameTestHelper helper) {
        for (String id : new String[] {"scorched_forge", "sunken_temple", "void_sanctum", "sculk_hollow"}) {
            DungeonThemes.Theme t = DungeonThemes.get(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, id));
            helper.assertTrue(t != null && t.id().getPath().equals(id) && !t.palette().isEmpty() && !t.hazards().isEmpty(), "theme loaded: " + id);
        }
        var p = helper.makeMockServerPlayerInLevel();
        ThemeHazards.apply(p, "heat");
        helper.assertTrue(p.hasEffect(MobEffects.HUNGER), "heat gives hunger");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        p.removeEffect(MobEffects.HUNGER);
        ThemeHazards.apply(p, "heat");
        helper.assertTrue(!p.hasEffect(MobEffects.HUNGER), "fire resistance ignores heat");
        helper.setBlock(new net.minecraft.core.BlockPos(1, 2, 1), net.minecraft.world.level.block.Blocks.AIR);
        ThemeHazards.apply(p, "void");
        helper.assertTrue(p.hasEffect(MobEffects.DARKNESS), "void gives darkness");
        RiftBoss b = (RiftBoss) helper.spawn(ModEntities.SPIRE_GUARDIAN.get(), 1, 2, 1);
        helper.assertTrue(!b.inCombatStance(), "sheathed boss uses the calm loops");
        helper.succeed();
    }
}

package com.khaoskrew.khaosnexus.compat.items.tome;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.items.CompatItems;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Hunter Tomes: evolution keeps spells, class gate, glyph cast with class attunement. Inert pass without Ars. */
@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class TomeGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void tomeClassGateAndTiers(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        HunterRanks.profile(p).classId = "berserker";
        helper.assertTrue(!HunterTomes.canUse(p), "berserker can't read tomes");
        for (HunterClass c : HunterClass.values()) {
            boolean caster = c == HunterClass.CASTER || c == HunterClass.LIFEBINDER || c == HunterClass.SUMMONER || c == HunterClass.NECROMANCER;
            helper.assertTrue(HunterTomes.casterClass(c) == caster, "caster set " + c);
        }
        HunterRanks.profile(p).xp = 0;
        helper.assertTrue(HunterTomes.tierFor(p) == 0, "fresh hunter = E tome");
        HunterRanks.profile(p).xp = HunterRanks.threshold(com.khaoskrew.khaosnexus.gate.GateRank.S);
        helper.assertTrue(HunterTomes.tierFor(p) == 5, "S hunter = S tome");
        for (int t = 1; t < 7; t++) helper.assertTrue(HunterTomes.SLOTS[t] > HunterTomes.SLOTS[t - 1] && HunterTomes.DISCOUNT[t] > HunterTomes.DISCOUNT[t - 1], "tiers grow");
        helper.assertTrue(CompatItems.ars() == (CompatItems.HUNTER_TOMES[0] != null), "tomes only with Ars");
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void tomeEvolvesAndCastsGlyph(GameTestHelper helper) {
        if (!CompatItems.ars()) { helper.succeed(); return; }
        String err = TomeTestOps.evolution();
        helper.assertTrue(err == null, "evolution: " + err);
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        HunterRanks.profile(p).classId = "caster";
        HunterRanks.profile(p).xp = HunterRanks.threshold(com.khaoskrew.khaosnexus.gate.GateRank.B);
        TomeTestOps.give(p, 0);
        helper.assertTrue(TomeEvents.evolveAll(p) == 1 && TomeTestOps.heldTier(p) == 3, "inventory tome evolved E->B on rank");
        helper.assertTrue(TomeEvents.evolveAll(p) == 0, "no double evolution");
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        float before = pig.getHealth();
        TomeTestOps.cast(p, pig);
        float dealt = before - pig.getHealth();
        helper.assertTrue(dealt > 0, "glyph cast through the tome damaged the target");
        helper.assertTrue(Math.abs(TomeEvents.damageScale(p, pig, 3) - 1.12F) < 1e-4, "caster attunement at B = +12%");
        HunterRanks.profile(p).classId = "berserker";
        helper.assertTrue(TomeEvents.damageScale(p, pig, 3) == 1.0F && !HunterTomes.canUse(p), "non-caster gets nothing");
        helper.succeed();
    }
}

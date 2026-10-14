package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.endgame.Prestige;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class SagaGameTests {
    static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, p); }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void underthroneAndSagaDataLoad(GameTestHelper h) {
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE).containsKey(id("underthrone")), "underthrone dimension type (the test server only loads the overworld)");
        h.assertTrue(JsonPool.OATHS.all().size() >= 4, "4 act I oaths");
        h.assertTrue(JsonPool.SEALS.get(id("hollow_crown")) != null, "hollow crown seal");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void oathsNeedParagonAndCostShards(GameTestHelper h) {
        HunterProfile p = new HunterProfile();
        ResourceLocation o = id("oath_of_the_unbroken");
        h.assertFalse(Saga.swear(p, o), "no oath before Paragon");
        p.endgame.putInt(Prestige.KEY, Prestige.NATIONAL);
        h.assertTrue(Saga.swear(p, o) && !Saga.swear(p, id("oath_of_the_quiet_star")), "one oath at a time");
        h.assertFalse(Saga.unlockNode(p), "nodes cost shards");
        Saga.addShards(p, 3);
        h.assertTrue(Saga.unlockNode(p) && Saga.unlockNode(p) && !Saga.unlockNode(p) && Saga.shards(p) == 1, "1+1 spent, next node costs 2");
        h.assertFalse(Saga.respec(p), "respec costs shards");
        Saga.addShards(p, 100);
        h.assertTrue(Saga.respec(p) && Saga.oath(p).isEmpty(), "respec clears");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void sealsAndAmbushRules(GameTestHelper h) {
        HunterProfile p = new HunterProfile();
        h.assertFalse(Saga.canOpenSeal(p, id("hollow_crown")), "needs Paragon");
        p.endgame.putInt(Prestige.KEY, Prestige.NATIONAL);
        h.assertTrue(Saga.canOpenSeal(p, id("hollow_crown")), "first seal open at Paragon");
        Saga.breakSeal(p, id("hollow_crown"));
        h.assertTrue(Saga.sealBroken(p, id("hollow_crown")), "seal state saved");
        RandomSource r = RandomSource.create(7);
        int hits = 0;
        for (int i = 0; i < 4000; i++) {
            h.assertFalse(Saga.rollAmbush(GateRank.B, true, r), "no ambush below A");
            h.assertFalse(Saga.rollAmbush(GateRank.S, false, r), "no ambush in special gates");
            if (Saga.rollAmbush(GateRank.A, true, r)) hits++;
        }
        h.assertTrue(hits > 120 && hits < 280, "about 5%: " + hits);
        h.succeed();
    }
}

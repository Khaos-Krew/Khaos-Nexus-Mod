package com.khaoskrew.khaosnexus.compat;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class CompatGameTests {
    @SuppressWarnings("removal")
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void compatOffPathsAreInert(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassCompat.activeCount() == 0, "no companion mods in the test run, so no bonuses are live");
        int before = player.getAttributes().getSyncableAttributes().stream().mapToInt(a -> a.getModifiers().size()).sum();
        for (HunterClass c : HunterClass.values()) {
            ClassCompat.apply(player, c);
        }
        ClassCompat.clear(player);
        int after = player.getAttributes().getSyncableAttributes().stream().mapToInt(a -> a.getModifiers().size()).sum();
        helper.assertTrue(before == after, "class compat leaves attributes untouched without the mods");

        boolean bossMods = Config.COMPAT_BOSS_MODS.get();
        double chance = Config.BOSS_MOD_CHANCE.get();
        List<? extends String> bosses = Config.OPTIONAL_BOSSES.get();
        List<? extends String> loot = Config.OPTIONAL_LOOT.get();
        try {
            RandomSource random = RandomSource.create(3);
            Config.BOSS_MOD_CHANCE.set(1.0D);
            Config.OPTIONAL_BOSSES.set(List.of("B=modid_missing:boss"));
            Config.COMPAT_BOSS_MODS.set(true);
            helper.assertTrue(CompatPools.boss(GateRank.B, random).isEmpty(), "unregistered boss ids are skipped");
            Config.OPTIONAL_BOSSES.set(List.of("B=minecraft:zombie"));
            helper.assertTrue(CompatPools.boss(GateRank.B, random).orElse(null) == EntityType.ZOMBIE, "registered boss joins the pool");
            helper.assertTrue(CompatPools.boss(GateRank.A, random).isEmpty(), "pool is per rank");
            Config.COMPAT_BOSS_MODS.set(false);
            helper.assertTrue(CompatPools.boss(GateRank.B, random).isEmpty(), "boss mods toggle off disables the pool");
            Config.OPTIONAL_LOOT.set(List.of("E=modid_missing:item", "E=#modid_missing:tag"));
            for (int i = 0; i < 30; i++) {
                helper.assertTrue(CompatPools.loot(GateRank.S, random).isEmpty(), "missing loot ids and empty tags add nothing");
            }
        } finally {
            Config.COMPAT_BOSS_MODS.set(bossMods);
            Config.BOSS_MOD_CHANCE.set(chance);
            Config.OPTIONAL_BOSSES.set(bosses);
            Config.OPTIONAL_LOOT.set(loot);
        }
        helper.succeed();
    }
}

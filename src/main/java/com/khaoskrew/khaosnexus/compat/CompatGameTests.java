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

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void compatDataAndApotheosisRulesWithoutMods(GameTestHelper helper) {
        for (HunterClass c : HunterClass.values()) {
            helper.assertFalse(ClassCompat.bonuses(c).isEmpty(), "class compat datapack has bonuses for " + c.id());
        }
        helper.assertTrue(ClassCompat.brokenIds().isEmpty(), "no installed mod has a wrong attribute id");
        ClassCompat.Bonus b = ClassCompat.parse(com.google.gson.JsonParser.parseString(
                "{\"attribute\":\"irons_spellbooks:spell_power\",\"amount\":0.1,\"operation\":\"add_multiplied_base\"}").getAsJsonObject());
        helper.assertTrue(b.modId().equals("irons_spellbooks") && b.op() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "bonus json parses mod and operation");
        if (!net.neoforged.fml.ModList.get().isLoaded("apotheosis")) {
            helper.assertTrue(ApothCompat.forgeScale() == 1.0D, "forge bonus is unscaled without Apotheosis");
            helper.assertFalse(ApothCompat.active(), "Apotheosis rules are inert without Apotheosis");
        }
        helper.assertTrue("pinnacle".equals(ApothCompat.tierFor(GateRank.S)) && "frontier".equals(ApothCompat.tierFor(GateRank.C)),
                "default gate rank to world tier map");
        helper.assertTrue(ApothCompat.isTierModifier(net.minecraft.resources.ResourceLocation.parse("apotheosis:summit/armor")), "tier modifier ids");
        helper.assertFalse(ApothCompat.isTierModifier(net.minecraft.resources.ResourceLocation.parse("apotheosis:affix/armor")), "affix ids are not tier ids");
        var warden = com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_WARDEN.get().create(helper.getLevel());
        helper.assertTrue(warden != null && ApothCompat.isBoss(warden) && (net.neoforged.fml.ModList.get().isLoaded("apotheosis") || !ApothCompat.shielded(warden)), "warden is tagged exempt, shield idle without Apotheosis");
        var ghoul = com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_GHOUL.get().create(helper.getLevel());
        helper.assertTrue(ghoul != null && !ApothCompat.isBoss(ghoul), "regular gate mobs are not exempt");
        helper.assertFalse(CompatPools.wake(ghoul), "wake list ignores unlisted mobs");
        warden.discard();
        ghoul.discard();
        helper.succeed();
    }

    /** Runs in both the plain and the compat test servers: checks per-mod presence branching of items and compat loot. */
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void compatItemsAndLootFollowInstalledMods(GameTestHelper helper) {
        var regs = helper.getLevel().registryAccess();
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        java.util.function.Function<String, Boolean> has = n -> items.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, n));
        helper.assertTrue(has.apply("rift_source_shard") == com.khaoskrew.khaosnexus.compat.items.CompatItems.ars(), "Ars shard registered only with Ars");
        helper.assertTrue(has.apply("glyph_rift_rend") == com.khaoskrew.khaosnexus.compat.items.CompatItems.ars(), "Ars glyph registered only with Ars");
        helper.assertTrue(has.apply("rift_hunters_sigil") == com.khaoskrew.khaosnexus.compat.items.CompatItems.relics(), "sigil only with Relics + Curios");
        for (String n : List.of("rift_lantern", "voidstep_anklet", "gatebreaker_band")) {
            helper.assertTrue(has.apply(n) == com.khaoskrew.khaosnexus.compat.items.CompatItems.artifacts(), n + " only with Artifacts + Curios");
        }
        var entries = com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.entries();
        java.util.Set<String> mods = new java.util.HashSet<>();
        for (var e : entries) {
            mods.add(e.mod());
            helper.assertFalse(e.chances().isEmpty(), "every compat loot entry has rank chances");
            if (com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.enabled(e.mod())) {
                helper.assertFalse(com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.stack(e, regs).isEmpty(), "installed-mod entry parses: " + e.stack());
            }
        }
        helper.assertTrue(mods.containsAll(List.of("ars_nouveau", "relics", "artifacts", "apotheosis")), "loot files for all four mods load");
        double mult = Config.COMPAT_LOOT_MULTIPLIER.get();
        try {
            Config.COMPAT_LOOT_MULTIPLIER.set(100.0D);
            RandomSource random = RandomSource.create(7);
            for (GateRank rank : GateRank.values()) {
                for (int i = 0; i < 20; i++) {
                    for (var s : com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.roll(rank, random, regs)) {
                        String ns = items.getKey(s.getItem()).getNamespace();
                        helper.assertTrue(ns.equals(KhaosNexus.MODID) || net.neoforged.fml.ModList.get().isLoaded(ns), "rolled loot only from installed mods: " + s);
                    }
                }
            }
            boolean any = entries.stream().anyMatch(e -> com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.enabled(e.mod()));
            helper.assertTrue(any || com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.roll(GateRank.S, random, regs).isEmpty(), "no companion mods means no compat loot");
            Config.COMPAT_LOOT_MULTIPLIER.set(0.0D);
            helper.assertTrue(com.khaoskrew.khaosnexus.compat.items.CompatLootModifier.roll(GateRank.S, random, regs).isEmpty(), "multiplier 0 disables compat loot");
        } finally {
            Config.COMPAT_LOOT_MULTIPLIER.set(mult);
        }
        helper.succeed();
    }
}

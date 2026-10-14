package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.spire.SpireService;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class DimensionGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void ourDimensionsAreNamespacedAndUseOwnBiomes(GameTestHelper h) {
        var reg = h.getLevel().registryAccess();
        var types = reg.registryOrThrow(Registries.DIMENSION_TYPE);
        for (String id : new String[] {"gate_dungeon", "spire"}) {
            h.assertTrue(types.containsKey(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, id)), "dimension type khaosnexus:" + id);
        }
        h.assertTrue(DimensionSafety.ours(SpireService.DIMENSION) && DimensionSafety.ours(DungeonRules.GATE_DUNGEON), "keys namespaced");
        h.assertFalse(DimensionSafety.ours(Level.NETHER) || DimensionSafety.ours(Level.OVERWORLD), "vanilla keys are not ours");
        // vanilla dimension types are untouched (no override from our datapack)
        h.assertTrue(types.get(BuiltinDimensionTypes.OVERWORLD).hasSkyLight() && types.get(BuiltinDimensionTypes.NETHER).ultraWarm(), "vanilla types intact");
        var biomes = reg.registryOrThrow(Registries.BIOME);
        for (String b : new String[] {"gate_void", "spire_void"}) {
            var biome = biomes.get(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, b));
            h.assertTrue(biome != null, "own biome " + b);
            var holder = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, b)));
            h.assertFalse(holder.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD) || holder.is(net.neoforged.neoforge.common.Tags.Biomes.IS_OVERWORLD)
                    || holder.is(net.minecraft.tags.BiomeTags.IS_NETHER) || holder.is(net.minecraft.tags.BiomeTags.IS_END), "not in any shared dimension tag, so foreign biome modifiers skip it");
            h.assertTrue(biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).isEmpty(), "no natural spawns of its own");
            h.assertTrue(biome.getGenerationSettings().features().stream().allMatch(f -> f.size() == 0), "no features injected");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void missingDimensionFallsBackToOverworld(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var gone = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("othermod", "removed"));
        h.assertTrue(DimensionSafety.resolve(server, gone) == server.overworld(), "missing -> overworld");
        h.assertTrue(DimensionSafety.resolve(server, null) == server.overworld(), "null -> overworld");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void foreignNaturalSpawnsBlockedOnlyInOurDimensions(GameTestHelper h) {
        var l = h.getLevel(); // the test level is the overworld: nothing is blocked here
        var zombie = EntityType.ZOMBIE.create(l);
        h.assertFalse(DimensionSafety.blocks(l, zombie, MobSpawnType.NATURAL), "no side effects outside our dimensions");
        var dungeon = DungeonService.level(server(h));
        if (dungeon != null) {
            h.assertTrue(DimensionSafety.blocks(dungeon, zombie, MobSpawnType.NATURAL), "foreign natural spawn blocked");
            h.assertFalse(DimensionSafety.blocks(dungeon, zombie, MobSpawnType.SPAWN_EGG), "commands/eggs/our gate spawns still work");
        }
        zombie.discard();
        h.succeed();
    }

    private static net.minecraft.server.MinecraftServer server(GameTestHelper h) { return h.getLevel().getServer(); }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void noVanillaDataOverrides(GameTestHelper h) {
        var rm = server(h).getResourceManager();
        for (var e : rm.listResources("tags", id -> id.getPath().endsWith(".json")).entrySet()) {
            if (!e.getValue().sourcePackId().equals("mod/" + KhaosNexus.MODID) || e.getKey().getNamespace().equals(KhaosNexus.MODID)) continue;
            try (var r = e.getValue().openAsReader()) {
                var j = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
                h.assertFalse(j.has("replace") && j.get("replace").getAsBoolean(), "tag " + e.getKey() + " must append, not replace");
            } catch (java.io.IOException ex) { h.fail(ex.toString()); }
        }
        int mine = 0;
        for (var e : rm.listResources("dimension", id -> id.getPath().endsWith(".json")).entrySet()) {
            if (e.getValue().sourcePackId().equals("mod/" + KhaosNexus.MODID)) mine++;
        }
        h.assertTrue(mine >= 2, "found our own dimension files (pack id check works): " + mine);
        for (String folder : new String[] {"dimension", "dimension_type", "worldgen/biome", "worldgen/noise_settings", "worldgen/structure", "point_of_interest_type"}) {
            for (var e : rm.listResources(folder, id -> id.getPath().endsWith(".json")).entrySet()) {
                if (e.getValue().sourcePackId().equals("mod/" + KhaosNexus.MODID)) {
                    h.assertTrue(e.getKey().getNamespace().equals(KhaosNexus.MODID), "only khaosnexus: ids in " + folder + ": " + e.getKey());
                }
            }
        }
        h.succeed();
    }
}

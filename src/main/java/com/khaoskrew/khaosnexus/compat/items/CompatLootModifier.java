package com.khaoskrew.khaosnexus.compat.items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Themed companion-mod loot in gate dungeon chests, data-driven:
 * data/<ns>/khaosnexus_compat_loot/<name>.json =
 * {"mod": "artifacts", "entries": [{"ranks": {"C": 0.04, "S": 0.12}, "stack": {"id": "...", "components": {...}}}]}.
 * A file is skipped unless its mod is installed and its compat toggle is on; chances are scaled by compatLootMultiplier.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public class CompatLootModifier extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KhaosNexus.MODID);
    public static final MapCodec<CompatLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, CompatLootModifier::new));
    static {
        SERIALIZERS.register("compat_loot", () -> CODEC);
    }

    public record Entry(String mod, Map<GateRank, Double> chances, JsonElement stack) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public CompatLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static boolean enabled(String mod) {
        if (!ModList.get().isLoaded(mod)) {
            return false;
        }
        SoftCompat.Hook hook = SoftCompat.byMod(mod);
        return hook == null || hook.active();
    }

    /** Rolls every enabled entry for a dungeon of this rank. Unknown items or bad components are skipped quietly. */
    public static List<ItemStack> roll(GateRank rank, RandomSource random, HolderLookup.Provider registries) {
        List<ItemStack> out = new ArrayList<>();
        double mult = Config.COMPAT_LOOT_MULTIPLIER.get();
        for (Entry e : ENTRIES) {
            Double chance = e.chances().get(rank);
            if (chance == null || mult <= 0 || !enabled(e.mod()) || random.nextDouble() >= chance * mult) {
                continue;
            }
            ItemStack s = stack(e, registries);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    public static ItemStack stack(Entry e, HolderLookup.Provider registries) {
        return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), e.stack()).result().orElse(ItemStack.EMPTY);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var id = context.getQueriedLootTableId();
        if (id.getNamespace().equals(KhaosNexus.MODID) && id.getPath().startsWith("dungeon/")) {
            GateRank rank = GateRank.byName(id.getPath().substring("dungeon/".length()).toUpperCase(Locale.ROOT));
            loot.addAll(roll(rank, context.getRandom(), context.getLevel().registryAccess()));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "khaosnexus_compat_loot");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            ENTRIES.clear();
            files.forEach((fid, json) -> {
                try {
                    JsonObject o = json.getAsJsonObject();
                    String mod = GsonHelper.getAsString(o, "mod");
                    for (JsonElement el : GsonHelper.getAsJsonArray(o, "entries")) {
                        JsonObject en = el.getAsJsonObject();
                        Map<GateRank, Double> ch = new EnumMap<>(GateRank.class);
                        GsonHelper.getAsJsonObject(en, "ranks").entrySet().forEach(r -> ch.put(GateRank.byName(r.getKey()), r.getValue().getAsDouble()));
                        ENTRIES.add(new Entry(mod, ch, en.get("stack")));
                    }
                } catch (RuntimeException ex) {
                    KhaosNexus.LOGGER.warn("Bad compat loot file {}: {}", fid, ex.toString());
                }
            });
            KhaosNexus.LOGGER.info("Loaded {} compat loot entries", ENTRIES.size());
        }
    }
}

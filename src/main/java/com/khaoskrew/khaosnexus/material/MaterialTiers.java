package com.khaoskrew.khaosnexus.material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Material tiers for big modpacks. Datapacks map item tags to a gate rank in
 * {@code data/<ns>/khaosnexus_material_tiers/*.json}: {"rank": "B", "tags": ["c:ingots/platinum"]}.
 * Tags that resolve to nothing (mod not installed) are skipped. Optionally every other {@code c:ingots/*} tag the
 * pack ships is picked up at a configurable rank, so unknown modded metals still show up in gate loot.
 * Resolved once tags are loaded; used by gate rewards and dungeon chests, and exposed for upgrade paths.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class MaterialTiers extends SimpleJsonResourceReloadListener {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    private static final Map<GateRank, List<String>> DECLARED = new EnumMap<>(GateRank.class);
    private static final Map<GateRank, List<Item>> RESOLVED = new EnumMap<>(GateRank.class);
    private static final Map<Item, GateRank> TIER_OF = new LinkedHashMap<>();

    public MaterialTiers() {
        super(GSON, "khaosnexus_material_tiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        DECLARED.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                GateRank rank = GateRank.byName(o.get("rank").getAsString());
                for (JsonElement t : o.getAsJsonArray("tags")) {
                    DECLARED.computeIfAbsent(rank, r -> new ArrayList<>()).add(t.getAsString());
                }
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Bad material tier file {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new MaterialTiers());
    }

    private static volatile boolean pending;

    private static void ensure() {
        if (pending && Config.SERVER_SPEC.isLoaded()) {
            resolve();
        }
    }

    @SubscribeEvent
    public static void onTags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            resolve();
        }
    }

    /** Builds the rank -> items table from the declared tags plus config overrides and auto-detected ingots. */
    public static void resolve() {
        if (!Config.SERVER_SPEC.isLoaded()) {
            pending = true;
            return;
        }
        pending = false;
        RESOLVED.clear();
        TIER_OF.clear();
        Map<GateRank, List<String>> declared = new EnumMap<>(GateRank.class);
        DECLARED.forEach((k, v) -> declared.put(k, new ArrayList<>(v)));
        for (String line : Config.MATERIAL_TIER_OVERRIDES.get()) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                declared.computeIfAbsent(GateRank.byName(parts[0].trim()), r -> new ArrayList<>()).add(parts[1].trim());
            }
        }
        Set<ResourceLocation> named = new HashSet<>();
        for (GateRank rank : GateRank.values()) {
            for (String tag : declared.getOrDefault(rank, List.of())) {
                ResourceLocation id = ResourceLocation.tryParse(tag);
                if (id != null && named.add(id)) {
                    add(rank, id);
                }
            }
        }
        if (Config.MATERIAL_AUTO_DETECT.get()) {
            GateRank auto = GateRank.byName(Config.MATERIAL_AUTO_RANK.get());
            BuiltInRegistries.ITEM.getTagNames()
                    .map(TagKey::location)
                    .filter(id -> id.getNamespace().equals("c") && id.getPath().startsWith("ingots/") && !named.contains(id))
                    .sorted()
                    .forEach(id -> add(auto, id));
        }
        KhaosNexus.LOGGER.info("Material tiers: {}", summary());
    }

    private static void add(GateRank rank, ResourceLocation tagId) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            Item item = holder.value();
            if (!TIER_OF.containsKey(item)) {
                TIER_OF.put(item, rank);
                RESOLVED.computeIfAbsent(rank, r -> new ArrayList<>()).add(item);
                return; // one representative item per tag
            }
        }
    }

    public static List<Item> forRank(GateRank rank) {
        ensure();
        return RESOLVED.getOrDefault(rank, List.of());
    }

    /** The tier a material belongs to, or null when it is not a known material. For upgrade paths and recipes. */
    public static GateRank tierOf(Item item) {
        ensure();
        return TIER_OF.get(item);
    }

    public static String summary() {
        StringBuilder b = new StringBuilder();
        for (GateRank rank : GateRank.values()) {
            b.append(rank).append('=').append(forRank(rank).size()).append(' ');
        }
        return b.toString().trim();
    }

    /**
     * Material reward for a cleared gate of this rank: mostly the rank's own tier, sometimes one tier lower,
     * rarely one tier higher. Empty when the pack has no material at or below the rank.
     */
    public static List<ItemStack> roll(GateRank rank, RandomSource random) {
        ensure();
        List<ItemStack> out = new ArrayList<>();
        if (!Config.MATERIAL_LOOT.get()) {
            return out;
        }
        int rolls = 1 + rank.ordinal() / 2;
        for (int i = 0; i < rolls; i++) {
            int shift = random.nextFloat() < 0.1F ? 1 : random.nextFloat() < 0.3F ? -1 : 0;
            int target = Math.max(0, Math.min(GateRank.values().length - 1, rank.ordinal() + shift));
            List<Item> pool = List.of();
            for (int t = target; t >= 0 && pool.isEmpty(); t--) {
                pool = forRank(GateRank.values()[t]);
            }
            if (!pool.isEmpty()) {
                out.add(new ItemStack(pool.get(random.nextInt(pool.size())), 1 + random.nextInt(2 + rank.ordinal())));
            }
        }
        return out;
    }
}

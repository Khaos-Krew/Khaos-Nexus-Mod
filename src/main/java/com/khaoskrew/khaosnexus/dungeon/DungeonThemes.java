package com.khaoskrew.khaosnexus.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Dungeon themes from data/<ns>/khaosnexus_dungeon_theme/<id>.json. A theme can name its own jigsaw pools; otherwise it
 * reskins the default pieces with a block palette plus light decoration. The theme is picked once per gate and saved there.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class DungeonThemes extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "rift_bastion");

    public record BiomeWeight(@Nullable TagKey<Biome> tag, @Nullable ResourceLocation biome, double multiplier) {}

    public record Decor(Block block, String on, float chance) {}

    public record Theme(ResourceLocation id, String nameKey, int weight, GateRank minRank, GateRank maxRank, List<String> requiredMods,
            List<BiomeWeight> biomes, Map<String, ResourceLocation> pools, Map<Block, Block> palette, List<Decor> decor,
            List<String> hazards) {
        public boolean available() {
            return this.requiredMods.stream().allMatch(m -> ModList.get().isLoaded(m))
                    && !Config.THEMES_DISABLED.get().contains(this.id.toString());
        }

        public boolean allows(GateRank rank) {
            return rank.ordinal() >= this.minRank.ordinal() && rank.ordinal() <= this.maxRank.ordinal();
        }
    }

    static final Map<ResourceLocation, Theme> THEMES = new LinkedHashMap<>();

    public DungeonThemes() {
        super(new Gson(), "khaosnexus_dungeon_theme");
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new DungeonThemes());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        THEMES.clear();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                THEMES.put(e.getKey(), parse(e.getKey(), e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Skipping dungeon theme {}: {}", e.getKey(), ex.getMessage());
            }
        });
        KhaosNexus.LOGGER.info("Loaded {} dungeon themes", THEMES.size());
    }

    public static Theme parse(ResourceLocation id, JsonObject o) {
        List<String> mods = new ArrayList<>();
        if (o.has("required_mods")) {
            o.getAsJsonArray("required_mods").forEach(m -> mods.add(m.getAsString()));
        }
        List<BiomeWeight> biomes = new ArrayList<>();
        if (o.has("biome_weights")) {
            for (JsonElement el : o.getAsJsonArray("biome_weights")) {
                JsonObject b = el.getAsJsonObject();
                String s = GsonHelper.getAsString(b, "biomes");
                double m = GsonHelper.getAsDouble(b, "multiplier", 1.0D);
                biomes.add(s.startsWith("#")
                        ? new BiomeWeight(TagKey.create(Registries.BIOME, ResourceLocation.parse(s.substring(1))), null, m)
                        : new BiomeWeight(null, ResourceLocation.parse(s), m));
            }
        }
        Map<String, ResourceLocation> pools = new HashMap<>();
        if (o.has("pools")) {
            o.getAsJsonObject("pools").entrySet().forEach(p -> pools.put(p.getKey(), ResourceLocation.parse(p.getValue().getAsString())));
        }
        Map<Block, Block> palette = new HashMap<>();
        if (o.has("palette")) {
            o.getAsJsonObject("palette").entrySet().forEach(p -> {
                Block from = block(p.getKey());
                Block to = block(p.getValue().getAsString());
                if (from != null && to != null) {
                    palette.put(from, to);
                }
            });
        }
        List<Decor> decor = new ArrayList<>();
        if (o.has("decor")) {
            for (JsonElement el : o.getAsJsonArray("decor")) {
                JsonObject d = el.getAsJsonObject();
                Block b = block(GsonHelper.getAsString(d, "block"));
                if (b != null) {
                    decor.add(new Decor(b, GsonHelper.getAsString(d, "on", "floor"), GsonHelper.getAsFloat(d, "chance", 0.05F)));
                }
            }
        }
        List<String> hazards = new ArrayList<>();
        if (o.has("hazards")) {
            o.getAsJsonArray("hazards").forEach(h -> hazards.add(h.isJsonObject() ? GsonHelper.getAsString(h.getAsJsonObject(), "type") : h.getAsString()));
        }
        String name = o.has("display_name") && o.get("display_name").isJsonObject()
                ? GsonHelper.getAsString(o.getAsJsonObject("display_name"), "translate")
                : "theme." + id.getNamespace() + "." + id.getPath();
        return new Theme(id, name, GsonHelper.getAsInt(o, "weight", 10),
                GateRank.byName(GsonHelper.getAsString(o, "min_rank", "E")), GateRank.byName(GsonHelper.getAsString(o, "max_rank", "S")),
                List.copyOf(mods), List.copyOf(biomes), Map.copyOf(pools), Map.copyOf(palette), List.copyOf(decor), List.copyOf(hazards));
    }

    @Nullable
    private static Block block(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) {
            return null; // optional-mod blocks simply drop out when the mod is absent
        }
        return BuiltInRegistries.BLOCK.get(rl);
    }

    @Nullable
    public static Theme get(@Nullable ResourceLocation id) {
        return id == null ? null : THEMES.get(id);
    }

    public static List<Theme> all() {
        return List.copyOf(THEMES.values());
    }

    public static String nameKey(@Nullable ResourceLocation id) {
        Theme t = get(id);
        return t != null ? t.nameKey() : "theme.khaosnexus.rift_bastion";
    }

    /** Weight of a theme for a gate in this biome (biome weighting scaled by dungeonThemes.biomeInfluence). */
    public static double weight(Theme t, Holder<Biome> biome) {
        double w = t.weight();
        double influence = Config.THEME_BIOME_INFLUENCE.get();
        if (influence > 0) {
            for (BiomeWeight b : t.biomes()) {
                boolean match = b.tag() != null ? biome.is(b.tag()) : biome.is(b.biome());
                if (match) {
                    w *= 1.0D + (b.multiplier() - 1.0D) * influence;
                }
            }
        }
        return w;
    }

    /** Picks the theme for a new gate. Forced theme first, then a weighted roll over eligible themes. */
    public static ResourceLocation pick(ServerLevel level, BlockPos pos, GateRank rank, RandomSource random) {
        String fixed = Config.THEME_FIXED.get();
        if (!fixed.isBlank()) {
            ResourceLocation f = ResourceLocation.tryParse(fixed);
            if (f != null && THEMES.containsKey(f)) {
                return f;
            }
        }
        if (!Config.THEMES_ENABLED.get()) {
            return DEFAULT;
        }
        Holder<Biome> biome = level.getBiome(pos);
        List<Theme> pool = THEMES.values().stream().filter(t -> t.available() && t.allows(rank)).toList();
        double total = pool.stream().mapToDouble(t -> weight(t, biome)).sum();
        if (total <= 0) {
            return DEFAULT;
        }
        double r = random.nextDouble() * total;
        for (Theme t : pool) {
            r -= weight(t, biome);
            if (r < 0) {
                return t.id();
            }
        }
        return pool.get(pool.size() - 1).id();
    }

    /** Reskins a built dungeon: palette swap (keeping matching block-state properties) then decoration. */
    public static void apply(ServerLevel level, Theme theme, BoundingBox box, RandomSource random) {
        if (theme.palette().isEmpty() && theme.decor().isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    BlockState s = level.getBlockState(c.set(x, y, z));
                    Block to = theme.palette().get(s.getBlock());
                    if (to != null) {
                        level.setBlock(c, copy(s, to.defaultBlockState()), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        if (theme.decor().isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY() + 1; y < box.maxY(); y++) {
                    if (!level.getBlockState(c.set(x, y, z)).isAir()) {
                        continue;
                    }
                    for (Decor d : theme.decor()) {
                        boolean ceiling = d.on().equals("ceiling");
                        BlockState support = level.getBlockState(n.set(x, ceiling ? y + 1 : y - 1, z));
                        if (support.isFaceSturdy(level, n, ceiling ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP)
                                && random.nextFloat() < d.chance()) {
                            BlockState place = d.block().defaultBlockState();
                            if (place.canSurvive(level, c)) {
                                level.setBlock(c, place, Block.UPDATE_CLIENTS);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copy(BlockState from, BlockState to) {
        for (Property p : from.getProperties()) {
            if (to.hasProperty(p)) {
                to = to.setValue(p, from.getValue(p));
            }
        }
        return to;
    }

    /** Hook for theme hazards (chill, heat, ...). Off by default: dungeonThemes.hazards. Nothing is implemented yet. */
    public static boolean hazardsActive(Theme theme) {
        return Config.THEME_HAZARDS.get() && !theme.hazards().isEmpty();
    }

    static Block unused() {
        return Blocks.AIR;
    }
}

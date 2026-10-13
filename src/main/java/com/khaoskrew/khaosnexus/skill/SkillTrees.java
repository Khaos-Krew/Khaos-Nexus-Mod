package com.khaoskrew.khaosnexus.skill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Class skill trees from data/<ns>/khaosnexus_skill_tree/<class>.json.
 * Node kinds: "primary"/"secondary" upgrade an active (stat power, cooldown or radius); "passive" and "capstone" add an attribute.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SkillTrees extends SimpleJsonResourceReloadListener {
    public enum Kind { PRIMARY, SECONDARY, PASSIVE, CAPSTONE }

    public enum Stat { POWER, COOLDOWN, RADIUS, ATTRIBUTE }

    public record Node(String id, Kind kind, Stat stat, double perRank, int maxRank, int cost, GateRank minRank, List<String> requires,
            @Nullable ResourceLocation attribute, AttributeModifier.Operation op, int x, int y) {}

    static final Map<HunterClass, List<Node>> TREES = new EnumMap<>(HunterClass.class);

    public SkillTrees() {
        super(new Gson(), "khaosnexus_skill_tree");
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new SkillTrees());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        TREES.clear();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                HunterClass c = HunterClass.byId(GsonHelper.getAsString(o, "class"));
                if (c == null) {
                    throw new IllegalArgumentException("unknown class");
                }
                List<Node> nodes = new ArrayList<>();
                for (JsonElement el : GsonHelper.getAsJsonArray(o, "nodes")) {
                    nodes.add(parse(el.getAsJsonObject()));
                }
                TREES.put(c, List.copyOf(nodes));
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Skipping skill tree {}: {}", e.getKey(), ex.getMessage());
            }
        });
        KhaosNexus.LOGGER.info("Loaded skill trees for {} classes", TREES.size());
    }

    public static Node parse(JsonObject n) {
        Kind kind = Kind.valueOf(GsonHelper.getAsString(n, "kind").toUpperCase(Locale.ROOT));
        Stat stat = Stat.valueOf(GsonHelper.getAsString(n, "stat", kind == Kind.PASSIVE || kind == Kind.CAPSTONE ? "attribute" : "power").toUpperCase(Locale.ROOT));
        List<String> req = new ArrayList<>();
        if (n.has("requires")) {
            n.getAsJsonArray("requires").forEach(r -> req.add(r.getAsString()));
        }
        String op = GsonHelper.getAsString(n, "operation", "add_value");
        AttributeModifier.Operation operation = switch (op) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        return new Node(GsonHelper.getAsString(n, "id"), kind, stat, GsonHelper.getAsDouble(n, "per_rank", 0.1D),
                GsonHelper.getAsInt(n, "max_rank", 1), GsonHelper.getAsInt(n, "cost", 1),
                GateRank.byName(GsonHelper.getAsString(n, "min_rank", "E")), List.copyOf(req),
                n.has("attribute") ? ResourceLocation.parse(GsonHelper.getAsString(n, "attribute")) : null, operation,
                GsonHelper.getAsInt(n, "x", 0), GsonHelper.getAsInt(n, "y", 0));
    }

    public static List<Node> tree(@Nullable HunterClass c) {
        return c == null ? List.of() : TREES.getOrDefault(c, List.of());
    }

    @Nullable
    public static Node node(@Nullable HunterClass c, String id) {
        return tree(c).stream().filter(n -> n.id().equals(id)).findFirst().orElse(null);
    }
}

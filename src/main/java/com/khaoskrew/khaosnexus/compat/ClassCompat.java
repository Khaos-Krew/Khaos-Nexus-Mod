package com.khaoskrew.khaosnexus.compat;

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
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Class passives on companion-mod attributes (Apothic Attributes, Iron's Spells, Ars Nouveau, or any mod). The bonuses
 * are datapack values in {@code data/<ns>/khaosnexus_class_compat/<class>.json}:
 * <pre>{"class": "caster", "bonuses": [{"attribute": "irons_spellbooks:spell_power", "amount": 0.1, "operation": "add_multiplied_base"}]}</pre>
 * An entry only applies when its attribute is registered and the owning mod's compat toggle is on; amounts are scaled
 * by {@code compat.classBonusScale}. There is no compile-time dependency on any of these mods.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ClassCompat extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "class_compat");

    public record Bonus(String modId, ResourceLocation attribute, double amount, AttributeModifier.Operation op) {}

    static final Map<HunterClass, List<Bonus>> BONUSES = new EnumMap<>(HunterClass.class);

    public ClassCompat() {
        super(new Gson(), "khaosnexus_class_compat");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        BONUSES.clear();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                JsonObject json = e.getValue().getAsJsonObject();
                HunterClass c = HunterClass.byId(GsonHelper.getAsString(json, "class"));
                if (c == null) {
                    throw new IllegalArgumentException("unknown class");
                }
                List<Bonus> list = BONUSES.computeIfAbsent(c, k -> new ArrayList<>());
                if (GsonHelper.getAsBoolean(json, "replace", false)) {
                    list.clear();
                }
                for (JsonElement el : GsonHelper.getAsJsonArray(json, "bonuses")) {
                    list.add(parse(el.getAsJsonObject()));
                }
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Skipping class compat file {}: {}", e.getKey(), ex.getMessage());
            }
        });
        KhaosNexus.LOGGER.info("Loaded companion class bonuses for {} classes", BONUSES.size());
    }

    public static Bonus parse(JsonObject o) {
        ResourceLocation attr = ResourceLocation.parse(GsonHelper.getAsString(o, "attribute"));
        String op = GsonHelper.getAsString(o, "operation", "add_value").toLowerCase(Locale.ROOT);
        AttributeModifier.Operation operation = switch (op) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        return new Bonus(GsonHelper.getAsString(o, "mod", attr.getNamespace()), attr, GsonHelper.getAsDouble(o, "amount"), operation);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new ClassCompat());
    }

    /** Re-apply after /reload so datapack edits take effect without relogging. */
    @SubscribeEvent
    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) { // a /reload; logins are handled by ClassEvents
            event.getRelevantPlayers().forEach(ClassService::reapply);
        }
    }

    public static List<Bonus> bonuses(HunterClass c) {
        return List.copyOf(BONUSES.getOrDefault(c, List.of()));
    }

    static boolean enabled(String modId) {
        ModConfigSpec.BooleanValue toggle = switch (modId) {
            case "apothic_attributes", "apotheosis" -> Config.COMPAT_APOTHEOSIS;
            case "irons_spellbooks" -> Config.COMPAT_IRONS;
            case "ars_nouveau" -> Config.COMPAT_ARS;
            default -> null;
        };
        return (toggle == null || toggle.get()) && ModList.get().isLoaded(modId);
    }

    /** Applies the companion bonuses for this class; removes all of them first. */
    public static void apply(ServerPlayer player, HunterClass hunterClass) {
        clear(player);
        if (hunterClass == null) {
            return;
        }
        double scale = Config.CLASS_BONUS_SCALE.get();
        for (Bonus bonus : BONUSES.getOrDefault(hunterClass, List.of())) {
            AttributeInstance instance = instance(player, bonus.attribute());
            if (instance != null && enabled(bonus.modId())) {
                // one modifier per attribute: sum duplicates of the same op
                AttributeModifier old = instance.getModifier(MODIFIER_ID);
                double amount = bonus.amount() * scale + (old != null && old.operation() == bonus.op() ? old.amount() : 0.0D);
                instance.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID, amount, bonus.op()));
            }
        }
    }

    public static void clear(ServerPlayer player) {
        for (Holder<Attribute> h : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = player.getAttribute(h);
            if (instance != null) {
                instance.removeModifier(MODIFIER_ID);
            }
        }
    }

    private static AttributeInstance instance(ServerPlayer player, ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).map(h -> player.getAttribute((Holder<Attribute>) h)).orElse(null);
    }

    /** Count of bonuses that are live in this game, for the compat status command. */
    public static long activeCount() {
        return BONUSES.values().stream().flatMap(List::stream)
                .filter(b -> enabled(b.modId()) && BuiltInRegistries.ATTRIBUTE.containsKey(b.attribute())).count();
    }

    /** Bonuses whose mod is installed but whose attribute id does not exist: a wrong id in the datapack. */
    public static List<String> brokenIds() {
        List<String> out = new ArrayList<>();
        BONUSES.forEach((c, list) -> list.stream()
                .filter(b -> ModList.get().isLoaded(b.modId()) && !BuiltInRegistries.ATTRIBUTE.containsKey(b.attribute()))
                .forEach(b -> out.add(c.id() + ": " + b.attribute())));
        return out;
    }
}

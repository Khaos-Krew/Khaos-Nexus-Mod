package com.khaoskrew.khaosnexus.forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.material.MaterialTiers;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Hunter Forge recipes from {@code data/<ns>/khaosnexus_forge/*.json}. Each recipe names the gear it accepts (items or
 * #tags), the attribute it raises, and one entry per forge level: the hunter rank needed, the material tier and count
 * (any material of that tier or higher, see {@link MaterialTiers}), rift essence and rift cores. A recipe can add a
 * class bonus based on the forging hunter's class, which turns ordinary gear into a class weapon.
 */
public class ForgeRecipes extends SimpleJsonResourceReloadListener {
    public static final String TAG = "KhaosForge";
    public static final String CLASS_TAG = "KhaosForgeClass";

    public record Level(GateRank rank, GateRank tier, int count, int essence, int core, double bonus) {}

    public record ClassBonus(ResourceLocation attribute, double perLevel) {}

    public record Recipe(ResourceLocation id, String name, List<String> inputs, ResourceLocation attribute, String slot,
            List<Level> levels, Map<String, ClassBonus> classBonus) {
        public boolean accepts(ItemStack stack) {
            for (String in : this.inputs) {
                if (in.startsWith("#")) {
                    ResourceLocation tag = ResourceLocation.tryParse(in.substring(1));
                    if (tag != null && stack.is(TagKey.create(Registries.ITEM, tag))) {
                        return true;
                    }
                } else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(in)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum Status { FORGED, PREVIEW, NO_RECIPE, MAXED, RANK_LOCKED, MISSING }

    public record Result(Status status, int level, Level next, Component message) {}

    private static final Map<ResourceLocation, Recipe> RECIPES = new LinkedHashMap<>();

    public ForgeRecipes() {
        super(new Gson(), "khaosnexus_forge");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        RECIPES.clear();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                RECIPES.put(e.getKey(), parse(e.getKey(), e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Skipping Hunter Forge recipe {}: {}", e.getKey(), ex.getMessage());
            }
        });
        KhaosNexus.LOGGER.info("Loaded {} Hunter Forge recipes", RECIPES.size());
    }

    public static Recipe parse(ResourceLocation id, JsonObject json) {
        List<String> inputs = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "inputs")) {
            inputs.add(el.getAsString());
        }
        List<Level> levels = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "levels")) {
            JsonObject o = el.getAsJsonObject();
            levels.add(new Level(GateRank.byName(GsonHelper.getAsString(o, "rank")), GateRank.byName(GsonHelper.getAsString(o, "tier")),
                    GsonHelper.getAsInt(o, "count", 4), GsonHelper.getAsInt(o, "essence", 0), GsonHelper.getAsInt(o, "core", 0),
                    GsonHelper.getAsDouble(o, "bonus")));
        }
        Map<String, ClassBonus> bonus = new HashMap<>();
        if (json.has("class_bonus")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("class_bonus").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                bonus.put(e.getKey(), new ClassBonus(ResourceLocation.parse(GsonHelper.getAsString(o, "attribute")),
                        GsonHelper.getAsDouble(o, "per_level")));
            }
        }
        return new Recipe(id, GsonHelper.getAsString(json, "name"), inputs, ResourceLocation.parse(GsonHelper.getAsString(json, "attribute")),
                GsonHelper.getAsString(json, "slot", "mainhand"), levels, bonus);
    }

    /** For tests and datapack tools. */
    public static void put(Recipe recipe) {
        RECIPES.put(recipe.id(), recipe);
    }

    public static List<Recipe> all() {
        return List.copyOf(RECIPES.values());
    }

    public static Recipe match(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Recipe r : RECIPES.values()) {
            if (r.accepts(stack)) {
                return r;
            }
        }
        return null;
    }

    public static int level(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(TAG);
    }

    public static Result upgrade(ServerPlayer player, ItemStack stack, boolean previewOnly) {
        return upgrade(player, stack, previewOnly, player.isCreative());
    }

    /** {@code free} skips the material cost (creative mode). */
    public static Result upgrade(ServerPlayer player, ItemStack stack, boolean previewOnly, boolean free) {
        Recipe recipe = match(stack);
        if (recipe == null) {
            return new Result(Status.NO_RECIPE, 0, null, Component.translatable("khaosnexus.forge.no_recipe"));
        }
        int cur = level(stack);
        if (cur >= recipe.levels().size()) {
            return new Result(Status.MAXED, cur, null, Component.translatable("khaosnexus.forge.maxed", cur));
        }
        Level next = recipe.levels().get(cur);
        Component cost = cost(next);
        if (HunterRanks.rankOf(player).ordinal() < next.rank().ordinal()) {
            return new Result(Status.RANK_LOCKED, cur, next,
                    Component.translatable("khaosnexus.forge.rank_locked", cur + 1, Component.translatable(next.rank().translationKey())));
        }
        if (previewOnly) {
            return new Result(Status.PREVIEW, cur, next, Component.translatable("khaosnexus.forge.preview", cur + 1, cost));
        }
        Inventory inv = player.getInventory();
        if (!free && (countMaterials(inv, next.tier()) < next.count()
                || count(inv, HunterForge.RIFT_ESSENCE.get()) < next.essence() || count(inv, HunterForge.RIFT_CORE.get()) < next.core())) {
            return new Result(Status.MISSING, cur, next, Component.translatable("khaosnexus.forge.missing", cur + 1, cost));
        }
        if (!free) {
            takeMaterials(inv, next.tier(), next.count());
            take(inv, HunterForge.RIFT_ESSENCE.get(), next.essence());
            take(inv, HunterForge.RIFT_CORE.get(), next.core());
        }
        HunterClass hunterClass = ClassService.equipped(player);
        apply(stack, recipe, cur + 1, hunterClass == null ? "" : hunterClass.id());
        return new Result(Status.FORGED, cur + 1, next, Component.translatable("khaosnexus.forge.forged", cur + 1));
    }

    public static Component cost(Level l) {
        Component c = Component.translatable("khaosnexus.forge.cost", l.count(), Component.translatable(l.tier().translationKey()), l.essence());
        return l.core() > 0 ? c.copy().append(Component.translatable("khaosnexus.forge.cost_core", l.core())) : c;
    }

    /** Writes the forge level and rebuilds this recipe's attribute modifiers on the stack. */
    public static void apply(ItemStack stack, Recipe recipe, int level, String classId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG, level);
        if (!classId.isEmpty() && !tag.contains(CLASS_TAG)) {
            tag.putString(CLASS_TAG, classId);
        }
        String boundClass = tag.getString(CLASS_TAG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        EquipmentSlotGroup group = slot(stack, recipe.slot());
        ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "forge_" + group.getSerializedName());
        ResourceLocation classModId = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "forge_class_" + group.getSerializedName());
        ItemAttributeModifiers base = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (base.modifiers().isEmpty()) {
            base = stack.getItem().getDefaultAttributeModifiers(stack);
        }
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : base.modifiers()) {
            if (!e.modifier().id().equals(modId) && !e.modifier().id().equals(classModId)) {
                builder.add(e.attribute(), e.modifier(), e.slot());
            }
        }
        double scale = com.khaoskrew.khaosnexus.compat.ApothCompat.forgeScale();
        double bonus = recipe.levels().get(level - 1).bonus() * scale;
        attribute(recipe.attribute()).ifPresent(h -> builder.add(h, new AttributeModifier(modId, bonus, AttributeModifier.Operation.ADD_VALUE), group));
        ClassBonus cb = recipe.classBonus().get(boundClass);
        if (cb != null) {
            attribute(cb.attribute()).ifPresent(h -> builder.add(h,
                    new AttributeModifier(classModId, cb.perLevel() * level * scale, AttributeModifier.Operation.ADD_VALUE), group));
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    private static java.util.Optional<Holder<Attribute>> attribute(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).map(h -> (Holder<Attribute>) h);
    }

    private static EquipmentSlotGroup slot(ItemStack stack, String slot) {
        if ("armor".equals(slot) && stack.getItem() instanceof ArmorItem armor) {
            return EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
        }
        return switch (slot) {
            case "offhand" -> EquipmentSlotGroup.OFFHAND;
            case "hand" -> EquipmentSlotGroup.HAND;
            case "armor" -> EquipmentSlotGroup.ARMOR;
            case "any" -> EquipmentSlotGroup.ANY;
            default -> EquipmentSlotGroup.MAINHAND;
        };
    }

    private static boolean qualifies(ItemStack s, GateRank tier) {
        GateRank t = s.isEmpty() ? null : MaterialTiers.tierOf(s.getItem());
        return t != null && t.ordinal() >= tier.ordinal();
    }

    public static int countMaterials(Inventory inv, GateRank tier) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (qualifies(s, tier)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Takes the lowest qualifying tiers first so rare materials are spent last. */
    private static void takeMaterials(Inventory inv, GateRank tier, int amount) {
        for (int t = tier.ordinal(); t < GateRank.values().length && amount > 0; t++) {
            for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && MaterialTiers.tierOf(s.getItem()) == GateRank.values()[t]) {
                    int take = Math.min(amount, s.getCount());
                    s.shrink(take);
                    amount -= take;
                }
            }
        }
    }

    private static int count(Inventory inv, Item item) {
        return inv.countItem(item);
    }

    private static void take(Inventory inv, Item item, int amount) {
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                amount -= take;
            }
        }
    }

    /** Lines for the Hunter Card forge tab. */
    public static List<com.khaoskrew.khaosnexus.network.CardPayload.ForgeLine> cardLines(ServerPlayer player) {
        List<com.khaoskrew.khaosnexus.network.CardPayload.ForgeLine> out = new ArrayList<>();
        GateRank rank = HunterRanks.rankOf(player);
        for (Recipe r : RECIPES.values()) {
            for (int i = 0; i < r.levels().size(); i++) {
                Level l = r.levels().get(i);
                out.add(new com.khaoskrew.khaosnexus.network.CardPayload.ForgeLine(r.name(), i + 1, l.rank().name(), l.tier().name(),
                        l.count(), l.essence(), l.core(), rank.ordinal() >= l.rank().ordinal()));
            }
        }
        return out;
    }

}

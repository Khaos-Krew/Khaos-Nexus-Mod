package com.khaoskrew.khaosnexus.compat;

import java.util.List;
import java.util.Map;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterClass;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Class passives on companion-mod attributes: Apotheosis (Apothic Attributes) for all 8 classes, light Iron's Spells
 * support and Ars Nouveau hooks for the magic classes. Attributes are looked up by id at runtime, so nothing loads
 * unless that mod is installed and its toggle is on; there is no compile-time dependency on any of them.
 */
public final class ClassCompat {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "class_compat");

    record Bonus(String modId, String attribute, double amount, AttributeModifier.Operation op) {}

    private static Bonus apoth(String attr, double amount) {
        return new Bonus("apothic_attributes", "apothic_attributes:" + attr, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static Bonus irons(String attr, double pct) {
        return new Bonus("irons_spellbooks", "irons_spellbooks:" + attr, pct, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static Bonus ars(String attr, double amount) {
        return new Bonus("ars_nouveau", "ars_nouveau:ars_nouveau.perk." + attr, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    static final Map<HunterClass, List<Bonus>> BONUSES = Map.of(
            HunterClass.TANK, List.of(apoth("ghost_health", 4.0D), apoth("overheal", 0.05D)),
            HunterClass.ASSASSIN, List.of(apoth("crit_chance", 0.08D), apoth("crit_damage", 0.15D), apoth("dodge_chance", 0.05D)),
            HunterClass.CASTER, List.of(apoth("prot_pierce", 2.0D), irons("spell_power", 0.10D), irons("max_mana", 0.10D),
                    irons("cooldown_reduction", 0.05D), ars("spell_damage", 1.0D), ars("max_mana", 25.0D)),
            HunterClass.RANGER, List.of(apoth("arrow_damage", 0.10D), apoth("arrow_velocity", 0.10D), apoth("draw_speed", 0.10D)),
            HunterClass.LIFEBINDER, List.of(apoth("healing_received", 0.15D), irons("holy_spell_power", 0.10D),
                    irons("nature_spell_power", 0.05D), irons("mana_regen", 0.10D), ars("mana_regen", 1.0D)),
            HunterClass.BERSERKER, List.of(apoth("life_steal", 0.05D), apoth("armor_shred", 0.10D), apoth("current_hp_damage", 0.02D)),
            HunterClass.SUMMONER, List.of(apoth("ghost_health", 2.0D), irons("summon_damage", 0.15D), irons("evocation_spell_power", 0.05D),
                    ars("max_mana", 15.0D)),
            HunterClass.NECROMANCER, List.of(apoth("life_steal", 0.04D), irons("blood_spell_power", 0.10D), irons("summon_damage", 0.10D),
                    ars("spell_damage", 0.5D)));

    private ClassCompat() {}

    private static boolean enabled(String modId) {
        ModConfigSpec.BooleanValue toggle = switch (modId) {
            case "apothic_attributes" -> Config.COMPAT_APOTHEOSIS;
            case "irons_spellbooks" -> Config.COMPAT_IRONS;
            case "ars_nouveau" -> Config.COMPAT_ARS;
            default -> null;
        };
        return toggle != null && toggle.get() && ModList.get().isLoaded(modId);
    }

    /** Applies the companion bonuses for this class; removes all of them first. */
    public static void apply(ServerPlayer player, HunterClass hunterClass) {
        clear(player);
        if (hunterClass == null) {
            return;
        }
        for (Bonus bonus : BONUSES.getOrDefault(hunterClass, List.of())) {
            AttributeInstance instance = instance(player, bonus);
            if (instance != null && enabled(bonus.modId())) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID, bonus.amount(), bonus.op()));
            }
        }
    }

    public static void clear(ServerPlayer player) {
        for (List<Bonus> list : BONUSES.values()) {
            for (Bonus bonus : list) {
                AttributeInstance instance = instance(player, bonus);
                if (instance != null) {
                    instance.removeModifier(MODIFIER_ID);
                }
            }
        }
    }

    private static AttributeInstance instance(ServerPlayer player, Bonus bonus) {
        ResourceLocation id = ResourceLocation.tryParse(bonus.attribute());
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).map(h -> player.getAttribute((Holder<Attribute>) h)).orElse(null);
    }

    /** Count of bonuses that are live in this game, for the compat status command. */
    public static long activeCount() {
        return BONUSES.values().stream().flatMap(List::stream)
                .filter(b -> enabled(b.modId()) && BuiltInRegistries.ATTRIBUTE.containsKey(ResourceLocation.parse(b.attribute()))).count();
    }
}

package com.khaoskrew.khaosnexus.hunter;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.khaoskrew.khaosnexus.KhaosNexus;

public enum HunterClass {
    TANK("tank", Attributes.ARMOR, 4.0D),
    ASSASSIN("assassin", Attributes.MOVEMENT_SPEED, 0.03D),
    CASTER("caster", Attributes.LUCK, 2.0D),
    RANGER("ranger", Attributes.ATTACK_SPEED, 0.4D),
    LIFEBINDER("lifebinder", Attributes.MAX_HEALTH, 8.0D),
    BERSERKER("berserker", Attributes.ATTACK_DAMAGE, 3.0D),
    SUMMONER("summoner", Attributes.MAX_HEALTH, 4.0D),
    NECROMANCER("necromancer", Attributes.ARMOR_TOUGHNESS, 2.0D);

    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "class_passive");

    private final String id;
    private final Holder<Attribute> attribute;
    private final double amount;

    HunterClass(String id, Holder<Attribute> attribute, double amount) {
        this.id = id;
        this.attribute = attribute;
        this.amount = amount;
    }

    public String id() {
        return this.id;
    }

    public Holder<Attribute> attribute() {
        return this.attribute;
    }

    public AttributeModifier modifier() {
        return new AttributeModifier(MODIFIER_ID, this.amount, AttributeModifier.Operation.ADD_VALUE);
    }

    public String translationKey() {
        return "khaosnexus.class." + this.id;
    }

    public String abilityKey() {
        return "khaosnexus.ability." + this.id + ".primary";
    }

    public String altAbilityKey() {
        return "khaosnexus.ability." + this.id + ".secondary";
    }

    public static HunterClass byId(String id) {
        if (id == null) {
            return null;
        }
        String needle = id.toLowerCase(Locale.ROOT);
        for (HunterClass hunterClass : values()) {
            if (hunterClass.id.equals(needle)) {
                return hunterClass;
            }
        }
        return null;
    }
}

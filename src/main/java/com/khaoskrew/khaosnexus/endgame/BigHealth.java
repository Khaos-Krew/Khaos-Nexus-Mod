package com.khaoskrew.khaosnexus.endgame;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Vanilla caps max health at 1024. Endgame bosses that need more keep the capped health bar and take proportionally
 * less damage instead, so the effective health matches the tuned value.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class BigHealth {
    public static final String SCALE = "khaosnexus.damage_taken_scale";

    private BigHealth() {}

    public static void set(LivingEntity mob, double effective) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.setBaseValue(effective);
        double actual = health.getValue();
        mob.setHealth((float) actual);
        if (actual > 0 && effective > actual + 0.5D) {
            mob.getPersistentData().putDouble(SCALE, actual / effective);
        } else {
            mob.getPersistentData().remove(SCALE);
        }
    }

    /** Effective max health, accounting for the damage scale. */
    public static double effectiveMax(LivingEntity mob) {
        double s = mob.getPersistentData().contains(SCALE) ? mob.getPersistentData().getDouble(SCALE) : 1.0D;
        return mob.getMaxHealth() / s;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().getPersistentData().contains(SCALE)) {
            event.setAmount((float) (event.getAmount() * event.getEntity().getPersistentData().getDouble(SCALE)));
        }
    }
}

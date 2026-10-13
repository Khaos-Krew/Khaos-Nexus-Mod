package com.khaoskrew.khaosnexus.compat.items;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.mob.RiftMob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Special effects of the rift artifacts. Curios is only touched after {@link CompatItems#artifacts()} is true. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class RiftArtifacts {
    private RiftArtifacts() {}

    static boolean worn(LivingEntity e, net.neoforged.neoforge.registries.DeferredItem<Item> item) {
        return item != null && CompatItems.artifacts() && SoftCompat.ARTIFACTS.active() && CuriosBridge.equipped(e, item.get());
    }

    /** Rift Lantern: night vision while a rift mob is within 24 blocks (dungeons, gate waves). */
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 20 != 0 || !worn(p, CompatItems.RIFT_LANTERN)) {
            return;
        }
        if (!p.level().getEntitiesOfClass(RiftMob.class, p.getBoundingBox().inflate(24)).isEmpty()) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
    }

    /** Voidstep Anklet: falls deal half damage. */
    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (worn(event.getEntity(), CompatItems.VOIDSTEP_ANKLET)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
        }
    }

    /** Gatebreaker Band: +20% damage against rift mobs. */
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof RiftMob && event.getSource().getEntity() instanceof LivingEntity attacker
                && worn(attacker, CompatItems.GATEBREAKER_BAND)) {
            event.setNewDamage(event.getNewDamage() * 1.2F);
        }
    }
}

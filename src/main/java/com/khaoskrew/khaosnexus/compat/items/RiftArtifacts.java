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
        return item != null && CompatItems.artifacts() && SoftCompat.ARTIFACTS.active() && safeEquipped(e, item.get());
    }

    private static boolean safeEquipped(LivingEntity e, Item item) {
        try {
            return CuriosBridge.equipped(e, item);
        } catch (LinkageError ex) {
            return false; // CuriosBridge itself failed to link (Curios API missing)
        }
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

    /** Warden's Broken Chain: the Rift Warden's hits deal 25% less and can't slow the wearer. */
    @SubscribeEvent
    public static void chain(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof com.khaoskrew.khaosnexus.mob.RiftBoss b
                && b.getType() == com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_WARDEN.get()
                && worn(event.getEntity(), CompatItems.WARDENS_BROKEN_CHAIN)) {
            event.setNewDamage(event.getNewDamage() * 0.75F);
            event.getEntity().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    /** Guardian's Plume: no knockback from the Spire Guardian (its spin). */
    @SubscribeEvent
    public static void plume(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        LivingEntity e = event.getEntity();
        if (e.getLastHurtByMob() != null && e.getLastHurtByMob().getType() == com.khaoskrew.khaosnexus.mob.ModEntities.SPIRE_GUARDIAN.get()
                && worn(e, CompatItems.GUARDIANS_PLUME)) {
            event.setCanceled(true);
        }
    }

    /** Boss trophies: the Warden and the Guardian each drop their trophy 20% of the time (party-killed or solo). */
    @SubscribeEvent
    public static void trophies(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (!CompatItems.artifacts() || !SoftCompat.ARTIFACTS.active() || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        net.neoforged.neoforge.registries.DeferredItem<Item> drop =
                dead.getType() == com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_WARDEN.get() ? CompatItems.WARDENS_BROKEN_CHAIN
                : dead.getType() == com.khaoskrew.khaosnexus.mob.ModEntities.SPIRE_GUARDIAN.get() ? CompatItems.GUARDIANS_PLUME : null;
        if (drop != null && dead.getRandom().nextFloat() < 0.2F) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), new net.minecraft.world.item.ItemStack(drop.get())));
        }
    }
}

package com.khaoskrew.khaosnexus.raid;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class RaidEvents {
    private RaidEvents() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Mob mob
                && event.getSource().getEntity() instanceof ServerPlayer player
                && mob.level() instanceof ServerLevel) {
            RaidService.recordDamage(mob, player, event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob
                && mob.level() instanceof ServerLevel level) {
            ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
            RaidService.onBossKilled(level, mob, killer);
        }
    }
}

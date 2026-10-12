package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SpireEvents {
    private SpireEvents() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension() == SpireService.DIMENSION) {
            event.setCanceled(true);
            SpireService.kickOnDeath(player);
            return;
        }
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level) {
            ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
            SpireService.onBossKilled(level, mob, killer);
        }
    }
}

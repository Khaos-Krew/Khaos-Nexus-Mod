package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ClassEvents {
    private ClassEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassService.reapply(player);
            Titles.apply(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassService.reapply(player);
            Titles.apply(player);
        }
    }

    @SubscribeEvent
    public static void onMinionHurtOwner(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof Mob mob)) {
            return;
        }
        if (ClassAbilities.isOwnedMinion(player, mob)) {
            event.setCanceled(true);
        }
    }
}

package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.mob.ModEntities;
import com.khaoskrew.khaosnexus.saga.ModSounds;
import com.khaoskrew.khaosnexus.saga.Saga;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/** Hollow King theme while Morvane is alive within 48 blocks; the camp ambience elsewhere in the Underthrone. */
@EventBusSubscriber(modid = KhaosNexus.MODID, value = Dist.CLIENT)
public final class BossMusic {
    private static final Music KING = new Music(ModSounds.MUSIC_KING, 0, 0, true);
    private static final Music CAMP = new Music(ModSounds.MUSIC_CAMP, 600, 2400, false);

    private BossMusic() {}

    @SubscribeEvent
    public static void select(SelectMusicEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean king = !mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mc.player.getBoundingBox().inflate(48.0D),
                e -> e.getType() == ModEntities.HOLLOW_KING.get() && e.isAlive()).isEmpty();
        if (king) event.overrideMusic(KING);
        else if (mc.level.dimension() == Saga.UNDERTHRONE) event.setMusic(CAMP);
    }
}

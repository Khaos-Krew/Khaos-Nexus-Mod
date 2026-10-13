package com.khaoskrew.khaosnexus.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A Rift mob with a boss bar (used by the raid Warden and the Spire Guardian). */
public class RiftBoss extends RiftMob {
    private final ServerBossEvent bossEvent;

    public RiftBoss(EntityType<? extends RiftBoss> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10)
                .setDarkenScreen(true);
        this.xpReward = 50;
    }

    public static RiftBoss warden(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.RED);
    }

    public static RiftBoss guardian(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.PURPLE);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}

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
    /** Weapon drawn (in hand) vs sheathed (on the back). Synced so the client shows the right sword bone. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DRAWN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RiftBoss.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /** Ticks without a target before the boss sheathes again. */
    private static final int SHEATHE_DELAY = 100;
    private final ServerBossEvent bossEvent;
    private int calmTicks;

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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAWN, false);
    }

    /** Client: game time when DRAWN last changed, so the renderer swaps sword bones mid-draw. */
    private long drawChangedAt = -100;

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DRAWN.equals(key)) {
            this.drawChangedAt = this.tickCount;
        }
    }

    /** Sword shown in hand? Swaps 8 ticks into the draw/sheathe animation (the grab frame). */
    public boolean showSwordInHand() {
        boolean switching = this.tickCount - this.drawChangedAt < 8;
        return switching ? !this.isWeaponDrawn() : this.isWeaponDrawn();
    }

    @Override
    protected boolean canStartAttack() {
        return this.isWeaponDrawn() && this.tickCount - this.drawChangedAt >= 18;
    }

    public boolean isWeaponDrawn() {
        return this.entityData.get(DRAWN);
    }

    private void setWeaponDrawn(boolean drawn) {
        if (drawn != this.isWeaponDrawn()) {
            this.entityData.set(DRAWN, drawn);
            this.drawChangedAt = this.tickCount;
            this.triggerAnim("action", drawn ? "draw" : "sheathe");
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.calmTicks = 0;
            this.setWeaponDrawn(true);
        } else if (this.isWeaponDrawn() && ++this.calmTicks > SHEATHE_DELAY) {
            this.setWeaponDrawn(false);
        }
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

    @Override
    public void setTarget(net.minecraft.world.entity.LivingEntity target) {
        boolean newTarget = target != null && this.getTarget() == null;
        super.setTarget(target);
        if (newTarget && !this.level().isClientSide) {
            this.setWeaponDrawn(true);
            this.triggerAnim("action", "roar");
        }
    }
}

package com.khaoskrew.khaosnexus.mob;

import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shared melee AI for the Rift gate mobs. The gate rank is synced so the client can tint the glow. */
public class RiftMob extends Monster implements GeoEntity {
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final String animPrefix;
    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(RiftMob.class, EntityDataSerializers.INT);

    public RiftMob(EntityType<? extends RiftMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.animPrefix = "animation." + EntityType.getKey(type).getPath() + ".";
    }

    public static AttributeSupplier.Builder attributes(double health, double damage, double speed, double knockbackResist) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResist)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RANK, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** @return the gate rank this mob came from, or null for a mob summoned outside a gate. */
    public GateRank getGateRank() {
        int ordinal = this.entityData.get(RANK);
        return ordinal < 0 || ordinal >= GateRank.values().length ? null : GateRank.values()[ordinal];
    }

    public void setGateRank(GateRank rank) {
        this.entityData.set(RANK, rank == null ? -1 : rank.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        GateRank rank = this.getGateRank();
        if (rank != null) {
            tag.putString("GateRank", rank.name());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GateRank")) {
            this.setGateRank(GateRank.byName(tag.getString("GateRank")));
        }
    }

    protected RawAnimation anim(String name, boolean loop) {
        RawAnimation raw = RawAnimation.begin();
        return loop ? raw.thenLoop(this.animPrefix + name) : raw.thenPlay(this.animPrefix + name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = this.anim("idle", true);
        RawAnimation walk = this.anim("walk", true);
        controllers.add(new AnimationController<>(this, "movement", 6,
                state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<RiftMob> action = new AnimationController<>(this, "action", 3, state -> software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("attack", this.anim("attack", false));
        if (this instanceof RiftBoss) {
            action.triggerableAnim("roar", this.anim("roar", false));
            if ("animation.rift_warden.".equals(this.animPrefix)) {
                action.triggerableAnim("draw", this.anim("draw", false)).triggerableAnim("sheathe", this.anim("sheathe", false));
            }
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.triggerAnim("action", "attack");
        }
        return hit;
    }
}

package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.ApothCompat;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Gate Mimic (1.2): sits among the real chests in C+ gate dungeons looking exactly like one. Opening it, hitting it, or
 * standing right beside it wakes it: it rattles, flings its lid open, unfolds four legs and hunts. Bites land on the snap
 * of the lid. On death it spills two rolls of the dungeon's own loot table, so killing it pays better than the chest it
 * replaced.
 */
public class GateMimic extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(GateMimic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DEBUG_POSE = SynchedEntityData.defineId(GateMimic.class, EntityDataSerializers.STRING);
    public static final double WAKE_RANGE = 2.2D;
    static final int WAKE_STUN = 24, BITE_IMPACT = 9, DEATH_TICKS = 48;
    private static final String P = "animation.gate_mimic.";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stun, bite;
    private float restYaw;

    public GateMimic(EntityType<? extends GateMimic> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 44.0D).add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D).add(Attributes.ARMOR, 4.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(AWAKE, false);
        b.define(DEBUG_POSE, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true) {
            @Override public boolean canUse() { return GateMimic.this.ready() && super.canUse(); }
            @Override public boolean canContinueToUse() { return GateMimic.this.ready() && super.canContinueToUse(); }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> this.isAwake()));
    }

    public boolean isAwake() { return this.entityData.get(AWAKE); }
    boolean ready() { return this.isAwake() && this.stun <= 0 && this.isAlive(); }
    public String getDebugPose() { return this.entityData.get(DEBUG_POSE); }

    /** Facing to hold while dormant (the chest it replaced faced this way). */
    public void rest(float yaw) {
        this.restYaw = yaw;
        this.setYRot(yaw); this.yBodyRot = yaw; this.yHeadRot = yaw;
    }

    public void wake(LivingEntity by) {
        if (this.isAwake() || this.level().isClientSide) return;
        this.entityData.set(AWAKE, true);
        this.stun = WAKE_STUN;
        this.setCustomName(null);
        this.triggerAnim("action", "roar");
        this.level().playSound(null, this.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.HOSTILE, 1.0F, 0.6F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.7F, 1.6F);
        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.8D, this.getZ(), 14, 0.3D, 0.2D, 0.3D, 0.02D);
        if (by != null && !(by instanceof Player p && (p.isCreative() || p.isSpectator()))) this.setTarget(by);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (!this.isAwake()) {
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
            this.getNavigation().stop();
            this.setTarget(null);
            this.rest(this.restYaw);
            if (this.getDebugPose().isEmpty() && this.tickCount % 5 == 0) {
                Player p = this.level().getNearestPlayer(this, WAKE_RANGE);
                if (p != null && !p.isCreative() && !p.isSpectator()) this.wake(p);
            }
            return;
        }
        if (this.stun > 0) { this.stun--; this.getNavigation().stop(); }
        if (this.bite > 0 && --this.bite == 0) {
            LivingEntity t = this.getTarget();
            this.level().playSound(null, this.blockPosition(), SoundEvents.CHEST_CLOSE, SoundSource.HOSTILE, 1.0F, 0.7F);
            if (t != null && t.isAlive() && this.distanceToSqr(t) < 3.4D * 3.4D) super.doHurtTarget(t);
        }
    }

    /** MeleeAttackGoal swings here; the damage waits for the lid to snap shut (impact frame). */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.bite > 0) return false;
        this.bite = BITE_IMPACT;
        this.triggerAnim("action", "attack");
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isAwake() && source.getEntity() instanceof LivingEntity l) this.wake(l);
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAwake()) {
            if (!this.level().isClientSide) this.wake(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override public boolean isPushable() { return this.isAwake() && super.isPushable(); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return this.isAwake() ? SoundEvents.CHEST_LOCKED : null; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_HIT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) this.triggerAnim("action", "death");
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    /** Rank the mimic was placed for (persistent flag from GateArena), default C. */
    public GateRank rank() {
        String r = this.getPersistentData().getString(ApothCompat.RANK_FLAG);
        try { return r.isEmpty() ? GateRank.C : GateRank.valueOf(r); } catch (IllegalArgumentException e) { return GateRank.C; }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recent) {
        super.dropCustomDeathLoot(level, source, recent);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "dungeon/" + this.rank().name().toLowerCase(java.util.Locale.ROOT)));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, this.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.CHEST);
        for (int i = 0; i < 2; i++) table.getRandomItems(params, this::spawnAtLocation);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", this.isAwake());
        tag.putFloat("RestYaw", this.restYaw);
        if (!this.getDebugPose().isEmpty()) tag.putString("DebugPose", this.getDebugPose());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AWAKE, tag.getBoolean("Awake"));
        this.restYaw = tag.contains("RestYaw") ? tag.getFloat("RestYaw") : this.getYRot();
        if (tag.contains("DebugPose")) this.entityData.set(DEBUG_POSE, tag.getString("DebugPose"));
    }

    private static RawAnimation loop(String n) { return RawAnimation.begin().thenLoop(P + n); }
    private static RawAnimation once(String n) { return RawAnimation.begin().thenPlay(P + n); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "movement", 5, s -> {
            if (!this.isAwake()) return s.setAndContinue(loop("dormant"));
            return s.setAndContinue(s.isMoving() ? loop("walk") : loop("idle"));
        }));
        c.add(new AnimationController<>(this, "action", 1, s -> PlayState.STOP)
                .triggerableAnim("roar", once("roar")).triggerableAnim("attack", once("attack")).triggerableAnim("death", once("death")));
        c.add(new AnimationController<>(this, "debug_pose", 0, s -> {
            String pose = this.getDebugPose();
            return pose.isEmpty() ? PlayState.STOP : s.setAndContinue(loop("freeze_" + pose));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    /** Chance one loot chest in a C+ dungeon is a mimic instead. */
    public static double chance(GateRank rank) {
        return rank.ordinal() < GateRank.C.ordinal() ? 0.0D : 0.20D + 0.05D * (rank.ordinal() - GateRank.C.ordinal());
    }

    public static boolean isMimicCandidate(Mob m) { return m instanceof GateMimic g && !g.isAwake(); }
}

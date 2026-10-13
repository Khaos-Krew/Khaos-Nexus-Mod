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
        if (this.getType() == ModEntities.VOID_CASTER.get()) {
            this.goalSelector.addGoal(1, new VoidSpellGoal(this));
        }
        this.goalSelector.addGoal(2, new WindupMeleeGoal(this));
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
        AnimationController<RiftMob> action = new AnimationController<>(this, "action", 1, state -> software.bernie.geckolib.animation.PlayState.STOP)
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

    /** Ticks from the start of the attack animation to the impact frame (just before the swing's end keyframe), per mob (matches animation.json). */
    protected int strikeDelay() {
        return switch (EntityType.getKey(this.getType()).getPath()) {
            case "rift_brute" -> 11;
            case "void_caster" -> 9;
            case "rift_warden" -> 16;
            case "spire_guardian" -> 14;
            default -> 7;
        };
    }

    private int windup;
    private net.minecraft.world.entity.LivingEntity windupTarget;

    public boolean isWindingUp() {
        return this.windup > 0;
    }

    /** Can this mob start an attack right now (bosses block it while drawing their weapon)? */
    protected boolean canStartAttack() {
        return true;
    }

    /** Starts the attack animation now; the hit lands when the animation reaches its strike frame. */
    public void startWindup(net.minecraft.world.entity.LivingEntity target) {
        this.windup = this.strikeDelay();
        this.windupTarget = target;
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.triggerAnim("action", "attack");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.windup > 0 && --this.windup == 0) {
            net.minecraft.world.entity.LivingEntity t = this.windupTarget;
            this.windupTarget = null;
            if (t != null && t.isAlive() && this.isWithinMeleeAttackRange(t) && this.hasLineOfSight(t)) {
                super.doHurtTarget(t);
            }
        }
        if (this.level().isClientSide && this.getType() == ModEntities.RIFT_STALKER.get() && this.random.nextInt(3) == 0) {
            // Rift Phantom: shadow wisps shed from its smoke tails.
            this.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.12F, 0.06F, 0.18F), 1.6F),
                    this.getX() + this.random.nextGaussian() * 0.25D, this.getY() + 0.3D + this.random.nextDouble() * 0.6D,
                    this.getZ() + this.random.nextGaussian() * 0.25D, 0, -0.02D, 0);
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, this.getX(), this.getY() + 0.4D, this.getZ(), 0, 0.01D, 0);
            }
        }
        if (this.level().isClientSide && this.swinging && this.getType() == ModEntities.VOID_CASTER.get()) {
            // Orb flare: void-purple sparks burst along the orb's orbit while the caster attacks.
            double r = 0.7D;
            for (int i = 0; i < 6; i++) {
                double a = this.random.nextDouble() * Math.PI * 2;
                double x = this.getX() + Math.cos(a) * r, y = this.getY() + 1.55D + this.random.nextGaussian() * 0.15D, z = this.getZ() + Math.sin(a) * r;
                this.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.79F, 0.35F, 0.93F), 1.4F), x, y, z, 0, 0.03D, 0);
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, x, y, z, (this.random.nextDouble() - 0.5D) * 0.6D, 0.1D, (this.random.nextDouble() - 0.5D) * 0.6D);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Direct hits (e.g. from other code paths) still animate; the melee goal uses startWindup instead.
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.triggerAnim("action", "attack");
        }
        return hit;
    }

    /** Void Caster: from range, raise the orb (same windup animation + flare) and hurl a void pearl on the impact frame. */
    static final class VoidSpellGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final RiftMob mob;
        private int cooldown = 20;
        private int cast;

        VoidSpellGoal(RiftMob mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean inRange() {
            net.minecraft.world.entity.LivingEntity t = this.mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = this.mob.distanceToSqr(t);
            return d > 16.0D && d < 400.0D && this.mob.hasLineOfSight(t);
        }

        @Override
        public boolean canUse() {
            return this.inRange();
        }

        @Override
        public boolean canContinueToUse() {
            return this.cast > 0 || this.inRange();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            net.minecraft.world.entity.LivingEntity t = this.mob.getTarget();
            if (t == null) return;
            this.mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            this.mob.getNavigation().stop();
            if (this.cast > 0) {
                if (--this.cast == 0) {
                    net.minecraft.world.phys.Vec3 from = this.mob.position().add(0, 1.5, 0).add(this.mob.getLookAngle().scale(0.8));
                    net.minecraft.world.phys.Vec3 dir = t.getEyePosition().subtract(0, 0.4, 0).subtract(from);
                    VoidOrb orb = new VoidOrb(this.mob.level(), this.mob, dir);
                    orb.setPos(from);
                    this.mob.level().addFreshEntity(orb);
                    this.mob.playSound(net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.7F);
                }
            } else if (--this.cooldown <= 0) {
                this.cooldown = 50 + this.mob.getRandom().nextInt(30);
                this.cast = this.mob.strikeDelay();
                this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.mob.triggerAnim("action", "attack");
                this.mob.playSound(net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, 1.3F);
            }
        }
    }

    /** Melee goal that plays the windup first and lets the strike frame deal the damage. */
    static final class WindupMeleeGoal extends MeleeAttackGoal {
        private final RiftMob rift;

        WindupMeleeGoal(RiftMob mob) {
            super(mob, 1.0D, false);
            this.rift = mob;
        }

        @Override
        protected void checkAndPerformAttack(net.minecraft.world.entity.LivingEntity target) {
            if (this.canPerformAttack(target) && !this.rift.isWindingUp() && this.rift.canStartAttack()) {
                this.resetAttackCooldown();
                this.rift.startWindup(target);
            }
        }
    }
}

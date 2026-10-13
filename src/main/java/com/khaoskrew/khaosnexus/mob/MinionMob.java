package com.khaoskrew.khaosnexus.mob;

import java.util.EnumSet;
import java.util.UUID;

import com.khaoskrew.khaosnexus.hunter.ClassAbilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Class minion (Summoner's Rift Familiar, Necromancer's Bone Thrall): follows its hunter and fights what they fight. */
public class MinionMob extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String prefix;
    private final boolean familiar;

    public MinionMob(EntityType<? extends MinionMob> type, Level level) {
        super(type, level);
        this.prefix = "animation." + EntityType.getKey(type).getPath() + ".";
        this.familiar = EntityType.getKey(type).getPath().equals("rift_familiar");
    }

    public static AttributeSupplier.Builder attributes(double health, double damage, double speed) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public LivingEntity getOwner() {
        String id = this.getPersistentData().getString(ClassAbilities.MINION_OWNER);
        if (id.isEmpty()) return null;
        try {
            return this.level().getPlayerByUUID(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwner(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new DefendOwner(this));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.triggerAnim("action", "attack");
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == this.getOwner()) return false;   // no friendly fire
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            long exp = this.getPersistentData().getLong(ClassAbilities.MINION_EXPIRES);
            if (exp > 0 && this.level().getGameTime() > exp) {
                ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.8, this.getZ(), 16, 0.3, 0.5, 0.3, 0.02);
                this.discard();
            }
        }
        if (this.level().isClientSide && this.random.nextInt(3) == 0) {
            if (this.familiar) {
                this.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.64F, 0.29F, 0.88F), 0.9F), this.getRandomX(0.5), this.getY() + 0.3 + this.random.nextDouble() * 0.6,
                        this.getRandomZ(0.5), 0, 0.01, 0);
            } else {
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.3, this.getZ(), 0, 0.02, 0);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(this.prefix + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(this.prefix + "walk");
        controllers.add(new AnimationController<>(this, "movement", 5, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "action", 1, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(this.prefix + "attack")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Stay near the owner; teleport back if left far behind. */
    static final class FollowOwner extends Goal {
        private final MinionMob mob;

        FollowOwner(MinionMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = this.mob.getOwner();
            return o != null && this.mob.getTarget() == null && this.mob.distanceToSqr(o) > 25.0D;
        }

        @Override
        public void tick() {
            LivingEntity o = this.mob.getOwner();
            if (o == null) return;
            if (this.mob.distanceToSqr(o) > 576.0D) {
                this.mob.teleportTo(o.getX(), o.getY(), o.getZ());
            } else {
                this.mob.getNavigation().moveTo(o, 1.25D);
            }
        }
    }

    /** Attack whatever hurt the owner, what the owner hit, or the nearest hostile near the owner. */
    static final class DefendOwner extends Goal {
        private final MinionMob mob;

        DefendOwner(MinionMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        private LivingEntity pick() {
            LivingEntity o = this.mob.getOwner();
            if (o == null) return null;
            for (LivingEntity c : new LivingEntity[] {o.getLastHurtByMob(), o.getLastHurtMob()}) {
                if (c != null && c.isAlive() && !(c instanceof MinionMob) && c != o) return c;
            }
            return this.mob.level().getEntitiesOfClass(Mob.class, o.getBoundingBox().inflate(10.0D),
                    m -> m instanceof Enemy && m.isAlive()).stream().min(java.util.Comparator.comparingDouble(this.mob::distanceToSqr)).orElse(null);
        }

        @Override
        public boolean canUse() {
            if (this.mob.tickCount % 10 != 0) return false;
            LivingEntity t = this.pick();
            if (t != null && t != this.mob.getTarget()) {
                this.mob.setTarget(t);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}

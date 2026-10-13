package com.khaoskrew.khaosnexus.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Void Caster spell: a slow homing-free void pearl that hits for magic damage and slows. */
public class VoidOrb extends AbstractHurtingProjectile implements ItemSupplier {
    public VoidOrb(EntityType<? extends VoidOrb> type, Level level) {
        super(type, level);
    }

    public VoidOrb(Level level, LivingEntity owner, Vec3 dir) {
        super(ModEntities.VOID_ORB.get(), owner, dir.normalize(), level);
        this.accelerationPower = 0.06D;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity owner = this.getOwner();
        Entity target = hit.getEntity();
        if (owner instanceof RiftMob && target instanceof RiftMob) {
            return;
        }
        if (target.hurt(this.damageSources().indirectMagic(this, owner), 6.0F) && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1), owner);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 40, 0.3, 0.3, 0.3, 0.15);
            server.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.02);
            this.discard();
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModEntities.VOID_ORB_ITEM.get());
    }
}

package com.khaoskrew.khaosnexus.saga;

import java.util.UUID;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.mob.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rift Wisp: a crown-shard spirit companion summoned with the Wisp Bell. It floats at the owner's shoulder,
 * draws in nearby drops and experience (6 blocks), and every 12s pulses a small heal while its owner is hurt in combat.
 * It cannot be hurt or targeted, never fights, and re-forms beside the owner after travel or relog while the bell is rung.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public class RiftWisp extends PathfinderMob implements GeoEntity {
    public static final String FLAG = "khaosnexus_wisp_out";
    public static final String ID = "khaosnexus_wisp_id";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private int pulseCd = 240;

    public RiftWisp(EntityType<? extends RiftWisp> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.4D);
    }

    @Override protected void registerGoals() {}
    @Override public boolean hurt(DamageSource s, float a) { return s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(s, a); }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean shouldBeSaved() { return false; }   // the bell re-forms it; never left behind in a chunk
    @Override public boolean isAttackable() { return false; }

    public void setOwner(Player p) { this.owner = p.getUUID(); }

    public Player owner() { return this.owner == null ? null : this.level().getPlayerByUUID(this.owner); }

    @Override
    public void tick() {
        super.tick();
        Player o = this.owner();
        if (this.level().isClientSide) {
            if (this.random.nextInt(2) == 0) this.level().addParticle(new DustParticleOptions(new org.joml.Vector3f(0.78F, 0.55F, 1.0F), 0.6F),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.3, this.getY() + 0.25, this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, 0, -0.01, 0);
            return;
        }
        if (o == null || o.level() != this.level() || !o.getPersistentData().getBoolean(FLAG) || !this.getUUID().toString().equals(o.getPersistentData().getString(ID))) {
            this.discard();
            return;
        }
        // hover beside the owner's right shoulder, eased
        double yaw = Math.toRadians(o.getYRot());
        Vec3 target = o.position().add(-Math.cos(yaw) * 0.9 - (-Math.sin(yaw)) * 0.3, o.getBbHeight() + 0.1 + 0.15 * Math.sin(this.tickCount * 0.1), -Math.sin(yaw) * 0.9 - Math.cos(yaw) * 0.3);
        Vec3 d = target.subtract(this.position());
        if (d.lengthSqr() > 400) this.setPos(target);
        else this.setDeltaMovement(d.scale(0.25));
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setYRot(o.getYRot()); this.yBodyRot = o.getYRot(); this.yHeadRot = o.getYRot();
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount % 5 == 0) {   // draw in drops and experience
            for (Entity e : level.getEntities(this, o.getBoundingBox().inflate(6), x -> (x instanceof ItemEntity it && !it.hasPickUpDelay()) || x instanceof ExperienceOrb)) {
                Vec3 pull = o.position().add(0, 0.5, 0).subtract(e.position());
                if (pull.lengthSqr() > 1.0) e.setDeltaMovement(pull.normalize().scale(0.35));
                if (this.tickCount % 20 == 0) level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.2, e.getZ(), 1, 0, 0, 0, 0);
            }
        }
        if (--this.pulseCd <= 0 && o.getHealth() < o.getMaxHealth() && o.getLastHurtByMobTimestamp() > o.tickCount - 100) {
            this.pulseCd = 240;
            o.heal(2.0F);
            this.triggerAnim("action", "pulse");
            level.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + 1.8, o.getZ(), 2, 0.3, 0.2, 0.3, 0);
            level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(0.8F, 0.5F, 1.0F), 1.2F), this.getX(), this.getY() + 0.25, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.6F);
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); if (this.owner != null) t.putUUID("Owner", this.owner); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); if (t.hasUUID("Owner")) this.owner = t.getUUID("Owner"); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "movement", 5, s -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.rift_wisp.idle"))));
        c.add(new AnimationController<>(this, "action", 2, s -> PlayState.STOP).triggerableAnim("pulse", RawAnimation.begin().thenPlay("animation.rift_wisp.pulse")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    // ------------------------------------------------------------ owner side
    static boolean present(ServerPlayer p) {
        String id = p.getPersistentData().getString(ID);
        if (id.isEmpty()) return false;
        try {
            Entity e = ((ServerLevel) p.level()).getEntity(UUID.fromString(id));
            return e instanceof RiftWisp && e.isAlive();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static RiftWisp summon(ServerPlayer p) {
        RiftWisp w = ModEntities.RIFT_WISP.get().create(p.level());
        if (w == null) return null;
        w.setOwner(p);
        w.moveTo(p.getX(), p.getY() + p.getBbHeight(), p.getZ(), p.getYRot(), 0);
        p.level().addFreshEntity(w);
        p.getPersistentData().putString(ID, w.getUUID().toString());
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, w.getX(), w.getY() + 0.25, w.getZ(), 14, 0.2, 0.2, 0.2, 0.06);
        return w;
    }

    @SubscribeEvent
    public static void reform(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 40 == 0 && p.getPersistentData().getBoolean(FLAG) && !p.isSpectator() && !present(p)) summon(p);
    }

    /** The Wisp Bell: ring to call the wisp, ring again to dismiss it. */
    public static class Bell extends Item {
        public Bell(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (player instanceof ServerPlayer sp) {
                boolean out = !sp.getPersistentData().getBoolean(FLAG);
                sp.getPersistentData().putBoolean(FLAG, out);
                if (out) summon(sp); else sp.getPersistentData().remove(ID);
                sp.displayClientMessage(Component.translatable(out ? "khaosnexus.wisp.called" : "khaosnexus.wisp.dismissed"), true);
                level.playSound(null, sp.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.5F, out ? 1.8F : 1.2F);
                sp.getCooldowns().addCooldown(this, 20);
            }
            return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, java.util.List<Component> t, TooltipFlag f) {
            t.add(Component.translatable("item.khaosnexus.wisp_bell.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}

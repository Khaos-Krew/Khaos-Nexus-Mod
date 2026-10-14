package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Combat effects of the four Oath branches. Node N is active once {@code OathNodes >= N}.
 * Timers live in the player's persistent data under {@code khaosnexus_oath}.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class OathEffects {
    private OathEffects() {}

    public static final String UNBROKEN = "khaosnexus:oath_of_the_unbroken";
    public static final String CRIMSON = "khaosnexus:oath_of_the_crimson_edge";
    public static final String QUIET = "khaosnexus:oath_of_the_quiet_star";
    public static final String WATCH = "khaosnexus:oath_of_the_long_watch";
    static final String MARK = "khaosnexus_oath_mark";
    static final String BLEED = "khaosnexus_oath_bleed";
    static final ResourceLocation MOD_A = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "oath_a");
    static final ResourceLocation MOD_B = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "oath_b");
    static final ResourceLocation MOD_FLURRY = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "oath_flurry");

    static CompoundTag t(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("khaosnexus_oath")) root.put("khaosnexus_oath", new CompoundTag());
        return root.getCompound("khaosnexus_oath");
    }

    /** Active node count for the given oath, or 0. */
    public static int nodes(ServerPlayer p, String oath) {
        HunterProfile prof = HunterRanks.profile(p);
        return oath.equals(Saga.oath(prof)) ? Saga.oathNodes(prof) : 0;
    }

    static void mod(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double v) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (v == 0) { if (cur != null) inst.removeModifier(id); return; }
        if (cur != null && cur.amount() == v) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        CompoundTag t = t(p);
        // stillness (Long Watch node 2)
        if (p.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) t.putLong("Moved", now);
        // flurry/starfall timers run every tick
        if (t.getLong("Flurry") > now && nodes(p, CRIMSON) >= 4) mod(p, Attributes.ATTACK_SPEED, MOD_FLURRY, 0.30); else mod(p, Attributes.ATTACK_SPEED, MOD_FLURRY, 0);
        int fall = t.getInt("Starfall");
        if (fall > 0 && now % 20 == 0) {
            t.putInt("Starfall", fall - 1);
            for (LivingEntity m : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6), x -> x instanceof Enemy && x.isAlive())) {
                m.hurt(level.damageSources().indirectMagic(p, p), 5.0F);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.getX(), p.getY() + 4, p.getZ(), 40, 3, 0.5, 3, 0.05);
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
        if (now % 20 != 0) return;
        HunterProfile prof = HunterRanks.profile(p);
        String oath = Saga.oath(prof);
        int n = Saga.oathNodes(prof);
        boolean un = oath.equals(UNBROKEN) && n >= 1, cr = oath.equals(CRIMSON) && n >= 1, qs = oath.equals(QUIET) && n >= 1;
        mod(p, Attributes.MAX_HEALTH, MOD_A, un ? 0.10 : qs ? -0.08 : 0);
        mod(p, Attributes.ATTACK_DAMAGE, MOD_A, un ? -0.05 : 0);
        mod(p, Attributes.ATTACK_SPEED, MOD_B, cr ? 0.08 : 0);
        mod(p, Attributes.ARMOR, MOD_B, cr ? -0.06 : 0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        // Unbroken 3: taunt pulse every 20s
        if (oath.equals(UNBROKEN) && n >= 3 && now % 400 == 0) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8), x -> x instanceof Enemy && x.isAlive())) m.setTarget(p);
            level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        // Unbroken 4: last stand
        if (oath.equals(UNBROKEN) && n >= 4 && p.getHealth() < p.getMaxHealth() * 0.3F && t.getLong("LastStand") <= now) {
            t.putLong("LastStand", now + 1200);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
            level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
        }
    }

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        long now = target.level().getGameTime();
        if (target.getPersistentData().getLong(MARK) > now) e.setAmount(e.getAmount() * 1.05F);
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || target == p) return;
        CompoundTag t = t(p);
        boolean projectile = e.getSource().is(DamageTypeTags.IS_PROJECTILE);
        if (projectile && nodes(p, WATCH) >= 1) {
            double d = p.distanceTo(target);
            if (d > 16) e.setAmount(e.getAmount() * 1.10F); else if (d < 6) e.setAmount(e.getAmount() * 0.90F);
            if (nodes(p, WATCH) >= 2 && now - t.getLong("Moved") >= 40 && p.getRandom().nextFloat() < 0.08F) e.setAmount(e.getAmount() * 1.5F);
            if (nodes(p, WATCH) >= 3) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                target.getPersistentData().putLong(MARK, now + 100);
            }
            if (nodes(p, WATCH) >= 4 && t.getLong("Volley") <= now) {
                t.putLong("Volley", now + 900);
                volley(p, target);
            }
        }
        boolean magic = e.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) || e.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC);
        if (magic && nodes(p, QUIET) >= 1) {
            e.setAmount(e.getAmount() * 1.10F);
        }
        if (target.getPersistentData().getLong(BLEED) > now) {
            e.setAmount(e.getAmount() + 1.0F);
        }
    }

    static void volley(ServerPlayer p, LivingEntity target) {
        ServerLevel level = (ServerLevel) p.level();
        for (int i = -1; i <= 1; i++) {
            net.minecraft.world.entity.projectile.Arrow a = new net.minecraft.world.entity.projectile.Arrow(level, p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            a.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            double dx = target.getX() - p.getX(), dz = target.getZ() - p.getZ(), dy = target.getY(0.5) - a.getY();
            double yaw = Math.atan2(dz, dx) + i * 0.08;
            double h = Math.sqrt(dx * dx + dz * dz);
            a.shoot(Math.cos(yaw) * h, dy + h * 0.05, Math.sin(yaw) * h, 3.0F, 0.0F);
            a.setBaseDamage(4.0);
            level.addFreshEntity(a);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @SubscribeEvent
    public static void crit(CriticalHitEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.isCriticalHit() || !(e.getTarget() instanceof LivingEntity target)) return;
        int n = nodes(p, CRIMSON);
        long now = p.level().getGameTime();
        if (n >= 2) target.getPersistentData().putLong(MARK, now + 100);
        if (n >= 4) {
            CompoundTag t = t(p);
            if (t.getLong("FlurryCd") <= now) {
                t.putLong("FlurryCd", now + 1200);
                t.putLong("Flurry", now + 120);
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            if (t.getLong("Flurry") > now) target.getPersistentData().putLong(BLEED, now + 60);
        }
    }

    @SubscribeEvent
    public static void block(LivingShieldBlockEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getBlocked() && nodes(p, UNBROKEN) >= 2) p.heal(p.getMaxHealth() * 0.02F);
    }

    @SubscribeEvent
    public static void kill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || nodes(p, CRIMSON) < 3) return;
        HunterProfile prof = HunterRanks.profile(p);
        long now = p.level().getGameTime();
        int cd = com.khaoskrew.khaosnexus.Config.ABILITY_COOLDOWN.get();
        if (prof.abilityReady > now) prof.abilityReady -= cd / 10;
        if (prof.abilityAltReady > now) prof.abilityAltReady -= com.khaoskrew.khaosnexus.Config.ABILITY_ALT_COOLDOWN.get() / 10;
    }

    /** Called from ClassAbilities after a successful cast; returns the (possibly shortened) cooldown. */
    public static int onCast(ServerPlayer p, int cooldown) {
        int n = nodes(p, QUIET);
        if (n < 1) return cooldown;
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        CompoundTag t = t(p);
        if (n >= 2) cooldown = cooldown * 3 / 4;
        if (n >= 3) {
            int c = t.getInt("Casts") + 1;
            t.putInt("Casts", c);
            if (c % 5 == 0) {
                for (LivingEntity m : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6), x -> x instanceof Enemy && x.isAlive())) {
                    m.hurt(level.damageSources().indirectMagic(p, p), 3.2F);
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 60, 2, 1, 2, 0.4);
            }
        }
        if (n >= 4 && t.getLong("StarCd") <= now) {
            t.putLong("StarCd", now + 900);
            t.putInt("Starfall", 4);
        }
        return cooldown;
    }
}

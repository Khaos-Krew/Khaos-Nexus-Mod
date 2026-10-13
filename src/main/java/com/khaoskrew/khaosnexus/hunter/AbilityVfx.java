package com.khaoskrew.khaosnexus.hunter;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-side particle + sound effects for every class ability (seen by everyone nearby). */
public final class AbilityVfx {
    private AbilityVfx() {}

    public static void play(ServerPlayer p, ServerLevel level, HunterClass cls, boolean primary) {
        switch (cls) {
            case TANK -> {
                if (primary) { ring(level, p.position(), 1.2, 1.0, ParticleTypes.WAX_ON, 24); column(level, p, dust(0xC8C8D8), 30); sound(level, p, SoundEvents.ANVIL_PLACE, 0.6F, 0.6F); }
                else { ring(level, p.position(), 8, 0.3, ParticleTypes.CRIT, 64); ring(level, p.position(), 4, 0.3, ParticleTypes.ANGRY_VILLAGER, 12); sound(level, p, SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F); }
            }
            case ASSASSIN -> {
                if (primary) { trail(level, p.position().subtract(p.getLookAngle().scale(5)), p.position(), ParticleTypes.LARGE_SMOKE, 30); trail(level, p.position().subtract(p.getLookAngle().scale(5)), p.position(), ParticleTypes.PORTAL, 40); sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.4F); }
                else { burst(level, p.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 40, 0.6); sound(level, p, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F); }
            }
            case CASTER -> {
                if (primary) {
                    LivingEntity t = target(p, level, 12);
                    Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6)).subtract(0, 0.3, 0);
                    if (t != null) { trail(level, from, t.position().add(0, t.getBbHeight() / 2, 0), ParticleTypes.ELECTRIC_SPARK, 40); trail(level, from, t.position().add(0, t.getBbHeight() / 2, 0), ParticleTypes.WITCH, 20); burst(level, t.position().add(0, t.getBbHeight() / 2, 0), ParticleTypes.END_ROD, 20, 0.15); }
                    sound(level, p, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.3F);
                } else { ring(level, p.position(), 1.0, 0.1, ParticleTypes.CLOUD, 20); sound(level, p, SoundEvents.PHANTOM_FLAP, 1.0F, 1.2F); }
            }
            case RANGER -> {
                if (primary) {
                    LivingEntity t = target(p, level, 24);
                    if (t != null) { trail(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() / 2, 0), ParticleTypes.CRIT, 50); burst(level, t.position().add(0, t.getBbHeight() / 2, 0), ParticleTypes.FIREWORK, 16, 0.1); }
                    sound(level, p, SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.9F);
                } else { ring(level, p.position(), 0.8, 0.1, ParticleTypes.CLOUD, 16); sound(level, p, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.4F); }
            }
            case LIFEBINDER -> {
                ring(level, p.position(), primary ? 8 : 1.2, 0.2, ParticleTypes.HAPPY_VILLAGER, primary ? 48 : 16);
                burst(level, p.position().add(0, 1.2, 0), ParticleTypes.HEART, primary ? 8 : 4, 0.5);
                if (primary) for (ServerPlayer ally : level.players()) if (ally != p && ally.distanceToSqr(p) <= 64) burst(level, ally.position().add(0, 1.2, 0), ParticleTypes.HEART, 4, 0.4);
                sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.2F);
            }
            case BERSERKER -> {
                ring(level, p.position(), 1.5, 0.2, dust(0xD02020), 32); burst(level, p.position().add(0, 1, 0), primary ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.FLAME, primary ? 6 : 30, 0.5);
                sound(level, p, primary ? SoundEvents.RAVAGER_ROAR : SoundEvents.BLAZE_SHOOT, 0.8F, 1.0F);
            }
            case SUMMONER -> {
                Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(1.5));
                if (primary) { ring(level, at, 1.4, 0.05, ParticleTypes.END_ROD, 32); column(level, at, ParticleTypes.SOUL_FIRE_FLAME, 30); sound(level, p, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.4F); }
                else { ring(level, p.position(), 3, 0.3, ParticleTypes.HAPPY_VILLAGER, 32); sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F); }
            }
            case NECROMANCER -> {
                Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(1.5));
                if (primary) { ring(level, at, 1.4, 0.05, ParticleTypes.SOUL_FIRE_FLAME, 32); column(level, at, ParticleTypes.SOUL, 20); burst(level, at, ParticleTypes.SQUID_INK, 12, 0.3); sound(level, p, SoundEvents.WITHER_SKELETON_AMBIENT, 1.0F, 0.6F); }
                else { ring(level, p.position(), 6, 0.3, ParticleTypes.SOUL, 48); ring(level, p.position(), 3, 0.3, ParticleTypes.SMOKE, 32); sound(level, p, SoundEvents.WITHER_SHOOT, 0.6F, 0.8F); }
            }
        }
    }

    private static ParticleOptions dust(int rgb) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), 1.2F);
    }

    private static void sound(ServerLevel level, ServerPlayer p, SoundEvent s, float vol, float pitch) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void ring(ServerLevel level, Vec3 c, double r, double y, ParticleOptions o, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(o, c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void column(ServerLevel level, ServerPlayer p, ParticleOptions o, int n) {
        column(level, p.position(), o, n);
    }

    private static void column(ServerLevel level, Vec3 c, ParticleOptions o, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * 0.7;
            level.sendParticles(o, c.x + Math.cos(a) * 0.6, c.y + i * 0.07, c.z + Math.sin(a) * 0.6, 1, 0, 0.02, 0, 0.01);
        }
    }

    private static void burst(ServerLevel level, Vec3 c, ParticleOptions o, int n, double spread) {
        level.sendParticles(o, c.x, c.y, c.z, n, spread, spread, spread, 0.05);
    }

    private static void trail(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions o, int n) {
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.lerp(b, i / (double) n);
            level.sendParticles(o, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static LivingEntity target(ServerPlayer p, ServerLevel level, double range) {
        Vec3 look = p.getLookAngle();
        return level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(range), LivingEntity::isAlive).stream()
                .filter(m -> !ClassAbilities.isOwnedMinion(p, m))
                .filter(m -> look.dot(m.position().subtract(p.position()).normalize()) > 0.2D)
                .min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
}

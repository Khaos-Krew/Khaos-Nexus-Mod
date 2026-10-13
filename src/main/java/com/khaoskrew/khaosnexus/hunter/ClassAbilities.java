package com.khaoskrew.khaosnexus.hunter;

import java.util.Comparator;
import java.util.List;

import com.khaoskrew.khaosnexus.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class ClassAbilities {
    public static final String MINION = "khaosnexus_minion";
    public static final String MINION_OWNER = "khaosnexus_minion_owner";

    private ClassAbilities() {}

    public static void cast(ServerPlayer player, boolean primary) {
        HunterClass hunterClass = ClassService.equipped(player);
        if (hunterClass == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.class.need"), false);
            return;
        }
        HunterProfile profile = HunterRanks.profile(player);
        long now = player.level().getGameTime();
        long readyAt = primary ? profile.abilityReady : profile.abilityAltReady;
        if (now < readyAt) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable("khaosnexus.ability.cooldown", seconds), false);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean used = switch (hunterClass) {
            case TANK -> primary ? effect(player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1)) : taunt(player, level);
            case ASSASSIN -> primary ? dash(player) : effect(player, new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
            case CASTER -> primary ? bolt(player, level) : effect(player, new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
            case RANGER -> primary ? shot(player, level) : effect(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
            case LIFEBINDER -> primary ? mend(player) : effect(player, new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
            case BERSERKER -> primary ? effect(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1)) : rage(player);
            case SUMMONER -> primary ? summon(player, level, com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_FAMILIAR.get(), true) : healMinions(player, level);
            case NECROMANCER -> primary ? summon(player, level, com.khaoskrew.khaosnexus.mob.ModEntities.BONE_THRALL.get(), false) : wither(player, level);
        };
        if (!used) {
            return;
        }
        AbilityVfx.play(player, level, hunterClass, primary);
        int cooldown = primary ? Config.ABILITY_COOLDOWN.get() : Config.ABILITY_ALT_COOLDOWN.get();
        if (primary) {
            profile.abilityReady = now + cooldown;
        } else {
            profile.abilityAltReady = now + cooldown;
        }
        HunterRanks.sync(player);
        String key = primary ? hunterClass.abilityKey() : hunterClass.altAbilityKey();
        player.displayClientMessage(Component.translatable("khaosnexus.ability.used", Component.translatable(key)), false);
    }

    private static boolean effect(ServerPlayer player, MobEffectInstance effect) {
        player.addEffect(effect);
        return true;
    }

    private static boolean dash(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double distance = 5.0D;
        player.teleportTo(player.getX() + look.x * distance, player.getY(), player.getZ() + look.z * distance);
        player.resetFallDistance();
        return true;
    }

    private static boolean taunt(ServerPlayer player, ServerLevel level) {
        AABB box = player.getBoundingBox().inflate(8.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            mob.setTarget(player);
        }
        return true;
    }

    private static boolean bolt(ServerPlayer player, ServerLevel level) {
        Mob target = nearest(player, level, 12.0D);
        if (target == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.no_target"), false);
            return false;
        }
        target.hurt(player.damageSources().magic(), 8.0F);
        return true;
    }

    private static boolean shot(ServerPlayer player, ServerLevel level) {
        Mob target = nearest(player, level, 24.0D);
        if (target == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.no_target"), false);
            return false;
        }
        target.hurt(player.damageSources().playerAttack(player), 7.0F);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        return true;
    }

    private static boolean mend(ServerPlayer player) {
        player.heal(8.0F);
        if (player.level() instanceof ServerLevel level) {
            for (ServerPlayer ally : level.players()) {
                if (ally != player && ally.distanceToSqr(player) <= 8.0D * 8.0D) {
                    ally.heal(6.0F);
                }
            }
        }
        return true;
    }

    private static boolean rage(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth() * 0.5F) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.rage_health"), false);
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        return true;
    }

    private static boolean summon(ServerPlayer player, ServerLevel level, EntityType<? extends Mob> type, boolean tame) {
        if (minionCount(player, level) >= Config.MINION_CAP.get()) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.minion_cap", Config.MINION_CAP.get()), false);
            return false;
        }
        Mob created = type.create(level);
        if (created == null) {
            return false;
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection());
        created.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(created, level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        if (created.isSpawnCancelled()) {
            created.discard();
            return false;
        }
        created.getPersistentData().putBoolean(MINION, true);
        created.getPersistentData().putString(MINION_OWNER, player.getStringUUID());
        created.setCustomName(Component.translatable("khaosnexus.ability.minion_name", player.getName()));
        created.setPersistenceRequired();
        if (tame && created instanceof Wolf wolf) {
            wolf.tame(player);
        }
        if (!level.addFreshEntity(created)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        Mob minion = created;
        server.tell(new TickTask(server.getTickCount() + 600, () -> {
            if (minion.isAlive()) {
                minion.discard();
            }
        }));
        return true;
    }

    private static boolean healMinions(ServerPlayer player, ServerLevel level) {
        boolean healed = false;
        for (Mob mob : minions(player, level)) {
            mob.heal(6.0F);
            healed = true;
        }
        if (!healed) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.no_minions"), false);
        }
        return healed;
    }

    private static boolean wither(ServerPlayer player, ServerLevel level) {
        boolean hit = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6.0D), entity -> entity.isAlive() && !isOwnedMinion(player, entity))) {
            mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            hit = true;
        }
        if (!hit) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.no_target"), false);
        }
        return hit;
    }

    private static Mob nearest(ServerPlayer player, ServerLevel level, double range) {
        Vec3 look = player.getLookAngle();
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range), LivingEntity::isAlive).stream()
                .filter(mob -> !isOwnedMinion(player, mob))
                .filter(mob -> look.dot(mob.position().subtract(player.position()).normalize()) > 0.2D)
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static int minionCount(ServerPlayer player, ServerLevel level) {
        return minions(player, level).size();
    }

    private static List<Mob> minions(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D), mob -> isOwnedMinion(player, mob));
    }

    public static boolean isOwnedMinion(ServerPlayer player, Mob mob) {
        return mob.getPersistentData().getBoolean(MINION)
                && player.getStringUUID().equals(mob.getPersistentData().getString(MINION_OWNER));
    }
}

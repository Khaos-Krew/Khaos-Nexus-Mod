package com.khaoskrew.khaosnexus.endgame;

import java.util.Arrays;
import java.util.Locale;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.compat.ModBossTuning;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Crimson Rifts and rift-overflow rules. State lives on NexusGateBlockEntity; this holds the tunable logic. */
public final class RedGates {
    public static final String TITLE = "red_gate_survivor";
    public static final String BREAK_TITLE = "break_stopper";

    private RedGates() {}

    public static boolean canBeRed(GateRank rank) {
        return Arrays.stream(Config.RED_GATE_RANKS.get().split(",")).map(s -> s.trim().toUpperCase(Locale.ROOT)).anyMatch(rank.name()::equals);
    }

    public static boolean rollRed(GateRank rank, RandomSource random) {
        return canBeRed(rank) && random.nextDouble() < Config.RED_GATE_CHANCE.get();
    }

    /** Stronger boss: more health, more damage (through the boss-mod damage multiplier when present). */
    public static void empower(Mob boss) {
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * Config.RED_BOSS_HEALTH.get());
            boss.setHealth(boss.getMaxHealth());
        }
        if (ModBossTuning.tuned(boss)) {
            CompoundTag d = boss.getPersistentData().getCompound("khaosnexus.mod_boss");
            d.putDouble("Damage", d.getDouble("Damage") * Config.RED_BOSS_DAMAGE.get());
        } else {
            AttributeInstance attack = boss.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * Config.RED_BOSS_DAMAGE.get());
            }
        }
        boss.getPersistentData().putBoolean("khaosnexus.red", true);
    }

    public static boolean canBreak(GateRank rank) {
        return Config.BREAKS_ENABLED.get() && rank.ordinal() >= GateRank.byName(Config.BREAK_MIN_RANK.get().trim().toUpperCase(Locale.ROOT)).ordinal();
    }

    /** Ticks an unopened gate waits before breaking. */
    public static long breakDelay(boolean red) {
        long t = Config.BREAK_MINUTES.get() * 1200L;
        return red ? t / 2 : t;
    }

    public static int scaleXp(int xp, boolean red) {
        return red ? (int) Math.round(xp * Config.RED_XP.get()) : xp;
    }
}

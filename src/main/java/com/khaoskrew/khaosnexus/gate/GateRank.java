package com.khaoskrew.khaosnexus.gate;

import java.util.Locale;

import com.khaoskrew.khaosnexus.Config;

import net.minecraft.util.RandomSource;

public enum GateRank {
    E(0xFFB5B5B5, Config.RANK_E),
    D(0xFF3DDC6A, Config.RANK_D),
    C(0xFF3D8BFF, Config.RANK_C),
    B(0xFFB44DFF, Config.RANK_B),
    A(0xFFFFC14D, Config.RANK_A),
    S(0xFFFF4D4D, Config.RANK_S);

    private final int color;
    private final Config.RankValues tuning;

    GateRank(int color, Config.RankValues tuning) {
        this.color = color;
        this.tuning = tuning;
    }

    public int color() {
        return this.color;
    }

    public Config.RankValues tuning() {
        return this.tuning;
    }

    public String translationKey() {
        return "khaosnexus.rank." + this.name().toLowerCase(Locale.ROOT);
    }

    public static GateRank byName(String name) {
        if (name == null || name.isEmpty()) {
            return E;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return E;
        }
    }

    public static GateRank roll(RandomSource random) {
        int total = 0;
        for (GateRank rank : values()) {
            total += Math.max(0, rank.tuning.weight.get());
        }
        if (total <= 0) {
            return E;
        }
        int pick = random.nextInt(total);
        for (GateRank rank : values()) {
            pick -= Math.max(0, rank.tuning.weight.get());
            if (pick < 0) {
                return rank;
            }
        }
        return E;
    }
}

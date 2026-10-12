package com.khaoskrew.khaosnexus.gate;

import java.util.Locale;

import com.khaoskrew.khaosnexus.Config;

import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;

public enum GateRank {
    E(0xFFB5B5B5, BossEvent.BossBarColor.WHITE, Config.RANK_E),
    D(0xFF3DDC6A, BossEvent.BossBarColor.GREEN, Config.RANK_D),
    C(0xFF3D8BFF, BossEvent.BossBarColor.BLUE, Config.RANK_C),
    B(0xFFB44DFF, BossEvent.BossBarColor.PURPLE, Config.RANK_B),
    A(0xFFFFC14D, BossEvent.BossBarColor.YELLOW, Config.RANK_A),
    S(0xFFFF4D4D, BossEvent.BossBarColor.RED, Config.RANK_S);

    private final int color;
    private final BossEvent.BossBarColor barColor;
    private final Config.RankValues tuning;

    GateRank(int color, BossEvent.BossBarColor barColor, Config.RankValues tuning) {
        this.color = color;
        this.barColor = barColor;
        this.tuning = tuning;
    }

    public int color() {
        return this.color;
    }

    public BossEvent.BossBarColor barColor() {
        return this.barColor;
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

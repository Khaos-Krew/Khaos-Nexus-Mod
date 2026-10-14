package com.khaoskrew.khaosnexus.compat.items.tome;

import com.khaoskrew.khaosnexus.endgame.Prestige;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.world.entity.player.Player;

/**
 * Hunter Tome rules that don't touch Ars Nouveau classes (safe to call without Ars installed).
 * Tiers: 0..5 = hunter rank E..S, 6 = Paragon (SS / Paragon).
 */
public final class HunterTomes {
    public static final String[] TIER_IDS = {"e", "d", "c", "b", "a", "s", "paragon"};
    /** Spell slots per tier. */
    public static final int[] SLOTS = {3, 4, 5, 6, 7, 9, 10};
    /** Ars glyph tier allowed (1..3): spell complexity. */
    public static final int[] GLYPH_TIER = {1, 1, 2, 2, 3, 3, 3};
    /** Flat mana (source) discount per cast. */
    public static final int[] DISCOUNT = {0, 5, 10, 15, 20, 30, 40};
    /** Class attunement strength (0 until C). Caster: +% spell damage; Necromancer: % of spell damage healed;
     *  Summoner: +% spell damage vs rift creatures (x2); Lifebinder: HP healed around you per cast (x0.1). */
    public static final int[] ATTUNE = {0, 0, 8, 12, 16, 22, 30};

    private HunterTomes() {}

    public static boolean casterClass(HunterClass c) {
        return c == HunterClass.CASTER || c == HunterClass.LIFEBINDER || c == HunterClass.SUMMONER || c == HunterClass.NECROMANCER;
    }

    public static HunterClass classOf(Player p) {
        return HunterClass.byId(HunterRanks.profile(p).classId);
    }

    public static boolean canUse(Player p) {
        return casterClass(classOf(p));
    }

    /** Tome tier the player's rank calls for. */
    public static int tierFor(Player p) {
        if (Prestige.level(HunterRanks.profile(p)) >= Prestige.SS) return 6;
        return HunterRanks.rankOf(p).ordinal();
    }
}

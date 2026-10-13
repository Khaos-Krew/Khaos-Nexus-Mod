package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.ModBossTuning;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** SS and National hunter ranks, earned by achievements. Stored as endgame "Prestige" (0 none, 1 SS, 2 National). */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class Prestige {
    public static final String KEY = "Prestige";
    public static final String RUSH_WINS = "BossRushWins";
    public static final String CHALLENGES = "ChallengeClears";
    public static final String TITLE_SS = "hunter_ss";
    public static final String TITLE_NATIONAL = "hunter_national";
    public static final int SS = 1;
    public static final int NATIONAL = 2;

    private Prestige() {}

    public static int level(HunterProfile p) {
        return p.endgame.getInt(KEY);
    }

    /** Missing requirements for the next prestige rank, as translatable lines. Empty = eligible. */
    public static List<Component> missing(HunterProfile p) {
        List<Component> out = new ArrayList<>();
        int lvl = level(p);
        if (HunterRanks.rankOfXp(p.xp) != GateRank.S) {
            out.add(Component.translatable("khaosnexus.prestige.need_s"));
        }
        if (lvl == 0) {
            if (Config.SS_NEEDS_RED.get() && !p.hasTitle(RedGates.TITLE)) {
                out.add(Component.translatable("khaosnexus.prestige.need_red"));
            }
            need(out, p.spireBest, Config.SS_SPIRE.get(), "khaosnexus.prestige.need_spire");
            need(out, p.endgame.getInt(RUSH_WINS), Config.SS_RUSH.get(), "khaosnexus.prestige.need_rush");
        } else if (lvl == SS) {
            need(out, p.spireBest, Config.NATIONAL_SPIRE.get(), "khaosnexus.prestige.need_spire");
            need(out, p.endgame.getInt(RUSH_WINS), Config.NATIONAL_RUSH.get(), "khaosnexus.prestige.need_rush");
            need(out, p.endgame.getInt(CHALLENGES), Config.NATIONAL_CHALLENGES.get(), "khaosnexus.prestige.need_challenge");
        }
        return out;
    }

    private static void need(List<Component> out, int have, int want, String key) {
        if (have < want) {
            out.add(Component.translatable(key, have, want));
        }
    }

    public static boolean tryPromote(ServerPlayer player) {
        HunterProfile p = HunterRanks.profile(player);
        if (!Config.PRESTIGE.get() || level(p) >= NATIONAL || !missing(p).isEmpty()) {
            return false;
        }
        int next = level(p) + 1;
        p.endgame.putInt(KEY, next);
        Titles.unlock(player, next == SS ? TITLE_SS : TITLE_NATIONAL);
        HunterRanks.sync(player);
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("khaosnexus.prestige.promoted",
                player.getDisplayName(), name(next)).withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    public static Component name(int level) {
        return Component.translatable(level >= NATIONAL ? "khaosnexus.prestige.national" : level == SS ? "khaosnexus.prestige.ss" : "khaosnexus.prestige.none");
    }

    public static int bonusSkillPoints(HunterProfile p) {
        return Config.PRESTIGE.get() ? level(p) * Config.PRESTIGE_SKILL_POINTS.get() : 0;
    }

    public static Component status(HunterProfile p) {
        Component out = Component.translatable("khaosnexus.prestige.status", name(level(p)));
        if (level(p) < NATIONAL) {
            List<Component> m = missing(p);
            if (m.isEmpty()) {
                out = out.copy().append("\n ").append(Component.translatable("khaosnexus.prestige.ready"));
            }
            for (Component c : m) {
                out = out.copy().append("\n - ").append(c);
            }
        }
        return out;
    }

    // ---- SS gates ----
    public static boolean rollSS(GateRank rank, RandomSource random) {
        return Config.PRESTIGE.get() && rank == GateRank.S && random.nextDouble() < Config.SS_GATE_CHANCE.get();
    }

    public static boolean canOpenSS(ServerPlayer player) {
        return level(HunterRanks.profile(player)) >= SS;
    }

    public static void empowerSS(Mob boss, RandomSource random) {
        double s = Config.SS_GATE_SCALE.get();
        double hp = s;
        double dmg = s;
        double speed = 1.0D;
        if (Config.SS_EXTRA_MODIFIER.get() && !WeeklyModifiers.all().isEmpty()) {
            List<WeeklyModifiers.Modifier> pool = new ArrayList<>(WeeklyModifiers.all().values());
            WeeklyModifiers.Modifier m = pool.get(random.nextInt(pool.size()));
            hp *= m.mobHealth();
            dmg *= m.mobDamage();
            speed *= m.mobSpeed();
            boss.getPersistentData().putString("khaosnexus.ss_modifier", m.id().toString());
        }
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * hp);
            boss.setHealth(boss.getMaxHealth());
        }
        if (ModBossTuning.tuned(boss)) {
            CompoundTag d = boss.getPersistentData().getCompound("khaosnexus.mod_boss");
            d.putDouble("Damage", d.getDouble("Damage") * dmg);
        } else {
            AttributeInstance attack = boss.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * dmg);
            }
        }
        AttributeInstance sp = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) {
            sp.setBaseValue(sp.getBaseValue() * speed);
        }
        boss.getPersistentData().putBoolean("khaosnexus.ss", true);
    }

    public static int scaleXp(int xp) {
        return (int) Math.round(xp * Config.SS_XP.get());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 200 == 13) {
            tryPromote(player);
        }
    }
}

package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.ApothCompat;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Weekly rotating gate modifiers. Each week (real-time, UTC Monday) the server rolls weeklyModifierCount modifiers
 * from datapack files, weighted, from a seed of (week, weeklySeed). They apply to gates of the weeklyRanks and pay
 * bonus hunter XP and rift cores.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class WeeklyModifiers {
    public record Modifier(ResourceLocation id, int weight, double mobHealth, double mobDamage, double mobSpeed,
            double damageDealt, double damageTaken, double xpMult, int bonusCores) {}

    private static final Map<ResourceLocation, Modifier> ALL = new LinkedHashMap<>();
    private static final long WEEK_MS = 7L * 24 * 3600 * 1000;
    /** 1970-01-01 was a Thursday; shift so weeks roll over on Monday 00:00 UTC. */
    private static final long MONDAY_SHIFT = 3L * 24 * 3600 * 1000;
    public static final String MOB_FLAG = "khaosnexus.weekly";

    private WeeklyModifiers() {}

    public static Map<ResourceLocation, Modifier> all() {
        return ALL;
    }

    public static long week(long epochMs) {
        return Math.floorDiv(epochMs + MONDAY_SHIFT, WEEK_MS);
    }

    public static long currentWeek() {
        return week(System.currentTimeMillis());
    }

    /** The modifiers active in a given week. Deterministic for (week, weeklySeed, loaded files). */
    public static List<Modifier> active(long week) {
        List<Modifier> out = new ArrayList<>();
        if (!Config.WEEKLY_ENABLED.get()) {
            return out;
        }
        if (!Config.WEEKLY_FORCE.get().isEmpty()) {
            for (String id : Config.WEEKLY_FORCE.get()) {
                Modifier m = ALL.get(ResourceLocation.tryParse(id));
                if (m != null) {
                    out.add(m);
                }
            }
            return out;
        }
        List<Modifier> pool = new ArrayList<>(ALL.values());
        pool.removeIf(m -> m.weight() <= 0);
        Random random = new Random(week * 0x9E3779B97F4A7C15L ^ Config.WEEKLY_SEED.get());
        for (int n = 0; n < Config.WEEKLY_COUNT.get() && !pool.isEmpty(); n++) {
            int total = pool.stream().mapToInt(Modifier::weight).sum();
            int r = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                r -= pool.get(i).weight();
                if (r < 0) {
                    out.add(pool.remove(i));
                    break;
                }
            }
        }
        return out;
    }

    public static List<Modifier> active() {
        return active(currentWeek());
    }

    public static boolean applies(GateRank rank) {
        return Arrays.stream(Config.WEEKLY_RANKS.get().split(",")).map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(rank.name()::equals);
    }

    private static double product(GateRank rank, java.util.function.ToDoubleFunction<Modifier> f) {
        if (!applies(rank)) {
            return 1.0D;
        }
        double v = 1.0D;
        for (Modifier m : active()) {
            v *= f.applyAsDouble(m);
        }
        return v;
    }

    /** Called by GateArena.spawnMob after rank scaling. */
    public static void applyToMob(Mob mob, GateRank rank) {
        if (!applies(rank) || active().isEmpty()) {
            return;
        }
        mob.getPersistentData().putBoolean(MOB_FLAG, true);
        scaleAttr(mob.getAttribute(Attributes.MAX_HEALTH), product(rank, Modifier::mobHealth));
        mob.setHealth(mob.getMaxHealth());
        scaleAttr(mob.getAttribute(Attributes.ATTACK_DAMAGE), product(rank, Modifier::mobDamage));
        scaleAttr(mob.getAttribute(Attributes.MOVEMENT_SPEED), product(rank, Modifier::mobSpeed));
    }

    private static void scaleAttr(AttributeInstance a, double mult) {
        if (a != null && mult != 1.0D) {
            a.setBaseValue(a.getBaseValue() * mult);
        }
    }

    public static int scaleXp(GateRank rank, int xp) {
        return (int) Math.round(xp * product(rank, Modifier::xpMult));
    }

    public static int bonusCores(GateRank rank) {
        return applies(rank) ? active().stream().mapToInt(Modifier::bonusCores).sum() : 0;
    }

    private static GateRank rankOf(Entity e) {
        if (e instanceof Mob m && m.getPersistentData().getBoolean(MOB_FLAG)) {
            String r = m.getPersistentData().getString(ApothCompat.RANK_FLAG);
            return r.isEmpty() ? null : GateRank.byName(r);
        }
        return null;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        GateRank victimRank = rankOf(event.getEntity());
        if (victimRank != null && event.getSource().getEntity() instanceof Player) {
            event.setAmount((float) (event.getAmount() * product(victimRank, Modifier::damageDealt)));
        }
        if (event.getEntity() instanceof Player) {
            GateRank attackerRank = rankOf(event.getSource().getEntity());
            if (attackerRank != null) {
                event.setAmount((float) (event.getAmount() * product(attackerRank, Modifier::damageTaken)));
            }
        }
    }

    public static Component describe() {
        List<Modifier> act = active();
        if (act.isEmpty()) {
            return Component.translatable("khaosnexus.weekly.none");
        }
        Component list = Component.empty();
        for (int i = 0; i < act.size(); i++) {
            ResourceLocation id = act.get(i).id();
            if (i > 0) {
                list = list.copy().append(", ");
            }
            list = list.copy().append(Component.translatable("khaosnexus.weekly." + id.getNamespace() + "." + id.getPath()));
        }
        return Component.translatable("khaosnexus.weekly.active", Config.WEEKLY_RANKS.get(), list);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !active().isEmpty()) {
            player.displayClientMessage(describe(), false);
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static Modifier parse(ResourceLocation id, JsonObject o) {
        JsonObject e = GsonHelper.getAsJsonObject(o, "effects", new JsonObject());
        JsonObject r = GsonHelper.getAsJsonObject(o, "rewards", new JsonObject());
        return new Modifier(id, GsonHelper.getAsInt(o, "weight", 10),
                GsonHelper.getAsDouble(e, "mob_health", 1.0D), GsonHelper.getAsDouble(e, "mob_damage", 1.0D),
                GsonHelper.getAsDouble(e, "mob_speed", 1.0D), GsonHelper.getAsDouble(e, "damage_dealt", 1.0D),
                GsonHelper.getAsDouble(e, "damage_taken", 1.0D), GsonHelper.getAsDouble(r, "xp_multiplier", 1.0D),
                GsonHelper.getAsInt(r, "rift_cores", 0));
    }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "khaosnexus_gate_modifiers");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            ALL.clear();
            files.forEach((id, json) -> {
                try {
                    ALL.put(id, parse(id, json.getAsJsonObject()));
                } catch (RuntimeException ex) {
                    KhaosNexus.LOGGER.warn("Bad gate modifier {}: {}", id, ex.toString());
                }
            });
            KhaosNexus.LOGGER.info("Loaded {} weekly gate modifiers", ALL.size());
        }
    }
}

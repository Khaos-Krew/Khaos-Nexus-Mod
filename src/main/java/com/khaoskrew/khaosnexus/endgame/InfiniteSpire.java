package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Endless Spire scaling and boss-floor affixes. Called from SpireService.generate and onBossKilled. */
public final class InfiniteSpire {
    public static final String AFFIX = "khaosnexus.spire_affix";

    private InfiniteSpire() {}

    public static double scale(int floor) {
        if (!Config.SPIRE_INFINITE.get() || floor <= Config.SPIRE_SCALE_FROM.get()) {
            return 1.0D;
        }
        double s = 1.0D + (floor - Config.SPIRE_SCALE_FROM.get()) * Config.SPIRE_SCALE_PER_FLOOR.get();
        return Math.min(s, Config.SPIRE_SCALE_CAP.get());
    }

    public static boolean bossFloor(int floor) {
        int n = Config.SPIRE_BOSS_INTERVAL.get();
        return Config.SPIRE_INFINITE.get() && n > 0 && floor % n == 0;
    }

    /** Deterministic weighted affix per (season, floor), so a retry meets the same boss. */
    public static ResourceLocation affix(int season, int floor) {
        if (!bossFloor(floor)) {
            return null;
        }
        List<Map.Entry<ResourceLocation, JsonObject>> pool = new ArrayList<>(JsonPool.SPIRE_AFFIXES.all().entrySet());
        pool.removeIf(e -> GsonHelper.getAsInt(e.getValue(), "weight", 10) <= 0
                || floor < GsonHelper.getAsInt(e.getValue(), "min_floor", 0));
        if (pool.isEmpty()) {
            return null;
        }
        int total = pool.stream().mapToInt(e -> GsonHelper.getAsInt(e.getValue(), "weight", 10)).sum();
        int r = new Random(season * 31L + floor * 0x9E3779B97F4A7C15L).nextInt(total);
        for (var e : pool) {
            r -= GsonHelper.getAsInt(e.getValue(), "weight", 10);
            if (r < 0) {
                return e.getKey();
            }
        }
        return pool.get(0).getKey();
    }

    public static void apply(Mob boss, int season, int floor) {
        double s = scale(floor);
        double hp = s;
        double dmg = s;
        double speed = 1.0D;
        ResourceLocation id = affix(season, floor);
        JsonObject a = id == null ? null : JsonPool.SPIRE_AFFIXES.get(id);
        if (a != null) {
            hp *= GsonHelper.getAsDouble(a, "health", 1.0D);
            dmg *= GsonHelper.getAsDouble(a, "damage", 1.0D);
            speed *= GsonHelper.getAsDouble(a, "speed", 1.0D);
            for (JsonElement el : GsonHelper.getAsJsonArray(a, "effects", new com.google.gson.JsonArray())) {
                JsonObject eo = el.getAsJsonObject();
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(GsonHelper.getAsString(eo, "id")))
                        .ifPresent(h -> boss.addEffect(new MobEffectInstance(h, -1, GsonHelper.getAsInt(eo, "amplifier", 0))));
            }
            boss.getPersistentData().putString(AFFIX, id.toString());
            boss.setCustomName(Component.translatable("khaosnexus.spire.boss", floor).copy().append(" ")
                    .append(Component.translatable("khaosnexus.affix." + id.getNamespace() + "." + id.getPath())));
        }
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && hp != 1.0D) {
            BigHealth.set(boss, health.getBaseValue() * hp);
        }
        mult(boss.getAttribute(Attributes.ATTACK_DAMAGE), dmg);
        mult(boss.getAttribute(Attributes.MOVEMENT_SPEED), speed);
    }

    private static void mult(AttributeInstance a, double m) {
        if (a != null && m != 1.0D) {
            a.setBaseValue(a.getBaseValue() * m);
        }
    }

    /** Bonus loot for an affixed boss floor. */
    public static List<ItemStack> rewards(Mob boss) {
        List<ItemStack> out = new ArrayList<>();
        String id = boss.getPersistentData().getString(AFFIX);
        JsonObject a = id.isEmpty() ? null : JsonPool.SPIRE_AFFIXES.get(ResourceLocation.parse(id));
        if (a != null) {
            int cores = GsonHelper.getAsInt(a, "rift_cores", 1);
            if (cores > 0) {
                out.add(new ItemStack(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get(), cores));
            }
        }
        return out;
    }
}

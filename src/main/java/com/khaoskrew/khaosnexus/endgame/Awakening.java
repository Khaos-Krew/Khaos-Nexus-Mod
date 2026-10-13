package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.network.ModPayloads;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Second Awakening. Quest steps come from awakeningSteps; options are datapack files with "class", "cooldown_seconds",
 * "self_effects", "enemy_effects" ([{id, amplifier, seconds}]), "radius", "damage", "heal_allies" and "particle".
 * Endgame keys: AwakenDone (quest finished), Awakening (chosen option id), AwakenReady (cooldown game time).
 */
public final class Awakening {
    public static final String DONE = "AwakenDone";
    public static final String CHOICE = "Awakening";
    public static final String READY = "AwakenReady";
    public static final String TITLE = "awakened";

    private Awakening() {}

    /** Quest steps not yet met; empty when the quest is complete. */
    public static List<Component> missing(HunterProfile p) {
        List<Component> out = new ArrayList<>();
        if (Prestige.level(p) < Prestige.NATIONAL) {
            out.add(Component.translatable("khaosnexus.awaken.need_national"));
        }
        for (String step : Config.AWAKENING_STEPS.get()) {
            String[] s = step.split(":");
            try {
                switch (s[0]) {
                    case "stat" -> {
                        int have = p.endgame.getInt(s[1]);
                        int want = Integer.parseInt(s[2]);
                        if (have < want) {
                            out.add(Component.translatable("khaosnexus.awaken.step_stat." + s[1], have, want));
                        }
                    }
                    case "spire" -> {
                        int want = Integer.parseInt(s[1]);
                        if (p.spireBest < want) {
                            out.add(Component.translatable("khaosnexus.prestige.need_spire", p.spireBest, want));
                        }
                    }
                    case "title" -> {
                        if (!p.hasTitle(s[1])) {
                            out.add(Component.translatable("khaosnexus.awaken.step_title", Component.translatable("khaosnexus.title." + s[1])));
                        }
                    }
                    default -> KhaosNexus.LOGGER.warn("Unknown awakening step {}", step);
                }
            } catch (RuntimeException ex) {
                KhaosNexus.LOGGER.warn("Bad awakening step {}", step);
            }
        }
        return out;
    }

    public static List<ResourceLocation> options(String classId) {
        List<ResourceLocation> out = new ArrayList<>();
        JsonPool.AWAKENINGS.all().forEach((id, o) -> {
            if (GsonHelper.getAsString(o, "class", "").equals(classId)) {
                out.add(id);
            }
        });
        out.sort(null);
        return out;
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("khaosnexus.awaken." + id.getNamespace() + "." + id.getPath());
    }

    /** Null when the choice was accepted, else a refusal translation key. */
    public static String choose(HunterProfile p, ResourceLocation id) {
        if (!Config.AWAKENING.get()) {
            return "khaosnexus.awaken.disabled";
        }
        if (!p.endgame.getString(CHOICE).isEmpty()) {
            return "khaosnexus.awaken.already";
        }
        if (!missing(p).isEmpty()) {
            return "khaosnexus.awaken.not_ready";
        }
        if (!options(p.classId).contains(id)) {
            return "khaosnexus.awaken.wrong_option";
        }
        p.endgame.putBoolean(DONE, true);
        p.endgame.putString(CHOICE, id.toString());
        return null;
    }

    public static boolean command(ServerPlayer player, String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : KhaosNexus.MODID + ":" + raw);
        HunterProfile p = HunterRanks.profile(player);
        String err = id == null ? "khaosnexus.awaken.wrong_option" : choose(p, id);
        if (err != null) {
            player.displayClientMessage(Component.translatable(err), false);
            return false;
        }
        celebrate(player, id);
        Titles.unlock(player, TITLE);
        HunterRanks.sync(player);
        return true;
    }

    /** The hype moment: lightning, totem screen effect, sound, server broadcast and a toast. */
    public static void celebrate(ServerPlayer player, ResourceLocation id) {
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.position());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.broadcastEntityEvent(player, (byte) 35);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 120, 0.6, 1.2, 0.6, 0.4);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("khaosnexus.awaken.broadcast",
                player.getDisplayName(), name(id)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        ModPayloads.sendFeedback(player, "khaosnexus.awaken.toast", "khaosnexus.awaken." + id.getNamespace() + "." + id.getPath(), "");
    }

    public static boolean has(Player player) {
        return Config.AWAKENING.get() && !HunterRanks.profile(player).endgame.getString(CHOICE).isEmpty();
    }

    /** Casts the awakened ability. Returns false when on cooldown or not unlocked. */
    public static boolean cast(ServerPlayer player) {
        HunterProfile p = HunterRanks.profile(player);
        String choice = p.endgame.getString(CHOICE);
        JsonObject o = choice.isEmpty() ? null : JsonPool.AWAKENINGS.get(ResourceLocation.parse(choice));
        if (o == null || !Config.AWAKENING.get()) {
            return false;
        }
        long now = player.level().getGameTime();
        long ready = p.endgame.getLong(READY);
        if (now < ready) {
            player.displayClientMessage(Component.translatable("khaosnexus.ability.cooldown", Math.max(1L, (ready - now + 19) / 20)), true);
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "self_effects", new com.google.gson.JsonArray())) {
            effect(e.getAsJsonObject(), player);
        }
        double r = GsonHelper.getAsDouble(o, "radius", 0.0D);
        if (r > 0) {
            float dmg = GsonHelper.getAsFloat(o, "damage", 0.0F);
            float heal = GsonHelper.getAsFloat(o, "heal_allies", 0.0F);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r))) {
                if (t == player) {
                    continue;
                }
                if (t instanceof Enemy && !com.khaoskrew.khaosnexus.hunter.ClassAbilities.isOwnedMinion(player, t instanceof net.minecraft.world.entity.Mob m ? m : null)) {
                    if (dmg > 0) {
                        t.hurt(player.damageSources().playerAttack(player), dmg);
                    }
                    for (JsonElement e : GsonHelper.getAsJsonArray(o, "enemy_effects", new com.google.gson.JsonArray())) {
                        effect(e.getAsJsonObject(), t);
                    }
                } else if (t instanceof Player ally && heal > 0) {
                    ally.heal(heal);
                }
            }
            if (heal > 0) {
                player.heal(heal);
            }
        }
        var particle = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.tryParse(GsonHelper.getAsString(o, "particle", "minecraft:end_rod")));
        if (particle instanceof net.minecraft.core.particles.SimpleParticleType sp) {
            level.sendParticles(sp, player.getX(), player.getY() + 1, player.getZ(), 60, Math.max(1, r / 2), 0.8, Math.max(1, r / 2), 0.05);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
        p.endgame.putLong(READY, now + GsonHelper.getAsInt(o, "cooldown_seconds", 60) * 20L);
        HunterRanks.sync(player);
        return true;
    }

    private static void effect(JsonObject e, LivingEntity target) {
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(GsonHelper.getAsString(e, "id"))).ifPresent(h ->
                target.addEffect(new MobEffectInstance(h, GsonHelper.getAsInt(e, "seconds", 10) * 20, GsonHelper.getAsInt(e, "amplifier", 0))));
    }

    public static Component status(HunterProfile p) {
        String choice = p.endgame.getString(CHOICE);
        if (!choice.isEmpty()) {
            return Component.translatable("khaosnexus.awaken.chosen", name(ResourceLocation.parse(choice)));
        }
        List<Component> m = missing(p);
        Component out = Component.translatable("khaosnexus.awaken.header");
        for (Component c : m) {
            out = out.copy().append("\n - ").append(c);
        }
        if (m.isEmpty()) {
            out = out.copy().append("\n").append(Component.translatable("khaosnexus.awaken.pick"));
            for (ResourceLocation id : options(p.classId)) {
                out = out.copy().append("\n > " + id.getPath() + ": ").append(name(id))
                        .append(" - ").append(Component.translatable("khaosnexus.awaken." + id.getNamespace() + "." + id.getPath() + ".desc"));
            }
        }
        return out;
    }
}

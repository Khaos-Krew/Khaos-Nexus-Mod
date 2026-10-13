package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Achievement particle auras. An aura file has "particle", optional "count"/"spread"/"speed", and "requires" with any
 * of: "title" (owned title id), "spire_best" (int), "rank" (minimum hunter rank letter), "stats" ({endgame key: min}).
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class Auras {
    public static final String KEY = "Aura";

    private Auras() {}

    public static boolean unlocked(HunterProfile p, JsonObject aura) {
        JsonObject r = GsonHelper.getAsJsonObject(aura, "requires", new JsonObject());
        if (r.has("title") && !p.hasTitle(GsonHelper.getAsString(r, "title"))) {
            return false;
        }
        if (p.spireBest < GsonHelper.getAsInt(r, "spire_best", 0)) {
            return false;
        }
        if (r.has("rank") && HunterRanks.rankOfXp(p.xp).ordinal() < GateRank.byName(GsonHelper.getAsString(r, "rank")).ordinal()) {
            return false;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> e : GsonHelper.getAsJsonObject(r, "stats", new JsonObject()).entrySet()) {
            if (p.endgame.getInt(e.getKey()) < e.getValue().getAsInt()) {
                return false;
            }
        }
        return true;
    }

    public static List<ResourceLocation> unlockedIds(HunterProfile p) {
        List<ResourceLocation> out = new ArrayList<>();
        JsonPool.AURAS.all().forEach((id, a) -> {
            if (unlocked(p, a)) {
                out.add(id);
            }
        });
        return out;
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("khaosnexus.aura." + id.getNamespace() + "." + id.getPath());
    }

    public static boolean equip(ServerPlayer player, String raw) {
        HunterProfile p = HunterRanks.profile(player);
        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : KhaosNexus.MODID + ":" + raw);
        JsonObject a = id == null ? null : JsonPool.AURAS.get(id);
        if (a == null || !unlocked(p, a)) {
            player.displayClientMessage(Component.translatable("khaosnexus.aura.locked", raw), false);
            return false;
        }
        p.endgame.putString(KEY, id.toString());
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.aura.equipped", name(id)), false);
        return true;
    }

    public static void clear(ServerPlayer player) {
        HunterRanks.profile(player).endgame.remove(KEY);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.aura.cleared"), false);
    }

    public static Component list(ServerPlayer player) {
        List<ResourceLocation> ids = unlockedIds(HunterRanks.profile(player));
        if (ids.isEmpty()) {
            return Component.translatable("khaosnexus.aura.none");
        }
        Component out = Component.translatable("khaosnexus.aura.list");
        for (ResourceLocation id : ids) {
            out = out.copy().append("\n - " + id.getPath() + ": ").append(name(id));
        }
        return out;
    }

    public static ParticleOptions particle(JsonObject a) {
        var type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.tryParse(GsonHelper.getAsString(a, "particle", "minecraft:enchant")));
        return type instanceof SimpleParticleType simple ? simple : null;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Config.AURAS.get()
                || player.tickCount % Config.AURA_INTERVAL.get() != 0 || player.isSpectator() || player.isInvisible()) {
            return;
        }
        String equipped = HunterRanks.profile(player).endgame.getString(KEY);
        if (equipped.isEmpty()) {
            return;
        }
        JsonObject a = JsonPool.AURAS.get(ResourceLocation.tryParse(equipped));
        ParticleOptions particle = a == null ? null : particle(a);
        if (particle == null || !unlocked(HunterRanks.profile(player), a)) {
            return;
        }
        double spread = GsonHelper.getAsDouble(a, "spread", 0.4D);
        ((ServerLevel) player.level()).sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(),
                GsonHelper.getAsInt(a, "count", 4), spread, 0.6D, spread, GsonHelper.getAsDouble(a, "speed", 0.01D));
    }
}

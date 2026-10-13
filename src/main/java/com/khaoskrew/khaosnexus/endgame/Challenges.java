package com.khaoskrew.khaosnexus.endgame;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Monthly challenge gates. A file: "month" ("YYYY-MM" to pin it to one month, or omit to join the rotation),
 * "rules" {"solo", "class" (class id or "any"), "min_rank", "time_limit_seconds"}, "stages" (list of lists of entity
 * ids), "health_scale", "heal_after", "title" (title id awarded on the first clear) and "rift_cores".
 * Every attempt is one life: dying ends the attempt.
 */
public final class Challenges {
    private static final String MONTH = "ChallengeMonth";
    private static final String TRIES = "ChallengeTries";
    private static final String CLEARED = "ChallengeCleared";

    private Challenges() {}

    public static String month(long epochMs) {
        return YearMonth.from(java.time.Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC)).toString();
    }

    /** The active challenge for a month: a file pinned to that month, else a rotation over unpinned files. */
    public static ResourceLocation active(String month) {
        if (!Config.CHALLENGES.get()) {
            return null;
        }
        if (!Config.CHALLENGE_FORCE.get().isBlank()) {
            ResourceLocation f = ResourceLocation.tryParse(Config.CHALLENGE_FORCE.get().trim());
            return f != null && JsonPool.CHALLENGES.get(f) != null ? f : null;
        }
        List<ResourceLocation> rotation = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonObject> e : JsonPool.CHALLENGES.all().entrySet()) {
            String pinned = GsonHelper.getAsString(e.getValue(), "month", "");
            if (pinned.equals(month)) {
                return e.getKey();
            }
            if (pinned.isEmpty()) {
                rotation.add(e.getKey());
            }
        }
        if (rotation.isEmpty()) {
            return null;
        }
        rotation.sort(null);
        YearMonth ym = YearMonth.parse(month);
        return rotation.get(Math.floorMod(ym.getYear() * 12 + ym.getMonthValue(), rotation.size()));
    }

    public static ResourceLocation active() {
        return active(month(System.currentTimeMillis()));
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("khaosnexus.challenge." + id.getNamespace() + "." + id.getPath());
    }

    /** Null when the player may attempt, else the refusal message. */
    public static Component refusal(HunterProfile p, JsonObject c, int partySize, String month) {
        JsonObject r = GsonHelper.getAsJsonObject(c, "rules", new JsonObject());
        if (GsonHelper.getAsBoolean(r, "solo", true) && partySize > 1) {
            return Component.translatable("khaosnexus.challenge.solo");
        }
        String cls = GsonHelper.getAsString(r, "class", "any");
        if (!cls.equals("any") && !cls.equals(p.classId)) {
            return Component.translatable("khaosnexus.challenge.class", Component.translatable("khaosnexus.class." + cls));
        }
        GateRank min = GateRank.byName(GsonHelper.getAsString(r, "min_rank", "S"));
        if (HunterRanks.rankOfXp(p.xp).ordinal() < min.ordinal()) {
            return Component.translatable("khaosnexus.challenge.rank", Component.translatable(min.translationKey()));
        }
        int cap = Config.CHALLENGE_ATTEMPTS.get();
        if (cap > 0 && month.equals(p.endgame.getString(MONTH)) && p.endgame.getInt(TRIES) >= cap) {
            return Component.translatable("khaosnexus.challenge.tries", cap);
        }
        return null;
    }

    public static void spendAttempt(HunterProfile p, String month) {
        if (!month.equals(p.endgame.getString(MONTH))) {
            p.endgame.putString(MONTH, month);
            p.endgame.putInt(TRIES, 0);
        }
        p.endgame.putInt(TRIES, p.endgame.getInt(TRIES) + 1);
    }

    public static List<List<EntityType<?>>> stages(JsonObject c) {
        List<List<EntityType<?>>> out = new ArrayList<>();
        for (JsonElement stage : GsonHelper.getAsJsonArray(c, "stages")) {
            List<EntityType<?>> group = new ArrayList<>();
            for (JsonElement id : stage.getAsJsonArray()) {
                ResourceLocation rl = ResourceLocation.tryParse(id.getAsString());
                if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                    group.add(BuiltInRegistries.ENTITY_TYPE.get(rl));
                }
            }
            if (!group.isEmpty()) {
                out.add(group);
            }
        }
        return out;
    }

    public static boolean start(ServerPlayer player) {
        ResourceLocation id = active();
        JsonObject c = id == null ? null : JsonPool.CHALLENGES.get(id);
        if (c == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.challenge.none"), false);
            return false;
        }
        HunterProfile p = HunterRanks.profile(player);
        String month = month(System.currentTimeMillis());
        Component no = refusal(p, c, PartyService.size(player), month);
        if (no != null) {
            player.displayClientMessage(no, false);
            return false;
        }
        List<List<EntityType<?>>> stages = stages(c);
        if (stages.isEmpty()) {
            player.displayClientMessage(Component.translatable("khaosnexus.challenge.none"), false);
            return false;
        }
        BossRush.Run run = new BossRush.Run(stages, GsonHelper.getAsInt(c, "heal_after", 0),
                GsonHelper.getAsDouble(c, "health_scale", 1.0D), BossRush.nextOrigin());
        run.label = "khaosnexus.challenge.mob";
        int limit = GsonHelper.getAsInt(GsonHelper.getAsJsonObject(c, "rules", new JsonObject()), "time_limit_seconds", 0);
        run.onWin = (pl, ticks) -> {
            if (limit > 0 && ticks > limit * 20L) {
                pl.displayClientMessage(Component.translatable("khaosnexus.challenge.too_slow", limit), false);
                return;
            }
            win(pl, id, c, ticks);
        };
        if (!BossRush.launch(player, List.of(player), run)) {
            return false;
        }
        spendAttempt(p, month);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.challenge.started", name(id)), false);
        return true;
    }

    static void win(ServerPlayer player, ResourceLocation id, JsonObject c, long ticks) {
        HunterProfile p = HunterRanks.profile(player);
        String key = id.toString();
        String cleared = p.endgame.getString(CLEARED);
        boolean first = !java.util.Arrays.asList(cleared.split(",")).contains(key);
        if (first) {
            p.endgame.putString(CLEARED, cleared.isEmpty() ? key : cleared + "," + key);
            p.endgame.putInt(Prestige.CHALLENGES, p.endgame.getInt(Prestige.CHALLENGES) + 1);
            int cores = GsonHelper.getAsInt(c, "rift_cores", 3);
            if (cores > 0) {
                ItemStack s = new ItemStack(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get(), cores);
                if (!player.getInventory().add(s)) {
                    player.drop(s, false);
                }
            }
        }
        String title = GsonHelper.getAsString(c, "title", "");
        if (!title.isEmpty()) {
            Titles.unlock(player, title);
        }
        Leaderboards.get(player.server).submit(Leaderboards.challengeBoard(id.getPath()), player.getUUID(), player.getGameProfile().getName(), ticks);
        player.displayClientMessage(Component.translatable("khaosnexus.challenge.won", name(id),
                String.format("%d:%02d", ticks / 1200, (ticks / 20) % 60)), false);
        HunterRanks.sync(player);
        Prestige.tryPromote(player);
    }

    public static Component describe(ServerPlayer player) {
        ResourceLocation id = active();
        JsonObject c = id == null ? null : JsonPool.CHALLENGES.get(id);
        if (c == null) {
            return Component.translatable("khaosnexus.challenge.none");
        }
        JsonObject r = GsonHelper.getAsJsonObject(c, "rules", new JsonObject());
        Component no = refusal(HunterRanks.profile(player), c, PartyService.size(player), month(System.currentTimeMillis()));
        return Component.translatable("khaosnexus.challenge.info", name(id),
                GsonHelper.getAsBoolean(r, "solo", true) ? "solo" : "party",
                GsonHelper.getAsString(r, "class", "any"),
                GsonHelper.getAsString(r, "min_rank", "S"))
                .append(no == null ? Component.translatable("khaosnexus.challenge.ready") : Component.literal("\n").append(no));
    }
}

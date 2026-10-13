package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server leaderboards: fastest clear per gate rank, highest Spire floor per season, weekly-modifier clears per week.
 * Each board keeps one best value per player and is trimmed to 50 entries.
 */
public final class Leaderboards extends SavedData {
    public static final String ID = KhaosNexus.MODID + "_leaderboards";
    private static final int KEEP = 50;

    public record Entry(UUID id, String name, long value) {}

    private final Map<String, Map<UUID, Entry>> boards = new HashMap<>();

    public static Leaderboards get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Leaderboards::new, Leaderboards::load), ID);
    }

    public static String clearBoard(GateRank rank) {
        return "clear_" + rank.name();
    }

    public static String spireBoard(int season) {
        return "spire_s" + season;
    }

    public static String weeklyBoard(long week) {
        return "weekly_w" + week;
    }

    public static String rushBoard() {
        return "rush_all";
    }

    public static String challengeBoard(String challenge) {
        return "challenge_" + challenge;
    }

    /** Lower is better for clear times; higher for everything else. */
    public static boolean lowerBetter(String board) {
        return board.startsWith("clear_") || board.startsWith("rush_") || board.startsWith("challenge_");
    }

    /** Records a value; returns the player's 1-based place on that board, or 0 when boards are off. */
    public int submit(String board, UUID id, String name, long value) {
        if (!Config.LEADERBOARDS.get()) {
            return 0;
        }
        Map<UUID, Entry> b = this.boards.computeIfAbsent(board, k -> new HashMap<>());
        Entry old = b.get(id);
        boolean low = lowerBetter(board);
        if (old == null || (low ? value < old.value() : value > old.value())) {
            b.put(id, new Entry(id, name, value));
            this.trim(board);
            this.setDirty();
        }
        List<Entry> top = this.top(board, KEEP);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** Adds to a counter board (weekly clears). */
    public void increment(String board, UUID id, String name) {
        Map<UUID, Entry> b = this.boards.computeIfAbsent(board, k -> new HashMap<>());
        Entry old = b.get(id);
        b.put(id, new Entry(id, name, old == null ? 1 : old.value() + 1));
        this.setDirty();
    }

    public List<Entry> top(String board, int n) {
        Comparator<Entry> c = Comparator.comparingLong(Entry::value);
        if (!lowerBetter(board)) {
            c = c.reversed();
        }
        List<Entry> all = new ArrayList<>(this.boards.getOrDefault(board, Map.of()).values());
        all.sort(c.thenComparing(Entry::name));
        return all.subList(0, Math.min(n, all.size()));
    }

    private void trim(String board) {
        Map<UUID, Entry> b = this.boards.get(board);
        if (b.size() > KEEP) {
            List<Entry> keep = this.top(board, KEEP);
            b.clear();
            keep.forEach(e -> b.put(e.id(), e));
        }
    }

    public static Component format(String board, List<Entry> top) {
        net.minecraft.network.chat.MutableComponent out = Component.translatable("khaosnexus.top.header." + board.replaceAll("_.*", ""), board.replaceAll("^[a-z]+_", "").replaceAll("^[sw](?=\\d)", ""));
        if (top.isEmpty()) {
            return out.append("\n").append(Component.translatable("khaosnexus.top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            Entry e = top.get(i);
            String v = lowerBetter(board) ? String.format("%d:%02d", e.value() / 1200, (e.value() / 20) % 60) : Long.toString(e.value());
            out.append("\n").append(Component.literal((i + 1) + ". " + e.name() + "  " + v));
        }
        return out;
    }

    public static void recordClear(ServerPlayer player, GateRank rank, long ticks) {
        Leaderboards lb = get(player.server);
        int place = lb.submit(clearBoard(rank), player.getUUID(), player.getGameProfile().getName(), ticks);
        if (place > 0 && place <= Config.LEADERBOARD_SIZE.get()) {
            player.displayClientMessage(Component.translatable("khaosnexus.top.placed", place, Component.translatable(rank.translationKey())), false);
        }
        if (WeeklyModifiers.applies(rank) && !WeeklyModifiers.active().isEmpty()) {
            lb.increment(weeklyBoard(WeeklyModifiers.currentWeek()), player.getUUID(), player.getGameProfile().getName());
        }
    }

    public static void recordSpire(ServerPlayer player, int season, int floor) {
        get(player.server).submit(spireBoard(season), player.getUUID(), player.getGameProfile().getName(), floor);
    }

    private static Leaderboards load(CompoundTag tag, HolderLookup.Provider registries) {
        Leaderboards lb = new Leaderboards();
        CompoundTag boards = tag.getCompound("Boards");
        for (String key : boards.getAllKeys()) {
            Map<UUID, Entry> b = new HashMap<>();
            for (Tag t : boards.getList(key, Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) t;
                b.put(e.getUUID("Id"), new Entry(e.getUUID("Id"), e.getString("Name"), e.getLong("Value")));
            }
            lb.boards.put(key, b);
        }
        return lb;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag boards = new CompoundTag();
        this.boards.forEach((key, b) -> {
            ListTag list = new ListTag();
            for (Entry e : b.values()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("Id", e.id());
                c.putString("Name", e.name());
                c.putLong("Value", e.value());
                list.add(c);
            }
            boards.put(key, list);
        });
        tag.put("Boards", boards);
        return tag;
    }
}

package com.khaoskrew.khaosnexus.title;

import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.network.ModPayloads;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Titles {
    public static final String GATE_BREAKER = "gate_breaker";
    public static final String RAID_VICTOR = "raid_victor";
    public static final String SPIRE_CLIMBER = "spire_climber";
    public static final String HUNTER_S = "hunter_s";
    public static final String CLASS_BOUND = "class_bound";

    private Titles() {}

    public static void unlock(ServerPlayer player, String id) {
        HunterProfile profile = HunterRanks.profile(player);
        if (profile.hasTitle(id)) {
            return;
        }
        profile.addTitle(id);
        com.khaoskrew.khaosnexus.compat.ProgressHooks.titleEarned(player, id);
        if (profile.title.isEmpty()) {
            profile.title = id;
            apply(player);
        }
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.title.unlocked", Component.translatable("khaosnexus.title." + id)), false);
        ModPayloads.sendFeedback(player, "khaosnexus.title.toast", "khaosnexus.title." + id, "");
    }

    public static boolean equip(ServerPlayer player, String id) {
        HunterProfile profile = HunterRanks.profile(player);
        if (!profile.hasTitle(id)) {
            player.displayClientMessage(Component.translatable("khaosnexus.title.locked", id), false);
            return false;
        }
        profile.title = id;
        apply(player);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.title.equipped", Component.translatable("khaosnexus.title." + id)), false);
        return true;
    }

    public static void clear(ServerPlayer player) {
        HunterRanks.profile(player).title = "";
        apply(player);
        HunterRanks.sync(player);
    }

    public static void apply(ServerPlayer player) {
        player.getSuffixes().clear();
        String title = HunterRanks.profile(player).title;
        if (!title.isEmpty()) {
            player.getSuffixes().add(Component.literal(" [").append(Component.translatable("khaosnexus.title." + title)).append("]"));
        }
        player.refreshDisplayName();
    }

    public static void list(ServerPlayer player) {
        HunterProfile profile = HunterRanks.profile(player);
        if (profile.titles.isEmpty()) {
            player.displayClientMessage(Component.translatable("khaosnexus.title.none"), false);
            return;
        }
        player.displayClientMessage(Component.translatable("khaosnexus.title.list", profile.titles, profile.title), false);
    }
}

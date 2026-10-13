package com.khaoskrew.khaosnexus.hunter;

import java.util.UUID;

import com.khaoskrew.khaosnexus.bounty.Bounties;
import com.khaoskrew.khaosnexus.title.Titles;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.network.ModPayloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
public final class HunterRanks {
    private HunterRanks() {}

    public static HunterProfile profile(Player player) {
        return player.getData(ModAttachments.HUNTER);
    }

    public static void sync(ServerPlayer player) {
        player.syncData(ModAttachments.HUNTER);
    }

    public static int threshold(GateRank rank) {
        return switch (rank) {
            case E -> 0;
            case D -> Config.XP_D.get();
            case C -> Config.XP_C.get();
            case B -> Config.XP_B.get();
            case A -> Config.XP_A.get();
            case S -> Config.XP_S.get();
        };
    }

    public static GateRank rankOfXp(int xp) {
        GateRank current = GateRank.E;
        for (GateRank rank : GateRank.values()) {
            if (xp >= threshold(rank)) {
                current = rank;
            }
        }
        return current;
    }

    public static GateRank rankOf(Player player) {
        return rankOfXp(profile(player).xp);
    }

    public static void setXp(ServerPlayer player, int xp) {
        HunterProfile profile = profile(player);
        GateRank before = rankOfXp(profile.xp);
        profile.xp = Math.max(0, xp);
        GateRank after = rankOfXp(profile.xp);
        sync(player);
        if (after.ordinal() > before.ordinal()) {
            announceRankUp(player, after);
            if (after == GateRank.S) {
                Titles.unlock(player, Titles.HUNTER_S);
            }
        }
    }

    public static void addXp(ServerPlayer player, int amount) {
        if (amount == 0) {
            return;
        }
        setXp(player, profile(player).xp + amount);
    }

    public static void onGateCleared(ServerLevel level, BlockPos pos, GateRank gateRank, UUID activator) {
        if (activator == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(activator);
        if (player == null) {
            return;
        }
        HunterProfile profile = profile(player);
        profile.gatesCleared++;
        addXp(player, gateRank.tuning().hunterXp.get());
        Bounties.onGateClear(player);
        Titles.unlock(player, Titles.GATE_BREAKER);
        player.displayClientMessage(Component.translatable(
                "khaosnexus.hunter.gate_xp",
                gateRank.tuning().hunterXp.get(),
                Component.translatable(rankOf(player).translationKey())), false);
    }

    private static void announceRankUp(ServerPlayer player, GateRank rank) {
        Component label = Component.translatable(rank.translationKey());
        player.displayClientMessage(Component.translatable("khaosnexus.hunter.rank_up", label), false);
        ModPayloads.sendFeedback(
                player,
                "khaosnexus.hunter.rank_up_title",
                "khaosnexus.hunter.rank_up",
                rank.translationKey());
    }
}

package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class HunterAccess {
    private HunterAccess() {}

    public static boolean canOpen(ServerPlayer player, GateRank gateRank) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!Config.GATE_ACCESS.get() || mode.isCreative() || mode == GameType.SPECTATOR) {
            return true;
        }
        if (HunterRanks.rankOf(player).ordinal() >= gateRank.ordinal()) {
            return true;
        }
        return HunterRanks.profile(player).partySize >= Config.GATE_PARTY_SIZE.get();
    }
}

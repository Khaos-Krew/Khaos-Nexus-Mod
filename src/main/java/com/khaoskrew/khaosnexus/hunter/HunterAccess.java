package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.server.level.ServerPlayer;

public final class HunterAccess {
    private HunterAccess() {}

    public static boolean canOpen(ServerPlayer player, GateRank gateRank) {
        if (!Config.GATE_ACCESS.get() || player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (HunterRanks.rankOf(player).ordinal() >= gateRank.ordinal()) {
            return true;
        }
        return HunterRanks.profile(player).partySize >= Config.GATE_PARTY_SIZE.get();
    }
}

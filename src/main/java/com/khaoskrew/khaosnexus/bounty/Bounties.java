package com.khaoskrew.khaosnexus.bounty;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.network.ModPayloads;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class Bounties {
    public static final int GATES = 1;
    public static final int RAIDS = 2;
    public static final int SPIRE = 4;

    private Bounties() {}

    public static void refresh(ServerPlayer player) {
        HunterProfile profile = HunterRanks.profile(player);
        long day = player.serverLevel().getDayTime() / 24000L;
        if (profile.bountyDay == day) {
            return;
        }
        profile.bountyDay = day;
        profile.bountyGates = 0;
        profile.bountyRaids = 0;
        profile.bountyClaims = 0;
    }

    public static void onGateClear(ServerPlayer player) {
        refresh(player);
        HunterRanks.profile(player).bountyGates++;
        claim(player, GATES, HunterRanks.profile(player).bountyGates, Config.BOUNTY_GATES.get(), Config.BOUNTY_GATE_TOKENS.get(), "khaosnexus.bounty.gates_done");
        HunterRanks.sync(player);
    }

    public static void onRaidClear(ServerPlayer player) {
        refresh(player);
        HunterProfile profile = HunterRanks.profile(player);
        profile.bountyRaids++;
        profile.raidsCleared++;
        claim(player, RAIDS, profile.bountyRaids, Config.BOUNTY_RAIDS.get(), Config.BOUNTY_RAID_TOKENS.get(), "khaosnexus.bounty.raids_done");
        HunterRanks.sync(player);
    }

    public static void onSpireFloor(ServerPlayer player, int floor) {
        refresh(player);
        if (floor < Config.BOUNTY_SPIRE_FLOOR.get()) {
            HunterRanks.sync(player);
            return;
        }
        claim(player, SPIRE, floor, Config.BOUNTY_SPIRE_FLOOR.get(), Config.BOUNTY_SPIRE_TOKENS.get(), "khaosnexus.bounty.spire_done");
        HunterRanks.sync(player);
    }

    public static void describe(ServerPlayer player) {
        refresh(player);
        HunterProfile profile = HunterRanks.profile(player);
        player.displayClientMessage(Component.translatable(
                "khaosnexus.bounty.list",
                profile.bountyGates, Config.BOUNTY_GATES.get(),
                profile.bountyRaids, Config.BOUNTY_RAIDS.get(),
                Math.max(profile.spireBest, profile.spireFloor - 1), Config.BOUNTY_SPIRE_FLOOR.get()), false);
        HunterRanks.sync(player);
    }

    private static void claim(ServerPlayer player, int bit, int progress, int target, int tokens, String message) {
        HunterProfile profile = HunterRanks.profile(player);
        if (progress < target || (profile.bountyClaims & bit) != 0) {
            return;
        }
        profile.bountyClaims |= bit;
        com.khaoskrew.khaosnexus.compat.ProgressHooks.bountyCompleted(player, message.replace("khaosnexus.bounty.", "").replace("_done", ""));
        ItemStack reward = new ItemStack(ModRegistries.NEXUS_TOKEN.get(), tokens);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable(message, tokens), false);
        ModPayloads.sendFeedback(player, "khaosnexus.bounty.toast", "khaosnexus.bounty.toast_body", "");
    }
}

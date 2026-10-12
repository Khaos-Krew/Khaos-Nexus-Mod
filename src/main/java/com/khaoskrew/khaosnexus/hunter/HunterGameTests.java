package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class HunterGameTests {
    private static final BlockPos GATE = new BlockPos(7, 1, 7);

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 40)
    public static void hunterRankFollowsXp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HunterRanks.setXp(player, 0);
        if (HunterRanks.rankOf(player) != GateRank.E) {
            helper.fail("zero XP should be hunter rank E");
        }
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.D));
        if (HunterRanks.rankOf(player) != GateRank.D) {
            helper.fail("the D threshold should set hunter rank D, was " + HunterRanks.rankOf(player));
        }
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.S));
        if (HunterRanks.rankOf(player) != GateRank.S) {
            helper.fail("the S threshold should set hunter rank S");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 400)
    public static void clearingAGateGrantsHunterXp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HunterRanks.setXp(player, 0);
        helper.setBlock(GATE, ModRegistries.NEXUS_GATE.get());
        NexusGateBlockEntity gate = helper.getBlockEntity(GATE);
        gate.setRank(GateRank.E);
        helper.useBlock(GATE, player);
        helper.succeedWhen(() -> {
            BlockPos absolute = helper.absolutePos(GATE);
            if (!helper.getLevel().getBlockState(absolute).getValue(NexusGateBlock.CLOSED)) {
                NexusGateBlockEntity live = helper.getBlockEntity(GATE);
                if (live.isActive()) {
                    helper.killAllEntitiesOfClass(Mob.class);
                }
                throw new GameTestAssertException("waiting for the gate to close");
            }
            if (HunterRanks.profile(player).xp <= 0) {
                helper.fail("clearing the gate did not grant hunter XP");
            }
            if (HunterRanks.profile(player).gatesCleared < 1) {
                helper.fail("the clear was not counted");
            }
        });
    }
}

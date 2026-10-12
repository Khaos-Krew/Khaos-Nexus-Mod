package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 40)
    public static void oneClassAtATime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        if (!ClassService.equip(player, "tank")) {
            helper.fail("tank did not equip");
        }
        AttributeInstance armor = player.getAttribute(HunterClass.TANK.attribute());
        if (armor == null || !armor.hasModifier(HunterClass.MODIFIER_ID)) {
            helper.fail("tank passive is missing");
        }
        HunterRanks.profile(player).swapReady = 0L;
        if (!ClassService.equip(player, "assassin")) {
            helper.fail("assassin did not equip");
        }
        if (armor.hasModifier(HunterClass.MODIFIER_ID)) {
            helper.fail("tank passive stayed on after the swap");
        }
        AttributeInstance speed = player.getAttribute(HunterClass.ASSASSIN.attribute());
        if (speed == null || !speed.hasModifier(HunterClass.MODIFIER_ID)) {
            helper.fail("assassin passive is missing");
        }
        if (ClassService.equipped(player) != HunterClass.ASSASSIN) {
            helper.fail("two classes were equipped");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 40)
    public static void higherGatesNeedRankOrParty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        HunterRanks.setXp(player, 0);
        helper.setBlock(GATE, ModRegistries.NEXUS_GATE.get());
        NexusGateBlockEntity gate = helper.getBlockEntity(GATE);
        gate.setRank(GateRank.S);
        helper.useBlock(GATE, player);
        if (gate.isActive()) {
            helper.fail("an E-rank hunter opened an S gate alone");
        }
        HunterRanks.setXp(player, HunterRanks.threshold(GateRank.S));
        helper.useBlock(GATE, player);
        if (!gate.isActive()) {
            helper.fail("an S-rank hunter could not open an S gate");
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

package com.khaoskrew.khaosnexus.gate;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class GateGameTests {
    private static final BlockPos GATE = new BlockPos(7, 1, 7);

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 400)
    public static void waveOpensAndCloses(GameTestHelper helper) {
        helper.setBlock(GATE, ModRegistries.NEXUS_GATE.get());
        NexusGateBlockEntity gate = helper.getBlockEntity(GATE);
        if (gate.getRank() == null) {
            helper.fail("placed gate has no rank");
        }
        helper.useBlock(GATE);
        helper.succeedWhen(() -> {
            BlockPos absolute = helper.absolutePos(GATE);
            if (!helper.getLevel().getBlockState(absolute).getValue(NexusGateBlock.CLOSED)) {
                NexusGateBlockEntity live = helper.getBlockEntity(GATE);
                if (live.isActive()) {
                    helper.killAllEntitiesOfClass(Mob.class);
                }
                throw new GameTestAssertException("waiting for the wave to clear");
            }
        });
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 100)
    public static void naturalSpawningCanBeDisabled(GameTestHelper helper) {
        boolean spawning = Config.NATURAL_SPAWNING.get();
        int perDay = Config.NATURAL_SPAWNS_PER_DAY.get();
        try {
            Config.NATURAL_SPAWNING.set(false);
            if (GateWorldSpawner.isEnabled()) {
                helper.fail("natural spawning stayed on after the config switch");
            }
            Config.NATURAL_SPAWNING.set(true);
            Config.NATURAL_SPAWNS_PER_DAY.set(0);
            if (GateWorldSpawner.isEnabled()) {
                helper.fail("a daily cap of zero still allowed natural gates");
            }
            if (GateWorldSpawner.trySpawn(helper.getLevel().getServer())) {
                helper.fail("trySpawn placed a gate while natural spawning was off");
            }
        } finally {
            Config.NATURAL_SPAWNING.set(spawning);
            Config.NATURAL_SPAWNS_PER_DAY.set(perDay);
        }
        if (helper.getLevel().getServer().getRecipeManager().byKey(ResourceLocation.parse("khaosnexus:nexus_gate")).isEmpty()) {
            helper.fail("missing nexus gate crafting recipe");
        }
        helper.succeed();
    }
}

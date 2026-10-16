package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.khaoskrew.khaosnexus.spire.SpireService;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 1.1.3 audit: escapes, non-explosion block changes, and gate loot rerolls. */
@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class AuditGameTests {
    @SuppressWarnings("removal")
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void dungeonBlocksPearlsBucketsAndGriefing(GameTestHelper helper) {
        var p = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var dim : java.util.List.of(DungeonRules.GATE_DUNGEON, SpireService.DIMENSION)) {
            helper.assertTrue(DungeonRules.blocksTeleport(dim, p), "pearl/chorus blocked in " + dim.location());
            helper.assertTrue(DungeonRules.blocksItemUse(dim, p, new ItemStack(Items.WATER_BUCKET)), "water bucket blocked");
            helper.assertTrue(DungeonRules.blocksItemUse(dim, p, new ItemStack(Items.LAVA_BUCKET)), "lava bucket blocked");
            helper.assertTrue(DungeonRules.blocksItemUse(dim, p, new ItemStack(Items.POWDER_SNOW_BUCKET)), "powder snow blocked");
            helper.assertTrue(DungeonRules.blocksItemUse(dim, p, new ItemStack(Items.LILY_PAD)), "lily pad blocked");
            helper.assertTrue(!DungeonRules.blocksItemUse(dim, p, new ItemStack(Items.BREAD)), "food still usable");
            var enderman = EntityType.ENDERMAN.create(helper.getLevel());
            helper.assertTrue(DungeonRules.deniesGrief(dim, enderman), "endermen can't grief");
        }
        helper.assertTrue(!DungeonRules.blocksTeleport(Level.OVERWORLD, p) && !DungeonRules.blocksItemUse(Level.OVERWORLD, p, new ItemStack(Items.WATER_BUCKET))
                && !DungeonRules.deniesGrief(Level.OVERWORLD, EntityType.ENDERMAN.create(helper.getLevel())), "overworld untouched");
        helper.assertTrue(!DungeonRules.blocksTeleport(DungeonRules.GATE_DUNGEON, helper.makeMockPlayer(GameType.CREATIVE)), "creative exempt");
        helper.succeed();
    }

    /** A collapsed (abandoned) dungeon closes its gate, so it can't be reopened for fresh chests and a new boss. */
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void abandonedDungeonClosesGate(GameTestHelper helper) {
        BlockPos at = new BlockPos(7, 1, 7);
        helper.setBlock(at, ModRegistries.NEXUS_GATE.get());
        NexusGateBlockEntity gate = helper.getBlockEntity(at);
        gate.collapseDungeon(helper.getLevel());
        helper.assertTrue(gate.isClosed(), "gate closed after collapse");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(at)).getValue(NexusGateBlock.CLOSED), "gate block closed");
        helper.succeed();
    }
}

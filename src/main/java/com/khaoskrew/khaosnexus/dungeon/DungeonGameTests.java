package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class DungeonGameTests {
    /** A finished instance is wiped (blocks and mobs) and its region is handed to the next dungeon. */
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 2400)
    public static void finishedInstanceIsWipedAndReused(GameTestHelper helper) {
        // The game test server only loads the preset's levels, so fall back to the test level; the instance
        // bookkeeping is the same in any level.
        ServerLevel found = DungeonService.level(helper.getLevel().getServer());
        ServerLevel dungeon = found != null ? found : helper.getLevel();
        DungeonInstances instances = DungeonInstances.get(dungeon);
        DungeonInstances.Lease lease = DungeonService.allocate(dungeon);
        DungeonService.Layout layout = DungeonService.build(dungeon, GateRank.E, lease.origin(), dungeon.getRandom());
        if (layout == null) {
            helper.fail("E dungeon failed to build");
            return;
        }
        DungeonService.populate(dungeon, GateRank.E, layout, dungeon.getRandom(), lease);
        AABB area = AABB.of(layout.box());
        if (dungeon.getEntitiesOfClass(Mob.class, area).isEmpty()) {
            helper.fail("dungeon spawned no mobs");
        }
        DungeonService.release(dungeon, lease, layout.box());
        if (instances.isLive(lease.slot(), lease.gen())) {
            helper.fail("released lease still live");
        }
        BlockPos probe = layout.spawn().below();
        helper.succeedWhen(() -> {
            if (!instances.isFree(lease.slot())) {
                instances.tick(dungeon);
                helper.assertTrue(false, "wiping");
            }
            helper.assertTrue(dungeon.getBlockState(probe).isAir(), "entrance floor survived the wipe");
            helper.assertTrue(dungeon.getEntitiesOfClass(Mob.class, area.inflate(16.0D)).isEmpty(), "old mobs survived the wipe");
            DungeonInstances.Lease again = DungeonService.allocate(dungeon);
            DungeonService.release(dungeon, again, layout.box());
            // The lowest free slot is handed out first, so a wiped region is reused before a new one is opened.
            helper.assertTrue(again.slot() <= lease.slot(), "freed slot was not reused");
        });
    }
}

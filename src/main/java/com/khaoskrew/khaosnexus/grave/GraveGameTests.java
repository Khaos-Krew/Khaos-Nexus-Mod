package com.khaoskrew.khaosnexus.grave;

import com.khaoskrew.khaosnexus.KhaosNexus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class GraveGameTests {
    private static BlockPos pad(GameTestHelper h, int dx) {
        BlockPos o = h.absolutePos(new BlockPos(0, 60, 0)).offset(dx, 0, 0);
        ServerLevel l = h.getLevel();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            l.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            for (int y = 0; y < 4; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        return o;
    }

    private static List<ItemStack> loot() {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Items.DIAMOND_SWORD));
        l.add(new ItemStack(Items.GOLDEN_APPLE, 5));
        return l;
    }

    private static SafeGraves.Anchor at(ServerLevel l, BlockPos p) {
        return new SafeGraves.Anchor(l.dimension(), p);
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void graveLandsAtGateEntrance(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = pad(h, 0);
        UUID owner = UUID.randomUUID();
        BlockPos g = SafeGraves.place(l.getServer(), owner, "Tester", at(l, o), List.of(), loot());
        h.assertTrue(o.equals(g), "grave at the entrance itself");
        h.assertTrue(l.getBlockEntity(g) instanceof ChestBlockEntity c && c.getItem(0).is(Items.DIAMOND_SWORD) && c.getItem(1).getCount() == 5, "items kept");
        l.setBlock(g, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void obstructedAnchorMovesToNearestSafeSpot(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = pad(h, 30);
        l.setBlock(o, Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(o.above(), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(o.east(), Blocks.WATER.defaultBlockState(), 3);
        BlockPos g = SafeGraves.place(l.getServer(), UUID.randomUUID(), "T", at(l, o), List.of(), loot());
        h.assertTrue(g != null && !g.equals(o) && !g.equals(o.east()) && g.distManhattan(o) <= 3, "nearby safe spot: " + g);
        h.assertTrue(l.getBlockState(g).is(Blocks.CHEST), "chest placed");
        l.setBlock(g, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void voidOrMissingDimensionGoesToRecoveryChest(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        UUID a = UUID.randomUUID();
        BlockPos sky = new BlockPos(h.absolutePos(BlockPos.ZERO).getX() + 4000, l.getMaxBuildHeight() - 20, 0);
        h.assertTrue(SafeGraves.place(l.getServer(), a, "A", at(l, sky), List.of(), loot()) == null, "no floor in the sky");
        UUID b = UUID.randomUUID();
        var gone = new SafeGraves.Anchor(ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("khaosnexus", "removed_realm")), BlockPos.ZERO);
        h.assertTrue(SafeGraves.place(l.getServer(), b, "B", gone, List.of(), loot()) == null, "missing dimension");
        h.assertTrue(SafeGraves.place(l.getServer(), b, "B", null, List.of(), loot()) == null, "no anchor at all");
        var data = SafeGraves.RecoveryData.get(l.getServer());
        h.assertTrue(data.count(a) == 2 && data.count(b) == 4, "items stored, never deleted");
        h.assertTrue(data.take(b).size() == 4 && data.count(b) == 0, "claim empties it");
        data.take(a);
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void partyMembersGetSeparateLockedGraves(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = pad(h, 60);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        BlockPos ga = SafeGraves.place(l.getServer(), a, "A", at(l, o), List.of(a, b), loot());
        BlockPos gb = SafeGraves.place(l.getServer(), b, "B", at(l, o), List.of(a, b), loot());
        h.assertTrue(ga != null && gb != null && !ga.equals(gb), "two graves, offset at the same anchor");
        h.assertTrue(l.getBlockEntity(gb) instanceof ChestBlockEntity c && c.getItem(0).is(Items.DIAMOND_SWORD), "second grave full");
        var stranger = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertFalse(SafeGraves.mayOpen(stranger, (ChestBlockEntity) l.getBlockEntity(ga)), "strangers locked out during the party window");
        h.assertTrue(SafeGraves.mayOpen(stranger, new ChestBlockEntity(o, Blocks.CHEST.defaultBlockState())), "normal chests unaffected");
        l.setBlock(ga, Blocks.AIR.defaultBlockState(), 3);
        l.setBlock(gb, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void overflowSpillsIntoMoreGraves(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = pad(h, 90);
        List<ItemStack> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) many.add(new ItemStack(Items.IRON_INGOT, 64));
        BlockPos g = SafeGraves.place(l.getServer(), UUID.randomUUID(), "T", at(l, o), List.of(), many);
        int chests = 0, stacks = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-3, 0, -3), o.offset(3, 2, 3))) {
            if (l.getBlockEntity(p) instanceof ChestBlockEntity c) {
                chests++;
                for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) stacks++;
                c.clearContent();
                l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        h.assertTrue(g != null && chests == 2 && stacks == 40, "40 stacks over 2 graves, got " + chests + "/" + stacks);
        h.succeed();
    }
}

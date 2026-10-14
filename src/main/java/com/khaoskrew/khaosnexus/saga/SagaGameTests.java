package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.endgame.Prestige;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class SagaGameTests {
    static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, p); }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void underthroneAndSagaDataLoad(GameTestHelper h) {
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE).containsKey(id("underthrone")), "underthrone dimension type (the test server only loads the overworld)");
        h.assertTrue(JsonPool.OATHS.all().size() >= 4, "4 act I oaths");
        h.assertTrue(JsonPool.SEALS.get(id("hollow_crown")) != null, "hollow crown seal");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void oathsNeedParagonAndCostShards(GameTestHelper h) {
        HunterProfile p = new HunterProfile();
        ResourceLocation o = id("oath_of_the_unbroken");
        h.assertFalse(Saga.swear(p, o), "no oath before Paragon");
        p.endgame.putInt(Prestige.KEY, Prestige.NATIONAL);
        h.assertTrue(Saga.swear(p, o) && !Saga.swear(p, id("oath_of_the_quiet_star")), "one oath at a time");
        h.assertFalse(Saga.unlockNode(p), "nodes cost shards");
        Saga.addShards(p, 3);
        h.assertTrue(Saga.unlockNode(p) && Saga.unlockNode(p) && !Saga.unlockNode(p) && Saga.shards(p) == 1, "1+1 spent, next node costs 2");
        h.assertFalse(Saga.respec(p), "respec costs shards");
        Saga.addShards(p, 100);
        h.assertTrue(Saga.respec(p) && Saga.oath(p).isEmpty(), "respec clears");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void sealsAndAmbushRules(GameTestHelper h) {
        HunterProfile p = new HunterProfile();
        h.assertFalse(Saga.canOpenSeal(p, id("hollow_crown")), "needs Paragon");
        p.endgame.putInt(Prestige.KEY, Prestige.NATIONAL);
        h.assertTrue(Saga.canOpenSeal(p, id("hollow_crown")), "first seal open at Paragon");
        Saga.breakSeal(p, id("hollow_crown"));
        h.assertTrue(Saga.sealBroken(p, id("hollow_crown")), "seal state saved");
        RandomSource r = RandomSource.create(7);
        int hits = 0;
        for (int i = 0; i < 4000; i++) {
            h.assertFalse(Saga.rollAmbush(GateRank.B, true, r), "no ambush below A");
            h.assertFalse(Saga.rollAmbush(GateRank.S, false, r), "no ambush in special gates");
            if (Saga.rollAmbush(GateRank.A, true, r)) hits++;
        }
        h.assertTrue(hits > 120 && hits < 280, "about 5%: " + hits);
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 400)
    public static void exileCampBuildsWithSealGateExitAndVendors(GameTestHelper h) {
        var l = h.getLevel();
        net.minecraft.core.BlockPos o = h.absolutePos(new net.minecraft.core.BlockPos(0, 120, 0)).offset(300, 0, 0);
        ExileCamp.build(l, o);
        h.assertTrue(ExileCamp.built(l, o), "seal gate in the arch");
        h.assertTrue(l.getBlockState(ExileCamp.exitPos(o)).is(com.khaoskrew.khaosnexus.registry.ModRegistries.SPIRE_GATE.get()), "exit gate");
        h.assertTrue(l.getBlockState(o).is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE), "bonfire");
        var a = ExileCamp.arrival(o);
        h.assertTrue(l.getBlockState(a.below()).isSolid() && l.getBlockState(a).isAir() && l.getBlockState(a.above()).isAir(), "safe arrival");
        var npcs = l.getEntitiesOfClass(net.minecraft.world.entity.npc.WanderingTrader.class, new net.minecraft.world.phys.AABB(o).inflate(20));
        h.assertTrue(npcs.size() >= 1, "vendors");
        npcs.forEach(n -> n.discard());
        h.assertFalse(UnderthroneService.inCamp(l, o), "camp protection only applies in the Underthrone");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void sealGatePicksSealsInOrder(GameTestHelper h) {
        HunterProfile p = new HunterProfile();
        h.assertTrue(id("hollow_crown").equals(SealGateBlock.nextSeal(p)), "first seal");
        Saga.breakSeal(p, id("hollow_crown"));
        h.assertTrue(id("hollow_crown").equals(SealGateBlock.nextSeal(p)), "replay the last seal when all are broken");
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 200)
    public static void sealGuardiansSpawnWithBossStats(GameTestHelper h) {
        h.assertTrue(SagaBosses.guardianFor(id("crystal_cavern")) == com.khaoskrew.khaosnexus.mob.ModEntities.VAELITH.get(), "crystal -> Vaelith");
        h.assertTrue(SagaBosses.guardianFor(id("corrupted_cathedral")) == com.khaoskrew.khaosnexus.mob.ModEntities.FALLEN_HIEROPHANT.get(), "cathedral -> Hierophant");
        var l = h.getLevel();
        var at = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        for (var type : java.util.List.of(com.khaoskrew.khaosnexus.mob.ModEntities.VAELITH.get(), com.khaoskrew.khaosnexus.mob.ModEntities.FALLEN_HIEROPHANT.get())) {
            var m = com.khaoskrew.khaosnexus.gate.GateArena.spawnMob(l, GateRank.C, type, at, 0, 1.0D, true);
            h.assertTrue(m instanceof com.khaoskrew.khaosnexus.mob.RiftBoss, "is a rift boss");
            var warden = com.khaoskrew.khaosnexus.gate.GateArena.spawnMob(l, GateRank.C, com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_WARDEN.get(), at, 0, 1.0D, true);
            h.assertTrue(m.getMaxHealth() > 200 && m.getMaxHealth() < warden.getMaxHealth(), "a real fight but under the Warden: " + m.getMaxHealth() + " vs " + warden.getMaxHealth());
            m.discard(); warden.discard();
        }
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void sealGuardianLootTablesDropTrophies(GameTestHelper h) {
        var l = h.getLevel();
        for (String[] e : new String[][] {{"vaelith", "vaelith_choir_mask"}, {"fallen_hierophant", "hierophant_censer"}}) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id("entities/" + e[0]));
            var table = l.getServer().reloadableRegistries().getLootTable(key);
            h.assertTrue(table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "loot table " + e[0]);
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var mob = (e[0].equals("vaelith") ? com.khaoskrew.khaosnexus.mob.ModEntities.VAELITH : com.khaoskrew.khaosnexus.mob.ModEntities.FALLEN_HIEROPHANT).get().create(l);
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(l)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, mob)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, mob.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, l.damageSources().playerAttack(player))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var drops = table.getRandomItems(params);
            h.assertTrue(drops.stream().anyMatch(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals(e[1])), "trophy drops: " + drops);
            mob.discard();
        }
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 200)
    public static void crownlessGeneralsCopyClassAndSpawnTagged(GameTestHelper h) {
        var E = com.khaoskrew.khaosnexus.mob.ModEntities.class;
        h.assertTrue(CrownlessGenerals.forClass(com.khaoskrew.khaosnexus.hunter.HunterClass.BERSERKER) == com.khaoskrew.khaosnexus.mob.ModEntities.GENERAL_BLADE.get(), "melee -> Blade");
        h.assertTrue(CrownlessGenerals.forClass(com.khaoskrew.khaosnexus.hunter.HunterClass.CASTER) == com.khaoskrew.khaosnexus.mob.ModEntities.GENERAL_ARCANIST.get(), "caster -> Arcanist");
        h.assertTrue(CrownlessGenerals.forClass(com.khaoskrew.khaosnexus.hunter.HunterClass.RANGER) == com.khaoskrew.khaosnexus.mob.ModEntities.GENERAL_ARCANIST.get(), "ranged -> Arcanist");
        var at = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        for (var type : java.util.List.of(com.khaoskrew.khaosnexus.mob.ModEntities.GENERAL_BLADE.get(), com.khaoskrew.khaosnexus.mob.ModEntities.GENERAL_ARCANIST.get())) {
            var m = CrownlessGenerals.spawn(h.getLevel(), GateRank.A, type, at, 0);
            h.assertTrue(m instanceof com.khaoskrew.khaosnexus.mob.RiftBoss rb && rb.isSagaBoss(), "general is a saga boss");
            h.assertTrue(m.getTags().contains(CrownlessGenerals.TAG), "tagged for shard drops");
            h.assertTrue(m.getCustomName().getString().contains(type.getDescription().getString()), "boss bar shows the general");
            m.discard();
        }
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform", timeoutTicks = 100)
    public static void oathNodesApplyAttributes(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var p = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(player);
        p.endgame.putInt(Prestige.KEY, Prestige.NATIONAL);
        double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        h.assertTrue(Saga.swear(p, id("oath_of_the_unbroken")), "swear");
        Saga.addShards(p, 1);
        h.assertTrue(Saga.unlockNode(p), "node 1");
        h.assertTrue(OathEffects.nodes(player, OathEffects.UNBROKEN) == 1, "node count");
        OathEffects.refresh(player);
        h.runAfterDelay(2, () -> {
            double now = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            h.assertTrue(Math.abs(now - base * 1.10) < 0.01, "+10% max health: " + base + " -> " + now);
            Saga.addShards(p, 100);
            Saga.respec(p);
            OathEffects.refresh(player);
            h.runAfterDelay(2, () -> {
                h.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) - base) < 0.01, "removed after respec");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void sagaArmorSetsCountAndCraft(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) player.setItemSlot(slots[i], new net.minecraft.world.item.ItemStack(com.khaoskrew.khaosnexus.saga.armor.SagaArmor.VESTMENT_SET[i].get()));
        h.assertTrue(com.khaoskrew.khaosnexus.saga.armor.SagaArmor.worn(player, "vestment") == 4, "full vestment set");
        h.assertTrue(com.khaoskrew.khaosnexus.saga.armor.SagaArmor.worn(player, "choir") == 0, "no choir pieces");
        int def = 0; for (var a : com.khaoskrew.khaosnexus.saga.armor.SagaArmor.VESTMENT_SET) def += ((net.minecraft.world.item.ArmorItem) a.get()).getDefense();
        h.assertTrue(def == 21, "set defense " + def);
        var rm = h.getLevel().getServer().getRecipeManager();
        for (String r : new String[] {"choir_helmet", "choir_boots", "vestment_chestplate", "vestment_boots", "wisp_bell"}) {
            h.assertTrue(rm.byKey(id(r)).isPresent(), "recipe " + r);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void wispWithoutOwnerFades(GameTestHelper h) {
        var w = com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_WISP.get().create(h.getLevel());
        w.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 3, 1.5)));
        h.getLevel().addFreshEntity(w);
        h.assertTrue(w.isInvulnerable() && !w.isPushable(), "wisp cannot be hurt or pushed");
        h.runAfterDelay(3, () -> { h.assertTrue(w.isRemoved(), "ownerless wisp is removed"); h.succeed(); });
    }
}

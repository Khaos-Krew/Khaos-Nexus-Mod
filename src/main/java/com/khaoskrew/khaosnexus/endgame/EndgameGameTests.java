package com.khaoskrew.khaosnexus.endgame;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class EndgameGameTests {
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void weeklyModifiersRollAndApply(GameTestHelper helper) {
        helper.assertTrue(WeeklyModifiers.all().size() >= 6, "bundled weekly modifiers load");
        long w = WeeklyModifiers.week(1_760_000_000_000L);
        helper.assertTrue(WeeklyModifiers.week(1_760_000_000_000L + 3600_000L) == w, "same week within an hour");
        List<? extends String> force = Config.WEEKLY_FORCE.get();
        String ranks = Config.WEEKLY_RANKS.get();
        try {
            Config.WEEKLY_FORCE.set(List.of());
            var a = WeeklyModifiers.active(w);
            helper.assertTrue(a.equals(WeeklyModifiers.active(w)), "the roll is deterministic per week");
            helper.assertTrue(a.size() == Config.WEEKLY_COUNT.get() && a.stream().distinct().count() == a.size(), "count distinct picks");
            Config.WEEKLY_FORCE.set(List.of("khaosnexus:ironhide"));
            Config.WEEKLY_RANKS.set("S");
            helper.assertTrue(WeeklyModifiers.active().size() == 1, "forced modifier list");
            helper.assertTrue(WeeklyModifiers.applies(GateRank.S) && !WeeklyModifiers.applies(GateRank.A), "rank filter");
            Mob plain = GateArena.spawnMob(helper.getLevel(), GateRank.A, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(1, 2, 1)), 0F, 1.0D);
            Mob boosted = GateArena.spawnMob(helper.getLevel(), GateRank.S, EntityType.ZOMBIE, helper.absolutePos(new BlockPos(3, 2, 3)), 0F, 1.0D);
            double ratio = boosted.getMaxHealth() / plain.getMaxHealth();
            double expected = GateRank.S.tuning().healthMultiplier.get() / GateRank.A.tuning().healthMultiplier.get() * 1.6D;
            helper.assertTrue(Math.abs(ratio - expected) < 0.01D, "Ironhide adds 60% health only in S gates");
            helper.assertTrue(WeeklyModifiers.scaleXp(GateRank.S, 100) == 120 && WeeklyModifiers.scaleXp(GateRank.A, 100) == 100, "xp bonus by rank");
            helper.assertTrue(WeeklyModifiers.bonusCores(GateRank.S) == 1, "bonus rift cores");
            plain.discard();
            boosted.discard();
            Config.WEEKLY_FORCE.set(List.of("khaosnexus:missing"));
            helper.assertTrue(WeeklyModifiers.active().isEmpty(), "unknown forced ids are ignored");
        } finally {
            Config.WEEKLY_FORCE.set(force);
            Config.WEEKLY_RANKS.set(ranks);
        }
        helper.assertTrue(WeeklyModifiers.parse(ResourceLocation.parse("t:x"), new com.google.gson.JsonObject()).mobHealth() == 1.0D, "missing fields default to 1");
        helper.succeed();
    }
}

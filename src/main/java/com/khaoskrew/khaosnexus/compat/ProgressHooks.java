package com.khaoskrew.khaosnexus.compat;

import java.util.Locale;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Progress events for quest/progression mods. Always fires the advancement trigger
 * {@code khaosnexus:hunter_event}. When FTB Quests is installed and its compat toggle is on,
 * also grants stages as vanilla player tags (FTB Library's default stage backend), so FTB Quests
 * "Stage" tasks work with no code dependency. No quest chapters are shipped.
 */
public final class ProgressHooks {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, KhaosNexus.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, HunterEventTrigger> HUNTER_EVENT =
            TRIGGERS.register("hunter_event", HunterEventTrigger::new);

    public static final String PREFIX = "khaosnexus.";

    private ProgressHooks() {}

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    public static void gateCleared(ServerPlayer p, String rank) {
        fire(p, "gate_cleared", rank, 0, "gate_cleared", "gate_cleared." + rank);
    }

    public static void rankUp(ServerPlayer p, String rank, int tier) {
        fire(p, "rank_up", rank, tier, "rank." + rank);
    }

    public static void classSelected(ServerPlayer p, String classId) {
        if (stagesActive()) {
            for (HunterClassId c : HunterClassId.values()) {
                removeStage(p, PREFIX + "class." + c.id);
            }
        }
        fire(p, "class_selected", classId, 0, "class." + classId, "class_chosen");
    }

    public static void bountyCompleted(ServerPlayer p, String bounty) {
        fire(p, "bounty_completed", bounty, 0, "bounty", "bounty." + bounty);
    }

    public static void raidBossKilled(ServerPlayer p, String bossId) {
        fire(p, "raid_boss_killed", bossId, 0, "raid_boss", "raid_boss." + bossId.replace(':', '.'));
    }

    public static void spireFloor(ServerPlayer p, int floor) {
        fire(p, "spire_floor", Integer.toString(floor), floor, floor % 5 == 0 ? "spire." + floor : null);
    }

    public static void titleEarned(ServerPlayer p, String title) {
        fire(p, "title_earned", title, 0, "title." + title);
    }

    private static boolean stagesActive() {
        return SoftCompat.FTB_QUESTS.active();
    }

    private static void fire(ServerPlayer p, String event, String value, int number, String... stages) {
        String v = value.toLowerCase(Locale.ROOT);
        HUNTER_EVENT.get().trigger(p, event, v, number);
        if (!stagesActive()) {
            return;
        }
        for (String s : stages) {
            if (s != null) {
                addStage(p, PREFIX + s.toLowerCase(Locale.ROOT));
            }
        }
    }

    private enum HunterClassId {
        TANK, ASSASSIN, CASTER, RANGER, LIFEBINDER, BERSERKER, SUMMONER, NECROMANCER;
        final String id = name().toLowerCase(Locale.ROOT);
    }

    /**
     * Stages go through FTB Library's StageHelper so whichever provider the pack uses (player tags by default, or
     * KubeJS / Game Stages when installed) sees them. Falls back to vanilla player tags.
     */
    private static Object provider() {
        try {
            Class<?> helper = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageHelper");
            Object instance = helper.getField("INSTANCE").get(null);
            return helper.getMethod("getProvider").invoke(instance);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    public static void addStage(ServerPlayer p, String stage) {
        if (!call(provider(), "add", p, stage)) {
            p.addTag(stage);
        }
    }

    public static void removeStage(ServerPlayer p, String stage) {
        if (!call(provider(), "remove", p, stage)) {
            p.removeTag(stage);
        }
    }

    public static boolean hasStage(ServerPlayer p, String stage) {
        Object provider = provider();
        if (provider != null) {
            try {
                return (Boolean) provider.getClass().getMethod("has", net.minecraft.world.entity.player.Player.class, String.class)
                        .invoke(provider, p, stage);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // fall through
            }
        }
        return p.getTags().contains(stage);
    }

    private static boolean call(Object provider, String method, ServerPlayer p, String stage) {
        if (provider == null) {
            return false;
        }
        try {
            provider.getClass().getMethod(method, ServerPlayer.class, String.class).invoke(provider, p, stage);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            KhaosNexus.LOGGER.debug("FTB stage provider call failed: {}", e.toString());
            return false;
        }
    }
}

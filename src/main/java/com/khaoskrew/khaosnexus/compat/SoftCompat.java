package com.khaoskrew.khaosnexus.compat;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Presence checks for companion mods. Nothing here calls those mods' APIs.
 */
public final class SoftCompat {
    public record Hook(String modId, String label, ModConfigSpec.BooleanValue toggle) {
        public boolean installed() {
            return ModList.get().isLoaded(this.modId);
        }

        public boolean active() {
            return this.toggle.get() && this.installed();
        }

        public Component status() {
            if (!this.toggle.get()) {
                return Component.translatable("khaosnexus.compat.disabled");
            }
            if (!this.installed()) {
                return Component.translatable("khaosnexus.compat.absent");
            }
            return Component.translatable("khaosnexus.compat.present");
        }
    }

    public static final Hook ARS = new Hook("ars_nouveau", "Ars Nouveau", Config.COMPAT_ARS);
    public static final Hook APOTHEOSIS = new Hook("apotheosis", "Apotheosis", Config.COMPAT_APOTHEOSIS);
    public static final Hook FTB_TEAMS = new Hook("ftbteams", "FTB Teams", Config.COMPAT_FTB_TEAMS);
    public static final Hook FTB_CHUNKS = new Hook("ftbchunks", "FTB Chunks", Config.COMPAT_FTB_CHUNKS);
    public static final Hook FTB_QUESTS = new Hook("ftbquests", "FTB Quests", Config.COMPAT_FTB_QUESTS);
    public static final Hook CURIOS = new Hook("curios", "Curios", Config.COMPAT_CURIOS);

    public static final Hook[] HOOKS = {ARS, APOTHEOSIS, FTB_TEAMS, FTB_CHUNKS, FTB_QUESTS, CURIOS};

    private SoftCompat() {}

    public static void logPresence() {
        for (Hook hook : HOOKS) {
            KhaosNexus.LOGGER.info("Optional companion {}: {}", hook.modId(), hook.installed() ? "present" : "absent");
        }
        KhaosNexus.LOGGER.info("Companion hooks only detect mods. FTB Quests chapters are not included.");
    }
}

package com.khaoskrew.khaosnexus.compat;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Registers FTB Quests task/reward types when FTB Quests is installed; the ftb package is never touched otherwise. */
@EventBusSubscriber(modid = KhaosNexus.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FtbQuestsCompat {
    private FtbQuestsCompat() {}

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        if (!ModList.get().isLoaded("ftbquests")) return;
        e.enqueueWork(() -> {
            try {
                com.khaoskrew.khaosnexus.compat.ftb.KhaosTasks.register();
                KhaosNexus.LOGGER.info("FTB Quests: registered khaosnexus task/reward types");
            } catch (RuntimeException | LinkageError ex) {
                KhaosNexus.LOGGER.warn("FTB Quests types skipped: {}", ex.toString());
            }
        });
    }
}

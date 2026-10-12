package com.khaoskrew.khaosnexus;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(KhaosNexus.MODID)
public class KhaosNexus {
    public static final String MODID = "khaosnexus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KhaosNexus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ModRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Khaos Nexus {} starting", MODID);
    }
}

package com.khaoskrew.khaosnexus;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;
import com.khaoskrew.khaosnexus.network.ModPayloads;
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
        com.khaoskrew.khaosnexus.mob.ModEntities.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Khaos Nexus {} starting", MODID);
        SoftCompat.logPresence();
    }
}

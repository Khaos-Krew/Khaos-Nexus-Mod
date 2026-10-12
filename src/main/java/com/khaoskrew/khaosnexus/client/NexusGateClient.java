package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = KhaosNexus.MODID, dist = Dist.CLIENT)
public final class NexusGateClient {
    public NexusGateClient(IEventBus modEventBus) {
        modEventBus.addListener(NexusGateClient::registerRenderers);
        modEventBus.addListener(NexusGateClient::registerKeys);
        modEventBus.addListener(NexusGateClient::registerHud);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistries.NEXUS_GATE_ENTITY.get(), NexusGateRenderer::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClassKeybinds.PRIMARY);
        event.register(ClassKeybinds.SECONDARY);
        event.register(ClassKeybinds.CLASS_MENU);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "hud"),
                HunterHud::render);
    }
}

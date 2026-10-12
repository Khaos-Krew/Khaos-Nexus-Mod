package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = KhaosNexus.MODID, dist = Dist.CLIENT)
public final class NexusGateClient {
    public NexusGateClient(IEventBus modEventBus) {
        modEventBus.addListener(NexusGateClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistries.NEXUS_GATE_ENTITY.get(), NexusGateRenderer::new);
    }
}

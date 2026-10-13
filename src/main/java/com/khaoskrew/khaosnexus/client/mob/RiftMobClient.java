package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.mob.ModEntities;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RiftMobClient {
    private RiftMobClient() {}

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // GeckoLib models are data-driven (geo/*.geo.json); there are no vanilla layer definitions.
    }

    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VOID_ORB.get(), c -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(c, 1.6F, true));
        event.registerEntityRenderer(ModEntities.RIFT_GHOUL.get(), c -> new RiftMobRenderer(c, "rift_ghoul", 0.5F));
        event.registerEntityRenderer(ModEntities.RIFT_STALKER.get(), c -> new RiftMobRenderer(c, "rift_stalker", 0.5F));
        event.registerEntityRenderer(ModEntities.VOID_CASTER.get(), c -> new RiftMobRenderer(c, "void_caster", 0.55F));
        event.registerEntityRenderer(ModEntities.RIFT_BRUTE.get(), c -> new RiftMobRenderer(c, "rift_brute", 1.0F));
        event.registerEntityRenderer(ModEntities.RIFT_WARDEN.get(), c -> new RiftMobRenderer(c, "rift_warden", 1.6F));
        event.registerEntityRenderer(ModEntities.SPIRE_GUARDIAN.get(), c -> new RiftMobRenderer(c, "spire_guardian", 1.3F));
    }
}

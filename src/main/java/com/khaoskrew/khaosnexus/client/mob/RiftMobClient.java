package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.mob.ModEntities;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RiftMobClient {
    public static final ModelLayerLocation GHOUL = layer("rift_ghoul");
    public static final ModelLayerLocation STALKER = layer("rift_stalker");
    public static final ModelLayerLocation CASTER = layer("void_caster");
    public static final ModelLayerLocation BRUTE = layer("rift_brute");

    private RiftMobClient() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, name), "main");
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GHOUL, RiftMobLayers::createRiftGhoul);
        event.registerLayerDefinition(STALKER, RiftMobLayers::createRiftStalker);
        event.registerLayerDefinition(CASTER, RiftMobLayers::createVoidCaster);
        event.registerLayerDefinition(BRUTE, RiftMobLayers::createRiftBrute);
    }

    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RIFT_GHOUL.get(), c -> new RiftMobRenderer(c, GHOUL, "rift_ghoul", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.RIFT_STALKER.get(), c -> new RiftMobRenderer(c, STALKER, "rift_stalker", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.VOID_CASTER.get(), c -> new RiftMobRenderer(c, CASTER, "void_caster", 0.55F, 1.0F));
        // The brute's spec geometry is already ~1.5x humanoid (about 3 blocks), so no extra render scale.
        event.registerEntityRenderer(ModEntities.RIFT_BRUTE.get(), c -> new RiftMobRenderer(c, BRUTE, "rift_brute", 1.0F, 1.0F));
    }
}

package com.khaoskrew.khaosnexus.client.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/** Underthrone sky: no sun/clouds, no thick nether fog; a dusky violet void that keeps the camp readable at range. */
public final class UnderthroneSky extends DimensionSpecialEffects {
    public UnderthroneSky() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "underthrone"), new UnderthroneSky());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        return fog.multiply(0.9F + 0.1F * brightness, 0.9F + 0.1F * brightness, 0.95F + 0.05F * brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}

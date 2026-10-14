package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.saga.armor.SagaArmor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** GeckoLib armor renderer for the Hollow Crown sets (translucent so the Choir glass keeps its alpha, plus a glow pass). */
public final class SagaArmorRenderer {
    private SagaArmorRenderer() {}

    public static HumanoidModel<?> create(String set) {
        GeoArmorRenderer<SagaArmor.Piece> r = new GeoArmorRenderer<>(new DefaultedItemGeoModel<SagaArmor.Piece>(
                ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "armor/" + set + "_armor"))) {
            @Override
            public RenderType getRenderType(SagaArmor.Piece a, ResourceLocation tex, MultiBufferSource buf, float pt) {
                return RenderType.entityTranslucent(tex);
            }
        };
        r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
        return r;
    }
}

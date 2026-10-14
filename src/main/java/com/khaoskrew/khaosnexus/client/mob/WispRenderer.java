package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.saga.RiftWisp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Rift Wisp: translucent glass shell plus a full-bright glow pass. */
public class WispRenderer extends GeoEntityRenderer<RiftWisp> {
    public WispRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "rift_wisp"), false));
        this.shadowRadius = 0.15F;
        ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/rift_wisp_glow.png");
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, RiftWisp mob, BakedGeoModel model, RenderType type, MultiBufferSource buffer,
                    VertexConsumer consumer, float partialTick, int light, int overlay) {
                RenderType eyes = RenderType.eyes(glow);
                this.getRenderer().reRender(model, pose, buffer, mob, eyes, buffer.getBuffer(eyes), partialTick, 0xF00000, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        });
    }

    @Override
    public RenderType getRenderType(RiftWisp mob, ResourceLocation texture, MultiBufferSource buffer, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected int getBlockLightLevel(RiftWisp e, net.minecraft.core.BlockPos p) {
        return 15;
    }
}

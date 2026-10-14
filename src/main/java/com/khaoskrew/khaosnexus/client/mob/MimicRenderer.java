package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.dungeon.GateMimic;
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

/** Gate Mimic: plain chest while dormant; amber eye, latch and leg joints glow once awake. */
public class MimicRenderer extends GeoEntityRenderer<GateMimic> {
    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "gate_mimic"), false));
        this.shadowRadius = 0.5F;
        ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/gate_mimic_glow.png");
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, GateMimic mob, BakedGeoModel model, RenderType type, MultiBufferSource buffer,
                    VertexConsumer consumer, float partialTick, int light, int overlay) {
                if (!mob.isAwake() && mob.getDebugPose().isEmpty()) return;   // a dormant mimic must read as a plain chest
                RenderType eyes = RenderType.eyes(glow);
                this.getRenderer().reRender(model, pose, buffer, mob, eyes, buffer.getBuffer(eyes), partialTick, 0xF00000, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        });
    }
}

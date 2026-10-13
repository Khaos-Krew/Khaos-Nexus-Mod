package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.mob.MinionMob;
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

/** GeckoLib renderer for class minions, with an emissive soul-glow pass. */
public class MinionRenderer extends GeoEntityRenderer<MinionMob> {
    public MinionRenderer(EntityRendererProvider.Context context, String name, float shadow) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, name), true));
        this.shadowRadius = shadow;
        ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/" + name + "_glow.png");
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MinionMob mob, BakedGeoModel model, RenderType type, MultiBufferSource buffer,
                    VertexConsumer consumer, float partialTick, int light, int overlay) {
                RenderType eyes = RenderType.eyes(glow);
                this.getRenderer().reRender(model, pose, buffer, mob, eyes, buffer.getBuffer(eyes), partialTick, 0xF00000,
                        OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        });
    }

    @Override
    public RenderType getRenderType(MinionMob mob, ResourceLocation texture, MultiBufferSource buffer, float partialTick) {
        return RenderType.entityTranslucent(texture);   // familiar's wisp tail fades out
    }
}

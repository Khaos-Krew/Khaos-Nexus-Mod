package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.mob.RiftMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RiftMobRenderer extends MobRenderer<RiftMob, RiftMobModel> {
    private final ResourceLocation texture;
    private final float scale;

    public RiftMobRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String name, float shadow, float scale) {
        super(context, new RiftMobModel(context.bakeLayer(layer), name.equals("void_caster")), shadow);
        this.texture = tex(name, "");
        this.scale = scale;
        this.addLayer(new Glow(this, tex(name, "_glow"), tex(name, "_glow_rank")));
    }

    private static ResourceLocation tex(String name, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/" + name + suffix + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(RiftMob mob) {
        return this.texture;
    }

    @Override
    protected void scale(RiftMob mob, PoseStack pose, float partialTick) {
        pose.scale(this.scale, this.scale, this.scale);
    }

    /** Emissive eyes, cracks and runes. Gate mobs glow in their gate's rank colour. */
    private static final class Glow extends RenderLayer<RiftMob, RiftMobModel> {
        private final ResourceLocation colored;
        private final ResourceLocation rankMask;

        Glow(RenderLayerParent<RiftMob, RiftMobModel> parent, ResourceLocation colored, ResourceLocation rankMask) {
            super(parent);
            this.colored = colored;
            this.rankMask = rankMask;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, RiftMob mob, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            GateRank rank = mob.getGateRank();
            ResourceLocation tex = rank == null ? this.colored : this.rankMask;
            int color = rank == null ? 0xFFFFFFFF : (0xFF000000 | rank.color());
            VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(tex));
            this.getParentModel().renderToBuffer(pose, consumer, 0xF00000, OverlayTexture.NO_OVERLAY, color);
        }
    }
}

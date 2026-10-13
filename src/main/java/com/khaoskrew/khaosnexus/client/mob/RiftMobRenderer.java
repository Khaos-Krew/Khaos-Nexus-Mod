package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.mob.RiftMob;
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
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib renderer for every Rift mob: geo/entity/NAME.geo.json, animations/entity/NAME.animation.json. */
public class RiftMobRenderer extends GeoEntityRenderer<RiftMob> {
    public RiftMobRenderer(EntityRendererProvider.Context context, String name, float shadow) {
        super(context, new DefaultedEntityGeoModel<RiftMob>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, name), true));
        this.shadowRadius = shadow;
        this.addRenderLayer(new Glow(this, tex(name, "_glow"), tex(name, "_glow_rank")));
    }

    private static ResourceLocation tex(String name, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/" + name + suffix + ".png");
    }

    /** Warden: greatsword in hand while drawn, on the back (in its scabbard) while sheathed. */
    @Override
    public void preRender(PoseStack pose, RiftMob mob, BakedGeoModel model, MultiBufferSource buffer, VertexConsumer consumer,
            boolean reRender, float partialTick, int light, int overlay, int color) {
        if (mob instanceof com.khaoskrew.khaosnexus.mob.RiftBoss boss) {
            boolean drawn = boss.isWeaponDrawn();
            model.getBone("sword_back").ifPresent(b -> {
                b.setHidden(drawn);
                model.getBone("sword").ifPresent(h -> h.setHidden(!drawn));
            });
        }
        super.preRender(pose, mob, model, buffer, consumer, reRender, partialTick, light, overlay, color);
    }

    @Override
    public RenderType getRenderType(RiftMob mob, ResourceLocation texture, MultiBufferSource buffer, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    /** Emissive eyes, veins and runes. Gate mobs glow in their gate's rank colour. */
    private static final class Glow extends GeoRenderLayer<RiftMob> {
        private final ResourceLocation colored;
        private final ResourceLocation rankMask;

        Glow(GeoRenderer<RiftMob> renderer, ResourceLocation colored, ResourceLocation rankMask) {
            super(renderer);
            this.colored = colored;
            this.rankMask = rankMask;
        }

        @Override
        public void render(PoseStack pose, RiftMob mob, BakedGeoModel model, RenderType renderType, MultiBufferSource buffer,
                VertexConsumer consumer, float partialTick, int light, int overlay) {
            GateRank rank = mob.getGateRank();
            RenderType glow = RenderType.eyes(rank == null ? this.colored : this.rankMask);
            int color = rank == null ? 0xFFFFFFFF : (0xFF000000 | rank.color());
            this.getRenderer().reRender(model, pose, buffer, mob, glow, buffer.getBuffer(glow), partialTick,
                    0xF00000, OverlayTexture.NO_OVERLAY, color);
        }
    }
}

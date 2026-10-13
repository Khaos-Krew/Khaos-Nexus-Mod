package com.khaoskrew.khaosnexus.client;

import org.joml.Matrix4f;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/** The Nexus Gate as a floating anime rift: layered, rank-tinted energy planes plus a white-hot core pass. */
public class NexusRiftRenderer extends GeoBlockRenderer<NexusGateBlockEntity> {
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/block/nexus_rift_core.png");

    public NexusRiftRenderer() {
        super(new DefaultedBlockGeoModel<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "nexus_rift")));
        this.addRenderLayer(new Core(this));
    }

    @Override
    public RenderType getRenderType(NexusGateBlockEntity gate, ResourceLocation texture, MultiBufferSource buffer, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public Color getRenderColor(NexusGateBlockEntity gate, float partialTick, int packedLight) {
        return Color.ofOpaque(gate.isClosed() ? 0x6A6A6A : gate.getRank().color());
    }

    @Override
    public void render(NexusGateBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        super.render(gate, partialTick, pose, buffer, LightTexture.FULL_BRIGHT, overlay);
        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable(gate.getRank().translationKey());
        pose.pushPose();
        pose.translate(0.5D, gate.isClosed() ? 2.0D : 6.3D, 0.5D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.05F, -0.05F, 0.05F);
        Matrix4f matrix = pose.last().pose();
        int color = gate.isClosed() ? 0xFF808080 : (0xFF000000 | gate.getRank().color());
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, true, matrix, buffer, Font.DisplayMode.NORMAL, 0x66000000, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(NexusGateBlockEntity gate) {
        var p = gate.getBlockPos();
        return new AABB(p.getX() - 2, p.getY(), p.getZ() - 1, p.getX() + 3, p.getY() + 7, p.getZ() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen(NexusGateBlockEntity gate) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    /** Additive white-hot rim and swirl highlights, untinted so the tear stays bright at its edges. */
    private static final class Core extends GeoRenderLayer<NexusGateBlockEntity> {
        Core(GeoRenderer<NexusGateBlockEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, NexusGateBlockEntity gate, BakedGeoModel model, RenderType renderType, MultiBufferSource buffer,
                VertexConsumer consumer, float partialTick, int light, int overlay) {
            if (gate.isClosed()) {
                return;
            }
            RenderType core = RenderType.eyes(CORE);
            this.getRenderer().reRender(model, pose, buffer, gate, core, buffer.getBuffer(core), partialTick,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xCCFFFFFF);
        }
    }
}

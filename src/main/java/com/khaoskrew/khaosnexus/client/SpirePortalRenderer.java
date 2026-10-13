package com.khaoskrew.khaosnexus.client;

import org.joml.Matrix4f;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.spire.SpirePortalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.Color;

/** Spire level gate: the rift model, rank-tinted and pulsing when lit, a dim rank-tinted frame with faint particles while the boss lives. */
public class SpirePortalRenderer extends GeoBlockRenderer<SpirePortalBlockEntity> {
    public SpirePortalRenderer() {
        super(new DefaultedBlockGeoModel<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "nexus_rift")));
    }

    @Override
    public RenderType getRenderType(SpirePortalBlockEntity gate, ResourceLocation texture, MultiBufferSource buffer, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public Color getRenderColor(SpirePortalBlockEntity gate, float partialTick, int packedLight) {
        return Color.ofOpaque(gate.isLit() ? gate.getRank().color() : dim(gate.getRank().color()));
    }

    /** Dormant tint: the rank colour at ~45% over a dark base, so the frame reads clearly without looking active. */
    static int dim(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return ((40 + r * 45 / 100) << 16) | ((40 + g * 45 / 100) << 8) | (40 + b * 45 / 100);
    }

    @Override
    public void render(SpirePortalBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        if (gate.isLit() && gate.getLevel() != null) {
            float pulse = 1.0F + 0.04F * (float) Math.sin((gate.getLevel().getGameTime() + partialTick) * 0.15D);
            pose.translate(0.5D, 3.0D, 0.5D);
            pose.scale(pulse, 1.0F, pulse);
            pose.translate(-0.5D, -3.0D, -0.5D);
            if (!Minecraft.getInstance().isPaused() && gate.getLevel().getRandom().nextInt(3) == 0) {
                var p = gate.getBlockPos();
                var r = gate.getLevel().getRandom();
                gate.getLevel().addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        p.getX() + 0.5D + (r.nextDouble() - 0.5D), p.getY() + r.nextDouble() * 5.5D, p.getZ() + 0.5D + (r.nextDouble() - 0.5D), 0, 0.02D, 0);
            }
        }
        if (!gate.isLit() && gate.getLevel() != null && !Minecraft.getInstance().isPaused() && gate.getLevel().getRandom().nextInt(12) == 0) {
            var p = gate.getBlockPos();
            var r = gate.getLevel().getRandom();
            gate.getLevel().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    p.getX() + 0.5D + (r.nextDouble() - 0.5D) * 1.6D, p.getY() + r.nextDouble() * 5.0D, p.getZ() + 0.5D + (r.nextDouble() - 0.5D), 0, -0.05D, 0);
        }
        super.render(gate, partialTick, pose, buffer, gate.isLit() ? LightTexture.FULL_BRIGHT : LightTexture.pack(Math.max(LightTexture.block(light), 11), Math.max(LightTexture.sky(light), 11)), overlay);
        pose.popPose();
        Font font = Minecraft.getInstance().font;
        Component text = gate.targetLevel() <= 0
                ? Component.translatable("khaosnexus.spire.portal_town", gate.targetFloor())
                : Component.translatable("khaosnexus.spire.portal_level", gate.targetFloor(), gate.targetLevel());
        pose.pushPose();
        pose.translate(0.5D, 6.3D, 0.5D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.05F, -0.05F, 0.05F);
        Matrix4f matrix = pose.last().pose();
        int color = 0xFF000000 | (gate.isLit() ? gate.getRank().color() : dim(gate.getRank().color()) | 0x404040);
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, true, matrix, buffer, Font.DisplayMode.NORMAL, 0x66000000, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpirePortalBlockEntity gate) {
        var p = gate.getBlockPos();
        return new AABB(p.getX() - 2, p.getY(), p.getZ() - 1, p.getX() + 3, p.getY() + 7, p.getZ() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen(SpirePortalBlockEntity gate) {
        return true;
    }
}

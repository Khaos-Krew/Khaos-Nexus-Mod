package com.khaoskrew.khaosnexus.client;

import org.joml.Matrix4f;

import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

public class NexusGateRenderer implements BlockEntityRenderer<NexusGateBlockEntity> {
    private final Font font;

    public NexusGateRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(NexusGateBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Component text = Component.translatable(gate.getRank().translationKey());
        pose.pushPose();
        pose.translate(0.5D, 1.25D, 0.5D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.1F, -0.1F, 0.1F);
        Matrix4f matrix = pose.last().pose();
        float x = -this.font.width(text) / 2.0F;
        int color = gate.isClosed() ? 0xFF808080 : gate.getRank().color();
        this.font.drawInBatch(text, x, 0.0F, color, true, matrix, buffer, Font.DisplayMode.NORMAL, 0x66000000, packedLight);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NexusGateBlockEntity gate) {
        return true;
    }
}

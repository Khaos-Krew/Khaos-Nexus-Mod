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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * Renders the 5x7 (3x5 opening, 2 deep) Nexus Gate frame around the gate block plus the
 * animated, rank-tinted rift surface. The gate block itself is the bottom-centre frame stone.
 */
public class NexusGateRenderer implements BlockEntityRenderer<NexusGateBlockEntity> {
    private static final ResourceLocation FRAME = tex("block/nexus_gate_frame");
    private static final ResourceLocation GLOW = tex("block/nexus_gate_frame_glow");
    private static final ResourceLocation PORTAL = tex("block/nexus_gate_portal");
    /** Frame segments in block-local coords: x0,y0,x1,y1 (z is always -0.5..1.5). */
    private static final float[][] FRAME_BOXES = {
            {-2, 0, -1, 7}, {2, 0, 3, 7}, {-1, 6, 2, 7}, {-1, 0, 0, 1}, {0, 0, 1, 1}, {1, 0, 2, 1}};
    private static final float Z0 = -0.5F, Z1 = 1.5F;

    private final Font font;

    public NexusGateRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, path);
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    @Override
    public void render(NexusGateBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean closed = gate.isClosed();
        int rankColor = closed ? 0xFF6A6A6A : gate.getRank().color();

        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite frame = sprite(FRAME);
        for (float[] b : FRAME_BOXES) {
            box(solid, pose.last(), frame, b[0], b[1], Z0, b[2], b[3], Z1, 0xFFFFFFFF, packedLight, 0F);
        }
        TextureAtlasSprite runes = sprite(GLOW);
        VertexConsumer runeBuf = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float pulse = closed ? 0.5F : 0.8F + 0.2F * (float) Math.sin((gate.getLevel() == null ? 0 : gate.getLevel().getGameTime() + partialTick) * 0.12F);
        int glowColor = withAlpha(rankColor, pulse);
        for (float[] b : FRAME_BOXES) {
            box(runeBuf, pose.last(), runes, b[0], b[1], Z0, b[2], b[3], Z1, glowColor, LightTexture.FULL_BRIGHT, 0.01F);
        }

        if (!closed) {
            TextureAtlasSprite rift = sprite(PORTAL);
            // Fetch buffers right before use: requesting another RenderType ends the previous builder.
            VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
            // One animated rift surface stretched over the whole 3x5 opening, both faces.
            int riftColor = withAlpha(rankColor, 0.92F);
            quadZ(glow, pose.last(), rift, -1, 1, 2, 6, 0.5F, riftColor, false);
            quadZ(glow, pose.last(), rift, -1, 1, 2, 6, 0.5F, riftColor, true);
        }

        Component text = Component.translatable(gate.getRank().translationKey());
        pose.pushPose();
        pose.translate(0.5D, 7.5D, 0.5D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.05F, -0.05F, 0.05F);
        Matrix4f matrix = pose.last().pose();
        float tx = -this.font.width(text) / 2.0F;
        this.font.drawInBatch(text, tx, 0.0F, rankColor, true, matrix, buffer, Font.DisplayMode.NORMAL, 0x66000000, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    private static int withAlpha(int rgb, float a) {
        return ((int) (Math.max(0F, Math.min(1F, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static void vtx(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, int color, float u, float v, int light, float nx, float ny, float nz) {
        c.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    private static void quadZ(VertexConsumer c, PoseStack.Pose p, TextureAtlasSprite s, float x0, float y0, float x1, float y1, float z, int color, boolean back) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        int light = LightTexture.FULL_BRIGHT;
        if (!back) {
            vtx(c, p, x0, y0, z, color, u0, v1, light, 0, 0, 1);
            vtx(c, p, x1, y0, z, color, u1, v1, light, 0, 0, 1);
            vtx(c, p, x1, y1, z, color, u1, v0, light, 0, 0, 1);
            vtx(c, p, x0, y1, z, color, u0, v0, light, 0, 0, 1);
        } else {
            vtx(c, p, x1, y0, z, color, u1, v1, light, 0, 0, -1);
            vtx(c, p, x0, y0, z, color, u0, v1, light, 0, 0, -1);
            vtx(c, p, x0, y1, z, color, u0, v0, light, 0, 0, -1);
            vtx(c, p, x1, y1, z, color, u1, v0, light, 0, 0, -1);
        }
    }

    /** Box built from 1x1 cells so the 32px stone texture tiles once per block face. */
    private static void box(VertexConsumer c, PoseStack.Pose p, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light, float e) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        x0 -= e; y0 -= e; z0 -= e; x1 += e; y1 += e; z1 += e;
        for (float a = x0; a < x1 - 0.01F; a++) {
            float b = Math.min(a + 1, x1);
            for (float y = y0; y < y1 - 0.01F; y++) {
                float t = Math.min(y + 1, y1);
                // front (z1) and back (z0)
                vtx(c, p, a, y, z1, color, u0, v1, light, 0, 0, 1); vtx(c, p, b, y, z1, color, u1, v1, light, 0, 0, 1);
                vtx(c, p, b, t, z1, color, u1, v0, light, 0, 0, 1); vtx(c, p, a, t, z1, color, u0, v0, light, 0, 0, 1);
                vtx(c, p, b, y, z0, color, u0, v1, light, 0, 0, -1); vtx(c, p, a, y, z0, color, u1, v1, light, 0, 0, -1);
                vtx(c, p, a, t, z0, color, u1, v0, light, 0, 0, -1); vtx(c, p, b, t, z0, color, u0, v0, light, 0, 0, -1);
            }
            for (float z = z0; z < z1 - 0.01F; z++) {
                float w = Math.min(z + 1, z1);
                // top and bottom
                vtx(c, p, a, y1, w, color, u0, v1, light, 0, 1, 0); vtx(c, p, b, y1, w, color, u1, v1, light, 0, 1, 0);
                vtx(c, p, b, y1, z, color, u1, v0, light, 0, 1, 0); vtx(c, p, a, y1, z, color, u0, v0, light, 0, 1, 0);
                vtx(c, p, a, y0, z, color, u0, v1, light, 0, -1, 0); vtx(c, p, b, y0, z, color, u1, v1, light, 0, -1, 0);
                vtx(c, p, b, y0, w, color, u1, v0, light, 0, -1, 0); vtx(c, p, a, y0, w, color, u0, v0, light, 0, -1, 0);
            }
        }
        for (float y = y0; y < y1 - 0.01F; y++) {
            float t = Math.min(y + 1, y1);
            for (float z = z0; z < z1 - 0.01F; z++) {
                float w = Math.min(z + 1, z1);
                // east (x1) and west (x0)
                vtx(c, p, x1, y, w, color, u0, v1, light, 1, 0, 0); vtx(c, p, x1, y, z, color, u1, v1, light, 1, 0, 0);
                vtx(c, p, x1, t, z, color, u1, v0, light, 1, 0, 0); vtx(c, p, x1, t, w, color, u0, v0, light, 1, 0, 0);
                vtx(c, p, x0, y, z, color, u0, v1, light, -1, 0, 0); vtx(c, p, x0, y, w, color, u1, v1, light, -1, 0, 0);
                vtx(c, p, x0, t, w, color, u1, v0, light, -1, 0, 0); vtx(c, p, x0, t, z, color, u0, v0, light, -1, 0, 0);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(NexusGateBlockEntity gate) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(NexusGateBlockEntity gate) {
        var p = gate.getBlockPos();
        return new AABB(p.getX() - 2, p.getY(), p.getZ() - 1, p.getX() + 3, p.getY() + 8, p.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}

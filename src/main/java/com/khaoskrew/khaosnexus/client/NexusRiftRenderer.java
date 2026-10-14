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

/** The Nexus Gate as a floating energy rift: layered, rank-tinted energy planes plus a white-hot core pass. */
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
        float[] k = openScale(gate, partialTick);
        pose.pushPose();
        if (k != null) {
            pose.translate(0.5D, 3.0D, 0.5D);
            pose.scale(k[0], k[1], k[0]);
            pose.translate(-0.5D, -3.0D, -0.5D);
        }
        super.render(gate, partialTick, pose, buffer, LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
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

    static final int CRACK_TICKS = 44;
    static final int COLLAPSE_TICKS = 24;

    /**
     * Crack-open: a hairline slit tears down the full height, then the rift bulges open (ease-out-back).
     * Collapse: when the gate closes it pinches to a slit and snaps back as the dormant husk. Null = no scaling.
     */
    static float[] openScale(NexusGateBlockEntity gate, float partialTick) {
        if (gate.getLevel() == null) {
            return null;
        }
        double now = gate.getLevel().getGameTime() + partialTick;
        boolean closed = gate.isClosed();
        if (gate.clientLastClosed == null) {
            gate.clientLastClosed = closed;
        } else if (gate.clientLastClosed != closed) {
            gate.clientLastClosed = closed;
            gate.clientClosedAt = (long) now;
        }
        if (closed && now - gate.clientClosedAt < COLLAPSE_TICKS) {
            float t = (float) ((now - gate.clientClosedAt) / COLLAPSE_TICKS);
            float sx = t < 0.5F ? 1.0F - 0.94F * ease(t * 2) : 0.06F + 0.94F * ease((t - 0.5F) * 2);
            spark(gate, t < 0.5F ? 3 : 0);
            return new float[] {sx, 1.0F};
        }
        if (closed || gate.bornAt <= 0L) {
            return null;
        }
        double age = now - gate.bornAt;
        if (age < 0 || age >= CRACK_TICKS) {
            return null;
        }
        float t = (float) (age / CRACK_TICKS);
        float slit = 0.3F;
        if (t < slit) {
            spark(gate, 2);
            return new float[] {0.05F, Math.max(0.02F, ease(t / slit))};
        }
        float u = (t - slit) / (1 - slit);
        float c = 1.70158F;
        float back = 1 + (c + 1) * (float) Math.pow(u - 1, 3) + c * (float) Math.pow(u - 1, 2);
        spark(gate, u < 0.4F ? 4 : 0);
        return new float[] {0.05F + 0.95F * back, 1.0F};
    }

    private static float ease(float t) {
        t = Math.min(1, Math.max(0, t));
        return t * t * (3 - 2 * t);
    }

    private static void spark(NexusGateBlockEntity gate, int n) {
        var level = gate.getLevel();
        if (n <= 0 || Minecraft.getInstance().isPaused()) {
            return;
        }
        var p = gate.getBlockPos();
        var r = level.getRandom();
        for (int i = 0; i < n; i++) {
            level.addParticle(r.nextBoolean() ? net.minecraft.core.particles.ParticleTypes.END_ROD : net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    p.getX() + 0.5D + (r.nextDouble() - 0.5D) * 0.3D, p.getY() + 0.5D + r.nextDouble() * 5.5D, p.getZ() + 0.5D + (r.nextDouble() - 0.5D) * 0.3D,
                    (r.nextDouble() - 0.5D) * 0.25D, (r.nextDouble() - 0.5D) * 0.1D, (r.nextDouble() - 0.5D) * 0.25D);
        }
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

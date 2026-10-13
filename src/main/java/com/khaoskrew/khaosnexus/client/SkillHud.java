package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;
import com.khaoskrew.khaosnexus.skill.SkillService;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Ability cooldown strip above the hotbar; follows the Hunter Card UI scale and opacity settings. */
public final class SkillHud {
    private SkillHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !Config.HUD_ENABLED.get()) {
            return;
        }
        HunterProfile p = mc.player.getData(ModAttachments.HUNTER);
        if (HunterClass.byId(p.classId) == null) {
            return;
        }
        float k = Config.UI_SCALE.get().floatValue();
        int a = (int) (255 * Config.UI_OPACITY.get());
        long now = mc.level.getGameTime();
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() - 62;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(k, k, 1);
        slot(g, mc, -64, ClassKeybinds.PRIMARY, p.abilityReady - now, a);
        slot(g, mc, 2, ClassKeybinds.SECONDARY, p.abilityAltReady - now, a);
        int free;
        try {
            free = SkillService.available(p, HunterClass.byId(p.classId));
        } catch (IllegalStateException e) { // server config not yet synced
            free = 0;
        }
        if (free > 0) {
            Component pts = Component.translatable("khaosnexus.hud.skill_points", free);
            g.drawString(mc.font, pts, -mc.font.width(pts) / 2, -10, 0xFFE0C070, true);
        }
        g.pose().popPose();
    }

    private static void slot(GuiGraphics g, Minecraft mc, int x, KeyMapping key, long left, int alpha) {
        boolean ready = left <= 0;
        g.fill(x, 0, x + 62, 12, (alpha << 24) | (ready ? 0x123A1E : 0x3A1220));
        Component txt = Component.literal("[").append(key.getTranslatedKeyMessage()).append("] ")
                .append(ready ? Component.translatable("khaosnexus.hud.ready")
                        : Component.literal(String.format("%.1fs", left / 20.0F)));
        g.drawString(mc.font, txt, x + 3, 2, ready ? 0xFF8CF0A0 : 0xFFF0A0A8, true);
    }
}

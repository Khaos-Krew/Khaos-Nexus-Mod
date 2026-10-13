package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class HunterHud {
    private HunterHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !Config.HUD_ENABLED.get()
                || !com.khaoskrew.khaosnexus.card.HunterCard.isUnlocked(minecraft.player)) {
            return;
        }
        HunterProfile profile = minecraft.player.getData(ModAttachments.HUNTER);
        GateRank rank = HunterRanks.rankOf(minecraft.player);
        HunterClass hunterClass = HunterClass.byId(profile.classId);
        Component className = hunterClass == null
                ? Component.translatable("khaosnexus.hud.no_class")
                : Component.translatable(hunterClass.translationKey());
        int x = Config.HUD_X.get();
        int y = Config.HUD_Y.get();
        float k = Config.HUD_SCALE.get().floatValue();
        int alpha = (int) (255 * Config.HUD_OPACITY.get());
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(k, k, 1);
        graphics.pose().translate(-x, -y, 0);
        graphics.fill(x - 4, y - 4, x + 180, y + 34, (alpha << 24) | 0x080A12);
        graphics.drawString(minecraft.font, Component.translatable(
                "khaosnexus.hud.rank", Component.translatable(rank.translationKey()), profile.xp), x, y, 0xFFF2F2F2, true);
        graphics.drawString(minecraft.font, Component.translatable(
                "khaosnexus.hud.class", className, profile.spireFloor, profile.partySize), x, y + 11, 0xFFE0C070, true);
        graphics.drawString(minecraft.font, Component.translatable(
                "khaosnexus.hud.bounty", profile.bountyGates, profile.bountyRaids), x, y + 22, 0xFF9FD0FF, true);
        graphics.pose().popPose();
    }
}

package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.network.ActionPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClassScreen extends Screen {
    public ClassScreen() {
        super(Component.translatable("khaosnexus.class.screen"));
    }

    @Override
    protected void init() {
        int y = 32;
        for (HunterClass hunterClass : HunterClass.values()) {
            this.addRenderableWidget(Button.builder(Component.translatable(hunterClass.translationKey()), button -> {
                PacketDistributor.sendToServer(new ActionPayload(ActionPayload.EQUIP, hunterClass.id()));
                this.onClose();
            }).bounds(this.width / 2 - 100, y, 200, 20).build());
            y += 22;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }
}

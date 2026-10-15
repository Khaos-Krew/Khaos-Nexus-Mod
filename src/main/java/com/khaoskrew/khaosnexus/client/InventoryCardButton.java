package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.network.ActionPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A small "Card" button on the survival inventory, top-right of the panel: opens the Hunter Card like the J key. */
@EventBusSubscriber(modid = KhaosNexus.MODID, value = Dist.CLIENT)
public final class InventoryCardButton {
    private InventoryCardButton() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inv) {
            int x = inv.getGuiLeft() + 176 - 22, y = inv.getGuiTop() - 20;
            event.addListener(Button.builder(Component.translatable("khaosnexus.card.inventory_button"),
                            b -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.OPEN_CARD, "")))
                    .bounds(x, y, 22, 18).tooltip(Tooltip.create(Component.translatable("khaosnexus.card.inventory_tooltip"))).build());
        }
    }
}

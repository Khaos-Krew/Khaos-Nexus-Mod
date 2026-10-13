package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.network.ActionPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = KhaosNexus.MODID, value = Dist.CLIENT)
public final class ClientInput {
    private ClientInput() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (ClassKeybinds.OPEN_CARD.consumeClick()) { // works before unlock too: the server unlocks on a carried card
            PacketDistributor.sendToServer(new ActionPayload(ActionPayload.OPEN_CARD, ""));
        }
        if (!com.khaoskrew.khaosnexus.card.HunterCard.isUnlocked(minecraft.player)) {
            // No Hunter Card yet: swallow the hunter keys so no hunter UI or ability fires.
            while (ClassKeybinds.PRIMARY.consumeClick() || ClassKeybinds.SECONDARY.consumeClick() || ClassKeybinds.CLASS_MENU.consumeClick()) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.card.need"), true);
            }
            return;
        }
        while (ClassKeybinds.PRIMARY.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(ActionPayload.PRIMARY, ""));
        }
        while (ClassKeybinds.SECONDARY.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(ActionPayload.SECONDARY, ""));
        }
        while (ClassKeybinds.CLASS_MENU.consumeClick()) {
            minecraft.setScreen(new ClassScreen());
        }
    }
}

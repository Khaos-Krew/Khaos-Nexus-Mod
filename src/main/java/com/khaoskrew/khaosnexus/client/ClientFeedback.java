package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.network.FeedbackPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class ClientFeedback {
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();

    private ClientFeedback() {}

    public static void show(FeedbackPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.translatable(payload.titleKey());
        Component body = payload.argKey().isEmpty()
                ? Component.translatable(payload.bodyKey())
                : Component.translatable(payload.bodyKey(), Component.translatable(payload.argKey()));
        SystemToast.add(minecraft.getToasts(), TOAST, title, body);
    }
}

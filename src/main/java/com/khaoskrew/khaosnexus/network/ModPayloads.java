package com.khaoskrew.khaosnexus.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {}

    private static void showFeedback(FeedbackPayload payload) {
        try {
            Class.forName("com.khaoskrew.khaosnexus.client.ClientFeedback")
                    .getMethod("show", FeedbackPayload.class)
                    .invoke(null, payload);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not show Khaos Nexus feedback", ex);
        }
    }

    private static void openCard(CardPayload payload) {
        try {
            Class.forName("com.khaoskrew.khaosnexus.client.HunterCardScreen").getMethod("open", CardPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not open the Hunter Card", ex);
        }
    }

    public static void sendFeedback(ServerPlayer player, String titleKey, String bodyKey, String argKey) {
        if (NetworkRegistry.hasChannel(player.connection, FeedbackPayload.TYPE.id())) {
            PacketDistributor.sendToPlayer(player, new FeedbackPayload(titleKey, bodyKey, argKey));
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(FeedbackPayload.TYPE, FeedbackPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> showFeedback(payload)));
        registrar.playToClient(CardPayload.TYPE, CardPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> openCard(payload)));
        registrar.playToClient(SagaPayload.TYPE, SagaPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    try {
                        Class.forName("com.khaoskrew.khaosnexus.client.HunterCardScreen").getMethod("saga", SagaPayload.class).invoke(null, payload);
                    } catch (ReflectiveOperationException ex) {
                        throw new IllegalStateException(ex);
                    }
                }));
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, ActionPayload::handle);
    }
}

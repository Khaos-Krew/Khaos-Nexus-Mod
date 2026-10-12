package com.khaoskrew.khaosnexus.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(FeedbackPayload.TYPE, FeedbackPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> showFeedback(payload)));
    }
}

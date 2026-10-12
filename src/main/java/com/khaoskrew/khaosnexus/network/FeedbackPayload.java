package com.khaoskrew.khaosnexus.network;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FeedbackPayload(String titleKey, String bodyKey, String argKey) implements CustomPacketPayload {
    public static final Type<FeedbackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FeedbackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FeedbackPayload::titleKey,
            ByteBufCodecs.STRING_UTF8, FeedbackPayload::bodyKey,
            ByteBufCodecs.STRING_UTF8, FeedbackPayload::argKey,
            FeedbackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

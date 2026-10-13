package com.khaoskrew.khaosnexus.network;

import java.util.List;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: open the Hunter Card screen with the nearby gates. */
public record CardPayload(List<Gate> gates, int radius) implements CustomPacketPayload {
    /** state: 0 open, 1 active, 2 cleared. */
    public record Gate(String rank, BlockPos pos, int distance, int state) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Gate> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Gate::rank, BlockPos.STREAM_CODEC, Gate::pos,
                ByteBufCodecs.VAR_INT, Gate::distance, ByteBufCodecs.VAR_INT, Gate::state, Gate::new);
    }

    public static final Type<CardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "hunter_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CardPayload> STREAM_CODEC = StreamCodec.composite(
            Gate.CODEC.apply(ByteBufCodecs.list(64)), CardPayload::gates, ByteBufCodecs.VAR_INT, CardPayload::radius, CardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

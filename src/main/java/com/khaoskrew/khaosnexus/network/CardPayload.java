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
public record CardPayload(List<Gate> gates, int radius, List<ForgeLine> forge) implements CustomPacketPayload {
    public CardPayload(List<Gate> gates, int radius) {
        this(gates, radius, List.of());
    }

    /** One Hunter Forge level for the card's Forge tab. */
    public record ForgeLine(String name, int level, String rank, String tier, int count, int essence, int core, boolean unlocked) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeLine> CODEC = StreamCodec.of((buf, l) -> {
            buf.writeUtf(l.name()); buf.writeVarInt(l.level()); buf.writeUtf(l.rank()); buf.writeUtf(l.tier());
            buf.writeVarInt(l.count()); buf.writeVarInt(l.essence()); buf.writeVarInt(l.core()); buf.writeBoolean(l.unlocked());
        }, buf -> new ForgeLine(buf.readUtf(), buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readBoolean()));
    }

    /** state: 0 open, 1 active, 2 cleared. */
    public record Gate(String rank, BlockPos pos, int distance, int state) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Gate> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Gate::rank, BlockPos.STREAM_CODEC, Gate::pos,
                ByteBufCodecs.VAR_INT, Gate::distance, ByteBufCodecs.VAR_INT, Gate::state, Gate::new);
    }

    public static final Type<CardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "hunter_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CardPayload> STREAM_CODEC = StreamCodec.composite(
            Gate.CODEC.apply(ByteBufCodecs.list(64)), CardPayload::gates, ByteBufCodecs.VAR_INT, CardPayload::radius,
            ForgeLine.CODEC.apply(ByteBufCodecs.list(128)), CardPayload::forge, CardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

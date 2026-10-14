package com.khaoskrew.khaosnexus.network;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Oath definitions (datapack driven) for the Hunter Card's Saga tab. */
public record SagaPayload(List<Oath> oaths, int respecCost) implements CustomPacketPayload {
    public record Oath(String id, String role, List<String> nodes, List<Integer> costs) {}

    public static final Type<SagaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "saga"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SagaPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarInt(p.oaths.size());
        for (Oath o : p.oaths) {
            buf.writeUtf(o.id); buf.writeUtf(o.role);
            buf.writeVarInt(o.nodes.size()); o.nodes.forEach(buf::writeUtf);
            buf.writeVarInt(o.costs.size()); o.costs.forEach(buf::writeVarInt);
        }
        buf.writeVarInt(p.respecCost);
    }, buf -> {
        int n = buf.readVarInt();
        List<Oath> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(), role = buf.readUtf();
            List<String> nodes = new ArrayList<>(); int k = buf.readVarInt(); for (int j = 0; j < k; j++) nodes.add(buf.readUtf());
            List<Integer> costs = new ArrayList<>(); k = buf.readVarInt(); for (int j = 0; j < k; j++) costs.add(buf.readVarInt());
            list.add(new Oath(id, role, nodes, costs));
        }
        return new SagaPayload(list, buf.readVarInt());
    });

    public static SagaPayload build() {
        List<Oath> list = new ArrayList<>();
        JsonPool.OATHS.all().forEach((id, j) -> list.add(fromJson(id, j)));
        return new SagaPayload(list, Config.OATH_RESPEC_SHARDS.get());
    }

    static Oath fromJson(ResourceLocation id, JsonObject j) {
        List<String> nodes = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        if (j.has("nodes")) for (JsonElement e : j.getAsJsonArray("nodes")) nodes.add(e.getAsString());
        if (j.has("node_cost")) for (JsonElement e : j.getAsJsonArray("node_cost")) costs.add(e.getAsInt());
        return new Oath(id.toString(), j.has("role") ? j.get("role").getAsString() : "", nodes, costs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

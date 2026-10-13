package com.khaoskrew.khaosnexus.network;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.ClassAbilities;
import com.khaoskrew.khaosnexus.hunter.ClassService;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ActionPayload(int action, String id) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActionPayload::action,
            ByteBufCodecs.STRING_UTF8, ActionPayload::id,
            ActionPayload::new);

    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int EQUIP = 2;
    public static final int OPEN_CARD = 3;
    public static final int SKILL_SPEND = 4;
    public static final int SKILL_RESPEC = 5;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (payload.action() == PRIMARY) {
                ClassAbilities.cast(player, true);
            } else if (payload.action() == SECONDARY) {
                ClassAbilities.cast(player, false);
            } else if (payload.action() == EQUIP) {
                ClassService.equip(player, payload.id());
            } else if (payload.action() == SKILL_SPEND || payload.action() == SKILL_RESPEC) {
                if (!com.khaoskrew.khaosnexus.card.HunterCard.isUnlocked(player)) {
                    return;
                }
                var r = payload.action() == SKILL_SPEND ? com.khaoskrew.khaosnexus.skill.SkillService.spend(player, payload.id())
                        : com.khaoskrew.khaosnexus.skill.SkillService.respec(player);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(payload.action() == SKILL_RESPEC
                        && r == com.khaoskrew.khaosnexus.skill.SkillService.Result.OK ? "khaosnexus.skill.respec_done"
                        : "khaosnexus.skill.result." + r.name().toLowerCase(java.util.Locale.ROOT)), true);
                com.khaoskrew.khaosnexus.card.HunterCardItem.sendCard(player);
            } else if (payload.action() == OPEN_CARD) {
                net.minecraft.world.item.ItemStack card = com.khaoskrew.khaosnexus.compat.CuriosCompat.wornCard(player);
                if (card.isEmpty()) {
                    for (net.minecraft.world.item.ItemStack s : player.getInventory().items) {
                        if (s.getItem() instanceof com.khaoskrew.khaosnexus.card.HunterCardItem) { card = s; break; }
                    }
                }
                if (card.isEmpty()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.card.none_equipped"), true);
                } else {
                    com.khaoskrew.khaosnexus.card.HunterCardItem.open(player, card);
                }
            }
        });
    }
}

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
    public static final int OATH_SWEAR = 6;
    public static final int OATH_NODE = 7;
    public static final int OATH_RESPEC = 8;
    public static final int GUILD = 9;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Per-player packet budget: at most MAX_PER_SECOND actions per 20 ticks; extra packets are dropped. */
    public static final java.util.Map<java.util.UUID, long[]> BUDGET = new java.util.concurrent.ConcurrentHashMap<>();
    public static final int MAX_PER_SECOND = 10;

    public static boolean allow(ServerPlayer player) {
        long now = player.server.getTickCount();
        long[] b = BUDGET.computeIfAbsent(player.getUUID(), k -> new long[] {now, 0});
        if (now - b[0] >= 20 || now < b[0]) {
            b[0] = now;
            b[1] = 0;
        }
        return ++b[1] <= MAX_PER_SECOND;
    }

    public static void handle(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !allow(player)) {
                return;
            }
            if (payload.action() == PRIMARY) {
                ClassAbilities.cast(player, true);
            } else if (payload.action() == SECONDARY) {
                if (player.isShiftKeyDown() && com.khaoskrew.khaosnexus.endgame.Awakening.has(player)) {
                    com.khaoskrew.khaosnexus.endgame.Awakening.cast(player);
                } else {
                    ClassAbilities.cast(player, false);
                }
            } else if (payload.action() == GUILD) {
                com.khaoskrew.khaosnexus.card.GuildActions.run(player, payload.id());
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
            } else if (payload.action() >= OATH_SWEAR && payload.action() <= OATH_RESPEC) {
                if (!com.khaoskrew.khaosnexus.card.HunterCard.isUnlocked(player)) {
                    return;
                }
                var prof = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(player);
                boolean ok;
                String key;
                if (payload.action() == OATH_SWEAR) {
                    var id = net.minecraft.resources.ResourceLocation.tryParse(payload.id());
                    ok = id != null && com.khaoskrew.khaosnexus.saga.Saga.swear(prof, id);
                    key = ok ? "khaosnexus.oath.ui.sworn" : "khaosnexus.oath.ui.cant_swear";
                } else if (payload.action() == OATH_NODE) {
                    ok = com.khaoskrew.khaosnexus.saga.Saga.unlockNode(prof);
                    key = ok ? "khaosnexus.oath.ui.node_done" : "khaosnexus.oath.ui.node_fail";
                } else {
                    ok = com.khaoskrew.khaosnexus.saga.Saga.respec(prof);
                    key = ok ? "khaosnexus.oath.ui.respec_done" : "khaosnexus.oath.ui.respec_fail";
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
                if (ok) {
                    player.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                }
                com.khaoskrew.khaosnexus.hunter.HunterRanks.sync(player);
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

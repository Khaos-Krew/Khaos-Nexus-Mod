package com.khaoskrew.khaosnexus.compat;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios slot progression (1.2): one extra ring slot from hunter rank B and one extra charm slot from rank S.
 * Transient modifiers, refreshed from the rank, so demotion or a config change never strands items. Only called after
 * {@link CuriosCompat#present()}; this class is the only one that links the Curios API directly.
 */
public final class CuriosSlots {
    private CuriosSlots() {}

    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "rank_ring");
    static final ResourceLocation CHARM = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "rank_charm");

    public static int ringBonus(GateRank r) { return r.ordinal() >= GateRank.B.ordinal() ? 1 : 0; }
    public static int charmBonus(GateRank r) { return r.ordinal() >= GateRank.S.ordinal() ? 1 : 0; }

    public static void refresh(ServerPlayer p) {
        GateRank rank = HunterRanks.rankOf(p);
        CuriosApi.getCuriosInventory(p).ifPresent(h -> {
            h.removeSlotModifier("ring", RING);
            h.removeSlotModifier("charm", CHARM);
            if (ringBonus(rank) > 0) h.addTransientSlotModifier("ring", RING, ringBonus(rank), AttributeModifier.Operation.ADD_VALUE);
            if (charmBonus(rank) > 0) h.addTransientSlotModifier("charm", CHARM, charmBonus(rank), AttributeModifier.Operation.ADD_VALUE);
        });
    }

    public static boolean wears(ServerPlayer p, Item item) {
        return CuriosApi.getCuriosInventory(p).map(h -> h.isEquipped(item)).orElse(false);
    }
}

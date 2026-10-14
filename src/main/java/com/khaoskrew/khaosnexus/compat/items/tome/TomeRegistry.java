package com.khaoskrew.khaosnexus.compat.items.tome;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Ars-only: isolated so CompatItems never links Ars classes when Ars is absent. */
public final class TomeRegistry {
    private TomeRegistry() {}

    public static void register(DeferredRegister.Items items, DeferredItem<Item>[] out) {
        for (int t = 0; t < out.length; t++) {
            final int tier = t;
            out[t] = items.register("hunter_tome_" + HunterTomes.TIER_IDS[t], () -> new HunterTomeItem(tier));
        }
        TomeEvents.register();
    }
}

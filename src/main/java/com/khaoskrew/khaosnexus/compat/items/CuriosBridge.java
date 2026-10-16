package com.khaoskrew.khaosnexus.compat.items;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/** Direct Curios calls. Only loaded once Curios is known to be installed; an incompatible Curios disables it. */
final class CuriosBridge {
    private static boolean broken;

    private CuriosBridge() {}

    static boolean equipped(LivingEntity e, Item item) {
        if (broken) {
            return false;
        }
        try {
            return CuriosApi.getCuriosInventory(e).map(h -> h.isEquipped(item)).orElse(false);
        } catch (LinkageError | RuntimeException ex) {
            broken = true;
            KhaosNexus.LOGGER.warn("Khaos Nexus: Curios lookups disabled (incompatible Curios version?): {}", ex.toString());
            return false;
        }
    }
}

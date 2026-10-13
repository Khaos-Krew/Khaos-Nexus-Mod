package com.khaoskrew.khaosnexus.compat.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/** Direct Curios calls. Only loaded once Curios is known to be installed. */
final class CuriosBridge {
    private CuriosBridge() {}

    static boolean equipped(LivingEntity e, Item item) {
        return CuriosApi.getCuriosInventory(e).map(h -> h.isEquipped(item)).orElse(false);
    }
}

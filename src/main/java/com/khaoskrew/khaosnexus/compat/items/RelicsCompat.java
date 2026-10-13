package com.khaoskrew.khaosnexus.compat.items;

import net.minecraft.world.item.Item;

/** Factory kept separate so Relics classes load only when Relics is installed. */
final class RelicsCompat {
    private RelicsCompat() {}

    static Item sigil() {
        return new RiftSigilItem();
    }
}

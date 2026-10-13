package com.khaoskrew.khaosnexus.compat.items;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

/** Registers the Rift Rend glyph with Ars Nouveau. Only called when Ars is installed. */
final class ArsCompat {
    private ArsCompat() {}

    static void init() {
        GlyphRegistry.registerSpell(RiftRendEffect.INSTANCE);
    }
}

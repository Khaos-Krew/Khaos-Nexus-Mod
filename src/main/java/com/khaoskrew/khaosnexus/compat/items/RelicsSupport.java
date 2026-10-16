package com.khaoskrew.khaosnexus.compat.items;

import java.util.function.Supplier;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.world.item.Item;

/**
 * Picks the Rift Hunter's Sigil implementation that matches the installed Relics API, by reflection only, so no Relics
 * class (or one of ours that extends one) is loaded unless that exact API is present.
 * <ul>
 * <li>Relics 0.11+/0.12 (RelicTemplate + WearableRelicItem): {@code RiftSigilItem}.</li>
 * <li>Relics 0.10.x (RelicItem + constructDefaultRelicData): {@code legacy.RiftSigilItemLegacy}, compiled against 0.10.7.8.</li>
 * </ul>
 */
final class RelicsSupport {
    static final String MODERN_IMPL = "com.khaoskrew.khaosnexus.compat.items.RiftSigilItem";
    static final String LEGACY_IMPL = "com.khaoskrew.khaosnexus.compat.items.legacy.RiftSigilItemLegacy";

    private RelicsSupport() {}

    static boolean has(String cls) {
        try {
            Class.forName(cls, false, RelicsSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static boolean hasMethod(String cls, String method) {
        try {
            for (var m : Class.forName(cls, false, RelicsSupport.class.getClassLoader()).getMethods()) {
                if (m.getName().equals(method)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException | LinkageError e) {
            // fall through
        }
        return false;
    }

    /** Which API is installed: "modern", "legacy" or null (unknown, sigil skipped). */
    static String api() {
        if (has("it.hurts.sskirillss.relics.items.relics.base.WearableRelicItem") && has("it.hurts.sskirillss.relics.api.relics.RelicTemplate")
                && has("it.hurts.sskirillss.relics.init.RelicsScalingModels")) {
            return "modern";
        }
        if (has("it.hurts.sskirillss.relics.items.relics.base.RelicItem")
                && hasMethod("it.hurts.sskirillss.relics.items.relics.base.IRelicItem", "constructDefaultRelicData")
                && has("it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData")) {
            return "legacy";
        }
        return null;
    }

    /** Factory for the matching sigil, or null (logged) when the installed Relics API is not one we know. */
    static Supplier<Item> sigilFactory() throws ReflectiveOperationException {
        String api = api();
        if (api == null) {
            KhaosNexus.LOGGER.warn("Khaos Nexus: unrecognised Relics version, Rift Hunter's Sigil disabled");
            return null;
        }
        // Loading the implementation here verifies its superclass/interfaces now (inside CompatItems' guard).
        Class<?> impl = Class.forName("modern".equals(api) ? MODERN_IMPL : LEGACY_IMPL, false, RelicsSupport.class.getClassLoader());
        var ctor = impl.getConstructor();
        KhaosNexus.LOGGER.info("Relics present ({} API): Rift Hunter's Sigil enabled", api);
        return () -> {
            try {
                return (Item) ctor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Rift Hunter's Sigil", e);
            }
        };
    }
}

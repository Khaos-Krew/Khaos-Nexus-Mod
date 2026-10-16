package com.khaoskrew.khaosnexus.compat.items;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Themed items for companion mods. Each group is registered only when its mod is installed, so without the mod the
 * items simply don't exist (no missing-registry warnings, nothing to remap). Companion classes are only touched from
 * inside the guarded branches.
 */
public final class CompatItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);

    public static DeferredItem<Item> RIFT_SOURCE_SHARD;
    public static DeferredItem<Item> RIFT_HUNTERS_SIGIL;
    public static DeferredItem<Item> RIFT_LANTERN;
    public static DeferredItem<Item> VOIDSTEP_ANKLET;
    public static DeferredItem<Item> GATEBREAKER_BAND;
    /** Hunter Tomes by tier (E, D, C, B, A, S, Paragon); null without Ars Nouveau. */
    @SuppressWarnings("unchecked")
    public static DeferredItem<Item>[] HUNTER_TOMES = new DeferredItem[7];
    public static DeferredItem<Item> WARDENS_BROKEN_CHAIN;
    public static DeferredItem<Item> GUARDIANS_PLUME;
    public static DeferredItem<Item> HUNTERS_LUCKY_COIN;

    private CompatItems() {}

    public static boolean ars() { return ModList.get().isLoaded("ars_nouveau"); }
    public static boolean relics() { return ModList.get().isLoaded("relics") && ModList.get().isLoaded("curios"); }
    public static boolean artifacts() { return ModList.get().isLoaded("artifacts") && ModList.get().isLoaded("curios"); }

    public static void register(IEventBus bus) {
        if (ars()) {
            guarded("Ars Nouveau", () -> {
                need("com.hollingsworth.arsnouveau.api.registry.GlyphRegistry", "com.hollingsworth.arsnouveau.api.spell.AbstractEffect",
                        "com.hollingsworth.arsnouveau.common.items.SpellBook", "com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment");
                // Link our Ars-facing classes now, so an API mismatch fails here (caught) instead of at registry time.
                need("com.khaoskrew.khaosnexus.compat.items.RiftRendEffect", "com.khaoskrew.khaosnexus.compat.items.GatesenseEffect",
                        "com.khaoskrew.khaosnexus.compat.items.tome.HunterTomeItem");
                ArsCompat.init();
                RIFT_SOURCE_SHARD = ITEMS.registerSimpleItem("rift_source_shard", new Item.Properties().rarity(Rarity.UNCOMMON));
                com.khaoskrew.khaosnexus.compat.items.tome.TomeRegistry.register(ITEMS, HUNTER_TOMES);
            });
        }
        if (relics()) {
            guarded("Relics", () -> {
                java.util.function.Supplier<Item> sigil = RelicsSupport.sigilFactory();
                if (sigil != null) {
                    RIFT_HUNTERS_SIGIL = ITEMS.register("rift_hunters_sigil", sigil);
                }
            });
        }
        if (artifacts()) {
            guarded("Artifacts", () -> {
                need("top.theillusivec4.curios.api.CuriosApi");
                RIFT_LANTERN = ITEMS.register("rift_lantern", () -> new RiftArtifactItem(RiftArtifactItem.Kind.LANTERN));
                VOIDSTEP_ANKLET = ITEMS.register("voidstep_anklet", () -> new RiftArtifactItem(RiftArtifactItem.Kind.ANKLET));
                GATEBREAKER_BAND = ITEMS.register("gatebreaker_band", () -> new RiftArtifactItem(RiftArtifactItem.Kind.BAND));
                WARDENS_BROKEN_CHAIN = ITEMS.register("wardens_broken_chain", () -> new RiftArtifactItem(RiftArtifactItem.Kind.CHAIN));
                GUARDIANS_PLUME = ITEMS.register("guardians_plume", () -> new RiftArtifactItem(RiftArtifactItem.Kind.PLUME));
                HUNTERS_LUCKY_COIN = ITEMS.register("hunters_lucky_coin", () -> new RiftArtifactItem(RiftArtifactItem.Kind.COIN));
            });
        }
        ITEMS.register(bus);
    }

    /** Runs one companion's setup; a missing or changed companion API logs a warning instead of crashing the game. */
    static void guarded(String mod, Runnable setup) {
        try {
            setup.run();
        } catch (LinkageError | ReflectiveOperationException | RuntimeException e) {
            KhaosNexus.LOGGER.warn("Khaos Nexus: {} integration disabled (incompatible version installed?): {}", mod, e.toString());
        }
    }

    /** Loads (and links) each class by name; throws if any is missing so the guarded block bails out. */
    static void need(String... classes) throws ClassNotFoundException {
        for (String c : classes) {
            Class.forName(c, false, CompatItems.class.getClassLoader());
        }
    }

    @FunctionalInterface
    interface Runnable {
        void run() throws ReflectiveOperationException;
    }
}

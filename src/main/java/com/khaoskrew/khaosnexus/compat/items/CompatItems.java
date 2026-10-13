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

    private CompatItems() {}

    public static boolean ars() { return ModList.get().isLoaded("ars_nouveau"); }
    public static boolean relics() { return ModList.get().isLoaded("relics") && ModList.get().isLoaded("curios"); }
    public static boolean artifacts() { return ModList.get().isLoaded("artifacts") && ModList.get().isLoaded("curios"); }

    public static void register(IEventBus bus) {
        if (ars()) {
            RIFT_SOURCE_SHARD = ITEMS.registerSimpleItem("rift_source_shard", new Item.Properties().rarity(Rarity.UNCOMMON));
            ArsCompat.init();
        }
        if (relics()) {
            RIFT_HUNTERS_SIGIL = ITEMS.register("rift_hunters_sigil", RelicsCompat::sigil);
        }
        if (artifacts()) {
            RIFT_LANTERN = ITEMS.register("rift_lantern", () -> new RiftArtifactItem(RiftArtifactItem.Kind.LANTERN));
            VOIDSTEP_ANKLET = ITEMS.register("voidstep_anklet", () -> new RiftArtifactItem(RiftArtifactItem.Kind.ANKLET));
            GATEBREAKER_BAND = ITEMS.register("gatebreaker_band", () -> new RiftArtifactItem(RiftArtifactItem.Kind.BAND));
        }
        ITEMS.register(bus);
    }
}

package com.khaoskrew.khaosnexus.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, KhaosNexus.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_GHOUL = register("rift_ghoul", 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_STALKER = register("rift_stalker", 0.6F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> VOID_CASTER = register("void_caster", 0.7F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_BRUTE = register("rift_brute", 1.4F, 2.9F);

    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> RIFT_WARDEN = ENTITIES.register("rift_warden",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::warden, MobCategory.MONSTER).sized(1.6F, 5.0F).clientTrackingRange(12)
                    .fireImmune().build(KhaosNexus.MODID + ":rift_warden"));
    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> SPIRE_GUARDIAN = ENTITIES.register("spire_guardian",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::guardian, MobCategory.MONSTER).sized(1.3F, 4.0F).clientTrackingRange(12)
                    .build(KhaosNexus.MODID + ":spire_guardian"));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_GHOUL_EGG = egg("rift_ghoul", RIFT_GHOUL, 0x25252E, 0xC12F41);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_STALKER_EGG = egg("rift_stalker", RIFT_STALKER, 0x2A1F3C, 0xC23346);
    public static final DeferredItem<DeferredSpawnEggItem> VOID_CASTER_EGG = egg("void_caster", VOID_CASTER, 0x301C35, 0xCA59EE);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_BRUTE_EGG = egg("rift_brute", RIFT_BRUTE, 0x463442, 0xB13648);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_WARDEN_EGG = egg("rift_warden", RIFT_WARDEN, 0x4A141C, 0xAD2530);
    public static final DeferredItem<DeferredSpawnEggItem> SPIRE_GUARDIAN_EGG = egg("spire_guardian", SPIRE_GUARDIAN, 0x5A5054, 0x7F18A9);

    private ModEntities() {}

    private static DeferredItem<DeferredSpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    public static java.util.List<Item> eggs() {
        return java.util.List.of(RIFT_GHOUL_EGG.get(), RIFT_STALKER_EGG.get(), VOID_CASTER_EGG.get(), RIFT_BRUTE_EGG.get(),
                RIFT_WARDEN_EGG.get(), SPIRE_GUARDIAN_EGG.get());
    }

    private static DeferredHolder<EntityType<?>, EntityType<RiftMob>> register(String name, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.<RiftMob>of(RiftMob::new, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(10)
                .build(KhaosNexus.MODID + ":" + name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(RIFT_GHOUL.get(), RiftMob.attributes(24.0D, 4.0D, 0.30D, 0.0D).build());
        event.put(RIFT_STALKER.get(), RiftMob.attributes(20.0D, 5.0D, 0.34D, 0.0D).build());
        event.put(VOID_CASTER.get(), RiftMob.attributes(22.0D, 3.0D, 0.26D, 0.0D).build());
        event.put(RIFT_BRUTE.get(), RiftMob.attributes(60.0D, 10.0D, 0.24D, 0.6D).build());
        event.put(RIFT_WARDEN.get(), RiftMob.attributes(300.0D, 16.0D, 0.27D, 1.0D).add(Attributes.ARMOR, 10.0D).build());
        event.put(SPIRE_GUARDIAN.get(), RiftMob.attributes(80.0D, 8.0D, 0.25D, 0.8D).add(Attributes.ARMOR, 6.0D).build());
    }
}

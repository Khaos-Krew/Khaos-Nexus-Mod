package com.khaoskrew.khaosnexus.mob;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, KhaosNexus.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_GHOUL = register("rift_ghoul", 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_STALKER = register("rift_stalker", 0.6F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> VOID_CASTER = register("void_caster", 0.7F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftMob>> RIFT_BRUTE = register("rift_brute", 1.4F, 2.9F);

    private ModEntities() {}

    private static DeferredHolder<EntityType<?>, EntityType<RiftMob>> register(String name, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.<RiftMob>of(RiftMob::new, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(10)
                .build(KhaosNexus.MODID + ":" + name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(RIFT_GHOUL.get(), RiftMob.attributes(24.0D, 4.0D, 0.30D, 0.0D).build());
        event.put(RIFT_STALKER.get(), RiftMob.attributes(20.0D, 5.0D, 0.34D, 0.0D).build());
        event.put(VOID_CASTER.get(), RiftMob.attributes(22.0D, 3.0D, 0.26D, 0.0D).build());
        event.put(RIFT_BRUTE.get(), RiftMob.attributes(60.0D, 10.0D, 0.24D, 0.6D).build());
    }
}

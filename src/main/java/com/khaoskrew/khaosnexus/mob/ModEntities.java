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

    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> VAELITH = ENTITIES.register("vaelith",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::vaelith, MobCategory.MONSTER).sized(1.4F, 4.4F).clientTrackingRange(12)
                    .fireImmune().build(KhaosNexus.MODID + ":vaelith"));
    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> FALLEN_HIEROPHANT = ENTITIES.register("fallen_hierophant",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::hierophant, MobCategory.MONSTER).sized(1.4F, 4.6F).clientTrackingRange(12)
                    .build(KhaosNexus.MODID + ":fallen_hierophant"));
    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> GENERAL_BLADE = ENTITIES.register("general_blade",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::general, MobCategory.MONSTER).sized(1.3F, 3.9F).clientTrackingRange(12)
                    .build(KhaosNexus.MODID + ":general_blade"));
    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> GENERAL_ARCANIST = ENTITIES.register("general_arcanist",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::general, MobCategory.MONSTER).sized(1.3F, 3.9F).clientTrackingRange(12)
                    .build(KhaosNexus.MODID + ":general_arcanist"));
    public static final DeferredHolder<EntityType<?>, EntityType<RiftBoss>> HOLLOW_KING = ENTITIES.register("hollow_king",
            () -> EntityType.Builder.<RiftBoss>of(RiftBoss::king, MobCategory.MONSTER).sized(1.9F, 6.4F).clientTrackingRange(14)
                    .fireImmune().build(KhaosNexus.MODID + ":hollow_king"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.khaoskrew.khaosnexus.saga.RiftWisp>> RIFT_WISP = ENTITIES.register("rift_wisp",
            () -> EntityType.Builder.<com.khaoskrew.khaosnexus.saga.RiftWisp>of(com.khaoskrew.khaosnexus.saga.RiftWisp::new, MobCategory.MISC).sized(0.5F, 0.6F).clientTrackingRange(8)
                    .fireImmune().noSummon().build(KhaosNexus.MODID + ":rift_wisp"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.khaoskrew.khaosnexus.dungeon.GateMimic>> GATE_MIMIC = ENTITIES.register("gate_mimic",
            () -> EntityType.Builder.<com.khaoskrew.khaosnexus.dungeon.GateMimic>of(com.khaoskrew.khaosnexus.dungeon.GateMimic::new, MobCategory.MONSTER).sized(1.4F, 1.9F).clientTrackingRange(10)
                    .build(KhaosNexus.MODID + ":gate_mimic"));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidOrb>> VOID_ORB = ENTITIES.register("void_orb",
            () -> EntityType.Builder.<VoidOrb>of(VoidOrb::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(2)
                    .build(KhaosNexus.MODID + ":void_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<MinionMob>> RIFT_FAMILIAR = ENTITIES.register("rift_familiar",
            () -> EntityType.Builder.<MinionMob>of(MinionMob::new, MobCategory.CREATURE).sized(0.8F, 1.0F).clientTrackingRange(10)
                    .build(KhaosNexus.MODID + ":rift_familiar"));
    public static final DeferredHolder<EntityType<?>, EntityType<MinionMob>> BONE_THRALL = ENTITIES.register("bone_thrall",
            () -> EntityType.Builder.<MinionMob>of(MinionMob::new, MobCategory.CREATURE).sized(0.6F, 1.9F).clientTrackingRange(10)
                    .build(KhaosNexus.MODID + ":bone_thrall"));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);
    /** Render-only item for the Void Caster's spell projectile (not in the creative tab). */
    public static final DeferredItem<Item> VOID_ORB_ITEM = ITEMS.registerSimpleItem("void_orb");
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_GHOUL_EGG = egg("rift_ghoul", RIFT_GHOUL, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_STALKER_EGG = egg("rift_stalker", RIFT_STALKER, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> VOID_CASTER_EGG = egg("void_caster", VOID_CASTER, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_BRUTE_EGG = egg("rift_brute", RIFT_BRUTE, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_WARDEN_EGG = egg("rift_warden", RIFT_WARDEN, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> SPIRE_GUARDIAN_EGG = egg("spire_guardian", SPIRE_GUARDIAN, 0xFFFFFF, 0xFFFFFF);
    public static final DeferredItem<DeferredSpawnEggItem> VAELITH_EGG = egg("vaelith", VAELITH, 0x334A78, 0x8AF0FF);
    public static final DeferredItem<DeferredSpawnEggItem> FALLEN_HIEROPHANT_EGG = egg("fallen_hierophant", FALLEN_HIEROPHANT, 0x3A1620, 0xB848FF);
    public static final DeferredItem<DeferredSpawnEggItem> GENERAL_BLADE_EGG = egg("general_blade", GENERAL_BLADE, 0x3A3A42, 0xFF5A3A);
    public static final DeferredItem<DeferredSpawnEggItem> GENERAL_ARCANIST_EGG = egg("general_arcanist", GENERAL_ARCANIST, 0x2A3436, 0x5AF0C8);
    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_KING_EGG = egg("hollow_king", HOLLOW_KING, 0x5E0E1E, 0xFFD06A);
    public static final DeferredItem<DeferredSpawnEggItem> GATE_MIMIC_EGG = egg("gate_mimic", GATE_MIMIC, 0x6A4524, 0xFFB43A);

    private ModEntities() {}

    private static DeferredItem<DeferredSpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    public static java.util.List<Item> eggs() {
        return java.util.List.of(RIFT_GHOUL_EGG.get(), RIFT_STALKER_EGG.get(), VOID_CASTER_EGG.get(), RIFT_BRUTE_EGG.get(),
                RIFT_WARDEN_EGG.get(), SPIRE_GUARDIAN_EGG.get(), GATE_MIMIC_EGG.get(), HOLLOW_KING_EGG.get());
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
        event.put(RIFT_FAMILIAR.get(), MinionMob.attributes(20.0D, 4.0D, 0.33D).build());
        event.put(BONE_THRALL.get(), MinionMob.attributes(24.0D, 5.0D, 0.26D).build());
        // Act I seal guardians: S gate tuning multiplies these. Between the Guardian and the Warden: a real fight, not a wall.
        event.put(VAELITH.get(), RiftMob.attributes(220.0D, 11.0D, 0.27D, 0.6D).add(Attributes.ARMOR, 6.0D).build());
        event.put(FALLEN_HIEROPHANT.get(), RiftMob.attributes(260.0D, 13.0D, 0.24D, 0.9D).add(Attributes.ARMOR, 10.0D).build());
        event.put(HOLLOW_KING.get(), RiftMob.attributes(900.0D, 16.0D, 0.27D, 1.0D).add(Attributes.ARMOR, 16.0D).add(Attributes.ARMOR_TOUGHNESS, 8.0D).add(Attributes.FOLLOW_RANGE, 40.0D).build());
        event.put(GENERAL_BLADE.get(), RiftMob.attributes(170.0D, 12.0D, 0.28D, 0.8D).add(Attributes.ARMOR, 12.0D).build());
        event.put(RIFT_WISP.get(), com.khaoskrew.khaosnexus.saga.RiftWisp.attributes().build());
        event.put(GENERAL_ARCANIST.get(), RiftMob.attributes(140.0D, 10.0D, 0.26D, 0.5D).add(Attributes.ARMOR, 5.0D).build());
        event.put(GATE_MIMIC.get(), com.khaoskrew.khaosnexus.dungeon.GateMimic.attributes().build());
        event.put(SPIRE_GUARDIAN.get(), RiftMob.attributes(80.0D, 8.0D, 0.25D, 0.8D).add(Attributes.ARMOR, 6.0D).build());
    }
}

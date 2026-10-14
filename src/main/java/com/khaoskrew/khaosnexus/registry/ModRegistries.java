package com.khaoskrew.khaosnexus.registry;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.gate.NexusGateItem;
import com.khaoskrew.khaosnexus.raid.PersonalLootBlock;
import com.khaoskrew.khaosnexus.raid.PersonalLootBlockEntity;
import com.khaoskrew.khaosnexus.raid.RaidAltarBlock;
import com.khaoskrew.khaosnexus.spire.SpireGateBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KhaosNexus.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KhaosNexus.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KhaosNexus.MODID);

    public static final DeferredBlock<NexusGateBlock> NEXUS_GATE = BLOCKS.registerBlock("nexus_gate", NexusGateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(NexusGateBlock.CLOSED) ? 4 : 12)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<com.khaoskrew.khaosnexus.spire.SpirePortalBlock> SPIRE_PORTAL = BLOCKS.registerBlock("spire_portal",
            com.khaoskrew.khaosnexus.spire.SpirePortalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .noCollission()
                    .noLootTable()
                    .lightLevel(state -> 12)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<SpireGateBlock> SPIRE_GATE = BLOCKS.registerBlock("spire_gate", SpireGateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 6.0F)
                    .lightLevel(state -> 10)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<RaidAltarBlock> RAID_ALTAR = BLOCKS.registerBlock("raid_altar", RaidAltarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<PersonalLootBlock> PERSONAL_LOOT = BLOCKS.registerBlock("personal_loot", PersonalLootBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredItem<BlockItem> NEXUS_GATE_ITEM = ITEMS.register("nexus_gate",
            () -> new NexusGateItem(NEXUS_GATE.get(), new Item.Properties()));

    public static final DeferredItem<Item> NEXUS_TOKEN = ITEMS.registerSimpleItem("nexus_token");
    public static final DeferredBlock<com.khaoskrew.khaosnexus.saga.SealGateBlock> SEAL_GATE = BLOCKS.registerBlock("seal_gate", com.khaoskrew.khaosnexus.saga.SealGateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).lightLevel(state -> 12).sound(SoundType.AMETHYST).noLootTable());
    public static final DeferredItem<BlockItem> SEAL_GATE_ITEM = ITEMS.registerSimpleBlockItem(SEAL_GATE);
    public static final DeferredItem<Item> VAELITH_CHOIR_MASK = ITEMS.registerItem("vaelith_choir_mask", p -> new com.khaoskrew.khaosnexus.saga.LoreItem(p, 2), new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<Item> SINGING_GLASS = ITEMS.registerItem("singing_glass", p -> new com.khaoskrew.khaosnexus.saga.LoreItem(p, 1), new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> HIEROPHANT_CENSER = ITEMS.registerItem("hierophant_censer", p -> new com.khaoskrew.khaosnexus.saga.LoreItem(p, 2), new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<Item> PROFANE_SIGIL = ITEMS.registerItem("profane_sigil", p -> new com.khaoskrew.khaosnexus.saga.LoreItem(p, 1), new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> WISP_BELL = ITEMS.registerItem("wisp_bell", com.khaoskrew.khaosnexus.saga.RiftWisp.Bell::new, new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<com.khaoskrew.khaosnexus.saga.CrownShardItem> CROWN_SHARD = ITEMS.registerItem("crown_shard",
            com.khaoskrew.khaosnexus.saga.CrownShardItem::new, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<BlockItem> SPIRE_GATE_ITEM = ITEMS.registerSimpleBlockItem(SPIRE_GATE);
    public static final DeferredItem<BlockItem> RAID_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(RAID_ALTAR);
    public static final DeferredItem<BlockItem> PERSONAL_LOOT_ITEM = ITEMS.registerSimpleBlockItem(PERSONAL_LOOT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalLootBlockEntity>> PERSONAL_LOOT_ENTITY =
            BLOCK_ENTITIES.register("personal_loot", () -> BlockEntityType.Builder
                    .of(PersonalLootBlockEntity::new, PERSONAL_LOOT.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.khaoskrew.khaosnexus.spire.SpirePortalBlockEntity>> SPIRE_PORTAL_ENTITY =
            BLOCK_ENTITIES.register("spire_portal", () -> BlockEntityType.Builder
                    .of(com.khaoskrew.khaosnexus.spire.SpirePortalBlockEntity::new, SPIRE_PORTAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NexusGateBlockEntity>> NEXUS_GATE_ENTITY =
            BLOCK_ENTITIES.register("nexus_gate", () -> BlockEntityType.Builder
                    .of(NexusGateBlockEntity::new, NEXUS_GATE.get())
                    // Vanilla uses the type argument for datafixers. Mods pass null.
                    .build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("khaosnexus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.khaosnexus"))
                    .icon(() -> new ItemStack(NEXUS_GATE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(NEXUS_GATE_ITEM.get());
                        com.khaoskrew.khaosnexus.compat.items.CompatItems.ITEMS.getEntries().forEach(e -> output.accept(e.get()));
                        output.accept(NEXUS_TOKEN.get());
                        output.accept(com.khaoskrew.khaosnexus.card.HunterCard.CARD.get());
                        output.accept(com.khaoskrew.khaosnexus.card.HunterCard.GUILD_DESK_ITEM.get());
                        output.accept(com.khaoskrew.khaosnexus.forge.HunterForge.FORGE_ITEM.get());
                        output.accept(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_ESSENCE.get());
                        output.accept(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get());
                        output.accept(SPIRE_GATE_ITEM.get());
                        output.accept(SEAL_GATE_ITEM.get());
                        output.accept(CROWN_SHARD.get()); output.accept(WISP_BELL.get());
                        for (var a : com.khaoskrew.khaosnexus.saga.armor.SagaArmor.CHOIR_SET) output.accept(a.get());
                        for (var a : com.khaoskrew.khaosnexus.saga.armor.SagaArmor.VESTMENT_SET) output.accept(a.get());
                        output.accept(VAELITH_CHOIR_MASK.get()); output.accept(SINGING_GLASS.get());
                        output.accept(HIEROPHANT_CENSER.get()); output.accept(PROFANE_SIGIL.get());
                        output.accept(RAID_ALTAR_ITEM.get());
                        output.accept(PERSONAL_LOOT_ITEM.get());
                        com.khaoskrew.khaosnexus.mob.ModEntities.eggs().forEach(output::accept);
                    })
                    .build());

    private ModRegistries() {}

    public static void register(IEventBus modEventBus) {
        com.khaoskrew.khaosnexus.saga.armor.SagaArmor.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}

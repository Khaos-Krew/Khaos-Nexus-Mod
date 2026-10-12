package com.khaoskrew.khaosnexus.registry;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.gate.NexusGateItem;

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
                    .lightLevel(state -> state.getValue(NexusGateBlock.CLOSED) ? 4 : 12)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> NEXUS_GATE_ITEM = ITEMS.register("nexus_gate",
            () -> new NexusGateItem(NEXUS_GATE.get(), new Item.Properties()));

    public static final DeferredItem<Item> NEXUS_TOKEN = ITEMS.registerSimpleItem("nexus_token");

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
                        output.accept(NEXUS_TOKEN.get());
                    })
                    .build());

    private ModRegistries() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}

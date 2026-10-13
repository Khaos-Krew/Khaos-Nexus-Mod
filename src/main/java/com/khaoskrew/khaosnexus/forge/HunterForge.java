package com.khaoskrew.khaosnexus.forge;

import java.util.List;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Hunter Forge block, rift essence and rift core. Recipes are data-driven, see {@link ForgeRecipes}. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class HunterForge {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KhaosNexus.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);

    public static final DeferredBlock<HunterForgeBlock> FORGE = BLOCKS.register("hunter_forge",
            () -> new HunterForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7)));
    public static final DeferredItem<BlockItem> FORGE_ITEM = ITEMS.registerSimpleBlockItem(FORGE);
    public static final DeferredItem<Item> RIFT_ESSENCE = ITEMS.registerSimpleItem("rift_essence");
    public static final DeferredItem<Item> RIFT_CORE = ITEMS.registerSimpleItem("rift_core", new Item.Properties().rarity(Rarity.RARE));

    private HunterForge() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new ForgeRecipes());
    }

    /** Essence for every cleared gate, cores from C rank up. */
    public static void addRewards(GateRank rank, RandomSource random, List<ItemStack> out) {
        out.add(new ItemStack(RIFT_ESSENCE.get(), 1 + rank.ordinal() + random.nextInt(2)));
        if (rank.ordinal() >= GateRank.C.ordinal() && random.nextFloat() < 0.25F + 0.15F * (rank.ordinal() - 2)) {
            out.add(new ItemStack(RIFT_CORE.get()));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int level = ForgeRecipes.level(stack);
        if (level > 0) {
            event.getToolTip().add(1, Component.translatable("khaosnexus.forge.tooltip.level", level).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.is(FORGE_ITEM.get())) {
            event.getToolTip().add(Component.translatable("khaosnexus.forge.tooltip.use").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("khaosnexus.forge.tooltip.card").withStyle(ChatFormatting.DARK_PURPLE));
        } else if (stack.is(RIFT_ESSENCE.get()) || stack.is(RIFT_CORE.get())) {
            event.getToolTip().add(Component.translatable("khaosnexus.forge.tooltip.material").withStyle(ChatFormatting.GRAY));
        }
    }
}

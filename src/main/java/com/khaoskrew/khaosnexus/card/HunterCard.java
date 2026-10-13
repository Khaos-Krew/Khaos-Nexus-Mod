package com.khaoskrew.khaosnexus.card;

import com.google.common.collect.ImmutableSet;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hunter Card: a player-bound item sold by the Guild Clerk villager. Holding one unlocks the hunter HUD and screens
 * (a per-player flag, so losing the card later changes nothing). A server config turns the requirement off.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class HunterCard {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KhaosNexus.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KhaosNexus.MODID);
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KhaosNexus.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, KhaosNexus.MODID);

    public static final DeferredBlock<Block> GUILD_DESK = BLOCKS.registerSimpleBlock("guild_desk",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> GUILD_DESK_ITEM = ITEMS.registerSimpleBlockItem(GUILD_DESK);
    public static final DeferredItem<HunterCardItem> CARD = ITEMS.register("hunter_card",
            () -> new HunterCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<PoiType, PoiType> GUILD_DESK_POI = POI.register("guild_desk",
            () -> new PoiType(ImmutableSet.copyOf(GUILD_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> GUILD_CLERK = PROFESSIONS.register("guild_clerk",
            () -> new VillagerProfession(KhaosNexus.MODID + ":guild_clerk",
                    holder -> holder.is(GUILD_DESK_POI.getKey()), holder -> holder.is(GUILD_DESK_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    private HunterCard() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        POI.register(bus);
        PROFESSIONS.register(bus);
    }

    private static boolean required() {
        try {
            return Config.HUNTER_CARD_REQUIRED.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when this player may see the hunter HUD and screens. Works on both sides (the profile is synced). */
    public static boolean isUnlocked(Player player) {
        return !required() || player.getData(ModAttachments.HUNTER).cardUnlocked;
    }

    public static void unlock(ServerPlayer player) {
        HunterProfile profile = player.getData(ModAttachments.HUNTER);
        if (!profile.cardUnlocked) {
            profile.cardUnlocked = true;
            HunterRanks.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HunterProfile profile = player.getData(ModAttachments.HUNTER);
            if (!profile.cardUnlocked && profile.hasProgress()) {
                unlock(player);
            }
        }
    }

    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent event) {
        if (event.getType() != GUILD_CLERK.get()) {
            return;
        }
        // The first card is cheap: one emerald. Replacements and guild goods cost more.
        event.getTrades().get(1).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 1),
                new ItemStack(CARD.get()), 12, 2, 0.05F));
        event.getTrades().get(1).add((trader, random) -> new MerchantOffer(new ItemCost(Items.PAPER, 12),
                new ItemStack(Items.EMERALD), 16, 2, 0.05F));
        event.getTrades().get(2).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 6),
                new ItemStack(Items.MAP), 12, 5, 0.05F));
        event.getTrades().get(3).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 12),
                new ItemStack(com.khaoskrew.khaosnexus.registry.ModRegistries.NEXUS_TOKEN.get()), 6, 10, 0.05F));
    }
}

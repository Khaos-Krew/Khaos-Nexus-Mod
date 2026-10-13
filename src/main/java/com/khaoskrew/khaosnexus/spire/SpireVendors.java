package com.khaoskrew.khaosnexus.spire;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Spire town vendors. A vendor file: "name" (shown above the NPC), optional "repair": true (a repair smith instead of
 * trades) and "trades": [{"cost": {item, count, per_floor}, "cost_b": {...}, "result": {item, count, per_floor,
 * potion, enchantments: [{id, level, per_floor, max}]}, "min_floor", "min_rank", "max_uses"}].
 * Counts and enchant levels scale with the floor; trades above the visitor's hunter rank are hidden.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SpireVendors {
    public static final String TAG = "khaosnexus.spire_vendor";
    public static final String REPAIR = "khaosnexus.spire_repair";

    private SpireVendors() {}

    static int scaled(JsonObject o, int floor) {
        return Mth.clamp((int) Math.round(GsonHelper.getAsInt(o, "count", 1) + GsonHelper.getAsDouble(o, "per_floor", 0.0D) * (floor - 1)), 1, 64);
    }

    static Item item(JsonObject o) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(o, "item", "minecraft:air"));
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }

    public static ItemStack result(JsonObject r, int floor, HolderLookup.Provider regs) {
        ItemStack stack = new ItemStack(item(r), scaled(r, floor));
        if (r.has("potion")) {
            Optional<Holder.Reference<Potion>> potion = BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(GsonHelper.getAsString(r, "potion")));
            potion.ifPresent(p -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(p)));
        }
        for (JsonElement el : GsonHelper.getAsJsonArray(r, "enchantments", new com.google.gson.JsonArray())) {
            JsonObject e = el.getAsJsonObject();
            int lvl = (int) Math.floor(GsonHelper.getAsInt(e, "level", 1) + GsonHelper.getAsDouble(e, "per_floor", 0.0D) * (floor - 1));
            lvl = Math.min(lvl, GsonHelper.getAsInt(e, "max", 5));
            final int level = lvl;
            if (level > 0) {
                regs.lookupOrThrow(Registries.ENCHANTMENT)
                        .get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(GsonHelper.getAsString(e, "id"))))
                        .ifPresent(h -> stack.enchant(h, level));
            }
        }
        return stack;
    }

    /** Offers for a vendor at a floor for a visitor of a given rank. */
    public static MerchantOffers offers(JsonObject vendor, int floor, GateRank rank, HolderLookup.Provider regs) {
        MerchantOffers out = new MerchantOffers();
        for (JsonElement el : GsonHelper.getAsJsonArray(vendor, "trades", new com.google.gson.JsonArray())) {
            JsonObject t = el.getAsJsonObject();
            if (floor < GsonHelper.getAsInt(t, "min_floor", 1)
                    || rank.ordinal() < GateRank.byName(GsonHelper.getAsString(t, "min_rank", "E")).ordinal()) {
                continue;
            }
            JsonObject c = GsonHelper.getAsJsonObject(t, "cost");
            ItemStack res = result(GsonHelper.getAsJsonObject(t, "result"), floor, regs);
            if (res.isEmpty() || item(c) == Items.AIR) {
                continue;
            }
            Optional<ItemCost> b = t.has("cost_b")
                    ? Optional.of(new ItemCost(item(GsonHelper.getAsJsonObject(t, "cost_b")), scaled(GsonHelper.getAsJsonObject(t, "cost_b"), floor)))
                    : Optional.empty();
            out.add(new MerchantOffer(new ItemCost(item(c), scaled(c, floor)), b, res, GsonHelper.getAsInt(t, "max_uses", 8), 0, 0.0F));
        }
        return out;
    }

    /** Spawns the vendors for a town, replacing any old ones there. */
    public static void spawn(ServerLevel level, BlockPos town, int floor, GateRank rank) {
        for (WanderingTrader old : level.getEntitiesOfClass(WanderingTrader.class, new AABB(town).inflate(SpireBuilder.TOWN_HALF + 2, 8, SpireBuilder.TOWN_HALF + 2))) {
            if (old.getPersistentData().getBoolean(TAG)) {
                old.discard();
            }
        }
        if (!Config.SPIRE_VENDORS.get()) {
            return;
        }
        var spots = SpireBuilder.vendorSpots(town);
        int i = 0;
        for (var e : JsonPool.SPIRE_VENDORS.all().entrySet()) {
            if (i >= spots.size()) {
                break;
            }
            JsonObject v = e.getValue();
            WanderingTrader npc = EntityType.WANDERING_TRADER.create(level);
            if (npc == null) {
                continue;
            }
            BlockPos at = spots.get(i++);
            npc.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, at.getX() < town.getX() ? -90.0F : 90.0F, 0.0F);
            npc.setNoAi(true);
            npc.setInvulnerable(true);
            npc.setPersistenceRequired();
            npc.setDespawnDelay(0);
            npc.setSilent(true);
            npc.setCustomName(Component.translatable(GsonHelper.getAsString(v, "name", "khaosnexus.vendor.default")));
            npc.setCustomNameVisible(true);
            npc.getPersistentData().putBoolean(TAG, true);
            if (GsonHelper.getAsBoolean(v, "repair", false)) {
                npc.getPersistentData().putBoolean(REPAIR, true);
            } else {
                npc.getOffers().clear();
                npc.getOffers().addAll(offers(v, floor, rank, level.registryAccess()));
            }
            level.addFreshEntity(npc);
        }
    }

    /** Cost to repair, in quarters of durability (rounded up). */
    public static int quarters(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) {
            return 0;
        }
        return Mth.ceil(stack.getDamageValue() * 4.0D / stack.getMaxDamage());
    }

    public static boolean repair(ServerPlayer player, ItemStack stack) {
        int q = quarters(stack);
        if (q == 0) {
            player.displayClientMessage(Component.translatable("khaosnexus.vendor.repair_none"), false);
            return false;
        }
        var cores = com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get();
        int coreCost = q * Config.SPIRE_REPAIR_CORES.get();
        int tokenCost = q * Config.SPIRE_REPAIR_TOKENS.get();
        var inv = player.getInventory();
        if (inv.countItem(cores) >= coreCost) {
            take(player, cores, coreCost);
            player.displayClientMessage(Component.translatable("khaosnexus.vendor.repaired_cores", coreCost), false);
        } else if (inv.countItem(ModRegistries.NEXUS_TOKEN.get()) >= tokenCost) {
            take(player, ModRegistries.NEXUS_TOKEN.get(), tokenCost);
            player.displayClientMessage(Component.translatable("khaosnexus.vendor.repaired_tokens", tokenCost), false);
        } else {
            player.displayClientMessage(Component.translatable("khaosnexus.vendor.repair_cost", coreCost, tokenCost), false);
            return false;
        }
        stack.setDamageValue(0);
        return true;
    }

    private static void take(ServerPlayer player, Item item, int n) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget().getPersistentData().getBoolean(REPAIR)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getEntity() instanceof ServerPlayer sp && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                repair(sp, sp.getMainHandItem());
            }
        }
    }
}

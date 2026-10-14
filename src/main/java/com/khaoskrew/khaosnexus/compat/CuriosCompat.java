package com.khaoskrew.khaosnexus.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.card.HunterCard;
import com.khaoskrew.khaosnexus.card.HunterCardItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Curios: a hunter_card slot (data/khaosnexus/curios). A card worn there counts as carried. Reflection only, so nothing
 * here loads Curios classes when it is absent.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class CuriosCompat {
    private static Method getInventory;
    private static Method findFirst;
    private static Method stackOf;
    private static boolean failed;

    private CuriosCompat() {}

    public static boolean present() {
        return ModList.get().isLoaded("curios");
    }

    /** The Hunter Card in the player's curios slots, or EMPTY. */
    public static ItemStack wornCard(Player player) {
        if (!present() || failed) {
            return ItemStack.EMPTY;
        }
        try {
            if (getInventory == null) {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                getInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                findFirst = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                        .getMethod("findFirstCurio", Predicate.class);
                stackOf = Class.forName("top.theillusivec4.curios.api.SlotResult").getMethod("stack");
            }
            Optional<?> handler = (Optional<?>) getInventory.invoke(null, player);
            if (handler.isEmpty()) {
                return ItemStack.EMPTY;
            }
            Predicate<ItemStack> isCard = s -> s.getItem() instanceof HunterCardItem;
            Optional<?> slot = (Optional<?>) findFirst.invoke(handler.get(), isCard);
            return slot.isPresent() ? (ItemStack) stackOf.invoke(slot.get()) : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            KhaosNexus.LOGGER.warn("Curios card slot unavailable: {}", e.toString());
            return ItemStack.EMPTY;
        }
    }

    /** True when the item is worn in any curios slot (false without Curios). */
    public static boolean wears(ServerPlayer p, net.minecraft.world.item.Item item) {
        if (!present() || failed) return false;
        try {
            return CuriosSlots.wears(p, item);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 100 == 0 && present()) {
            try {
                CuriosSlots.refresh(player);
            } catch (RuntimeException | LinkageError e) {
                KhaosNexus.LOGGER.debug("Curios rank slots skipped: {}", e.toString());
            }
        }
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && present()) {
            ItemStack card = wornCard(player);
            if (!card.isEmpty()) {
                if (HunterCardItem.owner(card) == null) {
                    HunterCardItem.bind(card, player);
                }
                if (player.getUUID().equals(HunterCardItem.owner(card))) {
                    HunterCard.unlock(player);
                }
            }
        }
    }
}

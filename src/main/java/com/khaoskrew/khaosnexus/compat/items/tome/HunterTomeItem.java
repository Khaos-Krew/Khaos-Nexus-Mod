package com.khaoskrew.khaosnexus.compat.items.tome;

import java.util.List;
import java.util.function.Consumer;

import com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

/**
 * Hunter Tome: an Ars Nouveau spell book that evolves with the owner's hunter rank. Only casting classes can open or
 * cast it. Rendered as a normal item (our own per-tier texture), not Ars's GeckoLib book.
 */
public class HunterTomeItem extends SpellBook implements IManaDiscountEquipment {
    public final int tomeTier;

    public HunterTomeItem(int tier) {
        super(new Item.Properties().stacksTo(1).fireResistant()
                        .rarity(tier >= 6 ? Rarity.EPIC : tier >= 4 ? Rarity.RARE : tier >= 2 ? Rarity.UNCOMMON : Rarity.COMMON)
                        .component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(HunterTomes.SLOTS[tier])),
                arsTier(HunterTomes.GLYPH_TIER[tier]));
        this.tomeTier = tier;
    }

    static SpellTier arsTier(int t) {
        return t >= 3 ? SpellTier.THREE : t == 2 ? SpellTier.TWO : SpellTier.ONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!HunterTomes.canUse(player)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("khaosnexus.tome.class_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        if (HunterTomes.canUse(player)) super.onOpenBookMenuKeyPressed(stack, player);
    }

    @Override
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        if (HunterTomes.canUse(player)) super.onRadialKeyPressed(stack, player);
    }

    @Override
    public int getManaDiscount(ItemStack stack) {
        return HunterTomes.DISCOUNT[this.tomeTier];
    }

    /** Use the flat item model (per-tier texture) instead of Ars's GeckoLib spell book renderer. */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        int t = this.tomeTier;
        lines.add(Component.translatable("khaosnexus.tome.tier." + HunterTomes.TIER_IDS[t]).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("khaosnexus.tome.stats", HunterTomes.SLOTS[t], HunterTomes.GLYPH_TIER[t], HunterTomes.DISCOUNT[t]).withStyle(ChatFormatting.GRAY));
        if (HunterTomes.ATTUNE[t] > 0) lines.add(Component.translatable("khaosnexus.tome.attune", HunterTomes.ATTUNE[t]).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("khaosnexus.tome.evolves").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, ctx, lines, flag);
    }
}

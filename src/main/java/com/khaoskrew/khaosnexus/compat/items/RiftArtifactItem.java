package com.khaoskrew.khaosnexus.compat.items;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * Rift artifacts (Artifacts + Curios installed). Plain items: the curio slot comes from data/curios/tags, stat bonuses
 * from the curios:attribute_modifiers component set in the loot file, and the special effect from {@link RiftArtifacts}.
 */
public class RiftArtifactItem extends Item {
    public enum Kind { LANTERN, ANKLET, BAND }

    public final Kind kind;

    public RiftArtifactItem(Kind kind) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(this.getDescriptionId() + ".effect").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

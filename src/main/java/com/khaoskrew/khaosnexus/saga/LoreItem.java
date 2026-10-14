package com.khaoskrew.khaosnexus.saga;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Boss trophy / saga material with translated lore lines (desc.0 .. desc.n-1). */
public class LoreItem extends Item {
    private final int lines;

    public LoreItem(Properties p, int lines) { super(p); this.lines = lines; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        for (int i = 0; i < this.lines; i++) tip.add(Component.translatable(this.getDescriptionId() + ".desc." + i).withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}

package com.khaoskrew.khaosnexus.saga;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

/** Kingsbane: Morvane's greatsword. Netherite-tier, +20% against rift things (HollowThrone.kingRelics); hits sap the target. */
public class Kingsbane extends SwordItem {
    public Kingsbane(Properties p) { super(Tiers.NETHERITE, p); }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        for (int i = 0; i < 3; i++) tip.add(Component.translatable(this.getDescriptionId() + ".desc." + i).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}

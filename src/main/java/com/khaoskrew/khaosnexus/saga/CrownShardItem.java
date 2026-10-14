package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Crown Shard: physical form of the saga currency. Use on a Spire Gate to descend; sneak-use to bank the stack. */
public class CrownShardItem extends Item {
    public CrownShardItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            HunterProfile p = HunterRanks.profile(sp);
            int n = stack.getCount();
            Saga.addShards(p, n);
            stack.shrink(n);
            HunterRanks.sync(sp);
            sp.displayClientMessage(Component.translatable("khaosnexus.shards.deposited", n, Saga.shards(p)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.khaosnexus.crown_shard.desc").withStyle(ChatFormatting.GRAY));
    }
}

package com.khaoskrew.khaosnexus.spire;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SpireGateBlock extends Block {
    public static final MapCodec<SpireGateBlock> CODEC = simpleCodec(SpireGateBlock::new);

    public SpireGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SpireGateBlock> codec() {
        return CODEC;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(com.khaoskrew.khaosnexus.registry.ModRegistries.CROWN_SHARD.get()) || level.dimension() == com.khaoskrew.khaosnexus.saga.Saga.UNDERTHRONE) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer sp && com.khaoskrew.khaosnexus.saga.UnderthroneService.enter(sp) && !sp.isCreative()) {
            stack.shrink(1);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.dimension() == com.khaoskrew.khaosnexus.saga.Saga.UNDERTHRONE) {
                com.khaoskrew.khaosnexus.saga.UnderthroneService.leave(serverPlayer);
            } else if (player.isShiftKeyDown() && com.khaoskrew.khaosnexus.saga.UnderthroneService.floorRouteOpen(serverPlayer)) {
                com.khaoskrew.khaosnexus.saga.UnderthroneService.enter(serverPlayer);
            } else if (level.dimension() == SpireService.DIMENSION) {
                SpireService.leave(serverPlayer);
            } else {
                SpireService.enter(serverPlayer);
            }
        }
        return InteractionResult.CONSUME;
    }
}

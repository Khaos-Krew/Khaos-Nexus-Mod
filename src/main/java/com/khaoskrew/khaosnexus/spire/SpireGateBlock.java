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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.dimension() == SpireService.DIMENSION) {
                SpireService.leave(serverPlayer);
            } else {
                SpireService.enter(serverPlayer);
            }
        }
        return InteractionResult.CONSUME;
    }
}

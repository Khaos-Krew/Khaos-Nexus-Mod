package com.khaoskrew.khaosnexus.raid;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RaidAltarBlock extends Block {
    public static final MapCodec<RaidAltarBlock> CODEC = simpleCodec(RaidAltarBlock::new);

    public RaidAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RaidAltarBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RaidService.start(serverPlayer, pos);
        }
        return InteractionResult.CONSUME;
    }
}

package com.khaoskrew.khaosnexus.raid;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PersonalLootBlock extends BaseEntityBlock {
    public static final MapCodec<PersonalLootBlock> CODEC = simpleCodec(PersonalLootBlock::new);

    public PersonalLootBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PersonalLootBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PersonalLootBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof PersonalLootBlockEntity loot)) {
            return InteractionResult.SUCCESS;
        }
        if (!loot.ownedBy(player)) {
            player.displayClientMessage(Component.translatable("khaosnexus.raid.not_yours"), false);
            return InteractionResult.FAIL;
        }
        loot.give(player);
        level.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }
}

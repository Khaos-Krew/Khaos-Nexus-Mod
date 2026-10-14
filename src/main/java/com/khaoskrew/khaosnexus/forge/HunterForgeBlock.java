package com.khaoskrew.khaosnexus.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click with gear to forge it one level; sneak-right-click to preview the cost. */
public class HunterForgeBlock extends Block {
    public HunterForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp0 && !level.isClientSide && com.khaoskrew.khaosnexus.saga.KingAwakening.tryAwaken(sp0, stack)) {
            ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 30, 0.3D, 0.4D, 0.3D, 0.05D);
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.7F);
            return ItemInteractionResult.CONSUME;
        }
        if (ForgeRecipes.match(stack) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer server)) {
            return ItemInteractionResult.SUCCESS;
        }
        ForgeRecipes.Result result = ForgeRecipes.upgrade(server, stack, player.isShiftKeyDown());
        player.displayClientMessage(result.message(), true);
        if (result.status() == ForgeRecipes.Status.FORGED) {
            ServerLevel sl = (ServerLevel) level;
            sl.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.2F);
            sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 40, 0.3D, 0.2D, 0.3D, 0.4D);
            sl.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 6, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.forge.hint"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

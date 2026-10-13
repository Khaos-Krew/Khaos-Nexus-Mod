package com.khaoskrew.khaosnexus.spire;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Spire level gate: a rift-style portal that lights when the level's boss falls. Walk in or use it to advance. */
public class SpirePortalBlock extends BaseEntityBlock {
    public static final MapCodec<SpirePortalBlock> CODEC = simpleCodec(SpirePortalBlock::new);

    public SpirePortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SpirePortalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpirePortalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SpirePortalBlockEntity portal) {
            SpireService.usePortal(sp, portal);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SpirePortalBlockEntity portal
                && portal.isLit() && sp.getPortalCooldown() == 0) {
            sp.setPortalCooldown(40);
            SpireService.usePortal(sp, portal);
        }
    }
}

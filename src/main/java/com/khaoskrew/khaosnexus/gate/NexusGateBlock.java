package com.khaoskrew.khaosnexus.gate;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class NexusGateBlock extends BaseEntityBlock {
    public static final MapCodec<NexusGateBlock> CODEC = simpleCodec(NexusGateBlock::new);
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");

    public NexusGateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CLOSED, false));
    }

    @Override
    protected MapCodec<NexusGateBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // The whole frame (including this block's stone) is drawn by NexusGateRenderer.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Client-side rift particles: rank-coloured dust drifting off the tear's ragged edges plus portal motes. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(CLOSED) || !(level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate)) {
            return;
        }
        int c = gate.getRank().color();
        org.joml.Vector3f rgb = new org.joml.Vector3f(((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F);
        for (int i = 0; i < 6; i++) {
            double t = random.nextDouble();
            double half = 1.25D * Math.pow(Math.sin(Math.PI * t), 0.8D);
            double x = pos.getX() + 0.5D + (random.nextBoolean() ? half : -half) + random.nextGaussian() * 0.08D;
            double y = pos.getY() + 0.5D + t * 5.0D;
            double z = pos.getZ() + 0.5D + random.nextGaussian() * 0.1D;
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(rgb, 1.2F + random.nextFloat()),
                    x, y, z, 0.0D, 0.02D, 0.0D);
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D + random.nextGaussian() * 0.5D, pos.getY() + 1.0D + random.nextDouble() * 4.5D,
                    pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusGateBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModRegistries.NEXUS_GATE_ENTITY.get(), NexusGateBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(this)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate) {
            gate.assignRank(level.getRandom());
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate) {
            player.displayClientMessage(gate.rankLine("khaosnexus.gate.placed"), false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSpectator() || !(level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(CLOSED) || gate.isClosed()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("khaosnexus.gate.closed"), false);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return gate.activate(player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate) {
            gate.discardWave();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

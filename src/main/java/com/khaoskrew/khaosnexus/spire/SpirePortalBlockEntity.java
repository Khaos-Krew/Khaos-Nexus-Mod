package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SpirePortalBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("animation.nexus_rift.open");
    // Dormant gates keep the open rift shape (dimmed by the renderer) so they read clearly as a gate.
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean lit;
    private GateRank rank = GateRank.E;
    /** Destination: floor and level (level 0 = the floor's town). */
    private int floor = 1;
    private int stage = 1;

    public SpirePortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.SPIRE_PORTAL_ENTITY.get(), pos, state);
    }

    public boolean isLit() {
        return this.lit;
    }

    public GateRank getRank() {
        return this.rank;
    }

    public int targetFloor() {
        return this.floor;
    }

    public int targetLevel() {
        return this.stage;
    }

    public void configure(int floor, int level, GateRank rank, boolean lit) {
        this.floor = floor;
        this.stage = level;
        this.rank = rank;
        this.lit = lit;
        this.changed();
    }

    public void setLit(boolean lit) {
        this.lit = lit;
        this.changed();
    }

    private void changed() {
        this.setChanged();
        if (this.getLevel() != null && !this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Lit", this.lit);
        tag.putString("Rank", this.rank.name());
        tag.putInt("Floor", this.floor);
        tag.putInt("Level", this.stage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lit = tag.getBoolean("Lit");
        this.rank = GateRank.byName(tag.getString("Rank"));
        this.floor = Math.max(1, tag.getInt("Floor"));
        this.stage = tag.getInt("Level");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rift", 10, state -> state.setAndContinue(OPEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

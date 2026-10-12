package com.khaoskrew.khaosnexus.gate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.hunter.HunterAccess;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NexusGateBlockEntity extends BlockEntity {
    private GateRank rank = GateRank.E;
    private boolean rankAssigned;
    private boolean active;
    private boolean closed;
    private int waveSize;
    @Nullable
    private UUID activator;
    private final List<UUID> waveMobs = new ArrayList<>();
    private final List<BlockPos> waveAnchors = new ArrayList<>();
    @Nullable
    private ServerBossEvent bossEvent;

    public NexusGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.NEXUS_GATE_ENTITY.get(), pos, state);
    }

    public GateRank getRank() {
        return this.rank;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void setRank(GateRank rank) {
        this.rank = rank;
        this.rankAssigned = true;
        this.setChanged();
        this.sync();
    }

    public void assignRank(RandomSource random) {
        if (this.rankAssigned) {
            return;
        }
        this.rank = GateRank.roll(random);
        this.rankAssigned = true;
        this.setChanged();
        this.sync();
    }

    public InteractionResult activate(Player player) {
        if (!(this.level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (this.closed || this.getBlockState().getValue(NexusGateBlock.CLOSED)) {
            player.displayClientMessage(Component.translatable("khaosnexus.gate.closed"), false);
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer && !HunterAccess.canOpen(serverPlayer, this.rank)) {
            player.displayClientMessage(Component.translatable(
                    "khaosnexus.gate.rank_locked",
                    Component.translatable(this.rank.translationKey()),
                    Component.translatable(HunterRanks.rankOf(serverPlayer).translationKey())), false);
            return InteractionResult.FAIL;
        }
        if (this.active) {
            int living = this.livingCount(server);
            this.updateBossBar(server, living);
            player.displayClientMessage(Component.translatable("khaosnexus.gate.remaining", this.rankLabel(), living), false);
            return InteractionResult.SUCCESS;
        }
        if (!GateArena.spawnWave(server, this)) {
            player.displayClientMessage(Component.translatable("khaosnexus.gate.spawn_failed"), false);
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            this.activator = serverPlayer.getUUID();
        }
        this.active = true;
        this.waveSize = this.waveMobs.size();
        this.setChanged();
        this.sync();
        this.updateBossBar(server, this.waveSize);
        GateArena.announce(server, this.getBlockPos(), this.rankLine("khaosnexus.gate.activated"), false);
        return InteractionResult.SUCCESS;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusGateBlockEntity gate) {
        if (level instanceof ServerLevel server) {
            gate.tickServer(server);
        }
    }

    private void tickServer(ServerLevel level) {
        if (!this.active || this.closed) {
            this.hideBossBar();
            return;
        }
        if (this.waveMobs.isEmpty() || level.getGameTime() % 10L != 0L) {
            return;
        }
        boolean pending = false;
        boolean changed = false;
        for (int i = this.waveMobs.size() - 1; i >= 0; i--) {
            UUID id = this.waveMobs.get(i);
            BlockPos anchor = i < this.waveAnchors.size() ? this.waveAnchors.get(i) : this.getBlockPos();
            Entity entity = level.getEntity(id);
            if (entity == null) {
                // A missing entity in an unloaded chunk is still out there. Only forget it once that chunk is loaded.
                if (!level.isLoaded(anchor)) {
                    pending = true;
                    continue;
                }
                this.removeMob(i);
                changed = true;
                continue;
            }
            if (!entity.isAlive()) {
                this.removeMob(i);
                changed = true;
                continue;
            }
            pending = true;
            GateArena.leash(entity, anchor);
        }
        if (changed) {
            this.setChanged();
        }
        int living = this.livingCount(level);
        this.updateBossBar(level, living);
        if (!pending) {
            this.finishWave(level);
        }
    }

    public void rememberMob(UUID id, BlockPos anchor) {
        this.waveMobs.add(id);
        this.waveAnchors.add(anchor);
    }

    public void discardWave() {
        if (!(this.level instanceof ServerLevel server)) {
            this.waveMobs.clear();
            this.waveAnchors.clear();
            this.active = false;
            return;
        }
        for (UUID id : this.waveMobs) {
            Entity entity = server.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
        this.waveMobs.clear();
        this.waveAnchors.clear();
        this.active = false;
        this.waveSize = 0;
        this.hideBossBar();
        this.setChanged();
    }

    private void finishWave(ServerLevel level) {
        GateRank cleared = this.rank;
        BlockPos pos = this.getBlockPos();
        this.active = false;
        this.closed = true;
        this.waveMobs.clear();
        this.waveAnchors.clear();
        this.waveSize = 0;
        this.hideBossBar();
        this.setChanged();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof NexusGateBlock && !state.getValue(NexusGateBlock.CLOSED)) {
            level.setBlock(pos, state.setValue(NexusGateBlock.CLOSED, true), Block.UPDATE_ALL);
        }
        this.sync();
        GateArena.dropReward(level, pos, cleared);
        GateArena.announce(level, pos, Component.translatable("khaosnexus.gate.cleared", Component.translatable(cleared.translationKey())), false);
        HunterRanks.onGateCleared(level, pos, cleared, this.activator);
    }

    public Component rankLine(String key) {
        return Component.translatable(key, this.rankLabel());
    }

    private Component rankLabel() {
        return Component.translatable(this.rank.translationKey());
    }

    private int livingCount(ServerLevel level) {
        int living = 0;
        for (UUID id : this.waveMobs) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                living++;
            }
        }
        return living;
    }

    private void removeMob(int index) {
        this.waveMobs.remove(index);
        if (index < this.waveAnchors.size()) {
            this.waveAnchors.remove(index);
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Rank", this.rank.name());
        tag.putBoolean("RankAssigned", this.rankAssigned);
        tag.putBoolean("Active", this.active);
        tag.putBoolean("Closed", this.closed);
        tag.putInt("WaveSize", this.waveSize);
        if (this.activator != null) {
            tag.putString("Activator", this.activator.toString());
        }
        ListTag ids = new ListTag();
        long[] anchors = new long[this.waveMobs.size()];
        for (int i = 0; i < this.waveMobs.size(); i++) {
            ids.add(StringTag.valueOf(this.waveMobs.get(i).toString()));
            BlockPos anchor = i < this.waveAnchors.size() ? this.waveAnchors.get(i) : this.getBlockPos();
            anchors[i] = anchor.asLong();
        }
        tag.put("WaveMobs", ids);
        tag.putLongArray("WaveAnchors", anchors);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.rank = GateRank.byName(tag.getString("Rank"));
        this.rankAssigned = tag.getBoolean("RankAssigned");
        this.active = tag.getBoolean("Active");
        this.closed = tag.getBoolean("Closed");
        this.waveSize = tag.getInt("WaveSize");
        this.activator = null;
        if (tag.contains("Activator")) {
            try {
                this.activator = UUID.fromString(tag.getString("Activator"));
            } catch (IllegalArgumentException ignored) {
                this.activator = null;
            }
        }
        this.waveMobs.clear();
        this.waveAnchors.clear();
        ListTag ids = tag.getList("WaveMobs", Tag.TAG_STRING);
        long[] anchors = tag.getLongArray("WaveAnchors");
        for (int i = 0; i < ids.size(); i++) {
            try {
                this.waveMobs.add(UUID.fromString(ids.getString(i)));
                this.waveAnchors.add(i < anchors.length ? BlockPos.of(anchors[i]) : this.getBlockPos());
            } catch (IllegalArgumentException ignored) {
                // Skip a corrupt id rather than failing the whole chunk load.
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.hideBossBar();
    }

    private void updateBossBar(ServerLevel level, int living) {
        int total = Math.max(this.waveSize, 1);
        ServerBossEvent bar = this.bossEvent();
        bar.setName(Component.translatable("khaosnexus.gate.bossbar", this.rankLabel(), living));
        bar.setColor(this.rank.barColor());
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, living / (float) total)));
        Vec3 center = Vec3.atCenterOf(this.getBlockPos());
        Set<ServerPlayer> keep = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= 48.0D * 48.0D) {
                bar.addPlayer(player);
                keep.add(player);
            }
        }
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!keep.contains(player)) {
                bar.removePlayer(player);
            }
        }
    }

    private ServerBossEvent bossEvent() {
        if (this.bossEvent == null) {
            this.bossEvent = new ServerBossEvent(
                    Component.translatable("khaosnexus.gate.bossbar", this.rankLabel(), this.waveSize),
                    this.rank.barColor(),
                    BossEvent.BossBarOverlay.PROGRESS);
            this.bossEvent.setProgress(1.0F);
        }
        return this.bossEvent;
    }

    private void hideBossBar() {
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }
}

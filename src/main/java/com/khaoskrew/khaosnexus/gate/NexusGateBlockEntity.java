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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NexusGateBlockEntity extends BlockEntity implements software.bernie.geckolib.animatable.GeoBlockEntity {
    private static final software.bernie.geckolib.animation.RawAnimation OPEN =
            software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.nexus_rift.open");
    private static final software.bernie.geckolib.animation.RawAnimation CLOSED_ANIM =
            software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.nexus_rift.closed");
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
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
    // Dungeon instance for this gate, when gate dungeons are on. Mobs then live in khaosnexus:gate_dungeon.
    private boolean inDungeon;
    @Nullable
    private BlockPos dungeonSpawn;
    private float dungeonYaw = 180.0F;
    @Nullable
    private net.minecraft.world.level.levelgen.structure.BoundingBox dungeonBox;
    private int dungeonSlot = -1;
    private long dungeonGen;
    private long lastOccupied;

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
        this.expireStaleDungeon(server);
        if (this.active && this.inDungeon && player instanceof ServerPlayer serverPlayer) {
            ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(server.getServer());
            if (dungeon != null && this.dungeonSpawn != null) {
                com.khaoskrew.khaosnexus.dungeon.DungeonService.sendIn(serverPlayer, dungeon, this.dungeonSpawn, this.dungeonYaw, server, this.getBlockPos());
                return InteractionResult.SUCCESS;
            }
        }
        if (!this.active && com.khaoskrew.khaosnexus.Config.GATE_DUNGEONS.get() && player instanceof ServerPlayer serverPlayer
                && com.khaoskrew.khaosnexus.dungeon.DungeonService.level(server.getServer()) != null) {
            return this.openDungeon(server, serverPlayer);
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

    private InteractionResult openDungeon(ServerLevel server, ServerPlayer player) {
        ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(server.getServer());
        if (dungeon == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.gate.spawn_failed"), false);
            return InteractionResult.FAIL;
        }
        var lease = com.khaoskrew.khaosnexus.dungeon.DungeonService.allocate(dungeon);
        var layout = com.khaoskrew.khaosnexus.dungeon.DungeonService.build(dungeon, this.rank, lease.origin(), server.getRandom());
        if (layout == null) {
            com.khaoskrew.khaosnexus.dungeon.DungeonService.release(dungeon, lease, null);
            player.displayClientMessage(Component.translatable("khaosnexus.gate.spawn_failed"), false);
            return InteractionResult.FAIL;
        }
        var boss = com.khaoskrew.khaosnexus.dungeon.DungeonService.populate(dungeon, this.rank, layout, server.getRandom(), lease);
        this.dungeonSlot = lease.slot();
        this.dungeonGen = lease.gen();
        this.lastOccupied = server.getGameTime();
        this.waveMobs.clear();
        this.waveAnchors.clear();
        if (boss != null) {
            this.rememberMob(boss.getUUID(), layout.boss());
        }
        this.inDungeon = true;
        this.dungeonSpawn = layout.spawn();
        this.dungeonYaw = layout.yaw();
        this.dungeonBox = layout.box();
        this.activator = player.getUUID();
        this.active = true;
        this.waveSize = Math.max(1, this.waveMobs.size());
        // Keep the gate ticking while everyone is inside the dungeon.
        ChunkPos chunk = new ChunkPos(this.getBlockPos());
        server.setChunkForced(chunk.x, chunk.z, true);
        this.setChanged();
        this.sync();
        GateArena.announce(server, this.getBlockPos(), this.rankLine("khaosnexus.gate.activated"), false);
        net.minecraft.world.phys.Vec3 here = net.minecraft.world.phys.Vec3.atCenterOf(this.getBlockPos());
        for (ServerPlayer member : com.khaoskrew.khaosnexus.party.PartyService.onlineMembers(player)) {
            if (member == player || (member.level() == server && member.distanceToSqr(here) <= 32.0D * 32.0D)) {
                com.khaoskrew.khaosnexus.dungeon.DungeonService.sendIn(member, dungeon, layout.spawn(), layout.yaw(), server, this.getBlockPos());
                member.displayClientMessage(Component.translatable("khaosnexus.dungeon.entered", this.rankLabel()), false);
            }
        }
        if (boss == null) {
            // Nothing to fight: clear right away rather than trapping the party.
            this.finishWave(server);
        }
        return InteractionResult.SUCCESS;
    }

    private ServerLevel mobLevel(ServerLevel level) {
        if (this.inDungeon) {
            ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(level.getServer());
            if (dungeon != null) {
                return dungeon;
            }
        }
        return level;
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
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        if (this.inDungeon && this.tendDungeon(level)) {
            return;
        }
        if (this.waveMobs.isEmpty()) {
            return;
        }
        ServerLevel gateLevel = level;
        level = this.mobLevel(level);
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
            this.finishWave(gateLevel);
        }
    }

    /**
     * Keeps this gate's dungeon lease alive and collapses it when the party has been gone too long.
     * True when the dungeon was collapsed (the gate is then open and inactive again).
     */
    private boolean tendDungeon(ServerLevel level) {
        if (this.expireStaleDungeon(level)) {
            return true;
        }
        ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(level.getServer());
        if (dungeon == null || this.dungeonSlot < 0) {
            return false;
        }
        long now = level.getGameTime();
        if (this.lastOccupied <= 0L) {
            this.lastOccupied = now;
        }
        com.khaoskrew.khaosnexus.dungeon.DungeonInstances.get(dungeon).touch(this.dungeonSlot, this.dungeonGen, now);
        var wide = this.dungeonBox.inflatedBy(16);
        if (dungeon.players().stream().anyMatch(p -> wide.isInside(p.blockPosition()))) {
            this.lastOccupied = now;
            return false;
        }
        int minutes = com.khaoskrew.khaosnexus.Config.DUNGEON_ABANDON_MINUTES.get();
        if (minutes > 0 && now - this.lastOccupied > minutes * 1200L) {
            GateArena.announce(level, this.getBlockPos(), this.rankLine("khaosnexus.dungeon.collapsed"), false);
            this.discardWave();
            this.sync();
            return true;
        }
        return false;
    }

    /** A gate whose instance was reclaimed (or that predates instance tracking) forgets it and can open fresh. */
    private boolean expireStaleDungeon(ServerLevel level) {
        if (!this.inDungeon) {
            return false;
        }
        ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(level.getServer());
        if (dungeon != null && this.dungeonSlot >= 0
                && com.khaoskrew.khaosnexus.dungeon.DungeonInstances.get(dungeon).isLive(this.dungeonSlot, this.dungeonGen)) {
            return false;
        }
        this.discardWave();
        this.sync();
        return true;
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
        ServerLevel mobs = this.mobLevel(server);
        for (UUID id : this.waveMobs) {
            Entity entity = mobs.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
        this.leaveDungeon(server);
        this.waveMobs.clear();
        this.waveAnchors.clear();
        this.active = false;
        this.waveSize = 0;
        this.hideBossBar();
        this.setChanged();
    }

    private void leaveDungeon(ServerLevel level) {
        if (!this.inDungeon) {
            return;
        }
        ServerLevel dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(level.getServer());
        if (dungeon != null && this.dungeonBox != null) {
            com.khaoskrew.khaosnexus.dungeon.DungeonService.evacuate(dungeon, this.dungeonBox);
            if (this.dungeonSlot >= 0) {
                com.khaoskrew.khaosnexus.dungeon.DungeonInstances.get(dungeon).release(this.dungeonSlot, this.dungeonGen, this.dungeonBox);
            }
        }
        ChunkPos chunk = new ChunkPos(this.getBlockPos());
        level.setChunkForced(chunk.x, chunk.z, false);
        this.inDungeon = false;
        this.dungeonSpawn = null;
        this.dungeonBox = null;
        this.dungeonSlot = -1;
    }

    private void finishWave(ServerLevel level) {
        this.leaveDungeon(level);
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
        if (this.inDungeon && this.dungeonSpawn != null && this.dungeonBox != null) {
            tag.putLong("DungeonSpawn", this.dungeonSpawn.asLong());
            tag.putFloat("DungeonYaw", this.dungeonYaw);
            var b = this.dungeonBox;
            tag.putIntArray("DungeonBox", new int[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            tag.putInt("DungeonSlot", this.dungeonSlot);
            tag.putLong("DungeonGen", this.dungeonGen);
            tag.putLong("DungeonSeen", this.lastOccupied);
        }
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
        this.inDungeon = tag.contains("DungeonSpawn");
        this.dungeonSpawn = this.inDungeon ? BlockPos.of(tag.getLong("DungeonSpawn")) : null;
        this.dungeonYaw = tag.contains("DungeonYaw") ? tag.getFloat("DungeonYaw") : 180.0F;
        int[] box = tag.getIntArray("DungeonBox");
        this.dungeonBox = box.length == 6 ? new net.minecraft.world.level.levelgen.structure.BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]) : null;
        this.inDungeon = this.inDungeon && this.dungeonBox != null;
        this.dungeonSlot = tag.contains("DungeonSlot") ? tag.getInt("DungeonSlot") : -1;
        this.dungeonGen = tag.getLong("DungeonGen");
        this.lastOccupied = tag.getLong("DungeonSeen");
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
            boolean inside = this.inDungeon && this.dungeonBox != null && this.dungeonBox.inflatedBy(16).isInside(player.blockPosition());
            if (inside || (!this.inDungeon && player.distanceToSqr(center) <= 48.0D * 48.0D)) {
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

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "rift", 10,
                state -> state.setAndContinue(this.isClosed() ? CLOSED_ANIM : OPEN)));
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

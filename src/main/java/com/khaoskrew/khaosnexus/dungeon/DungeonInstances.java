package com.khaoskrew.khaosnexus.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Tracks which regions of khaosnexus:gate_dungeon hold a live instance. A finished, abandoned or orphaned instance
 * is wiped a slice per tick and its slot goes back on the free list, so the dimension stops growing.
 * Each lease carries a generation; mobs and gates remember theirs, so anything from an older instance in a reused
 * slot is dropped instead of leaking into the new one.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class DungeonInstances extends SavedData {
    public static final String GEN_KEY = "KhaosDungeonGen";
    static final int SPACING = 1024;
    static final int ROW = 64;
    private static final String NAME = "khaosnexus_dungeon_slots";

    public record Lease(int slot, long gen, BlockPos origin) {}

    private static final class Live {
        long gen;
        long touched;
        @Nullable BoundingBox box;
    }

    private static final class Reset {
        int slot;
        BoundingBox box;
        long cursor;
        boolean entitiesCleared;
    }

    private int next;
    private long genCounter;
    private final TreeSet<Integer> free = new TreeSet<>();
    private final Map<Integer, Live> live = new HashMap<>();
    private final Deque<Reset> resets = new ArrayDeque<>();

    public static DungeonInstances get(ServerLevel dungeon) {
        return dungeon.getDataStorage().computeIfAbsent(new SavedData.Factory<>(DungeonInstances::new, DungeonInstances::load, null), NAME);
    }

    public static BlockPos originOf(int slot) {
        return new BlockPos((slot % ROW) * SPACING + 512, 64, (slot / ROW) * SPACING + 512);
    }

    /** Slot whose region contains this position, or -1 outside the grid. */
    public static int slotAt(BlockPos pos) {
        int sx = Math.floorDiv(pos.getX(), SPACING);
        int sz = Math.floorDiv(pos.getZ(), SPACING);
        return sx < 0 || sx >= ROW || sz < 0 ? -1 : sz * ROW + sx;
    }

    /** Lowest free slot, else a fresh one. */
    public Lease lease(long now) {
        Integer slot = this.free.pollFirst();
        int s = slot != null ? slot : this.next++;
        Live l = new Live();
        l.gen = ++this.genCounter;
        l.touched = now;
        this.live.put(s, l);
        this.setDirty();
        return new Lease(s, l.gen, originOf(s));
    }

    public void setBox(int slot, long gen, BoundingBox box) {
        Live l = this.live.get(slot);
        if (l != null && l.gen == gen) {
            l.box = box;
            this.setDirty();
        }
    }

    public boolean isLive(int slot, long gen) {
        Live l = this.live.get(slot);
        return l != null && l.gen == gen;
    }

    public void touch(int slot, long gen, long now) {
        Live l = this.live.get(slot);
        if (l != null && l.gen == gen && now - l.touched >= 200L) {
            l.touched = now;
            this.setDirty();
        }
    }

    /** Ends a lease and queues its region for wiping. Safe to call twice. */
    public void release(int slot, long gen, @Nullable BoundingBox fallback) {
        Live l = this.live.get(slot);
        if (l == null || l.gen != gen) {
            return;
        }
        this.live.remove(slot);
        Reset r = new Reset();
        r.slot = slot;
        BoundingBox box = l.box != null ? l.box : fallback;
        if (box == null) {
            // Unknown footprint: wipe the area any layout can reach around the origin.
            BlockPos o = originOf(slot);
            box = new BoundingBox(o.getX() - 200, o.getY() - 16, o.getZ() - 200, o.getX() + 200, o.getY() + 48, o.getZ() + 200);
        }
        r.box = box.inflatedBy(2);
        this.resets.add(r);
        this.setDirty();
        KhaosNexus.LOGGER.info("Gate dungeon slot {} released, wiping {} blocks", slot, volume(r.box));
    }

    public boolean isFree(int slot) {
        return this.free.contains(slot);
    }

    public int liveCount() {
        return this.live.size();
    }

    public int freeCount() {
        return this.free.size();
    }

    public int pendingResets() {
        return this.resets.size();
    }

    public int highWater() {
        return this.next;
    }

    private static long volume(BoundingBox b) {
        return (long) b.getXSpan() * b.getYSpan() * b.getZSpan();
    }

    /** Runs one tick of reset work; also reaps leases nobody has touched for a long time (gate gone). */
    void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 1200L == 0L) {
            long minutes = Math.max(30, Config.DUNGEON_ABANDON_MINUTES.get());
            long limit = minutes * 2L * 1200L;
            for (Map.Entry<Integer, Live> e : List.copyOf(this.live.entrySet())) {
                Live l = e.getValue();
                if (now - l.touched > limit && !occupied(level, l.box)) {
                    KhaosNexus.LOGGER.info("Gate dungeon slot {} has no gate tending it, reclaiming", e.getKey());
                    this.release(e.getKey(), l.gen, null);
                }
            }
        }
        Reset r = this.resets.peekFirst();
        if (r == null) {
            return;
        }
        if (occupied(level, r.box)) {
            // Somebody wandered in (or was left behind): send them out before wiping.
            DungeonService.evacuate(level, r.box);
        }
        if (!r.entitiesCleared) {
            discardEntities(level, r.box);
            r.entitiesCleared = true;
        }
        int budget = Config.DUNGEON_RESET_BLOCKS_PER_TICK.get();
        long total = volume(r.box);
        int xs = r.box.getXSpan();
        int zs = r.box.getZSpan();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        // Walk column-major (y fastest) so each chunk is visited once.
        while (budget-- > 0 && r.cursor < total) {
            long i = r.cursor++;
            int y = (int) (i % r.box.getYSpan());
            long col = i / r.box.getYSpan();
            int z = (int) (col % zs);
            int x = (int) (col / zs);
            c.set(r.box.minX() + x, r.box.minY() + y, r.box.minZ() + z);
            BlockState state = level.getBlockState(c);
            if (!state.isAir()) {
                level.setBlock(c, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        if (xs > 0 && r.cursor >= total) {
            discardEntities(level, r.box);
            this.resets.pollFirst();
            this.free.add(r.slot);
            KhaosNexus.LOGGER.info("Gate dungeon slot {} wiped and free for reuse", r.slot);
        }
        this.setDirty();
    }

    private static boolean occupied(ServerLevel level, @Nullable BoundingBox box) {
        if (box == null) {
            return false;
        }
        BoundingBox wide = box.inflatedBy(16);
        for (Player p : level.players()) {
            if (wide.isInside(p.blockPosition())) {
                return true;
            }
        }
        return false;
    }

    private static void discardEntities(ServerLevel level, BoundingBox box) {
        AABB area = AABB.of(box).inflate(16.0D);
        for (Entity e : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            e.discard();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = DungeonService.level(event.getServer());
        if (level != null) {
            get(level).tick(level);
        }
    }

    /** A mob saved in an old instance whose slot has since been wiped or reused never comes back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != DungeonRules.GATE_DUNGEON) {
            return;
        }
        CompoundTag data = event.getEntity().getPersistentData();
        if (!data.contains(GEN_KEY)) {
            return;
        }
        int slot = slotAt(event.getEntity().blockPosition());
        if (slot < 0 || !get(level).isLive(slot, data.getLong(GEN_KEY))) {
            event.setCanceled(true);
        }
    }

    static DungeonInstances load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonInstances d = new DungeonInstances();
        d.next = tag.getInt("Next");
        d.genCounter = tag.getLong("Gen");
        for (int s : tag.getIntArray("Free")) {
            d.free.add(s);
        }
        boolean legacy = !tag.contains("Live");
        for (Tag t : tag.getList("Live", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Live l = new Live();
            l.gen = c.getLong("Gen");
            l.touched = c.getLong("Touched");
            l.box = box(c.getIntArray("Box"));
            d.live.put(c.getInt("Slot"), l);
        }
        for (Tag t : tag.getList("Resets", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            BoundingBox b = box(c.getIntArray("Box"));
            if (b == null) {
                continue;
            }
            Reset r = new Reset();
            r.slot = c.getInt("Slot");
            r.box = b;
            r.cursor = c.getLong("Cursor");
            d.resets.add(r);
        }
        if (legacy) {
            // Worlds from 0.0.2 only stored a counter. Treat every old slot as an orphan: the reaper reclaims each
            // one once nobody is near it, and gates from that version reopen as new.
            for (int s = 0; s < d.next; s++) {
                Live l = new Live();
                l.gen = ++d.genCounter;
                l.touched = Long.MIN_VALUE / 4;
                d.live.put(s, l);
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Next", this.next);
        tag.putLong("Gen", this.genCounter);
        tag.putIntArray("Free", new ArrayList<>(this.free));
        ListTag lives = new ListTag();
        this.live.forEach((slot, l) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("Slot", slot);
            c.putLong("Gen", l.gen);
            c.putLong("Touched", l.touched);
            if (l.box != null) {
                c.putIntArray("Box", ints(l.box));
            }
            lives.add(c);
        });
        tag.put("Live", lives);
        ListTag rs = new ListTag();
        for (Reset r : this.resets) {
            CompoundTag c = new CompoundTag();
            c.putInt("Slot", r.slot);
            c.putIntArray("Box", ints(r.box));
            c.putLong("Cursor", r.cursor);
            rs.add(c);
        }
        tag.put("Resets", rs);
        return tag;
    }

    static int[] ints(BoundingBox b) {
        return new int[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()};
    }

    @Nullable
    static BoundingBox box(int[] a) {
        return a.length == 6 ? new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5]) : null;
    }
}

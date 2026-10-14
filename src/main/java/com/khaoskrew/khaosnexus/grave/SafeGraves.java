package com.khaoskrew.khaosnexus.grave;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.dungeon.DungeonRules;
import com.khaoskrew.khaosnexus.dungeon.DungeonService;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.spire.SpireService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 1.1.1 safe graves: a hunter who dies inside a gate dungeon or the Spire never leaves their items inside.
 * Drops are captured at HIGHEST priority (before any grave mod sees them, so Gravestone Mod/YIGD make no grave
 * inside) and placed in a locked grave chest at the gate entrance or Spire entry point. No safe spot means the
 * items go to a per-player Recovery Chest claimed with /khaos recover (at the Spire town or anywhere). Items are
 * never deleted: until placed, they are kept in the player's persistent data, so a crash mid-death loses nothing.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SafeGraves {
    public static final String SPIRE_ENTRY = "khaosnexus_spire_entry";
    public static final String PENDING = "khaosnexus_pending_grave";
    public static final String GRAVE_OWNER = "khaosnexus_grave_owner";
    public static final String GRAVE_PARTY = "khaosnexus_grave_party";
    public static final String GRAVE_OPEN_AT = "khaosnexus_grave_open_at";

    private SafeGraves() {}

    /** Anchor = where the grave goes. {@code null} when the player has none (fallback). */
    public record Anchor(ResourceKey<Level> dim, BlockPos pos) {}

    public static void saveSpireEntry(ServerPlayer player) {
        if (player.level().dimension() == SpireService.DIMENSION) {
            return; // floor-to-floor moves keep the original entry point
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Dim", player.level().dimension().location().toString());
        tag.putLong("Pos", player.blockPosition().asLong());
        tag.putLong("Time", player.level().getGameTime());
        player.getPersistentData().put(SPIRE_ENTRY, tag);
    }

    @Nullable
    public static Anchor anchorOf(ServerPlayer player) {
        String key = player.level().dimension() == SpireService.DIMENSION ? SPIRE_ENTRY : DungeonService.RETURN;
        CompoundTag tag = player.getPersistentData().getCompound(key);
        if (!tag.contains("Dim")) {
            return null;
        }
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
        if (dim == null) {
            return null;
        }
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        // gate entrances: the player stepped out 2 south of the gate block, start the search there
        return new Anchor(ResourceKey.create(Registries.DIMENSION, dim), key.equals(DungeonService.RETURN) ? pos.south(2) : pos);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!Config.SAFE_GRAVES.get() || !(event.getEntity() instanceof ServerPlayer player) || !DungeonRules.isDungeon(player.level())) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (!drop.getItem().isEmpty()) {
                items.add(drop.getItem().copy());
            }
        }
        if (items.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        HolderLookup.Provider reg = player.registryAccess();
        player.getPersistentData().put(PENDING, write(items, reg)); // crash safety
        place(player.server, player.getUUID(), player.getName().getString(), anchorOf(player), partyOf(player), items);
        player.getPersistentData().remove(PENDING);
    }

    /** Re-place items that were captured but never placed (crash between capture and placement). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(PENDING)) {
            List<ItemStack> items = read(player.getPersistentData().getList(PENDING, Tag.TAG_COMPOUND), player.registryAccess());
            RecoveryData.get(player.server).add(player.getUUID(), items);
            player.getPersistentData().remove(PENDING);
            player.displayClientMessage(Component.translatable("khaosnexus.grave.recovery"), false);
        }
    }

    static List<UUID> partyOf(ServerPlayer player) {
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer m : PartyService.onlineMembers(player)) {
            ids.add(m.getUUID());
        }
        return ids;
    }

    /** Places the items; returns the grave position, or null when they went to the recovery chest. */
    @Nullable
    public static BlockPos place(MinecraftServer server, UUID owner, String name, @Nullable Anchor anchor, List<UUID> party, List<ItemStack> items) {
        ServerLevel level = anchor == null ? null : server.getLevel(anchor.dim());
        BlockPos spot = level == null ? null : findSafeSpot(level, anchor.pos(), Config.SAFE_GRAVE_RADIUS.get());
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (spot == null) {
            RecoveryData.get(server).add(owner, items);
            if (online != null) {
                online.displayClientMessage(Component.translatable("khaosnexus.grave.recovery"), false);
            }
            return null;
        }
        long openAt = level.getGameTime() + Config.SAFE_GRAVE_PARTY_MINUTES.get() * 1200L;
        List<ItemStack> left = new ArrayList<>(items);
        BlockPos at = spot;
        while (!left.isEmpty()) {
            level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
            if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
                break;
            }
            for (int i = 0; i < chest.getContainerSize() && !left.isEmpty(); i++) {
                chest.setItem(i, left.remove(0));
            }
            CompoundTag data = chest.getPersistentData();
            data.putUUID(GRAVE_OWNER, owner);
            ListTag p = new ListTag();
            for (UUID id : party) {
                CompoundTag t = new CompoundTag();
                t.putUUID("Id", id);
                p.add(t);
            }
            data.put(GRAVE_PARTY, p);
            data.putLong(GRAVE_OPEN_AT, openAt);
            chest.setChanged();
            if (!left.isEmpty()) {
                BlockPos next = findSafeSpot(level, at, Config.SAFE_GRAVE_RADIUS.get());
                if (next == null) {
                    RecoveryData.get(server).add(owner, left);
                    break;
                }
                at = next;
            }
        }
        if (online != null) {
            online.displayClientMessage(Component.translatable("khaosnexus.grave.placed",
                    spot.getX(), spot.getY(), spot.getZ(), anchor.dim().location().toString()), false);
        }
        return spot;
    }

    /** Nearest spot within radius with a solid floor and 2 free blocks: no liquid, no portals, not near the void. */
    @Nullable
    public static BlockPos findSafeSpot(ServerLevel level, BlockPos center, int radius) {
        level.getChunk(center); // loads the chunk if needed
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    c.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    double d = dx * dx + dz * dz + dy * dy * 2.0D;
                    if (d < bestDist && safe(level, c)) {
                        best = c.immutable();
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    static boolean safe(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() + 5 || pos.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState floor = level.getBlockState(pos.below());
        if (!floor.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) || floor.is(Blocks.CHEST)) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            BlockState s = level.getBlockState(pos.above(i));
            if (!s.isAir() || !s.getFluidState().isEmpty()) {
                return false;
            }
        }
        return level.getFluidState(pos.below()).isEmpty();
    }

    public static boolean mayOpen(Player player, ChestBlockEntity chest) {
        CompoundTag data = chest.getPersistentData();
        if (!data.hasUUID(GRAVE_OWNER) || player.isCreative()) {
            return true;
        }
        if (player.getUUID().equals(data.getUUID(GRAVE_OWNER)) || chest.getLevel().getGameTime() >= data.getLong(GRAVE_OPEN_AT)) {
            return true;
        }
        for (Tag t : data.getList(GRAVE_PARTY, Tag.TAG_COMPOUND)) {
            if (((CompoundTag) t).getUUID("Id").equals(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof ChestBlockEntity chest && !mayOpen(event.getEntity(), chest)) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("khaosnexus.grave.locked"), true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof ChestBlockEntity chest && !mayOpen(event.getPlayer(), chest)) {
            event.setCanceled(true);
        }
    }

    public static ListTag write(List<ItemStack> items, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                list.add(s.save(reg));
            }
        }
        return list;
    }

    public static List<ItemStack> read(ListTag list, HolderLookup.Provider reg) {
        List<ItemStack> out = new ArrayList<>();
        for (Tag t : list) {
            ItemStack.parse(reg, t).ifPresent(out::add);
        }
        return out;
    }

    /** /khaos recover: hands back everything in the player's recovery chest (overflow drops at their feet). */
    public static int claim(ServerPlayer player) {
        List<ItemStack> items = RecoveryData.get(player.server).take(player.getUUID());
        for (ItemStack s : items) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
        player.displayClientMessage(Component.translatable("khaosnexus.grave.claimed", items.size()), false);
        return items.size();
    }

    /** Per-player recovery chest, stored on the overworld. */
    public static final class RecoveryData extends SavedData {
        private final CompoundTag stored = new CompoundTag();
        private HolderLookup.Provider reg;

        public static RecoveryData get(MinecraftServer server) {
            ServerLevel ow = server.overworld();
            RecoveryData d = ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RecoveryData::new,
                    (tag, r) -> {
                        RecoveryData x = new RecoveryData();
                        x.stored.merge(tag.getCompound("Players"));
                        return x;
                    }), "khaosnexus_recovery");
            d.reg = ow.registryAccess();
            return d;
        }

        public void add(UUID owner, List<ItemStack> items) {
            ListTag list = stored.getList(owner.toString(), Tag.TAG_COMPOUND);
            list.addAll(write(items, reg));
            stored.put(owner.toString(), list);
            setDirty();
        }

        public int count(UUID owner) {
            return stored.getList(owner.toString(), Tag.TAG_COMPOUND).size();
        }

        public List<ItemStack> take(UUID owner) {
            List<ItemStack> items = read(stored.getList(owner.toString(), Tag.TAG_COMPOUND), reg);
            stored.remove(owner.toString());
            setDirty();
            return items;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider r) {
            tag.put("Players", stored.copy());
            return tag;
        }
    }
}

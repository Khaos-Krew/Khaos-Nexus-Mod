package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.dungeon.DimensionSafety;
import com.khaoskrew.khaosnexus.grave.SafeGraves;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.khaoskrew.khaosnexus.spire.SpireService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Travel to and from the Underthrone hub, camp protection, and Crown Shard drops. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class UnderthroneService {
    public static final String ENTRY = "khaosnexus_underthrone_entry";
    public static final int SPIRE_FLOOR = 50;

    private UnderthroneService() {}

    public static boolean floorRouteOpen(ServerPlayer player) {
        return player.isCreative() || HunterRanks.profile(player).spireBest >= SPIRE_FLOOR;
    }

    public static boolean enter(ServerPlayer player) {
        ServerLevel under = player.server.getLevel(Saga.UNDERTHRONE);
        if (under == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.spire.missing"), false);
            return false;
        }
        if (player.level() != under) {
            CompoundTag tag;
            if (player.level().dimension() == SpireService.DIMENSION && player.getPersistentData().contains(SafeGraves.SPIRE_ENTRY)) {
                tag = player.getPersistentData().getCompound(SafeGraves.SPIRE_ENTRY).copy(); // keep the overworld spot, not the Spire
            } else {
                tag = new CompoundTag();
                tag.putString("Dim", player.level().dimension().location().toString());
                tag.putLong("Pos", player.blockPosition().asLong());
            }
            player.getPersistentData().put(ENTRY, tag);
        }
        BlockPos o = ExileCamp.ORIGIN;
        under.getChunk(o);
        if (!ExileCamp.built(under, o)) {
            ExileCamp.build(under, o);
        }
        boolean ok = DimensionSafety.teleport(player, Saga.UNDERTHRONE, ExileCamp.arrival(o), 180.0F, true);
        if (ok) {
            player.displayClientMessage(Component.translatable("khaosnexus.underthrone.entered"), false);
        }
        return ok;
    }

    public static void leave(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(ENTRY);
        ResourceLocation dim = tag.contains("Dim") ? ResourceLocation.tryParse(tag.getString("Dim")) : null;
        ResourceKey<net.minecraft.world.level.Level> key = dim == null ? null : ResourceKey.create(Registries.DIMENSION, dim);
        BlockPos pos = tag.contains("Pos") ? BlockPos.of(tag.getLong("Pos")) : player.server.overworld().getSharedSpawnPos();
        if (key == null || key == Saga.UNDERTHRONE || key == SpireService.DIMENSION) {
            key = null; // never "return" into a hub/run; fall back to overworld spawn
            pos = player.server.overworld().getSharedSpawnPos();
        }
        if (DimensionSafety.teleport(player, key, pos, player.getYRot(), true)) {
            player.getPersistentData().remove(ENTRY);
            player.displayClientMessage(Component.translatable("khaosnexus.underthrone.left"), false);
        }
    }

    public static boolean inCamp(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        return level instanceof ServerLevel l && l.dimension() == Saga.UNDERTHRONE
                && pos.distSqr(ExileCamp.ORIGIN) < (ExileCamp.RADIUS + 4) * (ExileCamp.RADIUS + 4) * 1.0D;
    }

    @SubscribeEvent
    public static void protectBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().isCreative() && inCamp(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void protectPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && !p.isCreative() && inCamp(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Gate/Spire bosses drop Crown Shards for SS/Paragon hunters (the saga currency comes from saga-tier content). */
    @SubscribeEvent
    public static void shardDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.getTags().contains("khaosnexus_dungeon_boss")
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        int prestige = com.khaoskrew.khaosnexus.endgame.Prestige.level(HunterRanks.profile(killer));
        int n = prestige <= 0 ? 0 : prestige + (mob.getPersistentData().getBoolean(SealGateBlock.SEAL_TAG) ? 2 : 0);
        if (n > 0) {
            event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(ModRegistries.CROWN_SHARD.get(), n)));
        }
    }
}

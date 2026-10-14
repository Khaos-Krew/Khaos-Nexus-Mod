package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.grave.SafeGraves;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Dimension safety for every Khaos Nexus dimension (gate dungeons, Spire, Underthrone and later realms).
 * - Everything is namespaced khaosnexus:, with our own void biomes (no vanilla biome keys), so other mods'
 *   tag-based biome modifiers (#c:is_overworld, #minecraft:is_overworld...) never reach our dimensions.
 * - Travel only through our own blocks (Nexus Gate, Spire Gate, Seal Gate) and this one teleport, never nether/end
 *   portal logic or a portal frame. A missing level falls back to the overworld spawn; a teleport another mod
 *   cancelled (EntityTravelToDimensionEvent) is detected and reported instead of assumed.
 * - Foreign natural spawns (biome-modifier or other mods' spawners) are blocked in our dimensions (config).
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class DimensionSafety {
    private DimensionSafety() {}

    public static boolean ours(ResourceKey<Level> key) {
        return key.location().getNamespace().equals(KhaosNexus.MODID);
    }

    /** The level for a key, or the overworld when that dimension is missing/unloaded (removed datapack, mod clash). */
    public static ServerLevel resolve(MinecraftServer server, @Nullable ResourceKey<Level> key) {
        ServerLevel level = key == null ? null : server.getLevel(key);
        if (level == null) {
            if (key != null) {
                KhaosNexus.LOGGER.warn("Dimension {} is not loaded; falling back to the overworld", key.location());
            }
            return server.overworld();
        }
        return level;
    }

    /**
     * Safe teleport: resolves the level (overworld fallback), finds a safe spot near the target (else the exact
     * spot when searching is off), and verifies it happened. Returns false when another mod cancelled the change.
     */
    public static boolean teleport(ServerPlayer player, @Nullable ResourceKey<Level> key, BlockPos target, float yaw, boolean search) {
        ServerLevel level = resolve(player.server, key);
        BlockPos at = target;
        if (key != null && level.dimension() != key) {
            at = level.getSharedSpawnPos();
        }
        if (search) {
            BlockPos safe = SafeGraves.findSafeSpot(level, at, 4);
            if (safe != null) {
                at = safe;
            }
        }
        return teleportExact(player, level, Vec3.atBottomCenterOf(at), yaw);
    }

    public static boolean teleportExact(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw) {
        player.teleportTo(level, pos.x, pos.y, pos.z, yaw, player.getXRot());
        player.fallDistance = 0.0F;
        if (player.level() != level) {
            player.displayClientMessage(Component.translatable("khaosnexus.dimension.blocked"), false);
            KhaosNexus.LOGGER.warn("Teleport of {} to {} was cancelled by another mod", player.getName().getString(), level.dimension().location());
            return false;
        }
        return true;
    }

    /** True when a spawn should be blocked: a foreign mob spawning naturally/chunk-gen in one of our dimensions. */
    public static boolean blocks(Level level, Entity entity, MobSpawnType type) {
        if (!Config.BLOCK_FOREIGN_SPAWNS.get() || !ours(level.dimension())) {
            return false;
        }
        boolean natural = type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL || type == MobSpawnType.EVENT;
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return natural && !id.getNamespace().equals(KhaosNexus.MODID);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (blocks(mob.level(), mob, event.getSpawnType())) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }
}

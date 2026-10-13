package com.khaoskrew.khaosnexus.gate;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class GateWorldSpawner {
    private GateWorldSpawner() {}

    public static boolean isEnabled() {
        return Config.NATURAL_SPAWNING.get() && Config.NATURAL_SPAWNS_PER_DAY.get() > 0;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            return;
        }
        int interval = Config.NATURAL_SPAWN_INTERVAL.get();
        if (interval <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }
        trySpawn(level.getServer());
    }

    public static boolean trySpawn(MinecraftServer server) {
        if (!isEnabled() || server.getPlayerList().getPlayers().isEmpty()) {
            return false;
        }
        ServerLevel overworld = server.overworld();
        long day = overworld.getDayTime() / 24000L;
        GateSpawnData data = GateSpawnData.get(overworld);
        if (!data.hasRoom(day, Config.NATURAL_SPAWNS_PER_DAY.get())) {
            return false;
        }
        var players = server.getPlayerList().getPlayers();
        ServerPlayer player = players.get(overworld.getRandom().nextInt(players.size()));
        for (int attempt = 0; attempt < 6; attempt++) {
            if (placeNear(player)) {
                data.markSpawned(day);
                return true;
            }
        }
        return false;
    }

    private static boolean placeNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();
        int min = Config.NATURAL_SPAWN_MIN_DISTANCE.get();
        int max = Math.max(min, Config.NATURAL_SPAWN_MAX_DISTANCE.get());
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int distance = min + (max == min ? 0 : random.nextInt(max - min + 1));
        int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        if (!level.hasChunkAt(x, z)) {
            return false;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (surface <= level.getMinBuildHeight() || surface >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        BlockPos pos = new BlockPos(x, surface, z);
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        if (!level.setBlockAndUpdate(pos, ModRegistries.NEXUS_GATE.get().defaultBlockState())) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof NexusGateBlockEntity gate)) {
            level.removeBlock(pos, false);
            return false;
        }
        Component message = Component.translatable(
                "khaosnexus.gate.appeared",
                Component.translatable(gate.getRank().translationKey()),
                pos.getX(),
                pos.getY(),
                pos.getZ());
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) <= 128.0D * 128.0D) {
                nearby.sendSystemMessage(message);
            }
        }
        KhaosNexus.LOGGER.info("Natural Nexus Gate appeared at {} {} {} ({})", pos.getX(), pos.getY(), pos.getZ(), gate.getRank());
        return true;
    }
}

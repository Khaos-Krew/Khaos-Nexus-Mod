package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.spire.SpireService;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Timed boss rush. A roster file: "bosses" (entity ids; ids of mods that are not installed are skipped), "length"
 * (fights per run, the list repeats if short), "heal_after" (fight number after which the party is healed once, 0 = never),
 * "health_scale" and "shuffle". Runs take place on a platform in the Spire dimension and are not saved across restarts.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class BossRush {
    public static final String TAG = "khaosnexus.boss_rush";
    public static final String TITLE = "rush_champion";

    static final class Run {
        final UUID id = UUID.randomUUID();
        final List<UUID> players = new ArrayList<>();
        final Map<UUID, Vec3> returnPos = new HashMap<>();
        final Map<UUID, ResourceKey<Level>> returnDim = new HashMap<>();
        final List<EntityType<?>> bosses;
        final int healAfter;
        final double healthScale;
        final BlockPos origin;
        int stage;
        long startTick;
        long nextSpawn = -1;
        UUID boss;

        Run(List<EntityType<?>> bosses, int healAfter, double healthScale, BlockPos origin) {
            this.bosses = bosses;
            this.healAfter = healAfter;
            this.healthScale = healthScale;
            this.origin = origin;
        }
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static int nextSlot;

    private BossRush() {}

    /** Resolves a roster to the concrete fight list. Unknown or unavailable ids are dropped. */
    public static List<EntityType<?>> roster(ResourceLocation id, long seed) {
        JsonObject o = JsonPool.BOSS_RUSH.get(id);
        List<EntityType<?>> pool = new ArrayList<>();
        if (o == null) {
            return pool;
        }
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "bosses")) {
            ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
            if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                pool.add(BuiltInRegistries.ENTITY_TYPE.get(rl));
            }
        }
        if (pool.isEmpty()) {
            return pool;
        }
        if (GsonHelper.getAsBoolean(o, "shuffle", true)) {
            Collections.shuffle(pool, new Random(seed));
        }
        int length = GsonHelper.getAsInt(o, "length", 5);
        List<EntityType<?>> out = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            out.add(pool.get(i % pool.size()));
        }
        return out;
    }

    public static Run runOf(UUID player) {
        for (Run r : RUNS.values()) {
            if (r.players.contains(player)) {
                return r;
            }
        }
        return null;
    }

    public static boolean start(ServerPlayer leader) {
        if (!Config.BOSS_RUSH.get()) {
            return false;
        }
        ServerLevel arenaLevel = leader.server.getLevel(SpireService.DIMENSION);
        GateRank min = GateRank.byName(Config.BOSS_RUSH_MIN_RANK.get().trim());
        if (arenaLevel == null || runOf(leader.getUUID()) != null) {
            leader.displayClientMessage(Component.translatable("khaosnexus.rush.busy"), false);
            return false;
        }
        List<ServerPlayer> party = new ArrayList<>();
        for (ServerPlayer m : PartyService.onlineMembers(leader)) {
            if (m == leader || (m.level() == leader.level() && m.distanceToSqr(leader) < 32 * 32 && runOf(m.getUUID()) == null)) {
                party.add(m);
            }
        }
        for (ServerPlayer m : party) {
            if (HunterRanks.rankOf(m).ordinal() < min.ordinal()) {
                leader.displayClientMessage(Component.translatable("khaosnexus.rush.rank", Component.translatable(min.translationKey())), false);
                return false;
            }
        }
        ResourceLocation rosterId = ResourceLocation.tryParse(Config.BOSS_RUSH_ROSTER.get());
        List<EntityType<?>> fights = rosterId == null ? List.of() : roster(rosterId, leader.getRandom().nextLong());
        if (fights.isEmpty()) {
            leader.displayClientMessage(Component.translatable("khaosnexus.rush.no_roster"), false);
            return false;
        }
        JsonObject o = JsonPool.BOSS_RUSH.get(rosterId);
        BlockPos origin = new BlockPos(-20000 - (nextSlot++ % 64) * 128, 100, 0);
        Run run = new Run(fights, GsonHelper.getAsInt(o, "heal_after", 3), GsonHelper.getAsDouble(o, "health_scale", 1.0D), origin);
        buildArena(arenaLevel, origin);
        for (ServerPlayer m : party) {
            run.players.add(m.getUUID());
            run.returnPos.put(m.getUUID(), m.position());
            run.returnDim.put(m.getUUID(), m.level().dimension());
            m.teleportTo(arenaLevel, origin.getX() + 0.5D, origin.getY() + 1, origin.getZ() - 8.5D, 0.0F, 0.0F);
            m.displayClientMessage(Component.translatable("khaosnexus.rush.started", fights.size()), false);
        }
        run.startTick = leader.server.getTickCount();
        run.nextSpawn = run.startTick + 60;
        RUNS.put(run.id, run);
        return true;
    }

    static void buildArena(ServerLevel level, BlockPos o) {
        level.getChunkAt(o);
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                boolean wall = Math.abs(dx) == 14 || Math.abs(dz) == 14;
                level.setBlockAndUpdate(o.offset(dx, -1, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                for (int dy = 0; dy <= 8; dy++) {
                    level.setBlockAndUpdate(o.offset(dx, dy, dz), wall && dy <= 3 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void spawnNext(MinecraftServer server, Run run) {
        ServerLevel level = server.getLevel(SpireService.DIMENSION);
        if (level == null) {
            return;
        }
        Mob boss = GateArena.spawnMob(level, GateRank.S, run.bosses.get(run.stage), run.origin.offset(0, 0, 6), 180.0F, run.healthScale, true);
        if (boss == null) {
            run.stage++;
            run.nextSpawn = server.getTickCount() + 1;
            return;
        }
        boss.getPersistentData().putUUID(TAG, run.id);
        boss.setCustomName(Component.translatable("khaosnexus.rush.boss", run.stage + 1, run.bosses.size(), boss.getType().getDescription()));
        run.boss = boss.getUUID();
        run.nextSpawn = -1;
        forEach(server, run, p -> p.displayClientMessage(Component.translatable("khaosnexus.rush.fight", run.stage + 1, run.bosses.size()), true));
    }

    private static void forEach(MinecraftServer server, Run run, java.util.function.Consumer<ServerPlayer> f) {
        for (UUID id : List.copyOf(run.players)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                f.accept(p);
            }
        }
    }

    /** Advances the run after a rush boss dies. Package-visible for tests. */
    static void onBossDown(MinecraftServer server, Run run) {
        run.stage++;
        run.boss = null;
        if (run.stage == run.healAfter) {
            forEach(server, run, p -> {
                p.setHealth(p.getMaxHealth());
                p.getFoodData().eat(20, 1.0F);
                p.displayClientMessage(Component.translatable("khaosnexus.rush.healed"), false);
            });
        }
        if (run.stage >= run.bosses.size()) {
            win(server, run);
        } else {
            run.nextSpawn = server.getTickCount() + Config.BOSS_RUSH_DELAY.get();
        }
    }

    private static void win(MinecraftServer server, Run run) {
        long ticks = server.getTickCount() - run.startTick;
        forEach(server, run, p -> {
            var prof = HunterRanks.profile(p);
            prof.endgame.putInt(Prestige.RUSH_WINS, prof.endgame.getInt(Prestige.RUSH_WINS) + 1);
            Leaderboards.get(server).submit(Leaderboards.rushBoard(), p.getUUID(), p.getGameProfile().getName(), ticks);
            Titles.unlock(p, TITLE);
            int cores = Config.BOSS_RUSH_REWARD_CORES.get();
            if (cores > 0) {
                ItemStack s = new ItemStack(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get(), cores);
                if (!p.getInventory().add(s)) {
                    p.drop(s, false);
                }
            }
            p.displayClientMessage(Component.translatable("khaosnexus.rush.won", String.format("%d:%02d", ticks / 1200, (ticks / 20) % 60)), false);
            HunterRanks.sync(p);
            Prestige.tryPromote(p);
        });
        end(server, run);
    }

    private static void end(MinecraftServer server, Run run) {
        forEach(server, run, p -> sendBack(server, run, p));
        discardBoss(server, run);
        RUNS.remove(run.id);
    }

    private static void discardBoss(MinecraftServer server, Run run) {
        ServerLevel level = server.getLevel(SpireService.DIMENSION);
        if (run.boss != null && level != null) {
            Entity e = level.getEntity(run.boss);
            if (e != null) {
                e.discard();
            }
        }
    }

    private static void sendBack(MinecraftServer server, Run run, ServerPlayer p) {
        ServerLevel dim = server.getLevel(run.returnDim.getOrDefault(p.getUUID(), Level.OVERWORLD));
        Vec3 pos = run.returnPos.get(p.getUUID());
        if (dim == null || pos == null) {
            dim = server.overworld();
            pos = Vec3.atBottomCenterOf(dim.getSharedSpawnPos());
        }
        p.teleportTo(dim, pos.x, pos.y, pos.z, p.getYRot(), 0.0F);
    }

    public static void leave(ServerPlayer p, boolean died) {
        Run run = runOf(p.getUUID());
        if (run == null) {
            return;
        }
        sendBack(p.server, run, p);
        run.players.remove(p.getUUID());
        p.displayClientMessage(Component.translatable(died ? "khaosnexus.rush.died" : "khaosnexus.rush.left"), false);
        if (run.players.isEmpty()) {
            discardBoss(p.server, run);
            RUNS.remove(run.id);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && runOf(p.getUUID()) != null) {
            event.setCanceled(true);
            p.setHealth(Math.max(1.0F, p.getMaxHealth() * 0.5F));
            leave(p, true);
            return;
        }
        if (event.getEntity() instanceof Mob mob && mob.getPersistentData().hasUUID(TAG)) {
            Run run = RUNS.get(mob.getPersistentData().getUUID(TAG));
            if (run != null && mob.getUUID().equals(run.boss)) {
                onBossDown(mob.getServer(), run);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            leave(p, false);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (Run run : List.copyOf(RUNS.values())) {
            if (run.nextSpawn >= 0 && server.getTickCount() >= run.nextSpawn && run.stage < run.bosses.size()) {
                spawnNext(server, run);
            } else if (run.boss != null && server.getTickCount() % 40 == 0) {
                ServerLevel level = server.getLevel(SpireService.DIMENSION);
                Entity e = level == null ? null : level.getEntity(run.boss);
                if (e == null || !e.isAlive()) {
                    // Boss vanished without dying (despawn or unload): count it so the run cannot soft-lock.
                    onBossDown(server, run);
                }
            }
        }
    }

    static Run testRun(List<EntityType<?>> bosses, int healAfter) {
        Run run = new Run(bosses, healAfter, 1.0D, BlockPos.ZERO);
        RUNS.put(run.id, run);
        return run;
    }

    static boolean running(Run run) {
        return RUNS.containsKey(run.id);
    }

    public static int activeRuns() {
        return RUNS.size();
    }
}

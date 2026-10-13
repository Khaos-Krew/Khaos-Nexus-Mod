package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.raid.RaidService;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Multi-phase group raids. A raid boss file: "entity", "base_health", "health_per_player", "tank_aggro" (boss locks on
 * to a Tank in the raid), and "phases" ordered by "at" (health fraction where the phase begins, 1.0 first). A phase can
 * have "adds" (entity ids spawned on entry), "boss_effects", "damage" and "speed" multipliers, and a "curse"
 * {effect, amplifier, seconds, interval_seconds, targets} that a Lifebinder ability cleanses.
 * Kills go through the existing raid loot (personal chests, lockout) because the boss carries the raid flag.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class GroupRaids {
    public static final String TAG = "khaosnexus.group_raid";
    public static final String TITLE = "raid_conqueror";

    public static final class State {
        final ResourceLocation id;
        final JsonObject def;
        final ResourceKey<Level> dim;
        final UUID boss;
        final List<UUID> party = new ArrayList<>();
        int phase;
        long nextCurse;

        State(ResourceLocation id, JsonObject def, ResourceKey<Level> dim, UUID boss) {
            this.id = id;
            this.def = def;
            this.dim = dim;
            this.boss = boss;
        }

        public int phase() {
            return this.phase;
        }
    }

    private static final Map<UUID, State> RAIDS = new HashMap<>();

    private GroupRaids() {}

    public static JsonArray phases(JsonObject def) {
        return GsonHelper.getAsJsonArray(def, "phases", new JsonArray());
    }

    /** Index of the phase for a health fraction: the last phase whose "at" is at or above it. */
    public static int phaseFor(JsonObject def, float fraction) {
        JsonArray ph = phases(def);
        int idx = 0;
        for (int i = 0; i < ph.size(); i++) {
            if (fraction <= GsonHelper.getAsFloat(ph.get(i).getAsJsonObject(), "at", 1.0F)) {
                idx = i;
            }
        }
        return idx;
    }

    public static State start(ServerPlayer leader, String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : KhaosNexus.MODID + ":" + raw);
        JsonObject def = id == null ? null : JsonPool.RAID_BOSSES.get(id);
        if (!Config.GROUP_RAIDS.get() || def == null) {
            leader.displayClientMessage(Component.translatable("khaosnexus.graid.unknown", raw), false);
            return null;
        }
        int size = PartyService.size(leader);
        if (size < Config.GROUP_RAID_MIN.get() || size > Config.GROUP_RAID_MAX.get()) {
            leader.displayClientMessage(Component.translatable("khaosnexus.graid.size", Config.GROUP_RAID_MIN.get(), Config.GROUP_RAID_MAX.get()), false);
            return null;
        }
        ServerLevel level = (ServerLevel) leader.level();
        ResourceLocation typeId = ResourceLocation.tryParse(GsonHelper.getAsString(def, "entity"));
        if (typeId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) {
            leader.displayClientMessage(Component.translatable("khaosnexus.graid.unknown", raw), false);
            return null;
        }
        Mob boss = GateArena.spawnMob(level, GateRank.S, BuiltInRegistries.ENTITY_TYPE.get(typeId),
                leader.blockPosition().relative(leader.getDirection(), 4), leader.getYRot() + 180.0F, 1.0D, true);
        if (boss == null) {
            return null;
        }
        State st = setup(boss, id, def, size);
        for (ServerPlayer m : PartyService.onlineMembers(leader)) {
            st.party.add(m.getUUID());
            m.displayClientMessage(Component.translatable("khaosnexus.graid.started", name(id), size).withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        if (!st.party.contains(leader.getUUID())) {
            st.party.add(leader.getUUID());
        }
        return st;
    }

    /** Applies raid flags and health to a spawned boss and registers its state. Package-visible for tests. */
    static State setup(Mob boss, ResourceLocation id, JsonObject def, int size) {
        BigHealth.set(boss, GsonHelper.getAsDouble(def, "base_health", 600.0D)
                + GsonHelper.getAsDouble(def, "health_per_player", 150.0D) * Math.max(0, size - 1));
        boss.getPersistentData().putBoolean(RaidService.BOSS, true);
        boss.getPersistentData().putString(TAG, id.toString());
        boss.setCustomName(name(id).copy().withStyle(ChatFormatting.DARK_PURPLE));
        State st = new State(id, def, boss.level().dimension(), boss.getUUID());
        RAIDS.put(boss.getUUID(), st);
        return st;
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("khaosnexus.graid." + id.getNamespace() + "." + id.getPath());
    }

    private static List<ServerPlayer> online(MinecraftServer server, State st) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : st.party) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive() && p.level().dimension() == st.dim) {
                out.add(p);
            }
        }
        return out;
    }

    /** Runs one tick of raid logic for a boss. Package-visible for tests. */
    static void tick(MinecraftServer server, State st, Mob boss) {
        long now = server.getTickCount();
        List<ServerPlayer> raiders = online(server, st);
        int target = phaseFor(st.def, boss.getHealth() / boss.getMaxHealth());
        while (st.phase < target) {
            st.phase++;
            enterPhase((ServerLevel) boss.level(), st, boss, raiders);
        }
        if (now % 20 == 0) {
            aggro(server, st, boss);
        }
        JsonObject phase = phases(st.def).isEmpty() ? null : phases(st.def).get(st.phase).getAsJsonObject();
        JsonObject curse = phase == null ? null : GsonHelper.getAsJsonObject(phase, "curse", null);
        if (curse != null && now >= st.nextCurse && !raiders.isEmpty()) {
            st.nextCurse = now + GsonHelper.getAsInt(curse, "interval_seconds", 15) * 20L;
            Holder<MobEffect> eff = effect(GsonHelper.getAsString(curse, "effect", "minecraft:wither"));
            if (eff != null) {
                int n = Math.min(raiders.size(), GsonHelper.getAsInt(curse, "targets", 2));
                List<ServerPlayer> pool = new ArrayList<>(raiders);
                java.util.Collections.shuffle(pool, new java.util.Random(now));
                for (int i = 0; i < n; i++) {
                    pool.get(i).addEffect(new MobEffectInstance(eff, GsonHelper.getAsInt(curse, "seconds", 12) * 20, GsonHelper.getAsInt(curse, "amplifier", 1)));
                    pool.get(i).displayClientMessage(Component.translatable("khaosnexus.graid.cursed").withStyle(ChatFormatting.DARK_RED), true);
                }
            }
        }
    }

    /** Tank aggro: the boss locks on to the nearest Tank in the raid. Returns the tank, or null. */
    static ServerPlayer aggro(MinecraftServer server, State st, Mob boss) {
        if (!GsonHelper.getAsBoolean(st.def, "tank_aggro", true)) {
            return null;
        }
        ServerPlayer tank = null;
        for (ServerPlayer p : online(server, st)) {
            if (HunterClass.byId(HunterRanks.profile(p).classId) == HunterClass.TANK && p.distanceToSqr(boss) < 32 * 32
                    && (tank == null || p.distanceToSqr(boss) < tank.distanceToSqr(boss))) {
                tank = p;
            }
        }
        if (tank != null) {
            boss.setTarget(tank);
        }
        return tank;
    }

    private static Holder<MobEffect> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
    }

    private static void enterPhase(ServerLevel level, State st, Mob boss, List<ServerPlayer> raiders) {
        JsonObject ph = phases(st.def).get(st.phase).getAsJsonObject();
        for (ServerPlayer p : raiders) {
            p.displayClientMessage(Component.translatable("khaosnexus.graid.phase", st.phase + 1).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        mult(boss.getAttribute(Attributes.ATTACK_DAMAGE), GsonHelper.getAsDouble(ph, "damage", 1.0D));
        mult(boss.getAttribute(Attributes.MOVEMENT_SPEED), GsonHelper.getAsDouble(ph, "speed", 1.0D));
        for (JsonElement e : GsonHelper.getAsJsonArray(ph, "boss_effects", new JsonArray())) {
            JsonObject eo = e.getAsJsonObject();
            Holder<MobEffect> h = effect(GsonHelper.getAsString(eo, "id"));
            if (h != null) {
                boss.addEffect(new MobEffectInstance(h, -1, GsonHelper.getAsInt(eo, "amplifier", 0)));
            }
        }
        int i = 0;
        for (JsonElement e : GsonHelper.getAsJsonArray(ph, "adds", new JsonArray())) {
            ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
            if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
                BlockPos at = boss.blockPosition().offset((i % 3 - 1) * 3, 0, (i / 3 % 3 - 1) * 3);
                Mob add = GateArena.spawnMob(level, GateRank.S, type, at, 0.0F, 1.0D);
                if (add != null && !raiders.isEmpty()) {
                    add.setTarget(raiders.get(i % raiders.size()));
                }
                i++;
            }
        }
    }

    private static void mult(AttributeInstance a, double m) {
        if (a != null && m != 1.0D) {
            a.setBaseValue(a.getBaseValue() * m);
        }
    }

    /** Lifebinder abilities cleanse raid curses from nearby allies. Called from ClassAbilities.cast. */
    public static int onAbility(ServerPlayer caster, HunterClass cls) {
        if (cls != HunterClass.LIFEBINDER || RAIDS.isEmpty()) {
            return 0;
        }
        double r = Config.CLEANSE_RADIUS.get();
        int cleansed = 0;
        for (State st : RAIDS.values()) {
            if (!st.party.contains(caster.getUUID())) {
                continue;
            }
            List<Holder<MobEffect>> curses = new ArrayList<>();
            for (JsonElement ph : phases(st.def)) {
                JsonObject c = GsonHelper.getAsJsonObject(ph.getAsJsonObject(), "curse", null);
                Holder<MobEffect> h = c == null ? null : effect(GsonHelper.getAsString(c, "effect", "minecraft:wither"));
                if (h != null) {
                    curses.add(h);
                }
            }
            for (ServerPlayer p : online(caster.server, st)) {
                if (p.distanceToSqr(caster) <= r * r) {
                    for (Holder<MobEffect> h : curses) {
                        if (p.removeEffect(h)) {
                            cleansed++;
                        }
                    }
                }
            }
        }
        if (cleansed > 0) {
            caster.displayClientMessage(Component.translatable("khaosnexus.graid.cleansed", cleansed), true);
        }
        return cleansed;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (State st : List.copyOf(RAIDS.values())) {
            ServerLevel level = server.getLevel(st.dim);
            Entity e = level == null ? null : level.getEntity(st.boss);
            if (!(e instanceof Mob boss) || !boss.isAlive()) {
                if (server.getTickCount() % 200 == 0) {
                    RAIDS.remove(st.boss);
                }
                continue;
            }
            tick(server, st, boss);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        State st = RAIDS.remove(event.getEntity().getUUID());
        if (st == null || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer p : online(level.getServer(), st)) {
            Titles.unlock(p, TITLE);
            int cores = Config.GROUP_RAID_CORES.get();
            if (cores > 0) {
                ItemStack s = new ItemStack(com.khaoskrew.khaosnexus.forge.HunterForge.RIFT_CORE.get(), cores);
                if (!p.getInventory().add(s)) {
                    p.drop(s, false);
                }
            }
            var prof = HunterRanks.profile(p);
            prof.endgame.putInt("GroupRaidClears", prof.endgame.getInt("GroupRaidClears") + 1);
            HunterRanks.sync(p);
            p.displayClientMessage(Component.translatable("khaosnexus.graid.won", name(st.id)).withStyle(ChatFormatting.GOLD), false);
        }
    }

    public static State state(UUID boss) {
        return RAIDS.get(boss);
    }
}

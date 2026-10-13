package com.khaoskrew.khaosnexus.compat;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Tuning for optional boss-mod bosses (Cataclysm, Mowzie's...) in gate dungeons: health and damage scales with
 * per-boss overrides, a spawn grace period, party health scaling, an engaged flag that holds the dungeon open,
 * and the optional Cataclysm rift phase. Damage is scaled once, in the damage event, so abilities and projectiles
 * are covered and melee is not scaled twice.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ModBossTuning {
    public static final String TAG = "khaosnexus_mod_boss";
    private static final boolean DEBUG = Boolean.getBoolean("khaosnexus.bossDebug");
    private static final String DATA = "khaosnexus.mod_boss";

    private ModBossTuning() {}

    /** {health, damage} scale for this boss: a modBossScale override or the globals. */
    public static double[] scales(ResourceLocation id) {
        for (String line : Config.MOD_BOSS_SCALE.get()) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(id.toString())) {
                String[] v = p[1].split(",");
                try {
                    return new double[] {Double.parseDouble(v[0].trim()), Double.parseDouble(v[1].trim())};
                } catch (RuntimeException e) {
                    KhaosNexus.LOGGER.warn("Bad modBossScale entry {}", line);
                }
            }
        }
        return new double[] {Config.MOD_BOSS_HEALTH.get(), Config.MOD_BOSS_DAMAGE.get()};
    }

    public static double healthScale(EntityType<?> type) {
        return scales(EntityType.getKey(type))[0];
    }

    /** Called right after GateArena.spawnMob: undo its attack-damage scaling and record the one damage multiplier. */
    public static void setUp(Mob boss, GateRank rank, long now) {
        double rankDamage = rank.tuning().damageMultiplier.get();
        AttributeInstance attack = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && rankDamage > 0) {
            attack.setBaseValue(attack.getBaseValue() / rankDamage);
        }
        CompoundTag d = new CompoundTag();
        d.putDouble("Damage", rankDamage * scales(EntityType.getKey(boss.getType()))[1]);
        d.putString("Rank", rank.name());
        int grace = Config.MOD_BOSS_GRACE.get() * 20;
        d.putLong("GraceUntil", now + grace);
        d.putBoolean("Graced", grace > 0);
        d.putLong("Home", boss.blockPosition().asLong());
        boss.getPersistentData().put(DATA, d);
        boss.addTag(TAG);
        if (grace > 0) {
            boss.setNoAi(true);
            boss.setInvulnerable(true);
        }
    }

    public static boolean tuned(Entity e) {
        return e instanceof Mob m && m.getTags().contains(TAG);
    }

    public static double damageMultiplier(Mob boss) {
        CompoundTag d = boss.getPersistentData().getCompound(DATA);
        return d.contains("Damage") ? d.getDouble("Damage") : 1.0D;
    }

    /** The boss behind a damage source: the attacker itself, or the owner of its projectile or summon. */
    public static Mob source(Entity attacker, Entity direct) {
        for (Entity e : new Entity[] {attacker, direct}) {
            Entity cur = e;
            for (int i = 0; i < 3 && cur != null; i++) {
                if (tuned(cur)) {
                    return (Mob) cur;
                }
                cur = cur instanceof TraceableEntity t ? t.getOwner() : cur instanceof OwnableEntity o ? o.getOwner() : null;
            }
        }
        return null;
    }

    /** True if a tuned boss in this box was fighting within the last 30 s; the dungeon then does not collapse. */
    public static boolean engagedIn(ServerLevel level, AABB box, long ignored) {
        long now = level.getGameTime();
        return !level.getEntitiesOfClass(Mob.class, box, m -> tuned(m) && m.isAlive()
                && m.getPersistentData().getCompound(DATA).getLong("EngagedUntil") > now).isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        long now = event.getEntity().level().getGameTime();
        if (tuned(event.getEntity())) {
            CompoundTag d = event.getEntity().getPersistentData().getCompound(DATA);
            if (d.getBoolean("Graced")) {
                event.setCanceled(true);
                return;
            }
            d.putLong("EngagedUntil", now + 600L);
            if (DEBUG) {
                KhaosNexus.LOGGER.info("[bossdebug] boss incoming {} ({})", event.getAmount(), event.getSource().getMsgId());
            }
        }
        if (DEBUG && event.getEntity() instanceof Player pl && !(source(event.getSource().getEntity(), event.getSource().getDirectEntity()) != null)) {
            KhaosNexus.LOGGER.info("[bossdebug] untraced hit on {} for {} ({} / {})", pl.getName().getString(), event.getAmount(),
                    event.getSource().getMsgId(), event.getSource().getEntity() == null ? "-" : EntityType.getKey(event.getSource().getEntity().getType()));
        }
        Mob boss = source(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (boss != null && boss != event.getEntity()) {
            if (DEBUG) {
                KhaosNexus.LOGGER.info("[bossdebug] {} hits {} for {} x {} ({} / {})", EntityType.getKey(boss.getType()),
                        event.getEntity().getName().getString(), event.getAmount(), damageMultiplier(boss),
                        event.getSource().getMsgId(), event.getSource().getDirectEntity() == null ? "-" : EntityType.getKey(event.getSource().getDirectEntity().getType()));
            }
            event.setAmount((float) (event.getAmount() * damageMultiplier(boss)));
            boss.getPersistentData().getCompound(DATA).putLong("EngagedUntil", now + 600L);
        }
    }

    @SubscribeEvent
    public static void onDamaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (DEBUG && event.getEntity() instanceof Player pl) {
            KhaosNexus.LOGGER.info("[bossdebug] {} took {} (hp {})", pl.getName().getString(), event.getNewDamage(), pl.getHealth());
        }
        if (DEBUG && tuned(event.getEntity())) {
            KhaosNexus.LOGGER.info("[bossdebug] boss took {} (hp {})", event.getNewDamage(), event.getEntity().getHealth());
        }
    }

    @SubscribeEvent
    public static void onHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent event) {
        if (tuned(event.getEntity())) {
            event.setAmount((float) (event.getAmount() * Config.MOD_BOSS_HEAL.get()));
        }
    }

    /** Boss-mod bosses must not break the dungeon floor (the void is below it). */
    @SubscribeEvent
    public static void onGrief(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent event) {
        Entity e = event.getEntity();
        if (tuned(e) || source(e, e) != null) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob boss) || !(boss.level() instanceof ServerLevel level) || !tuned(boss)) {
            return;
        }
        CompoundTag d = boss.getPersistentData().getCompound(DATA);
        long now = level.getGameTime();
        if (d.getBoolean("Graced") && now >= d.getLong("GraceUntil")) {
            d.putBoolean("Graced", false);
            boss.setNoAi(false);
            boss.setInvulnerable(false);
            applyParty(boss, level);
        }
        if (now % 10 == 0 && d.contains("Home")) {
            rescue(boss, level, BlockPos.of(d.getLong("Home")));
        }
        if (Config.CATACLYSM_RIFT_PHASE.get() && !d.getBoolean("RiftPhase")
                && "cataclysm".equals(EntityType.getKey(boss.getType()).getNamespace())
                && boss.getHealth() <= boss.getMaxHealth() * 0.3F && boss.isAlive()) {
            d.putBoolean("RiftPhase", true);
            riftPhase(boss, level, GateRank.byName(d.getString("Rank")));
        }
    }

    /** +modBossPartyHealth max health per extra hunter within 48 blocks when the grace period ends. */
    public static int applyParty(Mob boss, ServerLevel level) {
        int hunters = level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(48.0D),
                p -> !p.isSpectator() && !p.isCreative()).size();
        int extra = Math.max(0, hunters - 1);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (extra > 0 && health != null) {
            double mult = 1.0D + Config.MOD_BOSS_PARTY_HEALTH.get() * extra;
            health.setBaseValue(health.getBaseValue() * mult);
            boss.setHealth(boss.getMaxHealth());
        }
        boss.getPersistentData().getCompound(DATA).putInt("Party", hunters);
        return hunters;
    }

    /** Some bosses smash blocks regardless of mobGriefing; hunters knocked into the void below the arena are put back. */
    private static void rescue(Mob boss, ServerLevel level, BlockPos home) {
        AABB column = new AABB(home).inflate(48.0D, 0.0D, 48.0D).setMinY(level.getMinBuildHeight() - 64).setMaxY(home.getY() - 24);
        for (Player p : level.getEntitiesOfClass(Player.class, column, p -> p.isAlive() && !p.isSpectator())) {
            p.teleportTo(home.getX() + 0.5D, home.getY() + 1, home.getZ() + 6.5D);
            p.resetFallDistance();
        }
    }

    private static void riftPhase(Mob boss, ServerLevel level, GateRank rank) {
        boss.setAbsorptionAmount(boss.getAbsorptionAmount() + boss.getMaxHealth() * 0.1F);
        for (int i = 0; i < 2; i++) {
            BlockPos at = boss.blockPosition().offset(i == 0 ? 3 : -3, 0, 0);
            GateArena.spawnMob(level, rank, com.khaoskrew.khaosnexus.mob.ModEntities.RIFT_GHOUL.get(), at, 0.0F, 1.0D);
        }
    }
}

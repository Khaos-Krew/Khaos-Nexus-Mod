package com.khaoskrew.khaosnexus.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Apotheosis rules for rift mobs. No compile-time dependency: Apotheosis is reached through its attachment ids and,
 * for gate-rank world tiers, a few reflective calls that fail soft.
 *
 * <p>What Apotheosis 8.x does on its own: invaders only replace NATURAL / CHUNK_GENERATION spawns, and every rift mob is
 * spawned as EVENT, so rift mobs are never replaced. Elites only match the entity types listed in their data, which
 * never includes ours. What does reach rift mobs is (1) world-tier augments (armor, toughness, damage reduction from the
 * nearest player's world tier), applied in FinalizeSpawnEvent and again on join, and (2) the random-affix-gear
 * augmentation (12% of monsters get affix gear). Bosses are shielded from both unless
 * {@code rift.allowApotheosisInvaderBosses} is on.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ApothCompat {
    public static final TagKey<EntityType<?>> EXEMPT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "apotheosis_exempt"));
    /** Persistent-data flags set by rift spawners before the spawn event fires. */
    public static final String BOSS_FLAG = "khaosnexus.rift_boss";
    public static final String RANK_FLAG = "khaosnexus.gate_rank";
    private static final ResourceLocation APPLIED = ResourceLocation.fromNamespaceAndPath("apotheosis", "tier_augments_applied");
    private static final String[] TIERS = {"haven", "frontier", "ascent", "summit", "pinnacle"};

    private ApothCompat() {}

    public static boolean active() {
        return Config.COMPAT_APOTHEOSIS.get() && ModList.get().isLoaded("apotheosis");
    }

    public static boolean isBoss(Mob mob) {
        return mob.getType().is(EXEMPT) || mob.getPersistentData().getBoolean(BOSS_FLAG);
    }

    /** True when Apotheosis is installed and this mob must not be touched by it. */
    public static boolean shielded(Mob mob) {
        return active() && !Config.ALLOW_APOTH_BOSSES.get() && isBoss(mob);
    }

    /** Forge bonus multiplier: {@code forge.apotheosisScale} with Apotheosis installed, else 1. */
    public static double forgeScale() {
        return ModList.get().isLoaded("apotheosis") && Config.COMPAT_APOTHEOSIS.get() ? Config.FORGE_APOTHEOSIS_SCALE.get() : 1.0D;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFinalize(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!shielded(mob)) {
            return;
        }
        // Cancelling the event skips listeners that honour cancellation (Apotheosis does) but also skips the mob's own
        // finalizeSpawn, so run that here.
        event.setCanceled(true);
        mob.finalizeSpawn(event.getLevel(), event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
        markApplied(mob);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && shielded(mob)) {
            markApplied(mob);
        }
    }

    /** After Apotheosis has run: swap the player's world tier for the gate rank's tier on regular gate mobs. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalizeLate(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!active() || !Config.APOTH_GATE_TIERS.get() || isBoss(mob) || event.isSpawnCancelled()
                || !mob.getPersistentData().contains(RANK_FLAG)) {
            return;
        }
        String tier = tierFor(GateRank.byName(mob.getPersistentData().getString(RANK_FLAG)));
        if (tier != null) {
            applyTier(event.getLevel(), mob, tier);
        }
    }

    public static String tierFor(GateRank rank) {
        for (String line : Config.APOTH_TIER_BY_RANK.get()) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && GateRank.byName(p[0].trim()) == rank) {
                String t = p[1].trim().toLowerCase(Locale.ROOT);
                return List.of(TIERS).contains(t) ? t : null;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void markApplied(Mob mob) {
        AttachmentType type = NeoForgeRegistries.ATTACHMENT_TYPES.get(APPLIED);
        if (type != null) {
            mob.setData(type, Boolean.TRUE);
        }
    }

    /** Removes Apotheosis world-tier modifiers and applies the given tier's monster augments. */
    public static boolean applyTier(ServerLevelAccessor level, Mob mob, String tier) {
        try {
            Class<?> worldTier = Class.forName("dev.shadowsoffire.apotheosis.tiers.WorldTier");
            Class<?> target = Class.forName("dev.shadowsoffire.apotheosis.tiers.augments.TierAugment$Target");
            Class<?> registry = Class.forName("dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry");
            Class<?> augment = Class.forName("dev.shadowsoffire.apotheosis.tiers.augments.TierAugment");
            Object wt = enumValue(worldTier, tier);
            Object monsters = enumValue(target, "monsters");
            Method get = registry.getMethod("getAugments", worldTier, target);
            Method apply = augment.getMethod("apply", ServerLevelAccessor.class, LivingEntity.class);
            float pct = mob.getHealth() / mob.getMaxHealth();
            stripTierModifiers(mob);
            for (Object aug : (List<?>) get.invoke(null, wt, monsters)) {
                apply.invoke(aug, level, mob);
            }
            mob.setHealth(pct * mob.getMaxHealth());
            markApplied(mob);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            KhaosNexus.LOGGER.debug("Apotheosis world-tier hook unavailable: {}", e.toString());
            return false;
        }
    }

    private static Object enumValue(Class<?> type, String name) {
        for (Object o : type.getEnumConstants()) {
            if (((Enum<?>) o).name().equalsIgnoreCase(name)) {
                return o;
            }
        }
        throw new IllegalArgumentException(name);
    }

    /** Modifiers Apotheosis tier augments add are named apotheosis:&lt;tier&gt;/&lt;stat&gt;. */
    public static int stripTierModifiers(LivingEntity mob) {
        return stripAll(mob);
    }

    private static int stripAll(LivingEntity mob) {
        int removed = 0;
        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> h : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance inst = mob.getAttribute(h);
            if (inst != null) {
                removed += strip(inst);
            }
        }
        return removed;
    }

    private static int strip(AttributeInstance inst) {
        int removed = 0;
        for (AttributeModifier m : List.copyOf(inst.getModifiers())) {
            if (isTierModifier(m.id())) {
                inst.removeModifier(m.id());
                removed++;
            }
        }
        return removed;
    }

    public static boolean isTierModifier(ResourceLocation id) {
        if (!"apotheosis".equals(id.getNamespace())) {
            return false;
        }
        for (String t : TIERS) {
            if (id.getPath().startsWith(t + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Apotheosis world-tier modifiers on a mob, for the compat report. */
    public static List<String> tierModifiers(LivingEntity mob) {
        List<String> out = new ArrayList<>();
        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> h : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance inst = mob.getAttribute(h);
            if (inst != null) {
                for (AttributeModifier m : inst.getModifiers()) {
                    if (isTierModifier(m.id())) {
                        out.add(m.id().getPath() + "=" + m.amount());
                    }
                }
            }
        }
        return out;
    }
}

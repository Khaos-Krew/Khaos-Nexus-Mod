package com.khaoskrew.khaosnexus.dungeon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Mild environmental hazards for themed dungeons. Built dungeons register their box and theme here; every 5 s a player
 * standing inside one gets the theme's effect. In memory only: a dungeon rebuilt after a restart registers again, an
 * old one simply has no hazard (never a crash or a stuck effect).
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ThemeHazards {
    record Zone(ResourceKey<Level> dim, BoundingBox box, List<String> hazards) {}

    private static final List<Zone> ZONES = new ArrayList<>();

    private ThemeHazards() {}

    public static void track(ServerLevel level, BoundingBox box, DungeonThemes.Theme theme) {
        if (theme.hazards().isEmpty()) return;
        synchronized (ZONES) {
            ZONES.removeIf(z -> z.dim().equals(level.dimension()) && z.box().intersects(box));
            ZONES.add(new Zone(level.dimension(), box.inflatedBy(2), theme.hazards()));
            if (ZONES.size() > 256) ZONES.remove(0);
        }
    }

    public static List<String> at(ServerLevel level, BlockPos pos) {
        synchronized (ZONES) {
            for (Iterator<Zone> it = ZONES.iterator(); it.hasNext();) {
                Zone z = it.next();
                if (z.dim().equals(level.dimension()) && z.box().isInside(pos)) return z.hazards();
            }
        }
        return List.of();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 0 || p.isCreative() || p.isSpectator()
                || !com.khaoskrew.khaosnexus.Config.THEME_HAZARDS.get()) {
            return;
        }
        for (String h : at(p.serverLevel(), p.blockPosition())) {
            apply(p, h.contains(":") ? h.substring(h.indexOf(':') + 1) : h);
        }
    }

    /** Public for tests. */
    public static void apply(ServerPlayer p, String hazard) {
        switch (hazard) {
            case "chill" -> { if (!warded(p)) give(p, MobEffects.MOVEMENT_SLOWDOWN, 120); }
            case "heat" -> { if (!p.hasEffect(MobEffects.FIRE_RESISTANCE)) give(p, MobEffects.HUNGER, 120); }
            case "damp" -> { if (!warded(p)) give(p, MobEffects.DIG_SLOWDOWN, 120); }
            case "void" -> give(p, MobEffects.DARKNESS, 50);
            case "spores" -> give(p, MobEffects.WEAKNESS, 120);
            default -> { }
        }
    }

    private static void give(ServerPlayer p, Holder<MobEffect> e, int ticks) {
        p.addEffect(new MobEffectInstance(e, ticks, 0, true, true, true));
    }

    /** A lit campfire, torch or lantern within 3 blocks keeps the cold and damp off. */
    static boolean warded(ServerPlayer p) {
        for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-3, -2, -3), p.blockPosition().offset(3, 2, 3))) {
            BlockState s = p.level().getBlockState(q);
            if (s.is(BlockTags.CAMPFIRES) ? s.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
                    : s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.SOUL_TORCH)) {
                return true;
            }
        }
        return false;
    }
}

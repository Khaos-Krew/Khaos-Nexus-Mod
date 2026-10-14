package com.khaoskrew.khaosnexus.saga;

import javax.annotation.Nullable;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.mob.ModEntities;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Crownless Generals: rare ambush mini-bosses in normal A/S gate dungeons. The General mirrors the opener's
 * fighting style (melee classes draw the Blade, casters/ranged/support draw the Arcanist), gives a warning first,
 * and drops Crown Shards.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class CrownlessGenerals {
    private CrownlessGenerals() {}

    public static final String TAG = "khaosnexus_crownless_general";

    /** Which General answers this hunter (the "class copy"): melee classes face the Blade, the rest the Arcanist. */
    public static EntityType<?> forClass(@Nullable HunterClass c) {
        if (c == null) return ModEntities.GENERAL_BLADE.get();
        return switch (c) {
            case TANK, ASSASSIN, BERSERKER -> ModEntities.GENERAL_BLADE.get();
            default -> ModEntities.GENERAL_ARCANIST.get();
        };
    }

    public static void warn(ServerPlayer p, int seconds) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("khaosnexus.general.warn_title")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("khaosnexus.general.warn_sub", seconds)));
        p.displayClientMessage(Component.translatable("khaosnexus.general.warn_chat", seconds), false);
        p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 0.7F);
    }

    @Nullable
    public static Mob spawn(ServerLevel level, GateRank rank, EntityType<?> type, BlockPos at, float yaw) {
        Mob m = GateArena.spawnMob(level, rank, type, at, yaw, 1.0D, true);
        if (m == null) return null;
        m.setCustomName(m.getType().getDescription().copy());
        m.addTag(TAG);
        m.setPersistenceRequired();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 1.5, m.getZ(), 80, 0.8, 1.4, 0.8, 0.05);
        level.playSound(null, at, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 0.6F);
        for (ServerPlayer p : level.getPlayers(pl -> pl.distanceToSqr(m) < 48 * 48)) {
            p.displayClientMessage(Component.translatable("khaosnexus.general.arrived", m.getType().getDescription()), false);
        }
        return m;
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        if (!e.getEntity().getTags().contains(TAG)) return;
        int n = 2 + e.getEntity().getRandom().nextInt(2);
        e.getDrops().add(new ItemEntity(e.getEntity().level(), e.getEntity().getX(), e.getEntity().getY() + 1, e.getEntity().getZ(),
                new ItemStack(ModRegistries.CROWN_SHARD.get(), n)));
    }
}

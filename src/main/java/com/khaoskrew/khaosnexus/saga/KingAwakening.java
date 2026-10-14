package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.forge.HunterForge;
import com.khaoskrew.khaosnexus.mob.RiftMob;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Heart-Ember awakening at the Hunter Forge: hold Kingsbane or the Hollow Crown with Heart-Embers in the off hand and use
 * the forge. Costs 3 Heart-Embers + 4 Rift Cores. Awakened Kingsbane: charged hits call a small crownfall (one shard,
 * 2.5 blocks, 6 + 25% of the hit, 3 s cooldown). Awakened Crown: +12% damage dealt, -10% taken.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class KingAwakening {
    public static final int EMBERS = 3, CORES = 4;

    private KingAwakening() {}

    /** Returns true when the forge use was an awakening attempt (handled), false to fall through to normal forging. */
    public static boolean tryAwaken(ServerPlayer p, ItemStack held) {
        Item to = held.is(ModRegistries.KINGSBANE.get()) ? ModRegistries.AWAKENED_KINGSBANE.get()
                : held.is(ModRegistries.HOLLOW_CROWN.get()) ? ModRegistries.AWAKENED_CROWN.get() : null;
        if (to == null || !p.getOffhandItem().is(ModRegistries.HEART_EMBER.get())) return false;
        int cores = p.getInventory().countItem(HunterForge.RIFT_CORE.get());
        if (p.getOffhandItem().getCount() < EMBERS || cores < CORES) {
            p.displayClientMessage(Component.translatable("khaosnexus.awaken.missing", EMBERS, CORES), true);
            return true;
        }
        p.getOffhandItem().shrink(EMBERS);
        int left = CORES;
        for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(HunterForge.RIFT_CORE.get())) { int n = Math.min(left, s.getCount()); s.shrink(n); left -= n; }
        }
        ItemStack out = held.transmuteCopy(to, 1);   // keeps enchantments, forge levels, name
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, out);
        p.displayClientMessage(Component.translatable("khaosnexus.awaken.done", out.getHoverName()), false);
        return true;
    }

    @SubscribeEvent
    public static void chargedHit(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof LivingEntity target)) return;
        ItemStack w = p.getMainHandItem();
        if (!w.is(ModRegistries.AWAKENED_KINGSBANE.get()) || p.getAttackStrengthScale(0.5F) < 0.95F || p.getCooldowns().isOnCooldown(w.getItem())) return;
        ServerLevel l = p.serverLevel();
        float dmg = 6.0F + (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.25F;
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5D, 1.0D, 2.5D),
                e -> e != p && e.isAlive() && (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof RiftMob))) {
            e.hurt(p.damageSources().playerAttack(p), dmg);
        }
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, target.getX(), target.getY() + 4.0D, target.getZ(), 16, 0.1, 2.0, 0.1, 0.02);
        l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()), target.getX(), target.getY() + 0.2D, target.getZ(), 24, 0.8, 0.1, 0.8, 0.2);
        l.playSound(null, target.blockPosition(), ModSounds.SHARD_STRIKE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.3F);
        p.getCooldowns().addCooldown(w.getItem(), 60);
    }
}

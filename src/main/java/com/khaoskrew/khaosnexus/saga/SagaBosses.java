package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.dungeon.DungeonInstances;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.mob.ModEntities;
import com.khaoskrew.khaosnexus.party.PartyService;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Seal gates put the act's guardian at the end of the dungeon; killing it breaks the seal for the whole party. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SagaBosses {
    private SagaBosses() {}

    public static EntityType<?> guardianFor(@Nullable ResourceLocation theme) {
        return theme != null && theme.getPath().contains("crystal") ? ModEntities.VAELITH.get() : ModEntities.FALLEN_HIEROPHANT.get();
    }

    @Nullable
    public static Mob sealSwap(ServerLevel dungeon, NexusGateBlockEntity gate, @Nullable Mob boss) {
        String seal = gate.getPersistentData().getString(SealGateBlock.SEAL_TAG);
        if (seal.isEmpty() || boss == null) return boss;
        Mob guardian = GateArena.spawnMob(dungeon, GateRank.S, guardianFor(gate.getTheme()), boss.blockPosition(), boss.getYRot(), 1.0D, true);
        if (guardian == null) return boss;
        guardian.addTag("khaosnexus_dungeon_boss");
        guardian.getPersistentData().putLong(DungeonInstances.GEN_KEY, boss.getPersistentData().getLong(DungeonInstances.GEN_KEY));
        guardian.getPersistentData().putBoolean(SealGateBlock.SEAL_TAG, true);
        guardian.getPersistentData().putString(SealGateBlock.SEAL_TAG + "_id", seal);
        guardian.setPersistenceRequired();
        boss.discard();
        return guardian;
    }

    @SubscribeEvent
    public static void onGuardianDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        String seal = mob.getPersistentData().getString(SealGateBlock.SEAL_TAG + "_id");
        if (seal.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(seal);
        if (id == null) return;
        for (ServerPlayer p : PartyService.onlineMembers(killer)) {
            if (p.level() != mob.level() && p != killer) continue;
            var profile = HunterRanks.profile(p);
            if (!Saga.sealBroken(profile, id)) {
                Saga.breakSeal(profile, id);
                p.displayClientMessage(Component.translatable("khaosnexus.seal.broken", Component.translatable("khaosnexus.seal." + id.getPath())), false);
            }
            HunterRanks.sync(p);
        }
    }
}

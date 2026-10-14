package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.registry.ModRegistries;
import com.mojang.serialization.MapCodec;

import java.util.Comparator;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Seal Gate: the realm door used by every act. Opens the next unbroken seal (story order, data-driven in
 * khaosnexus_seals/) as an S-rank Nexus Gate in front of it, themed from that seal's dungeon themes. Reuses the
 * whole gate pipeline (parties, return point, safe graves). Seal gates are tagged so guardians/shards can tell.
 */
public class SealGateBlock extends Block {
    public static final MapCodec<SealGateBlock> CODEC = simpleCodec(SealGateBlock::new);
    public static final String SEAL_TAG = "khaosnexus_seal_gate";

    public SealGateBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<SealGateBlock> codec() { return CODEC; }

    /** The next seal for this hunter: the lowest-order unbroken one, or the last one for replays. */
    @Nullable
    public static ResourceLocation nextSeal(HunterProfile p) {
        var sorted = JsonPool.SEALS.all().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, com.google.gson.JsonObject> e) -> e.getValue().get("order").getAsInt())).toList();
        for (var e : sorted) if (!Saga.sealBroken(p, e.getKey())) return e.getKey();
        return sorted.isEmpty() ? null : sorted.get(sorted.size() - 1).getKey();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.CONSUME;
        BlockPos front = pos.south();
        if (sl.getBlockEntity(front) instanceof NexusGateBlockEntity gate && !gate.isClosed()) {
            return gate.activate(sp);
        }
        HunterProfile p = HunterRanks.profile(sp);
        ResourceLocation seal = nextSeal(p);
        if (seal == null || !(sp.isCreative() || Saga.canOpenSeal(p, seal))) {
            sp.displayClientMessage(Component.translatable("khaosnexus.seal.locked"), false);
            return InteractionResult.FAIL;
        }
        var json = JsonPool.SEALS.get(seal);
        Saga.spendShards(p, json.get("shard_cost").getAsInt());
        var themes = json.getAsJsonArray("themes");
        ResourceLocation theme = ResourceLocation.parse(themes.get(sl.random.nextInt(themes.size())).getAsString());
        sl.setBlock(front, ModRegistries.NEXUS_GATE.get().defaultBlockState(), 3);
        if (!(sl.getBlockEntity(front) instanceof NexusGateBlockEntity gate)) return InteractionResult.FAIL;
        gate.setRank(GateRank.S);
        gate.setTheme(theme);
        gate.getPersistentData().putString(SEAL_TAG, seal.toString());
        sp.displayClientMessage(Component.translatable("khaosnexus.seal.opened"), false);
        return gate.activate(sp);
    }
}

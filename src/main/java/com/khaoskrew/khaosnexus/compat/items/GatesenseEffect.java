package com.khaoskrew.khaosnexus.compat.items;

import java.util.Set;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Gatesense glyph: names the nearest open gate's rank and direction and draws a short particle trail toward it. */
public class GatesenseEffect extends AbstractEffect {
    public static final GatesenseEffect INSTANCE = new GatesenseEffect();

    private GatesenseEffect() {
        super(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "glyph_gatesense"), "Gatesense");
    }

    @Override
    public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity shooter, SpellStats stats, SpellContext ctx, SpellResolver resolver) {
        sense(level, shooter, stats);
    }

    @Override
    public void onResolveBlock(BlockHitResult hit, Level level, LivingEntity shooter, SpellStats stats, SpellContext ctx, SpellResolver resolver) {
        sense(level, shooter, stats);
    }

    /** Range in chunks: 4, +2 per Amplify or AOE. Public for tests. */
    public static NexusGateBlockEntity nearest(ServerLevel level, BlockPos from, int chunks) {
        NexusGateBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        int cx = from.getX() >> 4, cz = from.getZ() >> 4;
        for (int x = cx - chunks; x <= cx + chunks; x++) {
            for (int z = cz - chunks; z <= cz + chunks; z++) {
                LevelChunk c = level.getChunkSource().getChunkNow(x, z);
                if (c == null) continue;
                for (BlockEntity be : c.getBlockEntities().values()) {
                    if (be instanceof NexusGateBlockEntity g) {
                        double d = be.getBlockPos().distSqr(from);
                        if (d < bestD) { bestD = d; best = g; }
                    }
                }
            }
        }
        return best;
    }

    private static void sense(Level level, LivingEntity shooter, SpellStats stats) {
        if (!(level instanceof ServerLevel server) || !(shooter instanceof ServerPlayer p)) return;
        int chunks = 4 + 2 * (int) Math.max(0, stats.getAmpMultiplier() + stats.getAoeMultiplier());
        NexusGateBlockEntity g = nearest(server, p.blockPosition(), Math.min(chunks, 12));
        if (g == null) {
            p.displayClientMessage(Component.translatable("khaosnexus.gatesense.none"), true);
            return;
        }
        BlockPos at = g.getBlockPos();
        Vec3 dir = Vec3.atCenterOf(at).subtract(p.getEyePosition());
        int dist = (int) dir.length();
        p.displayClientMessage(Component.translatable("khaosnexus.gatesense.found",
                Component.translatable(g.getRank().translationKey()), dist, compass(dir)), true);
        Vec3 step = dir.normalize();
        for (int i = 2; i < Math.min(14, dist); i++) {
            Vec3 pt = p.getEyePosition().add(step.scale(i));
            server.sendParticles(p, ParticleTypes.REVERSE_PORTAL, true, pt.x, pt.y - 0.3D, pt.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    static String compass(Vec3 d) {
        String[] n = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        double a = Math.toDegrees(Math.atan2(-d.x, d.z));
        return n[(int) Math.floorMod(Math.round(a / 45.0D), 8)];
    }

    @Override
    public int getDefaultManaCost() {
        return 20;
    }

    @Override
    protected Set<AbstractAugment> getCompatibleAugments() {
        return this.augmentSetOf(AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE);
    }

    @Override
    public String getBookDescription() {
        return "Senses the nearest open gate: its rank, distance and direction. Amplify or AOE widens the search.";
    }
}

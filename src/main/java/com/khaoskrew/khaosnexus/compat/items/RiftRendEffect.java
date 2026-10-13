package com.khaoskrew.khaosnexus.compat.items;

import java.util.Set;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.mob.RiftMob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Rift Rend glyph: void damage that tears harder into rift mobs (x2) and leaves them weakened. */
public class RiftRendEffect extends AbstractEffect {
    public static final RiftRendEffect INSTANCE = new RiftRendEffect();

    private RiftRendEffect() {
        super(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "glyph_rift_rend"), "Rift Rend");
    }

    @Override
    public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity shooter, SpellStats stats, SpellContext ctx, SpellResolver resolver) {
        if (!(hit.getEntity() instanceof LivingEntity target) || !(level instanceof ServerLevel server)) {
            return;
        }
        double base = this.DAMAGE == null ? 4.0D : this.DAMAGE.get();
        double amp = this.AMP_VALUE == null ? 2.0D : this.AMP_VALUE.get();
        float dmg = (float) (base + amp * stats.getAmpMultiplier());
        boolean rift = target instanceof RiftMob;
        if (rift && SoftCompat.ARS.active()) {
            dmg *= 2.0F;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0));
        }
        target.hurt(level.damageSources().indirectMagic(shooter, shooter), Math.max(0.5F, dmg));
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.5D), target.getZ(), rift ? 30 : 12, 0.3D, 0.5D, 0.3D, 0.05D);
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        this.addDamageConfig(builder, 4.0D);
        this.addAmpConfig(builder, 2.0D);
    }

    @Override
    public int getDefaultManaCost() {
        return 35;
    }

    @Override
    protected Set<AbstractAugment> getCompatibleAugments() {
        return this.augmentSetOf(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public String getBookDescription() {
        return "Tears at the target with rift energy. Rift creatures take double damage and are weakened.";
    }
}

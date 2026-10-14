package com.khaoskrew.khaosnexus.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A Rift mob with a boss bar (used by the raid Warden and the Spire Guardian). */
public class RiftBoss extends RiftMob {
    /** Weapon drawn (in hand) vs sheathed (on the back). Synced so the client shows the right sword bone. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DRAWN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RiftBoss.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /** 0 = normal, 1 = enraged (below half health). Synced for the pulsing glow. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PHASE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RiftBoss.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.resources.ResourceLocation ENRAGE_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.khaoskrew.khaosnexus.KhaosNexus.MODID, "enrage");
    /** Ticks without a target before the boss sheathes again. */
    private static final int SHEATHE_DELAY = 100;
    private final ServerBossEvent bossEvent;
    private int calmTicks;

    public RiftBoss(EntityType<? extends RiftBoss> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10)
                .setDarkenScreen(true);
        this.xpReward = 50;
    }

    public static RiftBoss warden(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.RED);
    }

    public static RiftBoss vaelith(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.BLUE);
    }

    public static RiftBoss hierophant(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static RiftBoss general(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.WHITE);
    }

    /** Morvane, the Hollow King: three phases (66% / 33%), crownfall shard rain, a gold bar. */
    public static RiftBoss king(EntityType<? extends RiftBoss> type, Level level) {
        RiftBoss b = new RiftBoss(type, level, BossEvent.BossBarColor.YELLOW);
        b.xpReward = 400;
        return b;
    }

    public boolean isKing() {
        return this.getType() == ModEntities.HOLLOW_KING.get();
    }

    public static RiftBoss guardian(EntityType<? extends RiftBoss> type, Level level) {
        return new RiftBoss(type, level, BossEvent.BossBarColor.PURPLE);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAWN, false);
        builder.define(PHASE, 0);
    }

    /** Client: game time when DRAWN last changed, so the renderer swaps sword bones mid-draw. */
    private long drawChangedAt = -100;
    /** Length of the draw animation (1.3s); attacks and the roar wait until the ready stance. */
    public static final int DRAW_TICKS = 26;
    private int roarAt = -1;

    /** Client: true while the draw animation plays (renderer hides the hurt tint so the draw never reads as a flinch). */
    public boolean isDrawing() {
        return this.isWeaponDrawn() && this.tickCount - this.drawChangedAt < DRAW_TICKS;
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DRAWN.equals(key)) {
            this.drawChangedAt = this.tickCount;
        }
    }

    /** Sword shown in hand? Swaps 8 ticks into the draw/sheathe animation (the grab frame). */
    public boolean showSwordInHand() {
        boolean switching = this.tickCount - this.drawChangedAt < 8;
        return switching ? !this.isWeaponDrawn() : this.isWeaponDrawn();
    }

    @Override
    protected boolean canStartAttack() {
        return this.isWeaponDrawn() && this.tickCount - this.drawChangedAt >= DRAW_TICKS && this.special == null;
    }

    /** Movement controller: play the armed idle/walk. weapon out (the Warden waits for its draw to finish). */
    public boolean inCombatStance() {
        return this.isWeaponDrawn() && !(this.isWarden() && this.isDrawing());
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    private boolean isWarden() {
        return this.getType() == ModEntities.RIFT_WARDEN.get();
    }

    // ------------------------------------------------------------------ special attacks
    private String special;
    private int specialTick;
    private int specialCooldown = 100;
    private int barrierTicks;
    private boolean minionsCalled;

    /** Impact tick of each special, matching the animation keyframes in tools/gen_geo_assets.py. */
    private static int impactTick(String special) {
        return switch (special) {
            case "slam" -> 18;
            case "lunge" -> 10;
            case "spin" -> 14;
            case "volley" -> 12;
            case "crownfall" -> 18;
            default -> 8;
        };
    }

    private static int duration(String special) {
        return switch (special) {
            case "slam" -> 32;
            case "lunge" -> 26;
            case "spin" -> 28;
            case "volley" -> 22;
            case "crownfall" -> 32;
            default -> 24;
        };
    }

    private void startSpecial(String name) {
        this.special = name;
        this.specialTick = 0;
        this.triggerAnim("action", name);
        this.getNavigation().stop();
        if (this.isKing()) this.playSound("crownfall".equals(name) ? com.khaoskrew.khaosnexus.saga.ModSounds.CROWNFALL_RISE.get() : com.khaoskrew.khaosnexus.saga.ModSounds.KING_ROAR.get(), 1.6F, "crownfall".equals(name) ? 1.0F : 1.15F);
        else this.playSound(this.isWarden() ? net.minecraft.sounds.SoundEvents.RAVAGER_ROAR : net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F, 0.8F);
    }

    private void pickSpecial(net.minecraft.world.entity.LivingEntity target) {
        double d = this.distanceTo(target);
        boolean enraged = this.getPhase() > 0;
        if (this.isWarden()) {
            if (d < 5.5D) this.startSpecial("slam");
            else if (enraged && d < 16.0D && this.hasLineOfSight(target)) this.startSpecial("lunge");
            else return;
        } else if (this.isKing()) {
            int ph = this.getPhase();
            if (ph >= 1 && this.random.nextInt(3) == 0) this.startSpecial("crownfall");
            else if (ph >= 2 && this.barrierTicks <= 0 && this.random.nextInt(3) == 0) this.startSpecial("barrier");
            else if (d >= 5.0D && d < 22.0D && this.hasLineOfSight(target)) this.startSpecial("volley");
            else if (d < 4.8D) this.startSpecial("spin");
            else return;
            this.specialCooldown = (ph >= 2 ? 70 : ph == 1 ? 90 : 110) + this.random.nextInt(40);
            return;
        } else {
            boolean vaelith = this.getType() == ModEntities.VAELITH.get() || this.getType() == ModEntities.GENERAL_ARCANIST.get();
            if (enraged && this.barrierTicks <= 0 && this.random.nextBoolean()) this.startSpecial("barrier");
            else if (vaelith && d >= 5.0D && d < 20.0D && this.hasLineOfSight(target)) this.startSpecial("volley");
            else if (d < 4.5D) this.startSpecial("spin");
            else return;
        }
        this.specialCooldown = (enraged ? 90 : 140) + this.random.nextInt(60);
    }

    private void tickSpecial(net.minecraft.world.entity.LivingEntity target) {
        int t = ++this.specialTick;
        if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!"lunge".equals(this.special)) this.getNavigation().stop();
        if ("lunge".equals(this.special) && t == impactTick("lunge") && target != null) {
            net.minecraft.world.phys.Vec3 dir = target.position().subtract(this.position());
            float yaw = (float) (net.minecraft.util.Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;  // face the leap
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            double h = Math.min(dir.horizontalDistance(), 14.0D);
            dir = new net.minecraft.world.phys.Vec3(dir.x, 0, dir.z).normalize().scale(h * 0.11D);
            this.setDeltaMovement(dir.x, 0.75D, dir.z);
            this.hasImpulse = true;
        }
        if ("lunge".equals(this.special) && t > impactTick("lunge") + 4 && this.onGround()) {
            this.aoe(3.5D, 1.2F, 0.8D, true);
            this.special = null;
            return;
        }
        if ("crownfall".equals(this.special) && t == 4) this.markCrownfall();
        if (t == impactTick(this.special)) {
            switch (this.special) {
                case "crownfall" -> {
                    this.aoe(4.2D, 1.3F, 1.0D, true);
                    this.playSound(com.khaoskrew.khaosnexus.saga.ModSounds.CROWNFALL_IMPACT.get(), 2.5F, 1.0F);
                    this.crownfallTicks = 1;
                }
                case "slam" -> this.aoe(4.5D, 1.6F, 0.9D, true);
                case "spin" -> this.aoe(3.8D, 1.1F, 1.4D, false);
                case "volley" -> this.volley(target);
                case "barrier" -> {
                    this.barrierTicks = 120;
                    this.playSound(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 1.5F, 1.4F);
                }
                default -> { }
            }
        }
        if (t >= duration(this.special)) this.special = null;
    }

    // ------------------------------------------------------------------ Hollow King: crownfall
    private final java.util.List<net.minecraft.world.phys.Vec3> crownfallMarks = new java.util.ArrayList<>();
    private int crownfallTicks;
    private int rainCooldown = 160;

    /** Marks where every hunter in the throne room stands; shards land there 14+ ticks later (step out of the ring). */
    private void markCrownfall() {
        this.crownfallMarks.clear();
        for (net.minecraft.world.entity.player.Player p : this.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                this.getBoundingBox().inflate(28.0D, 8.0D, 28.0D), p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            this.crownfallMarks.add(p.position());
        }
        if (this.getTarget() != null && this.crownfallMarks.isEmpty()) this.crownfallMarks.add(this.getTarget().position());
    }

    private void tickCrownfall() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        if (this.crownfallTicks < 0 || (this.crownfallTicks == 0 && "crownfall".equals(this.special))) {
            for (net.minecraft.world.phys.Vec3 m : this.crownfallMarks) {   // warning ring while the King winds up
                double a = this.tickCount * 0.5D;
                for (int k = 0; k < 6; k++) server.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                        m.x + Math.cos(a + k) * 2.0D, m.y + 0.1D, m.z + Math.sin(a + k) * 2.0D, 1, 0, 0, 0, 0);
            }
        }
        if (this.crownfallTicks <= 0) return;
        int t = this.crownfallTicks++;
        if (t == 1 || t == 7) for (net.minecraft.world.phys.Vec3 m : this.crownfallMarks) this.shardStrike(server, m, t == 1);
        if (t > 8) { this.crownfallTicks = 0; this.crownfallMarks.clear(); }
    }

    /** One falling crown shard: 2.4-block radius, attack damage x0.9, flung up. */
    private void shardStrike(net.minecraft.server.level.ServerLevel server, net.minecraft.world.phys.Vec3 at, boolean first) {
        float dmg = (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.9F;
        for (net.minecraft.world.entity.LivingEntity e : server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(2.4D, 2.0D, 2.4D), e -> !(e instanceof RiftMob) && e.isAlive())) {
            if (e.hurt(this.damageSources().mobAttack(this), dmg)) { e.push(0, 0.6D, 0); e.hurtMarked = true; }
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y + 6.0D, at.z, 30, 0.2, 3.0, 0.2, 0.02);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, at.x, at.y + 0.3D, at.z, 2, 0.6, 0.1, 0.6, 0);
        server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()), at.x, at.y + 0.2D, at.z, 40, 1.0, 0.2, 1.0, 0.2);
        if (first) server.playSound(null, at.x, at.y, at.z, com.khaoskrew.khaosnexus.saga.ModSounds.SHARD_STRIKE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.8F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    /** Phase 2 at 66%: the King calls his court (three ghouls + a stalker). Phase 3 at 33%: full enrage. */
    private void kingPhase(int phase) {
        this.entityData.set(PHASE, phase);
        this.triggerAnim("action", "roar");
        this.playSound(com.khaoskrew.khaosnexus.saga.ModSounds.KING_PHASE.get(), 3.0F, 1.0F);
        this.specialCooldown = 30;
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var speed = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var damage = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double k = phase == 1 ? 0.10D : 0.22D;
        if (speed != null) speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ENRAGE_ID, k, mul));
        if (damage != null) damage.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ENRAGE_ID, k * 1.2D, mul));
        this.bossEvent.setColor(phase == 1 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        if (phase == 1 && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            for (int i = 0; i < 3; i++) {
                RiftMob add = (i == 2 ? ModEntities.RIFT_STALKER.get() : ModEntities.RIFT_GHOUL.get()).create(server);
                if (add == null) continue;
                double a = Math.PI * 2 * i / 3 + 0.4D;
                add.moveTo(this.getX() + Math.cos(a) * 4.0, this.getY(), this.getZ() + Math.sin(a) * 4.0, this.getYRot(), 0);
                add.setGateRank(this.getGateRank());
                add.addTag("khaosnexus_king_court");
                server.addFreshEntity(add);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, add.getX(), add.getY() + 1, add.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            }
        }
        if (phase == 2) { this.barrierTicks = 80; this.playSound(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 1.5F, 0.8F); }
    }

    /** Vaelith: three glass shards fanned at the target (one aimed, two 12 degrees off), each a normal void orb hit. */
    private void volley(net.minecraft.world.entity.LivingEntity target) {
        if (target == null) return;
        net.minecraft.world.phys.Vec3 from = this.position().add(0, 3.0, 0).add(this.getLookAngle().scale(1.2));
        net.minecraft.world.phys.Vec3 aim = target.getEyePosition().subtract(0, 0.4, 0).subtract(from);
        for (int i = -1; i <= 1; i++) {
            VoidOrb orb = new VoidOrb(this.level(), this, aim.yRot((float) Math.toRadians(12 * i)));
            orb.setPos(from);
            this.level().addFreshEntity(orb);
        }
        this.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 1.3F);
        this.playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK, 1.0F, 1.6F);
    }

    /** Hits every non-rift living thing in radius for attack damage * mult, with a shockwave of particles. */
    private void aoe(double radius, float mult, double knock, boolean slam) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        float dmg = (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * mult;
        for (net.minecraft.world.entity.LivingEntity e : server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(radius, 1.5D, radius), e -> !(e instanceof RiftMob) && e.isAlive() && this.distanceTo(e) <= radius + 0.5D)) {
            if (e.hurt(this.damageSources().mobAttack(this), dmg)) {
                net.minecraft.world.phys.Vec3 push = e.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(knock);
                e.push(push.x, slam ? 0.55D : 0.25D, push.z);
                e.hurtMarked = true;
            }
        }
        for (int i = 0; i < 40; i++) {   // expanding ring
            double a = Math.PI * 2 * i / 40;
            server.sendParticles(slam ? net.minecraft.core.particles.ParticleTypes.CRIT : net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    this.getX() + Math.cos(a) * radius * 0.8, this.getY() + 0.2, this.getZ() + Math.sin(a) * radius * 0.8, 1, 0.1, 0.05, 0.1, 0.05);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    this.getX() + Math.cos(a) * radius * 0.5, this.getY() + 0.3, this.getZ() + Math.sin(a) * radius * 0.5, 2, 0.1, 0.1, 0.1, 0.1);
        }
        if (slam) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3, this.getZ(), 3, 1.0, 0.1, 1.0, 0.0);
            this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.7F);
        } else {
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
        }
    }

    private void enrage() {
        this.entityData.set(PHASE, 1);
        this.triggerAnim("action", "roar");
        this.playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.6F);
        var speed = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var damage = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        if (speed != null) speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ENRAGE_ID, 0.25D, mul));
        if (damage != null) damage.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ENRAGE_ID, 0.3D, mul));
        this.bossEvent.setColor(BossEvent.BossBarColor.PINK);
        this.specialCooldown = 30;
        if (!this.isWarden() && !this.minionsCalled && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            this.minionsCalled = true;   // Guardian calls two ghouls through rift sparks
            for (int i = 0; i < 2; i++) {
                RiftMob ghoul = ModEntities.RIFT_GHOUL.get().create(server);
                if (ghoul == null) continue;
                double a = Math.PI * i + this.random.nextDouble();
                ghoul.moveTo(this.getX() + Math.cos(a) * 2.5, this.getY(), this.getZ() + Math.sin(a) * 2.5, this.getYRot(), 0);
                ghoul.setGateRank(this.getGateRank());
                server.addFreshEntity(ghoul);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, ghoul.getX(), ghoul.getY() + 1, ghoul.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this.barrierTicks > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 0.4F;   // arcane barrier soaks 60%
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("WeaponDrawn", this.isWeaponDrawn());
        tag.putInt("Phase", this.getPhase());
    }

    public boolean isWeaponDrawn() {
        return this.entityData.get(DRAWN);
    }

    private void setWeaponDrawn(boolean drawn) {
        if (drawn != this.isWeaponDrawn()) {
            this.entityData.set(DRAWN, drawn);
            this.drawChangedAt = this.tickCount;
            this.triggerAnim("action", drawn ? "draw" : "sheathe");
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (this.isKing()) {
            float f = this.getHealth() / this.getMaxHealth();
            if (this.getPhase() == 0 && f <= 0.66F) this.kingPhase(1);
            else if (this.getPhase() == 1 && f <= 0.33F) this.kingPhase(2);
            this.tickCrownfall();
            if (this.getPhase() >= 2 && target != null && --this.rainCooldown <= 0 && this.special == null) {   // ambient shard rain
                this.rainCooldown = 140 + this.random.nextInt(60);
                this.markCrownfall();
                this.crownfallTicks = -14;
            }
            if (this.crownfallTicks < 0 && ++this.crownfallTicks == 0) this.crownfallTicks = 1;
        } else if (this.getPhase() == 0 && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.enrage();
        }
        if (this.barrierTicks > 0 && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (--this.barrierTicks % 3 == 0) {
                double a = this.tickCount * 0.4D;
                for (int k = 0; k < 4; k++) {
                    double b = a + Math.PI * k / 2;
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX() + Math.cos(b) * 1.6,
                            this.getY() + 0.5 + (this.tickCount % 20) * 0.15, this.getZ() + Math.sin(b) * 1.6, 1, 0, 0, 0, 0);
                }
            }
        }
        if (this.roarAt >= 0 && this.tickCount >= this.roarAt) {
            this.roarAt = -1;
            this.triggerAnim("action", "roar");
        }
        if (this.special != null) {
            this.tickSpecial(target);
        } else if (target != null && target.isAlive() && this.isWeaponDrawn() && !this.isWindingUp() && --this.specialCooldown <= 0) {
            this.pickSpecial(target);
        }
        if (target != null && target.isAlive()) {
            this.calmTicks = 0;
            this.setWeaponDrawn(true);
        } else if (this.isWeaponDrawn() && ++this.calmTicks > SHEATHE_DELAY) {
            this.setWeaponDrawn(false);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DRAWN, tag.getBoolean("WeaponDrawn"));
        if (tag.getInt("Phase") > 0) this.entityData.set(PHASE, this.isKing() ? Math.min(2, tag.getInt("Phase")) : 1);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void setTarget(net.minecraft.world.entity.LivingEntity target) {
        boolean newTarget = target != null && this.getTarget() == null;
        super.setTarget(target);
        if (newTarget && !this.level().isClientSide) {
            boolean wasDrawn = this.isWeaponDrawn();
            this.setWeaponDrawn(true);
            if (wasDrawn) {
                this.triggerAnim("action", "roar");
            } else {
                this.roarAt = this.tickCount + DRAW_TICKS;   // roar after the draw finishes, never over it
            }
        }
    }
}

package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.CuriosCompat;
import com.khaoskrew.khaosnexus.dungeon.DimensionSafety;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.mob.ModEntities;
import com.khaoskrew.khaosnexus.mob.RiftMob;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Hollow Throne: Morvane's hall on its own rock deep in the Underthrone. Opened from the Seal Gate once every
 * seal is broken (sneak-use still replays the last seal). Arriving wakes the King if he is not already holding court;
 * his fall is recorded on the hunter's saga (King tag) and the room returns everyone to the camp shortly after.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class HollowThrone {
    public static final BlockPos ARENA = new BlockPos(0, 80, -400);
    public static final int RADIUS = 18;
    public static final String KING_TAG = "khaosnexus_hollow_king";
    private static long returnAt = -1;
    private static long emptySince = -1;

    private HollowThrone() {}

    public static BlockPos arrival(BlockPos o) { return o.offset(0, 0, 13); }
    public static BlockPos kingSpot(BlockPos o) { return o.offset(0, 0, -6); }

    public static boolean allSealsBroken(com.khaoskrew.khaosnexus.hunter.HunterProfile p) {
        var all = com.khaoskrew.khaosnexus.endgame.JsonPool.SEALS.all();
        if (all.isEmpty()) return false;
        for (var id : all.keySet()) if (!Saga.sealBroken(p, id)) return false;
        return true;
    }

    public static boolean built(ServerLevel l) {
        return l.getBlockState(ARENA.offset(0, -3, 0)).is(Blocks.LODESTONE);   // v2 hall marker (older halls rebuild)
    }

    /** Takes the hunter and nearby party members to the throne room and wakes the King. */
    public static boolean enter(ServerPlayer player) {
        ServerLevel under = player.server.getLevel(Saga.UNDERTHRONE);
        if (under == null) return false;
        under.getChunk(ARENA);
        if (!built(under)) build(under, ARENA);
        List<ServerPlayer> group = new java.util.ArrayList<>();
        for (ServerPlayer p : PartyService.onlineMembers(player)) if (p == player || (p.level() == player.level() && p.distanceTo(player) < 12.0F)) group.add(p);
        if (!group.contains(player)) group.add(player);
        boolean ok = false;
        for (ServerPlayer p : group) {
            if (DimensionSafety.teleport(p, Saga.UNDERTHRONE, arrival(ARENA), 180.0F, true)) {
                ok = true;
                p.displayClientMessage(Component.translatable("khaosnexus.king.enter"), false);
            }
        }
        if (ok) { summon(under); returnAt = -1; emptySince = -1; }
        return ok;
    }

    @Nullable
    public static Mob king(ServerLevel l) {
        return l.getEntitiesOfClass(Mob.class, new AABB(ARENA).inflate(RADIUS + 10, 24, RADIUS + 10), m -> m.getTags().contains(KING_TAG) && m.isAlive())
                .stream().findFirst().orElse(null);
    }

    @Nullable
    public static Mob summon(ServerLevel l) {
        Mob existing = king(l);
        if (existing != null) return existing;
        Mob k = GateArena.spawnMob(l, GateRank.S, ModEntities.HOLLOW_KING.get(), kingSpot(ARENA), 0.0F, 1.0D, true);
        if (k == null) return null;
        k.setCustomName(k.getType().getDescription().copy());
        k.addTag(KING_TAG);
        k.setPersistenceRequired();
        k.restrictTo(ARENA, RADIUS);
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, k.getX(), k.getY() + 2, k.getZ(), 120, 0.8, 2.0, 0.8, 0.2);
        return k;
    }

    @SubscribeEvent
    public static void onKingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.getTags().contains(KING_TAG) || !(mob.level() instanceof ServerLevel l)) return;
        for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, new AABB(ARENA).inflate(RADIUS + 12, 30, RADIUS + 12))) {
            int n = Saga.slayKing(HunterRanks.profile(p));
            p.displayClientMessage(Component.translatable(n == 1 ? "khaosnexus.king.slain_first" : "khaosnexus.king.slain", n), false);
            HunterRanks.sync(p);
        }
        for (Mob m : l.getEntitiesOfClass(Mob.class, new AABB(ARENA).inflate(RADIUS + 12, 30, RADIUS + 12), m -> m.getTags().contains("khaosnexus_king_court"))) m.kill();
        returnAt = l.getGameTime() + 260;
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel l) || l.dimension() != Saga.UNDERTHRONE || l.getGameTime() % 20 != 0) return;
        List<ServerPlayer> inside = l.getEntitiesOfClass(ServerPlayer.class, new AABB(ARENA).inflate(RADIUS + 12, 40, RADIUS + 12));
        if (returnAt >= 0 && l.getGameTime() >= returnAt) {
            returnAt = -1;
            for (ServerPlayer p : inside) {
                DimensionSafety.teleport(p, Saga.UNDERTHRONE, ExileCamp.arrival(ExileCamp.ORIGIN), 180.0F, true);
                p.displayClientMessage(Component.translatable("khaosnexus.king.return"), false);
            }
        }
        Mob k = inside.isEmpty() ? king(l) : null;
        if (k != null) {   // nobody left in the hall: the King returns to his rest (full reset next visit)
            if (emptySince < 0) emptySince = l.getGameTime();
            else if (l.getGameTime() - emptySince > 600) { k.discard(); emptySince = -1; }
        } else emptySince = -1;
    }

    /** Hollow Crown worn (Curios head/charm) or held off-hand: +8% damage dealt, -6% taken. Kingsbane: +20% vs rift things. */
    @SubscribeEvent
    public static void kingRelics(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer att) {
            int c = crown(att);
            if (c > 0) event.setAmount(event.getAmount() * (c == 2 ? 1.12F : 1.08F));
            boolean bane = att.getMainHandItem().is(ModRegistries.KINGSBANE.get()) || att.getMainHandItem().is(ModRegistries.AWAKENED_KINGSBANE.get());
            if (bane && event.getEntity() instanceof RiftMob) event.setAmount(event.getAmount() * 1.2F);
        }
        if (event.getEntity() instanceof ServerPlayer vic) {
            int c = crown(vic);
            if (c > 0) event.setAmount(event.getAmount() * (c == 2 ? 0.90F : 0.94F));
        }
    }

    /** 0 = no crown, 1 = Hollow Crown, 2 = Awakened Crown (worn in Curios or held off-hand). */
    public static int crown(ServerPlayer p) {
        if (p.getOffhandItem().is(ModRegistries.AWAKENED_CROWN.get()) || CuriosCompat.wears(p, ModRegistries.AWAKENED_CROWN.get())) return 2;
        return p.getOffhandItem().is(ModRegistries.HOLLOW_CROWN.get()) || CuriosCompat.wears(p, ModRegistries.HOLLOW_CROWN.get()) ? 1 : 0;
    }

    /** The Hollow Throne: a stepped gothic backrest with a glowing crying-obsidian seam, a halo ring of walls behind,
     *  skull-capped armrests, gilded finials, hanging soul lanterns and a crown of candles. Seat faces south. */
    static void throne(ServerLevel l, BlockPos t) {
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), chis = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState gild = Blocks.GILDED_BLACKSTONE.defaultBlockState(), wall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        set(l, t, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        set(l, t.below(), chis);
        for (int x : new int[] {-1, 1}) {   // armrests
            set(l, t.offset(x, 0, 0), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, x < 0 ? Direction.WEST : Direction.EAST)
                    .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            set(l, t.offset(x, 0, 1), wall);
            set(l, t.offset(x, 1, 1), Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(net.minecraft.world.level.block.SkullBlock.ROTATION, 8));
        }
        int[] h = {5, 8, 12, 8, 5};   // stepped backrest, tallest at the centre
        for (int i = 0; i < 5; i++) {
            int x = i - 2;
            for (int y = 0; y <= h[i]; y++) {
                BlockState b = y == h[i] ? gild : x == 0 && y >= 1 && y <= 7 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : y % 4 == 0 ? chis : bricks;
                set(l, t.offset(x, y, -1), b);
            }
            set(l, t.offset(x, h[i] + 1, -1), Math.abs(x) == 2 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState() : Blocks.CHAIN.defaultBlockState());
            if (Math.abs(x) == 1) set(l, t.offset(x, h[i] + 2, -1), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 4));
            if (Math.abs(x) == 2) set(l, t.offset(x, h[i] + 2, -1), gild);
        }
        for (int x : new int[] {-2, 2}) for (int y = 1; y <= 4; y++)   // upside-down stair buttresses on the sides
            set(l, t.offset(x, y, 0), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH)
                    .setValue(StairBlock.HALF, y % 2 == 0 ? net.minecraft.world.level.block.state.properties.Half.TOP : net.minecraft.world.level.block.state.properties.Half.BOTTOM));
        set(l, t.offset(0, 13, -1), Blocks.CHAIN.defaultBlockState()); set(l, t.offset(0, 14, -1), gild);
        for (int x = -1; x <= 1; x++) set(l, t.offset(x, 15, -1), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
        for (int k = 0; k < 20; k++) {   // halo ring behind the backrest
            double a = Math.PI * 2 * k / 20;
            BlockPos p = t.offset((int) Math.round(Math.cos(a) * 5), 7 + (int) Math.round(Math.sin(a) * 5), -2);
            set(l, p, k % 5 == 0 ? gild : wall);
        }
        for (int x : new int[] {-4, 4}) {   // hanging soul lanterns on chains from the halo
            for (int y = 9; y >= 7; y--) set(l, t.offset(x, y, -1), Blocks.CHAIN.defaultBlockState());
            set(l, t.offset(x, 6, -1), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        set(l, t.offset(0, 4, 1), Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12));
    }

    // ------------------------------------------------------------------ the hall
    static void set(ServerLevel l, BlockPos p, BlockState s) { l.setBlock(p, s, 2); }

    public static void build(ServerLevel l, BlockPos o) {
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-RADIUS - 3, 0, -RADIUS - 3), o.offset(RADIUS + 3, 20, RADIUS + 3))) set(l, p, Blocks.AIR.defaultBlockState());
        ExileCamp.island(l, o, RADIUS, 22);
        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) {   // floor: rings + gilded spokes
            double d = Math.sqrt(x * x + z * z);
            if (d > RADIUS - 1.5) continue;
            int r = ExileCamp.h(x, z), ring = (int) d;
            double ang = Math.atan2(z, x) / (Math.PI / 4);
            boolean spoke = Math.abs(ang - Math.round(ang)) < 0.6 / Math.max(1, d) && d > 3;
            BlockState s = spoke ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : ring % 5 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                    : r < 18 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : r < 26 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            set(l, o.offset(x, -1, z), s);
        }
        set(l, o.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        for (int i = 0; i < 10; i++) {   // ring of broken pillars, every other one with a soul brazier
            double a = Math.PI * 2 * i / 10 + 0.31;
            BlockPos c = o.offset((int) Math.round(Math.cos(a) * 14), 0, (int) Math.round(Math.sin(a) * 14));
            int tall = 5 + ExileCamp.h(i, 7) % 7;
            for (int y = 0; y < tall; y++) set(l, c.above(y), (y % 4 == 3 ? Blocks.CHISELED_POLISHED_BLACKSTONE : y % 3 == 1 ? Blocks.CRYING_OBSIDIAN : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            for (Direction d : Direction.Plane.HORIZONTAL) set(l, c.relative(d), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, d.getOpposite()));
            if (i % 2 == 0) { set(l, c.above(tall), Blocks.SOUL_CAMPFIRE.defaultBlockState()); }
            else { set(l, c.above(tall), Blocks.GILDED_BLACKSTONE.defaultBlockState()); for (int y = 1; y <= 3; y++) set(l, c.above(tall - y).relative(Direction.NORTH), Blocks.CHAIN.defaultBlockState()); }
            if (ExileCamp.h(i, 3) % 3 == 0) set(l, c.offset(1, 0, 2), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());   // rubble
        }
        // north dais and the Hollow Throne
        for (int step = 0; step < 3; step++) for (int x = -6 + step; x <= 6 - step; x++) for (int z = -15; z <= -8 + step * -1; z++)
            set(l, o.offset(x, step, z), (step == 2 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_DEEPSLATE).defaultBlockState());
        for (int x = -4; x <= 4; x++) set(l, o.offset(x, 0, -7), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        throne(l, o.offset(0, 3, -12));
        set(l, o.offset(0, -3, 0), Blocks.LODESTONE.defaultBlockState());
        for (int z = -7; z <= 12; z++) for (int x = -1; x <= 1; x++)   // crimson runner from the arrival to the dais
            set(l, o.offset(x, z == -7 ? 1 : 0, z), (Math.abs(x) == 1 ? Blocks.BLACK_CARPET : Blocks.RED_CARPET).defaultBlockState());
        for (int x : new int[] {-3, 3}) for (int z = -5; z <= 11; z += 4) {   // candle stands lining the runner
            set(l, o.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            set(l, o.offset(x, 1, z), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
        }
        for (int x : new int[] {-5, 5}) {   // banners of the crown flanking the dais
            for (int y = 0; y <= 7; y++) set(l, o.offset(x, 2 + y, -14), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            set(l, o.offset(x, 8, -13), Blocks.RED_WALL_BANNER.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.SOUTH));
            set(l, o.offset(x, 10, -14), Blocks.SOUL_LANTERN.defaultBlockState());
        }
        // south arrival steps
        for (int x = -2; x <= 2; x++) set(l, o.offset(x, -1, 13), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 10);
        for (int x = -RADIUS + 3; x <= RADIUS - 3; x += 6) for (int z = -RADIUS + 3; z <= RADIUS - 3; z += 6)
            if (x * x + z * z < (RADIUS - 3) * (RADIUS - 3) && l.getBlockState(o.offset(x, 4, z)).isAir()) set(l, o.offset(x, 4, z), light);
    }
}

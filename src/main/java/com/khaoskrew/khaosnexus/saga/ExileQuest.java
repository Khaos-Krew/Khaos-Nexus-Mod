package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.forge.HunterForge;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * "The Crownless Road": the Exile Camp questline. Three named exiles run it:
 * Captain Ysolde Marrow (quest giver), Brother Caddoc Venn (keeper of relics: Choir Mask and Censer turn-ins, cosmetics)
 * and Wren Halloway (quartermaster, stock grows with the turn-ins).
 * Steps: 1 meet the Captain, 2 fell a ward, 3 surrender the Choir Mask, 4 surrender the Censer, 5 break the seal,
 * 6 slay the Hollow King. The Hollow Throne opens only after steps 3-5.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class ExileQuest {
    public static final String NPC = "khaosnexus.exile_npc";
    public static final String CAPTAIN = "captain", KEEPER = "keeper", QUARTERMASTER = "quartermaster";
    public static final String HALO = "halo", TRAIL = "trail";

    private ExileQuest() {}

    static CompoundTag q(HunterProfile p) {
        CompoundTag t = Saga.tag(p).getCompound("Quest");
        Saga.tag(p).put("Quest", t);
        return t;
    }

    public static boolean flag(HunterProfile p, String f) { return q(p).getBoolean(f); }
    public static void set(HunterProfile p, String f) { q(p).putBoolean(f, true); }

    /** Both relics surrendered: the Captain will lead the way to the throne once the seal is broken. */
    public static boolean relicsGiven(HunterProfile p) { return flag(p, "mask") && flag(p, "censer"); }

    /** Current step 1..7 (7 = complete). */
    public static int step(HunterProfile p) {
        if (!flag(p, "met")) return 1;
        boolean anyWard = !Saga.tag(p).getCompound("Wards").isEmpty();
        if (!anyWard && !HollowThrone.allSealsBroken(p)) return 2;
        if (!flag(p, "mask")) return 3;
        if (!flag(p, "censer")) return 4;
        if (!HollowThrone.allSealsBroken(p)) return 5;
        if (Saga.kingsSlain(p) <= 0) return 6;
        return 7;
    }

    // ------------------------------------------------------------------ NPCs
    record Spot(String role, int dx, int dz, float yaw) {}
    static final Spot[] SPOTS = {new Spot(CAPTAIN, 3, 4, 20.0F), new Spot(KEEPER, -3, -12, 0.0F), new Spot(QUARTERMASTER, 6, -4, 90.0F)};

    public static void ensure(ServerLevel l, BlockPos o) {
        var near = l.getEntitiesOfClass(WanderingTrader.class, new AABB(o).inflate(ExileCamp.RADIUS + 4, 12, ExileCamp.RADIUS + 4), t -> t.getPersistentData().contains(NPC));
        if (near.size() >= SPOTS.length) return;
        near.forEach(WanderingTrader::discard);
        for (Spot s : SPOTS) {
            WanderingTrader npc = EntityType.WANDERING_TRADER.create(l);
            if (npc == null) continue;
            npc.moveTo(o.getX() + s.dx + 0.5D, o.getY(), o.getZ() + s.dz + 0.5D, s.yaw, 0.0F);
            npc.setYHeadRot(s.yaw); npc.setYBodyRot(s.yaw);
            npc.setNoAi(true); npc.setInvulnerable(true); npc.setPersistenceRequired(); npc.setDespawnDelay(0); npc.setSilent(true);
            npc.setCustomName(Component.translatable("khaosnexus.exile." + s.role));
            npc.setCustomNameVisible(true);
            npc.getPersistentData().putString(NPC, s.role);
            npc.getOffers().clear();
            l.addFreshEntity(npc);
        }
    }

    /** Wren's stock: base camp supplies, plus glasswork after the Mask and ember goods after the Censer. */
    public static MerchantOffers stock(HunterProfile p) {
        MerchantOffers o = new MerchantOffers();
        Item shard = ModRegistries.CROWN_SHARD.get(), core = HunterForge.RIFT_CORE.get(), ess = HunterForge.RIFT_ESSENCE.get();
        o.add(offer(ess, 6, null, 0, new ItemStack(Items.GOLDEN_CARROT, 8)));
        o.add(offer(ess, 10, null, 0, new ItemStack(Items.ENDER_PEARL, 4)));
        o.add(offer(core, 2, null, 0, new ItemStack(Items.TOTEM_OF_UNDYING)));
        o.add(offer(core, 1, null, 0, new ItemStack(shard, 1)));
        if (flag(p, "mask")) {
            o.add(offer(ModRegistries.SINGING_GLASS.get(), 3, null, 0, new ItemStack(Items.AMETHYST_SHARD, 16)));
            o.add(offer(shard, 3, ModRegistries.SINGING_GLASS.get(), 2, new ItemStack(Items.SPYGLASS)));
            o.add(offer(shard, 4, null, 0, new ItemStack(ModRegistries.SINGING_GLASS.get(), 2)));
        }
        if (flag(p, "censer")) {
            o.add(offer(ModRegistries.PROFANE_SIGIL.get(), 2, null, 0, new ItemStack(Items.BLAZE_ROD, 8)));
            o.add(offer(shard, 4, null, 0, new ItemStack(ModRegistries.PROFANE_SIGIL.get(), 2)));
            o.add(offer(shard, 8, core, 4, new ItemStack(ModRegistries.HEART_EMBER.get(), 1)));
        }
        return o;
    }

    static MerchantOffer offer(Item a, int ac, Item b, int bc, ItemStack out) {
        return new MerchantOffer(new ItemCost(a, ac), b == null ? Optional.empty() : Optional.of(new ItemCost(b, bc)), out, 12, 0, 0.0F);
    }

    static void say(ServerPlayer sp, String key, Object... args) {
        sp.displayClientMessage(Component.translatable(key, args).withStyle(net.minecraft.ChatFormatting.GOLD), false);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof WanderingTrader npc) || !npc.getPersistentData().contains(NPC)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) { event.setCanceled(true); return; }
        String role = npc.getPersistentData().getString(NPC);
        HunterProfile p = HunterRanks.profile(sp);
        if (QUARTERMASTER.equals(role)) {
            npc.getOffers().clear();
            npc.getOffers().addAll(stock(p));
            return;   // opens the trade screen as usual
        }
        event.setCanceled(true);
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (CAPTAIN.equals(role)) {
            if (!flag(p, "met")) { set(p, "met"); say(sp, "khaosnexus.quest.captain.intro"); }
            else say(sp, "khaosnexus.quest.step." + step(p));
        } else if (KEEPER.equals(role)) {
            if (sp.isShiftKeyDown()) { cycleCosmetic(sp, p); HunterRanks.sync(sp); return; }
            ItemStack held = sp.getMainHandItem();
            if (held.is(ModRegistries.VAELITH_CHOIR_MASK.get()) && !flag(p, "mask")) {
                held.shrink(1); set(p, "mask"); say(sp, "khaosnexus.quest.keeper.mask");
            } else if (held.is(ModRegistries.HIEROPHANT_CENSER.get()) && !flag(p, "censer")) {
                held.shrink(1); set(p, "censer"); say(sp, "khaosnexus.quest.keeper.censer");
            } else say(sp, flag(p, "mask") && flag(p, "censer") ? "khaosnexus.quest.keeper.done" : "khaosnexus.quest.keeper.ask");
            if (relicsGiven(p)) say(sp, "khaosnexus.quest.step." + step(p));
        }
        HunterRanks.sync(sp);
    }

    // ------------------------------------------------------------------ cosmetics (glass halo / ember censer trail)
    public static String cosmetic(HunterProfile p) { return q(p).getString("cosmetic"); }

    static void cycleCosmetic(ServerPlayer sp, HunterProfile p) {
        java.util.List<String> opts = new java.util.ArrayList<>(java.util.List.of(""));
        if (flag(p, "mask")) opts.add(HALO);
        if (flag(p, "censer")) opts.add(TRAIL);
        String next = opts.get((opts.indexOf(cosmetic(p)) + 1) % opts.size());
        q(p).putString("cosmetic", next);
        say(sp, "khaosnexus.quest.cosmetic." + (next.isEmpty() ? "none" : next));
    }

    @SubscribeEvent
    public static void cosmetics(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 4 != 0 || sp.isSpectator() || sp.isInvisible()) return;
        String c = cosmetic(HunterRanks.profile(sp));
        if (c.isEmpty()) return;
        ServerLevel l = sp.serverLevel();
        if (HALO.equals(c)) {
            double a = sp.tickCount * 0.15D;
            for (int k = 0; k < 3; k++) {
                double b = a + Math.PI * 2 * k / 3;
                l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, sp.getX() + Math.cos(b) * 0.45D, sp.getY() + sp.getBbHeight() + 0.35D, sp.getZ() + Math.sin(b) * 0.45D, 1, 0, 0, 0, 0);
            }
        } else if (TRAIL.equals(c) && sp.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3D) {
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, sp.getX(), sp.getY() + 0.15D, sp.getZ(), 2, 0.15, 0.05, 0.15, 0.005);
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, sp.getX(), sp.getY() + 0.3D, sp.getZ(), 1, 0.1, 0.05, 0.1, 0.005);
        }
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, path); }
}

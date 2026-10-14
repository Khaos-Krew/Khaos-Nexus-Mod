package com.khaoskrew.khaosnexus.compat;

import java.lang.reflect.Method;
import java.util.Map;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Apotheosis class-match gem bonus (1.2): each of our class gems socketed in worn gear or the main hand, whose class
 * matches the hunter's equipped class, adds +4% damage dealt and -2% damage taken (up to 4 gems). Socket data is read
 * through Apotheosis' SocketHelper by reflection, so nothing links when Apotheosis is absent.
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class GemClassBonus {
    private GemClassBonus() {}

    public static final Map<String, HunterClass> GEM_CLASS = Map.of(
            "bulwark", HunterClass.TANK, "shadowfang", HunterClass.ASSASSIN, "arcanum", HunterClass.CASTER, "hawkeye", HunterClass.RANGER,
            "lifethread", HunterClass.LIFEBINDER, "bloodrage", HunterClass.BERSERKER, "legion", HunterClass.SUMMONER, "phylactery", HunterClass.NECROMANCER);
    public static final int CAP = 4;
    static final String KEY = "khaosnexus_gem_match";
    private static Method getGems, gemHolder, holderId;
    private static boolean failed;

    /** Class of one of our gem ids, or null (generic gems like riftheart/gatekeeper/voidglass have none). */
    public static HunterClass classOf(ResourceLocation gem) {
        return gem != null && KhaosNexus.MODID.equals(gem.getNamespace()) ? GEM_CLASS.get(gem.getPath()) : null;
    }

    public static float dealtMult(int matches) { return 1.0F + 0.04F * Math.min(CAP, matches); }
    public static float takenMult(int matches) { return 1.0F - 0.02F * Math.min(CAP, matches); }

    static int countMatches(ServerPlayer p, HunterClass c) {
        if (c == null || failed || !ApothCompat.active()) return 0;
        int n = 0;
        try {
            if (getGems == null) {
                getGems = Class.forName("dev.shadowsoffire.apotheosis.socket.SocketHelper").getMethod("getGems", ItemStack.class);
                gemHolder = Class.forName("dev.shadowsoffire.apotheosis.socket.gem.GemInstance").getMethod("gem");
                holderId = Class.forName("dev.shadowsoffire.placebo.reload.DynamicHolder").getMethod("getId");
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack s = p.getItemBySlot(slot);
                if (s.isEmpty() || slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.BODY) continue;
                for (Object gi : (Iterable<?>) getGems.invoke(null, s)) {
                    Object id = holderId.invoke(gemHolder.invoke(gi));
                    if (id instanceof ResourceLocation rl && classOf(rl) == c) n++;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            KhaosNexus.LOGGER.warn("Apotheosis gem class bonus disabled: {}", e.toString());
            return 0;
        }
        return Math.min(CAP, n);
    }

    public static int matches(ServerPlayer p) {
        return p.getPersistentData().getInt(KEY);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 40 == 0 && ApothCompat.active()) {
            p.getPersistentData().putInt(KEY, countMatches(p, ClassService.equipped(p)));
        }
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && matches(p) > 0) e.setAmount(e.getAmount() * dealtMult(matches(p)));
        if (e.getEntity() instanceof ServerPlayer p && matches(p) > 0) e.setAmount(e.getAmount() * takenMult(matches(p)));
    }
}

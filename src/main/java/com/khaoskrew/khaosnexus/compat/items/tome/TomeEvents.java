package com.khaoskrew.khaosnexus.compat.items.tome;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.khaoskrew.khaosnexus.compat.items.CompatItems;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.mob.RiftMob;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Tome evolution and class attunements. Registered on the game bus only when Ars Nouveau is installed. */
public final class TomeEvents {
    private TomeEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TomeEvents::tick);
        NeoForge.EVENT_BUS.addListener(TomeEvents::damage);
        NeoForge.EVENT_BUS.addListener(TomeEvents::cast);
    }

    static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 40 == 0) evolveAll(p);
    }

    /** Evolves every Hunter Tome in the inventory to the owner's tier. Returns how many evolved. Public for tests. */
    public static int evolveAll(Player p) {
        int target = HunterTomes.tierFor(p), n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof HunterTomeItem t && t.tomeTier < target) {
                inv.setItem(i, evolve(s, target)); n++;
            }
        }
        if (n > 0 && p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("khaosnexus.tome.evolved", Component.translatable("khaosnexus.tome.tier." + HunterTomes.TIER_IDS[target])), false);
        }
        return n;
    }

    /** New tier, same inscribed spells/names/colours, more slots. */
    public static ItemStack evolve(ItemStack old, int tier) {
        ItemStack out = old.transmuteCopy(CompatItems.HUNTER_TOMES[tier].get(), 1);
        SpellCaster c = out.get(DataComponentRegistry.SPELL_CASTER);
        if (c != null) out.set(DataComponentRegistry.SPELL_CASTER, c.setMaxSlots(Math.max(c.getMaxSlots(), HunterTomes.SLOTS[tier])));
        return out;
    }

    static int heldTier(LivingEntity e) {
        int best = -1;
        for (ItemStack s : new ItemStack[] {e.getMainHandItem(), e.getOffhandItem()})
            if (s.getItem() instanceof HunterTomeItem t) best = Math.max(best, t.tomeTier);
        return best;
    }

    /** Public for tests: the damage multiplier / heal fraction a caster gets from a held tome. */
    public static float damageScale(Player p, LivingEntity target, int tier) {
        if (tier < 0) return 1.0F;
        float a = HunterTomes.ATTUNE[tier] / 100.0F;
        HunterClass c = HunterTomes.classOf(p);
        if (c == HunterClass.CASTER) return 1.0F + a;
        if (c == HunterClass.SUMMONER && target instanceof RiftMob) return 1.0F + 2 * a;
        return 1.0F;
    }

    static void damage(SpellDamageEvent.Pre e) {
        if (!(e.caster instanceof Player p) || !HunterTomes.canUse(p)) return;
        int tier = heldTier(p);
        if (tier < 0) return;
        e.damage *= damageScale(p, e.target instanceof LivingEntity l ? l : null, tier);
        if (HunterTomes.classOf(p) == HunterClass.NECROMANCER) p.heal(e.damage * HunterTomes.ATTUNE[tier] / 100.0F);
    }

    static void cast(SpellCastEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (heldTier(p) >= 0 && !HunterTomes.canUse(p)) { e.setCanceled(true); return; }
        int tier = heldTier(p);
        if (tier < 0 || HunterTomes.classOf(p) != HunterClass.LIFEBINDER || HunterTomes.ATTUNE[tier] == 0) return;
        if (p.getPersistentData().getLong("khaosnexus_tome_mend") > p.level().getGameTime()) return;
        p.getPersistentData().putLong("khaosnexus_tome_mend", p.level().getGameTime() + 40);
        float hp = HunterTomes.ATTUNE[tier] / 10.0F;
        for (Player ally : p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6))) ally.heal(hp);
    }
}

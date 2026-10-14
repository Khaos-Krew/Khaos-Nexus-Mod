package com.khaoskrew.khaosnexus.compat.items.tome;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.khaoskrew.khaosnexus.compat.items.CompatItems;
import com.khaoskrew.khaosnexus.compat.items.RiftRendEffect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Ars-only test operations (only called when Ars Nouveau is loaded). Returns an error string or null. */
public final class TomeTestOps {
    private TomeTestOps() {}

    public static String evolution() {
        ItemStack e = new ItemStack(CompatItems.HUNTER_TOMES[0].get());
        SpellCaster c = e.get(DataComponentRegistry.SPELL_CASTER);
        if (c == null || c.getMaxSlots() != HunterTomes.SLOTS[0]) return "E tome slots";
        Spell spell = new Spell(MethodTouch.INSTANCE, RiftRendEffect.INSTANCE);
        e.set(DataComponentRegistry.SPELL_CASTER, c.setSpell(spell, 1).setSpellName("Rend", 1));
        Spell stored = e.get(DataComponentRegistry.SPELL_CASTER).getSpell(1);
        if (stored.isEmpty()) return "inscribe";
        ItemStack s = TomeEvents.evolve(e, 5);
        if (!(s.getItem() instanceof HunterTomeItem t) || t.tomeTier != 5) return "evolved item";
        SpellCaster c2 = s.get(DataComponentRegistry.SPELL_CASTER);
        if (c2.getMaxSlots() != HunterTomes.SLOTS[5]) return "evolved slots " + c2.getMaxSlots();
        if (!c2.getSpell(1).equals(stored) || !"Rend".equals(c2.getSpellName(1))) return "spell kept";
        if (((HunterTomeItem) s.getItem()).getManaDiscount(s) != HunterTomes.DISCOUNT[5]) return "discount";
        if (t.getTier().value != 3 || ((HunterTomeItem) CompatItems.HUNTER_TOMES[0].get()).getTier().value != 1) return "glyph tier";
        return null;
    }

    public static void give(Player p, int tier) {
        p.getInventory().setItem(0, new ItemStack(CompatItems.HUNTER_TOMES[tier].get()));
        p.getInventory().selected = 0;
    }

    public static int heldTier(Player p) {
        return p.getMainHandItem().getItem() instanceof HunterTomeItem t ? t.tomeTier : -1;
    }

    /** Resolves Touch + Rift Rend on the target through Ars's resolver (fires SpellDamageEvent). */
    public static void cast(Player p, LivingEntity target) {
        Spell spell = new Spell(MethodTouch.INSTANCE, RiftRendEffect.INSTANCE);
        SpellResolver r = new SpellResolver(new SpellContext(p.level(), spell, p, new PlayerCaster(p), p.getMainHandItem()));
        r.onResolveEffect(p.level(), new EntityHitResult(target));
    }
}

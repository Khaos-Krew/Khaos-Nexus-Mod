package com.khaoskrew.khaosnexus.compat.items.legacy;

import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.mob.RiftMob;

import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Rift Hunter's Sigil for Relics 0.10.x (compiled against 0.10.7.8). Same two abilities as the modern sigil:
 * Rift Bane (more damage to rift mobs) and Gate Ward (less damage from rift mobs, relic level 5+).
 * Loaded only by reflection from RelicsSupport when the 0.10 API is detected.
 */
public class RiftSigilItemLegacy extends RelicItem {
    public RiftSigilItemLegacy() {
        super();
        NeoForge.EVENT_BUS.addListener(this::onHurt);
        NeoForge.EVENT_BUS.addListener(this::onKill);
    }

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("rift_bane")
                                .stat(StatData.builder("damage")
                                        .initialValue(0.08D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .thresholdValue(0.0D, 0.6D)
                                        .formatValue(v -> (int) MathUtils.round(v * 100.0D, 0))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("gate_ward")
                                .requiredLevel(5)
                                .stat(StatData.builder("resistance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.12D)
                                        .thresholdValue(0.0D, 0.35D)
                                        .formatValue(v -> (int) MathUtils.round(v * 100.0D, 0))
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().initialCost(100).maxLevel(10).step(100).build())
                .loot(LootData.builder().build())
                .build();
    }

    private static boolean on() {
        return SoftCompat.RELICS.active();
    }

    private void onHurt(LivingDamageEvent.Pre event) {
        if (!on() || event.getNewDamage() <= 0) {
            return;
        }
        try {
            LivingEntity target = event.getEntity();
            if (target instanceof RiftMob && event.getSource().getEntity() instanceof Player p) {
                for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                    if (this.canPlayerUseAbility(p, s, "rift_bane")) {
                        event.setNewDamage(event.getNewDamage() * (float) (1.0D + this.getStatValue(s, "rift_bane", "damage")));
                        this.spreadRelicExperience(p, s, 1);
                    }
                }
            } else if (target instanceof Player p && event.getSource().getEntity() instanceof RiftMob) {
                for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                    if (this.canPlayerUseAbility(p, s, "gate_ward")) {
                        event.setNewDamage(event.getNewDamage() * (float) (1.0D - Math.min(0.75D, this.getStatValue(s, "gate_ward", "resistance"))));
                        this.spreadRelicExperience(p, s, 1);
                    }
                }
            }
        } catch (LinkageError | RuntimeException e) {
            // Never let a Relics API quirk break combat.
        }
    }

    private void onKill(LivingDeathEvent event) {
        if (on() && event.getEntity() instanceof RiftMob && event.getSource().getEntity() instanceof Player p) {
            try {
                for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                    this.spreadRelicExperience(p, s, 5);
                }
            } catch (LinkageError | RuntimeException e) {
                // ignore
            }
        }
    }
}

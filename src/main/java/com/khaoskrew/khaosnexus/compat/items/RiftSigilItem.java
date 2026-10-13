package com.khaoskrew.khaosnexus.compat.items;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.mob.RiftMob;

import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchTemplate;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Rift Hunter's Sigil (Relics). Necklace relic that levels through Relics' own system:
 * - Rift Bane: more damage to rift mobs. Gains experience for every rift mob hit and kill.
 * - Gate Ward: less damage taken from rift mobs (unlocks with relic level). Gains experience from damage absorbed.
 */
public class RiftSigilItem extends WearableRelicItem {
    public RiftSigilItem() {
        NeoForge.EVENT_BUS.addListener(this::onHurt);
        NeoForge.EVENT_BUS.addListener(this::onKill);
    }

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("rift_bane")
                                .stat(AbilityStatTemplate.builder("damage")
                                        .initialValue(0.08D, 0.15D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.6D)
                                        .formatValue(v -> (int) MathUtils.round(v * 100.0D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("rift_hit").build())
                                        .source(ExperienceSourceTemplate.builder("rift_kill").build())
                                        .build())
                                .research(ResearchTemplate.builder()
                                        .star(0, 10, 4).star(1, 4, 14).star(2, 16, 14).star(3, 10, 24)
                                        .link(0, 1).link(0, 2).link(1, 3).link(2, 3)
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("gate_ward")
                                .requiredLevel(5)
                                .stat(AbilityStatTemplate.builder("resistance")
                                        .initialValue(0.05D, 0.1D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.35D)
                                        .formatValue(v -> (int) MathUtils.round(v * 100.0D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("rift_block").build())
                                        .build())
                                .research(ResearchTemplate.builder()
                                        .star(0, 6, 6).star(1, 14, 6).star(2, 10, 18)
                                        .link(0, 1).link(1, 2).link(2, 0)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder().initialCost(100.0D).step(100.0D).build())
                .loot(LootTemplate.builder().build())
                .build();
    }

    private static boolean on() {
        return SoftCompat.RELICS.active();
    }

    private void onHurt(LivingDamageEvent.Pre event) {
        if (!on() || event.getNewDamage() <= 0) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target instanceof RiftMob && event.getSource().getEntity() instanceof Player p) {
            for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                var ability = this.getRelicData(p, s).getAbilitiesData().getAbilityData("rift_bane");
                if (ability.canPlayerUse(p)) {
                    event.setNewDamage(event.getNewDamage() * (float) (1.0D + ability.getStatData("damage").getValue()));
                    this.getRelicData(p, s).getLevelingData().addExperience("rift_bane", "rift_hit", 1.0D);
                }
            }
        } else if (target instanceof Player p && event.getSource().getEntity() instanceof RiftMob) {
            for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                var ability = this.getRelicData(p, s).getAbilitiesData().getAbilityData("gate_ward");
                if (ability.canPlayerUse(p)) {
                    float before = event.getNewDamage();
                    event.setNewDamage(before * (float) (1.0D - Math.min(0.75D, ability.getStatData("resistance").getValue())));
                    this.getRelicData(p, s).getLevelingData().addExperience("gate_ward", "rift_block", Math.max(1.0D, before - event.getNewDamage()));
                }
            }
        }
    }

    private void onKill(LivingDeathEvent event) {
        if (on() && event.getEntity() instanceof RiftMob && event.getSource().getEntity() instanceof Player p) {
            for (ItemStack s : EntityUtils.findEquippedCurios(p, this)) {
                this.getRelicData(p, s).getLevelingData().addExperience("rift_bane", "rift_kill", 5.0D);
            }
        }
    }

    static {
        KhaosNexus.LOGGER.info("Relics present: Rift Hunter's Sigil enabled");
    }
}

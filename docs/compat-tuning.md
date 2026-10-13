# Companion-mod compat: behaviour and tuning

Every integration is soft: no companion mod is a compile or runtime dependency. Verified on NeoForge 1.21.1 with
Apotheosis 8.9.0 (Placebo 9.9.4, Apothic Attributes 2.11.0, Apothic Enchanting 1.6.4, Apothic Spawners 1.4.0),
Ars Nouveau 5.13.3, Iron's Spells 'n Spellbooks 3.16.3 (Iron's Lib 2.2.0), FTB Quests 2101.1.36 (FTB Library 2101.1.37,
FTB Teams 2101.1.11, Architectury 13.0.11), L_Ender's Cataclysm 3.33 and Mowzie's Mobs 1.8.2.

Run `/khaosnexus compat check` (op) in a modded world to re-verify everything below. It prints one line per check.

## Class bonuses (datapack)
`data/<ns>/khaosnexus_class_compat/<class>.json`:
```json
{"class": "caster", "replace": false, "bonuses": [
  {"attribute": "irons_spellbooks:spell_power", "amount": 0.10, "operation": "add_multiplied_base"},
  {"attribute": "ars_nouveau:ars_nouveau.perk.spell_damage", "amount": 1.0}
]}
```
`mod` defaults to the attribute namespace; an entry applies only when that mod is loaded and its toggle is on.
`compat.classBonusScale` multiplies every amount. All shipped ids were checked against the real registries
(Apothic Attributes, Iron's Spells, Ars Nouveau `ars_nouveau.perk.*`).

## Hunter Forge with Apotheosis
- Forging only edits `custom_data` (forge level, bound class) and its own two attribute modifiers
  (`khaosnexus:forge_<slot>`, `khaosnexus:forge_class_<slot>`). Apotheosis affixes, rarity, sockets and gems are
  separate components and are kept.
- Apotheosis reforging and socketing copy the stack, so forge levels survive both (checked with the reforge table code
  path and `/apoth reforge`).
- Re-forging replaces the forge modifier rather than stacking it.
- `forge.apotheosisScale` (default 0.75) scales forge bonuses while Apotheosis is installed, because affix and gem
  percentage damage multiplies the forge's flat bonus.

## Apotheosis on rift mobs — default behaviour
| Apotheosis system | Regular gate/dungeon mobs | Rift bosses (Warden, Guardian, dungeon/raid/Spire bosses) |
|---|---|---|
| Invaders (replace a natural spawn with a boss) | never: rift mobs spawn as EVENT | never |
| Elites (miniboss data entries) | never: entries list vanilla entity types only | never |
| World-tier augments (armor, toughness, pierce by the nearest player's tier) | yes, like any monster | **blocked** |
| Random affix gear augmentation (12% of monsters) | yes | **blocked** |

Keys:
- `rift.allowApotheosisInvaderBosses` (false): true lets Apotheosis treat bosses like ordinary monsters.
- Tag `khaosnexus:apotheosis_exempt` (entity_type): types always treated as bosses. Ships with `rift_warden` and
  `spire_guardian`; every mob spawned as a dungeon, raid or Spire boss is flagged as well.
- `rift.apotheosisGateTiers` (false): regular gate mobs get the Apotheosis world tier from `rift.apotheosisTierByRank`
  (default E/D=haven, C=frontier, B=ascent, A=summit, S=pinnacle) instead of the nearest player's tier.

## Boss mods in dungeons
- `compat.optionalBosses` RANK=entity list (Mowzie's Wroughtnaut, Umvuthi, Frostmaw; Ars Wilden Chimera; Cataclysm Ender
  Guardian, Ignis, Harbinger, Netherite Monstrosity), `compat.bossModChance` (0.35), `compat.bossMods` toggle.
- `compat.modBossHealthScale` (1.0) on top of the rank health multiplier. Vanilla caps max health at 1024, which S-rank
  modded bosses reach.
- `compat.modBossWakeUp` + `compat.modBossWake` (entity=nbtKey:value): bosses that spawn dormant for EVENT spawns are
  woken (Wroughtnaut `active`, Frostmaw `active`, Netherite Monstrosity `is_Awaken`).

## Loot
`compat.optionalLoot` and `compat.optionalLootChance` (0.2).

## Curios

With Curios installed, players get a `hunter_card` slot (data in `data/khaosnexus/curios/`, item tag `curios:hunter_card`). A card worn there counts as carried, so it binds and unlocks the HUD and UI. The **Open Hunter Card** key (default J) opens the card screen from that slot or from the inventory. Without Curios the data files are ignored and the code is reflection-only, so nothing loads.

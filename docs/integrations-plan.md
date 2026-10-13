# Khaos Nexus: integrations plan

Status as of 0.1.x. All integrations are soft: no hard dependency, code paths behind a `ModList` check, data behind
`neoforge:mod_loaded` conditions, and every bonus tunable in `khaosnexus-common.toml` or by datapack.

## Shipped (branch models/blockbench)

| Mod | Content | Toggle |
|---|---|---|
| Ars Nouveau | Rift Source Shard (imbuement: amethyst + rift essence + source gem, 500 source); glyph **Rift Rend** (double damage + Weakness vs rift mobs, 35 mana, Amplify/Dampen) | `arsNouveau` |
| Relics (+ Curios) | **Rift Hunter's Sigil** necklace: Rift Bane (bonus dmg vs rift mobs, XP from hits/kills), Gate Ward (dmg reduction, unlocks lvl 5, XP from absorbed hits) | `relics` |
| Artifacts (+ Curios) | **Rift Lantern** (belt, night vision near rift mobs), **Voidstep Anklet** (feet, half fall damage), **Gatebreaker Band** (ring, +20% vs rift mobs); stat bonuses via `curios:attribute_modifiers` in loot data | `artifacts` |
| Apotheosis | Gems **Riftheart**, **Voidglass**, **Gatekeeper** (weight 0: gate dungeons only), purity scaled by rank; existing world-tier, affix-boss exemption and forge scaling | `apotheosis` |

Loot: `data/<ns>/khaosnexus_compat_loot/<mod>.json` (`{"mod", "entries":[{"ranks":{"C":0.05}, "stack":{...}}]}`) is rolled
into `khaosnexus:dungeon/<rank>` chests by the `khaosnexus:compat_loot` global loot modifier. Entries for missing or
toggled-off mods are skipped at roll time; `compatLootMultiplier` scales every chance (0 disables).

Verified in the run-compat instance (screenshots `mod-art/bb13_compat_inventory.png`, `bb13_compat_tooltips.png`);
game test `compatItemsAndLootFollowInstalledMods` passes with and without the mods.

## Proposed next, in priority order

1. **JEI / EMI** (very high reach, low cost). Info pages for Hunter Card, gates, forge upgrades; recipe category for the
   Rift Forge and essence refining; hide debug items.
2. **Jade / WTHIT** (low cost). Gate rank, time left and party size on gate blocks; rift mob rank and boss phase.
3. **Curios (native slots)**. Hunter Card and class emblems as a Curios `charm` so they don't take inventory space;
   reuse `CuriosBridge`.
4. **JourneyMap / Xaero's Minimap** (medium). Waypoints for open gates and the daily bounty gate, colored by rank;
   optional gate markers on the world map.
5. **Create** (medium-high, big-pack staple). Crushing rift ores into extra essence, a mechanical "rift stabilizer"
   that keeps a gate open longer under stress; sequenced assembly for S-rank forge cores.
6. **Sophisticated Backpacks** (low). Rift-themed upgrade (auto-pick essence/cores), backpacks in A/S dungeon loot.
7. **Twilight Forest** (medium). TF-themed gate variant and dungeon palette; TF boss trophies count as rift cores;
   cross-loot of Apotheosis twilight gems already gated by condition.
8. **Big-pack staples** (ongoing): FTB Quests chapter (already partially supported), KubeJS events (`gateOpened`,
   `gateCleared`, `skillUnlocked`), Iron's Spellbooks class bonuses (done), Mowzie's / Cataclysm bosses in the boss
   pool (done, config `optionalBosses`), Patchouli guide book.

## Rules for new integrations

- Compile-only dependency via Modrinth maven; never shade.
- Register items only when the mod is present; data files carry `neoforge:mod_loaded`.
- Add a config toggle via `SoftCompat`, a loot file in `khaosnexus_compat_loot/`, and a game test that passes both in
  CI (no mods) and in `runCompatGameTestServer`.

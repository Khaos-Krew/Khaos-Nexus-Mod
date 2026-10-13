# Khaos Nexus: unique compat content proposal (design only, not built)

The goal is content that only makes sense because Khaos Nexus is installed alongside another mod, rather than generic stat items. Every idea is tied to the mod's own themes:
- **Gates** and the rift ranks E, D, C, B, A and S.
- **The 8 classes:** Tank, Assassin, Caster, Ranger, Lifebinder, Berserker, Summoner and Necromancer.
- **The Hunter Card,** the Spire, raids and bosses.

**Rules carried over from `integrations-plan.md`:**
- Soft dependency through a ModList check, and data files guarded by `neoforge:mod_loaded`.
- A config toggle per mod, with loot defined in `khaosnexus_compat_loot/`.
- A game test that passes both with and without the mod.

**Legend:**
- Work: **S** is under a day, **M** is 1–3 days, **L** is about a week.
- Art: **–** none, **tex** item or block textures, **model** a GeckoLib or Blockbench model or animation, **fx** particles or shaders.

APIs marked ✔ were checked against the jars in run-compat or used in shipped code. APIs marked ~ are from documentation or public source and still need checking against the jar before building.

---

## 1. Ars Nouveau (5.13)
✔ `AbstractEffect`, `GlyphRegistry.registerSpell`, `ars_nouveau:glyph` / `imbuement` recipes. ~ `AbstractRitual` + `RitualRegistry`, `ars_nouveau:enchanting_apparatus` recipe, `AbstractFamiliar`.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Ritual of Gate Binding** | Placed next to a closed gate's remains, the ritual tears open a fresh gate of the same rank and consumes source plus a Rift Core. It lets mage players farm a specific rank without waiting on world spawns, and the daily bounty never counts it. | M | tex (tablet) |
| 2 | **Glyph: Gatesense** (form augment / effect) | Shows the nearest open gate's rank and direction, and Amplify extends the range. Inside a gate it outlines the boss room. | S | tex |
| 3 | **Glyph: Rank Seal** | Applies a "Sealed" debuff to a rift mob that drops it one rank (lower HP and damage) for a few seconds. Bosses resist it unless they are hit by 3+ Sealed casts. | M | tex, fx |
| 4 | **Rift Source Jar / Rift Sourcelink** | A sourcelink that generates source while a gate is open within 32 blocks, more for higher ranks. It turns gates into a mana economy for Ars bases. | M | tex + model (block) |
| 5 | **Caster class synergy** | A Caster's Hunter Card level adds spell damage, and the Caster skill-tree capstone unlocks a 4th glyph tier slot effect: casting in a gate refunds 10% mana per rift kill. | S | – |
| 6 | **Familiar: Rift Wisp** | Bound with a Rift Core. It reveals gate mobs through walls and occasionally loots an extra rift essence. | L | model (GeckoLib) |

## 2. Iron's Spells 'n Spellbooks (3.16)
~ `AbstractSpell`, `SpellRegistry` DeferredRegister, `SchoolType` registry (`SchoolRegistry`), `AttributeRegistry` for school power and resist, `AbstractSpellCastingMob`.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Rift school** (`khaosnexus:rift`) | A new school with its own `rift_spell_power` and `rift_magic_resist` attributes, focus item = Rift Core. Rift mobs resist all schools except Rift. | M | tex (school icon, focus) |
| 2 | **Spell: Gate Step** | A short blink through a micro-rift. Inside a gate it can pass one wall, and the cooldown scales with gate rank. | M | fx |
| 3 | **Spell: Rank Crush** | An AoE whose damage is multiplied by the target's rift rank (E ×1 … S ×2), so it is weak in the overworld and strong in gates. | S | fx |
| 4 | **Spell: Call the Swarm** (Summoner/Necromancer) | Summons 2–4 `MinionMob`s from our own minion system, which count toward Summoner and Necromancer skill bonuses. | M | – (reuses minion models) |
| 5 | **Rift Spellbook** (rank-locked) | Its slot count grows with Hunter rank: E has 3 slots and S has 12. It only drops from S-rank gate bosses. | S | tex + model |
| 6 | **Rift Arcanist** (gate mob) | An `AbstractSpellCastingMob` variant that casts Iron's spells and spawns in B+ gates when Iron's is present. | L | model |

## 3. Apotheosis (8.9)
✔ Gem data (`apotheosis:gem`, gem_class, purity), world tiers, affix-boss exemption (shipped). ~ Affix data (`data/<ns>/affixes/`, types `apotheosis:attribute`, `apotheosis:potion`, custom `AffixType` codec), `apotheosis:bosses` data, `LootRarity`.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Rift affix set** (custom affix type `khaosnexus:rift_bane`) | Rolls bonus damage and damage reduction vs rift mobs, scaled by rarity. It only rolls on gear that drops from gates. | M | – |
| 2 | **"Gatebound" affix** | While inside a gate, gear gains +X% per gate rank. Outside gates it does nothing, which creates a gate-only loadout. | M | – |
| 3 | **Rift boss tier** | A `bosses` data entry so Apotheosis apex bosses can spawn as a "Rift Apex" inside A/S gates, with rift affixes and guaranteed Riftheart gems. | M | – |
| 4 | **Class gems** (8) | One gem per class; for example, Assassin crit on backstab or Lifebinder heal on socket. A gem gives a stronger bonus when it is socketed by a hunter of the matching class (checks the Hunter Card class). | M | tex ×8 |
| 5 | **Gate rank ↔ world tier** (extend shipped) | Clearing an S-rank gate counts toward the Apotheosis tier-unlock progression. | S | – |
| 6 | **Rift Reforging** | A Spire-floor reward item that rerolls one affix on Apotheosis gear at the Reforging Table, using Rift Cores instead of gem dust. | S | tex |

## 4. Relics (0.12)
✔ `WearableRelicItem`, `RelicTemplate` / `AbilitiesTemplate` / `LevelingTemplate`, `ExperienceSources`, `addExperience` (shipped sigil).

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Eight class relics** (one per class) | Each one levels only from that class's actions, for example: <ul><li>Tank Bulwark Crest: XP from damage blocked, ability "Taunt Pulse".</li><li>Assassin Shadow Fang: XP from backstabs, ability "Vanish".</li><li>Lifebinder Thread of Life: XP from healing allies.</li><li>Necromancer Phylactery: XP from minion kills, ability "Raise Elite".</li></ul> | L (8 relics; shared template makes each ~0.5 d) | tex ×8 |
| 2 | **Hunter Card relic link** | The relic's level caps at the player's Hunter rank, so an E-rank hunter can't max out an S-tier relic. | S | – |
| 3 | **Gate Compass relic** | Abilities: "Rank Sight" shows nearby gate ranks, and "Gatebreak" opens a gate early. Levels from gates cleared. | M | tex |
| 4 | **Spire Fragment relic** | Levels per new Spire floor reached. Abilities give resistance per floor and a "checkpoint" that restarts a failed floor once per day. | M | tex |
| 5 | **Raid Banner relic** | Levels from raid clears. A party-wide aura scales with the number of party members. | M | tex |

## 5. Artifacts (13.2)
✔ Curios-slotted items and event-driven effects (shipped). Artifacts has no public "artifact type" API, so our items act through our own events and appear in Artifacts-style loot.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Gate Mimic** | A rift-themed mimic that spawns in the treasure rooms of C+ gate dungeons. It drops one of our artifacts and a Rift Core. | M | model (GeckoLib) |
| 2 | **Boss-trophy artifacts** (one per boss) | Each boss drops a trophy, for example:<ul><li>Warden's Broken Chain (feet): immunity to the Warden's root.</li><li>Spire Guardian's Plume (head): spin-attack knockback immunity.</li></ul>Each counters that boss's mechanic, which rewards repeat runs. | M | tex per boss |
| 3 | **Rift Pocketwatch** | Shows the remaining gate timer on the HUD and adds 30 s to gate collapse once per gate. | S | tex |
| 4 | **Hunter's Lucky Coin** | Raises the chance of rank-up rewards from the daily gate bounty. | S | tex |

## 6. Curios (9.5)
✔ `curios:attribute_modifiers`, slot tags. ~ Custom slot types via `data/<ns>/curios/slots/*.json` + `curios/entities/*.json`.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **"Hunter Card" slot** | A dedicated Curios slot for the Hunter Card, so it no longer uses inventory space while all its features still work. | S | tex (slot icon) |
| 2 | **"Class Emblem" slot** | A new slot that accepts one class emblem (crafted at the Rift Forge). The emblem changes class passives, for example Berserker Emblem of Fury vs Emblem of Endurance. | M | tex ×8–16 |
| 3 | **Rank-gated slots** | Extra ring and charm slots unlock at Hunter rank B and S, which gives Curios-heavy packs a long-term progression goal. | S | – |

## 7. FTB Quests (2101.1)
~ Custom `TaskType` / `RewardType` registration (`TaskTypes.register`, `RewardTypes.register`), quest book data in `config/ftbquests/quests` shipped as a default pack.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Task types** | `khaosnexus:clear_gate` (rank, count, solo/party), `reach_hunter_rank`, `spire_floor`, `kill_rift_boss`, `unlock_skill`. | M | – |
| 2 | **Reward types** | `skill_points`, `hunter_xp` and `forge_core`, all applied to the Hunter Card. | S | – |
| 3 | **Bundled "Hunter's Path" chapter** | An optional default chapter that teaches the mod from E to S, with one branch per class. | M | – (uses item icons) |
| 4 | **Raid board quests** | Weekly repeatable quests synced with our daily and raid bounty system. | S | – |

## 8. L_Ender's Cataclysm (3.33)
✔ Its bosses are already in our boss pool via `optionalBosses` (shipped).

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Cataclysm S-rank gates** | A themed gate variant (Ignis forge or Leviathan abyss palette) whose boss room spawns that Cataclysm boss with rank scaling and our boss bar. | M | tex (palette blocks reuse vanilla) |
| 2 | **Rift-touched Cataclysm bosses** | A cataclysm boss spawned in a gate gains a rift phase at 30% HP: it summons rift mobs and gets a void shield, built on our `RiftBoss` phase hooks. | M | fx |
| 3 | **Cross-forge** | Cataclysm boss materials (e.g. Ignitium) can be used at the Rift Forge to push gear past +10. | S | – |
| 4 | **Hunter Card trophies** | Kills of Cataclysm bosses appear on the Card's boss page. | S | tex (icons) |

## 9. Mowzie's Mobs (1.8)
✔ Its bosses are already in our boss pool.

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Mowzie gate themes** | A Ferrous Wroughtnaut forge-hall gate (B) and a Frostmaw ice gate (A), each with its own dungeon palette. | M | – |
| 2 | **Barako "Sun Gate" raid** | A raid where a party fights Barako plus Barakoa waves, with rewards scaled by rank. | M | – |
| 3 | **Class combos** | Mowzie mask or ability items get class bonuses, for example the Wroughtnaut axe gets Berserker scaling and the Sol Visage gets Caster scaling. Each bonus is a data entry in `class_compat`. | S | – |

## 10. Create (0.5.x / 6.x)
~ `KineticBlockEntity`, `ProcessingRecipe` types (crushing, milling, haunting via `FanProcessingType`), `SequencedAssemblyRecipe`, ponder scenes (`PonderRegistrationHelper`).

| # | Addition | What it does | Work | Art |
|---|---|---|---|---|
| 1 | **Rift Stabilizer** (kinetic block) | Consumes stress (SU) next to an open gate to slow its collapse timer, so higher-rank gates need more SU. This gives Create bases a reason to build near gates. | L | model + tex, ponder |
| 2 | **Fan processing: "Rift Bathing"** | A new fan type (air passing through a Rift Core block) that turns ores into rift-infused variants with double essence yield. | M | fx, tex |
| 3 | **Crushing & milling recipes** | Rift ores and boss drops crush into essence and dust; pure data. | S | – |
| 4 | **Sequenced assembly: S-rank Forge Core** | A multi-step assembly that makes the endgame Forge Core, as an alternative to boss drops. | S | tex (intermediate item) |
| 5 | **Gate Siphon** (kinetic generator) | A rotating drill placed in a cleared gate's remnant that slowly outputs essence while spun. | M | model |

## 11. JEI / EMI
| # | Addition | Work | Art |
|---|---|---|---|
| 1 | A Rift Forge recipe category and an essence-refining category (`IRecipeCategory`). | M | – |
| 2 | Info pages for gates, ranks, the Hunter Card, the Spire and skills. | S | – |
| 3 | A "Gate loot" category showing per-rank drop tables, including compat loot with real chances × the loot multiplier. | M | – |

## 12. Jade / WTHIT
| # | Addition | Work | Art |
|---|---|---|---|
| 1 | Gate block tooltip: rank, time left, party, mobs remaining. | S | – |
| 2 | Rift mob tooltip: rank badge, boss phase, the class that counters it best. | S | – |
| 3 | Rift Forge tooltip: current upgrade progress. | S | – |

## 13. JourneyMap / Xaero's Minimap
| # | Addition | Work | Art |
|---|---|---|---|
| 1 | Auto-waypoints for open gates, colored by rank, removed when the gate collapses. | M | tex (rank icons) |
| 2 | Daily-bounty gate highlighted with a pulsing marker. | S | – |
| 3 | Spire waypoint plus the last checkpoint floor. | S | – |

## 14. Sophisticated Backpacks
~ `UpgradeItemBase` / `IUpgradeWrapper`.

| # | Addition | Work | Art |
|---|---|---|---|
| 1 | **Rift Collector upgrade:** auto-picks essence and cores and auto-compacts essence into blocks. | M | tex |
| 2 | **Hunter's Pack:** a backpack that is a Hunter Card storage tier. Its slot count unlocks with rank. | M | tex |
| 3 | Backpacks in A/S gate loot. | S | – |

## 15. Twilight Forest
| # | Addition | Work | Art |
|---|---|---|---|
| 1 | **Twilight gates** that spawn only in the Twilight dimension, with TF mob pools and palettes. | M | – |
| 2 | TF boss trophies count as Rift Cores at the Rift Forge. | S | – |
| 3 | A "Twilight" Spire branch: Spire floors 30+ alternate with TF-themed floors. | L | – |

## 16. Big-pack staples
| Mod | Addition | Work | Art |
|---|---|---|---|
| KubeJS | Server events `gateOpened`, `gateCleared`, `rankUp`, `skillUnlocked` and `bossPhase`, plus bindings to edit gate pools. | M | – |
| Patchouli | The "Hunter's Handbook" guide covering gates, classes, skills and the Spire, generated from our skill-tree data. | M | tex (cover) |
| Waystones | Gate waystones: a temporary waystone at the gate entrance that disappears when the gate closes. | S | tex |
| Accessories (Curios alternative) | Mirror the Curios slots (Hunter Card, Class Emblem) through Accessories' compat layer. | S | – |

---

## Suggested build order
1. **Low cost, high visibility:** the Curios Hunter Card slot, Jade tooltips, FTB Quests task and reward types, JEI info and the Rift Forge category.
2. **The signature pieces:**
   - Iron's Rift school.
   - Apotheosis rift affixes and the Rift Apex boss tier.
   - The 8 class relics.
   - The Ars Ritual of Gate Binding.
3. **Larger builds:**
   - The Create Rift Stabilizer and fan type.
   - Cataclysm rift-touched bosses.
   - The Gate Mimic.
   - Twilight gates.

**Art budget:**
- About 40 item textures. We already generate these with `tools/gen_compat_textures.py`, then hand-touch them.
- 4–5 GeckoLib models: the Gate Mimic, Rift Wisp, Rift Arcanist, Rift Stabilizer and Gate Siphon.
- One school icon, plus rank waypoint icons.

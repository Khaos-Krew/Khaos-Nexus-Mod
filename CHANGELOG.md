# Changelog

## 1.0.0 (2026-10-14)

The first full release. Everything below is new since 0.1.2-beta and can be tuned in the server config (`[endgame]` and `[spire]`) and through datapack folders.

### The Nexus Spire, rebuilt
- The Spire is now a real place in its own dimension. Each **floor is 10 levels**, and each level is a themed arena with walls, pillars and lighting, plus its own guardian. The style changes floor by floor across 5 bundled themes (`khaosnexus_spire_themes`).
- Beating a level's guardian lights its **rift-style gate**, animated and tinted by rank, which leads to the next level. After level 10 it leads to the next floor.
- Every floor starts in a **safe town** with:
  - an Alchemist (potions) and an Armorer (gear). Their stock and enchantments scale with your floor and rank (`khaosnexus_spire_vendors`).
  - a repair **Smith** (Rift Cores or Nexus Tokens).
  - a **checkpoint**.
- **Endless scaling:** past floor 3, guardians gain +6% health and damage per floor (capped). Each floor's final guardian rolls an affix (Armored, Swift, Vampiric, Berserk, Titanic) for bonus Rift Cores.
- Progress is tracked as floor and level. Leaderboards show F#-L#. Old saves convert automatically: 10 old floors become 1 new floor.
- Flight and block breaking stay disabled in the Spire.

### Post-S endgame
- **Weekly modifiers** for A and S gates, with bonus XP and Rift Cores. See `/khaosnexus weekly`.
- **Red Gates:** rare, locked until cleared, with a stronger boss, double XP, bonus cores and a title.
- **Dungeon breaks:** gates left uncleared too long spill their mobs into the world.
- **Leaderboards:** `/khaosnexus top`.
- **Auras:** cosmetic particle auras unlocked by achievements. See `/khaosnexus aura`. They also show on the Hunter Card.
- **Class mastery:** XP earned at S rank fills a mastery track per class, levels 1–50, spent on sidegrade perks. See `/khaosnexus mastery`.
- **SS and National ranks:** earned by achievements. They grant a title, an aura and bonus skill points, and SS hunters can open **SS gates**. See `/khaosnexus prestige`.
- Bosses tuned past vanilla's 1024 max health now take proportionally less damage, so their full health still counts.

### Hunter Card
- The rank line shows SS or National prestige.
- The Spire line shows floor, level and floors cleared.
- The equipped aura is shown.

### Coming in 1.1 (included, off by default)
Boss rush, monthly challenge gates, Second Awakening and 8-player group raids are in the jar but switched off. Server owners can try them with the `bossRush`, `challengeGates`, `secondAwakening` and `groupRaids` toggles.

### Defaults changed
- `[spire] checkpointInterval` is now 1 (every floor town is a checkpoint). Existing config files keep their saved value.
- `[raids] maxParty` now allows up to 8 (the default stays 5).

## 0.1.2-beta (2026-10-13)

### Balance
- Hunter XP per level: 10 → 13 (`skillXpPerLevel`). The level-25 cap now lands at 312 XP, just before rank S (320), instead of halfway through rank A, so the last skill points arrive as you reach S rank. Existing servers keep the value already saved in their config.
- A-rank gate reward: 1 → 2 netherite scrap, so it clearly beats B rank's 2 diamonds.

### Cataclysm bosses
- All four were re-tested solo at the 0.1.1 numbers. No changes were needed (results are in `docs/compat-content-proposal.md`).
- The void rescue under boss arenas now only moves hunters more than 24 blocks below the boss, so lower rooms of the same dungeon are left alone.

## 0.1.1-beta (2026-10-13)

### Companion-mod items (each loads only when its mod is installed)
- **Ars Nouveau:**
  - Rift Source Shard, made by imbuement.
  - New glyph, Rift Rend: double damage and Weakness against rift creatures. Works with Amplify and Dampen.
- **Relics (needs Curios):** Rift Hunter's Sigil necklace. It levels up its Rift Bane and Gate Ward abilities by fighting rift creatures.
- **Artifacts (needs Curios):**
  - Rift Lantern (belt): night vision while rift creatures are near.
  - Voidstep Anklet (feet): half fall damage.
  - Gatebreaker Band (ring): +20% damage to rift creatures.
- **Apotheosis:** three gems, Riftheart, Voidglass and Gatekeeper, that only drop in gate dungeons.
- **Loot:** these items drop in dungeon chests based on gate rank. Drop rates are set in datapack files (`khaosnexus_compat_loot/`) and scaled by `compatLootMultiplier`. Each mod can be switched off in the config.

### Cataclysm bosses in gates
- Cataclysm bosses now only appear in S-rank gates. Turn on `cataclysmAnyRank` to allow them at lower ranks. The Ender Guardian moved from A to S.
- **Weaker in gates:**
  - Boss-mod bosses are weaker inside gates. `modBossHealthScale` now defaults to 0.5, and the new `modBossDamageScale` (0.6) applies once to every hit, including abilities and projectiles.
  - `modBossScale` sets health and damage per boss. Defaults: Ender Guardian 0.5/0.5, Ignis 0.2/0.4, Harbinger 0.5/0.65, Netherite Monstrosity 0.35/0.4.
  - New `modBossHealScale` (0.3) reduces how much these bosses heal from life steal and natural regeneration inside gates.
- **Fair fights:**
  - A 5-second grace period when the boss spawns (`modBossGraceSeconds`).
  - The dungeon doesn't collapse while the boss is in a fight.
  - +25% boss health per extra hunter nearby (`modBossPartyHealth`).
  - Apotheosis doesn't upgrade these bosses.
  - Bosses can't grief the arena, and hunters knocked into the void under the arena are put back.
- New `cataclysmRiftPhase` setting, off by default. When on, the boss gets a void shield and summons rift mobs once at 30% health.

## 0.1.0-beta (2026-10-13)

Everything since 0.0.2-alpha.

### Skills
- Hunter levels: hunter XP now raises a hunter level, and each level grants skill points. Points come from XP, so existing saves get them automatically.
- Every class has a skill tree with 7 to 9 nodes. Both actives can be upgraded through ranks (power, cooldown, radius). The tree also has passive stat nodes and a capstone that unlocks at rank A.
- New Skills tab on the Hunter Card: see the tree, spend points, and read the tooltips for rank, cost and why a node is locked.
- Respec costs 1 Rift Core by default (configurable, free in creative).
- New cooldown HUD for both class abilities, showing their keybinds and free skill points. It follows the Hunter Card UI scale and opacity.
- Skill trees are data-driven (`data/<ns>/khaosnexus_skill_tree/<class>.json`) and synced from the server. New server config options: `skillXpPerLevel`, `skillPointsPerLevel`, `skillMaxLevel`, `skillRespecItem`, `skillRespecCount`.

### Skill balance
- Every rank of active power, cooldown and radius now gives +12% power, -8% cooldown or +15% radius.
- Speed passives are now multiplicative (+2-3% per rank). Before, they added a flat amount to base speed, which was far too strong.
- Unused luck nodes are replaced with useful stats.
- Capstones (rank A, 3 points) are class-specific:
  - Caster, Lifebinder and Necromancer: +25% power on both abilities.
  - Ranger and Summoner: -20% cooldown on both abilities.
  - Tank: +4 armor toughness.
  - Assassin: +15% attack speed.
  - Berserker: +15% attack damage.
- XP curve: 10 XP per hunter level, level cap 25 (24 points). Finishing one tree (23 points) lands between rank A and rank S.
- The Hunter Card's Info tab now shows the hunter level.

### Hunter Card, gear and progression
- Hunter Card with a Guild Clerk villager and a Guild Desk. Hunter HUD and keys unlock with the card, and the card has profile, nearby gates, settings and forge tabs. Tabs scale with the UI. New open-card key (J).
- Optional Curios `hunter_card` slot. A card in this slot counts as held.
- Hunter Forge: data-driven gear and class-weapon upgrades by material tier, using rift essence and rift cores. Upgrades are rank-gated.
- Material tier scaling with data-driven tags and a loot modifier.

### Gates and dungeons
- Gates crack open and collapse with an animation.
- Dungeon theme framework: themes are data-driven, picked with biome weighting and saved on the gate, and the card shows each gate's theme. Adds the Frostbound Crypt and Overgrown Ruin themes, plus an optional Create subway entry.
- Finished, abandoned and orphaned dungeon instances are reclaimed. If the dungeon level is missing, the gate falls back to waves.

### Mobs and bosses
- Model pass on every rift mob, and a texture pass at 32px per block.
- Attacks wind up first, and damage lands on the strike frame.
- Rift Warden:
  - Draws its sword cleanly from the back sheath: no hurt flash, and the roar waits until it is in its ready stance.
  - Slam attack.
  - When enraged, a lunge that leans forward toward the target.
- Spire Guardian:
  - Attacks with a halberd overhead chop and a wide sweep.
  - Spins with the halberd held out.
  - Arcane barrier and minion call.
- Bosses enrage at 50% health.

### Compatibility (all optional)
- Class bonuses from Apothic Attributes, Iron's Spells and Ars Nouveau.
- Apotheosis rift rules.
- FTB stage provider.
- Boss and loot pools.
- `/khaosnexus compat check`.

### Tech
- Game tests run headless in the build and in CI (19 tests), covering forge, materials, card, dungeons, themes, compat and skills.

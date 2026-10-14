# Post-S-rank endgame plan (design only)

The goal is to keep S-rank hunters engaged without endless stat inflation. This means variety, mastery and social goals, plus a progression that stays slow but visible.

**Scoring:**
- **Fun** is rated 1–5.
- **Effort** is S (under a day), M (1–3 days) or L (about a week or more).
- **Priority** is fun relative to effort. Higher rows are better value.

| # | Idea | What it is | Fun | Effort | Priority |
|---|---|---|---|---|---|
| 1 | **Weekly rotating modifiers** | Every week S gates roll 2 modifiers from a datapack list, for example "Void-touched: mobs blink", "Glass cannon: everyone deals and takes ×1.5" or "No healing items". Clears under the modifiers give bonus Rift Cores. | 5 | S–M | ★★★★★ |
| 2 | **Red gates / rift overflows** | A rare gate (about 1 in 40 S gates) that locks its entrance until cleared, with no leaving and a stronger boss. Ignoring an open gate for 30 minutes causes a "break": the mobs spill into the overworld around the gate, and the server is warned. | 5 | M | ★★★★★ |
| 3 | **Infinite Spire floors** | After the authored floors, floors are procedural with +6% health and damage per floor. Every 10th floor is a boss floor with an affix. Personal-best floor is shown on the Hunter Card. | 5 | M | ★★★★☆ |
| 4 | **Leaderboards** | Server boards for fastest clear per rank, highest Spire floor this season and weekly-modifier clears. Shown on a Hunter Card tab and in `/khaosnexus top`. Seasons reset with `spire newseason`. | 4 | S–M | ★★★★☆ |
| 5 | **Mastery tracks** (per class) | XP past S rank goes into class mastery levels 1–50. Each level gives a small sidegrade choice, for example "Shadow Step leaves a decoy" or "+1 summon, −10% summon HP". This is horizontal growth rather than raw stats. | 4 | M | ★★★★☆ |
| 6 | **Boss rush** | A chain of 5 gate bosses (ours plus installed boss-mod bosses) with one heal between fights. Timed, with leaderboard support. | 4 | M | ★★★☆☆ |
| 7 | **Challenge gates** (handcrafted) | A monthly authored dungeon with fixed rules: solo only, one life, class-locked. It rewards a unique title. | 4 | M | ★★★☆☆ |
| 8 | **Prestige / Paragon rank** (SS, then Paragon) | Tied to achievements rather than XP grind: clear a crimson rift, reach Spire floor 50 and win a boss rush. It grants a title, an aura, a +1 skill point cap and access to SS gates (S tuning ×1.3 with an extra modifier). | 4 | M | ★★★☆☆ |
| 9 | **Ascension** | A one-time quest chain at Paragon level that unlocks a second active ability per class, chosen from 2 options. Big hype moment with an animation and a screen effect. | 5 | L | ★★★☆☆ |
| 10 | **Group raids (8 players)** | Multi-phase raid bosses with role mechanics (tank aggro, healer cleanse), using the existing raid and party system. Personal loot already exists. | 5 | L | ★★★☆☆ |
| 11 | **Cosmetic titles and auras** | Titles exist already. Add particle auras by achievement (red-gate survivor, Spire 100, boss-rush champion), shown in third person and on the Hunter Card. | 3 | S | ★★★☆☆ |
| 12 | **Guilds** | Persistent parties with a guild level, a guild bank and guild-only weekly gates. | 3 | L | ★★☆☆☆ |

**Recommended order:** 1 → 2 → 4 → 3 → 11 → 5 → 8 → 6 → 7 → 9 → 10 → 12.

**Anti-boredom rules:**
- Rewards past S are mostly sidegrades, cosmetics and rank prestige, never a big damage multiplier.
- Everything is datapack- and config-tunable.
- Weekly and seasonal resets keep goals fresh, and a fallen leaderboard spot is always recoverable.

## Implementation status (built)

Everything below is in the `[endgame]` section of the server config, and each feature's content is a datapack folder you can override or extend.

| # | Feature | Commands | Datapack folder | Key config |
|---|---|---|---|---|
| 1 | Weekly modifiers | `/khaosnexus weekly` | `khaosnexus_gate_modifiers` | `weeklyModifiers`, `weeklyModifierCount`, `weeklyRanks`, `weeklySeed`, `weeklyForce` |
| 2 | Red gates and rift overflows | — | — | `redGateChance`, `redGateRanks`, `redBoss*`, `redLockExit`, `dungeonBreaks`, `dungeonBreakMinutes` |
| 4 | Leaderboards | `/khaosnexus top <E..S\|spire\|weekly\|rush>` | — | `leaderboards`, `leaderboardSize` |
| 3 | Infinite Spire | `/khaosnexus spire` | `khaosnexus_spire_affixes` | `spireInfinite`, `spireScaleFrom`, `spireScalePerFloor`, `spireScaleCap`, `spireBossInterval` |
| 11 | Auras | `/khaosnexus aura [equip <id>\|clear]` | `khaosnexus_auras` | `auras`, `auraInterval` |
| 5 | Mastery tracks | `/khaosnexus mastery [pick <id>\|reset]` | `khaosnexus_mastery` | `mastery`, `masteryXpPerLevel`, `masteryMaxLevel` |
| 8 | SS / Paragon rank, SS gates | `/khaosnexus prestige` | — | `prestigeRanks`, `ss*`, `national*`, `prestigeSkillPoints`, `ssGate*` |
| 6 | Boss rush | `/khaosnexus bossrush start\|leave` | `khaosnexus_boss_rush` | `bossRush`, `bossRushRoster`, `bossRushMinRank`, `bossRushRewardCores` |
| 7 | Challenge gates | `/khaosnexus challenge [start\|leave]` | `khaosnexus_challenges` | `challengeGates`, `challengeForce`, `challengeAttemptsPerMonth` |
| 9 | Ascension | `/khaosnexus awaken [choose <id>\|use]`, sneak + secondary ability key | `khaosnexus_awakenings` | `secondAwakening`, `awakeningSteps` |
| 10 | Group raids (8) | `/khaosnexus raid group <boss>` | `khaosnexus_raid_bosses` | `groupRaids`, `groupRaidMinParty`, `groupRaidMaxParty`, `raidCleanseRadius` |
| 12 | Guilds | on hold (may overlap with the server's own setup) | | |

Notes:
- Bosses that need more than vanilla's 1024 max health keep a capped health bar and take proportionally less damage (`BigHealth`).
- Boss rush and challenge runs happen on an arena platform in the Spire dimension. They are not saved across a server restart: players in a run when the server stops log back in where they were.
- Mastery perks are attribute sidegrades (a gain paired with a cost), grouped so you pick one per group.

# Post-S-rank endgame plan (design only)

The goal is to keep S-rank hunters engaged without endless stat inflation. This means variety, mastery and social goals, plus a progression that stays slow but visible.

**Scoring:**
- **Fun** is rated 1–5.
- **Effort** is S (under a day), M (1–3 days) or L (about a week or more).
- **Priority** is fun relative to effort. Higher rows are better value.

| # | Idea | What it is | Fun | Effort | Priority |
|---|---|---|---|---|---|
| 1 | **Weekly rotating modifiers** | Every week S gates roll 2 modifiers from a datapack list, for example "Void-touched: mobs blink", "Glass cannon: everyone deals and takes ×1.5" or "No healing items". Clears under the modifiers give bonus Rift Cores. | 5 | S–M | ★★★★★ |
| 2 | **Red gates / dungeon breaks** | A rare gate (about 1 in 40 S gates) that locks its entrance until cleared, with no leaving and a stronger boss. Ignoring an open gate for 30 minutes causes a "break": the mobs spill into the overworld around the gate, and the server is warned. | 5 | M | ★★★★★ |
| 3 | **Infinite Spire floors** | After the authored floors, floors are procedural with +6% health and damage per floor. Every 10th floor is a boss floor with an affix. Personal-best floor is shown on the Hunter Card. | 5 | M | ★★★★☆ |
| 4 | **Leaderboards** | Server boards for fastest clear per rank, highest Spire floor this season and weekly-modifier clears. Shown on a Hunter Card tab and in `/khaosnexus top`. Seasons reset with `spire newseason`. | 4 | S–M | ★★★★☆ |
| 5 | **Mastery tracks** (per class) | XP past S rank goes into class mastery levels 1–50. Each level gives a small sidegrade choice, for example "Shadow Step leaves a decoy" or "+1 summon, −10% summon HP". This is horizontal growth rather than raw stats. | 4 | M | ★★★★☆ |
| 6 | **Boss rush** | A chain of 5 gate bosses (ours plus installed boss-mod bosses) with one heal between fights. Timed, with leaderboard support. | 4 | M | ★★★☆☆ |
| 7 | **Challenge gates** (handcrafted) | A monthly authored dungeon with fixed rules: solo only, one life, class-locked. It rewards a unique title. | 4 | M | ★★★☆☆ |
| 8 | **Prestige / National-level rank** (SS, then National) | Tied to achievements rather than XP grind: clear a red gate, reach Spire floor 50 and win a boss rush. It grants a title, an aura, a +1 skill point cap and access to SS gates (S tuning ×1.3 with an extra modifier). | 4 | M | ★★★☆☆ |
| 9 | **Second Awakening** | A one-time quest chain at National level that unlocks a second active ability per class, chosen from 2 options. Big hype moment with an animation and a screen effect. | 5 | L | ★★★☆☆ |
| 10 | **Group raids (8 players)** | Multi-phase raid bosses with role mechanics (tank aggro, healer cleanse), using the existing raid and party system. Personal loot already exists. | 5 | L | ★★★☆☆ |
| 11 | **Cosmetic titles and auras** | Titles exist already. Add particle auras by achievement (red-gate survivor, Spire 100, boss-rush champion), shown in third person and on the Hunter Card. | 3 | S | ★★★☆☆ |
| 12 | **Guilds** | Persistent parties with a guild level, a guild bank and guild-only weekly gates. | 3 | L | ★★☆☆☆ |

**Recommended order:** 1 → 2 → 4 → 3 → 11 → 5 → 8 → 6 → 7 → 9 → 10 → 12.

**Anti-boredom rules:**
- Rewards past S are mostly sidegrades, cosmetics and rank prestige, never a big damage multiplier.
- Everything is datapack- and config-tunable.
- Weekly and seasonal resets keep goals fresh, and a fallen leaderboard spot is always recoverable.

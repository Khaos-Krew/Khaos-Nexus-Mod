package com.khaoskrew.khaosnexus;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common spec stays empty for now. Gate spawn chances, wave size, stat
 * scaling, and clear rewards live in the server spec so each world can tune them.
 */
public final class Config {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec SPEC = COMMON_BUILDER.build();

    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    static {
        SERVER_BUILDER.comment(
                "Nexus Gate waves. A placed gate rolls a rank from E through S, and right-clicking it summons a wave of vanilla mobs.",
                "Clearing the wave drops that rank's reward and closes the gate.",
                "Mob and item ids are registry names such as minecraft:zombie. The defaults mix the mod's own Rift mobs with vanilla mobs.")
                .push("gates");
    }

    public static final ModConfigSpec.IntValue ARENA_RADIUS = SERVER_BUILDER
            .comment("How many blocks from the gate the wave is placed.")
            .defineInRange("arenaRadius", 4, 2, 24);

    public static final ModConfigSpec.IntValue LEASH_RADIUS = SERVER_BUILDER
            .comment("Mobs that wander farther than this are pulled back to their spawn point. Keep this larger than arenaRadius.")
            .defineInRange("leashRadius", 10, 3, 48);

    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNING = SERVER_BUILDER
            .comment("When true, Nexus Gates appear near players on their own. This is on by default. Set it to false to leave gates to the creative tab and commands.")
            .define("naturalSpawning", true);

    public static final ModConfigSpec.IntValue NATURAL_SPAWNS_PER_DAY = SERVER_BUILDER
            .comment("How many gates may appear on their own during one Minecraft day, across the whole server. 0 disables natural spawning.")
            .defineInRange("naturalSpawnsPerDay", 3, 0, 100);

    public static final ModConfigSpec.IntValue NATURAL_SPAWN_MIN_DISTANCE = SERVER_BUILDER
            .comment("Closest a natural gate may appear to a player, in blocks.")
            .defineInRange("naturalSpawnMinDistance", 24, 8, 128);

    public static final ModConfigSpec.IntValue NATURAL_SPAWN_MAX_DISTANCE = SERVER_BUILDER
            .comment("Farthest a natural gate may appear from a player, in blocks.")
            .defineInRange("naturalSpawnMaxDistance", 64, 16, 256);

    public static final ModConfigSpec.IntValue NATURAL_SPAWN_INTERVAL = SERVER_BUILDER
            .comment("How often the server tries to place the next natural gate, in ticks. 20 ticks is one second.")
            .defineInRange("naturalSpawnInterval", 200, 20, 72000);

    public static final RankValues RANK_E = RankValues.define(SERVER_BUILDER, "E",
            List.of("khaosnexus:rift_ghoul", "minecraft:zombie"), 3, 45, 1.0D, 1.0D, "minecraft:emerald", 2, 5, 8);
    public static final RankValues RANK_D = RankValues.define(SERVER_BUILDER, "D",
            List.of("khaosnexus:rift_ghoul", "khaosnexus:rift_stalker", "minecraft:skeleton"), 5, 25, 1.35D, 1.15D, "minecraft:emerald", 4, 12, 14);
    public static final RankValues RANK_C = RankValues.define(SERVER_BUILDER, "C",
            List.of("khaosnexus:rift_stalker", "khaosnexus:void_caster", "minecraft:spider"), 6, 15, 1.75D, 1.35D, "minecraft:diamond", 1, 24, 22);
    public static final RankValues RANK_B = RankValues.define(SERVER_BUILDER, "B",
            List.of("khaosnexus:void_caster", "khaosnexus:rift_brute", "khaosnexus:rift_stalker", "minecraft:pillager"), 8, 8, 2.25D, 1.55D, "minecraft:diamond", 2, 40, 34);
    public static final RankValues RANK_A = RankValues.define(SERVER_BUILDER, "A",
            List.of("khaosnexus:rift_brute", "khaosnexus:void_caster", "minecraft:vindicator", "minecraft:witch"), 8, 5, 3.0D, 1.8D, "minecraft:netherite_scrap", 1, 65, 50);
    public static final RankValues RANK_S = RankValues.define(SERVER_BUILDER, "S",
            List.of("khaosnexus:rift_brute", "khaosnexus:void_caster", "minecraft:evoker", "minecraft:ravager"), 6, 2, 4.0D, 2.0D, "minecraft:netherite_ingot", 1, 100, 80);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Hunter rank is separate from vanilla XP. Clearing a gate grants the rank's hunterXp.")
                .push("hunter");
    }

    public static final ModConfigSpec.IntValue XP_D = SERVER_BUILDER
            .comment("Hunter XP required to reach rank D. Rank E is always 0.")
            .defineInRange("xpRankD", 20, 1, 1000000);
    public static final ModConfigSpec.IntValue XP_C = SERVER_BUILDER
            .comment("Hunter XP required to reach rank C.")
            .defineInRange("xpRankC", 60, 1, 1000000);
    public static final ModConfigSpec.IntValue XP_B = SERVER_BUILDER
            .comment("Hunter XP required to reach rank B.")
            .defineInRange("xpRankB", 120, 1, 1000000);
    public static final ModConfigSpec.IntValue XP_A = SERVER_BUILDER
            .comment("Hunter XP required to reach rank A.")
            .defineInRange("xpRankA", 200, 1, 1000000);
    public static final ModConfigSpec.IntValue XP_S = SERVER_BUILDER
            .comment("Hunter XP required to reach rank S.")
            .defineInRange("xpRankS", 320, 1, 1000000);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("One class is equipped at a time. Swapping is free after a short cooldown.")
                .push("classes");
    }

    public static final ModConfigSpec.IntValue CLASS_SWAP_COOLDOWN = SERVER_BUILDER
            .comment("Ticks to wait before equipping a different class. 20 ticks is one second.")
            .defineInRange("swapCooldown", 200, 0, 72000);
    public static final ModConfigSpec.IntValue ABILITY_COOLDOWN = SERVER_BUILDER
            .comment("Ticks before the class primary ability (V) can be used again.")
            .defineInRange("primaryCooldown", 100, 0, 72000);
    public static final ModConfigSpec.IntValue ABILITY_ALT_COOLDOWN = SERVER_BUILDER
            .comment("Ticks before the class secondary ability (B) can be used again.")
            .defineInRange("secondaryCooldown", 160, 0, 72000);
    public static final ModConfigSpec.IntValue MINION_CAP = SERVER_BUILDER
            .comment("How many Summoner or Necromancer minions one player can have out.")
            .defineInRange("minionCap", 3, 1, 8);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("A player can solo a gate at their hunter rank or lower. A higher gate needs a party of this size.")
                .push("access");
    }

    public static final ModConfigSpec.BooleanValue GATE_ACCESS = SERVER_BUILDER
            .comment("When true, survival players cannot open a gate above their hunter rank unless their party is large enough.")
            .define("enabled", true);
    public static final ModConfigSpec.IntValue GATE_PARTY_SIZE = SERVER_BUILDER
            .comment("Party size that may open a gate above the player's hunter rank. 1 allows anyone.")
            .defineInRange("partySize", 2, 1, 5);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Daily bounties reset each Minecraft day and pay Nexus Tokens. Tokens are not crafted.")
                .push("bounties");
    }

    public static final ModConfigSpec.IntValue BOUNTY_GATES = SERVER_BUILDER
            .comment("Gate clears required for the daily gate bounty.")
            .defineInRange("gateClears", 2, 1, 100);
    public static final ModConfigSpec.IntValue BOUNTY_GATE_TOKENS = SERVER_BUILDER
            .defineInRange("gateTokens", 4, 0, 64);
    public static final ModConfigSpec.IntValue BOUNTY_RAIDS = SERVER_BUILDER
            .comment("Raid clears required for the daily raid bounty.")
            .defineInRange("raidClears", 1, 1, 20);
    public static final ModConfigSpec.IntValue BOUNTY_RAID_TOKENS = SERVER_BUILDER
            .defineInRange("raidTokens", 6, 0, 64);
    public static final ModConfigSpec.IntValue BOUNTY_SPIRE_FLOOR = SERVER_BUILDER
            .comment("Spire floor that completes the daily Spire bounty.")
            .defineInRange("spireFloor", 2, 1, 10000);
    public static final ModConfigSpec.IntValue BOUNTY_SPIRE_TOKENS = SERVER_BUILDER
            .defineInRange("spireTokens", 6, 0, 64);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Raids use the built-in party. Boss health grows with party size, and each fighter gets a personal chest.")
                .push("raids");
    }

    public static final ModConfigSpec.IntValue RAID_MIN_PARTY = SERVER_BUILDER
            .comment("Smallest party that can start a raid. 1 lets a solo player test it.")
            .defineInRange("minParty", 1, 1, 5);
    public static final ModConfigSpec.IntValue RAID_MAX_PARTY = SERVER_BUILDER
            .comment("Largest raid party.")
            .defineInRange("maxParty", 5, 1, 5);
    public static final ModConfigSpec.DoubleValue RAID_HEALTH_SCALE = SERVER_BUILDER
            .comment("Boss health multiplier added for each party member after the first.")
            .defineInRange("healthScale", 0.5D, 0.0D, 10.0D);
    public static final ModConfigSpec.ConfigValue<String> RAID_REWARD = SERVER_BUILDER
            .comment("Item id placed in each player's personal loot chest.")
            .define("rewardItem", "minecraft:diamond", Config::isRegistryId);
    public static final ModConfigSpec.IntValue RAID_REWARD_COUNT = SERVER_BUILDER
            .defineInRange("rewardCount", 1, 1, 64);
    public static final ModConfigSpec.IntValue RAID_LOCKOUT_DAYS = SERVER_BUILDER
            .comment("Minecraft days before the same player can take raid loot again. 0 pays every clear.")
            .defineInRange("lockoutDays", 7, 0, 365);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("The Nexus Spire is an endless tower. A checkpoint is saved every few floors, and a new season starts only when an admin asks.")
                .push("spire");
    }

    public static final ModConfigSpec.IntValue SPIRE_CHECKPOINT = SERVER_BUILDER
            .comment("Floors between checkpoints.")
            .defineInRange("checkpointInterval", 10, 1, 100);
    public static final ModConfigSpec.DoubleValue SPIRE_HEALTH_PER_FLOOR = SERVER_BUILDER
            .comment("Extra boss health added for each floor.")
            .defineInRange("healthPerFloor", 4.0D, 0.0D, 100.0D);
    public static final ModConfigSpec.IntValue SPIRE_DEATH_WEAKNESS = SERVER_BUILDER
            .comment("Weakness duration, in ticks, after dying in the Spire. 20 ticks is one second.")
            .defineInRange("deathWeakness", 600, 0, 72000);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Rules inside gate dungeons and the Spire. Creative and spectator players are never affected.")
                .push("dungeons");
    }

    public static final ModConfigSpec.BooleanValue GATE_DUNGEONS = SERVER_BUILDER
            .comment("Opening a gate teleports the party into a generated dungeon in khaosnexus:gate_dungeon.",
                    "Killing the boss clears the gate. When false, gates spawn their wave around the gate block instead.")
            .define("gateDungeons", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DUNGEON_BOSSES = SERVER_BUILDER
            .comment("Boss mob per gate rank, in order E, D, C, B, A, S.")
            .defineList("bosses", List.of("khaosnexus:rift_brute", "khaosnexus:rift_brute", "khaosnexus:rift_brute",
                    "khaosnexus:spire_guardian", "khaosnexus:spire_guardian", "khaosnexus:rift_warden"),
                    () -> "khaosnexus:rift_brute", o -> o instanceof String);

    public static final ModConfigSpec.IntValue DUNGEON_ABANDON_MINUTES = SERVER_BUILDER
            .comment("Minutes a gate dungeon can sit with nobody inside before it collapses. The gate reopens as new,",
                    "and the dungeon's region is wiped and reused. 0 never collapses an instance that still has its gate.")
            .defineInRange("abandonMinutes", 60, 0, 10080);

    public static final ModConfigSpec.IntValue DUNGEON_RESET_BLOCKS_PER_TICK = SERVER_BUILDER
            .comment("Blocks wiped per server tick when a finished dungeon's region is reset for reuse.")
            .defineInRange("resetBlocksPerTick", 32768, 1024, 1048576);

    public static final ModConfigSpec.BooleanValue DUNGEON_NO_FLIGHT = SERVER_BUILDER
            .comment("Stop flight inside dungeons: creative-style mayfly, elytra gliding and modded flight are cancelled every tick.")
            .define("blockFlight", true);

    public static final ModConfigSpec.BooleanValue DUNGEON_NO_BUILD = SERVER_BUILDER
            .comment("Stop block breaking and placing inside dungeons. Doors, chests, levers and buttons still work.")
            .define("blockBuilding", true);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment(
                "Optional companion mods. A hook stays off unless that mod is installed and its toggle is true.",
                "No compile-time dependency on any of them; hooks look up attributes, entities and stage providers by id at runtime.")
                .push("compat");
    }

    public static final ModConfigSpec.BooleanValue COMPAT_ARS = SERVER_BUILDER
            .comment("Ars Nouveau: Caster, Lifebinder, Summoner and Necromancer gain Ars spell damage and mana; Wilden bosses join gate boss pools.")
            .define("arsNouveau", true);
    public static final ModConfigSpec.BooleanValue COMPAT_IRONS = SERVER_BUILDER
            .comment("Iron's Spells 'n Spellbooks: light support. Caster, Lifebinder, Summoner and Necromancer scale its spell power schools.")
            .define("ironsSpells", true);
    public static final ModConfigSpec.BooleanValue COMPAT_BOSS_MODS = SERVER_BUILDER
            .comment("Let installed boss mods (for example Cataclysm or Mowzie's Mobs) join dungeon boss pools from optionalBosses.")
            .define("bossMods", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OPTIONAL_BOSSES = SERVER_BUILDER
            .comment("RANK=entity_id entries. Used only when that entity exists; each dungeon of that rank has bossModChance to use one.")
            .defineListAllowEmpty("optionalBosses", List.of(
                    "B=mowziesmobs:ferrous_wroughtnaut", "B=mowziesmobs:umvuthi", "A=mowziesmobs:frostmaw",
                    "A=ars_nouveau:wilden_boss", "A=cataclysm:ender_guardian", "S=cataclysm:ignis", "S=cataclysm:the_harbinger",
                    "S=cataclysm:netherite_monstrosity"), () -> "S=modid:boss", o -> o instanceof String str && str.contains("="));
    public static final ModConfigSpec.DoubleValue BOSS_MOD_CHANCE = SERVER_BUILDER
            .comment("Chance a dungeon uses an optional boss when one is available for its rank.")
            .defineInRange("bossModChance", 0.35D, 0.0D, 1.0D);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OPTIONAL_LOOT = SERVER_BUILDER
            .comment("RANK=item_id or RANK=#tag entries added to gate rewards when present (accessory mods, relics and so on).",
                    "Each entry is rolled with a 20% chance per clear at that rank or higher.")
            .defineListAllowEmpty("optionalLoot", List.of(
                    "B=#curios:charm", "A=#curios:ring", "A=#curios:necklace", "S=#curios:belt"),
                    () -> "A=modid:item", o -> o instanceof String str && str.contains("="));
    public static final ModConfigSpec.DoubleValue OPTIONAL_LOOT_CHANCE = SERVER_BUILDER
            .comment("Chance per optionalLoot entry per clear.")
            .defineInRange("optionalLootChance", 0.2D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue MOD_BOSS_HEALTH = SERVER_BUILDER
            .comment("Extra health multiplier for optional boss-mod bosses in dungeons, on top of the rank multiplier.",
                    "Those bosses already have large health pools, so the default leaves them as their mod made them.")
            .defineInRange("modBossHealthScale", 1.0D, 0.1D, 10.0D);
    public static final ModConfigSpec.BooleanValue MOD_BOSS_WAKE = SERVER_BUILDER
            .comment("Wake optional bosses that spawn dormant (Mowzie's Wroughtnaut and Frostmaw, Cataclysm's Netherite Monstrosity)",
                    "so they fight in the boss room. Entries in modBossWake say how.")
            .define("modBossWakeUp", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOD_BOSS_WAKE_LIST = SERVER_BUILDER
            .comment("entity_id=nbtKey:value entries merged into a dormant boss's NBT after it spawns in a dungeon.")
            .defineListAllowEmpty("modBossWake", List.of(
                    "mowziesmobs:ferrous_wroughtnaut=active:1", "mowziesmobs:frostmaw=active:1",
                    "cataclysm:netherite_monstrosity=is_Awaken:1"),
                    () -> "modid:boss=key:1", o -> o instanceof String str && str.contains("=") && str.contains(":"));
    public static final ModConfigSpec.DoubleValue CLASS_BONUS_SCALE = SERVER_BUILDER
            .comment("Multiplier for every companion-mod class bonus. The bonuses themselves are datapack values in",
                    "data/<namespace>/khaosnexus_class_compat/<class>.json.")
            .defineInRange("classBonusScale", 1.0D, 0.0D, 10.0D);
    public static final ModConfigSpec.BooleanValue COMPAT_APOTHEOSIS = SERVER_BUILDER
            .comment("Apotheosis and Apothic Attributes: class bonuses on Apothic attributes, the forge scale below and the rift rules below.")
            .define("apotheosis", true);
    public static final ModConfigSpec.BooleanValue COMPAT_FTB_TEAMS = SERVER_BUILDER
            .comment("FTB Teams. Detection only. Parties stay on the built-in system.")
            .define("ftbTeams", true);
    public static final ModConfigSpec.BooleanValue COMPAT_FTB_CHUNKS = SERVER_BUILDER
            .comment("FTB Chunks. Detection only.")
            .define("ftbChunks", true);
    public static final ModConfigSpec.BooleanValue COMPAT_FTB_QUESTS = SERVER_BUILDER
            .comment("FTB Quests: hunter progress grants stages through FTB Library's stage provider (tags, KubeJS or Game Stages). No chapters are shipped.")
            .define("ftbQuests", true);
    public static final ModConfigSpec.BooleanValue COMPAT_CURIOS = SERVER_BUILDER
            .comment("Curios. Detection only.")
            .define("curios", true);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Hunter Forge.").push("forge");
    }

    public static final ModConfigSpec.DoubleValue FORGE_APOTHEOSIS_SCALE = SERVER_BUILDER
            .comment("Forge bonuses are multiplied by this while Apotheosis is installed, because Apotheosis affixes and gems",
                    "(percent damage, crit) multiply the forge's flat bonus. 1.0 = no change.")
            .defineInRange("apotheosisScale", 0.75D, 0.0D, 1.0D);

    static {
        SERVER_BUILDER.pop();
        SERVER_BUILDER.comment("Rift mobs and other mods' spawn systems.").push("rift");
    }

    public static final ModConfigSpec.BooleanValue ALLOW_APOTH_BOSSES = SERVER_BUILDER
            .comment("Let Apotheosis treat rift bosses (entity tag khaosnexus:apotheosis_exempt, plus every dungeon, raid and Spire boss)",
                    "like ordinary monsters: world-tier stat augments and random affix gear. Off keeps bosses as tuned here.")
            .define("allowApotheosisInvaderBosses", false);
    public static final ModConfigSpec.BooleanValue APOTH_GATE_TIERS = SERVER_BUILDER
            .comment("Regular gate and dungeon mobs use the Apotheosis world tier from apotheosisTierByRank instead of the",
                    "nearest player's world tier. Off leaves Apotheosis's own behaviour.")
            .define("apotheosisGateTiers", false);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> APOTH_TIER_BY_RANK = SERVER_BUILDER
            .comment("RANK=world_tier (haven, frontier, ascent, summit, pinnacle).")
            .defineListAllowEmpty("apotheosisTierByRank", List.of("E=haven", "D=haven", "C=frontier", "B=ascent", "A=summit", "S=pinnacle"),
                    () -> "S=pinnacle", o -> o instanceof String str && str.contains("="));

    static {
        SERVER_BUILDER.pop();
    }

    static {
        SERVER_BUILDER.comment("Material tiers: modded ores and ingots scale gate rewards by rank.",
                "Datapacks map item tags to ranks in data/<namespace>/khaosnexus_material_tiers/*.json.").push("materials");
    }

    public static final ModConfigSpec.BooleanValue MATERIAL_LOOT = SERVER_BUILDER
            .comment("Add tiered materials to gate rewards and dungeon chests.")
            .define("tieredLoot", true);

    public static final ModConfigSpec.BooleanValue MATERIAL_AUTO_DETECT = SERVER_BUILDER
            .comment("Pick up every other c:ingots/* tag in the pack at autoRank, so unlisted modded metals still drop.")
            .define("autoDetectIngots", true);

    public static final ModConfigSpec.ConfigValue<String> MATERIAL_AUTO_RANK = SERVER_BUILDER
            .comment("Rank for auto-detected ingots (E, D, C, B, A or S).")
            .define("autoRank", "C");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MATERIAL_TIER_OVERRIDES = SERVER_BUILDER
            .comment("Extra RANK=tag entries, for example \"S=c:ingots/my_metal\". Added on top of the datapack tiers.")
            .defineListAllowEmpty("tierOverrides", List.of(), () -> "C=c:ingots/example", o -> o instanceof String str && str.contains("="));

    static {
        SERVER_BUILDER.pop();
    }

    static {
        SERVER_BUILDER.comment("Hunter Card: the item that unlocks the hunter HUD and screens.").push("hunterCard");
    }

    public static final ModConfigSpec.BooleanValue HUNTER_CARD_REQUIRED = SERVER_BUILDER
            .comment("When true, the hunter HUD and screens stay hidden until a player has obtained a Hunter Card",
                    "(sold by the Guild Clerk villager). When false, every player has them from the start.")
            .define("required", true);

    public static final ModConfigSpec.IntValue CARD_GATE_RADIUS = SERVER_BUILDER
            .comment("Radius in blocks of the Hunter Card's nearby-gates list.")
            .defineInRange("gateRadius", 256, 16, 2048);

    static {
        SERVER_BUILDER.pop();
    }

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    private Config() {}

    public static int leashRadius() {
        return Math.max(LEASH_RADIUS.get(), ARENA_RADIUS.get() + 2);
    }

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue HUD_ENABLED = CLIENT_BUILDER
            .comment("Draw the hunter HUD panel.")
            .define("hudEnabled", true);
    public static final ModConfigSpec.IntValue HUD_X = CLIENT_BUILDER
            .comment("HUD panel left edge, in GUI pixels.")
            .defineInRange("hudX", 6, 0, 4000);
    public static final ModConfigSpec.IntValue HUD_Y = CLIENT_BUILDER
            .comment("HUD panel top edge, in GUI pixels.")
            .defineInRange("hudY", 6, 0, 4000);

    public static final ModConfigSpec.DoubleValue HUD_SCALE = CLIENT_BUILDER
            .comment("Hunter HUD scale.").defineInRange("hudScale", 1.0D, 0.5D, 2.0D);
    public static final ModConfigSpec.DoubleValue HUD_OPACITY = CLIENT_BUILDER
            .comment("Hunter HUD background opacity.").defineInRange("hudOpacity", 0.55D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue UI_SCALE = CLIENT_BUILDER
            .comment("Hunter Card screen scale.").defineInRange("uiScale", 1.0D, 0.6D, 1.6D);
    public static final ModConfigSpec.DoubleValue UI_OPACITY = CLIENT_BUILDER
            .comment("Hunter Card screen panel opacity.").defineInRange("uiOpacity", 0.85D, 0.2D, 1.0D);

    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    static boolean isRegistryId(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }

    public static final class RankValues {
        public final ModConfigSpec.ConfigValue<List<? extends String>> mobs;
        public final ModConfigSpec.IntValue mobCount;
        public final ModConfigSpec.IntValue weight;
        public final ModConfigSpec.DoubleValue healthMultiplier;
        public final ModConfigSpec.DoubleValue damageMultiplier;
        public final ModConfigSpec.ConfigValue<String> rewardItem;
        public final ModConfigSpec.IntValue rewardCount;
        public final ModConfigSpec.IntValue rewardXp;
        public final ModConfigSpec.IntValue hunterXp;

        private RankValues(
                ModConfigSpec.ConfigValue<List<? extends String>> mobs,
                ModConfigSpec.IntValue mobCount,
                ModConfigSpec.IntValue weight,
                ModConfigSpec.DoubleValue healthMultiplier,
                ModConfigSpec.DoubleValue damageMultiplier,
                ModConfigSpec.ConfigValue<String> rewardItem,
                ModConfigSpec.IntValue rewardCount,
                ModConfigSpec.IntValue rewardXp,
                ModConfigSpec.IntValue hunterXp) {
            this.mobs = mobs;
            this.mobCount = mobCount;
            this.weight = weight;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.rewardItem = rewardItem;
            this.rewardCount = rewardCount;
            this.rewardXp = rewardXp;
            this.hunterXp = hunterXp;
        }

        private static RankValues define(
                ModConfigSpec.Builder builder,
                String rank,
                List<String> defaultMobs,
                int defaultCount,
                int defaultWeight,
                double defaultHealth,
                double defaultDamage,
                String defaultReward,
                int defaultRewardCount,
                int defaultXp,
                int defaultHunterXp) {
            builder.comment("Spawn list, stat scaling, placement weight, and clear reward for rank " + rank + ".")
                    .push(rank);
            RankValues values = new RankValues(
                    builder.comment("Entity ids summoned for this rank. The wave cycles through the list.")
                            .defineList("mobs", defaultMobs, () -> "minecraft:zombie", Config::isRegistryId),
                    builder.comment("How many mobs this rank summons.")
                            .defineInRange("mobCount", defaultCount, 1, 40),
                    builder.comment("Relative chance a newly placed gate rolls this rank. 0 disables it.")
                            .defineInRange("weight", defaultWeight, 0, 10000),
                    builder.comment("Multiplier applied to each mob's max health after it spawns.")
                            .defineInRange("healthMultiplier", defaultHealth, 0.1D, 50.0D),
                    builder.comment("Multiplier applied to each mob's attack damage when it has that attribute.")
                            .defineInRange("damageMultiplier", defaultDamage, 0.1D, 50.0D),
                    builder.comment("Item id dropped when the wave is cleared.")
                            .define("rewardItem", defaultReward, Config::isRegistryId),
                    builder.comment("How many of the reward item to drop. 0 drops none.")
                            .defineInRange("rewardCount", defaultRewardCount, 0, 64),
                    builder.comment("Experience granted at the gate when the wave is cleared.")
                            .defineInRange("rewardXp", defaultXp, 0, 100000),
                    builder.comment("Hunter XP granted to the player who opened the gate when the wave is cleared.")
                            .defineInRange("hunterXp", defaultHunterXp, 0, 100000));
            builder.pop();
            return values;
        }
    }
}

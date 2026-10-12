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
                "Mob and item ids are registry names such as minecraft:zombie. The defaults use only vanilla so the mod stays standalone.")
                .push("gates");
    }

    public static final ModConfigSpec.IntValue ARENA_RADIUS = SERVER_BUILDER
            .comment("How many blocks from the gate the wave is placed.")
            .defineInRange("arenaRadius", 4, 2, 24);

    public static final ModConfigSpec.IntValue LEASH_RADIUS = SERVER_BUILDER
            .comment("Mobs that wander farther than this are pulled back to their spawn point. Keep this larger than arenaRadius.")
            .defineInRange("leashRadius", 10, 3, 48);

    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNING = SERVER_BUILDER
            .comment("When true, Nexus Gates appear near players on their own. Set this to false to leave gates to crafting and placing only.")
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
            List.of("minecraft:zombie"), 3, 45, 1.0D, 1.0D, "minecraft:emerald", 2, 5);
    public static final RankValues RANK_D = RankValues.define(SERVER_BUILDER, "D",
            List.of("minecraft:zombie", "minecraft:skeleton"), 5, 25, 1.35D, 1.15D, "minecraft:emerald", 4, 12);
    public static final RankValues RANK_C = RankValues.define(SERVER_BUILDER, "C",
            List.of("minecraft:skeleton", "minecraft:spider"), 6, 15, 1.75D, 1.35D, "minecraft:diamond", 1, 24);
    public static final RankValues RANK_B = RankValues.define(SERVER_BUILDER, "B",
            List.of("minecraft:pillager", "minecraft:husk"), 8, 8, 2.25D, 1.55D, "minecraft:diamond", 2, 40);
    public static final RankValues RANK_A = RankValues.define(SERVER_BUILDER, "A",
            List.of("minecraft:vindicator", "minecraft:witch"), 8, 5, 3.0D, 1.8D, "minecraft:netherite_scrap", 1, 65);
    public static final RankValues RANK_S = RankValues.define(SERVER_BUILDER, "S",
            List.of("minecraft:evoker", "minecraft:ravager"), 6, 2, 4.0D, 2.0D, "minecraft:netherite_ingot", 1, 100);

    static {
        SERVER_BUILDER.pop();
    }

    static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    private Config() {}

    public static int leashRadius() {
        return Math.max(LEASH_RADIUS.get(), ARENA_RADIUS.get() + 2);
    }

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

        private RankValues(
                ModConfigSpec.ConfigValue<List<? extends String>> mobs,
                ModConfigSpec.IntValue mobCount,
                ModConfigSpec.IntValue weight,
                ModConfigSpec.DoubleValue healthMultiplier,
                ModConfigSpec.DoubleValue damageMultiplier,
                ModConfigSpec.ConfigValue<String> rewardItem,
                ModConfigSpec.IntValue rewardCount,
                ModConfigSpec.IntValue rewardXp) {
            this.mobs = mobs;
            this.mobCount = mobCount;
            this.weight = weight;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.rewardItem = rewardItem;
            this.rewardCount = rewardCount;
            this.rewardXp = rewardXp;
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
                int defaultXp) {
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
                            .defineInRange("rewardXp", defaultXp, 0, 100000));
            builder.pop();
            return values;
        }
    }
}

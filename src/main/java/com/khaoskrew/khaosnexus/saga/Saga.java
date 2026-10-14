package com.khaoskrew.khaosnexus.saga;

import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.endgame.JsonPool;
import com.khaoskrew.khaosnexus.endgame.Prestige;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 1.2 Hollow Crown saga core, shared by every later realm (1.3 to 1.5 add seals/oaths as data files only):
 * Crown Shards currency, Oaths (third skill branch, unlocked at Paragon), Seal Gates and the General ambush roll.
 * State lives in the hunter profile's endgame tag under "Saga", so 1.1 saves migrate with no work.
 */
public final class Saga {
    public static final ResourceKey<Level> UNDERTHRONE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "underthrone"));
    static final String KEY = "Saga";

    private Saga() {}

    static CompoundTag tag(HunterProfile p) {
        if (!p.endgame.contains(KEY)) {
            p.endgame.put(KEY, new CompoundTag());
        }
        return p.endgame.getCompound(KEY);
    }

    // ---- Crown Shards ----
    public static int shards(HunterProfile p) {
        return tag(p).getInt("Shards");
    }

    public static void addShards(HunterProfile p, int n) {
        tag(p).putInt("Shards", Math.max(0, shards(p) + n));
    }

    /** Sinks (oath nodes, respec, reforging, seal keys) all go through here. */
    public static boolean spendShards(HunterProfile p, int n) {
        if (n < 0 || shards(p) < n) {
            return false;
        }
        tag(p).putInt("Shards", shards(p) - n);
        return true;
    }

    // ---- Oaths (third skill branch, keeps mastery picks) ----
    public static String oath(HunterProfile p) {
        return tag(p).getString("Oath");
    }

    public static int oathNodes(HunterProfile p) {
        return tag(p).getInt("OathNodes");
    }

    public static boolean canSwear(HunterProfile p, ResourceLocation id) {
        return Prestige.level(p) >= Prestige.NATIONAL && JsonPool.OATHS.get(id) != null && oath(p).isEmpty();
    }

    public static boolean swear(HunterProfile p, ResourceLocation id) {
        if (!canSwear(p, id)) {
            return false;
        }
        tag(p).putString("Oath", id.toString());
        tag(p).putInt("OathNodes", 0);
        return true;
    }

    /** Buys the next node of the sworn Oath with Crown Shards. */
    public static boolean unlockNode(HunterProfile p) {
        JsonObject o = oath(p).isEmpty() ? null : JsonPool.OATHS.get(ResourceLocation.parse(oath(p)));
        if (o == null) {
            return false;
        }
        var costs = o.getAsJsonArray("node_cost");
        int next = oathNodes(p);
        if (next >= costs.size() || !spendShards(p, costs.get(next).getAsInt())) {
            return false;
        }
        tag(p).putInt("OathNodes", next + 1);
        return true;
    }

    public static boolean respec(HunterProfile p) {
        if (oath(p).isEmpty() || !spendShards(p, Config.OATH_RESPEC_SHARDS.get())) {
            return false;
        }
        tag(p).remove("Oath");
        tag(p).remove("OathNodes");
        return true;
    }

    // ---- Seals ----
    public static boolean sealBroken(HunterProfile p, ResourceLocation seal) {
        return tag(p).getCompound("Seals").getBoolean(seal.toString());
    }

    public static boolean canOpenSeal(HunterProfile p, ResourceLocation seal) {
        JsonObject s = JsonPool.SEALS.get(seal);
        if (s == null || Prestige.level(p) < s.get("min_prestige").getAsInt()) {
            return false;
        }
        int order = s.get("order").getAsInt();
        for (var e : JsonPool.SEALS.all().entrySet()) {
            if (e.getValue().get("order").getAsInt() < order && !sealBroken(p, e.getKey())) {
                return false; // seals break in story order
            }
        }
        return shards(p) >= s.get("shard_cost").getAsInt();
    }

    public static void breakSeal(HunterProfile p, ResourceLocation seal) {
        CompoundTag seals = tag(p).getCompound("Seals");
        seals.putBoolean(seal.toString(), true);
        tag(p).put("Seals", seals);
    }

    /** Each guardian listed on a seal is a ward; the seal breaks once every ward has fallen. */
    public static boolean wardSlain(HunterProfile p, ResourceLocation seal, ResourceLocation guardian) {
        return tag(p).getCompound("Wards").getCompound(seal.toString()).getBoolean(guardian.toString());
    }

    /** Records a slain ward; returns how many of the seal's wards are still standing. */
    public static int slayWard(HunterProfile p, ResourceLocation seal, ResourceLocation guardian) {
        CompoundTag wards = tag(p).getCompound("Wards");
        CompoundTag w = wards.getCompound(seal.toString());
        w.putBoolean(guardian.toString(), true);
        wards.put(seal.toString(), w);
        tag(p).put("Wards", wards);
        return wardsLeft(p, seal);
    }

    public static int wardsLeft(HunterProfile p, ResourceLocation seal) {
        JsonObject s = JsonPool.SEALS.get(seal);
        if (s == null || !s.has("guardians")) return 0;
        int left = 0;
        for (var g : s.getAsJsonArray("guardians")) if (!wardSlain(p, seal, ResourceLocation.parse(g.getAsString()))) left++;
        return left;
    }

    /** Records a Hollow King kill; returns the new total. */
    public static int slayKing(HunterProfile p) {
        CompoundTag k = tag(p).getCompound("King");
        k.putInt("Slain", k.getInt("Slain") + 1);
        tag(p).put("King", k);
        return k.getInt("Slain");
    }

    public static int kingsSlain(HunterProfile p) {
        return tag(p).getCompound("King").getInt("Slain");
    }

    public static void resetSeals(HunterProfile p) {
        tag(p).remove("Seals"); tag(p).remove("Wards"); tag(p).remove("King");
    }

    public static String status(HunterProfile p) {
        return "Seals=" + tag(p).getCompound("Seals") + " Wards=" + tag(p).getCompound("Wards") + " King=" + tag(p).getCompound("King") + " Shards=" + shards(p);
    }

    // ---- Crownless General ambush ----
    /** Rolled when a normal gate opens: only A/S, never Crimson Rifts/challenges (caller passes normal=true). */
    public static boolean rollAmbush(GateRank rank, boolean normal, RandomSource random) {
        return normal && rank.ordinal() >= GateRank.A.ordinal() && random.nextDouble() < Config.GENERAL_AMBUSH_CHANCE.get();
    }
}

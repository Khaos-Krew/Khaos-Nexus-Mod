package com.khaoskrew.khaosnexus.skill;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hunter level, skill points and the spend / respec rules. Points per class: every class gets the full pool. */
public final class SkillService {
    public enum Result { OK, NO_CLASS, UNKNOWN, MAXED, NO_POINTS, RANK_LOCKED, NEEDS_PARENT, NO_RESPEC_ITEM }

    private SkillService() {}

    public static int level(int xp) {
        return Math.min(Config.SKILL_MAX_LEVEL.get(), 1 + xp / Math.max(1, Config.SKILL_XP_PER_LEVEL.get()));
    }

    public static int totalPoints(HunterProfile p) {
        return (level(p.xp) - 1) * Config.SKILL_POINTS_PER_LEVEL.get();
    }

    /** Ranks for the given class only; keys in the profile are class.node. */
    public static Map<String, Integer> ranks(HunterProfile p, @Nullable HunterClass c) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (c == null || p.skills.isEmpty()) {
            return out;
        }
        String prefix = c.id() + ".";
        for (String part : p.skills.split(",")) {
            int eq = part.lastIndexOf(':');
            if (eq > 0 && part.startsWith(prefix)) {
                try {
                    out.put(part.substring(prefix.length(), eq), Integer.parseInt(part.substring(eq + 1)));
                } catch (NumberFormatException ignored) {
                    // skip corrupt entry
                }
            }
        }
        return out;
    }

    public static int rank(HunterProfile p, @Nullable HunterClass c, String node) {
        return ranks(p, c).getOrDefault(node, 0);
    }

    public static int spent(HunterProfile p, @Nullable HunterClass c) {
        int total = 0;
        for (var e : ranks(p, c).entrySet()) {
            SkillTrees.Node n = SkillTrees.node(c, e.getKey());
            total += (n == null ? 1 : n.cost()) * e.getValue();
        }
        return total;
    }

    public static int available(HunterProfile p, @Nullable HunterClass c) {
        return Math.max(0, totalPoints(p) - spent(p, c));
    }

    private static void setRank(HunterProfile p, HunterClass c, String node, int rank) {
        StringBuilder sb = new StringBuilder();
        String key = c.id() + "." + node;
        boolean written = false;
        if (!p.skills.isEmpty()) {
            for (String part : p.skills.split(",")) {
                int eq = part.lastIndexOf(':');
                if (eq > 0 && part.substring(0, eq).equals(key)) {
                    if (rank > 0) {
                        sb.append(sb.isEmpty() ? "" : ",").append(key).append(':').append(rank);
                    }
                    written = true;
                } else if (!part.isEmpty()) {
                    sb.append(sb.isEmpty() ? "" : ",").append(part);
                }
            }
        }
        if (!written && rank > 0) {
            sb.append(sb.isEmpty() ? "" : ",").append(key).append(':').append(rank);
        }
        p.skills = sb.toString();
    }

    /** Validation shared by the server and the card UI. */
    public static Result check(HunterProfile p, @Nullable HunterClass c, String nodeId) {
        if (c == null) {
            return Result.NO_CLASS;
        }
        SkillTrees.Node n = SkillTrees.node(c, nodeId);
        if (n == null) {
            return Result.UNKNOWN;
        }
        if (rank(p, c, nodeId) >= n.maxRank()) {
            return Result.MAXED;
        }
        if (HunterRanks.rankOfXp(p.xp).ordinal() < n.minRank().ordinal()) {
            return Result.RANK_LOCKED;
        }
        for (String req : n.requires()) {
            if (rank(p, c, req) <= 0) {
                return Result.NEEDS_PARENT;
            }
        }
        if (available(p, c) < n.cost()) {
            return Result.NO_POINTS;
        }
        return Result.OK;
    }

    public static Result spend(ServerPlayer player, String nodeId) {
        HunterProfile p = HunterRanks.profile(player);
        HunterClass c = ClassService.equipped(player);
        Result r = check(p, c, nodeId);
        if (r == Result.OK) {
            setRank(p, c, nodeId, rank(p, c, nodeId) + 1);
            applyPassives(player);
            HunterRanks.sync(player);
        }
        return r;
    }

    @Nullable
    public static Item respecItem() {
        String id = Config.SKILL_RESPEC_ITEM.get();
        if (id.isBlank() || Config.SKILL_RESPEC_COUNT.get() <= 0) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? null : BuiltInRegistries.ITEM.get(rl);
    }

    /** Refunds every point of the equipped class. Costs the configured item unless the player is creative. */
    public static Result respec(ServerPlayer player) {
        HunterProfile p = HunterRanks.profile(player);
        HunterClass c = ClassService.equipped(player);
        if (c == null) {
            return Result.NO_CLASS;
        }
        Item cost = respecItem();
        int need = Config.SKILL_RESPEC_COUNT.get();
        if (cost != null && !player.getAbilities().instabuild) {
            if (player.getInventory().countItem(cost) < need) {
                return Result.NO_RESPEC_ITEM;
            }
            int left = need;
            for (ItemStack s : player.getInventory().items) {
                if (left > 0 && s.is(cost)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        for (String id : ranks(p, c).keySet()) {
            setRank(p, c, id, 0);
        }
        applyPassives(player);
        HunterRanks.sync(player);
        return Result.OK;
    }

    /** Multiplier for an active: 1 + sum(per_rank * rank) of matching nodes (cooldown nodes reduce, floor 0.25). */
    public static double mult(Player player, boolean primary, SkillTrees.Stat stat) {
        HunterProfile p = player.getData(com.khaoskrew.khaosnexus.hunter.ModAttachments.HUNTER);
        HunterClass c = HunterClass.byId(p.classId);
        double sum = 0;
        SkillTrees.Kind kind = primary ? SkillTrees.Kind.PRIMARY : SkillTrees.Kind.SECONDARY;
        for (var e : ranks(p, c).entrySet()) {
            SkillTrees.Node n = SkillTrees.node(c, e.getKey());
            if (n != null && (n.kind() == kind || n.kind() == SkillTrees.Kind.CAPSTONE) && n.stat() == stat) {  // capstones boost both actives
                sum += n.perRank() * e.getValue();
            }
        }
        return stat == SkillTrees.Stat.COOLDOWN ? Math.max(0.25D, 1.0D - sum) : 1.0D + sum;
    }

    static ResourceLocation modifierId(String node) {
        return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "skill/" + node);
    }

    /** Clears every skill attribute modifier and re-adds the equipped class's passives and capstone. */
    public static void applyPassives(ServerPlayer player) {
        for (Holder<Attribute> h : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance inst = player.getAttribute(h);
            if (inst != null) {
                for (AttributeModifier m : java.util.List.copyOf(inst.getModifiers())) {
                    if (m.id().getNamespace().equals(KhaosNexus.MODID) && m.id().getPath().startsWith("skill/")) {
                        inst.removeModifier(m.id());
                    }
                }
            }
        }
        HunterProfile p = HunterRanks.profile(player);
        HunterClass c = ClassService.equipped(player);
        for (var e : ranks(p, c).entrySet()) {
            SkillTrees.Node n = SkillTrees.node(c, e.getKey());
            if (n == null || n.attribute() == null || e.getValue() <= 0) {
                continue;
            }
            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(n.attribute());
            if (holder.isEmpty()) {
                continue;
            }
            AttributeInstance inst = player.getAttribute(holder.get());
            if (inst != null) {
                inst.addOrReplacePermanentModifier(new AttributeModifier(modifierId(n.id()), n.perRank() * e.getValue(), n.op()));
            }
        }
    }

    public static com.khaoskrew.khaosnexus.network.CardPayload.SkillHeader header(ServerPlayer player) {
        HunterProfile p = HunterRanks.profile(player);
        HunterClass c = ClassService.equipped(player);
        Item cost = respecItem();
        return new com.khaoskrew.khaosnexus.network.CardPayload.SkillHeader(level(p.xp), available(p, c), totalPoints(p),
                c == null ? "" : c.id(), cost == null ? "" : BuiltInRegistries.ITEM.getKey(cost).toString(), cost == null ? 0 : Config.SKILL_RESPEC_COUNT.get());
    }

    public static java.util.List<com.khaoskrew.khaosnexus.network.CardPayload.SkillLine> lines(ServerPlayer player) {
        HunterProfile p = HunterRanks.profile(player);
        HunterClass c = ClassService.equipped(player);
        java.util.List<com.khaoskrew.khaosnexus.network.CardPayload.SkillLine> out = new java.util.ArrayList<>();
        for (SkillTrees.Node n : SkillTrees.tree(c)) {
            int state = switch (check(p, c, n.id())) {
                case OK -> 0;
                case RANK_LOCKED -> 1;
                case NEEDS_PARENT -> 2;
                case MAXED -> 3;
                default -> 4;
            };
            out.add(new com.khaoskrew.khaosnexus.network.CardPayload.SkillLine(n.id(), n.kind().name().toLowerCase(java.util.Locale.ROOT),
                    rank(p, c, n.id()), n.maxRank(), n.cost(), n.minRank().name(), n.x(), n.y(), state));
        }
        return out;
    }
}

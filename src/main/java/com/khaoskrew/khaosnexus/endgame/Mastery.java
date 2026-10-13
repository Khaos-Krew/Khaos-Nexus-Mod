package com.khaoskrew.khaosnexus.endgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Per-class mastery tracks past rank S. A perk file: "class" (class id or "any"), "min_level", "cost", optional
 * "group" (only one perk per group, so groups are sidegrade choices) and "modifiers" [{attribute, amount, operation}].
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class Mastery {
    private static final String XP = "MasteryXp";
    private static final String PERKS = "MasteryPerks";
    private static final String MOD_PREFIX = "mastery_";

    private Mastery() {}

    public static int xp(HunterProfile p, String cls) {
        return p.endgame.getCompound(XP).getInt(cls);
    }

    public static int level(HunterProfile p, String cls) {
        return Math.min(Config.MASTERY_MAX_LEVEL.get(), xp(p, cls) / Config.MASTERY_XP_PER_LEVEL.get());
    }

    public static List<String> perks(HunterProfile p, String cls) {
        String s = p.endgame.getCompound(PERKS).getString(cls);
        return s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split(",")));
    }

    public static int spent(HunterProfile p, String cls) {
        int n = 0;
        for (String id : perks(p, cls)) {
            JsonObject o = JsonPool.MASTERY.get(ResourceLocation.parse(id));
            n += o == null ? 0 : GsonHelper.getAsInt(o, "cost", 1);
        }
        return n;
    }

    /** Called from HunterRanks.addXp. Only XP earned while already S counts. */
    public static void onXp(HunterProfile p, GateRank rankBefore, int amount) {
        if (!Config.MASTERY.get() || amount <= 0 || rankBefore != GateRank.S || p.classId.isEmpty()) {
            return;
        }
        CompoundTag t = p.endgame.getCompound(XP);
        int cap = Config.MASTERY_MAX_LEVEL.get() * Config.MASTERY_XP_PER_LEVEL.get();
        t.putInt(p.classId, Math.min(cap, t.getInt(p.classId) + amount));
        p.endgame.put(XP, t);
    }

    /** Returns null on success, else a translation key explaining the refusal. */
    public static String pick(HunterProfile p, ResourceLocation id) {
        JsonObject o = JsonPool.MASTERY.get(id);
        String cls = p.classId;
        if (o == null) {
            return "khaosnexus.mastery.unknown";
        }
        String perkClass = GsonHelper.getAsString(o, "class", "any");
        if (cls.isEmpty() || !(perkClass.equals("any") || perkClass.equals(cls))) {
            return "khaosnexus.mastery.wrong_class";
        }
        List<String> owned = perks(p, cls);
        if (owned.contains(id.toString())) {
            return "khaosnexus.mastery.owned";
        }
        if (level(p, cls) < GsonHelper.getAsInt(o, "min_level", 1)) {
            return "khaosnexus.mastery.level";
        }
        if (spent(p, cls) + GsonHelper.getAsInt(o, "cost", 1) > level(p, cls)) {
            return "khaosnexus.mastery.points";
        }
        String group = GsonHelper.getAsString(o, "group", "");
        if (!group.isEmpty()) {
            for (String other : owned) {
                JsonObject oo = JsonPool.MASTERY.get(ResourceLocation.parse(other));
                if (oo != null && group.equals(GsonHelper.getAsString(oo, "group", ""))) {
                    return "khaosnexus.mastery.group";
                }
            }
        }
        owned.add(id.toString());
        CompoundTag t = p.endgame.getCompound(PERKS);
        t.putString(cls, String.join(",", owned));
        p.endgame.put(PERKS, t);
        return null;
    }

    public static void reset(HunterProfile p) {
        CompoundTag t = p.endgame.getCompound(PERKS);
        t.remove(p.classId);
        p.endgame.put(PERKS, t);
    }

    public static AttributeModifier.Operation op(String s) {
        return switch (s) {
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    /** Re-applies the active class's perk modifiers, removing stale ones. */
    public static void apply(Player player) {
        HunterProfile p = HunterRanks.profile(player);
        List<String> active = Config.MASTERY.get() && !p.classId.isEmpty() ? perks(p, p.classId) : List.of();
        for (Holder<Attribute> attr : BuiltInRegistries.ATTRIBUTE.holders().map(h -> (Holder<Attribute>) h).toList()) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) {
                continue;
            }
            for (AttributeModifier m : List.copyOf(inst.getModifiers())) {
                if (m.id().getNamespace().equals(KhaosNexus.MODID) && m.id().getPath().startsWith(MOD_PREFIX)) {
                    inst.removeModifier(m.id());
                }
            }
        }
        for (String perk : active) {
            JsonObject o = JsonPool.MASTERY.get(ResourceLocation.parse(perk));
            if (o == null) {
                continue;
            }
            int i = 0;
            for (JsonElement el : GsonHelper.getAsJsonArray(o, "modifiers", new com.google.gson.JsonArray())) {
                JsonObject m = el.getAsJsonObject();
                var holder = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(GsonHelper.getAsString(m, "attribute")));
                AttributeInstance inst = holder.map(player::getAttribute).orElse(null);
                if (inst != null) {
                    ResourceLocation mid = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID,
                            MOD_PREFIX + perk.replace(':', '_') + "_" + i++);
                    inst.addOrReplacePermanentModifier(new AttributeModifier(mid, GsonHelper.getAsDouble(m, "amount"),
                            op(GsonHelper.getAsString(m, "operation", "add_value"))));
                }
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static Component status(HunterProfile p) {
        if (p.classId.isEmpty()) {
            return Component.translatable("khaosnexus.mastery.no_class");
        }
        Component out = Component.translatable("khaosnexus.mastery.status", p.classId, level(p, p.classId),
                Config.MASTERY_MAX_LEVEL.get(), level(p, p.classId) - spent(p, p.classId));
        for (var e : JsonPool.MASTERY.all().entrySet()) {
            String c = GsonHelper.getAsString(e.getValue(), "class", "any");
            if (c.equals("any") || c.equals(p.classId)) {
                boolean owned = perks(p, p.classId).contains(e.getKey().toString());
                out = out.copy().append("\n " + (owned ? "[x] " : "[ ] ") + e.getKey().getPath() + " (lv "
                        + GsonHelper.getAsInt(e.getValue(), "min_level", 1) + "): ")
                        .append(Component.translatable("khaosnexus.mastery." + e.getKey().getNamespace() + "." + e.getKey().getPath()));
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 100 == 7) {
            apply(player);
        }
    }

    public static boolean command(ServerPlayer player, String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : KhaosNexus.MODID + ":" + raw);
        HunterProfile p = HunterRanks.profile(player);
        String err = id == null ? "khaosnexus.mastery.unknown" : pick(p, id);
        if (err != null) {
            player.displayClientMessage(Component.translatable(err, raw), false);
            return false;
        }
        apply(player);
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.mastery.picked",
                Component.translatable("khaosnexus.mastery." + id.getNamespace() + "." + id.getPath())), false);
        return true;
    }
}

package com.khaoskrew.khaosnexus.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Optional boss and loot pools from companion mods; an entry is only used when what it names exists. */
public final class CompatPools {
    private CompatPools() {}

    private static boolean matches(String line, GateRank rank, boolean atOrBelow) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            return false;
        }
        GateRank r = GateRank.byName(parts[0].trim());
        return atOrBelow ? r.ordinal() <= rank.ordinal() : r == rank;
    }

    public static Optional<EntityType<?>> boss(GateRank rank, RandomSource random) {
        if (!Config.COMPAT_BOSS_MODS.get() || random.nextDouble() >= Config.BOSS_MOD_CHANCE.get()) {
            return Optional.empty();
        }
        List<EntityType<?>> pool = new ArrayList<>();
        for (String line : Config.OPTIONAL_BOSSES.get()) {
            if (matches(line, rank, false)) {
                ResourceLocation id = ResourceLocation.tryParse(line.split("=", 2)[1].trim());
                if (id != null && !allowedAt(id, rank)) {
                    continue;
                }
                if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    pool.add(BuiltInRegistries.ENTITY_TYPE.get(id));
                }
            }
        }
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
    }

    /** Cataclysm bosses are S-rank only unless compat.cataclysmAnyRank is on. */
    public static boolean allowedAt(ResourceLocation id, GateRank rank) {
        return rank == GateRank.S || !"cataclysm".equals(id.getNamespace()) || Config.CATACLYSM_ANY_RANK.get();
    }

    public static List<ItemStack> loot(GateRank rank, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (String line : Config.OPTIONAL_LOOT.get()) {
            if (!matches(line, rank, true) || random.nextDouble() >= Config.OPTIONAL_LOOT_CHANCE.get()) {
                continue;
            }
            String ref = line.split("=", 2)[1].trim();
            List<Item> items = new ArrayList<>();
            if (ref.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(ref.substring(1));
                if (id != null) {
                    for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id))) {
                        items.add(h.value());
                    }
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(ref);
                if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                    items.add(BuiltInRegistries.ITEM.get(id));
                }
            }
            if (!items.isEmpty()) {
                out.add(new ItemStack(items.get(random.nextInt(items.size()))));
            }
        }
        return out;
    }

    /** Wakes a dormant boss-mod boss by merging the NBT from {@code compat.modBossWake}. Returns true if anything changed. */
    public static boolean wake(net.minecraft.world.entity.Mob boss) {
        if (!Config.MOD_BOSS_WAKE.get()) {
            return false;
        }
        String id = EntityType.getKey(boss.getType()).toString();
        net.minecraft.nbt.CompoundTag extra = new net.minecraft.nbt.CompoundTag();
        for (String line : Config.MOD_BOSS_WAKE_LIST.get()) {
            String[] p = line.split("=", 2);
            int c = p.length == 2 ? p[1].lastIndexOf(':') : -1;
            if (c > 0 && p[0].trim().equals(id)) {
                String key = p[1].substring(0, c).trim();
                String val = p[1].substring(c + 1).trim();
                try {
                    extra.putInt(key, Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    extra.putString(key, val);
                }
            }
        }
        if (extra.isEmpty()) {
            return false;
        }
        net.minecraft.nbt.CompoundTag tag = boss.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        tag.merge(extra);
        java.util.UUID uuid = boss.getUUID();
        boss.load(tag);
        boss.setUUID(uuid);
        return true;
    }
}

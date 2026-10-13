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
                if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    pool.add(BuiltInRegistries.ENTITY_TYPE.get(id));
                }
            }
        }
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
    }

    public static List<ItemStack> loot(GateRank rank, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (String line : Config.OPTIONAL_LOOT.get()) {
            if (!matches(line, rank, true) || random.nextFloat() >= 0.2F) {
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
}

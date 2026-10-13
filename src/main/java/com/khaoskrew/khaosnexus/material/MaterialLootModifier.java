package com.khaoskrew.khaosnexus.material;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Adds tiered pack materials to gate dungeon chests (loot tables khaosnexus:dungeon/<rank>). */
public class MaterialLootModifier extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KhaosNexus.MODID);
    public static final MapCodec<MaterialLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, MaterialLootModifier::new));
    static {
        SERIALIZERS.register("material_tiers", () -> CODEC);
    }

    public MaterialLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var id = context.getQueriedLootTableId();
        if (id.getNamespace().equals(KhaosNexus.MODID) && id.getPath().startsWith("dungeon/")) {
            GateRank rank = GateRank.byName(id.getPath().substring("dungeon/".length()).toUpperCase(java.util.Locale.ROOT));
            loot.addAll(MaterialTiers.roll(rank, context.getRandom()));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

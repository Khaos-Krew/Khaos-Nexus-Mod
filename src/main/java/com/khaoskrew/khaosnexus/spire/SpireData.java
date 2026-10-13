package com.khaoskrew.khaosnexus.spire;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SpireData extends SavedData {
    public static final String ID = KhaosNexus.MODID + "_spire";
    public int season = 1;
    public int bestFloor;
    public String holder = "";

    public static SpireData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(SpireData::new, SpireData::load), ID);
    }

    private static SpireData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpireData data = new SpireData();
        data.season = Math.max(1, tag.getInt("Season"));
        data.bestFloor = tag.getInt("BestFloor");
        data.holder = tag.getString("Holder");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Season", this.season);
        tag.putInt("BestFloor", this.bestFloor);
        tag.putString("Holder", this.holder);
        return tag;
    }
}

package com.khaoskrew.khaosnexus.gate;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One counter for the whole server, stored on the overworld, so a Minecraft day
 * allows only the configured number of natural gates no matter the dimension.
 */
public final class GateSpawnData extends SavedData {
    public static final String ID = KhaosNexus.MODID + "_gate_spawns";

    private long day = Long.MIN_VALUE;
    private int spawned;

    public static GateSpawnData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(GateSpawnData::new, GateSpawnData::load), ID);
    }

    private static GateSpawnData load(CompoundTag tag, HolderLookup.Provider registries) {
        GateSpawnData data = new GateSpawnData();
        data.day = tag.getLong("Day");
        data.spawned = tag.getInt("Spawned");
        return data;
    }

    public boolean hasRoom(long today, int limit) {
        if (this.day != today) {
            return limit > 0;
        }
        return this.spawned < limit;
    }

    public void markSpawned(long today) {
        if (this.day != today) {
            this.day = today;
            this.spawned = 0;
        }
        this.spawned++;
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("Day", this.day);
        tag.putInt("Spawned", this.spawned);
        return tag;
    }
}

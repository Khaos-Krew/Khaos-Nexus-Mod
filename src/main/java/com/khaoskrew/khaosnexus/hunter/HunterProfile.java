package com.khaoskrew.khaosnexus.hunter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * One player's hunter progress. Saved on the player and synced to that client.
 */
public final class HunterProfile implements INBTSerializable<CompoundTag> {
    public int xp;
    public String classId = "";
    public long swapReady;
    public long abilityReady;
    public long abilityAltReady;
    public int spireFloor = 1;
    public int spireCheckpoint = 1;
    public int spireSeason;
    public int spireBest;
    public String title = "";
    public String titles = "";
    public long bountyDay = -1L;
    public int bountyGates;
    public int bountyRaids;
    public int bountyClaims;
    public long raidWeek = -1L;
    public int partySize = 1;
    public int gatesCleared;
    public int raidsCleared;
    /** Set once the player has held a Hunter Card. Never cleared, so losing the card keeps the unlock. */
    public boolean cardUnlocked;
    /** Skill ranks, "class.node:rank" comma list. */
    public String skills = "";

    public static final StreamCodec<RegistryFriendlyByteBuf, HunterProfile> STREAM_CODEC = StreamCodec.of(
            HunterProfile::encode,
            HunterProfile::decode);

    public boolean hasProgress() {
        return this.xp > 0 || !this.classId.isEmpty() || this.gatesCleared > 0 || this.raidsCleared > 0
                || this.spireBest > 0 || !this.titles.isEmpty();
    }

    public boolean hasTitle(String id) {
        if (id == null || id.isEmpty() || this.titles.isEmpty()) {
            return false;
        }
        for (String part : this.titles.split(",")) {
            if (part.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public void addTitle(String id) {
        if (this.hasTitle(id)) {
            return;
        }
        this.titles = this.titles.isEmpty() ? id : this.titles + "," + id;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Xp", this.xp);
        tag.putString("ClassId", this.classId);
        tag.putLong("SwapReady", this.swapReady);
        tag.putLong("AbilityReady", this.abilityReady);
        tag.putLong("AbilityAltReady", this.abilityAltReady);
        tag.putInt("SpireFloor", this.spireFloor);
        tag.putInt("SpireCheckpoint", this.spireCheckpoint);
        tag.putInt("SpireSeason", this.spireSeason);
        tag.putInt("SpireBest", this.spireBest);
        tag.putString("Title", this.title);
        tag.putString("Titles", this.titles);
        tag.putLong("BountyDay", this.bountyDay);
        tag.putInt("BountyGates", this.bountyGates);
        tag.putInt("BountyRaids", this.bountyRaids);
        tag.putInt("BountyClaims", this.bountyClaims);
        tag.putLong("RaidWeek", this.raidWeek);
        tag.putInt("PartySize", this.partySize);
        tag.putInt("GatesCleared", this.gatesCleared);
        tag.putInt("RaidsCleared", this.raidsCleared);
        tag.putBoolean("CardUnlocked", this.cardUnlocked);
        tag.putString("Skills", this.skills);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.xp = tag.getInt("Xp");
        this.classId = tag.getString("ClassId");
        this.swapReady = tag.getLong("SwapReady");
        this.abilityReady = tag.getLong("AbilityReady");
        this.abilityAltReady = tag.getLong("AbilityAltReady");
        this.spireFloor = Math.max(1, tag.contains("SpireFloor") ? tag.getInt("SpireFloor") : 1);
        this.spireCheckpoint = Math.max(1, tag.contains("SpireCheckpoint") ? tag.getInt("SpireCheckpoint") : 1);
        this.spireSeason = tag.getInt("SpireSeason");
        this.spireBest = tag.getInt("SpireBest");
        this.title = tag.getString("Title");
        this.titles = tag.getString("Titles");
        this.bountyDay = tag.contains("BountyDay") ? tag.getLong("BountyDay") : -1L;
        this.bountyGates = tag.getInt("BountyGates");
        this.bountyRaids = tag.getInt("BountyRaids");
        this.bountyClaims = tag.getInt("BountyClaims");
        this.raidWeek = tag.contains("RaidWeek") ? tag.getLong("RaidWeek") : -1L;
        this.partySize = Math.max(1, tag.contains("PartySize") ? tag.getInt("PartySize") : 1);
        this.gatesCleared = tag.getInt("GatesCleared");
        this.raidsCleared = tag.getInt("RaidsCleared");
        // Saves from before the Hunter Card: anyone who already made progress keeps their hunter UI.
        this.skills = tag.getString("Skills");
        this.cardUnlocked = tag.contains("CardUnlocked") ? tag.getBoolean("CardUnlocked") : this.hasProgress();
    }

    private static void encode(RegistryFriendlyByteBuf buf, HunterProfile profile) {
        buf.writeVarInt(profile.xp);
        buf.writeUtf(profile.classId);
        buf.writeVarLong(profile.swapReady);
        buf.writeVarLong(profile.abilityReady);
        buf.writeVarLong(profile.abilityAltReady);
        buf.writeVarInt(profile.spireFloor);
        buf.writeVarInt(profile.spireCheckpoint);
        buf.writeVarInt(profile.spireSeason);
        buf.writeVarInt(profile.spireBest);
        buf.writeUtf(profile.title);
        buf.writeUtf(profile.titles);
        buf.writeLong(profile.bountyDay);
        buf.writeVarInt(profile.bountyGates);
        buf.writeVarInt(profile.bountyRaids);
        buf.writeVarInt(profile.bountyClaims);
        buf.writeLong(profile.raidWeek);
        buf.writeVarInt(profile.partySize);
        buf.writeVarInt(profile.gatesCleared);
        buf.writeVarInt(profile.raidsCleared);
        buf.writeBoolean(profile.cardUnlocked);
        buf.writeUtf(profile.skills);
    }

    private static HunterProfile decode(RegistryFriendlyByteBuf buf) {
        HunterProfile profile = new HunterProfile();
        profile.xp = buf.readVarInt();
        profile.classId = buf.readUtf();
        profile.swapReady = buf.readVarLong();
        profile.abilityReady = buf.readVarLong();
        profile.abilityAltReady = buf.readVarLong();
        profile.spireFloor = buf.readVarInt();
        profile.spireCheckpoint = buf.readVarInt();
        profile.spireSeason = buf.readVarInt();
        profile.spireBest = buf.readVarInt();
        profile.title = buf.readUtf();
        profile.titles = buf.readUtf();
        profile.bountyDay = buf.readLong();
        profile.bountyGates = buf.readVarInt();
        profile.bountyRaids = buf.readVarInt();
        profile.bountyClaims = buf.readVarInt();
        profile.raidWeek = buf.readLong();
        profile.partySize = buf.readVarInt();
        profile.gatesCleared = buf.readVarInt();
        profile.raidsCleared = buf.readVarInt();
        profile.cardUnlocked = buf.readBoolean();
        profile.skills = buf.readUtf();
        return profile;
    }
}

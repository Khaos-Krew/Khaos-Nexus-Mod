package com.khaoskrew.khaosnexus.compat.ftb;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.saga.Saga;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * FTB Quests task and reward types (1.2). Registered from {@link com.khaoskrew.khaosnexus.compat.FtbQuestsCompat} only
 * when FTB Quests is loaded. Tasks: {@code khaosnexus:hunter_rank} (reach a rank), {@code khaosnexus:oath_sworn}
 * (swear any or a named oath). Rewards: {@code khaosnexus:hunter_xp}, {@code khaosnexus:crown_shards}.
 */
public final class KhaosTasks {
    private KhaosTasks() {}

    static TaskType RANK, OATH;
    static RewardType XP, SHARDS;

    static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, p); }

    public static void register() {
        RANK = TaskTypes.register(id("hunter_rank"), RankTask::new, () -> Icon.getIcon("khaosnexus:item/hunter_card"));
        OATH = TaskTypes.register(id("oath_sworn"), OathTask::new, () -> Icon.getIcon("khaosnexus:item/crown_shard"));
        XP = RewardTypes.register(id("hunter_xp"), (i, q) -> new ValueReward(i, q, true), () -> Icon.getIcon("khaosnexus:item/hunter_card"));
        SHARDS = RewardTypes.register(id("crown_shards"), (i, q) -> new ValueReward(i, q, false), () -> Icon.getIcon("khaosnexus:item/crown_shard"));
    }

    public static final class RankTask extends AbstractBooleanTask {
        String rank = "C";
        public RankTask(long id, Quest q) { super(id, q); }
        @Override public TaskType getType() { return RANK; }
        @Override public int autoSubmitOnPlayerTick() { return 40; }
        @Override public boolean canSubmit(TeamData d, ServerPlayer p) {
            return HunterRanks.rankOf(p).ordinal() >= GateRank.valueOf(rank).ordinal();
        }
        @Override public void writeData(CompoundTag t, HolderLookup.Provider r) { super.writeData(t, r); t.putString("rank", rank); }
        @Override public void readData(CompoundTag t, HolderLookup.Provider r) { super.readData(t, r); rank = sane(t.getString("rank")); }
        @Override public void writeNetData(RegistryFriendlyByteBuf b) { super.writeNetData(b); b.writeUtf(rank); }
        @Override public void readNetData(RegistryFriendlyByteBuf b) { super.readNetData(b); rank = sane(b.readUtf()); }
        @Override public void fillConfigGroup(ConfigGroup c) { super.fillConfigGroup(c); c.addString("rank", rank, v -> rank = sane(v), "C"); }
        @Override public MutableComponent getAltTitle() { return Component.literal("Reach Hunter Rank " + rank); }
        static String sane(String s) {
            try { return GateRank.valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)).name(); } catch (RuntimeException e) { return "C"; }
        }
    }

    public static final class OathTask extends AbstractBooleanTask {
        String oath = "";
        public OathTask(long id, Quest q) { super(id, q); }
        @Override public TaskType getType() { return OATH; }
        @Override public int autoSubmitOnPlayerTick() { return 40; }
        @Override public boolean canSubmit(TeamData d, ServerPlayer p) {
            String sworn = Saga.oath(HunterRanks.profile(p));
            return sworn != null && !sworn.isEmpty() && (oath.isEmpty() || sworn.endsWith(oath));
        }
        @Override public void writeData(CompoundTag t, HolderLookup.Provider r) { super.writeData(t, r); t.putString("oath", oath); }
        @Override public void readData(CompoundTag t, HolderLookup.Provider r) { super.readData(t, r); oath = t.getString("oath"); }
        @Override public void writeNetData(RegistryFriendlyByteBuf b) { super.writeNetData(b); b.writeUtf(oath); }
        @Override public void readNetData(RegistryFriendlyByteBuf b) { super.readNetData(b); oath = b.readUtf(); }
        @Override public void fillConfigGroup(ConfigGroup c) { super.fillConfigGroup(c); c.addString("oath", oath, v -> oath = v, ""); }
        @Override public MutableComponent getAltTitle() { return Component.literal(oath.isEmpty() ? "Swear an Oath" : "Swear the Oath: " + oath); }
    }

    public static final class ValueReward extends Reward {
        final boolean xp;
        int amount;
        public ValueReward(long id, Quest q, boolean xp) { super(id, q); this.xp = xp; this.amount = xp ? 100 : 1; }
        @Override public RewardType getType() { return xp ? XP : SHARDS; }
        @Override public void claim(ServerPlayer p, boolean notify) {
            if (xp) HunterRanks.addXp(p, amount);
            else { Saga.addShards(HunterRanks.profile(p), amount); }
        }
        @Override public void writeData(CompoundTag t, HolderLookup.Provider r) { super.writeData(t, r); t.putInt("amount", amount); }
        @Override public void readData(CompoundTag t, HolderLookup.Provider r) { super.readData(t, r); amount = Math.max(1, t.getInt("amount")); }
        @Override public void writeNetData(RegistryFriendlyByteBuf b) { super.writeNetData(b); b.writeVarInt(amount); }
        @Override public void readNetData(RegistryFriendlyByteBuf b) { super.readNetData(b); amount = b.readVarInt(); }
        @Override public void fillConfigGroup(ConfigGroup c) { super.fillConfigGroup(c); c.addInt("amount", amount, v -> amount = v, xp ? 100 : 1, 1, 100000); }
        @Override public MutableComponent getAltTitle() { return Component.literal("+" + amount + (xp ? " Hunter XP" : " Crown Shards")); }
    }
}

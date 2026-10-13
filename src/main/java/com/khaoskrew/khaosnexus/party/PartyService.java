package com.khaoskrew.khaosnexus.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PartyService {
    private PartyService() {}

    public static int size(ServerPlayer player) {
        return data(player).sizeOf(player.getUUID());
    }

    public static List<ServerPlayer> onlineMembers(ServerPlayer player) {
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : data(player).membersOf(player.getUUID())) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(id);
            if (member != null) {
                members.add(member);
            }
        }
        if (members.isEmpty()) {
            members.add(player);
        }
        return members;
    }

    public static void create(ServerPlayer player) {
        PartyData data = data(player);
        if (data.leader(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("khaosnexus.party.already"), false);
            return;
        }
        data.create(player.getUUID());
        refresh(player);
        player.displayClientMessage(Component.translatable("khaosnexus.party.created"), false);
    }

    public static void invite(ServerPlayer from, ServerPlayer to) {
        if (from == to) {
            return;
        }
        PartyData data = data(from);
        UUID leader = data.leader(from.getUUID());
        if (leader == null) {
            data.create(from.getUUID());
            leader = from.getUUID();
        }
        if (!leader.equals(from.getUUID())) {
            from.displayClientMessage(Component.translatable("khaosnexus.party.leader_only"), false);
            return;
        }
        if (data.sizeOf(leader) >= Config.RAID_MAX_PARTY.get()) {
            from.displayClientMessage(Component.translatable("khaosnexus.party.full", Config.RAID_MAX_PARTY.get()), false);
            return;
        }
        data.invite(to.getUUID(), leader);
        to.displayClientMessage(Component.translatable("khaosnexus.party.invited", from.getName()), false);
        from.displayClientMessage(Component.translatable("khaosnexus.party.invite_sent", to.getName()), false);
    }

    public static void accept(ServerPlayer player) {
        PartyData data = data(player);
        UUID leader = data.takeInvite(player.getUUID());
        if (leader == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.party.no_invite"), false);
            return;
        }
        if (data.sizeOf(leader) >= Config.RAID_MAX_PARTY.get()) {
            player.displayClientMessage(Component.translatable("khaosnexus.party.full", Config.RAID_MAX_PARTY.get()), false);
            return;
        }
        data.leave(player.getUUID());
        data.join(leader, player.getUUID());
        refresh(player);
        player.displayClientMessage(Component.translatable("khaosnexus.party.joined"), false);
    }

    public static void leave(ServerPlayer player) {
        data(player).leave(player.getUUID());
        refresh(player);
        player.displayClientMessage(Component.translatable("khaosnexus.party.left"), false);
    }

    public static void list(ServerPlayer player) {
        Set<UUID> members = data(player).membersOf(player.getUUID());
        if (members.isEmpty()) {
            player.displayClientMessage(Component.translatable("khaosnexus.party.solo"), false);
            return;
        }
        player.displayClientMessage(Component.translatable("khaosnexus.party.list", members.size(), Config.RAID_MAX_PARTY.get()), false);
    }

    public static void refresh(ServerPlayer any) {
        ServerLevel overworld = any.server.overworld();
        PartyData data = PartyData.get(overworld);
        for (ServerPlayer player : any.server.getPlayerList().getPlayers()) {
            HunterRanks.profile(player).partySize = data.sizeOf(player.getUUID());
            HunterRanks.sync(player);
        }
    }

    private static PartyData data(ServerPlayer player) {
        return PartyData.get(player.server.overworld());
    }

    public static final class PartyData extends SavedData {
        public static final String ID = KhaosNexus.MODID + "_parties";
        private final Map<UUID, UUID> leaderOf = new HashMap<>();
        private final Map<UUID, Set<UUID>> members = new HashMap<>();
        private final Map<UUID, UUID> invites = new HashMap<>();

        public static PartyData get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(new Factory<>(PartyData::new, PartyData::load), ID);
        }

        private static PartyData load(CompoundTag tag, HolderLookup.Provider registries) {
            PartyData data = new PartyData();
            ListTag parties = tag.getList("Parties", Tag.TAG_COMPOUND);
            for (int i = 0; i < parties.size(); i++) {
                CompoundTag party = parties.getCompound(i);
                UUID leader = UUID.fromString(party.getString("Leader"));
                Set<UUID> group = new HashSet<>();
                for (Tag member : party.getList("Members", Tag.TAG_STRING)) {
                    group.add(UUID.fromString(member.getAsString()));
                }
                group.add(leader);
                data.members.put(leader, group);
                for (UUID member : group) {
                    data.leaderOf.put(member, leader);
                }
            }
            return data;
        }

        public UUID leader(UUID player) {
            return this.leaderOf.get(player);
        }

        public int sizeOf(UUID player) {
            UUID leader = this.leaderOf.get(player);
            if (leader == null) {
                return 1;
            }
            return this.members.getOrDefault(leader, Set.of(player)).size();
        }

        public Set<UUID> membersOf(UUID player) {
            UUID leader = this.leaderOf.get(player);
            if (leader == null) {
                return Set.of(player);
            }
            return Set.copyOf(this.members.getOrDefault(leader, Set.of(player)));
        }

        public void create(UUID leader) {
            this.leave(leader);
            Set<UUID> group = new HashSet<>();
            group.add(leader);
            this.members.put(leader, group);
            this.leaderOf.put(leader, leader);
            this.setDirty();
        }

        public void invite(UUID invitee, UUID leader) {
            this.invites.put(invitee, leader);
            this.setDirty();
        }

        public UUID takeInvite(UUID invitee) {
            UUID leader = this.invites.remove(invitee);
            if (leader != null) {
                this.setDirty();
            }
            return leader;
        }

        public void join(UUID leader, UUID member) {
            Set<UUID> group = this.members.computeIfAbsent(leader, ignored -> new HashSet<>());
            group.add(leader);
            group.add(member);
            for (UUID id : group) {
                this.leaderOf.put(id, leader);
            }
            this.setDirty();
        }

        public void leave(UUID member) {
            UUID leader = this.leaderOf.remove(member);
            if (leader == null) {
                return;
            }
            Set<UUID> group = this.members.get(leader);
            if (group != null) {
                group.remove(member);
                if (group.isEmpty() || member.equals(leader)) {
                    for (UUID id : group) {
                        this.leaderOf.remove(id);
                    }
                    this.members.remove(leader);
                }
            }
            this.setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag parties = new ListTag();
            for (Map.Entry<UUID, Set<UUID>> entry : this.members.entrySet()) {
                CompoundTag party = new CompoundTag();
                party.putString("Leader", entry.getKey().toString());
                ListTag ids = new ListTag();
                for (UUID member : entry.getValue()) {
                    ids.add(StringTag.valueOf(member.toString()));
                }
                party.put("Members", ids);
                parties.add(party);
            }
            tag.put("Parties", parties);
            return tag;
        }
    }
}

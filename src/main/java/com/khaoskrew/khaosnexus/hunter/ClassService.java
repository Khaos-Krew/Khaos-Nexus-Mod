package com.khaoskrew.khaosnexus.hunter;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.title.Titles;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ClassService {
    private static final List<Holder<Attribute>> TRACKED = List.of(
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.MOVEMENT_SPEED,
            Attributes.MAX_HEALTH,
            Attributes.LUCK);

    private ClassService() {}

    public static boolean equip(ServerPlayer player, String id) {
        HunterClass hunterClass = HunterClass.byId(id);
        if (hunterClass == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.class.unknown", id), false);
            return false;
        }
        HunterProfile profile = HunterRanks.profile(player);
        if (hunterClass.id().equals(profile.classId)) {
            player.displayClientMessage(Component.translatable("khaosnexus.class.already", Component.translatable(hunterClass.translationKey())), false);
            return true;
        }
        long now = player.level().getGameTime();
        if (now < profile.swapReady && !profile.classId.isEmpty()) {
            long seconds = Math.max(1L, (profile.swapReady - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable("khaosnexus.class.cooldown", seconds), false);
            return false;
        }
        clearPassives(player);
        AttributeInstance instance = player.getAttribute(hunterClass.attribute());
        if (instance != null) {
            instance.addOrReplacePermanentModifier(hunterClass.modifier());
        }
        if (hunterClass.attribute() == Attributes.MAX_HEALTH) {
            player.setHealth(player.getMaxHealth());
        }
        profile.classId = hunterClass.id();
        profile.swapReady = now + Config.CLASS_SWAP_COOLDOWN.get();
        HunterRanks.sync(player);
        player.displayClientMessage(Component.translatable("khaosnexus.class.equipped", Component.translatable(hunterClass.translationKey())), false);
        Titles.unlock(player, Titles.CLASS_BOUND);
        return true;
    }

    public static void clearPassives(ServerPlayer player) {
        for (Holder<Attribute> attribute : TRACKED) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(HunterClass.MODIFIER_ID);
            }
        }
    }

    public static HunterClass equipped(ServerPlayer player) {
        return HunterClass.byId(HunterRanks.profile(player).classId);
    }

    public static void reapply(ServerPlayer player) {
        HunterClass hunterClass = equipped(player);
        clearPassives(player);
        if (hunterClass == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(hunterClass.attribute());
        if (instance != null) {
            instance.addOrReplacePermanentModifier(hunterClass.modifier());
        }
    }
}

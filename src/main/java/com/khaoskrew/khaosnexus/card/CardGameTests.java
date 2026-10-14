package com.khaoskrew.khaosnexus.card;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KhaosNexus.MODID)
@PrefixGameTestTemplate(false)
public class CardGameTests {
    @SuppressWarnings("removal")
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void cardUnlocksAndSticks(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        boolean required = Config.HUNTER_CARD_REQUIRED.get();
        try {
            Config.HUNTER_CARD_REQUIRED.set(true);
            helper.assertFalse(HunterCard.isUnlocked(player), "a new player starts locked");
            ItemStack card = new ItemStack(HunterCard.CARD.get());
            HunterCardItem.bind(card, player);
            helper.assertTrue(player.getUUID().equals(HunterCardItem.owner(card)), "card binds to its holder");
            HunterCard.unlock(player);
            helper.assertTrue(HunterCard.isUnlocked(player), "card unlocks the hunter UI");
            card.setCount(0);
            helper.assertTrue(HunterCard.isUnlocked(player), "losing the card keeps the unlock");
            HunterProfile old = new HunterProfile();
            CompoundTag legacy = new CompoundTag();
            legacy.putInt("Xp", 50);
            old.deserializeNBT(player.registryAccess(), legacy);
            helper.assertTrue(old.cardUnlocked, "existing hunter progress unlocks automatically");
            player.getData(ModAttachments.HUNTER).cardUnlocked = false;
            Config.HUNTER_CARD_REQUIRED.set(false);
            helper.assertTrue(HunterCard.isUnlocked(player), "config off removes the card requirement");
        } finally {
            Config.HUNTER_CARD_REQUIRED.set(required);
        }
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(templateNamespace = KhaosNexus.MODID, template = "gate_platform")
    public static void starterCardOnce(GameTestHelper helper) {
        boolean required = Config.HUNTER_CARD_REQUIRED.get();
        boolean starter = Config.STARTER_CARD.get();
        try {
            Config.HUNTER_CARD_REQUIRED.set(true);
            Config.STARTER_CARD.set(true);
            ServerPlayer player = helper.makeMockServerPlayerInLevel(); // joining runs the login handler
            HunterCard.welcome(player);
            helper.assertTrue(player.getInventory().countItem(HunterCard.CARD.get()) == 1, "first join gives one card");
            HunterCard.welcome(player);
            helper.assertTrue(player.getInventory().countItem(HunterCard.CARD.get()) == 1, "second join gives nothing");
            for (var id : HunterCard.RECIPES) {
                helper.assertTrue(player.getRecipeBook().contains(id), "recipe unlocked: " + id);
                helper.assertTrue(player.server.getRecipeManager().byKey(id).isPresent(), "recipe exists: " + id);
            }
            Config.STARTER_CARD.set(false);
            ServerPlayer other = helper.makeMockServerPlayerInLevel();
            HunterCard.welcome(other);
            helper.assertTrue(other.getInventory().countItem(HunterCard.CARD.get()) == 0, "config off: no starter card");
        } finally {
            Config.HUNTER_CARD_REQUIRED.set(required);
            Config.STARTER_CARD.set(starter);
        }
        helper.succeed();
    }
}

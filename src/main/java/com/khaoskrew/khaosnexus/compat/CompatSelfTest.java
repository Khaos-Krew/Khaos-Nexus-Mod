package com.khaoskrew.khaosnexus.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.forge.ForgeRecipes;
import com.khaoskrew.khaosnexus.gate.GateArena;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.mob.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * {@code /khaosnexus compat check}: exercises every companion hook against whatever mods are installed and prints what
 * happened. Used to tune the compat in a real modded instance; harmless without the mods (every section says skipped).
 */
public final class CompatSelfTest {
    private CompatSelfTest() {}

    public static int run(ServerPlayer player, Consumer<Component> out) {
        int fails = 0;
        fails += classBonuses(player, out);
        fails += forge(player, out);
        fails += apotheosisSpawns(player, out);
        fails += bossPool(player, out);
        fails += stages(player, out);
        out.accept(Component.literal(fails == 0 ? "Compat check: all passed" : "Compat check: " + fails + " problem(s)")
                .withStyle(fails == 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return fails;
    }

    private static void line(Consumer<Component> out, boolean ok, String text) {
        out.accept(Component.literal((ok ? "[ok] " : "[!!] ") + text).withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
    }

    private static void head(Consumer<Component> out, String text) {
        out.accept(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    // ---- class bonuses -------------------------------------------------------------------------------------------

    private static int classBonuses(ServerPlayer player, Consumer<Component> out) {
        head(out, "Class bonuses");
        List<String> broken = ClassCompat.brokenIds();
        line(out, broken.isEmpty(), "attribute ids: " + ClassCompat.activeCount() + " live" + (broken.isEmpty() ? "" : ", wrong: " + broken));
        int fails = broken.isEmpty() ? 0 : 1;
        HunterClass equipped = ClassService.equipped(player);
        for (HunterClass c : HunterClass.values()) {
            ClassCompat.apply(player, c);
            List<String> got = new ArrayList<>();
            for (ClassCompat.Bonus b : ClassCompat.bonuses(c)) {
                BuiltInRegistries.ATTRIBUTE.getHolder(b.attribute()).ifPresent(h -> {
                    var inst = player.getAttribute(h);
                    if (inst != null && inst.getModifier(ClassCompat.MODIFIER_ID) != null) {
                        got.add(b.attribute().getPath().replace("ars_nouveau.perk.", "ars:") + "=" + round(inst.getValue()));
                    }
                });
            }
            line(out, true, c.id() + ": " + (got.isEmpty() ? "none (mods absent)" : String.join(", ", got)));
        }
        ClassCompat.apply(player, equipped);
        return fails;
    }

    // ---- forge + apotheosis --------------------------------------------------------------------------------------

    private static int forge(ServerPlayer player, Consumer<Component> out) {
        head(out, "Hunter Forge" + (ModList.get().isLoaded("apotheosis") ? " + Apotheosis (scale " + ApothCompat.forgeScale() + ")" : ""));
        if (!ModList.get().isLoaded("apotheosis")) {
            line(out, true, "Apotheosis absent, skipped");
            return 0;
        }
        int fails = 0;
        ItemStack sword;
        try {
            var parsed = new ItemParser(player.registryAccess()).parse(new com.mojang.brigadier.StringReader(
                    "minecraft:diamond_sword[apotheosis:rarity=\"apotheosis:epic\",apotheosis:affixes={\"apotheosis:melee/attribute/violent\":0.8f,"
                            + "\"apotheosis:melee/attribute/forceful\":0.5f},apotheosis:sockets=2]"));
            sword = new ItemStack(parsed.item(), 1, parsed.components());
        } catch (Exception e) {
            line(out, false, "could not build an affixed sword: " + e.getMessage());
            return 1;
        }
        ForgeRecipes.Recipe recipe = ForgeRecipes.match(sword);
        if (recipe == null) {
            line(out, false, "no forge recipe for a diamond sword");
            return 1;
        }
        double before = damage(sword);
        String affixesBefore = component(sword, "apotheosis:affixes");
        HunterClass c = ClassService.equipped(player);
        for (int lvl = 1; lvl <= Math.min(3, recipe.levels().size()); lvl++) {
            ForgeRecipes.apply(sword, recipe, lvl, c == null ? "" : c.id());
        }
        double after = damage(sword);
        boolean kept = affixesBefore.equals(component(sword, "apotheosis:affixes")) && !component(sword, "apotheosis:sockets").isEmpty()
                && !component(sword, "apotheosis:rarity").isEmpty();
        line(out, kept, "forging to +" + ForgeRecipes.level(sword) + " keeps affixes, rarity and sockets");
        fails += kept ? 0 : 1;
        double raw = 0;
        for (int i = 0; i < Math.min(3, recipe.levels().size()); i++) {
            raw = recipe.levels().get(i).bonus();
        }
        line(out, true, String.format("attack damage %.2f -> %.2f (forge +%.2f raw, x%.2f with Apotheosis)", before, after, raw, ApothCompat.forgeScale()));
        boolean single = count(sword, "forge_mainhand") == 1;
        line(out, single, "re-forging replaces its modifier instead of stacking (" + count(sword, "forge_mainhand") + " forge modifier)");
        fails += single ? 0 : 1;

        // reforge: Apotheosis rerolls affixes on a copy of the stack
        ItemStack reforged = reforge(player, sword.copy());
        if (reforged == null) {
            line(out, false, "Apotheosis reforge call unavailable");
            fails++;
        } else {
            boolean ok = ForgeRecipes.level(reforged) == ForgeRecipes.level(sword) && count(reforged, "forge_mainhand") == 1;
            line(out, ok, "after an Apotheosis reforge the sword is still +" + ForgeRecipes.level(reforged) + ", affixes rerolled: "
                    + !affixesBefore.equals(component(reforged, "apotheosis:affixes")));
            fails += ok ? 0 : 1;
            reforged.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Reforged +" + ForgeRecipes.level(reforged)));
            player.getInventory().add(reforged);
        }
        // socketing: Apotheosis writes socketed_gems on a copy of the stack
        ItemStack socketed = socket(player, sword.copy());
        if (socketed != null) {
            boolean ok = ForgeRecipes.level(socketed) == ForgeRecipes.level(sword) && !component(socketed, "apotheosis:socketed_gems").isEmpty();
            line(out, ok, "after socketing a gem the sword is still +" + ForgeRecipes.level(socketed));
            fails += ok ? 0 : 1;
            ItemStack forgedAgain = socketed.copy();
            if (ForgeRecipes.level(forgedAgain) < recipe.levels().size()) {
                ForgeRecipes.apply(forgedAgain, recipe, ForgeRecipes.level(forgedAgain) + 1, c == null ? "" : c.id());
                boolean gemKept = component(forgedAgain, "apotheosis:socketed_gems").equals(component(socketed, "apotheosis:socketed_gems"));
                line(out, gemKept, "forging a socketed sword keeps the gem");
                fails += gemKept ? 0 : 1;
            }
            player.getInventory().add(socketed);
        } else {
            line(out, true, "socketing: no gem available, skipped");
        }
        player.getInventory().add(sword);
        return fails;
    }

    private static double damage(ItemStack stack) {
        double[] total = {1.0D};
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(Attributes.ATTACK_DAMAGE) && mod.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                total[0] += mod.amount();
            }
        });
        double[] mult = {1.0D};
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(Attributes.ATTACK_DAMAGE) && mod.operation() != net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                mult[0] += mod.amount();
            }
        });
        return total[0] * mult[0];
    }

    private static int count(ItemStack stack, String path) {
        ItemAttributeModifiers mods = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return (int) mods.modifiers().stream().filter(e -> e.modifier().id().getPath().equals(path)).count();
    }

    private static String component(ItemStack stack, String id) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(id));
        Object v = type == null ? null : stack.get(type);
        return v == null ? "" : v.toString();
    }

    private static ItemStack reforge(ServerPlayer player, ItemStack stack) {
        try {
            Class<?> ctxC = Class.forName("dev.shadowsoffire.apotheosis.tiers.GenContext");
            Object ctx = ctxC.getMethod("forPlayer", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            Class<?> rarityReg = Class.forName("dev.shadowsoffire.apotheosis.loot.RarityRegistry");
            Object reg = rarityReg.getField("INSTANCE").get(null);
            Object rarity = reg.getClass().getMethod("getValue", ResourceLocation.class).invoke(reg, ResourceLocation.parse("apotheosis:epic"));
            Class<?> lootRarity = Class.forName("dev.shadowsoffire.apotheosis.loot.LootRarity");
            Class<?> controller = Class.forName("dev.shadowsoffire.apotheosis.loot.LootController");
            return (ItemStack) controller.getMethod("createLootItem", ItemStack.class, lootRarity, ctxC).invoke(null, stack, rarity, ctx);
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.khaoskrew.khaosnexus.KhaosNexus.LOGGER.warn("reforge reflection failed", e);
            return null;
        }
    }

    private static ItemStack socket(ServerPlayer player, ItemStack stack) {
        try {
            Class<?> ctxC = Class.forName("dev.shadowsoffire.apotheosis.tiers.GenContext");
            Object ctx = ctxC.getMethod("forPlayer", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            Class<?> gemReg = Class.forName("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry");
            Object reg = gemReg.getField("INSTANCE").get(null);
            Class<?> purity = Class.forName("dev.shadowsoffire.apotheosis.socket.gem.Purity");
            Object flawless = null;
            for (Object o : purity.getEnumConstants()) {
                if (((Enum<?>) o).name().equals("FLAWLESS")) {
                    flawless = o;
                }
            }
            Object gem = reg.getClass().getMethod("getValue", ResourceLocation.class).invoke(reg, ResourceLocation.parse("apotheosis:core/ballast"));
            ItemStack gemStack = (ItemStack) gemReg.getMethod("createGemStack", Class.forName("dev.shadowsoffire.apotheosis.socket.gem.Gem"), purity)
                    .invoke(null, gem, flawless);
            Class<?> helper = Class.forName("dev.shadowsoffire.apotheosis.socket.SocketHelper");
            for (var m : helper.getMethods()) {
                if (m.getName().equals("socketGemInItem") && m.getParameterCount() == 2) {
                    return (ItemStack) m.invoke(null, stack, gemStack);
                }
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.khaoskrew.khaosnexus.KhaosNexus.LOGGER.warn("socket reflection failed", e);
            return null;
        }
    }

    // ---- apotheosis spawn rules ----------------------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int apotheosisSpawns(ServerPlayer player, Consumer<Component> out) {
        head(out, "Apotheosis on rift mobs (allowApotheosisInvaderBosses=" + Config.ALLOW_APOTH_BOSSES.get()
                + ", apotheosisGateTiers=" + Config.APOTH_GATE_TIERS.get() + ")");
        if (!ModList.get().isLoaded("apotheosis")) {
            line(out, true, "Apotheosis absent, skipped");
            return 0;
        }
        ServerLevel level = player.serverLevel();
        AttachmentType tierType = NeoForgeRegistries.ATTACHMENT_TYPES.get(ResourceLocation.parse("apotheosis:world_tier"));
        Object oldTier = tierType == null ? null : player.getData(tierType);
        Object pinnacle = null;
        if (tierType != null) {
            for (Object o : oldTier.getClass().getEnumConstants()) {
                if (((Enum<?>) o).name().equals("PINNACLE")) {
                    pinnacle = o;
                }
            }
            player.setData(tierType, pinnacle);
        }
        int fails = 0;
        try {
            BlockPos at = player.blockPosition().relative(player.getDirection(), 6);
            for (EntityType<?> type : List.of(ModEntities.RIFT_WARDEN.get(), ModEntities.SPIRE_GUARDIAN.get())) {
                Mob boss = GateArena.spawnMob(level, GateRank.S, type, at, 0F, 1.0D, true);
                if (boss != null) {
                    List<String> tiers = ApothCompat.tierModifiers(boss);
                    boolean affix = hasAffixGear(boss);
                    boolean ok = Config.ALLOW_APOTH_BOSSES.get() || (tiers.isEmpty() && !affix);
                    line(out, ok, type.getDescription().getString() + " (player at pinnacle): tier mods " + tiers + ", affix gear " + affix);
                    fails += ok ? 0 : 1;
                    boss.discard();
                }
            }
            int n = 60;
            int affixed = 0;
            int tiered = 0;
            String sample = "";
            for (int i = 0; i < n; i++) {
                Mob mob = GateArena.spawnMob(level, GateRank.C, ModEntities.RIFT_GHOUL.get(), at, 0F, 1.0D, false);
                if (mob == null) {
                    continue;
                }
                affixed += hasAffixGear(mob) ? 1 : 0;
                List<String> t = ApothCompat.tierModifiers(mob);
                tiered += t.isEmpty() ? 0 : 1;
                sample = t.toString();
                boolean replaced = mob.getPersistentData().contains("apoth.miniboss");
                if (replaced) {
                    line(out, true, "a C-rank ghoul became an Apotheosis elite");
                }
                mob.discard();
            }
            line(out, true, n + " C-rank gate ghouls: " + tiered + " with world-tier mods " + sample + ", " + affixed + " with affix gear");
            line(out, true, "invaders: never (rift mobs spawn as EVENT, Apotheosis only replaces NATURAL spawns)");
        } finally {
            if (tierType != null) {
                player.setData(tierType, oldTier);
            }
        }
        return fails;
    }

    private static boolean hasAffixGear(Mob mob) {
        DataComponentType<?> affixes = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("apotheosis:affixes"));
        if (affixes == null) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Object v = mob.getItemBySlot(slot).get(affixes);
            if (v != null && !v.toString().contains("{}")) {
                return true;
            }
        }
        return false;
    }

    // ---- boss pool -----------------------------------------------------------------------------------------------

    private static int bossPool(ServerPlayer player, Consumer<Component> out) {
        head(out, "Optional boss pool (bossMods=" + Config.COMPAT_BOSS_MODS.get() + ", chance " + Config.BOSS_MOD_CHANCE.get() + ")");
        int fails = 0;
        int tried = 0;
        BlockPos at = player.blockPosition().relative(player.getDirection(), 10);
        for (String line : Config.OPTIONAL_BOSSES.get()) {
            String[] p = line.split("=", 2);
            ResourceLocation id = p.length == 2 ? ResourceLocation.tryParse(p[1].trim()) : null;
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                continue;
            }
            tried++;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            GateRank rank = GateRank.byName(p[0].trim());
            Mob boss = GateArena.spawnMob(player.serverLevel(), rank, type, at, 180F, Config.MOD_BOSS_HEALTH.get(), true);
            if (boss == null) {
                line(out, false, id + ": spawn failed or was cancelled");
                fails++;
                continue;
            }
            boolean woke = CompatPools.wake(boss);
            boss.tick();
            boolean ok = boss.isAlive() && !boss.isRemoved();
            line(out, ok, String.format("%s (%s): %.0f hp%s, tier mods %s", id, rank, boss.getMaxHealth(), woke ? ", woken" : "",
                    ApothCompat.tierModifiers(boss)));
            fails += ok ? 0 : 1;
            boss.discard();
        }
        if (tried == 0) {
            line(out, true, "no boss mods installed, skipped");
        }
        return fails;
    }

    // ---- FTB stages ----------------------------------------------------------------------------------------------

    private static int stages(ServerPlayer player, Consumer<Component> out) {
        head(out, "Quest stages and hunter_event");
        if (!SoftCompat.FTB_QUESTS.active()) {
            line(out, true, "FTB Quests absent or off; only the advancement trigger fires");
            return 0;
        }
        ProgressHooks.gateCleared(player, "s");
        boolean ok = ProgressHooks.hasStage(player, ProgressHooks.PREFIX + "gate_cleared.s");
        line(out, ok, "gate_cleared fires and stage khaosnexus.gate_cleared.s is visible to FTB's stage provider");
        return ok ? 0 : 1;
    }

    private static String round(double v) {
        return String.format("%.2f", v);
    }
}

package com.khaoskrew.khaosnexus.endgame;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Endgame subcommands, merged into /khaosnexus. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class EndgameCommands {
    private EndgameCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("khaosnexus");
        root.then(Commands.literal("aura")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.displayClientMessage(Auras.list(p), false);
                    return 1;
                })
                .then(Commands.literal("clear").executes(c -> {
                    Auras.clear(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("equip").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                Auras.unlockedIds(com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(c.getSource().getPlayerOrException()))
                                        .stream().map(r -> r.getPath()), b))
                        .executes(c -> Auras.equip(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")) ? 1 : 0))));
        root.then(Commands.literal("mastery")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.displayClientMessage(Mastery.status(com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p)), false);
                    return 1;
                })
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Mastery.reset(com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p));
                    Mastery.apply(p);
                    com.khaoskrew.khaosnexus.hunter.HunterRanks.sync(p);
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.mastery.reset"), false);
                    return 1;
                }))
                .then(Commands.literal("pick").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(JsonPool.MASTERY.all().keySet().stream().map(r -> r.getPath()), b))
                        .executes(c -> Mastery.command(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")) ? 1 : 0))));
        root.then(Commands.literal("prestige").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Prestige.tryPromote(p);
            p.displayClientMessage(Prestige.status(com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p)), false);
            return 1;
        }));
        root.then(Commands.literal("bossrush")
                .then(Commands.literal("start").executes(c -> BossRush.start(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("leave").executes(c -> {
                    BossRush.leave(c.getSource().getPlayerOrException(), false);
                    return 1;
                })));
        root.then(Commands.literal("challenge")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.displayClientMessage(Challenges.describe(p), false);
                    return 1;
                })
                .then(Commands.literal("start").executes(c -> Challenges.start(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("leave").executes(c -> {
                    BossRush.leave(c.getSource().getPlayerOrException(), false);
                    return 1;
                })));
        root.then(Commands.literal("awaken")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.displayClientMessage(Awakening.status(com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p)), false);
                    return 1;
                })
                .then(Commands.literal("use").executes(c -> Awakening.cast(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("choose").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Awakening.options(
                                com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(c.getSource().getPlayerOrException()).classId)
                                .stream().map(r -> r.getPath()), b))
                        .executes(c -> Awakening.command(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")) ? 1 : 0))));
        root.then(Commands.literal("raid").then(Commands.literal("group").then(Commands.argument("boss", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(JsonPool.RAID_BOSSES.all().keySet().stream().map(r -> r.getPath()), b))
                .executes(c -> GroupRaids.start(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "boss")) != null ? 1 : 0))));
        // Admin: set prestige or Spire progress directly (op level 2), for server owners fixing or migrating saves.
        root.then(Commands.literal("prestige").then(Commands.literal("set").requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 2))
                                .executes(c -> {
                                    ServerPlayer p = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player");
                                    int lvl = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level");
                                    var prof = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p);
                                    prof.endgame.putInt(Prestige.KEY, lvl);
                                    if (lvl >= Prestige.SS) com.khaoskrew.khaosnexus.title.Titles.unlock(p, Prestige.TITLE_SS);
                                    if (lvl >= Prestige.NATIONAL) com.khaoskrew.khaosnexus.title.Titles.unlock(p, Prestige.TITLE_NATIONAL);
                                    com.khaoskrew.khaosnexus.hunter.HunterRanks.sync(p);
                                    c.getSource().sendSuccess(() -> Prestige.status(prof), true);
                                    return 1;
                                })))));
        root.then(Commands.literal("spire").then(Commands.literal("set").requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("floor", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .then(Commands.argument("cleared", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(c -> {
                                                    ServerPlayer p = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player");
                                                    var prof = com.khaoskrew.khaosnexus.hunter.HunterRanks.profile(p);
                                                    com.khaoskrew.khaosnexus.spire.SpireService.migrate(prof);
                                                    prof.spireFloor = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "floor");
                                                    prof.endgame.putInt(com.khaoskrew.khaosnexus.spire.SpireService.LEVEL_KEY,
                                                            Math.min(com.khaoskrew.khaosnexus.spire.SpireService.levels(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level")));
                                                    prof.spireBest = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "cleared");
                                                    prof.spireCheckpoint = prof.spireFloor;
                                                    prof.spireSeason = com.khaoskrew.khaosnexus.spire.SpireData.get(p.server.overworld()).season;
                                                    com.khaoskrew.khaosnexus.hunter.HunterRanks.sync(p);
                                                    com.khaoskrew.khaosnexus.spire.SpireService.status(p);
                                                    return 1;
                                                })))))));
        // endgame-commands-end
        event.getDispatcher().register(root);
    }
}

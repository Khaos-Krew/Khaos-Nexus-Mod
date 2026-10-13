package com.khaoskrew.khaosnexus.command;

import java.util.Locale;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.compat.SoftCompat;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.bounty.Bounties;
import com.khaoskrew.khaosnexus.party.PartyService;
import com.khaoskrew.khaosnexus.raid.RaidService;
import com.khaoskrew.khaosnexus.spire.SpireService;
import com.khaoskrew.khaosnexus.title.Titles;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class KhaosCommands {
    private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS = (context, builder) -> {
        for (GateRank rank : GateRank.values()) {
            builder.suggest(rank.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };

    private KhaosCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("khaosnexus")
                .then(Commands.literal("rank")
                        .executes(context -> showRank(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests(RANK_SUGGESTIONS)
                                                .executes(context -> setRank(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "rank"))))))
                        .then(Commands.literal("addxp")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    HunterRanks.addXp(player, amount);
                                                    context.getSource().sendSuccess(() -> Component.translatable(
                                                            "khaosnexus.command.rank_set",
                                                            player.getName(),
                                                            Component.translatable(HunterRanks.rankOf(player).translationKey()),
                                                            HunterRanks.profile(player).xp), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("class")
                        .executes(context -> showClass(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("equip")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (HunterClass hunterClass : HunterClass.values()) {
                                                builder.suggest(hunterClass.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            return ClassService.equip(player, StringArgumentType.getString(context, "id")) ? 1 : 0;
                                        }))))
                .then(Commands.literal("bounty")
                        .executes(context -> {
                            Bounties.describe(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("party")
                        .executes(context -> {
                            PartyService.list(context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("create").executes(context -> {
                            PartyService.create(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            PartyService.invite(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player"));
                                            return 1;
                                        })))
                        .then(Commands.literal("accept").executes(context -> {
                            PartyService.accept(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("leave").executes(context -> {
                            PartyService.leave(context.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("preview").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("rank", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    com.khaoskrew.khaosnexus.gate.GateRank rank = com.khaoskrew.khaosnexus.gate.GateRank.byName(
                                            com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rank").split(" ")[0].toUpperCase(Locale.ROOT));
                                    var dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(player.server);
                                    if (dungeon == null) {
                                        return 0;
                                    }
                                    var lease = com.khaoskrew.khaosnexus.dungeon.DungeonService.allocate(dungeon);
                                    String[] words = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rank").split(" ");
                                    ResourceLocation themeId = null;
                                    for (int w = 1; w < words.length; w++) {
                                        if (!words[w].equals("cutaway")) {
                                            themeId = ResourceLocation.parse(words[w].contains(":") ? words[w] : "khaosnexus:" + words[w]);
                                        }
                                    }
                                    var layout = com.khaoskrew.khaosnexus.dungeon.DungeonService.build(dungeon, rank, lease.origin(), player.getRandom(), themeId);
                                    if (layout == null) {
                                        return 0;
                                    }
                                    var spawnedBoss = com.khaoskrew.khaosnexus.dungeon.DungeonService.populate(dungeon, rank, layout, player.getRandom(), lease);
                                    if (context.getInput().endsWith(" cutaway")) {
                                        com.khaoskrew.khaosnexus.dungeon.DungeonService.cutaway(dungeon, layout.box());
                                        var b0 = layout.box();
                                        double span = Math.max(b0.getXSpan(), b0.getZSpan());
                                        player.teleportTo(dungeon, (b0.minX() + b0.maxX()) / 2.0D, b0.maxY() + span * 0.5D + 10.0D, (b0.minZ() + b0.maxZ()) / 2.0D, 180.0F, 90.0F);
                                    } else {
                                        com.khaoskrew.khaosnexus.dungeon.DungeonService.sendIn(player, dungeon, layout.spawn(), layout.yaw(), player.level(), player.blockPosition());
                                    }
                                    var b = layout.box();
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                            "Dungeon " + rank + ": " + layout.pieces() + " pieces, box " + b.minX() + " " + b.minZ() + " to " + b.maxX() + " " + b.maxZ()
                                            + ", boss " + (layout.boss() == null ? "none" : layout.boss().toShortString())
                                            + (spawnedBoss == null ? "" : " " + net.minecraft.world.entity.EntityType.getKey(spawnedBoss.getType()))), false);
                                    return 1;
                                })))
                        .then(Commands.literal("instances").requires(source -> source.hasPermission(2)).executes(context -> {
                            var dungeon = com.khaoskrew.khaosnexus.dungeon.DungeonService.level(context.getSource().getServer());
                            if (dungeon == null) {
                                return 0;
                            }
                            var d = com.khaoskrew.khaosnexus.dungeon.DungeonInstances.get(dungeon);
                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "Dungeon instances: " + d.liveCount() + " live, " + d.pendingResets() + " wiping, "
                                    + d.freeCount() + " free, " + d.highWater() + " regions ever used"), false);
                            return d.liveCount();
                        }))
                        .then(Commands.literal("leave").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (player.level().dimension() == com.khaoskrew.khaosnexus.dungeon.DungeonRules.GATE_DUNGEON
                                    && com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity.lockedIn(player)) {
                                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.dungeon.red_locked"), false);
                                return 0;
                            }
                            if (player.level().dimension() != com.khaoskrew.khaosnexus.dungeon.DungeonRules.GATE_DUNGEON
                                    || !com.khaoskrew.khaosnexus.dungeon.DungeonService.sendBack(player)) {
                                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.dungeon.not_inside"), false);
                                return 0;
                            }
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("khaosnexus.dungeon.left"), false);
                            return 1;
                        })))
                .then(Commands.literal("top")
                        .then(Commands.argument("board", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.List.of("E", "D", "C", "B", "A", "S", "spire", "weekly", "rush"), b))
                                .executes(context -> {
                                    String arg = StringArgumentType.getString(context, "board");
                                    var server = context.getSource().getServer();
                                    String board = switch (arg.toLowerCase(Locale.ROOT)) {
                                        case "spire" -> com.khaoskrew.khaosnexus.endgame.Leaderboards.spireBoard(
                                                com.khaoskrew.khaosnexus.spire.SpireData.get(server.overworld()).season);
                                        case "rush" -> com.khaoskrew.khaosnexus.endgame.Leaderboards.rushBoard();
                                        case "weekly" -> com.khaoskrew.khaosnexus.endgame.Leaderboards.weeklyBoard(
                                                com.khaoskrew.khaosnexus.endgame.WeeklyModifiers.currentWeek());
                                        default -> com.khaoskrew.khaosnexus.endgame.Leaderboards.clearBoard(
                                                com.khaoskrew.khaosnexus.gate.GateRank.byName(arg.toUpperCase(Locale.ROOT)));
                                    };
                                    var top = com.khaoskrew.khaosnexus.endgame.Leaderboards.get(server)
                                            .top(board, com.khaoskrew.khaosnexus.Config.LEADERBOARD_SIZE.get());
                                    context.getSource().sendSuccess(() -> com.khaoskrew.khaosnexus.endgame.Leaderboards.format(board, top), false);
                                    return top.size();
                                })))
                .then(Commands.literal("weekly").executes(context -> {
                    context.getSource().sendSuccess(com.khaoskrew.khaosnexus.endgame.WeeklyModifiers::describe, false);
                    return 1;
                }))
                .then(Commands.literal("raid")
                        .then(Commands.literal("start").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return RaidService.start(player, player.blockPosition()) ? 1 : 0;
                        })))
                .then(Commands.literal("spire")
                        .executes(context -> {
                            SpireService.status(context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("enter").executes(context -> {
                            SpireService.enter(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("leave").executes(context -> {
                            SpireService.leave(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("checkpoint").executes(context -> {
                            SpireService.checkpoint(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("newseason").requires(source -> source.hasPermission(2)).executes(context -> {
                            SpireService.newSeason(context.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("title")
                        .executes(context -> {
                            Titles.list(context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("equip")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> Titles.equip(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "id")) ? 1 : 0)))
                        .then(Commands.literal("clear").executes(context -> {
                            Titles.clear(context.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("compat")
                        .then(Commands.literal("check").requires(source -> source.hasPermission(2)).executes(context -> {
                            CommandSourceStack source = context.getSource();
                            return com.khaoskrew.khaosnexus.compat.CompatSelfTest.run(source.getPlayerOrException(),
                                    c -> source.sendSuccess(() -> c, false)) == 0 ? 1 : 0;
                        }))
                        .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    for (SoftCompat.Hook hook : SoftCompat.HOOKS) {
                        source.sendSuccess(() -> Component.translatable(
                                "khaosnexus.compat.line", hook.label(), hook.status()), false);
                    }
                    source.sendSuccess(() -> Component.translatable("khaosnexus.compat.note"), false);
                    return 1;
                })));
    }

    private static int showClass(ServerPlayer player) {
        HunterClass hunterClass = ClassService.equipped(player);
        if (hunterClass == null) {
            player.displayClientMessage(Component.translatable("khaosnexus.class.none"), false);
        } else {
            player.displayClientMessage(Component.translatable(
                    "khaosnexus.class.current",
                    Component.translatable(hunterClass.translationKey())), false);
        }
        return 1;
    }

    public static int showRank(ServerPlayer player) {
        HunterProfile profile = HunterRanks.profile(player);
        GateRank rank = HunterRanks.rankOf(player);
        player.displayClientMessage(Component.translatable(
                "khaosnexus.command.rank",
                Component.translatable(rank.translationKey()),
                profile.xp), false);
        return 1;
    }

    private static int setRank(CommandSourceStack source, ServerPlayer player, String name) {
        GateRank rank = GateRank.byName(name.toUpperCase(Locale.ROOT));
        if (!rank.name().equalsIgnoreCase(name)) {
            source.sendFailure(Component.translatable("khaosnexus.command.unknown_rank", name));
            return 0;
        }
        HunterRanks.setXp(player, HunterRanks.threshold(rank));
        source.sendSuccess(() -> Component.translatable(
                "khaosnexus.command.rank_set",
                player.getName(),
                Component.translatable(rank.translationKey()),
                HunterRanks.profile(player).xp), true);
        return 1;
    }
}

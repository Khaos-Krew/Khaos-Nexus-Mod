package com.khaoskrew.khaosnexus.command;

import java.util.Locale;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.ClassService;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                        })))));
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

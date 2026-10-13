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
        // endgame-commands-end
        event.getDispatcher().register(root);
    }
}

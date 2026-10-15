package com.khaoskrew.khaosnexus.card;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Hunter Card "Guild" tab: whitelisted button ids mapped to the player commands they stand for. A button runs its
 * command as the player, with the player's own permission level, so every command keeps its usual checks (admin
 * subcommands are never on the list). Ids outside the list are ignored. */
public final class GuildActions {
    public static final Map<String, String> COMMANDS = new LinkedHashMap<>();
    static {
        COMMANDS.put("spire_enter", "khaosnexus spire enter");
        COMMANDS.put("spire_leave", "khaosnexus spire leave");
        COMMANDS.put("spire_status", "khaosnexus spire");
        COMMANDS.put("spire_checkpoint", "khaosnexus spire checkpoint");
        COMMANDS.put("top", "khaosnexus top spire");
        COMMANDS.put("weekly", "khaosnexus weekly");
        COMMANDS.put("prestige", "khaosnexus prestige");
        COMMANDS.put("mastery", "khaosnexus mastery");
        COMMANDS.put("aura", "khaosnexus aura");
        COMMANDS.put("title", "khaosnexus title");
        COMMANDS.put("party", "khaosnexus party");
        COMMANDS.put("bounty", "khaosnexus bounty");
        COMMANDS.put("class", "khaosnexus class");
    }

    private GuildActions() {}

    /** Runs a Guild button for the player. Returns false for unknown ids, a locked card, or a command that is not registered. */
    public static boolean run(ServerPlayer player, String id) {
        String cmd = COMMANDS.get(id);
        if (cmd == null || !HunterCard.isUnlocked(player)) {
            return false;
        }
        var dispatcher = player.server.getCommands().getDispatcher();
        var source = player.createCommandSourceStack();
        if (dispatcher.parse(cmd, source).getReader().canRead()) {
            return false;   // not registered on this build
        }
        player.server.getCommands().performPrefixedCommand(source, cmd);
        return true;
    }
}

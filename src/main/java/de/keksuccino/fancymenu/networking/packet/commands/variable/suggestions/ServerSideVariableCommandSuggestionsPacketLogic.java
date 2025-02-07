package de.keksuccino.fancymenu.networking.packet.commands.variable.suggestions;

import de.keksuccino.fancymenu.commands.VariableCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerSideVariableCommandSuggestionsPacketLogic {

    private static final Logger LOGGER = Bukkit.getLogger();

    protected static boolean handle(Player player, VariableCommandSuggestionsPacket packet) {
        try {
            String uuid = player.getUniqueId().toString();
            VariableCommand.CACHED_VARIABLE_SUGGESTIONS.put(uuid, packet.variable_suggestions);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to process /fmvariable command suggestions packet!", ex);
        }
        return false;
    }

}

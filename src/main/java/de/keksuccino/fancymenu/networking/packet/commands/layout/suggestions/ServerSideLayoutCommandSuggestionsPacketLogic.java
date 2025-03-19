package de.keksuccino.fancymenu.networking.packet.commands.layout.suggestions;

import de.keksuccino.fancymenu.commands.LayoutCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerSideLayoutCommandSuggestionsPacketLogic {

    private static final Logger LOGGER = Bukkit.getLogger();

    protected static boolean handle(Player player, LayoutCommandSuggestionsPacket packet) {
        try {
            String uuid = player.getUniqueId().toString();
            LayoutCommand.CACHED_LAYOUT_SUGGESTIONS.put(uuid, packet.layout_suggestions);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to process /fmlayout command suggestions packet!", ex);
        }
        return false;
    }

}
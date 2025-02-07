package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packet.commands.opengui.OpenGuiCommandPacket;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.entity.Player;

import java.util.Collection;

public class OpenGuiScreenCommand {

    public static void register() {
        new CommandAPICommand("openguiscreen")
                .withArguments(new TextArgument("screen_identifier").replaceSuggestions(ArgumentSuggestions.strings("<screen_identifier>")))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("target_players").withPermission(CommandPermission.OP))
                .executes((sender, args) -> {
                    String screen_identifier = (String) args.get("screen_identifier");
                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args.get("target_players");
                    if (players != null) {
                        for (Player player : players) {
                            OpenGuiCommandPacket packet = new OpenGuiCommandPacket();
                            packet.screen_identifier = screen_identifier;
                            PacketHandler.sendToClient(player, packet);
                        }
                    } else if (sender instanceof Player player){
                        OpenGuiCommandPacket packet = new OpenGuiCommandPacket();
                        packet.screen_identifier = screen_identifier;
                        PacketHandler.sendToClient(player, packet);
                    }
                })
                .register();
    }

}

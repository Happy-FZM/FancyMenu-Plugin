package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packet.commands.closegui.CloseGuiCommandPacket;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CloseGuiScreenCommand {

    public static void register() {
        new CommandAPICommand("closeguiscreen")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("target_players").withPermission(CommandPermission.OP))
                .executes((sender, args) -> {
                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args.get("target_players");
                    if (players != null) {
                        for (Player player : players) {
                            CloseGuiCommandPacket closeGuiScreenCommand = new CloseGuiCommandPacket();
                            PacketHandler.sendToClient(player, closeGuiScreenCommand);
                        }
                    } else if (sender instanceof Player player) {
                        CloseGuiCommandPacket packet = new CloseGuiCommandPacket();
                        PacketHandler.sendToClient(player, packet);
                    }
                })
                .register();
    }

}

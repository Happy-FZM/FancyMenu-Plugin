package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packet.commands.layout.command.LayoutCommandPacket;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

public class LayoutCommand {

    public static final Map<String, List<String>> CACHED_LAYOUT_SUGGESTIONS = Collections.synchronizedMap(new HashMap<>());

    // fmlayout <layout_name> <enabled [true|false]> <target_players>
    public static void register() {

        List<Argument<?>> arguments = new ArrayList<>();
        arguments.add(new TextArgument("layout_name").replaceSuggestions(ArgumentSuggestions.stringCollection(info ->
                getVariableSuggestions(info.sender())))
        );

        new CommandAPICommand("fmlayout")
                .withArguments(arguments)
                .withArguments(new BooleanArgument("enabled"))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("target_players").withPermission(CommandPermission.OP))
                .executes((sender, args) -> {

                    String layout_name = (String) args.get("layout_name");
                    Boolean enabled = (Boolean) args.get("enabled");
                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args.get("target_players");

                    setLayoutState(sender, layout_name, Boolean.TRUE.equals(enabled), players);

                })
                .register();
    }

    private static List<String> getVariableSuggestions(CommandSender sender) {
        if (sender instanceof Player player) {
            List<String> l = new ArrayList<>(Objects.requireNonNullElse(CACHED_LAYOUT_SUGGESTIONS.get(player.getUniqueId().toString()), new ArrayList<>()));
            if (l.isEmpty()) {
                l.add("<no_layouts_found>");
            }
            return l;
        } else {
            return new ArrayList<>();
        }
    }

    private static void setLayoutState(CommandSender sender, String layoutName, boolean enabled, @Nullable Collection<Player> targets) {
        try {
            if (targets == null) {
                if (sender instanceof Player player) {
                    LayoutCommandPacket packet = new LayoutCommandPacket();
                    packet.layout_name = layoutName;
                    packet.enabled = enabled;
                    PacketHandler.sendToClient(player, packet);
                }
            } else {
                for (Player target : targets) {
                    LayoutCommandPacket packet = new LayoutCommandPacket();
                    packet.layout_name = layoutName;
                    packet.enabled = enabled;
                    PacketHandler.sendToClient(target, packet);
                }
            }
        } catch (Exception ex) {
            sender.sendMessage("§cError while executing /fmlayout command!");
            ex.printStackTrace();
        }
    }

}

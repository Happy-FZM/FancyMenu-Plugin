package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packet.commands.variable.command.VariableCommandPacket;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class VariableCommand {

    public static final Map<String, List<String>> CACHED_VARIABLE_SUGGESTIONS = Collections.synchronizedMap(new HashMap<>());

    // fmvariable <get_or_set> <variable_name> [<send_chat_feedback>] [<set_to_value>]
    // fmvariable get some_variable
    // fmvariable set some_variable new_value true
    public static void register() {

        List<Argument<?>> arguments = new ArrayList<>();
        arguments.add(new TextArgument("variable_name").replaceSuggestions(ArgumentSuggestions.stringCollection(info ->
                getVariableSuggestions(info.sender())))
        );

        new CommandAPICommand("fmvariable")
                .withArguments(new LiteralArgument("get"))
                .withArguments(arguments)
                .executesPlayer((sender, args) -> {
                    String variable_name = (String) args.get("variable_name");
                    getVariable(sender, variable_name);
                })
                .register();

        new CommandAPICommand("fmvariable")
                .withArguments(new LiteralArgument("set"))
                .withArguments(arguments)
                .withArguments(new BooleanArgument("send_chat_feedback"))
                .withArguments(new TextArgument("set_to_value").replaceSuggestions(ArgumentSuggestions.strings("<set_to_value>")))
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("target_players").withPermission(CommandPermission.OP))
                .executes((sender, args) -> {

                    String variable_name = (String) args.get("variable_name");
                    Boolean send_chat_feedback = (Boolean) args.get("send_chat_feedback");
                    String set_to_value = (String) args.get("set_to_value");

                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args.get("target_players");
                    if (players != null) {
                        for (Player player : players) {
                            setVariable(player, variable_name, set_to_value, Boolean.TRUE.equals(send_chat_feedback));
                        }
                    } else {
                        setVariable(sender, variable_name, set_to_value, Boolean.TRUE.equals(send_chat_feedback));
                    }

                })
                .register();

    }

    private static List<String> getVariableSuggestions(CommandSender sender) {
        if (sender instanceof Player player) {
            return CACHED_VARIABLE_SUGGESTIONS.get(player.getUniqueId().toString());
        } else {
            return new ArrayList<>();
        }
    }

    private static void getVariable(CommandSender sender, String variableName) {
        try {
            if (sender instanceof Player player) {
                if (variableName != null) {
                    VariableCommandPacket packet = new VariableCommandPacket();
                    packet.set = false;
                    packet.variable_name = variableName;
                    PacketHandler.sendToClient(player, packet);
                }
            }
        } catch (Exception ex) {
            sender.sendMessage("§cError while executing command!");
            ex.printStackTrace();
        }
    }

    private static void setVariable(CommandSender sender, String variableName, String setToValue, boolean sendFeedback) {
        try {
            if (sender instanceof Player player) {
                if ((variableName != null) && (setToValue != null)) {
                    VariableCommandPacket packet = new VariableCommandPacket();
                    packet.set = true;
                    packet.variable_name = variableName;
                    packet.set_to_value = setToValue;
                    packet.feedback = sendFeedback;
                    PacketHandler.sendToClient(player, packet);
                }
            }
        } catch (Exception ex) {
            sender.sendMessage("§cError while executing command!");
            ex.printStackTrace();
        }
    }

}

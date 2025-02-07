package de.keksuccino.fancymenu.networking.packet.commands.variable.suggestions;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;

public class VariableCommandSuggestionsPacket extends Packet {

    public List<String> variable_suggestions;

    @Override
    public boolean processPacket(@Nullable Player sender) {
        if (sender == null) return false;
        return ServerSideVariableCommandSuggestionsPacketLogic.handle(sender, this);
    }

}

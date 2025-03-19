package de.keksuccino.fancymenu.networking.packet.commands.layout.suggestions;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;

public class LayoutCommandSuggestionsPacket extends Packet {

    public List<String> layout_suggestions;

    @Override
    public boolean processPacket(@Nullable Player sender) {
        if (sender == null) return false;
        return ServerSideLayoutCommandSuggestionsPacketLogic.handle(sender, this);
    }

}

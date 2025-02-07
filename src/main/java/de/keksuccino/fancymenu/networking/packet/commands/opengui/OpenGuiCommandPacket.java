package de.keksuccino.fancymenu.networking.packet.commands.opengui;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class OpenGuiCommandPacket extends Packet {

    public String screen_identifier;

    @Override
    public boolean processPacket(@Nullable Player sender) {
        return false;
    }

}

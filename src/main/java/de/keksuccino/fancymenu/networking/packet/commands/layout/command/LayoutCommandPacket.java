package de.keksuccino.fancymenu.networking.packet.commands.layout.command;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class LayoutCommandPacket extends Packet {

    public String layout_name;
    public boolean enabled;

    @Override
    public boolean processPacket(@Nullable Player sender) {
        return false;
    }

}

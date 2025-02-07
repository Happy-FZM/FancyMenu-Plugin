package de.keksuccino.fancymenu.networking.packet.commands.closegui;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class CloseGuiCommandPacket extends Packet {

    @Override
    public boolean processPacket(@Nullable Player sender) {
        return false;
    }
}

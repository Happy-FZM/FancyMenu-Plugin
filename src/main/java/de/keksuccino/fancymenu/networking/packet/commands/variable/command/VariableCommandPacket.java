package de.keksuccino.fancymenu.networking.packet.commands.variable.command;

import de.keksuccino.fancymenu.networking.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class VariableCommandPacket extends Packet {

    public boolean set;
    public String variable_name;
    public String set_to_value;
    public boolean feedback;

    @Override
    public boolean processPacket(@Nullable Player sender) {
        return false;
    }

}

package de.keksuccino.fancymenu.networking.packet.commands.variable.command;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class VariableCommandPacketCodec extends PacketCodec<VariableCommandPacket> {

    public VariableCommandPacketCodec() {
        super("variable_command", VariableCommandPacket.class);
    }

}

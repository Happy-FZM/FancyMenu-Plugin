package de.keksuccino.fancymenu.networking.packet.commands.variable.suggestions;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class VariableCommandSuggestionsPacketCodec extends PacketCodec<VariableCommandSuggestionsPacket> {

    public VariableCommandSuggestionsPacketCodec() {
        super("variable_command_suggestions", VariableCommandSuggestionsPacket.class);
    }

}

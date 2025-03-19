package de.keksuccino.fancymenu.networking.packet.commands.layout.suggestions;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class LayoutCommandSuggestionsPacketCodec extends PacketCodec<LayoutCommandSuggestionsPacket> {

    public LayoutCommandSuggestionsPacketCodec() {
        super("layout_command_suggestions", LayoutCommandSuggestionsPacket.class);
    }

}

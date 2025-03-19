package de.keksuccino.fancymenu.networking.packet.commands.layout.command;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class LayoutCommandPacketCodec extends PacketCodec<LayoutCommandPacket> {

    public LayoutCommandPacketCodec() {
        super("layout_command", LayoutCommandPacket.class);
    }

}
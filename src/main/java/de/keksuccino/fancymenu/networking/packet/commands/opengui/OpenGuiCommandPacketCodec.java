package de.keksuccino.fancymenu.networking.packet.commands.opengui;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class OpenGuiCommandPacketCodec extends PacketCodec<OpenGuiCommandPacket> {

    public OpenGuiCommandPacketCodec() {
        super("open_gui_command", OpenGuiCommandPacket.class);
    }

}

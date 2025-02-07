package de.keksuccino.fancymenu.networking;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public abstract class Packet {

    /**
     * @param sender The sender of the packet in case it was sent from client to server. This is NULL if the packet was sent by the server to the client!
     */
    public abstract boolean processPacket(@Nullable Player sender);


}

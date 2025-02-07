package de.keksuccino.fancymenu.networking;

import com.google.gson.Gson;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketCodec<T extends Packet> {

    private static final Logger LOGGER = Bukkit.getLogger();

    protected final String packetIdentifier;
    protected final Class<T> type;

    public PacketCodec( String packetIdentifier,  Class<T> type) {
        this.packetIdentifier = Objects.requireNonNull(packetIdentifier);
        this.type = Objects.requireNonNull(type);
    }

    @Nullable
    public String serialize( T packet) {
        try {
            Gson gson = this.buildGson();
            return this.getPacketIdentifier() + ":" + Objects.requireNonNull(gson.toJson(Objects.requireNonNull(packet)));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to serialize packet!", ex);
        }
        return null;
    }

    @Nullable
    public T deserialize( String dataWithoutIdentifier) {
        try {
            Gson gson = this.buildGson();
            return Objects.requireNonNull(gson.fromJson(Objects.requireNonNull(dataWithoutIdentifier), this.type));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to deserialize packet!", ex);
        }
        return null;
    }

    
    protected Gson buildGson() {
        return new Gson();
    }

    
    public String getPacketIdentifier() {
        return this.packetIdentifier;
    }

    
    public Class<T> getType() {
        return this.type;
    }

}

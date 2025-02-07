package de.keksuccino.fancymenu.networking;

import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketRegistry {

    private static final Logger LOGGER = Bukkit.getLogger();
    private static final Map<String, PacketCodec<?>> CODECS = new HashMap<>();

    private static boolean registrationsAllowed = true;

    /**
     * Register packets here.<br>
     * {@link PacketCodec}s should get registered during mod-init.
     **/
    public static void register(PacketCodec<?> codec) {
        if (!registrationsAllowed) throw new RuntimeException("Tried to register PacketCodec too late! PacketCodecs need to get registered as early as possible!");
        if (CODECS.containsKey(Objects.requireNonNull(codec.getPacketIdentifier()))) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] PacketCodec with identifier '" + codec.getPacketIdentifier() + "' already registered! Overriding codec!");
        }
        CODECS.put(codec.getPacketIdentifier(), codec);
    }

    
    public static List<PacketCodec<?>> getCodecs() {
        return new ArrayList<>(CODECS.values());
    }

    @Nullable
    public static PacketCodec<?> getCodec(String identifier) {
        return CODECS.get(identifier);
    }

    @SuppressWarnings("all")
    @Nullable
    public static <T extends Packet> PacketCodec<T> getCodecFor(T packet) {
        try {
            for (PacketCodec<?> codec : CODECS.values()) {
                if (codec.getType() == packet.getClass()) return (PacketCodec<T>) codec;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to get codec for packet!", ex);
        }
        return null;
    }

    public static void endRegistrationPhase() {
        registrationsAllowed = false;
    }

}

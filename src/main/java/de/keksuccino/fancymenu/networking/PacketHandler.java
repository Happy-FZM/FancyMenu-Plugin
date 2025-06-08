package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.networking.packet.Packets;
import de.keksuccino.fancymenu.networking.utils.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketHandler {

    private final FancyMenu plugin;
    private static final Logger LOGGER = Bukkit.getLogger();
    private static BiConsumer<Player, String> sendToClientPlayerAndDataConsumer = null;
    private static final String CHANNEL = "fancymenu:play";
    private static final String CHANNEL_FABRIC = "fancymenu:packet_bridge";
    private static final String CHANNEL_FABRIC_NEW = "minecraft:fancymenu_packet_bridge";
    private static final String CHANNEL_NEOFORGE = "fancymenu:fancymenu_bridge_packet";
    private static final Map<String, Dispatcher> CACHED_PLAYER_CLIENT = Collections.synchronizedMap(new HashMap<>());
    private static Dispatcher FORGE;
    private static Dispatcher FABRIC;
    private static Dispatcher NEW_FABRIC;
    private static Dispatcher NEOFORGE;


    public static void init() {
        new PacketHandler();
    }

    public PacketHandler() {
        this.plugin = FancyMenu.getInstance();

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        FORGE = new ForgeDispatcher();
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, FORGE);

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_FABRIC);
        FABRIC = new FabricDispatcher();
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_FABRIC, FABRIC);

        // Support 1.21+
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_FABRIC_NEW);
        NEW_FABRIC = new NewFabricDispatcher();
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_FABRIC_NEW, NEW_FABRIC);

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NEOFORGE);
        NEOFORGE = new NeoForgeDispatcher();
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_NEOFORGE, NEOFORGE);

        sendToClientPlayerAndDataConsumer = this::sendToClient;
        Packets.registerAll();
    }

    private void sendToClient(final Player player, final String dataWithIdentifier) {
        Dispatcher dispatcher = CACHED_PLAYER_CLIENT.get(player.getName());
        if (dispatcher != null) {
            dispatcher.sendToClient(player, dataWithIdentifier);
        } else {
            FORGE.sendToClient(player, dataWithIdentifier);
            FABRIC.sendToClient(player, dataWithIdentifier);
            NEW_FABRIC.sendToClient(player, dataWithIdentifier);
            NEOFORGE.sendToClient(player, dataWithIdentifier);
        }
    }

    public static <T extends Packet> void sendToClient(Player toPlayer, T packet) {
        Objects.requireNonNull(sendToClientPlayerAndDataConsumer, "Tried to send packet to client too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                sendToClientPlayerAndDataConsumer.accept(Objects.requireNonNull(toPlayer), Objects.requireNonNull(codec.serialize(packet)));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to send packet to client!", ex);
            }
        } else {
            LOGGER.log(Level.WARNING, "[FANCYMENU] No codec found for packet: " + packet.getClass(), new NullPointerException("Codec returned for packet was NULL!"));
        }
    }

    protected static Packet deserializePacket(Supplier<Packet> packetSupplier) {
        try {
            return packetSupplier.get();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to deserialize packet!", ex);
        }
        return null;
    }

    private static void handlePacket(Player player, String dataWithIdentifier) {
        if (!dataWithIdentifier.contains(":")) return;
        String[] dataSplit = dataWithIdentifier.split(":", 2);
        PacketCodec<?> codec = PacketRegistry.getCodec(dataSplit[0]);
        if (codec == null) {
            LOGGER.log(Level.WARNING, "[FANCYMENU] No codec for packet data found with identifier: " + dataSplit[0], new NullPointerException("Codec returned for identifier was NULL!"));
            return;
        }
        Packet packet = deserializePacket(() -> Objects.requireNonNull(codec.deserialize(dataSplit[1])));
        if (packet != null) {
            try {
                packet.processPacket(player);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[FANCYMENU] Failed to process packet on client!", ex);
            }
        }
    }

    public static abstract class Dispatcher implements PluginMessageListener {

        @Override
        public void onPluginMessageReceived(String channel, Player player, byte[] message) {

        }

        protected void sendToClient(final Player player, final String dataWithIdentifier) {

        }

    }

    public static class ForgeDispatcher extends Dispatcher {

        @Override
        public void onPluginMessageReceived(String channel, Player player, byte[] message) {
            PacketHandler.CACHED_PLAYER_CLIENT.put(player.getName(), this);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
            byte index = buffer.readByte();
            String side = buffer.readUtf();
            String dataWithIdentifier = buffer.readUtf();
            if (index == 0 && "server".equals(side)) {
                handlePacket(player, dataWithIdentifier);
            }
        }

        @Override
        protected void sendToClient(final Player player, final String dataWithIdentifier) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeByte(0);
            buffer.writeUtf("client");
            buffer.writeUtf(dataWithIdentifier);
            player.sendPluginMessage(FancyMenu.getInstance(), CHANNEL, buffer.array());
        }

    }

    public static class FabricDispatcher extends Dispatcher {

        @Override
        public void onPluginMessageReceived(String channel, Player player, byte[] message) {
            PacketHandler.CACHED_PLAYER_CLIENT.put(player.getName(), this);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
            String side = buffer.readUtf();
            String dataWithIdentifier = buffer.readUtf();
            if ("server".equals(side)) {
                handlePacket(player, dataWithIdentifier);
            }
        }

        @Override
        protected void sendToClient(final Player player, final String dataWithIdentifier) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeUtf("client");
            buffer.writeUtf(dataWithIdentifier);
            player.sendPluginMessage(FancyMenu.getInstance(), CHANNEL_FABRIC, buffer.array());
        }

    }

    public static class NewFabricDispatcher extends FabricDispatcher {

        @Override
        protected void sendToClient(final Player player, final String dataWithIdentifier) {
            FriendlyByteBuf buffer = new FriendlyByteBuf();
            buffer.writeUtf("client");
            buffer.writeUtf(dataWithIdentifier);
            byte[] data = new byte[buffer.writerIndex()];
            buffer.getBytes(0, data);
            player.sendPluginMessage(FancyMenu.getInstance(), CHANNEL_FABRIC_NEW, data);
        }

    }

    public static class NeoForgeDispatcher extends NewFabricDispatcher {

        @Override
        protected void sendToClient(final Player player, final String dataWithIdentifier) {
            FriendlyByteBuf buffer = new FriendlyByteBuf();
            buffer.writeUtf("client");
            buffer.writeUtf(dataWithIdentifier);
            byte[] data = new byte[buffer.writerIndex()];
            buffer.getBytes(0, data);
            player.sendPluginMessage(FancyMenu.getInstance(), CHANNEL_NEOFORGE, data);
        }

    }

}

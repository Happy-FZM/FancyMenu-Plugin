package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FancyMenu extends JavaPlugin implements Listener {

    private static FancyMenu instance;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true).skipReloadDatapacks(true));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        instance = this;
        PacketHandler.init();
        Commands.init();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
    }

    public static FancyMenu getInstance() {
        return instance;
    }

    @EventHandler
    public void onJoin(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            player.sendMessage("你右键了方块: " + clickedBlock.getBlockData().getAsString());
        }
    }

}

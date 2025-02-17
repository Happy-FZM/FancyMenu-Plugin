package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.plugin.java.JavaPlugin;

public final class FancyMenu extends JavaPlugin {

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
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
    }

    public static FancyMenu getInstance() {
        return instance;
    }

}

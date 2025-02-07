package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.FancyMenu;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Commands {

    public static void init() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(FancyMenu.getInstance()).silentLogs(true));
        CloseGuiScreenCommand.register();
        OpenGuiScreenCommand.register();
        VariableCommand.register();
    }

}

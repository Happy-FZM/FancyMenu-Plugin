package de.keksuccino.fancymenu.commands;

public class Commands {

    public static void init() {
        CloseGuiScreenCommand.register();
        OpenGuiScreenCommand.register();
        VariableCommand.register();
    }

}

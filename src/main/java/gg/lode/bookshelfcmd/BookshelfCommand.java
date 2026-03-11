package gg.lode.bookshelfcmd;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BookshelfCommand extends CommandAPICommand {

    public BookshelfCommand(String commandName) {
        super(commandName);
    }

    public BookshelfCommand(String commandName, String permission) {
        super(commandName);
        withPermission(permission);
    }

    public BookshelfCommand(String commandName, String permission, String... aliases) {
        super(commandName);
        withPermission(permission);
        withAliases(aliases);
    }

    @Override
    public void register(JavaPlugin plugin) {
        register(plugin.getName().toLowerCase());
    }

}

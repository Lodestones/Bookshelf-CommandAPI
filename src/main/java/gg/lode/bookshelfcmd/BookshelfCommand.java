package gg.lode.bookshelfcmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIPaper;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

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
    @SuppressWarnings("unchecked")
    public void register(String namespace) {
        try {
            // Use reflection to access the toLiteralCommandNode method
            // This method exists in CommandAPICommand but may not be public
            Method toLiteralMethod = CommandAPICommand.class.getDeclaredMethod("toLiteralCommandNode");
            toLiteralMethod.setAccessible(true);
            LiteralCommandNode<Object> node = (LiteralCommandNode<Object>) toLiteralMethod.invoke(this);
            
            // Convert the node to a builder
            LiteralArgumentBuilder<Object> builder = node.createBuilder();

            // Copy over all children nodes
            for (CommandNode<Object> child : node.getChildren()) {
                if (child instanceof LiteralCommandNode<Object> literalChild) {
                    builder.then(literalChild.createBuilder());
                } else {
                    // For non-literal children, add them directly
                    builder.then(child);
                }
            }

            // Register the command node
            CommandAPIPaper.getPaper().registerCommandNode(builder, namespace);
        } catch (Exception e) {
            // Fallback to default registration if reflection fails
            super.register(namespace);
        }
    }

    @Override
    public void register(JavaPlugin plugin) {
        register(plugin.getName().toLowerCase());
    }

}

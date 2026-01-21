package gg.lode.bookshelfcmd.merge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.Brigadier;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIPaper;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * A CommandAPICommand that supports merging with existing vanilla commands.
 * <p>
 * Use {@link #merge(String)} to merge your command branches with an existing
 * vanilla command, preserving all vanilla functionality while adding your own.
 * <p>
 * Use {@link #register(String)} to replace/override the command entirely (default behavior).
 * <p>
 * Example:
 * <pre>{@code
 * new MergeableCommand("give")
 *     .withArguments(new LiteralArgument("custom"))
 *     .executesPlayer(player -> player.sendMessage("Custom give!"))
 *     .merge("myplugin"); // Merges with vanilla /give
 * }</pre>
 */
public class MergeableCommand extends CommandAPICommand {

    public MergeableCommand(String commandName) {
        super(commandName);
    }

    /**
     * Merge this command with an existing vanilla command.
     * <p>
     * This preserves all vanilla subcommands/branches while adding
     * your custom branches on top.
     *
     * @param namespace The namespace to register under
     */
    @SuppressWarnings("unchecked")
    public void merge(String namespace) {
        CommandNode<?> vanillaNode = Brigadier.getRootNode().getChild(getName());

        if (!(vanillaNode instanceof LiteralCommandNode<?> vanillaLiteral)) {
            // No vanilla command found, just register normally
            register(namespace);
            return;
        }

        try {
            // Get this command as a node
            Method toLiteralMethod = CommandAPICommand.class.getDeclaredMethod("toLiteralCommandNode");
            toLiteralMethod.setAccessible(true);
            LiteralCommandNode<Object> customNode = (LiteralCommandNode<Object>) toLiteralMethod.invoke(this);

            // Start with vanilla command structure
            LiteralArgumentBuilder<Object> merged = ((LiteralCommandNode<Object>) vanillaLiteral).createBuilder();

            // Add all vanilla children
            for (CommandNode<Object> child : ((LiteralCommandNode<Object>) vanillaLiteral).getChildren()) {
                merged.then(child);
            }

            // Add custom children from this command
            for (CommandNode<Object> child : customNode.getChildren()) {
                merged.then(child);
            }

            // Register merged command
            CommandAPIPaper.getPaper().registerCommandNode(merged, namespace);

        } catch (Exception e) {
            // Fallback: register normally if reflection fails
            register(namespace);
        }
    }

    /**
     * Merge this command with an existing vanilla command.
     *
     * @param plugin The plugin to use for namespace
     */
    public void merge(JavaPlugin plugin) {
        merge(plugin.getName().toLowerCase());
    }
}

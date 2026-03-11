package gg.lode.bookshelfcmd.merge;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.Brigadier;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

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

        if (!(vanillaNode instanceof LiteralCommandNode<?>)) {
            // No vanilla command found, just register normally
            register(namespace);
            return;
        }

        try {
            // Save vanilla children before registration overwrites them
            var vanillaChildren = new ArrayList<>(((LiteralCommandNode<Object>) vanillaNode).getChildren());

            // Register the custom command normally (this replaces the vanilla node)
            super.register(namespace);

            // Get the newly registered node and re-add vanilla children
            CommandNode<?> registeredNode = Brigadier.getRootNode().getChild(getName());
            if (registeredNode instanceof LiteralCommandNode<?>) {
                LiteralCommandNode<Object> registeredLiteral = (LiteralCommandNode<Object>) registeredNode;
                for (CommandNode<Object> vanillaChild : vanillaChildren) {
                    if (registeredLiteral.getChild(vanillaChild.getName()) == null) {
                        registeredLiteral.addChild(vanillaChild);
                    }
                }
            }
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

package gg.lode.bookshelfcmd.merge;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.Brigadier;
import dev.jorel.commandapi.CommandAPIPaper;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for merging vanilla/existing command trees with custom branches.
 * <p>
 * Example usage:
 * <pre>{@code
 * CommandMerger.merge("give")
 *     .addBranch(new CommandAPICommand("give")
 *         .withArguments(new LiteralArgument("custom"))
 *         .executes(info -> info.sender().sendMessage("Custom branch")))
 *     .register("myplugin");
 * }</pre>
 */
public class CommandMerger {

    private final String commandName;
    private final List<ArgumentBuilder<?, ?>> customBranches = new ArrayList<>();
    private LiteralCommandNode<?> vanillaNode;

    private CommandMerger(String commandName) {
        this.commandName = commandName;
    }

    /**
     * Start merging a command by name.
     *
     * @param commandName The name of the existing command to merge with
     * @return A new CommandMerger instance
     */
    public static CommandMerger merge(String commandName) {
        CommandMerger merger = new CommandMerger(commandName);
        CommandNode<?> node = Brigadier.getRootNode().getChild(commandName);
        if (node instanceof LiteralCommandNode<?> literalNode) {
            merger.vanillaNode = literalNode;
        }
        return merger;
    }

    /**
     * Add a custom branch to the merged command tree.
     *
     * @param branch The argument builder representing the custom branch
     * @return This merger for chaining
     */
    public CommandMerger addBranch(ArgumentBuilder<?, ?> branch) {
        customBranches.add(branch);
        return this;
    }

    /**
     * Add multiple custom branches to the merged command tree.
     *
     * @param branches The argument builders representing custom branches
     * @return This merger for chaining
     */
    public CommandMerger addBranches(ArgumentBuilder<?, ?>... branches) {
        for (ArgumentBuilder<?, ?> branch : branches) {
            customBranches.add(branch);
        }
        return this;
    }

    /**
     * Register the merged command under the specified namespace.
     *
     * @param namespace The namespace to register under (typically plugin name)
     */
    @SuppressWarnings("unchecked")
    public void register(String namespace) {
        if (vanillaNode == null) {
            throw new IllegalStateException("No existing command found with name: " + commandName);
        }

        LiteralArgumentBuilder<Object> builder = ((LiteralCommandNode<Object>) vanillaNode).createBuilder();

        // Copy all existing children from vanilla command
        for (CommandNode<Object> child : ((LiteralCommandNode<Object>) vanillaNode).getChildren()) {
            builder.then(child);
        }

        // Add custom branches
        for (ArgumentBuilder<?, ?> branch : customBranches) {
            builder.then((ArgumentBuilder<Object, ?>) branch);
        }

        CommandAPIPaper.getPaper().registerCommandNode(builder, namespace);
    }

    /**
     * Check if the vanilla command was found.
     *
     * @return true if the vanilla command exists
     */
    public boolean hasVanillaCommand() {
        return vanillaNode != null;
    }

    /**
     * Get the vanilla command node if it exists.
     *
     * @return The vanilla command node, or null if not found
     */
    public LiteralCommandNode<?> getVanillaNode() {
        return vanillaNode;
    }
}

package gg.lode.bookshelfcmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.SuggestionsBranch;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandFlagArgument extends FlagArgument {
    private final SuggestionsBranch<CommandSender> replacements = SuggestionsBranch.suggest();

    public CommandFlagArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags, Set<String> wordFlags) {
        super(nodeName, flags, valueFlags, wordFlags);

        replaceSuggestions((info, builder) -> {
            // Extract information
            CommandSender sender = info.sender();
            CommandMap commandMap = CommandAPIBukkit.get().getCommandMap();
            String command = info.currentArg();

            // Setup context for errors
            StringReader context = new StringReader(command);

            if (!command.contains(" ")) {
                // Suggesting command name
                ArgumentSuggestions<CommandSender> replacement = replacements.getNextSuggestion(sender);
                if (replacement != null) {
                    return replacement.suggest(new SuggestionInfo<>(sender, new CommandArguments(new Object[0], new LinkedHashMap<>(), new String[0], new LinkedHashMap<>(), info.currentInput()), command, command), builder);
                }

                List<String> results = commandMap.tabComplete(sender, command);
                // No applicable commands
                if (results == null) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);
                }

                // Remove / that gets prefixed to command name if the sender is a player
                if (sender instanceof Player) {
                    for (String result : results) {
                        builder.suggest(result.substring(1));
                    }
                } else {
                    for (String result : results) {
                        builder.suggest(result);
                    }
                }

                return builder.buildFuture();
            }


            // Verify commandLabel
            String commandLabel = command.substring(0, command.indexOf(" "));
            Command target = commandMap.getCommand(commandLabel);
            if (target == null) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);
            }

            // Get arguments
            String[] arguments = command.split(" ");
            if (!arguments[0].isEmpty() && command.endsWith(" ")) {
                // If command ends with space add an empty argument
                arguments = Arrays.copyOf(arguments, arguments.length + 1);
                arguments[arguments.length - 1] = "";
            }

            // Build suggestion
            builder = builder.createOffset(builder.getStart() + command.lastIndexOf(" ") + 1);

            int lastIndex = arguments.length - 1;
            String[] previousArguments = Arrays.copyOf(arguments, lastIndex);
            ArgumentSuggestions<CommandSender> replacement = replacements.getNextSuggestion(sender, previousArguments);
            if (replacement != null) {
                return replacement.suggest(new SuggestionInfo<>(sender, new CommandArguments(previousArguments, new LinkedHashMap<>(), previousArguments, new LinkedHashMap<>(), info.currentInput()), command, arguments[lastIndex]), builder);
            }

            // Remove command name from arguments for normal tab-completion
            arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

            // Get location sender is looking at if they are a Player, matching vanilla behavior
            // No builtin Commands use the location parameter, but they could
            Location location = null;
            if (sender instanceof Player player) {
                Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
                if (block != null) {
                    location = block.getLocation();
                }
            }

            // Build suggestions for new argument
            for (String tabCompletion : target.tabComplete(sender, commandLabel, arguments, location)) {
                builder.suggest(tabCompletion);
            }

            // Flag handling logic
            for (String str : command.split(" ")) {
                if (str.startsWith("-")) {
                    Set<Character> usedCharFlags = new HashSet<>();
                    Set<String> usedWordFlagSet = new HashSet<>();
                    for (String part : info.currentInput().split("\\s+")) {
                        if (part.startsWith("--") && part.length() > 2) {
                            usedWordFlagSet.add(part.substring(2));
                        } else if (part.startsWith("-") && part.length() > 1) {
                            for (char c : part.substring(1).toCharArray()) {
                                usedCharFlags.add(c);
                            }
                        }
                    }

                    List<Character> unusedCharFlags = allFlags.stream()
                            .filter(f -> !usedCharFlags.contains(f))
                            .toList();
                    List<String> unusedWordFlags = wordFlags.stream()
                            .filter(w -> !usedWordFlagSet.contains(w))
                            .toList();

                    if (!info.currentInput().endsWith(" ")) {
                        if (str.startsWith("--")) {
                            String partial = str.substring(2);
                            for (String w : unusedWordFlags) {
                                if (w.startsWith(partial)) builder.suggest("--" + w);
                            }
                        } else {
                            for (Character f : unusedCharFlags) {
                                String clean = str.substring(1);
                                builder.suggest("-" + clean + f);
                            }
                        }
                    } else {
                        for (Character f : unusedCharFlags) {
                            builder.suggest("-" + f);
                        }
                        for (String w : unusedWordFlags) {
                            builder.suggest("--" + w);
                        }
                    }
                }
            }
            return builder.buildFuture();
        });
    }

    public CommandFlagArgument(String nodeName, Set<Character> flags) {
        this(nodeName, flags, Set.of(), Set.of());
    }

    public CommandFlagArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags) {
        this(nodeName, flags, valueFlags, Set.of());
    }
}

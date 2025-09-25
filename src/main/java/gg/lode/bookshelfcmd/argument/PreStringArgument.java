package gg.lode.bookshelfcmd.argument;

import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.Function;

public class PreStringArgument extends StringArgument {

    public PreStringArgument(String nodeName, List<String> suggestedArguments) {
        super(nodeName);
        replaceSuggestions(ArgumentSuggestions.strings(s -> suggestedArguments.toArray(String[]::new)));
    }

    public PreStringArgument(String nodeName, Function<SuggestionInfo<CommandSender>, String[]> suggestionFunction) {
        super(nodeName);
        replaceSuggestions(ArgumentSuggestions.strings(suggestionFunction));
    }

}

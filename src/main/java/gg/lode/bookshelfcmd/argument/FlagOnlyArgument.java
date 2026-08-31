package gg.lode.bookshelfcmd.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.GreedyArgument;
import dev.jorel.commandapi.executors.CommandArguments;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link FlagArgument} that rejects anything which is not a flag, at parse
 * time rather than at execution time.
 *
 * <p>This exists for the self-target syntax branches — {@code /heal -s} beside
 * {@code /heal <targets> [flags]}. Brigadier tries a node's children in
 * registration order and commits to the first one that parses, so the
 * self-target branch has to be registered first for {@code /heal -s} to reach
 * it at all: a hyphen is a legal character in a Minecraft name, so the entity
 * selector happily parses {@code -s} and only fails later, once the branch is
 * already chosen.
 *
 * <p>Registering it first is only safe if it also refuses {@code /heal Steve}.
 * {@link FlagArgument} cannot — it extends {@code GreedyStringArgument}, whose
 * raw type swallows any text. Neither can a {@code CustomArgument}: CommandAPI
 * runs its parser during execution, which is again too late to fall through.
 * So this supplies its own brigadier {@link ArgumentType}, which throws while
 * brigadier is still choosing a branch and therefore lets it move on to the
 * next one.
 */
public class FlagOnlyArgument extends Argument<String> implements GreedyArgument {

    private static final DynamicCommandExceptionType NOT_A_FLAG = new DynamicCommandExceptionType(
            token -> new LiteralMessage("Expected a flag, found '" + token + "'"));

    public FlagOnlyArgument(String nodeName, Set<Character> flags) {
        this(nodeName, flags, new HashSet<>(), new HashSet<>());
    }

    public FlagOnlyArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags) {
        this(nodeName, flags, valueFlags, new HashSet<>());
    }

    public FlagOnlyArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags, Set<String> wordFlags) {
        super(nodeName, () -> new FlagOnlyType(valueFlags));
        Set<Character> allFlags = new HashSet<>(flags);
        allFlags.addAll(valueFlags);
        replaceSuggestions(FlagArgument.flagSuggestions(allFlags, wordFlags));
    }

    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PRIMITIVE_GREEDY_STRING;
    }

    @Override
    public <Source> String parseArgument(CommandContext<Source> context, String key, CommandArguments previousArgs) {
        return context.getArgument(key, String.class);
    }

    /**
     * Consumes the rest of the input the way a greedy string does, but throws
     * unless every token is either a flag or the value of a flag that takes
     * one.
     */
    private static final class FlagOnlyType implements ArgumentType<String> {

        private final Set<Character> valueFlags;

        private FlagOnlyType(Set<Character> valueFlags) {
            this.valueFlags = valueFlags;
        }

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());

            boolean expectingValue = false;
            for (String token : text.trim().split("\\s+")) {
                if (token.isEmpty()) continue;
                if (FlagArgument.isFlagToken(token)) {
                    expectingValue = !token.startsWith("--")
                            && valueFlags.contains(token.charAt(token.length() - 1));
                    continue;
                }
                if (expectingValue) {
                    expectingValue = false;
                    continue;
                }
                reader.setCursor(start);
                throw NOT_A_FLAG.createWithContext(reader, token);
            }
            return text;
        }
    }
}

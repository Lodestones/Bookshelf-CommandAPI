package gg.lode.bookshelfcmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * One whitespace-delimited token, whatever characters it contains.
 *
 * <p>Exists because brigadier's word type — what CommandAPI's
 * {@code StringArgument} is built on — accepts only
 * {@code [a-zA-Z0-9_.+-]}. A namespaced id such as {@code minecraft:diamond}
 * stops dead at the colon and everything after it is reported as trailing
 * data, and the quotable string type is no better unquoted.
 *
 * <p>It deliberately validates nothing. An earlier attempt rejected unknown
 * words here, to let brigadier tell two syntax branches apart by their first
 * argument; that cost the argument its suggestions — a parse that throws on
 * partial input, the empty string included, leaves brigadier nothing to offer
 * completions for — and turned a mistyped item into a command that would not
 * run. Branches are told apart by their second argument instead, and what a
 * token means is decided in the executor.
 */
public class TokenArgument extends Argument<String> {

    /**
     * @param completions given what has been typed so far, lowercased, returns
     *                    the completions to offer. Matching belongs to the
     *                    caller: a namespaced vocabulary usually wants a bare
     *                    prefix to match the part after the colon too.
     */
    public TokenArgument(String nodeName, Function<String, Collection<String>> completions) {
        super(nodeName, () -> new TokenType(completions));
    }

    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PRIMITIVE_STRING;
    }

    @Override
    public <Source> String parseArgument(CommandContext<Source> context, String key, CommandArguments previousArgs) {
        return context.getArgument(key, String.class);
    }

    private static final class TokenType implements CustomArgumentType<String, String> {

        private final Function<String, Collection<String>> completions;

        private TokenType(Function<String, Collection<String>> completions) {
            this.completions = completions;
        }

        @Override
        public ArgumentType<String> getNativeType() {
            // Paper refuses to register an argument type it does not recognise,
            // so it has to be told which vanilla type the client should see.
            // The quotable string is the closest fit: it is what the client
            // uses to decide where this argument ends.
            return StringArgumentType.string();
        }

        @Override
        public String parse(StringReader reader) {
            int start = reader.getCursor();
            while (reader.canRead() && reader.peek() != ' ') {
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }

        /**
         * Completions have to come from the argument type itself. The default
         * implementation forwards to the native type, which suggests nothing,
         * so a custom type that does not override this shows an empty list
         * however its CommandAPI suggestions were declared.
         */
        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                  SuggestionsBuilder builder) {
            for (String candidate : completions.apply(builder.getRemaining().toLowerCase(Locale.ROOT))) {
                builder.suggest(candidate);
            }
            return builder.buildFuture();
        }
    }
}

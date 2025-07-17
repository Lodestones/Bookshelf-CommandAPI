package gg.lode.bookshelfcmd.argument;

import dev.jorel.commandapi.arguments.GreedyStringArgument;

import java.util.*;

public class FlagArgument extends GreedyStringArgument {

    protected final Set<Character> allFlags; // e.g., "dse" means "-d", "-s", "-e" are valid flags
    protected final Set<Character> valueFlags; // e.g., "d" means "-d" expects a value

    public FlagArgument(String nodeName, Set<Character> flags) {
        this(nodeName, flags, new HashSet<>());
    }

    public FlagArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags) {
        super(nodeName);
        this.valueFlags = valueFlags;
        this.allFlags = new HashSet<>(flags);
        this.allFlags.addAll(valueFlags);

        replaceSuggestions((info, builder) -> {
            String fullInput = info.currentInput().trim();
            String currentArg = info.currentArg();
            boolean endsWithSpace = info.currentInput().endsWith(" ");

            // Flag handling logic
            Set<Character> usedFlags = new HashSet<>();
            for (String part : fullInput.split("\\s+")) {
                if (part.startsWith("-") && part.length() > 1) {
                    for (char c : part.substring(1).toCharArray()) {
                        usedFlags.add(c);
                    }
                }
            }

            List<Character> unusedFlags = allFlags.stream()
                    .filter(f -> !usedFlags.contains(f))
                    .toList();

            if (!endsWithSpace) {
                String clean = currentArg.substring(1);
                for (Character f : unusedFlags) {
                    builder.suggest("-" + clean + f);
                }
            } else {
                for (Character f : unusedFlags) {
                    builder.suggest(currentArg + "-" + f);
                }
            }

            return builder.buildFuture();
        });
    }

    /**
     * Checks if all characters of the given flagSet exist in the command input.
     *
     * @param input   The full command input string, e.g. "/test -sd 20 -e 50"
     * @param flagSet The flag characters to check for, e.g. "se"
     * @return true if all flags are present, false otherwise
     */
    public static boolean hasFlags(String input, String flagSet) {
        if (flagSet == null || flagSet.isEmpty() || input == null) return false;

        // Parse flags (does not consider value flags, just presence)
        Set<Character> presentFlags = new HashSet<>();
        String[] parts = input.trim().split("\\s+");

        for (String part : parts) {
            if (part.startsWith("-") && part.length() > 1 && !part.matches("-\\d+")) {
                String chunk = part.substring(1);
                for (char c : chunk.toCharArray()) {
                    presentFlags.add(c);
                }
            }
        }

        // Check each required flag
        for (char c : flagSet.toCharArray()) {
            if (!presentFlags.contains(c)) return false;
        }

        return true;
    }

    /**
     * Parses flags and their values from a full command input string.
     * Supports grouped flags like -es and values after value flags.
     *
     * @param input          Full input string, e.g. "/test sub -d 20 -es 50 -c 23"
     * @param flagsWithValue Set of flags that expect a value, e.g. Set.of('d', 'e', 'c')
     * @return ParsedFlags containing active flags and any assigned values
     */
    public static ParsedFlags parseFlags(String input, Set<Character> flagsWithValue) {
        ParsedFlags parsed = new ParsedFlags();
        if (input == null || input.isEmpty()) return parsed;

        String[] args = input.trim().split("\\s+");

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (!arg.startsWith("-") || arg.length() < 2 || arg.matches("-\\d+")) continue;

            String chunk = arg.substring(1);

            for (int j = 0; j < chunk.length(); j++) {
                char flag = chunk.charAt(j);
                parsed.activeFlags.add(flag);

                boolean isLastInChunk = j == chunk.length() - 1;
                boolean expectsValue = flagsWithValue.contains(flag);

                if (expectsValue && isLastInChunk && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    parsed.flagValues.put(flag, args[++i]); // store and skip value
                } else if (expectsValue && isLastInChunk) {
                    parsed.flagValues.put(flag, null); // no value supplied
                }
            }
        }

        return parsed;
    }

    /**
     * Removes all flags and their values from the input string.
     *
     * @param input          The full command input, e.g. "/test sub -d 20 -es 50 hello world"
     * @param flagsWithValue Set of flags that expect a value, e.g. Set.of('d', 'e', 's')
     * @return A sanitized string with only non-flag arguments, e.g. "/test sub hello world"
     */
    public static String sanitizeInput(String input, Set<Character> flagsWithValue) {
        if (input == null || input.isEmpty()) return "";

        String[] args = input.trim().split("\\s+");
        List<String> result = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // Detect flag tokens like "-a" or "-bc", but not negative numbers "-123"
            if (arg.startsWith("-") && arg.length() > 1 && !arg.matches("-\\d+")) {
                String chunk = arg.substring(1);

                // If the last flag in this group expects a value, skip the next token
                char lastFlag = chunk.charAt(chunk.length() - 1);
                if (flagsWithValue.contains(lastFlag) && i + 1 < args.length
                        && !args[i + 1].startsWith("-")) {
                    i++; // skip the value token
                }
                // skip this flag token
            } else {
                // normal argument or negative number
                result.add(arg);
            }
        }

        return String.join(" ", result);
    }

    public static class ParsedFlags {
        private final Set<Character> activeFlags = new HashSet<>();
        private final Map<Character, String> flagValues = new HashMap<>();

        public boolean hasFlag(char flag) {
            return activeFlags.contains(flag) || flagValues.containsKey(flag);
        }

        public String getFlagValue(char flag) {
            return flagValues.get(flag);
        }
    }
}

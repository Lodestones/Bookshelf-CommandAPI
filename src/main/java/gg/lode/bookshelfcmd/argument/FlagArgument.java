package gg.lode.bookshelfcmd.argument;

import dev.jorel.commandapi.arguments.GreedyStringArgument;

import java.util.*;

public class FlagArgument extends GreedyStringArgument {

    protected final Set<Character> allFlags; // e.g., "dse" means "-d", "-s", "-e" are valid flags
    protected final Set<Character> valueFlags; // e.g., "d" means "-d" expects a value
    protected final Set<String> wordFlags; // e.g., "skip" means "--skip" is a valid boolean flag

    public FlagArgument(String nodeName, Set<Character> flags) {
        this(nodeName, flags, new HashSet<>(), new HashSet<>());
    }

    public FlagArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags) {
        this(nodeName, flags, valueFlags, new HashSet<>());
    }

    public FlagArgument(String nodeName, Set<Character> flags, Set<Character> valueFlags, Set<String> wordFlags) {
        super(nodeName);
        this.valueFlags = valueFlags;
        this.allFlags = new HashSet<>(flags);
        this.allFlags.addAll(valueFlags);
        this.wordFlags = new HashSet<>(wordFlags);

        replaceSuggestions((info, builder) -> {
            String fullInput = info.currentInput().trim();
            String currentArg = info.currentArg();
            boolean endsWithSpace = info.currentInput().endsWith(" ");

            // Collect used single-char flags
            Set<Character> usedFlags = new HashSet<>();
            Set<String> usedWordFlags = new HashSet<>();
            for (String part : fullInput.split("\\s+")) {
                if (part.startsWith("--") && part.length() > 2) {
                    usedWordFlags.add(part.substring(2));
                } else if (part.startsWith("-") && part.length() > 1) {
                    for (char c : part.substring(1).toCharArray()) {
                        usedFlags.add(c);
                    }
                }
            }

            List<Character> unusedCharFlags = allFlags.stream()
                    .filter(f -> !usedFlags.contains(f))
                    .toList();

            List<String> unusedWordFlags = this.wordFlags.stream()
                    .filter(w -> !usedWordFlags.contains(w))
                    .toList();

            if (!endsWithSpace) {
                if (currentArg.startsWith("--")) {
                    String partial = currentArg.substring(2);
                    for (String w : unusedWordFlags) {
                        if (w.startsWith(partial)) {
                            builder.suggest("--" + w);
                        }
                    }
                } else if (currentArg.startsWith("-")) {
                    String clean = currentArg.substring(1);
                    for (Character f : unusedCharFlags) {
                        builder.suggest("-" + clean + f);
                    }
                }
            } else {
                for (Character f : unusedCharFlags) {
                    builder.suggest(currentArg + "-" + f);
                }
                for (String w : unusedWordFlags) {
                    builder.suggest(currentArg + "--" + w);
                }
            }

            return builder.buildFuture();
        });
    }

    /**
     * Checks if a short flag or its long word flag equivalent is present.
     *
     * @param input     The full command input string
     * @param shortFlag The single-character flag to check, e.g. 'r'
     * @param longFlag  The long word flag to check (without --), e.g. "remove"
     * @return true if either -shortFlag or --longFlag is present
     */
    public static boolean hasFlag(String input, char shortFlag, String longFlag) {
        return hasFlags(input, String.valueOf(shortFlag)) || hasWordFlag(input, longFlag);
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
            if (part.startsWith("-") && part.length() > 1 && !part.startsWith("--") && !part.matches("-\\d+")) {
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
     * Checks if a word flag (--word) is present in the command input.
     *
     * @param input The full command input string, e.g. "/randomnick @a --skip"
     * @param word  The word flag to look for (without --), e.g. "skip"
     * @return true if --word is present, false otherwise
     */
    public static boolean hasWordFlag(String input, String word) {
        if (input == null || word == null || word.isEmpty()) return false;
        String target = "--" + word;
        for (String part : input.trim().split("\\s+")) {
            if (part.equals(target)) return true;
        }
        return false;
    }

    /**
     * Parses flags and their values from a full command input string.
     * Supports grouped flags like -es and values after value flags.
     * Also parses word flags (--skip).
     *
     * @param input          Full input string, e.g. "/test sub -d 20 -es 50 -c 23 --skip"
     * @param flagsWithValue Set of flags that expect a value, e.g. Set.of('d', 'e', 'c')
     * @return ParsedFlags containing active flags, assigned values, and active word flags
     */
    public static ParsedFlags parseFlags(String input, Set<Character> flagsWithValue) {
        return parseFlags(input, flagsWithValue, Set.of());
    }

    /**
     * Parses flags and their values from a full command input string.
     * Supports grouped flags like -es, values after value flags, and word flags (--skip, --duration 7d).
     *
     * @param input              Full input string, e.g. "/test sub -d 20 -es 50 --duration 7d"
     * @param flagsWithValue     Set of single-char flags that expect a value, e.g. Set.of('d', 'e', 'c')
     * @param wordFlagsWithValue Set of word flags that expect a value, e.g. Set.of("duration", "delay")
     * @return ParsedFlags containing active flags, assigned values, and active word flags
     */
    public static ParsedFlags parseFlags(String input, Set<Character> flagsWithValue, Set<String> wordFlagsWithValue) {
        ParsedFlags parsed = new ParsedFlags();
        if (input == null || input.isEmpty()) return parsed;

        String[] args = input.trim().split("\\s+");

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (!arg.startsWith("-") || arg.length() < 2) continue;

            // Word flag: --skip or --duration 7d
            if (arg.startsWith("--")) {
                String word = arg.substring(2);
                parsed.activeWordFlags.add(word);
                if (wordFlagsWithValue.contains(word) && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    parsed.wordFlagValues.put(word, args[++i]);
                }
                continue;
            }

            if (arg.matches("-\\d+")) continue;

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
     * Removes all single-char flags and their values from the input string.
     *
     * @param input          The full command input, e.g. "/test sub -d 20 -es 50 hello world"
     * @param flagsWithValue Set of flags that expect a value, e.g. Set.of('d', 'e', 's')
     * @return A sanitized string with only non-flag arguments, e.g. "/test sub hello world"
     */
    public static String sanitizeInput(String input, Set<Character> flagsWithValue) {
        return sanitizeInput(input, flagsWithValue, Set.of());
    }

    /**
     * Removes all single-char flags, their values, and word flags from the input string.
     *
     * @param input          The full command input, e.g. "/test sub -d 20 hello --skip"
     * @param flagsWithValue Set of single-char flags that expect a value
     * @param wordFlagsToStrip Set of word flag names to strip (without --), e.g. Set.of("skip")
     * @return A sanitized string with only non-flag arguments
     */
    public static String sanitizeInput(String input, Set<Character> flagsWithValue, Set<String> wordFlagsToStrip) {
        return sanitizeInput(input, flagsWithValue, wordFlagsToStrip, Set.of());
    }

    /**
     * Removes all single-char flags, their values, word flags, and word flag values from the input string.
     *
     * @param input                  The full command input
     * @param flagsWithValue         Set of single-char flags that expect a value
     * @param wordFlagsToStrip       Set of word flag names to strip (without --)
     * @param wordFlagsWithValue     Set of word flags that expect a value (their value will also be stripped)
     * @return A sanitized string with only non-flag arguments
     */
    public static String sanitizeInput(String input, Set<Character> flagsWithValue, Set<String> wordFlagsToStrip, Set<String> wordFlagsWithValue) {
        if (input == null || input.isEmpty()) return "";

        String[] args = input.trim().split("\\s+");
        List<String> result = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // Word flag: --skip, --force, --duration 7d, etc.
            if (arg.startsWith("--") && arg.length() > 2) {
                String word = arg.substring(2);
                if (wordFlagsToStrip.isEmpty() || wordFlagsToStrip.contains(word)) {
                    // Also skip the value token if this word flag expects one
                    if (wordFlagsWithValue.contains(word) && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        i++;
                    }
                    continue; // strip it
                }
                result.add(arg);
                continue;
            }

            // Detect single-char flag tokens like "-a" or "-bc", but not negative numbers "-123"
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
        private final Set<String> activeWordFlags = new HashSet<>();
        private final Map<String, String> wordFlagValues = new HashMap<>();

        public boolean hasFlag(char flag) {
            return activeFlags.contains(flag) || flagValues.containsKey(flag);
        }

        public boolean hasFlag(char shortFlag, String longFlag) {
            return hasFlag(shortFlag) || hasWordFlag(longFlag);
        }

        public String getFlagValue(char flag) {
            return flagValues.get(flag);
        }

        public String getFlagValue(char shortFlag, String longFlag) {
            String val = getFlagValue(shortFlag);
            return val != null ? val : getWordFlagValue(longFlag);
        }

        public boolean hasWordFlag(String word) {
            return activeWordFlags.contains(word);
        }

        public String getWordFlagValue(String word) {
            return wordFlagValues.get(word);
        }
    }
}

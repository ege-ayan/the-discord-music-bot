package io.github.egeayan.musicbot.command;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class CommandParser {

    private final String prefix;

    public CommandParser(String prefix) {
        if (Objects.requireNonNull(prefix, "prefix").isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
    }

    public Optional<ParsedCommand> parse(String content) {
        String text = content.strip();
        if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.empty();
        }
        String rest = text.substring(prefix.length());
        if (rest.isEmpty() || Character.isWhitespace(rest.charAt(0))) {
            return Optional.empty();
        }
        int split = indexOfWhitespace(rest);
        String name = (split < 0 ? rest : rest.substring(0, split)).toLowerCase(Locale.ROOT);
        String args = split < 0 ? "" : rest.substring(split).strip();
        return Optional.of(new ParsedCommand(name, args));
    }

    private static int indexOfWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}

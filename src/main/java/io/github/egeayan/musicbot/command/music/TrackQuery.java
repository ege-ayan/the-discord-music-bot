package io.github.egeayan.musicbot.command.music;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class TrackQuery {

    private static final List<String> SEARCH_PREFIXES = List.of("ytsearch:", "ytmsearch:", "scsearch:");

    private TrackQuery() {}

    static Optional<String> input(String args, List<String> attachmentUrls) {
        String trimmed = args.strip();
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).strip();
        }
        if (!trimmed.isEmpty()) {
            return Optional.of(trimmed);
        }
        return attachmentUrls.stream().findFirst();
    }

    static String toIdentifier(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return input;
        }
        if (SEARCH_PREFIXES.stream().anyMatch(lower::startsWith)) {
            return input;
        }
        return "ytsearch:" + input;
    }
}

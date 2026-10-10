package io.github.egeayan.musicbot.command;

import io.github.egeayan.musicbot.music.Requester;
import io.github.egeayan.musicbot.music.TrackInfo;
import java.time.Duration;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

public final class Replies {

    public static final String SUCCESS = "🎶";
    public static final String WARNING = "💡";
    public static final String ERROR = "🚫";

    private Replies() {}

    public static String success(String message) {
        return SUCCESS + " " + message;
    }

    public static String warning(String message) {
        return WARNING + " " + message;
    }

    public static String error(String message) {
        return ERROR + " " + message;
    }

    public static String escape(String text) {
        return MarkdownSanitizer.escape(text);
    }

    public static String title(TrackInfo info) {
        return "**" + escape(info.title()) + "**";
    }

    public static String requester(Requester requester) {
        return "**" + escape(requester.displayName()) + "**";
    }

    public static String titleWithDuration(TrackInfo info) {
        return title(info) + " (`" + duration(info) + "`)";
    }

    public static String duration(TrackInfo info) {
        return info.stream() ? "LIVE" : duration(info.duration());
    }

    public static String duration(Duration duration) {
        long totalSeconds = Math.round(duration.toMillis() / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0 ? "%d:%02d:%02d".formatted(hours, minutes, seconds) : "%02d:%02d".formatted(minutes, seconds);
    }
}

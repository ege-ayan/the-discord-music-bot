package io.github.egeayan.musicbot.music;

import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record TrackInfo(
        String title, String author, @Nullable String uri, Duration duration, boolean stream) {

    public TrackInfo {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(duration, "duration");
    }
}

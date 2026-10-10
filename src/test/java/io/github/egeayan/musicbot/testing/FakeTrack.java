package io.github.egeayan.musicbot.testing;

import io.github.egeayan.musicbot.music.Track;
import io.github.egeayan.musicbot.music.TrackInfo;
import java.time.Duration;

public final class FakeTrack implements Track {

    private final TrackInfo info;

    public FakeTrack(String title) {
        this(title, Duration.ofMinutes(3));
    }

    public FakeTrack(String title, Duration duration) {
        this.info = new TrackInfo(title, "Artist", "https://example.com/" + title, duration, false);
    }

    @Override
    public TrackInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return "FakeTrack[" + info.title() + "]";
    }
}

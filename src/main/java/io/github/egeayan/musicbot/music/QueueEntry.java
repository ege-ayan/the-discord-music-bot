package io.github.egeayan.musicbot.music;

import java.util.Objects;

public record QueueEntry(Track track, Requester requester) {

    public QueueEntry {
        Objects.requireNonNull(track, "track");
        Objects.requireNonNull(requester, "requester");
    }

    public TrackInfo info() {
        return track.info();
    }
}

package io.github.egeayan.musicbot.music;

import java.util.List;
import java.util.Objects;

public sealed interface LoadResult {

    record Single(Track track) implements LoadResult {
        public Single {
            Objects.requireNonNull(track, "track");
        }
    }

    record Playlist(String name, List<Track> tracks) implements LoadResult {
        public Playlist {
            Objects.requireNonNull(name, "name");
            tracks = List.copyOf(tracks);
            if (tracks.isEmpty()) {
                throw new IllegalArgumentException("playlist must not be empty");
            }
        }
    }

    record NoMatches() implements LoadResult {}

    record Failed(String message) implements LoadResult {
        public Failed {
            Objects.requireNonNull(message, "message");
        }
    }
}

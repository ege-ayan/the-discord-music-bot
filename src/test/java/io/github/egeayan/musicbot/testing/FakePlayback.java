package io.github.egeayan.musicbot.testing;

import io.github.egeayan.musicbot.music.AudioPlayback;
import io.github.egeayan.musicbot.music.Track;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class FakePlayback implements AudioPlayback {

    private final List<Track> played = new ArrayList<>();
    private @Nullable Track playing;
    private int stops;

    @Override
    public void play(Track track) {
        played.add(track);
        playing = track;
    }

    @Override
    public void stop() {
        playing = null;
        stops++;
    }

    public List<Track> played() {
        return List.copyOf(played);
    }

    public @Nullable Track playing() {
        return playing;
    }

    public int stops() {
        return stops;
    }
}

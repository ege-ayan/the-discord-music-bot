package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.github.egeayan.musicbot.music.Track;
import io.github.egeayan.musicbot.music.TrackInfo;
import java.time.Duration;
import java.util.Objects;

public record LavaplayerTrack(AudioTrack audioTrack) implements Track {

    public LavaplayerTrack {
        Objects.requireNonNull(audioTrack, "audioTrack");
    }

    @Override
    public TrackInfo info() {
        AudioTrackInfo info = audioTrack.getInfo();
        return new TrackInfo(
                blankToDefault(info.title, "Unknown title"),
                blankToDefault(info.author, "Unknown artist"),
                info.uri,
                info.isStream ? Duration.ZERO : Duration.ofMillis(audioTrack.getDuration()),
                info.isStream);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

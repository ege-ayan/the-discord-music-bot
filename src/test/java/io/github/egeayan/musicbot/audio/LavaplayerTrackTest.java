package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.github.egeayan.musicbot.music.TrackInfo;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LavaplayerTrackTest {

    private static AudioTrack trackWith(AudioTrackInfo info, long durationMs) {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);
        when(track.getDuration()).thenReturn(durationMs);
        return track;
    }

    @Test
    void mapsMetadata() {
        var info = new AudioTrackInfo("Title", "Author", 213_000, "id", false, "https://youtu.be/id");

        assertThat(new LavaplayerTrack(trackWith(info, 213_000)).info())
                .isEqualTo(new TrackInfo("Title", "Author", "https://youtu.be/id", Duration.ofSeconds(213), false));
    }

    @Test
    void fillsInMissingTitleAndAuthor() {
        var info = new AudioTrackInfo(" ", null, 1_000, "id", false, null);

        TrackInfo mapped = new LavaplayerTrack(trackWith(info, 1_000)).info();

        assertThat(mapped.title()).isEqualTo("Unknown title");
        assertThat(mapped.author()).isEqualTo("Unknown artist");
    }

    @Test
    void streamsHaveNoDuration() {
        var info = new AudioTrackInfo("Radio", "Station", Long.MAX_VALUE, "id", true, "https://radio");

        TrackInfo mapped = new LavaplayerTrack(trackWith(info, Long.MAX_VALUE)).info();

        assertThat(mapped.stream()).isTrue();
        assertThat(mapped.duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void equalityIsIdentityOfWrappedTrack() {
        AudioTrack a = mock(AudioTrack.class);
        AudioTrack b = mock(AudioTrack.class);

        assertThat(new LavaplayerTrack(a)).isEqualTo(new LavaplayerTrack(a)).isNotEqualTo(new LavaplayerTrack(b));
    }
}

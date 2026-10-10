package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class YoutubeFallbackSourceManagerTest {

    private final AudioSourceManager youtube = mock(AudioSourceManager.class);
    private final YoutubeFallbackSourceManager source =
            new YoutubeFallbackSourceManager(youtube, new YtDlpResolver("yt-dlp", Duration.ofSeconds(1)));
    private final AudioPlayerManager manager = mock(AudioPlayerManager.class);
    private final AudioReference reference = new AudioReference("ytsearch:song", null);

    @AfterEach
    void shutdown() {
        source.shutdown();
    }

    private static AudioTrack track(String id) {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo())
                .thenReturn(new AudioTrackInfo(id, "artist", 1000, id, false, "https://www.youtube.com/watch?v=" + id));
        return track;
    }

    private void youtubeReturns(AudioItem item) {
        when(youtube.loadItem(any(), any())).thenReturn(item);
    }

    @Test
    void wrapsSingleTracksAndKeepsTheirInfo() {
        AudioTrack original = track("a");
        youtubeReturns(original);

        AudioItem item = source.loadItem(manager, reference);

        assertThat(item).isInstanceOf(YoutubeFallbackTrack.class);
        assertThat(((AudioTrack) item).getInfo()).isSameAs(original.getInfo());
        assertThat(((AudioTrack) item).getSourceManager()).isSameAs(source);
    }

    @Test
    void wrapsEveryPlaylistTrackAndPreservesSelectionAndSearchFlag() {
        AudioTrack first = track("a");
        AudioTrack second = track("b");
        youtubeReturns(new BasicAudioPlaylist("Search results", List.of(first, second), second, true));

        var playlist = (AudioPlaylist) source.loadItem(manager, reference);

        assertThat(playlist.getName()).isEqualTo("Search results");
        assertThat(playlist.isSearchResult()).isTrue();
        assertThat(playlist.getTracks())
                .hasSize(2)
                .allSatisfy(t -> assertThat(t).isInstanceOf(YoutubeFallbackTrack.class));
        assertThat(playlist.getSelectedTrack()).isSameAs(playlist.getTracks().get(1));
    }

    @Test
    void keepsPlaylistsWithoutASelection() {
        youtubeReturns(new BasicAudioPlaylist("Mix", List.of(track("a")), null, false));

        var playlist = (AudioPlaylist) source.loadItem(manager, reference);

        assertThat(playlist.getSelectedTrack()).isNull();
    }

    @Test
    void passesThroughOtherResults() {
        var redirect = new AudioReference("https://example.com", null);
        youtubeReturns(redirect);

        assertThat(source.loadItem(manager, reference)).isSameAs(redirect);
    }

    @Test
    void passesThroughNoMatches() {
        assertThat(source.loadItem(manager, reference)).isNull();
    }

    @Test
    void clonesStayWrapped() {
        youtubeReturns(track("a"));

        var track = (AudioTrack) source.loadItem(manager, reference);

        assertThat(track.makeClone()).isInstanceOf(YoutubeFallbackTrack.class).isNotSameAs(track);
    }

    @Test
    void usesTheYoutubeSourceName() {
        when(youtube.getSourceName()).thenReturn("youtube");

        assertThat(source.getSourceName()).isEqualTo("youtube");
    }
}

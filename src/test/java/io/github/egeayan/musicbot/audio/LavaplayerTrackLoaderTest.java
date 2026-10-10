package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import io.github.egeayan.musicbot.music.LoadResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LavaplayerTrackLoaderTest {

    @Mock
    AudioPlayerManager playerManager;

    @Mock
    AudioTrack first;

    @Mock
    AudioTrack second;

    @Captor
    ArgumentCaptor<AudioLoadResultHandler> handler;

    private CompletableFuture<LoadResult> load() {
        var future = new LavaplayerTrackLoader(playerManager).load(42L, "query");
        verify(playerManager).loadItemOrdered(eq(42L), eq("query"), handler.capture());
        return future;
    }

    @Test
    void singleTrack() {
        var future = load();

        handler.getValue().trackLoaded(first);

        assertThat(future).isCompletedWithValue(new LoadResult.Single(new LavaplayerTrack(first)));
    }

    @Test
    void searchResultPicksTopHit() {
        var future = load();

        handler.getValue().playlistLoaded(new BasicAudioPlaylist("Search", List.of(first, second), null, true));

        assertThat(future).isCompletedWithValue(new LoadResult.Single(new LavaplayerTrack(first)));
    }

    @Test
    void playlistLinkWithSelectedTrackPlaysOnlyThatTrack() {
        var future = load();

        handler.getValue().playlistLoaded(new BasicAudioPlaylist("Mix", List.of(first, second), second, false));

        assertThat(future).isCompletedWithValue(new LoadResult.Single(new LavaplayerTrack(second)));
    }

    @Test
    void fullPlaylist() {
        var future = load();

        handler.getValue().playlistLoaded(new BasicAudioPlaylist("Mix", List.of(first, second), null, false));

        assertThat(future)
                .isCompletedWithValue(new LoadResult.Playlist(
                        "Mix", List.of(new LavaplayerTrack(first), new LavaplayerTrack(second))));
    }

    @Test
    void emptyPlaylistCountsAsNoMatches() {
        var future = load();

        handler.getValue().playlistLoaded(new BasicAudioPlaylist("Empty", List.of(), null, false));

        assertThat(future).isCompletedWithValue(new LoadResult.NoMatches());
    }

    @Test
    void noMatches() {
        var future = load();

        handler.getValue().noMatches();

        assertThat(future).isCompletedWithValue(new LoadResult.NoMatches());
    }

    @Test
    void loadFailureCarriesFriendlyMessage() {
        var future = load();

        handler.getValue()
                .loadFailed(new FriendlyException("This video is private", FriendlyException.Severity.COMMON, null));

        assertThat(future).isCompletedWithValue(new LoadResult.Failed("This video is private"));
    }
}

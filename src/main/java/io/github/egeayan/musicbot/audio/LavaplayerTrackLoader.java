package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.egeayan.musicbot.music.LoadResult;
import io.github.egeayan.musicbot.music.Track;
import io.github.egeayan.musicbot.music.TrackLoader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class LavaplayerTrackLoader implements TrackLoader {

    private final AudioPlayerManager playerManager;

    public LavaplayerTrackLoader(AudioPlayerManager playerManager) {
        this.playerManager = Objects.requireNonNull(playerManager, "playerManager");
    }

    @Override
    public CompletableFuture<LoadResult> load(long guildId, String identifier) {
        var result = new CompletableFuture<LoadResult>();
        playerManager.loadItemOrdered(guildId, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                result.complete(new LoadResult.Single(new LavaplayerTrack(track)));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                result.complete(fromPlaylist(playlist));
            }

            @Override
            public void noMatches() {
                result.complete(new LoadResult.NoMatches());
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                result.complete(
                        new LoadResult.Failed(Objects.requireNonNullElse(exception.getMessage(), "Unknown error")));
            }
        });
        return result;
    }

    private static LoadResult fromPlaylist(AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        if (tracks.isEmpty()) {
            return new LoadResult.NoMatches();
        }
        AudioTrack selected = playlist.getSelectedTrack();
        if (selected != null) {
            return new LoadResult.Single(new LavaplayerTrack(selected));
        }
        if (playlist.isSearchResult()) {
            return new LoadResult.Single(new LavaplayerTrack(tracks.getFirst()));
        }
        List<Track> wrapped = tracks.stream().<Track>map(LavaplayerTrack::new).toList();
        return new LoadResult.Playlist(Objects.requireNonNullElse(playlist.getName(), "Untitled playlist"), wrapped);
    }
}

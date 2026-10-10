package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YoutubeFallbackSourceManager implements AudioSourceManager {

    private static final Logger LOG = LoggerFactory.getLogger(YoutubeFallbackSourceManager.class);

    private final AudioSourceManager youtube;
    private final YtDlpResolver resolver;
    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

    public YoutubeFallbackSourceManager(AudioSourceManager youtube, YtDlpResolver resolver) {
        this.youtube = Objects.requireNonNull(youtube, "youtube");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public String getSourceName() {
        return youtube.getSourceName();
    }

    @Override
    public @Nullable AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        return wrap(youtube.loadItem(manager, reference));
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        throw new UnsupportedOperationException("Tracks from this source cannot be encoded");
    }

    @Override
    public @Nullable AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        AudioTrack decoded = youtube.decodeTrack(trackInfo, input);
        return decoded == null ? null : new YoutubeFallbackTrack(decoded, this);
    }

    @Override
    public void shutdown() {
        youtube.shutdown();
        try {
            httpInterfaceManager.close();
        } catch (IOException e) {
            LOG.debug("Could not close the yt-dlp HTTP client", e);
        }
    }

    YtDlpResolver resolver() {
        return resolver;
    }

    HttpInterface httpInterface() {
        return httpInterfaceManager.getInterface();
    }

    @Nullable
    AudioItem wrap(@Nullable AudioItem item) {
        return switch (item) {
            case AudioTrack track -> new YoutubeFallbackTrack(track, this);
            case AudioPlaylist playlist -> wrap(playlist);
            case null, default -> item;
        };
    }

    private AudioPlaylist wrap(AudioPlaylist playlist) {
        List<AudioTrack> original = playlist.getTracks();
        List<AudioTrack> wrapped = original.stream()
                .<AudioTrack>map(track -> new YoutubeFallbackTrack(track, this))
                .toList();
        AudioTrack selectedTrack = playlist.getSelectedTrack();
        int selected = selectedTrack == null ? -1 : original.indexOf(selectedTrack);
        return new BasicAudioPlaylist(
                playlist.getName(), wrapped, selected < 0 ? null : wrapped.get(selected), playlist.isSearchResult());
    }
}

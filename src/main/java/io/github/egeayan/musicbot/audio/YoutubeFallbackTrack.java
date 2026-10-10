package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class YoutubeFallbackTrack extends DelegatedAudioTrack {

    private static final Logger LOG = LoggerFactory.getLogger(YoutubeFallbackTrack.class);

    private final AudioTrack youtube;
    private final YoutubeFallbackSourceManager source;

    YoutubeFallbackTrack(AudioTrack youtube, YoutubeFallbackSourceManager source) {
        super(youtube.getInfo());
        this.youtube = youtube;
        this.source = source;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try {
            processDelegate((InternalAudioTrack) youtube.makeClone(), executor);
        } catch (Exception youtubeFailure) {
            if (Thread.currentThread().isInterrupted() || getPosition() > 0) {
                throw youtubeFailure;
            }
            LOG.info(
                    "YouTube refused {}, falling back to yt-dlp: {}",
                    trackInfo.identifier,
                    TrackEndForwarder.summarize(youtubeFailure.getMessage()));
            try {
                processWithYtDlp(executor);
            } catch (Exception fallbackFailure) {
                var failure = new FriendlyException(
                        "YouTube refused to play this track.", FriendlyException.Severity.SUSPICIOUS, youtubeFailure);
                failure.addSuppressed(fallbackFailure);
                throw failure;
            }
        }
    }

    private void processWithYtDlp(LocalAudioTrackExecutor executor) throws Exception {
        String url = trackInfo.uri != null ? trackInfo.uri : "https://www.youtube.com/watch?v=" + trackInfo.identifier;
        YtDlpResolver.ResolvedStream stream = source.resolver().resolve(url);
        try (HttpInterface http = source.httpInterface();
                var input = new PersistentHttpStream(http, stream.uri(), Units.CONTENT_LENGTH_UNKNOWN)) {
            InternalAudioTrack track = switch (stream.extension()) {
                case "webm" -> new MatroskaAudioTrack(trackInfo, input);
                case "m4a", "mp4" -> new MpegAudioTrack(trackInfo, input);
                default -> throw new IOException("Unsupported yt-dlp container " + stream.extension());
            };
            processDelegate(track, executor);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new YoutubeFallbackTrack(youtube, source);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return source;
    }
}

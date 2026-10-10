package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import io.github.egeayan.musicbot.music.Track;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrackEndForwarder extends AudioEventAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TrackEndForwarder.class);
    private static final int MAX_REASON_LENGTH = 300;

    private final Consumer<Track> onTrackFinished;
    private final BiConsumer<Track, String> onTrackFailed;
    private final Executor executor;

    public TrackEndForwarder(
            Consumer<Track> onTrackFinished, BiConsumer<Track, String> onTrackFailed, Executor executor) {
        this.onTrackFinished = Objects.requireNonNull(onTrackFinished, "onTrackFinished");
        this.onTrackFailed = Objects.requireNonNull(onTrackFailed, "onTrackFailed");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            forward(track);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOG.warn("Track {} failed to play", track.getIdentifier(), exception);
        String reason = summarize(exception.getMessage());
        run(() -> onTrackFailed.accept(new LavaplayerTrack(track), reason));
    }

    static String summarize(@Nullable String message) {
        String firstLine = message == null
                ? ""
                : message.lines()
                        .map(line -> line.replaceFirst("^\\(yts\\.version:[^)]*\\)", "")
                                .strip())
                        .filter(line -> !line.isEmpty())
                        .findFirst()
                        .orElse("");
        if (firstLine.isEmpty()) {
            return "Unknown error";
        }
        return firstLine.length() <= MAX_REASON_LENGTH
                ? firstLine
                : firstLine.substring(0, MAX_REASON_LENGTH - 1) + "…";
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOG.warn("Track {} got stuck for {} ms, skipping it", track.getIdentifier(), thresholdMs);
        forward(track);
    }

    private void forward(AudioTrack track) {
        run(() -> onTrackFinished.accept(new LavaplayerTrack(track)));
    }

    private void run(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                LOG.error("Track event handler failed", e);
            }
        });
    }
}

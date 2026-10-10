package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import io.github.egeayan.musicbot.music.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrackEndForwarderTest {

    private final List<Track> finished = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private final Executor deferred = scheduled::add;
    private final TrackEndForwarder forwarder =
            new TrackEndForwarder(finished::add, (failed, reason) -> failures.add(failed + ": " + reason), deferred);
    private final AudioPlayer player = mock(AudioPlayer.class);
    private final AudioTrack track = mock(AudioTrack.class);

    private void runScheduled() {
        scheduled.forEach(Runnable::run);
        scheduled.clear();
    }

    @ParameterizedTest
    @EnumSource(names = {"FINISHED", "LOAD_FAILED"})
    void forwardsNaturalEnds(AudioTrackEndReason reason) {
        forwarder.onTrackEnd(player, track, reason);
        runScheduled();

        assertThat(finished).containsExactly(new LavaplayerTrack(track));
    }

    @ParameterizedTest
    @EnumSource(names = {"STOPPED", "REPLACED", "CLEANUP"})
    void ignoresEndsCausedByTheSession(AudioTrackEndReason reason) {
        forwarder.onTrackEnd(player, track, reason);
        runScheduled();

        assertThat(finished).isEmpty();
    }

    @Test
    void stuckTrackIsTreatedAsFinished() {
        forwarder.onTrackStuck(player, track, 10_000);
        runScheduled();

        assertThat(finished).containsExactly(new LavaplayerTrack(track));
    }

    @Test
    void reportsPlaybackFailuresWithTheirReason() {
        forwarder.onTrackException(player, track, new FriendlyException("broken", Severity.SUSPICIOUS, null));

        assertThat(failures).isEmpty();
        runScheduled();

        assertThat(failures).containsExactly(new LavaplayerTrack(track) + ": broken");
        assertThat(finished).isEmpty();
    }

    @Test
    void summarizesMultiLineFailuresToTheirFirstMeaningfulLine() {
        String message = """
                (yts.version: 1.18.2) All clients failed to load the item.

                Client [WEB] failed: No supported audio streams available
                        at dev.lavalink.youtube.track.format.TrackFormats.getBestFormat(TrackFormats.java:47)
                """;

        assertThat(TrackEndForwarder.summarize(message)).isEqualTo("All clients failed to load the item.");
    }

    @Test
    void capsVeryLongFailureReasons() {
        assertThat(TrackEndForwarder.summarize("x".repeat(5000))).hasSize(300).endsWith("…");
    }

    @Test
    void fallsBackWhenTheFailureHasNoMessage() {
        assertThat(TrackEndForwarder.summarize(null)).isEqualTo("Unknown error");
        assertThat(TrackEndForwarder.summarize("  \n ")).isEqualTo("Unknown error");
    }

    @Test
    void handlerFailuresDoNotEscapeToTheExecutor() {
        var failing = new TrackEndForwarder(
                _ -> {
                    throw new IllegalStateException("boom");
                },
                (_, _) -> {},
                deferred);
        failing.onTrackEnd(player, track, AudioTrackEndReason.FINISHED);

        runScheduled();
    }

    @Test
    void neverCallsTheSessionOnTheAudioThread() {
        forwarder.onTrackEnd(player, track, AudioTrackEndReason.FINISHED);

        assertThat(finished).isEmpty();
        assertThat(scheduled).hasSize(1);
    }
}

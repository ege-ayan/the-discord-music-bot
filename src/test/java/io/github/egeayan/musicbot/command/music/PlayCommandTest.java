package io.github.egeayan.musicbot.command.music;

import static io.github.egeayan.musicbot.testing.FakeCommandContext.MUSIC_CHANNEL;
import static io.github.egeayan.musicbot.testing.FakeCommandContext.OTHER_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.command.MemberVoice;
import io.github.egeayan.musicbot.music.LoadResult;
import io.github.egeayan.musicbot.music.Track;
import io.github.egeayan.musicbot.testing.FakeCommandContext;
import io.github.egeayan.musicbot.testing.FakeTrack;
import io.github.egeayan.musicbot.testing.FakeTrackLoader;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayCommandTest {

    private final FakeTrackLoader loader = new FakeTrackLoader();
    private final PlayCommand command = new PlayCommand(loader, "!");

    @Test
    void searchesYouTubeJoinsVoiceAndStartsPlaying() {
        var track = new FakeTrack("Never Gonna Give You Up", Duration.ofSeconds(213));
        loader.willReturn("ytsearch:rick astley", new LoadResult.Single(track));
        var ctx = FakeCommandContext.create().withArgs("rick astley");

        command.execute(ctx);

        assertThat(ctx.connectAttempts()).containsExactly(MUSIC_CHANNEL);
        assertThat(ctx.session().current())
                .hasValueSatisfying(e -> assertThat(e.track()).isSameAs(track));
        assertThat(ctx.lastReply()).isEqualTo("🎶 Added **Never Gonna Give You Up** (`03:33`) to begin playing");
    }

    @Test
    void queuesBehindCurrentTrackWithoutReconnecting() {
        loader.willReturn("ytsearch:first", new LoadResult.Single(new FakeTrack("first")));
        loader.willReturn("ytsearch:second", new LoadResult.Single(new FakeTrack("second")));
        var ctx = FakeCommandContext.create();
        command.execute(ctx.withArgs("first"));

        command.execute(ctx.withArgs("second"));

        assertThat(ctx.connectAttempts()).hasSize(1);
        assertThat(ctx.lastReply()).contains("to the queue at position 1");
    }

    @Test
    void urlsArePassedThroughUnchanged() {
        var url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        loader.willReturn(url, new LoadResult.Single(new FakeTrack("rick")));

        command.execute(FakeCommandContext.create().withArgs("<" + url + ">"));

        assertThat(loader.requested()).containsExactly(url);
    }

    @Test
    void playsAttachmentWhenNoArguments() {
        loader.willReturn("https://cdn.discordapp.com/song.mp3", new LoadResult.Single(new FakeTrack("song")));
        var ctx = FakeCommandContext.create().withAttachments("https://cdn.discordapp.com/song.mp3");

        command.execute(ctx);

        assertThat(ctx.session().current()).isPresent();
    }

    @Test
    void enqueuesWholePlaylist() {
        List<Track> tracks = List.of(new FakeTrack("a"), new FakeTrack("b"), new FakeTrack("c"));
        loader.willReturn("https://yt/playlist", new LoadResult.Playlist("Mix", tracks));
        var ctx = FakeCommandContext.create().withArgs("https://yt/playlist");

        command.execute(ctx);

        assertThat(ctx.session().current()).isPresent();
        assertThat(ctx.session().upcoming()).hasSize(2);
        assertThat(ctx.lastReply()).isEqualTo("🎶 Found playlist **Mix** with `3` entries; added to the queue!");
    }

    @Test
    void showsUsageWithoutInput() {
        var ctx = FakeCommandContext.create();

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("💡 Usage: `!play <song title | URL>`");
        assertThat(loader.requested()).isEmpty();
    }

    @Test
    void requiresListeningBeforeLoading() {
        var ctx = FakeCommandContext.create()
                .withArgs("song")
                .withBotIn(MUSIC_CHANNEL)
                .withAuthorVoice(new MemberVoice(OTHER_CHANNEL, false));

        command.execute(ctx);

        assertThat(loader.requested()).isEmpty();
        assertThat(ctx.lastReply()).startsWith("🚫 You must be listening in #music");
    }

    @Test
    void reportsNoMatchesWithoutJoiningVoice() {
        var ctx = FakeCommandContext.create().withArgs("asdfghjkl");

        command.execute(ctx);

        assertThat(ctx.connectAttempts()).isEmpty();
        assertThat(ctx.lastReply()).isEqualTo("💡 No results found for `asdfghjkl`.");
    }

    @Test
    void reportsLoadFailures() {
        loader.willReturn("ytsearch:blocked", new LoadResult.Failed("This video is unavailable"));
        var ctx = FakeCommandContext.create().withArgs("blocked");

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("🚫 Error loading track: This video is unavailable");
        assertThat(ctx.session().current()).isEmpty();
    }

    @Test
    void reportsUnexpectedLoaderErrors() {
        loader.willFail("ytsearch:boom", new IllegalStateException("boom"));
        var ctx = FakeCommandContext.create().withArgs("boom");

        command.execute(ctx);

        assertThat(ctx.lastReply()).startsWith("🚫 Something went wrong");
    }

    @Test
    void doesNotEnqueueWhenVoiceConnectionIsDenied() {
        loader.willReturn("ytsearch:song", new LoadResult.Single(new FakeTrack("song")));
        var ctx = FakeCommandContext.create().withArgs("song").withConnectAllowed(false);

        command.execute(ctx);

        assertThat(ctx.session().current()).isEmpty();
        assertThat(ctx.lastReply()).isEqualTo("🚫 I am unable to connect to #music!");
    }
}

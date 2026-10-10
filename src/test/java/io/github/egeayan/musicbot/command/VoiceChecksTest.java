package io.github.egeayan.musicbot.command;

import static io.github.egeayan.musicbot.testing.FakeCommandContext.MUSIC_CHANNEL;
import static io.github.egeayan.musicbot.testing.FakeCommandContext.OTHER_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.testing.FakeCommandContext;
import org.junit.jupiter.api.Test;

class VoiceChecksTest {

    @Test
    void listeningInAnyChannelWhenBotIsDisconnected() {
        var ctx = FakeCommandContext.create().withAuthorVoice(new MemberVoice(OTHER_CHANNEL, false));

        assertThat(VoiceChecks.requireListening(ctx)).isTrue();
        assertThat(ctx.replies()).isEmpty();
    }

    @Test
    void mustShareChannelWithBot() {
        var ctx = FakeCommandContext.create()
                .withBotIn(MUSIC_CHANNEL)
                .withAuthorVoice(new MemberVoice(OTHER_CHANNEL, false));

        assertThat(VoiceChecks.requireListening(ctx)).isFalse();
        assertThat(ctx.lastReply()).contains("#music");
    }

    @Test
    void deafenedUsersAreNotListening() {
        var ctx = FakeCommandContext.create().withAuthorVoice(new MemberVoice(MUSIC_CHANNEL, true));

        assertThat(VoiceChecks.requireListening(ctx)).isFalse();
        assertThat(ctx.lastReply()).contains("a voice channel");
    }

    @Test
    void usersOutsideVoiceAreNotListening() {
        var ctx = FakeCommandContext.create().withAuthorVoice(null);

        assertThat(VoiceChecks.requireListening(ctx)).isFalse();
    }

    @Test
    void requirePlayingFailsWhenIdle() {
        var ctx = FakeCommandContext.create();

        assertThat(VoiceChecks.requirePlaying(ctx)).isFalse();
        assertThat(ctx.lastReply()).contains("There must be music playing");
    }
}

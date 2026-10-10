package io.github.egeayan.musicbot.command.music;

import static io.github.egeayan.musicbot.testing.FakeCommandContext.MUSIC_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.egeayan.musicbot.music.QueueEntry;
import io.github.egeayan.musicbot.testing.FakeCommandContext;
import io.github.egeayan.musicbot.testing.FakeTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkipCommandTest {

    private final SkipCommand command = new SkipCommand(0.55);
    private FakeCommandContext alice;

    @BeforeEach
    void aliceIsPlayingSomething() {
        alice = FakeCommandContext.create().withBotIn(MUSIC_CHANNEL).withListeners(1L, 2L, 3L, 4L);
        alice.session().enqueue(new QueueEntry(new FakeTrack("song"), alice.author()));
    }

    @Test
    void requesterSkipsOwnTrackImmediately() {
        command.execute(alice);

        assertThat(alice.lastReply()).isEqualTo("🎶 Skipped **song**");
        assertThat(alice.session().current()).isEmpty();
    }

    @Test
    void othersVoteUntilThresholdIsReached() {
        var bob = alice.asUser(2, "bob");
        var carol = alice.asUser(3, "carol");
        var dave = alice.asUser(4, "dave");

        command.execute(bob);
        command.execute(carol);
        assertThat(bob.lastReply()).isEqualTo("🎶 You voted to skip the song `[1 votes, 3/4 needed]`");
        assertThat(alice.session().current()).isPresent();

        command.execute(dave);
        assertThat(dave.lastReply())
                .isEqualTo("🎶 You voted to skip the song `[3 votes, 3/4 needed]`\n"
                        + "🎶 Skipped **song** (requested by **alice**)");
        assertThat(alice.session().current()).isEmpty();
    }

    @Test
    void warnsOnDuplicateVote() {
        var bob = alice.asUser(2, "bob");

        command.execute(bob);
        command.execute(bob);

        assertThat(bob.lastReply()).isEqualTo("💡 You already voted to skip this song `[1 votes, 3/4 needed]`");
    }

    @Test
    void requiresMusicPlaying() {
        var idle = FakeCommandContext.create();

        command.execute(idle);

        assertThat(idle.lastReply()).isEqualTo("🚫 There must be music playing to use that!");
    }

    @Test
    void requiresListening() {
        var outsider = alice.asUser(9, "eve").withAuthorVoice(null);

        command.execute(outsider);

        assertThat(outsider.lastReply()).startsWith("🚫 You must be listening in #music");
        assertThat(alice.session().current()).isPresent();
    }

    @Test
    void rejectsInvalidRatio() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SkipCommand(1.5));
    }
}

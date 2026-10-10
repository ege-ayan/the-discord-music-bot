package io.github.egeayan.musicbot.command.music;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.music.QueueEntry;
import io.github.egeayan.musicbot.music.Requester;
import io.github.egeayan.musicbot.testing.FakeCommandContext;
import io.github.egeayan.musicbot.testing.FakeTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveCommandTest {

    private static final Requester BOB = new Requester(2, "bob");

    private final RemoveCommand command = new RemoveCommand("!");
    private FakeCommandContext alice;

    @BeforeEach
    void queueWithTracksFromAliceAndBob() {
        alice = FakeCommandContext.create();
        alice.session().enqueue(new QueueEntry(new FakeTrack("playing"), alice.author()));
        alice.session().enqueue(new QueueEntry(new FakeTrack("alice-next"), alice.author()));
        alice.session().enqueue(new QueueEntry(new FakeTrack("bob-next"), BOB));
    }

    @Test
    void removesOwnTrack() {
        command.execute(alice.withArgs("1"));

        assertThat(alice.lastReply()).isEqualTo("🎶 Removed **alice-next** from the queue");
        assertThat(alice.session().upcoming()).extracting(e -> e.info().title()).containsExactly("bob-next");
    }

    @Test
    void cannotRemoveSomeoneElsesTrack() {
        command.execute(alice.withArgs("2"));

        assertThat(alice.lastReply()).isEqualTo("🚫 You cannot remove **bob-next** because you didn't add it!");
        assertThat(alice.session().upcoming()).hasSize(2);
    }

    @Test
    void djCanRemoveSomeoneElsesTrack() {
        command.execute(alice.withDj(true).withArgs("2"));

        assertThat(alice.lastReply()).isEqualTo("🎶 Removed **bob-next** from the queue (requested by **bob**)");
    }

    @Test
    void rejectsOutOfRangePosition() {
        command.execute(alice.withArgs("3"));

        assertThat(alice.lastReply()).isEqualTo("🚫 Position must be a valid integer between 1 and 2!");
    }

    @Test
    void rejectsNonNumericPosition() {
        command.execute(alice.withArgs("first"));

        assertThat(alice.lastReply()).isEqualTo("🚫 Usage: `!remove <position>`");
    }

    @Test
    void reportsEmptyQueue() {
        var ctx = FakeCommandContext.create().withArgs("1");

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("🚫 There is nothing in the queue!");
    }

    @Test
    void requiresListening() {
        command.execute(alice.withAuthorVoice(null).withArgs("1"));

        assertThat(alice.lastReply()).startsWith("🚫 You must be listening");
        assertThat(alice.session().upcoming()).hasSize(2);
    }
}

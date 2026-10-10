package io.github.egeayan.musicbot.command.music;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.music.QueueEntry;
import io.github.egeayan.musicbot.music.Requester;
import io.github.egeayan.musicbot.testing.FakeCommandContext;
import io.github.egeayan.musicbot.testing.FakeTrack;
import org.junit.jupiter.api.Test;

class ForceSkipCommandTest {

    private final ForceSkipCommand command = new ForceSkipCommand();

    @Test
    void djSkipsAnyTrack() {
        var ctx = FakeCommandContext.create().withDj(true);
        ctx.session().enqueue(new QueueEntry(new FakeTrack("first"), new Requester(7, "bob")));
        ctx.session().enqueue(new QueueEntry(new FakeTrack("second"), new Requester(7, "bob")));

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("🎶 Skipped **first** (requested by **bob**)");
        assertThat(ctx.session().current())
                .hasValueSatisfying(e -> assertThat(e.info().title()).isEqualTo("second"));
    }

    @Test
    void nonDjIsRejected() {
        var ctx = FakeCommandContext.create().withDj(false);
        ctx.session().enqueue(new QueueEntry(new FakeTrack("first"), new Requester(7, "bob")));

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("🚫 Only DJs can force skip!");
        assertThat(ctx.session().current()).isPresent();
    }

    @Test
    void requiresMusicPlaying() {
        var ctx = FakeCommandContext.create().withDj(true);

        command.execute(ctx);

        assertThat(ctx.lastReply()).isEqualTo("🚫 There must be music playing to use that!");
    }
}

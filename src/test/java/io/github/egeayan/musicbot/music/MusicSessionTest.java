package io.github.egeayan.musicbot.music;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.testing.FakePlayback;
import io.github.egeayan.musicbot.testing.FakeTrack;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MusicSessionTest {

    private static final Requester ALICE = new Requester(1, "alice");
    private static final Requester BOB = new Requester(2, "bob");
    private static final Requester CAROL = new Requester(3, "carol");

    private final FakePlayback playback = new FakePlayback();
    private final AtomicInteger idleCalls = new AtomicInteger();
    private final MusicSession session = new MusicSession(playback, idleCalls::incrementAndGet);

    private static QueueEntry entry(String title, Requester requester) {
        return new QueueEntry(new FakeTrack(title), requester);
    }

    @Nested
    class Enqueue {

        @Test
        void firstEntryStartsPlayingImmediately() {
            var first = entry("one", ALICE);

            assertThat(session.enqueue(first)).isEqualTo(new EnqueueResult.StartedPlaying(first));
            assertThat(playback.playing()).isSameAs(first.track());
            assertThat(session.current()).contains(first);
        }

        @Test
        void laterEntriesAreQueuedWithOneBasedPosition() {
            session.enqueue(entry("one", ALICE));
            var second = entry("two", ALICE);
            var third = entry("three", ALICE);

            assertThat(session.enqueue(second)).isEqualTo(new EnqueueResult.Queued(second, 1));
            assertThat(session.enqueue(third)).isEqualTo(new EnqueueResult.Queued(third, 2));
            assertThat(session.upcoming()).containsExactly(second, third);
        }

        @Test
        void queueIsFairAcrossRequesters() {
            session.enqueue(entry("a0", ALICE));
            var a1 = entry("a1", ALICE);
            var a2 = entry("a2", ALICE);
            var b1 = entry("b1", BOB);
            session.enqueue(a1);
            session.enqueue(a2);

            assertThat(session.enqueue(b1)).isEqualTo(new EnqueueResult.Queued(b1, 2));
            assertThat(session.upcoming()).containsExactly(a1, b1, a2);
        }
    }

    @Nested
    class TrackEnd {

        @Test
        void finishedTrackAdvancesToNext() {
            var first = entry("one", ALICE);
            var second = entry("two", BOB);
            session.enqueue(first);
            session.enqueue(second);

            session.onTrackFinished(first.track());

            assertThat(session.current()).contains(second);
            assertThat(playback.played()).containsExactly(first.track(), second.track());
        }

        @Test
        void finishingLastTrackGoesIdle() {
            var only = entry("one", ALICE);
            session.enqueue(only);

            session.onTrackFinished(only.track());

            assertThat(session.current()).isEmpty();
            assertThat(playback.stops()).isEqualTo(1);
            assertThat(idleCalls).hasValue(1);
        }

        @Test
        void staleFinishNotificationIsIgnored() {
            var first = entry("one", ALICE);
            var second = entry("two", BOB);
            var third = entry("three", CAROL);
            session.enqueue(first);
            session.enqueue(second);
            session.enqueue(third);
            session.skip();

            session.onTrackFinished(first.track());

            assertThat(session.current()).contains(second);
            assertThat(session.upcoming()).containsExactly(third);
        }

        @Test
        void playbackRestartsAfterIdle() {
            var first = entry("one", ALICE);
            session.enqueue(first);
            session.onTrackFinished(first.track());
            var next = entry("two", ALICE);

            assertThat(session.enqueue(next)).isInstanceOf(EnqueueResult.StartedPlaying.class);
            assertThat(playback.playing()).isSameAs(next.track());
        }
    }

    @Nested
    class Skip {

        @Test
        void skipReturnsSkippedEntryAndPlaysNext() {
            var first = entry("one", ALICE);
            var second = entry("two", BOB);
            session.enqueue(first);
            session.enqueue(second);

            assertThat(session.skip()).contains(first);
            assertThat(session.current()).contains(second);
        }

        @Test
        void skippingLastTrackGoesIdle() {
            session.enqueue(entry("one", ALICE));

            session.skip();

            assertThat(session.current()).isEmpty();
            assertThat(idleCalls).hasValue(1);
        }

        @Test
        void skipWithNothingPlayingDoesNothing() {
            assertThat(session.skip()).isEmpty();
            assertThat(playback.stops()).isZero();
            assertThat(idleCalls).hasValue(0);
        }
    }

    @Nested
    class VoteSkip {

        private final Set<Long> threeListeners = Set.of(ALICE.userId(), BOB.userId(), CAROL.userId());

        @Test
        void requesterSkipsInstantly() {
            var first = entry("one", ALICE);
            session.enqueue(first);

            var result = session.voteSkip(ALICE.userId(), threeListeners, 0.55);

            assertThat(result).isEqualTo(new VoteSkipResult.SkippedByRequester(first));
            assertThat(session.current()).isEmpty();
        }

        @Test
        void zeroRatioSkipsInstantly() {
            session.enqueue(entry("one", ALICE));

            assertThat(session.voteSkip(BOB.userId(), threeListeners, 0))
                    .isInstanceOf(VoteSkipResult.SkippedByRequester.class);
        }

        @Test
        void votesAccumulateUntilThreshold() {
            var first = entry("one", ALICE);
            session.enqueue(first);
            var fourListeners = Set.of(1L, 2L, 3L, 4L);

            assertThat(session.voteSkip(2, fourListeners, 0.55))
                    .isEqualTo(new VoteSkipResult.VoteRecorded(false, 1, 3, 4));
            assertThat(session.voteSkip(3, fourListeners, 0.55))
                    .isEqualTo(new VoteSkipResult.VoteRecorded(false, 2, 3, 4));
            assertThat(session.voteSkip(4, fourListeners, 0.55))
                    .isEqualTo(new VoteSkipResult.SkippedByVote(first, 3, 3, 4));
            assertThat(session.current()).isEmpty();
        }

        @Test
        void repeatedVoteIsNotCountedTwice() {
            session.enqueue(entry("one", ALICE));

            session.voteSkip(BOB.userId(), threeListeners, 1.0);
            var result = session.voteSkip(BOB.userId(), threeListeners, 1.0);

            assertThat(result).isEqualTo(new VoteSkipResult.VoteRecorded(true, 1, 3, 3));
        }

        @Test
        void votesFromUsersWhoLeftDoNotCount() {
            session.enqueue(entry("one", ALICE));
            session.voteSkip(BOB.userId(), threeListeners, 1.0);

            var result = session.voteSkip(CAROL.userId(), Set.of(ALICE.userId(), CAROL.userId()), 1.0);

            assertThat(result).isEqualTo(new VoteSkipResult.VoteRecorded(false, 1, 2, 2));
        }

        @Test
        void votesResetWhenTrackChanges() {
            var first = entry("one", ALICE);
            session.enqueue(first);
            session.enqueue(entry("two", ALICE));
            session.voteSkip(BOB.userId(), threeListeners, 1.0);

            session.onTrackFinished(first.track());
            var result = session.voteSkip(CAROL.userId(), threeListeners, 1.0);

            assertThat(result).isEqualTo(new VoteSkipResult.VoteRecorded(false, 1, 3, 3));
        }

        @Test
        void nothingPlaying() {
            assertThat(session.voteSkip(1, threeListeners, 0.5)).isEqualTo(new VoteSkipResult.NothingPlaying());
        }
    }

    @Nested
    class Remove {

        @Test
        void requesterCanRemoveOwnEntry() {
            session.enqueue(entry("playing", ALICE));
            var mine = entry("mine", BOB);
            session.enqueue(mine);

            assertThat(session.remove(1, BOB.userId(), false)).isEqualTo(new RemoveResult.Removed(mine));
            assertThat(session.upcoming()).isEmpty();
        }

        @Test
        void othersCannotRemoveWithoutPrivilege() {
            session.enqueue(entry("playing", ALICE));
            var bobs = entry("bobs", BOB);
            session.enqueue(bobs);

            assertThat(session.remove(1, CAROL.userId(), false)).isEqualTo(new RemoveResult.NotPermitted(bobs));
            assertThat(session.upcoming()).containsExactly(bobs);
        }

        @Test
        void privilegedUserCanRemoveAnyEntry() {
            session.enqueue(entry("playing", ALICE));
            var bobs = entry("bobs", BOB);
            session.enqueue(bobs);

            assertThat(session.remove(1, CAROL.userId(), true)).isEqualTo(new RemoveResult.Removed(bobs));
        }

        @Test
        void outOfRangePositionsAreRejected() {
            session.enqueue(entry("playing", ALICE));
            session.enqueue(entry("next", ALICE));

            assertThat(session.remove(0, ALICE.userId(), true)).isEqualTo(new RemoveResult.InvalidPosition(1));
            assertThat(session.remove(2, ALICE.userId(), true)).isEqualTo(new RemoveResult.InvalidPosition(1));
            assertThat(session.remove(-5, ALICE.userId(), true)).isEqualTo(new RemoveResult.InvalidPosition(1));
        }

        @Test
        void removingDoesNotAffectCurrentTrack() {
            var playing = entry("playing", ALICE);
            session.enqueue(playing);
            session.enqueue(entry("next", ALICE));

            session.remove(1, ALICE.userId(), false);

            assertThat(session.current()).contains(playing);
        }
    }
}

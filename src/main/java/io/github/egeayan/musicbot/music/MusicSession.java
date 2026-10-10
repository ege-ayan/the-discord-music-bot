package io.github.egeayan.musicbot.music;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class MusicSession {

    private final AudioPlayback playback;
    private final Runnable onIdle;
    private final FairQueue<QueueEntry> queue =
            new FairQueue<>(entry -> entry.requester().userId());
    private final Set<Long> skipVotes = new HashSet<>();

    private @Nullable QueueEntry current;

    public MusicSession(AudioPlayback playback, Runnable onIdle) {
        this.playback = Objects.requireNonNull(playback, "playback");
        this.onIdle = Objects.requireNonNull(onIdle, "onIdle");
    }

    public synchronized EnqueueResult enqueue(QueueEntry entry) {
        if (current == null) {
            start(entry);
            return new EnqueueResult.StartedPlaying(entry);
        }
        return new EnqueueResult.Queued(entry, queue.add(entry) + 1);
    }

    public synchronized Optional<QueueEntry> current() {
        return Optional.ofNullable(current);
    }

    public synchronized List<QueueEntry> upcoming() {
        return queue.snapshot();
    }

    public synchronized Optional<QueueEntry> skip() {
        QueueEntry skipped = current;
        if (skipped != null) {
            advance();
        }
        return Optional.ofNullable(skipped);
    }

    public synchronized VoteSkipResult voteSkip(long userId, Set<Long> listeners, double ratio) {
        QueueEntry playing = current;
        if (playing == null) {
            return new VoteSkipResult.NothingPlaying();
        }
        if (playing.requester().userId() == userId || ratio <= 0) {
            advance();
            return new VoteSkipResult.SkippedByRequester(playing);
        }

        boolean alreadyVoted = !skipVotes.add(userId);
        int votes = (int) skipVotes.stream().filter(listeners::contains).count();
        int required = (int) Math.ceil(listeners.size() * ratio);
        if (votes >= required) {
            advance();
            return new VoteSkipResult.SkippedByVote(playing, votes, required, listeners.size());
        }
        return new VoteSkipResult.VoteRecorded(alreadyVoted, votes, required, listeners.size());
    }

    public synchronized RemoveResult remove(int position, long userId, boolean privileged) {
        if (position < 1 || position > queue.size()) {
            return new RemoveResult.InvalidPosition(queue.size());
        }
        QueueEntry entry = queue.get(position - 1);
        if (!privileged && entry.requester().userId() != userId) {
            return new RemoveResult.NotPermitted(entry);
        }
        queue.remove(position - 1);
        return new RemoveResult.Removed(entry);
    }

    public synchronized void onTrackFinished(Track track) {
        if (current != null && current.track().equals(track)) {
            advance();
        }
    }

    private void advance() {
        Optional<QueueEntry> next = queue.poll();
        if (next.isPresent()) {
            start(next.get());
            return;
        }
        current = null;
        skipVotes.clear();
        playback.stop();
        onIdle.run();
    }

    private void start(QueueEntry entry) {
        current = entry;
        skipVotes.clear();
        playback.play(entry.track());
    }
}

package io.github.egeayan.musicbot.music;

public sealed interface VoteSkipResult {

    record NothingPlaying() implements VoteSkipResult {}

    record SkippedByRequester(QueueEntry skipped) implements VoteSkipResult {}

    record SkippedByVote(QueueEntry skipped, int votes, int required, int listeners) implements VoteSkipResult {}

    record VoteRecorded(boolean alreadyVoted, int votes, int required, int listeners) implements VoteSkipResult {}
}

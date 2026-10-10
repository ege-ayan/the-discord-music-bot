package io.github.egeayan.musicbot.music;

public sealed interface EnqueueResult {

    QueueEntry entry();

    record StartedPlaying(QueueEntry entry) implements EnqueueResult {}

    record Queued(QueueEntry entry, int position) implements EnqueueResult {}
}

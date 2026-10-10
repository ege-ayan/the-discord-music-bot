package io.github.egeayan.musicbot.music;

public sealed interface RemoveResult {

    record Removed(QueueEntry entry) implements RemoveResult {}

    record InvalidPosition(int queueSize) implements RemoveResult {}

    record NotPermitted(QueueEntry entry) implements RemoveResult {}
}

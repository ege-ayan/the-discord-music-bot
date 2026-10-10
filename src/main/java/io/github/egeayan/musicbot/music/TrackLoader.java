package io.github.egeayan.musicbot.music;

import java.util.concurrent.CompletableFuture;

public interface TrackLoader {

    CompletableFuture<LoadResult> load(long guildId, String identifier);
}

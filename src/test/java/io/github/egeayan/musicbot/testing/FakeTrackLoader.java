package io.github.egeayan.musicbot.testing;

import io.github.egeayan.musicbot.music.LoadResult;
import io.github.egeayan.musicbot.music.TrackLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class FakeTrackLoader implements TrackLoader {

    private final Map<String, CompletableFuture<LoadResult>> results = new HashMap<>();
    private final List<String> requested = new ArrayList<>();

    public FakeTrackLoader willReturn(String identifier, LoadResult result) {
        results.put(identifier, CompletableFuture.completedFuture(result));
        return this;
    }

    public FakeTrackLoader willFail(String identifier, Throwable error) {
        results.put(identifier, CompletableFuture.failedFuture(error));
        return this;
    }

    @Override
    public CompletableFuture<LoadResult> load(long guildId, String identifier) {
        requested.add(identifier);
        return results.getOrDefault(identifier, CompletableFuture.completedFuture(new LoadResult.NoMatches()));
    }

    public List<String> requested() {
        return List.copyOf(requested);
    }
}

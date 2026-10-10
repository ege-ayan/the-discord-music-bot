package io.github.egeayan.musicbot.music;

import java.util.Objects;

public record Requester(long userId, String displayName) {

    public Requester {
        Objects.requireNonNull(displayName, "displayName");
    }
}

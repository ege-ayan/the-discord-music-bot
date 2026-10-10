package io.github.egeayan.musicbot.command;

import java.util.Objects;

public record VoiceChannelRef(long id, String mention) {

    public VoiceChannelRef {
        Objects.requireNonNull(mention, "mention");
    }

    public boolean sameChannelAs(VoiceChannelRef other) {
        return id == other.id;
    }
}

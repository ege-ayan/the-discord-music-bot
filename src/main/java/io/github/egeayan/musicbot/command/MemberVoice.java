package io.github.egeayan.musicbot.command;

import java.util.Objects;

public record MemberVoice(VoiceChannelRef channel, boolean deafened) {

    public MemberVoice {
        Objects.requireNonNull(channel, "channel");
    }
}

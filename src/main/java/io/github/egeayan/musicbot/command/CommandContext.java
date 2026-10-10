package io.github.egeayan.musicbot.command;

import io.github.egeayan.musicbot.music.MusicSession;
import io.github.egeayan.musicbot.music.Requester;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommandContext {

    String args();

    List<String> attachmentUrls();

    Requester author();

    boolean authorIsDj();

    Optional<MemberVoice> authorVoice();

    Optional<VoiceChannelRef> selfVoiceChannel();

    Set<Long> listeners();

    boolean connect(VoiceChannelRef channel);

    long guildId();

    MusicSession session();

    void reply(String message);
}

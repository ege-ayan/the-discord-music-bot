package io.github.egeayan.musicbot.testing;

import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.MemberVoice;
import io.github.egeayan.musicbot.command.VoiceChannelRef;
import io.github.egeayan.musicbot.music.MusicSession;
import io.github.egeayan.musicbot.music.Requester;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class FakeCommandContext implements CommandContext {

    public static final VoiceChannelRef MUSIC_CHANNEL = new VoiceChannelRef(100, "#music");
    public static final VoiceChannelRef OTHER_CHANNEL = new VoiceChannelRef(200, "#other");

    private final MusicSession session;
    private final SharedVoice voice;
    private final List<String> replies = new ArrayList<>();

    private Requester author = new Requester(1, "alice");
    private String args = "";
    private List<String> attachments = List.of();
    private boolean dj;
    private @Nullable MemberVoice authorVoice = new MemberVoice(MUSIC_CHANNEL, false);

    private FakeCommandContext(MusicSession session, SharedVoice voice) {
        this.session = session;
        this.voice = voice;
    }

    public static FakeCommandContext create() {
        return new FakeCommandContext(new MusicSession(new FakePlayback(), () -> {}), new SharedVoice());
    }

    public static FakeCommandContext create(MusicSession session) {
        return new FakeCommandContext(session, new SharedVoice());
    }

    public FakeCommandContext asUser(long userId, String name) {
        var other = new FakeCommandContext(session, voice);
        other.author = new Requester(userId, name);
        other.authorVoice = authorVoice;
        return other;
    }

    public FakeCommandContext withArgs(String args) {
        this.args = args;
        return this;
    }

    public FakeCommandContext withAttachments(String... urls) {
        this.attachments = List.of(urls);
        return this;
    }

    public FakeCommandContext withDj(boolean dj) {
        this.dj = dj;
        return this;
    }

    public FakeCommandContext withAuthorVoice(@Nullable MemberVoice voice) {
        this.authorVoice = voice;
        return this;
    }

    public FakeCommandContext withBotIn(@Nullable VoiceChannelRef channel) {
        voice.selfChannel = channel;
        return this;
    }

    public FakeCommandContext withListeners(Long... userIds) {
        voice.listeners = Set.of(userIds);
        return this;
    }

    public FakeCommandContext withConnectAllowed(boolean allowed) {
        voice.connectAllowed = allowed;
        return this;
    }

    public List<String> replies() {
        return List.copyOf(replies);
    }

    public String lastReply() {
        return replies.getLast();
    }

    public List<VoiceChannelRef> connectAttempts() {
        return List.copyOf(voice.connectAttempts);
    }

    @Override
    public String args() {
        return args;
    }

    @Override
    public List<String> attachmentUrls() {
        return attachments;
    }

    @Override
    public Requester author() {
        return author;
    }

    @Override
    public boolean authorIsDj() {
        return dj;
    }

    @Override
    public Optional<MemberVoice> authorVoice() {
        return Optional.ofNullable(authorVoice);
    }

    @Override
    public Optional<VoiceChannelRef> selfVoiceChannel() {
        return Optional.ofNullable(voice.selfChannel);
    }

    @Override
    public Set<Long> listeners() {
        return voice.listeners;
    }

    @Override
    public boolean connect(VoiceChannelRef channel) {
        voice.connectAttempts.add(channel);
        if (voice.connectAllowed) {
            voice.selfChannel = channel;
        }
        return voice.connectAllowed;
    }

    @Override
    public long guildId() {
        return 42;
    }

    @Override
    public MusicSession session() {
        return session;
    }

    @Override
    public void reply(String message) {
        replies.add(message);
    }

    private static final class SharedVoice {
        private @Nullable VoiceChannelRef selfChannel;
        private Set<Long> listeners = Set.of();
        private boolean connectAllowed = true;
        private final List<VoiceChannelRef> connectAttempts = new ArrayList<>();
    }
}

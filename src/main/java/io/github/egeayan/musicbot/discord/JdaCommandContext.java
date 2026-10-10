package io.github.egeayan.musicbot.discord;

import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.DjPolicy;
import io.github.egeayan.musicbot.command.MemberVoice;
import io.github.egeayan.musicbot.command.ParsedCommand;
import io.github.egeayan.musicbot.command.VoiceChannelRef;
import io.github.egeayan.musicbot.music.MusicSession;
import io.github.egeayan.musicbot.music.Requester;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdaCommandContext implements CommandContext {

    private static final Logger LOG = LoggerFactory.getLogger(JdaCommandContext.class);

    private final Message message;
    private final Member member;
    private final ParsedCommand command;
    private final DjPolicy djPolicy;
    private final GuildMusicRegistry registry;

    JdaCommandContext(
            Message message, Member member, ParsedCommand command, DjPolicy djPolicy, GuildMusicRegistry registry) {
        this.message = message;
        this.member = member;
        this.command = command;
        this.djPolicy = djPolicy;
        this.registry = registry;
    }

    @Override
    public String args() {
        return command.args();
    }

    @Override
    public List<String> attachmentUrls() {
        return message.getAttachments().stream().map(Message.Attachment::getUrl).toList();
    }

    @Override
    public Requester author() {
        return new Requester(member.getIdLong(), member.getEffectiveName());
    }

    @Override
    public boolean authorIsDj() {
        Set<Long> roleIds = member.getRoles().stream().map(Role::getIdLong).collect(Collectors.toSet());
        return djPolicy.isDj(member.getIdLong(), member.hasPermission(Permission.MANAGE_SERVER), roleIds);
    }

    @Override
    public Optional<MemberVoice> authorVoice() {
        GuildVoiceState state = member.getVoiceState();
        AudioChannel channel = channelOf(state);
        return channel == null ? Optional.empty() : Optional.of(new MemberVoice(ref(channel), state.isDeafened()));
    }

    @Override
    public Optional<VoiceChannelRef> selfVoiceChannel() {
        return Optional.ofNullable(selfChannel()).map(JdaCommandContext::ref);
    }

    @Override
    public Set<Long> listeners() {
        AudioChannel channel = selfChannel();
        if (channel == null) {
            return Set.of();
        }
        return channel.getMembers().stream()
                .filter(m -> !m.getUser().isBot())
                .filter(m -> m.getVoiceState() != null && !m.getVoiceState().isDeafened())
                .map(Member::getIdLong)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean connect(VoiceChannelRef target) {
        AudioChannel channel = guild().getChannelById(AudioChannel.class, target.id());
        if (channel == null) {
            return false;
        }
        try {
            guild().getAudioManager().openAudioConnection(channel);
            return true;
        } catch (InsufficientPermissionException e) {
            LOG.debug("Missing permission to join {}", channel, e);
            return false;
        }
    }

    @Override
    public long guildId() {
        return guild().getIdLong();
    }

    @Override
    public MusicSession session() {
        return registry.forGuild(guild());
    }

    @Override
    public void reply(String text) {
        message.reply(MessageCreateData.fromContent(text))
                .queue(
                        null,
                        failure -> LOG.warn("Could not reply in {}: {}", message.getChannel(), failure.getMessage()));
    }

    private Guild guild() {
        return message.getGuild();
    }

    private @Nullable AudioChannel selfChannel() {
        return guild().getAudioManager().getConnectedChannel();
    }

    private static @Nullable AudioChannel channelOf(@Nullable GuildVoiceState state) {
        return state == null ? null : state.getChannel();
    }

    private static VoiceChannelRef ref(AudioChannel channel) {
        return new VoiceChannelRef(channel.getIdLong(), channel.getAsMention());
    }
}

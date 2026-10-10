package io.github.egeayan.musicbot.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import io.github.egeayan.musicbot.audio.AudioPlayerSendHandler;
import io.github.egeayan.musicbot.audio.LavaplayerPlayback;
import io.github.egeayan.musicbot.audio.TrackEndForwarder;
import io.github.egeayan.musicbot.command.Replies;
import io.github.egeayan.musicbot.music.MusicSession;
import io.github.egeayan.musicbot.music.Track;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuildMusicRegistry implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GuildMusicRegistry.class);

    private final AudioPlayerManager playerManager;
    private final Executor trackEvents;
    private final ConcurrentMap<Long, GuildAudio> guilds = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> announceChannels = new ConcurrentHashMap<>();

    public GuildMusicRegistry(AudioPlayerManager playerManager, Executor trackEvents) {
        this.playerManager = Objects.requireNonNull(playerManager, "playerManager");
        this.trackEvents = Objects.requireNonNull(trackEvents, "trackEvents");
    }

    public MusicSession forGuild(Guild guild) {
        return guilds.computeIfAbsent(guild.getIdLong(), _ -> create(guild)).session();
    }

    public void announceIn(long guildId, long channelId) {
        announceChannels.put(guildId, channelId);
    }

    public void remove(long guildId) {
        announceChannels.remove(guildId);
        GuildAudio audio = guilds.remove(guildId);
        if (audio != null) {
            audio.player().destroy();
        }
    }

    @Override
    public void close() {
        guilds.keySet().forEach(this::remove);
    }

    private GuildAudio create(Guild guild) {
        JDA jda = guild.getJDA();
        long guildId = guild.getIdLong();
        AudioPlayer player = playerManager.createPlayer();
        MusicSession session = new MusicSession(new LavaplayerPlayback(player), () -> disconnect(jda, guildId));
        player.addListener(new TrackEndForwarder(
                session::onTrackFinished,
                (track, reason) -> announceFailure(jda, guildId, track, reason),
                trackEvents));
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
        guild.getAudioManager().setConnectionListener(new VoiceConnectionLogger(guildId));
        return new GuildAudio(player, session);
    }

    private void announceFailure(JDA jda, long guildId, Track track, String reason) {
        Long channelId = announceChannels.get(guildId);
        GuildMessageChannel channel =
                channelId == null ? null : jda.getChannelById(GuildMessageChannel.class, channelId);
        if (channel == null || !channel.canTalk()) {
            return;
        }
        String text = Replies.error("Could not play " + Replies.title(track.info()) + ": " + Replies.escape(reason));
        if (text.length() > Message.MAX_CONTENT_LENGTH) {
            text = text.substring(0, Message.MAX_CONTENT_LENGTH - 1) + "…";
        }
        channel.sendMessage(MessageCreateData.fromContent(text))
                .queue(null, failure -> LOG.warn("Could not announce in {}: {}", channel, failure.getMessage()));
    }

    private static void disconnect(JDA jda, long guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
        }
    }

    private record GuildAudio(AudioPlayer player, MusicSession session) {}
}

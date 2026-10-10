package io.github.egeayan.musicbot.discord;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import io.github.egeayan.musicbot.audio.AudioPlayerManagers;
import io.github.egeayan.musicbot.audio.LavaplayerTrackLoader;
import io.github.egeayan.musicbot.command.CommandParser;
import io.github.egeayan.musicbot.command.CommandRegistry;
import io.github.egeayan.musicbot.command.DjPolicy;
import io.github.egeayan.musicbot.command.music.ForceSkipCommand;
import io.github.egeayan.musicbot.command.music.PlayCommand;
import io.github.egeayan.musicbot.command.music.RemoveCommand;
import io.github.egeayan.musicbot.command.music.SkipCommand;
import io.github.egeayan.musicbot.config.BotConfig;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MusicBot implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MusicBot.class);

    private static final EnumSet<GatewayIntent> INTENTS =
            EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES);

    private final JDA jda;
    private final GuildMusicRegistry registry;
    private final AudioPlayerManager playerManager;
    private final ExecutorService trackEvents;

    private MusicBot(
            JDA jda, GuildMusicRegistry registry, AudioPlayerManager playerManager, ExecutorService trackEvents) {
        this.jda = jda;
        this.registry = registry;
        this.playerManager = playerManager;
        this.trackEvents = trackEvents;
    }

    public static MusicBot start(BotConfig config) throws InterruptedException {
        MessageRequest.setDefaultMentions(EnumSet.noneOf(Message.MentionType.class));
        MessageRequest.setDefaultMentionRepliedUser(false);

        AudioPlayerManager playerManager = AudioPlayerManagers.create(config.youtube());
        ExecutorService trackEvents = Executors.newVirtualThreadPerTaskExecutor();
        var registry = new GuildMusicRegistry(playerManager, trackEvents);

        var commands = new CommandRegistry(List.of(
                new PlayCommand(new LavaplayerTrackLoader(playerManager), config.prefix()),
                new SkipCommand(config.skipRatio()),
                new ForceSkipCommand(),
                new RemoveCommand(config.prefix())));
        var commandListener = new CommandListener(
                new CommandParser(config.prefix()),
                commands,
                new DjPolicy(config.ownerId(), config.djRoleId()),
                registry);

        JDABuilder builder = JDABuilder.createLight(config.token(), INTENTS)
                .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .setAudioModuleConfig(new AudioModuleConfig().withDaveSessionFactory(new JDaveSessionFactory()))
                .setEventPool(Executors.newVirtualThreadPerTaskExecutor(), true)
                .setActivity(Activity.listening(config.prefix() + "play"))
                .addEventListeners(commandListener, new GuildCleanupListener(registry));
        if (config.proxy() != null) {
            useProxy(builder, config.proxy());
        }
        JDA jda = builder.build();
        jda.awaitReady();
        LOG.info(
                "Logged in as {} in {} guild(s)",
                jda.getSelfUser().getName(),
                jda.getGuildCache().size());
        return new MusicBot(jda, registry, playerManager, trackEvents);
    }

    private static void useProxy(JDABuilder builder, BotConfig.HttpProxy proxy) {
        LOG.info("Connecting to Discord through HTTP proxy {}", proxy);
        var address = InetSocketAddress.createUnresolved(proxy.host(), proxy.port());
        builder.setHttpClientBuilder(new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, address)));
        var websockets = new WebSocketFactory();
        websockets.getProxySettings().setHost(proxy.host()).setPort(proxy.port());
        builder.setWebsocketFactory(websockets);
    }

    @Override
    public void close() {
        LOG.info("Shutting down");
        jda.shutdown();
        try {
            if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                jda.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jda.shutdownNow();
        }
        registry.close();
        playerManager.shutdown();
        trackEvents.close();
    }
}

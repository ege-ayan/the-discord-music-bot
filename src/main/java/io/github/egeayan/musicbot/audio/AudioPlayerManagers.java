package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.MWeb;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.TvHtml5Simply;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
import dev.lavalink.youtube.clients.skeleton.Client;
import io.github.egeayan.musicbot.config.BotConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AudioPlayerManagers {

    private static final Logger LOG = LoggerFactory.getLogger(AudioPlayerManagers.class);
    private static final Duration YT_DLP_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration YT_DLP_UPDATE_TIMEOUT = Duration.ofMinutes(2);

    private AudioPlayerManagers() {}

    @SuppressWarnings("deprecation")
    public static AudioPlayerManager create(BotConfig.YouTube youtubeConfig) {
        var manager = new DefaultAudioPlayerManager();
        manager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        AudioSourceManager youtube = youtube(youtubeConfig);
        manager.registerSourceManager(ytDlp(youtubeConfig)
                .<AudioSourceManager>map(resolver -> new YoutubeFallbackSourceManager(youtube, resolver))
                .orElse(youtube));

        AudioSourceManagers.registerRemoteSources(
                manager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        return manager;
    }

    private static Optional<YtDlpResolver> ytDlp(BotConfig.YouTube config) {
        if (config.ytDlpPath() != null) {
            return Optional.of(new YtDlpResolver(config.ytDlpPath(), YT_DLP_TIMEOUT));
        }
        return BundledYtDlp.install().map(path -> {
            var resolver = new YtDlpResolver(path.toString(), YT_DLP_TIMEOUT);
            Thread.ofVirtual().name("yt-dlp-update").start(() -> update(resolver));
            return resolver;
        });
    }

    private static void update(YtDlpResolver resolver) {
        try {
            LOG.info("yt-dlp: {}", resolver.update(YT_DLP_UPDATE_TIMEOUT));
        } catch (IOException e) {
            LOG.warn("Could not update the bundled yt-dlp: {}", e.getMessage());
        }
    }

    private static List<Client> clients(BotConfig.YouTube config) {
        List<Client> clients = new ArrayList<>(
                List.of(new VisionOs(), new Music(), new AndroidVr(), new Web(), new MWeb(), new TvHtml5Simply()));
        if (config.oauth()) {
            clients.add(new Tv());
        }
        clients.add(new WebEmbedded());
        return clients;
    }

    private static YoutubeAudioSourceManager youtube(BotConfig.YouTube config) {
        var options = new YoutubeSourceOptions().setAllowSearch(true);
        if (config.remoteCipherUrl() != null) {
            options.setRemoteCipher(config.remoteCipherUrl(), config.remoteCipherPassword(), null);
        }
        var youtube = new YoutubeAudioSourceManager(options, clients(config).toArray(Client[]::new));
        if (config.oauth()) {
            youtube.useOauth2(config.oauthRefreshToken(), config.oauthRefreshToken() != null);
        }
        return youtube;
    }
}

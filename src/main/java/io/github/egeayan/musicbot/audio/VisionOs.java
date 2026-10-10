package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import dev.lavalink.youtube.clients.ClientConfig;
import dev.lavalink.youtube.clients.ClientOptions;
import dev.lavalink.youtube.clients.skeleton.StreamingNonMusicClient;
import org.jspecify.annotations.Nullable;

final class VisionOs extends StreamingNonMusicClient {

    static final String CLIENT_NAME = "VISIONOS";

    private final ClientOptions options;

    VisionOs() {
        options = new ClientOptions();
        options.setPlaylistLoading(false);
        options.setSearching(false);
        options.setVideoLoading(false);
    }

    @Override
    protected ClientConfig getBaseClientConfig(HttpInterface httpInterface) {
        return new ClientConfig()
                .withClientName(CLIENT_NAME)
                .withUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15"
                        + " (KHTML, like Gecko) Version/26.0 Safari/605.1.15")
                .withClientField("clientVersion", "1.02")
                .withClientField("deviceMake", "Apple")
                .withClientField("deviceModel", "RealityDevice17,1")
                .withClientField("osName", "visionOS")
                .withClientField("osVersion", "26.5.23O471");
    }

    @Override
    public @Nullable String getPlayerParams() {
        return null;
    }

    @Override
    public ClientOptions getOptions() {
        return options;
    }

    @Override
    public String getIdentifier() {
        return CLIENT_NAME;
    }

    @Override
    public boolean requirePlayerScript() {
        return false;
    }
}

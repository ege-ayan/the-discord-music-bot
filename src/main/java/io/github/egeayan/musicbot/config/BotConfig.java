package io.github.egeayan.musicbot.config;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record BotConfig(
        String token,
        String prefix,
        long ownerId,
        long djRoleId,
        double skipRatio,
        @Nullable HttpProxy proxy,
        YouTube youtube) {

    public BotConfig {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(youtube, "youtube");
    }

    @Override
    public String toString() {
        return "BotConfig[prefix=%s, ownerId=%d, djRoleId=%d, skipRatio=%s, proxy=%s, youtube=%s]"
                .formatted(prefix, ownerId, djRoleId, skipRatio, proxy, youtube);
    }

    public record HttpProxy(String host, int port) {

        public HttpProxy {
            Objects.requireNonNull(host, "host");
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    public record YouTube(
            boolean oauth,
            @Nullable String oauthRefreshToken,
            @Nullable String remoteCipherUrl,
            @Nullable String remoteCipherPassword,
            @Nullable String ytDlpPath) {

        @Override
        public String toString() {
            return "YouTube[oauth=%s, refreshToken=%s, remoteCipher=%s, ytDlp=%s]"
                    .formatted(oauth, oauthRefreshToken != null, remoteCipherUrl, ytDlpPath);
        }
    }
}

package io.github.egeayan.musicbot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class ConfigLoader {

    public static final String TOKEN_ENV = "DISCORD_TOKEN";
    public static final String CONFIG_PATH_ENV = "CONFIG_PATH";
    public static final Path DEFAULT_PATH = Path.of("config.yml");

    static final String DEFAULT_PREFIX = "!";
    static final double DEFAULT_SKIP_RATIO = 0.55;

    private static final ObjectMapper MAPPER = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Map<String, String> env;

    public ConfigLoader(Map<String, String> env) {
        this.env = Map.copyOf(env);
    }

    public static ConfigLoader fromSystemEnvironment() {
        return new ConfigLoader(System.getenv());
    }

    public Path resolvePath() {
        String override = env.get(CONFIG_PATH_ENV);
        return override == null || override.isBlank() ? DEFAULT_PATH : Path.of(override);
    }

    public BotConfig load() {
        return load(resolvePath());
    }

    public BotConfig load(Path path) {
        RawConfig raw = Files.exists(path) ? read(path) : RawConfig.EMPTY;
        if (!Files.exists(path) && isBlank(env.get(TOKEN_ENV))) {
            throw new ConfigException("No config found at " + path.toAbsolutePath() + " and " + TOKEN_ENV
                    + " is not set. Copy config.example.yml to config.yml and fill in your bot token.");
        }
        return validate(raw);
    }

    private static RawConfig read(Path path) {
        try {
            String content = Files.readString(path);
            return content.isBlank() ? RawConfig.EMPTY : MAPPER.readValue(content, RawConfig.class);
        } catch (IOException | JacksonException e) {
            throw new ConfigException("Could not read config " + path.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private BotConfig validate(RawConfig raw) {
        String token = firstNonBlank(env.get(TOKEN_ENV), raw.token());
        String prefix = Objects.requireNonNullElse(raw.prefix(), DEFAULT_PREFIX);
        double skipRatio = Objects.requireNonNullElse(raw.skipRatio(), DEFAULT_SKIP_RATIO);
        long ownerId = Objects.requireNonNullElse(raw.ownerId(), 0L);
        long djRoleId = Objects.requireNonNullElse(raw.djRoleId(), 0L);
        RawYouTube rawYoutube = Objects.requireNonNullElse(raw.youtube(), RawYouTube.EMPTY);
        String refreshToken = blankToNull(rawYoutube.oauthRefreshToken());
        var youtube = new BotConfig.YouTube(
                Boolean.TRUE.equals(rawYoutube.oauth()) || refreshToken != null,
                refreshToken,
                blankToNull(rawYoutube.remoteCipherUrl()),
                blankToNull(rawYoutube.remoteCipherPassword()),
                blankToNull(rawYoutube.ytDlpPath()));

        List<String> problems = new ArrayList<>();
        if (token == null) {
            problems.add("token is required (set it in the config file or via " + TOKEN_ENV + ")");
        }
        if (prefix.isBlank() || prefix.chars().anyMatch(Character::isWhitespace)) {
            problems.add("prefix must be non-empty and contain no whitespace");
        }
        if (!(skipRatio >= 0 && skipRatio <= 1)) {
            problems.add("skipRatio must be between 0 and 1");
        }
        if (ownerId < 0 || djRoleId < 0) {
            problems.add("ownerId and djRoleId must be Discord snowflake IDs");
        }
        BotConfig.HttpProxy proxy = null;
        String rawProxy = blankToNull(raw.proxy());
        if (rawProxy != null) {
            proxy = parseProxy(rawProxy);
            if (proxy == null) {
                problems.add("proxy must look like host:port, e.g. 127.0.0.1:8080");
            }
        }
        if (youtube.remoteCipherUrl() != null && !youtube.remoteCipherUrl().matches("(?i)^https?://\\S+$")) {
            problems.add("youtube.remoteCipherUrl must be an http(s) URL, e.g. http://localhost:8001");
        }
        if (!problems.isEmpty()) {
            throw new ConfigException("Invalid configuration:\n - " + String.join("\n - ", problems));
        }
        return new BotConfig(token, prefix, ownerId, djRoleId, skipRatio, proxy, youtube);
    }

    private static BotConfig.@Nullable HttpProxy parseProxy(String value) {
        String address = value.replaceFirst("(?i)^https?://", "").replaceFirst("/+$", "");
        int colon = address.lastIndexOf(':');
        if (colon <= 0 || colon == address.length() - 1) {
            return null;
        }
        try {
            int port = Integer.parseInt(address.substring(colon + 1));
            return port >= 1 && port <= 65535 ? new BotConfig.HttpProxy(address.substring(0, colon), port) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable String firstNonBlank(@Nullable String first, @Nullable String second) {
        return !isBlank(first) ? first : blankToNull(second);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return isBlank(value) ? null : value.strip();
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    record RawConfig(
            @Nullable String token,
            @Nullable String prefix,
            @Nullable Long ownerId,
            @Nullable Long djRoleId,
            @Nullable Double skipRatio,
            @Nullable String proxy,
            @Nullable RawYouTube youtube) {

        static final RawConfig EMPTY = new RawConfig(null, null, null, null, null, null, null);
    }

    record RawYouTube(
            @Nullable Boolean oauth,
            @Nullable String oauthRefreshToken,
            @Nullable String remoteCipherUrl,
            @Nullable String remoteCipherPassword,
            @Nullable String ytDlpPath) {

        static final RawYouTube EMPTY = new RawYouTube(null, null, null, null, null);
    }
}

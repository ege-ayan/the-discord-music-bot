package io.github.egeayan.musicbot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigLoaderTest {

    @TempDir
    Path dir;

    private Path write(String yaml) throws IOException {
        return Files.writeString(dir.resolve("config.yml"), yaml);
    }

    @Test
    void readsAllFields() throws IOException {
        var path = write("""
                token: file-token
                prefix: "?"
                ownerId: 111
                djRoleId: 222
                skipRatio: 0.75
                proxy: 127.0.0.1:8080
                youtube:
                  oauthRefreshToken: refresh
                  remoteCipherUrl: http://localhost:8001
                  remoteCipherPassword: secret
                  ytDlpPath: /usr/local/bin/yt-dlp
                """);

        var config = new ConfigLoader(Map.of()).load(path);

        assertThat(config)
                .isEqualTo(new BotConfig(
                        "file-token",
                        "?",
                        111,
                        222,
                        0.75,
                        new BotConfig.HttpProxy("127.0.0.1", 8080),
                        new BotConfig.YouTube(
                                true, "refresh", "http://localhost:8001", "secret", "/usr/local/bin/yt-dlp")));
    }

    @Test
    void oauthCanBeEnabledBeforeARefreshTokenExists() throws IOException {
        var youtube = new ConfigLoader(Map.of())
                .load(write("token: abc\nyoutube:\n  oauth: true"))
                .youtube();

        assertThat(youtube.oauth()).isTrue();
        assertThat(youtube.oauthRefreshToken()).isNull();
    }

    @Test
    void rejectsRemoteCipherUrlWithoutScheme() throws IOException {
        var path = write("token: abc\nyoutube:\n  remoteCipherUrl: localhost:8001");

        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(path))
                .hasMessageContaining("youtube.remoteCipherUrl must be an http(s) URL");
    }

    @Test
    void youtubeSectionIsOptional() throws IOException {
        assertThat(new ConfigLoader(Map.of()).load(write("token: abc")).youtube())
                .isEqualTo(new BotConfig.YouTube(false, null, null, null, null));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"127.0.0.1:8080", "http://127.0.0.1:8080", "HTTP://127.0.0.1:8080/"})
    void acceptsProxyWithOrWithoutScheme(String proxy) throws IOException {
        var config = new ConfigLoader(Map.of()).load(write("token: abc\nproxy: \"" + proxy + "\""));

        assertThat(config.proxy()).isEqualTo(new BotConfig.HttpProxy("127.0.0.1", 8080));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"127.0.0.1", "127.0.0.1:", ":8080", "localhost:abc", "localhost:70000"})
    void rejectsMalformedProxy(String proxy) throws IOException {
        var path = write("token: abc\nproxy: \"" + proxy + "\"");

        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(path)).hasMessageContaining("proxy must look like");
    }

    @Test
    void proxyIsOptional() throws IOException {
        assertThat(new ConfigLoader(Map.of())
                        .load(write("token: abc\nproxy: \"\""))
                        .proxy())
                .isNull();
    }

    @Test
    void appliesDefaults() throws IOException {
        var config = new ConfigLoader(Map.of()).load(write("token: abc"));

        assertThat(config.prefix()).isEqualTo("!");
        assertThat(config.skipRatio()).isEqualTo(0.55);
        assertThat(config.ownerId()).isZero();
        assertThat(config.djRoleId()).isZero();
        assertThat(config.youtube().oauthRefreshToken()).isNull();
    }

    @Test
    void environmentTokenOverridesFile() throws IOException {
        var config = new ConfigLoader(Map.of("DISCORD_TOKEN", "env-token")).load(write("token: file-token"));

        assertThat(config.token()).isEqualTo("env-token");
    }

    @Test
    void fileIsOptionalWhenTokenComesFromEnvironment() {
        var config = new ConfigLoader(Map.of("DISCORD_TOKEN", "env-token")).load(dir.resolve("missing.yml"));

        assertThat(config.token()).isEqualTo("env-token");
        assertThat(config.prefix()).isEqualTo("!");
    }

    @Test
    void missingFileWithoutTokenExplainsWhatToDo() {
        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(dir.resolve("missing.yml")))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("config.example.yml");
    }

    @Test
    void reportsAllValidationProblemsAtOnce() throws IOException {
        var path = write("""
                token: "  "
                prefix: "a b"
                skipRatio: 2
                """);

        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("token is required")
                .hasMessageContaining("prefix")
                .hasMessageContaining("skipRatio");
    }

    @Test
    void unknownKeysAreRejectedToCatchTypos() throws IOException {
        var path = write("""
                token: abc
                skipRaito: 0.5
                """);

        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("skipRaito");
    }

    @Test
    void emptyFileStillNeedsToken() throws IOException {
        var path = write("");

        assertThatThrownBy(() -> new ConfigLoader(Map.of()).load(path)).hasMessageContaining("token is required");
    }

    @Test
    void configPathCanBeOverriddenByEnvironment() {
        assertThat(new ConfigLoader(Map.of("CONFIG_PATH", "/etc/bot.yml")).resolvePath())
                .isEqualTo(Path.of("/etc/bot.yml"));
        assertThat(new ConfigLoader(Map.of()).resolvePath()).isEqualTo(ConfigLoader.DEFAULT_PATH);
    }

    @Test
    void toStringNeverLeaksSecrets() throws IOException {
        var config = new ConfigLoader(Map.of()).load(write("""
                token: super-secret
                youtube:
                  oauthRefreshToken: also-secret
                  remoteCipherPassword: cipher-secret
                """));

        assertThat(config.toString())
                .doesNotContain("super-secret")
                .doesNotContain("also-secret")
                .doesNotContain("cipher-secret");
    }
}

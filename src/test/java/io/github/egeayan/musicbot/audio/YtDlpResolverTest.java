package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class YtDlpResolverTest {

    @TempDir
    Path dir;

    private Path script(String body) throws IOException {
        Path script = dir.resolve("yt-dlp");
        Files.writeString(script, "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }

    @Test
    void parsesExtensionAndUrl() throws IOException {
        var stream = YtDlpResolver.parse("webm\nhttps://rr1.googlevideo.com/videoplayback?id=1\n");

        assertThat(stream)
                .isEqualTo(new YtDlpResolver.ResolvedStream(
                        "webm", URI.create("https://rr1.googlevideo.com/videoplayback?id=1")));
    }

    @Test
    void rejectsOutputWithoutAUrl() {
        assertThatThrownBy(() -> YtDlpResolver.parse("webm\nNA\n"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unexpected yt-dlp output");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void passesTheVideoUrlAndReadsTheStream() throws IOException {
        Path ytDlp = script("""
                for last; do :; done
                echo M4A
                echo "https://example.com/stream?for=$last"
                """);

        var stream = new YtDlpResolver(ytDlp.toString(), Duration.ofSeconds(10)).resolve("https://youtu.be/abc");

        assertThat(stream.extension()).isEqualTo("m4a");
        assertThat(stream.uri()).hasToString("https://example.com/stream?for=https://youtu.be/abc");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void reportsTheLastErrorLineWhenYtDlpFails() throws IOException {
        Path ytDlp = script("""
                echo "WARNING: something minor" >&2
                echo "ERROR: Video unavailable" >&2
                exit 1
                """);

        assertThatThrownBy(() -> new YtDlpResolver(ytDlp.toString(), Duration.ofSeconds(10)).resolve("x"))
                .isInstanceOf(IOException.class)
                .hasMessage("yt-dlp failed: ERROR: Video unavailable");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void givesUpWhenYtDlpHangs() throws IOException {
        Path ytDlp = script("sleep 10");

        assertThatThrownBy(() -> new YtDlpResolver(ytDlp.toString(), Duration.ofMillis(200)).resolve("x"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void updatesItselfAndReportsTheOutcome() throws IOException {
        Path ytDlp = script("""
                [ "$1" = "--update" ] || exit 2
                echo "Latest version: stable@2026.09.01"
                echo "Updated yt-dlp to stable@2026.09.01"
                """);

        String outcome = new YtDlpResolver(ytDlp.toString(), Duration.ofSeconds(10)).update(Duration.ofSeconds(10));

        assertThat(outcome).isEqualTo("Updated yt-dlp to stable@2026.09.01");
    }

    @Test
    void failsClearlyWhenYtDlpIsMissing() {
        var resolver = new YtDlpResolver(dir.resolve("missing").toString(), Duration.ofSeconds(1));

        assertThatThrownBy(() -> resolver.resolve("x")).isInstanceOf(IOException.class);
    }
}

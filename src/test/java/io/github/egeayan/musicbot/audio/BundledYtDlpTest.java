package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BundledYtDlpTest {

    @TempDir
    Path root;

    private final AtomicInteger binaryReads = new AtomicInteger();

    private Function<String, @Nullable InputStream> resources(Map<String, String> files) {
        return name -> {
            String content = files.get(name);
            if (content == null) {
                return null;
            }
            if (!name.equals("VERSION")) {
                binaryReads.incrementAndGet();
            }
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
    }

    @ParameterizedTest
    @CsvSource({
        "Mac OS X, aarch64, yt-dlp_macos",
        "Mac OS X, x86_64, yt-dlp_macos",
        "Linux, amd64, yt-dlp_linux",
        "Linux, aarch64, yt-dlp_linux_aarch64",
        "Windows 11, amd64, yt-dlp.exe"
    })
    void picksTheBinaryForThePlatform(String os, String arch, String asset) {
        assertThat(BundledYtDlp.assetFor(os, arch)).contains(asset);
    }

    @ParameterizedTest
    @CsvSource({"Linux, riscv64", "FreeBSD, amd64", "SunOS, sparcv9"})
    void hasNoBinaryForUnsupportedPlatforms(String os, String arch) {
        assertThat(BundledYtDlp.assetFor(os, arch)).isEmpty();
    }

    @Test
    void unpacksTheBinaryIntoAVersionedExecutableFile() throws IOException {
        var files = resources(Map.of("VERSION", "2026.08.19\n", "yt-dlp_macos", "binary"));

        Optional<Path> installed = BundledYtDlp.install(root, "yt-dlp_macos", files);

        Path expected = root.resolve("2026.08.19").resolve("yt-dlp_macos");
        assertThat(installed).contains(expected);
        assertThat(expected).hasContent("binary").isExecutable();
        try (var siblings = Files.list(expected.getParent())) {
            assertThat(siblings).containsExactly(expected);
        }
    }

    @Test
    void reusesAnAlreadyUnpackedBinary() throws IOException {
        var files = resources(Map.of("VERSION", "1", "yt-dlp_linux", "binary"));

        BundledYtDlp.install(root, "yt-dlp_linux", files);
        BundledYtDlp.install(root, "yt-dlp_linux", files);

        assertThat(binaryReads).hasValue(1);
    }

    @Test
    void keepsASelfUpdatedBinary() throws IOException {
        var files = resources(Map.of("VERSION", "1", "yt-dlp_linux", "bundled"));
        Path installed = BundledYtDlp.install(root, "yt-dlp_linux", files).orElseThrow();
        Files.writeString(installed, "updated");

        BundledYtDlp.install(root, "yt-dlp_linux", files);

        assertThat(installed).hasContent("updated");
    }

    @Test
    void reportsNothingWhenTheJarHasNoBundledBinaries() throws IOException {
        assertThat(BundledYtDlp.install(root, "yt-dlp_linux", resources(Map.of())))
                .isEmpty();
        assertThat(BundledYtDlp.install(root, "yt-dlp_linux", resources(Map.of("VERSION", "1"))))
                .isEmpty();
    }
}

package io.github.egeayan.musicbot.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BundledYtDlp {

    private static final Logger LOG = LoggerFactory.getLogger(BundledYtDlp.class);
    private static final String RESOURCE_DIR = "/yt-dlp/";

    private BundledYtDlp() {}

    public static Optional<Path> install() {
        Optional<String> asset = assetFor(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
        if (asset.isEmpty()) {
            LOG.warn(
                    "No bundled yt-dlp for {} {}; set youtube.ytDlpPath to enable the fallback",
                    System.getProperty("os.name"),
                    System.getProperty("os.arch"));
            return Optional.empty();
        }
        try {
            return install(
                    cacheRoot(), asset.get(), name -> BundledYtDlp.class.getResourceAsStream(RESOURCE_DIR + name));
        } catch (IOException | UncheckedIOException e) {
            LOG.warn("Could not unpack the bundled yt-dlp: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<String> assetFor(String osName, String osArch) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = osArch.toLowerCase(Locale.ROOT);
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            return Optional.of("yt-dlp_macos");
        }
        if (os.startsWith("windows")) {
            return Optional.of("yt-dlp.exe");
        }
        if (os.startsWith("linux")) {
            return switch (arch) {
                case "amd64", "x86_64" -> Optional.of("yt-dlp_linux");
                case "aarch64", "arm64" -> Optional.of("yt-dlp_linux_aarch64");
                default -> Optional.empty();
            };
        }
        return Optional.empty();
    }

    static Optional<Path> install(Path root, String asset, Function<String, @Nullable InputStream> resources)
            throws IOException {
        String version;
        try (InputStream versionFile = resources.apply("VERSION")) {
            if (versionFile == null) {
                return Optional.empty();
            }
            version = new String(versionFile.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        Path target = root.resolve(version).resolve(asset);
        if (Files.isRegularFile(target) && Files.isExecutable(target)) {
            return Optional.of(target);
        }
        try (InputStream binary = resources.apply(asset)) {
            if (binary == null) {
                return Optional.empty();
            }
            Files.createDirectories(target.getParent());
            Path partial = Files.createTempFile(target.getParent(), asset, ".part");
            try {
                Files.copy(binary, partial, StandardCopyOption.REPLACE_EXISTING);
                if (!partial.toFile().setExecutable(true, true)) {
                    throw new IOException("Could not mark " + partial + " as executable");
                }
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException _) {
                LOG.debug("Another process unpacked {} first", target);
            } finally {
                Files.deleteIfExists(partial);
            }
        }
        LOG.info("Unpacked bundled yt-dlp {} to {}", version, target);
        return Optional.of(target);
    }

    private static Path cacheRoot() {
        String xdg = System.getenv("XDG_CACHE_HOME");
        Path base = xdg == null || xdg.isBlank() ? Path.of(System.getProperty("user.home"), ".cache") : Path.of(xdg);
        return base.resolve("the-discord-music-bot").resolve("yt-dlp");
    }
}

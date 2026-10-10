package io.github.egeayan.musicbot.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class YtDlpResolver {

    static final String FORMAT = "bestaudio[protocol^=http][ext=webm]/bestaudio[protocol^=http][ext=m4a]";

    private final String executable;
    private final Duration timeout;

    public YtDlpResolver(String executable, Duration timeout) {
        this.executable = Objects.requireNonNull(executable, "executable");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public ResolvedStream resolve(String videoUrl) throws IOException {
        return parse(run(
                timeout,
                "--no-warnings",
                "--no-playlist",
                "--force-ipv4",
                "-f",
                FORMAT,
                "--print",
                "%(ext)s",
                "--print",
                "%(url)s",
                "--",
                videoUrl));
    }

    public String update(Duration updateTimeout) throws IOException {
        return lastLine(run(updateTimeout, "--update"));
    }

    private String run(Duration limit, String... args) throws IOException {
        List<String> command = new ArrayList<>(args.length + 1);
        command.add(executable);
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();
        CompletableFuture<String> stdout = drain(process.getInputStream());
        CompletableFuture<String> stderr = drain(process.getErrorStream());
        try {
            if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("yt-dlp did not answer within " + limit.toSeconds() + "s");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for yt-dlp");
        }
        if (process.exitValue() != 0) {
            throw new IOException("yt-dlp failed: " + lastLine(stderr.join()));
        }
        return stdout.join();
    }

    static ResolvedStream parse(String output) throws IOException {
        List<String> lines = output.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
        if (lines.size() < 2 || !lines.get(1).matches("(?i)^https?://\\S+$")) {
            throw new IOException("Unexpected yt-dlp output: " + lastLine(output));
        }
        return new ResolvedStream(lines.get(0).toLowerCase(Locale.ROOT), URI.create(lines.get(1)));
    }

    private static CompletableFuture<String> drain(InputStream stream) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (stream) {
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                Thread::startVirtualThread);
    }

    private static String lastLine(String text) {
        return text.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((_, second) -> second)
                .orElse("no output");
    }

    public record ResolvedStream(String extension, URI uri) {}
}

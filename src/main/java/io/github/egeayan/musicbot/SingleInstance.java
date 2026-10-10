package io.github.egeayan.musicbot;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class SingleInstance implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstance(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static Path lockFile() {
        String xdg = System.getenv("XDG_CACHE_HOME");
        Path base = xdg == null || xdg.isBlank() ? Path.of(System.getProperty("user.home"), ".cache") : Path.of(xdg);
        return base.resolve("the-discord-music-bot").resolve("bot.lock");
    }

    public static Optional<SingleInstance> tryAcquire(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException _) {
            lock = null;
        }
        if (lock == null) {
            channel.close();
            return Optional.empty();
        }
        return Optional.of(new SingleInstance(channel, lock));
    }

    @Override
    public void close() throws IOException {
        lock.release();
        channel.close();
    }
}

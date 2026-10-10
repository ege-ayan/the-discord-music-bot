package io.github.egeayan.musicbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceTest {

    @TempDir
    Path dir;

    @Test
    void onlyOneProcessCanHoldTheLock() throws IOException {
        Path file = dir.resolve("bot.lock");

        try (SingleInstance _ = SingleInstance.tryAcquire(file).orElseThrow()) {
            assertThat(SingleInstance.tryAcquire(file)).isEmpty();
        }

        try (SingleInstance _ = SingleInstance.tryAcquire(file).orElseThrow()) {
            assertThat(file).exists();
        }
    }
}

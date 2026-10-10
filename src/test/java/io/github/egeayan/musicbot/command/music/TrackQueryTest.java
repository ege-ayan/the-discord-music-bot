package io.github.egeayan.musicbot.command.music;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrackQueryTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "never gonna give you up                     | ytsearch:never gonna give you up",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ | https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "HTTP://example.com/song.mp3                 | HTTP://example.com/song.mp3",
                "scsearch:lofi                               | scsearch:lofi",
                "YTMSEARCH:daft punk                         | YTMSEARCH:daft punk",
                "httpster                                    | ytsearch:httpster",
            })
    void mapsInputToIdentifier(String input, String identifier) {
        assertThat(TrackQuery.toIdentifier(input)).isEqualTo(identifier);
    }

    @Test
    void stripsAngleBracketsUsedToSuppressEmbeds() {
        assertThat(TrackQuery.input("<https://youtu.be/x>", List.of())).contains("https://youtu.be/x");
    }

    @Test
    void fallsBackToFirstAttachment() {
        assertThat(TrackQuery.input("  ", List.of("https://cdn/a.mp3", "https://cdn/b.mp3")))
                .contains("https://cdn/a.mp3");
    }

    @Test
    void argumentsWinOverAttachments() {
        assertThat(TrackQuery.input("song", List.of("https://cdn/a.mp3"))).contains("song");
    }

    @Test
    void emptyWhenNothingGiven() {
        assertThat(TrackQuery.input("", List.of())).isEmpty();
    }
}

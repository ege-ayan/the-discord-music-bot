package io.github.egeayan.musicbot.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.egeayan.musicbot.music.TrackInfo;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RepliesTest {

    @ParameterizedTest(name = "{0} ms -> {1}")
    @CsvSource({
        "0, 00:00",
        "999, 00:01",
        "65000, 01:05",
        "599499, 09:59",
        "3600000, 1:00:00",
        "3723000, 1:02:03",
        "36000000, 10:00:00",
    })
    void formatsDurations(long millis, String expected) {
        assertThat(Replies.duration(Duration.ofMillis(millis))).isEqualTo(expected);
    }

    @Test
    void streamsShowAsLive() {
        var live = new TrackInfo("Radio", "Station", null, Duration.ZERO, true);

        assertThat(Replies.duration(live)).isEqualTo("LIVE");
    }

    @Test
    void titlesAreMarkdownEscaped() {
        var info = new TrackInfo("**bold** _song_", "x", null, Duration.ofSeconds(61), false);

        assertThat(Replies.titleWithDuration(info)).isEqualTo("**\\*\\*bold\\*\\* \\_song\\_** (`01:01`)");
    }
}

package io.github.egeayan.musicbot.audio;

import static org.assertj.core.api.Assertions.assertThat;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class VisionOsTest {

    private final VisionOs client = new VisionOs();

    @Test
    void isOnlyUsedForPlayback() {
        var options = client.getOptions();

        assertThat(options.getPlayback()).isTrue();
        assertThat(options.getVideoLoading()).isFalse();
        assertThat(options.getSearching()).isFalse();
        assertThat(options.getPlaylistLoading()).isFalse();
    }

    @Test
    void needsNoPlayerScriptOrParams() {
        assertThat(client.requirePlayerScript()).isFalse();
        assertThat(client.getPlayerParams()).isNull();
        assertThat(client.getIdentifier()).isEqualTo("VISIONOS");
    }

    @Test
    void identifiesAsAVisionProDevice() throws IOException {
        var config = client.getBaseClientConfig(null);
        JsonBrowser context =
                JsonBrowser.parse(config.toJsonString()).get("context").get("client");

        assertThat(config.getName()).isEqualTo("VISIONOS");
        assertThat(config.getUserAgent()).contains("Safari");
        assertThat(context.get("clientName").text()).isEqualTo("VISIONOS");
        assertThat(context.get("clientVersion").text()).isEqualTo("1.02");
        assertThat(context.get("deviceModel").text()).isEqualTo("RealityDevice17,1");
        assertThat(context.get("osName").text()).isEqualTo("visionOS");
    }

    @Test
    void handsOutIndependentConfigs() {
        var first = client.getBaseClientConfig(null);
        first.withClientField("clientVersion", "changed");

        assertThat(client.getBaseClientConfig(null).getRoot().toString()).doesNotContain("changed");
    }
}

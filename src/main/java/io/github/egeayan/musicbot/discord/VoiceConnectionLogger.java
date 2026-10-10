package io.github.egeayan.musicbot.discord;

import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceConnectionLogger implements ConnectionListener {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceConnectionLogger.class);

    private final long guildId;

    VoiceConnectionLogger(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onStatusChange(ConnectionStatus status) {
        if (status.name().startsWith("ERROR") || status.name().startsWith("DISCONNECTED")) {
            LOG.warn("Voice connection in guild {}: {}", guildId, status);
        } else if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.NOT_CONNECTED) {
            LOG.info("Voice connection in guild {}: {}", guildId, status);
        } else {
            LOG.debug("Voice connection in guild {}: {}", guildId, status);
        }
    }
}

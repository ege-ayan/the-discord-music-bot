package io.github.egeayan.musicbot;

import io.github.egeayan.musicbot.config.BotConfig;
import io.github.egeayan.musicbot.config.ConfigException;
import io.github.egeayan.musicbot.config.ConfigLoader;
import io.github.egeayan.musicbot.discord.MusicBot;
import java.io.IOException;
import java.util.Optional;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static SingleInstance instance;

    private Main() {}

    static void main() {
        try {
            Optional<SingleInstance> acquired = SingleInstance.tryAcquire(SingleInstance.lockFile());
            if (acquired.isEmpty()) {
                LOG.error("Another copy of the bot is already running. Stop it before starting a new one.");
                System.exit(1);
                return;
            }
            instance = acquired.get();
        } catch (IOException e) {
            LOG.error("Could not lock {}: {}", SingleInstance.lockFile(), e.getMessage());
            System.exit(1);
            return;
        }

        BotConfig config;
        try {
            config = ConfigLoader.fromSystemEnvironment().load();
        } catch (ConfigException e) {
            LOG.error(e.getMessage());
            System.exit(1);
            return;
        }
        LOG.info("Loaded {}", config);

        try {
            MusicBot bot = MusicBot.start(config);
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(
                            () -> {
                                bot.close();
                                try {
                                    instance.close();
                                } catch (IOException e) {
                                    LOG.warn("Could not release the instance lock: {}", e.getMessage());
                                }
                            },
                            "shutdown"));
        } catch (InvalidTokenException e) {
            LOG.error("Discord rejected the bot token. Check the token in your config or {}.", ConfigLoader.TOKEN_ENV);
            System.exit(1);
        } catch (ErrorResponseException e) {
            LOG.error("Could not reach Discord: {}", e.getMessage(), e);
            System.exit(1);
        } catch (IllegalStateException e) {
            LOG.error(
                    "Could not connect to Discord. Make sure MESSAGE CONTENT INTENT is enabled for your bot "
                            + "in the Discord developer portal.",
                    e);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(1);
        }
    }
}

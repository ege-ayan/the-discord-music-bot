package io.github.egeayan.musicbot.discord;

import java.util.Objects;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class GuildCleanupListener extends ListenerAdapter {

    private final GuildMusicRegistry registry;

    public GuildCleanupListener(GuildMusicRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        registry.remove(event.getGuild().getIdLong());
    }
}

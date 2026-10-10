package io.github.egeayan.musicbot.discord;

import io.github.egeayan.musicbot.command.Command;
import io.github.egeayan.musicbot.command.CommandParser;
import io.github.egeayan.musicbot.command.CommandRegistry;
import io.github.egeayan.musicbot.command.DjPolicy;
import io.github.egeayan.musicbot.command.ParsedCommand;
import io.github.egeayan.musicbot.command.Replies;
import java.util.Objects;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CommandListener.class);

    private final CommandParser parser;
    private final CommandRegistry commands;
    private final DjPolicy djPolicy;
    private final GuildMusicRegistry registry;

    public CommandListener(
            CommandParser parser, CommandRegistry commands, DjPolicy djPolicy, GuildMusicRegistry registry) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.djPolicy = Objects.requireNonNull(djPolicy, "djPolicy");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Member member = event.getMember();
        if (!event.isFromGuild() || member == null || event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }
        parser.parse(event.getMessage().getContentRaw())
                .ifPresent(parsed ->
                        commands.find(parsed.name()).ifPresent(command -> run(command, parsed, event, member)));
    }

    private void run(Command command, ParsedCommand parsed, MessageReceivedEvent event, Member member) {
        registry.announceIn(event.getGuild().getIdLong(), event.getChannel().getIdLong());
        var ctx = new JdaCommandContext(event.getMessage(), member, parsed, djPolicy, registry);
        try {
            command.execute(ctx);
        } catch (RuntimeException e) {
            LOG.error(
                    "Command '{}' failed in guild {}",
                    command.name(),
                    event.getGuild().getId(),
                    e);
            ctx.reply(Replies.error("Something went wrong while running that command."));
        }
    }
}

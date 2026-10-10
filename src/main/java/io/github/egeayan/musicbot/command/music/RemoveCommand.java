package io.github.egeayan.musicbot.command.music;

import io.github.egeayan.musicbot.command.Command;
import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.Replies;
import io.github.egeayan.musicbot.command.VoiceChecks;
import io.github.egeayan.musicbot.music.RemoveResult;
import java.util.Objects;

public final class RemoveCommand implements Command {

    private final String prefix;

    public RemoveCommand(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!VoiceChecks.requireListening(ctx)) {
            return;
        }
        if (ctx.session().upcoming().isEmpty()) {
            ctx.reply(Replies.error("There is nothing in the queue!"));
            return;
        }
        int position;
        try {
            position = Integer.parseInt(ctx.args().strip());
        } catch (NumberFormatException e) {
            ctx.reply(Replies.error("Usage: `" + prefix + name() + " <position>`"));
            return;
        }

        RemoveResult result = ctx.session().remove(position, ctx.author().userId(), ctx.authorIsDj());
        ctx.reply(
                switch (result) {
                    case RemoveResult.Removed(var entry) -> {
                        boolean own = entry.requester().userId() == ctx.author().userId();
                        yield Replies.success("Removed " + Replies.title(entry.info()) + " from the queue"
                                + (own ? "" : " (requested by " + Replies.requester(entry.requester()) + ")"));
                    }
                    case RemoveResult.InvalidPosition(int size) ->
                        Replies.error("Position must be a valid integer between 1 and " + size + "!");
                    case RemoveResult.NotPermitted(var entry) ->
                        Replies.error(
                                "You cannot remove " + Replies.title(entry.info()) + " because you didn't add it!");
                });
    }
}

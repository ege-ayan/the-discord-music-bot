package io.github.egeayan.musicbot.command.music;

import io.github.egeayan.musicbot.command.Command;
import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.Replies;
import io.github.egeayan.musicbot.command.VoiceChecks;
import java.util.Set;

public final class ForceSkipCommand implements Command {

    @Override
    public String name() {
        return "forceskip";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("fs");
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.authorIsDj()) {
            ctx.reply(Replies.error("Only DJs can force skip!"));
            return;
        }
        if (!VoiceChecks.requirePlaying(ctx)) {
            return;
        }
        ctx.session()
                .skip()
                .ifPresentOrElse(
                        skipped -> ctx.reply(Replies.success("Skipped " + Replies.title(skipped.info())
                                + " (requested by " + Replies.requester(skipped.requester()) + ")")),
                        () -> ctx.reply(Replies.error("There must be music playing to use that!")));
    }
}

package io.github.egeayan.musicbot.command.music;

import io.github.egeayan.musicbot.command.Command;
import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.Replies;
import io.github.egeayan.musicbot.command.VoiceChecks;
import io.github.egeayan.musicbot.music.VoteSkipResult;
import java.util.Set;

public final class SkipCommand implements Command {

    private final double skipRatio;

    public SkipCommand(double skipRatio) {
        if (!(skipRatio >= 0 && skipRatio <= 1)) {
            throw new IllegalArgumentException("skipRatio must be between 0 and 1");
        }
        this.skipRatio = skipRatio;
    }

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("s");
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!VoiceChecks.requirePlaying(ctx) || !VoiceChecks.requireListening(ctx)) {
            return;
        }
        VoteSkipResult result = ctx.session().voteSkip(ctx.author().userId(), ctx.listeners(), skipRatio);
        ctx.reply(
                switch (result) {
                    case VoteSkipResult.NothingPlaying() -> Replies.error("There must be music playing to use that!");
                    case VoteSkipResult.SkippedByRequester(var skipped) ->
                        Replies.success("Skipped " + Replies.title(skipped.info()));
                    case VoteSkipResult.SkippedByVote(var skipped, int votes, int required, int listeners) ->
                        Replies.success("You voted to skip the song " + tally(votes, required, listeners)) + "\n"
                                + Replies.success("Skipped " + Replies.title(skipped.info()) + " (requested by "
                                        + Replies.requester(skipped.requester()) + ")");
                    case VoteSkipResult.VoteRecorded(boolean alreadyVoted, int votes, int required, int listeners) ->
                        alreadyVoted
                                ? Replies.warning(
                                        "You already voted to skip this song " + tally(votes, required, listeners))
                                : Replies.success("You voted to skip the song " + tally(votes, required, listeners));
                });
    }

    private static String tally(int votes, int required, int listeners) {
        return "`[" + votes + " votes, " + required + "/" + listeners + " needed]`";
    }
}

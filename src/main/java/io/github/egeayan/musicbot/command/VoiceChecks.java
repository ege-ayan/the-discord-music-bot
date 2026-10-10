package io.github.egeayan.musicbot.command;

import java.util.Optional;

public final class VoiceChecks {

    private VoiceChecks() {}

    public static boolean requireListening(CommandContext ctx) {
        Optional<VoiceChannelRef> self = ctx.selfVoiceChannel();
        boolean listening = ctx.authorVoice()
                .filter(voice -> !voice.deafened())
                .filter(voice -> self.isEmpty() || voice.channel().sameChannelAs(self.get()))
                .isPresent();
        if (!listening) {
            String where = self.map(VoiceChannelRef::mention).orElse("a voice channel");
            ctx.reply(Replies.error("You must be listening in " + where + " to use that!"));
        }
        return listening;
    }

    public static boolean requirePlaying(CommandContext ctx) {
        if (ctx.session().current().isEmpty()) {
            ctx.reply(Replies.error("There must be music playing to use that!"));
            return false;
        }
        return true;
    }
}

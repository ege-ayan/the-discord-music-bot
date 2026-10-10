package io.github.egeayan.musicbot.command.music;

import io.github.egeayan.musicbot.command.Command;
import io.github.egeayan.musicbot.command.CommandContext;
import io.github.egeayan.musicbot.command.Replies;
import io.github.egeayan.musicbot.command.VoiceChannelRef;
import io.github.egeayan.musicbot.command.VoiceChecks;
import io.github.egeayan.musicbot.music.EnqueueResult;
import io.github.egeayan.musicbot.music.LoadResult;
import io.github.egeayan.musicbot.music.QueueEntry;
import io.github.egeayan.musicbot.music.Track;
import io.github.egeayan.musicbot.music.TrackLoader;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayCommand implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(PlayCommand.class);

    private final TrackLoader loader;
    private final String prefix;

    public PlayCommand(TrackLoader loader, String prefix) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    @Override
    public String name() {
        return "play";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("p");
    }

    @Override
    public void execute(CommandContext ctx) {
        Optional<String> input = TrackQuery.input(ctx.args(), ctx.attachmentUrls());
        if (input.isEmpty()) {
            ctx.reply(Replies.warning("Usage: `" + prefix + name() + " <song title | URL>`"));
            return;
        }
        if (!VoiceChecks.requireListening(ctx)) {
            return;
        }
        VoiceChannelRef target = ctx.authorVoice().orElseThrow().channel();
        String query = input.get();

        loader.load(ctx.guildId(), TrackQuery.toIdentifier(query)).whenComplete((result, error) -> {
            if (error != null) {
                LOG.error("Loading '{}' failed unexpectedly", query, error);
                ctx.reply(Replies.error("Something went wrong while loading that."));
                return;
            }
            handle(ctx, query, target, result);
        });
    }

    private void handle(CommandContext ctx, String query, VoiceChannelRef target, LoadResult result) {
        switch (result) {
            case LoadResult.Single(Track track) -> {
                if (ensureConnected(ctx, target)) {
                    ctx.reply(describe(ctx.session().enqueue(new QueueEntry(track, ctx.author()))));
                }
            }
            case LoadResult.Playlist(String playlistName, var tracks) -> {
                if (ensureConnected(ctx, target)) {
                    tracks.forEach(track -> ctx.session().enqueue(new QueueEntry(track, ctx.author())));
                    ctx.reply(Replies.success("Found playlist **" + Replies.escape(playlistName) + "** with `"
                            + tracks.size() + "` entries; added to the queue!"));
                }
            }
            case LoadResult.NoMatches() ->
                ctx.reply(Replies.warning("No results found for `" + codeSafe(query) + "`."));
            case LoadResult.Failed(String message) ->
                ctx.reply(Replies.error("Error loading track: " + Replies.escape(message)));
        }
    }

    private static boolean ensureConnected(CommandContext ctx, VoiceChannelRef target) {
        if (ctx.selfVoiceChannel().isPresent() || ctx.connect(target)) {
            return true;
        }
        ctx.reply(Replies.error("I am unable to connect to " + target.mention() + "!"));
        return false;
    }

    private static String describe(EnqueueResult result) {
        String track = Replies.titleWithDuration(result.entry().info());
        return switch (result) {
            case EnqueueResult.StartedPlaying _ -> Replies.success("Added " + track + " to begin playing");
            case EnqueueResult.Queued(var _, int position) ->
                Replies.success("Added " + track + " to the queue at position " + position);
        };
    }

    private static String codeSafe(String text) {
        return text.replace("`", "'");
    }
}

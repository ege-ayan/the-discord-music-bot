package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import io.github.egeayan.musicbot.music.AudioPlayback;
import io.github.egeayan.musicbot.music.Track;
import java.util.Objects;

public final class LavaplayerPlayback implements AudioPlayback {

    private final AudioPlayer player;

    public LavaplayerPlayback(AudioPlayer player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    @Override
    public void play(Track track) {
        if (!(track instanceof LavaplayerTrack(var audioTrack))) {
            throw new IllegalArgumentException(
                    "Unsupported track type: " + track.getClass().getName());
        }
        player.playTrack(audioTrack);
    }

    @Override
    public void stop() {
        player.stopTrack();
    }
}

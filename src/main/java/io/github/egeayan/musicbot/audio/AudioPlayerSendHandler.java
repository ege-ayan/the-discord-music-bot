package io.github.egeayan.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import java.nio.ByteBuffer;
import java.util.Objects;
import net.dv8tion.jda.api.audio.AudioSendHandler;

public final class AudioPlayerSendHandler implements AudioSendHandler {

    private static final int MAX_OPUS_FRAME_SIZE = 1024;

    private final AudioPlayer player;
    private final ByteBuffer buffer = ByteBuffer.allocate(MAX_OPUS_FRAME_SIZE);
    private final MutableAudioFrame frame = new MutableAudioFrame();

    public AudioPlayerSendHandler(AudioPlayer player) {
        this.player = Objects.requireNonNull(player, "player");
        this.frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return player.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer.flip();
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}

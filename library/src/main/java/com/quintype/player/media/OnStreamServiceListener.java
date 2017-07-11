package com.quintype.player.media;


import com.quintype.player.media.models.Audio;

public interface OnStreamServiceListener {

    void streamStopped();

    void updateTimerValue(String timeLeft);

    void updateBufferValue(int bufferValue);

    void restoreUI(Audio stream, boolean isPlaying);

    void setLoading();

    void streamPlaying();

    void error(String string);

    void updatePlayerState(StreamService.State state);

}
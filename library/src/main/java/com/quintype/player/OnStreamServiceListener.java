package com.quintype.player;


import com.quintype.player.models.Audio;
/**
 * Created by akshaykoul on 04/07/17.
 */

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

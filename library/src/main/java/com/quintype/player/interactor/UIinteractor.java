package com.quintype.player.interactor;


import com.quintype.player.StreamService;
import com.quintype.player.models.Audio;
/**
 * Created by akshaykoul on 04/07/17.
 */

public interface UIinteractor {

    void initializeUI(Audio stream, boolean isPlaying);

    void setLoading();

    void setToStopped();

    void setToPlaying();

    void updateTimer(String timeLeft);

    void updateBuffer(int value);

    void error(String error);

    void updatePlayerState(StreamService.State state);
}
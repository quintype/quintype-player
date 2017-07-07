package com.quintype.camelot.media.interactor;


import com.quintype.camelot.media.StreamService;
import com.quintype.camelot.media.models.Audio;

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
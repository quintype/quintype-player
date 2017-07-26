package com.quintype.player;


import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import com.quintype.player.interactor.MainInteractor;
import com.quintype.player.interactor.UIinteractor;
import com.quintype.player.models.Audio;
import com.quintype.player.utils.StorageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshaykoul on 04/07/17.
 */

public class MainPresenter implements OnStreamServiceListener {

    private List<UIinteractor> uIinteractors = new ArrayList<>();
    private MainInteractor interactor;

    public MainPresenter(MainInteractor interactor) {

        this.interactor = interactor;
    }

    /**
     * Starts the MediaPlayer service
     */
    public void startService() {

        interactor.startService(this);
    }

    /**
     * Binds the service to the Application.
     */
    public void unBindService() {
        if (getInteractorCount() == 1) {
            interactor.unbindService();
        }
    }

    /**
     * Plays the current stream
     */
    public void playStream() {

        interactor.playStream();
    }

    /**
     * Plays the next item in the stream.(circles to first if the last item in the list is playing)
     */
    public void nextStream() {

        interactor.nextStream();
    }

    /**
     * Plays the previous item in the stream.(circles to last if the first item in the list is
     * playing)
     */
    public void previousStream() {

        interactor.previousStream();
    }

    /**
     * To set up a sleep timer on the media player will stop playback if the set time expires
     *
     * @param ms time in mil seconds.
     */
    public void setSleepTimer(int ms) {

        interactor.setSleepTimer(ms);
    }

    /**
     * Check if wifi only streaming is enabled or not.
     *
     * @return
     */
    public boolean isStreamWifiOnly() {

        return interactor.isStreamWifiOnly();
    }

    /**
     * This is used to enable wifi only streaming.
     *
     * @param checked
     */
    public void setStreamWifiOnly(boolean checked) {

        interactor.setStreamWifiOnly(checked);
    }

    @Override
    public void streamStopped() {

        for (UIinteractor view : uIinteractors) {

            view.bufferingStopped();
        }
    }

    @Override
    public void updatePlayerState(StreamService.State state) {
        for (UIinteractor view : uIinteractors) {

            view.updatePlayerState(state);
        }
    }

    @Override
    public void updateTimerValue(String timeLeft) {

        for (UIinteractor view : uIinteractors) {

            view.updateTimer(timeLeft);
        }
    }

    @Override
    public void updateBufferValue(int bufferValue) {
        for (UIinteractor view : uIinteractors) {

            view.updateBuffer(bufferValue);
        }
    }

    public void restoreUI(Audio stream, boolean isPlaying) {

        for (UIinteractor view : uIinteractors) {

            view.initializeUI(stream, isPlaying);
        }
    }

    public void setLoading() {
        for (UIinteractor view : uIinteractors) {

            view.setBuffering();
        }
    }

    public void streamPlaying() {
        for (UIinteractor view : uIinteractors) {

            view.setToPlaying();
        }
    }

    @Override
    public void error(String error) {
        for (UIinteractor view : uIinteractors) {

            view.error(error);
        }
    }

    private void updatePlaylist(ArrayList<Audio> streams, StorageUtil storage) {
        storage.storeAudio(streams);
        interactor.updatePlaylist(streams);
    }


    /**
     * To play a new list of tracks (Updates the now playing list)
     *
     * @param streams    new list of tracks to be played
     * @param audioIndex Position of the intended song to be played
     * @param storage    Storage Utils to store the preferences locally
     */
    public void playNewTrack(ArrayList<Audio> streams, int audioIndex, StorageUtil storage) {
        updatePlaylist(streams, storage);
        storage.storeAudioIndex(audioIndex);
        interactor.playNewStream(audioIndex);
    }

    /**
     * To play a new track in the same list as the now playing songs
     *
     * @param audioIndex Position of the intended song to be played
     * @param storage    Storage Utils to store the preferences locally
     */
    public void playNewTrack(int audioIndex, StorageUtil storage) {
        storage.storeAudioIndex(audioIndex);
        interactor.playNewStream(audioIndex);
    }

    public int getCurrentMediaPosition() {
        return interactor.getCurrentMediaPosition();
    }


    public boolean isInitialized() {
        return interactor.isInitialized();
    }

    public boolean isMediaPlaying() {
        return interactor.isMediaPlaying();
    }

    public boolean isMediaNotStopped() {
        return interactor.isMediaNotStopped();
    }

    public void seek(int pos) {
        interactor.seek(pos);
    }

    public void addInteractor(UIinteractor view) {
        uIinteractors.add(view);
    }

    public int getInteractorCount() {
        return uIinteractors.size();
    }

    public void removeInteractor(UIinteractor view) {
        uIinteractors.remove(view);
    }

    /**
     * Initializes the MainPresenter
     *
     * @param application Application object (should be initialized in the onCreate of Application
     *                    class)
     * @param cls         The .class file of the Activity which you need to open when a media
     *                    notification is clicked
     * @return MainPresenter
     */

    public static MainPresenter init(Application application, Class<?> cls) {
        MainInteractor interactor = new MainInteractor(application, PreferenceManager
                .getDefaultSharedPreferences(application), (ConnectivityManager)
                application.getSystemService(Context.CONNECTIVITY_SERVICE), new StorageUtil
                (application.getApplicationContext()), cls);
        return new MainPresenter(interactor);
    }
}

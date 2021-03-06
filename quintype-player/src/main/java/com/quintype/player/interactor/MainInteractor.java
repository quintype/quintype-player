package com.quintype.player.interactor;

import android.app.ActivityManager;
import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.quintype.player.R;
import com.quintype.player.OnStreamServiceListener;
import com.quintype.player.StreamService;
import com.quintype.player.models.Audio;
import com.quintype.player.utils.MediaConstants;
import com.quintype.player.utils.StorageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by akshaykoul on 04/07/17.
 */

public class MainInteractor {

    private final static String TAG = MainInteractor.class.getSimpleName();
    private final static String LAST_STREAM_IDENTIFIER = "last_stream_identifier";
    private final static String STREAM_WIFI_ONLY = "stream_wifi_only";

    private Application application;
    private SharedPreferences preferences;
    private ConnectivityManager connectivityManager;

    private StreamService streamService;
    private OnStreamServiceListener presenter;

    private Boolean boundToService = false;
    private StorageUtil storage;

    private List<Audio> streams;
    //    private Audio currentStream;
    private int currentlyPlaying = 0;

    private Class<?> notificationActivity;


    public MainInteractor(Application application, SharedPreferences preferences,
                          ConnectivityManager connectivityManager, StorageUtil storage, Class<?>
                                  cls) {

        this.application = application;
        this.preferences = preferences;
        this.connectivityManager = connectivityManager;
        this.storage = storage;
        this.notificationActivity = cls;
        streams = storage.loadAudio();
        if (streams != null) {
            currentlyPlaying = storage.loadAudioIndex();
//            currentStream = streams.get(currentlyPlaying);
        } else {
            streams = new ArrayList<>();
        }
    }

    public void startService(OnStreamServiceListener presenter) {

        this.presenter = presenter;

        Intent intent = new Intent(application, StreamService.class);
        if (!isServiceAlreadyRunning()) {
            Log.i(TAG, "onStart: service not running, starting service.");
            application.startService(intent);
        }

        if (!boundToService) {
            Log.i(TAG, "onStart: binding to service.");
            boundToService = application.bindService(intent, serviceConnection, Context
                    .BIND_AUTO_CREATE);
        }
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {

        Log.i(TAG, "onStart: registering broadcast receiver.");
        IntentFilter broadcastIntentFilter = new IntentFilter();
        broadcastIntentFilter.addAction(StreamService.STREAM_DONE_LOADING_INTENT);
        broadcastIntentFilter.addAction(StreamService.TIMER_DONE_INTENT);
        broadcastIntentFilter.addAction(StreamService.TIMER_UPDATE_INTENT);
        broadcastIntentFilter.addAction(StreamService.BUFFER_UPDATE_INTENT);
        LocalBroadcastManager.getInstance(application).registerReceiver((broadcastReceiver),
                broadcastIntentFilter);
    }

    public void unbindService() {

        if (boundToService) {
            application.unbindService(serviceConnection);
            boundToService = false;
        }
        LocalBroadcastManager.getInstance(application).unregisterReceiver(broadcastReceiver);

//        preferences.edit().putInt(LAST_STREAM_IDENTIFIER, currentStream != null ?
//                currentlyPlaying : 0).apply();
        storage.storeAudioIndex(currentlyPlaying);
    }

    public void playStream() {

        boolean connectedToWifi = checkIfOnWifi();

        switch (streamService.getState()) {
            case STOPPED:
                if (connectedToWifi || !isStreamWifiOnly()) {
//                    streamService.playStream(currentStream);
                    streamService.playStream();
                    presenter.setBuffering();
                    if (!connectedToWifi) {
                        presenter.error(application.getString(R.string.no_wifi_toast));
                    }
                } else {
                    presenter.error(application.getString(R.string.no_wifi_setting_toast));
                }
                break;
            case PAUSED:
                if (connectedToWifi || !isStreamWifiOnly()) {
                    streamService.resumeStream();
                    presenter.streamPlaying();
                    if (!connectedToWifi) {
                        presenter.error(application.getString(R.string.no_wifi_toast));
                    }
                } else {
                    presenter.error(application.getString(R.string.no_wifi_setting_toast));
                }
                break;
            case PLAYING:
                streamService.pauseStream();
                presenter.streamStopped();
                break;
        }
    }

    public void playNewStream(int pos) {

        if (pos != currentlyPlaying) {
            updateCurrentlyPlaying(pos);
            if (streamService.getState() == StreamService.State.PLAYING || streamService.getState
                    () == StreamService.State.PAUSED) {
                streamService.stopStreaming();
                playStream();
            } else {
                playStream();
            }
        } else {
            if (streamService.getState() == StreamService.State.PLAYING || streamService.getState
                    () == StreamService.State.PAUSED) {
                playStream();
            }
        }
    }

    public void nextStream() {

        if (currentlyPlaying != (streams.size() - 1)) {
            updateCurrentlyPlaying(currentlyPlaying + 1);
        } else {
            updateCurrentlyPlaying(0);
        }

        if (streamService.getState() == StreamService.State.PLAYING || streamService.getState()
                == StreamService.State.PAUSED) {
            streamService.stopStreaming();
            playStream();
        }

    }

    public void previousStream() {

        if (currentlyPlaying != 0) {
            updateCurrentlyPlaying(currentlyPlaying - 1);
        } else {
            updateCurrentlyPlaying(streams.size() - 1);
        }

        if (streamService.getState() == StreamService.State.PLAYING || streamService.getState()
                == StreamService.State.PAUSED) {
            streamService.stopStreaming();
            playStream();
        }

    }

    public void setSleepTimer(int option) {

        if (streamService.getState() == StreamService.State.PLAYING) {
            streamService.setSleepTimer(calculateMs(option));
        } else {
            presenter.error(application.getString(R.string.start_stream_error_toast));
        }
    }

    public boolean isStreamWifiOnly() {

        return preferences.getBoolean(STREAM_WIFI_ONLY, false);
    }

    public void setStreamWifiOnly(boolean checked) {

        if (checked)
            if (((streamService.getState() == StreamService.State.PLAYING) || (streamService
                    .getState() == StreamService.State.PAUSED)))
                if (!checkIfOnWifi()) {
                    streamService.stopStreaming();
                    presenter.streamStopped();
                    presenter.error(application.getString(R.string.toast_no_wifi_but_playing));
                }

        preferences.edit().putBoolean(STREAM_WIFI_ONLY, checked).apply();
    }

    /**
     * See if the StreamService is already running in the background.
     *
     * @return boolean indicating if the service runs
     */
    private boolean isServiceAlreadyRunning() {
        ActivityManager manager = (ActivityManager) application.getSystemService(Context
                .ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer
                .MAX_VALUE)) {
            if (StreamService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the intents the broadcast receiver receives
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {

        if (intent.getAction().equals(StreamService.STREAM_DONE_LOADING_INTENT)) {
            boolean success = intent.getBooleanExtra(StreamService.STREAM_DONE_LOADING_SUCCESS,
                    false);
            if (!success) {
                presenter.streamStopped();
                presenter.error(application.getString(R.string.stream_error_toast));

            } else {
                presenter.streamPlaying();
            }
        } else if (intent.getAction().equals(StreamService.TIMER_DONE_INTENT)) {
            presenter.streamStopped();
        } else if (intent.getAction().equals(StreamService.TIMER_UPDATE_INTENT)) {
            long timerValue = (long) intent.getIntExtra(StreamService.TIMER_UPDATE_VALUE, 0);
            presenter.updateTimerValue(formatTimer(timerValue));
        } else if (intent.getAction().equals(StreamService.BUFFER_UPDATE_INTENT)) {
            int bufferValue = intent.getIntExtra(StreamService.BUFFER_UPDATE_VALUE, 0);
            presenter.updateBufferValue(bufferValue);
        } else
            Log.d(TAG, "doing nothing");

    }

    private String formatTimer(long timeLeft) {

        if (timeLeft > TimeUnit.HOURS.toMillis(1)) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(timeLeft),
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeLeft)),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeft)));
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeLeft)),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeft)));
        }
    }

    private int calculateMs(int option) {
        switch (option) {
            case 0:
                return 0;
            case 1:
                return (int) TimeUnit.MINUTES.toMillis(15);
            case 2:
                return (int) TimeUnit.MINUTES.toMillis(20);
            case 3:
                return (int) TimeUnit.MINUTES.toMillis(30);
            case 4:
                return (int) TimeUnit.MINUTES.toMillis(40);
            case 5:
                return (int) TimeUnit.MINUTES.toMillis(50);
            case 6:
                return (int) TimeUnit.HOURS.toMillis(1);
            case 7:
                return (int) TimeUnit.HOURS.toMillis(2);
            case 8:
                return (int) TimeUnit.HOURS.toMillis(3);
            default:
                return 0;
        }
    }

    private boolean checkIfOnWifi() {

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                return false;
            }
        }
        return true;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       final IBinder service) {
            Log.i(TAG, "onServiceConnected: successfully bound to service.");
            StreamService.StreamBinder binder = (StreamService.StreamBinder) service;
            streamService = binder.getService();
            Audio currentStream = streamService.getPlayingStream();
            streamService.setNotificationActivity(notificationActivity);
            streamService.setStreamUpdatedListener(new StreamService.StreamUpdatedListener() {
                @Override
                public void onStreamUpdated(int pos) {
                    if (pos != currentlyPlaying) {
                        updateCurrentlyPlaying(pos);
                    }
                }

                @Override
                public void playerStateUpdated(StreamService.State state) {
                    updatePlayerState(state);
                }
            });
            if (currentStream != null) {
                presenter.restoreUI(currentStream, streamService.getState() == StreamService
                        .State.PLAYING);
            } else if (!streams.isEmpty()) {
                if (!streams.isEmpty()) {
                    int last = storage.loadAudioIndex();
                    currentStream = streams.get(last);
                    presenter.restoreUI(currentStream, false);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "onServiceDisconnected: disconnected from service.");
            streamService = null;
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            handleIntent(intent);
        }
    };


    public void updatePlaylist(List<Audio> playlist) {
        int currentStreamId = 0;
        if (!streams.isEmpty()) {
            currentStreamId = streams.get(currentlyPlaying).getId();
        }
        streams.clear();
        streams.addAll(playlist);
        if (currentStreamId != 0) {
            for (int i = 0; i < streams.size(); i++) {
                Audio stream = streams.get(i);
                if (currentStreamId == stream.getId()) {
                    currentlyPlaying = i;
                    break;
                }

            }
        }
        storage.storeAudio((ArrayList<Audio>) playlist);
        updateCurrentlyPlaying(currentlyPlaying);
    }

    public void updatePlayerState(StreamService.State state) {
        presenter.updatePlayerState(state);
    }

    public void updateCurrentlyPlaying(int pos) {
        if (!streams.isEmpty()) {
            if ((streams.size() > pos)) {
                currentlyPlaying = pos;

            } else {
                currentlyPlaying = 0;
            }
            storage.storeAudioIndex(currentlyPlaying);
            presenter.restoreUI(streams.get(currentlyPlaying), false);
        }
    }

    public int getCurrentMediaPosition() {
        return streamService.getCurrentStreamPosition();
    }

    public Audio getCurrentStream() {
        return streamService.getCurrentStream();
    }

    /**
     * Check if the player is in playing state
     */
    public boolean isMediaPlaying() {
        return (streamService.getState() == StreamService.State.PLAYING);
    }

    /**
     * Check if the player is not stopped
     */
    public boolean isMediaNotStopped() {
        return (streamService.getState() != StreamService.State.STOPPED);
    }

    /**
     * Seek up to a position on a media player
     *
     * @param pos
     */
    public void seek(int pos) {
        streamService.seek(pos);
    }

    public boolean isInitialized() {
        return streamService.isInitialized();
    }

    public void downloadPodcast(String streamURL, Integer trackID) {
        Uri downloadUri = Uri.parse(streamURL);

        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(trackID + MediaConstants.MP3_EXTENSION);
        request.setDescription(MediaConstants.DOWNLOAD_DESCRIPTION);
        /*The download directory is set to the package name by default, and the file name is trackID with ".mp3" extension*/
        //TODO For time being we are assuming all the files as mp3, but it can be of any format. Need to get the format from server.
        request.setDestinationInExternalPublicDir(application.getPackageName(), trackID + MediaConstants.MP3_EXTENSION);

        DownloadManager downloadManager = (DownloadManager) application.getSystemService(DOWNLOAD_SERVICE);
        /*We are storing the download ID and track ID in a HashMap . We will be getting only the downloadID from the Intent of the broadcast receiver,
        so to confirm which track is being downloaded we need this HashMap*/
        long id = downloadManager.enqueue(request);
        storage.addToDownloadManagerHistory(id, trackID);
    }

}

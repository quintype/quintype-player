package com.quintype.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.quintype.player.models.Audio;
import com.quintype.player.utils.MediaConstants;
import com.quintype.player.utils.StorageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;

/**
 * Created by akshaykoul on 04/07/17.
 */

public class StreamService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = StreamService.class.getSimpleName();

    private final float MAX_VOLUME = 1.0f;


    public static final String ACTION_PLAY = "com.quintype.musicstreaming.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.quintype.musicstreaming.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.quintype.musicstreaming.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.quintype.musicstreaming.ACTION_NEXT";
    public static final String ACTION_STOP = "com.quintype.musicstreaming.ACTION_STOP";

    public static final String STREAM_DONE_LOADING_INTENT = "stream_done_loading_intent";
    public static final String STREAM_DONE_LOADING_SUCCESS = "stream_done_loading_success";
    public static final String TIMER_UPDATE_INTENT = "timer_update_intent";
    public static final String BUFFER_UPDATE_INTENT = "buffer_update_intent";
    public static final String TIMER_UPDATE_VALUE = "timer_update_value";
    public static final String BUFFER_UPDATE_VALUE = "buffer_update_value";
    public static final String TIMER_DONE_INTENT = "timer_done_intent";


    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private AudioManager mAudioManager;
    private Class<?> notificationActivity;

    public enum State {PAUSED, STOPPED, PLAYING, PREPARING}

    private State state = State.STOPPED;

    private final IBinder streamBinder = new StreamBinder();
    private MediaPlayer player;
    private Audio currentStream;
    List<Audio> nowPlayingList;
    int nowPlayingPosition = 0;
    private LocalBroadcastManager broadcastManager;
    private CountDownTimer countDownTimer;
    StreamUpdatedListener streamUpdatedListener;
    private static final int STOP_DELAY = 30000;
//    boolean isUIUnbinded = false;

    HeadphonesStateReceiver headphoneStateReceiver;
    private StorageUtil storageUtil;

    public void onCreate() {
        super.onCreate();
        try {
            initMediaSession();
        } catch (RemoteException e) {
        }

        storageUtil = new StorageUtil(getApplicationContext());
        player = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // only for lollipop and newer versions
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());
        } else {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        }
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnBufferingUpdateListener(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        headphoneStateReceiver = new HeadphonesStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headphoneStateReceiver, filter);

    }

    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    public class StreamBinder extends Binder {
        public StreamService getService() {
            return StreamService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
//        toBackground(false);
//        isUIUnbinded = false;
        return streamBinder;
    }

    @Override
    public void onRebind(Intent intent) {
//        toBackground(true);
//        isUIUnbinded = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (player.isPlaying() || state == State.PREPARING) {
//            toForeground();
        }
//        isUIUnbinded = true;
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Intent intent = new Intent(BUFFER_UPDATE_INTENT);
        intent.putExtra(BUFFER_UPDATE_VALUE, i);
        broadcastManager.sendBroadcast(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void toBackground(final boolean removeNotification) {
        stopForeground(removeNotification);
    }

    /**
     * Run the service in the foreground
     * and show a notification
     */

    public void setNotificationActivity(Class<?> notificationActivity) {
        this.notificationActivity = notificationActivity;
    }


    public void showNotification() {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (state == State.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (state == State.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        } else if (state == State.PREPARING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action by default
            play_pauseAction = playbackAction(1);
        }

        Intent contentIntent = new Intent(this, notificationActivity);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        contentIntent.putExtra(MediaConstants.NOTIFICATION_TYPE_MEDIA, true);
        int requestID = (int) System.currentTimeMillis();
        final PendingIntent contentPendingIntent = PendingIntent.getActivity
                (getApplicationContext(),
                        requestID, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent closeIntent = new Intent(getApplicationContext(), StreamService.class);
        closeIntent.setAction(ACTION_STOP);
        PendingIntent pendingCloseIntent = PendingIntent.getService(getApplicationContext(), 1,
                closeIntent, 0);


        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_empty_music2); //replace with your own image

        // Create a new Notification
        final NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new
                NotificationCompat.Builder(this)
                .setContentIntent(contentPendingIntent)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(pendingCloseIntent))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.black_opacity_50))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)

                // Set Notification content information
                .setContentText(currentStream.getArtist())
                .setContentTitle(currentStream.getGenre())
                .setContentInfo(currentStream.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify
//                (NOTIFICATION_ID, notificationBuilder.build());

        setNotificationPlaybackState(notificationBuilder);
        if (!TextUtils.isEmpty(currentStream.getArtwork())) {
            Glide.with(this)
                    .load(currentStream.getArtwork())
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super
                                Bitmap> glideAnimation) {
                            notificationBuilder.setLargeIcon(resource);
                            Notification notification = notificationBuilder.build();
                            startForeground(MediaConstants.MEDIA_NOTIFY_ID, notification);
                            if (state == State.PAUSED) {
                                toBackground(false);
                            }
                        }
                    });
        }
        Notification notification = notificationBuilder.build();
        startForeground(MediaConstants.MEDIA_NOTIFY_ID, notification);
    }

    /**
     * Handle intent from notification
     *
     * @param playbackAction intent to add pending intent to
     */
    private void handleIntent(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            Log.i(TAG, "handleIntent: stopping stream from notification");

            stopStreaming();
            stopSleepTimer();
            toBackground(true);
            stopSelf();
        }
    }

    public State getState() {

        return state;
    }

    public void setStreamUpdatedListener(StreamUpdatedListener streamUpdatedListener) {
        this.streamUpdatedListener = streamUpdatedListener;
    }

    /**
     * Start play a stream
     */
    public void playStream() {

        int status = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        nowPlayingList = storageUtil.loadAudio();
        nowPlayingPosition = storageUtil.loadAudioIndex();
        Audio stream = nowPlayingList.get(nowPlayingPosition);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        // If a stream was already running stop it and reset
        if (player.isPlaying()) {
            player.stop();
        }

        player.reset();

        try {
            state = State.PREPARING;

//            List<String> keys = Arrays.asList(getResources().getStringArray(R.array.api_keys));
//            String key = keys.get((new Random()).nextInt(keys.size()));

            if (stream.isDownloaded()) {
                /*The audio file is downloaded, hence the data source should be the file descriptor of the downloaded file */
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + this.getPackageName() + "/" + stream.getId() + MediaConstants.MP3_EXTENSION;

                File downloadedFile = new File(filePath);
                Log.d("## filePath", downloadedFile.getPath());
                FileInputStream fileInputStream = new FileInputStream(downloadedFile);
                player.setDataSource(fileInputStream.getFD());
                fileInputStream.close();
            } else {
                /* Audio file not available in local storage, */
                player.setDataSource(this, Uri.parse(stream.getStreamUrl()));
            }


//            player.setDataSource(this, Uri.parse(String.format("%s?client_id=%s", stream
//                    .getStreamUrl(), getString(R.string.soundcloud_client_id))));
            player.setLooping(false);
            player.setVolume(MAX_VOLUME, MAX_VOLUME);
            this.currentStream = stream;
        } catch (Exception e) {
            Log.e(TAG, "playStream: ", e);
        }
        player.prepareAsync();
        showNotification();
    }

    public void pauseStream() {

        if (state == State.PLAYING) {

            mDelayedStopHandler.removeCallbacksAndMessages(null);
            mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
            player.pause();
            stopSleepTimer();
            state = State.PAUSED;
        }

        streamUpdatedListener.playerStateUpdated(state);
//        if (isUIUnbinded) {
        showNotification();
//            toForeground();
        toBackground(false);
//        }
    }

    public void resumeStream() {

        try {
            if (state == State.PAUSED) {

                mDelayedStopHandler.removeCallbacksAndMessages(null);
                player.start();
                state = State.PLAYING;
            }

            streamUpdatedListener.playerStateUpdated(state);
//        if (isUIUnbinded) {
//            toForeground();
//        }
            showNotification();
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            NotificationManager notificationManager = (NotificationManager) getApplicationContext
                    ().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(MediaConstants.MEDIA_NOTIFY_ID);
        }
    }

    /**
     * Stop the MediaPlayer if something is streaming
     */
    public void stopStreaming() {

        if (state == State.PLAYING || state == State.PAUSED) {
            mDelayedStopHandler.removeCallbacksAndMessages(null);
            mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
            player.stop();
            player.reset();
            state = State.STOPPED;
        }
    }

    /**
     * Get the stream that is playing right now, if any
     *
     * @return the playing stream or null
     */
    public Audio getPlayingStream() {

        if (state == State.PLAYING || state == State.PAUSED) {
            return currentStream;
        }
        return null;
    }


    /**
     * Get the current position of stream
     *
     * @return the playing stream or null
     */
    public int getCurrentStreamPosition() {

        if (state == State.PLAYING || state == State.PAUSED) {
            return player.getCurrentPosition();
        }
        return 0;
    }

    /**
     * Get currently playing stream Audio
     *
     * @return Audio
     */
    public Audio getCurrentStream() {

        if (state == State.PLAYING || state == State.PAUSED) {
            return currentStream;
        }
        return null;
    }

    /**
     * Set a sleep timer
     *
     * @param milliseconds to wait before sleep
     */
    public void setSleepTimer(int milliseconds) {
        Log.i(TAG, "setSleepTimer: setting sleep timer for " + milliseconds + "ms");

        stopSleepTimer();

        if (milliseconds != 0) {

            countDownTimer = new CountDownTimer(milliseconds, 1000) {

                public void onTick(long millisUntilFinished) {
                    Intent intent = new Intent(TIMER_UPDATE_INTENT);
                    intent.putExtra(TIMER_UPDATE_VALUE, (int) millisUntilFinished);
                    broadcastManager.sendBroadcast(intent);
                    if (millisUntilFinished < TimeUnit.SECONDS.toMillis(30)) {
                        //lower the volume by respective step
                        lowerVolume((int) ((int) millisUntilFinished / TimeUnit.SECONDS.toMillis
                                (1)));
                    }
                }

                public void onFinish() {
                    stopStreaming();
                    stopSleepTimer();
                    timerDoneBroadcast();
                    toBackground(true);
                }

            }.start();
        }
    }

    /**
     * Lowers the volume of the stream to a step
     *
     * @param step out of a max of 30
     */
    private void lowerVolume(int step) {

        float voulme = ((float) step) / 30f;
        player.setVolume(voulme, voulme);
    }

    /**
     * Stop the sleep timer and restore volume to max
     */
    private void stopSleepTimer() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        player.setVolume(MAX_VOLUME, MAX_VOLUME);
    }

    private void timerDoneBroadcast() {
//        setSleepTimer: sleep timer is done, notifying bindings.

        Intent intent = new Intent(TIMER_DONE_INTENT);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNextStream();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        mp.reset();
        notifyStreamLoaded(false);
        state = State.STOPPED;
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        notifyStreamLoaded(true);
        mp.start();
        state = State.PLAYING;
    }

    /**
     * Send out a broadcast indicating stream was started with success or couldn't be found
     *
     * @param success
     */
    private void notifyStreamLoaded(boolean success) {

        Intent intent = new Intent(STREAM_DONE_LOADING_INTENT);
        intent.putExtra(STREAM_DONE_LOADING_SUCCESS, success);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        toBackground(true);
        unregisterReceiver(headphoneStateReceiver);
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
    }


    /**
     * Seek up to a position on a media player
     *
     * @param position seek position
     */
    public void seek(int position) {
        if (player != null) {
            if (position < 0) {
                position = 0;
            } else if (position > currentStream.getDuration()) {
                position = currentStream.getDuration();
            }
            player.seekTo((int) position);
//            notifyChange(POSITION_CHANGED);
        }
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, StreamService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeStream();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseStream();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                playNextStream();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                playPreviousStream();
            }

            @Override
            public void onStop() {
                super.onStop();
                toBackground(true);

            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void playNextStream() {
        try {
            if (nowPlayingPosition != (nowPlayingList.size() - 1)) {
                updateCurrentlyPlaying(nowPlayingPosition + 1);
            } else {
                updateCurrentlyPlaying(0);
            }

            if (state == State.PLAYING || state
                    == State.PAUSED) {
                stopStreaming();
                playStream();
                //            if (isUIUnbinded) {
                //                toForeground();
                //            }
                showNotification();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            NotificationManager notificationManager = (NotificationManager) getApplicationContext
                    ().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(MediaConstants.MEDIA_NOTIFY_ID);
        }
    }

    private void playPreviousStream() {
        try {
            if (nowPlayingPosition != 0) {
                updateCurrentlyPlaying(nowPlayingPosition - 1);
            } else {
                updateCurrentlyPlaying(nowPlayingList.size() - 1);
            }

            if (state == State.PLAYING || state == State.PAUSED) {
                stopStreaming();
                playStream();
                //                    toForeground();
                showNotification();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            NotificationManager notificationManager = (NotificationManager) getApplicationContext
                    ().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(MediaConstants.MEDIA_NOTIFY_ID);
        }
    }

    private void updateMetaData() {
        if (currentStream != null) {
            // Update the current metadata
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, currentStream
                            .getArtwork())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentStream.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, currentStream.getGenre())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentStream.getTitle())
                    .build());
        }
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager
            .OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(final int focusChange) {
            if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
                pauseStream();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
                resumeStream();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mAudioManager.abandonAudioFocus(this);
                // Stop playback
                pauseStream();
            } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                pauseStream();
            }
        }
    };

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<StreamService> mWeakReference;

        private DelayedStopHandler(StreamService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            StreamService service = mWeakReference.get();
            if (service != null && service.player != null) {
                if (service.player.isPlaying() || service.state == State.PLAYING) {
//                    Ignoring delayed stop since the media player is in use.
                    return;
                }
//                Stopping service with delay handler.
                service.stopSelf();
            }
        }
    }

    private void updateCurrentlyPlaying(int pos) {
        nowPlayingPosition = pos;
        storageUtil.storeAudioIndex(pos);
        streamUpdatedListener.onStreamUpdated(pos);
    }

    public boolean isInitialized() {
        return currentStream != null;
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (state == State.PLAYING
                && getCurrentStreamPosition() >= 0) {
            builder.setWhen(System.currentTimeMillis() - getCurrentStreamPosition()).setShowWhen
                    (true).setUsesChronometer(true);
        } else {
            builder.setWhen(0).setShowWhen(false).setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(state == State.PLAYING);
    }


    public interface StreamUpdatedListener {
        public void playerStateUpdated(State state);

        public void onStreamUpdated(int pos);
    }

    public class HeadphonesStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isInitialStickyBroadcast() && intent.getAction().equals(Intent
                    .ACTION_HEADSET_PLUG)) {
                int headsetState = intent.getIntExtra("state", -1);
                switch (headsetState) {
                    case 0:
//                        Headset is unplugged
                        if (state == State.PLAYING) {
                            pauseStream();
                        }
                        break;
                    case 1:
//                       Headset is plugged
                        if (state == State.PAUSED) {
                            resumeStream();
                        }
                        break;
                    default:
//                        I have no idea what the headset state is
                }
            }
        }
    }
}
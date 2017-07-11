package com.quintype.camelotplayer.activities;

import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.quintype.camelot.media.StreamService;
import com.quintype.camelot.media.models.Audio;
import com.quintype.camelot.media.models.NowPlaying;
import com.quintype.camelot.media.utils.StorageUtil;
import com.quintype.camelotplayer.Constants;
import com.quintype.camelotplayer.R;
import com.quintype.camelotplayer.fragments.QuickControlsFragment;
import com.quintype.camelotplayer.fragments.SoundcloudListFragment;
import com.quintype.camelotplayer.widgets.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MainActivity extends PlayerFragmentActivity {


    public static final String TAG = MainActivity.class.getName();
    boolean quickControlsInitialized = false;

    QuickControlsFragment quickControlsFragment = new QuickControlsFragment();
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.sliding_layout)
    SlidingUpPanelLayout slidingLayout;
    Unbinder unbinder;
    private StorageUtil storageUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        toolbar.getContext();
        setPanelSlideListeners(slidingLayout);
        slidingLayout.hidePanel();
        storageUtil = new StorageUtil(getApplicationContext());
        setSupportActionBar(toolbar);
        new InitQuickControls().execute("");
        addFragment(SoundcloudListFragment.create(), SoundcloudListFragment.class.getSimpleName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackStackChanged() {

    }

    @Override
    public void initializeUI(Audio stream, boolean isPlaying) {
        Log.d(TAG, "Playing " + stream.getTitle());
        slidingLayout.showPanel();
        quickControlsFragment.initializePlayer(stream, isPlaying);

    }

    @Override
    public void setLoading() {
        quickControlsFragment.setLoading();
    }

    @Override
    public void setToStopped() {
        quickControlsFragment.setToStopped();
    }

    @Override
    public void setToPlaying() {
        quickControlsFragment.setToPlaying();
    }

    @Override
    public void updateTimer(String timeLeft) {
    }

    @Override
    public void updateBuffer(int value) {
        quickControlsFragment.updateBufferedProgress(value);
    }

    @Override
    public void error(String error) {
        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updatePlayerState(StreamService.State state) {
        quickControlsFragment.updateState(state == StreamService.State.PLAYING);

    }

    @Override
    public void clickAnalyticsEvent(String categoryId, String actionId, String labelId, long
            value) {

    }

    @Override
    public void propagateEvent(Pair<String, Object> event) {
        switch (event.first) {
            case Constants.EVENT_TRACK_CLICK:
                ArrayList<Audio> nowPlayingList = ((NowPlaying) event.second).getmNowPlayingList();
                int nowPlayingPos = ((NowPlaying) event.second).getmNowplayingPosition();
                Audio audio = nowPlayingList.get(nowPlayingPos);
                Toast.makeText(mContext, "Track clicked =" + audio.getTitle(), Toast.LENGTH_SHORT);
                presenter.playNewTrack(nowPlayingList, nowPlayingPos, storageUtil);
                break;
            case Constants.EVENT_PLAY_PAUSE_CLICK:
                presenter.playStream();
                break;
            case Constants.EVENT_NEXT_CLICK:
                presenter.nextStream();
                break;
            case Constants.EVENT_PREVIOUS_CLICK:
                presenter.previousStream();
            case Constants.EVENT_SEEK_TEN_SEC_BACK:
                int pos = getCurrentTrackPosition();
                presenter.seek(pos - Constants.TEN_SECONDS_IN_MILIS);
                break;
            case Constants.EVENT_UPDATE_PLAYLIST:
//                playList.clear();
//                playList.addAll(getAudioFromTracks((ArrayList<Track>) event.second));
//                presenter.updatePlaylist(playList);
                break;
            default:
                Toast.makeText(mContext, "Unhandled event " + event.first, Toast.LENGTH_SHORT)
                        .show();
        }
    }

    @Override
    public int getCurrentTrackPosition() {
        return presenter.getCurrentMediaPosition();
    }

    @Override
    public boolean isPlaybackNotStopped() {
        return presenter.isMediaNotStopped();
    }

    @Override
    public void seek(int position) {
        presenter.seek(position);
    }

    public class InitQuickControls extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            FragmentManager fragmentManager1 = getFragmentManager();
            fragmentManager1.beginTransaction()
                    .replace(R.id.quick_controls_container, quickControlsFragment)
                    .commitAllowingStateLoss();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            quickControlsInitialized = true;
            QuickControlsFragment.topContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        // only for gingerbread and newer versions
//                        Intent intent = new Intent(MainActivity.this, NowPlayingActivity.class);
//                        ActivityOptionsCompat options = ActivityOptionsCompat.
//                                makeSceneTransitionAnimation(MainActivity.this,
//                                        quickControlsContainer, "player");
//                        startActivity(intent, options.toBundle());
//                    } else {
//                        startActivity(new Intent(MainActivity.this, NowPlayingActivity.class));
//                    }

                }
            });
        }

        @Override
        protected void onPreExecute() {
        }
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    public void setPanelSlideListeners(SlidingUpPanelLayout panelLayout) {
        panelLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                View nowPlayingCard = QuickControlsFragment.topContainer;
                nowPlayingCard.setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelCollapsed(View panel) {
                View nowPlayingCard = QuickControlsFragment.topContainer;
                nowPlayingCard.setAlpha(1);
            }

            @Override
            public void onPanelExpanded(View panel) {
                View nowPlayingCard = QuickControlsFragment.topContainer;
                nowPlayingCard.setAlpha(0);
            }

            @Override
            public void onPanelAnchored(View panel) {

            }

            @Override
            public void onPanelHidden(View panel) {

            }
        });
    }
}

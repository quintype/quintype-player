/*
 * Copyright (C) 2015 Naman Dwivedi
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.quintype.musicplayer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.quintype.player.models.Audio;
import com.quintype.musicplayer.Constants;
import com.quintype.musicplayer.R;
import com.quintype.musicplayer.Utilities;
import com.quintype.musicplayer.widgets.PlayPauseButton;

public class QuickControlsFragment extends Fragment {


    public static View topContainer;
    private ProgressBar mProgress;
    private SeekBar mSeekBar;
    Audio currentStream;
    TextView songDuration;

    Handler seekBarHandler = new Handler();
    public Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {

            if (callbacks != null) {
                int position = callbacks.getCurrentTrackPosition();
                mProgress.setProgress(position);
                mSeekBar.setProgress(position);
                if (songDuration != null)
                    songDuration.setText(String.format("- %s", Utilities.makeShortTimeString
                            (getActivity(), (currentStream.getDuration() - position) / 1000)));

                if (callbacks.isPlaybackNotStopped()) {
                    mProgress.postDelayed(mUpdateProgress, 50);
                } else mProgress.removeCallbacks(this);
            }

        }
    };
    private PlayPauseButton mPlayPause, mPlayPauseExpanded;
    private TextView mTitle, mTitleExpanded;
    private TextView mArtist, mArtistExpanded;
    private ImageView mBlurredArt;
    private View rootView;
    private View playPauseWrapper, playPauseWrapperExpanded;
    private ImageView previous, next;
    private ProgressBar streamProgress, streamProgressExpanded;
    private boolean dueToPlayPause = false;
    private final View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dueToPlayPause = true;
            if (!mPlayPause.isPlayed()) {
                mPlayPause.setPlayed(true);
                mPlayPause.startAnimation();
            } else {
                mPlayPause.setPlayed(false);
                mPlayPause.startAnimation();
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callbacks.propagateEvent(new Pair<String, Object>(Constants
                            .EVENT_PLAY_PAUSE_CLICK
                            , Constants.EVENT_PLAY_PAUSE_CLICK));
                }
            }, 200);

        }
    };
    private final View.OnClickListener mPlayPauseExpandedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dueToPlayPause = true;
            if (!mPlayPauseExpanded.isPlayed()) {
                mPlayPauseExpanded.setPlayed(true);
                mPlayPauseExpanded.startAnimation();
            } else {
                mPlayPauseExpanded.setPlayed(false);
                mPlayPauseExpanded.startAnimation();
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    callbacks.propagateEvent(new Pair<String, Object>(Constants
                            .EVENT_PLAY_PAUSE_CLICK
                            , Constants.EVENT_PLAY_PAUSE_CLICK));
                }
            }, 200);

        }
    };
    private FragmentCallbacks callbacks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
        this.rootView = rootView;

        mPlayPause = (PlayPauseButton) rootView.findViewById(R.id.play_pause);
        mPlayPauseExpanded = (PlayPauseButton) rootView.findViewById(R.id.playpause);
        playPauseWrapper = rootView.findViewById(R.id.play_pause_wrapper);
        playPauseWrapperExpanded = rootView.findViewById(R.id.playpausewrapper);
        playPauseWrapper.setOnClickListener(mPlayPauseListener);
        playPauseWrapperExpanded.setOnClickListener(mPlayPauseExpandedListener);
        mProgress = (ProgressBar) rootView.findViewById(R.id.song_progress_normal);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.song_progress);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mArtist = (TextView) rootView.findViewById(R.id.artist);
        songDuration = (TextView) rootView.findViewById(R.id.song_duration_remaining);
        mTitleExpanded = (TextView) rootView.findViewById(R.id.song_title);
        mArtistExpanded = (TextView) rootView.findViewById(R.id.song_artist);
        mBlurredArt = (ImageView) rootView.findViewById(R.id.blurred_album_art);
        next = (ImageView) rootView.findViewById(R.id.next);
        previous = (ImageView) rootView.findViewById(R.id.previous);
        topContainer = rootView.findViewById(R.id.topContainer);
        streamProgress = (ProgressBar) rootView.findViewById(R.id.pb_playback_control);
        streamProgressExpanded = (ProgressBar) rootView.findViewById(R.id
                .pb_playback_control_expanded);

        mTitle.setSelected(true);
        mTitleExpanded.setSelected(true);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mProgress
                .getLayoutParams();
        mProgress.measure(0, 0);
        layoutParams.setMargins(0, -(mProgress.getMeasuredHeight() / 2), 0, 0);
        mProgress.setLayoutParams(layoutParams);

        mPlayPause.setColor(Color.WHITE);
        mPlayPauseExpanded.setColor(Color.WHITE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b && callbacks.isPlaybackNotStopped()) {
                    callbacks.seek(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        callbacks.propagateEvent(new Pair<String, Object>(Constants
                                .EVENT_NEXT_CLICK
                                , Constants.EVENT_NEXT_CLICK));
                    }
                }, 200);

            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        callbacks.propagateEvent(new Pair<String, Object>(Constants
                                .EVENT_SEEK_TEN_SEC_BACK
                                , Constants.EVENT_SEEK_TEN_SEC_BACK));
                    }
                }, 200);

            }
        });


        return rootView;
    }

    public void updateNowplayingCard(Audio stream, boolean isPlaying) {
        mTitle.setText(stream.getTitle());
        mArtist.setText(stream.getArtist());
        mTitleExpanded.setText(stream.getTitle());
        mArtistExpanded.setText(stream.getArtist());
        if (!dueToPlayPause) {
            Glide.with(this).load(stream.getArtwork()).error(R.drawable.ic_empty_music2).into
                    (mBlurredArt);

        }
        dueToPlayPause = false;
        mProgress.setMax(stream.getDuration());
        mSeekBar.setMax(stream.getDuration());
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
        mSeekBar.setProgress(0);
        mSeekBar.setSecondaryProgress(0);
        seekBarHandler.postDelayed(mUpdateProgress, 500);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onResume() {
        super.onResume();
        topContainer = rootView.findViewById(R.id.topContainer);

    }

    public void updateState(boolean isPlaying) {
        if (isPlaying) {
            if (!mPlayPause.isPlayed()) {
                mPlayPause.setPlayed(true);
                mPlayPause.startAnimation();
            }
            if (!mPlayPauseExpanded.isPlayed()) {
                mPlayPauseExpanded.setPlayed(true);
                mPlayPauseExpanded.startAnimation();
            }
        } else {
            if (mPlayPause.isPlayed()) {
                mPlayPause.setPlayed(false);
                mPlayPause.startAnimation();
            }
            if (mPlayPauseExpanded.isPlayed()) {
                mPlayPauseExpanded.setPlayed(false);
                mPlayPauseExpanded.startAnimation();
            }
        }
    }

    public void initializePlayer(Audio stream, boolean isPlaying) {
        currentStream = stream;
        updateNowplayingCard(stream, isPlaying);
        updateState(isPlaying);
    }

    public void startBuffering() {

        streamProgress.setVisibility(View.VISIBLE);
        streamProgressExpanded.setVisibility(View.VISIBLE);
        playPauseWrapper.setEnabled(false);
        playPauseWrapperExpanded.setEnabled(false);
//        mPlayPause.setVisibility(View.INVISIBLE);
//        mPlayPauseExpanded.setVisibility(View.INVISIBLE);
//        nextButton.setEnabled(false);
//        previousButton.setEnabled(false);
//        showSoundsButton.setEnabled(false);
//
//        playButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_48dp));
    }

    public void setToPlaying() {

        stopLoading();

        updateState(true);
    }

    public void stopBuffering() {

        stopLoading();
        updateState(false);
    }

    public void updateBufferedProgress(int bufferValue) {
        double ratio = bufferValue / 100.0;
        if (currentStream != null) {
            double bufferingLevel = (int) (currentStream.getDuration() * ratio);
            if (mProgress != null) {
                mProgress.setSecondaryProgress((int) bufferingLevel);
            }
            if (mSeekBar != null) {
                mSeekBar.setSecondaryProgress((int) bufferingLevel);
            }
        }
    }


    private void stopLoading() {

        streamProgress.setVisibility(View.INVISIBLE);
        streamProgressExpanded.setVisibility(View.INVISIBLE);
        playPauseWrapper.setEnabled(true);
        playPauseWrapperExpanded.setEnabled(true);
//        mPlayPause.setVisibility(View.VISIBLE);
//        mPlayPauseExpanded.setVisibility(View.VISIBLE);
//        nextButton.setEnabled(true);
//        previousButton.setEnabled(true);
//        showSoundsButton.setEnabled(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentCallbacks) {
            callbacks = (FragmentCallbacks) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement MusicFragmentCallbacks");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }
}

package com.quintype.musicplayer.fragments;

import android.app.Fragment;
import android.util.Pair;


public interface FragmentCallbacks {

    void addFragment(Fragment fragment, String mBackStack);

    void replaceFragment(Fragment fragment, String mBackStack);

    void clickAnalyticsEvent(String categoryId, String actionId, String labelId, long value);

//    public Fragment getmFragment();

//    public void popCurrentFragment();

    void propagateEvent(Pair<String, Object> event);

    int getCurrentTrackPosition();

    boolean isPlaybackNotStopped();

    void seek(int position);
}

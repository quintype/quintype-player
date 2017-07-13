package com.quintype.musicplayer;

import android.app.Application;

import com.quintype.player.MainPresenter;
import com.quintype.musicplayer.activities.MainActivity;

/**
 * Created by akshaykoul on 11/07/17.
 */

public class SampleApplication extends Application {


    MainPresenter presenter;

    @Override
    public void onCreate() {
        super.onCreate();
        presenter = MainPresenter.init(this, MainActivity.class);
    }

    public MainPresenter getPresenter() {
        return presenter;
    }
}

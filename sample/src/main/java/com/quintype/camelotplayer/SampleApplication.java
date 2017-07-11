package com.quintype.camelotplayer;

import android.app.Application;

import com.quintype.camelot.media.MainPresenter;
import com.quintype.camelotplayer.activities.MainActivity;

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

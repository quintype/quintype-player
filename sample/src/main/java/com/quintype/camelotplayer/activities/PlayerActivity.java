package com.quintype.camelotplayer.activities;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.quintype.camelot.media.MainPresenter;
import com.quintype.camelot.media.interactor.UIinteractor;
import com.quintype.camelotplayer.SampleApplication;

public abstract class PlayerActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener, UIinteractor {

    MainPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = ((SampleApplication) getApplication()).getPresenter();
        presenter.addInteractor(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.startService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.unBindService();
    }

    @Override
    protected void onDestroy() {
        presenter.removeInteractor(this);
        super.onDestroy();
    }
}

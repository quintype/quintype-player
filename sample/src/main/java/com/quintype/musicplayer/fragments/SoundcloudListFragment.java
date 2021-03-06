package com.quintype.musicplayer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.quintype.musicplayer.Constants;
import com.quintype.musicplayer.R;
import com.quintype.musicplayer.adapters.TrackAdapter;
import com.quintype.musicplayer.api.SoundCloudApiClient;
import com.quintype.musicplayer.models.Track;

import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

public class SoundcloudListFragment extends Fragment {


    RecyclerView rvRecyclerView;
    FrameLayout llMainContainer;
    LinearLayout llRetry;
    AppCompatButton retryButton;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeContainer;
    TrackAdapter trackAdapter;

    private FragmentCallbacks callbacks;

    public SoundcloudListFragment() {
        // Required empty public constructor
    }

    public static SoundcloudListFragment create() {
        SoundcloudListFragment fragment = new SoundcloudListFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_soundcloud_list, container, false);
        rvRecyclerView = (RecyclerView) view.findViewById(R.id
                .fragment_soundcloud_list_rv_recyclerview);
        llMainContainer = (FrameLayout) view.findViewById(R.id
                .fragment_soundcloud_ll_main_container);
        progressBar = (ProgressBar) view.findViewById(R.id.pb_tag_fragment);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        trackAdapter = new TrackAdapter(callbacks);
        rvRecyclerView.setAdapter(trackAdapter);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                trackAdapter.clearAll();
                loadTracks();
            }
        });
        loadTracks();
        return view;
    }

    private void loadTracks() {
        String[] querylist = getResources().getStringArray(R.array.soundcloud_random_query);
        Random random = new Random();
        int pos = random.nextInt(querylist.length);
        Log.d(TAG, "Making the Call");
        SoundCloudApiClient.getApiService().searchTracks(querylist[pos], getString(R.string
                .soundcloud_api_key)).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                swipeContainer.setRefreshing(false);
                if (response.isSuccessful()) {
                    trackAdapter.addTracks(response.body());
                    progressBar.setVisibility(View.GONE);
                    callbacks.propagateEvent(new Pair<String, Object>(Constants
                            .EVENT_UPDATE_PLAYLIST, response.body()));
                } else {
                    Log.d(TAG, "Call failure");
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                swipeContainer.setRefreshing(false);
                Log.d(TAG, "Call failure");
            }
        });
    }

//    public void onButtonPressed(Uri uri) {
//        if (callbacks != null) {
//            callbacks.propagateEvent();
//        }
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentCallbacks) {
            callbacks = (FragmentCallbacks) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

}

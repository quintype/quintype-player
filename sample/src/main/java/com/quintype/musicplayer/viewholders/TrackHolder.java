package com.quintype.camelotplayer.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.quintype.camelotplayer.R;
import com.quintype.camelotplayer.models.Track;

import java.net.URL;

import static com.quintype.camelotplayer.adapters.TrackAdapter.getHirezArtwork;

/**
 * Created by akshaykoul on 06/04/17.
 */

public class TrackHolder extends RecyclerView.ViewHolder {
    LinearLayout llChildMainContainer;
    ImageView ivChildImage;
    ImageView ivChildPlayIcon;
    TextView tvChildTitle;
    RequestManager glideRequestManager;
    URL url;

    public TrackHolder(View itemView) {
        super(itemView);
        glideRequestManager = Glide.with(itemView.getContext());
    }

    public static TrackHolder create(View view) {
        TrackHolder trackHolder = new TrackHolder(view);
        trackHolder.llChildMainContainer = (LinearLayout) view.findViewById(R.id.watch_list_item_ll_main_container);
        trackHolder.ivChildImage = (ImageView) view.findViewById(R.id.watch_list_item_iv_header_image);
        trackHolder.ivChildPlayIcon = (ImageView) view.findViewById(R.id.list_watch_item_iv_play_icon);
        trackHolder.tvChildTitle = (TextView) view.findViewById(R.id.watch_list_item_tv_header_title);

        trackHolder.ivChildPlayIcon.setVisibility(View.GONE);
        return trackHolder;
    }

    public void bind(Track track) {
        tvChildTitle.setText(track.getTitle().trim());

        glideRequestManager.load(getHirezArtwork(track.getArtworkUrl())).error(R.drawable
                .ic_empty_music2).into(ivChildImage);
    }

}

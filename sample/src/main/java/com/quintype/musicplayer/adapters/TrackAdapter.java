package com.quintype.camelotplayer.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.quintype.player.media.models.Audio;
import com.quintype.player.media.models.NowPlaying;
import com.quintype.camelotplayer.Constants;
import com.quintype.camelotplayer.R;
import com.quintype.camelotplayer.fragments.FragmentCallbacks;
import com.quintype.camelotplayer.models.Track;
import com.quintype.camelotplayer.viewholders.TrackHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshaykoul on 06/04/17.
 */

public class TrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Track> tracks = new ArrayList<>();
    FragmentCallbacks fragmentCallbacks;

    public TrackAdapter(FragmentCallbacks fragmentCallbacks) {
        this.fragmentCallbacks = fragmentCallbacks;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_watch_item,
                parent, false);
        return TrackHolder.create(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof TrackHolder) {
            ((TrackHolder) holder).bind(tracks.get(position));
            final String clientId = holder.itemView.getContext().getString(R.string
                    .soundcloud_api_key);
            ((TrackHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fragmentCallbacks.propagateEvent(new Pair<String, Object>(Constants
                            .EVENT_TRACK_CLICK, new NowPlaying(getAudioFromTracks
                            (tracks, clientId), position)));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void addTracks(List<Track> newTracks) {
        int start = tracks.size();
        tracks.addAll(newTracks);
        int end = tracks.size();
        notifyItemRangeInserted(start, end);
    }

    public void clearAll() {
        tracks.clear();
        notifyDataSetChanged();
    }

    private ArrayList<Audio> getAudioFromTracks(List<Track> trackList, String
            clientId) {
        ArrayList<Audio> adioList = new ArrayList<>();
        for (Track track : trackList) {
            Audio audio = new Audio(track.getId(), track.getDuration(), track
                    .getDescription(),
                    track.getTitle(), track.getGenre(), track.getUser().getUsername(),
                    getHirezArtwork(track.getArtworkUrl()), track.getStreamUrl() + "?client_id="
                    + clientId);
            adioList.add(audio);
        }
        return adioList;
    }

    public static String getHirezArtwork(String artworkUrl) {
        if (artworkUrl != null) {
            return artworkUrl.replace("large.jpg", "t500x500.jpg");
        } else {
            return artworkUrl;
        }
    }
}

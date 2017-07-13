package com.quintype.musicplayer;

import android.content.Context;

import com.quintype.musicplayer.R;
import com.quintype.musicplayer.models.Track;
import com.quintype.player.models.Audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshaykoul on 11/07/17.
 */

public class Utilities {


    public static ArrayList<Audio> getAudioFromTracks(List<Track> trackList, String
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

    public static final String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }
}

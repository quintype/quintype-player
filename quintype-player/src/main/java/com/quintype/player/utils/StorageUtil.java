package com.quintype.player.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quintype.player.models.Audio;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by akshaykoul on 04/07/17.
 */

public class StorageUtil {

    private final String STORAGE = " com.quintype.musicstreaming.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    //private HashMap<Long, Integer> downloadManagerDownloadHistory;

    public final String DOWNLOADED_PODCAST_ID_LIST = "downloadedPodcastIDList";
    public final String DOWNLOAD_MANAGER_HISTORY_LIST = "downloadManagerHistory";
    public final String AUDIO_ARRAY_LIST = "audioArrayList";
    public final String AUDIO_INDEX = "audioIndex";

    public StorageUtil(Context context) {
        this.context = context;
    }

    public void storeAudio(ArrayList<Audio> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString(AUDIO_ARRAY_LIST, json);
        editor.apply();
    }

    public ArrayList<Audio> loadAudio() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(AUDIO_ARRAY_LIST, null);
        Type type = new TypeToken<ArrayList<Audio>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(AUDIO_INDEX, index);
        editor.apply();
    }

    public int loadAudioIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt(AUDIO_INDEX, -1);//return -1 if no data found
    }

    public void clearCachedAudioPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    public Boolean deleteFile(String downloadLocation, int fileName) {
        File extStore = Environment.getExternalStorageDirectory();
        File myFile = new File(extStore.getAbsolutePath() + "/" + downloadLocation + "/" + fileName + MediaConstants.MP3_EXTENSION);
        return myFile.delete();
    }

    public Boolean checkIfFileExists(String downloadLocation, int fileName) {
        Boolean isFileAvailable = false;
        /*Check the array list first, */
        ArrayList<Integer> downloadedPodcast = getDownloadedPodcast();
        if (downloadedPodcast.size() != 0) {
            isFileAvailable = checkDownloadedPodcastList(fileName);

        } else {
            /* If the list is empty then there is a possibility for two scenarios,
                    1. The user is downloading for the first time,
                    2. There is also a possibility that user might cleared the Apps Data\Cache.
              For safer side we need to check the download destination directory for any downloaded tacks and upload the downloadedPodcast list.*/

            File sdCardRoot = Environment.getExternalStorageDirectory();
            File directory = new File(sdCardRoot, downloadLocation);

            if (directory.listFiles() != null) {
                for (File audioFile : directory.listFiles()) {
                    if (audioFile.isFile()) {
                        addToDownloadedPodcast(Integer.parseInt(audioFile.getName().substring(0, audioFile.getName().length() - 4)));
                    }
                }
                   /*Once the downloadedPodcast list is updated by checking the storage refer the updated list again  */
                if (downloadedPodcast.size() != 0) {
                    isFileAvailable = checkDownloadedPodcastList(fileName);
                } else /*No any downloaded podcast in storage*/
                    isFileAvailable = false;

            } else {
                /*No any downloaded podcast in storage*/
                isFileAvailable = false;
            }


        }
        return isFileAvailable;
    }

    private Boolean checkDownloadedPodcastList(int fileName) {
        Boolean isaudioIDAvailable = false;
        ArrayList<Integer> downloadedPodcast = getDownloadedPodcast();
    /*The download list is not empty, i.e the user have a download history. Its enough to check the array list alone.*/
        for (int i = 0; i < downloadedPodcast.size(); i++) {
            if (downloadedPodcast.get(i).equals(fileName)) {
                isaudioIDAvailable = true;
                break;
            } else {
                isaudioIDAvailable = false;
            }
        }
        return isaudioIDAvailable;
    }

    public ArrayList<Integer> getDownloadedPodcast() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(DOWNLOADED_PODCAST_ID_LIST, null);
        Type type = new TypeToken<ArrayList<Integer>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void addToDownloadedPodcast(Integer audioID) {
        ArrayList<Integer> downloadedPodcast = getDownloadedPodcast();
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (downloadedPodcast.contains(audioID)) {
            downloadedPodcast.add(audioID);
            Gson gson = new Gson();
            String json = gson.toJson(downloadedPodcast);
            editor.putString(DOWNLOADED_PODCAST_ID_LIST, json);
            editor.apply();
        }
    }

    public void removeFromDownloadedPodcastList(Integer audioID) {
        ArrayList<Integer> downloadedPodcast = getDownloadedPodcast();
        SharedPreferences.Editor editor = preferences.edit();
        if (downloadedPodcast.contains(audioID)) {
            downloadedPodcast.remove(audioID);
            Gson gson = new Gson();
            String json = gson.toJson(downloadedPodcast);
            editor.putString(DOWNLOADED_PODCAST_ID_LIST, json);
            editor.apply();
        }
    }

    public HashMap<Long, Integer> getDownloadManagerHistory() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(DOWNLOAD_MANAGER_HISTORY_LIST, null);
        Type type = new TypeToken<HashMap<Long, Integer>>() {
        }.getType();
        return gson.fromJson(json, type);

    }

    public void addToDownloadManagerDownloadHistory(Long downloadID, Integer audioID) {
        HashMap<Long, Integer> downloadManagerHistory = getDownloadManagerHistory();
        downloadManagerHistory.put(downloadID, audioID);

        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(downloadManagerHistory);
        editor.putString(DOWNLOAD_MANAGER_HISTORY_LIST, json);
        editor.apply();
    }

    public Integer getAudioID(Long downloadedId) {
        Integer audioID = null;
        Iterator it = getDownloadManagerHistory().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getKey().equals(downloadedId)) {
                audioID = (Integer) pair.getValue();
            }
        }
        return audioID;
    }

}
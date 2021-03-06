package com.quintype.player.models;

import java.io.Serializable;

/**
 * Created by akshaykoul on 04/07/17.
 */

public class Audio implements Serializable {


    private int id;
    private int duration;
    private String data;
    private String title;
    private String genre;
    private String artist;
    private String artwork;
    private String streamUrl;
    private boolean isDownloaded;

    public Audio(int id, int duration, String data, String title, String genre, String artist,
                 String artwork, String streamUrl, boolean isDownloaded) {
        this.id = id;
        this.data = data;
        this.genre = genre;
        this.title = title;
        this.artist = artist;
        this.artwork = artwork;
        this.duration = duration;
        this.streamUrl = streamUrl;
        this.isDownloaded = isDownloaded;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getArtwork() {
        return artwork;
    }

    public void setArtwork(String artwork) {
        this.artwork = artwork;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }
}
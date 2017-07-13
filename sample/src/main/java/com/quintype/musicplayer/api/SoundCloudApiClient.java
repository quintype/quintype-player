package com.quintype.musicplayer.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SoundCloudApiClient {

    private static final String TAG = SoundCloudApiClient.class.getName();
    private static com.quintype.musicplayer.api.SoundcloudApiService soundcloudApiService;

    public static com.quintype.musicplayer.api.SoundcloudApiService getApiService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

//        TimberLoggingInterceptor timberLoggingInterceptor =  new TimberLoggingInterceptor();

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.soundcloud.com")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        soundcloudApiService = retrofit.create(com.quintype.musicplayer.api.SoundcloudApiService.class);
        return soundcloudApiService;
    }
}

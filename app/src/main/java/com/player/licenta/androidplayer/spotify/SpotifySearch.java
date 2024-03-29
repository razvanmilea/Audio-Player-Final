package com.player.licenta.androidplayer.spotify;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.player.licenta.androidplayer.model.Song;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by razvan on 8/16/18.
 */

public class SpotifySearch extends AsyncTask<String, Void, Void> {

    private final static String TAG = "SpotifySearch";
    private String mTrackId;
    private String mSongTitle;
    private String mSongArtist;
    private Context mContext;
    private String mToken;
    private Song mSong;

    public static String checkChosenFeature;
    public static int counterValidatedTrackId = 0;


    public SpotifySearch(Context context, Song song, String token) {
        mContext = context;
        mSong = song;
        mToken = token;
    }

    public String getTrackInfo(String authToken, String songTitle, String songArtist, String chosenFeature) {
        try {

            mSongTitle = songTitle;
            if (mSongTitle.contains("(") && mSongTitle.contains(")")) {
                mSongTitle = mSongTitle.substring(0, mSongTitle.indexOf("("));
            }
            mSongArtist = songArtist;

            URI uri = getSearchURI(songTitle, songArtist);

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet();
            httpGet.setURI(uri);
            httpGet.setHeader(SpotifyConstants.AUTHORIZATION, authToken);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            InputStream is = httpResponse.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(" ");
            }
            String response = sb.toString();
            boolean isSongFound = validateTrack(response);
            if (isSongFound) {
                //SpotifyAnalysis spotifyAnalysis = new SpotifyAnalysis(mContext, mSong, mToken);
                //spotifyAnalysis.execute(mTrackId, authToken, songTitle, songArtist, chosenFeature);
                Log.d(TAG, "Song found" + mTrackId);
                return mTrackId;
            } else {
                Log.d(TAG, "Song not found");
            }
            Log.d(TAG, "Response from Spotify" + response);


        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public URI getSearchURI(String songTitle, String songArtist) throws URISyntaxException {
        //TODO
        String songTitleConverted = songTitle.replace(" ", SpotifyConstants.SPACE);
        String songArtistConverted = songArtist.replace(" ", SpotifyConstants.SPACE);

        String concatURL = SpotifyConstants.SEARCH_URL + SpotifyConstants.QUERY +
                songTitleConverted + SpotifyConstants.SPACE + songArtistConverted + SpotifyConstants.AND +
                SpotifyConstants.TYPE + SpotifyConstants.TRACK;
        Log.d(TAG, concatURL);
        return new URI(concatURL);

    }

    @Override
    protected Void doInBackground(String... songData) {
        String authToken = songData[0];
        String songTitle = songData[1];
        String songArtist = songData[2];
        String chosenFeature = songData[3];
        getTrackInfo(authToken, songTitle, songArtist, chosenFeature);
        return null;
    }

    public boolean validateTrack(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray items = jsonObject.getJSONObject(SpotifyConstants.TRACKS).
                    getJSONArray(SpotifyConstants.ITEMS);
            for (int i = 0; i < items.length(); i++) {
                String songName = items.getJSONObject(i).getString(SpotifyConstants.NAME);
                if (songName.toLowerCase().contains(mSongTitle.toLowerCase())) {
                    JSONArray artistsArray = items.getJSONObject(i)
                            .getJSONArray(SpotifyConstants.ARTISTS);
                    for (int j = 0; j < artistsArray.length(); j++) {
                        String artistName = artistsArray.getJSONObject(j).getString(SpotifyConstants.NAME);
                        if (artistName.toLowerCase().contains(mSongArtist.toLowerCase())) {
                            mTrackId = items.getJSONObject(i).getString(SpotifyConstants.ID);
                            Log.d(TAG, "Track id found: " + mTrackId);
                            return true;
                        }
                    }
                }
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return false;
    }
}

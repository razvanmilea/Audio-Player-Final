package com.player.licenta.androidplayer.spotify;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.player.licenta.androidplayer.R;
import com.player.licenta.androidplayer.activities.MainActivity;
import com.player.licenta.androidplayer.model.Song;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.player.licenta.androidplayer.spotify.SpotifySearch.checkChosenFeature;

/**
 * Created by razvan on 8/27/18.
 */

public class SpotifyAuthentication extends AsyncTask<ArrayList<Song>, String, Void> {

    private String mToken;
    private ArrayList<Song> songList;
    private Context mContext;
    private String mChosenFeature;
    private MainActivity mainActivity;


    public static HashMap<String, Song> songFeatureCache = new HashMap<>();

    private final static String TAG = "SpotifyAuthentication";

    public SpotifyAuthentication(Context context, String chosenFeature, MainActivity activity){
        mContext = context;
        this.mChosenFeature = chosenFeature;
        this.mainActivity = activity;
    }

    @Override
    protected Void doInBackground(ArrayList<Song>... receivedSongList) {
        songList = receivedSongList[0];
        mToken = getAuthenticationToken();

        return null;
    }

    private String getAuthenticationToken() {

        try {
            URI uri = new URI(SpotifyConstants.AUTH_TOKEN_URL);
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost();
            httpPost.setURI(uri);
            httpPost.setHeader(SpotifyConstants.AUTHORIZATION, SpotifyConstants.AUTH_HEADER);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(SpotifyConstants.GRANT_TYPE,
                    SpotifyConstants.CLIENT_CREDENTIALS));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse httpResponse = httpClient.execute(httpPost);

            InputStream is = httpResponse.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(" ");
            }

            String response = sb.toString();
            if (response.contains("access_token")) {
                JSONObject jsonObject = new JSONObject(response);
                mToken = SpotifyConstants.BEARER +
                        jsonObject.getString("access_token");
                getTrackData(mToken);
            }
            Log.d(TAG, "Response from Spotify" + mToken);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mToken;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mainActivity.hideProgressDialog();
    }

    private void getTrackData(String mToken) {
        checkChosenFeature = mChosenFeature;
        for (Song song : songList) {
            String songTitle = song.getSongTitle();
            String songArtist = song.getSongArtist();
            SpotifySearch spotifySearch = new SpotifySearch(mContext, song, mToken);
            String trackId = spotifySearch.getTrackInfo(mToken, songTitle, songArtist, mChosenFeature);
            if(trackId != null){
                SpotifyAnalysis spotifyAnalysis = new SpotifyAnalysis(mContext, song, mToken, trackId);
                String audioFeatures = spotifyAnalysis.getAudioFeatures();
                if (audioFeatures != null){
                    try {
                        JSONObject jsonObject = new JSONObject(audioFeatures);
                        double featureValue = jsonObject.getDouble(mChosenFeature);
                        if(featureValue > 0.6){
                            songFeatureCache.put(mChosenFeature, song);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


    }



}

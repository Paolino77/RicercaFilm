package com.daginge.tmdbsearch;

import com.daginge.tmdbsearch.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends Activity implements AppConstants {

    public static final String EXTRA_MESSAGE = "com.daginge.tmdbsearch.MESSAGE";
    public static final String EXTRA_QUERY = "com.daginge.tmdbsearch.QUERY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new TMDBConfManager().execute();
        } else {
            TextView textView = new TextView(this);
            textView.setText("@string/no_connection");
            setContentView(textView);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    
    /**
     * Fires an intent to the {@link TMDBSearchResultActivity} with the query.
     * {@link TMDBSearchResultActivity} does all the downloading and rendering.
     * @param view
     */
    public void queryTMDB(View view) {
        Intent intent = new Intent(this, TMDBSearchResultActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String query = editText.getText().toString();
        intent.putExtra(EXTRA_QUERY, query);
        startActivity(intent);
    }


    private class TMDBConfManager extends AsyncTask implements AppConstants {
        @Override
        protected String doInBackground(Object... params) {
            try {
                return loadConfiguration();
            } catch (IOException e) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            Toast.makeText(MainActivity.this,"@string/conf_updated",Toast.LENGTH_LONG);
        };


        public String loadConfiguration() throws IOException
        {
            /* Carica i parametri di Configurazione di themoviedb */
            // Build URL
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(APIURL +"/configuration");
            stringBuilder.append("?api_key=" + TMDB_API_KEY);
            URL url = new URL(stringBuilder.toString());
            //http://api.themoviedb.org/3/configuration?api_key=ef68bfed72780ce7ae801b9daba23069

            InputStream stream = null;
            try {
                // Establish a connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.addRequestProperty("Accept", "application/json"); // Required to get TMDB to play nicely.
                conn.setDoInput(true);
                conn.connect();

                int responseCode = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response code is: " + responseCode + " " + conn.getResponseMessage());

                stream = conn.getInputStream();
                return salvaConfigurazione(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        private String salvaConfigurazione(InputStream stream) throws IOException, UnsupportedEncodingException {
            String streamAsString = "";
            try {
                //SharedPreferences sharedPref = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
                //SharedPreferences sharedPref  = getPreferences(MODE_PRIVATE);
                //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                //SharedPreferences.Editor editor = sharedPref.edit();


                Reader reader = null;
                reader = new InputStreamReader(stream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(reader);
                streamAsString = bufferedReader.readLine();

                JSONObject jsonConfObject = new JSONObject(streamAsString);
                JSONObject images = jsonConfObject.getJSONObject("images");
                String baseUrl = images.getString("base_url");
                String secureBaseUrl= images.getString("secure_base_url");
                JSONArray backdropSizes = (JSONArray) images.get("backdrop_sizes");
                JSONArray logoSizes = (JSONArray) images.get("logo_sizes");
                JSONArray posterSizes = (JSONArray) images.get("poster_sizes");
                JSONArray profileSizes = (JSONArray) images.get("profile_sizes");
                JSONArray stillSizes = (JSONArray) images.get("still_sizes");
                JSONArray changeKeys = jsonConfObject.getJSONArray("change_keys");

                /*editor.putString("baseUrl",baseUrl);
                editor.putString("secureBaseUrl",secureBaseUrl);
                editor.putString("backdropSizes",backdropSizes.toString());
                editor.putString("logoSizes",logoSizes.toString());
                editor.putString("posterSizes",posterSizes.toString());
                editor.putString("profileSizes",profileSizes.toString());
                editor.putString("stillSizes",stillSizes.toString());
                editor.putString("changeKeys",changeKeys.toString());
                editor.commit();*/

                Log.i(DEBUG_TAG,"Configurazione movie baseUrl("+baseUrl+") secureBaseUrl("+secureBaseUrl+")" +
                        "backdropSizes("+backdropSizes+") logoSizes("+logoSizes+") posterSizes("+posterSizes+") " +
                        "profileSizes("+profileSizes+") stillSizes("+stillSizes+") change_keys("+changeKeys+")");

            } catch (JSONException e) {
                System.err.println(e);
                Log.d(DEBUG_TAG, "Error parsing JSON. String was: " + streamAsString+ "Exc="+e.getMessage());
            }
            return "";
        }
    }

}
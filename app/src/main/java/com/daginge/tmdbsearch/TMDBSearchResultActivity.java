package com.daginge.tmdbsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bumptech.glide.Glide;
import com.daginge.tmdbsearch.MovieResult.Builder;
import com.daginge.tmdbsearch.AppConstants;

public class TMDBSearchResultActivity  extends Activity implements AppConstants {

    //SharedPreferences sharedPref = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imdbsearch_result);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get the intent to get the query.
        Intent intent = getIntent();
        String query = intent.getStringExtra(MainActivity.EXTRA_QUERY);
        
        // Check if the NetworkConnection is active and connected.
        ConnectivityManager connMgr = (ConnectivityManager) 
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new TMDBQueryManager().execute(query);
        } else {
            TextView textView = new TextView(this);
            textView.setText("@string/no_connection");
            setContentView(textView);
        }
        
    }


    public class myMovieAdapter extends BaseAdapter {
        String title;
        String poster_path;
        boolean adult;
        ArrayList<MovieResult> result;
        Context ctx;

        /*myMovieAdapter(){
            title = "";
            poster_path = "";
            adult = false;
        }*/

        public myMovieAdapter(ArrayList<MovieResult> result, Context ctx)
        {
            this.result = result;
            this.ctx = ctx;
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return result.size();
        }

        public MovieResult getItem(int position) {
            // TODO Auto-generated method stub
            return result.get(position);
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public String getTitle(int position){
            MovieResult mymovie = result.get(position);
            String title = mymovie.getTitle();
            return title;
        }

        public String getPoster_path(int position){
            MovieResult mymovie = result.get(position);
            String path = mymovie.getPosterPath();
            return path;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            //SharedPreferences sharedPref = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
            LayoutInflater inflater = getLayoutInflater();
            View row;
            row = inflater.inflate(R.layout.movie_result_list_item, parent, false);
            TextView title, poster_path;
            ImageView imageView = (ImageView) findViewById(R.id.poster);
            title = (TextView) row.findViewById(R.id.movie_result_list_item);
            poster_path = (TextView) row.findViewById(R.id.movie_result_poster_path);
            //i1=(ImageView)row.findViewById(R.id.poster);

            String imgpath = "";
                    //String imgpath = sharedPref.getString("baseUrl","http://image.tmdb.org/t/p/");//Secondo parametro valore default
            //scelgo la dimensione del poster.

            //java.lang.reflect.Array posterSizes = (Array) sharedPref.getString("posterSizes","");

            //String posterSizes =  sharedPref.getString("posterSizes","");
            String posterSizes = "";
            ArrayList<String> list = new ArrayList<String>();
            try
            {
                JSONArray jsonArray = new JSONArray(posterSizes);
                if (jsonArray != null) {
                    int len = jsonArray.length();
                    for (int i=0;i<len;i++){
                        list.add(jsonArray.get(i).toString());
                    }
                }
            } catch (JSONException e) {
                System.err.println(e);
                Log.d(DEBUG_TAG, "Error parsing posterSizes. posterSizes was: " + posterSizes);
            }

            imgpath += list.get(0).toString(); //dimensione posterSize
            imgpath += poster_path;

            Log.i(DEBUG_TAG,"prova caricamento poster "+imgpath);

            Glide.with(ctx).load(imgpath).into(imageView);
            title.setText(getTitle(position));
            poster_path.setText(getPoster_path(position));
            //i1.setImageResource(imge[position]);

            return (row);
        }
    }
    /**
     * Updates the View with the results. This is called asynchronously
     * when the results are ready.
     * @param result The results to be presented to the user.
     */
    public void updateViewWithResults(ArrayList<MovieResult> result) {
        ListView listView = new ListView(this);
        //Log.d("updateViewWithResults", result.toString());
        // Add results to listView.
        //visualizzare il numero di risultati


        //ArrayAdapter<MovieResult> adapter = new ArrayAdapter<MovieResult>(this, R.layout.movie_result_list_item, result);
        myMovieAdapter myadapter = new myMovieAdapter(result,this);
        listView.setAdapter(myadapter);
        
        // Update Activity to show listView
        setContentView(listView);
    }
    
    private class TMDBQueryManager extends AsyncTask {
        

        @Override
        protected ArrayList<MovieResult> doInBackground(Object... params) {
            try {
                return searchIMDB((String) params[0]);
            } catch (IOException e) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Object result) {
            updateViewWithResults((ArrayList<MovieResult>) result);
        };

        /**
         * Searches IMDBs API for the given query
         * @param query The query to search.
         * @return A list of all hits.
         */
        public ArrayList<MovieResult> searchIMDB(String query) throws IOException {
            // Build URL
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(APIURL+"/search/movie");
            stringBuilder.append("?api_key=" + TMDB_API_KEY);
            stringBuilder.append("&query=" + query);
            URL url = new URL(stringBuilder.toString());


            
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
                return parseResult(stringify(stream));
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        private ArrayList<MovieResult> parseResult(String result) {
            String streamAsString = result;
            ArrayList<MovieResult> results = new ArrayList<MovieResult>();
            try {
                JSONObject jsonObject = new JSONObject(streamAsString);
                JSONArray array = (JSONArray) jsonObject.get("results");
                String totalResult = jsonObject.getString("total_results");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonMovieObject = array.getJSONObject(i);
                    Log.d(DEBUG_TAG,"oggetto JSON "+jsonMovieObject.toString());
                    Builder movieBuilder = MovieResult.newBuilder(
                            Integer.parseInt(jsonMovieObject.getString("id")),
                            jsonMovieObject.getString("title"))
                            .setBackdropPath(jsonMovieObject.getString("backdrop_path"))
                            .setOriginalTitle(jsonMovieObject.getString("original_title"))
                            .setPopularity(jsonMovieObject.getString("popularity"))
                            .setPosterPath(jsonMovieObject.getString("poster_path"))
                            .setReleaseDate(jsonMovieObject.getString("release_date"));
                    results.add(movieBuilder.build());
                }
            } catch (JSONException e) {
                System.err.println(e);
                Log.d(DEBUG_TAG, "Error parsing JSON. String was: " + streamAsString);
            }
            return results;
        }
        
        public String stringify(InputStream stream) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            return bufferedReader.readLine();
        }
    }

}

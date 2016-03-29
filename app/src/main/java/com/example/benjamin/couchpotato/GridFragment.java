package com.example.benjamin.couchpotato;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GridFragment extends Fragment {

    private ImageAdapter mPosterAdapter;

    public GridFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.grid_fragment, container, false);

        mPosterAdapter = new ImageAdapter(getActivity(), new ArrayList<String>());

        GridView gridView = (GridView) rootView.findViewById(R.id.gridview_posters);
        gridView.setAdapter(mPosterAdapter);
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                Toast.makeText(getActivity(), "TEST", Toast.LENGTH_SHORT).show();
//                Toast.makeText(getActivity(), "Updating content...", Toast.LENGTH_SHORT).show();
//                updateContent();
//            }
//        });

        return rootView;
    }

    private void updateContent() {
        FetchContentTask contentTask = new FetchContentTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()); //TODO: Add SharedPreferences
        String category = prefs.getString("category", "popular");
        contentTask.execute(category);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateContent();
    }

    public class FetchContentTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchContentTask.class.getSimpleName();

        private String[] getPosterUrlsFromJson(String contentJsonStr) throws JSONException {

            Log.d(LOG_TAG, contentJsonStr);

            int numItems = 20;
            String[] posterUrls = new String[numItems];

            final String POSTER_BASE_URL = "http://image.tmdb.org/t/p/";
            final String POSTER_SIZE = "w185";

            final String TMDB_RESULTS = "results";
            final String TMDB_POSTER_PATH = "poster_path";

            JSONObject contentJson = new JSONObject(contentJsonStr);
            JSONArray itemArray = contentJson.getJSONArray(TMDB_RESULTS);

            for (int i = 0; i < itemArray.length(); i++) {
                JSONObject item = itemArray.getJSONObject(i);
                String posterUrl = POSTER_BASE_URL + POSTER_SIZE + item.getString(TMDB_POSTER_PATH);
                posterUrls[i] = posterUrl;
            }

            return posterUrls;
        }

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0)
                return null;
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String contentJsonStr = null;

            String contentType = "movie";

            try {
                final String CONTENT_BASE_URL = "http://api.themoviedb.org/3/";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(CONTENT_BASE_URL).buildUpon()
                        .appendPath(contentType)
                        .appendPath(params[0])
                        .appendQueryParameter(API_KEY_PARAM, BuildConfig.THE_MOVIE_DB_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null)
                    return null;
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null)
                    buffer.append(line + "\n");

                if (buffer.length() == 0)
                    return null;

                contentJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getPosterUrlsFromJson(contentJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mPosterAdapter.clear();
                for (String item : result)
                    mPosterAdapter.add(item);
            }
        }
    }
}

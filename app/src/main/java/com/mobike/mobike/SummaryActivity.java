package com.mobike.mobike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * This activity displays the route that has just been recorded and gives
 * the user the choice to save or delete it.
 */

public class SummaryActivity extends ActionBarActivity {

    private static final int SHARE_REQUEST = 1;
    private static final String TAG = "SummaryActivity";
    private static final String UploadURL = "http://mobike.ddns.net/SRV/routes/create";
    private static final String DEFAULT_ACCOUNT_NAME = "no account";
    public static final String ROUTE_ID = "com.mobike.mobike.ROUTE_ID";
    private  EditText routeNameText, routeDescriptionText;
    private TextView length, duration;
    private long durationInSeconds;
    private String routeName, routeDescription, email, routeID;
    private Context context = this;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Polyline route; // the recorded route
    private List<LatLng> points; // the points of the route

    /**
     * This method is called when the activity is created; it checks if the map is set up
     * and then adds all the recorded location to the route, for it to be displayed on the map.
     * @param savedInstanceState the saved data of the activity instance
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        setUpMapIfNeeded();
        route = mMap.addPolyline(new PolylineOptions().width(6).color(Color.BLUE));
        GPSDatabase db = new GPSDatabase(this);
        db.open();
        points = db.getAllLocations();
        db.close();
        route.setPoints(points);
        routeNameText = (EditText) findViewById(R.id.route_name_text);
        routeDescriptionText = (EditText) findViewById(R.id.route_description_text);

        //set length and duration text views
        GPSDatabase db2 = new GPSDatabase(this);
        length = (TextView) findViewById(R.id.length_text_view);
        length.setText("Length: " + String.format("%.02f", db2.getTotalLength()/1000) + " km");
        duration = (TextView) findViewById(R.id.duration_text_view);
        durationInSeconds = db2.getTotalDuration();
        duration.setText("Duration: " + String.valueOf(durationInSeconds/3600) + " h " + String.valueOf(durationInSeconds/60) + " m " + String.valueOf(durationInSeconds%60) + " s");
        Log.v(TAG, "length: " + db2.getTotalLength() + " -- duration: " + db2.getTotalDuration());
        db2.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summary_map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This method gets all the recorded location from the database,
     * initializes the route and centers the map at the middle of the route.
     *
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        GPSDatabase db = new GPSDatabase(this);
        db.open();
        // Taking all the points of the route
        points = db.getAllLocations();
        if (points.size() > 0) {
            //saving the first and the last ones
            LatLng start = points.get(0);
            LatLng end = points.get(points.size() - 1);

            // Adding the start and end markers
            mMap.addCircle(new CircleOptions().center(start).fillColor(Color.GREEN).
                    strokeColor(Color.BLACK).radius(10));
            mMap.addCircle(new CircleOptions().center(end).fillColor(Color.RED).
                    strokeColor(Color.BLACK).radius(10));
            // Zooming on the route
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(points.get(points.size() / 2),
                    MapsActivity.CAMERA_ZOOM_VALUE - 5);
            mMap.animateCamera(update);
        }

        db.close();
    }

    /**
     * This method is called when the user choose to delete the recorded route.
     * @param view the view
     */
    public void deleteRoute(View view) {
        // go to the mapsActivity and delete the route on the map (points = newArrayList<LatLng>;
                                                                // route,setPoints(points);
        finish();
    }

    public void saveRoute(View view) {
        // Parte l'upload del percorso
        if (routeNameText.getText().toString().length() > 0) {
            routeName = routeNameText.getText().toString();
            routeDescription = routeDescriptionText.getText().toString();
            SharedPreferences sharedPref = getSharedPreferences(LoginActivity.ACCOUNT_NAME, Context.MODE_PRIVATE);
            email = sharedPref.getString(LoginActivity.ACCOUNT_NAME, DEFAULT_ACCOUNT_NAME);
            Log.v(TAG, "email = " + email);
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new UploadRouteTask().execute(this);
            } else {
                Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
            }

            //TODO: passare alla ShareActivity l'id del percorso sul server per comporre l'url (restituito come risposta alla POST)
            // Avvia l'activity per la condivisione del tracciato sui social networks
        } else {
            Toast.makeText(this, "Insert a route name", Toast.LENGTH_SHORT).show();
        }
    }

    // Method called when ShareActivity finishes, returns to MapsActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult()");
        finish();
    }


    // AsyncTask that performs the upload of the route
    private class UploadRouteTask extends AsyncTask<Context, Void, String> {

        @Override
        protected String doInBackground(Context... context) {
            try {
                return uploadRoute(context[0]);
            } catch (IOException e) {
                return "Unable to upload the route. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }

        private String uploadRoute(Context context) throws IOException {
            HttpURLConnection urlConnection = null;
            try {
                URL u = new URL(UploadURL);
                urlConnection = (HttpURLConnection) u.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "text/plain");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.connect();
                GPSDatabase db = new GPSDatabase(context);
                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                out.write(db.exportRouteInJson(email, routeName, routeDescription).toString());
                out.close();
                db.close();
                int httpResult = urlConnection.getResponseCode();
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    routeID = br.readLine();
                    Log.v(TAG, routeID);
                    br.close();
                    Intent intent = new Intent(context, ShareActivity.class);
                    intent.putExtra(ROUTE_ID, routeID);
                    startActivityForResult(intent, SHARE_REQUEST);
                    return "Upload completed!";
                }
                else {
                    // scrive un messaggio di errore con codice httpResult
                    Log.v(TAG, " httpResult = " + httpResult);
                    return "Error code: " + httpResult;
                }
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
        }
    }
}

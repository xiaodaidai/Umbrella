package com.thinkful.umbrella;

import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        WebServiceTask webserviceTask = new WebServiceTask();
        webserviceTask.execute(mCurrentLocation);
        Log.i("Umbrella", "start location updates");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        WebServiceTask webserviceTask = new WebServiceTask();
        webserviceTask.execute(location);
    }

    private class WebServiceTask extends AsyncTask<Location, String, String> {
        @Override
        protected String doInBackground(Location... params) {
            // return "Hello " + params[0];
            String useUmbrellaStr = "Don't know, sorry about that.";
            HttpURLConnection urlConnection = null;
            Location location = params[0];
            if (location != null) {
                try {
                    // URL url = new URL("http://api.openweathermap.org/data/2" +
                    //        ".5/forecast/daily?q=London&mode=json&units=metric&cnt=1");
                    URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?lat=" + location.getLatitude()
                            + "&lon=" + location.getLongitude() + "&mode=json&units=metric&cnt=1");
                    Log.i("Umbrella", "URL: " +  url);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    useUmbrellaStr = useUmbrella(urlConnection.getInputStream());
                } catch (IOException e) {
                    Log.e("MainActivity", "Error ", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
            return useUmbrellaStr;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TextView textview = (TextView) findViewById(R.id.hello);
            textview.setText("Should I take an umbrella today? "+s);
        }
    }

    private String useUmbrella(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            //JSON needs to be parsed here
            JSONObject forecastJson = new JSONObject(stringBuilder.toString());
            JSONArray weatherArray = forecastJson.getJSONArray("list");
            JSONObject todayForecast = weatherArray.getJSONObject(0);
            if (todayForecast.has("rain") || todayForecast.has("snow")) {
                return("Yes");
            } else {
                return("No");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        return "Don't know, sorry about that.";
    }
    @Override
    protected void onPause() {
        super.onPause();
        //stop location updates
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void startLocationUpdates() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

}

package xyz.eleventhour.averageweather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.eleventhour.averageweather.WeatherLocation.*;

import java.util.Calendar;

/**
 * Created by Kailas on 5/14/2016.
 *
 * Singleton class that manages all network and data requests
 */
public final class NetworkManager implements Response.Listener<JSONObject>, Response.ErrorListener{

    private static NetworkManager singleton = null;

    public GoogleApiClient apiClient;
    //calling activity must extend DataDisplayActivity
    public DataDisplayActivity activity;

    //URLs
    private static final String dataURL = "http://[domain.com]/api/weather/main.php?",backgroundURL = "http://[domain.com]/api/weather/backgroundURL.php?";

    //Tag used on Volley requests
    private static final String DATA_REQUEST_TAG = "TAGGLE",URL_REQUEST_TAG="URLED";

    //Code used to request permission for location
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 4;

    public JSONObject weatherData;
    public WeatherLocation lastLocationRequested;
    //Volley queue and ImageLoader
    private RequestQueue queue;
    private ImageLoader imageLoader;

    //Volley retry value
    private final int TIMEOUT_DURATION = 50000,NUM_RETRIES = 3, BACKOFF_MULTIPLIER = 2;

    public enum CODE {PERMISSION_DENIED,SUCCESS,NULL_CLIENT,NULL_LOCATION,NETWORK_ERROR,NULL_RESPONSE}

    //get singleton
    public static NetworkManager getManager(DataHandlerActivity activity, GoogleApiClient client){
        if(singleton == null){
            singleton = new NetworkManager(activity);
        }

        singleton.apiClient = client;
        singleton.activity = activity;

        return singleton;
    }

    //Get singleton from non DataHandlerActivity
    public static NetworkManager getManager(DataDisplayActivity activity){
        if(singleton == null){
            singleton = new NetworkManager(activity);
        }

        singleton.activity = activity;

        return singleton;
    }

    //private constructor
    private NetworkManager(DataDisplayActivity activity) {
        queue = Volley.newRequestQueue(activity.getApplicationContext());

        //Setup image loader w/cache
        imageLoader = new ImageLoader(queue, new ImageLoader.ImageCache() {

            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            private final LruCache<String, Bitmap>
                    cache = new LruCache<String, Bitmap>(cacheSize);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

    }

    public ImageLoader getImageLoader(){
        return imageLoader;
    }

    //gets weather data at current location
    private void getDataAtCurrentLocation(boolean details){
        CODE locationCode = getLocationData();

        if(locationCode != CODE.SUCCESS){
            activity.handleError(locationCode);
        } else if(lastLocationRequested.location == null){
            activity.handleError(CODE.NULL_LOCATION);
        } else {
            checkCachedData(details, lastLocationRequested);
        }
    }

    //gets weather data at specified location
    public void getData(boolean details, WeatherLocation location){
        if(location != null) {
            lastLocationRequested = location;

            if (location.locationText.equals(StorageManager.MY_LOCATION_NAME)) {
                getDataAtCurrentLocation(details);
            } else if(location.location == null) {
                activity.handleError(CODE.NULL_LOCATION);
            } else{
                checkCachedData(details, location);
            }
        } else {
            activity.handleError(CODE.NULL_LOCATION);
        }
    }

    //initiate volley request for weather data
    private CODE getWeatherData(boolean details, WeatherLocation location){
        if(location.location == null){activity.handleError(CODE.NULL_LOCATION);return CODE.NULL_LOCATION;}
        String fullURL = dataURL + "lat=" + String.valueOf(location.location.lat) + "&lon=" + String.valueOf(location.location.lon) + "&details=" + details;
        JsonObjectRequest weatherDataRequest = new JsonObjectRequest(Request.Method.GET,fullURL,null,this,this);
        //Requests tagged for easy cancellation
        weatherDataRequest.setTag(DATA_REQUEST_TAG);
        weatherDataRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_DURATION,NUM_RETRIES, BACKOFF_MULTIPLIER));
        queue.add(weatherDataRequest);
        return CODE.SUCCESS;
    }

    public void getBackgroundURL(String lat,String lon,Response.Listener<String> responseListener,Response.ErrorListener errorListener){
        String url = backgroundURL + "lat=" + lat + "&lon=" + lon;
        StringRequest urlRequest = new StringRequest(Request.Method.GET,url,responseListener,errorListener);
        //Requests tagged for easy cancellation
        urlRequest.setTag(URL_REQUEST_TAG);
        queue.add(urlRequest);
    }

    //asynchronously get cached data (TODO)
    private void checkCachedData(boolean details, WeatherLocation location){
        onGotCachedData(null, details, location);
    }

    //callback to determine if cached data is sufficient
    public void onGotCachedData(String data, boolean details, WeatherLocation location){
        //if data is acceptable return it
        if(data != null){
            //manageData(data);
        }
        //else get it from network
        getWeatherData(details, location);
    }

    //handle network response
    @Override
    public void onErrorResponse(VolleyError error) {
        activity.handleError(CODE.NETWORK_ERROR);
    }
    @Override
    public void onResponse(JSONObject response) {
        manageData(response);
    }

    //returns data to activity via callback
    private void manageData(JSONObject data){
        if(data != null) {
            //Update meta with time and loc
            data = setMeta(data);
            this.weatherData = data;
            activity.handleData(this.weatherData);
        } else {
            activity.handleError(CODE.NULL_RESPONSE);
        }
    }

    // get location from GoogleAPIClient and request permission if necessary
    private CODE getLocationData() {
        if (apiClient != null) {

            //Make sure permissions are granted
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return CODE.PERMISSION_DENIED;
            }

            Location rawLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            if(rawLocation != null) {
                lastLocationRequested.location = new SimpleLocation(rawLocation);
            } else {
                return CODE.NULL_LOCATION;
            }
            return CODE.SUCCESS;
        }

        return CODE.NULL_CLIENT;
    }

    public void prepareForDeath(){
        cancelAllDataRequests();

        if(queue != null){
            queue.cancelAll(URL_REQUEST_TAG);
        }

        //Notify storage manager so it can cache state
        StorageManager.prepareForExtermination(activity);
    }

    //Cancel all pending network requests for data
    public void cancelAllDataRequests(){
        if(queue != null){queue.cancelAll(DATA_REQUEST_TAG);}
    }


    private JSONObject setMeta(JSONObject dataToAddTo){
        try {
            JSONObject meta = dataToAddTo.getJSONObject(Keys.meta_data);
            meta.put(Keys.updatedTime,Calendar.getInstance().getTimeInMillis());

            meta.put(Keys.latitude,lastLocationRequested.location.lat);
            meta.put(Keys.longitude,lastLocationRequested.location.lon);

            //If not "My Location" set location name
            if(!lastLocationRequested.locationText.equals(StorageManager.MY_LOCATION_NAME)){
                meta.put(Keys.location,lastLocationRequested.locationText);
            }

            meta.put(Keys.locationIsLive,lastLocationRequested.locationText.equals(StorageManager.MY_LOCATION_NAME));
        } catch (JSONException e) {
            e.printStackTrace();
            //TODO handle
        }

        return dataToAddTo;
    }

    //Activity class that can handle weather data and errors from callbacks and also provide fragments with necessary components to format and retrieve data
    public static abstract class DataHandlerActivity extends DataDisplayActivity implements GoogleApiClient.OnConnectionFailedListener {
        //Methods needed by child fragments to display data and get icons
        public abstract DataFormatter getFormatter();
        public abstract @Nullable JSONObject getData();
        public abstract void requestDataRefresh();
        public abstract void updateDataLocation();

        public abstract GoogleApiClient getAPIClient();

    }

    //Activity class that can handle weather data and errors from callbacks
    public static abstract class DataDisplayActivity extends AppCompatActivity {
        //Callbacks called by NetworkManager when getting data
        public abstract void handleData(JSONObject data);
        public abstract void handleError(CODE error);

        public abstract boolean isOver21();

        public abstract NetworkManager getNetworkManager();
        //Make sure NetworkManager always has reference to current activity
        @Override
        protected void onResume() {
            super.onResume();
            getNetworkManager().activity = this;
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if(getNetworkManager() != null){
                getNetworkManager().prepareForDeath();
            }
        }
    }

}


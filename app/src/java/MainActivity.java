package xyz.eleventhour.averageweather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.Places;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends NetworkManager.DataHandlerActivity implements GoogleApiClient.ConnectionCallbacks {

    private GoogleApiClient googleApiClient;

    //Manages Network
    private NetworkManager networkManager;

    //Manages SharedPrefs and caching
    private StorageManager storageManager;

    //Used to tell whether or not to play transitions
    private boolean isOver21 = false;

    //Spins between available locations to view
    private Spinner locationSpinner;

    //Stores the most recent update type for fragments if present, default is refresh request
    private DataDisplayFragment.UPDATE_TYPE currentUpdateType = DataDisplayFragment.UPDATE_TYPE.SIMPLE_UPDATE;

    //whether the activity was restored from saved instance
    private boolean isRestored = false;

    //Time that overview fragments were updated, independent of NetworkManager time, used to test if they are out of sync
    private long overviewDataUpdatedTime = 0;

    //Whether to check for data in onConnected of api client
    private boolean checkForDataOnConnection = false;

    //Views
    private DrawerLayout mainDrawer;
    private NavigationView navigationView;

    @Override
    public GoogleApiClient getAPIClient() {
        return googleApiClient;
    }

    //Called by fragments to attempt data retrieval
    @Nullable
    @Override
    public JSONObject getData() {
        return networkManager.weatherData;
    }

    @Override
    public boolean isOver21() {
        return isOver21;
    }

    //Called by fragments
    @Override
    public DataFormatter getFormatter() {
        return DataFormatter.getFormatter(storageManager,this);
    }

    //Called by fragments
    @Override
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Override
    public void requestDataRefresh() {
        //Set current update type to refresh
        currentUpdateType = DataDisplayFragment.UPDATE_TYPE.SIMPLE_UPDATE;
        //Get data about current selected location
        requestNewData(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set default settings to prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Set transitions bool
        isOver21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        setGoogleAPIClient();
        storageManager = StorageManager.getSingleton(this);
        networkManager = NetworkManager.getManager(this, googleApiClient);

        isRestored = savedInstanceState != null;

        if(!isRestored) {
            //Set to loading layout at start if layout isn't restored
            setupLayout();
            View wheel = findViewById(R.id.content_loading_wheel);
            if (wheel != null) {
                wheel.setVisibility(View.VISIBLE);
            }
        } else {
            setupLayout();
        }
    }

    //Setup main navigation elements
    private void setupNav(){
        //Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //Set nav drawer toggle
        mainDrawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,mainDrawer,R.string.open_drawer,R.string.close_drawer);
        mainDrawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView)findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                onNavItemSelected(item);
                return true;
            }
        });
    }

    //Called to handle nav menu clicks
    private void onNavItemSelected(MenuItem item){
        mainDrawer.closeDrawers();
        //Set fragment if it isnt already selected
        if(!item.isChecked()) {
            setFragment(item.getItemId(), true);
        }
    }

    //Set associated values for a fragment based on its id in nav menu
    private void setFragment(int itemId, boolean replaceCurrentFrag){
        FragmentManager fragmentManager = getSupportFragmentManager();


        Fragment fragment = null;

        String fragmentName = null;

        switch (itemId){
            case R.id.locations_nav_fragment:
                fragmentName = LocationManagerFragment.FRAGMENT_NAME;
                fragment = new LocationManagerFragment();
                setFragmentToolbarFromResource(R.layout.location_manager_toolbar_layout);
                break;
            case R.id.overview_nav_fragment:
                //Set name, used to search for fragment in manager for reuse
                fragmentName = OverviewFragment.FRAGMENT_NAME;
                //Set fragment
                fragment = new OverviewFragment();
                //Set toolbar
                setFragmentToolbarFromResource(R.layout.overview_toolbar_layout);
                setupOverviewLocationSpinner();
                break;
            case R.id.settings_nav_fragment:
                fragmentName = SettingsFragment.FRAGMENT_NAME;
                fragment = new SettingsFragment();
                setFragmentToolbarFromResource(R.layout.settings_toolbar_layout);
                break;
            case R.id.about_nav_fragment:
                fragmentName = AboutFragment.FRAGMENT_NAME;
                fragment = new AboutFragment();
                setFragmentToolbarFromResource(R.layout.about_toolbar_layout);
                break;
            case R.id.contact_nav_link:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("*/*");
                intent.setData(Uri.parse("mailto:eleventhour@kailas.xyz"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hey! what's up?");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return;
            case R.id.report_nav_link:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://groups.google.com")));
                return;
        }

        //Check if version of fragment still exists in fragment manager to be used
        Fragment oldFragment = fragmentManager.findFragmentByTag(fragmentName);
        if(oldFragment != null){
            fragment = oldFragment;
            Log.e("Debug","Using old frag,memt");
        }

        //Replace with new fragment
        if(fragment != null && replaceCurrentFrag){
            fragmentManager.beginTransaction().replace(R.id.content_frame,fragment,fragmentName).addToBackStack(fragmentName).commit();
        }

        navigationView.setCheckedItem(itemId);
    }


    //Overrides onBackPressed to make sure that the correct item in nav drawer is checked when fragment is change by back button
    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        //If drawer is open, just close it
        if(mainDrawer.isDrawerOpen(navigationView)) {
            mainDrawer.closeDrawers();
        } else {
            if (manager.getBackStackEntryCount() > 1) {
                String newFragmentName = manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 2).getName();
                switch (newFragmentName) {
                    //Set checked item in nav drawer and set toolbar to correct layout
                    case LocationManagerFragment.FRAGMENT_NAME:
                        setFragment(R.id.locations_nav_fragment,false);
                        break;
                    case OverviewFragment.FRAGMENT_NAME:
                        setFragment(R.id.overview_nav_fragment,false);
                        break;
                    case SettingsFragment.FRAGMENT_NAME:
                        setFragment(R.id.settings_nav_fragment,false);
                        break;
                    case AboutFragment.FRAGMENT_NAME:
                        setFragment(R.id.about_nav_fragment,false);
                }
            } else {
                manager.popBackStack();
            }
            super.onBackPressed();
        }

    }

    //Setup default fragments and layout
    private void setupLayout(){
        setContentView(R.layout.main_activity);

        setupNav();

        //Set appropriate fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.findFragmentById(R.id.content_frame) == null) {
            //add overview fragment if none other is present
            setFragment(R.id.overview_nav_fragment,true);
        } else {
            //setFragment but don't replace
            NavIDableFragment fragment = ((NavIDableFragment) fragmentManager.findFragmentById(R.id.content_frame));
            if(fragment != null) {
                setFragment(fragment.getIdInNavMenu(), false);
            }
        }

        //Slide toolbar down
        if(!isRestored) {
            View toolbar = findViewById(R.id.default_toolbar);
            AnimationManager.dropDown(toolbar);
        }
    }

    //Sets toolbar content to a provided resource
    private void setFragmentToolbarFromResource(int resId){
        FrameLayout toolbarContentLayout = (FrameLayout) findViewById(R.id.main_toolbar_content_holder);
        //Remove previous views
        toolbarContentLayout.removeAllViews();

        View newToolBarLayout = LayoutInflater.from(this).inflate(resId,toolbarContentLayout,true);
        //Fade in new layout
        AnimationManager.loadText(newToolBarLayout,null);
    }

    //Sets up location spinner for overview fragment toolbar content
    private void setupOverviewLocationSpinner(){
        locationSpinner = (Spinner) findViewById(R.id.overview_location_filter);

        locationSpinner.setAdapter(new OverviewSpinnerAdapter(this,R.layout.overview_spinner_item_layout,storageManager.getLocations()));
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WeatherLocation selectedLocation = storageManager.getLocations().get(position);
                /*When a new location item is selected call updateDataLocation if:the location is not the same as the previously requested location
                 */
                if(networkManager.lastLocationRequested != null && !selectedLocation.locationText.equals(networkManager.lastLocationRequested.locationText)) {
                    updateDataLocation();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Current data is kept
            }
        });
    }

    //set to main view and add overview fragment
    @Override
    public void handleData(JSONObject data) {
        //Callback to all current fragments about new data passing update type
        for (Fragment fragment:
                getSupportFragmentManager().getFragments()) {
            //Make sure to only update added fragments
            if(fragment instanceof DataDisplayFragment && fragment.isAdded()) {
                //Notify fragments
                ((DataDisplayFragment) fragment).updateCallback(currentUpdateType);
            }
        }

        overviewDataUpdatedTime = Calendar.getInstance().getTimeInMillis();

        //Hide loading wheel
        View wheel = findViewById(R.id.content_loading_wheel);
        if(wheel != null){
            wheel.setVisibility(View.INVISIBLE);
        }

        View loadingContainer = findViewById(R.id.loading_layout);
        //If the loading view is set, change to main overview
        if (loadingContainer != null) {
            //Animate loading view leaving up
            AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(this,R.animator.slide_loading_out);
            animatorSet.setTarget(loadingContainer);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    //Setup layout after animation
                    setupLayout();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    setupLayout();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            animatorSet.start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                mainDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Gets new data at selected location and notifies all attached fragments
    public void updateDataLocation(){
        //Set data update type, override default type
        currentUpdateType = DataDisplayFragment.UPDATE_TYPE.COMPLETE_REFRESH;
        //Request new data for location when a new one is selected
        requestNewData(true);
        //Notify all fragments about change
        for (Fragment fragment:
                getSupportFragmentManager().getFragments()) {
            if(fragment instanceof DataDisplayFragment && fragment.isAdded()) {
                //Notify fragments
                ((DataDisplayFragment) fragment).prepareForLocationChange();
            }
        }
    }

    //handle error in getting data
    @Override
    public void handleError(NetworkManager.CODE error) {
        //TODO: something here
        Log.e("Debug",error.name());
    }

    //handle user's response to permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case NetworkManager.LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //if permission if granted retry getting data
                    currentUpdateType = DataDisplayFragment.UPDATE_TYPE.SIMPLE_UPDATE;
                    requestNewData(true);
                } else {
                    //TODO:reprimand user
                }
                break;
        }
    }

    //get data when GoogleAPIClient is connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(checkForDataOnConnection){
            checkForDataOnConnection = false;
            getDataIfNecessary();
        }
    }

    //return whether the current selected location is the "My Location" choice
    private boolean theCurrentSelectedLocationIsLive(){
        return storageManager.getLocations().get(0).locationText.equals(StorageManager.MY_LOCATION_NAME);
    }

    @Override
    protected void onResume()
    {
        setHeaderImg();

        //Checks for new data onResume of activity, if apiClient isn't ready set bool to check when it is
        super.onResume();
        if(googleApiClient.isConnected() || !theCurrentSelectedLocationIsLive()) {
            checkForDataOnConnection = false;
            getDataIfNecessary();
        } else {
            checkForDataOnConnection = true;
        }
    }

    //Sets colors based on the time of day
    private void setHeaderImg(){

        int drawableID;

        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        //Set background image based on current time of day
        if(hourOfDay < 6 || hourOfDay > 20){
            drawableID = R.drawable.night_background_default;
        } else if(hourOfDay < 9){
            drawableID = R.drawable.dawn_background_default;
        } else if(hourOfDay > 17){
            drawableID = R.drawable.dusk_background_default;
        } else {
            drawableID = R.drawable.day_background_default;
        }

        //Set header image if it exists
        if(findViewById(R.id.nav_header_image) != null){
            ((ImageView) findViewById(R.id.nav_header_image)).setImageResource(drawableID);
        }
    }

    //Checks if new data should be gotten
    private void getDataIfNecessary(){
        long millisSinceOverviewWasUpdated = Calendar.getInstance().getTimeInMillis() - overviewDataUpdatedTime;

        currentUpdateType = DataDisplayFragment.UPDATE_TYPE.SIMPLE_UPDATE;
        if (networkManager.weatherData == null) {
            requestNewData(true);
        } else if (millisSinceOverviewWasUpdated > 3600000 || (theCurrentSelectedLocationIsLive() && millisSinceOverviewWasUpdated > 1800000)) {

            try {
                //If newer data is available in networkmanager but is not currently being displayed, show it
                if (Calendar.getInstance().getTimeInMillis() - getData().getJSONObject(Keys.meta_data).getLong(Keys.updatedTime) < 1500000) {
                    handleData(getData());
                } else {
                    requestNewData(true);
                }
            } catch (JSONException | NullPointerException e) {
                requestNewData(true);
            }
        }
    }

    //unused
    @Override
    public void onConnectionSuspended(int i) {}

    //set GoogleAPIClient (used for location)
    private void setGoogleAPIClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .build();
    }

    //Called if GoogleAPIClient failed at something
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO: Do something here
    }

    private void requestNewData(boolean details){
        WeatherLocation location;

        //Show loading wheel
        if(currentUpdateType == DataDisplayFragment.UPDATE_TYPE.COMPLETE_REFRESH) {
            View wheel = findViewById(R.id.content_loading_wheel);
            if (wheel != null) {
                wheel.setVisibility(View.VISIBLE);

            }
        }

        //Cancel pending requests to prevent duplicate calls
        networkManager.cancelAllDataRequests();

        //If method is being called before layout is drawn use first location in list, (most recent)
        //else use current selected
        if(locationSpinner == null){
            location = storageManager.getLocations().get(0);
        } else {
            location = (WeatherLocation) locationSpinner.getSelectedItem();
            //put selected element at front or list, then notify adapter
            storageManager.getLocations().add(0,storageManager.getLocations().remove(locationSpinner.getSelectedItemPosition()));
            ((OverviewSpinnerAdapter) locationSpinner.getAdapter()).notifyDataSetChanged();
            locationSpinner.setSelection(0,true);
        }

        if(networkManager != null) {
            networkManager.getData(details,location);
        }
    }

    //Interface for fragments that can provide activity with reference which item id in nav drawer they correspond to
    public interface NavIDableFragment {
        int getIdInNavMenu();
    }

    //Called when an attribution is clicked on and starts a new activity in browser
    public void onAttributionClick(View view) {
        Uri uri = null;

        switch (view.getId()){
            case R.id.forecast_badge:
                uri = Uri.parse("https://forecast.io");
                break;
            case R.id.openweathermap_badge:
                uri =Uri.parse("http://openweathermap.org/");
                break;
            case R.id.volley:
                uri =Uri.parse("https://android.googlesource.com/platform/frameworks/volley/");
                break;
            case R.id.MatWeather:
                uri =Uri.parse("https://prithusworks.blogspot.in/2015/07/matweather-material-weather-icon-set.html");
                break;
            case R.id.AppIconAttr:
                uri =Uri.parse("https://github.com/Maddoc42/Android-Material-Icon-Generator");
                break;
            case R.id.contact_attr_link:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("*/*");
                intent.setData(Uri.parse("mailto:eleventhour@kailas.xyz"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hey! what's up?");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return;
        }

        if(uri != null){
            startActivity(new Intent(Intent.ACTION_VIEW,uri));
        }
    }

}

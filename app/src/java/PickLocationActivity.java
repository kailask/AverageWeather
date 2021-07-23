package xyz.eleventhour.averageweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Created by Kailas on 8/1/2016.
 */
public class PickLocationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private PendingResult<AutocompletePredictionBuffer> results;

    private LocationSearchResultAdapter locationListAdapter;

    private SearchView searchView;

    private AutocompleteFilter.Builder builder = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES);

    private ViewGroup errorLayout;

    private RecyclerView locationList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_location_activity);

        setup();

        errorLayout = ((ViewGroup) findViewById(R.id.location_picker_error_layout));

        locationList = ((RecyclerView) findViewById(R.id.location_result_list));
        locationList.setLayoutManager(new LinearLayoutManager(this));
        locationListAdapter = new LocationSearchResultAdapter(null,this);
        locationList.setAdapter(locationListAdapter);

        //Activity maintains an apiclient separate from others
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();
    }

    //Request autocomplete suggestions
    private void onNewQuery(final String query){
        if(results != null){
            results.cancel();
        }

        //Bounds encompass the world to eliminate bias
        results = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient,query,new LatLngBounds(new LatLng(-85,-180),new LatLng(85,180)),builder.build());

        results.setResultCallback(new ResultCallbacks<AutocompletePredictionBuffer>() {
            @Override
            public void onSuccess(@NonNull AutocompletePredictionBuffer autocompletePredictions) {

                //If there were no previous predictions show list sliding in
                if(locationListAdapter.getItemCount() <= 0){
                    AnimationManager.slideFadeDown(locationList,getApplicationContext());
                }

                //Release old predictions, notify adapter
                locationListAdapter.release();
                locationListAdapter.locations = autocompletePredictions;
                locationListAdapter.notifyDataSetChanged();

                //Show error layout if there are no predictions
                if(autocompletePredictions.getCount() < 1 && query.length() > 0){
                    errorLayout.setVisibility(View.VISIBLE);
                } else {
                    errorLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Status status) {
                //TODO handle
            }
        });
    }

    private void setup(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.pick_location_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pick_location_toolbar_menu,menu);

        MenuItem searchItem = menu.findItem(R.id.location_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setIconified(false);

        searchView.setQueryHint(getResources().getString(R.string.hint_search));

        searchView.setOnQueryTextListener(this);

        //Disable landscape fullscreen
        int options = searchView.getImeOptions();
        searchView.setImeOptions(options | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        //searchView cannot be closed
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        onNewQuery(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO handle
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(results != null){
            results.cancel();
        }

        //Release adapter buffer to prevent leak
        locationListAdapter.release();
    }

    public void onLocationClick(String placeID){
        //Return placeID to calling activity for lookup
        setResult(Activity.RESULT_OK,new Intent().putExtra(LocationManagerFragment.PLACE_ID_KEY,placeID));
        supportFinishAfterTransition();
    }
}

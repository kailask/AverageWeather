package xyz.eleventhour.averageweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;


/**
 * Created by Kailas on 7/26/2016.
 */
public class LocationManagerFragment extends Fragment implements MainActivity.NavIDableFragment {

    public static final int ADD_LOCATION_REQUEST_CODE = 6;
    public static final String PLACE_ID_KEY = "place_id";

    public static final String FRAGMENT_NAME = "JEFF";

    private StorageManager storageManager;

    private FloatingActionButton addLocationFab;

    private RecyclerView locationList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        storageManager = StorageManager.getSingleton(getActivity());

        View layout = inflater.inflate(R.layout.manage_locations_fragment,container,false);

        locationList = (RecyclerView) layout.findViewById(R.id.location_manager_list);
        addLocationFab = ((FloatingActionButton) layout.findViewById(R.id.add_location_fab));

        //Setup adapter for list
        locationList.setLayoutManager(new LinearLayoutManager(getActivity()));

        LocationManagerAdapter adapter = new LocationManagerAdapter(storageManager.getLocations(),getActivity());
        locationList.setAdapter(adapter);

        //Setup ItemTouchHelper for swipe-dismiss and drag
        LocationManagerListHelperCallback callback = new LocationManagerListHelperCallback(adapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(locationList);


        //Start PickLocationActivity to get a new location
        addLocationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(),PickLocationActivity.class);

                //Set activity transition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().getWindow().setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.location_manager_to_search_transition));
                }

                startActivityForResult(i,ADD_LOCATION_REQUEST_CODE, ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle());
            }
        });

        return layout;
    }

    @Override
    public int getIdInNavMenu() {
        return R.id.locations_nav_fragment;
    }

    //Called on return from PickLocationActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ADD_LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            String placeID = data.getStringExtra(PLACE_ID_KEY);

            //Show loading wheel
            final View wheel = getActivity().findViewById(R.id.content_loading_wheel);
            if (wheel != null) {
                wheel.setVisibility(View.VISIBLE);
            }

            //Get Place from id
            Places.GeoDataApi.getPlaceById(((NetworkManager.DataHandlerActivity) getActivity()).getAPIClient(),placeID).setResultCallback(new ResultCallbacks<PlaceBuffer>() {
                @Override
                public void onSuccess(@NonNull PlaceBuffer places) {
                    //Add place to storageManager and notify adapter
                    if(places.getStatus().isSuccess() && places.getCount() > 0){
                        Place place = places.get(0);
                        WeatherLocation weatherLocation = new WeatherLocation(((String) place.getName()), ((float) place.getLatLng().latitude), ((float) place.getLatLng().longitude));
                        storageManager.getLocations().add(0,weatherLocation);
                        locationList.getAdapter().notifyDataSetChanged();
                        if(wheel != null){
                            wheel.setVisibility(View.INVISIBLE);
                        }
                    }

                    //Release buffer
                    places.release();
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    //TODO handle
                }
            });
        }
    }
}

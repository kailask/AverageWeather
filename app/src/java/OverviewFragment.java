package xyz.eleventhour.averageweather;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

/**
 * Created by Kailas on 5/22/2016.
 *
 * Fragment that displays overview data in child fragments
 */
public class OverviewFragment extends Fragment implements DataDisplayFragment {

    private View layout;
    private VerticalSwipeToRefresh swipeToRefresh;
    private ScrollView scrollView;

    private TextView updatedText;

    //Prevents double click opening two activities
    private boolean wasJustClicked = false;

    //Used to help tell when to show the footer (it should be invisible if the fragments havent been set)
    private boolean fragmentsHaveBeenSet = false;

    NetworkManager.DataHandlerActivity activity;

    public static final String FRAGMENT_NAME = "I_WAS_NEVER_GIVEN_A_NAME";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.overview_fragment, container, false);

        if(savedInstanceState != null){
            fragmentsHaveBeenSet = true;
        }

        //Set swipe to refresh listener
        swipeToRefresh = (VerticalSwipeToRefresh) layout.findViewById(R.id.swipe_layout);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateFragments();
            }
        });
        scrollView = (ScrollView) layout.findViewById(R.id.overview_scrollview);
        updatedText = ((TextView) layout.findViewById(R.id.footer_updated_time));

        activity = ((NetworkManager.DataHandlerActivity) getActivity());
        return layout;
    }

    //Request updated data
    private void updateFragments(){
        ((NetworkManager.DataHandlerActivity)getActivity()).requestDataRefresh();
    }

    //Animate cards out to indicate refresh is in progress
    @Override
    public void prepareForLocationChange() {
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_list_down);
        animatorSet.setTarget(layout);
        animatorSet.start();
    }

    //Called when new data is available
    @Override
    public void updateCallback(UPDATE_TYPE type){
        JSONObject newData = ((NetworkManager.DataHandlerActivity)getActivity()).getData();
        scrollView.smoothScrollTo(0,0);

        swipeToRefresh.setRefreshing(false);

        if(newData != null) {
            switch (type) {
                case SIMPLE_UPDATE:
                    setFragments(true, newData);
                    break;
                case COMPLETE_REFRESH:
                    setFragments(false, newData);
                    break;
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        //Make sure to reset wasJustClicked
        wasJustClicked = false;

        //Footer should not be visible if fragments have not bees set the first time
        if(fragmentsHaveBeenSet) {
            layout.findViewById(R.id.footer_layout).setVisibility(View.VISIBLE);

            //Updated time must be set on start since it is not restored
            JSONObject data = activity.getData();
            if(data != null){
                //Set updated since time
                try {
                    updatedText.setText(activity.getFormatter().formatUpdatedSince(data.getJSONObject(Keys.meta_data).getLong(Keys.updatedTime)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        swipeToRefresh.setRefreshing(false);
    }

    //Called by child fragments onClick
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    @SafeVarargs
    public final void registerClick(CLICK_TYPE type, int position, Pair<View, String>... sharedViews) {
        //Prevent double click
        if(!wasJustClicked) {
            wasJustClicked = true;

            Intent detailIntent = null;

            NetworkManager.DataDisplayActivity activity = (NetworkManager.DataDisplayActivity) getActivity();

            //Make sure activity transitions are null
            if (activity.isOver21()) {
                activity.getWindow().setExitTransition(null);
                activity.getWindow().setReturnTransition(null);
            }

            switch (type) {
                case CURRENT_CARD:
                    detailIntent = new Intent(activity, CurrentlyDetailActivity.class);
                    break;
                case DAILY_CARD:
                    detailIntent = new Intent(activity, DailyDetailActivity.class);
                    break;
            }

            if (detailIntent != null) {
                //Put position if necessary
                if (position != POS_NOT_AVAILABLE) {
                    detailIntent.putExtra(POS_KEY, position);
                }

                //If there are shared views, use them
                if (sharedViews.length > 0) {
                    startActivity(detailIntent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedViews).toBundle());
                } else {
                    startActivity(detailIntent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
                }
            }
        }

    }

    //Called by activity to get toolbar layout
    @Override
    public int getIdInNavMenu() {
        return R.id.overview_nav_fragment;
    }

    //Add sub fragments
    private void setFragments(boolean isRefreshRequest, JSONObject data) {

        fragmentsHaveBeenSet = true;

        layout.findViewById(R.id.footer_layout).setVisibility(View.VISIBLE);

        //Strings retrieved from JSON data to be passed in arguments to different fragments
        String iconID = null;
        String temperature = null, description = null, hourlyData = null, hourlyDesc = null,dailyData = null , dailyDesc = null, timezoneID = null;
        FragmentManager fragmentManager = getChildFragmentManager();

        if(isRefreshRequest && (fragmentManager.getFragments() == null || fragmentManager.getFragments().size() < 3)){
            isRefreshRequest = false;
        }

        try {
            //Currently Data
            iconID = data.getString(Keys.iconID);
            temperature = String.valueOf(data.getInt(Keys.temperature));
            description = data.getString(Keys.description);

            //Hourly Data
            JSONObject hourly = data.getJSONObject(Keys.hourly_data);
            if(hourly != null) {
                hourlyData = hourly.getJSONArray(Keys.data).toString();
                hourlyDesc = hourly.getString(Keys.description);
            }

            //Daily Data
            JSONObject daily = data.getJSONObject(Keys.daily_data);
            if(daily != null) {
                dailyData = daily.getJSONArray(Keys.data).toString();
                dailyDesc = daily.getString(Keys.description);
            }

            JSONObject meta = data.getJSONObject(Keys.meta_data);

            //Set updated since time
            updatedText.setText(activity.getFormatter().formatUpdatedSince(meta.getLong(Keys.updatedTime)));

            timezoneID = meta.getString(Keys.timezone);

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: Handle?
        }

        //Set args
        Bundle currentArgs = new Bundle();
        currentArgs.putString(CurrentWeatherCardFragment.TEMP_KEY, temperature);
        currentArgs.putString(CurrentWeatherCardFragment.DESC_KEY, description);
        currentArgs.putString(CurrentWeatherCardFragment.ICON_KEY, iconID);
        currentArgs.putString(CurrentWeatherCardFragment.TIMEZONE_KEY,timezoneID);

        Bundle hourlyArgs = new Bundle();
        hourlyArgs.putString(WeatherPreviewCardFragment.DESC_KEY,hourlyDesc);
        hourlyArgs.putString(WeatherPreviewCardFragment.DATA_KEY,hourlyData);
        hourlyArgs.putString(WeatherPreviewCardFragment.TYPE_KEY,WeatherPreviewCardFragment.HOURLY);
        hourlyArgs.putString(WeatherPreviewCardFragment.TIMEZONE_KEY,timezoneID);

        Bundle dailyArgs = new Bundle();
        dailyArgs.putString(WeatherPreviewCardFragment.DESC_KEY,dailyDesc);
        dailyArgs.putString(WeatherPreviewCardFragment.DATA_KEY,dailyData);
        dailyArgs.putString(WeatherPreviewCardFragment.TYPE_KEY,WeatherPreviewCardFragment.DAILY);
        dailyArgs.putString(WeatherPreviewCardFragment.TIMEZONE_KEY,timezoneID);


        //Add fragments if necessary, otherwise update the views of fragment that exists
        if(!isRefreshRequest) {
            WeatherPreviewCardFragment dailyFragment = new WeatherPreviewCardFragment();
            dailyFragment.setArguments(dailyArgs);
            fragmentManager.beginTransaction().replace(R.id.daily_preview_frame, dailyFragment).commit();

            WeatherPreviewCardFragment hourlyFragment = new WeatherPreviewCardFragment();
            hourlyFragment.setArguments(hourlyArgs);
            fragmentManager.beginTransaction().replace(R.id.hourly_preview_frame, hourlyFragment).commit();

            CurrentWeatherCardFragment currentFragment = new CurrentWeatherCardFragment();
            currentFragment.setArguments(currentArgs);
            fragmentManager.beginTransaction().replace(R.id.currently_card_frame, currentFragment).commit();

            //Dont play entrance animation if only refreshing
            AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_list_up);
            animatorSet.setTarget(layout);
            animatorSet.start();
        } else {
            WeatherPreviewCardFragment weatherPreviewCardFragment = (WeatherPreviewCardFragment) fragmentManager.findFragmentById(R.id.daily_preview_frame);
            weatherPreviewCardFragment.updateViews(dailyArgs);

            weatherPreviewCardFragment = (WeatherPreviewCardFragment) fragmentManager.findFragmentById(R.id.hourly_preview_frame);
            weatherPreviewCardFragment.updateViews(hourlyArgs);

            CurrentWeatherCardFragment currentWeatherCardFragment = (CurrentWeatherCardFragment)fragmentManager.findFragmentById(R.id.currently_card_frame);
            currentWeatherCardFragment.updateViews(currentArgs);
        }
    }

}

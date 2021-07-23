package xyz.eleventhour.averageweather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ActionMenuView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Kailas on 5/28/2016.
 */
public class CurrentlyDetailActivity extends DetailActivity implements AppBarLayout.OnOffsetChangedListener, Transition.TransitionListener, Response.ErrorListener,Response.Listener<String>{

    //Play scroll animations only if in portrait
    private boolean isPortrait = true;

    private AppBarLayout scrollManager;

    private TextView toolbarDescription, headerDescription, headerLocation, headerTemp;
    private NetworkImageView background;
    private View dataHolder,toolbar;
    private ImageView headerIcon;

    //Values for scrolling animation
    private boolean toolbarIsVisible = false, headerLayoutIsVisible = true;
    private final float TOOLBAR_ANIMATION_THRESHOLD = 0.9f;
    private final float HEADER_ANIMATION_THRESHOLD = .5f;
    private final int ANIMATION_DURATION_LONG = 200;

    //Needed to get icons for night/day at appropriate times
    private TimeZone timeZone = TimeZone.getDefault();

    private boolean isOver21,transitionFinished = false;

    private String backgroundImageURL = null;

    @Override
    public boolean isOver21() {
        return isOver21;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.current_detail_activity);

        isPortrait = true;//(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);

        toolbarDescription = (TextView) findViewById(R.id.detail_toolbar_desc);
        headerDescription = (TextView) findViewById(R.id.detail_desc_text_large);
        headerLocation = (TextView) findViewById(R.id.detail_location_text);
        headerTemp=(TextView) findViewById(R.id.detail_temp_text_large);
        background = (NetworkImageView) findViewById(R.id.detail_activity_background);
        dataHolder = findViewById(R.id.detail_data_holder);
        toolbar = findViewById(R.id.detail_toolbar);
        headerIcon = ((ImageView) findViewById(R.id.detail_header_icon));

        setTimeDependentAttrs();

        //Animations are only played if in portrait mode
        if(isPortrait) {
            scrollManager = (AppBarLayout) findViewById(R.id.detail_appbar);
            scrollManager.addOnOffsetChangedListener(this);
        }

        //Used to set animations only if material transitions are available
        isOver21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        //If activity is restored transition will not play so transitionFinished needs to be set true
        if(savedInstanceState != null){
            transitionFinished = true;
        }

        if(isOver21){
            TransitionSet transitionSetIn = (TransitionSet) TransitionInflater.from(this).inflateTransition(R.transition.current_detail_activity_transition);
            getWindow().setEnterTransition(transitionSetIn);
            TransitionSet transitionSetOut = (TransitionSet) TransitionInflater.from(this).inflateTransition(R.transition.current_detail_out);
            getWindow().setReturnTransition(transitionSetOut);

            //Set listener to perform enter animations after transition finishes
            transitionSetIn.addListener(this);
        } else {
            onAfterTransition();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(transitionFinished){
            onAfterTransition();
        }

        //Set background
        setTimeDependentAttrs();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        float percentage = (float) Math.abs(verticalOffset) / (float) appBarLayout.getTotalScrollRange();

        handleHeaderAnimation(percentage);
        handleToolbarAnimation(percentage);

    }

    //Handle the animation of the toolbar description sliding and toolbar fading in
    private void handleToolbarAnimation(float percentage){

        if(percentage >= TOOLBAR_ANIMATION_THRESHOLD && !toolbarIsVisible){
            toolbarIsVisible = true;
            toolbarDescription.animate().setInterpolator(new DecelerateInterpolator()).setDuration(ANIMATION_DURATION_LONG).translationX(0).alpha(1).start();

            //Animate toolbar background color fade
            ObjectAnimator.ofObject(toolbar,"backgroundColor",new ArgbEvaluator(),ContextCompat.getColor(this,R.color.transparent),ContextCompat.getColor(this,R.color.colorPrimary))
                    .setDuration(ANIMATION_DURATION_LONG)
                    .start();

        } else if(percentage <= TOOLBAR_ANIMATION_THRESHOLD && toolbarIsVisible){
            toolbarIsVisible = false;
            toolbarDescription.animate().setInterpolator(new AccelerateInterpolator()).setDuration(ANIMATION_DURATION_LONG).translationX(500).alpha(0).start();

            //Animate toolbar background color fade
            ObjectAnimator.ofObject(toolbar,"backgroundColor",new ArgbEvaluator(),ContextCompat.getColor(this,R.color.colorPrimary),ContextCompat.getColor(this,R.color.transparent))
                    .setDuration(ANIMATION_DURATION_LONG)
                    .start();
        }
    }

    //Handle animation of header text fading in and out
    private void handleHeaderAnimation(float percentage){

        if(percentage <= HEADER_ANIMATION_THRESHOLD && !headerLayoutIsVisible){
            headerLayoutIsVisible = true;
            headerDescription.animate().setDuration(ANIMATION_DURATION_LONG).alpha(1f).start();
            headerLocation.animate().setDuration(ANIMATION_DURATION_LONG).alpha(1f).start();
            headerTemp.animate().setDuration(ANIMATION_DURATION_LONG).alpha(1f).start();
            headerIcon.animate().setDuration(ANIMATION_DURATION_LONG).alpha(1f).start();

        } else if(percentage >= HEADER_ANIMATION_THRESHOLD && headerLayoutIsVisible){
            headerLayoutIsVisible = false;
            headerDescription.animate().setDuration(ANIMATION_DURATION_LONG).alpha(0f).start();
            headerLocation.animate().setDuration(ANIMATION_DURATION_LONG).alpha(0f).start();
            headerTemp.animate().setDuration(ANIMATION_DURATION_LONG).alpha(0f).start();
            headerIcon.animate().setDuration(ANIMATION_DURATION_LONG).alpha(0f).start();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        //TODO
    }

    //Set image URL to URL returned by manager
    @Override
    public void onResponse(String response) {
        if(response != null) {

            //Eliminate html entity
            response = response.replaceAll("&amp;","&");

            backgroundImageURL = response;

            //Only set image after transition has finished to avoid weird crazy glitches in coodinatorlayout/shared transition behavior
            if(transitionFinished) {
                background.setImageUrl(response, manager.getImageLoader());
            }
        }
    }

    //Parse data and set it to views
    public void setData(JSONObject jsonData){

        String iconID = null;
        String lat = null,lon=null;

        if(jsonData != null) {
            DataFormatter.JSONDataGetter dataGetter = new DataFormatter.JSONDataGetter(jsonData);

            String desc = dataGetter.getString(Keys.description);
            iconID = dataGetter.getString(Keys.iconID);

            DataFormatter.JSONDataGetter metaData = new DataFormatter.JSONDataGetter(dataGetter.getJSONObject(Keys.meta_data));

            AnimationManager.loadText(findViewById(R.id.detail_updated_text),formatter.formatUpdatedSince(metaData.getLong(Keys.updatedTime)));

            AnimationManager.loadText(headerLocation,formatter.formatText(metaData.getString(Keys.location)));

            lat = metaData.getString(Keys.latitude);
            lon = metaData.getString(Keys.longitude);

            updated = metaData.getLong(Keys.updatedTime);

            //Set city icon
            if(metaData.getBool(Keys.locationIsLive)){
                headerIcon.setImageResource(R.drawable.ic_my_location_white);
            } else {
                headerIcon.setImageResource(R.drawable.ic_location_city_white);
            }

            //Set header and toolbar values
            toolbarDescription.setText(formatter.formatText(desc));
            AnimationManager.loadText(headerDescription,formatter.formatTextCaps(desc));
            AnimationManager.loadText(headerTemp,formatter.formatTemp(dataGetter.getInt(Keys.temperature)));

            //Set other card values
            AnimationManager.loadText(findViewById(R.id.detail_card_percip_text),formatter.formatPercip((dataGetter.getDouble(Keys.percip))));
            AnimationManager.loadText(findViewById(R.id.detail_card_percipProb_text),formatter.formatPercipProb(dataGetter.getDouble(Keys.percip_prob)));

            AnimationManager.loadText(findViewById(R.id.detail_card_wind_text),formatter.formatWind(dataGetter.getInt(Keys.wind_speed)));
            AnimationManager.loadText(findViewById(R.id.detail_card_wind_baring_text),formatter.formatWindBaring(dataGetter.getInt(Keys.wind_baring)));

            AnimationManager.loadText(findViewById(R.id.detail_card_cloud_cover_text),formatter.formatClouds(dataGetter.getDouble(Keys.cloud_cover)));
            AnimationManager.loadText(findViewById(R.id.detail_card_nearest_storm_text),formatter.formatNearestStorm(dataGetter.getInt(Keys.nearest_storm)));

            AnimationManager.loadText(findViewById(R.id.detail_card_visibility_text),formatter.formatVisibility(dataGetter.getInt(Keys.visibility)));

            AnimationManager.loadText(findViewById(R.id.detail_humidity_value),formatter.formatHumidity(dataGetter.getDouble(Keys.humidity)));
            AnimationManager.loadText(findViewById(R.id.detail_pressure_value),formatter.formatPressure(dataGetter.getInt(Keys.pressure)));

            timeZone = dataGetter.getTimezone();

            try{
                //Get sunrise and sunset from first day in daily array
                DataFormatter.JSONDataGetter firstDay = new DataFormatter.JSONDataGetter(jsonData.getJSONObject(Keys.daily_data).getJSONArray(Keys.data).getJSONObject(0));

                AnimationManager.loadText(findViewById(R.id.detail_sunrise_value),formatter.formatSuntime(firstDay.getLong(Keys.sunrise),timeZone));
                AnimationManager.loadText(findViewById(R.id.detail_sunset_value),formatter.formatSuntime(firstDay.getLong(Keys.sunset),timeZone));
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO: error here
            }

            NetworkImageView imageView = (NetworkImageView) findViewById(R.id.detail_icon_large);

            imageView.setDefaultImageResId(R.drawable.ic_weather_large_default);
            imageView.setErrorImageResId(R.drawable.ic_unknown_value_large);

            ImageLoader imageLoader = manager.getImageLoader();

            setTimeDependentAttrs();

            if(imageLoader != null) {
                //Set icon
                if (iconID != null) {
                    //Use calendar to determine the time current time and get appropriate icon
                    Calendar currentTime = Calendar.getInstance(timeZone);
                    String iconURL = formatter.getIconURL(DataFormatter.ICON_LARGE, iconID, currentTime.get(Calendar.HOUR_OF_DAY));
                    imageView.setImageUrl(iconURL, imageLoader);
                }

                if (lat != null && lon != null) {
                    //Request URL
                    manager.getBackgroundURL(lat,lon,this,this);
                }
            }

        } else {
            manager.getData(false,manager.lastLocationRequested);
        }
    }


    private void setTimeDependentAttrs(){
        //Get calendar for time of update
        Calendar calendar = Calendar.getInstance(timeZone);

        int drawableID;
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

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

        background.setDefaultImageResId(drawableID);
        background.setErrorImageResId(drawableID);
    }

    @Override
    public void supportFinishAfterTransition() {
        //Fade out header text
        AnimationManager.fade(headerTemp,View.INVISIBLE);
        AnimationManager.fade(headerLocation,View.INVISIBLE);
        AnimationManager.fade(headerDescription,View.INVISIBLE);
        AnimationManager.fade(headerIcon,View.INVISIBLE);

        //Slide down list
        dataHolder.animate().translationY(1500).setDuration(1000).setInterpolator(new DecelerateInterpolator()).start();

        super.supportFinishAfterTransition();
    }

    //Animate cards up over shared element after transition is over
    @Override
    public void onTransitionCancel(Transition transition) {
        onAfterTransition();
    }
    @Override
    public void onTransitionEnd(Transition transition) {
        onAfterTransition();
    }

    private void onAfterTransition(){
        Animator animator = AnimatorInflater.loadAnimator(this,R.animator.slide_list_up);
        animator.setTarget(dataHolder);
        animator.start();

        //Only set image after transition has finished to avoid weird crazy glitches in coordinatorlayout/shared transition behavior
        if(backgroundImageURL != null) {
            background.setImageUrl(backgroundImageURL, manager.getImageLoader());
        }

        transitionFinished = true;
    }


    //Unused implementations
    @Override
    public void onTransitionStart(Transition transition) {
    }

    @Override
    public void onTransitionPause(Transition transition) {
    }

    @Override
    public void onTransitionResume(Transition transition) {
    }
}

package xyz.eleventhour.averageweather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Kailas on 6/15/2016.
 */
public class DailyDetailActivity extends DetailActivity implements AppBarLayout.OnOffsetChangedListener, Transition.TransitionListener{

    private ViewPager pager;

    //Keep track of if the pager position has already been set
    private boolean positionHasBeenSet = false;

    private float descFadeThreshold = .1f;
    //Whether or not the header desc is faded
    private boolean isFaded = false;

    private TextView headerDesc, toolbarTextView;
    private TabLayout tabs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.daily_detail_activity);

        pager = (ViewPager) findViewById(R.id.daily_detail_pager);
        tabs = (TabLayout) findViewById(R.id.detail_tabs_layout);
        headerDesc = (TextView) findViewById(R.id.detail_week_desc_text);
        toolbarTextView = (TextView) findViewById(R.id.daily_detail_toolbar_text);


        if(isOver21()){
            TransitionSet transitionSetIn = (TransitionSet) TransitionInflater.from(this).inflateTransition(R.transition.daily_detail_activity_transition);
            transitionSetIn.setInterpolator(new DecelerateInterpolator());
            getWindow().setEnterTransition(transitionSetIn);

            TransitionSet transitionSetOut = (TransitionSet) TransitionInflater.from(this).inflateTransition(R.transition.daily_detail_activity_transition);
            transitionSetOut.setInterpolator(new AccelerateInterpolator());
            getWindow().setReturnTransition(transitionSetOut);

            transitionSetIn.addListener(this);
        } else {
            tabs.setVisibility(View.VISIBLE);
        }

        //Set listener for fading header description
        AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.daily_detail_appbar);
        appBarLayout.addOnOffsetChangedListener(this);

        super.onCreate(savedInstanceState);

        if(pager.getAdapter() != null) {
            tabs.setupWithViewPager(pager);

            //Set pager and tab position from bundle args (based on which preview card was tapped), must be done after layout is drawn
            tabs.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    tabs.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setPosition();
                }
            });
        }

        //Tabs may be invisible after rotation because onTransitionEnd is never called
        if(savedInstanceState != null){
            tabs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        float percentageCollapsed = Math.abs(verticalOffset)/appBarLayout.getTotalScrollRange();

        percentageCollapsed = new AccelerateInterpolator().getInterpolation(percentageCollapsed);

        //Fade header if necessary on appbar offset change
        if(percentageCollapsed >= descFadeThreshold && !isFaded){
            AnimationManager.fade(headerDesc, View.INVISIBLE);
            isFaded = true;
        } else if(percentageCollapsed < descFadeThreshold && isFaded){
            AnimationManager.fade(headerDesc,View.VISIBLE);
            isFaded = false;
        }

    }

    @Override
    void setData(JSONObject data) {
        if(data != null) {

            try {
                DataFormatter.JSONDataGetter getter = new DataFormatter.JSONDataGetter(data);
                //Extract array from data
                JSONObject dailyData = getter.getJSONObject(Keys.daily_data);
                JSONArray array = dailyData.getJSONArray(Keys.data);

                //If there is already an adapter, update the data, otherwise add one
                if (pager.getAdapter() != null) {
                    DailyDetailPagerAdapter adapter = (DailyDetailPagerAdapter) pager.getAdapter();
                    adapter.data = array;

                    adapter.notifyDataSetChanged();
                } else {
                    pager.setAdapter(new DailyDetailPagerAdapter(array, this, formatter, manager,getter.getTimezone()));
                }

                //Set header description text
                AnimationManager.loadText(headerDesc,formatter.formatText(dailyData.getString(Keys.description)));

                //Set location to toolbar
                setToolbarText(data.getJSONObject(Keys.meta_data).getString(Keys.location));

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            manager.getData(true,manager.lastLocationRequested);
        }

    }

    private void setPosition(){
        //Set pager position from intent extra
        int position = getIntent().getExtras().getInt(DataDisplayFragment.POS_KEY,0);
        if(position < pager.getAdapter().getCount() &&  !positionHasBeenSet){
            //if The position has already been set once use smooth scroll
            pager.setCurrentItem(position,false);
            tabs.getTabAt(position).select();
            positionHasBeenSet = true;
        }
    }

    private void setToolbarText(String location){
        String toolbarText = getString(R.string.weekly_toolbar_text_prefix) + formatter.formatText(location);
        AnimationManager.loadText(toolbarTextView,toolbarText);
    }

    @Override
    public void supportFinishAfterTransition() {
        AnimationManager.fade(tabs,View.INVISIBLE);
        super.supportFinishAfterTransition();
    }

    @Override
    public void onTransitionCancel(Transition transition) {
        //Fade in tabs to prevent blinking
        AnimationManager.fade(tabs,View.VISIBLE);
    }

    @Override
    public void onTransitionEnd(Transition transition) {
        //Fade in tabs to prevent blinking
        AnimationManager.fade(tabs,View.VISIBLE);
    }

    //Unused implementations
    @Override
    public void onTransitionPause(Transition transition) {
    }
    @Override
    public void onTransitionResume(Transition transition) {
    }
    @Override
    public void onTransitionStart(Transition transition) {
    }
}

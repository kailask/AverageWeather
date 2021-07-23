package xyz.eleventhour.averageweather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.TimeZone;
import java.util.concurrent.TimeoutException;

/**
 * Created by Kailas on 5/15/2016.
 *
 * Fragment class used for daily and hourly preview fragments
 */
public class WeatherPreviewCardFragment extends Fragment implements PreviewItemClickListener{

    //Keys used in arguments bundle
    public static final String DESC_KEY = "DESCRIPTION", DATA_KEY = "DATA", TYPE_KEY = "TYPE", HOURLY = "HOURLY_TYPE", DAILY = "DAILY_TYPE", ANIMATION_KEY="PLAY_ANIMATION", TIMEZONE_KEY = "TIMEZONE_KEY";

    private TextView descView;
    private RecyclerView dataList;

    private JSONArray data;

    private TimeZone timezone;

    //Formatter from activity for displaying data
    private DataFormatter dataFormatter;
    //Used to get icons and URLs as necessary
    private NetworkManager networkManager;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //layout to return
        View layout = inflater.inflate(R.layout.preview_card_fragment, container, false);
        findViews(layout);

        //Activity must be DataHandlerActivity
        if((NetworkManager.DataHandlerActivity) getActivity() != null){
            dataFormatter = ((NetworkManager.DataHandlerActivity) getActivity()).getFormatter();
            networkManager = ((NetworkManager.DataHandlerActivity) getActivity()).getNetworkManager();

            //isRefresh is false
            setData(getArguments(),false);

        } else {
            //Return error layout (Code 13)
            setErrorData();
        }

        return layout;
    }

    private void setErrorData(){
        AnimationManager.loadText(descView,"Error \uD83D\uDE1E");
    }

    //Get references to views
    private void findViews(View layout){
        descView = (TextView) layout.findViewById(R.id.preview_card_description);
        dataList = (RecyclerView) layout.findViewById(R.id.preview_card_list_holder);
    }

    //initialize view values from args
    public void updateViews(Bundle newData){
        setData(newData, true);
    }

    //Set data to views
    private void setData(Bundle arguments, boolean isRefresh) {

        if (arguments != null) {

            //Get values from args
            String description = arguments.getString(DESC_KEY, "Undefined");
            String dataString = arguments.getString(DATA_KEY, null);
            final String type = arguments.getString(TYPE_KEY, null);
            boolean playAnimation = arguments.getBoolean(ANIMATION_KEY,true);

            String timezoneString = arguments.getString(TIMEZONE_KEY,null);
            if(timezoneString != null){
                timezone = TimeZone.getTimeZone(timezoneString);
            } else {
                timezone = TimeZone.getDefault();
            }

            AnimationManager.loadText(descView, dataFormatter.formatText(description));

            //Get RecyclerView data
            if (dataString != null) {
                try {
                    data = new JSONArray(dataString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    data = new JSONArray();
                    setErrorData();
                }

                //set up recycler view
                if (type != null) {
                    dataList.setVisibility(View.VISIBLE);

                    //Setup animators for listview
                    final Animator enterAnimator = AnimatorInflater.loadAnimator(getActivity(),R.animator.slide_list_in);
                    enterAnimator.setTarget(dataList);

                    //Animate view sliding in if necessary
                    if(isRefresh && playAnimation){
                        AnimationManager.zoomOut(dataList, getActivity(), new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {}
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setAdapter(type);
                                enterAnimator.start();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                setAdapter(type);
                                enterAnimator.start();
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {}
                        });
                    } else {
                        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

                        dataList.setHasFixedSize(true);
                        dataList.setLayoutManager(layoutManager);
                        setAdapter(type);

                        if(playAnimation){
                            enterAnimator.start();
                        }
                    }

                } else {
                    dataList.setVisibility(View.INVISIBLE);
                    setErrorData();
                    //TODO: ask to report code 10
                }
            }
        } else {
            setErrorData();
        }
    }

    private void setAdapter(String type){
        //set recycler adapter based on type of data which is determined by string passed in arguments
        switch (type) {
            case DAILY:
                dataList.setAdapter(new DailyPreviewDataAdapter(data, dataFormatter, networkManager,this,timezone));
                break;
            case HOURLY:
                dataList.setAdapter(new HourlyPreviewDataAdapter(data, dataFormatter, networkManager,this,timezone));
        }
    }

    @Override
    public void onClick(int position, View icon) {
        ((DataDisplayFragment) getParentFragment()).registerClick(DataDisplayFragment.CLICK_TYPE.DAILY_CARD, position);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Set list pos back to 0 in case animation was interrupted
        dataList.setX(0);
    }
}

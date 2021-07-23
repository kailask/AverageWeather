package xyz.eleventhour.averageweather;

import android.support.v4.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.sql.Time;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Kailas on 5/12/2016.
 *
 * Fragment class for the current weather card in overview
 */
public class CurrentWeatherCardFragment extends Fragment implements View.OnClickListener{


    //Keys used to pass data in bundle
    public static final String ICON_KEY = "ICON", TEMP_KEY = "TEMP", DESC_KEY = "DESCRIPTION",TIMEZONE_KEY="TIMEZONE_KEY";

    //Views
    private NetworkImageView iconView;
    private TextView tempView, descView;
    private View background;

    //Formatter from activity for displaying data
    private DataFormatter dataFormatter;
    //Used to get icons and URLs as necessary
    private NetworkManager networkManager;

    //Used to register clicks
    private DataDisplayFragment parent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.currently_card_fragment, container, false);

        findViews(layout);

        //Activity must be DataHandlerActivity, parent must be DataDisplayFragment
        if(getParentFragment() instanceof DataDisplayFragment){
            dataFormatter = ((NetworkManager.DataHandlerActivity) getActivity()).getFormatter();
            networkManager = ((NetworkManager.DataHandlerActivity) getActivity()).getNetworkManager();
            setData(getArguments());

            parent = (DataDisplayFragment) getParentFragment();
        } else {
            setErrorValues();
        }

        //On click start new activity
        layout.setOnClickListener(this);

        return layout;
    }

    //Called by parent to refresh data in fragment
    public void updateViews(Bundle args){
        setData(args);
    }

    //Find views in layout
    private void findViews(View layout){
        tempView = (TextView) layout.findViewById(R.id.currently_temperature);
        descView = (TextView) layout.findViewById(R.id.currently_card_description);
        iconView = (NetworkImageView) layout.findViewById(R.id.currently_card_icon);
        background = layout.findViewById(R.id.currently_card_background);
    }

    //Set data from arguments
    private void setData(Bundle arguments){

        if(arguments != null) {

            //Get values from bundle
            String description = arguments.getString(DESC_KEY);
            String temperature = arguments.getString(TEMP_KEY);
            String iconID = arguments.getString(ICON_KEY);
            String timezoneID = arguments.getString(TIMEZONE_KEY);

            TimeZone timeZone;

            if(timezoneID != null){
                timeZone = TimeZone.getTimeZone(timezoneID);
            } else {
                timeZone = TimeZone.getDefault();
            }

            //Format data if it exists
            temperature = (temperature != null)?dataFormatter.formatTemp(Integer.parseInt(temperature)):"--";
            description = (description != null)?dataFormatter.formatTextCaps(description):"Undefined";

            //Set icon default and error pngs
            iconView.setErrorImageResId(R.drawable.ic_unknown_value_large);
            iconView.setDefaultImageResId(R.drawable.ic_weather_large_default);

            //Set icon
            if (iconID != null) {
                //Get calendar to determine the time current time and get appropriate icon
                Calendar calendar = Calendar.getInstance(timeZone);
                String iconURL = dataFormatter.getIconURL(DataFormatter.ICON_LARGE, iconID, calendar.get(Calendar.HOUR_OF_DAY));
                iconView.setImageUrl(iconURL, networkManager.getImageLoader());
            }

            //Fade text in
            AnimationManager.loadText(descView, description);
            AnimationManager.loadText(tempView, temperature);

        } else {
            setErrorValues();
        }
    }

    //Set error values
    private void setErrorValues(){
        AnimationManager.loadText(descView,"Error \uD83D\uDE1E");
    }

    //Tell parent fragment about click
    @Override
    public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            parent.registerClick(DataDisplayFragment.CLICK_TYPE.CURRENT_CARD, DataDisplayFragment.POS_NOT_AVAILABLE, new Pair<View, String>(iconView, getString(R.string.current_icon_shared_transition_name)), new Pair<>(background, getString(R.string.current_background_transition_name)));
        } else {
            parent.registerClick(DataDisplayFragment.CLICK_TYPE.CURRENT_CARD, DataDisplayFragment.POS_NOT_AVAILABLE);
        }
    }
}

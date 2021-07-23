package xyz.eleventhour.averageweather;

import android.support.v4.util.Pair;
import android.view.View;

/**
 * Created by Kailas on 5/22/2016.
 *
 * Implemented by fragments to communicate with DataHandlerActivity
 */
public interface DataDisplayFragment extends MainActivity.NavIDableFragment {
     int POS_NOT_AVAILABLE = -11;
    String POS_KEY = "SOMETHING_ETC";

    //Called when a refresh is available
    void updateCallback(UPDATE_TYPE type);

    //Called on fragments to preform any animations prior to receiving new location data
    void prepareForLocationChange();

    void registerClick(CLICK_TYPE type, int position, Pair<View, String>... sharedViews);

    enum CLICK_TYPE {CURRENT_CARD,HOURLY_CARD,DAILY_CARD}
    //Different animation including loading animation are used based on type of data update
    enum UPDATE_TYPE{SIMPLE_UPDATE, COMPLETE_REFRESH}

}

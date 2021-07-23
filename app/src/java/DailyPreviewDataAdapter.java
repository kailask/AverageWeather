package xyz.eleventhour.averageweather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

/**
 * Created by Kailas on 5/15/2016.
 *
 * Adapter for DailyData extends PreviewDataAdapter
 */
public class DailyPreviewDataAdapter extends PreviewDataAdapter {

    public DailyPreviewDataAdapter(JSONArray data, DataFormatter formatter, NetworkManager networkManager, PreviewItemClickListener listener, TimeZone timeZone) {
        super(data,formatter, networkManager,listener, timeZone);
    }

    @Override
    public DailyPreviewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_daily_weather_preview_card,parent,false);
        return new DailyPreviewHolder(layout);
    }

    @Override
    public void onBindViewHolder(final PreviewDataHolder holder, int position) {
        //cast from polymorphic type to HourlyPreviewHolder to set specific values
        final DailyPreviewHolder dailyPreviewHolder = (DailyPreviewHolder) holder;

        String iconID = null;

        try {
            JSONObject currentObj = data.getJSONObject(position);
            DataFormatter.JSONDataGetter dataGetter = new DataFormatter.JSONDataGetter(currentObj);

            iconID = currentObj.getString(Keys.iconID);

            //Data is formatted by formatter before being displayed
            dailyPreviewHolder.tempHi.setText(formatter.formatTemp(dataGetter.getInt(Keys.max_temp)));
            dailyPreviewHolder.tempLow.setText(formatter.formatTemp(dataGetter.getInt(Keys.min_temp)));
            dailyPreviewHolder.time.setText(formatter.formatTimeIntoDays(dataGetter.getLong(Keys.time),timeZone));

            super.onBindViewHolder(holder,dataGetter);
        } catch (JSONException e) {
            e.printStackTrace();
            displayError(holder);
            //TODO: handle this somehow
        }

        //Set icon
        if(iconID != null){
            //Constant 12 is passed in because all daily previews should show a day icon
            String iconURL = formatter.getIconURL(formatter.ICON_SMALL,iconID,12);
            holder.icon.setImageUrl(iconURL,networkManager.getImageLoader());
        }

        //Set listener
        dailyPreviewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(dailyPreviewHolder.getAdapterPosition(),holder.icon);
            }
        });

    }

    public class DailyPreviewHolder extends PreviewDataHolder{
        public TextView tempHi;
        public TextView tempLow;
        public View layout;

        public DailyPreviewHolder(View layout){
            super(layout);

            this.layout = layout;
            tempHi = (TextView) layout.findViewById(R.id.daily_preview_card_temp_hi);
            tempLow = (TextView) layout.findViewById(R.id.daily_preview_card_temp_low);
            time = (TextView) layout.findViewById(R.id.daily_preview_time_short);
            percip = (TextView) layout.findViewById(R.id.daily_preview_card_percip);
            percipProb = (TextView) layout.findViewById(R.id.daily_preview_card_percipProb);
            icon = (NetworkImageView) layout.findViewById(R.id.daily_preview_icon);

            //Set icon default and error icon
            icon.setDefaultImageResId(R.drawable.ic_weather_large_default);
            icon.setErrorImageResId(R.drawable.ic_unknown_value_large);
        }
    }
}

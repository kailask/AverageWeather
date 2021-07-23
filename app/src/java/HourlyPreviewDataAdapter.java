package xyz.eleventhour.averageweather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Kailas on 5/15/2016.
 *
 * Adapter for HourlyData extends PreviewDataAdapter
 */
public class HourlyPreviewDataAdapter extends PreviewDataAdapter {

    @Override
    public HourlyPreviewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_hourly_weather_preview_card,parent,false);
        return new HourlyPreviewHolder(holder);
    }

    @Override
    public void onBindViewHolder(PreviewDataHolder holder, int position) {
        //cast from polymorphic type to HourlyPreviewHolder to set specific values
        HourlyPreviewHolder hourlyPreviewHolder = (HourlyPreviewHolder) holder;

        long time = 0;
        String iconID = null;

        try {
            JSONObject currentObj = data.getJSONObject(position);
            if(currentObj != null) {
                DataFormatter.JSONDataGetter getter = new DataFormatter.JSONDataGetter(currentObj);

                //Data is formatted by formatter before being displayed
                hourlyPreviewHolder.temp.setText(formatter.formatTemp(getter.getInt(Keys.temperature)));
                hourlyPreviewHolder.wind.setText(formatter.formatWind(getter.getInt(Keys.wind_speed)));
                hourlyPreviewHolder.time.setText(formatter.formatTimeIntoHours(getter.getLong(Keys.time),timeZone));
                hourlyPreviewHolder.description.setText(formatter.formatText(getter.getString(Keys.description)));

                super.onBindViewHolder(holder, getter);

                //If percipProb is 0 dont show it
                if (getter.getDouble(Keys.percip_prob) == null || getter.getDouble(Keys.percip_prob) <= 0) {
                    hourlyPreviewHolder.percipProb.setVisibility(View.INVISIBLE);
                } else {
                    hourlyPreviewHolder.percipProb.setVisibility(View.VISIBLE);
                }

                //Set values for getting icon
                time = getter.getLong(Keys.time);
                iconID = getter.getString(Keys.iconID);

            }

        } catch (JSONException|NullPointerException e) {
            e.printStackTrace();
            //TODO: handle this somehow
            displayError(hourlyPreviewHolder);
        }

        //Set icon
        if(iconID != null && time != 0){
            //Get calendar to determine the time the icon is supposed to represent
            Calendar calendar = Calendar.getInstance(timeZone);
            //Convert to milliseconds
            calendar.setTime(new Date(time*1000));
            String iconURL = formatter.getIconURL(formatter.ICON_SMALL,iconID,calendar.get(Calendar.HOUR_OF_DAY));
            holder.icon.setImageUrl(iconURL,networkManager.getImageLoader());
        }
    }

    public void displayError(HourlyPreviewHolder holder) {
        super.displayError(holder);
        holder.description.setText(R.string.code_12);
    }

    public HourlyPreviewDataAdapter(JSONArray data, DataFormatter formatter, NetworkManager networkManager, PreviewItemClickListener listener, TimeZone timeZone) {
        super(data,formatter, networkManager,listener, timeZone);
    }

    public class HourlyPreviewHolder extends PreviewDataHolder{

        public TextView description;
        public TextView wind;
        public TextView temp;

        public HourlyPreviewHolder(View layout){
            super(layout);
            description = (TextView) layout.findViewById(R.id.hourly_preview_card_description);
            temp = (TextView) layout.findViewById(R.id.hourly_preview_card_temp);
            wind = (TextView) layout.findViewById(R.id.hourly_preview_card_wind);
            time = (TextView) layout.findViewById(R.id.hourly_preview_time_short);
            percip = (TextView) layout.findViewById(R.id.hourly_preview_card_percip);
            percipProb = (TextView) layout.findViewById(R.id.hourly_preview_card_percipProb);
            icon = (NetworkImageView) layout.findViewById(R.id.hourly_preview_icon);

            //Set icon default and error icon
            icon.setDefaultImageResId(R.drawable.ic_weather_large_default);
            icon.setErrorImageResId(R.drawable.ic_unknown_value_large);
        }
    }
}

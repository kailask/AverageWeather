package xyz.eleventhour.averageweather;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Stack;
import java.util.TimeZone;

/**
 * Created by Kailas on 6/16/2016.
 */
public class DailyDetailPagerAdapter extends PagerAdapter {

    protected JSONArray data;
    private Context context;
    private DataFormatter formatter;
    private NetworkManager networkManager;

    private TimeZone timeZone;

    //Stack of views to provide recycling
    private Stack<DayPageHolder> recycledViews = new Stack<>();

    public DailyDetailPagerAdapter(JSONArray data, Context context, DataFormatter formatter, NetworkManager networkManager, TimeZone timeZone) {
        super();
        this.data = data;
        this.context = context;
        this.formatter = formatter;
        this.networkManager = networkManager;
        this.timeZone = timeZone;
    }


    @Override
    public int getCount() {
        return data.length();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //Get new page
        DayPageHolder holder = getNextPage(container);
        String iconID = null;

        //Reset scroll
        holder.scrollView.scrollTo(0,0);

        try {
            JSONObject currentObj = data.getJSONObject(position);

            DataFormatter.JSONDataGetter dataGetter = new DataFormatter.JSONDataGetter(currentObj);

            iconID = dataGetter.getString(Keys.iconID);

            //Set new values to holder views
            AnimationManager.loadText(holder.desc,formatter.formatText(dataGetter.getString(Keys.description)));
            AnimationManager.loadText(holder.hi,formatter.formatTemp(dataGetter.getInt(Keys.max_temp)));
            AnimationManager.loadText(holder.hiTime,formatter.formatAtTime(dataGetter.getLong(Keys.tempMaxTime),timeZone));
            AnimationManager.loadText(holder.lo,formatter.formatTemp(dataGetter.getInt(Keys.min_temp)));
            AnimationManager.loadText(holder.loTime,formatter.formatAtTime(dataGetter.getLong(Keys.tempMinTime),timeZone));
            AnimationManager.loadText(holder.percip,formatter.formatPercip((dataGetter.getDouble(Keys.percip))));
            AnimationManager.loadText(holder.percipProb,formatter.formatPercipProb(dataGetter.getDouble(Keys.percip_prob)));
            AnimationManager.loadText(holder.windSpeed,formatter.formatWind(dataGetter.getInt(Keys.wind_speed)));
            AnimationManager.loadText(holder.windBaring,formatter.formatWindBaring(dataGetter.getInt(Keys.wind_baring)));
            AnimationManager.loadText(holder.cloudCover,formatter.formatClouds(dataGetter.getDouble(Keys.cloud_cover)));
            AnimationManager.loadText(holder.visibility,formatter.formatVisibility(dataGetter.getInt(Keys.visibility)));
            AnimationManager.loadText(holder.humidity,formatter.formatHumidity(dataGetter.getDouble(Keys.humidity)));
            AnimationManager.loadText(holder.pressure,formatter.formatPressure(dataGetter.getInt(Keys.pressure)));

            AnimationManager.loadText(holder.sunrise,formatter.formatSuntime(dataGetter.getLong(Keys.sunrise),timeZone));
            AnimationManager.loadText(holder.sunset,formatter.formatSuntime(dataGetter.getLong(Keys.sunset),timeZone));


        } catch (JSONException e) {
            e.printStackTrace();
            AnimationManager.loadText(holder.desc, context.getString(R.string.error));
        }

        //Set icon
        if(iconID != null){
            //Constant 12 is passed in because all daily previews should show a day icon
            String iconURL = formatter.getIconURL(formatter.ICON_SMALL,iconID,12);
            holder.icon.setImageUrl(iconURL,networkManager.getImageLoader());
        }

        container.addView(holder.layout);
        return holder.layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        //Add view to recycled stack to be reuesd
        recycledViews.push((DayPageHolder)((View) view).getTag());
        collection.removeView((View) view);
    }

    private DayPageHolder getNextPage(ViewGroup parent){
        if(recycledViews.isEmpty()){
            //Created new view
            return new DayPageHolder(LayoutInflater.from(context).inflate(R.layout.daily_detail_page_layout,parent,false));
        } else {
            //If recycled views are available return one
            return recycledViews.pop();
        }
    }

    private class DayPageHolder{
        //Holder with references to layout and views
        public View layout,desc,hi,hiTime,lo,loTime,percip,percipProb,windSpeed,windBaring,
        cloudCover,visibility, humidity,pressure,sunrise,sunset;
        public NetworkImageView icon;
        public NestedScrollView scrollView;

        public DayPageHolder(View layout){
            this.layout = layout;
            layout.setTag(this);

            //Get references to necessary views
            desc=layout.findViewById(R.id.daily_detail_temp_card_desc_title);
            hi=layout.findViewById(R.id.detail_temp_card_hi_text);
            hiTime=layout.findViewById(R.id.detail_temp_card_hi_time_text);
            lo=layout.findViewById(R.id.detail_temp_card_lo_text);
            loTime=layout.findViewById(R.id.detail_temp_card_lo_time_text);
            percip=layout.findViewById(R.id.detail_card_percip_text);
            percipProb=layout.findViewById(R.id.detail_card_percipProb_text);
            windSpeed=layout.findViewById(R.id.detail_card_wind_text);
            windBaring=layout.findViewById(R.id.detail_card_wind_baring_text);
            cloudCover=layout.findViewById(R.id.detail_card_cloud_cover_text);
            visibility=layout.findViewById(R.id.detail_card_visibility_text);
            pressure=layout.findViewById(R.id.detail_pressure_value);
            sunrise=layout.findViewById(R.id.detail_sunrise_value);
            sunset=layout.findViewById(R.id.detail_sunset_value);
            humidity =layout.findViewById(R.id.detail_humidity_value);
            icon = (NetworkImageView) layout.findViewById(R.id.daily_detail_temp_icon);
            scrollView = (NestedScrollView) layout.findViewById(R.id.daily_detail_scroll_view);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        try {
            Long time = data.getJSONObject(position).getLong(Keys.time);
            return formatter.formatTimeIntoDays(time,timeZone);

        } catch (JSONException e) {
            e.printStackTrace();
            return "!!!";
        }

    }
}

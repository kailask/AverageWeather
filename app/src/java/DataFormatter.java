package xyz.eleventhour.averageweather;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.Display;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Kailas on 5/16/2016.
 *
 * Singleton class manages all formatting of data before it is displayed and URLs
 */
public class DataFormatter {

    //Different types of units with multipliers from default values and suffixes for display
    private enum UNIT{
        DEGREE_F("°"),MILE(" mi"),MPH(" mph"),INCHES("in"),DIRECTION_DEGREE("°"),PERCENT("%",100),MILLIBARS(" mBAR"),
        DEGREE_C("°",.55556f,-32), KILOMETER(" km",1.60934f),KPH(" kph",1.60934f),KNOT(" kn",0.868976f), MILLIMETER("mm",25.4f);

        public String suffix;
        public float multiplier;
        public float difference;

        UNIT(String suffix){
            this.multiplier = 1;
            this.suffix = suffix;
            this.difference = 0;
        }

        UNIT(String suffix, float multiplier, float difference){
            this.multiplier = multiplier;
            this.suffix = suffix;
            this.difference = difference;
        }

        UNIT(String suffix, float multiplier){
            this.multiplier = multiplier;
            this.suffix = suffix;
            this.difference = 0;
        }
    }

    //Codes for icon sizes
    public static final int ICON_SMALL = 1;
    public static final int ICON_LARGE = 2;

    private static final String iconURL = "http://[domain.com]/res/weather/icon";

    private UNIT PERCIP_FORMAT,PERCIPPROB_FORMAT,TEMP_FORMAT,WIND_FORMAT,
            CLOUDCOVER_FORMAT, WINDBARING_FORMAT, NEARESTSTORM_FORMAT,
            HUMIDITY_FORMAT,PRESSURE_FORMAT,VISIBILITY_FORMAT;
    //Formatters for UNIX timestamps
    private SimpleDateFormat shortTime = new SimpleDateFormat("h a");
    private SimpleDateFormat longTime = new SimpleDateFormat("c', 'h a");
    private SimpleDateFormat dailyTime = new SimpleDateFormat("E");
    private SimpleDateFormat mediumTime = new SimpleDateFormat("h':'mm a");

    private DecimalFormat numFormatterLong = new DecimalFormat("#.###");

    //singleton instance
    private static DataFormatter instance;

    public static DataFormatter getFormatter(StorageManager storageManager, Context context){
        if(instance == null){
            instance = new DataFormatter(storageManager,context);
        } else {
            instance.setUnits(storageManager,context);
        }

        return instance;
    }

    //private constructor
    private DataFormatter(StorageManager storageManager, Context context){
        setUnits(storageManager, context);
    }

    private void setUnits(StorageManager storageManager, Context context){
        //TODO get values from Prefs (storageManager) and set based on settings values
        PERCIPPROB_FORMAT = HUMIDITY_FORMAT = CLOUDCOVER_FORMAT = UNIT.PERCENT;
        WINDBARING_FORMAT = UNIT.DIRECTION_DEGREE;
        PRESSURE_FORMAT = UNIT.MILLIBARS;

        Resources resources = context.getResources();

        //Set temperature unit
        String IMPERIAL = resources.getString(R.string.pref_default_temp_unit);

        if(storageManager.preferences.getString(resources.getString(R.string.pref_temp_unit_key),IMPERIAL).equals(IMPERIAL)){
            TEMP_FORMAT = UNIT.DEGREE_F;
        } else {
            TEMP_FORMAT = UNIT.DEGREE_C;
        }

        //Set speed unit
        IMPERIAL = resources.getString(R.string.pref_default_speed_unit);
        String METRIC = resources.getString(R.string.pref_other_speed_unit);

        String speedUnit = storageManager.preferences.getString(resources.getString(R.string.pref_speed_unit_key),IMPERIAL);

        if(speedUnit.equals(IMPERIAL)){
            WIND_FORMAT = UNIT.MPH;
        } else if(speedUnit.equals(METRIC)){
            WIND_FORMAT = UNIT.KPH;
        } else {
            WIND_FORMAT = UNIT.KNOT;
        }

        //Set distance unit
        IMPERIAL = resources.getString(R.string.pref_default_distance_unit);

        if(storageManager.preferences.getString(resources.getString(R.string.pref_distance_unit_key),IMPERIAL).equals(IMPERIAL)){
            VISIBILITY_FORMAT = NEARESTSTORM_FORMAT = UNIT.MILE;
        } else {
            VISIBILITY_FORMAT = NEARESTSTORM_FORMAT = UNIT.KILOMETER;
        }

        //Set rainfall unit
        IMPERIAL = resources.getString(R.string.pref_default_rain_unit);

        if(storageManager.preferences.getString(resources.getString(R.string.pref_rain_unit_key),IMPERIAL).equals(IMPERIAL)){
            PERCIP_FORMAT = UNIT.INCHES;
        } else {
            PERCIP_FORMAT = UNIT.MILLIMETER;
        }
    }

    public String formatPercipProb(Double percipProb){
        return formatDouble(percipProb,PERCIPPROB_FORMAT);
    }

    public String formatClouds(Double cloudCover){
        return formatDouble(cloudCover,CLOUDCOVER_FORMAT);
    }

    public String formatWind(Integer windSpeed){
        return formatInteger(windSpeed,WIND_FORMAT);
    }

    public String formatWindBaring(Integer windBaring){
        return formatInteger(windBaring,WINDBARING_FORMAT);
    }

    public String formatNearestStorm(Integer nearestStorm){
        return formatInteger(nearestStorm,NEARESTSTORM_FORMAT);
    }

    public String formatHumidity(Double humidity){
        return formatDouble(humidity,HUMIDITY_FORMAT);
    }

    public String formatPressure(Integer pressure){
        return formatInteger(pressure,PRESSURE_FORMAT);
    }

    //Generic method for formatting integers
    public String formatInteger(Integer value, UNIT type){
        if(value != null){
            return String.valueOf((int)(value * type.multiplier))+type.suffix;
        } else {
            return "--"+type.suffix;
        }
    }

    //Generic method for formatting doubles
    public String formatDouble(Double value, UNIT type){
        if(value != null){
            return String.valueOf((int)(value * type.multiplier))+type.suffix;
        } else {
            return "--"+type.suffix;
        }
    }

    public String formatPercip(Double percip){
        if(percip != null) {
            percip *= PERCIP_FORMAT.multiplier;
            return String.valueOf(numFormatterLong.format(percip)) + PERCIP_FORMAT.suffix;
        } else {
            return "---.-"+PERCIP_FORMAT.suffix;
        }
    }

    public String formatTemp(Integer temp){
        if(temp != null) {
            temp = (int)((temp+ TEMP_FORMAT.difference) * TEMP_FORMAT.multiplier);
            return String.valueOf(temp) + TEMP_FORMAT.suffix;
        } else {
            return "--"+TEMP_FORMAT.suffix;
        }
    }

    //Format UNIX time into different formats
    public String formatTimeIntoHours(Long UNIXTime,TimeZone timeZone){
        if(UNIXTime == null){return "Undefined";}
        UNIXTime *= 1000;

        Calendar calendar = Calendar.getInstance();

        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.setTime(new Date(UNIXTime));

        //If the time is a day ahead display which day it is as well
        if(calendar.get(Calendar.DAY_OF_MONTH) != currentDay){
            setFormatterTimezone(longTime,timeZone);
            return String.valueOf(longTime.format(UNIXTime));
        }

        setFormatterTimezone(shortTime,timeZone);
        return String.valueOf(shortTime.format(UNIXTime));
    }

    public String formatTimeIntoDays(Long UNIXTime,TimeZone timeZone){
        if(UNIXTime == null){return "Undefined";}
        UNIXTime *= 1000;
        setFormatterTimezone(dailyTime,timeZone);
        return dailyTime.format(UNIXTime);
    }

    //Format text to prevent lower case first letters
    public String formatText(String desc){
        if(desc == null || desc.length() < 1){
            return "Undefined";
        }
        //Capitalize first letter
        return desc.substring(0,1).toUpperCase() + desc.substring(1);
    }

    //Format text in upper case
    public String formatTextCaps(String desc){
        if(desc == null){
            return "Undefined";
        }

        return desc.toUpperCase();
    }

    public String formatUpdatedSince(Long time){
        if(time == null){return "Undefined";}
        //Timezone should be the current timezone
        mediumTime.setTimeZone(TimeZone.getDefault());
        return "Updated " + mediumTime.format(time);
    }

    public String formatSuntime(Long suntime,TimeZone timeZone){
        if(suntime == null){return "Undefined";}
        suntime *= 1000;
        setFormatterTimezone(mediumTime,timeZone);
        return mediumTime.format(suntime);
    }

    //Get url for getting icon from network
    public String getIconURL(int iconSize, String iconID, int hourInDay){
        String URL = iconURL;
        //If hourInDay is during night_background_default get night_background_default icon
        URL += (hourInDay < 7 || hourInDay >= 19)?"/night":"/day";

        //Append size to url based on iconSize passed in. Small is default

        //!!!!!*********TO ENABLE SMALL ICONS REPLACE THE SECOND "/large/" VALUE!!!!!*********
        URL += (iconSize == ICON_LARGE)?"/large/":"/large/";
        //Add icon id
        URL += iconID;
        URL += ".png";
        return URL;
    }

    public String formatVisibility(Integer visibility){
        //If visibility is greater than 10 return >10
        if (visibility == null || visibility >= 10) {
            return ">" + (int) (10 * VISIBILITY_FORMAT.multiplier) + VISIBILITY_FORMAT.suffix;
        }
        int visibilityNum = (int) (visibility * VISIBILITY_FORMAT.multiplier);
        return visibilityNum + VISIBILITY_FORMAT.suffix;
    }

    public String formatAtTime(Long UNIXTime,TimeZone timeZone){
        if(UNIXTime == null){return "Undefined";}
        UNIXTime *= 1000;

        setFormatterTimezone(mediumTime,timeZone);
        return " at " + mediumTime.format(UNIXTime);
    }

    //Utility method to set the timezone to a formatter
    private void setFormatterTimezone(SimpleDateFormat formatter,TimeZone timeZone){
        if(timeZone != null){
            formatter.setTimeZone(timeZone);
        } else {
            formatter.setTimeZone(TimeZone.getDefault());
        }
    }

    //Object is created as needed to help with extracting data from JSONObject; handles method call in its own individual try catch to prevent on catch ending those after it
    public static class JSONDataGetter{
        private JSONObject currentData;

        public JSONDataGetter(JSONObject data){
            currentData = data;
        }

        public Integer getInt(String key){
            try{
                return currentData.getInt(key);
            } catch (JSONException e){
                return null;
            }
        }

        public String getString(String key){
            try {
                return currentData.getString(key);
            } catch (JSONException e){
                return null;
            }
        }

        public Double getDouble(String key){
            try{
                return currentData.getDouble(key);
            } catch (JSONException e){
                return null;
            }
        }

        public Long getLong(String key){
            try{
                return currentData.getLong(key);
            } catch (JSONException e){
                return null;
            }
        }

        public JSONObject getJSONObject(String key){
            try {
                return  currentData.getJSONObject(key);
            } catch (JSONException e){
                return null;
            }
        }

        public boolean getBool(String key){
            try {
                return currentData.getBoolean(key);
            } catch (JSONException e){
                return false;
            }
        }

        public void setData(String key){
            JSONObject newData = getJSONObject(key);
            if(newData != null){
                currentData = newData;
            }
        }

        public TimeZone getTimezone(){
            JSONObject meta = getJSONObject(Keys.meta_data);
            if(meta != null){
                try {
                    return TimeZone.getTimeZone(meta.getString(Keys.timezone));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }
    }
}

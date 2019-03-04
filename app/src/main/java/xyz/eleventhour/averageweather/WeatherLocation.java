package xyz.eleventhour.averageweather;
import android.location.Location;

import java.io.Serializable;

//Class to store one weather location, used as a lighter alternative to android.location.Location
public class WeatherLocation implements Serializable{
    public SimpleLocation location;
    public String locationText;

    public WeatherLocation(String name, float lat, float lon){
        location = new SimpleLocation(lat,lon);
        locationText = name;
    }

    public WeatherLocation(String name, SimpleLocation location){
        locationText = name;
        this.location = location;
    }

    @Override
    public String toString() {
        return locationText;
    }


    //Inner class that stores lat and lon, may be null
    public static class SimpleLocation implements Serializable{
        public double lat, lon;

        public SimpleLocation(float lat,float lon){
            this.lat = lat;
            this.lon = lon;
        }

        public SimpleLocation(Location location){
            lat = location.getLatitude();
            lon = location.getLongitude();
        }

        @Override
        public String toString() {
            return "("+lat+","+lon+")";
        }
    }
}
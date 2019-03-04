package xyz.eleventhour.averageweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Kailas on 5/16/2016.
 *
 * Singleton class manages all calls to SharedPrefs and caching/retrieving of data
 */
public class StorageManager {
    //Name of current location, needed as constant to uniquely identify location
    public static final String MY_LOCATION_NAME = "My Location";

    //Names for different stored files
    public static final String LOCATIONS_FILE_NAME = "avg_weather_location_list.dat";

    public SharedPreferences preferences;

    private static StorageManager singleton;
    //List of current user locations
    private ArrayList<WeatherLocation> locations;

    private StorageManager(Context context){
        locations = new ArrayList<>();

        //Get shared prefs
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        populateLocationList(context);
    }

    public static StorageManager getSingleton(Context context){
        if(singleton == null){
            singleton = new StorageManager(context);
        }
        return singleton;
    }

    protected ArrayList<WeatherLocation> getLocations(){
        return locations;
    }

    //Get available locations from file
    private void populateLocationList(Context ctx){
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = ctx.openFileInput(LOCATIONS_FILE_NAME);
            ois = new ObjectInputStream(fis);

            int arraySize = ois.readInt();

            for (int i = 0; i < arraySize; i++) {
                locations.add((WeatherLocation)ois.readObject());
            }

        } catch (IOException | ClassNotFoundException e){

        } finally {
            try {
                if (fis != null) fis.close();
                if (ois != null) ois.close();
            } catch (IOException e){}
        }

        if(locations.size() <= 0){
            locations.add(new WeatherLocation(MY_LOCATION_NAME,null));
        }

    }

    //Write location list to file
    private void saveLocationList(Context context){
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = context.openFileOutput(LOCATIONS_FILE_NAME,Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);

            oos.writeInt(locations.size());

            for (WeatherLocation object :
                    locations) {
                oos.writeObject(object);
            }

        } catch (IOException e){

        } finally {
            try {
                if (fos != null) fos.close();
                if (oos != null) oos.close();
            } catch (IOException e){}
        }
    }

    private void saveState(Context context){
        saveLocationList(context);
    }


    //Called by NetworkManager when the activity OnStops
    public static void prepareForExtermination(Context context){
        if(singleton != null){
            singleton.saveState(context);
        }
    }
}

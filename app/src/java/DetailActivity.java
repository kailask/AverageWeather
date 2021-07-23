package xyz.eleventhour.averageweather;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by Kailas on 6/9/2016.
 *
 * Parent class of all detail activities
 */
public abstract class DetailActivity extends NetworkManager.DataDisplayActivity {

    protected DataFormatter formatter;
    protected NetworkManager manager;

    protected long updated = Calendar.getInstance().getTimeInMillis();

    public boolean isOver21(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        formatter = DataFormatter.getFormatter(StorageManager.getSingleton(this),this);
        manager = NetworkManager.getManager(this);

        //Set data
        JSONObject data = manager.weatherData;
        setData(data);

        //Setup actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //Method to parse data and set to appropriate views
    abstract void setData(JSONObject data);


    //If an error is encountered or the data is null, return to previous activity
    @Override
    public void handleData(JSONObject data) {
        if(data != null) {
            setData(data);
        } else {
            supportFinishAfterTransition();
        }
    }

    @Override
    public void handleError(NetworkManager.CODE error) {
        //TODO add more robust error handling
        supportFinishAfterTransition();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //If data is > 1hr old request new data
        if(Calendar.getInstance().getTimeInMillis()- updated > 2500000){
            manager.getData(false,manager.lastLocationRequested);
        }
    }

    @Override
    public NetworkManager getNetworkManager() {
        return manager;
    }
}

package xyz.eleventhour.averageweather;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

/**
 * Created by Kailas on 5/15/2016.
 *
 * Abstract class template for the different types of PreviewData adapters
 */
public abstract class PreviewDataAdapter extends RecyclerView.Adapter<PreviewDataAdapter.PreviewDataHolder> {

    public JSONArray data;
    //Formatter necessary to make data readable
    public DataFormatter formatter;
    //Necessary to get icons
    public NetworkManager networkManager;

    public TimeZone timeZone;

    protected PreviewItemClickListener listener;

    public PreviewDataAdapter(JSONArray data, DataFormatter formatter, NetworkManager networkManager, PreviewItemClickListener listener, TimeZone timeZone) {
        this.data = data;
        this.formatter = formatter;
        this.networkManager = networkManager;
        this.listener = listener;
        this.timeZone = timeZone;
    }

    @Override
    public int getItemCount() {
        return data.length();
    }

    public void onBindViewHolder(PreviewDataHolder holder, DataFormatter.JSONDataGetter currentObj) {

        //Data is formatted by formatter before being displayed
        holder.percip.setText(formatter.formatPercip(currentObj.getDouble(Keys.percip)));
        holder.percipProb.setText(formatter.formatPercipProb(currentObj.getDouble(Keys.percip_prob)));

    }

    //Display error on card if error parsing data
    public void displayError(PreviewDataHolder holder){
        holder.time.setText(R.string.error);
    }

    //Template for Holder extended by children used as polymorphic type
    public abstract class PreviewDataHolder extends RecyclerView.ViewHolder{

        //Fields present in all holders
        public NetworkImageView icon;
        public TextView time;
        public TextView percip;
        public TextView percipProb;
        public View layout;

        public PreviewDataHolder(View layout){
            super(layout);
            this.layout = layout;
        }
    }
}

package xyz.eleventhour.averageweather;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Kailas on 6/30/2016.
 */
public class OverviewSpinnerAdapter extends ArrayAdapter<WeatherLocation> {

    List<WeatherLocation> locations;
    Context context;
    int closedRes = R.layout.overview_spinner_item_closed_layout;
    int openRes;

    public OverviewSpinnerAdapter(Context context, int resource, List<WeatherLocation> objects) {
        super(context, resource, objects);
        locations=objects;
        this.context = context;
        openRes = resource;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) LayoutInflater.from(context).inflate(openRes,parent,false);
        textView.setText(locations.get(position).toString());
        return textView;
    }

    //Closed textview should'nt have padding
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) LayoutInflater.from(context).inflate(closedRes,parent,false);
        textView.setText(locations.get(position).toString());
        return textView;
    }
}

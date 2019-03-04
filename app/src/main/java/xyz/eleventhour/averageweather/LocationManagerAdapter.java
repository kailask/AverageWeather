package xyz.eleventhour.averageweather;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Kailas on 8/3/2016.
 */
public class LocationManagerAdapter extends RecyclerView.Adapter<LocationManagerAdapter.LocationManagerHolder>{

    private ArrayList<WeatherLocation> locations;
    private Context context;

    public LocationManagerAdapter(ArrayList<WeatherLocation> locations, Context context){
        this.locations = locations;
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    @Override
    public LocationManagerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LocationManagerHolder(LayoutInflater.from(context).inflate(R.layout.location_manager_location,parent,false));
    }

    @Override
    public void onBindViewHolder(LocationManagerHolder holder, int position) {
        holder.title.setText(locations.get(position).locationText);

        //Set different icon if it is "My Location"
        if(locations.get(position).locationText.equals(StorageManager.MY_LOCATION_NAME)){
            holder.icon.setImageResource(R.drawable.ic_my_location);
            holder.coords.setText("");
        } else {
            holder.icon.setImageResource(R.drawable.ic_location_city);
            holder.coords.setText(locations.get(position).location.toString());
        }
    }

    public class LocationManagerHolder extends RecyclerView.ViewHolder{
        public TextView title;
        public TextView coords;
        public ImageView icon;

        public LocationManagerHolder(View layout){
            super(layout);
            title = ((TextView) layout.findViewById(R.id.location_title));
            coords = ((TextView) layout.findViewById(R.id.location_coords));
            icon = ((ImageView) layout.findViewById(R.id.location_icon));
        }
    }

    //Callbacks from ItemTouchHelper for drag and dismiss
    public void onItemDismiss(int position) {
        locations.remove(position);
        notifyItemRemoved(position);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        locations.add(toPosition,locations.remove(fromPosition));
        notifyItemMoved(fromPosition,toPosition);
    }
}

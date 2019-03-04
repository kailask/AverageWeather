package xyz.eleventhour.averageweather;

import android.support.v7.widget.RecyclerView;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.AutocompletePredictionBuffer;

/**
 * Created by Kailas on 8/1/2016.
 */
public class LocationSearchResultAdapter extends RecyclerView.Adapter<LocationSearchResultAdapter.SearchResultHolder> {

    public AutocompletePredictionBuffer locations;

    private PickLocationActivity activity;

    private CharacterStyle blackHighlight;

    public LocationSearchResultAdapter(AutocompletePredictionBuffer locations, PickLocationActivity activity){
        this.locations = locations;
        this.activity = activity;

        blackHighlight = new ForegroundColorSpan(activity.getResources().getColor(R.color.black));
    }

    @Override
    public int getItemCount() {
        //Locations may be null
        if(locations != null){
            return locations.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public SearchResultHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SearchResultHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.location_result,parent,false));
    }

    @Override
    public void onBindViewHolder(SearchResultHolder holder, int position) {
        try {
            holder.title.setText(locations.get(position).getPrimaryText(blackHighlight));
            holder.subtitle.setText(locations.get(position).getSecondaryText(blackHighlight));
        } catch (IllegalArgumentException e){
            //If buffer has been released finish activity
            e.printStackTrace();
            activity.supportFinishAfterTransition();
        }
    }

    public class SearchResultHolder extends RecyclerView.ViewHolder{
        public TextView title;
        public TextView subtitle;

        public SearchResultHolder(View layout){
            super(layout);
            title = (TextView) layout.findViewById(R.id.location_result_title);
            subtitle = ((TextView) layout.findViewById(R.id.location_result_subtitle));

            //OnClick register with activity, pass placeID for lookup
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onLocationClick(locations.get(getAdapterPosition()).getPlaceId());
                }
            });
        }
    }

    //Locations can be released by activity in relevant lifecycle callback
    public void release(){
        if(locations != null){
            locations.release();
        }
    }
}

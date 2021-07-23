package xyz.eleventhour.averageweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Created by Kailas on 6/1/2016.
 *
 * Fades in image
 */
public class FadeNetworkImageView extends NetworkImageView {

    public FadeNetworkImageView(Context context) {
        super(context);
    }

    public FadeNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FadeNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageUrl(String url, ImageLoader imageLoader) {
        AlphaAnimation animation = new AlphaAnimation(0f,1f);
        animation.setDuration(200);
        super.setImageUrl(url, imageLoader);
        this.startAnimation(animation);
    }
}

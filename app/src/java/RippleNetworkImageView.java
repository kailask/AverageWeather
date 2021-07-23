package xyz.eleventhour.averageweather;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;

import com.android.volley.toolbox.NetworkImageView;

/**
 * Created by Kailas on 6/9/2016.
 *
 * NetworkImageView that reveals its image
 */
public class RippleNetworkImageView extends NetworkImageView {

    //Only play ripple animation once
    private boolean hasRippled = false;

    public RippleNetworkImageView(Context context) {
        super(context);
    }

    public RippleNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RippleNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageResource(int resId) {
        reveal();
        super.setImageResource(resId);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        reveal();
        super.setImageBitmap(bm);
    }

    //Reveal image whenever setting image
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void reveal(){
        if(!hasRippled) {
            hasRippled = true;
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            float finalRadius = (float) Math.hypot(cx, cy);

            Animator anim = ViewAnimationUtils.createCircularReveal(this, cx, cy, 0, finalRadius);
            anim.setStartDelay(100);
            anim.setDuration(350);
            setVisibility(View.VISIBLE);
            anim.start();
        } else {
            setAlpha(0f);
            animate().setDuration(400).alpha(1).start();
        }
    }
}

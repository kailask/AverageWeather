package xyz.eleventhour.averageweather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

/**
 * Created by Kailas on 5/19/2016.
 *
 * Manages common animations
 *
 */
public class AnimationManager {

    public static void loadText(View textView, String text){
        textView.setAlpha(0f);
        if(textView instanceof TextView){
            ((TextView)textView).setText(text);
        }
        textView.animate().alpha(1f).setDuration(600);
    }

    //Drop view down, used for toolbar
    public static void dropDown(View view){
        ObjectAnimator drop = ObjectAnimator.ofFloat(view,"translationY",-200,0);
        ObjectAnimator fade = ObjectAnimator.ofFloat(view,"alpha",0f,1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(drop).with(fade);
        animatorSet.setDuration(400);
        animatorSet.start();
    }

    //Zoom view out, used for listview refresh
    public static void zoomOut(View view, Context context, Animator.AnimatorListener listener){
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(context,R.animator.zoom_out);
        set.setTarget(view);
        set.addListener(listener);
        set.start();
    }

    public static void fade(View view, int toVisibility){
        AlphaAnimation animation;
        if(toVisibility == View.VISIBLE){
            animation = new AlphaAnimation(0,1);
        } else {
            animation = new AlphaAnimation(1,0);
        }
        animation.setFillAfter(true);
        animation.setDuration(80);
        view.startAnimation(animation);
        //Make sure view if visible if fading in
        if(toVisibility == View.VISIBLE)view.setVisibility(View.VISIBLE);
    }

    public static void slideFadeDown(View viewToSlide, Context context){
        AnimatorSet animatorSet = ((AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.slide_fade));
        animatorSet.setTarget(viewToSlide);
        animatorSet.start();
    }
}

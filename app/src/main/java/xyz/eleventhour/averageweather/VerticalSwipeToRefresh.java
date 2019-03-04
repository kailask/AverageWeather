package xyz.eleventhour.averageweather;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by Kailas on 5/22/2016.
 *
 * Sub-class of SwipeRefreshLayout handles horizontal scrolling better
 */
public class VerticalSwipeToRefresh extends SwipeRefreshLayout{
    private int mTouchSlop;
    private float mPrevX;

    private boolean mDeclined;

    public VerticalSwipeToRefresh(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPrevX = MotionEvent.obtain(event).getX();
                mDeclined = false;
                break;

            case MotionEvent.ACTION_MOVE:
                final float eventX = event.getX();
                float xDiff = Math.abs(eventX - mPrevX);

                if (mDeclined|| xDiff > mTouchSlop) {
                    mDeclined = true;
                    return false;
                }
        }

        return super.onInterceptTouchEvent(event);
    }
}

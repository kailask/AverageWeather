package xyz.eleventhour.averageweather;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * Created by Kailas on 5/29/2016.
 */
public class LargeDetailHeaderIconBehavior extends CoordinatorLayout.Behavior<ImageView> {

    private float  collapsedIconSize, openIconSize, collapsedXPos;

    private float dependencyDivisionFactor;

    private Resources resources;

    private boolean isOver21;

    //Start at 0 and is set on first ViewChanged call
    private float openYPos = 0, openXPos = 0, dependencyCollapsedYPos = 0,collapsedYPos = 0;

    public LargeDetailHeaderIconBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resources = context.getResources();
        collapsedIconSize = resources.getDimension(R.dimen.icon_medium);
        openIconSize = resources.getDimension(R.dimen.icon_large_size);
        collapsedXPos = resources.getDimension(R.dimen.detail_toolbar_icon_margin_left);
        dependencyDivisionFactor = Float.parseFloat(resources.getString(R.string.current_detail_icon_position_division_factor));

        isOver21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, ImageView child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    private void initValues(View child,AppBarLayout dependency){
        if(openYPos == 0){
            openYPos = (dependency.getHeight()/ dependencyDivisionFactor) - (child.getHeight()/2f);
        }

        if(openXPos == 0){
            openXPos = child.getX();
        }

        if(dependencyCollapsedYPos == 0){
            dependencyCollapsedYPos = dependency.getTotalScrollRange();
        }

        if(collapsedYPos == 0){
            float actionBarHeight = dependency.findViewById(R.id.detail_toolbar).getHeight();
            collapsedYPos = (actionBarHeight - collapsedIconSize) / 2;
            //If translucent status bar the status bar height needs to be added
            if(isOver21){collapsedYPos += resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));}
        }
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, ImageView child, View dependency) {
        AppBarLayout appBarLayout = (AppBarLayout) dependency;

        initValues(child, appBarLayout);

        float percentageCollapsed = (Math.abs(appBarLayout.getY()) / dependencyCollapsedYPos);

        //Set icon pos
        int xDiff = (int)(openXPos - collapsedXPos);
        int yDiff = (int)(openYPos - collapsedYPos);

        float scaledPercentage = new DecelerateInterpolator().getInterpolation(percentageCollapsed);

        child.setX(openXPos-(xDiff*scaledPercentage));
        child.setY(openYPos-(yDiff*scaledPercentage));

        //Set icon scale
        scaledPercentage = new AccelerateDecelerateInterpolator().getInterpolation(percentageCollapsed);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        int sizeDiff = (int) (openIconSize - collapsedIconSize);
        int amountToSubtract = (int) (sizeDiff * scaledPercentage);

        params.width = (int) (openIconSize - amountToSubtract);
        params.height = (int) (openIconSize - amountToSubtract);

        child.setLayoutParams(params);

        return true;
    }
}

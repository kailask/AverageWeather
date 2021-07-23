package xyz.eleventhour.averageweather;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Kailas on 6/25/2016.
 */
public class DailyDetailHeaderTabsBehavior extends CoordinatorLayout.Behavior<TabLayout> {

    float tabBarHeight, dependencyMaxValue = 0, openYPos = 0,collapsedYPos = 0;

    private Resources resources;

    private boolean isOver21;

    public DailyDetailHeaderTabsBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.resources = context.getResources();

        isOver21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;


        tabBarHeight = resources.getDimension(R.dimen.tab_bar_height);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, TabLayout child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    private void initValues(AppBarLayout dependency){
        //Init values if 0
        if(dependencyMaxValue == 0){
            dependencyMaxValue = dependency.getTotalScrollRange();
        }

        if(openYPos == 0){
            openYPos=dependency.findViewById(R.id.detail_tab_bar_placeholder).getBottom();
        }
        if(collapsedYPos == 0){
            //Collapsed position should be just above the toolbar
            collapsedYPos = dependency.findViewById(R.id.detail_toolbar).getHeight()-tabBarHeight;
            //If translucent status bar the status bar height needs to be added
            if(isOver21){collapsedYPos += resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));}
        }
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, TabLayout child, View dependency) {
        AppBarLayout appBarLayout = (AppBarLayout) dependency;

        initValues(appBarLayout);

        float percentageCollapsed = (Math.abs(appBarLayout.getY()) / dependencyMaxValue);

        float scaledDiff = ((1-percentageCollapsed)*(openYPos-collapsedYPos));
        child.setY(scaledDiff + collapsedYPos);

        return true;
    }
}

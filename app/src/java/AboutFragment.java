package xyz.eleventhour.averageweather;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Kailas on 8/8/2016.
 */
public class AboutFragment extends Fragment implements MainActivity.NavIDableFragment {

    public static final String FRAGMENT_NAME = "ELIZA";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.about_fragment_layout,container,false);

        PackageManager manager = getActivity().getPackageManager();

        try {
            ((TextView) layout.findViewById(R.id.version_text)).setText(manager.getPackageInfo(getActivity().getPackageName(),0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return layout;
    }

    @Override
    public int getIdInNavMenu() {
        return R.id.about_nav_fragment;
    }
}

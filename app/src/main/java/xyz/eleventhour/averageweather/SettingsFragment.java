package xyz.eleventhour.averageweather;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.NotificationCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.RemoteViews;

/**
 * Created by Kailas on 8/4/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements MainActivity.NavIDableFragment, SharedPreferences.OnSharedPreferenceChangeListener{
    public static final String FRAGMENT_NAME = "ALSO JEFF";

    private NetworkManager.DataHandlerActivity activity;

    private SharedPreferences sharedPreferences;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        activity = ((NetworkManager.DataHandlerActivity) getActivity());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        // show the current value in the settings screen
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            pickPreferenceObject(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public int getIdInNavMenu() {
        return R.id.settings_nav_fragment;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = getPreferenceScreen().findPreference(key);

        if(preference instanceof ListPreference) {
            String value = sharedPreferences.getString(key, "");
            preference.setSummary(value);
        } else if(preference instanceof SwitchPreference){
            Boolean value = sharedPreferences.getBoolean(key,false);
            preference.setSummary(value.toString());
            if(value){
                RemoteViews content = new RemoteViews(activity.getPackageName(),R.layout.notification_current_layout_large);
                RemoteViews alsoContent = new RemoteViews(activity.getPackageName(),R.layout.notification_current_layout_small);
                NetworkManager.DataHandlerActivity activity = ((NetworkManager.DataHandlerActivity) getActivity());
                DataFormatter.JSONDataGetter getter = new DataFormatter.JSONDataGetter(activity.getData());

                android.support.v4.app.NotificationCompat.Builder notification = new NotificationCompat.Builder(getActivity())
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setContent(content)
                        .setSmallIcon(R.drawable.ic_cloud_black);

                NotificationManager manager = ((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE));

                manager.notify(2, notification.build());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void pickPreferenceObject(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory cat = (PreferenceCategory) p;
            for (int i = 0; i < cat.getPreferenceCount(); i++) {
                pickPreferenceObject(cat.getPreference(i));
            }
        } else {
            initSummary(p);
        }
    }

    private void initSummary(Preference p) {
        if(p instanceof ListPreference) {
            String value = sharedPreferences.getString(p.getKey(), "");
            p.setSummary(value);
        }
    }
}

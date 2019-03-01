package com.camera.zshot.zshot.ui;

import android.app.Activity;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.camera.zshot.zshot.R;
import com.camera.zshot.zshot.keys.Keys;

public class AppSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.SettingsRootView,new CameraSettings()).commit();
    }

    public static class CameraSettings extends PreferenceFragment
    {
        PreferenceScreen preferenceScreen;
        OnPreferenceClickListener onPreferenceClickListener;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            preferenceScreen = this.getPreferenceScreen();

            PreferenceCategory CameraCategory = new PreferenceCategory(getActivity());
            CameraCategory.setTitle(R.string.titleCameraSettings);
            CameraCategory.setEnabled(true);

            Preference CameraSettings = new Preference(getActivity());
            CameraSettings.setTitle(R.string.titleCameraSettings);
            CameraSettings.setSummary(R.string.titleCameraSettingsSummary);
            CameraSettings.setKey(Keys.CameraSettingPreferenceKey);
            CameraSettings.setOnPreferenceClickListener(onPreferenceClickListener);

        }
    }

    static class OnPreferenceClickListener implements Preference.OnPreferenceClickListener {

        private Activity activity;

        OnPreferenceClickListener(Activity activity)
        {
            this.activity = activity;
        }


        @Override
        public boolean onPreferenceClick(Preference preference) {

            switch (preference.getKey())
            {
                case Keys.CameraSettingPreferenceKey:
                    activity.startActivity(new Intent(activity, CameraSetting.class));
                    break;


            }

            return false;
        }
    }


}

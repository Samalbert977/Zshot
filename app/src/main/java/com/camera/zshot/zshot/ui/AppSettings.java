package com.camera.zshot.zshot.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.camera.zshot.zshot.R;
import com.camera.zshot.zshot.keys.Keys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class AppSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction().replace(R.id.SettingsRootView, new CameraSettings()).commit();
    }

    public static class CameraSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        PreferenceScreen preferenceScreen;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Context context = getPreferenceManager().getContext();
            preferenceScreen = getPreferenceManager().createPreferenceScreen(context);

            PreferenceCategory CameraCategory = new PreferenceCategory(context);
            CameraCategory.setTitle(R.string.titleCameraSettings);
            CameraCategory.setEnabled(true);

            Preference CameraSettings = new Preference(context);
            CameraSettings.setTitle(R.string.titleCameraSettings);
            CameraSettings.setSummary(R.string.titleCameraSettingsSummary);
            CameraSettings.setKey(Keys.CameraSettingPreferenceKey);
            CameraSettings.setOnPreferenceClickListener(this);

            preferenceScreen.addPreference(CameraSettings);

            setPreferenceScreen(preferenceScreen);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            switch (preference.getKey()) {
                case Keys.CameraSettingPreferenceKey:
                    startActivity(new Intent(getContext(), CameraSetting.class));
                    break;
            }
            return false;
        }
    }
}

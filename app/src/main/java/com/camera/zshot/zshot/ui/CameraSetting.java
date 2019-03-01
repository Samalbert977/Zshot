package com.camera.zshot.zshot.ui;

import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;

import com.camera.zshot.zshot.R;
import com.camera.zshot.zshot.camera.Camera;
import com.camera.zshot.zshot.keys.Keys;

public class CameraSetting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        getFragmentManager().beginTransaction().replace(R.id.SettingsMainRootView,new CameraSettingMain()).commit();
    }

    public static class CameraSettingMain extends PreferenceFragment
    {
        PreferenceScreen preferenceScreen;
        private String[] ResEntries = null;
        private String[] ResEntriesValues = null;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            preferenceScreen = this.getPreferenceScreen();

            ListPreference ResolutionList = new ListPreference(getActivity());
            ResolutionList.setTitle(R.string.ResolutionTitle);
            ResolutionList.setSummary(R.string.ResolutionSummary);
            ResolutionList.setKey(Keys.ImageResolutionKey);
            ProcessCameraOutputSizes();
            ResolutionList.setEntries(ResEntries);
            ResolutionList.setEntryValues(ResEntriesValues);

            preferenceScreen.addPreference(ResolutionList);
        }

        private void ProcessCameraOutputSizes()
        {
         Size[] outputsizes =  Camera.getCameraresolutions();

         if(outputsizes == null)
             return ;

         ResEntries = new String[outputsizes.length];
         ResEntriesValues = new String[outputsizes.length];

         for(int i = 0; i<outputsizes.length;i++) {

             int value = outputsizes[i].getWidth()*outputsizes[i].getHeight()/1000000;
             ResEntries[i] = String.valueOf(value).concat(" MP");
             ResEntriesValues[i] = String.valueOf(outputsizes[i].getWidth()).concat("*").concat(String.valueOf(outputsizes[i].getHeight()));
         }
        }

    }



}

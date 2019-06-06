package com.camera.zshot.zshot.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import android.util.Size;

import com.camera.zshot.zshot.R;
import com.camera.zshot.zshot.camera.Camera;
import com.camera.zshot.zshot.keys.Keys;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class CameraSetting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

       getSupportFragmentManager().beginTransaction().replace(R.id.SettingsMainRootView,new CameraSettingMain()).commit();
    }

    public static class CameraSettingMain extends PreferenceFragmentCompat
    {
        PreferenceScreen preferenceScreen;
        private String[] ResEntries = null;
        private String[] ResEntriesValues = null;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getPreferenceManager().getContext();
            preferenceScreen = getPreferenceManager().createPreferenceScreen(context);

            ListPreference ResolutionList = new ListPreference(context);
            ResolutionList.setTitle(R.string.ResolutionTitle);
            ResolutionList.setSummary(R.string.ResolutionSummary);
            ResolutionList.setKey(Keys.ImageResolutionKey);
            ProcessCameraOutputSizes();
            ResolutionList.setEntries(ResEntries);
            ResolutionList.setEntryValues(ResEntriesValues);

            preferenceScreen.addPreference(ResolutionList);

            EditTextPreference ContrastPref = new EditTextPreference(context);
            ContrastPref.setTitle(R.string.ContrastPrefTitle);
            ContrastPref.setSummary(R.string.ContrastPrefSummary);
            ContrastPref.setDefaultValue("0.5");
            ContrastPref.setKey(Keys.contrastLevelKey);
            ContrastPref.setDialogMessage("0~10");

            preferenceScreen.addPreference(ContrastPref);

            setPreferenceScreen(preferenceScreen);
        }

        private void ProcessCameraOutputSizes()
        {
         Size[] outputsizes =  Camera.getCameraresolutions();

         if(outputsizes == null)
             return ;
         List<Integer> buffer = new ArrayList<>();
         List<String> buffer1 = new ArrayList<>();
         for(int i = 0; i<outputsizes.length;i++) {

             int value = outputsizes[i].getWidth()*outputsizes[i].getHeight()/1000000;
             if(value > 1) {
                 buffer.add(value);
                 buffer1.add(String.valueOf(outputsizes[i].getWidth()).concat("_").concat(String.valueOf(outputsizes[i].getHeight())));
             }
         }

         ResEntries = new String[buffer.size()];
         ResEntriesValues = new String[buffer1.size()];

         for(int i=0;i<buffer.size();i++)
         {
             ResEntries[i] = String.valueOf(buffer.get(i)).concat(" MP");
             ResEntriesValues[i] = buffer1.get(i);
         }

        }

    }



}

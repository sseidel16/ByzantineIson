package com.coderss.ison;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;

import com.coderss.ison.utility.Player;
import com.coderss.ison.utility.Preferences;
import com.coderss.ison.utility.SoundSet;

public class AppSettings extends AppCompatActivity {

    private Preferences preferences;
    private Player player;

    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        super.onCreate(savedInstanceState);

        player = new Player(this);

        setContentView(R.layout.app_settings);
        setTitle("Preferences");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(getApplicationContext(), IsonActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    public static class MySettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            ListPreference prefSelectSound = findPreference("keySelectSound");
            String[] soundSetEntries = SoundSet.getSoundSets(getActivity().getAssets());
            String[] soundSetEntryValues = new String[soundSetEntries.length];
            for (int soundSetI = 0; soundSetI < soundSetEntries.length; soundSetI++) {
                soundSetEntryValues[soundSetI] = Integer.toString(soundSetI);
            }

            prefSelectSound.setEntries(soundSetEntries);
            prefSelectSound.setEntryValues(soundSetEntryValues);
            prefSelectSound.setDefaultValue(soundSetEntryValues[0]);
            prefSelectSound.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                System.out.println("Setting sound set");
                AppSettings appSettings = (AppSettings) getActivity();
                try {
                    int soundSetI = Integer.parseInt(newValue.toString());
                    SoundSet.loadSoundSet(appSettings.player, appSettings.getAssets(), soundSetI);
                    return true;
                } catch (Throwable th) {
                    th.printStackTrace();
                    return false;
                }
            });
        }
    }

}

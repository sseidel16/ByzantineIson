package com.coderss.ison;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.coderss.ison.utility.Player;
import com.coderss.ison.utility.Preferences;
import com.coderss.ison.utility.Scale;
import com.coderss.ison.utility.SoundSet;

public class AppSettings extends AppCompatActivity {

    private Preferences preferences;
    private Player player;

    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(getBaseContext());

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

            Preference resetScales = findPreference("keyScaleReset");
            resetScales.setOnPreferenceClickListener(preference -> {
                ResetConfirmation dialog = new ResetConfirmation();
                dialog.show(getActivity().getSupportFragmentManager(),"dialog");
                return false;
            });
        }
    }

    public static class ResetConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? All created and edited scales will be lost");
            builder.setPositiveButton("OK", (dialog, id) -> Scale.emergencyReset(getActivity()));
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

}

package com.coderss.ison;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;

import com.coderss.ison.utility.Preferences;

public class AppSettings extends AppCompatActivity {

    private Preferences preferences;

    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        super.onCreate(savedInstanceState);
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
        }
    }

}

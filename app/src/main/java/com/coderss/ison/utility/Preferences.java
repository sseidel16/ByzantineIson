package com.coderss.ison.utility;

import android.content.SharedPreferences;

public class Preferences {

    private SharedPreferences sharedPreferences;

    public Preferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isBaseNoteSliderDiscrete() {
        return sharedPreferences.getString("listBaseSlider", "discrete").equals("discrete");
    }

    public float getFrequencyChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("listFrequencyChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public float getVolumeChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("listVolumeChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public boolean isDarkTheme() {
        return sharedPreferences.getString("keyTheme", "dark").equals("dark");
    }
}

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

    public boolean isLeftToRight() {
        return sharedPreferences.getString("keyHorizontalDirection", "l2r").equals("l2r");
    }

    public boolean isTopToBottom() {
        return sharedPreferences.getString("keyVerticalDirection", "b2t").equals("t2b");
    }

    public float getButtonHeight() {
        int sixteenths;
        try {
            sixteenths = Integer.parseInt(sharedPreferences.getString("keyButtonHeight", "05"), 16);
        } catch (Throwable th) {
            th.printStackTrace();
            sixteenths = 5;
        }
        return sixteenths / 16f;
    }

}

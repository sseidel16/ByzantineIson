package com.coderss.ison.utility;

import android.content.SharedPreferences;
import android.content.res.AssetManager;

public class Preferences {

    private static boolean soundSetInitialized = false;
    private SharedPreferences sharedPreferences;

    public Preferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void initializeSoundSet(Player player, AssetManager assets) {
        if (!soundSetInitialized) {
            int soundSetI = getSelectSound();
            SoundSet.loadSoundSet(player, assets, soundSetI);

            soundSetInitialized = true;
        }
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

    public boolean isDarkTheme() {
        return sharedPreferences.getString("keyTheme", "dark").equals("dark");
    }

    public int getNotesBelow() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyNotesBelow", "4"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public int getNotesAbove() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyNotesAbove", "8"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public int getSelectSound() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keySelectSound", "0"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public boolean isBaseNoteSliderDiscrete() {
        return sharedPreferences.getString("listBaseSlider", "discrete").equals("discrete");
    }

    public boolean isShowingMinorChangers() {
        return sharedPreferences.getBoolean("keyShowMinorChangers", false);
    }

    public boolean isShowingMajorChangers() {
        return sharedPreferences.getBoolean("keyShowMajorChangers", false);
    }

    public float getVolumeChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("listVolumeChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public float getFrequencyChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("listFrequencyChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public boolean isLeftToRight() {
        return sharedPreferences.getString("keyHorizontalDirection", "l2r").equals("l2r");
    }

    public boolean isTopToBottom() {
        return sharedPreferences.getString("keyVerticalDirection", "b2t").equals("t2b");
    }

    public boolean isFlowHorizontal() {
        return sharedPreferences.getString("keyFlowDirection", "horizontal").equals("horizontal");
    }

    public int getDimensionLimit() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyDimensionLimit", "1"));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

}

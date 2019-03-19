package com.coderss.ison.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.coderss.ison.R;

import androidx.preference.PreferenceManager;

public class Preferences {

    private static boolean soundSetInitialized = false;
    private SharedPreferences sharedPreferences;
    private Context baseContext;

    public Preferences(Context baseContext) {
        this.baseContext = baseContext;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext);
    }

    public void initializeSoundSet(Player player) {
        if (!soundSetInitialized) {
            int soundSetI = getSelectSound();
            SoundSet.loadSoundSet(player, baseContext.getAssets(), soundSetI);

            soundSetInitialized = true;
        }
    }

    public float getButtonHeight() {
        int sixteenths;
        try {
            sixteenths = Integer.parseInt(sharedPreferences.getString("keyButtonHeight",
                    baseContext.getString(R.string.keyButtonHeightDefault)), 16);
        } catch (Throwable th) {
            th.printStackTrace();
            sixteenths = 5;
        }
        return sixteenths / 16f;
    }

    public float getDockWidth() {
        int sixteenths;
        try {
            sixteenths = Integer.parseInt(sharedPreferences.getString("keyDockWidth",
                    baseContext.getString(R.string.keyDockWidthDefault)), 16);
        } catch (Throwable th) {
            th.printStackTrace();
            sixteenths = 8;
        }
        return sixteenths / 16f;
    }

    public boolean isDarkTheme() {
        return sharedPreferences.getString("keyTheme",
                baseContext.getString(R.string.keyThemeDefault)).equals("dark");
    }

    public int getNotesBelow() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyNotesBelow",
                    baseContext.getString(R.string.keyNotesBelowDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public int getNotesAbove() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyNotesAbove",
                    baseContext.getString(R.string.keyNotesAboveDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public int getSelectSound() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keySelectSound",
                    baseContext.getString(R.string.keySelectSoundDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public boolean isBaseNoteSliderDiscrete() {
        return sharedPreferences.getString("keyBaseSlider",
                baseContext.getString(R.string.keyBaseSliderDefault)).equals("discrete");
    }

    public boolean isKeepingScreenOn() {
        return sharedPreferences.getBoolean("keyKeepScreenOn",
                baseContext.getString(R.string.keyKeepScreenOnDefault).equals("true"));
    }

    public boolean isShowingMinorChangers() {
        return sharedPreferences.getBoolean("keyShowMinorChangers",
                baseContext.getString(R.string.keyShowMinorChangersDefault).equals("true"));
    }

    public boolean isShowingMajorChangers() {
        return sharedPreferences.getBoolean("keyShowMajorChangers",
                baseContext.getString(R.string.keyShowMajorChangersDefault).equals("true"));
    }

    public float getVolumeChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("keyVolumeChangeTime",
                    baseContext.getString(R.string.keyVolumeChangeTimeDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public float getFrequencyChangeTime() {
        try {
            return Float.parseFloat(sharedPreferences.getString("keyFrequencyChangeTime",
                    baseContext.getString(R.string.keyFrequencyChangeTimeDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

    public boolean isLeftToRight() {
        return sharedPreferences.getString("keyHorizontalDirection",
                baseContext.getString(R.string.keyHorizontalDirectionDefault)).equals("l2r");
    }

    public boolean isTopToBottom() {
        return sharedPreferences.getString("keyVerticalDirection",
                baseContext.getString(R.string.keyVerticalDirectionDefault)).equals("t2b");
    }

    public boolean isFlowHorizontal() {
        return sharedPreferences.getString("keyFlowDirection",
                baseContext.getString(R.string.keyFlowDirectionDefault)).equals("horizontal");
    }

    public int getDimensionLimit() {
        try {
            return Integer.parseInt(sharedPreferences.getString("keyDimensionLimit",
                    baseContext.getString(R.string.keyDimensionLimitDefault)));
        } catch (Throwable th) {
            th.printStackTrace();
            return 0;
        }
    }

}

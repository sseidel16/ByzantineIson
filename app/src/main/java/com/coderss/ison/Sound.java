package com.coderss.ison;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

public class Sound {

    float frequency;
    short[] data;

    public Sound(float frequency, String path, AssetManager assets, Loader parent) {
        this.frequency = frequency;
        data = Reader.readWAV(path, assets, parent);
    }

    public void destroy() {
        data = null;
    }

}

package com.coderss.ison;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Sound {

    float frequency;
    short[] data;

    public Sound(float frequency, String path, AssetManager assets, Loader parent) {
        this.frequency = frequency;
        data = readWAV(path, assets);
    }

    public void destroy() {
        data = null;
    }

    private short[] readWAV(String file, AssetManager assets) {
        try {
            DataInputStream dis = new DataInputStream(assets.open(file));
            int byteDataLength = dis.readInt() * 2;
            byte[] byteData = new byte[byteDataLength];
            short[] shortData = new short[byteDataLength / 2];
            dis.readFully(byteData);
            ByteBuffer.wrap(byteData).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shortData);
            return shortData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;//out of memory
        }
    }

}

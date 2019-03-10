package com.coderss.ison;

import java.io.DataInputStream;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

public class Reader {

    public static short[] readWAV(String file, AssetManager assets, Loader parent) {
        try {
            DataInputStream dis = new DataInputStream(assets.open(file));
            int dataLength = dis.readInt();
            short[] data = new short[dataLength];
            parent.setSecondaryProgress(0.0);
            for (int i = 0; i < data.length; ++i) {
                if (parent.isCancelled()) return null;
                data[i] = dis.readShort();
                parent.setSecondaryProgress((int)((100.0 * (i + 1)) / dataLength));
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;//out of memory
        }
    }


}

package com.coderss.ison;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

public class SoundSet {

    Sound[] notes;

    int soundSetIndex;//indicates the sound set

    public SoundSet(int soundSetIndex, AssetManager assets, Loader parent) {
        this.soundSetIndex = soundSetIndex;
        try {
            parent.setPrimaryProgress(0.0);
            String folder = getSoundSets(assets)[soundSetIndex];
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(assets.open("SoundSets/" + folder + "/list.txt")));
            Vector<Sound> temp = new Vector<>(10, 10);
            int totalSounds = Integer.parseInt(br.readLine());
            for (int i = 0; i < totalSounds; ++i) {
                if (parent.isCancelled()) break;
                String line = br.readLine();
                if (line == null || line.equals("")) break;
                float frequency = Float.parseFloat(line);
                Sound current = new Sound(frequency,
                        "SoundSets/" + folder + "/" + line,
                        assets,
                        parent);
                if (current.data == null) break;//out of memory
                temp.add(current);
                parent.setPrimaryProgress((100.0 * (i + 1)) / totalSounds);
            }
            notes = new Sound[temp.size()];
            for (int i = 0; i < notes.length; ++i) {
                notes[i] = temp.get(i);
            }
            temp.removeAllElements();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        for (int i = 0; i < notes.length; ++i) {
            notes[i].destroy();
        }
    }

    public static String[] getSoundSets(AssetManager assets) {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(assets.open("SoundSets/SoundSets.txt")));
            int n = Integer.parseInt(br.readLine());
            String[] soundSets = new String[n];
            for (int i = 0; i < n; ++i) soundSets[i] = br.readLine();
            br.close();
            return soundSets;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[0];
    }

}

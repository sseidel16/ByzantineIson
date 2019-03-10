package com.coderss.ison;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

public class SoundSet {

    Sound[] notes;
    Sound bestSound;

    int soundSetIndex;//indicates the sound set

    short preSample = 0;
    //the final sample of the previous buffer

    double startingPoint = 0.0;
    //used by Sound.getNextBuffer, usually negative indicates whether preSample will be factored in

    short[] scaledBuffer = new short[1024];
    //array filled by Sound.getNextBuffer
    //since the amount of data returned is unknown, 1024 is specified as a maximum

    int scaledBufferSize;
    //indicates how full scaledBuffer is (of relevant data)

    int bufferIndex = 0;

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
                double frequency = Double.parseDouble(line);
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

    public void fillSoundBuffer(short[] buffer,
                                AtomicReference<Float> atomicFreq,
                                AtomicReference<Float> atomicVolume,
                                Runnable checkPref) {
        for (int i = 0; i < buffer.length; ++i) {
            float freq = atomicFreq.get();
            float volume = atomicVolume.get();
            setBestSound(freq);
            if (bufferIndex >= scaledBufferSize) {
                bestSound.fillNextBuffer(this, freq, volume);
                bufferIndex = 0;
            }
            buffer[i] = scaledBuffer[bufferIndex];
            checkPref.run();
            ++bufferIndex;
        }
    }

    public void setBestSound(double frequency) {
        double shortestDistance = -1.0;
        Sound best = null;
        for (int i = 0; i < notes.length; ++i) {
            double distance = Math.abs(notes[i].frequency - frequency);
            if (shortestDistance == -1.0 ||
                    distance < shortestDistance) {
                shortestDistance = distance;
                best = notes[i];
            }
        }
        bestSound = best;
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

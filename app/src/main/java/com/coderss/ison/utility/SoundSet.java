package com.coderss.ison.utility;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.res.AssetManager;

public class SoundSet {

    public static int soundSetIndex = -1;

    public static void loadSoundSet(Player player, AssetManager assets, int soundSetIndex) {
        try {
            String folder = getSoundSets(assets)[soundSetIndex];

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(assets.open("SoundSets/" + folder + "/list.txt")));

            int totalSounds = Integer.parseInt(br.readLine());

            short[][] soundDataArray = new short[totalSounds][];
            float[] frequencyArray = new float[totalSounds];

            for (int i = 0; i < totalSounds; ++i) {
                String frequencyStr = br.readLine();

                float frequency = Float.parseFloat(frequencyStr);
                String path = "SoundSets/" + folder + "/" + frequencyStr;
                short[] data = loadSound(path, assets);
                soundDataArray[i] = data;
                frequencyArray[i] = frequency;
            }

            player.setSounds(soundDataArray, frequencyArray);
            SoundSet.soundSetIndex = soundSetIndex;
        } catch (Exception e) {
            e.printStackTrace();
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

    private static short[] loadSound(String path, AssetManager assets) {
        try {
            DataInputStream dis = new DataInputStream(assets.open(path));
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

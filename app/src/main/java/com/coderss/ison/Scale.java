package com.coderss.ison;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.AssetManager;

public class Scale {

    static final String[] noteNames = {"Nh", "Pa", "Bou", "Ga", "Di", "Ke", "Zw"};

    public static ArrayList<Scale> loadScales(Context context) {
        ArrayList<Scale> scales = new ArrayList<Scale>(4);
        DataInputStream dis = null;
        File filev2 = new File(context.getFilesDir().getPath() + "/scales.v2");
        File filev1 = new File(context.getFilesDir().getPath() + "/scales.txt");
        System.out.println(filev1.exists() + "," + filev2.exists());
        scales.clear();
        if (filev1.exists()) {//get scales from old file format
            try {
                System.out.println("Upgrading");
                BufferedReader br = new BufferedReader(
                        new FileReader(filev1));
                while (true) {
                    String name = br.readLine();
                    if (name == null || name.equals("")) break;
                    int[] widths = new int[7];
                    for (int i = 0; i < 7; ++i) {
                        widths[i] = Integer.parseInt(br.readLine());
                    }
                    int baseNote = 0;//base note that didnt exist
                    scales.add(new Scale(widths, name, baseNote));
                }
                br.close();
                filev1.delete();
                writeScales(context, scales);
            } catch (Exception e) {
                e.printStackTrace();
                emergencyReset(context);
            }
        } else if (filev2.exists()) {//get scales from new written file
            try {
                dis = new DataInputStream(
                        new FileInputStream(filev2));
                while (true) {
                    String name = dis.readUTF();
                    if (name.equals("EOF")) break;
                    int[] widths = new int[7];
                    for (int i = 0; i < widths.length; ++i) {
                        widths[i] = dis.readInt();
                    }
                    int baseNote = dis.readInt();
                    scales.add(new Scale(widths, name, baseNote));
                }
                dis.close();
            } catch (Exception e) {
                e.printStackTrace();
                emergencyReset(context);
            }
        }
        if (!filev1.exists() && !filev2.exists()) {//get scales from assets (last resort)
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(context.getResources().getAssets().open("scales.txt")));
                while (true) {
                    String name = br.readLine();
                    if (name == null || name.equals("")) break;
                    int[] widths = new int[7];
                    for (int i = 0; i < 7; ++i) {
                        widths[i] = Integer.parseInt(br.readLine());
                    }
                    int baseNote = Integer.parseInt(br.readLine());
                    scales.add(new Scale(widths, name, baseNote));
                }
                br.close();
                writeScales(context, scales);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        return scales;
    }

    public static void emergencyReset(Context context) {
        File filev2 = new File(context.getFilesDir().getPath() + "/scales.v2");
        File filev1 = new File(context.getFilesDir().getPath() + "/scales.txt");
        System.out.println("Deleting");
        if (filev1 != null && filev1.exists()) filev1.delete();
        if (filev2 != null && filev2.exists()) filev2.delete();
    }

    public static void writeScales(Context context, ArrayList<Scale> scales) {
        try {
            File file = new File(context.getFilesDir().getPath() + "/scales.v2");
            DataOutputStream dos = new DataOutputStream(
                    new FileOutputStream(file));
            for (int i = 0; i < scales.size(); ++i) {
                dos.writeUTF(scales.get(i).name);
                for (int ii = 0; ii < scales.get(i).widths.length; ++ii) {
                    dos.writeInt(scales.get(i).widths[ii]);
                }
                dos.writeInt(scales.get(i).baseNote);
            }
            dos.writeUTF("EOF");
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final int BASE_NOTE_INDEX = 4;
    static final int TOTAL_KEYS = 13;
    String name;
    int[] notes;//lo ga to hi pa
    int[] widths;
    int totalSteps;
    int baseNote;//0-6

    private Scale() {
    }

    public Scale(int[] widths, String name, int baseNote) {
        this.name = name;
        this.baseNote = baseNote;
        totalSteps = 0;
        this.widths = new int[widths.length];
        for (int i = 0; i < 7; ++i) {
            this.widths[i] = widths[i];
            totalSteps += widths[i];
        }

        //currentNoteIndex is from 0 to 6 start at the bottom button
        int currentNoteIndex = baseNote - BASE_NOTE_INDEX;
        System.out.println(name + baseNote);
        currentNoteIndex = correctZeroToSix(currentNoteIndex);

        notes = new int[TOTAL_KEYS];
        int totalMoriaToBaseNote = 0;//moria from button[0] to button[BASE_NOTE_INDEX]
        for (int i = 0; i < TOTAL_KEYS; ++i) {
            if (i == 0) {
                notes[i] = 0;
            } else {
                if (i <= BASE_NOTE_INDEX)
                    totalMoriaToBaseNote += widths[correctZeroToSix(currentNoteIndex - 1)];
                notes[i] = notes[i - 1] + widths[correctZeroToSix(currentNoteIndex - 1)];
            }
            currentNoteIndex = correctZeroToSix(currentNoteIndex + 1);
        }

        for (int i = 0; i < TOTAL_KEYS; ++i) {
            notes[i] -= totalMoriaToBaseNote;
        }
    }

    public Scale copy() {
        Scale copy = new Scale();
        copy.name = name;
        copy.notes = new int[notes.length];
        for (int i = 0; i < notes.length; ++i) copy.notes[i] = notes[i];
        copy.widths = new int[widths.length];
        for (int i = 0; i < widths.length; ++i) copy.widths[i] = widths[i];
        copy.totalSteps = totalSteps;
        copy.baseNote = baseNote;
        return copy;
    }

    public static int correctZeroToSix(int n) {
        while (n < 0) n += 7;
        while (n > 6) n -= 7;
        return n;
    }

}

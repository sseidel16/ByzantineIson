package com.coderss.ison.utility;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.util.concurrent.atomic.AtomicReference;

public class Player {

    private static final int NUM_BUFFERS = 2;

    static {
        System.loadLibrary("native-lib");
    }

    // Native methods
    private static native void native_createEngine(int apiLevel);
    private static native void native_destroyEngine();
    private static native AudioTrack native_createAudioPlayer(int frameRate, int framesPerBuffer, int numBuffers);
    private static native void native_destroyAudioPlayer();
    private static native void native_setFrequency(float frequency);
    private static native void native_setVolume(float volume);
    private static native void native_setSounds(short[][] soundDataArray, float[] frequencyArray);
    private static native void native_setPlayerPreferences(
            float frequencyChangeTime,
            float volumeChangeTime);

    private static native void native_setWorkCycles(int workCycles);
    private static native void native_setLoadStabilizationEnabled(boolean isEnabled);

    // load the native sound processor
    private static boolean libraryLoaded = false;

    private static synchronized void loadLibrary(int sampleRate, int framesPerBuffer) {
        if (!libraryLoaded) {
            libraryLoaded = true;

            System.loadLibrary("native-lib");

            System.out.println("Native create1 in");
            native_createEngine(Build.VERSION.SDK_INT);
            System.out.println("Native create1 out");
            System.out.println("Native create2 in");
            native_createAudioPlayer(sampleRate, framesPerBuffer, NUM_BUFFERS);
            System.out.println("Native create2 out");
        }
    }

    private AtomicReference<Float> freq = new AtomicReference<>();
    private AtomicReference<Float> volume = new AtomicReference<>();

    public Player(Context context, float volume, float freq) {
        this.freq.set(freq);
        this.volume.set(volume);

        System.out.println("Grabbing audio parameters");
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int sampleRate = 0;
        int framesPerBuffer = 0;

        if (am != null) {
            String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(sampleRateStr);
            String framesPerBufferStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            framesPerBuffer = Integer.parseInt(framesPerBufferStr);
        }
        if (sampleRate == 0) sampleRate = 44100; // Use a default value if property not found
        if (framesPerBuffer == 0) framesPerBuffer = 256; // Use a default value if property not found

        // load library if not already loaded
        loadLibrary(sampleRate, framesPerBuffer);
    }

    public void setSounds(short[][] soundDataArray, float[] frequencyArray) {
        native_setSounds(soundDataArray, frequencyArray);
    }

    public void start() {
        //startJavaSpeaker();

        System.out.println("Native setFrequency in: " + freq.get());
        native_setFrequency(freq.get());
        System.out.println("Native setFrequency out");

        System.out.println("Native setVolume in: " + volume.get());
        native_setVolume(volume.get());
        System.out.println("Native setVolume out");
        //native_setWorkCycles(60000);
    }

    public void setFrequency(float frequency) {//Play this frequency
		changeFreq(frequency);
		setVolume(1);
    }

    public void setVolume(float volume) {
        this.volume.set(volume);

        System.out.println("Native_setVolume in: " + volume);
        native_setVolume(volume);
        System.out.println("Native_setVolume out");
    }

    public void setPreferences(float frequencyChangeTime, float volumeChangeTime) {
        native_setPlayerPreferences(
                frequencyChangeTime,
                volumeChangeTime);
    }

    public void changeFreq(float frequency) {
        if (freq.get() <= 0) freq.set(frequency);

        System.out.println("Native setFrequency in" + frequency);
        native_setFrequency(frequency);
        System.out.println("Native setFrequency out");
    }

    public double getVolume() {
        return volume.get();
    }

    public double getFrequency() {
        return freq.get();
    }

    private void destroy() {
        System.out.println("Native destroy1 in");
        native_destroyAudioPlayer();
        System.out.println("Native destroy1 out");
        System.out.println("Native destroy2 in");
        native_destroyEngine();
        System.out.println("Native destroy2 out");
    }

}

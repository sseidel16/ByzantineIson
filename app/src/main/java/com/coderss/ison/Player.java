package com.coderss.ison;

import android.content.Context;
import android.media.AudioFormat;
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
    private static native AudioTrack native_createAudioPlayer(int frameRate,
                                                              int framesPerBuffer,
                                                              int numBuffers,
                                                              short[][] soundDataArray,
                                                              float[] frequencyArray);
    private static native void native_destroyAudioPlayer();
    private static native void native_setFrequency(float frequency);
    private static native void native_setVolume(float volume);
    private static native void native_setWorkCycles(int workCycles);
    private static native void native_setLoadStabilizationEnabled(boolean isEnabled);

    // load the native sound processor
    static {
        System.loadLibrary("native-lib");
    }

    private static final float FREQ_SHIFT_TIME = 0.05f;//in seconds

    private float MAX_FREQ_SHIFT;
    private SoundSet soundSet;
    private int minSize;

    private Speaker speaker;

    private float prefFreq;
    private float prefVolume;
    private AtomicReference<Float> freq = new AtomicReference<>();
    private AtomicReference<Float> volume = new AtomicReference<>();

    private int sampleRate = 0;
    private int framesPerBuffer = 0;

    public Player(Context context, SoundSet soundSet, float volume, float freq) {
        this.soundSet = soundSet;
        this.freq.set(freq);
        this.volume.set(volume);
        this.prefFreq = freq;
        this.prefVolume = volume;

        // calculate minimum size
        minSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        System.out.println("Grabbing audio parameters");
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (am != null) {
            String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(sampleRateStr);
            String framesPerBufferStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            framesPerBuffer = Integer.parseInt(framesPerBufferStr);
        }
        if (sampleRate == 0) sampleRate = 44100; // Use a default value if property not found
        if (framesPerBuffer == 0) framesPerBuffer = 256; // Use a default value if property not found

        short[][] soundDataArray = new short[soundSet.notes.length][];
        float[] frequencyArray = new float[soundSet.notes.length];

        for (int soundI = 0; soundI < soundDataArray.length; soundI++) {
            soundDataArray[soundI] = soundSet.notes[soundI].data;
            frequencyArray[soundI] = soundSet.notes[soundI].frequency;
        }

        System.out.println("Native create1 in");
        native_createEngine(Build.VERSION.SDK_INT);
        System.out.println("Native create1 out");
        System.out.println("Native create2 in");
        native_createAudioPlayer(
                sampleRate,
                framesPerBuffer,
                NUM_BUFFERS,
                soundDataArray,
                frequencyArray);
        System.out.println("Native create2 out");
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

    private void startJavaSpeaker() {
        speaker = new Speaker();
        speaker.start();
    }

    public class Speaker extends Thread {

        private final float MAX_VOL_SHIFT = 0.00005f;

        private short[] data;
        private AudioTrack track;

        private Runnable checkPrefs;

        private Speaker() {

            checkPrefs = () -> {
                if (Math.abs(freq.get() - prefFreq) < MAX_FREQ_SHIFT) {
                    freq.set(prefFreq);
                } else if (freq.get() > prefFreq) {
                    freq.set(freq.get() - MAX_FREQ_SHIFT);
                } else if (freq.get() < prefFreq) {
                    freq.set(freq.get() + MAX_FREQ_SHIFT);
                }
                if (Math.abs(volume.get() - prefVolume) < MAX_VOL_SHIFT) {
                    volume.set(prefVolume);
                } else if (volume.get() > prefVolume) {
                    volume.set(volume.get() - MAX_VOL_SHIFT);
                } else if (volume.get() < prefVolume) {
                    volume.set(volume.get() + MAX_VOL_SHIFT);
                }
            };
        }

        public void run() {
            System.out.println("Playing sound at frequency: " + freq);

            data = new short[minSize / 2];
            track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minSize, AudioTrack.MODE_STREAM);
            track.play();
            while (!interrupted()) {
                soundSet.fillSoundBuffer(data, freq, volume, checkPrefs);
                track.write(data, 0, data.length);
            }
            track.stop();
            track.release();
        }
    }

    public void playFreq(float f) {//Play this frequency
		changeFreq(f);
		changeVolume(1);
    }

    public void changeVolume(float volume) {
        prefVolume = volume;

        System.out.println("Native_setVolume in: " + volume);
        native_setVolume(volume);
        System.out.println("Native_setVolume out");
    }

    public void changeFreq(float frequency) {
        if (freq.get() <= 0) freq.set(frequency);
        prefFreq = frequency;
        MAX_FREQ_SHIFT = Math.abs(freq.get() - frequency) / (FREQ_SHIFT_TIME * 44100f);

        System.out.println("Native setFrequency in" + frequency);
        native_setFrequency(frequency);
        System.out.println("Native setFrequency out");
    }

    public double getPrefVolume() {
        return prefVolume;
    }

    public double getPrefFreq() {
        return prefFreq;
    }

    public void stop() {
        //stopJavaPlayer();
    }

    public void stopJavaPlayer() {
        if (speaker != null) speaker.interrupt();
    }

    public void destroy() {
        System.out.println("Native destroy1 in");
        native_destroyAudioPlayer();
        System.out.println("Native destroy1 out");
        System.out.println("Native destroy2 in");
        native_destroyEngine();
        System.out.println("Native destroy2 out");
    }

}

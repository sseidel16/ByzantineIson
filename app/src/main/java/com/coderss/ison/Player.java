package com.coderss.ison;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class Player {

    static final float FREQ_SHIFT_TIME = 0.05f;//in seconds
    static final float MAX_VOL_SHIFT = 0.00005f;
    float MAX_FREQ_SHIFT;
    private SoundSet soundSet;
    private float freq;
    private float volume;
    private float prefFreq;
    private float prefVolume;
    private int minSize;

    private LinkedList<Speaker> speakers;

    public Player(SoundSet soundSet, float volume, float freq) {
        this.soundSet = soundSet;
        this.freq = freq;
        this.volume = volume;
        this.prefFreq = freq;
        this.prefVolume = volume;

        speakers = new LinkedList<>();

        // calculate minimum size
        minSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private void startSpeaker() {
        while (!speakers.isEmpty()) {
            speakers.pollFirst().quiet();
        }

        Speaker speaker = new Speaker(freq, volume);
        speaker.start();
        speakers.offerLast(speaker);
    }

    public class Speaker extends Thread {

        private final float MAX_VOL_SHIFT = 0.005f;

        private short[] data;
        private AudioTrack track;
        private AtomicReference<Float> prefFreq = new AtomicReference<>();
        private AtomicReference<Float> prefVolume = new AtomicReference<>();
        private AtomicReference<Float> freq = new AtomicReference<>();
        private AtomicReference<Float> volume = new AtomicReference<>();

        private Runnable checkPrefs;

        public Speaker(float freq, float volume) {
            this.freq.set(freq);
            this.volume.set(volume);
            this.prefFreq.set(freq);
            this.prefVolume.set(volume);


            checkPrefs = () -> {
                if (Math.abs(this.freq.get() - this.prefFreq.get()) < MAX_FREQ_SHIFT) {
                    this.freq.set(this.prefFreq.get());
                } else if (this.freq.get() > this.prefFreq.get()) {
                    this.freq.set(this.freq.get() - MAX_FREQ_SHIFT);
                } else if (this.freq.get() < this.prefFreq.get()) {
                    this.freq.set(this.freq.get() + MAX_FREQ_SHIFT);
                }
                if (Math.abs(this.volume.get() - this.prefVolume.get()) < MAX_VOL_SHIFT) {
                    this.volume.set(this.prefVolume.get());
                } else if (this.volume.get() > this.prefVolume.get()) {
                    this.volume.set(this.volume.get() - MAX_VOL_SHIFT);
                } else if (this.volume.get() < this.prefVolume.get()) {
                    this.volume.set(this.volume.get() + MAX_VOL_SHIFT);
                }
            };
        }

        private void quiet() {
            prefVolume.set(0f);
        }

        private void kill() {
            interrupt();
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
            while (!interrupted() && volume.get() > 0) {
                soundSet.fillSoundBuffer(data, freq, volume, checkPrefs);
                track.write(data, 0, data.length);
            }
            track.stop();
            track.release();
        }
    }

    public void playFreq(float f) {//Play this frequency
        volume = 1f;
        prefVolume = 1f;
        freq = f;
        prefFreq = f;
        startSpeaker();
//		changeFreq(f);
//		changeVolume(1.0);
    }

    public void changeVolume(float volume) {
        prefVolume = volume;
    }

    public void changeFreq(float f) {
        System.out.println("Changing to " + f);
        if (freq == 0.0) freq = f;
        prefFreq = f;
        MAX_FREQ_SHIFT = Math.abs(freq - f) / (FREQ_SHIFT_TIME * 44100f);
    }

    public double getPrefVolume() {
        return prefVolume;
    }

    public double getPrefFreq() {
        return prefFreq;
    }

    public void stop(boolean kill) {
        while (!speakers.isEmpty()) {
            if (kill) {
                speakers.pollFirst().kill();
            } else {
                speakers.pollFirst().quiet();
            }
        }
    }

}

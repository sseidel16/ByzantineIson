package com.coderss.ison;

import com.coderss.ison.SoundSetLoader.Loader;

import android.content.res.AssetManager;

public class Sound {

    double frequency;
    short[] data;
    int startIndex = 0;

    public Sound(double frequency, String path, AssetManager assets, Loader parent) {
        this.frequency = frequency;
        data = Reader.readWAV(path, assets, parent);
    }

    public double getDataAt(double windowIndex, short preSample) {//index is from startIndex
        if (windowIndex < 0.0) {
            return linearalize(preSample, data[startIndex], windowIndex + 1.0);
        } else {
            int x_a = startIndex + (int)Math.floor(windowIndex);
            int x_b = x_a + 1;
            return linearalize(data[x_a], data[x_b], windowIndex - Math.floor(windowIndex));
        }
    }

    //startingPoint is usually negative
    public void fillNextBuffer(SoundSet soundSet, double frequency, double volume) {
        //set the finalIndex of the connectable window
        int finalIndex = 0;
        boolean endOfBufferReached = false;
        boolean positive = true;
        for (int i = startIndex; true; ++i) {
            if (i == data.length) {
                endOfBufferReached = true;
                finalIndex = i - 1;
                break;
            }
            if (data[i] < 0) positive = false;
            else if (!positive) {
                //we did not reach the end of the buffer
                finalIndex = i - 1;
                break;
            }
        }
        //Ok, now scale and fill soundSet buffer
        int index = 0;
        //index of scaled soundSet buffer - always starts at 0 and increments by 1
        double scaledIncrement = frequency / this.frequency;
        //for every 1 increment of soundSet buffer, sound buffer (data) will increment by scaledIncrement
        double scaledIndex = soundSet.startingPoint;
        while (true) {
            if (startIndex + scaledIndex >= finalIndex) {
                break;
            }
            soundSet.scaledBuffer[index] =
                    (short)(volume * getDataAt(scaledIndex, soundSet.preSample));
            scaledIndex += scaledIncrement;
            //soundSet.queue.offer(
            //		(short)(Player.volume * getDataAt(scaledIndex, soundSet.preSample)));
            ++index;
        }
        soundSet.startingPoint = (startIndex + scaledIndex) - (finalIndex + 1);
        soundSet.scaledBufferSize = index;
        soundSet.preSample = data[finalIndex];
        if (endOfBufferReached) startIndex = 0;
        else startIndex = finalIndex + 1;
    }

    public static double linearalize(short y_a, short y_b, double x) {
        return y_a + (x * (y_b - y_a));
    }

    public void destroy() {
        data = null;
    }

}

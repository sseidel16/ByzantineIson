package com.coderss.ison;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

public class Player {
	
	static final double FREQ_SHIFT_TIME = 0.05;//in seconds
	static final double MAX_VOL_SHIFT = 0.00005;
	double MAX_FREQ_SHIFT;
	SoundSet soundSet;
	double freq;
	double volume;
	double prefFreq;
	double prefVolume;
	Speaker speaker;
	
	public Player(SoundSet soundSet, double volume, double freq) {
		this.soundSet = soundSet;
		this.freq = freq;
		this.volume = volume;
		this.prefFreq = freq;
		this.prefVolume = volume;
	}
	
	public void start() {
		speaker = new Speaker();
		speaker.execute();
	}
	
	public class Speaker extends AsyncTask<Void, Void, Void> {
		
		short[] data;
		AudioTrack track;
		int minSize;

		@Override
		protected Void doInBackground(Void... params) {
			minSize = AudioTrack.getMinBufferSize(44100,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			data = new short[minSize / 2];
			track = new AudioTrack(
					AudioManager.STREAM_MUSIC,
					44100,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					minSize, AudioTrack.MODE_STREAM);
			track.play();
			while (true) {
				if (isCancelled()) break;
				soundSet.fillSoundBuffer(data, Player.this);
				track.write(data, 0, data.length);
			}
			track.stop();
			return null;
		}
	}
	
	public void playFreq(double f) {//Play this frequency
		changeFreq(f);
		changeVolume(1.0);
	}
	
	public void changeVolume(double volume) {
		prefVolume = volume;
	}
	
	public void changeFreq(double f) {
		System.out.println("Changing to " + f);
		if (freq == 0.0) freq = f;
		prefFreq = f;
		MAX_FREQ_SHIFT = Math.abs(freq - f) / (FREQ_SHIFT_TIME * 44100.0);
	}
	
	public void getCloserToFreq() {
		if (Math.abs(freq - prefFreq) < MAX_FREQ_SHIFT) {
			freq = prefFreq;
		} else if (freq > prefFreq) {
			freq -= MAX_FREQ_SHIFT;
		} else if (freq < prefFreq) {
			freq += MAX_FREQ_SHIFT;
		}
		if (Math.abs(volume - prefVolume) < MAX_VOL_SHIFT) {
			volume = prefVolume;
		} else if (volume > prefVolume) {
			volume -= MAX_VOL_SHIFT;
		} else if (volume < prefVolume) {
			volume += MAX_VOL_SHIFT;
		}
	}

}

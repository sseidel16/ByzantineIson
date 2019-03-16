package com.coderss.ison;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;

public class SoundSetLoader extends Activity {

    ProgressBar bar1;
    ProgressBar bar2;
    AssetManager assets;
    Loader loader;
    static int currentIndex;//used

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound_loader);
        setUpComponents();
        assets = getResources().getAssets();
    }

    protected void onStart() {
        super.onStart();
        loader = new Loader();
        loader.execute(currentIndex);
    }

    protected void onStop() {
        super.onStop();
        loader.cancel(true);
    }

    public void setUpComponents() {
        bar1 = this.findViewById(R.id.progressBar1);
        bar2 = this.findViewById(R.id.progressBar2);
    }

    public class Loader extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                SoundSet soundSet = new SoundSet(params[0], assets, this);
                if (isCancelled()) {
                    soundSet.destroy();
                    return -1;
                } else {
                    if (IsonActivity.soundSet != null) IsonActivity.soundSet.destroy();
                    IsonActivity.soundSet = soundSet;
                    Intent intent = new Intent(SoundSetLoader.this, IsonActivity.class);
                    SoundSetLoader.this.startActivity(intent);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setPrimaryProgress(double progress) {
            if (progress >= bar1.getProgress() + 5 || progress == 0.0)
                bar1.setProgress((int)progress);
        }

        public void setSecondaryProgress(double progress) {
            if (progress >= bar2.getProgress() + 5 || progress == 0.0)
                bar2.setProgress((int)progress);
        }

    }

}
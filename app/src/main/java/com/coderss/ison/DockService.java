package com.coderss.ison;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DockService extends Service {

	float initialWindowX;
	float initialTouchX;

	Button[] button;
	Button halt;
	Button back;
	Player player;
	double[] frequencies;
	int currentScaleIndex;
	double base;
	int note;
	ArrayList<Scale> scales;
	LinearLayout parentDockLayout;
	ScrollView scroller;
	LinearLayout layout;
	WindowManager wm;

	WindowManager.LayoutParams dockParams;
	
	LinearLayout parentCloseLayout;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (IsonActivity.soundSet == null) {
			loadSoundSet(0);
			stopSelf();
			return Service.START_NOT_STICKY;
		}
		player = new Player(IsonActivity.soundSet, 0, 0);
		scales = Scale.loadScales(this);
        currentScaleIndex = intent.getIntExtra("com.coderss.ison.currentScaleIndex", 0);
    	base = intent.getDoubleExtra("com.coderss.ison.base", 261.6);
    	note = -1;//no note pressed
		setScale(currentScaleIndex);
		wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		dockParams = new WindowManager.LayoutParams(
						 WindowManager.LayoutParams.WRAP_CONTENT,
						 WindowManager.LayoutParams.WRAP_CONTENT,
						 WindowManager.LayoutParams.TYPE_PHONE,
						 WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
						 PixelFormat.RGBA_8888);
		dockParams.gravity = Gravity.LEFT | Gravity.TOP;
		dockParams.x = 0;
		dockParams.y = 0;
		parentDockLayout = new LinearLayout(this);
		parentDockLayout.setOrientation(LinearLayout.VERTICAL);
			TextView move = new TextView(this);
			move.setText("<<>>");
			move.setTextSize(metrics.densityDpi / 6);
			move.setGravity(Gravity.CENTER_HORIZONTAL);
		parentDockLayout.addView(move);
			scroller = new ScrollView(this);
				layout = new LinearLayout(this);
				layout.setOrientation(LinearLayout.VERTICAL);
				setUpButtons(layout);
			scroller.setVerticalScrollBarEnabled(true);
			scroller.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			scroller.addView(layout);
		parentDockLayout.addView(scroller);
		parentDockLayout.setBackgroundColor(0xFFFFFFFF);
		parentDockLayout.setOnTouchListener(new View.OnTouchListener() {
			boolean closeWindowOnDrop = false;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//scroller.invalidate();
				// TODO Auto-generated method stub
				DisplayMetrics metrics = new DisplayMetrics();
				wm.getDefaultDisplay().getMetrics(metrics);
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					initialWindowX = dockParams.x;
					initialTouchX = event.getRawX();
//					dockParams.gravity = Gravity.LEFT | Gravity.TOP;
					dockParams.x = (int)(initialWindowX + (event.getRawX() - initialTouchX));
					dockParams.alpha = 1.0f;
					closeWindowOnDrop = false;
					wm.updateViewLayout(parentDockLayout, dockParams);
				} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
					if (event.getRawY() < metrics.heightPixels / 2) {
//						dockParams.gravity = Gravity.LEFT | Gravity.TOP;
						dockParams.x = (int)(initialWindowX + (event.getRawX() - initialTouchX));
						dockParams.alpha = 1.0f;
						closeWindowOnDrop = false;
						wm.updateViewLayout(parentDockLayout, dockParams);
					} else {
//						dockParams.gravity = Gravity.LEFT | Gravity.TOP;
//						dockParams.x = (int)(initialWindowX + (event.getRawX() - initialTouchX));
						dockParams.alpha = (metrics.heightPixels - event.getRawY()) / (metrics.heightPixels * (2.0f / 3.0f));
						closeWindowOnDrop = true;
						wm.updateViewLayout(parentDockLayout, dockParams);
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					if (closeWindowOnDrop)  {
						DockService.this.stopSelf();
					} else {
						dockParams.height = LayoutParams.WRAP_CONTENT;
						wm.updateViewLayout(parentDockLayout, dockParams);
					}
				}
				return false;
			}
		});
		wm.addView(parentDockLayout, dockParams);
		return Service.START_NOT_STICKY;
	}
	
	public void setUpButtons(LinearLayout layout) {
		button = new Button[Scale.TOTAL_KEYS];
		//layout.removeAllViews();
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		for (int y = 0; y < Scale.TOTAL_KEYS; ++y) {
			int realY = (Scale.TOTAL_KEYS - y) - 1;
			button[realY] = new Button(this);
			button[realY].setId(realY);
			button[realY].setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
			button[realY].setLayoutParams(params);
			button[realY].setOnClickListener(new Button.OnClickListener() {

				@Override
				public void onClick(View v) {
					buttonPressed(v.getId());
				}
				
			});
			layout.addView(button[y]);
		}
		setButtonText();
		addButtonColorFilter();
		halt = (Button)new Button(this);
		halt.setText("Stop");
		setHaltButtonText();
		halt.setOnClickListener(arg0 -> {
			if (player.getPrefVolume() > 0.0) {
				buttonPressed(-1);
			} else {
				DockService.this.stopSelf();
			}
		});
		layout.addView(halt);
		back = (Button)new Button(this);
		back.setText("Return");
		back.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getBaseContext(), IsonActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplication().startActivity(intent);
				stopSelf();
			}
		});
		layout.addView(back);
	}
	
//	@Override
//	public void onSaveInstanceState(Bundle savedInstanceState) {
//		super.onSaveInstanceState(savedInstanceState);
//		savedInstanceState.putDouble("Volume", player.volume);
//		savedInstanceState.putDouble("Frequency", player.freq);
//		savedInstanceState.putInt("Note", note);
//		savedInstanceState.putDouble("Base", base);
//		System.out.println("Saving");
//	}
	
	public void onDestroy() {
		super.onDestroy();
		player.stop();
		wm.removeView(parentDockLayout);
	}
	
	public void setScale(int pick) {
		currentScaleIndex = pick;
		frequencies = new double[scales.get(pick).notes.length];
		for (int i = 0; i < frequencies.length; ++i) {
			frequencies[i] = base *
					Math.pow(Math.pow(2.0, 1.0/scales.get(pick).totalSteps),
							(double)scales.get(pick).notes[i]);
		}
		player.changeFreq((float)getFrequency());
	}
	
	public void buttonPressed(int index) {
		removeButtonColorFilter();
		note = index;
		if (index != -1) {
			player.playFreq((float)getFrequency());
		} else {
			player.changeVolume(0);
		}
		addButtonColorFilter();
		setHaltButtonText();
	}
	
	public void setHaltButtonText() {
		if (player.getPrefVolume() == 0.0) {
		    halt.setText("Exit");
        } else {
		    halt.setText("Stop");
        }
	}
	
	public void removeButtonColorFilter() {
		if (note != -1) {
			button[note].getBackground().clearColorFilter();
			button[note].invalidate();
		}
	}
	
	public void addButtonColorFilter() {
		if (note != -1) {
			button[note].getBackground().setColorFilter(0x88333333, PorterDuff.Mode.DARKEN);
		}
	}
	
	public void setButtonText() {
		int currentNote = Scale.correctZeroToSix(
				scales.get(currentScaleIndex).baseNote + Scale.BASE_NOTE_INDEX);
		for (int i = 0; i <Scale.TOTAL_KEYS; ++i) {
			if (i == Scale.BASE_NOTE_INDEX)
				button[i].setText("<" + Scale.noteNames[currentNote] + ">");
			else
				button[i].setText(Scale.noteNames[currentNote]);
			currentNote = Scale.correctZeroToSix(currentNote - 1);
		}
	}
	
	protected void loadSoundSet(int index) {
		SoundSetLoader.currentIndex = index;
		Intent intent = new Intent(this.getApplicationContext(), SoundSetLoader.class);
		startActivity(intent);
	}
	
	public double getFrequency() {
		if (note == -1) return frequencies[0];
		else return frequencies[note];
	}

}

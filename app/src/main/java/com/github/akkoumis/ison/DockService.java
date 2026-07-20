package com.github.akkoumis.ison;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import com.github.akkoumis.ison.utility.Player;
import com.github.akkoumis.ison.utility.Preferences;
import com.github.akkoumis.ison.utility.Scale;

import androidx.preference.PreferenceManager;

public class DockService extends Service {

    private float initialWindowX;
    private float initialTouchX;

    private Button[] button;
    private Player player;
    private double[] frequencies;
    private int currentScaleIndex;
    private double base;
    private int note;
    private ArrayList<Scale> scales;
    private LinearLayout parentDockLayout;
    private WindowManager wm;

    private WindowManager.LayoutParams dockParams;

    private Preferences preferences;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    public int onStartCommand(Intent intent, int flags, int startId) {
        preferences = new Preferences(getBaseContext());
        int notesBelow = preferences.getNotesBelow();
        int totalNotes = notesBelow + 1 + preferences.getNotesAbove();

        player = new Player(this);

        scales = Scale.loadScales(this);
        currentScaleIndex = intent.getIntExtra("com.github.akkoumis.ison.currentScaleIndex", 0);
        base = intent.getDoubleExtra("com.github.akkoumis.ison.base", 261.6);
        note = intent.getIntExtra("com.github.akkoumis.ison.note", -1);

        setScale(currentScaleIndex, totalNotes, notesBelow);

        wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        dockParams = new WindowManager.LayoutParams(
                (int)(getResources().getDisplayMetrics().densityDpi * preferences.getDockWidth()),
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888);

        dockParams.gravity = Gravity.START | Gravity.TOP;
        dockParams.x = 0;
        dockParams.y = 0;

        parentDockLayout = new LinearLayout(this);
        parentDockLayout.setOrientation(LinearLayout.VERTICAL);

        int handleHeight = (int)(getResources().getDisplayMetrics().densityDpi * 0.25);
        FrameLayout handleContainer = new FrameLayout(this);
        handleContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, handleHeight));
        handleContainer.setBackgroundColor(0xFFCCCCCC); // Light gray bar

        View handleIndicator = new View(this);
        int indicatorWidth = (int)(getResources().getDisplayMetrics().densityDpi * 0.4);
        int indicatorHeight = (int)(getResources().getDisplayMetrics().densityDpi * 0.03);
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
                indicatorWidth, indicatorHeight, Gravity.CENTER);
        handleIndicator.setLayoutParams(indicatorParams);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(indicatorHeight / 2.0f);
        shape.setColor(0xFF888888); // Darker gray handle
        handleIndicator.setBackground(shape);

        handleContainer.addView(handleIndicator);
        parentDockLayout.addView(handleContainer);

        ScrollView scroller = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        setUpButtons(layout, totalNotes, notesBelow);
        scroller.setVerticalScrollBarEnabled(true);
        scroller.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        scroller.addView(layout);
        parentDockLayout.addView(scroller);

        parentDockLayout.setBackgroundColor(0xFFFFFFFF);

        parentDockLayout.setOnTouchListener(new View.OnTouchListener() {
            boolean closeWindowOnDrop = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //scroller.invalidate();
                // TODO Auto-generated method stub
                int heightPixels = getScreenHeight();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    initialWindowX = dockParams.x;
                    initialTouchX = event.getRawX();
                    dockParams.x = (int)(initialWindowX + (event.getRawX() - initialTouchX));
                    dockParams.alpha = 1.0f;
                    closeWindowOnDrop = false;
                    wm.updateViewLayout(parentDockLayout, dockParams);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getRawY() < heightPixels / 2) {
                        dockParams.x = (int)(initialWindowX + (event.getRawX() - initialTouchX));
                        dockParams.alpha = 1.0f;
                        closeWindowOnDrop = false;
                        wm.updateViewLayout(parentDockLayout, dockParams);
                    } else {
                        dockParams.alpha = (heightPixels - event.getRawY()) / (heightPixels * (2.0f / 3.0f));
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
        player.start();
        wm.addView(parentDockLayout, dockParams);
        return Service.START_NOT_STICKY;
    }

    public void setUpButtons(LinearLayout layout, int totalNotes, int notesBelow) {

        button = new Button[totalNotes];
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        boolean isTopToBottom = preferences.isTopToBottom();
        for (int buttonI = 0; buttonI < totalNotes; buttonI++) {

            int realY;
            if (isTopToBottom) {
                realY = buttonI;
            } else {
                realY = (totalNotes - buttonI) - 1;
            }

            button[realY] = new Button(this);
            button[realY].setId(realY);
            button[realY].setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
            button[realY].setLayoutParams(params);
            button[realY].setOnClickListener(v -> buttonPressed(v.getId()));
            layout.addView(button[realY]);
        }
        setButtonText(totalNotes, notesBelow);
        addButtonColorFilter();

        Button halt = new Button(this);
        halt.setText("Stop");
        halt.setOnClickListener(arg0 -> buttonPressed(-1));
        layout.addView(halt);

        Button back = new Button(this);
        back.setText("Return");
        back.setOnClickListener(arg0 -> {
            Intent intent = new Intent(getBaseContext(), IsonActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("com.github.akkoumis.ison.currentScaleIndex", currentScaleIndex);
            intent.putExtra("com.github.akkoumis.ison.base", base);
            intent.putExtra("com.github.akkoumis.ison.note", note);
            startActivity(intent);
            stopSelf();
        });
        layout.addView(back);

        Button exit = new Button(this);
        exit.setText("Exit");
        exit.setOnClickListener(arg0 -> stopSelf());
        layout.addView(exit);
    }

    public void onDestroy() {
        super.onDestroy();
        wm.removeView(parentDockLayout);
    }

    public void setScale(int pick, int totalNotes, int notesBelow) {
        currentScaleIndex = pick;

        int notes[] = scales.get(currentScaleIndex).getNotes(totalNotes, notesBelow);
        frequencies = new double[notes.length];
        for (int i = 0; i < frequencies.length; ++i) {
            frequencies[i] = base *
                    Math.pow(Math.pow(2.0, 1.0 / scales.get(pick).totalSteps), notes[i]);
        }
        player.changeFreq((float)getFrequency());
    }

    public void buttonPressed(int index) {
        removeButtonColorFilter();
        note = index;
        if (index != -1) {
            player.setFrequency((float)getFrequency());
        } else {
            player.setVolume(0);
        }
        addButtonColorFilter();
    }

    public void removeButtonColorFilter() {
        if (note != -1) {
            button[note].getBackground().clearColorFilter();
            button[note].invalidate();
        }
    }

    public void addButtonColorFilter() {
        if (note != -1) {
            button[note].getBackground().setColorFilter(new PorterDuffColorFilter(0x88333333, PorterDuff.Mode.DARKEN));
        }
    }

    private int getScreenHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            return bounds.height();
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            return metrics.heightPixels;
        }
    }

    public void setButtonText(int totalNotes, int notesBelow) {
        int currentNote = Scale.correctZeroToSix(
                scales.get(currentScaleIndex).baseNote - notesBelow);
        for (int buttonI = 0; buttonI < totalNotes; buttonI++) {
            if (buttonI == notesBelow) {
                button[buttonI].setText("<" + Scale.NOTE_NAMES[currentNote] + ">");
            } else {
                button[buttonI].setText(Scale.NOTE_NAMES[currentNote]);
            }
            currentNote = Scale.correctZeroToSix(currentNote + 1);
        }
    }

    public double getFrequency() {
        if (note == -1) return frequencies[0];
        else return frequencies[note];
    }

}

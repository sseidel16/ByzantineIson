package com.coderss.ison;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class IsonActivity extends AppCompatActivity {

    //SoundSet instance initialized by SoundSetLoader
    static SoundSet soundSet;

    //Statics set by AppSettings
    static int OFFSET = 0;
    static int COLUMNS = 1;
    static int ROWS = 13;
    static float BUTTON_HEIGHT = 5f / 16f;//fraction of an inch
    static boolean TOP_TO_BOTTOM = false;
    static boolean LEFT_TO_RIGHT = true;
    static boolean BY_ROW = false;

    //Button array referring to the different note buttons
    private Button[] button;

    //Button instance referring to the Stop/Select Sound button
    private Button halt;

    //TextView that is updated to show frequency
    private TextView frequency;

    //Player instance controlling sound output
    private Player player;

    //array of current frequencies
    private double[] frequencies;

    //current scale that is selected
    private int currentScaleIndex;

    //current base note frequency
    private double base;

    //current note being pressed, -1 if not is being pressed
    private int note;

    //all scale objects, previously vector
    ArrayList<Scale> scales;

    // application preferences
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //this is called when the view/activity is loaded
        super.onCreate(savedInstanceState);

        // ensure we have a sound set
        if (soundSet == null) {
            // this will change activities to load sound set and destroy the current one
            loadSoundSet(this, 0);
            return;
        }

        if (savedInstanceState != null) {
            // if we are resuming the app, then resume the previous state
            double volume = savedInstanceState.getDouble("Volume");
            double freq = savedInstanceState.getDouble("Frequency");
            base = savedInstanceState.getDouble("Base");
            note = savedInstanceState.getInt("Note");
            player = new Player(this, soundSet, (float)volume, (float)freq); //create Player
        } else {
            note = -1;			//no button pressed at first
            base = 261.6;		//default base is 261.6
            player = new Player(this, soundSet, 0, 0); //create player
        }
        scales = Scale.loadScales(this); //load all scales

        // load shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // send preferences to player
        float frequencyChangeTime, volumeChangeTime;
        try {
            frequencyChangeTime =
                    Float.parseFloat(sharedPreferences.getString("listFrequencyChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            frequencyChangeTime = 0;
        }
        try {
            volumeChangeTime =
                    Float.parseFloat(sharedPreferences.getString("listVolumeChangeTime", "0.0"));
        } catch (Throwable th) {
            th.printStackTrace();
            volumeChangeTime = 0;
        }
        player.setPreferences(frequencyChangeTime, volumeChangeTime);

        setScale(0); //set the current scale to Diatonic (index 0)
        //the setScale method is defined below

        setContentView(R.layout.activity_main);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        //this code saves the app state.
        //For example, if the app is closed it saves the note and base frequency
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putDouble("Volume", player.getVolume());
        savedInstanceState.putDouble("Frequency", player.getFrequency());
        savedInstanceState.putInt("Note", note);
        savedInstanceState.putDouble("Base", base);
        System.out.println("Saving");
    }

    protected void onStart() {
        //this is called when the activity/view actually shows on the screen
        super.onStart();
        //stop the dock in case it is running
        stopService(new Intent(IsonActivity.this, DockService.class));

        //start the audio stream.
        //The volume will just be zero until somebody selects a note to be played
        player.start();

        //Add all the note buttons to the screen
        setUpComponents(); //this method is defined later in this file
    }

    public void onStop() {
        //this is called when the view/activity stops showing on the screen
        super.onStop();

        //this stops any note that is playing
        player.stop();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        //dont worry about this method
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //this is called if somebody touches the Dock button
        if (item.getItemId() == R.id.openDock) {
            Intent dockIntent =
                    new Intent(IsonActivity.this, DockService.class);
            dockIntent.putExtra("com.coderss.ison.currentScaleIndex", currentScaleIndex);
            dockIntent.putExtra("com.coderss.ison.base", base);
            dockIntent.putExtra("com.coderss.ison.note", note);
            startService(dockIntent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.openLayoutSettings) {
            Intent intent = new Intent(getApplicationContext(), AppSettings.class);
            startActivity(intent);
            finish();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    public void setUpComponents() {

        button = new Button[Scale.TOTAL_KEYS];
        TableLayout buttonTable = this.findViewById(R.id.buttonTable);
        buttonTable.removeAllViews();
        TableRow.LayoutParams buttonParams = new TableRow.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                (int)(this.getResources().getDisplayMetrics().densityDpi * BUTTON_HEIGHT), 1);

        for (int i = 0; i < Scale.TOTAL_KEYS; ++i) {
            button[i] = new AppCompatButton(this) {
                public boolean performClick() {
                    buttonPressed(getId());
                    return super.performClick();
                }

                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        performClick();
                        return true;
                    }
                    return false;
                }
            };
        }
        for (int y = 0; y < ROWS; ++y) {
            TableRow row = new TableRow(this);
            for (int x = 0; x < COLUMNS; ++x) {
                int index;

                int realX, realY;
                if (LEFT_TO_RIGHT) realX = x;
                else realX = (COLUMNS - 1) - x;
                if (TOP_TO_BOTTOM) realY = y;
                else realY = (ROWS - 1) - y;

                if (BY_ROW) {
                    index = (realY * COLUMNS) + realX - OFFSET;
                } else {
                    index = (realX * ROWS) + realY - OFFSET;
                }

                if (index >= 0 && index < Scale.TOTAL_KEYS) {
                    button[index].setId(index);
                    button[index].setTypeface(Typeface.createFromAsset(getResources().getAssets(), "greek.ttf"));
                    button[index].setLayoutParams(buttonParams);
                    row.addView(button[index]);
                } else {
                    TextView dummy = new TextView(this);
                    dummy.setLayoutParams(buttonParams);
                    row.addView(dummy);
                }
            }
            buttonTable.addView(row);
        }

        setButtonText();
        addButtonColorFilter();
        halt = this.findViewById(R.id.halt);
        setHaltButtonText();
        halt.setOnClickListener(arg0 -> {
            if (player.getVolume() > 0.0) {
                buttonPressed(-1);
            } else {
                SoundPicker dialog = new SoundPicker();
                Bundle args = new Bundle();
                args.putInt("sound_set_index", soundSet.soundSetIndex);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "dialog");
            }
        });

        double[] bases = {
                196.00, 207.65, 220.00, 233.08, 246.94,
                261.63, 277.18, 293.66, 311.13, 329.63};
        boolean isBaseNoteSliderDiscrete =
                sharedPreferences.getString("listBaseSlider", "discrete").equals("discrete");

        SeekBar baseNoteSlider = this.findViewById(R.id.seekBar1);

        if (isBaseNoteSliderDiscrete) {
            baseNoteSlider.setMax(bases.length - 1);
            baseNoteSlider.setProgress(5);
        } else {
            baseNoteSlider.setMax(100);
            baseNoteSlider.setProgress(50);
        }

        baseNoteSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (isBaseNoteSliderDiscrete) {
                    base = bases[progress];
                } else {
                    base = bases[0] + (bases[bases.length - 1] - bases[0]) * progress / seekBar.getMax();
                }

                setScale(currentScaleIndex);
                player.changeFreq((float)getFrequency());
                setFrequencyText();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });

        String[] scaleStrings = new String[scales.size() + 1];
        scaleStrings[0] = "Manage Scales";
        for (int index = 1; index < scaleStrings.length; ++index) {
            scaleStrings[index] = scales.get(index - 1).name;
        }
        Spinner j = this.findViewById(R.id.numberOfColumns);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                scaleStrings) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                return view;
            }
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getDropDownView(position, convertView, parent);
                System.out.println(position + "," + view.getText());
                if (position == 0) view.setTypeface(Typeface.DEFAULT_BOLD);
                else view.setTypeface(Typeface.DEFAULT);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        j.setAdapter(adapter);
        j.setSelection(1);
        j.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                if (arg2 == 0) {
                    Intent intent = new Intent(IsonActivity.this, ScaleManager.class);
                    IsonActivity.this.startActivity(intent);
                    finish();
                } else {
                    setScale(arg2 - 1);
                    setButtonText();
                    setFrequencyText();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });
        frequency = this.findViewById(R.id.textView4);
        setFrequencyText();
    }

    public static class SoundPicker extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setSingleChoiceItems(
                    SoundSet.getSoundSets(getResources().getAssets()),
                    getArguments().getInt("sound_set_index"),
                    (dialog, which) -> {
                        loadSoundSet(getActivity(), which);
                    }
            );
            return builder.create();
        }
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
            player.setFrequency((float)getFrequency());
        } else {
            player.setVolume(0);
        }
        addButtonColorFilter();
        setHaltButtonText();
        setFrequencyText();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.moveTaskToBack(true);
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    public void setHaltButtonText() {
        if (player.getVolume() == 0.0) {
            halt.setText("Select Sound");
        } else {
            halt.setText("Stop");
        }
    }

    public void setFrequencyText() {
        if (player.getVolume() == 0.0) {
            frequency.setText("Frequency");
        } else {
            DecimalFormat format = new DecimalFormat("###.#Hz");
            String formatted = format.format(player.getFrequency());
            frequency.setText(formatted);
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
        int currentNoteIndex = Scale.correctZeroToSix(
                scales.get(currentScaleIndex).baseNote - Scale.BASE_NOTE_INDEX);
        //current note is a number 0 to 6
        //corresponding to the current note name
        for (int i = 0; i < Scale.TOTAL_KEYS; ++i) {
            if (i == Scale.BASE_NOTE_INDEX)
                button[i].setText("<" + Scale.noteNames[currentNoteIndex] + ">");
            else
                button[i].setText(Scale.noteNames[currentNoteIndex]);
            currentNoteIndex = Scale.correctZeroToSix(currentNoteIndex + 1);
        }
    }

    protected static void loadSoundSet(Activity activity, int index) {
        SoundSetLoader.currentIndex = index;
        Intent intent = new Intent(activity.getApplicationContext(), SoundSetLoader.class);
        activity.startActivity(intent);
        activity.finish();
    }

    public double getFrequency() {
        if (note == -1) return frequencies[0];
        else return frequencies[note];
    }

    public void onDestroy() {
        super.onDestroy();

        //if (player != null) {
        //    player.destroy();
        //}
    }

}

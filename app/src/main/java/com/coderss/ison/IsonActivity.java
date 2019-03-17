package com.coderss.ison;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
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

import com.coderss.ison.utility.Player;
import com.coderss.ison.utility.Preferences;
import com.coderss.ison.utility.Scale;

import static android.widget.LinearLayout.VERTICAL;

public class IsonActivity extends AppCompatActivity {

    //Button array referring to the different note buttons
    private Button[] button;

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
    private ArrayList<Scale> scales;

    // application preferences
    private Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        //this is called when the view/activity is loaded
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // if we are resuming the app, then resume the previous state
            base = savedInstanceState.getDouble("Base");
            note = savedInstanceState.getInt("Note");
            player = new Player(this); //create Player
        } else {
            Intent intent = getIntent();
            currentScaleIndex = intent.getIntExtra("com.coderss.ison.currentScaleIndex", 0);
            base = intent.getDoubleExtra("com.coderss.ison.base", 261.6);
            note = intent.getIntExtra("com.coderss.ison.note", -1);
            player = new Player(this); //create player
        }
        scales = Scale.loadScales(this); //load all scales

        // send preferences to player
        float frequencyChangeTime = preferences.getFrequencyChangeTime();
        float volumeChangeTime = preferences.getVolumeChangeTime();
        player.setPreferences(frequencyChangeTime, volumeChangeTime);

        setScale(0); //set the current scale to Diatonic (index 0)
        //the setScale method is defined below

        preferences.initializeSoundSet(player, getAssets());

        setContentView(R.layout.activity_main);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        //this code saves the app state.
        //For example, if the app is closed it saves the note and base frequency
        super.onSaveInstanceState(savedInstanceState);
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
            Intent dockIntent = new Intent(IsonActivity.this, DockService.class);
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
        int notesBelow = preferences.getNotesBelow();
        int totalNotes = notesBelow + 1 + preferences.getNotesAbove();

        button = new Button[totalNotes];
        TableLayout buttonTable = this.findViewById(R.id.buttonTable);
        buttonTable.removeAllViews();
        TableRow.LayoutParams buttonParams = new TableRow.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                (int)(this.getResources().getDisplayMetrics().densityDpi *
                        preferences.getButtonHeight()),
                VERTICAL);

        for (int buttonI = 0; buttonI < totalNotes; buttonI++) {
            button[buttonI] = new AppCompatButton(this) {
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

        boolean isFlowHorizontal = preferences.isFlowHorizontal();
        boolean isLeftToRight = preferences.isLeftToRight();
        boolean isTopToBottom = preferences.isTopToBottom();
        int dimensionLimit = preferences.getDimensionLimit();
        int columns = (isFlowHorizontal
                ? Math.min(dimensionLimit, totalNotes)
                : (int)Math.ceil(totalNotes / (float)dimensionLimit));
        int rows = (!isFlowHorizontal
                ? Math.min(dimensionLimit, totalNotes)
                : (int)Math.ceil(totalNotes / (float)dimensionLimit));
        for (int y = 0; y < rows; ++y) {
            TableRow row = new TableRow(this);
            for (int x = 0; x < columns; ++x) {
                int index;

                int realX, realY;
                if (isLeftToRight) {
                    realX = x;
                } else {
                    realX = (columns - 1) - x;
                }

                if (isTopToBottom) {
                    realY = y;
                } else {
                    realY = (rows - 1) - y;
                }

                if (isFlowHorizontal) {
                    index = (realY * columns) + realX;
                } else {
                    index = (realX * rows) + realY;
                }

                if (index >= 0 && index < totalNotes) {
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

        setButtonText(totalNotes, notesBelow);
        addButtonColorFilter();
        Button halt = this.findViewById(R.id.halt);
        halt.setOnClickListener(arg0 -> buttonPressed(-1));

        double[] bases = {
                196.00, 207.65, 220.00, 233.08, 246.94,
                261.63, 277.18, 293.66, 311.13, 329.63};

        SeekBar baseNoteSlider = this.findViewById(R.id.seekBar1);

        boolean isBaseNoteSliderDiscrete = preferences.isBaseNoteSliderDiscrete();
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

        Spinner j = this.findViewById(R.id.scaleSpinner);
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
                    int notesBelow = preferences.getNotesBelow();
                    int totalNotes = notesBelow + 1 + preferences.getNotesAbove();

                    setScale(arg2 - 1);
                    setButtonText(totalNotes, notesBelow);
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

    public void setScale(int pick) {
        int notesBelow = preferences.getNotesBelow();
        int totalNotes = notesBelow + 1 + preferences.getNotesAbove();

        currentScaleIndex = pick;
        int[] notes = scales.get(currentScaleIndex).getNotes(totalNotes, notesBelow);

        frequencies = new double[notes.length];
        for (int noteI = 0; noteI < frequencies.length; ++noteI) {
            frequencies[noteI] = base *
                    Math.pow(Math.pow(2.0, 1.0/scales.get(pick).totalSteps),
                            (double)notes[noteI]);
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
        setFrequencyText();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.moveTaskToBack(true);
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    public void setFrequencyText() {
        if (note == -1) {
            frequency.setText("Frequency");
        } else {
            DecimalFormat format = new DecimalFormat("###.#Hz");
            String formatted = format.format(getFrequency());
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

    public void setButtonText(int totalNotes, int notesBelow) {
        int currentNoteIndex = Scale.correctZeroToSix(scales.get(currentScaleIndex).baseNote - notesBelow);

        //current note is a number 0 to 6
        //corresponding to the current note name
        for (int buttonI = 0; buttonI < totalNotes; ++buttonI) {
            if (buttonI == notesBelow)
                button[buttonI].setText("<" + Scale.NOTE_NAMES[currentNoteIndex] + ">");
            else
                button[buttonI].setText(Scale.NOTE_NAMES[currentNoteIndex]);
            currentNoteIndex = Scale.correctZeroToSix(currentNoteIndex + 1);
        }
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

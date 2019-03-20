package com.coderss.ison;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
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

    public static final double[] BASES = {196.00, 207.65, 220.00, 233.08, 246.94, 261.63, 277.18, 293.66, 311.13, 329.63};

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
        preferences = new Preferences(getBaseContext());

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        //this is called when the view/activity is loaded
        super.onCreate(savedInstanceState);

        // keep the screen on if appropriate preference is set
        if (preferences.isKeepingScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

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

        // make sure native player is off if no note is selected
        if (note == -1) player.setVolume(0);

        // load all scales
        scales = Scale.loadScales(this);

        // send preferences to player
        float frequencyChangeTime = preferences.getFrequencyChangeTime();
        float volumeChangeTime = preferences.getVolumeChangeTime();
        int blendMode = preferences.getBlendMode();
        player.setPreferences(frequencyChangeTime, volumeChangeTime, blendMode);

        setScale(0); //set the current scale to Diatonic (index 0)
        //the setScale method is defined below

        preferences.initializeSoundSet(player);

        setContentView(R.layout.activity_main);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        //this code saves the app state.
        //For example, if the app is closed it saves the note and base frequency
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("Note", note);
        savedInstanceState.putDouble("Base", base);
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
        // no need to worry about this method
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //this is called if somebody touches the Dock button
        if (item.getItemId() == R.id.openIsonDock) {

            // check if we have permission to draw overlays before opening the dock
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
                return false;
            }

            Intent dockIntent = new Intent(IsonActivity.this, DockService.class);
            dockIntent.putExtra("com.coderss.ison.currentScaleIndex", currentScaleIndex);
            dockIntent.putExtra("com.coderss.ison.base", base);
            dockIntent.putExtra("com.coderss.ison.note", note);
            startService(dockIntent);
            finish();

            return true;
        } else if (item.getItemId() == R.id.openAppSettings) {
            Intent intent = new Intent(getApplicationContext(), AppSettings.class);
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.openScaleManager) {
            Intent intent = new Intent(getApplicationContext(), ScaleManager.class);
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
                (int)(getResources().getDisplayMetrics().densityDpi *
                        preferences.getButtonHeight()),
                VERTICAL);

        for (int buttonI = 0; buttonI < totalNotes; buttonI++) {
            button[buttonI] = new AppCompatButton(this);
            button[buttonI].setOnClickListener(v -> buttonPressed(v.getId()));
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

        Button majorMinus = findViewById(R.id.minus3);
        majorMinus.setOnClickListener(v -> changeBaseFrequency(base * Math.pow(2, -1.0 / 12.0)));
        Button majorPlus = findViewById(R.id.plus3);
        majorPlus.setOnClickListener(v -> changeBaseFrequency(base * Math.pow(2, 1.0 / 12.0)));
        Button minorMinus = findViewById(R.id.minus1);
        minorMinus.setOnClickListener(v -> changeBaseFrequency(base * Math.pow(2, -1.0 / 36.0)));
        Button minorPlus = findViewById(R.id.plus1);
        minorPlus.setOnClickListener(v -> changeBaseFrequency(base * Math.pow(2, 1.0 / 36.0)));

        boolean isShowingMinorChangers = preferences.isShowingMinorChangers();
        boolean isShowingMajorChangers = preferences.isShowingMajorChangers();

        if (!isShowingMinorChangers) {
            minorMinus.setVisibility(View.GONE);
            minorPlus.setVisibility(View.GONE);
        }

        if (!isShowingMajorChangers) {
            majorMinus.setVisibility(View.GONE);
            majorPlus.setVisibility(View.GONE);
        }

        Button halt = this.findViewById(R.id.halt);
        halt.setOnClickListener(arg0 -> buttonPressed(-1));

        SeekBar baseNoteSlider = this.findViewById(R.id.seekBar1);

        boolean isBaseNoteSliderDiscrete = preferences.isBaseNoteSliderDiscrete();
        if (isBaseNoteSliderDiscrete) {
            baseNoteSlider.setMax(BASES.length - 1);
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
                    changeBaseFrequency(BASES[progress]);
                } else {
                    changeBaseFrequency(BASES[0] + (BASES[BASES.length - 1] - BASES[0]) * progress / seekBar.getMax());
                }
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

        String[] scaleStrings = new String[scales.size()];
        for (int index = 0; index < scaleStrings.length; index++) {
            scaleStrings[index] = scales.get(index).name;
        }

        Spinner scaleSpinner = this.findViewById(R.id.scaleSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, scaleStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scaleSpinner.setAdapter(adapter);
        scaleSpinner.setSelection(0);
        scaleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                int notesBelow = preferences.getNotesBelow();
                int totalNotes = notesBelow + 1 + preferences.getNotesAbove();

                setScale(arg2);
                setButtonText(totalNotes, notesBelow);
                setFrequencyText();
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

    private void changeBaseFrequency(double base) {
        if (base < BASES[0]) base = BASES[0];
        else if (base > BASES[BASES.length - 1]) base = BASES[BASES.length - 1];

        this.base = base;
        setScale(currentScaleIndex);
        player.changeFreq((float)getFrequency());
        setFrequencyText();
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

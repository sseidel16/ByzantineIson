package com.coderss.ison;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.coderss.ison.utility.Preferences;
import com.coderss.ison.utility.Scale;

public class ScaleManager extends AppCompatActivity {

    private Preferences preferences;

    private int previousScale;

    private TextView[] widths;
    private Button save;

    private EditText nameBox;
    private Spinner scaleSelector;

    //changed by ChangeScaleConfirmation
    private SeekBar[] seekBar;
    private Spinner baseNoteSelector;
    private ArrayList<Scale> scales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(getBaseContext());

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        super.onCreate(savedInstanceState);
        scales = Scale.loadScales(this.getApplicationContext());

        setContentView(R.layout.scale_manager);
        setTitle("Scale Manager");
        setUpButtons();
    }

    public void onStart() {
        super.onStart();
        (this.findViewById(R.id.scrollView1)).requestFocus();
    }

    public void setUpButtons() {
        // grab the spinners and set adapters
        scaleSelector = findViewById(R.id.scaleSpinner);
        updateScaleSpinnerAdapter();

        baseNoteSelector = this.findViewById(R.id.spinner2);
        updateBaseSpinnerAdapter();

        ((TextView)this.findViewById(R.id.textViewGreek1)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek2)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek3)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek4)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek5)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek6)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
        ((TextView)this.findViewById(R.id.textViewGreek7)).
                setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));

        widths = new TextView[7];
        widths[0] = this.findViewById(R.id.width1);
        widths[1] = this.findViewById(R.id.width2);
        widths[2] = this.findViewById(R.id.width3);
        widths[3] = this.findViewById(R.id.width4);
        widths[4] = this.findViewById(R.id.width5);
        widths[5] = this.findViewById(R.id.width6);
        widths[6] = this.findViewById(R.id.width7);

        seekBar = new SeekBar[7];
        seekBar[0] = this.findViewById(R.id.seekBar1);
        seekBar[1] = this.findViewById(R.id.seekBar2);
        seekBar[2] = this.findViewById(R.id.seekBar3);
        seekBar[3] = this.findViewById(R.id.seekBar4);
        seekBar[4] = this.findViewById(R.id.seekBar5);
        seekBar[5] = this.findViewById(R.id.seekBar6);
        seekBar[6] = this.findViewById(R.id.seekBar7);

        nameBox = this.findViewById(R.id.editText1);

        save = this.findViewById(R.id.save);

        // update initial seek bar positions
        setScale(scaleSelector.getSelectedItemPosition());

        for (int i = 0; i < 7; ++i) {
            widths[i].setWidth(widths[i].getLineHeight() * 2);
            seekBar[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    updateWidthTextsFromSeekBars();

                    updateSaveVisibility(widgetsDifferFromScale(scaleSelector.getSelectedItemPosition()));
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
        }

        previousScale = -1;//scaleSelector.getSelectedItemPosition();
        scaleSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int newSelection, long arg3) {
                // make sure we are selecting a new scale before changing anything
                if (previousScale == -1) {
                    previousScale = newSelection;
                    setScale(newSelection);
                } else if (previousScale != newSelection) {
                    if (widgetsDifferFromScale(previousScale)) {
                        ChangeScaleConfirmation dialog = new ChangeScaleConfirmation();
                        Bundle args = new Bundle();
                        args.putInt("previous_scale_position", previousScale);
                        args.putInt("new_scale_position", newSelection);
                        dialog.setArguments(args);
                        dialog.show(getSupportFragmentManager(), "dialog");
                    } else {
                        setScale(newSelection);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) { }
        });

        baseNoteSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSaveVisibility(widgetsDifferFromScale(scaleSelector.getSelectedItemPosition()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        save.setOnClickListener(arg0 -> {
            int[] widths = new int[7];
            for (int i = 0; i < 7; i++) {
                widths[i] = seekBar[i].getProgress();
            }
            scales.set(scaleSelector.getSelectedItemPosition(),
                    new Scale(widths, nameBox.getText().toString(),
                            baseNoteSelector.getSelectedItemPosition()));
            Scale.writeScales(ScaleManager.this, scales);
            goBackToIsonActivity();
        });

        Button delete = this.findViewById(R.id.delete);
        delete.setOnClickListener(arg0 -> {
            DeleteConfirmation dialog = new DeleteConfirmation();
            Bundle args = new Bundle();
            args.putInt("delete_scale_position", scaleSelector.getSelectedItemPosition());
            dialog.setArguments(args);
            dialog.show(getSupportFragmentManager(),"dialog");
        });

        Button create = this.findViewById(R.id.create);
        create.setOnClickListener(arg0 -> {
            createNewScale();
            updateScaleSpinnerAdapter();
            scaleSelector.setSelection(scales.size() - 1);
        });

        updateSaveVisibility(widgetsDifferFromScale(scaleSelector.getSelectedItemPosition()));
    }

    private void updateNameBox() {
        nameBox.setText(getSelectedScale().name);
    }

    private void updateSeekBarsFromScale() {
        Scale scale = getSelectedScale();
        for (int i = 0; i < 7; i++) {
            seekBar[i].setProgress(scale.widths[i]);
        }
    }

    private void updateWidthTextsFromSeekBars() {
        for (int i = 0; i < 7; i++) {
            widths[i].setText(Integer.toString(seekBar[i].getProgress()));
        }
    }

    private boolean widgetsDifferFromScale(int scaleIndex) {
        Scale scale = scales.get(scaleIndex);

        boolean nameEdited = !nameBox.getText().toString().equals(scale.name);
        boolean widthEdited = false;
        for (int i = 0; i < 7; i++) {
            widthEdited = widthEdited || seekBar[i].getProgress() != scale.widths[i];
        }
        boolean baseEdited = baseNoteSelector.getSelectedItemPosition() != scale.baseNote;

        return nameEdited || widthEdited || baseEdited;
    }

    private void updateSaveVisibility(boolean edited) {
        save.setVisibility(edited ? View.VISIBLE : View.INVISIBLE);
    }

    public static class ChangeScaleConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            int previousSelection = getArguments().getInt("previous_scale_position", 0);
            int newSelection = getArguments().getInt("new_scale_position", 0);

            ScaleManager scaleManager = (ScaleManager)getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(scaleManager);
            builder.setMessage("Are you sure? Your changes to " +
                    scaleManager.scales.get(previousSelection).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scaleManager.setScale(newSelection);
            });
            builder.setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                scaleManager.scaleSelector.setSelection(previousSelection);//revert
            });
            return builder.create();
        }
    }

    public static class DeleteConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            ScaleManager scaleManager = (ScaleManager)getActivity();
            int scaleSelection = scaleManager.scaleSelector.getSelectedItemPosition();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? " +
                    scaleManager.scales.get(scaleSelection).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scaleManager.scales.remove(scaleSelection);

                if (scaleManager.scales.isEmpty()) {
                    scaleManager.createNewScale();
                } else {
                    Scale.writeScales(getActivity(), scaleManager.scales);
                    scaleManager.scales = Scale.loadScales(scaleManager);
                }

                scaleManager.updateScaleSpinnerAdapter();
                int newSelection;

                //if the selectedScale was deleted, decrease the selectedScale
                if (scaleSelection >= scaleManager.scales.size()) {
                    newSelection = scaleManager.scales.size() - 1;
                } else {
                    newSelection = scaleSelection;
                }

                // since the scale was deleted, no confirmation necessary
                scaleManager.setScale(newSelection);
                scaleManager.scaleSelector.setSelection(newSelection);
            });
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

    public static class CancelConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            ScaleManager scaleManager = (ScaleManager) getActivity();
            int scaleSelection = scaleManager.scaleSelector.getSelectedItemPosition();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? Your changes to " +
                    scaleManager.scales.get(scaleSelection).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scaleManager.goBackToIsonActivity();
            });
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

    private void updateScaleSpinnerAdapter() {
        String[] scaleStrings = new String[scales.size()];
        for (int index = 0; index < scaleStrings.length; index++) {
            scaleStrings[index] = scales.get(index).name;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                scaleStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scaleSelector.setAdapter(adapter);
    }

    private void updateBaseSpinnerAdapter() {
        ArrayAdapter<String> baseNoteAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                Scale.NOTE_NAMES) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                view.setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
                return view;
            }
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView)super.getDropDownView(position, convertView, parent);
                view.setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
                return view;
            }
        };
        baseNoteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        baseNoteSelector.setAdapter(baseNoteAdapter);
    }

    public void createNewScale() {
        Scale newScale = new Scale(new int[] {3, 3, 3, 3, 3, 3, 3}, "Untitled", 0);
        scales.add(newScale);
        Scale.writeScales(this, scales);
        scales = Scale.loadScales(getApplicationContext());
    }

    public Scale getSelectedScale() {
        return scales.get(scaleSelector.getSelectedItemPosition());
    }

    public void setScale(int scaleIndex) {
        System.out.println("Switching to " + scaleIndex);
        previousScale = scaleIndex;
        updateNameBox();
        updateSeekBarsFromScale();
        updateWidthTextsFromSeekBars();
        baseNoteSelector.setSelection(getSelectedScale().baseNote);
    }

    public void cancelButton() {
        if (widgetsDifferFromScale(scaleSelector.getSelectedItemPosition())) {
            CancelConfirmation dialog = new CancelConfirmation();
            dialog.show(getSupportFragmentManager(),"dialog");
        } else {
            goBackToIsonActivity();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelButton();
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    protected void goBackToIsonActivity() {
        Intent intent = new Intent(getApplicationContext(), IsonActivity.class);
        startActivity(intent);
        finish();
    }

}

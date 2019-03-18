package com.coderss.ison;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

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

public class ScaleManager extends FragmentActivity {

    private Preferences preferences;

    private int selectedScale = 0;
    private Scale editedScale;
    private SeekBar[] seekBar;
    private EditText nameBox;
    private Spinner scaleSelector;

    //changed by ChangeScaleConfirmation
    private Spinner baseNoteSelector;
    static ArrayList<Scale> scales;
    static boolean dialogOpen = false;

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
        if (editedScale == null) {
            editedScale = scales.get(selectedScale).copy();
            System.out.println(editedScale);
        }
        setContentView(R.layout.scale_manager);
        setTitle("Scale Manager");
        setUpButtons();
    }

    public void onStart() {
        super.onStart();
        (this.findViewById(R.id.scrollView1)).requestFocus();
    }

    public void setUpButtons() {
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

        TextView[] width = new TextView[7];
        width[0] = this.findViewById(R.id.width1);
        width[1] = this.findViewById(R.id.width2);
        width[2] = this.findViewById(R.id.width3);
        width[3] = this.findViewById(R.id.width4);
        width[4] = this.findViewById(R.id.width5);
        width[5] = this.findViewById(R.id.width6);
        width[6] = this.findViewById(R.id.width7);

        seekBar = new SeekBar[7];
        seekBar[0] = this.findViewById(R.id.seekBar1);
        seekBar[1] = this.findViewById(R.id.seekBar2);
        seekBar[2] = this.findViewById(R.id.seekBar3);
        seekBar[3] = this.findViewById(R.id.seekBar4);
        seekBar[4] = this.findViewById(R.id.seekBar5);
        seekBar[5] = this.findViewById(R.id.seekBar6);
        seekBar[6] = this.findViewById(R.id.seekBar7);

        for (int i = 0; i < 7; ++i) {
            width[i].setWidth(width[i].getLineHeight() * 2);
            final int index = i;
            final TextView currentWidth = width[i];
            seekBar[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    currentWidth.setText(progress + "");
                    editedScale.widths[index] = progress;
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
            seekBar[i].setProgress(editedScale.widths[i]);
            //must be after the listener so it is called
        }

        scaleSelector = findViewById(R.id.scaleSpinner);
        updateSpinner(this);
        scaleSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int newSelection, long arg3) {
                if (newSelection != selectedScale) {
                    if (hasBeenChanged()) {
                        if (!dialogOpen) {
                            ChangeScaleConfirmation dialog = new ChangeScaleConfirmation();
                            Bundle args = new Bundle();
                            args.putInt("selected_scale_position", selectedScale);
                            args.putInt("new_scale_position", newSelection);
                            dialog.setArguments(args);
                            dialog.show(getSupportFragmentManager(),"dialog");
                        }
                    } else setScale(newSelection);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        nameBox = this.findViewById(R.id.editText1);
        nameBox.setText(scales.get(selectedScale).name);

        baseNoteSelector = this.findViewById(R.id.spinner2);
        ArrayAdapter<String> baseNoteAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item,
                        Scale.NOTE_NAMES) {
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView)super.getView(position, convertView, parent);
                        view.setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
                        return view;
                    }
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView)super.getDropDownView(position, convertView, parent);
                        view.setTypeface(Typeface.createFromAsset(getResources().getAssets(),"greek.ttf"));
                        return view;
                    }
                };
        baseNoteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        baseNoteSelector.setAdapter(baseNoteAdapter);
        baseNoteSelector.setSelection(editedScale.baseNote);

        Button save = this.findViewById(R.id.save);
        save.setOnClickListener(arg0 -> {
            scales.add(scaleSelector.getSelectedItemPosition(),
                    new Scale(editedScale.widths, nameBox.getText().toString(),
                            baseNoteSelector.getSelectedItemPosition()));
            scales.remove(scaleSelector.getSelectedItemPosition() + 1);
            Scale.writeScales(ScaleManager.this, scales);
            goBackToIsonActivity();
        });

        Button reset = this.findViewById(R.id.reset);
        reset.setOnClickListener(arg0 -> {
            ResetConfirmation dialog = new ResetConfirmation();
            dialog.show(getSupportFragmentManager(),"dialog");
        });

        Button delete = this.findViewById(R.id.delete);
        delete.setOnClickListener(arg0 -> {
            DeleteConfirmation dialog = new DeleteConfirmation();
            Bundle args = new Bundle();
            args.putInt("selected_scale_position", scaleSelector.getSelectedItemPosition());
            dialog.setArguments(args);
            dialog.show(getSupportFragmentManager(),"dialog");
        });

        Button create = this.findViewById(R.id.create);
        create.setOnClickListener(arg0 -> {
            createNewScale(ScaleManager.this);
            updateSpinner(ScaleManager.this);
            scaleSelector.setSelection(scales.size() - 1);
        });
    }

    public static class ChangeScaleConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            ScaleManager scaleManager = (ScaleManager)getActivity();
            dialogOpen = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(scaleManager);
            builder.setMessage("Are you sure? Your changes to " +
                    scales.get(scaleManager.selectedScale).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scaleManager.setScale(getArguments().getInt("new_scale_position"));
                dialogOpen = false;
            });
            builder.setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
                scaleManager.scaleSelector.setSelection(scaleManager.selectedScale);//revert
                dialogOpen = false;
            });
            return builder.create();
        }
    }

    public static class ResetConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            ScaleManager scaleManager = (ScaleManager) getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? All created and edited scales will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                Scale.emergencyReset(getActivity());
                scaleManager.goBackToIsonActivity();
            });
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

    public static class DeleteConfirmation extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstance) {
            ScaleManager scaleManager = (ScaleManager)getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? " +
                    scales.get(scaleManager.selectedScale).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scales.remove(getArguments().getInt("selected_scale_position"));
                if (scales.size() == 0) createNewScale(getActivity());
                else {
                    Scale.writeScales(getActivity(), scales);
                    scales = Scale.loadScales(scaleManager);
                }
                scaleManager.updateSpinner(getActivity());
                int newSelection;

                //if the selectedScale was deleted, decrease the selectedScale
                if (scaleManager.selectedScale >= scales.size()) newSelection = scales.size() - 1;
                else newSelection = scaleManager.selectedScale;
                System.out.println(newSelection);
                //since the scale was deleted, no confirmation necessary
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure? Your changes to " +
                    scaleManager.scales.get(scaleManager.selectedScale).name +
                    " will be lost");
            builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
                scaleManager.goBackToIsonActivity();
            });
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

    private void updateSpinner(Activity activity) {
        String[] scaleStrings = new String[scales.size()];
        for (int index = 0; index < scaleStrings.length; ++index)
            scaleStrings[index] = scales.get(index).name;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_spinner_item,
                scaleStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scaleSelector.setAdapter(adapter);
    }

    public static void createNewScale(Activity activity) {
        Scale newScale = new Scale(new int[] {3, 3, 3, 3, 3, 3, 3}, "Untitled", 0);
        scales.add(newScale);
        Scale.writeScales(activity, scales);
        scales = Scale.loadScales(activity.getApplicationContext());
    }

    public boolean hasBeenChanged() {
        if (!nameBox.getText().toString().equals(getSelectedScale().name)) return true;
        for (int i = 0; i < editedScale.widths.length; ++i)
            if (editedScale.widths[i] != getSelectedScale().widths[i]) return true;
        if (baseNoteSelector.getSelectedItemPosition() != getSelectedScale().baseNote) return true;
        return false;
    }

    public Scale getSelectedScale() {
        return scales.get(selectedScale);
    }

    public void setScale(int scaleIndex) {
        System.out.println("Switching to " + scaleIndex);
        if (scaleIndex == 0) {
            (new Exception()).printStackTrace();
        }
        selectedScale = scaleIndex;
        editedScale = getSelectedScale().copy();
        for (int i = 0; i < 7; ++i) {
            seekBar[i].setProgress(editedScale.widths[i]);
        }
        nameBox.setText(getSelectedScale().name);
        baseNoteSelector.setSelection(getSelectedScale().baseNote);
    }

    public void cancelButton() {
        if (hasBeenChanged()) {
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
        //these are static so we cannot wait around for onDestroy which is not called right away
        editedScale = null;
        scales.clear();

        finish();
        Intent intent = new Intent(getApplicationContext(), IsonActivity.class);
        startActivity(intent);
    }

}

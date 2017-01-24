package com.coderss.ison;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class ScaleManager extends FragmentActivity {
	
	TextView[] width;
	static int selectedScale = 0;
	static Scale editedScale;
	static SeekBar[] seekBar;
	static EditText nameBox;
	static Spinner scaleSelector;
	//changed by ChangeScaleConfirmation
	static Spinner baseNoteSelector;
	static ArrayList<Scale> scales;
	static boolean dialogOpen = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		scales = Scale.loadScales(this.getApplicationContext());
		System.out.println("created");
		if (editedScale == null) {
			editedScale = scales.get(selectedScale).copy();
			System.out.println(editedScale);
		}
		setContentView(R.layout.scale_manager);
		setUpButtons();
	}
	
	public void onStart() {
		super.onStart();
		((ScrollView)this.findViewById(R.id.scrollView1)).requestFocus();
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
		width = new TextView[7];
		width[0] = (TextView)this.findViewById(R.id.width1);
		width[1] = (TextView)this.findViewById(R.id.width2);
		width[2] = (TextView)this.findViewById(R.id.width3);
		width[3] = (TextView)this.findViewById(R.id.width4);
		width[4] = (TextView)this.findViewById(R.id.width5);
		width[5] = (TextView)this.findViewById(R.id.width6);
		width[6] = (TextView)this.findViewById(R.id.width7);
		seekBar = new SeekBar[7];
		seekBar[0] = (SeekBar)this.findViewById(R.id.seekBar1);
		seekBar[1] = (SeekBar)this.findViewById(R.id.seekBar2);
		seekBar[2] = (SeekBar)this.findViewById(R.id.seekBar3);
		seekBar[3] = (SeekBar)this.findViewById(R.id.seekBar4);
		seekBar[4] = (SeekBar)this.findViewById(R.id.seekBar5);
		seekBar[5] = (SeekBar)this.findViewById(R.id.seekBar6);
		seekBar[6] = (SeekBar)this.findViewById(R.id.seekBar7);
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
		scaleSelector = (Spinner)findViewById(R.id.numberOfColumns);
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
		nameBox = (EditText)this.findViewById(R.id.editText1);
		nameBox.setText(scales.get(selectedScale).name);
		baseNoteSelector = (Spinner)this.findViewById(R.id.spinner2);
		ArrayAdapter<String> baseNoteAdapter =
				new ArrayAdapter<String>(this,
											android.R.layout.simple_spinner_item,
											Scale.noteNames) {
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
		Button save = (Button)this.findViewById(R.id.save);
		save.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				scales.add(scaleSelector.getSelectedItemPosition(),
						new Scale(editedScale.widths, nameBox.getText().toString(),
								baseNoteSelector.getSelectedItemPosition()));
				scales.remove(scaleSelector.getSelectedItemPosition() + 1);
				Scale.writeScales(ScaleManager.this, scales);
				goBackToIsonActivity(ScaleManager.this);
			}
		});
		Button reset = (Button)this.findViewById(R.id.reset);
		reset.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ResetConfirmation dialog = new ResetConfirmation();
				dialog.show(getSupportFragmentManager(),"dialog");
			}
		});
		Button cancel = (Button)this.findViewById(R.id.cancel);
		cancel.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				cancelButton();
			}
		});
		Button delete = (Button)this.findViewById(R.id.delete);
		delete.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DeleteConfirmation dialog = new DeleteConfirmation();
				Bundle args = new Bundle();
				args.putInt("selected_scale_position", scaleSelector.getSelectedItemPosition());
				dialog.setArguments(args);
				dialog.show(getSupportFragmentManager(),"dialog");
			}
		});
		Button create = (Button)this.findViewById(R.id.create);
		create.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				createNewScale(ScaleManager.this);
				updateSpinner(ScaleManager.this);
				scaleSelector.setSelection(scales.size() - 1);
			}
		});
	}
	
	public static class ChangeScaleConfirmation extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstance) {
			dialogOpen = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Are you sure? Your changes to " +
								scales.get(selectedScale).name +
								" will be lost");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					setScale(getArguments().getInt("new_scale_position"));
					dialogOpen = false;
                }
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					scaleSelector.setSelection(selectedScale);//revert
					dialogOpen = false;
                }
			});
			return builder.create();
		}
	}
	
	public static class ResetConfirmation extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstance) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Are you sure? All created and edited scales will be lost");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Scale.emergencyReset(getActivity());
					goBackToIsonActivity(getActivity());
                }
			});
			builder.setNegativeButton("Cancel", null);
			return builder.create();
		}
	}
	
	public static class DeleteConfirmation extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstance) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Are you sure? " +
								scales.get(selectedScale).name +
								" will be lost");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					scales.remove(getArguments().getInt("selected_scale_position"));
					if (scales.size() == 0) createNewScale(getActivity());
					else {
						Scale.writeScales(getActivity(), scales);
						scales = Scale.loadScales(getActivity().getApplicationContext());
					}
					updateSpinner(getActivity());
					int newSelection;
					
					//if the selectedScale was deleted, decrease the selectedScale
					if (selectedScale >= scales.size()) newSelection = scales.size() - 1;
					else newSelection = selectedScale;
					System.out.println(newSelection);
					//since the scale was deleted, no confirmation necessary
					setScale(newSelection);
					scaleSelector.setSelection(newSelection);
                }
			});
			builder.setNegativeButton("Cancel", null);
			return builder.create();
		}
	}
	
	public static class CancelConfirmation extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstance) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Are you sure? Your changes to " +
								scales.get(selectedScale).name +
								" will be lost");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					goBackToIsonActivity(getActivity());
                }
			});
			builder.setNegativeButton("Cancel", null);
			return builder.create();
		}
	}
	
	public static void updateSpinner(Activity activity) {
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
	
	public static boolean hasBeenChanged() {
		if (!nameBox.getText().toString().equals(getSelectedScale().name)) return true;
		for (int i = 0; i < editedScale.widths.length; ++i)
			if (editedScale.widths[i] != getSelectedScale().widths[i]) return true;
		if (baseNoteSelector.getSelectedItemPosition() != getSelectedScale().baseNote) return true;
		return false;
	}
	
	public static Scale getSelectedScale() {
		return scales.get(selectedScale);
	}
	
	public static void setScale(int scaleIndex) {
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
			goBackToIsonActivity(this);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	cancelButton();
	    	return true;
	    } else return super.onKeyDown(keyCode, event);
	}
	
	protected static void goBackToIsonActivity(Activity activity) {
		//these are static so we cannot wait around for onDestroy which is not called right away
		editedScale = null;
		scales.clear();
		
		activity.finish();
		Intent intent = new Intent(activity.getApplicationContext(), IsonActivity.class);
		activity.startActivity(intent);
	}
	
}

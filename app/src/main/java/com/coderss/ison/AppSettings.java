package com.coderss.ison;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

public class AppSettings extends AppCompatActivity {

    public static void readLayoutSettings(Context context) {
        File filev2 = new File(context.getFilesDir().getPath() + "/layout.v2");
        if (filev2.exists()) {//get scales from v2 file format
            try {
                DataInputStream dis = new DataInputStream(
                        new FileInputStream(filev2));
                //read here
                dis.close();
            } catch (Exception e) {
                e.printStackTrace();
                if (filev2 != null && filev2.exists()) filev2.delete();
            }
        }
        if (!filev2.exists()) {//get scales from assets (last resort)
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(context.getResources().getAssets().open("layout.txt")));
                //load here
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private RadioButton byRow;
    private RadioButton byColumn;
    private RadioButton leftToRight;
    private RadioButton rightToLeft;
    private RadioButton topToBottom;
    private RadioButton bottomToTop;
    private Spinner numberOfRows;
    private Spinner numberOfColumns;
    private Spinner offset;
    private Spinner buttonHeight;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_settings);
        setTitle("Preferences");
        setUpComponents();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();
    }

    public void setUpComponents() {
        this.findViewById(R.id.tableRow1).setBackgroundColor(Color.LTGRAY);
        this.findViewById(R.id.tableRow3).setBackgroundColor(Color.LTGRAY);
        this.findViewById(R.id.tableRow6).setBackgroundColor(Color.LTGRAY);
        this.findViewById(R.id.tableRow9).setBackgroundColor(Color.LTGRAY);

        byRow = this.findViewById(R.id.byRow);
        byRow.setChecked(IsonActivity.BY_ROW);
        byRow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IsonActivity.BY_ROW = isChecked;
        });
        byColumn = this.findViewById(R.id.byColumn);
        byColumn.setChecked(!IsonActivity.BY_ROW);

        leftToRight = this.findViewById(R.id.leftToRight);
        leftToRight.setChecked(IsonActivity.LEFT_TO_RIGHT);
        leftToRight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IsonActivity.LEFT_TO_RIGHT = isChecked;
        });
        rightToLeft = this.findViewById(R.id.rightToLeft);
        rightToLeft.setChecked(!IsonActivity.LEFT_TO_RIGHT);

        topToBottom = this.findViewById(R.id.topToBottom);
        topToBottom.setChecked(IsonActivity.TOP_TO_BOTTOM);
        topToBottom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IsonActivity.TOP_TO_BOTTOM = isChecked;
        });
        bottomToTop = this.findViewById(R.id.bottomToTop);
        bottomToTop.setChecked(!IsonActivity.TOP_TO_BOTTOM);

        numberOfRows = this.findViewById(R.id.numberOfRows);
        populateRowsSpinner();

        numberOfRows.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                IsonActivity.ROWS = position + 1;
                int minColumns = (int)Math.ceil((IsonActivity.OFFSET + Scale.TOTAL_KEYS) / (double)IsonActivity.ROWS);
                if (IsonActivity.COLUMNS < minColumns)
                    numberOfColumns.setSelection(minColumns - 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        numberOfRows.setSelection(IsonActivity.ROWS - 1);

        numberOfColumns = this.findViewById(R.id.numberOfColumns);
        populateColumnsSpinner();

        numberOfColumns.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                IsonActivity.COLUMNS = position + 1;
                int minRows = (int)Math.ceil((IsonActivity.OFFSET + Scale.TOTAL_KEYS) / (double)IsonActivity.COLUMNS);
                if (IsonActivity.ROWS < minRows) {
                    numberOfRows.setSelection(minRows - 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        numberOfColumns.setSelection(IsonActivity.COLUMNS - 1);

        offset = this.findViewById(R.id.offset);
        populateOffsetSpinner();

        offset.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (IsonActivity.OFFSET != position) {
                    IsonActivity.OFFSET = position;

                    //because the content depends on the offset
                    populateRowsSpinner();
                    //attempt to keep the same row selection
                    if (IsonActivity.ROWS > numberOfRows.getCount())
                        numberOfRows.setSelection(numberOfRows.getCount() - 1);
                    else
                        numberOfRows.setSelection(IsonActivity.ROWS - 1);
                    //because the content depends on the offset
                    populateColumnsSpinner();
                    //attempt to keep the same column selection
                    if (IsonActivity.COLUMNS > numberOfColumns.getCount())
                        numberOfColumns.setSelection(numberOfColumns.getCount() - 1);
                    else
                        numberOfColumns.setSelection(IsonActivity.COLUMNS - 1);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        offset.setSelection(IsonActivity.OFFSET);

        buttonHeight = (Spinner)this.findViewById(R.id.buttonHeight);
        populateButtonHeightSpinner();

        buttonHeight.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                IsonActivity.BUTTON_HEIGHT = (position + 1) * (1f / 16f);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        buttonHeight.setSelection((int)(IsonActivity.BUTTON_HEIGHT / (1f / 16f)) - 1);
    }

    public void populateRowsSpinner() {
        String[] rowsOptions = new String[Scale.TOTAL_KEYS + IsonActivity.OFFSET];
        for (int i = 0; i < rowsOptions.length; ++i) {
            if (i == 0) rowsOptions[i] = "1 Row";
            else rowsOptions[i] = (i + 1) + " Rows";
        }
        ArrayAdapter<String> rowAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                rowsOptions);
        rowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberOfRows.setAdapter(rowAdapter);
    }

    public void populateColumnsSpinner() {
        String[] columnsOptions = new String[Scale.TOTAL_KEYS + IsonActivity.OFFSET];
        for (int i = 0; i < columnsOptions.length; ++i) {
            if (i == 0) columnsOptions[i] = "1 Column";
            else columnsOptions[i] = (i + 1) + " Columns";
        }
        ArrayAdapter<String> columnAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                columnsOptions);
        columnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberOfColumns.setAdapter(columnAdapter);
    }

    public void populateOffsetSpinner() {
        String[] offsetOptions = new String[Scale.TOTAL_KEYS];
        for (int i = 0; i < offsetOptions.length; ++i) {
            if (i == 0) offsetOptions[i] = "Do not offset";
            else offsetOptions[i] = "Offset by " + i;
        }
        ArrayAdapter<String> offsetAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                offsetOptions);
        offsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        offset.setAdapter(offsetAdapter);
    }

    public void populateButtonHeightSpinner() {
        String[] buttonHeightOptions = {
                "1/16 inch",
                "2/16 (1/8) inch",
                "3/16 inch",
                "4/16 (1/4) inch",
                "5/16 inch",
                "6/16 (3/8) inch",
                "7/16 inch",
                "8/16 (1/2) inch",
                "9/16 inch",
                "10/16 (5/8) inch",
                "11/16 inch",
                "12/16 (3/4) inch",
                "13/16 inch",
                "14/16 (7/8) inch",
                "15/16 inch",
                "16/16 (1) inch"};
        ArrayAdapter<String> offsetAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                buttonHeightOptions);
        offsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        buttonHeight.setAdapter(offsetAdapter);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(getApplicationContext(), IsonActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    public static void readLayoutSettings() {

    }

    public static class MySettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

}

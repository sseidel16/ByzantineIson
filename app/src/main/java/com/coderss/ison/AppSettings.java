package com.coderss.ison;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.coderss.ison.utility.Preferences;
import com.coderss.ison.utility.Scale;

public class AppSettings extends AppCompatActivity {

    private Preferences preferences;

    private RadioButton byRow;
    private RadioButton byColumn;
    private Spinner numberOfRows;
    private Spinner numberOfColumns;

    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        if (preferences.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

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
        byRow = this.findViewById(R.id.byRow);
        byRow.setChecked(IsonActivity.BY_ROW);
        byRow.setOnCheckedChangeListener((buttonView, isChecked) -> IsonActivity.BY_ROW = isChecked);
        byColumn = this.findViewById(R.id.byColumn);
        byColumn.setChecked(!IsonActivity.BY_ROW);

        numberOfRows = this.findViewById(R.id.numberOfRows);
        populateRowsSpinner();

        numberOfRows.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                IsonActivity.ROWS = position + 1;
                int minColumns = (int)Math.ceil((Scale.TOTAL_KEYS) / (double)IsonActivity.ROWS);
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
                int minRows = (int)Math.ceil((Scale.TOTAL_KEYS) / (double)IsonActivity.COLUMNS);
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

    }

    public void populateRowsSpinner() {
        String[] rowsOptions = new String[Scale.TOTAL_KEYS];
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
        String[] columnsOptions = new String[Scale.TOTAL_KEYS];
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(getApplicationContext(), IsonActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    public static class MySettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

}

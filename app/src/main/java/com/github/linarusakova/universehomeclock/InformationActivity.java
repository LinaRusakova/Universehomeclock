package com.github.linarusakova.universehomeclock;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InformationActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private DBHelper dbHelper;
    private ApiRecord apiRecord;

    EditText editApiKey;
    CheckBox checkBoxSetDefaultAPIKey;

    Button buttonAddApiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);
        db = getBaseContext().openOrCreateDatabase(DBHelper.DB_NAME, MODE_PRIVATE, null);
        dbHelper = new DBHelper(getBaseContext());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        editApiKey = findViewById(R.id.editApiKey);
        editApiKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                buttonAddApiKey.setClickable(true);
            }
        });

        editApiKey.setTextColor(getResources().getColor(R.color.yellow));
        checkBoxSetDefaultAPIKey = findViewById(R.id.checkBoxDefaultAPI);
        buttonAddApiKey = findViewById(R.id.buttonSaveApiKey);

        Handler keyAPIHandler = new Handler();
        keyAPIHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                                    apiRecord = (ApiRecord) tryGetCustomAPIKeyFromDB(db);
                                    editApiKey.setText(apiRecord.getKeyAPI());
                                    checkBoxSetDefaultAPIKey.setChecked(!apiRecord.isUsing());
                                    editApiKey.setFreezesText(apiRecord.isUsing());
                                    editApiKey.setActivated(apiRecord.isUsing());
                                    editApiKey.setClickable(apiRecord.isUsing());
                                    editApiKey.setFocusable(apiRecord.isUsing());
                                    editApiKey.setFocusableInTouchMode(apiRecord.isUsing());
                                    buttonAddApiKey.setClickable(apiRecord.isUsing() && editApiKey.getText().length() > 0);
                                }
                        );
                    }
                }
        );


        buttonAddApiKey.setOnClickListener(this);

        checkBoxSetDefaultAPIKey.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                editApiKey.setFocusable(!isChecked);
                editApiKey.setFocusableInTouchMode(!isChecked);
                editApiKey.setActivated(!isChecked);
                editApiKey.setClickable(!isChecked);
                editApiKey.setFreezesText(isChecked);
                buttonAddApiKey.setEnabled(!isChecked);
                buttonAddApiKey.setActivated(!isChecked);
                setDefaultAPIKey(isChecked);
            }
        });
    }

    private void setDefaultAPIKey(boolean useDefaultAPIKey) {
        saveToDB.post(new Runnable() {
            @Override
            public void run() {
                db = dbHelper.getWritableDatabase();
                dbHelper.updateApiRecord(db, DBHelper.DB_TABLE_NAME_APIKEY, useDefaultAPIKey);
            }
        });
    }

    private ApiRecord tryGetCustomAPIKeyFromDB(SQLiteDatabase db) {
        return dbHelper.getApiRecord(db);
    }

    Handler saveToDB = new Handler();

    public void saveApiKeyValue(String apiKey) {

        saveToDB.post(new Runnable() {
            @Override
            public void run() {
                db = dbHelper.getWritableDatabase();
                dbHelper.insertApiRecord(db, DBHelper.DB_TABLE_NAME_APIKEY, apiKey);
            }
        });
    }

    @Override
    public void onClick(View v) {
        String apikey = editApiKey.getText().toString();
        buttonAddApiKey.setClickable(false);
        saveApiKeyValue(apikey);
        CharSequence text = getResources().getString(R.string.toastWeatherApiSaved);
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        db = dbHelper.getWritableDatabase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
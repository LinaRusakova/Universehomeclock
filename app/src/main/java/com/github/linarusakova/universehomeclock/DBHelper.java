package com.github.linarusakova.universehomeclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class DBHelper extends SQLiteOpenHelper {

    SQLiteDatabase DB;
    private final String LOG_TAG = "DB HELPER = ";
    public static final String DB_NAME = "locationDB";
    private static final int DB_VERSION = 1;
    int oldVersion, newVersion;

    public static final String DB_TABLE_NAME = "locations";
    public static final String DB_TABLE_NAME_CHECKED = "checklocation";
    public static final String DB_TABLE_NAME_APIKEY = "apikeyDB";
    private static final String api_key_column = "APIvalue";
    private static final String location_name_column = "name";
    private static final String latitude_column = "latitude";
    private static final String longitude_column = "longitude";

    private static DBHelper dbHelper;
    private String isUsing_trigger_column = "isUsing";

    public static DBHelper getInstance(Context context) {
        if (dbHelper == null)
            dbHelper = new DBHelper(context);
        return dbHelper;
    }

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, "--- onCreate database ---");
        oldVersion = 1;
        newVersion = 1;
        onCreateOrUpgradeDB(db, oldVersion, newVersion);
    }

    public void insertDBRecord(SQLiteDatabase db, String tableName, String locationName, String latitude, String longitude) {

        ContentValues locationValues = new ContentValues();
        locationValues.put(location_name_column, locationName);
        locationValues.put(latitude_column, latitude);
        locationValues.put(longitude_column, longitude);
        db.insert(tableName, null, locationValues);
        Log.d(LOG_TAG, "--- insert Record to DB:" + locationName + ", lat: " + latitude + ", long: " + longitude + ". ");

    }

    public void update(SQLiteDatabase db, String tableName, String locationName, String latitude, String longitude) {
        ContentValues locationValues = new ContentValues();
        locationValues.put(location_name_column, locationName);
        locationValues.put(latitude_column, latitude);
        locationValues.put(longitude_column, longitude);
        db.update(tableName, locationValues, "_ID= ?",
                new String[]{Integer.toString(1)});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreateOrUpgradeDB(db, oldVersion, newVersion);
        Log.d(LOG_TAG, "--- onUpgrade database ---");
    }

    public void onCreateOrUpgradeDB(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) {
        } else {
        }
    }

    public void deleteRowFromDB(SQLiteDatabase db, String tableName, Location selectedLocation) {
        int n = db.delete(tableName,
                location_name_column + "= ? and "
                        + latitude_column + " = ? and "
                        + longitude_column + " like ?",
                new String[]{selectedLocation.getLocationName(), selectedLocation.getLatitude(), selectedLocation.getLongitude()});
        Log.i("delete() = ", n + "-rows = {" + selectedLocation + "}");
    }

    public void insertApiRecord(SQLiteDatabase db, String dbTableName, String apiKey) {
        ContentValues apiKeyValues = new ContentValues();

        String cryptedAPIKey = null;
        try {
            cryptedAPIKey = Base64.encodeToString(apiKey.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        apiKeyValues.put(api_key_column, cryptedAPIKey);
        apiKeyValues.put(isUsing_trigger_column, 1);
        int nRecords = db.update(dbTableName, apiKeyValues,
                "_ID=?", new String[]{Integer.toString(1)});
        Log.i("API RECORD", String.valueOf(nRecords) + "- Records  updated");
        Log.i("API RECORD", getApiRecord(db).toString());

    }

    public ApiRecord updateApiRecord(SQLiteDatabase db, String dbTableName, Boolean useDefaultAPIKey) {

        int zeroVSOne = !useDefaultAPIKey ? 1 : 0;
        ContentValues apiKeyValues = new ContentValues();
        apiKeyValues.put(isUsing_trigger_column, zeroVSOne);
        String whereClause = "_ID='1'";
        try {
            int nRecords = db.update(dbTableName, apiKeyValues, whereClause, null);
            Log.i("API RECORD", String.valueOf(nRecords));
        } catch (Exception e) {
            Log.e("APIRECORD", "Record not updated");
        }
        return getApiRecord(db);
    }

    public ApiRecord getApiRecord(SQLiteDatabase db) {

        String apiKey = "";
        boolean isUsing = false;
        String id = "";
        String cleanAPIKey="";

        try (Cursor cursor = db.query(DB_TABLE_NAME_APIKEY,
                new String[]{"_ID", api_key_column, isUsing_trigger_column},
                "_ID = ?",
                new String[]{Integer.toString(1)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                id = cursor.getString(0);
                apiKey = cursor.getString(1);
                isUsing = Integer.parseInt(cursor.getString(2)) == 1;
            }
        }
        Log.i("APIRECORD", new ApiRecord(apiKey, isUsing).toString());

        try {
            cleanAPIKey = new String(Base64.decode(apiKey, Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Log.i("APIRECORD", new ApiRecord(cleanAPIKey, isUsing).toString());
        return new ApiRecord(cleanAPIKey, isUsing);
    }


    public void prepareAllDataBases(SQLiteDatabase db) {

        DB = db;
        DB.execSQL("drop table IF EXISTS " + DB_TABLE_NAME);
        DB.execSQL("CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ("
                + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + location_name_column + " text, "
                + latitude_column + " text, "
                + longitude_column + " text);");
        DB.execSQL("drop table IF EXISTS " + DB_TABLE_NAME_CHECKED);
        DB.execSQL("CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_CHECKED + " ("
                + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + location_name_column + " text, "
                + latitude_column + " text, "
                + longitude_column + " text);");

        ContentValues locationValues = new ContentValues();
        locationValues.put(location_name_column, "Earth");
        locationValues.put(latitude_column, "0");
        locationValues.put(longitude_column, "0");
        DB.insert(DB_TABLE_NAME, null, locationValues);
        DB.insert(DB_TABLE_NAME_CHECKED, null, locationValues);

        DB.execSQL("drop table if exists " + DB_TABLE_NAME_APIKEY);
        DB.execSQL("CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_APIKEY + " ("
                + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + api_key_column + " TEXT,"
                + isUsing_trigger_column + " INTEGER);");
        ContentValues apiKeyValues = new ContentValues();
        apiKeyValues.put(api_key_column, "WFhY");
        apiKeyValues.put(isUsing_trigger_column, 0);
        DB.insert(DB_TABLE_NAME_APIKEY, null, apiKeyValues);
    }

    public boolean tableExists(SQLiteDatabase db, String table) {
        boolean result = false;
        String sql = "select count(*) xcount from sqlite_master where type='table' and name='"
                + table + "'";
        Cursor cursor = db.rawQuery(sql, null);
        cursor.moveToFirst();
        if (cursor.getInt(0) > 0)
            result = true;
        cursor.close();
        return result;
    }
}

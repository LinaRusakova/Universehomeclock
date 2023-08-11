package com.github.linarusakova.universehomeclock;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class ListLocationActivity extends AppCompatActivity {
    private SQLiteDatabase db;
    private DBHelper dbHelper;
    Location selectedLocation;
    List<Location> locations = new ArrayList<>();
    ListView listLocationsView;
    ListLocationAdapter adapterLocation;
    Button buttonDelete;

    public class ListLocationAdapter extends BaseAdapter {
        Context context;
        List<Location> locationList;
        LayoutInflater inflter;

        public ListLocationAdapter(Context context, List<Location> locationList) {
            this.context = context;
            this.locationList = locationList;
            inflter = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return locationList.size();
        }

        @Override
        public Location getItem(int position) {
            return locationList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int item, View view, ViewGroup parent) {
            View newView = view;
            if (newView == null) {
                view = inflter.inflate(R.layout.listitem, parent, false);
            }
            TextView nameLocation = (TextView) view.findViewById(R.id.itemCityName);
            TextView latitude = (TextView) view.findViewById(R.id.itemLatitude);
            TextView longitude = (TextView) view.findViewById(R.id.itemLongitude);
            buttonDelete = (Button) view.findViewById(R.id.itemButtonDelete);
            buttonDelete.setOnClickListener(this::onClick);
            buttonDelete.setTag(item);
            Location tempLocation = locationList.get(item);
            nameLocation.setText(tempLocation.getLocationName());
            latitude.setText(tempLocation.getLatitude());
            longitude.setText(tempLocation.getLongitude());
            return view;
        }

        public void addLocation(Location newLocation) {
            locationList.add(newLocation);
            notifyDataSetChanged();
        }

        public void onClick(View v) {

            Location chooselocation = getItem(Byte.valueOf(v.getTag().toString()));
            db = getBaseContext().openOrCreateDatabase(DBHelper.DB_NAME, MODE_PRIVATE, null);
            dbHelper.deleteRowFromDB(db, DBHelper.DB_TABLE_NAME, chooselocation);
            CharSequence text = getResources().getString(R.string.toastLocationDeleted);;
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getBaseContext(), text, duration);
            toast.show();
            getAllRecordsFromDB();
            this.locationList = locations;
            this.notifyDataSetChanged();

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_location);
        dbHelper = new DBHelper(this.getBaseContext());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        listLocationsView = findViewById(R.id.list_locations);
        db = getBaseContext().openOrCreateDatabase(DBHelper.DB_NAME, MODE_PRIVATE, null);
        Intent intentFromAddLocation = getIntent();
        boolean change = intentFromAddLocation.getBooleanExtra("change", false);
        getAllRecordsFromDB();
        adapterLocation = new ListLocationAdapter(getApplicationContext(), locations);
        adapterLocation.notifyDataSetChanged();
        listLocationsView.setAdapter(adapterLocation);
        if (change) {
            getAllRecordsFromDB();
            adapterLocation.notifyDataSetChanged();
        }

        listLocationsView.setOnItemClickListener((adapter, view, item, id) -> {
            selectedLocation = adapterLocation.getItem(item);
            choseLocation(selectedLocation);
            selectedLocation = null;
            Intent intent = new Intent(ListLocationActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    Handler saveToDB = new Handler();

    public void choseLocation(Location selectedLocation) {
        saveToDB.post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    db = getBaseContext().openOrCreateDatabase(DBHelper.DB_NAME, MODE_PRIVATE, null);
                    dbHelper.update(db, DBHelper.DB_TABLE_NAME_CHECKED, selectedLocation.getLocationName(), String.valueOf(selectedLocation.getLatitude()), String.valueOf(selectedLocation.getLongitude()));

                    db.close();
                });
            }
        });
    }

    private void getAllRecordsFromDB() {

        List<Location> locations = new ArrayList<>();
        db = getBaseContext().openOrCreateDatabase(DBHelper.DB_NAME, MODE_PRIVATE, null);
        Cursor query = db.rawQuery("SELECT * FROM " + DBHelper.DB_TABLE_NAME, null);
        while (query.moveToNext()) {
            String locationName = query.getString(1);
            String latitude = query.getString(2);
            String longitude = query.getString(3);
            locations.add(new Location(locationName, latitude, longitude));
        }

        this.locations = locations;
        query.close();
        db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_create_location) {
            Intent intent = new Intent(this, CreateLocationActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        adapterLocation.notifyDataSetChanged();
    }
}

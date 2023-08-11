package com.github.linarusakova.universehomeclock;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class CreateLocationActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private DBHelper dbHelper;
    String newLocationName, newLatitude, newLongitude;
    EditText editLocationName, editLocationLat, editLocationLon;
    Handler saveToDB = new Handler();

    private String DEFAULT_API_KEY;
    private ApiRecord apiKeyRecord;
    String currentAPIKey;
    Location selectedLocation;
    ListView listLocationsView;
    List<Location> locations = new ArrayList<>();
    ListLocationAdapter adapterLocation;

    class ListLocationAdapter extends BaseAdapter {
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
                view = inflter.inflate(R.layout.search_listitem, parent, false);
            }
            TextView nameLocation = (TextView) view.findViewById(R.id.itemCityName);
            TextView latitude = (TextView) view.findViewById(R.id.itemLatitude);
            TextView longitude = (TextView) view.findViewById(R.id.itemLongitude);
            Location tempLocation = locationList.get(item);
            nameLocation.setText(tempLocation.getLocationName());
            latitude.setText(tempLocation.getLatitude());
            longitude.setText(tempLocation.getLongitude());
            return view;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_location);
        dbHelper = DBHelper.getInstance(this.getBaseContext());
        db = dbHelper.getWritableDatabase();
        DEFAULT_API_KEY = getResources().getString(R.string.valueApiKey);
        apiKeyRecord = dbHelper.getApiRecord(db);

        if (apiKeyRecord.isUsing()) {
            currentAPIKey = apiKeyRecord.getKeyAPI();
        } else {
            try {
                currentAPIKey = new String(Base64.decode(DEFAULT_API_KEY, Base64.DEFAULT), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        timer = new Timer();
        editLocationName = findViewById(R.id.editLocationName);
        editLocationLat = findViewById(R.id.editLatitude);
        editLocationLon = findViewById(R.id.editLongitude);

        Button buttonAddLocation = findViewById(R.id.buttonAddLocation);
        buttonAddLocation.setOnClickListener(this);

        Button buttonSearchLocation = findViewById(R.id.buttonSearchLocation);
        buttonSearchLocation.setOnClickListener(this);

        listLocationsView = findViewById(R.id.list_locations);
        adapterLocation = new ListLocationAdapter(getApplicationContext(), locations);
        listLocationsView.setAdapter(adapterLocation);
        listLocationsView.setOnItemClickListener((adapter, view, item, id) -> {
            selectedLocation = adapterLocation.getItem(item);
            editLocationName.setText(selectedLocation.getLocationName());
            editLocationLat.setText(selectedLocation.getLatitude());
            editLocationLon.setText(selectedLocation.getLongitude());
            selectedLocation = null;
            locations.clear();
            adapterLocation.notifyDataSetChanged();
        });

    }

    public void saveNewLocationToDB() {
        newLocationName = String.valueOf(editLocationName.getText());
        newLatitude = String.valueOf(editLocationLat.getText());
        newLongitude = String.valueOf(editLocationLon.getText());
        saveToDB.post(new Runnable() {
            @Override
            public void run() {
                dbHelper.insertDBRecord(db, DBHelper.DB_TABLE_NAME, newLocationName, newLatitude, newLongitude);
                dbHelper.update(db, DBHelper.DB_TABLE_NAME_CHECKED, newLocationName, newLatitude, newLongitude);
                db.close();
            }
        });
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.buttonAddLocation) {
            saveNewLocationToDB();
            CharSequence text = getResources().getString(R.string.toastTextLocationAdded);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();

            Intent intent = new Intent(this, ListLocationActivity.class);
            intent.putExtra("change", true);
            startActivity(intent);
        } else if (v.getId() == R.id.buttonSearchLocation) {
            searchLocationByName();
        }
    }

    List<Message> messageList;
    InputStream in;
    Timer timer;

    private void searchLocationByName() {

        newLocationName = String.valueOf(editLocationName.getText());

        Log.i("SEARCH = ", "Search started by name = " + newLocationName);
        String urlString = "https://api.openweathermap.org/geo/1.0/direct?q=" + newLocationName + "&limit=5&appid=" + currentAPIKey;
        if (newLocationName.length() != 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    HttpsURLConnection conn = null;
                    try {
                        URL url = new URL(urlString);
                        conn = (HttpsURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setReadTimeout(10000);
                        conn.connect();
                        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                            in = new BufferedInputStream(conn.getInputStream());
                        }
                        try {
                            messageList = readJsonStream(in);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    } catch (IOException e) {
//                        e.printStackTrace();
                        showErrorHTTPRequestMessage();
                        timer.cancel();
return;
                    } finally {
                        assert conn != null;
                        conn.disconnect();
                    }

                    runOnUiThread(() -> {
                        for (Message m : messageList) {
                            locations.add(new Location(m.nameLocation + "[" + m.nameCountry + ", " + m.nameState + "]", m.latitude, m.longitude));
                        }
                        adapterLocation.notifyDataSetChanged();
                    });
                }
            }, 100);
        }
    }

    Handler handler = new Handler();
    private void showErrorHTTPRequestMessage() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = getResources().getString(R.string.toastWeatherApiKeyWrong);
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(CreateLocationActivity.this, text, duration);
                toast.show();
            }
        });
    }

    public List<Message> readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readMessagesArray(reader);
        } finally {
            reader.close();
        }
    }

    public List<Message> readMessagesArray(JsonReader reader) throws IOException {
        List<Message> messages = new ArrayList<Message>();
        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readMessage(reader));
        }
        reader.endArray();
        return messages;
    }

    public Message readMessage(JsonReader reader) throws IOException {
        String nameLocation = null;
        String nameCountry = null;
        String nameState = null;
        String latitude = null;
        String longitude = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                nameLocation = reader.nextString();
            } else if (name.equals("country")) {
                nameCountry = reader.nextString();
            } else if (name.equals("state")) {
                nameState = reader.nextString();
            } else if (name.equals("lat")) {
                latitude = reader.nextString();
            } else if (name.equals("lon")) {
                longitude = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Message(nameLocation, nameCountry, nameState, latitude, longitude);
    }
}
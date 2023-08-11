package com.github.linarusakova.universehomeclock;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.WHITE;
import static android.graphics.Color.YELLOW;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_CITY_NAME = "city";
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    boolean isLocation = false;
    private String DEFAULT_API_KEY;

    private ApiRecord apiKeyRecord;
    final int NORMAL_PRESSURE_CONST = 755;
    ImageView weatherPic;
    TextView temperature;
    TextView wind;
    TextView pressure;

    TextView weatherDescription;
    TextView weatherCity;

    ImageButton buttonInformation;
    Button button;

    String windText = "";
    String windDesc = "";
    String windUnits = "";
    String temperatureText = "";
    String iconText = "";
    String descriptionText = "";

    String pressureNormal = "";
    String pressureHigh = "";
    String pressureLow = "";
    String pressureText = "";
    String pressureDesc = "";
    String pressureUnits = "";
    String weatherCityText = "";
    String weatherCityTextFromDB = "";
    String language = "";

    String lat = "";
    String lon = "";
    int currentDayOfWeek = 0;
    boolean change = false;


    Timer timer, timerWeather, startTimer;

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("lat", lat);
        savedInstanceState.putString("lon", lon);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            DEFAULT_API_KEY = new String(Base64.decode(getResources().getString(R.string.valueApiKey), Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        dbHelper = DBHelper.getInstance(this.getBaseContext());
        db = dbHelper.getWritableDatabase();


        firstStartApp(!dbHelper.tableExists(db, DBHelper.DB_TABLE_NAME_APIKEY));

        apiKeyRecord = tryGetCustomApiKeyFromDB(db);

        Intent intent = getIntent();
        change = intent.getBooleanExtra("change", false);
        if (change) {
            weatherCityTextFromDB = intent.getStringExtra(EXTRA_CITY_NAME);
            lat = intent.getStringExtra(EXTRA_LATITUDE);
            lon = intent.getStringExtra(EXTRA_LONGITUDE);
            isLocation = true;
            change = false;
        } else {
            getLocationFromDB();
            isLocation = true;
        }


        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setAttributes(localLayoutParams);

        buttonInformation = (ImageButton) findViewById(R.id.buttonInformation);
        buttonInformation.setOnClickListener(this);
        if (savedInstanceState != null) {
            change = savedInstanceState.getBoolean("change");
            lat = savedInstanceState.getString("lat");
            lon = savedInstanceState.getString("lon");
        }
        final TextView timeText = findViewById(R.id.MainClock);

        timer = new Timer();
        timerWeather = new Timer();
        startTimer = new Timer();
        long delay = 0;
        long period = 1000;

        timeText.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        weatherPic = findViewById(R.id.weatherPic);
        weatherDescription = findViewById(R.id.weatherDescription);
        language = getResources().getString(R.string.language_app);
        temperature = findViewById(R.id.temperature);
        wind = findViewById(R.id.wind);
        windDesc = getResources().getString(R.string.windDesc);
        windUnits = getResources().getString(R.string.windUnits);
        pressure = findViewById(R.id.pressure);
        pressureDesc = getResources().getString(R.string.pressureDesc);
        pressureUnits = getResources().getString(R.string.pressureUnits);
        weatherCity = findViewById(R.id.weatherCity);
        timeText.setBackgroundResource(R.color.black);
        button = findViewById(R.id.button);

        weatherCity.setOnClickListener(this);

        Resources res = getResources();
        Map<String, Drawable> iconsMap = new HashMap<>();
        iconsMap.put("01d", ResourcesCompat.getDrawable(res, R.drawable.icon01d, null));
        iconsMap.put("02d", ResourcesCompat.getDrawable(res, R.drawable.icon02d, null));
        iconsMap.put("03d", ResourcesCompat.getDrawable(res, R.drawable.icon03d, null));
        iconsMap.put("04d", ResourcesCompat.getDrawable(res, R.drawable.icon04d, null));
        iconsMap.put("09d", ResourcesCompat.getDrawable(res, R.drawable.icon09d, null));
        iconsMap.put("10d", ResourcesCompat.getDrawable(res, R.drawable.icon10d, null));
        iconsMap.put("11d", ResourcesCompat.getDrawable(res, R.drawable.icon11d, null));
        iconsMap.put("13d", ResourcesCompat.getDrawable(res, R.drawable.icon13d, null));
        iconsMap.put("50d", ResourcesCompat.getDrawable(res, R.drawable.icon50d, null));
        iconsMap.put("01n", ResourcesCompat.getDrawable(res, R.drawable.icon01d, null));
        iconsMap.put("02n", ResourcesCompat.getDrawable(res, R.drawable.icon02d, null));
        iconsMap.put("03n", ResourcesCompat.getDrawable(res, R.drawable.icon03d, null));
        iconsMap.put("04n", ResourcesCompat.getDrawable(res, R.drawable.icon04d, null));
        iconsMap.put("09n", ResourcesCompat.getDrawable(res, R.drawable.icon09d, null));
        iconsMap.put("10n", ResourcesCompat.getDrawable(res, R.drawable.icon10d, null));
        iconsMap.put("11n", ResourcesCompat.getDrawable(res, R.drawable.icon11d, null));
        iconsMap.put("13n", ResourcesCompat.getDrawable(res, R.drawable.icon13d, null));
        iconsMap.put("50n", ResourcesCompat.getDrawable(res, R.drawable.icon50d, null));

        pressureNormal = getResources().getString(R.string.pressureNORMAL);
        pressureHigh = getResources().getString(R.string.pressureH_PRESSURE);
        pressureLow = getResources().getString(R.string.pressureL_PRESSURE);


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {

                    Date currentDate = new Date();

                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"), new Locale(language));
                    cal.setTime(currentDate);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    if (currentDayOfWeek == 0) {
                        currentDayOfWeek = dayOfWeek;
                    }
                    if (dayOfWeek != currentDayOfWeek) {
                        String idBeforeDayOfWeek = "day" + (currentDayOfWeek);
                        TextView beforeDayOfWeekView = findViewById(getResources().getIdentifier(idBeforeDayOfWeek, "id", getPackageName()));
                        beforeDayOfWeekView.setBackgroundColor(Integer.parseInt("#009688"));
                    }

                    String idDayOfWeek = "day" + (dayOfWeek);
                    TextView dayOfWeekView = findViewById(getResources().getIdentifier(idDayOfWeek, "id", getPackageName()));

                    dayOfWeekView.setBackgroundColor(YELLOW);

                    DateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    String timeCurrent = timeFormat.format(currentDate);

                    timeText.setText(timeCurrent);
                    SimpleDateFormat timeFormatHour = new SimpleDateFormat("HH", Locale.getDefault());
                    int hour = Integer.parseInt(timeFormatHour.format(currentDate));
                    if (hour >= 7 && hour <= 22) {
                        localLayoutParams.screenBrightness = 1F;
                        getWindow().setAttributes(localLayoutParams);
                        timeText.setTextColor(WHITE);
                    } else {
                        localLayoutParams.screenBrightness = 0F;
                        getWindow().setAttributes(localLayoutParams);
                        timeText.setTextColor(GREEN);
                    }
                });
            }
        }, delay, period);


        timerWeather.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isLocation) {
                    getWeather();
                }
                runOnUiThread(() -> {

                    weatherPic.setImageDrawable(iconsMap.get(String.valueOf(iconText)));
                    temperature.setText(temperatureText);
                    wind.setText(windText);
                    pressure.setText(pressureText);
                    weatherDescription.setText(descriptionText);
                    weatherCity.setText(weatherCityText);
                });
            }
        }, 0, 1800000);

    }

    private void getWeather() {

        String currentAPIKey;
        if (apiKeyRecord.isUsing()) {
            currentAPIKey = apiKeyRecord.getKeyAPI();
        } else {
            currentAPIKey = DEFAULT_API_KEY;
        }


        String units = "metric";
        String urlString = "https://api.openweathermap.org/data/2.5/weather?" + "lat=" + lat + "&lon=" + lon + "&appid=" + currentAPIKey + "&units=" + units + "&lang=" + language;
        HttpsURLConnection conn = null;
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlString);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setReadTimeout(10000);
            conn.connect();
            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader rd = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                System.out.println(result);
                rd.close();
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();
            }

            JsonParser parser = new JsonParser();
            JsonElement rootNode = parser.parse(result.toString());
            JsonObject details = rootNode.getAsJsonObject();

            JsonElement weatherNode = details.get("weather");
            JsonObject weather = weatherNode.getAsJsonArray().get(0).getAsJsonObject();
            JsonElement getIcon = weather.get("icon");
            iconText = String.valueOf(getIcon).replaceAll("\"", "");

            JsonElement getDescription = weather.get("description");
            descriptionText = String.valueOf(getDescription).replaceAll("\"", "");

            // Temperature data section
            JsonObject mainNode = details.get("main").getAsJsonObject();
            JsonElement getTemp = mainNode.get("temp");
            temperatureText = getTemp.toString() + "℃";

            // Pressure data section
            JsonElement getPressure = mainNode.get("pressure");
            double pressureDouble = getPressure.getAsDouble();
            double keyX = 0.750062;
            pressureDouble = pressureDouble * keyX;
            int pressureSubstr = String.valueOf(pressureDouble).indexOf('.');
            int pressure = Integer.parseInt(String.valueOf(pressureDouble).substring(0, pressureSubstr));
            int deviationPressure = pressure - NORMAL_PRESSURE_CONST;
            pressureText = pressureDesc + ": " + pressure + " " + pressureUnits + " [";
            if (deviationPressure > 0) {
                pressureText += pressureHigh + " +" + Math.abs(deviationPressure) + "]";
            } else if (deviationPressure == 0) {
                pressureText += pressureNormal + "]";
            } else {
                pressureText += pressureLow + " -" + Math.abs(deviationPressure) + "]";
            }

            // Wind data section
            JsonObject windNode = details.get("wind").getAsJsonObject();
            JsonElement getWindSpeed = windNode.get("speed");
            windText = windDesc + ": " + getWindSpeed.toString() + " " + windUnits;

            // City data section
            weatherCityText = details.get("name").getAsString();
            if (weatherCityText.isEmpty()) weatherCityText = weatherCityTextFromDB;
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorHTTPRequestMessage();
            timerWeather.cancel();
        } finally {
            assert conn != null;

            conn.disconnect();
        }
    }

    Handler handler = new Handler();

    private void showErrorHTTPRequestMessage() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                CharSequence text = getResources().getString(R.string.toastWeatherApiKeyWrong);
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(MainActivity.this, text, duration);
                toast.show();
            }
        });
    }


    private void getLocationFromDB() {
        Cursor query = db.rawQuery("SELECT * FROM " + DBHelper.DB_TABLE_NAME_CHECKED, null);
        query.moveToFirst();
        weatherCityTextFromDB = query.getString(1);
        lat = query.getString(2);
        lon = query.getString(3);
        query.close();
        db.close();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.weatherCity) {
            Intent intent = new Intent(MainActivity.this, ListLocationActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.buttonInformation) {
            Intent intent = new Intent(MainActivity.this, InformationActivity.class);
            startActivity(intent);
        }
    }

    private ApiRecord tryGetCustomApiKeyFromDB(SQLiteDatabase db) {
        return dbHelper.getApiRecord(db);
    }

    @Override
    public void onPause() {

        timerWeather.cancel();
        super.onPause();
    }

    @Override
    public void onRestart() {

        super.onRestart();
        db = dbHelper.getWritableDatabase();
        apiKeyRecord = tryGetCustomApiKeyFromDB(db);
        try {
            DEFAULT_API_KEY = new String(Base64.decode(getResources().getString(R.string.valueApiKey), Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void firstStartApp(boolean isFirstStart) {
        if (isFirstStart) {
            dbHelper.prepareAllDataBases(db);
        }
    }
}
package com.github.linarusakova.universehomeclock;

public class Location {
    private String locationName;
    private String latitude;
    private String longitude;

    boolean defaultValue = false;

//    public static final Location[] startLocationDB = {
//            new Location("Коломяги", 60.029189f, 30.248826f),
//            new Location("Псков", 57.815497f, 28.333912f)
//    };

    public Location(String locationName, String latitude, String longitude) {
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location() {

    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return locationName +
                ": lat " + latitude +
                ", lon" + longitude
                ;
    }
    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}

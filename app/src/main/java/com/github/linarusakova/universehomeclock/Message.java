package com.github.linarusakova.universehomeclock;

public class Message {

    String nameLocation, nameCountry, nameState, latitude, longitude;

    @Override
    public String toString() {
        return "Message{" +
                "nameLocation='" + nameLocation + '\'' +
                ", nameCountry='" + nameCountry + '\'' +
                ", nameState='" + nameState + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }

    public Message(String nameLocation, String nameCountry, String nameState, String latitude, String longitude) {
        this.nameLocation = nameLocation;
        this.nameCountry = nameCountry;
        this.nameState = nameState;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

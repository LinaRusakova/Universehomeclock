package com.github.linarusakova.universehomeclock;

public class ApiRecord {
    private final String keyAPI;

    private boolean using;

    public String getKeyAPI() {
        return keyAPI;
    }

    public boolean isUsing() {
        return using;
    }

    @Override
    public String toString() {
        return "ApiRecord{" +
                "keyAPI='" + keyAPI + '\'' +
                ", using=" + using+
                '}';
    }

    public ApiRecord(String keyAPI, boolean isUsing) {
        this.keyAPI = keyAPI;
        this.using=isUsing;
    }
}

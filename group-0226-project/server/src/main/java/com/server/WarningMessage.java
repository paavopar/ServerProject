package com.server;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class WarningMessage {
    private String nickname;
    private Double latitude;
    private Double longitude;
    private String dangertype;
    private OffsetDateTime sent;
    private String areacode;
    private String weather;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public WarningMessage() {}
    
    public WarningMessage(String nickname, Double latitude, Double longitude, String dangertype, OffsetDateTime sent) {
        this.nickname = nickname;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dangertype = dangertype;
        this.sent = sent;

    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getDangertype() {
        return dangertype;
    }
    
    public void setDangertype(String dangertype) {
        this.dangertype = dangertype;
    }

    public String getSent() {
        return sent.format(FORMATTER);
    }

    public boolean setSent(String sent) {
        try{
        this.sent = OffsetDateTime.parse(sent, FORMATTER);
        } catch (Exception e ){
            return false;
        }
        return true;
    }

    public void setSent(final long time){
        Instant instant = Instant.ofEpochMilli(time);
        sent = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public long getLongTime(){
        return sent.toInstant().toEpochMilli();
    }

    public String getAreacode() {
        return areacode;
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }

    private String phonenumber;
    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }
    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }


}


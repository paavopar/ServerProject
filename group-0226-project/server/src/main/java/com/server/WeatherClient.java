package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WeatherClient {
    
    public String getWeather(final double latitude, final double longitude) throws MalformedURLException, IOException{
        try{
        String xml = "<coordinates>\n"
                + "  <latitude>" + latitude + "</latitude>\n"
                + "  <longitude>" + longitude + "</longitude>\n"
                + "</coordinates>";
        URL url = new URL("http://localhost:4001/weather");
        HttpURLConnection exchange = (HttpURLConnection) url.openConnection();

        exchange.setRequestMethod("POST");
        exchange.setRequestProperty("Content-Type", "application/xml");
        exchange.setDoOutput(true);

        OutputStream o = exchange.getOutputStream();
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        o.write(bytes);
        o.flush();
        o.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getInputStream()));
        String line;
        String returnString = "";
        while(null != (line = reader.readLine())){
            returnString+=line;
        }
        reader.close();
        exchange.disconnect();
        String [] weather = returnString.split("<temperature>|</temperature><Unit>|</Unit>");
        return (weather[1].trim() + " " + weather[2].trim());
    } catch (MalformedURLException mfe ){
        System.out.println("MalformedURLException happened");
    } catch (IOException ioe){
        System.out.println("IOException happened");
    }
    return null;
    }
}

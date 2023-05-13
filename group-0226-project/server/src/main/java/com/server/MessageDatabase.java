package com.server;

import java.io.File;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageDatabase {
    private static MessageDatabase dbInstance = null;
    private Connection dbConnection = null;
    private static SecureRandom secure = null;

    public static synchronized MessageDatabase getInstance() {
        if (null == dbInstance) {
            dbInstance = new MessageDatabase();
        }
        return dbInstance;
    }

    private MessageDatabase() {
    }

    public synchronized void open(final String dbName) {
        try {
            secure = new SecureRandom();
            boolean exists = new File(dbName).exists();
            String database = "jdbc:sqlite:" + dbName;
            dbConnection = DriverManager.getConnection(database);

            if (!exists) {
                init();
            }
        } catch (SQLException se) {
            System.out.println("SQLException happened while opening:" + se.getMessage());
        } catch (Exception e) {

        }
    }

    public void init() throws SQLException {
        if (null != dbConnection) {
            String user = "create table users (username varchar(50) NOT NULL, password varchar(50) NOT NULL, email varchar(50), primary key(username))";
            String message = "create table messages (nickname varchar(50) NOT NULL, " +
                    "latitude REAL NOT NULL, longitude REAL NOT NULL, sent BIGINT NOT NULL, dangertype varchar(100) NOT NULL, areacode VARCHAR(20), "
                    +
                    "phonenumber VARCHAR(20), weather VARCHAR (50), primary key(nickname, sent))";
            try (Statement createStatement = dbConnection.createStatement()) {
                createStatement.executeUpdate(user);
                createStatement.executeUpdate(message);
            } catch (SQLException e) {
                System.out.println("SQLException happened while initializing: " + e.getMessage());
            }
        }
    }

    public void close() throws SQLException {
        if (dbConnection != null) {
            dbConnection.close();
        }
        dbConnection = null;
    }

    public boolean addUser(final String username, final String password, final String email) throws SQLException {
        byte[] bytes = new byte[13];
        secure.nextBytes(bytes);
        String saltBytes = Base64.getEncoder().encodeToString(bytes);
        String salt = "$6$" + saltBytes;
        String hashedPassword = Crypt.crypt(password, salt);
        String statement = "SELECT * FROM users WHERE username = " + "'" + username + "'";
        try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(statement);
            if (set.next()) {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("SQLException happened while adding user1: " + e.getMessage());
        }
        String insertStatement = "INSERT INTO users VALUES ('" + username + "', '" + hashedPassword + "', '" + email + "')";
        try (Statement createStatement = dbConnection.createStatement()) {
            int i = createStatement.executeUpdate(insertStatement);
            return (i > 0);
        } catch (SQLException e) {
            System.out.println("SQLException happened while adding user2: " + e.getMessage());
        }
        return false;
    }

    public boolean authenticate(final String username, final String password) {
        String statement = "SELECT * FROM users WHERE username = " + "'" + username + "'";
        try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(statement);
            if (!set.next()) {
                return false;
            }
            String hashedPasswordfromDB = set.getString("password");
            String passwordHash = Crypt.crypt(password, hashedPasswordfromDB);
            return passwordHash.equals(hashedPasswordfromDB);
        } catch (SQLException e) {
            System.out.println("SQLException happened while adding user: " + e.getMessage());
        } catch (Exception e) {

        }
        return false;
    }

    public boolean addMessage(final WarningMessage message) {
        String insertStatement = "INSERT INTO messages VALUES ('" + message.getNickname() + "', "
                + message.getLatitude() + ", " + message.getLongitude() + ", " +
                message.getLongTime() + ", " + "'" + message.getDangertype() + "'";
        if (message.getAreacode() != null) {
            insertStatement += ", " + "'" + message.getAreacode() + "'";
        } else {
            insertStatement += ", NULL"; 
        }
        if (message.getPhonenumber() != null) {
            insertStatement += ", " + "'" + message.getPhonenumber() + "'";
        } else {
            insertStatement += ", NULL"; 
        }
        if (message.getWeather() != null) {
            insertStatement += ", " + "'" + message.getWeather() + "'";
        } else {
            insertStatement += ", NULL"; 
        }
        insertStatement += ")";

        try (Statement createStatement = dbConnection.createStatement()) {
            int i = createStatement.executeUpdate(insertStatement);
            return (i > 0);
        } catch (SQLException e) {
            System.out.println("SQLException happened while adding message: " + e.getMessage());
        }
        return false;
    }

    public JSONArray getMessages() throws JSONException {
        String queryStatement = "SELECT * FROM messages";
        try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(queryStatement);
            return getJsonArray(set);
        } catch (SQLException e) {
            System.out.println("SQLException happened while getting messages: " + e.getMessage());
        } return null;
    }

    public JSONArray getMessagesWithinTimePeriod(long start, long end)throws SQLException{
        String queryStatement = "SELECT * FROM messages WHERE sent > " + start + " AND " + 
        "sent < " +end;
        try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(queryStatement);
            return getJsonArray(set);
        } catch (SQLException e) {
            System.out.println("SQLException happened while getting messages within a time period: " + e.getMessage());
        } return null;
    }

    public JSONArray getMessagesInArea(double longitudeup, double longitudedown, double latitudeup, double latitudedown) throws SQLException{
        String queryStatement = "SELECT * FROM messages WHERE latitude >= " + latitudedown + " AND " + "latitude <= " + latitudeup + " AND " +
         "longitude >= " + longitudedown + " AND " + "longitude <= " + longitudeup;
         try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(queryStatement);
            return getJsonArray(set);
        } catch (SQLException e) {
            System.out.println("SQLException happened while getting messages from a certain area: " + e.getMessage());
        } return null;
    }

    public JSONArray getMessagesFromUser(final String user) throws SQLException {
        String queryStatement = "SELECT * FROM messages WHERE nickname = " + "'" + user + "'";
        try (Statement createStatement = dbConnection.createStatement()) {
            ResultSet set = createStatement.executeQuery(queryStatement);
            return getJsonArray(set);
        } catch (SQLException e) {
            System.out.println("SQLException happened while getting message from user: " + e.getMessage());
        } return null;
    }

    private JSONArray getJsonArray(ResultSet set) throws SQLException{
        JSONArray jsonArr = new JSONArray();
        while (set.next()){
            JSONObject o = new JSONObject();
            o.put("nickname", set.getString("nickname"));
            o.put("latitude", set.getDouble("latitude"));
            o.put("longitude", set.getDouble("longitude"));
            o.put("dangertype", set.getString("dangertype"));
            o.put("nickname", set.getString("nickname"));
            WarningMessage warningmsg = new WarningMessage();
            warningmsg.setSent(set.getLong("sent"));
            o.put("sent", warningmsg.getSent());
            if (set.getString("areacode")!= null){
                o.put("areacode", set.getString("areacode"));
            }
            if (set.getString("phonenumber")!= null ){
                o.put("phonenumber", set.getString("phonenumber"));
            }
            if (set.getString("weather")!= null ){
                o.put("weather", set.getString("weather"));
            }
            jsonArr.put(o);
        } return jsonArr;
    }
}

package com.server;

import com.sun.net.httpserver.*;

import java.sql.SQLException;

public class UserAuthenticator extends BasicAuthenticator {
    
    private static MessageDatabase db;
    private static final String DBNAME = "Database";

    public UserAuthenticator() {
        super("warning");
        db = MessageDatabase.getInstance();
    }

    @Override
    public boolean checkCredentials(String username, String password){
        boolean isValid = false;
        db.open(DBNAME);
        isValid = db.authenticate(username, password);
        try {
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isValid;
    }

    public boolean addUser(String username, String password, String email) throws SQLException {
        db.open(DBNAME);
        boolean added = db.addUser(username, password, email);
        db.close();
        return added;
    }
}
package com.unibazaar.services;

import com.unibazaar.dtos.UserProfileDTO;

public class Session {
    private static Session instance;
    private UserProfileDTO currentUser;
    private String accessToken;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public UserProfileDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserProfileDTO currentUser) {
        this.currentUser = currentUser;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void clear() {
        this.currentUser = null;
        this.accessToken = null;
    }
}

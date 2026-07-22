package com.unibazaar.services.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.unibazaar.daos.UserDAO;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.repositories.UserRepository;
import com.unibazaar.services.IAuthService;
import com.unibazaar.services.Session;

import java.util.Optional;

public class AuthServiceImpl implements IAuthService {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final UserRepository userRepository = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    public void register(String name, String email, String password, String role) {
        JsonObject authBody = new JsonObject();
        authBody.addProperty("email", email);
        authBody.addProperty("password", password);

        String authResponse = client.postAuth("signup", gson.toJson(authBody));
        JsonObject authResObj = gson.fromJson(authResponse, JsonObject.class);
        
        String userId;
        if (authResObj.has("user") && !authResObj.get("user").isJsonNull()) {
            userId = authResObj.getAsJsonObject("user").get("id").getAsString();
        } else if (authResObj.has("id")) {
            userId = authResObj.get("id").getAsString();
        } else {
            throw new RuntimeException("Unexpected signup response from Supabase.");
        }

        JsonObject userBody = new JsonObject();
        userBody.addProperty("id", userId);
        userBody.addProperty("name", name);
        userBody.addProperty("email", email);
        userBody.addProperty("role", role);
        userBody.addProperty("is_gpa_verified", false);

        client.post("users", gson.toJson(userBody));
    }

    @Override
    public void login(String email, String password) {
        JsonObject authBody = new JsonObject();
        authBody.addProperty("email", email);
        authBody.addProperty("password", password);

        String authResponse = client.postAuth("token?grant_type=password", gson.toJson(authBody));
        JsonObject authResObj = gson.fromJson(authResponse, JsonObject.class);

        String accessToken = authResObj.get("access_token").getAsString();
        String userId = authResObj.getAsJsonObject("user").get("id").getAsString();

        Optional<UserProfileDTO> profileOpt = userRepository.findById(userId);
        if (profileOpt.isEmpty()) {
            throw new RuntimeException("User profile not found in public schema.");
        }

        Session.getInstance().setAccessToken(accessToken);
        Session.getInstance().setCurrentUser(profileOpt.get());
    }

    @Override
    public void closeAccount(String userId) {
        userRepository.deleteUser(userId);
    }
}

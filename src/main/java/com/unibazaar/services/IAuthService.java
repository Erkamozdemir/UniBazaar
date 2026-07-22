package com.unibazaar.services;

public interface IAuthService {
    void register(String name, String email, String password, String role);
    void login(String email, String password);
    void closeAccount(String userId);
}

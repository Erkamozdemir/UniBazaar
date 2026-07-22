package com.unibazaar.controllers;

import com.unibazaar.services.IAuthService;
import com.unibazaar.services.impl.AuthServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    private final IAuthService authService = new AuthServiceImpl();

    @FXML
    private void onLoginClicked() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            statusLabel.setText("Please fill all fields.");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Logging in...");

        Thread.startVirtualThread(() -> {
            try {
                authService.login(email, password);
                Platform.runLater(this::navigateToDashboard);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Login failed: " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onRegisterLinkClicked() {
        navigateTo("/com/unibazaar/register.fxml", "Register - UniBazaar");
    }

    private void navigateToDashboard() {
        com.unibazaar.dtos.UserProfileDTO user = com.unibazaar.services.Session.getInstance().getCurrentUser();
        if (user != null && "SELLER".equals(user.role())) {
            navigateTo("/com/unibazaar/seller-dashboard.fxml", "UniBazaar");
        } else {
            navigateTo("/com/unibazaar/dashboard.fxml", "UniBazaar - Dashboard");
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            statusLabel.setText("Navigation failed.");
        }
    }
}

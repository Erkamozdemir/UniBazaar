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

public class RegisterController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button studentRoleBtn;
    @FXML
    private Button sellerRoleBtn;
    @FXML
    private Label statusLabel;
    @FXML
    private Button registerButton;

    private final IAuthService authService = new AuthServiceImpl();
    private String selectedRole = "STUDENT";

    @FXML
    public void initialize() {
        updateRoleButtons();
    }

    @FXML
    private void onStudentRoleClicked() {
        selectedRole = "STUDENT";
        updateRoleButtons();
    }

    @FXML
    private void onSellerRoleClicked() {
        selectedRole = "SELLER";
        updateRoleButtons();
    }

    private void updateRoleButtons() {
        studentRoleBtn.getStyleClass().remove("category-chip-active");
        sellerRoleBtn.getStyleClass().remove("category-chip-active");

        if ("STUDENT".equals(selectedRole)) {
            studentRoleBtn.getStyleClass().add("category-chip-active");
        } else {
            sellerRoleBtn.getStyleClass().add("category-chip-active");
        }
    }

    @FXML
    private void onRegisterClicked() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = selectedRole;

        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            statusLabel.setText("Please fill all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        if ("STUDENT".equals(role) && !email.endsWith(".edu.tr")) {
            statusLabel.setText("Student role requires an .edu.tr email address.");
            return;
        }

        registerButton.setDisable(true);
        statusLabel.setText("Registering...");

        Thread.startVirtualThread(() -> {
            try {
                authService.register(name, email, password, role);
                Platform.runLater(() -> {
                    statusLabel.setText("Success! Check your email to verify your account.");
                    statusLabel.setStyle("-fx-text-fill: #10b981;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Registration failed: " + e.getMessage());
                    registerButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onLoginLinkClicked() {
        try {
            Stage stage = (Stage) registerButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/unibazaar/login.fxml"));
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login - UniBazaar");
        } catch (IOException e) {
            statusLabel.setText("Navigation failed.");
        }
    }
}

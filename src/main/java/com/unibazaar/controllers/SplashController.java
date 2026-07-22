package com.unibazaar.controllers;

import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.services.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SplashController {

    @FXML private Label welcomeBackLabel;
    @FXML private HBox signInBox;

    @FXML
    public void initialize() {
        UserProfileDTO user = Session.getInstance().getCurrentUser();
        if (user != null) {
            welcomeBackLabel.setText("Welcome Back, " + user.name());
            welcomeBackLabel.setVisible(true);
            welcomeBackLabel.setManaged(true);
            signInBox.setVisible(false);
            signInBox.setManaged(false);

            welcomeBackLabel.setOnMouseClicked(e -> {
                if ("SELLER".equals(user.role())) {
                    navigateTo("/com/unibazaar/seller-dashboard.fxml", "UniBazaar - Seller Panel");
                } else {
                    navigateTo("/com/unibazaar/dashboard.fxml", "UniBazaar");
                }
            });
        }
    }

    @FXML
    private void onSignInClicked() {
        navigateTo("/com/unibazaar/login.fxml", "Sign In - UniBazaar");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            Stage stage = (Stage) welcomeBackLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException ignored) {}
    }
}

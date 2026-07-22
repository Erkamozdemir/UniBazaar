package com.unibazaar.controllers;

import com.unibazaar.dtos.CategoryDTO;
import com.unibazaar.services.ICloudinaryService;
import com.unibazaar.services.IListingService;
import com.unibazaar.services.impl.CloudinaryServiceImpl;
import com.unibazaar.services.impl.ListingServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class AddListingController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<CategoryDTO> categoryComboBox;
    @FXML private TextField brandField;
    @FXML private TextField priceField;
    @FXML private TextField courseCodeField;
    @FXML private ImageView previewImage;
    @FXML private Label imagePathLabel;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private final IListingService listingService = new ListingServiceImpl();
    private final ICloudinaryService cloudinaryService = new CloudinaryServiceImpl();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        loadCategories();
    }

    private void loadCategories() {
        Thread.startVirtualThread(() -> {
            try {
                List<CategoryDTO> categories = listingService.getAllCategories();
                Platform.runLater(() -> {
                    categoryComboBox.getItems().clear();
                    categoryComboBox.getItems().addAll(categories);
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load categories"));
            }
        });
    }

    @FXML
    private void onSelectImageClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imagePathLabel.setText(file.getName());
            previewImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void onSaveClicked() {
        if (titleField.getText().isBlank() || priceField.getText().isBlank()) {
            statusLabel.setText("Title and Price are required.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText());
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid price format.");
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setText("Saving...");

        Thread.startVirtualThread(() -> {
            try {
                String imageUrl = null;
                if (selectedImageFile != null) {
                    imageUrl = cloudinaryService.uploadImage(selectedImageFile);
                }

                Integer categoryId = categoryComboBox.getValue() != null ? categoryComboBox.getValue().id() : null;
                String sellerId = com.unibazaar.services.Session.getInstance().getCurrentUser().id();

                listingService.createListing(
                    sellerId,
                    categoryId,
                    titleField.getText(),
                    descriptionField.getText().isBlank() ? null : descriptionField.getText(),
                    price,
                    imageUrl,
                    courseCodeField.getText().isBlank() ? null : courseCodeField.getText(),
                    brandField.getText().isBlank() ? null : brandField.getText()
                );

                Platform.runLater(() -> navigateToDashboard());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to save: " + e.getMessage());
                    saveButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onGoBackClicked() {
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        try {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/com/unibazaar/seller-dashboard.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("UniBazaar");
        } catch (IOException ignored) {}
    }
}

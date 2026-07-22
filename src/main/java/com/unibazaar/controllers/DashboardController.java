package com.unibazaar.controllers;

import com.unibazaar.dtos.CategoryDTO;
import com.unibazaar.dtos.ListingSummaryDTO;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.services.IListingService;
import com.unibazaar.services.Session;
import com.unibazaar.services.impl.ListingServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.unibazaar.services.IAuthService;
import com.unibazaar.services.impl.AuthServiceImpl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Cursor;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.stage.FileChooser;
import java.io.File;
import java.math.BigDecimal;

public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label userInfoLabel;
    @FXML
    private TextField searchField;
    @FXML
    private HBox categoryChipsBox;
    @FXML
    private Label statusLabel;
    @FXML
    private FlowPane listingsGrid;
    @FXML
    private VBox interactionsSection;
    @FXML
    private HBox interactionsBox;

    private final IListingService listingService = new ListingServiceImpl();
    private final com.unibazaar.services.IChatService chatService = new com.unibazaar.services.impl.ChatServiceImpl();
    private final IAuthService authService = new AuthServiceImpl();
    private Integer selectedCategoryId = null;

    @FXML
    public void initialize() {
        UserProfileDTO user = Session.getInstance().getCurrentUser();
        if (user != null) {
            if (user.gpa() != null && user.isGpaVerified()) {
                userInfoLabel.setText(user.role() + " • GPA: " + user.gpa().toPlainString() + " ✓");
            } else {
                userInfoLabel.setText(user.role() + " (GPA Unverified)");
            }
        }

        loadCategoryChips();
        loadListings(null, null);
        loadInteractions();
    }

    private void loadInteractions() {
        UserProfileDTO user = Session.getInstance().getCurrentUser();
        if (user == null)
            return;

        Thread.startVirtualThread(() -> {
            try {
                List<com.unibazaar.dtos.ChatDTO> chats = chatService.getChatsForUser(user.id());
                List<Integer> listingIds = chats.stream().map(com.unibazaar.dtos.ChatDTO::listingId).distinct()
                        .toList();

                if (listingIds.isEmpty()) {
                    Platform.runLater(() -> interactionsSection.setVisible(false));
                    Platform.runLater(() -> interactionsSection.setManaged(false));
                    return;
                }

                List<ListingSummaryDTO> interactedListings = listingService.getListingsByIds(listingIds);
                Platform.runLater(() -> {
                    interactionsBox.getChildren().clear();
                    for (ListingSummaryDTO listing : interactedListings) {
                        VBox card = createListingCard(listing);
                        interactionsBox.getChildren().add(card);
                    }
                    interactionsSection.setVisible(true);
                    interactionsSection.setManaged(true);
                });
            } catch (Exception ignored) {
            }
        });
    }

    @FXML
    private void onSearchAction() {
        String query = searchField.getText();
        loadListings(query.isBlank() ? null : query, selectedCategoryId);
    }

    private void loadCategoryChips() {
        Thread.startVirtualThread(() -> {
            try {
                List<CategoryDTO> categories = listingService.getAllCategories();
                List<CategoryDTO> roots = categories.stream()
                        .filter(c -> c.parentId() == null)
                        .toList();

                Platform.runLater(() -> {
                    categoryChipsBox.getChildren().clear();

                    Button allChip = new Button("All");
                    allChip.getStyleClass().addAll("category-chip", "category-chip-active");
                    allChip.setOnAction(e -> {
                        selectedCategoryId = null;
                        setActiveChip(allChip);
                        loadListings(searchField.getText().isBlank() ? null : searchField.getText(), null);
                    });
                    categoryChipsBox.getChildren().add(allChip);

                    for (CategoryDTO cat : roots) {
                        Button chip = new Button(cat.name());
                        chip.getStyleClass().add("category-chip");
                        chip.setOnAction(e -> {
                            selectedCategoryId = cat.id();
                            setActiveChip(chip);
                            loadListings(searchField.getText().isBlank() ? null : searchField.getText(), cat.id());
                        });
                        categoryChipsBox.getChildren().add(chip);
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void setActiveChip(Button active) {
        for (var node : categoryChipsBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().remove("category-chip-active");
            }
        }
        if (!active.getStyleClass().contains("category-chip-active")) {
            active.getStyleClass().add("category-chip-active");
        }
    }

    private void loadListings(String query, Integer categoryId) {
        statusLabel.setText("Loading...");
        Thread.startVirtualThread(() -> {
            try {
                List<ListingSummaryDTO> listings = listingService.getAllListings(query, categoryId, null, null, null);
                Platform.runLater(() -> displayListings(listings));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load listings: " + e.getMessage()));
            }
        });
    }

    private void displayListings(List<ListingSummaryDTO> listings) {
        listingsGrid.getChildren().clear();

        if (listings.isEmpty()) {
            statusLabel.setText("No listings found.");
            return;
        }

        statusLabel.setText(listings.size() + " listings found");

        for (ListingSummaryDTO listing : listings) {
            VBox card = createListingCard(listing);
            listingsGrid.getChildren().add(card);
        }
    }

    private VBox createListingCard(ListingSummaryDTO listing) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(260);
        card.getStyleClass().add("listing-card");
        card.setCursor(Cursor.HAND);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(232);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        if (listing.imageUrl() != null && !listing.imageUrl().isBlank()) {
            try {
                imageView.setImage(new Image(listing.imageUrl(), true));
            } catch (Exception ignored) {
            }
        }

        Label titleLabel = new Label(listing.title());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        Label catLabel = new Label(listing.categoryName() != null ? listing.categoryName() : "");
        catLabel.getStyleClass().add("card-category");

        Label priceLabel = new Label("₺" + listing.originalPrice().toPlainString());
        priceLabel.getStyleClass().add("card-price");

        card.getChildren().addAll(imageView, titleLabel, catLabel, priceLabel);
        card.setOnMouseClicked(event -> openListingDetail(listing.id()));
        return card;
    }

    private void openListingDetail(int listingId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unibazaar/listing-detail.fxml"));
            Parent root = loader.load();
            ListingDetailController controller = loader.getController();
            controller.setListingId(listingId);

            Stage stage = (Stage) listingsGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            statusLabel.setText("Failed to open listing details.");
        }
    }

    @FXML
    private void onMyChatsClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unibazaar/chat.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) listingsGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            statusLabel.setText("Failed to open chats.");
        }
    }

    @FXML
    private void onVerifyGpa() {
        UserProfileDTO user = Session.getInstance().getCurrentUser();
        if (user == null) return;
        if (user.isGpaVerified()) {
            showAlert(Alert.AlertType.INFORMATION, "Already Verified", "Your GPA is already verified.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Transcript PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File transcriptFile = fileChooser.showOpenDialog(listingsGrid.getScene().getWindow());

        if (transcriptFile != null) {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Verify GPA");
            dialog.setHeaderText("Transcript selected: " + transcriptFile.getName());
            dialog.setContentText("Please enter your current CGPA (e.g. 3.50):");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    BigDecimal gpa = new BigDecimal(result.get());
                    statusLabel.setText("Uploading transcript...");
                    Thread.startVirtualThread(() -> {
                        try {
                            String url = new com.unibazaar.services.impl.CloudinaryServiceImpl().uploadImage(transcriptFile);
                            new com.unibazaar.daos.UserDAO().verifyGpa(user.id(), gpa, url);
                            
                            UserProfileDTO updatedUser = new com.unibazaar.daos.UserDAO().findById(user.id()).get();
                            Session.getInstance().setCurrentUser(updatedUser);
                            
                            Platform.runLater(() -> {
                                userInfoLabel.setText(updatedUser.role() + " • GPA: " + updatedUser.gpa().toPlainString() + " ✓");
                                showAlert(Alert.AlertType.INFORMATION, "Success", "GPA verified successfully!");
                                statusLabel.setText("");
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> statusLabel.setText("Verification failed: " + e.getMessage()));
                        }
                    });
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid GPA.");
                }
            }
        }
    }

    @FXML
    private void onLogOut() {
        Session.getInstance().setCurrentUser(null);
        Session.getInstance().setAccessToken(null);
        navigateToSplash();
    }

    @FXML
    private void onCloseAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Close Account");
        alert.setHeaderText("Are you sure you want to close your account?");
        alert.setContentText("This action is permanent and cannot be undone. All your listings and chats will be deleted.");

        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                try {
                    String userId = Session.getInstance().getCurrentUser().id();
                    authService.closeAccount(userId);
                    onLogOut();
                } catch (Exception e) {
                    statusLabel.setText("Failed to close account: " + e.getMessage());
                }
            }
        });
    }

    private void navigateToSplash() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unibazaar/splash.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listingsGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            statusLabel.setText("Failed to log out.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

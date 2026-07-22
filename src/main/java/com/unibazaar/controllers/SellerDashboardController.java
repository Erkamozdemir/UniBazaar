package com.unibazaar.controllers;

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
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;

public class SellerDashboardController {

    @FXML private Label sellerNameLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane listingsGrid;
    @FXML private Button addListingBtn;

    private final IListingService listingService = new ListingServiceImpl();

    @FXML
    public void initialize() {
        if (addListingBtn != null) {
            addListingBtn.setOnMouseEntered(e -> {
                addListingBtn.setScaleX(1.05);
                addListingBtn.setScaleY(1.05);
            });
            addListingBtn.setOnMouseExited(e -> {
                addListingBtn.setScaleX(1.0);
                addListingBtn.setScaleY(1.0);
            });
        }

        UserProfileDTO user = Session.getInstance().getCurrentUser();
        if (user != null) {
            sellerNameLabel.setText("Welcome back, " + user.name());
            loadListings(user.id());
        } else {
            statusLabel.setText("User not logged in.");
        }
    }

    private void loadListings(String sellerId) {
        statusLabel.setText("Loading your listings...");
        Thread.startVirtualThread(() -> {
            try {
                List<ListingSummaryDTO> listings = listingService.getListingsBySeller(sellerId);
                Platform.runLater(() -> displayListings(listings));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load listings: " + e.getMessage()));
            }
        });
    }

    private void displayListings(List<ListingSummaryDTO> listings) {
        listingsGrid.getChildren().clear();

        if (listings.isEmpty()) {
            statusLabel.setText("You have no listings yet.");
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
        card.setPrefWidth(220);
        card.getStyleClass().add("listing-card");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(190);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        if (listing.imageUrl() != null && !listing.imageUrl().isBlank()) {
            try {
                imageView.setImage(new Image(listing.imageUrl(), true));
            } catch (Exception ignored) {}
        }

        Label titleLabel = new Label(listing.title());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        javafx.scene.control.MenuButton optionsMenu = new javafx.scene.control.MenuButton("...");
        optionsMenu.setStyle("-fx-background-color: transparent; -fx-text-fill: #F0EBE3; -fx-font-weight: bold;");
        
        javafx.scene.control.MenuItem editPriceItem = new javafx.scene.control.MenuItem("Edit Price");
        editPriceItem.setOnAction(e -> openEditPriceDialog(listing));
        
        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete Listing");
        deleteItem.setOnAction(e -> confirmAndDeleteListing(listing));
        
        optionsMenu.getItems().addAll(editPriceItem, deleteItem);

        Button productChatBtn = new Button("💬");
        productChatBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7BC8A4; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 4 0 0;");
        productChatBtn.setOnAction(e -> openProductChats(listing.id()));

        javafx.scene.layout.HBox topBox = new javafx.scene.layout.HBox(productChatBtn, titleLabel, new javafx.scene.layout.Region(), optionsMenu);
        if (listing.status() != null && !"active".equals(listing.status())) {
            Label statusBadge = new Label(listing.status().toUpperCase());
            statusBadge.setStyle("-fx-background-color: #B53F45; -fx-text-fill: #F0EBE3; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            topBox.getChildren().add(1, statusBadge);
        }
        
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setSpacing(5);
        javafx.scene.layout.HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(topBox.getChildren().get(topBox.getChildren().size() - 2), javafx.scene.layout.Priority.ALWAYS);

        Label priceLabel = new Label("₺" + listing.originalPrice().toPlainString());
        priceLabel.getStyleClass().add("card-price");

        Button campaignBtn = new Button(listing.isCampaignActive() ? "✓ Campaigns Active" : "+ Add to Campaign");
        campaignBtn.getStyleClass().add("filter-button");
        campaignBtn.setMaxWidth(Double.MAX_VALUE);
        campaignBtn.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        campaignBtn.setWrapText(true);
        campaignBtn.setOnAction(e -> openCampaignsDialog(listing, campaignBtn));

        card.getChildren().addAll(imageView, topBox, priceLabel, campaignBtn);
        return card;
    }

    private void openEditPriceDialog(ListingSummaryDTO listing) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(listing.originalPrice().toPlainString());
        dialog.setTitle("Edit Price");
        dialog.setHeaderText("Enter new price for: " + listing.title());
        dialog.setContentText("Price (₺):");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("auth-box");

        dialog.showAndWait().ifPresent(newPriceStr -> {
            try {
                java.math.BigDecimal newPrice = new java.math.BigDecimal(newPriceStr);
                Thread.startVirtualThread(() -> {
                    try {
                        listingService.updatePrice(listing.id(), newPrice);
                        Platform.runLater(() -> {
                            statusLabel.setText("Price updated!");
                            loadListings(Session.getInstance().getCurrentUser().id());
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> statusLabel.setText("Failed to update price."));
                    }
                });
            } catch (NumberFormatException ex) {
                statusLabel.setText("Invalid price format.");
            }
        });
    }

    private void confirmAndDeleteListing(ListingSummaryDTO listing) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Listing");
        alert.setHeaderText("Delete: " + listing.title());
        alert.setContentText("Are you sure you want to delete this listing? This cannot be undone.");
        
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("auth-box");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Thread.startVirtualThread(() -> {
                    try {
                        listingService.deleteListing(listing.id());
                        Platform.runLater(() -> {
                            statusLabel.setText("Listing deleted.");
                            loadListings(Session.getInstance().getCurrentUser().id());
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> statusLabel.setText("Failed to delete listing: " + ex.getMessage()));
                    }
                });
            }
        });
    }

    private void openCampaignsDialog(ListingSummaryDTO listing, Button campaignBtn) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Select Campaigns");
        dialog.setHeaderText("Select campaigns for: " + listing.title());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("auth-box");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        CheckBox camp1 = new CheckBox("GPA > 3.0 (%5 Discount)");
        CheckBox camp2 = new CheckBox("GPA > 3.5 (%10 Discount)");
        CheckBox camp3 = new CheckBox("GPA > 3.8 (%15 Discount)");
        CheckBox camp4 = new CheckBox("University Specific (%10 Discount)");

        camp1.getStyleClass().add("auth-label");
        camp2.getStyleClass().add("auth-label");
        camp3.getStyleClass().add("auth-label");
        camp4.getStyleClass().add("auth-label");

        javafx.scene.control.ComboBox<String> uniComboBox = new javafx.scene.control.ComboBox<>();
        uniComboBox.getItems().addAll(loadUniversities());
        uniComboBox.setPromptText("Select University");
        uniComboBox.setVisible(false);
        uniComboBox.setManaged(false);

        camp4.setOnAction(e -> {
            uniComboBox.setVisible(camp4.isSelected());
            uniComboBox.setManaged(camp4.isSelected());
            if (dialogPane.getScene() != null && dialogPane.getScene().getWindow() != null) {
                dialogPane.getScene().getWindow().sizeToScene();
            }
        });

        
        if (listing.isCampaignActive()) {
            camp1.setSelected(true);
        }

        content.getChildren().addAll(camp1, camp2, camp3, camp4, uniComboBox);
        dialogPane.setContent(content);

        ButtonType saveButtonType = new ButtonType("Save Campaigns", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return camp1.isSelected() || camp2.isSelected() || camp3.isSelected() || (camp4.isSelected() && uniComboBox.getValue() != null);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(isActive -> {
            Thread.startVirtualThread(() -> {
                try {
                    String targetUni = camp4.isSelected() ? uniComboBox.getValue() : null;
                    listingService.toggleCampaignStatus(listing.id(), isActive, targetUni);
                    Platform.runLater(() -> {
                        statusLabel.setText("Campaigns updated: " + listing.title());
                        campaignBtn.setText(isActive ? "✓ Campaigns Active" : "+ Add to Campaign");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLabel.setText("Failed to update campaigns."));
                }
            });
        });
    }

    private List<String> loadUniversities() {
        try {
            String content = java.nio.file.Files.readString(java.nio.file.Paths.get(System.getProperty("user.home"), ".gemini", "antigravity", "brain", "e81745c0-8102-4378-b8ee-c8e402a152aa", "scratch", "universities.json"));
            return new com.google.gson.Gson().fromJson(content, new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
        } catch (Exception e) {
            return List.of("Failed to load universities");
        }
    }

    @FXML
    private void onAddListingClicked() {
        try {
            Stage stage = (Stage) sellerNameLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/unibazaar/add-listing.fxml"));
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("UniBazaar - Add Listing");
        } catch (IOException e) {
            statusLabel.setText("Failed to open add listing screen.");
        }
    }

    @FXML
    private void onCloseAccountClicked() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Close Account");
        alert.setHeaderText("Are you sure you want to close your account?");
        alert.setContentText("This action is permanent and cannot be undone. All your listings and chats will be deleted.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("auth-box");

        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                try {
                    String userId = Session.getInstance().getCurrentUser().id();
                    new com.unibazaar.services.impl.AuthServiceImpl().closeAccount(userId);
                    onLogoutClicked();
                } catch (Exception e) {
                    statusLabel.setText("Failed to close account: " + e.getMessage());
                }
            }
        });
    }

    private void openProductChats(int listingId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unibazaar/chat.fxml"));
            Parent root = loader.load();
            
            com.unibazaar.controllers.ChatController controller = loader.getController();
            controller.setFilterListingId(listingId);
            
            Stage stage = (Stage) listingsGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            statusLabel.setText("Failed to open product chats.");
        }
    }

    @FXML
    private void onLogoutClicked() {
        Session.getInstance().clear();
        try {
            Stage stage = (Stage) sellerNameLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/unibazaar/splash.fxml"));
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("UniBazaar");
        } catch (IOException ignored) {}
    }
}

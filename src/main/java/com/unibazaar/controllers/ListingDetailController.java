package com.unibazaar.controllers;

import com.unibazaar.daos.UserDAO;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.repositories.UserRepository;
import com.unibazaar.services.IListingService;
import com.unibazaar.services.ITransactionService;
import com.unibazaar.services.impl.ListingServiceImpl;
import com.unibazaar.services.impl.TransactionServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

public class ListingDetailController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label courseCodeLabel;
    @FXML
    private Label originalPriceLabel;
    @FXML
    private Label discountPriceLabel;
    @FXML
    private ImageView listingImage;
    @FXML
    private Button purchaseButton;
    @FXML
    private Button offerButton;
    @FXML
    private Label campaignBadge;

    @FXML
    private Label ratingLabel;
    @FXML
    private Label conditionLabel;
    @FXML
    private Label locationLabel;

    private final IListingService listingService = new ListingServiceImpl();
    private final ITransactionService transactionService = new TransactionServiceImpl();
    private final UserRepository userRepository = new UserDAO();

    private int listingId;
    private String currentBuyerId = com.unibazaar.services.Session.getInstance().getCurrentUser() != null
            ? com.unibazaar.services.Session.getInstance().getCurrentUser().id()
            : null;
    private ListingDetailDTO currentListing;

    public void setListingId(int listingId) {
        this.listingId = listingId;
        loadListingDetail();
    }

    public void setBuyerId(String buyerId) {
        this.currentBuyerId = buyerId;
    }

    private void loadListingDetail() {
        Thread.startVirtualThread(() -> {
            try {
                Optional<ListingDetailDTO> opt = listingService.getListingById(listingId);
                if (opt.isPresent()) {
                    currentListing = opt.get();
                    Platform.runLater(this::populateUI);
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Listing not found."));
                }
            } catch (Exception e) {
                Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Error", "Failed to load listing: " + e.getMessage()));
            }
        });
    }

    private void populateUI() {
        try {
            Optional<UserProfileDTO> sellerOpt = new com.unibazaar.daos.UserDAO().findById(currentListing.sellerId());
            if (sellerOpt.isPresent()) {
                UserProfileDTO seller = sellerOpt.get();
                String ratingStr = seller.reviewCount() > 0 ? " (" + seller.rating().toPlainString() + " ★, " + seller.reviewCount() + " reviews)" : " (No reviews yet)";
                sellerLabel.setText("Sold by: " + seller.name() + ratingStr);
            } else {
                sellerLabel.setText("Sold by: " + currentListing.sellerName());
            }
        } catch (Exception e) {
            sellerLabel.setText("Sold by: " + currentListing.sellerName());
        }
        titleLabel.setText(currentListing.title());
        descriptionLabel.setText(currentListing.description());
        courseCodeLabel.setText(currentListing.courseCode() != null ? currentListing.courseCode() : "");
        originalPriceLabel.setText("\u20BA" + currentListing.originalPrice().toPlainString());

        conditionLabel
                .setText("Condition: " + (currentListing.condition() != null ? currentListing.condition() : "Used"));
        locationLabel
                .setText("Location: " + (currentListing.location() != null ? currentListing.location() : "Campus"));

        Thread.startVirtualThread(() -> {
            try {
                Optional<UserProfileDTO> sellerOpt = userRepository.findById(currentListing.sellerId());
                Platform.runLater(() -> {
                    if (sellerOpt.isPresent()) {
                        UserProfileDTO seller = sellerOpt.get();
                        ratingLabel.setText("Seller Rating: ⭐ " + seller.rating().toPlainString() + " ("
                                + seller.reviewCount() + " reviews)");
                    }
                });
            } catch (Exception ignored) {
            }
        });

        if (currentListing.imageUrl() != null && !currentListing.imageUrl().isBlank()) {
            try {
                listingImage.setImage(new Image(currentListing.imageUrl(), true));
            } catch (Exception ignored) {
            }
        }

        if (currentListing.isCampaignActive()) {
            campaignBadge.setText("\uD83C\uDF93 GPA Discount Campaign Active");
            campaignBadge.setVisible(true);
            loadDiscountPrice();
        } else {
            campaignBadge.setVisible(false);
            discountPriceLabel.setText("");
        }
    }

    private void loadDiscountPrice() {
        Thread.startVirtualThread(() -> {
            try {
                Optional<UserProfileDTO> buyerOpt = userRepository.findById(currentBuyerId);
                if (buyerOpt.isPresent()) {
                    BigDecimal finalPrice = listingService.calculateFinalPrice(buyerOpt.get(), currentListing);
                    Platform.runLater(() -> {
                        if (finalPrice.compareTo(currentListing.originalPrice()) < 0) {
                            discountPriceLabel.setText("Your Price: \u20BA" + finalPrice.toPlainString());
                            discountPriceLabel.getStyleClass().add("discount-price");
                            originalPriceLabel.getStyleClass().add("strikethrough");
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> discountPriceLabel.setText(""));
            }
        });
    }

    @FXML
    private void onPurchaseClicked() {
        purchaseButton.setDisable(true);
        Thread.startVirtualThread(() -> {
            try {
                transactionService.purchaseItem(currentBuyerId, listingId);

                
                Optional<com.unibazaar.dtos.ChatDTO> chatOpt = chatService.startOrGetChat(listingId, currentBuyerId,
                        currentListing.sellerId());
                if (chatOpt.isPresent()) {
                    int chatId = chatOpt.get().id();
                    chatService.sendMessage(chatId, currentBuyerId,
                            "I have purchased this item! Let's arrange a handoff time and location.");
                    chatService.closeListingChats(listingId, chatId);
                }

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Purchase completed successfully!\nA message has been sent to the seller.");
                    purchaseButton.setText("Purchased \u2713");
                });
            } catch (SecurityException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Access Denied", e.getMessage());
                    purchaseButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Purchase failed: " + e.getMessage());
                    purchaseButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onMakeOfferClicked() {
        if (currentListing == null || currentBuyerId == null) return;

        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Make an Offer");
        dialog.setHeaderText("Original Price: \u20BA" + currentListing.originalPrice().toPlainString());
        
        BigDecimal minOffer = currentListing.originalPrice().multiply(new BigDecimal("0.80"));
        dialog.setContentText(String.format("Enter your offer (Min: \u20BA%.2f, Max: \u20BA%.2f):", 
                                minOffer.doubleValue(), 
                                currentListing.originalPrice().doubleValue()));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal offer = new BigDecimal(result.get());
                if (offer.compareTo(minOffer) < 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Offer", "Your offer cannot be less than 80% of the original price.");
                    return;
                }
                if (offer.compareTo(currentListing.originalPrice()) > 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Offer", "Your offer cannot be higher than the original price.");
                    return;
                }

                offerButton.setDisable(true);
                Thread.startVirtualThread(() -> {
                    try {
                        transactionService.makeOffer(currentBuyerId, listingId, offer);

                        Optional<com.unibazaar.dtos.ChatDTO> chatOpt = chatService.startOrGetChat(listingId, currentBuyerId, currentListing.sellerId());
                        if (chatOpt.isPresent()) {
                            int chatId = chatOpt.get().id();
                            chatService.sendMessage(chatId, currentBuyerId, 
                                "I have made an offer of \u20BA" + offer.toPlainString() + " for this item!");
                        }

                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Offer Sent", "Your offer has been submitted and the seller has been notified!");
                            offerButton.setText("Offer Sent");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit offer: " + e.getMessage());
                            offerButton.setDisable(false);
                        });
                    }
                });
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid numeric amount.");
            }
        }
    }

    private final com.unibazaar.services.IChatService chatService = new com.unibazaar.services.impl.ChatServiceImpl();

    @FXML
    private void onMessageSellerClicked() {
        if (currentListing == null || currentBuyerId == null)
            return;

        Thread.startVirtualThread(() -> {
            try {
                Optional<com.unibazaar.dtos.ChatDTO> chatOpt = chatService.startOrGetChat(listingId, currentBuyerId,
                        currentListing.sellerId());
                if (chatOpt.isPresent()) {
                    Platform.runLater(() -> openChatScreen(chatOpt.get().id()));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Failed to initialize chat."));
                }
            } catch (Exception e) {
                Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Error", "Failed to start chat: " + e.getMessage()));
            }
        });
    }

    private void openChatScreen(int chatId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unibazaar/chat.fxml"));
            Parent root = loader.load();
            ChatController controller = loader.getController();
            controller.selectChat(chatId);

            Stage stage = (Stage) purchaseButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load chat screen.");
        }
    }

    @FXML
    private void onBackClicked() {
        try {
            com.unibazaar.dtos.UserProfileDTO currentUser = com.unibazaar.services.Session.getInstance()
                    .getCurrentUser();
            String fxmlPath = (currentUser != null && "SELLER".equals(currentUser.role()))
                    ? "/com/unibazaar/seller-dashboard.fxml"
                    : "/com/unibazaar/dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to dashboard.");
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

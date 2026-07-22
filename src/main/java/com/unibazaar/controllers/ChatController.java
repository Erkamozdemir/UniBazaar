package com.unibazaar.controllers;

import com.unibazaar.dtos.ChatDTO;
import com.unibazaar.dtos.MessageDTO;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.services.IChatService;
import com.unibazaar.services.Session;
import com.unibazaar.services.impl.ChatServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatController {

    @FXML private ListView<ChatDTO> chatsList;
    @FXML private Label chatTitleLabel;
    @FXML private Label statusLabel;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextArea messageInput;

    @FXML private HBox ratingBox;
    @FXML private javafx.scene.control.ComboBox<Integer> ratingComboBox;

    @FXML private HBox transactionBox;
    @FXML private Label transactionStatusLabel;
    @FXML private javafx.scene.control.Button confirmSaleButton;

    private final IChatService chatService = new ChatServiceImpl();
    private final com.unibazaar.services.ITransactionService transactionService = new com.unibazaar.services.impl.TransactionServiceImpl();
    private final com.unibazaar.daos.UserDAO userDAO = new com.unibazaar.daos.UserDAO();
    private final com.unibazaar.repositories.ListingRepository listingRepository = new com.unibazaar.daos.ListingDAO();
    private final com.unibazaar.repositories.TransactionRepository transactionRepo = new com.unibazaar.daos.TransactionDAO();

    private UserProfileDTO currentUser;
    private ChatDTO currentChat;
    private Timer pollingTimer;
    private Integer filterListingId = null;

    public void setFilterListingId(int listingId) {
        this.filterListingId = listingId;
        loadChats();
    }

    @FXML
    public void initialize() {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) {
            statusLabel.setText("You must be logged in to view chats.");
            return;
        }

        ratingComboBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingComboBox.getSelectionModel().select(4); 

        chatsList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ChatDTO chat, boolean empty) {
                super.updateItem(chat, empty);
                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(chat.listingTitle() + " - " + chat.otherParticipantName());
                    setStyle("-fx-text-fill: #F0EBE3; -fx-font-weight: bold;");
                }
            }
        });

        chatsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectChat(newVal.id());
            }
        });

        loadChats();
    }

    private void loadChats() {
        Thread.startVirtualThread(() -> {
            try {
                List<ChatDTO> chats = chatService.getChatsForUser(currentUser.id());
                if (filterListingId != null) {
                    chats = chats.stream().filter(c -> c.listingId() == filterListingId).toList();
                }
                final List<ChatDTO> finalChats = chats;
                Platform.runLater(() -> {
                    chatsList.getItems().setAll(finalChats);
                    if (!finalChats.isEmpty() && currentChat == null) {
                        chatsList.getSelectionModel().selectFirst();
                    } else if (finalChats.isEmpty()) {
                        statusLabel.setText("No messages yet for this product.");
                        chatTitleLabel.setText("");
                        messagesContainer.getChildren().clear();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load chats."));
            }
        });
    }

    public void selectChat(int chatId) {
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }

        Thread.startVirtualThread(() -> {
            try {
                List<ChatDTO> chats = chatService.getChatsForUser(currentUser.id());
                currentChat = chats.stream().filter(c -> c.id() == chatId).findFirst().orElse(null);
                
                if (currentChat != null) {
                    boolean hasPurchased = transactionService.hasPurchased(currentChat.listingId(), currentUser.id());
                    
                    Platform.runLater(() -> {
                        chatTitleLabel.setText(currentChat.listingTitle() + " with " + currentChat.otherParticipantName());
                        chatsList.getSelectionModel().select(currentChat);
                        
                        if (hasPurchased) {
                            ratingBox.setVisible(true);
                            ratingBox.setManaged(true);
                        } else {
                            ratingBox.setVisible(false);
                            ratingBox.setManaged(false);
                        }
                        
                        transactionBox.setVisible(false);
                        transactionBox.setManaged(false);
                        try {
                            var listingOpt = listingRepository.findById(currentChat.listingId());
                            if (listingOpt.isPresent() && "pending".equals(listingOpt.get().status())) {
                                String chatBuyerId = currentUser.id().equals(listingOpt.get().sellerId()) 
                                                        ? currentChat.otherParticipantId() 
                                                        : currentUser.id();
                                var transOpt = transactionRepo.findPendingByListingAndBuyer(currentChat.listingId(), chatBuyerId);
                                if (transOpt.isPresent()) {
                                    var trans = transOpt.get();
                                    boolean isBuyer = currentUser.id().equals(trans.buyerId());
                                    boolean isSeller = currentUser.id().equals(listingOpt.get().sellerId());
                                    
                                    boolean chatMatchesTransaction = (isBuyer && currentChat.otherParticipantId().equals(listingOpt.get().sellerId())) ||
                                                                     (isSeller && currentChat.otherParticipantId().equals(trans.buyerId()));

                                    if (chatMatchesTransaction) {
                                        transactionBox.setVisible(true);
                                        transactionBox.setManaged(true);
                                        
                                        String tStatus = trans.status();
                                        if (isBuyer && "buyer_confirmed".equals(tStatus)) {
                                            confirmSaleButton.setDisable(true);
                                            confirmSaleButton.setText("Confirmed");
                                            transactionStatusLabel.setText("Waiting for seller...");
                                        } else if (isSeller && "seller_confirmed".equals(tStatus)) {
                                            confirmSaleButton.setDisable(true);
                                            confirmSaleButton.setText("Confirmed");
                                            transactionStatusLabel.setText("Waiting for buyer...");
                                        } else {
                                            confirmSaleButton.setDisable(false);
                                            confirmSaleButton.setText("Confirm Sale");
                                            transactionStatusLabel.setText("Pending Transaction");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {}
                        
                        startPolling();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to select chat."));
            }
        });
    }

    private void startPolling() {
        loadMessages();
        pollingTimer = new Timer(true);
        pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentChat != null) {
                    loadMessages();
                }
            }
        }, 5000, 5000);
    }

    private void loadMessages() {
        if (currentChat == null) return;
        Thread.startVirtualThread(() -> {
            try {
                List<MessageDTO> messages = chatService.getMessagesForChat(currentChat.id());
                Platform.runLater(() -> displayMessages(messages));
            } catch (Exception ignored) {}
        });
    }

    private void displayMessages(List<MessageDTO> messages) {
        messagesContainer.getChildren().clear();
        for (MessageDTO msg : messages) {
            boolean isMe = msg.senderId().equals(currentUser.id());
            
            HBox row = new HBox();
            row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 10, 5, 10));

            Label bubble = new Label(msg.content());
            bubble.setWrapText(true);
            bubble.setMaxWidth(400);
            bubble.setPadding(new Insets(10));
            bubble.setStyle("-fx-background-radius: 15; " +
                            (isMe ? "-fx-background-color: #7BC8A4; -fx-text-fill: #0A1A14;" 
                                  : "-fx-background-color: #132A22; -fx-text-fill: #F0EBE3;"));

            row.getChildren().add(bubble);
            messagesContainer.getChildren().add(row);
        }
        messagesScroll.setVvalue(1.0); 
    }

    @FXML
    private void onSendClicked() {
        if (currentChat == null || messageInput.getText().isBlank()) return;
        String content = messageInput.getText().trim();
        messageInput.clear();

        Thread.startVirtualThread(() -> {
            try {
                chatService.sendMessage(currentChat.id(), currentUser.id(), content);
                loadMessages();
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to send message."));
            }
        });
    }

    @FXML
    private void onSubmitRating() {
        if (currentChat == null || ratingComboBox.getValue() == null) return;
        
        int rating = ratingComboBox.getValue();
        String sellerId = currentChat.otherParticipantId();
        int listingId = currentChat.listingId();
        
        Thread.startVirtualThread(() -> {
            try {
                userDAO.addReview(currentUser.id(), sellerId, listingId, rating, "Rated from chat");
                Platform.runLater(() -> {
                    ratingBox.setVisible(false);
                    ratingBox.setManaged(false);
                    statusLabel.setText("Rating submitted successfully!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to submit rating. You might have already rated this listing."));
            }
        });
    }

    @FXML
    private void onConfirmSale() {
        if (currentChat == null) return;
        Thread.startVirtualThread(() -> {
            try {
                var listingOpt = listingRepository.findById(currentChat.listingId());
                if (listingOpt.isEmpty()) return;
                String chatBuyerId = currentUser.id().equals(listingOpt.get().sellerId()) 
                                        ? currentChat.otherParticipantId() 
                                        : currentUser.id();
                boolean isBuyer = currentUser.id().equals(chatBuyerId);
                transactionService.confirmTransaction(currentChat.listingId(), chatBuyerId, isBuyer);
                Platform.runLater(() -> {
                    statusLabel.setText("Sale confirmed.");
                    selectChat(currentChat.id()); 
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to confirm sale."));
            }
        });
    }

    @FXML
    private void onCancelTransaction() {
        if (currentChat == null) return;
        Thread.startVirtualThread(() -> {
            try {
                var listingOpt = listingRepository.findById(currentChat.listingId());
                if (listingOpt.isEmpty()) return;
                String chatBuyerId = currentUser.id().equals(listingOpt.get().sellerId()) 
                                        ? currentChat.otherParticipantId() 
                                        : currentUser.id();
                transactionService.cancelTransaction(currentChat.listingId(), chatBuyerId);
                Platform.runLater(() -> {
                    statusLabel.setText("Transaction cancelled.");
                    selectChat(currentChat.id()); 
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to cancel transaction."));
            }
        });
    }

    @FXML
    private void onBackClicked() {
        try {
            stopPolling();
            String fxmlPath = (currentUser != null && "SELLER".equals(currentUser.role())) 
                ? "/com/unibazaar/seller-dashboard.fxml" 
                : "/com/unibazaar/dashboard.fxml";
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) chatsList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            statusLabel.setText("Failed to return to dashboard.");
        }
    }

    public void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
        }
    }
}
